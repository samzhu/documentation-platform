# Documentation Platform - PRD

> **版本**：2.3.0
> **狀態**：Draft

---

## 1. 產品概述

### 1.1 產品定位

文件管理後台，採用 **Read-Write 架構**，負責文件同步、向量嵌入、Web 管理介面。

### 1.2 目標用戶

| 用戶類型 | 使用場景 |
|----------|----------|
| 開發團隊 | 團隊共享的內部文件管理 |
| DevOps | 自動同步與監控 |
| 系統管理員 | API Key 管理與設定 |

### 1.3 核心價值

| 價值 | 說明 |
|------|------|
| 視覺化 | Apple Liquid Glass 風格 Web UI |
| 自動同步 | GitHub 儲存庫自動抓取與索引 |
| 智慧索引 | Gemini 向量嵌入 + 全文索引 |

---

## 2. 系統架構

```
┌─────────────────────────────────────────────────────────────────────┐
│                     Documentation Platform                           │
├─────────────────────────────────────────────────────────────────────┤
│  Presentation Layer                                                  │
│  ├── Web UI (React 19 + Vite, SPA)                                   │
│  └── REST API (6 個 Controller)                                      │
├─────────────────────────────────────────────────────────────────────┤
│  Service Layer                                                       │
│  ├── LibraryService (CRUD)         ├── SyncService (同步引擎)        │
│  ├── SearchService (混合搜尋)       └── EmbeddingService (向量嵌入)   │
├─────────────────────────────────────────────────────────────────────┤
│  Infrastructure Layer                                                │
│  ├── GitHubClient (文件抓取)        ├── LocalFileClient (本地文件)    │
│  └── DocumentParser (MD/HTML/AsciiDoc)                              │
├─────────────────────────────────────────────────────────────────────┤
│  Scheduler                                                           │
│  └── SyncScheduler (定時同步)                                        │
└─────────────────────────────────────────────────────────────────────┘
                                │
                                ▼
┌─────────────────────────────────────────────────────────────────────┐
│                    PostgreSQL + pgvector                             │
└─────────────────────────────────────────────────────────────────────┘
```

### 2.1 技術堆疊

| 組件 | 技術 | 版本 |
|------|------|------|
| 語言 | Java | 25 |
| 框架 | Spring Boot | 4.0.2 |
| 前端 | React + Vite | 19 / 6.x |
| AI | Spring AI (Gemini Embedding) | 2.0.0-M2 |
| 向量 | PostgreSQL + pgvector | - |
| 測試 | Testcontainers | - |

---

## 3. Web UI 規格

> **架構**：React 19 + Vite + HashRouter（SPA）

### 3.1 前端專案結構

```
frontend/
├── src/
│   ├── components/          # 共用組件（Sidebar、RequireAuth）
│   ├── pages/               # 頁面組件（Dashboard、Libraries、Search...）
│   ├── hooks/               # Custom Hooks（useAuth）
│   ├── services/            # API 與認證服務（api.js、auth.js）
│   └── styles/              # CSS 樣式
├── package.json
├── vite.config.js           # Vite 配置（輸出到 src/main/resources/static）
└── index.html
```

### 3.2 開發與建構

**前端**（`frontend/` 目錄）：
```bash
npm install        # 安裝依賴
npm run dev        # 開發模式（port 5173，代理 API 到 8080）
npm run build      # 建構 → 輸出到 src/main/resources/static
```

**後端**（根目錄）：
```bash
./gradlew bootRun  # 開發/執行
./gradlew build    # 建構 JAR
```

### 3.3 主要頁面（5 個）

| 頁面 | 路徑 | 功能 |
|------|------|------|
| Dashboard | `/#/` | 系統概覽：統計卡片（函式庫、文件、向量片段、API 金鑰） |
| Libraries | `/#/libraries` | 函式庫列表：卡片式顯示、新增、刪除、同步觸發 |
| Search | `/#/search` | 文件搜尋：語意搜尋介面 |
| Sync History | `/#/sync-history` | 同步記錄：歷史列表、狀態追蹤 |
| API Keys | `/#/api-keys` | API Key 管理：建立、撤銷 |

### 3.4 關鍵頁面說明

#### Dashboard

- 統計卡片：Libraries、Documents、Chunks、Sync Status
- 最近同步活動：表格顯示最近 5 筆
- 快速操作：Add Library、Test Search

#### Libraries

- 卡片式顯示每個函式庫
- 顯示：名稱、描述、版本數、文件數、最後同步時間
- 操作：View、Edit、Delete

#### Search

- 搜尋框 + 篩選條件（Library、Version）
- 結果列表：標題、片段、分數、來源

---

## 4. REST API 規格

### 4.1 端點列表

| 端點 | 方法 | 說明 |
|------|------|------|
| `/api/libraries` | GET | 列出函式庫 |
| `/api/libraries` | POST | 新增函式庫 |
| `/api/libraries/{id}` | GET | 取得函式庫詳情 |
| `/api/libraries/{id}` | PUT | 更新函式庫 |
| `/api/libraries/{id}` | DELETE | 刪除函式庫 |
| `/api/libraries/{id}/sync` | POST | 觸發同步 |
| `/api/search` | GET | 搜尋文件 |
| `/api/api-keys` | GET | 列出 API Keys |
| `/api/api-keys` | POST | 建立 API Key |
| `/api/api-keys/{id}` | DELETE | 撤銷 API Key |

### 4.2 請求/回應範例

#### 新增函式庫

```http
POST /api/libraries
Content-Type: application/json

{
  "name": "spring-boot",
  "displayName": "Spring Boot",
  "description": "Spring Boot 官方文件",
  "sourceType": "GITHUB",
  "sourceUrl": "https://github.com/spring-projects/spring-boot",
  "category": "backend"
}
```

#### 觸發同步

```http
POST /api/libraries/0ABC123DEF456/sync
Content-Type: application/json

{
  "version": "4.0.1"
}
```

---

## 5. 同步引擎

### 5.1 支援的來源

| 來源類型 | 說明 | 狀態 |
|----------|------|------|
| GITHUB | GitHub 公開儲存庫 | 已支援 |
| LOCAL | 本地目錄 | 已支援 |
| MANUAL | 手動上傳 | 規劃中 |

### 5.2 同步流程

```
1. 觸發同步（手動或排程）
     │
     ▼
2. 根據 sourceType 選擇 Client
   ┌──────────────┬──────────────┐
   │ GitHubClient │ LocalClient  │
   └──────────────┴──────────────┘
     │
     ▼
3. 抓取文件列表
     │
     ▼
4. 解析文件內容
   ┌────────────┬────────────┬────────────┐
   │ Markdown   │ AsciiDoc   │ HTML       │
   │ Parser     │ Parser     │ Parser     │
   └────────────┴────────────┴────────────┘
     │
     ▼
5. 文件切塊（Chunking）
     │
     ▼
6. 生成向量嵌入（Gemini）
     │
     ▼
7. 儲存到資料庫
     │
     ▼
8. 更新同步歷史
```

### 5.3 排程配置

```yaml
platform:
  sync:
    cron: "0 0 2 * * *"  # 每天凌晨 2 點
```

---

## 6. 向量嵌入

### 6.1 模型配置

| 項目 | 值 |
|------|-----|
| 模型 | gemini-embedding-001 |
| 維度 | 768 |
| Provider | Google GenAI |

### 6.2 區塊化策略

- 區塊大小：約 500-1000 字元
- 重疊：100 字元
- 保留標題層級資訊

---

## 7. 配置與環境變數

### 7.1 application.yaml 範例

```yaml
spring:
  datasource:
    url: ${platform-db-url:jdbc:postgresql://localhost:5432/mydatabase}
    username: ${platform-db-username:myuser}
    password: ${platform-db-password:secret}

  ai:
    google:
      genai:
        embedding:
          api-key: ${platform-google-api-key}
          text:
            options:
              model: gemini-embedding-001
              dimensions: 768

    vectorstore:
      pgvector:
        dimensions: 768
        distance-type: COSINE_DISTANCE
        index-type: HNSW

platform:
  features:
    web-ui: true
    api-key-auth: false
    sync-scheduling: true

  sync:
    cron: "0 0 2 * * *"

  search:
    hybrid:
      alpha: 0.3
      min-similarity: 0.5
```

### 7.2 環境變數

| 環境變數 | 說明 | 必要 |
|----------|------|------|
| `platform-google-api-key` | Google GenAI API Key | 是 |
| `platform-db-url` | 資料庫連線 URL | 是 |
| `platform-db-username` | 資料庫使用者名稱 | 是 |
| `platform-db-password` | 資料庫密碼 | 是 |
| `platform-oauth2-client-id` | OAuth2 Client ID | 是 |
| `platform-oauth2-client-secret` | OAuth2 Client Secret | 是 |

---

## 8. 認證與授權

### 8.1 OAuth2 整合

管理平台使用 OAuth2 進行認證，整合現有的 Spring Authorization Server。

| 項目 | 值 |
|------|-----|
| Authorization Server | （由環境變數 `platform-oauth2-issuer-uri` 注入） |
| 認證流程 | Authorization Code + PKCE |
| 使用者 | 內部管理員（Web UI） |

### 8.2 Profile 分離配置

OAuth2 配置使用 Profile 分離，支援不同環境：

| Profile | 說明 | OAuth2 狀態 |
|---------|------|-------------|
| `local` | 本地開發 | 禁用（允許所有請求） |
| `oauth2` | 生產環境 | 啟用（需要認證） |

**本地開發**：
```bash
./gradlew bootRun  # 使用 local profile，OAuth2 禁用
```

**生產環境**：
```bash
java -jar app.jar --spring.profiles.active=oauth2
```

### 8.3 配置檔案

| 檔案 | 說明 |
|------|------|
| `application.yaml` | Resource Server 配置（JWT 驗證） |
| `application-local.yaml` | 本地開發，禁用 OAuth2 自動配置 |
| `application-oauth2.yaml` | OAuth2 Client 配置（前端登入） |

**application-oauth2.yaml**：
```yaml
spring:
  security:
    oauth2:
      client:
        registration:
          omnihubs:
            provider: omnihubs
            client-id: ${platform-oauth2-client-id}
            client-secret: ${platform-oauth2-client-secret}
            authorization-grant-type: authorization_code
            redirect-uri: "{baseUrl}/login/oauth2/code/{registrationId}"
            scope:
              - openid
              - profile
        provider:
          omnihubs:
            issuer-uri: ${platform-oauth2-issuer-uri}

platform:
  features:
    oauth2: true
```

### 8.4 API 端點權限

| 端點 | 認證需求 |
|------|----------|
| `/api/search/**` | 公開 |
| `/api/dashboard/stats` | 公開 |
| `/api/libraries/**` | 需要 OAuth2 Token |
| `/api/api-keys/**` | 需要 OAuth2 Token |
| `/api/sync/**` | 需要 OAuth2 Token |

### 8.5 API Key 管理

此平台負責 API Key 的 CRUD 管理，供 MCP Server 使用：

| 項目 | 規範 |
|------|------|
| 格式 | `dmcp_` + Base64-URL-encoded（32 字節隨機數） |
| 儲存 | BCrypt 雜湊 |
| 用途 | MCP Server 驗證（與本平台共用資料庫）|

---

## 9. 部署指南

### 9.1 Docker 快速啟動

```bash
docker run -d \
  --name documentation-platform \
  -p 8080:8080 \
  -e platform-google-api-key=your-api-key \
  -e platform-db-url=jdbc:postgresql://db:5432/mydatabase \
  -e platform-db-username=myuser \
  -e platform-db-password=secret \
  ghcr.io/samzhu/documentation-platform:latest
```

### 9.2 docker-compose.yaml 範例（含 MCP Server）

```yaml
services:
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

  mcp-server:
    image: ghcr.io/samzhu/documentation-mcp-server:latest
    ports:
      - "8081:8080"
    environment:
      platform-db-url: jdbc:postgresql://db:5432/mydatabase
      platform-db-username: myuser
      platform-db-password: secret
    depends_on:
      - db

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

---

## 變更記錄

| 版本 | 日期 | 變更內容 | 作者 |
|------|------|----------|------|
| 2.8.0 | 2026-01-27 | **模組化與修復整合版本**：前端架構完整模組化（42 個 CSS 模組、精簡 index.css）、按鈕系統重構（Warning/Info/Outline 變體、React Button 組件）、Tag 組件視覺改進（Liquid Glass 風格）、修復 Libraries 統計數據顯示問題（跨表查詢） | Claude |
| 2.3.0 | 2026-01-26 | **前端 UI 重新設計**：React + Vite 前後端分離架構、新增文件庫獨立頁面、Library Detail 分離設計、多選版本批次同步 | Claude |
| 2.2.0 | 2026-01-26 | **React + Vite 前端重構**：Thymeleaf → React 19 + Vite、OAuth2 Profile 分離配置 | Claude |
| 2.0.0 | 2026-01-26 | **架構重構里程碑**：Spring Boot 4.0.2、Spring AI 2.0.0-M2、React 19、Apple Liquid Glass 設計系統完整實作 | Claude |
| 1.0.0 | 2026-01-24 | 初始版本，從 DocMCP Server 拆分 | Claude |
