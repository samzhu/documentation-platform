# Backend CLAUDE.md

後端專案 AI 輔助開發指引。

## 技術堆疊

| 項目 | 版本 | 說明 |
|------|------|------|
| Java | 25 | 語言版本 |
| Spring Boot | 4.0.2 | Web 框架 |
| Spring AI | 2.0.0-M2 | AI 整合（Gemini Embedding） |
| PostgreSQL | latest | 資料庫 + pgvector |

## 開發指令

```bash
../gradlew bootRun    # 執行（自動啟動 PostgreSQL via Docker Compose）
../gradlew test       # 單元測試（使用 Testcontainers）
../gradlew build      # 建構專案
```

## 專案結構

```
backend/
├── CLAUDE.md              # 本檔案
├── docs/
│   └── README.md          # 後端技術文件索引
└── src/
    ├── main/
    │   ├── java/.../platform/
    │   │   ├── config/        # Spring 配置
    │   │   ├── domain/        # 領域模型
    │   │   ├── repository/    # Spring Data JDBC
    │   │   ├── service/       # 業務邏輯層
    │   │   ├── web/api/       # REST Controllers
    │   │   ├── infrastructure/# 外部整合（GitHub、Parser）
    │   │   └── scheduler/     # 排程任務
    │   └── resources/
    │       ├── application.yaml
    │       └── static/        # 前端建構輸出
    └── test/
```

## Entity 規範

- **Immutable**：使用 `@Value` + `@With` + `@Version`
- **ID 格式**：TSID（13 字元 Crockford Base32）
- **向量維度**：768（gemini-embedding-001）

## TDD 流程

1. **Red** - 先寫失敗的測試
2. **Green** - 寫最小程式碼讓測試通過
3. **Refactor** - 重構保持測試綠燈

```bash
../gradlew test --tests "YourTest" --continuous
```

## 配置檔案

| 檔案 | 說明 |
|------|------|
| `src/main/resources/application.yaml` | 基礎配置 |
| `src/main/resources/application-local.yaml` | 本地開發覆蓋 |
| `config/application-dev.yaml` | DEBUG 日誌 |
| `config/application-secrets.properties` | 敏感資訊（gitignored） |

## API Key 管理

> 此專案負責 API Key 的 CRUD，MCP Server 只負責驗證（Read-only）。

### 格式規範

| 項目 | 規範 |
|------|------|
| 格式 | `dmcp_` + Base64-URL-encoded（32 字節隨機數） |
| key_prefix | 前 12 字元，用於快速查詢 |
| 儲存 | BCrypt 雜湊，原始 Key 只顯示一次 |

### 產生流程

```java
// 1. 產生隨機數
byte[] randomBytes = new byte[32];
new SecureRandom().nextBytes(randomBytes);

// 2. Base64-URL 編碼 + 前綴
String rawKey = "dmcp_" + Base64.getUrlEncoder()
    .withoutPadding().encodeToString(randomBytes);

// 3. 擷取 prefix + BCrypt 雜湊
String keyPrefix = rawKey.substring(0, 12);
String keyHash = passwordEncoder.encode(rawKey);

// 4. 儲存 keyHash、keyPrefix，回傳 rawKey（只這一次）
```
