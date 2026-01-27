package io.github.samzhu.documentation.platform.web.dto;

import io.github.samzhu.documentation.platform.domain.model.Document;

import java.time.OffsetDateTime;

/**
 * 文件列表資料傳輸物件
 * <p>
 * 用於 Web API 回傳文件列表（不含內容，避免資料量過大）。
 * </p>
 *
 * @param id        文件 ID（TSID 格式）
 * @param versionId 版本 ID（TSID 格式）
 * @param title     文件標題
 * @param path      文件路徑
 * @param type      文件類型（如 MARKDOWN、HTML）
 * @param createdAt 建立時間
 * @param updatedAt 更新時間
 */
public record DocumentDto(
        String id,
        String versionId,
        String title,
        String path,
        String type,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
    /**
     * 從 Document 實體轉換
     *
     * @param doc 文件實體
     * @return DocumentDto
     */
    public static DocumentDto from(Document doc) {
        return new DocumentDto(
                doc.getId(),
                doc.getVersionId(),
                doc.getTitle(),
                doc.getPath(),
                doc.getDocType(),
                doc.getCreatedAt(),
                doc.getUpdatedAt()
        );
    }
}
