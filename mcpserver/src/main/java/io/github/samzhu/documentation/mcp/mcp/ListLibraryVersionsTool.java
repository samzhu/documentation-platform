package io.github.samzhu.documentation.mcp.mcp;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.samzhu.documentation.mcp.domain.model.LibraryVersion;
import io.github.samzhu.documentation.mcp.service.LibraryQueryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springaicommunity.mcp.annotation.McpTool;
import org.springaicommunity.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * MCP Tool：列出文件庫版本
 */
@Component
public class ListLibraryVersionsTool {

    private static final Logger log = LoggerFactory.getLogger(ListLibraryVersionsTool.class);

    private final LibraryQueryService libraryQueryService;
    private final ObjectMapper objectMapper;

    public ListLibraryVersionsTool(LibraryQueryService libraryQueryService,
                                   ObjectMapper objectMapper) {
        this.libraryQueryService = libraryQueryService;
        this.objectMapper = objectMapper;
    }

    /**
     * 列出指定文件庫的所有版本
     */
    @McpTool(name = "list_library_versions",
             description = "列出指定文件庫的所有版本，包含版本號、狀態、是否為最新版等資訊。")
    public String listLibraryVersions(
            @McpToolParam(description = "文件庫名稱", required = true) String libraryName) {

        try {
            List<LibraryVersion> versions = libraryQueryService.findVersionsByLibraryName(libraryName);

            List<Map<String, Object>> result = versions.stream()
                    .map(v -> {
                        Map<String, Object> map = new LinkedHashMap<>();
                        map.put("version", v.getVersion());
                        map.put("isLatest", v.getIsLatest());
                        map.put("isLts", v.getIsLts());
                        map.put("status", v.getStatus());
                        map.put("releaseDate", v.getReleaseDate());
                        return map;
                    })
                    .toList();

            return objectMapper.writeValueAsString(result);
        } catch (IllegalArgumentException e) {
            log.warn("查詢版本參數錯誤: {}", e.getMessage());
            return """
                    {"error": "%s"}""".formatted(e.getMessage());
        } catch (Exception e) {
            log.error("列出版本時發生錯誤", e);
            return """
                    {"error": "列出版本失敗: %s"}""".formatted(e.getMessage());
        }
    }
}
