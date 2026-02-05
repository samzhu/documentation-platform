# Documentation Platform 優化實作企劃書

> 基於現有程式碼分析與業界最佳實踐

---

## 目錄

1. [現況分析](#現況分析)
2. [Phase 1：基礎優化](#phase-1基礎優化)
3. [Phase 2：搜尋品質提升](#phase-2搜尋品質提升)
4. [Phase 3：效能優化](#phase-3效能優化)
5. [Phase 4：品質保證](#phase-4品質保證)
6. [實作時程建議](#實作時程建議)

---

## 現況分析

### 現有架構

| 元件 | 檔案位置 | 現況 |
|------|----------|------|
| Embedding 服務 | `service/EmbeddingService.java` | 封裝 Spring AI EmbeddingModel，支援單一/批次處理 |
| 向量儲存 | `infrastructure/vectorstore/DocumentChunkVectorStore.java` | 自訂 VectorStore 實作，批次上限 100 |
| 文件切塊 | `service/DocumentChunker.java` | 固定大小切塊（1000 字元 + 200 重疊） |
| 搜尋服務 | `service/SearchService.java` | RRF 混合搜尋（alpha=0.3） |
| 同步服務 | `service/SyncService.java` | 非同步 GitHub/Local 同步 |

### 資料庫 Schema

```sql
-- document_chunks 表結構
CREATE TABLE document_chunks (
    id VARCHAR(13) PRIMARY KEY,
    document_id VARCHAR(13) NOT NULL,
    chunk_index INTEGER NOT NULL,
    content TEXT NOT NULL,
    embedding vector(768),  -- Gemini embedding-001
    token_count INTEGER,
    metadata JSONB DEFAULT '{}'
);

-- 現有索引（無 HNSW 參數優化）
CREATE INDEX idx_document_chunks_metadata ON document_chunks USING GIN(metadata);
```

### 配置參數

```yaml
# application.yaml 現有配置
spring.ai.vectorstore.pgvector:
  dimensions: 768
  distance-type: COSINE_DISTANCE
  index-type: HNSW

platform.search.hybrid:
  alpha: 0.3
  min-similarity: 0.5
```

---

## Phase 1：基礎優化

### 1.1 Gemini Batch API 評估

#### 現況分析

目前 `DocumentChunkVectorStore.add()` 已使用批次處理：

```java
// 現有程式碼 - 每批 100 個
private static final int EMBEDDING_BATCH_SIZE = 100;

for (int batchStart = 0; batchStart < documents.size(); batchStart += EMBEDDING_BATCH_SIZE) {
    List<Document> batch = documents.subList(batchStart, batchEnd);
    List<float[]> embeddings = embeddingModel.embed(texts);
    // ...
}
```

#### 評估結論

| 方案 | 成本 | 延遲 | 適用場景 |
|------|------|------|----------|
| 現有同步批次 | 標準價格 | 即時 | 即時同步、小量更新 |
| Gemini Batch API | 50% 折扣 | 最長 24 小時 | 大規模離線處理 |

**建議**：保留現有架構。Gemini Batch API 適合離線預處理大量歷史資料，不適合即時同步場景。

#### 可選：新增離線批次處理功能

如需支援大規模初始化，可新增排程任務：

```java
// 新增檔案：service/BatchEmbeddingService.java
@Service
public class BatchEmbeddingService {

    /**
     * 提交離線批次嵌入任務
     * 適用場景：初始化大量歷史文件（10萬+）
     *
     * @param versionId 版本 ID
     * @return 批次任務 ID
     */
    public String submitBatchJob(String versionId) {
        // 1. 查詢所有待處理的 chunks
        // 2. 生成 JSONL 檔案
        // 3. 呼叫 Gemini Batch API
        // 4. 記錄任務狀態
    }

    /**
     * 輪詢批次任務狀態
     */
    @Scheduled(fixedDelay = 60000)
    public void pollBatchJobs() {
        // 檢查進行中的批次任務
        // 完成後更新 document_chunks.embedding
    }
}
```

---

### 1.2 pgvector HNSW 索引調優

#### 變更項目

**1. 更新 schema.sql - 新增優化的 HNSW 索引**

```sql
-- 新增至 schema.sql

-- 刪除舊的預設 HNSW 索引（如存在）
DROP INDEX IF EXISTS idx_document_chunks_embedding;

-- 建立優化的 HNSW 索引
-- m=24: 每個節點連接數（預設 16），提高索引品質
-- ef_construction=100: 建構時搜尋範圍（預設 64），建立更精確的索引
CREATE INDEX IF NOT EXISTS idx_document_chunks_embedding_hnsw ON document_chunks
    USING hnsw (embedding vector_cosine_ops)
    WITH (m = 24, ef_construction = 100);

-- 索引建立後的維護建議（手動執行）
-- VACUUM ANALYZE document_chunks;
```

**2. 更新 application.yaml - 新增 HNSW 搜尋參數**

```yaml
platform:
  search:
    hybrid:
      alpha: 0.3
      min-similarity: 0.5
    # 新增 HNSW 配置
    hnsw:
      # ef_search: 搜尋時的候選佇列大小
      # 越大品質越好但速度越慢，建議 100-200
      ef-search: 100
```

**3. 更新 DocumentChunkVectorStore.java - 搜尋前設定 ef_search**

```java
// 新增常數（或從配置讀取）
@Value("${platform.search.hnsw.ef-search:100}")
private int hnswEfSearch;

@Override
public List<Document> similaritySearch(SearchRequest request) {
    // 設定 HNSW 搜尋參數（SESSION 範圍）
    jdbcTemplate.execute("SET LOCAL hnsw.ef_search = " + hnswEfSearch);

    // 其餘程式碼不變...
}
```

#### 參數說明

| 參數 | 預設值 | 建議值 | 說明 |
|------|--------|--------|------|
| `m` | 16 | 24 | 每個節點的連接數，影響索引大小和搜尋品質 |
| `ef_construction` | 64 | 100 | 建構索引時的搜尋範圍，越大索引越精確 |
| `ef_search` | 40 | 100 | 搜尋時的候選佇列大小，越大召回率越高 |

#### 預期效益

- 搜尋召回率提升 10-20%
- 搜尋速度維持穩定（ef_search=100 時約 2-5ms）

---

## Phase 2：搜尋品質提升

### 2.1 語意切塊 (Semantic Chunking)

#### 現況問題

現有 `DocumentChunker` 使用固定大小切塊：

```java
// 現有邏輯
private static final int DEFAULT_CHUNK_SIZE = 1000;
private static final int DEFAULT_OVERLAP = 200;

// 問題：可能切斷語意完整的段落
```

#### 建議方案：層級式語意切塊

```java
// 新增檔案：service/SemanticDocumentChunker.java
@Service
public class SemanticDocumentChunker {

    private final EmbeddingModel embeddingModel;

    // 語意相似度閾值（低於此值則切割）
    private static final double SIMILARITY_THRESHOLD = 0.5;

    /**
     * 語意切塊策略
     *
     * 1. 先按段落（\n\n）分割
     * 2. 計算相鄰段落的嵌入相似度
     * 3. 相似度低於閾值處進行切割
     * 4. 確保每個 chunk 不超過 token 上限
     */
    public List<ChunkResult> chunkSemantically(String content) {
        // Step 1: 按段落分割
        List<String> paragraphs = splitByParagraphs(content);

        // Step 2: 計算段落嵌入
        List<float[]> embeddings = embeddingModel.embed(
            paragraphs.stream().map(this::truncateForEmbedding).toList()
        );

        // Step 3: 根據相似度合併/分割
        List<ChunkResult> chunks = new ArrayList<>();
        StringBuilder currentChunk = new StringBuilder();
        int currentTokens = 0;

        for (int i = 0; i < paragraphs.size(); i++) {
            String paragraph = paragraphs.get(i);
            int paragraphTokens = estimateTokenCount(paragraph);

            // 檢查是否需要切割
            boolean shouldSplit = false;
            if (i > 0) {
                double similarity = cosineSimilarity(embeddings.get(i-1), embeddings.get(i));
                shouldSplit = similarity < SIMILARITY_THRESHOLD;
            }

            // 檢查 token 上限
            if (currentTokens + paragraphTokens > MAX_CHUNK_TOKENS || shouldSplit) {
                if (currentChunk.length() > 0) {
                    chunks.add(new ChunkResult(chunks.size(), currentChunk.toString(), currentTokens));
                    currentChunk = new StringBuilder();
                    currentTokens = 0;
                }
            }

            currentChunk.append(paragraph).append("\n\n");
            currentTokens += paragraphTokens;
        }

        // 處理最後一個 chunk
        if (currentChunk.length() > 0) {
            chunks.add(new ChunkResult(chunks.size(), currentChunk.toString().trim(), currentTokens));
        }

        return chunks;
    }

    private double cosineSimilarity(float[] a, float[] b) {
        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;
        for (int i = 0; i < a.length; i++) {
            dotProduct += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }
        return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
    }
}
```

#### 配置選項

```yaml
platform:
  chunking:
    # 切塊策略：FIXED（現有）或 SEMANTIC（語意）
    strategy: SEMANTIC
    semantic:
      # 語意相似度閾值
      similarity-threshold: 0.5
      # 最大 chunk token 數
      max-tokens: 500
```

#### 預期效益

- 減少 35% 無關上下文
- 提升檢索精準度 20-40%

---

### 2.2 導入 Reranking 層

#### 架構設計

```
搜尋查詢
    ↓
混合搜尋（現有）
    ↓ Top-50 候選
Reranker
    ↓ Top-10 結果
回傳用戶
```

#### 實作方案

**方案 A：使用 Spring AI DocumentPostProcessor（推薦）**

```java
// 新增檔案：infrastructure/reranker/CrossEncoderReranker.java
@Component
public class CrossEncoderReranker implements DocumentPostProcessor {

    // 使用 ONNX Runtime 執行輕量級 Cross-Encoder
    private final OrtEnvironment env;
    private final OrtSession session;

    public CrossEncoderReranker() {
        // 載入預訓練模型 ms-marco-MiniLM-L-6-v2
        this.env = OrtEnvironment.getEnvironment();
        this.session = env.createSession("models/ms-marco-MiniLM-L-6-v2.onnx");
    }

    @Override
    public List<Document> process(List<Document> documents, String query) {
        if (documents.size() <= 10) {
            return documents;
        }

        // 對每個文件計算 query-document 相關性分數
        List<ScoredDocument> scored = documents.stream()
            .map(doc -> new ScoredDocument(doc, computeRelevanceScore(query, doc.getText())))
            .sorted(Comparator.comparingDouble(ScoredDocument::score).reversed())
            .limit(10)
            .toList();

        return scored.stream().map(ScoredDocument::document).toList();
    }

    private float computeRelevanceScore(String query, String document) {
        // 使用 Cross-Encoder 計算相關性
        // 輸入：[CLS] query [SEP] document [SEP]
        // 輸出：相關性分數 (0-1)
    }
}
```

**方案 B：使用外部 Reranking API（Cohere / Jina）**

```java
@Service
public class ExternalRerankerService {

    @Value("${platform.reranker.api-key}")
    private String apiKey;

    private final RestClient restClient;

    public List<Document> rerank(String query, List<Document> documents) {
        // 呼叫 Cohere Rerank API
        var request = Map.of(
            "query", query,
            "documents", documents.stream().map(Document::getText).toList(),
            "top_n", 10
        );

        var response = restClient.post()
            .uri("https://api.cohere.ai/v1/rerank")
            .header("Authorization", "Bearer " + apiKey)
            .body(request)
            .retrieve()
            .body(RerankResponse.class);

        // 根據回傳的排序重新排列文件
        return response.results().stream()
            .map(r -> documents.get(r.index()))
            .toList();
    }
}
```

#### 整合至 SearchService

```java
// 修改 SearchService.java
@Service
public class SearchService {

    private final DocumentPostProcessor reranker;  // 新增

    public List<SearchResultItem> hybridSearch(...) {
        // 1. 執行混合搜尋，取得 Top-50
        int fetchLimit = limit * 5;  // 改為 5 倍
        List<SearchResultItem> candidates = hybridSearchInternal(..., fetchLimit);

        // 2. Reranking（新增）
        if (reranker != null && candidates.size() > limit) {
            List<Document> docs = candidates.stream()
                .map(this::toSpringAiDocument)
                .toList();
            List<Document> reranked = reranker.process(docs, query);
            candidates = reranked.stream()
                .map(this::toSearchResultItem)
                .limit(limit)
                .toList();
        }

        return candidates;
    }
}
```

#### 配置選項

```yaml
platform:
  search:
    reranker:
      # 啟用 Reranking
      enabled: true
      # 策略：LOCAL（本地模型）或 EXTERNAL（外部 API）
      strategy: LOCAL
      # 候選數量（送入 Reranker 的文件數）
      candidate-count: 50
      # 本地模型設定
      local:
        model-path: "models/ms-marco-MiniLM-L-6-v2.onnx"
      # 外部 API 設定
      external:
        provider: cohere  # cohere / jina
        api-key: ${RERANKER_API_KEY:}
```

#### 預期效益

- 檢索精準度提升 20-35%
- 減少 hallucination 機率
- 延遲增加約 2-10ms（本地模型）

---

## Phase 3：效能優化

### 3.1 Virtual Threads 最佳化

#### 檢查項目

需檢查程式碼中的 `synchronized` 區塊，避免 Thread Pinning：

```java
// ❌ 會造成 Pinning（阻塞 carrier thread）
synchronized (lock) {
    // blocking I/O
}

// ✅ 改用 ReentrantLock
private final ReentrantLock lock = new ReentrantLock();

public void doWork() {
    lock.lock();
    try {
        // blocking I/O
    } finally {
        lock.unlock();
    }
}
```

#### 檢查清單

| 檔案 | 檢查項目 | 狀態 |
|------|----------|------|
| `SyncService.java` | 無 synchronized | ✅ |
| `EmbeddingService.java` | 無 synchronized | ✅ |
| `SearchService.java` | 無 synchronized | ✅ |
| `DocumentChunkVectorStore.java` | 無 synchronized | ✅ |
| `GitHubContentFetcher.java` | 需檢查 | 待確認 |

#### 建議：新增 Virtual Threads 監控

```java
// 新增至 config/ObservabilityConfig.java
@Configuration
public class ObservabilityConfig {

    @Bean
    public MeterBinder virtualThreadMetrics() {
        return registry -> {
            // 監控 Virtual Thread 數量
            Gauge.builder("jvm.threads.virtual",
                Thread::activeCount)
                .description("Number of active virtual threads")
                .register(registry);
        };
    }
}
```

---

### 3.2 React 19 效能優化

#### 3.2.1 Lazy Loading

```jsx
// 修改 App.jsx
import { lazy, Suspense } from 'react';

// 將非首頁的頁面改為 lazy loading
const Dashboard = lazy(() => import('./pages/Dashboard'));
const Libraries = lazy(() => import('./pages/Libraries'));
const LibraryDetail = lazy(() => import('./pages/LibraryDetail'));
const DocumentList = lazy(() => import('./pages/DocumentList'));
const DocumentDetail = lazy(() => import('./pages/DocumentDetail'));
const Search = lazy(() => import('./pages/Search'));
const SyncHistory = lazy(() => import('./pages/SyncHistory'));
const SyncDetail = lazy(() => import('./pages/SyncDetail'));
const ApiKeys = lazy(() => import('./pages/ApiKeys'));
const Settings = lazy(() => import('./pages/Settings'));

// 在路由中使用 Suspense
<Suspense fallback={<LoadingSpinner />}>
  <Routes>
    {/* ... */}
  </Routes>
</Suspense>
```

#### 3.2.2 useTransition 標記非緊急更新

```jsx
// 修改 pages/Search.jsx
import { useState, useTransition } from 'react';

function Search() {
  const [query, setQuery] = useState('');
  const [results, setResults] = useState([]);
  const [isPending, startTransition] = useTransition();

  const handleSearch = (newQuery) => {
    setQuery(newQuery);  // 緊急更新：立即更新輸入框

    startTransition(() => {
      // 非緊急更新：搜尋結果可以延遲
      fetchSearchResults(newQuery).then(setResults);
    });
  };

  return (
    <div>
      <input value={query} onChange={e => handleSearch(e.target.value)} />
      {isPending && <LoadingIndicator />}
      <SearchResults results={results} />
    </div>
  );
}
```

#### 3.2.3 列表虛擬化

```jsx
// 安裝依賴
// npm install react-window

// 修改 pages/DocumentList.jsx
import { FixedSizeList } from 'react-window';

function DocumentList({ documents }) {
  const Row = ({ index, style }) => (
    <div style={style}>
      <DocumentCard document={documents[index]} />
    </div>
  );

  return (
    <FixedSizeList
      height={600}
      itemCount={documents.length}
      itemSize={80}
      width="100%"
    >
      {Row}
    </FixedSizeList>
  );
}
```

#### 3.2.4 Vite 配置優化

```js
// 修改 vite.config.js
export default defineConfig({
  build: {
    rollupOptions: {
      output: {
        manualChunks: {
          // 將大型依賴分離
          'react-vendor': ['react', 'react-dom', 'react-router-dom'],
          'ui-vendor': ['@radix-ui/react-dialog', '@radix-ui/react-dropdown-menu'],
        }
      }
    }
  }
});
```

---

## Phase 4：品質保證

### 4.1 RAG 評估機制

#### 評估指標

| 指標 | 說明 | 目標 |
|------|------|------|
| Relevance | 回答與問題的相關性 | > 0.8 |
| Faithfulness | 回答是否基於檢索的文件 | > 0.9 |
| Context Precision | 檢索文件的精準度 | > 0.7 |
| Context Recall | 檢索文件的召回率 | > 0.8 |

#### 實作方案

```java
// 新增檔案：service/RagEvaluationService.java
@Service
public class RagEvaluationService {

    private final ChatModel chatModel;

    /**
     * 評估搜尋結果的相關性
     */
    public EvaluationResult evaluateRelevance(String query, List<SearchResultItem> results) {
        String prompt = """
            評估以下搜尋結果與查詢的相關性（0-1 分）：

            查詢：%s

            搜尋結果：
            %s

            請以 JSON 格式回傳：{"score": 0.85, "reasoning": "..."}
            """.formatted(query, formatResults(results));

        String response = chatModel.call(prompt);
        return parseEvaluationResult(response);
    }

    /**
     * 檢測可能的 Hallucination
     */
    public HallucinationCheckResult checkHallucination(
            String query,
            String answer,
            List<SearchResultItem> sources) {

        String prompt = """
            檢查以下回答是否包含幻覺（未在來源文件中提及的資訊）：

            問題：%s
            回答：%s
            來源文件：%s

            請以 JSON 格式回傳：
            {"hasHallucination": false, "details": [...]}
            """.formatted(query, answer, formatSources(sources));

        String response = chatModel.call(prompt);
        return parseHallucinationResult(response);
    }
}
```

#### 評估儀表板 API

```java
// 新增檔案：web/api/EvaluationController.java
@RestController
@RequestMapping("/api/v1/evaluation")
public class EvaluationController {

    @GetMapping("/metrics")
    public EvaluationMetrics getMetrics(
            @RequestParam(defaultValue = "7d") String period) {
        // 回傳過去 N 天的評估指標
    }

    @PostMapping("/run")
    public EvaluationResult runEvaluation(
            @RequestBody EvaluationRequest request) {
        // 執行單次評估
    }
}
```

---

## 實作時程建議

### Phase 1（1-2 週）

| 任務 | 估計時間 | 優先級 |
|------|----------|--------|
| pgvector HNSW 索引調優 | 2 天 | 高 |
| 索引參數測試與調整 | 2 天 | 高 |
| Gemini Batch API 評估 | 1 天 | 低 |

### Phase 2（2-3 週）

| 任務 | 估計時間 | 優先級 |
|------|----------|--------|
| 語意切塊實作 | 5 天 | 高 |
| Reranker 整合（本地模型） | 5 天 | 高 |
| 搜尋品質測試 | 3 天 | 高 |

### Phase 3（1 週）

| 任務 | 估計時間 | 優先級 |
|------|----------|--------|
| Virtual Threads 檢查 | 1 天 | 中 |
| React Lazy Loading | 2 天 | 中 |
| 效能測試與優化 | 2 天 | 中 |

### Phase 4（持續）

| 任務 | 估計時間 | 優先級 |
|------|----------|--------|
| RAG 評估機制基礎架構 | 3 天 | 低 |
| 評估儀表板 | 2 天 | 低 |
| 持續監控與調整 | 持續 | 低 |

---

## 參考資料

### pgvector
- [HNSW Indexes with pgvector](https://www.crunchydata.com/blog/hnsw-indexes-with-postgres-and-pgvector)
- [pgvector Performance Tips](https://www.crunchydata.com/blog/pgvector-performance-for-developers)

### Reranking
- [Advanced RAG: Hybrid Search and Re-ranking](https://dev.to/kuldeep_paul/advanced-rag-from-naive-retrieval-to-hybrid-search-and-re-ranking-4km3)
- [Cross-Encoder Reranking](https://medium.com/@rossashman/the-art-of-rag-part-3-reranking-with-cross-encoders-688a16b64669)

### Chunking
- [Semantic Chunking for RAG](https://www.multimodal.dev/post/semantic-chunking-for-rag)
- [Chunking Strategies for RAG](https://weaviate.io/blog/chunking-strategies-for-rag)

### Spring AI
- [Spring AI RAG Reference](https://docs.spring.io/spring-ai/reference/api/retrieval-augmented-generation.html)
- [Advanced RAG with Spring AI](https://vaadin.com/blog/advanced-rag-techniques-with-spring-ai)

### Gemini
- [Gemini Batch API](https://ai.google.dev/gemini-api/docs/batch-api)
