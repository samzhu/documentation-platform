package io.github.samzhu.documentation.mcp.mcp;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.samzhu.documentation.mcp.domain.model.Library;
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
 * MCP Tool：列出所有文件庫
 */
@Component
public class ListLibrariesTool {

    private static final Logger log = LoggerFactory.getLogger(ListLibrariesTool.class);

    private final LibraryQueryService libraryQueryService;
    private final ObjectMapper objectMapper;

    public ListLibrariesTool(LibraryQueryService libraryQueryService,
                             ObjectMapper objectMapper) {
        this.libraryQueryService = libraryQueryService;
        this.objectMapper = objectMapper;
    }

    /**
     * 列出所有可用的文件庫及其基本資訊
     */
    @McpTool(name = "list_libraries",
             description = "列出所有可用的文件庫及其基本資訊，包含名稱、描述、分類、版本數和文件數。")
    public String listLibraries(
            @McpToolParam(description = "篩選類別（如 backend、frontend）") String category) {

        try {
            List<Library> libraries = libraryQueryService.findAllLibraries(category);

            List<Map<String, Object>> result = libraries.stream()
                    .map(lib -> {
                        Map<String, Object> map = new LinkedHashMap<>();
                        map.put("name", lib.getName());
                        map.put("displayName", lib.getDisplayName());
                        map.put("description", lib.getDescription());
                        map.put("category", lib.getCategory());
                        map.put("tags", lib.getTags());
                        map.put("documentCount", libraryQueryService.countDocuments(lib.getId()));
                        return map;
                    })
                    .toList();

            return objectMapper.writeValueAsString(result);
        } catch (Exception e) {
            log.error("列出文件庫時發生錯誤", e);
            return """
                    {"error": "列出文件庫失敗: %s"}""".formatted(e.getMessage());
        }
    }
}
