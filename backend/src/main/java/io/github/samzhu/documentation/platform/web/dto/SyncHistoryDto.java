package io.github.samzhu.documentation.platform.web.dto;

import io.github.samzhu.documentation.platform.domain.enums.SyncStatus;
import io.github.samzhu.documentation.platform.domain.model.SyncHistory;

import java.time.OffsetDateTime;
import java.util.Map;

/**
 * 同步歷史資料傳輸物件
 * <p>
 * 用於 Web API 回傳同步歷史資訊，包含關聯的文件庫和版本資訊。
 * </p>
 *
 * @param id                 同步 ID（TSID 格式）
 * @param versionId          版本 ID（TSID 格式）
 * @param libraryId          文件庫 ID（TSID 格式）
 * @param libraryName        文件庫名稱
 * @param version            版本號
 * @param status             同步狀態
 * @param startedAt          開始時間
 * @param completedAt        完成時間
 * @param documentsProcessed 已處理文件數
 * @param chunksCreated      已建立區塊數
 * @param errorMessage       錯誤訊息
 * @param metadata           額外的元資料
 */
public record SyncHistoryDto(
        String id,
        String versionId,
        String libraryId,
        String libraryName,
        String version,
        SyncStatus status,
        OffsetDateTime startedAt,
        OffsetDateTime completedAt,
        Integer documentsProcessed,
        Integer chunksCreated,
        String errorMessage,
        Map<String, Object> metadata
) {
    /**
     * 從 SyncHistory 實體轉換（不含關聯資訊）
     */
    public static SyncHistoryDto from(SyncHistory history) {
        return from(history, null, null, null);
    }

    /**
     * 從 SyncHistory 實體轉換（包含關聯資訊）
     *
     * @param history     同步歷史實體
     * @param libraryId   文件庫 ID
     * @param libraryName 文件庫名稱
     * @param version     版本號
     * @return SyncHistoryDto
     */
    public static SyncHistoryDto from(SyncHistory history, String libraryId,
                                       String libraryName, String version) {
        return new SyncHistoryDto(
                history.getId(),
                history.getVersionId(),
                libraryId,
                libraryName,
                version,
                history.getStatus(),
                history.getStartedAt(),
                history.getCompletedAt(),
                history.getDocumentsProcessed(),
                history.getChunksCreated(),
                history.getErrorMessage(),
                history.getMetadata()
        );
    }
}
