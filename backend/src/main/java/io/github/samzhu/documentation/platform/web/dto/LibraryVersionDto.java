package io.github.samzhu.documentation.platform.web.dto;

import io.github.samzhu.documentation.platform.domain.enums.VersionStatus;
import io.github.samzhu.documentation.platform.domain.model.LibraryVersion;

import java.time.LocalDate;
import java.time.OffsetDateTime;

/**
 * 函式庫版本資料傳輸物件
 * <p>
 * 用於 Web API 回傳版本資訊。
 * </p>
 *
 * @param id          版本 ID（TSID 格式）
 * @param libraryId   函式庫 ID（TSID 格式）
 * @param version     版本號
 * @param isLatest    是否為最新版本
 * @param status      版本狀態
 * @param docsPath    文件路徑
 * @param releaseDate 發布日期
 * @param createdAt      建立時間
 * @param updatedAt      更新時間
 * @param documentCount  該版本的文件數量
 * @param lastSyncAt     最後一次同步完成時間
 */
public record LibraryVersionDto(
        String id,
        String libraryId,
        String version,
        Boolean isLatest,
        VersionStatus status,
        String docsPath,
        LocalDate releaseDate,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        Long documentCount,
        OffsetDateTime lastSyncAt
) {
    /**
     * 從 LibraryVersion 實體轉換（預設 documentCount=0、lastSyncAt=null）
     */
    public static LibraryVersionDto from(LibraryVersion version) {
        return from(version, 0L, null);
    }

    /**
     * 從 LibraryVersion 實體轉換，帶入文件數量與最後同步時間
     *
     * @param version       版本實體
     * @param documentCount 文件數量
     * @param lastSyncAt    最後同步完成時間
     */
    public static LibraryVersionDto from(LibraryVersion version, long documentCount, OffsetDateTime lastSyncAt) {
        return new LibraryVersionDto(
                version.getId(),
                version.getLibraryId(),
                version.getVersion(),
                version.getIsLatest(),
                version.getStatus(),
                version.getDocsPath(),
                version.getReleaseDate(),
                version.getCreatedAt(),
                version.getUpdatedAt(),
                documentCount,
                lastSyncAt
        );
    }
}
