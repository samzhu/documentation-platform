package io.github.samzhu.documentation.platform.web.api;

import io.github.samzhu.documentation.platform.domain.model.Library;
import io.github.samzhu.documentation.platform.domain.model.LibraryVersion;
import io.github.samzhu.documentation.platform.domain.model.SyncHistory;
import io.github.samzhu.documentation.platform.repository.LibraryRepository;
import io.github.samzhu.documentation.platform.repository.LibraryVersionRepository;
import io.github.samzhu.documentation.platform.service.SyncService;
import io.github.samzhu.documentation.platform.web.dto.SyncHistoryDto;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * 同步狀態 REST API
 * <p>
 * 提供同步歷史查詢功能，用於追蹤文件同步的執行狀態和結果。
 * </p>
 */
@RestController
@RequestMapping("/api/sync")
public class SyncApiController {

    private final SyncService syncService;
    private final LibraryVersionRepository versionRepository;
    private final LibraryRepository libraryRepository;

    /**
     * 建構函式
     *
     * @param syncService       同步服務
     * @param versionRepository 版本 Repository
     * @param libraryRepository 文件庫 Repository
     */
    public SyncApiController(SyncService syncService,
                             LibraryVersionRepository versionRepository,
                             LibraryRepository libraryRepository) {
        this.syncService = syncService;
        this.versionRepository = versionRepository;
        this.libraryRepository = libraryRepository;
    }

    /**
     * 取得同步歷史
     * <p>
     * 查詢同步歷史記錄，可依函式庫或版本篩選。
     * 回傳結果包含關聯的文件庫和版本資訊。
     * </p>
     *
     * @param libraryId 函式庫 ID（可選，用於查詢特定函式庫的版本，TSID 格式）
     * @param versionId 版本 ID（可選，直接指定版本，TSID 格式）
     * @param limit     結果數量上限（預設 10）
     * @return 同步歷史列表
     */
    @GetMapping("/history")
    public List<SyncHistoryDto> getSyncHistory(
            @RequestParam(required = false) String libraryId,
            @RequestParam(required = false) String versionId,
            @RequestParam(defaultValue = "10") int limit
    ) {
        // 如果指定了 versionId，直接使用；否則查詢所有
        String targetVersionId = versionId;

        List<SyncHistory> histories = syncService.getSyncHistory(targetVersionId, limit);

        if (histories.isEmpty()) {
            return List.of();
        }

        // 批次查詢版本資訊（避免 N+1 問題）
        Set<String> versionIds = histories.stream()
                .map(SyncHistory::getVersionId)
                .collect(Collectors.toSet());

        Map<String, LibraryVersion> versionMap = StreamSupport
                .stream(versionRepository.findAllById(versionIds).spliterator(), false)
                .collect(Collectors.toMap(LibraryVersion::getId, Function.identity()));

        // 批次查詢文件庫資訊
        Set<String> libraryIds = versionMap.values().stream()
                .map(LibraryVersion::getLibraryId)
                .collect(Collectors.toSet());

        Map<String, Library> libraryMap = StreamSupport
                .stream(libraryRepository.findAllById(libraryIds).spliterator(), false)
                .collect(Collectors.toMap(Library::getId, Function.identity()));

        // 轉換為 DTO，包含關聯資訊
        return histories.stream()
                .map(history -> {
                    LibraryVersion version = versionMap.get(history.getVersionId());
                    Library library = version != null ? libraryMap.get(version.getLibraryId()) : null;

                    return SyncHistoryDto.from(
                            history,
                            library != null ? library.getId() : null,
                            library != null ? (library.getDisplayName() != null ? library.getDisplayName() : library.getName()) : null,
                            version != null ? version.getVersion() : null
                    );
                })
                .toList();
    }

    /**
     * 取得單一同步記錄
     * <p>
     * 依 ID 取得特定同步記錄的詳細資訊，包含同步狀態、文件數量及關聯的文件庫資訊。
     * </p>
     *
     * @param id 同步 ID（TSID 格式，13 字元）
     * @return 同步歷史，若不存在則回傳 404
     */
    @GetMapping("/{id}")
    public ResponseEntity<SyncHistoryDto> getSyncStatus(@PathVariable String id) {
        return syncService.getSyncStatus(id)
                .map(history -> {
                    // 查詢關聯的版本和文件庫資訊
                    LibraryVersion version = versionRepository.findById(history.getVersionId()).orElse(null);
                    Library library = version != null
                            ? libraryRepository.findById(version.getLibraryId()).orElse(null)
                            : null;

                    return SyncHistoryDto.from(
                            history,
                            library != null ? library.getId() : null,
                            library != null ? (library.getDisplayName() != null ? library.getDisplayName() : library.getName()) : null,
                            version != null ? version.getVersion() : null
                    );
                })
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
