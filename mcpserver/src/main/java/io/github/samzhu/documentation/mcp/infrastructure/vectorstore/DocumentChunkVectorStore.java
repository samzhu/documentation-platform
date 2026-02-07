package io.github.samzhu.documentation.mcp.infrastructure.vectorstore;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pgvector.PGvector;
import org.postgresql.util.PGobject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.document.DocumentMetadata;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.filter.FilterExpressionConverter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.util.StringUtils;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

/**
 * DocumentChunk VectorStore 唯讀實作
 * <p>
 * 實作 Spring AI VectorStore 介面，使用現有的 document_chunks 表進行向量搜尋。
 * 此為唯讀版本，僅支援 similaritySearch，不支援寫入操作。
 * </p>
 */
public class DocumentChunkVectorStore implements VectorStore {

    private static final Logger log = LoggerFactory.getLogger(DocumentChunkVectorStore.class);

    // Metadata 鍵名常數 - 用於 document_chunks.metadata JSONB 欄位
    public static final String METADATA_VERSION_ID = "versionId";
    public static final String METADATA_DOCUMENT_ID = "documentId";
    public static final String METADATA_CHUNK_INDEX = "chunkIndex";
    public static final String METADATA_TOKEN_COUNT = "tokenCount";
    public static final String METADATA_DOCUMENT_TITLE = "documentTitle";
    public static final String METADATA_DOCUMENT_PATH = "documentPath";

    // 相似度搜尋 SQL - 使用餘弦距離 (<=>)
    // distance = 1 - similarity，所以 distance < threshold 等同於 similarity > (1 - threshold)
    private static final String SQL_SIMILARITY_SEARCH = """
        SELECT dc.id, dc.document_id, dc.chunk_index, dc.content,
               dc.embedding, dc.token_count, dc.metadata,
               dc.embedding <=> ? AS distance
        FROM document_chunks dc
        WHERE dc.embedding IS NOT NULL
        %s
        AND dc.embedding <=> ? < ?
        ORDER BY distance
        LIMIT ?
        """;

    private final JdbcTemplate jdbcTemplate;
    private final EmbeddingModel embeddingModel;
    private final FilterExpressionConverter filterExpressionConverter;
    private final DocumentRowMapper documentRowMapper;

    /**
     * 建構子
     *
     * @param jdbcTemplate   JDBC 操作模板
     * @param embeddingModel 嵌入模型（用於生成查詢向量）
     * @param objectMapper   JSON 序列化工具
     * @param dimensions     向量維度（用於日誌紀錄）
     */
    public DocumentChunkVectorStore(JdbcTemplate jdbcTemplate,
                                     EmbeddingModel embeddingModel,
                                     ObjectMapper objectMapper,
                                     int dimensions) {
        this.jdbcTemplate = jdbcTemplate;
        this.embeddingModel = embeddingModel;
        this.filterExpressionConverter = new DocumentChunkFilterExpressionConverter();
        this.documentRowMapper = new DocumentRowMapper(objectMapper);

        log.info("初始化 DocumentChunkVectorStore（唯讀），向量維度: {}", dimensions);
    }

    /**
     * 唯讀版 - 不支援新增
     */
    @Override
    public void add(List<Document> documents) {
        throw new UnsupportedOperationException("MCP Server 為唯讀模式，不支援新增文件");
    }

    /**
     * 唯讀版 - 不支援刪除
     */
    @Override
    public void delete(List<String> idList) {
        throw new UnsupportedOperationException("MCP Server 為唯讀模式，不支援刪除文件");
    }

    /**
     * 唯讀版 - 不支援依條件刪除
     */
    @Override
    public void delete(Filter.Expression filterExpression) {
        throw new UnsupportedOperationException("MCP Server 為唯讀模式，不支援刪除文件");
    }

    /**
     * 向量相似度搜尋
     * <p>
     * 使用 pgvector 的餘弦距離進行相似度搜尋。
     * 支援透過 filterExpression 過濾特定 versionId 的文件。
     * </p>
     *
     * @param request 搜尋請求（包含查詢文字、topK、similarityThreshold、filterExpression）
     * @return 相似度最高的 Document 列表
     */
    @Override
    public List<Document> similaritySearch(SearchRequest request) {
        if (request.getQuery() == null || request.getQuery().isBlank()) {
            return List.of();
        }

        log.debug("執行語意搜尋，查詢: {}, topK: {}, threshold: {}",
                request.getQuery(), request.getTopK(), request.getSimilarityThreshold());

        // 將查詢文字轉換為向量
        float[] queryEmbedding = embeddingModel.embed(request.getQuery());
        PGvector queryVector = new PGvector(queryEmbedding);

        // 處理過濾條件 - 使用 JSONPath 格式
        String jsonPathFilter = "";
        if (request.getFilterExpression() != null) {
            String nativeFilterExpression = filterExpressionConverter.convertExpression(request.getFilterExpression());
            if (StringUtils.hasText(nativeFilterExpression)) {
                jsonPathFilter = " AND metadata::jsonb @@ '" + nativeFilterExpression + "'::jsonpath ";
            }
        }

        // 計算距離閾值：distance = 1 - similarity
        double distanceThreshold = 1 - request.getSimilarityThreshold();
        int topK = request.getTopK() > 0 ? request.getTopK() : 10;

        // 建構 SQL
        String sql = String.format(SQL_SIMILARITY_SEARCH, jsonPathFilter);

        // 執行查詢 - 直接傳遞 PGvector 物件
        List<Document> results = jdbcTemplate.query(
                sql,
                documentRowMapper,
                queryVector,      // 用於計算 distance
                queryVector,      // 用於 WHERE 條件
                distanceThreshold,
                topK
        );

        log.debug("語意搜尋完成，找到 {} 個結果", results.size());
        return results;
    }

    /**
     * 取得 VectorStore 名稱
     */
    @Override
    public String getName() {
        return "DocumentChunkVectorStore";
    }

    /**
     * Document RowMapper
     * <p>
     * 將資料庫查詢結果轉換為 Spring AI Document 物件。
     * </p>
     */
    private static class DocumentRowMapper implements RowMapper<Document> {

        private static final String COLUMN_ID = "id";
        private static final String COLUMN_CONTENT = "content";
        private static final String COLUMN_METADATA = "metadata";
        private static final String COLUMN_DISTANCE = "distance";

        private final ObjectMapper objectMapper;

        public DocumentRowMapper(ObjectMapper objectMapper) {
            this.objectMapper = objectMapper;
        }

        @Override
        public Document mapRow(ResultSet rs, int rowNum) throws SQLException {
            String id = rs.getString(COLUMN_ID);
            String content = rs.getString(COLUMN_CONTENT);
            PGobject pgMetadata = rs.getObject(COLUMN_METADATA, PGobject.class);
            float distance = rs.getFloat(COLUMN_DISTANCE);

            Map<String, Object> metadata = toMap(pgMetadata);
            metadata.put(DocumentMetadata.DISTANCE.value(), distance);

            // 使用 Document.builder() 並設定 score = 1 - distance
            return Document.builder()
                    .id(id)
                    .text(content)
                    .metadata(metadata)
                    .score(1.0 - distance)
                    .build();
        }

        /**
         * 將 PGobject 轉換為 Map
         */
        @SuppressWarnings("unchecked")
        private Map<String, Object> toMap(PGobject pgObject) {
            String source = pgObject.getValue();
            try {
                return (Map<String, Object>) objectMapper.readValue(source, Map.class);
            } catch (JsonProcessingException e) {
                throw new RuntimeException("JSON 解析失敗", e);
            }
        }
    }
}
