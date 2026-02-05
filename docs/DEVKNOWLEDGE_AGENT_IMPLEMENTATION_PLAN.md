# DevKnowledge Agent - 完整技術實施企劃書

> **版本**：1.0.0
> **日期**：2026-01-31
> **狀態**：技術規劃完成
> **作者**：DevKnowledge Team

---

## 目錄

1. [執行摘要](#1-執行摘要)
2. [現有架構盤點](#2-現有架構盤點)
3. [目標架構設計](#3-目標架構設計)
4. [Phase 1：MCP Server 與專案上下文](#4-phase-1mcp-server-與專案上下文)
5. [Phase 2：主動監控與通知系統](#5-phase-2主動監控與通知系統)
6. [Phase 3：團隊知識庫系統](#6-phase-3團隊知識庫系統)
7. [Phase 4：生態擴展與整合](#7-phase-4生態擴展與整合)
8. [資料庫擴展設計](#8-資料庫擴展設計)
9. [完整 API 規格](#9-完整-api-規格)
10. [安全性設計](#10-安全性設計)
11. [測試策略](#11-測試策略)
12. [部署架構](#12-部署架構)
13. [開發檢查清單](#13-開發檢查清單)
14. [風險評估與緩解](#14-風險評估與緩解)

---

## 1. 執行摘要

### 1.1 專案目標

將現有的 **Documentation Platform** 擴展為 **DevKnowledge Agent**，提供：

| 功能 | 說明 | Phase |
|------|------|-------|
| MCP Server | 讓 Claude Code / Cursor 直接查詢版本化技術文件 | 1 |
| 專案上下文解析 | 自動識別 package.json / pom.xml / build.gradle 中的依賴 | 1 |
| 主動式監控 | 偵測技術文件變更，推送 Breaking Change 通知 | 2 |
| CVE 安全掃描 | 監控 NVD/OSV 漏洞資料庫，即時警報 | 2 |
| 團隊知識庫 | 累積可傳承的 Best Practices 與技術決策記錄 | 3 |
| 生態整合 | VS Code Extension、Discord Bot、GitHub Action | 4 |

### 1.2 現有基礎能力

| 能力 | 現有實作 | 複用程度 |
|------|----------|----------|
| 向量搜尋 | pgvector + Gemini 768 維度 | 100% |
| 文件同步 | GitHub / 本地檔案 | 90%（需擴展變更偵測） |
| 版本管理 | LibraryVersion (isLatest/isLts) | 100% |
| API 認證 | BCrypt API Key + OAuth2 | 100% |
| 混合搜尋 | RRF 融合演算法 | 100% |

### 1.3 實施時程

```
2026-Q1                    2026-Q2                    2026-Q3
├── Phase 1 (4 週)         ├── Phase 2 (4 週)         ├── Phase 3 (4 週)
│   ✓ MCP Server           │   ✓ 變更偵測             │   ✓ Knowledge CRUD
│   ✓ 專案上下文解析        │   ✓ Breaking Change 分析  │   ✓ 自動學習機制
│   ✓ 版本過濾 API          │   ✓ CVE 監控             │   ✓ 管理 UI
│   ✓ CLI 工具              │   ✓ Slack 通知           │
│                          │                          │   Phase 4 (4 週)
└── MVP Release            └── Beta Release           │   ✓ VS Code Extension
                                                      │   ✓ Discord Bot
                                                      │   ✓ GitHub Action
                                                      └── GA 1.0 Release
```

---

## 2. 現有架構盤點

### 2.1 後端目錄結構

```
backend/src/main/java/io/github/samzhu/documentation/platform/
├── config/                          # Spring 配置類 (12 個)
│   ├── SecurityConfig.java          # OAuth2 + API Key 認證
│   ├── VectorStoreConfig.java       # pgvector 配置
│   ├── ExecutorConfig.java          # 非同步執行緒池
│   └── ...
├── domain/model/                    # 領域模型
│   ├── Library.java                 # 函式庫實體
│   ├── LibraryVersion.java          # 版本實體 (isLatest, isLts)
│   ├── Document.java                # 文件實體
│   ├── DocumentChunk.java           # 向量區塊 (embedding 768 維)
│   ├── CodeExample.java             # 程式碼範例
│   ├── SyncHistory.java             # 同步記錄
│   └── ApiKey.java                  # API 金鑰
├── repository/                      # Spring Data JDBC Repository
├── service/                         # 業務邏輯層
│   ├── SearchService.java           # 全文/語意/混合搜尋
│   ├── LibraryService.java          # 函式庫 CRUD + GitHub 整合
│   ├── SyncService.java             # 文件同步引擎 (非同步)
│   ├── DocumentService.java         # 文件內容檢索
│   ├── EmbeddingService.java        # Gemini 向量嵌入
│   ├── VersionService.java          # 版本管理
│   ├── DocumentChunker.java         # 滑動視窗分塊
│   └── IdService.java               # TSID 生成器
├── infrastructure/                  # 基礎設施整合
│   ├── github/                      # GitHub API 客戶端 (策略模式)
│   ├── parser/                      # 文件解析器 (MD/HTML/AsciiDoc)
│   └── vectorstore/                 # pgvector 向量儲存
├── web/api/                         # REST API Controller
│   ├── SearchApiController.java
│   ├── LibraryApiController.java
│   ├── DocumentApiController.java
│   └── ...
├── security/                        # 認證模組
│   ├── ApiKeyAuthenticationFilter.java
│   └── OAuth2AuthenticationSuccessHandler.java
└── scheduler/                       # 排程任務
    └── SyncScheduler.java
```

### 2.2 現有資料表

```sql
-- 核心表
libraries              -- 函式庫主表
library_versions       -- 版本 (is_latest, is_lts, status)
documents              -- 文件 (content, content_hash, search_vector)
document_chunks        -- 向量區塊 (embedding vector(768))
code_examples          -- 程式碼範例
sync_history           -- 同步歷史
api_keys               -- API 金鑰 (BCrypt hash)
```

### 2.3 關鍵技術參數

| 項目 | 值 |
|------|-----|
| 向量模型 | gemini-embedding-001 |
| 向量維度 | 768 |
| 向量距離 | COSINE_DISTANCE |
| 索引類型 | HNSW |
| 混合搜尋 Alpha | 0.3 (關鍵字 30%、語意 70%) |
| 最低相似度 | 0.5 |
| RRF K 常數 | 60 |

---

## 3. 目標架構設計

### 3.1 系統架構圖

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                           DevKnowledge Agent                                 │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│  ┌───────────────────────────────────────────────────────────────────────┐  │
│  │                    Integration Layer (新增)                            │  │
│  │  ┌────────────┐ ┌────────────┐ ┌────────────┐ ┌────────────┐          │  │
│  │  │ MCP Server │ │ Slack Bot  │ │Discord Bot │ │    CLI     │          │  │
│  │  │  (Phase 1) │ │ (Phase 2)  │ │ (Phase 4)  │ │ (Phase 1)  │          │  │
│  │  └────────────┘ └────────────┘ └────────────┘ └────────────┘          │  │
│  └───────────────────────────────────────────────────────────────────────┘  │
│                                    │                                         │
│  ┌───────────────────────────────────────────────────────────────────────┐  │
│  │                    Agent Layer (新增)                                  │  │
│  │  ┌─────────────────┐ ┌─────────────────┐ ┌─────────────────────────┐  │  │
│  │  │ DocumentMonitor │ │  SecurityScan   │ │ KnowledgeLearning       │  │  │
│  │  │     Agent       │ │     Agent       │ │       Agent             │  │  │
│  │  │   (Phase 2)     │ │   (Phase 2)     │ │     (Phase 3)           │  │  │
│  │  └─────────────────┘ └─────────────────┘ └─────────────────────────┘  │  │
│  └───────────────────────────────────────────────────────────────────────┘  │
│                                    │                                         │
│  ┌───────────────────────────────────────────────────────────────────────┐  │
│  │                    Core Services                                       │  │
│  │  ┌─────────────┐ ┌─────────────┐ ┌─────────────┐ ┌─────────────────┐  │  │
│  │  │SearchService│ │ SyncService │ │LibraryServ. │ │VersionService  │  │  │
│  │  │   (現有)    │ │   (擴展)    │ │   (現有)    │ │    (現有)      │  │  │
│  │  └─────────────┘ └─────────────┘ └─────────────┘ └─────────────────┘  │  │
│  │  ┌─────────────┐ ┌─────────────┐ ┌─────────────┐ ┌─────────────────┐  │  │
│  │  │ContextServ. │ │KnowledgeServ│ │ AlertService│ │NotificationServ│  │  │
│  │  │   (新增)    │ │   (新增)    │ │   (新增)    │ │    (新增)      │  │  │
│  │  └─────────────┘ └─────────────┘ └─────────────┘ └─────────────────┘  │  │
│  └───────────────────────────────────────────────────────────────────────┘  │
│                                    │                                         │
│  ┌───────────────────────────────────────────────────────────────────────┐  │
│  │                    Data Layer (PostgreSQL + pgvector)                  │  │
│  │  ┌─────────┐ ┌─────────┐ ┌─────────┐ ┌─────────┐ ┌─────────┐          │  │
│  │  │library_ │ │document_│ │project_ │ │knowledge│ │security_│          │  │
│  │  │ chunk   │ │  diff   │ │ context │ │ _entry  │ │ _alert  │          │  │
│  │  │ (現有)  │ │ (新增)  │ │ (新增)  │ │ (新增)  │ │ (新增)  │          │  │
│  │  └─────────┘ └─────────┘ └─────────┘ └─────────┘ └─────────┘          │  │
│  └───────────────────────────────────────────────────────────────────────┘  │
│                                                                              │
└─────────────────────────────────────────────────────────────────────────────┘
         │              │              │              │
         ▼              ▼              ▼              ▼
    ┌─────────┐   ┌─────────┐   ┌─────────┐   ┌─────────┐
    │ GitHub  │   │ NVD/OSV │   │  Slack  │   │ Gemini  │
    │  API    │   │   API   │   │   API   │   │   API   │
    └─────────┘   └─────────┘   └─────────┘   └─────────┘
```

### 3.2 新增模組規劃

```
backend/src/main/java/io/github/samzhu/documentation/platform/
├── mcp/                              # Phase 1: MCP Server
│   ├── McpController.java
│   ├── McpToolRegistry.java
│   ├── model/
│   │   ├── McpRequest.java
│   │   ├── McpResponse.java
│   │   └── McpToolDefinition.java
│   └── tools/
│       ├── SearchDocsTool.java
│       ├── GetVersionsTool.java
│       ├── GetCodeExampleTool.java
│       ├── GetBreakingChangesTool.java
│       └── GetMigrationGuideTool.java
├── context/                          # Phase 1: 專案上下文
│   ├── service/
│   │   └── ProjectContextService.java
│   ├── parser/
│   │   ├── ProjectConfigParser.java      # 介面
│   │   ├── PackageJsonParser.java
│   │   ├── PomXmlParser.java
│   │   ├── BuildGradleParser.java
│   │   ├── RequirementsTxtParser.java
│   │   └── GoModParser.java
│   └── model/
│       ├── ProjectContext.java
│       └── DependencyInfo.java
├── monitor/                          # Phase 2: 主動監控
│   ├── service/
│   │   ├── ChangeDetectionService.java
│   │   ├── BreakingChangeAnalyzer.java
│   │   └── CveMonitorService.java
│   ├── model/
│   │   ├── DocumentDiff.java
│   │   ├── BreakingChange.java
│   │   └── SecurityAlert.java
│   └── scheduler/
│       ├── DocumentMonitorScheduler.java
│       └── CveScanScheduler.java
├── notification/                     # Phase 2: 通知系統
│   ├── service/
│   │   ├── NotificationService.java
│   │   ├── SlackNotifier.java
│   │   ├── DiscordNotifier.java
│   │   └── TeamsNotifier.java
│   ├── model/
│   │   ├── NotificationConfig.java
│   │   └── NotificationPayload.java
│   └── bot/
│       └── SlackBotController.java
├── knowledge/                        # Phase 3: 團隊知識庫
│   ├── service/
│   │   ├── TeamKnowledgeService.java
│   │   └── KnowledgeLearningService.java
│   ├── model/
│   │   ├── KnowledgeEntry.java
│   │   └── TechnicalDecision.java
│   └── web/
│       └── KnowledgeApiController.java
└── cli/                              # Phase 1: CLI (獨立專案)
    └── (使用 GraalVM Native Image)
```

---

## 4. Phase 1：MCP Server 與專案上下文

### 4.1 MCP 協議概述

MCP (Model Context Protocol) 是 Anthropic 提出的標準協議，讓 AI 助手連接外部工具和資料來源。

```
┌────────────────┐     JSON-RPC 2.0       ┌────────────────────┐
│  Claude Code   │ ◄──────────────────► │  DevKnowledge MCP  │
│    (Client)    │      over SSE/HTTP    │      (Server)      │
└────────────────┘                       └────────────────────┘
```

### 4.2 MCP Server 實作規格

#### 4.2.1 Controller 端點

**檔案**：`mcp/McpController.java`

```java
@RestController
@RequestMapping("/mcp")
public class McpController {

    /**
     * MCP 初始化 - 回傳伺服器資訊和能力
     */
    @PostMapping("/initialize")
    public McpInitializeResponse initialize(@RequestBody McpInitializeRequest request) {
        return McpInitializeResponse.builder()
            .protocolVersion("2024-11-05")
            .serverInfo(ServerInfo.of("devknowledge", "1.0.0"))
            .capabilities(Capabilities.builder()
                .tools(ToolCapabilities.enabled())
                .resources(ResourceCapabilities.enabled())
                .build())
            .build();
    }

    /**
     * 列出可用工具
     */
    @PostMapping("/tools/list")
    public McpToolListResponse listTools() {
        return McpToolListResponse.of(toolRegistry.getAllTools());
    }

    /**
     * 執行工具
     */
    @PostMapping("/tools/call")
    public McpToolCallResponse callTool(@RequestBody McpToolCallRequest request) {
        McpTool tool = toolRegistry.getTool(request.name());
        return tool.execute(request.arguments());
    }

    /**
     * 列出可用資源
     */
    @PostMapping("/resources/list")
    public McpResourceListResponse listResources() {
        // 列出所有可查詢的函式庫
    }

    /**
     * 讀取資源
     */
    @PostMapping("/resources/read")
    public McpResourceReadResponse readResource(@RequestBody McpResourceReadRequest request) {
        // 讀取特定函式庫/版本的文件
    }
}
```

#### 4.2.2 MCP Tools 定義

| Tool 名稱 | 功能 | 參數 |
|-----------|------|------|
| `search_docs` | 搜尋技術文件 | query (必填), library, version, mode, limit |
| `get_library_versions` | 取得版本列表 | library (必填) |
| `get_code_example` | 取得程式碼範例 | library (必填), version, topic, language |
| `get_breaking_changes` | 取得 Breaking Changes | library (必填), fromVersion, toVersion |
| `get_migration_guide` | 取得遷移指南 | library (必填), fromVersion (必填), toVersion (必填) |

**search_docs Tool 實作**：

```java
@Component
public class SearchDocsTool implements McpTool {

    @Override
    public McpToolDefinition getDefinition() {
        return McpToolDefinition.builder()
            .name("search_docs")
            .description("搜尋技術文件，支援版本過濾和語意搜尋")
            .inputSchema(JsonSchema.object()
                .property("query", JsonSchema.string()
                    .description("搜尋關鍵字或問題"))
                .property("library", JsonSchema.string()
                    .description("函式庫名稱，如 spring-boot"))
                .property("version", JsonSchema.string()
                    .description("版本號，如 4.0.2，預設為最新版"))
                .property("mode", JsonSchema.string()
                    .enumValues("hybrid", "semantic", "fulltext")
                    .description("搜尋模式，預設為 hybrid"))
                .property("limit", JsonSchema.integer()
                    .description("結果數量限制，預設 10")
                    .defaultValue(10))
                .required("query")
                .build())
            .build();
    }

    @Override
    public McpToolResult execute(Map<String, Object> arguments) {
        String query = (String) arguments.get("query");
        String library = (String) arguments.get("library");
        String version = (String) arguments.get("version");
        String mode = (String) arguments.getOrDefault("mode", "hybrid");
        int limit = ((Number) arguments.getOrDefault("limit", 10)).intValue();

        // 呼叫 SearchService
        List<SearchResult> results = searchService.search(
            query, library, version, SearchMode.valueOf(mode.toUpperCase()), limit
        );

        return McpToolResult.success(formatAsMarkdown(results));
    }
}
```

### 4.3 專案上下文解析

#### 4.3.1 支援的配置檔

| 檔案 | 專案類型 | 解析重點 |
|------|----------|----------|
| `package.json` | Node.js/npm | dependencies, devDependencies |
| `pom.xml` | Maven | dependencies, parent, dependencyManagement |
| `build.gradle` / `build.gradle.kts` | Gradle | dependencies, plugins |
| `requirements.txt` | Python | 套件==版本 |
| `Cargo.toml` | Rust | [dependencies] |
| `go.mod` | Go | require (...) |

#### 4.3.2 ProjectContextService 規格

**檔案**：`context/service/ProjectContextService.java`

```java
@Service
public class ProjectContextService {

    private final List<ProjectConfigParser> parsers;
    private final LibraryRepository libraryRepository;
    private final GitHubClient gitHubClient;

    /**
     * 從 GitHub 解析專案上下文
     */
    public ProjectContext parseFromGitHub(String owner, String repo, String ref) {
        // 1. 取得 repo 根目錄檔案列表
        List<String> rootFiles = gitHubClient.listFiles(owner, repo, ref, "/");

        // 2. 找到並解析配置檔
        List<DependencyInfo> dependencies = new ArrayList<>();
        String projectType = null;

        for (ProjectConfigParser parser : parsers) {
            if (rootFiles.contains(parser.getFileName())) {
                String content = gitHubClient.getFileContent(
                    owner, repo, ref, parser.getFileName()
                );
                dependencies.addAll(parser.parse(content));
                projectType = parser.getProjectType();
                break; // 優先順序：第一個找到的
            }
        }

        // 3. 比對已知的函式庫
        Map<String, MatchedLibrary> matchedLibraries = matchWithKnownLibraries(dependencies);

        return ProjectContext.builder()
            .projectName(repo)
            .projectType(projectType)
            .dependencies(dependencies)
            .matchedLibraries(matchedLibraries)
            .analyzedAt(OffsetDateTime.now())
            .build();
    }

    /**
     * 從本地目錄解析專案上下文
     */
    public ProjectContext parseFromLocal(Path projectPath) {
        // 類似邏輯
    }

    /**
     * 帶專案上下文的搜尋
     */
    public List<SearchResult> searchWithContext(
        String query,
        ProjectContext context,
        int limit
    ) {
        // 自動限定在專案依賴的函式庫和版本範圍內搜尋
        return searchService.searchWithVersionFilter(
            query,
            context.matchedLibraries(),
            limit
        );
    }
}
```

#### 4.3.3 DependencyInfo 資料模型

```java
public record DependencyInfo(
    String groupId,           // "org.springframework.boot"
    String artifactId,        // "spring-boot-starter-web"
    String version,           // "4.0.2"
    String scope,             // "compile", "test", "dev"
    boolean isDirect,         // 直接依賴 vs 傳遞依賴
    String rawDeclaration     // 原始宣告字串
) {}
```

### 4.4 版本感知搜尋 API 擴展

**擴展 SearchService**：

```java
@Service
public class SearchService {

    // 現有方法...

    /**
     * 帶版本過濾的搜尋
     */
    public List<SearchResult> searchWithVersionFilter(
        String query,
        Map<String, MatchedLibrary> libraryVersions,
        int limit
    ) {
        // 1. 構建版本過濾條件
        // (library_name = 'spring-boot' AND version = '4.0.2')
        // OR (library_name = 'spring-ai' AND version = '2.0.0')
        FilterExpression filter = buildVersionFilter(libraryVersions);

        // 2. 執行帶過濾的混合搜尋
        return hybridSearchWithFilter(query, filter, limit);
    }

    private FilterExpression buildVersionFilter(Map<String, MatchedLibrary> libs) {
        List<FilterExpression> conditions = libs.values().stream()
            .map(lib -> FilterExpression.and(
                FilterExpression.eq("libraryName", lib.name()),
                FilterExpression.eq("version", lib.version())
            ))
            .toList();

        return FilterExpression.or(conditions);
    }
}
```

### 4.5 CLI 工具規格

**專案結構**：

```
cli/
├── package.json
├── tsconfig.json
├── src/
│   ├── index.ts               # 進入點
│   ├── commands/
│   │   ├── search.ts          # devknowledge search
│   │   ├── config.ts          # devknowledge config
│   │   ├── context.ts         # devknowledge context
│   │   └── breaking-changes.ts
│   ├── services/
│   │   ├── api-client.ts      # API 呼叫封裝
│   │   └── project-detector.ts # 專案類型偵測
│   └── utils/
│       ├── formatter.ts       # 輸出格式化
│       └── config-store.ts    # 設定檔管理
├── bin/
│   └── devknowledge           # 執行檔
└── README.md
```

**CLI 命令**：

```bash
# 安裝
npm install -g @devknowledge/cli
# 或
brew install devknowledge

# 配置
devknowledge config set api-url https://your-instance.com
devknowledge config set api-key your-key

# 搜尋文件
devknowledge search "Spring Boot CORS configuration"
devknowledge search "Spring AI embedding" --library spring-ai --version 2.0.0

# 從目前專案上下文搜尋（自動偵測 package.json / pom.xml）
devknowledge search "how to configure datasource" --with-context

# 查看 breaking changes
devknowledge breaking-changes spring-boot 3.x 4.x

# 同步專案上下文
devknowledge context sync
devknowledge context show
```

### 4.6 MCP NPM 套件

**mcp-server-devknowledge**：

```typescript
// mcp-server-devknowledge/src/index.ts
import { Server } from "@modelcontextprotocol/sdk/server/index.js";
import { StdioServerTransport } from "@modelcontextprotocol/sdk/server/stdio.js";

const DEVKNOWLEDGE_URL = process.env.DEVKNOWLEDGE_URL || "http://localhost:8080";
const DEVKNOWLEDGE_API_KEY = process.env.DEVKNOWLEDGE_API_KEY;

const server = new Server(
  { name: "devknowledge", version: "1.0.0" },
  { capabilities: { tools: {}, resources: {} } }
);

// 列出工具
server.setRequestHandler("tools/list", async () => {
  const response = await fetch(`${DEVKNOWLEDGE_URL}/mcp/tools/list`, {
    headers: { "X-API-Key": DEVKNOWLEDGE_API_KEY }
  });
  return response.json();
});

// 執行工具
server.setRequestHandler("tools/call", async (request) => {
  const response = await fetch(`${DEVKNOWLEDGE_URL}/mcp/tools/call`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      "X-API-Key": DEVKNOWLEDGE_API_KEY
    },
    body: JSON.stringify(request.params)
  });
  return response.json();
});

// 啟動
const transport = new StdioServerTransport();
await server.connect(transport);
```

**Claude Code 配置**：

```json
// ~/.config/claude/claude_desktop_config.json
{
  "mcpServers": {
    "devknowledge": {
      "command": "npx",
      "args": ["@devknowledge/mcp-server"],
      "env": {
        "DEVKNOWLEDGE_URL": "https://your-instance.com",
        "DEVKNOWLEDGE_API_KEY": "your-api-key"
      }
    }
  }
}
```

### 4.7 Phase 1 檔案清單

| 檔案 | 類型 | 說明 |
|------|------|------|
| `mcp/McpController.java` | 新增 | MCP 端點控制器 |
| `mcp/McpToolRegistry.java` | 新增 | 工具註冊中心 |
| `mcp/model/McpRequest.java` | 新增 | 請求 DTO |
| `mcp/model/McpResponse.java` | 新增 | 回應 DTO |
| `mcp/model/McpToolDefinition.java` | 新增 | 工具定義 |
| `mcp/tools/SearchDocsTool.java` | 新增 | 搜尋工具 |
| `mcp/tools/GetVersionsTool.java` | 新增 | 版本查詢工具 |
| `mcp/tools/GetCodeExampleTool.java` | 新增 | 程式碼範例工具 |
| `mcp/tools/GetBreakingChangesTool.java` | 新增 | Breaking Changes 工具 |
| `mcp/tools/GetMigrationGuideTool.java` | 新增 | 遷移指南工具 |
| `context/service/ProjectContextService.java` | 新增 | 專案上下文服務 |
| `context/parser/ProjectConfigParser.java` | 新增 | 解析器介面 |
| `context/parser/PackageJsonParser.java` | 新增 | package.json 解析 |
| `context/parser/PomXmlParser.java` | 新增 | pom.xml 解析 |
| `context/parser/BuildGradleParser.java` | 新增 | build.gradle 解析 |
| `context/model/ProjectContext.java` | 新增 | 專案上下文實體 |
| `context/model/DependencyInfo.java` | 新增 | 依賴資訊 |
| `service/SearchService.java` | 擴展 | 新增版本過濾搜尋 |
| `web/api/ContextApiController.java` | 新增 | 專案上下文 API |
| `cli/` | 新增 | CLI 獨立專案 |
| `mcp-server-devknowledge/` | 新增 | MCP NPM 套件 |

### 4.8 Phase 1 驗收標準

| 項目 | 驗收條件 | 測試方式 |
|------|----------|----------|
| MCP 初始化 | `/mcp/initialize` 回傳正確的 capabilities | 單元測試 |
| MCP 工具列表 | `/mcp/tools/list` 回傳 5 個工具定義 | 單元測試 |
| search_docs | 可正確搜尋並回傳格式化結果 | 整合測試 |
| 專案上下文 | 可解析 package.json / pom.xml / build.gradle | 單元測試 |
| 版本過濾搜尋 | 搜尋結果限定在指定版本 | 整合測試 |
| CLI search | 可執行 `devknowledge search` 並輸出結果 | E2E 測試 |
| MCP NPM 套件 | Claude Code 可成功連接並查詢 | 手動驗證 |

---

## 5. Phase 2：主動監控與通知系統

### 5.1 文件變更偵測

#### 5.1.1 ChangeDetectionService 規格

**檔案**：`monitor/service/ChangeDetectionService.java`

```java
@Service
public class ChangeDetectionService {

    /**
     * 偵測兩個版本間的文件變更
     */
    public List<DocumentDiff> detectChanges(
        String libraryId,
        String fromVersion,
        String toVersion
    ) {
        List<Document> fromDocs = documentRepository.findByVersionId(fromVersionId);
        List<Document> toDocs = documentRepository.findByVersionId(toVersionId);

        List<DocumentDiff> diffs = new ArrayList<>();

        // 建立 path -> Document 的對應
        Map<String, Document> fromMap = toPathMap(fromDocs);
        Map<String, Document> toMap = toPathMap(toDocs);

        // 新增的文件
        toMap.keySet().stream()
            .filter(path -> !fromMap.containsKey(path))
            .forEach(path -> diffs.add(DocumentDiff.added(
                toMap.get(path), libraryId, fromVersion, toVersion
            )));

        // 刪除的文件
        fromMap.keySet().stream()
            .filter(path -> !toMap.containsKey(path))
            .forEach(path -> diffs.add(DocumentDiff.removed(
                fromMap.get(path), libraryId, fromVersion, toVersion
            )));

        // 修改的文件（比對 contentHash）
        fromMap.keySet().stream()
            .filter(toMap::containsKey)
            .filter(path -> !fromMap.get(path).getContentHash()
                .equals(toMap.get(path).getContentHash()))
            .forEach(path -> diffs.add(DocumentDiff.modified(
                fromMap.get(path),
                toMap.get(path),
                libraryId,
                fromVersion,
                toVersion
            )));

        return diffs;
    }

    /**
     * 同步後自動偵測變更（在 SyncService 完成後呼叫）
     */
    @EventListener
    public void onSyncCompleted(SyncCompletedEvent event) {
        // 找出前一個版本
        Optional<LibraryVersion> previousVersion = versionService
            .findPreviousVersion(event.libraryId(), event.version());

        if (previousVersion.isPresent()) {
            List<DocumentDiff> diffs = detectChanges(
                event.libraryId(),
                previousVersion.get().getVersion(),
                event.version()
            );

            // 儲存變更記錄
            documentDiffRepository.saveAll(diffs);

            // 分析 Breaking Changes
            analyzeBreakingChanges(diffs);
        }
    }
}
```

#### 5.1.2 DocumentDiff 資料模型

```java
@Table("document_diffs")
public record DocumentDiff(
    @Id String id,
    String libraryId,
    String fromVersion,
    String toVersion,
    String documentPath,
    DiffType type,              // ADDED, REMOVED, MODIFIED
    String beforeContent,       // 變更前內容（僅 MODIFIED）
    String afterContent,        // 變更後內容
    String summary,             // AI 生成的變更摘要
    String highlights,          // JSON: 重要變更高亮
    ChangeImpact impact,        // LOW, MEDIUM, HIGH, BREAKING
    OffsetDateTime detectedAt,
    @Version Long version
) {}

public enum DiffType { ADDED, REMOVED, MODIFIED }
public enum ChangeImpact { LOW, MEDIUM, HIGH, BREAKING }
```

### 5.2 Breaking Change 分析

#### 5.2.1 BreakingChangeAnalyzer 規格

**檔案**：`monitor/service/BreakingChangeAnalyzer.java`

```java
@Service
public class BreakingChangeAnalyzer {

    private final GeminiClient geminiClient;

    /**
     * 使用 AI 分析 Breaking Changes
     */
    public BreakingChangeAnalysis analyze(DocumentDiff diff) {
        if (diff.type() == DiffType.ADDED) {
            return BreakingChangeAnalysis.none(); // 新增通常不是 breaking
        }

        String prompt = """
            分析以下技術文件變更，識別 Breaking Changes：

            **變更類型**：%s
            **文件路徑**：%s

            **變更前內容**：
            ```
            %s
            ```

            **變更後內容**：
            ```
            %s
            ```

            請以 JSON 格式回傳分析結果：
            {
              "hasBreakingChanges": boolean,
              "breakingChanges": [
                {
                  "type": "API_REMOVED" | "SIGNATURE_CHANGED" | "BEHAVIOR_CHANGED" | "DEPRECATION",
                  "description": "變更說明",
                  "affectedApis": ["受影響的 API 列表"],
                  "severity": "CRITICAL" | "HIGH" | "MEDIUM" | "LOW",
                  "migrationGuide": "遷移步驟說明"
                }
              ],
              "summary": "整體變更摘要（1-2 句）"
            }
            """.formatted(
                diff.type(),
                diff.documentPath(),
                truncate(diff.beforeContent(), 3000),
                truncate(diff.afterContent(), 3000)
            );

        String response = geminiClient.generateContent(prompt);
        return parseAnalysisResult(response);
    }

    /**
     * 批次分析 Release Notes
     */
    public ReleaseAnalysis analyzeRelease(String libraryId, String version) {
        // 嘗試找到 CHANGELOG / RELEASE_NOTES
        Optional<Document> releaseNotes = findReleaseNotes(libraryId, version);

        if (releaseNotes.isEmpty()) {
            return ReleaseAnalysis.notAvailable();
        }

        String prompt = """
            分析以下 Release Notes，提取重要資訊：

            %s

            請回傳：
            1. 主要新功能
            2. Breaking Changes
            3. 棄用警告
            4. 安全性修復
            5. 建議的遷移步驟
            """.formatted(releaseNotes.get().getContent());

        return parseReleaseAnalysis(geminiClient.generateContent(prompt));
    }
}
```

### 5.3 CVE 安全監控

#### 5.3.1 CveMonitorService 規格

**檔案**：`monitor/service/CveMonitorService.java`

```java
@Service
public class CveMonitorService {

    private final NvdApiClient nvdApiClient;
    private final OsvApiClient osvApiClient;
    private final ProjectContextRepository projectRepository;
    private final SecurityAlertRepository alertRepository;
    private final NotificationService notificationService;

    /**
     * 定期掃描 CVE（每 30 分鐘）
     */
    @Scheduled(cron = "0 */30 * * * *")
    public void scheduledScan() {
        List<ProjectContext> projects = projectRepository.findAll();

        for (ProjectContext project : projects) {
            scanProject(project);
        }
    }

    /**
     * 掃描單一專案
     */
    public List<SecurityAlert> scanProject(ProjectContext project) {
        List<SecurityAlert> newAlerts = new ArrayList<>();

        for (DependencyInfo dep : project.dependencies()) {
            // 查詢 NVD
            List<NvdVulnerability> nvdResults = nvdApiClient.search(
                dep.groupId(),
                dep.artifactId(),
                dep.version()
            );

            // 查詢 OSV
            List<OsvVulnerability> osvResults = osvApiClient.query(
                formatPackageId(dep),
                dep.version()
            );

            // 合併並去重
            List<SecurityAlert> alerts = mergeVulnerabilities(
                project.id(),
                dep,
                nvdResults,
                osvResults
            );

            // 過濾已存在的警報
            alerts = filterExisting(alerts);

            if (!alerts.isEmpty()) {
                alertRepository.saveAll(alerts);
                newAlerts.addAll(alerts);
            }
        }

        // 發送通知
        if (!newAlerts.isEmpty()) {
            notificationService.sendSecurityAlerts(project, newAlerts);
        }

        return newAlerts;
    }
}
```

#### 5.3.2 SecurityAlert 資料模型

```java
@Table("security_alerts")
public record SecurityAlert(
    @Id String id,
    String cveId,                   // CVE-2026-12345
    String projectId,               // 關聯的專案
    String affectedDependency,      // org.example:lib:1.2.3
    Severity severity,              // CRITICAL, HIGH, MEDIUM, LOW
    String title,
    String description,
    String fixedVersion,            // 修復版本
    String advisoryUrl,             // 官方公告連結
    OffsetDateTime publishedAt,
    OffsetDateTime detectedAt,
    AlertStatus status,             // NEW, ACKNOWLEDGED, RESOLVED, IGNORED
    String acknowledgedBy,
    OffsetDateTime acknowledgedAt,
    @Version Long version
) {}

public enum Severity { CRITICAL, HIGH, MEDIUM, LOW }
public enum AlertStatus { NEW, ACKNOWLEDGED, RESOLVED, IGNORED }
```

### 5.4 通知系統

#### 5.4.1 NotificationService 規格

**檔案**：`notification/service/NotificationService.java`

```java
@Service
public class NotificationService {

    private final Map<NotificationChannel, Notifier> notifiers;
    private final NotificationConfigRepository configRepository;

    /**
     * 發送文件變更通知
     */
    public void sendDocumentChangeNotification(
        String libraryId,
        String version,
        List<DocumentDiff> diffs,
        BreakingChangeAnalysis analysis
    ) {
        if (!analysis.hasBreakingChanges()) {
            return; // 只通知有 breaking changes 的情況
        }

        List<NotificationConfig> configs = configRepository
            .findByLibraryIdAndEnabled(libraryId, true);

        NotificationPayload payload = NotificationPayload.builder()
            .type(NotificationType.BREAKING_CHANGE)
            .libraryName(libraryId)
            .version(version)
            .breakingChanges(analysis.breakingChanges())
            .summary(analysis.summary())
            .timestamp(OffsetDateTime.now())
            .build();

        for (NotificationConfig config : configs) {
            if (config.notifyOn().contains(NotificationType.BREAKING_CHANGE)) {
                Notifier notifier = notifiers.get(config.channel());
                notifier.send(config, payload);
            }
        }
    }

    /**
     * 發送安全警報
     */
    public void sendSecurityAlerts(ProjectContext project, List<SecurityAlert> alerts) {
        // 按嚴重程度排序
        List<SecurityAlert> sorted = alerts.stream()
            .sorted(Comparator.comparing(SecurityAlert::severity))
            .toList();

        NotificationPayload payload = NotificationPayload.builder()
            .type(NotificationType.SECURITY_ALERT)
            .projectName(project.projectName())
            .alerts(sorted)
            .criticalCount(countBySeverity(sorted, Severity.CRITICAL))
            .highCount(countBySeverity(sorted, Severity.HIGH))
            .timestamp(OffsetDateTime.now())
            .build();

        // 根據嚴重程度選擇通知方式
        if (hasCritical(sorted)) {
            sendUrgentNotification(project, payload);
        } else {
            sendNormalNotification(project, payload);
        }
    }
}
```

#### 5.4.2 SlackNotifier 實作

**檔案**：`notification/service/SlackNotifier.java`

```java
@Component
public class SlackNotifier implements Notifier {

    private final SlackClient slackClient;

    @Override
    public NotificationChannel getChannel() {
        return NotificationChannel.SLACK;
    }

    @Override
    public void send(NotificationConfig config, NotificationPayload payload) {
        SlackMessage message = switch (payload.type()) {
            case SECURITY_ALERT -> buildSecurityAlertMessage(payload);
            case BREAKING_CHANGE -> buildBreakingChangeMessage(payload);
            case NEW_VERSION -> buildNewVersionMessage(payload);
        };

        slackClient.postMessage(
            config.channelId(),
            message,
            config.slackToken()
        );
    }

    private SlackMessage buildSecurityAlertMessage(NotificationPayload payload) {
        return SlackMessage.builder()
            .blocks(List.of(
                HeaderBlock.of(":rotating_light: 安全警報 - " + payload.projectName()),
                SectionBlock.of(String.format(
                    "*發現 %d 個安全漏洞*\n• Critical: %d\n• High: %d",
                    payload.alerts().size(),
                    payload.criticalCount(),
                    payload.highCount()
                )),
                DividerBlock.of(),
                // 列出每個 CVE
                ...payload.alerts().stream()
                    .limit(5)
                    .map(this::formatAlertBlock)
                    .toList(),
                ActionsBlock.of(
                    Button.primary("查看詳情", payload.detailsUrl()),
                    Button.of("產生修復 PR", payload.fixPrUrl())
                )
            ))
            .build();
    }

    private SlackMessage buildBreakingChangeMessage(NotificationPayload payload) {
        return SlackMessage.builder()
            .blocks(List.of(
                HeaderBlock.of(":warning: Breaking Change - " +
                    payload.libraryName() + " " + payload.version()),
                SectionBlock.of(payload.summary()),
                DividerBlock.of(),
                ...payload.breakingChanges().stream()
                    .map(bc -> SectionBlock.of(
                        "*" + bc.type() + "*\n" + bc.description()
                    ))
                    .toList(),
                ContextBlock.of("影響專案：" + String.join(", ", payload.affectedProjects()))
            ))
            .build();
    }
}
```

### 5.5 Phase 2 檔案清單

| 檔案 | 類型 | 說明 |
|------|------|------|
| `monitor/service/ChangeDetectionService.java` | 新增 | 變更偵測服務 |
| `monitor/service/BreakingChangeAnalyzer.java` | 新增 | Breaking Change 分析 |
| `monitor/service/CveMonitorService.java` | 新增 | CVE 監控服務 |
| `monitor/model/DocumentDiff.java` | 新增 | 文件差異實體 |
| `monitor/model/BreakingChange.java` | 新增 | Breaking Change 實體 |
| `monitor/model/SecurityAlert.java` | 新增 | 安全警報實體 |
| `monitor/scheduler/DocumentMonitorScheduler.java` | 新增 | 監控排程 |
| `monitor/scheduler/CveScanScheduler.java` | 新增 | CVE 掃描排程 |
| `notification/service/NotificationService.java` | 新增 | 通知服務 |
| `notification/service/SlackNotifier.java` | 新增 | Slack 通知 |
| `notification/service/DiscordNotifier.java` | 新增 | Discord 通知 |
| `notification/service/TeamsNotifier.java` | 新增 | Teams 通知 |
| `notification/model/NotificationConfig.java` | 新增 | 通知配置實體 |
| `notification/model/NotificationPayload.java` | 新增 | 通知內容 |
| `notification/bot/SlackBotController.java` | 新增 | Slack Bot 端點 |
| `infrastructure/nvd/NvdApiClient.java` | 新增 | NVD API 客戶端 |
| `infrastructure/osv/OsvApiClient.java` | 新增 | OSV API 客戶端 |
| `repository/DocumentDiffRepository.java` | 新增 | 差異 Repository |
| `repository/SecurityAlertRepository.java` | 新增 | 警報 Repository |
| `repository/NotificationConfigRepository.java` | 新增 | 通知配置 Repository |
| `web/api/MonitorApiController.java` | 新增 | 監控 API |
| `web/api/AlertApiController.java` | 新增 | 警報 API |
| `web/api/NotificationApiController.java` | 新增 | 通知配置 API |
| `service/SyncService.java` | 擴展 | 觸發變更偵測事件 |

### 5.6 Phase 2 驗收標準

| 項目 | 驗收條件 | 測試方式 |
|------|----------|----------|
| 變更偵測 | 同步後自動偵測文件新增/修改/刪除 | 整合測試 |
| Breaking Change 分析 | AI 可識別 API 移除、簽章變更等 | 整合測試 (Mock Gemini) |
| CVE 掃描 | 可查詢 NVD/OSV 並比對專案依賴 | 整合測試 (Mock API) |
| Slack 通知 | Breaking Change 可推送到 Slack | 整合測試 (Mock Slack) |
| 安全警報 | CRITICAL CVE 即時推送 | 整合測試 |
| 警報管理 | 可確認/忽略/解決警報 | API 測試 |

---

## 6. Phase 3：團隊知識庫系統

### 6.1 Knowledge Base 架構

#### 6.1.1 KnowledgeEntry 資料模型

```java
@Table("knowledge_entries")
public record KnowledgeEntry(
    @Id String id,
    String teamId,
    String title,
    String content,                 // Markdown 格式
    KnowledgeType type,             // PROBLEM_SOLUTION, BEST_PRACTICE, FAQ, TECHNICAL_DECISION
    List<String> tags,
    List<String> relatedLibraries,  // 關聯的函式庫 ID
    String createdBy,
    String updatedBy,
    int usageCount,                 // 被查詢次數
    int helpfulVotes,               // 有幫助投票
    int notHelpfulVotes,
    float[] embedding,              // 768 維度向量
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt,
    @Version Long version
) {}

public enum KnowledgeType {
    PROBLEM_SOLUTION,    // 問題與解決方案
    BEST_PRACTICE,       // 最佳實踐
    FAQ,                 // 常見問題
    TECHNICAL_DECISION   // 技術決策記錄 (ADR)
}
```

#### 6.1.2 TechnicalDecision 資料模型 (ADR)

```java
@Table("technical_decisions")
public record TechnicalDecision(
    @Id String id,
    String teamId,
    String title,                   // 如 "選擇 pgvector 作為向量資料庫"
    String context,                 // 背景說明
    String decision,                // 決策內容
    List<Alternative> alternatives, // 考慮過的替代方案
    String rationale,               // 決策理由
    List<String> consequences,      // 後果/影響
    String decidedBy,
    List<String> participants,
    OffsetDateTime decidedAt,
    DecisionStatus status,          // PROPOSED, ACCEPTED, SUPERSEDED, DEPRECATED
    String supersededBy,            // 被哪個決策取代
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt,
    @Version Long version
) {}

public record Alternative(
    String name,
    String description,
    List<String> pros,
    List<String> cons
) {}

public enum DecisionStatus { PROPOSED, ACCEPTED, SUPERSEDED, DEPRECATED }
```

### 6.2 TeamKnowledgeService 規格

**檔案**：`knowledge/service/TeamKnowledgeService.java`

```java
@Service
public class TeamKnowledgeService {

    private final KnowledgeRepository knowledgeRepository;
    private final EmbeddingService embeddingService;
    private final VectorStore vectorStore;

    /**
     * 新增知識條目
     */
    @Transactional
    public KnowledgeEntry createEntry(String teamId, CreateKnowledgeRequest request) {
        // 1. 生成向量嵌入
        String textToEmbed = request.title() + "\n\n" + request.content();
        float[] embedding = embeddingService.embed(textToEmbed);

        // 2. 建立實體
        KnowledgeEntry entry = KnowledgeEntry.builder()
            .id(idService.generateId())
            .teamId(teamId)
            .title(request.title())
            .content(request.content())
            .type(request.type())
            .tags(request.tags())
            .relatedLibraries(request.relatedLibraries())
            .createdBy(getCurrentUserId())
            .embedding(embedding)
            .createdAt(OffsetDateTime.now())
            .build();

        return knowledgeRepository.save(entry);
    }

    /**
     * 語意搜尋團隊知識
     */
    public List<KnowledgeEntry> search(String teamId, String query, int limit) {
        // 1. 生成查詢向量
        float[] queryEmbedding = embeddingService.embed(query);

        // 2. 向量搜尋（限定團隊）
        String filter = "team_id = '" + teamId + "'";
        List<VectorSearchResult> results = vectorStore.similaritySearch(
            queryEmbedding,
            limit,
            0.5,  // 最低相似度
            filter
        );

        // 3. 轉換並增加使用次數
        return results.stream()
            .map(r -> {
                KnowledgeEntry entry = knowledgeRepository.findById(r.id()).orElseThrow();
                knowledgeRepository.incrementUsageCount(entry.id());
                return entry;
            })
            .toList();
    }

    /**
     * 融合搜尋：團隊知識 + 官方文件
     */
    public FusedSearchResult fusedSearch(
        String teamId,
        String query,
        ProjectContext context,
        int limit
    ) {
        // 1. 搜尋團隊知識
        List<KnowledgeEntry> teamResults = search(teamId, query, limit);

        // 2. 搜尋官方文件（帶專案上下文）
        List<SearchResult> officialResults = context != null
            ? searchService.searchWithContext(query, context, limit)
            : searchService.hybridSearch(query, null, null, limit);

        return FusedSearchResult.builder()
            .teamKnowledge(teamResults)
            .officialDocs(officialResults)
            .query(query)
            .searchedAt(OffsetDateTime.now())
            .build();
    }

    /**
     * 投票
     */
    @Transactional
    public void vote(String entryId, boolean helpful) {
        if (helpful) {
            knowledgeRepository.incrementHelpfulVotes(entryId);
        } else {
            knowledgeRepository.incrementNotHelpfulVotes(entryId);
        }
    }
}
```

### 6.3 自動學習機制

#### 6.3.1 KnowledgeLearningService 規格

**檔案**：`knowledge/service/KnowledgeLearningService.java`

```java
@Service
public class KnowledgeLearningService {

    private final GeminiClient geminiClient;
    private final TeamKnowledgeService knowledgeService;

    /**
     * 從高評價的 Q&A 互動中學習
     */
    public Optional<KnowledgeEntry> learnFromInteraction(
        String teamId,
        SearchInteraction interaction
    ) {
        // 只學習評價高的回答
        if (interaction.helpfulVotes() < 3) {
            return Optional.empty();
        }

        // 檢查是否已有類似知識
        List<KnowledgeEntry> similar = knowledgeService.search(
            teamId,
            interaction.question(),
            3
        );

        if (hasSimilarEntry(similar, 0.85)) {
            return Optional.empty(); // 已有類似內容
        }

        // AI 評估是否值得收錄
        String evaluation = geminiClient.generateContent("""
            評估以下 Q&A 是否值得收錄到團隊知識庫：

            **問題**：%s

            **回答**：%s

            **有幫助投票**：%d

            請判斷：
            1. 這是通用問題還是特定專案問題？
            2. 答案是否完整且正確？
            3. 是否對其他團隊成員有參考價值？

            回傳 JSON：
            {
              "shouldAdd": boolean,
              "reason": "原因",
              "suggestedTitle": "建議標題",
              "suggestedTags": ["標籤1", "標籤2"],
              "suggestedType": "PROBLEM_SOLUTION | BEST_PRACTICE | FAQ"
            }
            """.formatted(
                interaction.question(),
                interaction.answer(),
                interaction.helpfulVotes()
            ));

        EvaluationResult result = parseEvaluation(evaluation);

        if (result.shouldAdd()) {
            return Optional.of(knowledgeService.createEntry(teamId,
                CreateKnowledgeRequest.builder()
                    .title(result.suggestedTitle())
                    .content(formatAsKnowledge(interaction))
                    .type(result.suggestedType())
                    .tags(result.suggestedTags())
                    .build()
            ));
        }

        return Optional.empty();
    }

    /**
     * 從 GitHub PR Review 學習
     */
    @EventListener
    public void onPullRequestReview(GitHubPullRequestEvent event) {
        if (event.action() != GitHubAction.REVIEW_SUBMITTED) {
            return;
        }

        // 找出有教育意義的 review comments
        List<ReviewComment> valuable = event.review().comments().stream()
            .filter(c -> c.reactions().thumbsUp() >= 2)
            .filter(this::isEducational)
            .toList();

        for (ReviewComment comment : valuable) {
            String teamId = resolveTeamId(event.repository());

            knowledgeService.createEntry(teamId,
                CreateKnowledgeRequest.builder()
                    .title(extractTitle(comment))
                    .content(formatReviewAsKnowledge(comment, event.pullRequest()))
                    .type(KnowledgeType.BEST_PRACTICE)
                    .tags(extractTags(comment))
                    .relatedLibraries(detectLibraries(event.pullRequest()))
                    .build()
            );
        }
    }

    private boolean isEducational(ReviewComment comment) {
        // 檢查是否包含解釋性內容
        String body = comment.body().toLowerCase();
        return body.contains("應該") ||
               body.contains("建議") ||
               body.contains("最佳實踐") ||
               body.contains("best practice") ||
               body.contains("注意") ||
               body.contains("原因是");
    }
}
```

### 6.4 Knowledge Base Web UI

#### 6.4.1 前端頁面結構

```
frontend/src/pages/
├── knowledge/
│   ├── Knowledge.jsx           # 知識庫首頁（列表 + 搜尋）
│   ├── KnowledgeEntry.jsx      # 知識條目詳情
│   ├── KnowledgeCreate.jsx     # 新增知識條目
│   ├── KnowledgeEdit.jsx       # 編輯知識條目
│   └── TechnicalDecisions.jsx  # 技術決策記錄列表
├── components/
│   ├── KnowledgeCard.jsx       # 知識卡片
│   ├── KnowledgeTypeFilter.jsx # 類型篩選
│   ├── TagCloud.jsx            # 標籤雲
│   └── VoteButtons.jsx         # 投票按鈕
```

#### 6.4.2 Knowledge.jsx 組件

```jsx
// frontend/src/pages/knowledge/Knowledge.jsx

export default function Knowledge() {
  const [entries, setEntries] = useState([]);
  const [searchQuery, setSearchQuery] = useState('');
  const [filter, setFilter] = useState({
    type: 'all',
    tags: [],
    sortBy: 'relevance' // relevance, recent, popular
  });
  const [searchMode, setSearchMode] = useState('fused'); // fused, team, official

  const handleSearch = async () => {
    const results = await api.searchKnowledge({
      query: searchQuery,
      mode: searchMode,
      type: filter.type,
      tags: filter.tags,
      limit: 20
    });
    setEntries(results);
  };

  return (
    <div className="knowledge-page">
      <header className="knowledge-header">
        <h1>團隊知識庫</h1>
        <button
          className="btn btn-primary"
          onClick={() => navigate('/knowledge/new')}
        >
          新增知識條目
        </button>
      </header>

      <div className="search-section">
        <SearchBar
          value={searchQuery}
          onChange={setSearchQuery}
          onSearch={handleSearch}
          placeholder="搜尋團隊知識..."
        />

        <div className="search-mode-toggle">
          <button
            className={searchMode === 'fused' ? 'active' : ''}
            onClick={() => setSearchMode('fused')}
          >
            融合搜尋
          </button>
          <button
            className={searchMode === 'team' ? 'active' : ''}
            onClick={() => setSearchMode('team')}
          >
            僅團隊知識
          </button>
          <button
            className={searchMode === 'official' ? 'active' : ''}
            onClick={() => setSearchMode('official')}
          >
            僅官方文件
          </button>
        </div>
      </div>

      <div className="content-area">
        <aside className="filter-sidebar">
          <KnowledgeTypeFilter
            value={filter.type}
            onChange={(type) => setFilter({ ...filter, type })}
          />
          <TagCloud
            selected={filter.tags}
            onChange={(tags) => setFilter({ ...filter, tags })}
          />
        </aside>

        <main className="results-area">
          {searchMode === 'fused' && entries.teamKnowledge && (
            <section className="team-results">
              <h2>團隊知識</h2>
              {entries.teamKnowledge.map(entry => (
                <KnowledgeCard key={entry.id} entry={entry} />
              ))}
            </section>
          )}

          {searchMode === 'fused' && entries.officialDocs && (
            <section className="official-results">
              <h2>官方文件</h2>
              {entries.officialDocs.map(doc => (
                <DocumentCard key={doc.id} document={doc} />
              ))}
            </section>
          )}

          {searchMode !== 'fused' && (
            <KnowledgeList entries={entries} />
          )}
        </main>
      </div>
    </div>
  );
}
```

### 6.5 Phase 3 檔案清單

| 檔案 | 類型 | 說明 |
|------|------|------|
| `knowledge/service/TeamKnowledgeService.java` | 新增 | 知識庫服務 |
| `knowledge/service/KnowledgeLearningService.java` | 新增 | 自動學習服務 |
| `knowledge/model/KnowledgeEntry.java` | 新增 | 知識條目實體 |
| `knowledge/model/TechnicalDecision.java` | 新增 | 技術決策實體 |
| `knowledge/web/KnowledgeApiController.java` | 新增 | 知識庫 API |
| `knowledge/web/DecisionApiController.java` | 新增 | 技術決策 API |
| `repository/KnowledgeRepository.java` | 新增 | 知識 Repository |
| `repository/TechnicalDecisionRepository.java` | 新增 | 決策 Repository |
| `frontend/src/pages/knowledge/Knowledge.jsx` | 新增 | 知識庫首頁 |
| `frontend/src/pages/knowledge/KnowledgeEntry.jsx` | 新增 | 條目詳情 |
| `frontend/src/pages/knowledge/KnowledgeCreate.jsx` | 新增 | 新增頁面 |
| `frontend/src/pages/knowledge/KnowledgeEdit.jsx` | 新增 | 編輯頁面 |
| `frontend/src/pages/knowledge/TechnicalDecisions.jsx` | 新增 | 決策列表 |
| `frontend/src/components/KnowledgeCard.jsx` | 新增 | 知識卡片 |
| `frontend/src/components/VoteButtons.jsx` | 新增 | 投票元件 |
| `frontend/src/services/knowledge-api.js` | 新增 | 知識 API 客戶端 |

### 6.6 Phase 3 驗收標準

| 項目 | 驗收條件 | 測試方式 |
|------|----------|----------|
| CRUD API | 可新增/讀取/更新/刪除知識條目 | API 測試 |
| 向量搜尋 | 知識條目可進行語意搜尋 | 整合測試 |
| 融合搜尋 | 結果區分團隊知識 vs 官方文件 | 整合測試 |
| 投票機制 | 可投票並統計 | API 測試 |
| 自動學習 | 高評價 Q&A 可觸發知識建立 | 整合測試 |
| Web UI | 可在瀏覽器中操作知識庫 | E2E 測試 |

---

## 7. Phase 4：生態擴展與整合

### 7.1 VS Code Extension

**專案結構**：

```
vscode-devknowledge/
├── package.json
├── tsconfig.json
├── src/
│   ├── extension.ts              # 進入點
│   ├── commands/
│   │   ├── searchDocs.ts         # 搜尋文件命令
│   │   ├── showBreakingChanges.ts
│   │   └── insertCodeExample.ts
│   ├── providers/
│   │   ├── hoverProvider.ts      # 游標懸停顯示文件
│   │   ├── completionProvider.ts # 自動完成
│   │   └── codeLensProvider.ts   # 程式碼透鏡
│   ├── views/
│   │   ├── knowledgePanel.ts     # 側邊欄知識面板
│   │   └── searchResultsView.ts  # 搜尋結果視圖
│   └── services/
│       └── apiClient.ts
├── resources/
│   └── icon.png
└── README.md
```

**主要功能**：

| 功能 | 說明 |
|------|------|
| 快捷搜尋 | Cmd+Shift+D 開啟搜尋面板 |
| 游標懸停 | 懸停在函式庫名稱上顯示文件預覽 |
| 程式碼透鏡 | 在 import 行上方顯示 "查看文件" 連結 |
| 知識面板 | 側邊欄顯示團隊知識庫 |
| 自動完成 | 根據文件提供 API 建議 |

### 7.2 Discord Bot

**功能設計**：

```
/search <query>              # 搜尋文件
/breaking-changes <library>  # 查看 breaking changes
/subscribe <library>         # 訂閱函式庫更新
/unsubscribe <library>       # 取消訂閱
/scan <github-url>           # 掃描專案漏洞
```

### 7.3 GitHub Action

**devknowledge-scan Action**：

```yaml
# .github/workflows/devknowledge-scan.yml
name: DevKnowledge Security Scan

on:
  push:
    branches: [main]
  pull_request:
  schedule:
    - cron: '0 0 * * *'

jobs:
  scan:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4

      - name: DevKnowledge Dependency Scan
        uses: devknowledge/action@v1
        with:
          api-url: ${{ secrets.DEVKNOWLEDGE_URL }}
          api-key: ${{ secrets.DEVKNOWLEDGE_API_KEY }}
          fail-on: critical  # critical, high, medium, low
          ignore-file: .devknowledge-ignore

      - name: Comment PR with results
        if: github.event_name == 'pull_request'
        uses: devknowledge/action/comment@v1
        with:
          github-token: ${{ secrets.GITHUB_TOKEN }}
```

### 7.4 Phase 4 驗收標準

| 項目 | 驗收條件 |
|------|----------|
| VS Code Extension | 發布到 VS Code Marketplace |
| Discord Bot | 可回應 /search 和 /subscribe 指令 |
| GitHub Action | PR 自動掃描並留言結果 |

---

## 8. 資料庫擴展設計

### 8.1 新增資料表 DDL

```sql
-- ============================================
-- Phase 1: 專案上下文
-- ============================================

CREATE TABLE project_contexts (
    id VARCHAR(13) PRIMARY KEY,
    project_name VARCHAR(255) NOT NULL,
    project_type VARCHAR(50) NOT NULL,          -- maven, gradle, npm, etc.
    github_owner VARCHAR(255),
    github_repo VARCHAR(255),
    github_ref VARCHAR(255),                    -- branch or tag
    dependencies JSONB NOT NULL DEFAULT '[]',   -- DependencyInfo[]
    matched_libraries JSONB,                    -- { libraryId: version }
    analyzed_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    version BIGINT DEFAULT 0,

    CONSTRAINT uq_project_github UNIQUE (github_owner, github_repo)
);

CREATE INDEX idx_project_type ON project_contexts(project_type);
CREATE INDEX idx_project_github ON project_contexts(github_owner, github_repo);

-- ============================================
-- Phase 2: 文件變更追蹤
-- ============================================

CREATE TABLE document_diffs (
    id VARCHAR(13) PRIMARY KEY,
    library_id VARCHAR(13) NOT NULL REFERENCES libraries(id) ON DELETE CASCADE,
    from_version VARCHAR(50) NOT NULL,
    to_version VARCHAR(50) NOT NULL,
    document_path VARCHAR(500) NOT NULL,
    diff_type VARCHAR(20) NOT NULL,             -- ADDED, REMOVED, MODIFIED
    before_content TEXT,
    after_content TEXT,
    summary TEXT,                               -- AI 生成的摘要
    highlights JSONB,                           -- 重要變更高亮
    impact VARCHAR(20) DEFAULT 'LOW',           -- LOW, MEDIUM, HIGH, BREAKING
    detected_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version BIGINT DEFAULT 0,

    CONSTRAINT uq_diff_path UNIQUE (library_id, from_version, to_version, document_path)
);

CREATE INDEX idx_diff_library ON document_diffs(library_id);
CREATE INDEX idx_diff_versions ON document_diffs(from_version, to_version);
CREATE INDEX idx_diff_impact ON document_diffs(impact);

-- ============================================
-- Phase 2: 安全警報
-- ============================================

CREATE TABLE security_alerts (
    id VARCHAR(13) PRIMARY KEY,
    cve_id VARCHAR(50),
    project_id VARCHAR(13) REFERENCES project_contexts(id) ON DELETE CASCADE,
    affected_dependency VARCHAR(255) NOT NULL,  -- groupId:artifactId:version
    severity VARCHAR(20) NOT NULL,              -- CRITICAL, HIGH, MEDIUM, LOW
    title VARCHAR(500),
    description TEXT,
    fixed_version VARCHAR(50),
    advisory_url VARCHAR(500),
    published_at TIMESTAMP WITH TIME ZONE,
    detected_at TIMESTAMP WITH TIME ZONE NOT NULL,
    status VARCHAR(20) DEFAULT 'NEW',           -- NEW, ACKNOWLEDGED, RESOLVED, IGNORED
    acknowledged_by VARCHAR(255),
    acknowledged_at TIMESTAMP WITH TIME ZONE,
    resolved_at TIMESTAMP WITH TIME ZONE,
    version BIGINT DEFAULT 0,

    CONSTRAINT uq_alert_cve_project UNIQUE (cve_id, project_id, affected_dependency)
);

CREATE INDEX idx_alert_project ON security_alerts(project_id);
CREATE INDEX idx_alert_severity ON security_alerts(severity);
CREATE INDEX idx_alert_status ON security_alerts(status);
CREATE INDEX idx_alert_cve ON security_alerts(cve_id);

-- ============================================
-- Phase 2: 通知配置
-- ============================================

CREATE TABLE notification_configs (
    id VARCHAR(13) PRIMARY KEY,
    team_id VARCHAR(13) NOT NULL,
    name VARCHAR(100) NOT NULL,
    channel VARCHAR(20) NOT NULL,               -- SLACK, DISCORD, TEAMS, EMAIL
    channel_config JSONB NOT NULL,              -- 頻道特定配置
    library_ids VARCHAR(13)[],                  -- 監控的 library
    notify_on VARCHAR(50)[] NOT NULL,           -- BREAKING_CHANGE, DEPRECATION, SECURITY, NEW_VERSION
    is_enabled BOOLEAN DEFAULT TRUE,
    created_by VARCHAR(255),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    version BIGINT DEFAULT 0
);

CREATE INDEX idx_notification_team ON notification_configs(team_id);
CREATE INDEX idx_notification_enabled ON notification_configs(is_enabled);

-- ============================================
-- Phase 3: 團隊知識庫
-- ============================================

CREATE TABLE knowledge_entries (
    id VARCHAR(13) PRIMARY KEY,
    team_id VARCHAR(13) NOT NULL,
    title VARCHAR(255) NOT NULL,
    content TEXT NOT NULL,
    entry_type VARCHAR(50) NOT NULL,            -- PROBLEM_SOLUTION, BEST_PRACTICE, FAQ, TECHNICAL_DECISION
    tags VARCHAR(100)[],
    related_libraries VARCHAR(13)[],
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    usage_count INT DEFAULT 0,
    helpful_votes INT DEFAULT 0,
    not_helpful_votes INT DEFAULT 0,
    embedding vector(768),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    version BIGINT DEFAULT 0
);

CREATE INDEX idx_knowledge_team ON knowledge_entries(team_id);
CREATE INDEX idx_knowledge_type ON knowledge_entries(entry_type);
CREATE INDEX idx_knowledge_tags ON knowledge_entries USING GIN(tags);
CREATE INDEX idx_knowledge_embedding ON knowledge_entries
    USING hnsw (embedding vector_cosine_ops);

-- ============================================
-- Phase 3: 技術決策記錄 (ADR)
-- ============================================

CREATE TABLE technical_decisions (
    id VARCHAR(13) PRIMARY KEY,
    team_id VARCHAR(13) NOT NULL,
    title VARCHAR(255) NOT NULL,
    context TEXT NOT NULL,
    decision TEXT NOT NULL,
    alternatives JSONB,                         -- Alternative[]
    rationale TEXT,
    consequences JSONB,                         -- string[]
    decided_by VARCHAR(255),
    participants VARCHAR(255)[],
    decided_at TIMESTAMP WITH TIME ZONE,
    status VARCHAR(20) DEFAULT 'PROPOSED',      -- PROPOSED, ACCEPTED, SUPERSEDED, DEPRECATED
    superseded_by VARCHAR(13),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    version BIGINT DEFAULT 0
);

CREATE INDEX idx_decision_team ON technical_decisions(team_id);
CREATE INDEX idx_decision_status ON technical_decisions(status);
```

### 8.2 Flyway 遷移腳本

```
backend/src/main/resources/db/migration/
├── V1__init.sql                    # 現有 schema
├── V2__project_context.sql         # Phase 1: 專案上下文
├── V3__document_diffs.sql          # Phase 2: 變更追蹤
├── V4__security_alerts.sql         # Phase 2: 安全警報
├── V5__notification_configs.sql    # Phase 2: 通知配置
├── V6__knowledge_entries.sql       # Phase 3: 知識庫
└── V7__technical_decisions.sql     # Phase 3: 技術決策
```

---

## 9. 完整 API 規格

### 9.1 Phase 1 API

#### MCP API

| 端點 | 方法 | 說明 |
|------|------|------|
| `/mcp/initialize` | POST | MCP 初始化 |
| `/mcp/tools/list` | POST | 列出可用工具 |
| `/mcp/tools/call` | POST | 執行工具 |
| `/mcp/resources/list` | POST | 列出可用資源 |
| `/mcp/resources/read` | POST | 讀取資源 |

#### 專案上下文 API

| 端點 | 方法 | 說明 |
|------|------|------|
| `POST /api/context` | POST | 解析專案上下文 |
| `GET /api/context/{id}` | GET | 取得專案上下文 |
| `GET /api/context/{id}/search` | GET | 帶上下文搜尋 |
| `DELETE /api/context/{id}` | DELETE | 刪除專案上下文 |

### 9.2 Phase 2 API

#### 監控 API

| 端點 | 方法 | 說明 |
|------|------|------|
| `GET /api/monitor/diffs` | GET | 取得文件變更列表 |
| `GET /api/monitor/diffs/{id}` | GET | 取得變更詳情 |
| `GET /api/monitor/breaking-changes` | GET | 取得 breaking changes |

#### 警報 API

| 端點 | 方法 | 說明 |
|------|------|------|
| `GET /api/alerts` | GET | 取得安全警報列表 |
| `GET /api/alerts/{id}` | GET | 取得警報詳情 |
| `POST /api/alerts/{id}/acknowledge` | POST | 確認警報 |
| `POST /api/alerts/{id}/resolve` | POST | 解決警報 |
| `POST /api/alerts/{id}/ignore` | POST | 忽略警報 |

#### 通知 API

| 端點 | 方法 | 說明 |
|------|------|------|
| `GET /api/notifications/configs` | GET | 列出通知配置 |
| `POST /api/notifications/configs` | POST | 新增通知配置 |
| `PUT /api/notifications/configs/{id}` | PUT | 更新通知配置 |
| `DELETE /api/notifications/configs/{id}` | DELETE | 刪除通知配置 |
| `POST /api/notifications/test` | POST | 測試通知 |

### 9.3 Phase 3 API

#### 知識庫 API

| 端點 | 方法 | 說明 |
|------|------|------|
| `GET /api/knowledge` | GET | 列出知識條目 |
| `POST /api/knowledge` | POST | 新增知識條目 |
| `GET /api/knowledge/{id}` | GET | 取得知識條目 |
| `PUT /api/knowledge/{id}` | PUT | 更新知識條目 |
| `DELETE /api/knowledge/{id}` | DELETE | 刪除知識條目 |
| `GET /api/knowledge/search` | GET | 搜尋知識庫 |
| `GET /api/knowledge/fused-search` | GET | 融合搜尋 |
| `POST /api/knowledge/{id}/vote` | POST | 投票 |

#### 技術決策 API

| 端點 | 方法 | 說明 |
|------|------|------|
| `GET /api/decisions` | GET | 列出技術決策 |
| `POST /api/decisions` | POST | 新增技術決策 |
| `GET /api/decisions/{id}` | GET | 取得技術決策 |
| `PUT /api/decisions/{id}` | PUT | 更新技術決策 |
| `POST /api/decisions/{id}/accept` | POST | 接受決策 |
| `POST /api/decisions/{id}/supersede` | POST | 取代決策 |

---

## 10. 安全性設計

### 10.1 認證機制

| 場景 | 認證方式 | 實作 |
|------|----------|------|
| Web UI | OAuth2 PKCE | 現有 SecurityConfig |
| MCP Server | API Key | X-API-Key header |
| CLI | API Key | 本地配置檔 |
| Slack Bot | Slack App Token | 環境變數 |
| GitHub Action | API Key | GitHub Secret |

### 10.2 API Key 驗證擴展

```java
// 擴展現有的 ApiKeyAuthenticationFilter

@Override
protected void doFilterInternal(
    HttpServletRequest request,
    HttpServletResponse response,
    FilterChain chain
) {
    String apiKey = request.getHeader("X-API-Key");

    if (apiKey != null) {
        // 驗證 API Key
        Optional<ApiKey> key = apiKeyService.validate(apiKey);

        if (key.isPresent() && key.get().isActive()) {
            // 設定 Authentication
            ApiKeyAuthentication auth = new ApiKeyAuthentication(key.get());
            SecurityContextHolder.getContext().setAuthentication(auth);

            // 更新最後使用時間
            apiKeyService.updateLastUsed(key.get().getId());
        }
    }

    chain.doFilter(request, response);
}
```

### 10.3 權限控制

```java
// 基於團隊的權限

@PreAuthorize("hasPermission(#teamId, 'KNOWLEDGE_READ')")
public List<KnowledgeEntry> listKnowledge(String teamId);

@PreAuthorize("hasPermission(#teamId, 'KNOWLEDGE_WRITE')")
public KnowledgeEntry createEntry(String teamId, CreateKnowledgeRequest request);

@PreAuthorize("hasPermission(#teamId, 'ALERT_MANAGE')")
public void acknowledgeAlert(String teamId, String alertId);
```

### 10.4 資料隔離

- 每個 Team 有獨立的知識庫
- 專案上下文僅限建立者和團隊成員存取
- 安全警報依專案權限過濾
- MCP 查詢受 API Key 關聯的權限限制

---

## 11. 測試策略

### 11.1 單元測試

| 模組 | 測試重點 | Mock 策略 |
|------|----------|-----------|
| MCP Tools | 工具定義正確性、執行邏輯 | Mock SearchService |
| Parser | 各種配置檔解析正確性 | 使用測試檔案 |
| ChangeDetection | 差異偵測邏輯 | Mock Repository |
| Notifier | 訊息格式正確性 | Mock HTTP Client |

### 11.2 整合測試

| 場景 | 測試方式 | 環境 |
|------|----------|------|
| MCP Server | MCP Client SDK | Testcontainers PostgreSQL |
| 向量搜尋 | 真實 Gemini API | @Tag("integration") |
| Slack 通知 | Mock Slack API | MockWebServer |
| CVE 監控 | Mock NVD/OSV | MockWebServer |

### 11.3 E2E 測試

```bash
# MCP 整合測試
npx @devknowledge/mcp-server test

# CLI 測試
devknowledge search "spring boot" --dry-run

# 通知測試
curl -X POST /api/notifications/test \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"channel": "SLACK", "config": {...}}'
```

### 11.4 測試覆蓋率目標

| 層級 | 目標 |
|------|------|
| Service | >= 80% |
| Controller | >= 70% |
| Repository | >= 60% |
| 整體 | >= 75% |

---

## 12. 部署架構

### 12.1 Docker Compose（開發/單機）

```yaml
# docker-compose.yml

services:
  devknowledge:
    build: ./backend
    ports:
      - "8080:8080"
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/devknowledge
      SPRING_DATASOURCE_USERNAME: devknowledge
      SPRING_DATASOURCE_PASSWORD: ${DB_PASSWORD}
      PLATFORM_GOOGLE_API_KEY: ${GOOGLE_API_KEY}
      SLACK_BOT_TOKEN: ${SLACK_BOT_TOKEN}
    depends_on:
      postgres:
        condition: service_healthy

  mcp-server:
    build: ./mcp-server-devknowledge
    ports:
      - "3000:3000"
    environment:
      DEVKNOWLEDGE_URL: http://devknowledge:8080
      DEVKNOWLEDGE_API_KEY: ${API_KEY}

  postgres:
    image: pgvector/pgvector:pg17
    environment:
      POSTGRES_DB: devknowledge
      POSTGRES_USER: devknowledge
      POSTGRES_PASSWORD: ${DB_PASSWORD}
    volumes:
      - pgdata:/var/lib/postgresql/data
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U devknowledge"]
      interval: 5s
      timeout: 5s
      retries: 5

volumes:
  pgdata:
```

### 12.2 Kubernetes（生產）

```yaml
# k8s/deployment.yaml

apiVersion: apps/v1
kind: Deployment
metadata:
  name: devknowledge
spec:
  replicas: 3
  selector:
    matchLabels:
      app: devknowledge
  template:
    metadata:
      labels:
        app: devknowledge
    spec:
      containers:
        - name: devknowledge
          image: ghcr.io/org/devknowledge:latest
          ports:
            - containerPort: 8080
          env:
            - name: SPRING_DATASOURCE_URL
              valueFrom:
                secretKeyRef:
                  name: devknowledge-secrets
                  key: database-url
          resources:
            requests:
              memory: "512Mi"
              cpu: "250m"
            limits:
              memory: "2Gi"
              cpu: "1000m"
          readinessProbe:
            httpGet:
              path: /actuator/health/readiness
              port: 8080
            initialDelaySeconds: 30
            periodSeconds: 10
          livenessProbe:
            httpGet:
              path: /actuator/health/liveness
              port: 8080
            initialDelaySeconds: 60
            periodSeconds: 30
```

### 12.3 監控指標

| 指標 | 說明 | 類型 |
|------|------|------|
| `mcp_requests_total` | MCP 請求總數 | Counter |
| `mcp_request_duration_seconds` | MCP 請求延遲 | Histogram |
| `search_latency_seconds` | 搜尋延遲 | Histogram |
| `sync_duration_seconds` | 同步耗時 | Histogram |
| `alert_count` | 未處理警報數 | Gauge |
| `knowledge_entries_total` | 知識條目總數 | Gauge |
| `cve_scan_last_run` | CVE 掃描最後執行時間 | Gauge |

---

## 13. 開發檢查清單

### 13.1 Phase 1 檢查清單

- [ ] **MCP Server**
  - [ ] McpController 端點實作
  - [ ] McpToolRegistry 工具註冊
  - [ ] search_docs Tool
  - [ ] get_library_versions Tool
  - [ ] get_code_example Tool
  - [ ] get_breaking_changes Tool
  - [ ] get_migration_guide Tool
  - [ ] MCP 初始化回應
  - [ ] 單元測試
  - [ ] 整合測試

- [ ] **專案上下文**
  - [ ] ProjectContextService
  - [ ] PackageJsonParser
  - [ ] PomXmlParser
  - [ ] BuildGradleParser
  - [ ] RequirementsTxtParser
  - [ ] GoModParser
  - [ ] 專案上下文 API Controller
  - [ ] 單元測試

- [ ] **版本過濾搜尋**
  - [ ] SearchService 擴展
  - [ ] 版本過濾條件建構
  - [ ] 整合測試

- [ ] **CLI**
  - [ ] search 命令
  - [ ] config 命令
  - [ ] context 命令
  - [ ] breaking-changes 命令
  - [ ] NPM 發布準備

- [ ] **MCP NPM 套件**
  - [ ] 實作轉發邏輯
  - [ ] README 文件
  - [ ] NPM 發布

- [ ] **文件**
  - [ ] API 文件更新
  - [ ] 配置說明
  - [ ] 使用者指南

### 13.2 Phase 2 檢查清單

- [ ] **變更偵測**
  - [ ] ChangeDetectionService
  - [ ] DocumentDiff 實體
  - [ ] SyncCompletedEvent 事件發布
  - [ ] 變更記錄儲存

- [ ] **Breaking Change 分析**
  - [ ] BreakingChangeAnalyzer
  - [ ] AI 提示詞設計
  - [ ] 結果解析

- [ ] **CVE 監控**
  - [ ] NvdApiClient
  - [ ] OsvApiClient
  - [ ] CveMonitorService
  - [ ] SecurityAlert 實體
  - [ ] 排程掃描

- [ ] **通知系統**
  - [ ] NotificationService
  - [ ] SlackNotifier
  - [ ] DiscordNotifier
  - [ ] TeamsNotifier
  - [ ] NotificationConfig 實體
  - [ ] 通知配置 API

- [ ] **資料庫**
  - [ ] document_diffs 表
  - [ ] security_alerts 表
  - [ ] notification_configs 表
  - [ ] Flyway 遷移腳本

### 13.3 Phase 3 檢查清單

- [ ] **知識庫核心**
  - [ ] KnowledgeEntry 實體
  - [ ] TechnicalDecision 實體
  - [ ] TeamKnowledgeService
  - [ ] 向量索引

- [ ] **自動學習**
  - [ ] KnowledgeLearningService
  - [ ] Q&A 學習邏輯
  - [ ] PR Review 學習

- [ ] **API**
  - [ ] KnowledgeApiController
  - [ ] DecisionApiController
  - [ ] 投票 API

- [ ] **前端**
  - [ ] Knowledge.jsx
  - [ ] KnowledgeEntry.jsx
  - [ ] KnowledgeCreate.jsx
  - [ ] KnowledgeEdit.jsx
  - [ ] TechnicalDecisions.jsx
  - [ ] 組件：KnowledgeCard, VoteButtons

### 13.4 Phase 4 檢查清單

- [ ] **VS Code Extension**
  - [ ] 搜尋命令
  - [ ] 游標懸停
  - [ ] 程式碼透鏡
  - [ ] 知識面板
  - [ ] Marketplace 發布

- [ ] **Discord Bot**
  - [ ] /search 命令
  - [ ] /subscribe 命令
  - [ ] 通知發送

- [ ] **GitHub Action**
  - [ ] 掃描邏輯
  - [ ] PR 留言
  - [ ] Marketplace 發布

---

## 14. 風險評估與緩解

| 風險 | 可能性 | 影響 | 緩解措施 |
|------|--------|------|----------|
| MCP 協議變更 | 中 | 高 | 抽象化 MCP 層，使用適配器模式 |
| Gemini API 限流 | 高 | 中 | 實作快取、批次處理、fallback 策略 |
| 向量維度不匹配 | 低 | 高 | 嚴格版本管理、遷移腳本預備 |
| Slack API 變更 | 低 | 中 | 使用官方 SDK、監控 changelog |
| 資料量增長 | 中 | 中 | 分頁優化、索引調整、資料清理策略 |
| CVE 資料庫不可用 | 中 | 中 | 多來源備援 (NVD + OSV)、本地快取 |
| 團隊成員離職 | 中 | 中 | 完整文件、知識庫累積 |

---

## 附錄

### A. 相關資源

- [MCP 官方規格](https://modelcontextprotocol.io/)
- [Spring AI 文件](https://docs.spring.io/spring-ai/)
- [pgvector 文件](https://github.com/pgvector/pgvector)
- [Slack API 文件](https://api.slack.com/)
- [NVD API 文件](https://nvd.nist.gov/developers)
- [OSV API 文件](https://osv.dev/docs/)

### B. 詞彙表

| 術語 | 說明 |
|------|------|
| MCP | Model Context Protocol，AI 連接外部工具的標準協議 |
| RRF | Reciprocal Rank Fusion，混合搜尋融合演算法 |
| TSID | Time Sorted ID，時間排序的唯一識別碼 (13 字元 Crockford Base32) |
| CVE | Common Vulnerabilities and Exposures，通用漏洞揭露 |
| ADR | Architecture Decision Records，架構決策記錄 |
| NVD | National Vulnerability Database，美國國家漏洞資料庫 |
| OSV | Open Source Vulnerabilities，開源漏洞資料庫 |
| HNSW | Hierarchical Navigable Small Worlds，向量索引演算法 |

### C. 配置檔範例

**application.yaml 擴展**：

```yaml
platform:
  # 現有配置...

  # Phase 1: MCP
  mcp:
    enabled: true
    tools:
      search-docs: true
      get-versions: true
      get-code-example: true
      get-breaking-changes: true
      get-migration-guide: true

  # Phase 2: 監控
  monitor:
    change-detection:
      enabled: true
    cve-scan:
      enabled: true
      cron: "0 */30 * * * *"
      sources:
        - nvd
        - osv

  # Phase 2: 通知
  notification:
    slack:
      enabled: true
      bot-token: ${SLACK_BOT_TOKEN:}
    discord:
      enabled: false
    teams:
      enabled: false

  # Phase 3: 知識庫
  knowledge:
    auto-learn:
      enabled: true
      min-helpful-votes: 3
```

---

> **文件維護者**：DevKnowledge Team
> **最後更新**：2026-01-31
> **下一次審查**：2026-02-28
