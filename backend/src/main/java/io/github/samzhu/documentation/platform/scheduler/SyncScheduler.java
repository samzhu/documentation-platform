package io.github.samzhu.documentation.platform.scheduler;

import io.github.samzhu.documentation.platform.config.FeatureFlags;
import io.github.samzhu.documentation.platform.domain.enums.SourceType;
import io.github.samzhu.documentation.platform.domain.model.Library;
import io.github.samzhu.documentation.platform.domain.model.LibraryVersion;
import io.github.samzhu.documentation.platform.repository.LibraryRepository;
import io.github.samzhu.documentation.platform.repository.LibraryVersionRepository;
import io.github.samzhu.documentation.platform.service.SyncService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 同步排程器
 * <p>
 * 負責定時執行文件同步任務。
 * 預設每天凌晨 2 點執行，可透過配置調整。
 * </p>
 */
@Component
public class SyncScheduler {

    private static final Logger log = LoggerFactory.getLogger(SyncScheduler.class);

    /** GitHub URL 解析模式，用於提取 owner 和 repo */
    private static final Pattern GITHUB_URL_PATTERN = Pattern.compile("https://github\\.com/([^/]+)/([^/]+)");

    private final SyncService syncService;
    private final LibraryRepository libraryRepository;
    private final LibraryVersionRepository versionRepository;
    private final FeatureFlags featureFlags;

    /**
     * 建構同步排程器
     *
     * @param syncService       同步服務，負責實際的文件同步邏輯
     * @param libraryRepository 函式庫儲存庫，用於查詢所有函式庫
     * @param versionRepository 版本儲存庫，用於查詢函式庫的所有版本
     * @param featureFlags      功能開關，控制排程是否啟用
     */
    public SyncScheduler(SyncService syncService,
                          LibraryRepository libraryRepository,
                          LibraryVersionRepository versionRepository,
                          FeatureFlags featureFlags) {
        this.syncService = syncService;
        this.libraryRepository = libraryRepository;
        this.versionRepository = versionRepository;
        this.featureFlags = featureFlags;
    }

    /**
     * 定時同步任務
     * <p>
     * 每天凌晨 2 點執行，遍歷所有 GitHub 來源的函式庫並同步最新版本的文件。
     * 只有在 docmcp.features.sync-scheduling=true 時才會執行。
     * </p>
     */
    @Scheduled(cron = "${docmcp.sync.cron:0 0 2 * * *}")
    public void scheduledSync() {
        // 檢查同步排程功能是否啟用
        if (!featureFlags.isSyncScheduling()) {
            log.debug("Sync scheduling is disabled, skipping scheduled sync");
            return;
        }

        log.info("Starting scheduled sync for all libraries");

        try {
            // 取得所有函式庫
            Iterable<Library> libraries = libraryRepository.findAll();

            // 逐一處理每個函式庫
            for (Library library : libraries) {
                // 只處理 GitHub 來源的函式庫
                if (library.getSourceType() != SourceType.GITHUB) {
                    log.debug("Skipping non-GitHub library: {}", library.getName());
                    continue;
                }

                syncLibrary(library);
            }

            log.info("Scheduled sync completed");

        } catch (Exception e) {
            log.error("Scheduled sync failed", e);
        }
    }

    /**
     * 同步單一函式庫的所有版本
     *
     * @param library 要同步的函式庫
     */
    private void syncLibrary(Library library) {
        String sourceUrl = library.getSourceUrl();

        // 檢查來源 URL 是否有效
        if (sourceUrl == null || sourceUrl.isBlank()) {
            log.warn("Library {} has no source URL, skipping", library.getName());
            return;
        }

        // 解析 GitHub URL 取得 owner 和 repo
        Matcher matcher = GITHUB_URL_PATTERN.matcher(sourceUrl);
        if (!matcher.find()) {
            log.warn("Invalid GitHub URL for library {}: {}", library.getName(), sourceUrl);
            return;
        }

        String owner = matcher.group(1);
        String repo = matcher.group(2);

        // 取得該函式庫的所有版本並逐一同步
        List<LibraryVersion> versions = versionRepository.findByLibraryId(library.getId());
        for (LibraryVersion version : versions) {
            try {
                log.info("Syncing library: {} version: {}", library.getName(), version.getVersion());

                // 取得文件路徑，預設為 "docs"
                String docsPath = version.getDocsPath() != null ? version.getDocsPath() : "docs";
                String ref = version.getVersion();

                // 執行 GitHub 同步
                syncService.syncFromGitHub(version.getId(), owner, repo, docsPath, ref);

            } catch (Exception e) {
                log.error("Failed to sync library {} version {}: {}",
                        library.getName(), version.getVersion(), e.getMessage());
            }
        }
    }
}
