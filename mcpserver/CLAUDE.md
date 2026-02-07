# MCP Server CLAUDE.md

MCP Server 子專案 AI 輔助開發指引。

## 技術堆疊

| 項目 | 版本 | 說明 |
|------|------|------|
| Java | 25 | 語言版本 |
| Spring Boot | 4.0.2 | Web 框架 |
| Spring AI | 2.0.0-M2 | MCP Server + Google GenAI Embedding |
| PostgreSQL | 18 | 資料庫 + pgvector（與 backend 共用） |
| MCP Security | 0.1.1 | API Key 認證（org.springaicommunity） |

## 開發指令

```bash
./gradlew bootRun      # 執行（需要 PostgreSQL + Google API Key）
./gradlew compileJava  # 編譯檢查
./gradlew build -x processAot -x test  # 建構（跳過 AOT 和測試）
```

## 專案結構

```
mcpserver/
├── CLAUDE.md              # 本檔案
├── docs/
│   └── PRD.md             # MCP Server 產品規格
└── src/
    └── main/
        ├── java/.../mcp/
        │   ├── config/            # Spring 配置（Security、JDBC、VectorStore、Search）
        │   ├── domain/
        │   │   ├── enums/         # SourceType、VersionStatus、ApiKeyStatus
        │   │   └── model/         # 唯讀 Entity（Library、LibraryVersion、Document、CodeExample、ApiKey）
        │   ├── infrastructure/
        │   │   └── vectorstore/   # DocumentChunkVectorStore（唯讀）、FilterExpressionConverter
        │   ├── repository/        # Spring Data JDBC（Library、Version、Document、CodeExample、ApiKey）
        │   ├── security/          # DatabaseApiKeyEntityRepository
        │   ├── service/           # LibraryQueryService、SearchService
        │   │   └── dto/           # SearchResultItem
        │   └── mcp/               # MCP Tools/Resources/Prompts
        │       ├── SearchDocumentsTool.java
        │       ├── ListLibrariesTool.java
        │       ├── ListLibraryVersionsTool.java
        │       ├── GetDocumentTool.java
        │       ├── ListDocumentsTool.java
        │       ├── DocumentResources.java
        │       └── DocumentPrompts.java
        └── resources/
            └── application.yaml
```

## 核心設計原則

### 唯讀架構
- Entity **不含** `@Version`、`@With`、`create()` — 只讀不寫
- Schema 由 backend Liquibase 管理，`liquibase.enabled=false`
- VectorStore 寫入方法拋出 `UnsupportedOperationException`

### 無 Lombok
- Entity 全部手寫（final fields + constructor + getters + equals/hashCode/toString）
- 與 backend 使用 Lombok `@Value` 不同

### VectorStore 抽象層
- 使用自訂 `DocumentChunkVectorStore`（非官方 `PgVectorStore`）
- 原因：`document_chunks` 表結構不同於官方 `vector_store` 表
- 語意搜尋透過 `VectorStore.similaritySearch(SearchRequest)` 進行
- **不需 DocumentChunk entity** — VectorStore 直接回傳 Spring AI `Document`
- **不需 EmbeddingService** — VectorStore 內部自動呼叫 `EmbeddingModel.embed()`

### MCP 註解
- 套件路徑：`org.springaicommunity.mcp.annotation.*`
- Tool：`@McpTool` + `@McpToolParam`
- Resource：`@McpResource`（回傳 `ReadResourceResult`）
- Prompt：`@McpPrompt` + `@McpArg`（回傳 `GetPromptResult`）

## 搜尋模式

| 模式 | 實作 | 說明 |
|------|------|------|
| fulltext | `DocumentRepository.fullTextSearch()` | PostgreSQL tsvector |
| semantic | `VectorStore.similaritySearch()` | pgvector 餘弦距離 |
| hybrid（預設） | fulltext + semantic → RRF 融合 | K=60, alpha=0.3 |

## 配置檔案

| 檔案 | 說明 |
|------|------|
| `application.yaml` | 正式環境就緒的基底配置 |
| `compose.yaml` | Docker Compose（pgvector + Grafana LGTM） |

## 向量嵌入

| 項目 | 值 |
|------|-----|
| 模型 | gemini-embedding-001 |
| 維度 | 768 |
| Provider | Google GenAI（與 backend 使用相同 API Key） |
