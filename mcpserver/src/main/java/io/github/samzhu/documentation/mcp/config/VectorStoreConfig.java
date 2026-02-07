package io.github.samzhu.documentation.mcp.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.samzhu.documentation.mcp.infrastructure.vectorstore.DocumentChunkVectorStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.pgvector.autoconfigure.PgVectorStoreProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * VectorStore 配置類別
 * <p>
 * 配置自訂的 DocumentChunkVectorStore（唯讀版）作為系統的 VectorStore 實作。
 * 使用 @Primary 確保系統預設使用此實作。
 * </p>
 */
@Configuration
public class VectorStoreConfig {

    private static final Logger log = LoggerFactory.getLogger(VectorStoreConfig.class);

    /**
     * 建立 DocumentChunkVectorStore Bean（唯讀版）
     *
     * @param jdbcTemplate            JDBC 操作模板
     * @param embeddingModel          嵌入模型（Google GenAI）
     * @param objectMapper            JSON 序列化工具
     * @param pgVectorStoreProperties PgVector 配置屬性（從 spring.ai.vectorstore.pgvector.* 讀取）
     * @return VectorStore 實例
     */
    @Bean
    @Primary
    public VectorStore documentChunkVectorStore(
            JdbcTemplate jdbcTemplate,
            EmbeddingModel embeddingModel,
            ObjectMapper objectMapper,
            PgVectorStoreProperties pgVectorStoreProperties) {

        int dimensions = pgVectorStoreProperties.getDimensions();
        log.info("初始化 DocumentChunkVectorStore（唯讀），向量維度: {}", dimensions);

        return new DocumentChunkVectorStore(jdbcTemplate, embeddingModel, objectMapper, dimensions);
    }
}
