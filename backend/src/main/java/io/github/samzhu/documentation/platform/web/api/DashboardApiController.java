package io.github.samzhu.documentation.platform.web.api;

import io.github.samzhu.documentation.platform.domain.enums.ApiKeyStatus;
import io.github.samzhu.documentation.platform.repository.ApiKeyRepository;
import io.github.samzhu.documentation.platform.repository.DocumentChunkRepository;
import io.github.samzhu.documentation.platform.repository.DocumentRepository;
import io.github.samzhu.documentation.platform.repository.LibraryRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 儀表板 REST API
 * <p>
 * 提供系統統計資料，供前端儀表板顯示。
 * </p>
 */
@RestController
@RequestMapping("/api/dashboard")
public class DashboardApiController {

    private final LibraryRepository libraryRepository;
    private final DocumentRepository documentRepository;
    private final DocumentChunkRepository documentChunkRepository;
    private final ApiKeyRepository apiKeyRepository;

    /**
     * 建構函式
     *
     * @param libraryRepository       函式庫儲存庫
     * @param documentRepository      文件儲存庫
     * @param documentChunkRepository 文件區塊儲存庫
     * @param apiKeyRepository        API 金鑰儲存庫
     */
    public DashboardApiController(
            LibraryRepository libraryRepository,
            DocumentRepository documentRepository,
            DocumentChunkRepository documentChunkRepository,
            ApiKeyRepository apiKeyRepository) {
        this.libraryRepository = libraryRepository;
        this.documentRepository = documentRepository;
        this.documentChunkRepository = documentChunkRepository;
        this.apiKeyRepository = apiKeyRepository;
    }

    /**
     * 取得儀表板統計資料
     * <p>
     * 回傳系統整體統計，包含函式庫、文件、向量片段、API 金鑰數量。
     * </p>
     *
     * @return 統計資料
     */
    @GetMapping("/stats")
    public DashboardStatsResponse getStats() {
        long libraryCount = libraryRepository.count();
        long documentCount = documentRepository.count();
        long chunkCount = documentChunkRepository.count();
        long apiKeyCount = apiKeyRepository.countByStatus(ApiKeyStatus.ACTIVE);

        return new DashboardStatsResponse(
                libraryCount,
                documentCount,
                chunkCount,
                apiKeyCount
        );
    }

    /**
     * 儀表板統計回應
     *
     * @param libraryCount  函式庫數量
     * @param documentCount 文件數量
     * @param chunkCount    向量片段數量
     * @param apiKeyCount   啟用中的 API 金鑰數量
     */
    public record DashboardStatsResponse(
            long libraryCount,
            long documentCount,
            long chunkCount,
            long apiKeyCount
    ) {}
}
