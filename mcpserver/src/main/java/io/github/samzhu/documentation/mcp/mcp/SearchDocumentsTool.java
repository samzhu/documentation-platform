package io.github.samzhu.documentation.mcp.mcp;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.samzhu.documentation.mcp.config.SearchProperties;
import io.github.samzhu.documentation.mcp.service.SearchService;
import io.github.samzhu.documentation.mcp.service.dto.SearchResultItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springaicommunity.mcp.annotation.McpTool;
import org.springaicommunity.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * MCP Tool：搜尋文件
 * <p>
 * 支援全文、語意、混合三種搜尋模式。
 * </p>
 */
@Component
public class SearchDocumentsTool {

    private static final Logger log = LoggerFactory.getLogger(SearchDocumentsTool.class);

    private final SearchService searchService;
    private final SearchProperties searchProperties;
    private final ObjectMapper objectMapper;

    public SearchDocumentsTool(SearchService searchService,
                               SearchProperties searchProperties,
                               ObjectMapper objectMapper) {
        this.searchService = searchService;
        this.searchProperties = searchProperties;
        this.objectMapper = objectMapper;
    }

    /**
     * 搜尋技術文件
     */
    @McpTool(name = "search_documents",
             description = "搜尋技術文件，支援全文、語意、混合三種模式。" +
                     "預設使用混合模式（結合關鍵字與語意搜尋）。" +
                     "可限定特定文件庫和版本。")
    public String searchDocuments(
            @McpToolParam(description = "搜尋關鍵字或語意描述", required = true) String query,
            @McpToolParam(description = "限定文件庫名稱（如 spring-boot）") String libraryName,
            @McpToolParam(description = "限定版本號（如 3.2.0）") String version,
            @McpToolParam(description = "搜尋模式：hybrid（預設）/fulltext/semantic") String mode,
            @McpToolParam(description = "回傳筆數上限，預設10，最大20") Integer limit) {

        try {
            int effectiveLimit = (limit != null && limit > 0)
                    ? limit : searchProperties.defaultLimit();

            List<SearchResultItem> results = searchService.search(
                    libraryName, version, query, mode, effectiveLimit);

            return objectMapper.writeValueAsString(results);
        } catch (IllegalArgumentException e) {
            log.warn("搜尋參數錯誤: {}", e.getMessage());
            return """
                    {"error": "%s"}""".formatted(e.getMessage());
        } catch (Exception e) {
            log.error("搜尋文件時發生錯誤", e);
            return """
                    {"error": "搜尋失敗: %s"}""".formatted(e.getMessage());
        }
    }
}
