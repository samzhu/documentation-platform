package io.github.samzhu.documentation.platform.repository;

import io.github.samzhu.documentation.platform.TestcontainersConfiguration;
import io.github.samzhu.documentation.platform.domain.enums.SourceType;
import io.github.samzhu.documentation.platform.domain.model.Library;
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
 * Library Repository 整合測試
 * <p>
 * 使用 Testcontainers 啟動真實 PostgreSQL 資料庫進行測試。
 * 驗證所有自訂查詢方法的正確性。
 * </p>
 */
@Tag("integration")
@SpringBootTest
@Import(TestcontainersConfiguration.class)
@Transactional
class LibraryRepositoryIntegrationTest {

    @MockitoBean
    EmbeddingModel embeddingModel;

    @Autowired
    LibraryRepository libraryRepository;

    @Autowired
    IdService idService;

    @BeforeEach
    void setUp() {
        // 清除所有測試資料
        libraryRepository.deleteAll();
    }

    @Test
    @DisplayName("應能儲存並依名稱查詢 Library")
    void shouldSaveAndFindByName() {
        // Given - 準備測試資料
        Library library = Library.create(
                idService.generateId(),
                "spring-boot",
                "Spring Boot",
                "Spring Boot 框架文件",
                SourceType.GITHUB,
                "https://github.com/spring-projects/spring-boot",
                "backend",
                List.of("java", "framework")
        );

        // When - 儲存並查詢
        libraryRepository.save(library);
        Optional<Library> found = libraryRepository.findByName("spring-boot");

        // Then - 驗證結果
        assertThat(found).isPresent();
        assertThat(found.get().getDisplayName()).isEqualTo("Spring Boot");
        assertThat(found.get().getDescription()).isEqualTo("Spring Boot 框架文件");
        assertThat(found.get().getSourceType()).isEqualTo(SourceType.GITHUB);
        assertThat(found.get().getCategory()).isEqualTo("backend");
    }

    @Test
    @DisplayName("查詢不存在的名稱應回傳空")
    void shouldReturnEmptyWhenNameNotFound() {
        // When - 查詢不存在的名稱
        Optional<Library> found = libraryRepository.findByName("non-existent");

        // Then - 應為空
        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("應能依分類查詢 Library 列表")
    void shouldFindByCategory() {
        // Given - 準備多個不同分類的 Library
        Library backendLib1 = Library.create(
                idService.generateId(),
                "spring-boot",
                "Spring Boot",
                "Spring Boot 框架",
                SourceType.GITHUB,
                "https://github.com/spring-projects/spring-boot",
                "backend",
                List.of("java")
        );
        Library backendLib2 = Library.create(
                idService.generateId(),
                "quarkus",
                "Quarkus",
                "Quarkus 框架",
                SourceType.GITHUB,
                "https://github.com/quarkusio/quarkus",
                "backend",
                List.of("java")
        );
        Library frontendLib = Library.create(
                idService.generateId(),
                "react",
                "React",
                "React 前端框架",
                SourceType.GITHUB,
                "https://github.com/facebook/react",
                "frontend",
                List.of("javascript")
        );

        libraryRepository.saveAll(List.of(backendLib1, backendLib2, frontendLib));

        // When - 查詢 backend 分類
        List<Library> backendLibraries = libraryRepository.findByCategory("backend");

        // Then - 應找到 2 個 backend Library
        assertThat(backendLibraries).hasSize(2);
        assertThat(backendLibraries)
                .extracting(Library::getName)
                .containsExactlyInAnyOrder("spring-boot", "quarkus");
    }

    @Test
    @DisplayName("查詢不存在的分類應回傳空列表")
    void shouldReturnEmptyListWhenCategoryNotFound() {
        // Given - 準備一個 Library
        Library library = Library.create(
                idService.generateId(),
                "spring-boot",
                "Spring Boot",
                "Spring Boot 框架",
                SourceType.GITHUB,
                "https://github.com/spring-projects/spring-boot",
                "backend",
                List.of("java")
        );
        libraryRepository.save(library);

        // When - 查詢不存在的分類
        List<Library> found = libraryRepository.findByCategory("devops");

        // Then - 應為空列表
        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("應能依來源類型查詢 Library 列表")
    void shouldFindBySourceType() {
        // Given - 準備不同來源類型的 Library
        Library githubLib = Library.create(
                idService.generateId(),
                "spring-boot",
                "Spring Boot",
                "GitHub 來源",
                SourceType.GITHUB,
                "https://github.com/spring-projects/spring-boot",
                "backend",
                List.of()
        );
        Library localLib = Library.create(
                idService.generateId(),
                "internal-lib",
                "Internal Library",
                "本地來源",
                SourceType.LOCAL,
                "/local/path/to/docs",
                "internal",
                List.of()
        );
        Library manualLib = Library.create(
                idService.generateId(),
                "manual-docs",
                "Manual Docs",
                "手動上傳",
                SourceType.MANUAL,
                null,
                "manual",
                List.of()
        );

        libraryRepository.saveAll(List.of(githubLib, localLib, manualLib));

        // When - 查詢 GITHUB 來源
        List<Library> githubLibraries = libraryRepository.findBySourceType(SourceType.GITHUB);

        // Then - 應只找到 1 個 GITHUB 來源的 Library
        assertThat(githubLibraries).hasSize(1);
        assertThat(githubLibraries.getFirst().getName()).isEqualTo("spring-boot");
    }

    @Test
    @DisplayName("應能查詢所有 Library")
    void shouldFindAll() {
        // Given - 準備多個 Library
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

        libraryRepository.saveAll(List.of(lib1, lib2));

        // When - 查詢所有
        List<Library> all = libraryRepository.findAll();

        // Then - 應找到 2 個 Library
        assertThat(all).hasSize(2);
    }

    @Test
    @DisplayName("儲存後應自動設定樂觀鎖版本號")
    void shouldSetVersionOnSave() {
        // Given - 準備測試資料
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

        // version 初始為 null（新實體）
        assertThat(library.getVersion()).isNull();

        // When - 儲存
        Library saved = libraryRepository.save(library);

        // Then - 樂觀鎖版本號應由 Spring Data JDBC 自動設定
        assertThat(saved.getVersion()).isNotNull();
    }

    @Test
    @DisplayName("tags 應正確儲存和讀取")
    void shouldSaveAndRetrieveTags() {
        // Given - 準備帶有多個 tags 的 Library
        List<String> tags = List.of("java", "framework", "microservices");
        Library library = Library.create(
                idService.generateId(),
                "spring-boot",
                "Spring Boot",
                "Spring Boot 框架",
                SourceType.GITHUB,
                "https://github.com/spring-projects/spring-boot",
                "backend",
                tags
        );

        // When - 儲存並重新查詢
        libraryRepository.save(library);
        Optional<Library> found = libraryRepository.findByName("spring-boot");

        // Then - tags 應正確還原
        assertThat(found).isPresent();
        assertThat(found.get().getTags()).containsExactlyInAnyOrderElementsOf(tags);
    }
}
