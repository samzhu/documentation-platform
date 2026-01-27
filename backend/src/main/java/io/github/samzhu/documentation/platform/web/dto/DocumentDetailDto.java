package io.github.samzhu.documentation.platform.web.dto;

import io.github.samzhu.documentation.platform.domain.model.CodeExample;
import io.github.samzhu.documentation.platform.domain.model.Document;
import io.github.samzhu.documentation.platform.domain.model.DocumentChunk;
import io.github.samzhu.documentation.platform.service.DocumentService;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

/**
 * 文件詳情資料傳輸物件
 * <p>
 * 用於 Web API 回傳文件完整資訊，包含內容、區塊和程式碼範例。
 * </p>
 *
 * @param id           文件 ID（TSID 格式）
 * @param versionId    版本 ID（TSID 格式）
 * @param title        文件標題
 * @param path         文件路徑
 * @param content      文件內容
 * @param type         文件類型（如 MARKDOWN、HTML）
 * @param metadata     額外的元資料
 * @param createdAt    建立時間
 * @param updatedAt    更新時間
 * @param chunks       文件區塊列表
 * @param codeExamples 程式碼範例列表
 */
public record DocumentDetailDto(
        String id,
        String versionId,
        String title,
        String path,
        String content,
        String type,
        Map<String, Object> metadata,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        List<ChunkDto> chunks,
        List<CodeExampleDto> codeExamples
) {
    /**
     * 從 DocumentContent 轉換
     *
     * @param content DocumentService.DocumentContent
     * @return DocumentDetailDto
     */
    public static DocumentDetailDto from(DocumentService.DocumentContent content) {
        Document doc = content.document();

        List<ChunkDto> chunkDtos = content.chunks().stream()
                .map(ChunkDto::from)
                .toList();

        List<CodeExampleDto> codeExampleDtos = content.codeExamples().stream()
                .map(CodeExampleDto::from)
                .toList();

        return new DocumentDetailDto(
                doc.getId(),
                doc.getVersionId(),
                doc.getTitle(),
                doc.getPath(),
                doc.getContent(),
                doc.getDocType(),
                doc.getMetadata(),
                doc.getCreatedAt(),
                doc.getUpdatedAt(),
                chunkDtos,
                codeExampleDtos
        );
    }

    /**
     * 文件區塊 DTO（不含 embedding）
     */
    public record ChunkDto(
            String id,
            int chunkIndex,
            String content,
            int tokenCount
    ) {
        public static ChunkDto from(DocumentChunk chunk) {
            return new ChunkDto(
                    chunk.getId(),
                    chunk.getChunkIndex(),
                    chunk.getContent(),
                    chunk.getTokenCount() != null ? chunk.getTokenCount() : 0
            );
        }
    }

    /**
     * 程式碼範例 DTO
     */
    public record CodeExampleDto(
            String id,
            String language,
            String code,
            String description,
            Integer startLine,
            Integer endLine
    ) {
        public static CodeExampleDto from(CodeExample example) {
            return new CodeExampleDto(
                    example.getId(),
                    example.getLanguage(),
                    example.getCode(),
                    example.getDescription(),
                    example.getStartLine(),
                    example.getEndLine()
            );
        }
    }
}
