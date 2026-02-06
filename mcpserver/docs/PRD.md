# Documentation MCP Server - PRD

> **版本**：1.1.0
> **狀態**：Draft

---

## 1. 產品概述

### 1.1 產品定位

Documentation MCP Server 是基於 **Model Context Protocol (MCP)** 的唯讀文件查詢服務，讓外部 AI 系統（Claude Desktop、Cursor、VS Code Copilot 等）透過標準化協定存取 Documentation Platform 的文件資料與語意搜尋能力。

### 1.2 與 Documentation Platform 的關係

```
┌─────────────────────────────┐     ┌─────────────────────────────┐
│   Documentation Platform     │     │   Documentation MCP Server   │
│   (Read-Write 管理後台)       │     │   (Read-Only MCP 服務)       │
│                              │     │                              │
│  • Web UI 管理介面            │     │  • MCP Tools（AI 呼叫）       │
│  • REST API（CRUD）           │     │  • MCP Resources（資源存取）  │
│  • 同步引擎（GitHub 抓取）     │     │  • MCP Prompts（提示範本）    │
│  • 向量嵌入（文件索引）        │     │  • API Key 認證              │
│  • API Key 管理（建立/撤銷）   │     │  • 語意搜尋（查詢用嵌入）     │
│  • OAuth2 認證               │     │                              │
└──────────────┬───────────────┘     └──────────────┬───────────────┘
               │                                     │
               └──────────┐      ┌───────────────────┘
                          ▼      ▼
               ┌──────────────────────────┐
               │  PostgreSQL + pgvector    │
               │  （共用資料庫）             │
               └──────────────────────────┘
```

| 項目 | Documentation Platform | MCP Server |
|------|----------------------|------------|
| 角色 | 寫入端（管理、同步、索引） | 讀取端（查詢、搜尋） |
| 使用者 | 管理員（Web UI） | AI 助手（MCP 客戶端） |
| 認證 | OAuth2（管理操作） | API Key（`{id}.{secret}` 格式） |
| 資料庫 | 讀寫 | 唯讀 |

### 1.3 目標用戶

| 用戶類型 | 使用場景 |
|----------|----------|
| AI 助手 | Claude Desktop、Cursor 等 MCP 客戶端查詢技術文件 |
| 開發者 | 透過 AI 助手快速搜尋技術文件與程式碼範例 |
| 系統管理員 | 部署與設定 MCP Server |

### 1.4 核心價值

| 價值 | 說明 |
|------|------|
| 標準化 | 遵循 MCP 協定，相容所有 MCP 客戶端 |
| 智慧搜尋 | 混合搜尋（全文 + 語意），RRF 融合排序 |
| 安全存取 | API Key 認證（mcp-server-security），速率限制 |
| 輕量部署 | 獨立服務，支援 GraalVM Native Image |

---

## 2. 系統架構

### 2.1 技術堆疊

| 組件 | 技術 | 版本 |
|------|------|------|
| 語言 | Java | 25 |
| 框架 | Spring Boot | 4.0.2 |
| MCP Server | spring-ai-starter-mcp-server-webmvc | 2.0.0-M2 |
| MCP 安全 | org.springaicommunity:mcp-server-security | 0.1.1 |
| AI 嵌入 | Vertex AI Embedding（gemini-embedding-001） | 2.0.0-M2 |
| 資料庫 | Spring Data JDBC + PostgreSQL + pgvector | - |
| 可觀測性 | OpenTelemetry + Micrometer Brave | - |
| 建構 | Gradle 9.3.0 + GraalVM Native 0.11.4 | - |

> **重要依賴修正**：模板預設的 `spring-ai-starter-mcp-server`（STDIO 傳輸）必須替換為
> `spring-ai-starter-mcp-server-webmvc`，因為 `mcp-server-security:0.1.1` **僅支援 WebMVC**，
> 不支援 WebFlux 及 STDIO。
> 參考：[MCP Security README](https://github.com/spring-ai-community/mcp-security?tab=readme-ov-file#mcp-security)

### 2.2 架構圖

```
┌─────────────────────────────────────────────────────────────────────┐
│                   Documentation MCP Server                          │
├─────────────────────────────────────────────────────────────────────┤
│  Transport Layer                                                    │
│  └── Streamable-HTTP（POST /mcp）                                   │
│      spring.ai.mcp.server.protocol=STREAMABLE                      │
├─────────────────────────────────────────────────────────────────────┤
│  Security Layer（mcp-server-security）                               │
│  └── API Key 認證（McpServerApiKeyConfigurer）                       │
│      Header: X-API-key: {id}.{secret}                              │
├─────────────────────────────────────────────────────────────────────┤
│  MCP Capabilities                                                   │
│  ├── Tools（5 個）  → AI 模型自動呼叫（@McpTool）                     │
│  ├── Resources（2 個）→ 應用程式注入上下文（@McpResource）             │
│  └── Prompts（2 個） → 使用者選擇的提示範本（@McpPrompt）             │
├─────────────────────────────────────────────────────────────────────┤
│  Service Layer                                                      │
│  ├── SearchService（混合搜尋：全文 + 語意 + RRF）                     │
│  ├── EmbeddingService（查詢向量生成，Vertex AI）                      │
│  └── LibraryQueryService（文件庫/版本/文件查詢）                      │
├─────────────────────────────────────────────────────────────────────┤
│  Data Access Layer（Read-Only）                                      │
│  ├── LibraryRepository          ├── DocumentChunkRepository         │
│  ├── LibraryVersionRepository   ├── CodeExampleRepository           │
│  ├── DocumentRepository         └── ApiKeyRepository                │
└─────────────────────────────────────────────────────────────────────┘
                                │
                                ▼
┌─────────────────────────────────────────────────────────────────────┐
│                    PostgreSQL + pgvector（共用資料庫）                 │
│  ├── libraries              ├── documents                           │
│  ├── library_versions       ├── document_chunks（embedding 768D）    │
│  ├── api_keys               └── code_examples                      │
└─────────────────────────────────────────────────────────────────────┘
```

### 2.3 傳輸協定

| 項目 | 規格 |
|------|------|
| 傳輸方式 | Streamable-HTTP（推薦，取代舊版 SSE） |
| 端點 | `POST /mcp`（JSON-RPC over HTTP） |
| Starter | `spring-ai-starter-mcp-server-webmvc` |
| 伺服器類型 | SYNC（同步模式） |

**傳輸協定對照（參考）：**

| 協定 | Starter | 適用場景 | mcp-server-security 支援 |
|------|---------|----------|-------------------------|
| STDIO | `spring-ai-starter-mcp-server` | 嵌入式 CLI 工具 | ❌ 不支援 |
| SSE | webmvc / webflux | 即時串流（已棄用） | ❌ 不支援（已棄用） |
| **STREAMABLE** | **webmvc** / webflux | **HTTP + 可選 SSE 串流** | ✅ 支援（僅 WebMVC） |
| STATELESS | webmvc / webflux | 無狀態微服務 | ✅ 支援（僅 WebMVC） |

---

## 3. MCP 功能規格

### 3.1 Tools（模型控制 — AI 自動呼叫）

MCP Tools 是 AI 模型根據對話上下文自動判斷何時呼叫的功能。使用 `@McpTool` + `@McpToolParam` 註解定義。

#### Tool 1: `search_documents`

| 項目 | 規格 |
|------|------|
| 名稱 | `search_documents` |
| 說明 | 搜尋技術文件，支援全文、語意、混合三種模式 |
| 使用時機 | 使用者詢問技術問題時，AI 自動搜尋相關文件 |

**參數：**

| 參數 | 類型 | 必填 | 說明 |
|------|------|------|------|
| query | String | 是 | 搜尋關鍵字或語意描述 |
| libraryName | String | 否 | 限定搜尋的文件庫名稱（如 `spring-boot`） |
| version | String | 否 | 限定搜尋的版本號（如 `4.0.2`） |
| mode | String | 否 | 搜尋模式：`hybrid`（預設）、`fulltext`、`semantic` |
| limit | Integer | 否 | 回傳筆數上限（預設 10，最大 20） |

**回傳：** 搜尋結果列表，包含文件標題、內容片段、相關性分數、來源路徑。

#### Tool 2: `list_libraries`

| 項目 | 規格 |
|------|------|
| 名稱 | `list_libraries` |
| 說明 | 列出所有可用的文件庫及其基本資訊 |
| 使用時機 | 使用者詢問「有哪些文件？」或 AI 需要知道可搜尋範圍時 |

**參數：**

| 參數 | 類型 | 必填 | 說明 |
|------|------|------|------|
| category | String | 否 | 篩選類別（如 `backend`、`frontend`） |

**回傳：** 文件庫列表，包含名稱、描述、版本數、文件數。

#### Tool 3: `list_library_versions`

| 項目 | 規格 |
|------|------|
| 名稱 | `list_library_versions` |
| 說明 | 列出指定文件庫的所有版本 |
| 使用時機 | AI 需要確認版本資訊時 |

**參數：**

| 參數 | 類型 | 必填 | 說明 |
|------|------|------|------|
| libraryName | String | 是 | 文件庫名稱 |

**回傳：** 版本列表，包含版本號、狀態（ACTIVE/DEPRECATED/EOL）、是否最新版、是否 LTS。

#### Tool 4: `get_document`

| 項目 | 規格 |
|------|------|
| 名稱 | `get_document` |
| 說明 | 取得指定文件的完整內容 |
| 使用時機 | AI 需要閱讀完整文件內容時 |

**參數：**

| 參數 | 類型 | 必填 | 說明 |
|------|------|------|------|
| libraryName | String | 是 | 文件庫名稱 |
| version | String | 是 | 版本號 |
| path | String | 是 | 文件路徑（如 `getting-started/index.adoc`） |

**回傳：** 文件完整內容、標題、類型、metadata。

#### Tool 5: `list_documents`

| 項目 | 規格 |
|------|------|
| 名稱 | `list_documents` |
| 說明 | 列出指定版本下的所有文件路徑 |
| 使用時機 | AI 需要瀏覽文件目錄結構時 |

**參數：**

| 參數 | 類型 | 必填 | 說明 |
|------|------|------|------|
| libraryName | String | 是 | 文件庫名稱 |
| version | String | 是 | 版本號 |

**回傳：** 文件列表，包含標題、路徑、文件類型。

### 3.2 Resources（應用控制 — 注入上下文）

MCP Resources 是由應用程式（MCP 客戶端）決定何時讀取的資料來源。使用 `@McpResource` 註解定義。

#### Resource 1: `docs://{libraryName}/{version}/{path}`

| 項目 | 規格 |
|------|------|
| URI 樣板 | `docs://{libraryName}/{version}/{path}` |
| 名稱 | Documentation Content |
| 說明 | 透過 URI 直接存取文件內容 |
| MIME 類型 | 依文件類型：`text/markdown`、`text/html`、`text/asciidoc` |

**範例：**
```
docs://spring-boot/4.0.2/getting-started/index.adoc
docs://react/19.0.0/hooks/useState.md
```

#### Resource 2: `library://{libraryName}`

| 項目 | 規格 |
|------|------|
| URI 樣板 | `library://{libraryName}` |
| 名稱 | Library Metadata |
| 說明 | 文件庫後設資料（版本清單、描述、分類） |
| MIME 類型 | `application/json` |

### 3.3 Prompts（使用者控制 — 明確選擇）

MCP Prompts 是使用者透過 UI 選單或斜線命令主動選擇的提示範本。使用 `@McpPrompt` + `@McpArg` 註解定義。

#### Prompt 1: `search-docs`

| 項目 | 規格 |
|------|------|
| 名稱 | `search-docs` |
| 說明 | 文件搜尋提示範本，引導 AI 有效搜尋並整理結果 |

**參數：**

| 參數 | 必填 | 說明 |
|------|------|------|
| query | 是 | 搜尋主題或問題 |
| libraryName | 否 | 限定文件庫 |

**產生的 Prompt 訊息：**
```
你是一位技術文件助手。請使用 search_documents 工具搜尋以下主題的相關文件，
然後整理搜尋結果，提供清楚的摘要和引用來源。

搜尋主題：{query}
限定文件庫：{libraryName}

請注意：
1. 優先搜尋最新版本的文件
2. 引用原始文件路徑作為來源
3. 如果搜尋結果不足，嘗試使用不同的關鍵字重新搜尋
```

#### Prompt 2: `explain-with-docs`

| 項目 | 規格 |
|------|------|
| 名稱 | `explain-with-docs` |
| 說明 | 根據文件解釋技術概念的提示範本 |

**參數：**

| 參數 | 必填 | 說明 |
|------|------|------|
| topic | 是 | 技術概念或功能名稱 |
| libraryName | 是 | 目標文件庫 |

### 3.4 功能摘要

| 能力 | 數量 | 控制者 | 互動模式 | 註解 |
|------|------|--------|----------|------|
| Tools | 5 個 | AI 模型 | 自動判斷呼叫時機 | `@McpTool` |
| Resources | 2 個 | 應用程式 | 應用程式決定何時讀取 | `@McpResource` |
| Prompts | 2 個 | 使用者 | 使用者明確選擇 | `@McpPrompt` |

---

## 4. 資料存取

### 4.1 共用資料庫設計

MCP Server 與 Documentation Platform **共用同一個 PostgreSQL 資料庫**，簡化部署架構。

| 項目 | 規格 |
|------|------|
| 存取模式 | 唯讀（Read-Only） |
| 共用表格 | `libraries`、`library_versions`、`documents`、`document_chunks`、`code_examples`、`api_keys` |
| 不使用的表格 | `sync_history`（僅管理端使用） |
| Schema 管理 | 由 Documentation Platform 的 Liquibase 負責，MCP Server **不執行** migration |

### 4.2 Entity 映射

MCP Server 定義自己的 Entity 類別（對應相同的資料表結構，但為獨立模組）：

| Entity | 資料表 | 主鍵格式 | 用途 |
|--------|--------|----------|------|
| Library | `libraries` | TSID (13 chars) | 文件庫基本資訊 |
| LibraryVersion | `library_versions` | TSID | 版本資訊查詢 |
| Document | `documents` | TSID | 文件內容讀取、全文搜尋 |
| DocumentChunk | `document_chunks` | TSID | 語意搜尋（pgvector 768D） |
| CodeExample | `code_examples` | TSID | 程式碼範例查詢 |
| ApiKey | `api_keys` | TSID | API Key 驗證 |

**Entity 設計原則：**
- 使用 `@Value`（Lombok immutable）+ `@With` + `@Version`，與 backend 一致
- 所有 ID 為 TSID（13 字元 Crockford Base32）
- 不包含寫入用的 factory method（唯讀）

### 4.3 關鍵查詢

#### 語意搜尋（Semantic Search）

```sql
SELECT dc.* FROM document_chunks dc
JOIN documents d ON dc.document_id = d.id
WHERE d.version_id = :versionId
  AND dc.embedding IS NOT NULL
ORDER BY dc.embedding <=> cast(:queryEmbedding as vector)
LIMIT :limit
```

- 使用 pgvector `<=>` 運算子（cosine distance）
- 查詢向量格式：`"[0.1,0.2,...,0.768]"`（768 維浮點數陣列字串）

#### 全文搜尋（Full-Text Search）

```sql
SELECT * FROM documents
WHERE version_id = :versionId
  AND search_vector @@ plainto_tsquery('english', :query)
ORDER BY ts_rank(search_vector, plainto_tsquery('english', :query)) DESC
LIMIT :limit
```

- 使用 PostgreSQL `tsvector` / `plainto_tsquery`
- `search_vector` 欄位已由 backend 建立 GIN 索引

#### 混合搜尋（Hybrid Search — RRF）

| 項目 | 規格 |
|------|------|
| 演算法 | RRF（Reciprocal Rank Fusion） |
| 公式 | `score = α × 1/(K + rank_keyword) + (1-α) × 1/(K + rank_semantic)` |
| K 值 | 60（防止首名過度主導） |
| α 預設值 | 0.3（30% 關鍵字、70% 語意） |
| 最低相似度 | 0.5（語意搜尋門檻） |

---

## 5. 認證與安全

### 5.1 API Key 認證

使用 `mcp-server-security:0.1.1` 提供的 `McpServerApiKeyConfigurer` 機制。

| 項目 | 規格 |
|------|------|
| 認證方式 | API Key（HTTP Header） |
| Header 名稱 | `X-API-key`（mcp-server-security 預設） |
| 金鑰格式 | `{id}.{secret}` |
| 金鑰來源 | Documentation Platform 管理後台建立 |
| 儲存方式 | BCrypt 雜湊（`api_keys.key_hash` 欄位） |

**金鑰格式說明：**

```
X-API-key: {apiKeyId}.{rawSecret}
            ├─ TSID id（api_keys.id）
            └─ 原始金鑰值（與 api_keys.key_hash 比對）
```

**範例：** `X-API-key: 0ABC123DEF456.dmcp_aB1cD2eF3gH4iJ5kL6mN7oP8qR9sT`

> Documentation Platform 建立 API Key 時會顯示 `id` 與原始金鑰，使用者組合為 `{id}.{secret}` 格式設定到 MCP 客戶端。

### 5.2 自訂 DatabaseApiKeyEntityRepository

`mcp-server-security` 預設提供 `InMemoryApiKeyEntityRepository`（記憶體存放，BCrypt 計算量大，不適合生產環境）。需實作自訂的 `ApiKeyEntityRepository<T>` 從共用資料庫讀取：

```
InMemoryApiKeyEntityRepository    DatabaseApiKeyEntityRepository
（預設，記憶體）                    （自訂，讀取 api_keys 表）
┌──────────────────────┐          ┌──────────────────────────────┐
│ 硬編碼 API Keys       │    →    │ SELECT FROM api_keys         │
│ BCrypt encode on init │          │ WHERE id = :id               │
│ 不適合生產環境         │          │ AND status = 'ACTIVE'        │
└──────────────────────┘          │ AND (expires_at IS NULL       │
                                  │      OR expires_at > NOW())   │
                                  └──────────────────────────────┘
```

**實作要點：**
- 實作 `ApiKeyEntityRepository<T>` 介面
- 以 `{id}` 部分（TSID）查詢 `api_keys` 表
- 驗證 `status = ACTIVE` 且未過期
- 回傳 entity 供框架進行 BCrypt 比對 `{secret}` 部分

### 5.3 SecurityFilterChain 配置

```
SecurityFilterChain：
  ├── /actuator/health, /actuator/info → 公開（健康檢查、服務資訊）
  ├── /mcp → API Key 認證（McpServerApiKeyConfigurer）
  └── 其他 → 拒絕存取
```

配置範例（概念）：

```java
@Bean
SecurityFilterChain securityFilterChain(HttpSecurity http) {
    return http
        .authorizeHttpRequests(auth -> auth
            .requestMatchers("/actuator/health", "/actuator/info").permitAll()
            .anyRequest().authenticated()
        )
        .with(mcpServerApiKey(), apiKey -> {
            apiKey.apiKeyRepository(databaseApiKeyEntityRepository);
        })
        .build();
}
```

### 5.4 方法層級安全（可選）

透過 `@EnableMethodSecurity` + `@PreAuthorize` 在 Tool 層級控制存取，可取得認證使用者資訊：

```java
@PreAuthorize("isAuthenticated()")
@McpTool(name = "search_documents", description = "...")
public String searchDocuments(...) {
    var auth = SecurityContextHolder.getContext().getAuthentication();
    var apiKeyName = auth.getName();  // API Key 名稱
    // ...
}
```

---

## 6. 向量嵌入

### 6.1 嵌入模型相容性

| 項目 | Documentation Platform | MCP Server |
|------|----------------------|------------|
| 用途 | 文件索引（批量嵌入） | 查詢向量生成（單筆） |
| 模型 | gemini-embedding-001 | gemini-embedding-001 |
| 維度 | 768 | 768 |
| Provider | Google GenAI（`spring-ai-starter-model-google-genai`） | Vertex AI（`spring-ai-starter-model-vertex-ai-embedding`） |

> **關鍵**：兩端必須使用**相同的嵌入模型與維度**，確保向量空間一致。
> `gemini-embedding-001` 無論透過 Google GenAI 或 Vertex AI 存取，產生的向量相同。

### 6.2 查詢向量生成流程

```
使用者問題（自然語言）
    │
    ▼
EmbeddingModel（Vertex AI gemini-embedding-001）
    │
    ▼
768 維查詢向量（float[]）
    │
    ▼
轉為 PostgreSQL vector 字串格式："[0.1,0.2,...,0.768]"
    │
    ▼
pgvector cosine distance 查詢（<=> 運算子）
    │
    ▼
排序後的 DocumentChunk 列表
```

---

## 7. 配置與環境變數

### 7.1 application.yaml

```yaml
spring:
  application:
    name: documentation-mcp-server

  # 資料庫（與 Documentation Platform 共用）
  datasource:
    url: ${platform-db-url:jdbc:postgresql://localhost:5432/mydatabase}
    username: ${platform-db-username:myuser}
    password: ${platform-db-password:secret}
    hikari:
      maximum-pool-size: 5
      minimum-idle: 2
      max-lifetime: 1680000    # 28 分鐘（< Cloud SQL 30 分鐘）
      keepalive-time: 300000   # 5 分鐘

  # Liquibase 停用（Schema 由 Documentation Platform 管理）
  liquibase:
    enabled: false

  # MCP Server 配置
  ai:
    mcp:
      server:
        name: documentation-mcp-server
        version: 1.0.0
        instructions: "Technical documentation search and retrieval MCP server. Use search_documents to find relevant docs, list_libraries to see available libraries."
        type: SYNC
        protocol: STREAMABLE
        annotation-scanner:
          enabled: true
        capabilities:
          tool: true
          resource: true
          prompt: true
          completion: false

    # Vertex AI 嵌入模型（查詢向量生成用）
    vertex:
      ai:
        embedding:
          options:
            model: gemini-embedding-001

# 搜尋配置
platform:
  search:
    hybrid:
      alpha: 0.3               # 30% 關鍵字、70% 語意
      min-similarity: 0.5      # 語意搜尋最低相似度門檻
    default-limit: 10
    max-limit: 20
```

### 7.2 環境變數

| 環境變數 | 說明 | 必要 | 預設值 |
|----------|------|------|--------|
| `platform-db-url` | PostgreSQL 連線 URL | 是 | `jdbc:postgresql://localhost:5432/mydatabase` |
| `platform-db-username` | 資料庫使用者名稱 | 是 | `myuser` |
| `platform-db-password` | 資料庫密碼 | 是 | `secret` |
| `GOOGLE_CLOUD_PROJECT` | GCP 專案 ID（Vertex AI） | 是 | - |
| `GOOGLE_CLOUD_LOCATION` | GCP 區域 | 否 | `us-central1` |
| `GOOGLE_APPLICATION_CREDENTIALS` | GCP 服務帳號金鑰路徑 | 是（生產） | - |

### 7.3 Profile 配置

| Profile | 說明 | 用途 |
|---------|------|------|
| `local` | Docker Compose 自動啟動、DevTools | 本地開發 |
| `dev` | DEBUG 日誌、OTLP 停用 | 開發環境 |
| （預設） | 完整安全與可觀測性 | 生產環境 |

---

## 8. build.gradle 調整事項

模板預設依賴需進行以下修正：

```groovy
dependencies {
    // ❌ 移除：STDIO 傳輸，不支援 mcp-server-security
    // implementation 'org.springframework.ai:spring-ai-starter-mcp-server'

    // ✅ 替換為：WebMVC 傳輸，支援 Streamable-HTTP + API Key Security
    implementation 'org.springframework.ai:spring-ai-starter-mcp-server-webmvc'

    // 保留
    implementation 'org.springaicommunity:mcp-server-security:0.1.1'
    implementation 'org.springframework.boot:spring-boot-starter-security'
    implementation 'org.springframework.boot:spring-boot-starter-data-jdbc'
    implementation 'org.springframework.ai:spring-ai-starter-model-vertex-ai-embedding'
    // ...其餘不變
}
```

---

## 9. 專案結構

```
mcpserver/
├── build.gradle
├── settings.gradle
├── compose.yaml                                # Docker Compose（開發用）
├── docs/
│   └── PRD.md                                  # 本文件
└── src/
    ├── main/
    │   ├── java/io/github/samzhu/documentation/mcp/
    │   │   ├── DocumentationMcpServerApplication.java
    │   │   ├── config/
    │   │   │   ├── SecurityConfig.java               # SecurityFilterChain + API Key
    │   │   │   └── SearchProperties.java             # 搜尋參數配置
    │   │   ├── domain/
    │   │   │   └── model/                            # Entity（唯讀，對應共用表）
    │   │   │       ├── Library.java
    │   │   │       ├── LibraryVersion.java
    │   │   │       ├── Document.java
    │   │   │       ├── DocumentChunk.java
    │   │   │       ├── CodeExample.java
    │   │   │       └── ApiKey.java
    │   │   ├── repository/                           # Spring Data JDBC Repository
    │   │   │   ├── LibraryRepository.java
    │   │   │   ├── LibraryVersionRepository.java
    │   │   │   ├── DocumentRepository.java
    │   │   │   ├── DocumentChunkRepository.java
    │   │   │   └── ApiKeyRepository.java
    │   │   ├── security/
    │   │   │   └── DatabaseApiKeyEntityRepository.java  # 自訂 API Key 存取
    │   │   ├── service/
    │   │   │   ├── SearchService.java                # 混合搜尋（全文 + 語意 + RRF）
    │   │   │   ├── EmbeddingService.java             # 查詢向量生成
    │   │   │   └── LibraryQueryService.java          # 文件庫/版本/文件查詢
    │   │   └── mcp/
    │   │       ├── SearchDocumentsTool.java           # @McpTool: search_documents
    │   │       ├── ListLibrariesTool.java             # @McpTool: list_libraries
    │   │       ├── ListLibraryVersionsTool.java       # @McpTool: list_library_versions
    │   │       ├── GetDocumentTool.java               # @McpTool: get_document
    │   │       ├── ListDocumentsTool.java             # @McpTool: list_documents
    │   │       ├── DocumentResources.java             # @McpResource: docs://, library://
    │   │       └── DocumentPrompts.java               # @McpPrompt: search-docs, explain-with-docs
    │   └── resources/
    │       ├── application.yaml
    │       ├── application-local.yaml
    │       └── application-dev.yaml
    └── test/
        └── java/io/github/samzhu/documentation/mcp/
            ├── DocumentationMcpServerApplicationTests.java
            ├── mcp/
            │   └── SearchDocumentsToolTest.java
            └── service/
                └── SearchServiceTest.java
```

---

## 10. 部署指南

### 10.1 Docker Compose（與 Documentation Platform 共同部署）

```yaml
services:
  # 文件管理後台（Read-Write）
  platform:
    image: ghcr.io/samzhu/documentation-platform:latest
    ports:
      - "8080:8080"
    environment:
      platform-google-api-key: ${PLATFORM_GOOGLE_API_KEY}
      platform-db-url: jdbc:postgresql://db:5432/mydatabase
      platform-db-username: myuser
      platform-db-password: secret
    depends_on:
      - db

  # MCP Server（Read-Only）
  mcp-server:
    image: ghcr.io/samzhu/documentation-mcp-server:latest
    ports:
      - "8081:8080"
    environment:
      platform-db-url: jdbc:postgresql://db:5432/mydatabase
      platform-db-username: myuser
      platform-db-password: secret
      GOOGLE_CLOUD_PROJECT: ${GCP_PROJECT_ID}
    depends_on:
      - db

  # 共用資料庫
  db:
    image: pgvector/pgvector:pg18
    environment:
      POSTGRES_DB: mydatabase
      POSTGRES_USER: myuser
      POSTGRES_PASSWORD: secret
    volumes:
      - pgdata:/var/lib/postgresql/data

volumes:
  pgdata:
```

### 10.2 MCP 客戶端設定範例

#### Claude Desktop（`claude_desktop_config.json`）

```json
{
  "mcpServers": {
    "documentation": {
      "url": "http://localhost:8081/mcp",
      "headers": {
        "X-API-key": "{apiKeyId}.{rawSecret}"
      }
    }
  }
}
```

#### Cursor / VS Code

在 MCP 設定中加入：

```json
{
  "mcp": {
    "servers": {
      "documentation": {
        "url": "http://localhost:8081/mcp",
        "headers": {
          "X-API-key": "{apiKeyId}.{rawSecret}"
        }
      }
    }
  }
}
```

### 10.3 GraalVM Native Image（可選）

```bash
# 編譯為原生執行檔（需要 GraalVM 25+）
cd mcpserver && ../gradlew nativeCompile

# 或建構為容器映像
cd mcpserver && ../gradlew bootBuildImage
```

---

## 11. 可觀測性

### 11.1 監控堆疊

| 組件 | 技術 | 用途 |
|------|------|------|
| 分散式追蹤 | Micrometer Tracing + Brave | 請求追蹤鏈路 |
| 指標匯出 | Micrometer + OTLP Registry | 效能指標 |
| DataSource 指標 | datasource-micrometer | 連線池監控 |
| 健康檢查 | Spring Boot Actuator | `/actuator/health` |
| 視覺化 | Grafana LGTM（開發用 compose.yaml） | Logs / Traces / Metrics |

### 11.2 Actuator 端點

| 端點 | 認證 | 說明 |
|------|------|------|
| `/actuator/health` | 公開 | 健康檢查（含資料庫連線） |
| `/actuator/info` | 公開 | 應用程式資訊 |
| `/actuator/metrics` | 受保護 | Micrometer 指標 |

---

## 12. 開發指引

### 12.1 快速指令

| 任務 | 指令 |
|------|------|
| 開發執行 | `cd mcpserver && ../gradlew bootRun` |
| 執行測試 | `cd mcpserver && ../gradlew test` |
| 建構 JAR | `cd mcpserver && ../gradlew build` |
| Native 編譯 | `cd mcpserver && ../gradlew nativeCompile` |

### 12.2 MCP 註解快速參考

```java
// Tool 定義
@McpTool(name = "tool_name", description = "說明")
public String myTool(
    @McpToolParam(description = "參數說明", required = true) String param) { ... }

// Resource 定義
@McpResource(uri = "scheme://{var}", name = "Name", description = "說明")
public String myResource(String var) { ... }

// Prompt 定義
@McpPrompt(name = "prompt-name", description = "說明")
public GetPromptResult myPrompt(
    @McpArg(name = "arg", description = "說明", required = true) String arg) { ... }
```

### 12.3 參考連結

- [MCP Server Boot Starter](https://docs.spring.io/spring-ai/reference/api/mcp/mcp-server-boot-starter-docs.html)
- [MCP Security（Experimental）](https://github.com/spring-ai-community/mcp-security?tab=readme-ov-file#mcp-security)
- [MCP Annotations](https://docs.spring.io/spring-ai/reference/api/mcp/mcp-annotations-server.html)
- [Model Context Protocol 規格](https://modelcontextprotocol.io/specification/2025-06-18)

---

## 13. Phase 2：A2A（Agent-to-Agent）協定支援

> **狀態**：規劃中，待套件穩定後實施
> **前置條件**：Phase 1（MCP Server 核心功能）完成且穩定運行

### 13.1 A2A 協定簡介

A2A（Agent2Agent）是 Google 於 2025 年 4 月發布的開放通訊協定，讓 AI Agent 之間能夠相互發現、溝通與協作。目前已捐贈至 Linux Foundation 管理，最新版本為 **0.3**（2025-07-31）。

### 13.2 A2A 與 MCP 的互補關係

A2A 與 MCP 是**互補而非競爭**的協定，解決不同層次的問題：

```
            A2A（水平：Agent ↔ Agent 協作）
    ┌──────────────────────────────────────────┐
    │                                          │
    │   Agent A  ←── 對話/委派任務 ──→  Agent B  │
    │     │                              │     │
    │     │ MCP（垂直：Agent → Tool）     │ MCP │
    │     ▼                              ▼     │
    │   工具/資料庫                    工具/API  │
    └──────────────────────────────────────────┘
```

| 面向 | MCP（Phase 1） | A2A（Phase 2） |
|------|---------------|---------------|
| 建立者 | Anthropic | Google |
| 用途 | Agent → 工具/資料存取 | Agent ↔ Agent 協作 |
| 互動模式 | 結構化輸入/輸出 | 多輪對話、任務委派 |
| 比喻 | 技師使用診斷儀器 | 店長協調技師與零件供應商 |

### 13.3 Spring AI A2A 支援現況

| 項目 | 狀態 |
|------|------|
| Spring AI Core | **尚未納入**，Spring 團隊表示持續觀察中 |
| 社群套件 | `org.springaicommunity:spring-ai-a2a-server-autoconfigure:0.2.0` |
| 版本需求 | Spring Boot 4.0+ / Spring AI 2.0.0-M2+（與本專案相容） |
| A2A 協定版本 | 0.3.0 |
| Spring 官方文章 | [Spring AI Agentic Patterns Part 5: A2A Integration](https://spring.io/blog/2026/01/29/spring-ai-agentic-patterns-a2a-integration)（2026-01-29） |
| GitHub Issue | [spring-projects/spring-ai#2911](https://github.com/spring-projects/spring-ai/issues/2911) |

### 13.4 啟動條件（待觀察）

Phase 2 將在以下條件滿足後啟動：

| 條件 | 說明 | 目前狀態 |
|------|------|----------|
| 社群套件版本 ≥ 1.0 | API 穩定，不再有破壞性變更 | ❌ 目前 0.2.0 |
| 納入 Spring AI Core | 由 Spring 官方維護，有長期支援保障 | ❌ 社群孵化中 |
| A2A 協定規格穩定 | Linux Foundation 發布正式版規格 | ❌ 目前 v0.3 |
| Phase 1 穩定運行 | MCP Server 核心功能無重大 Bug | ⏳ 開發中 |

### 13.5 Phase 2 預期架構

在同一個 Spring Boot 應用中同時提供 MCP + A2A 兩種協定：

```
                  ┌───────────────────────────────────────┐
                  │   Documentation MCP + A2A Server       │
                  │                                        │
  MCP 客戶端 ────>│  /mcp（Streamable-HTTP）                │
  (Claude, Cursor)│  → @McpTool: search, list, get         │
                  │                                        │
  A2A 客戶端 ────>│  /a2a（JSON-RPC）                       │
  (其他 AI Agent) │  → Agent Card: /.well-known/agent.json  │
                  │  → Skills: 文件搜尋、技術解答            │
                  │                                        │
                  │  ┌────────────────────────────────┐    │
                  │  │ 共用 Service Layer              │    │
                  │  │ SearchService / EmbeddingService │    │
                  │  │ LibraryQueryService             │    │
                  │  └────────────────────────────────┘    │
                  └────────────────────┬───────────────────┘
                                       │
                                       ▼
                         PostgreSQL + pgvector（共用）
```

### 13.6 Phase 2 預期依賴

```groovy
dependencies {
    // Phase 1：MCP Server（已有）
    implementation 'org.springframework.ai:spring-ai-starter-mcp-server-webmvc'
    implementation 'org.springaicommunity:mcp-server-security:0.1.1'

    // Phase 2：A2A Server（待套件穩定後加入）
    implementation 'org.springaicommunity:spring-ai-a2a-server-autoconfigure:${a2aVersion}'
}
```

### 13.7 Phase 2 預期功能

#### Agent Card（`/.well-known/agent.json`）

Agent Card 是 A2A 協定的身份描述文件，其他 Agent 透過此文件發現本服務的能力：

```json
{
  "name": "Documentation Agent",
  "description": "技術文件搜尋與查詢 Agent，提供混合語意搜尋能力",
  "url": "http://localhost:8081/a2a/",
  "version": "1.0.0",
  "protocolVersion": "0.3.0",
  "capabilities": {
    "streaming": false
  },
  "defaultInputModes": ["text"],
  "defaultOutputModes": ["text"],
  "skills": [
    {
      "id": "doc_search",
      "name": "Documentation Search",
      "description": "搜尋技術文件，支援全文與語意混合搜尋",
      "tags": ["documentation", "search", "semantic"]
    },
    {
      "id": "doc_explain",
      "name": "Technical Explanation",
      "description": "根據官方文件解釋技術概念",
      "tags": ["documentation", "explanation"]
    }
  ]
}
```

#### A2A 端點

| 端點 | 方法 | 說明 |
|------|------|------|
| `/a2a` | POST | JSON-RPC `sendMessage` 請求 |
| `/a2a/card` | GET | Agent Card 發現 |
| `/a2a/tasks/{id}` | GET | 任務狀態查詢 |

### 13.8 Phase 2 參考連結

- [A2A 協定規格](https://a2a-protocol.org/latest/specification/)
- [GitHub - a2aproject/A2A](https://github.com/a2aproject/A2A)
- [GitHub - spring-ai-community/spring-ai-a2a](https://github.com/spring-ai-community/spring-ai-a2a)
- [A2A 與 MCP 的關係](https://a2a-protocol.org/latest/topics/a2a-and-mcp/)
- [Spring AI Agentic Patterns Part 5](https://spring.io/blog/2026/01/29/spring-ai-agentic-patterns-a2a-integration)

---

## 變更記錄

| 版本 | 日期 | 變更內容 | 作者 |
|------|------|----------|------|
| 1.1.0 | 2026-02-05 | 新增 Phase 2：A2A（Agent-to-Agent）協定支援規劃（待套件穩定） | Claude |
| 1.0.0 | 2026-02-05 | 初始版本：MCP Server PRD 定義（Tools、Resources、Prompts、API Key 安全、架構、部署） | Claude |
