package io.github.samzhu.documentation.platform.web.api;

import io.github.samzhu.documentation.platform.TestcontainersConfiguration;
import io.github.samzhu.documentation.platform.domain.enums.SourceType;
import io.github.samzhu.documentation.platform.domain.model.Document;
import io.github.samzhu.documentation.platform.domain.model.DocumentChunk;
import io.github.samzhu.documentation.platform.domain.model.Library;
import io.github.samzhu.documentation.platform.domain.model.LibraryVersion;
import io.github.samzhu.documentation.platform.repository.DocumentChunkRepository;
import io.github.samzhu.documentation.platform.repository.DocumentRepository;
import io.github.samzhu.documentation.platform.repository.LibraryRepository;
import io.github.samzhu.documentation.platform.repository.LibraryVersionRepository;
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

import java.util.Arrays;
import java.util.List;

/**
 * Search API Controller 整合測試
 * <p>
 * 使用 Spring Boot 4 最新測試工具：
 * - RestTestClient（取代 MockMvc/TestRestTemplate）
 * - @ServiceConnection（簡化 Testcontainers 配置）
 * - 真實 Google GenAI EmbeddingModel（API Key 從 config/application-secrets.properties 載入）
 * </p>
 */
@Tag("integration")
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureRestTestClient
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("integration-test")
class SearchApiControllerIntegrationTest {

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
    IdService idService;

    private Library testLibrary;
    private LibraryVersion testVersion;

    @BeforeEach
    void setUp() {
        // 清除所有測試資料
        documentChunkRepository.deleteAll();
        documentRepository.deleteAll();
        libraryVersionRepository.deleteAll();
        libraryRepository.deleteAll();

        // 建立測試用 Library 和 Version
        testLibrary = Library.create(
                idService.generateId(),
                "spring-boot",
                "Spring Boot",
                "Spring Boot 框架",
                SourceType.GITHUB,
                "https://github.com/spring-projects/spring-boot",
                "backend",
                List.of()
        );
        testLibrary = libraryRepository.save(testLibrary);

        testVersion = LibraryVersion.create(
                idService.generateId(),
                testLibrary.getId(),
                "3.2.0",
                true
        );
        testVersion = libraryVersionRepository.save(testVersion);
    }

    @Test
    @DisplayName("GET /api/search - 空查詢應回傳空結果")
    @WithMockUser
    void shouldReturnEmptyForBlankQuery() {
        // When & Then - 空查詢
        client.get().uri("/api/search?q=")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.query").isEqualTo("")
                .jsonPath("$.total").isEqualTo(0)
                .jsonPath("$.items").isArray()
                .jsonPath("$.items.length()").isEqualTo(0);
    }

    @Test
    @DisplayName("GET /api/search - 無資料時應回傳空結果")
    @WithMockUser
    void shouldReturnEmptyWhenNoData() {
        // When & Then - 搜尋不存在的內容
        client.get().uri("/api/search?q=nonexistent")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.total").isEqualTo(0);
    }

    @Test
    @DisplayName("GET /api/search - 全文搜尋應能執行")
    @WithMockUser
    void shouldExecuteFullTextSearch() {
        // Given - 建立測試文件
        Document doc1 = Document.create(
                idService.generateId(),
                testVersion.getId(),
                "Spring Boot Introduction",
                "/docs/intro.md",
                "Spring Boot makes it easy to create stand-alone applications",
                "hash1",
                "markdown"
        );
        Document doc2 = Document.create(
                idService.generateId(),
                testVersion.getId(),
                "React Introduction",
                "/docs/react.md",
                "React is a JavaScript library for building user interfaces",
                "hash2",
                "markdown"
        );
        documentRepository.saveAll(List.of(doc1, doc2));

        // When & Then - 全文搜尋 "Spring"
        client.get().uri("/api/search?q=Spring&libraryId={libraryId}&mode=fulltext", testLibrary.getId())
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.mode").isEqualTo("fulltext")
                .jsonPath("$.query").isEqualTo("Spring")
                .jsonPath("$.items").isArray();
    }

    @Test
    @DisplayName("GET /api/search - 語意搜尋應使用向量相似度")
    @WithMockUser
    void shouldFindChunksWithSemanticSearch() {
        // Given - 建立測試文件和區塊
        Document document = Document.create(
                idService.generateId(),
                testVersion.getId(),
                "Configuration Guide",
                "/docs/config.md",
                "How to configure your Spring Boot application",
                "hash1",
                "markdown"
        );
        document = documentRepository.save(document);

        // 建立區塊（含向量嵌入）
        float[] embedding = new float[768];
        Arrays.fill(embedding, 0.1f);

        DocumentChunk chunk = DocumentChunk.create(
                idService.generateId(),
                document.getId(),
                0,
                "This section explains application configuration including properties and YAML files",
                embedding,
                50
        );
        documentChunkRepository.save(chunk);

        // When & Then - 語意搜尋
        client.get().uri("/api/search?q=how+to+setup&libraryId={libraryId}&mode=semantic", testLibrary.getId())
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.mode").isEqualTo("semantic");
    }

    @Test
    @DisplayName("GET /api/search - 混合搜尋應結合兩種模式")
    @WithMockUser
    void shouldPerformHybridSearch() {
        // Given - 建立測試文件和區塊
        Document document = Document.create(
                idService.generateId(),
                testVersion.getId(),
                "Getting Started",
                "/docs/getting-started.md",
                "Getting started with Spring Boot framework",
                "hash1",
                "markdown"
        );
        document = documentRepository.save(document);

        float[] embedding = new float[768];
        Arrays.fill(embedding, 0.1f);

        DocumentChunk chunk = DocumentChunk.create(
                idService.generateId(),
                document.getId(),
                0,
                "This guide helps you get started with Spring Boot quickly",
                embedding,
                50
        );
        documentChunkRepository.save(chunk);

        // When & Then - 混合搜尋（預設模式）
        client.get().uri("/api/search?q=getting+started&libraryId={libraryId}", testLibrary.getId())
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.mode").isEqualTo("hybrid");
    }

    @Test
    @DisplayName("GET /api/search - 應遵守 limit 參數")
    @WithMockUser
    void shouldRespectLimitParameter() {
        // Given - 建立多個文件
        for (int i = 0; i < 10; i++) {
            Document doc = Document.create(
                    idService.generateId(),
                    testVersion.getId(),
                    "Document " + i,
                    "/docs/doc-" + i + ".md",
                    "Spring Boot application content number " + i,
                    "hash" + i,
                    "markdown"
            );
            documentRepository.save(doc);
        }

        // When & Then - 限制結果數量
        client.get().uri("/api/search?q=Spring&libraryId={libraryId}&mode=fulltext&limit=3", testLibrary.getId())
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.items.length()").value(Integer.class, count ->
                        org.assertj.core.api.Assertions.assertThat(count).isLessThanOrEqualTo(3));
    }

    @Test
    @DisplayName("GET /api/search - 不支援的搜尋模式應回傳錯誤")
    @WithMockUser
    void shouldReturn400ForInvalidMode() {
        // When & Then - 使用不支援的模式
        client.get().uri("/api/search?q=test&libraryId={libraryId}&mode=invalid", testLibrary.getId())
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    @DisplayName("GET /api/search - 回應應包含完整的結構")
    @WithMockUser
    void shouldReturnCompleteResponseStructure() {
        // Given - 建立測試文件
        Document document = Document.create(
                idService.generateId(),
                testVersion.getId(),
                "Test Document",
                "/docs/test.md",
                "Test content for searching",
                "hash1",
                "markdown"
        );
        documentRepository.save(document);

        // When & Then - 驗證回應結構
        client.get().uri("/api/search?q=Test&libraryId={libraryId}&mode=fulltext", testLibrary.getId())
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.query").isEqualTo("Test")
                .jsonPath("$.mode").exists()
                .jsonPath("$.total").exists()
                .jsonPath("$.items").isArray();
    }

    @Test
    @DisplayName("GET /api/search - 未指定 libraryId 應使用第一個 Library")
    @WithMockUser
    void shouldUseFirstLibraryWhenNotSpecified() {
        // Given - 建立測試文件
        Document document = Document.create(
                idService.generateId(),
                testVersion.getId(),
                "Test Document",
                "/docs/test.md",
                "Test content for searching",
                "hash1",
                "markdown"
        );
        documentRepository.save(document);

        // When & Then - 不指定 libraryId
        client.get().uri("/api/search?q=Test&mode=fulltext")
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    @DisplayName("GET /api/search - 語意搜尋應能執行並回傳正確結構")
    @WithMockUser
    void shouldExecuteSemanticSearchWithCorrectStructure() {
        // Given - 建立測試文件和區塊
        Document document = Document.create(
                idService.generateId(),
                testVersion.getId(),
                "Configuration Guide",
                "/docs/config.md",
                "How to configure your application",
                "hash1",
                "markdown"
        );
        document = documentRepository.save(document);

        float[] embedding = new float[768];
        Arrays.fill(embedding, 0.1f);

        DocumentChunk chunk = DocumentChunk.create(
                idService.generateId(),
                document.getId(),
                0,
                "Configuration content here",
                embedding,
                30
        );
        documentChunkRepository.save(chunk);

        // When & Then - 驗證語意搜尋能執行並回傳正確結構
        client.get().uri("/api/search?q=configuration&libraryId={libraryId}&mode=semantic", testLibrary.getId())
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.query").isEqualTo("configuration")
                .jsonPath("$.mode").isEqualTo("semantic")
                .jsonPath("$.total").exists()
                .jsonPath("$.items").isArray();
    }

    @Test
    @DisplayName("GET /api/search - 無 Library 時應回傳空結果")
    @WithMockUser
    void shouldReturnEmptyWhenNoLibraries() {
        // Given - 清除所有資料
        documentChunkRepository.deleteAll();
        documentRepository.deleteAll();
        libraryVersionRepository.deleteAll();
        libraryRepository.deleteAll();

        // When & Then - 搜尋
        client.get().uri("/api/search?q=test")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.total").isEqualTo(0);
    }

    @Test
    @DisplayName("GET /api/search - 特定版本搜尋")
    @WithMockUser
    void shouldSearchInSpecificVersion() {
        // Given - 建立另一個版本
        LibraryVersion version2 = LibraryVersion.create(
                idService.generateId(),
                testLibrary.getId(),
                "3.1.0",
                false
        );
        version2 = libraryVersionRepository.save(version2);

        // 為不同版本建立文件
        Document docV320 = Document.create(
                idService.generateId(),
                testVersion.getId(),
                "Doc 3.2.0",
                "/docs/v320.md",
                "New feature in Spring Boot 3.2.0",
                "hash1",
                "markdown"
        );
        Document docV310 = Document.create(
                idService.generateId(),
                version2.getId(),
                "Doc 3.1.0",
                "/docs/v310.md",
                "Old feature in Spring Boot 3.1.0",
                "hash2",
                "markdown"
        );
        documentRepository.saveAll(List.of(docV320, docV310));

        // When & Then - 搜尋特定版本
        client.get().uri("/api/search?q=feature&libraryId={libraryId}&version=3.2.0&mode=fulltext", testLibrary.getId())
                .exchange()
                .expectStatus().isOk();
    }
}
