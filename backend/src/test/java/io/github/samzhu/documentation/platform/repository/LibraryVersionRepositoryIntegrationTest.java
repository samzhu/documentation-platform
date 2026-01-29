package io.github.samzhu.documentation.platform.repository;

import io.github.samzhu.documentation.platform.TestcontainersConfiguration;
import io.github.samzhu.documentation.platform.domain.enums.SourceType;
import io.github.samzhu.documentation.platform.domain.enums.VersionStatus;
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
 * LibraryVersion Repository 整合測試
 * <p>
 * 使用 Testcontainers 啟動真實 PostgreSQL 資料庫進行測試。
 * 驗證版本查詢、LTS 版本、最新版本等功能。
 * </p>
 */
@Tag("integration")
@SpringBootTest
@Import(TestcontainersConfiguration.class)
@Transactional
class LibraryVersionRepositoryIntegrationTest {

    @MockitoBean
    EmbeddingModel embeddingModel;

    @Autowired
    LibraryVersionRepository libraryVersionRepository;

    @Autowired
    LibraryRepository libraryRepository;

    @Autowired
    IdService idService;

    private Library testLibrary;

    @BeforeEach
    void setUp() {
        // 清除所有測試資料
        libraryVersionRepository.deleteAll();
        libraryRepository.deleteAll();

        // 建立測試用 Library
        testLibrary = Library.create(
                idService.generateId(),
                "spring-boot",
                "Spring Boot",
                "Spring Boot 框架",
                SourceType.GITHUB,
                "https://github.com/spring-projects/spring-boot",
                "backend",
                List.of("java")
        );
        testLibrary = libraryRepository.save(testLibrary);
    }

    @Test
    @DisplayName("應能儲存並查詢 LibraryVersion")
    void shouldSaveAndFindVersion() {
        // Given - 準備版本資料
        LibraryVersion version = LibraryVersion.create(
                idService.generateId(),
                testLibrary.getId(),
                "3.2.0",
                true
        );

        // When - 儲存
        LibraryVersion saved = libraryVersionRepository.save(version);

        // Then - 驗證儲存結果
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getLibraryId()).isEqualTo(testLibrary.getId());
        assertThat(saved.getVersion()).isEqualTo("3.2.0");
        assertThat(saved.getIsLatest()).isTrue();
        assertThat(saved.getIsLts()).isFalse();
        assertThat(saved.getStatus()).isEqualTo(VersionStatus.ACTIVE);
    }

    @Test
    @DisplayName("應能查詢指定 Library 的最新版本")
    void shouldFindLatestByLibraryId() {
        // Given - 建立多個版本，其中一個標記為 latest
        LibraryVersion oldVersion = LibraryVersion.create(
                idService.generateId(),
                testLibrary.getId(),
                "3.1.0",
                false
        );
        LibraryVersion latestVersion = LibraryVersion.create(
                idService.generateId(),
                testLibrary.getId(),
                "3.2.0",
                true
        );

        libraryVersionRepository.saveAll(List.of(oldVersion, latestVersion));

        // When - 查詢最新版本
        Optional<LibraryVersion> found = libraryVersionRepository.findLatestByLibraryId(testLibrary.getId());

        // Then - 應找到標記為 latest 的版本
        assertThat(found).isPresent();
        assertThat(found.get().getVersion()).isEqualTo("3.2.0");
        assertThat(found.get().getIsLatest()).isTrue();
    }

    @Test
    @DisplayName("若無最新版本標記應回傳空")
    void shouldReturnEmptyWhenNoLatestVersion() {
        // Given - 建立沒有標記為 latest 的版本
        LibraryVersion version = LibraryVersion.create(
                idService.generateId(),
                testLibrary.getId(),
                "3.1.0",
                false
        );
        libraryVersionRepository.save(version);

        // When - 查詢最新版本
        Optional<LibraryVersion> found = libraryVersionRepository.findLatestByLibraryId(testLibrary.getId());

        // Then - 應為空
        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("應能依 Library ID 和版本號查詢")
    void shouldFindByLibraryIdAndVersion() {
        // Given - 建立版本
        LibraryVersion version = LibraryVersion.create(
                idService.generateId(),
                testLibrary.getId(),
                "3.2.0",
                true
        );
        libraryVersionRepository.save(version);

        // When - 依 Library ID 和版本號查詢
        Optional<LibraryVersion> found = libraryVersionRepository.findByLibraryIdAndVersion(
                testLibrary.getId(),
                "3.2.0"
        );

        // Then - 應找到對應版本
        assertThat(found).isPresent();
        assertThat(found.get().getVersion()).isEqualTo("3.2.0");
    }

    @Test
    @DisplayName("查詢不存在的版本號應回傳空")
    void shouldReturnEmptyWhenVersionNotFound() {
        // When - 查詢不存在的版本
        Optional<LibraryVersion> found = libraryVersionRepository.findByLibraryIdAndVersion(
                testLibrary.getId(),
                "9.9.9"
        );

        // Then - 應為空
        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("應能查詢指定 Library 的所有版本")
    void shouldFindByLibraryId() {
        // Given - 建立多個版本
        LibraryVersion v1 = LibraryVersion.create(
                idService.generateId(),
                testLibrary.getId(),
                "3.0.0",
                false
        );
        LibraryVersion v2 = LibraryVersion.create(
                idService.generateId(),
                testLibrary.getId(),
                "3.1.0",
                false
        );
        LibraryVersion v3 = LibraryVersion.create(
                idService.generateId(),
                testLibrary.getId(),
                "3.2.0",
                true
        );

        libraryVersionRepository.saveAll(List.of(v1, v2, v3));

        // When - 查詢該 Library 的所有版本
        List<LibraryVersion> versions = libraryVersionRepository.findByLibraryId(testLibrary.getId());

        // Then - 應找到 3 個版本
        assertThat(versions).hasSize(3);
        assertThat(versions)
                .extracting(LibraryVersion::getVersion)
                .containsExactlyInAnyOrder("3.0.0", "3.1.0", "3.2.0");
    }

    @Test
    @DisplayName("應能查詢 LTS 版本")
    void shouldFindLtsByLibraryId() {
        // Given - 建立一般版本和 LTS 版本
        LibraryVersion normalVersion = LibraryVersion.create(
                idService.generateId(),
                testLibrary.getId(),
                "3.2.0",
                true
        );
        LibraryVersion ltsVersion = LibraryVersion.createLts(
                idService.generateId(),
                testLibrary.getId(),
                "3.1.0",
                false
        );

        libraryVersionRepository.saveAll(List.of(normalVersion, ltsVersion));

        // When - 查詢 LTS 版本
        Optional<LibraryVersion> found = libraryVersionRepository.findLtsByLibraryId(testLibrary.getId());

        // Then - 應找到 LTS 版本
        assertThat(found).isPresent();
        assertThat(found.get().getVersion()).isEqualTo("3.1.0");
        assertThat(found.get().getIsLts()).isTrue();
    }

    @Test
    @DisplayName("若無 LTS 版本應回傳空")
    void shouldReturnEmptyWhenNoLtsVersion() {
        // Given - 只建立一般版本
        LibraryVersion normalVersion = LibraryVersion.create(
                idService.generateId(),
                testLibrary.getId(),
                "3.2.0",
                true
        );
        libraryVersionRepository.save(normalVersion);

        // When - 查詢 LTS 版本
        Optional<LibraryVersion> found = libraryVersionRepository.findLtsByLibraryId(testLibrary.getId());

        // Then - 應為空
        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("應能查詢所有 LTS 版本")
    void shouldFindAllLts() {
        // Given - 建立另一個 Library
        Library anotherLibrary = Library.create(
                idService.generateId(),
                "quarkus",
                "Quarkus",
                "Quarkus 框架",
                SourceType.GITHUB,
                "https://github.com/quarkusio/quarkus",
                "backend",
                List.of()
        );
        anotherLibrary = libraryRepository.save(anotherLibrary);

        // 建立各種版本
        LibraryVersion springLts = LibraryVersion.createLts(
                idService.generateId(),
                testLibrary.getId(),
                "3.1.0",
                false
        );
        LibraryVersion springNormal = LibraryVersion.create(
                idService.generateId(),
                testLibrary.getId(),
                "3.2.0",
                true
        );
        LibraryVersion quarkusLts = LibraryVersion.createLts(
                idService.generateId(),
                anotherLibrary.getId(),
                "3.2.0",
                false
        );

        libraryVersionRepository.saveAll(List.of(springLts, springNormal, quarkusLts));

        // When - 查詢所有 LTS 版本
        List<LibraryVersion> ltsVersions = libraryVersionRepository.findAllLts();

        // Then - 應找到 2 個 LTS 版本
        assertThat(ltsVersions).hasSize(2);
        assertThat(ltsVersions).allMatch(LibraryVersion::getIsLts);
    }

    @Test
    @DisplayName("應能依狀態查詢版本")
    void shouldFindByLibraryIdAndStatus() {
        // Given - 建立不同狀態的版本
        LibraryVersion activeVersion = LibraryVersion.create(
                idService.generateId(),
                testLibrary.getId(),
                "3.2.0",
                true
        );

        // 注意：需要使用建構子來建立 DEPRECATED 狀態的版本
        LibraryVersion deprecatedVersion = new LibraryVersion(
                idService.generateId(),
                testLibrary.getId(),
                "2.7.0",
                false,
                false,
                VersionStatus.DEPRECATED,
                null,
                null,
                null,
                null,
                null
        );

        libraryVersionRepository.saveAll(List.of(activeVersion, deprecatedVersion));

        // When - 查詢 ACTIVE 狀態的版本
        List<LibraryVersion> activeVersions = libraryVersionRepository.findByLibraryIdAndStatus(
                testLibrary.getId(),
                VersionStatus.ACTIVE
        );

        // Then - 應只找到 1 個 ACTIVE 版本
        assertThat(activeVersions).hasSize(1);
        assertThat(activeVersions.getFirst().getVersion()).isEqualTo("3.2.0");
    }

    @Test
    @DisplayName("儲存後應自動設定樂觀鎖版本號")
    void shouldSetVersionOnSave() {
        // Given - 準備版本資料
        LibraryVersion version = LibraryVersion.create(
                idService.generateId(),
                testLibrary.getId(),
                "3.2.0",
                true
        );

        // entityVersion 初始為 null（新實體）
        assertThat(version.getEntityVersion()).isNull();

        // When - 儲存
        LibraryVersion saved = libraryVersionRepository.save(version);

        // Then - 樂觀鎖版本號應由 Spring Data JDBC 自動設定
        assertThat(saved.getEntityVersion()).isNotNull();
    }
}
