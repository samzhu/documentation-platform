# Documentation Platform - 專案記憶

## 專案架構
- **Monorepo**：`frontend/`（React 19）、`backend/`（Spring Boot 4.0.2）、`mcpserver/`（MCP Server）
- 三者共用同一個 PostgreSQL + pgvector 資料庫
- `gradlew` 位於 `backend/` 目錄內，不在根目錄

## 關鍵技術決策

### 可觀測性（OpenTelemetry）→ [詳細](observability.md)
- `spring-boot-starter-opentelemetry` 已包含 bridge-otel + registry-otlp + exporter-otlp
- **不可同時使用** `micrometer-tracing-bridge-brave`，兩者互斥
- Micrometer Metrics OTLP **僅支援 HTTP**（無 gRPC），Issue #5040 追蹤中

### MCP Server
- `spring-ai-starter-mcp-server`（STDIO）需換成 `spring-ai-starter-mcp-server-webmvc`
- 原因：`mcp-server-security:0.1.1` 僅支援 WebMVC（不支援 WebFlux/STDIO/SSE）
- API Key 格式：`{id}.{secret}`，Header: `X-API-key`
- PRD 位於 `mcpserver/docs/PRD.md`

### A2A（Phase 2，待穩定）
- 社群套件：`org.springaicommunity:spring-ai-a2a-server-autoconfigure:0.2.0`
- 啟動條件：套件 ≥ 1.0 或納入 Spring AI Core

### 向量嵌入
- 模型：gemini-embedding-001，768 維
- backend 用 Google GenAI，mcpserver 用 Vertex AI（向量相同）

## 常見陷阱
- `datasource-micrometer` 用 BOM 管理版本（`2.1.0`），不要寫死版本號
- Cloud Run 部署 Metrics 必須走 HTTP `:4318`，Traces/Logs 走 gRPC `:4317`
