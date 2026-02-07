package io.github.samzhu.documentation.mcp.mcp;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.samzhu.documentation.mcp.domain.model.Document;
import io.github.samzhu.documentation.mcp.domain.model.Library;
import io.github.samzhu.documentation.mcp.service.LibraryQueryService;
import io.modelcontextprotocol.spec.McpSchema.ReadResourceResult;
import io.modelcontextprotocol.spec.McpSchema.TextResourceContents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springaicommunity.mcp.annotation.McpResource;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * MCP Resources：文件資源
 * <p>
 * 提供透過 URI 直接存取文件內容與文件庫後設資料的 Resource 介面。
 * </p>
 */
@Component
public class DocumentResources {

    private static final Logger log = LoggerFactory.getLogger(DocumentResources.class);

    private final LibraryQueryService libraryQueryService;
    private final ObjectMapper objectMapper;

    public DocumentResources(LibraryQueryService libraryQueryService,
                             ObjectMapper objectMapper) {
        this.libraryQueryService = libraryQueryService;
        this.objectMapper = objectMapper;
    }

    /**
     * 取得文件內容
     * <p>
     * URI 格式：docs://{libraryName}/{version}/{path}
     * </p>
     */
    @McpResource(
            uri = "docs://{libraryName}/{version}/{path}",
            name = "Documentation Content",
            description = "取得指定文件庫版本的文件內容，支援 markdown 和 html 格式",
            mimeType = "text/plain"
    )
    public ReadResourceResult getDocumentContent(String libraryName, String version, String path) {
        try {
            Document doc = libraryQueryService.findDocument(libraryName, version, path);
            String mimeType = resolveMimeType(doc.getDocType());
            String uri = "docs://%s/%s/%s".formatted(libraryName, version, path);

            return new ReadResourceResult(List.of(
                    new TextResourceContents(uri, mimeType, doc.getContent())
            ));
        } catch (IllegalArgumentException e) {
            log.warn("取得文件資源失敗: {}", e.getMessage());
            String uri = "docs://%s/%s/%s".formatted(libraryName, version, path);
            return new ReadResourceResult(List.of(
                    new TextResourceContents(uri, "text/plain", "錯誤: " + e.getMessage())
            ));
        }
    }

    /**
     * 取得文件庫後設資料
     * <p>
     * URI 格式：library://{libraryName}
     * </p>
     */
    @McpResource(
            uri = "library://{libraryName}",
            name = "Library Metadata",
            description = "取得指定文件庫的後設資料（名稱、描述、分類、標籤等）",
            mimeType = "application/json"
    )
    public ReadResourceResult getLibraryMetadata(String libraryName) {
        try {
            Library library = libraryQueryService.findLibraryByName(libraryName);
            long docCount = libraryQueryService.countDocuments(library.getId());

            Map<String, Object> metadata = new LinkedHashMap<>();
            metadata.put("name", library.getName());
            metadata.put("displayName", library.getDisplayName());
            metadata.put("description", library.getDescription());
            metadata.put("sourceType", library.getSourceType());
            metadata.put("sourceUrl", library.getSourceUrl());
            metadata.put("category", library.getCategory());
            metadata.put("tags", library.getTags());
            metadata.put("documentCount", docCount);

            String json = objectMapper.writeValueAsString(metadata);
            String uri = "library://%s".formatted(libraryName);

            return new ReadResourceResult(List.of(
                    new TextResourceContents(uri, "application/json", json)
            ));
        } catch (Exception e) {
            log.warn("取得文件庫後設資料失敗: {}", e.getMessage());
            String uri = "library://%s".formatted(libraryName);
            return new ReadResourceResult(List.of(
                    new TextResourceContents(uri, "text/plain", "錯誤: " + e.getMessage())
            ));
        }
    }

    /**
     * 依文件類型決定 MIME type
     */
    private String resolveMimeType(String docType) {
        if (docType == null) return "text/plain";
        return switch (docType.toLowerCase()) {
            case "markdown", "md" -> "text/markdown";
            case "html" -> "text/html";
            default -> "text/plain";
        };
    }
}
