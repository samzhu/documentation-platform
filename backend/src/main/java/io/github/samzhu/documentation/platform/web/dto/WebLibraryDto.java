package io.github.samzhu.documentation.platform.web.dto;

import io.github.samzhu.documentation.platform.domain.enums.SourceType;
import io.github.samzhu.documentation.platform.domain.model.Library;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Web API 用函式庫資料傳輸物件
 * <p>
 * 包含完整的函式庫資訊，用於 REST API 回應。
 * </p>
 *
 * @param id            唯一識別碼（TSID 格式）
 * @param name          函式庫名稱
 * @param displayName   顯示名稱
 * @param description   描述
 * @param sourceType    來源類型
 * @param sourceUrl     來源網址
 * @param category      分類
 * @param tags          標籤
 * @param createdAt     建立時間
 * @param updatedAt     更新時間
 * @param documentCount 文件數量
 * @param chunkCount    區塊數量
 */
public record WebLibraryDto(
        String id,
        String name,
        String displayName,
        String description,
        SourceType sourceType,
        String sourceUrl,
        String category,
        List<String> tags,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        long documentCount,
        long chunkCount
) {
    /**
     * 從 Library 實體轉換（用於向後相容，統計數字為 0）
     */
    public static WebLibraryDto from(Library library) {
        return from(library, 0L, 0L);
    }

    /**
     * 從 Library 實體轉換（包含統計數字）
     *
     * @param library       函式庫實體
     * @param documentCount 文件數量
     * @param chunkCount    區塊數量
     * @return WebLibraryDto
     */
    public static WebLibraryDto from(Library library, long documentCount, long chunkCount) {
        return new WebLibraryDto(
                library.getId(),
                library.getName(),
                library.getDisplayName(),
                library.getDescription(),
                library.getSourceType(),
                library.getSourceUrl(),
                library.getCategory(),
                library.getTags(),
                library.getCreatedAt(),
                library.getUpdatedAt(),
                documentCount,
                chunkCount
        );
    }
}
