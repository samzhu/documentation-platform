package io.github.samzhu.documentation.platform.web.api;

import io.github.samzhu.documentation.platform.domain.model.Library;
import io.github.samzhu.documentation.platform.domain.model.LibraryVersion;
import io.github.samzhu.documentation.platform.domain.model.SyncHistory;
import io.github.samzhu.documentation.platform.repository.DocumentChunkRepository;
import io.github.samzhu.documentation.platform.repository.DocumentRepository;
import io.github.samzhu.documentation.platform.service.LibraryService;
import io.github.samzhu.documentation.platform.service.SyncService;
import io.github.samzhu.documentation.platform.web.dto.BatchSyncRequest;
import io.github.samzhu.documentation.platform.web.dto.BatchSyncResponse;
import io.github.samzhu.documentation.platform.web.dto.CreateLibraryRequest;
import io.github.samzhu.documentation.platform.web.dto.GitHubReleasesResponse;
import io.github.samzhu.documentation.platform.web.dto.LibraryVersionDto;
import io.github.samzhu.documentation.platform.web.dto.SyncHistoryDto;
import io.github.samzhu.documentation.platform.web.dto.TriggerSyncRequest;
import io.github.samzhu.documentation.platform.web.dto.UpdateLibraryRequest;
import io.github.samzhu.documentation.platform.web.dto.WebLibraryDto;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 函式庫 REST API
 * <p>
 * 提供函式庫的 CRUD 操作和同步觸發 API。
 * 所有端點回傳純 JSON 格式。
 * </p>
 */
@RestController
@RequestMapping("/api/libraries")
public class LibraryApiController {

    /**
     * GitHub URL 解析正規表達式
     * 用於從 GitHub URL 中擷取 owner 和 repo 名稱
     */
    private static final Pattern GITHUB_URL_PATTERN = Pattern.compile(
            "^https://github\\.com/([^/]+)/([^/]+)(?:/.*)?$"
    );

    private final LibraryService libraryService;
    private final SyncService syncService;
    private final DocumentRepository documentRepository;
    private final DocumentChunkRepository documentChunkRepository;

    /**
     * 建構函式
     *
     * @param libraryService            函式庫服務
     * @param syncService               同步服務
     * @param documentRepository        文件資料存取介面
     * @param documentChunkRepository   文件區塊資料存取介面
     */
    public LibraryApiController(LibraryService libraryService,
                                 SyncService syncService,
                                 DocumentRepository documentRepository,
                                 DocumentChunkRepository documentChunkRepository) {
        this.libraryService = libraryService;
        this.syncService = syncService;
        this.documentRepository = documentRepository;
        this.documentChunkRepository = documentChunkRepository;
    }

    /**
     * 列出所有函式庫
     * <p>
     * 支援依分類篩選函式庫列表。包含每個函式庫的文件數量和區塊數量統計。
     * </p>
     *
     * @param category 分類篩選（可選）
     * @return 函式庫列表
     */
    @GetMapping
    public List<WebLibraryDto> listLibraries(@RequestParam(required = false) String category) {
        return libraryService.listLibraries(category).stream()
                .map(library -> {
                    // 統計該函式庫的文件數量和區塊數量
                    long docCount = documentRepository.countByLibraryId(library.getId());
                    long chunkCount = documentChunkRepository.countByLibraryId(library.getId());
                    return WebLibraryDto.from(library, docCount, chunkCount);
                })
                .toList();
    }

    /**
     * 建立新函式庫
     * <p>
     * 建立新的文件函式庫，指定來源類型和 URL。
     * </p>
     *
     * @param request 建立請求
     * @return 建立的函式庫（HTTP 201 Created）
     */
    @PostMapping
    public ResponseEntity<WebLibraryDto> createLibrary(@RequestBody @Valid CreateLibraryRequest request) {
        Library library = libraryService.createLibrary(
                request.name(),
                request.displayName(),
                request.description(),
                request.sourceType(),
                request.sourceUrl(),
                request.category(),
                request.tags()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(WebLibraryDto.from(library));
    }

    /**
     * 取得單一函式庫
     * <p>
     * 依 ID 取得函式庫詳細資訊。包含文件數量和區塊數量統計。
     * </p>
     *
     * @param id 函式庫 ID（TSID 格式，13 字元）
     * @return 函式庫資訊
     */
    @GetMapping("/{id}")
    public WebLibraryDto getLibrary(@PathVariable String id) {
        Library library = libraryService.getLibraryById(id);
        // 統計該函式庫的文件數量和區塊數量
        long docCount = documentRepository.countByLibraryId(library.getId());
        long chunkCount = documentChunkRepository.countByLibraryId(library.getId());
        return WebLibraryDto.from(library, docCount, chunkCount);
    }

    /**
     * 更新函式庫
     * <p>
     * 更新函式庫的顯示名稱、描述、來源等資訊。
     * </p>
     *
     * @param id      函式庫 ID（TSID 格式，13 字元）
     * @param request 更新請求
     * @return 更新後的函式庫
     */
    @PutMapping("/{id}")
    public WebLibraryDto updateLibrary(@PathVariable String id,
                                        @RequestBody @Valid UpdateLibraryRequest request) {
        Library library = libraryService.updateLibrary(
                id,
                request.displayName(),
                request.description(),
                request.sourceType(),
                request.sourceUrl(),
                request.category(),
                request.tags()
        );
        return WebLibraryDto.from(library);
    }

    /**
     * 刪除函式庫
     * <p>
     * 刪除函式庫及其所有版本和文件。此操作不可逆。
     * </p>
     *
     * @param id 函式庫 ID（TSID 格式，13 字元）
     * @return HTTP 204 No Content
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteLibrary(@PathVariable String id) {
        libraryService.deleteLibrary(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * 取得函式庫的所有版本
     * <p>
     * 列出指定函式庫的所有版本資訊。
     * </p>
     *
     * @param id 函式庫 ID（TSID 格式，13 字元）
     * @return 版本列表
     */
    @GetMapping("/{id}/versions")
    public List<LibraryVersionDto> listVersions(@PathVariable String id) {
        return libraryService.getLibraryVersionsById(id).stream()
                .map(LibraryVersionDto::from)
                .toList();
    }

    /**
     * 觸發同步
     * <p>
     * 手動觸發指定版本的文件同步。從 GitHub 抓取文件並更新向量嵌入。
     * </p>
     *
     * @param id      函式庫 ID（TSID 格式，13 字元）
     * @param request 同步請求（包含版本資訊）
     * @return 同步歷史（HTTP 202 Accepted）
     */
    @PostMapping("/{id}/sync")
    public ResponseEntity<SyncHistoryDto> triggerSync(@PathVariable String id,
                                                       @RequestBody @Valid TriggerSyncRequest request) {
        Library library = libraryService.getLibraryById(id);

        // 解析 GitHub URL，擷取 owner 和 repo
        Matcher matcher = GITHUB_URL_PATTERN.matcher(library.getSourceUrl());
        if (!matcher.matches()) {
            throw new IllegalArgumentException("無效的 GitHub URL: " + library.getSourceUrl());
        }

        String owner = matcher.group(1);
        String repo = matcher.group(2);

        // 取得或建立版本
        LibraryVersion version = libraryService.resolveLibrary(library.getName(), request.version()).version();

        // 觸發同步（非同步執行）
        SyncHistory syncHistory = syncService.syncFromGitHub(
                version.getId(),
                owner,
                repo,
                version.getDocsPath() != null ? version.getDocsPath() : "docs",
                "v" + request.version()
        ).join();

        return ResponseEntity.status(HttpStatus.ACCEPTED).body(SyncHistoryDto.from(syncHistory));
    }

    /**
     * 取得 GitHub Releases 列表
     * <p>
     * 從 GitHub API 取得指定函式庫的 Release 列表，並自動帶入已知的文件路徑。
     * 已存在於系統中的版本會被標記。
     * </p>
     *
     * @param id    函式庫 ID（TSID 格式，13 字元）
     * @param limit 回傳數量上限（預設 20）
     * @return GitHub Releases 回應
     */
    @GetMapping("/{id}/github-releases")
    public GitHubReleasesResponse getGitHubReleases(@PathVariable String id,
                                                     @RequestParam(defaultValue = "20") int limit) {
        return libraryService.getGitHubReleases(id, limit);
    }

    /**
     * 批次建立版本並同步
     * <p>
     * 一次建立多個版本並觸發同步任務。適用於初次匯入函式庫時批次處理多個版本。
     * </p>
     *
     * @param id      函式庫 ID（TSID 格式，13 字元）
     * @param request 批次同步請求
     * @return 批次同步回應（HTTP 202 Accepted）
     */
    @PostMapping("/{id}/batch-sync")
    public ResponseEntity<BatchSyncResponse> batchSync(@PathVariable String id,
                                                        @RequestBody @Valid BatchSyncRequest request) {
        BatchSyncResponse response = libraryService.batchCreateAndSync(id, request);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }
}
