package io.github.samzhu.documentation.platform;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Testcontainers 測試配置
 * <p>
 * 使用 Spring Boot 推薦的 @ServiceConnection 自動配置資料庫連線。
 * 啟用容器重用（withReuse）加速本地開發測試。
 * </p>
 * <p>
 * 若要啟用容器重用，請在 ~/.testcontainers.properties 中設定：
 * <pre>
 * testcontainers.reuse.enable=true
 * </pre>
 * </p>
 */
@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

  /**
   * 建立 PostgreSQL + pgvector 測試容器
   * 使用 @ServiceConnection 自動配置 DataSource
   */
  @Bean
  @ServiceConnection
  @SuppressWarnings({"resource", "rawtypes"})
  PostgreSQLContainer pgvectorContainer() {
    return new PostgreSQLContainer(DockerImageName.parse("pgvector/pgvector:pg16"))
        .withReuse(true);  // 本地開發加速：重用容器避免每次啟動
  }

}

