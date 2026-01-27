package io.github.samzhu.documentation.platform.web.api;

import io.github.samzhu.documentation.platform.domain.model.Document;
import io.github.samzhu.documentation.platform.service.DocumentService;
import io.github.samzhu.documentation.platform.web.dto.DocumentDto;
import io.github.samzhu.documentation.platform.web.dto.DocumentDetailDto;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 文件 REST API
 * <p>
 * 提供文件列表和詳情查詢功能。
 * </p>
 */
@RestController
@RequestMapping("/api/documents")
public class DocumentApiController {

    private final DocumentService documentService;

    /**
     * 建構函式
     *
     * @param documentService 文件服務
     */
    public DocumentApiController(DocumentService documentService) {
        this.documentService = documentService;
    }

    /**
     * 取得文件列表
     * <p>
     * 必須提供 versionId 參數來查詢特定版本的文件。
     * </p>
     *
     * @param versionId 版本 ID（TSID 格式，必填）
     * @return 文件列表
     */
    @GetMapping
    public ResponseEntity<List<DocumentDto>> listDocuments(
            @RequestParam(required = true) String versionId) {

        List<Document> documents = documentService.getDocumentsByVersionId(versionId);

        List<DocumentDto> dtos = documents.stream()
                .map(DocumentDto::from)
                .toList();

        return ResponseEntity.ok(dtos);
    }

    /**
     * 取得單一文件詳情
     * <p>
     * 包含文件內容、區塊和程式碼範例。
     * </p>
     *
     * @param id 文件 ID（TSID 格式）
     * @return 文件詳情
     */
    @GetMapping("/{id}")
    public ResponseEntity<DocumentDetailDto> getDocument(@PathVariable String id) {
        DocumentService.DocumentContent content = documentService.getDocumentContent(id);

        return ResponseEntity.ok(DocumentDetailDto.from(content));
    }
}
