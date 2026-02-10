package io.github.samzhu.documentation.mcp;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Testcontainers 測試配置
 * <p>
 * 使用 Spring Boot 推薦的 @ServiceConnection 自動配置資料庫連線。
 * processTestAot 階段需要真實 DB 連線才能完成 Bean 分析，
 * Testcontainers 自動啟動 PostgreSQL 容器並提供連線資訊。
 * </p>
 */
@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

  /**
   * 建立 PostgreSQL + pgvector 測試容器
   * 使用 @ServiceConnection 自動配置 DataSource，
   * 取代 application.yaml 中的 placeholder 配置
   */
  @Bean
  @ServiceConnection
  @SuppressWarnings({"resource", "rawtypes"})
  PostgreSQLContainer pgvectorContainer() {
    return new PostgreSQLContainer(DockerImageName.parse("pgvector/pgvector:pg18"))
        .withReuse(true);
  }

}
