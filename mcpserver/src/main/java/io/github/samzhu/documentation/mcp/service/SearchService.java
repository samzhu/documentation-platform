package io.github.samzhu.documentation.mcp.service;

import io.github.samzhu.documentation.mcp.config.SearchProperties;
import io.github.samzhu.documentation.mcp.domain.model.Library;
import io.github.samzhu.documentation.mcp.repository.DocumentRepository;
import io.github.samzhu.documentation.mcp.repository.LibraryRepository;
import io.github.samzhu.documentation.mcp.repository.LibraryVersionRepository;
import io.github.samzhu.documentation.mcp.service.dto.SearchResultItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static io.github.samzhu.documentation.mcp.infrastructure.vectorstore.DocumentChunkVectorStore.*;

/**
 * 搜尋服務
 * <p>
 * 提供全文檢索、語意搜尋和混合搜尋功能。
 * 全文檢索使用 PostgreSQL 的 tsvector/tsquery。
 * 語意搜尋使用 pgvector 的向量相似度計算。
 * 混合搜尋使用 RRF（Reciprocal Rank Fusion）演算法融合兩種搜尋結果。
 * </p>
 */
@Service
@Transactional(readOnly = true)
public class SearchService {

    private static final Logger log = LoggerFactory.getLogger(SearchService.class);

    /** RRF 常數 K，防止排名第一的結果權重過大 */
    private static final int RRF_K = 60;

    private final DocumentRepository documentRepository;
    private final LibraryRepository libraryRepository;
    private final LibraryVersionRepository versionRepository;
    private final VectorStore vectorStore;
    private final SearchProperties searchProperties;

    public SearchService(DocumentRepository documentRepository,
                         LibraryRepository libraryRepository,
                         LibraryVersionRepository versionRepository,
                         VectorStore vectorStore,
                         SearchProperties searchProperties) {
        this.documentRepository = documentRepository;
        this.libraryRepository = libraryRepository;
        this.versionRepository = versionRepository;
        this.vectorStore = vectorStore;
        this.searchProperties = searchProperties;
    }

    /**
     * 全文檢索
     *
     * @param versionId 版本 ID
     * @param query     搜尋關鍵字
     * @param limit     結果數量上限
     * @return 搜尋結果列表
     */
    public List<SearchResultItem> fullTextSearch(String versionId, String query, int limit) {
        if (query == null || query.isBlank()) {
            return List.of();
        }

        var documents = documentRepository.fullTextSearch(versionId, query, limit);
        return documents.stream()
                .map(doc -> SearchResultItem.fromDocument(
                        doc.getId(), doc.getTitle(), doc.getPath(),
                        truncateContent(doc.getContent(), 500),
                        1.0  // 全文檢索不回傳分數，使用預設值
                ))
                .toList();
    }

    /**
     * 語意搜尋
     *
     * @param versionId 版本 ID
     * @param query     自然語言查詢
     * @param limit     結果數量上限
     * @param threshold 相似度閾值
     * @return 搜尋結果列表
     */
    public List<SearchResultItem> semanticSearch(String versionId, String query,
                                                  int limit, double threshold) {
        if (query == null || query.isBlank()) {
            return List.of();
        }

        // 使用 VectorStore 執行語意搜尋，透過 filterExpression 限制搜尋範圍
        SearchRequest request = SearchRequest.builder()
                .query(query)
                .topK(limit)
                .similarityThreshold(threshold)
                .filterExpression(METADATA_VERSION_ID + " == '" + versionId + "'")
                .build();

        List<org.springframework.ai.document.Document> results = vectorStore.similaritySearch(request);

        if (results.isEmpty()) {
            return List.of();
        }

        // 從結果中取得 document IDs 並批次查詢文件資訊
        List<String> documentIds = results.stream()
                .map(doc -> {
                    Object docIdObj = doc.getMetadata().get(METADATA_DOCUMENT_ID);
                    return docIdObj != null ? docIdObj.toString() : null;
                })
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        Map<String, io.github.samzhu.documentation.mcp.domain.model.Document> documentMap =
                StreamSupport.stream(documentRepository.findAllById(documentIds).spliterator(), false)
                        .collect(Collectors.toMap(
                                io.github.samzhu.documentation.mcp.domain.model.Document::getId,
                                Function.identity()));

        // 轉換為搜尋結果
        return results.stream()
                .map(doc -> toSearchResultItem(doc, documentMap))
                .filter(Objects::nonNull)
                .toList();
    }

    /**
     * 混合搜尋（使用 RRF 演算法融合全文搜尋與語意搜尋結果）
     *
     * @param versionId 版本 ID
     * @param query     搜尋查詢
     * @param limit     結果數量上限
     * @return 融合後的搜尋結果列表（依 RRF 分數排序）
     */
    public List<SearchResultItem> hybridSearch(String versionId, String query, int limit) {
        if (query == null || query.isBlank()) {
            return List.of();
        }

        double alpha = searchProperties.hybrid().alpha();
        double minSimilarity = searchProperties.hybrid().minSimilarity();

        log.debug("執行混合搜尋: query='{}', versionId={}, alpha={}", query, versionId, alpha);

        // 取得更多結果以確保融合後有足夠的資料
        int fetchLimit = limit * 2;

        List<SearchResultItem> keywordResults = fullTextSearch(versionId, query, fetchLimit);
        List<SearchResultItem> semanticResults = semanticSearch(versionId, query, fetchLimit, minSimilarity);

        log.debug("關鍵字搜尋結果: {} 筆, 語意搜尋結果: {} 筆",
                keywordResults.size(), semanticResults.size());

        // 如果任一搜尋無結果，直接返回另一種搜尋的結果
        if (keywordResults.isEmpty() && semanticResults.isEmpty()) {
            return List.of();
        }
        if (keywordResults.isEmpty()) {
            return semanticResults.stream().limit(limit).toList();
        }
        if (semanticResults.isEmpty()) {
            return keywordResults.stream().limit(limit).toList();
        }

        // 計算 RRF 分數並融合
        Map<String, Double> rrfScores = calculateRRFScores(keywordResults, semanticResults, alpha);

        // 建立結果 ID 到 SearchResultItem 的對應（優先使用語意搜尋結果）
        Map<String, SearchResultItem> resultMap = new LinkedHashMap<>();
        for (SearchResultItem item : semanticResults) {
            resultMap.put(getResultKey(item), item);
        }
        for (SearchResultItem item : keywordResults) {
            resultMap.putIfAbsent(getResultKey(item), item);
        }

        // 依 RRF 分數排序並返回
        return rrfScores.entrySet().stream()
                .filter(entry -> resultMap.containsKey(entry.getKey()))
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .limit(limit)
                .map(entry -> {
                    SearchResultItem original = resultMap.get(entry.getKey());
                    double normalizedScore = normalizeRRFScore(entry.getValue());
                    return original.withScore(normalizedScore);
                })
                .toList();
    }

    /**
     * 搜尋文件（統一入口，支援跨 Library 搜尋）
     *
     * @param libraryName 函式庫名稱（null 表示搜尋所有函式庫的最新版本）
     * @param version     版本號（null 表示最新版本）
     * @param query       搜尋查詢
     * @param mode        搜尋模式（hybrid/fulltext/semantic）
     * @param limit       結果數量上限
     * @return 搜尋結果列表
     */
    public List<SearchResultItem> search(String libraryName, String version,
                                          String query, String mode, int limit) {
        if (query == null || query.isBlank()) {
            return List.of();
        }

        // 限制回傳筆數
        int effectiveLimit = Math.min(limit, searchProperties.maxLimit());
        String effectiveMode = (mode != null && !mode.isBlank()) ? mode : "hybrid";

        // 指定了函式庫 → 搜尋單一函式庫
        if (libraryName != null && !libraryName.isBlank()) {
            return searchInLibrary(libraryName, version, query, effectiveMode, effectiveLimit);
        }

        // 未指定函式庫 → 搜尋所有函式庫的 latest version
        return searchAcrossLibraries(query, effectiveMode, effectiveLimit);
    }

    // ========== 私有輔助方法 ==========

    /**
     * 搜尋單一函式庫
     */
    private List<SearchResultItem> searchInLibrary(String libraryName, String version,
                                                    String query, String mode, int limit) {
        Library library = libraryRepository.findByName(libraryName)
                .orElseThrow(() -> new IllegalArgumentException("找不到函式庫: " + libraryName));

        String versionId = resolveVersionId(library.getId(), version);

        return switch (mode) {
            case "fulltext" -> fullTextSearch(versionId, query, limit);
            case "semantic" -> semanticSearch(versionId, query, limit,
                    searchProperties.hybrid().minSimilarity());
            default -> hybridSearch(versionId, query, limit);
        };
    }

    /**
     * 搜尋所有函式庫的最新版本
     */
    private List<SearchResultItem> searchAcrossLibraries(String query, String mode, int limit) {
        List<Library> libraries = libraryRepository.findAll();
        List<SearchResultItem> allResults = new ArrayList<>();

        for (Library library : libraries) {
            try {
                var latestVersion = versionRepository.findLatestByLibraryId(library.getId());
                if (latestVersion.isEmpty()) continue;

                String versionId = latestVersion.get().getId();
                List<SearchResultItem> results = switch (mode) {
                    case "fulltext" -> fullTextSearch(versionId, query, limit);
                    case "semantic" -> semanticSearch(versionId, query, limit,
                            searchProperties.hybrid().minSimilarity());
                    default -> hybridSearch(versionId, query, limit);
                };
                allResults.addAll(results);
            } catch (Exception e) {
                log.warn("搜尋函式庫 {} 時發生錯誤: {}", library.getName(), e.getMessage());
            }
        }

        // 依分數排序並限制結果數量
        return allResults.stream()
                .sorted(Comparator.comparingDouble(SearchResultItem::score).reversed())
                .limit(limit)
                .toList();
    }

    /**
     * 解析版本 ID
     */
    private String resolveVersionId(String libraryId, String version) {
        if (version != null && !version.isBlank()) {
            return versionRepository.findByLibraryIdAndVersion(libraryId, version)
                    .map(v -> v.getId())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "找不到版本: libraryId=%s, version=%s".formatted(libraryId, version)));
        } else {
            return versionRepository.findLatestByLibraryId(libraryId)
                    .map(v -> v.getId())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "找不到最新版本: libraryId=%s".formatted(libraryId)));
        }
    }

    /**
     * 將 Spring AI Document 轉換為 SearchResultItem
     */
    private SearchResultItem toSearchResultItem(
            org.springframework.ai.document.Document doc,
            Map<String, io.github.samzhu.documentation.mcp.domain.model.Document> documentMap) {

        Map<String, Object> metadata = doc.getMetadata();
        String documentId = getMetadataString(metadata, METADATA_DOCUMENT_ID);
        if (documentId == null) return null;

        var dbDoc = documentMap.get(documentId);
        if (dbDoc == null) return null;

        String chunkId = doc.getId();
        int chunkIndex = getMetadataInt(metadata, METADATA_CHUNK_INDEX, 0);
        double score = doc.getScore() != null ? doc.getScore() : 0.0;

        return SearchResultItem.fromChunk(
                dbDoc.getId(), chunkId, dbDoc.getTitle(), dbDoc.getPath(),
                doc.getText(), score, chunkIndex
        );
    }

    /**
     * 計算 RRF 分數
     */
    private Map<String, Double> calculateRRFScores(List<SearchResultItem> keywordResults,
                                                    List<SearchResultItem> semanticResults,
                                                    double alpha) {
        Map<String, Double> scores = new HashMap<>();

        for (int i = 0; i < keywordResults.size(); i++) {
            String key = getResultKey(keywordResults.get(i));
            double rrfScore = alpha * (1.0 / (RRF_K + i + 1));
            scores.merge(key, rrfScore, Double::sum);
        }

        for (int i = 0; i < semanticResults.size(); i++) {
            String key = getResultKey(semanticResults.get(i));
            double rrfScore = (1 - alpha) * (1.0 / (RRF_K + i + 1));
            scores.merge(key, rrfScore, Double::sum);
        }

        return scores;
    }

    /**
     * 取得搜尋結果的唯一識別鍵
     */
    private String getResultKey(SearchResultItem item) {
        if (item.chunkId() != null) {
            return "chunk:" + item.chunkId();
        }
        return "doc:" + item.documentId();
    }

    /**
     * 將 RRF 分數正規化為 0-1 範圍
     */
    private double normalizeRRFScore(double rrfScore) {
        double maxPossibleScore = 2.0 / (RRF_K + 1);
        return Math.min(1.0, rrfScore / maxPossibleScore);
    }

    /**
     * 截斷內容
     */
    private String truncateContent(String content, int maxLength) {
        if (content == null) return "";
        if (content.length() <= maxLength) return content;
        return content.substring(0, maxLength) + "...";
    }

    private String getMetadataString(Map<String, Object> metadata, String key) {
        if (metadata == null || !metadata.containsKey(key)) return null;
        Object value = metadata.get(key);
        return value != null ? value.toString() : null;
    }

    private int getMetadataInt(Map<String, Object> metadata, String key, int defaultValue) {
        if (metadata == null || !metadata.containsKey(key)) return defaultValue;
        Object value = metadata.get(key);
        if (value instanceof Number n) return n.intValue();
        try { return Integer.parseInt(value.toString()); }
        catch (NumberFormatException e) { return defaultValue; }
    }
}
