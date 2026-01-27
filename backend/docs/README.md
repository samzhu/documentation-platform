# 後端技術文件

詳細的後端架構與開發說明。

## 文件清單

| 文件 | 說明 |
|------|------|
| [../CLAUDE.md](../CLAUDE.md) | AI 開發指引（快速參考） |
| [../../docs/PRD.md](../../docs/PRD.md) | 產品需求文件 |

## 架構分層詳解

### Web 層（REST API）

```
web/api/
├── LibraryController.java      # 文件庫 CRUD
├── DocumentController.java     # 文件管理
├── SearchController.java       # 向量搜尋
├── SyncController.java         # 同步觸發
├── ApiKeyController.java       # API Key 管理
└── SettingsController.java     # 系統設定
```

### Service 層

```
service/
├── LibraryService.java         # 文件庫業務邏輯
├── DocumentService.java        # 文件處理
├── EmbeddingService.java       # 向量嵌入（Spring AI）
├── SearchService.java          # 混合搜尋
├── SyncService.java            # GitHub 同步
└── ApiKeyService.java          # API Key 生命週期
```

### Infrastructure 層

```
infrastructure/
├── github/                     # GitHub API 整合
│   ├── GitHubClient.java
│   └── GitHubContentFetcher.java
└── parser/                     # Markdown 解析
    └── MarkdownParser.java
```

## 資料庫 Schema

### 核心表格

| 表格 | 說明 |
|------|------|
| `libraries` | 文件庫定義 |
| `documents` | 文件內容 |
| `document_chunks` | 文件區塊（含向量） |
| `sync_histories` | 同步歷史 |
| `api_keys` | API Key（與 MCP Server 共用） |

### pgvector 使用

```sql
-- 向量欄位
embedding vector(768)

-- 相似度搜尋
SELECT * FROM document_chunks
ORDER BY embedding <=> $1::vector
LIMIT 10;
```
