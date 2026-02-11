# MCP Server 疑難排解

## processAot 失敗：Failed to determine a suitable driver class

### 症狀

執行 `./gradlew processAot` 或 `bootBuildImage` 時出現：

```
Caused by: org.springframework.boot.autoconfigure.jdbc.DataSourceProperties$DataSourceBeanCreationException:
  Failed to determine a suitable driver class
```

即使已正確設定 `spring.datasource.url`、PostgreSQL service container 正常運行，仍會失敗。

### 根因

Spring Boot 已知問題（[#48240](https://github.com/spring-projects/spring-boot/issues/48240)、[#47781](https://github.com/spring-projects/spring-boot/issues/47781)）。

`processAot` 會 fork 一個獨立的 JVM 啟動完整的 Spring ApplicationContext 做 AOT 分析。在這個過程中：

1. **環境變數和 Gradle `-D` 參數不會傳遞到 fork 的 JVM**
2. Spring Data JDBC 的 `AbstractJdbcConfiguration.jdbcDialect(NamedParameterJdbcOperations)` 在 context 啟動時會強制實例化 `DataSource` 來自動偵測資料庫方言
3. 由於 fork JVM 拿不到 DB 連線資訊，DataSource 實例化失敗

### 修復方式

在 `JdbcConfig.java` 明確宣告 `JdbcDialect` bean，跳過自動偵測：

```java
import org.springframework.data.jdbc.core.dialect.JdbcDialect;
import org.springframework.data.jdbc.core.dialect.JdbcPostgresDialect;

@Bean
JdbcDialect jdbcDialect() {
    return JdbcPostgresDialect.INSTANCE;
}
```

對應檔案：`src/main/java/.../config/JdbcConfig.java`

### 注意事項

- `@Bean` 方法的回傳型別**必須**是 `JdbcDialect`，不可以是父型別 `Dialect`
- Spring 使用 `@Bean` 方法的**宣告回傳型別**做 bean type matching
- 若宣告為 `Dialect`，框架以 `JdbcDialect` 型別查詢時會找不到，拋出 `NoSuchBeanDefinitionException`

---

## Jackson 2.x vs 3.x 套件名稱

### 症狀

編譯或 `processAot` 時找不到 `ObjectMapper` bean，或出現 class not found 錯誤。

### 根因

Spring Boot 4.x 內建 Jackson 3.x，套件名稱從 `com.fasterxml.jackson` 改為 `tools.jackson`。

### 對應表

| Jackson 2.x | Jackson 3.x |
|---|---|
| `com.fasterxml.jackson.databind.ObjectMapper` | `tools.jackson.databind.ObjectMapper` |
| `com.fasterxml.jackson.core.JsonProcessingException`（checked） | `tools.jackson.core.JacksonException`（unchecked） |
| `com.fasterxml.jackson.core.type.TypeReference` | `tools.jackson.core.type.TypeReference` |

---

## Google GenAI Embedding 測試時要求 project-id

### 症狀

測試啟動時出現：

```
spring.ai.google.genai.embedding.project-id must be set
```

### 根因

`GoogleGenAiEmbeddingConnectionAutoConfiguration` 判斷邏輯：

- `api-key` 有值 → Gemini Developer API 模式（不需 project-id）
- `api-key` 為空 → Vertex AI 模式（需要 project-id）

若 `application.yaml` 的 `api-key` 預設為空字串，測試環境就會進入 Vertex AI 模式。

### 修復方式

在測試中提供假的 api-key：

```java
@SpringBootTest(properties = {
    "spring.ai.google.genai.embedding.api-key=test-key"
})
```

或在 `application.yaml` 設定預設值：

```yaml
spring:
  ai:
    google:
      genai:
        embedding:
          api-key: ${platform-google-api-key:test-key}
```
