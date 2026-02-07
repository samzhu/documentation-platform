# CLAUDE.md

程式碼都要寫上好理解的繁體中文註解。

## 專案概述

文件管理後台（Monorepo 架構）

| 子專案 | 技術 | 目錄 |
|--------|------|------|
| 前端 | React 19 + Vite 6 | `frontend/` |
| 後端 | Spring Boot 4.0.2 + Spring AI | `backend/` |
| MCP Server | Spring Boot 4.0.2 + Spring AI MCP | `mcpserver/` |

## 快速指令

| 任務 | 指令 |
|------|------|
| 前端開發 | `cd frontend && npm run dev` |
| 後端開發 | `cd backend && ./gradlew bootRun` |
| 後端測試 | `cd backend && ./gradlew test` |
| MCP Server 開發 | `cd mcpserver && ./gradlew bootRun` |
| MCP Server 編譯 | `cd mcpserver && ./gradlew compileJava` |

## 子專案 CLAUDE.md

進入子目錄後，Claude 會自動載入對應的 CLAUDE.md：

| 目錄 | 載入的指引 |
|------|-----------|
| `frontend/` | 前端專屬（React、CSS、設計系統） |
| `backend/` | 後端專屬（Entity、API、TDD） |
| `mcpserver/` | MCP Server 專屬（MCP Tools/Resources/Prompts、唯讀架構） |

## 共用規範

- **ID 格式**：TSID（13 字元 Crockford Base32）
- **PRD 維護**：修改功能時同步更新對應的 `docs/PRD.md`
- **共用資料庫**：backend、mcpserver 共用同一個 PostgreSQL + pgvector，Schema 由 backend Liquibase 管理

## 問題處理原則

1. **先研究再行動** - 不要急著改 code
2. **查閱官方文件** - Release Notes、Migration Guide
3. **確認版本關係** - 上下游框架相容性
4. **正確解決問題** - 避免 Workaround
