package io.github.samzhu.documentation.mcp.mcp;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.samzhu.documentation.mcp.domain.model.Document;
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
 * MCP Tool：列出文件路徑
 */
@Component
public class ListDocumentsTool {

    private static final Logger log = LoggerFactory.getLogger(ListDocumentsTool.class);

    private final LibraryQueryService libraryQueryService;
    private final ObjectMapper objectMapper;

    public ListDocumentsTool(LibraryQueryService libraryQueryService,
                             ObjectMapper objectMapper) {
        this.libraryQueryService = libraryQueryService;
        this.objectMapper = objectMapper;
    }

    /**
     * 列出指定版本下的所有文件路徑
     */
    @McpTool(name = "list_documents",
             description = "列出指定版本下的所有文件路徑，回傳輕量結果（不含文件內容）。")
    public String listDocuments(
            @McpToolParam(description = "文件庫名稱", required = true) String libraryName,
            @McpToolParam(description = "版本號", required = true) String version) {

        try {
            List<Document> documents = libraryQueryService.findDocuments(libraryName, version);

            // 回傳輕量結果（排除 content）
            List<Map<String, Object>> result = documents.stream()
                    .map(doc -> {
                        Map<String, Object> map = new LinkedHashMap<>();
                        map.put("title", doc.getTitle());
                        map.put("path", doc.getPath());
                        map.put("docType", doc.getDocType());
                        return map;
                    })
                    .toList();

            return objectMapper.writeValueAsString(result);
        } catch (IllegalArgumentException e) {
            log.warn("列出文件參數錯誤: {}", e.getMessage());
            return """
                    {"error": "%s"}""".formatted(e.getMessage());
        } catch (Exception e) {
            log.error("列出文件時發生錯誤", e);
            return """
                    {"error": "列出文件失敗: %s"}""".formatted(e.getMessage());
        }
    }
}
