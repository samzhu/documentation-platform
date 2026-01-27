package io.github.samzhu.documentation.platform.web.api;

import io.github.samzhu.documentation.platform.service.LibraryService;
import io.github.samzhu.documentation.platform.service.SearchService;
import io.github.samzhu.documentation.platform.service.dto.SearchResultItem;
import io.github.samzhu.documentation.platform.web.dto.SearchResultDto;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 搜尋 REST API
 * <p>
 * 提供文件搜尋功能，支援全文搜尋、語意搜尋和混合搜尋。
 * 語意搜尋使用向量嵌入進行相似度比對。
 * </p>
 */
@RestController
@RequestMapping("/api/search")
public class SearchApiController {

    /**
     * 語意搜尋預設相似度閾值
     * 低於此閾值的結果會被過濾
     */
    private static final double DEFAULT_SEMANTIC_THRESHOLD = 0.5;

    private final SearchService searchService;
    private final LibraryService libraryService;

    /**
     * 建構函式
     *
     * @param searchService  搜尋服務
     * @param libraryService 函式庫服務
     */
    public SearchApiController(SearchService searchService, LibraryService libraryService) {
        this.searchService = searchService;
        this.libraryService = libraryService;
    }

    /**
     * 搜尋文件
     * <p>
     * 支援三種搜尋模式：
     * <ul>
     *   <li>fulltext - 全文搜尋，使用 PostgreSQL 全文索引</li>
     *   <li>semantic - 語意搜尋，使用向量嵌入相似度比對</li>
     *   <li>hybrid - 混合搜尋（預設），結合全文和語意搜尋的結果</li>
     * </ul>
     * </p>
     *
     * @param query     搜尋查詢字串
     * @param libraryId 函式庫 ID（可選，TSID 格式）
     * @param version   版本（可選）
     * @param mode      搜尋模式：fulltext、semantic、hybrid（預設）
     * @param limit     結果數量上限（預設 10）
     * @return 搜尋結果
     */
    @GetMapping
    public SearchResultDto search(
            @RequestParam("q") String query,
            @RequestParam(required = false) String libraryId,
            @RequestParam(required = false) String version,
            @RequestParam(defaultValue = "hybrid") String mode,
            @RequestParam(defaultValue = "10") int limit
    ) {
        // 檢查查詢是否為空
        if (query == null || query.isBlank()) {
            return new SearchResultDto(query, mode, 0, List.of());
        }

        // 如果沒有指定 libraryId，取得第一個函式庫作為預設
        // libraryId 為 TSID 格式（13 字元）
        if (libraryId == null || libraryId.isBlank()) {
            var libraries = libraryService.listLibraries(null);
            if (libraries.isEmpty()) {
                return new SearchResultDto(query, mode, 0, List.of());
            }
            libraryId = libraries.getFirst().getId();
        }

        // 依搜尋模式執行對應的搜尋方法
        List<SearchResultItem> results = switch (mode.toLowerCase()) {
            case "fulltext" -> searchService.fullTextSearch(libraryId, version, query, limit);
            case "semantic" -> searchService.semanticSearch(libraryId, version, query, limit, DEFAULT_SEMANTIC_THRESHOLD);
            case "hybrid" -> searchService.hybridSearch(libraryId, version, query, limit);
            default -> throw new IllegalArgumentException("不支援的搜尋模式: " + mode);
        };

        return SearchResultDto.from(query, mode, results);
    }
}
