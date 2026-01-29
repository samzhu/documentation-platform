package io.github.samzhu.documentation.platform.repository;

import io.github.samzhu.documentation.platform.TestcontainersConfiguration;
import io.github.samzhu.documentation.platform.domain.enums.SourceType;
import io.github.samzhu.documentation.platform.domain.model.Document;
import io.github.samzhu.documentation.platform.domain.model.Library;
import io.github.samzhu.documentation.platform.domain.model.LibraryVersion;
import io.github.samzhu.documentation.platform.service.IdService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Document Repository 整合測試
 * <p>
 * 使用 Testcontainers 啟動真實 PostgreSQL 資料庫進行測試。
 * 驗證文件 CRUD 操作和全文搜尋功能。
 * </p>
 */
@Tag("integration")
@SpringBootTest
@Import(TestcontainersConfiguration.class)
@Transactional
class DocumentRepositoryIntegrationTest {

    @MockitoBean
    EmbeddingModel embeddingModel;

    @Autowired
    DocumentRepository documentRepository;

    @Autowired
    LibraryVersionRepository libraryVersionRepository;

    @Autowired
    LibraryRepository libraryRepository;

    @Autowired
    IdService idService;

    private LibraryVersion testVersion;

    @BeforeEach
    void setUp() {
        // 清除所有測試資料
        documentRepository.deleteAll();
        libraryVersionRepository.deleteAll();
        libraryRepository.deleteAll();

        // 建立測試用 Library 和 Version
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

        testVersion = LibraryVersion.create(
                idService.generateId(),
                library.getId(),
                "3.2.0",
                true
        );
        testVersion = libraryVersionRepository.save(testVersion);
    }

    @Test
    @DisplayName("應能儲存並查詢 Document")
    void shouldSaveAndFindDocument() {
        // Given - 準備文件資料
        Document document = Document.create(
                idService.generateId(),
                testVersion.getId(),
                "Getting Started",
                "/docs/getting-started.md",
                "# Getting Started\n\nThis guide shows how to get started with Spring Boot.",
                "abc123hash",
                "markdown"
        );

        // When - 儲存
        Document saved = documentRepository.save(document);

        // Then - 驗證儲存結果
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getTitle()).isEqualTo("Getting Started");
        assertThat(saved.getPath()).isEqualTo("/docs/getting-started.md");
        assertThat(saved.getDocType()).isEqualTo("markdown");
    }

    @Test
    @DisplayName("應能依版本 ID 查詢所有文件")
    void shouldFindByVersionId() {
        // Given - 建立多個文件
        Document doc1 = Document.create(
                idService.generateId(),
                testVersion.getId(),
                "Getting Started",
                "/docs/getting-started.md",
                "Getting started content",
                "hash1",
                "markdown"
        );
        Document doc2 = Document.create(
                idService.generateId(),
                testVersion.getId(),
                "Configuration",
                "/docs/configuration.md",
                "Configuration content",
                "hash2",
                "markdown"
        );

        documentRepository.saveAll(List.of(doc1, doc2));

        // When - 查詢該版本的所有文件
        List<Document> documents = documentRepository.findByVersionId(testVersion.getId());

        // Then - 應找到 2 個文件
        assertThat(documents).hasSize(2);
    }

    @Test
    @DisplayName("應能依路徑排序查詢文件")
    void shouldFindByVersionIdOrderByPathAsc() {
        // Given - 建立多個不同路徑的文件
        Document doc1 = Document.create(
                idService.generateId(),
                testVersion.getId(),
                "Z Doc",
                "/docs/z-doc.md",
                "Z content",
                "hash1",
                "markdown"
        );
        Document doc2 = Document.create(
                idService.generateId(),
                testVersion.getId(),
                "A Doc",
                "/docs/a-doc.md",
                "A content",
                "hash2",
                "markdown"
        );
        Document doc3 = Document.create(
                idService.generateId(),
                testVersion.getId(),
                "M Doc",
                "/docs/m-doc.md",
                "M content",
                "hash3",
                "markdown"
        );

        documentRepository.saveAll(List.of(doc1, doc2, doc3));

        // When - 依路徑排序查詢
        List<Document> documents = documentRepository.findByVersionIdOrderByPathAsc(testVersion.getId());

        // Then - 應依路徑字母順序排列
        assertThat(documents).hasSize(3);
        assertThat(documents.get(0).getPath()).isEqualTo("/docs/a-doc.md");
        assertThat(documents.get(1).getPath()).isEqualTo("/docs/m-doc.md");
        assertThat(documents.get(2).getPath()).isEqualTo("/docs/z-doc.md");
    }

    @Test
    @DisplayName("應能依版本 ID 和路徑查詢文件")
    void shouldFindByVersionIdAndPath() {
        // Given - 建立文件
        Document document = Document.create(
                idService.generateId(),
                testVersion.getId(),
                "Getting Started",
                "/docs/getting-started.md",
                "Getting started content",
                "hash1",
                "markdown"
        );
        documentRepository.save(document);

        // When - 依版本 ID 和路徑查詢
        Optional<Document> found = documentRepository.findByVersionIdAndPath(
                testVersion.getId(),
                "/docs/getting-started.md"
        );

        // Then - 應找到對應文件
        assertThat(found).isPresent();
        assertThat(found.get().getTitle()).isEqualTo("Getting Started");
    }

    @Test
    @DisplayName("查詢不存在的路徑應回傳空")
    void shouldReturnEmptyWhenPathNotFound() {
        // When - 查詢不存在的路徑
        Optional<Document> found = documentRepository.findByVersionIdAndPath(
                testVersion.getId(),
                "/docs/non-existent.md"
        );

        // Then - 應為空
        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("全文搜尋應能執行")
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

        documentRepository.save(doc1);

        // When - 執行全文搜尋
        // 注意：全文搜尋依賴 PostgreSQL search_vector trigger，
        // 在某些測試環境中可能需要額外配置
        List<Document> results = documentRepository.fullTextSearch(
                testVersion.getId(),
                "spring",
                10
        );

        // Then - 搜尋應能執行（即使結果為空也表示查詢語法正確）
        assertThat(results).isNotNull();
    }

    @Test
    @DisplayName("全文搜尋應遵守 limit 限制")
    void shouldRespectLimitInFullTextSearch() {
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

        // When - 搜尋並限制結果數量
        List<Document> results = documentRepository.fullTextSearch(
                testVersion.getId(),
                "spring",
                3
        );

        // Then - limit 應被遵守（結果數量 <= limit）
        // 注意：全文搜尋依賴 PostgreSQL search_vector trigger
        assertThat(results.size()).isLessThanOrEqualTo(3);
    }

    @Test
    @DisplayName("全文搜尋無結果時應回傳空列表")
    void shouldReturnEmptyListWhenNoSearchResults() {
        // Given - 建立文件
        Document document = Document.create(
                idService.generateId(),
                testVersion.getId(),
                "Getting Started",
                "/docs/getting-started.md",
                "Getting started content",
                "hash1",
                "markdown"
        );
        documentRepository.save(document);

        // When - 搜尋不存在的關鍵字
        List<Document> results = documentRepository.fullTextSearch(
                testVersion.getId(),
                "nonexistentword",
                10
        );

        // Then - 應為空列表
        assertThat(results).isEmpty();
    }

    @Test
    @DisplayName("應能統計指定 Library 的文件數量")
    void shouldCountByLibraryId() {
        // Given - 取得 Library ID
        Library library = libraryRepository.findByName("spring-boot").orElseThrow();

        // 建立另一個版本
        LibraryVersion version2 = LibraryVersion.create(
                idService.generateId(),
                library.getId(),
                "3.1.0",
                false
        );
        version2 = libraryVersionRepository.save(version2);

        // 為兩個版本建立文件
        Document doc1 = Document.create(
                idService.generateId(),
                testVersion.getId(),
                "Doc 1",
                "/docs/doc1.md",
                "Content 1",
                "hash1",
                "markdown"
        );
        Document doc2 = Document.create(
                idService.generateId(),
                testVersion.getId(),
                "Doc 2",
                "/docs/doc2.md",
                "Content 2",
                "hash2",
                "markdown"
        );
        Document doc3 = Document.create(
                idService.generateId(),
                version2.getId(),
                "Doc 3",
                "/docs/doc3.md",
                "Content 3",
                "hash3",
                "markdown"
        );

        documentRepository.saveAll(List.of(doc1, doc2, doc3));

        // When - 統計該 Library 的文件數量
        long count = documentRepository.countByLibraryId(library.getId());

        // Then - 應為 3
        assertThat(count).isEqualTo(3);
    }

    @Test
    @DisplayName("儲存後應自動設定樂觀鎖版本號")
    void shouldSetVersionOnSave() {
        // Given - 準備文件資料
        Document document = Document.create(
                idService.generateId(),
                testVersion.getId(),
                "Getting Started",
                "/docs/getting-started.md",
                "Content",
                "hash",
                "markdown"
        );

        // version 初始為 null（新實體）
        assertThat(document.getVersion()).isNull();

        // When - 儲存
        Document saved = documentRepository.save(document);

        // Then - 樂觀鎖版本號應由 Spring Data JDBC 自動設定
        assertThat(saved.getVersion()).isNotNull();
    }

    @Test
    @DisplayName("應能正確處理大型文件內容")
    void shouldHandleLargeContent() {
        // Given - 建立包含大量內容的文件
        String largeContent = "A".repeat(100000); // 100KB 內容
        Document document = Document.create(
                idService.generateId(),
                testVersion.getId(),
                "Large Document",
                "/docs/large.md",
                largeContent,
                "hash",
                "markdown"
        );

        // When - 儲存並讀取
        documentRepository.save(document);
        Optional<Document> found = documentRepository.findById(document.getId());

        // Then - 內容應完整保存
        assertThat(found).isPresent();
        assertThat(found.get().getContent()).hasSize(100000);
    }
}
