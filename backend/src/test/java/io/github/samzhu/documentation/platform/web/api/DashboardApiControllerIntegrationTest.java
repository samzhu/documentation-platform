package io.github.samzhu.documentation.platform.web.api;

import io.github.samzhu.documentation.platform.TestcontainersConfiguration;
import io.github.samzhu.documentation.platform.domain.enums.SourceType;
import io.github.samzhu.documentation.platform.domain.model.*;
import io.github.samzhu.documentation.platform.repository.*;
import io.github.samzhu.documentation.platform.service.IdService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.client.RestTestClient;

import java.util.List;

/**
 * Dashboard API Controller 整合測試
 * <p>
 * 使用 Spring Boot 4 最新測試工具：
 * - RestTestClient（取代 MockMvc/TestRestTemplate）
 * - @ServiceConnection（簡化 Testcontainers 配置）
 * - 真實 Google AI EmbeddingModel（API Key 從 config/application-secrets.properties 載入）
 * </p>
 */
@Tag("integration")
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureRestTestClient
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("integration-test")
class DashboardApiControllerIntegrationTest {

    @Autowired
    RestTestClient client;

    @Autowired
    LibraryRepository libraryRepository;

    @Autowired
    LibraryVersionRepository libraryVersionRepository;

    @Autowired
    DocumentRepository documentRepository;

    @Autowired
    DocumentChunkRepository documentChunkRepository;

    @Autowired
    ApiKeyRepository apiKeyRepository;

    @Autowired
    IdService idService;

    @BeforeEach
    void setUp() {
        // 清除所有測試資料（順序重要：先刪除子實體）
        documentChunkRepository.deleteAll();
        documentRepository.deleteAll();
        libraryVersionRepository.deleteAll();
        libraryRepository.deleteAll();
        apiKeyRepository.deleteAll();
    }

    @Test
    @DisplayName("GET /api/dashboard/stats - 空資料庫應回傳零值")
    @WithMockUser
    void shouldReturnZeroStatsWhenEmpty() {
        // When & Then - 查詢統計
        client.get().uri("/api/dashboard/stats")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.libraryCount").isEqualTo(0)
                .jsonPath("$.documentCount").isEqualTo(0)
                .jsonPath("$.chunkCount").isEqualTo(0)
                .jsonPath("$.apiKeyCount").isEqualTo(0);
    }

    @Test
    @DisplayName("GET /api/dashboard/stats - 應回傳正確的 Library 數量")
    @WithMockUser
    void shouldReturnCorrectLibraryCount() {
        // Given - 建立多個 Library
        Library lib1 = Library.create(
                idService.generateId(),
                "spring-boot",
                "Spring Boot",
                "Spring Boot 框架",
                SourceType.GITHUB,
                "https://github.com/spring-projects/spring-boot",
                "backend",
                List.of()
        );
        Library lib2 = Library.create(
                idService.generateId(),
                "react",
                "React",
                "React 框架",
                SourceType.GITHUB,
                "https://github.com/facebook/react",
                "frontend",
                List.of()
        );
        Library lib3 = Library.create(
                idService.generateId(),
                "vue",
                "Vue.js",
                "Vue.js 框架",
                SourceType.GITHUB,
                "https://github.com/vuejs/vue",
                "frontend",
                List.of()
        );
        libraryRepository.saveAll(List.of(lib1, lib2, lib3));

        // When & Then - 驗證數量
        client.get().uri("/api/dashboard/stats")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.libraryCount").isEqualTo(3);
    }

    @Test
    @DisplayName("GET /api/dashboard/stats - 應回傳正確的 Document 數量")
    @WithMockUser
    void shouldReturnCorrectDocumentCount() {
        // Given - 建立 Library、Version 和多個 Document
        Library library = Library.create(
                idService.generateId(),
                "spring-boot",
                "Spring Boot",
                "Spring Boot 框架",
                SourceType.GITHUB,
                "https://github.com/spring-projects/spring-boot",
                "backend",
                List.of()
        );
        library = libraryRepository.save(library);

        LibraryVersion version = LibraryVersion.create(
                idService.generateId(),
                library.getId(),
                "3.2.0",
                true
        );
        version = libraryVersionRepository.save(version);

        Document doc1 = Document.create(
                idService.generateId(),
                version.getId(),
                "Getting Started",
                "/docs/getting-started.md",
                "Content 1",
                "hash1",
                "markdown"
        );
        Document doc2 = Document.create(
                idService.generateId(),
                version.getId(),
                "Configuration",
                "/docs/config.md",
                "Content 2",
                "hash2",
                "markdown"
        );
        documentRepository.saveAll(List.of(doc1, doc2));

        // When & Then - 驗證數量
        client.get().uri("/api/dashboard/stats")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.libraryCount").isEqualTo(1)
                .jsonPath("$.documentCount").isEqualTo(2);
    }

    @Test
    @DisplayName("GET /api/dashboard/stats - 應回傳正確的 Chunk 數量")
    @WithMockUser
    void shouldReturnCorrectChunkCount() {
        // Given - 建立完整的資料階層
        Library library = Library.create(
                idService.generateId(),
                "spring-boot",
                "Spring Boot",
                "Spring Boot 框架",
                SourceType.GITHUB,
                "https://github.com/spring-projects/spring-boot",
                "backend",
                List.of()
        );
        library = libraryRepository.save(library);

        LibraryVersion version = LibraryVersion.create(
                idService.generateId(),
                library.getId(),
                "3.2.0",
                true
        );
        version = libraryVersionRepository.save(version);

        Document document = Document.create(
                idService.generateId(),
                version.getId(),
                "Getting Started",
                "/docs/getting-started.md",
                "Full content",
                "hash1",
                "markdown"
        );
        document = documentRepository.save(document);

        // 建立多個 Chunk
        DocumentChunk chunk1 = DocumentChunk.create(
                idService.generateId(),
                document.getId(),
                0,
                "Chunk 1 content",
                new float[768],
                50
        );
        DocumentChunk chunk2 = DocumentChunk.create(
                idService.generateId(),
                document.getId(),
                1,
                "Chunk 2 content",
                new float[768],
                50
        );
        DocumentChunk chunk3 = DocumentChunk.create(
                idService.generateId(),
                document.getId(),
                2,
                "Chunk 3 content",
                new float[768],
                50
        );
        documentChunkRepository.saveAll(List.of(chunk1, chunk2, chunk3));

        // When & Then - 驗證數量
        client.get().uri("/api/dashboard/stats")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.chunkCount").isEqualTo(3);
    }

    @Test
    @DisplayName("GET /api/dashboard/stats - 應只統計 ACTIVE 狀態的 API Key")
    @WithMockUser
    void shouldCountOnlyActiveApiKeys() {
        // Given - 建立不同狀態的 API Key
        ApiKey activeKey1 = ApiKey.create(
                idService.generateId(),
                "Active Key 1",
                "$2a$10$TestHash1",
                "dmcp_activ1",
                1000,
                null,
                "admin"
        );
        ApiKey activeKey2 = ApiKey.create(
                idService.generateId(),
                "Active Key 2",
                "$2a$10$TestHash2",
                "dmcp_activ2",
                1000,
                null,
                "admin"
        );

        // 建立已撤銷的 Key（使用建構子設定 REVOKED 狀態）
        ApiKey revokedKey = new ApiKey(
                idService.generateId(),
                "Revoked Key",
                "$2a$10$TestHash3",
                "dmcp_revok1",
                io.github.samzhu.documentation.platform.domain.enums.ApiKeyStatus.REVOKED,
                1000,
                null,
                null,
                "admin",
                null,
                null,
                null
        );

        apiKeyRepository.saveAll(List.of(activeKey1, activeKey2, revokedKey));

        // When & Then - 只應統計 ACTIVE 狀態的 Key
        client.get().uri("/api/dashboard/stats")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.apiKeyCount").isEqualTo(2);
    }

    @Test
    @DisplayName("GET /api/dashboard/stats - 應回傳完整的統計物件")
    @WithMockUser
    void shouldReturnCompleteStatsObject() {
        // Given - 建立各種資料
        Library library = Library.create(
                idService.generateId(),
                "spring-boot",
                "Spring Boot",
                "Spring Boot 框架",
                SourceType.GITHUB,
                "https://github.com/spring-projects/spring-boot",
                "backend",
                List.of()
        );
        library = libraryRepository.save(library);

        LibraryVersion version = LibraryVersion.create(
                idService.generateId(),
                library.getId(),
                "3.2.0",
                true
        );
        version = libraryVersionRepository.save(version);

        Document document = Document.create(
                idService.generateId(),
                version.getId(),
                "Getting Started",
                "/docs/getting-started.md",
                "Content",
                "hash",
                "markdown"
        );
        document = documentRepository.save(document);

        DocumentChunk chunk = DocumentChunk.create(
                idService.generateId(),
                document.getId(),
                0,
                "Chunk content",
                new float[768],
                50
        );
        documentChunkRepository.save(chunk);

        ApiKey apiKey = ApiKey.create(
                idService.generateId(),
                "Test Key",
                "$2a$10$TestHash",
                "dmcp_testky",
                1000,
                null,
                "admin"
        );
        apiKeyRepository.save(apiKey);

        // When & Then - 驗證所有欄位
        client.get().uri("/api/dashboard/stats")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.libraryCount").isEqualTo(1)
                .jsonPath("$.documentCount").isEqualTo(1)
                .jsonPath("$.chunkCount").isEqualTo(1)
                .jsonPath("$.apiKeyCount").isEqualTo(1);
    }

    @Test
    @DisplayName("GET /api/dashboard/stats - 統計應包含所有版本的資料")
    @WithMockUser
    void shouldCountDocumentsAcrossAllVersions() {
        // Given - 建立 Library 和多個版本
        Library library = Library.create(
                idService.generateId(),
                "spring-boot",
                "Spring Boot",
                "Spring Boot 框架",
                SourceType.GITHUB,
                "https://github.com/spring-projects/spring-boot",
                "backend",
                List.of()
        );
        library = libraryRepository.save(library);

        LibraryVersion version1 = LibraryVersion.create(
                idService.generateId(),
                library.getId(),
                "3.1.0",
                false
        );
        version1 = libraryVersionRepository.save(version1);

        LibraryVersion version2 = LibraryVersion.create(
                idService.generateId(),
                library.getId(),
                "3.2.0",
                true
        );
        version2 = libraryVersionRepository.save(version2);

        // 為每個版本建立文件
        Document doc1 = Document.create(
                idService.generateId(),
                version1.getId(),
                "Doc for 3.1.0",
                "/docs/doc1.md",
                "Content 1",
                "hash1",
                "markdown"
        );
        Document doc2 = Document.create(
                idService.generateId(),
                version2.getId(),
                "Doc for 3.2.0",
                "/docs/doc2.md",
                "Content 2",
                "hash2",
                "markdown"
        );
        documentRepository.saveAll(List.of(doc1, doc2));

        // When & Then - 應統計所有版本的文件
        client.get().uri("/api/dashboard/stats")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.libraryCount").isEqualTo(1)
                .jsonPath("$.documentCount").isEqualTo(2);
    }
}
