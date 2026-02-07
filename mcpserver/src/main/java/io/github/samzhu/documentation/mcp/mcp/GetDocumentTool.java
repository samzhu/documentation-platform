package io.github.samzhu.documentation.mcp.mcp;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.samzhu.documentation.mcp.domain.model.CodeExample;
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
 * MCP Tool：取得文件內容
 */
@Component
public class GetDocumentTool {

    private static final Logger log = LoggerFactory.getLogger(GetDocumentTool.class);

    private final LibraryQueryService libraryQueryService;
    private final ObjectMapper objectMapper;

    public GetDocumentTool(LibraryQueryService libraryQueryService,
                           ObjectMapper objectMapper) {
        this.libraryQueryService = libraryQueryService;
        this.objectMapper = objectMapper;
    }

    /**
     * 取得指定文件的完整內容
     */
    @McpTool(name = "get_document",
             description = "取得指定文件的完整內容，包含標題、內容、文件類型、元資料和程式碼範例。")
    public String getDocument(
            @McpToolParam(description = "文件庫名稱", required = true) String libraryName,
            @McpToolParam(description = "版本號", required = true) String version,
            @McpToolParam(description = "文件路徑", required = true) String path) {

        try {
            Document doc = libraryQueryService.findDocument(libraryName, version, path);
            List<CodeExample> codeExamples = libraryQueryService.findCodeExamples(doc.getId());

            // 組裝回傳結果
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("title", doc.getTitle());
            result.put("path", doc.getPath());
            result.put("content", doc.getContent());
            result.put("docType", doc.getDocType());
            result.put("metadata", doc.getMetadata());

            // 附加程式碼範例
            List<Map<String, Object>> examples = codeExamples.stream()
                    .map(ex -> {
                        Map<String, Object> exMap = new LinkedHashMap<>();
                        exMap.put("language", ex.getLanguage());
                        exMap.put("code", ex.getCode());
                        exMap.put("description", ex.getDescription());
                        exMap.put("startLine", ex.getStartLine());
                        exMap.put("endLine", ex.getEndLine());
                        return exMap;
                    })
                    .toList();
            result.put("codeExamples", examples);

            return objectMapper.writeValueAsString(result);
        } catch (IllegalArgumentException e) {
            log.warn("取得文件參數錯誤: {}", e.getMessage());
            return """
                    {"error": "%s"}""".formatted(e.getMessage());
        } catch (Exception e) {
            log.error("取得文件時發生錯誤", e);
            return """
                    {"error": "取得文件失敗: %s"}""".formatted(e.getMessage());
        }
    }
}
