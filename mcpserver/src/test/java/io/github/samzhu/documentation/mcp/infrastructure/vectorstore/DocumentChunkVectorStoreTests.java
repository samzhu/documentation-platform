package io.github.samzhu.documentation.mcp.infrastructure.vectorstore;

import com.pgvector.PGvector;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.postgresql.util.PGobject;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import tools.jackson.databind.ObjectMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * DocumentChunkVectorStore 單元測試
 * <p>
 * 使用純 Mockito mock JdbcTemplate 和 EmbeddingModel，
 * 驗證向量搜尋 SQL 建構、EmbeddingModel 呼叫、唯讀限制等邏輯。
 * </p>
 *
 * @see DocumentChunkVectorStore
 */
@ExtendWith(MockitoExtension.class)
class DocumentChunkVectorStoreTests {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @Mock
    private EmbeddingModel embeddingModel;

    private DocumentChunkVectorStore vectorStore;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper();
        vectorStore = new DocumentChunkVectorStore(jdbcTemplate, embeddingModel, objectMapper, 768);
    }

    // ========== 唯讀限制 ==========

    @Test
    @DisplayName("add 操作拋出 UnsupportedOperationException")
    void addThrowsUnsupported() {
        assertThatThrownBy(() -> vectorStore.add(List.of()))
                .isInstanceOf(UnsupportedOperationException.class)
                .hasMessageContaining("唯讀模式");
    }

    @Test
    @DisplayName("delete(idList) 操作拋出 UnsupportedOperationException")
    void deleteByIdThrowsUnsupported() {
        assertThatThrownBy(() -> vectorStore.delete(List.of("id1")))
                .isInstanceOf(UnsupportedOperationException.class)
                .hasMessageContaining("唯讀模式");
    }

    @Test
    @DisplayName("getName 回傳正確名稱")
    void getNameReturnsCorrectName() {
        assertThat(vectorStore.getName()).isEqualTo("DocumentChunkVectorStore");
    }

    // ========== 語意搜尋 ==========

    @Test
    @DisplayName("空查詢回傳空列表，不呼叫 EmbeddingModel")
    void emptyQueryReturnsEmptyWithoutEmbedding() {
        SearchRequest request = SearchRequest.builder().query("").build();

        List<Document> results = vectorStore.similaritySearch(request);

        assertThat(results).isEmpty();
        verify(embeddingModel, never()).embed(anyString());
    }

    @Test
    @DisplayName("空白查詢回傳空列表")
    void blankQueryReturnsEmpty() {
        SearchRequest request = SearchRequest.builder().query("   ").build();

        List<Document> results = vectorStore.similaritySearch(request);

        assertThat(results).isEmpty();
        verify(embeddingModel, never()).embed(anyString());
    }

    @Test
    @DisplayName("正常查詢：呼叫 EmbeddingModel 生成向量並執行 SQL")
    void normalQueryCallsEmbeddingModelAndExecutesSQL() {
        // 模擬 EmbeddingModel 回傳 768 維向量
        float[] fakeEmbedding = new float[768];
        fakeEmbedding[0] = 0.1f;
        fakeEmbedding[1] = 0.2f;
        given(embeddingModel.embed("如何設定 Spring Boot")).willReturn(fakeEmbedding);

        // 模擬 JdbcTemplate 回傳空結果
        given(jdbcTemplate.query(anyString(), any(RowMapper.class),
                any(), any(), any(), any()))
                .willReturn(List.of());

        SearchRequest request = SearchRequest.builder()
                .query("如何設定 Spring Boot")
                .topK(5)
                .similarityThreshold(0.7)
                .build();

        List<Document> results = vectorStore.similaritySearch(request);

        assertThat(results).isEmpty();
        // 驗證 EmbeddingModel 被呼叫
        verify(embeddingModel).embed("如何設定 Spring Boot");
        // 驗證 SQL 被執行（4 個參數：queryVector x2, distanceThreshold, topK）
        verify(jdbcTemplate).query(anyString(), any(RowMapper.class),
                any(PGvector.class), any(PGvector.class), eq(0.30000000000000004), eq(5));
    }

    @Test
    @DisplayName("帶 filterExpression 的查詢：SQL 中包含 JSONPath 過濾")
    void queryWithFilterExpressionIncludesJsonPath() {
        float[] fakeEmbedding = new float[768];
        given(embeddingModel.embed("test query")).willReturn(fakeEmbedding);

        given(jdbcTemplate.query(anyString(), any(RowMapper.class),
                any(), any(), any(), any()))
                .willReturn(List.of());

        SearchRequest request = SearchRequest.builder()
                .query("test query")
                .topK(10)
                .similarityThreshold(0.5)
                .filterExpression("versionId == 'VER001'")
                .build();

        vectorStore.similaritySearch(request);

        // 驗證執行的 SQL 包含 JSONPath 過濾條件
        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(jdbcTemplate).query(sqlCaptor.capture(), any(RowMapper.class),
                any(), any(), any(), any());

        String executedSql = sqlCaptor.getValue();
        assertThat(executedSql).contains("metadata::jsonb @@ '");
        assertThat(executedSql).contains("jsonpath");
    }

    @Test
    @DisplayName("topK 預設為 10（當傳入 0 或負數時）")
    void defaultTopKWhenZeroOrNegative() {
        float[] fakeEmbedding = new float[768];
        given(embeddingModel.embed("query")).willReturn(fakeEmbedding);

        given(jdbcTemplate.query(anyString(), any(RowMapper.class),
                any(), any(), any(), any()))
                .willReturn(List.of());

        SearchRequest request = SearchRequest.builder()
                .query("query")
                .topK(0)
                .similarityThreshold(0.5)
                .build();

        vectorStore.similaritySearch(request);

        // 驗證 topK 被設為預設值 10
        verify(jdbcTemplate).query(anyString(), any(RowMapper.class),
                any(PGvector.class), any(PGvector.class), eq(0.5), eq(10));
    }
}
