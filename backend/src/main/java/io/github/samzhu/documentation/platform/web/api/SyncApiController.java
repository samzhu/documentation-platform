package io.github.samzhu.documentation.platform.web.api;

import io.github.samzhu.documentation.platform.domain.model.Library;
import io.github.samzhu.documentation.platform.domain.model.LibraryVersion;
import io.github.samzhu.documentation.platform.domain.model.SyncHistory;
import io.github.samzhu.documentation.platform.repository.LibraryRepository;
import io.github.samzhu.documentation.platform.repository.LibraryVersionRepository;
import io.github.samzhu.documentation.platform.service.SyncService;
import io.github.samzhu.documentation.platform.web.dto.SyncHistoryDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * 同步 REST API
 * <p>
 * 提供同步觸發和同步歷史查詢功能。
 * </p>
 */
@RestController
@RequestMapping("/api/sync")
public class SyncApiController {

    private static final Logger log = LoggerFactory.getLogger(SyncApiController.class);

    /**
     * GitHub URL 解析正規表達式
     */
    private static final Pattern GITHUB_URL_PATTERN = Pattern.compile(
            "^https://github\\.com/([^/]+)/([^/]+)(?:/.*)?$"
    );

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
     * 觸發指定版本的文件同步
     * <p>
     * 根據函式庫 ID 和版本 ID 觸發非同步同步任務，
     * 從 GitHub 抓取文件並更新向量嵌入。
     * </p>
     *
     * @param libraryId 函式庫 ID（TSID 格式，13 字元）
     * @param versionId 版本 ID（TSID 格式，13 字元）
     * @return 同步歷史（HTTP 202 Accepted）
     */
    @PostMapping("/libraries/{libraryId}/versions/{versionId}")
    public ResponseEntity<SyncHistoryDto> triggerVersionSync(
            @PathVariable String libraryId,
            @PathVariable String versionId) {

        log.info("收到版本同步請求: libraryId={}, versionId={}", libraryId, versionId);

        // 查詢函式庫
        Library library = libraryRepository.findById(libraryId)
                .orElseThrow(() -> new IllegalArgumentException("找不到函式庫: " + libraryId));

        // 查詢版本
        LibraryVersion version = versionRepository.findById(versionId)
                .orElseThrow(() -> new IllegalArgumentException("找不到版本: " + versionId));

        // 解析 GitHub URL，擷取 owner 和 repo
        Matcher matcher = GITHUB_URL_PATTERN.matcher(library.getSourceUrl());
        if (!matcher.matches()) {
            throw new IllegalArgumentException("無效的 GitHub URL: " + library.getSourceUrl());
        }

        String owner = matcher.group(1);
        String repo = matcher.group(2);
        String docsPath = version.getDocsPath() != null ? version.getDocsPath() : "docs";

        // 觸發同步（非同步執行）
        SyncHistory syncHistory = syncService.syncFromGitHub(
                versionId,
                owner,
                repo,
                docsPath,
                "v" + version.getVersion()
        ).join();

        String libraryName = library.getDisplayName() != null ? library.getDisplayName() : library.getName();
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(SyncHistoryDto.from(syncHistory, libraryId, libraryName, version.getVersion()));
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
        log.debug("查詢同步歷史: libraryId={}, versionId={}", libraryId, versionId);

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
