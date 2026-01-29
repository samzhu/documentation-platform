package io.github.samzhu.documentation.platform.web.api;

import io.github.samzhu.documentation.platform.TestcontainersConfiguration;
import io.github.samzhu.documentation.platform.domain.enums.SourceType;
import io.github.samzhu.documentation.platform.domain.model.Library;
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
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.client.RestTestClient;

import java.util.List;


/**
 * Library API Controller 整合測試
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
class LibraryApiControllerIntegrationTest {

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

    @BeforeEach
    void setUp() {
        // 清除所有測試資料（順序重要：先刪除子實體）
        documentChunkRepository.deleteAll();
        documentRepository.deleteAll();
        libraryVersionRepository.deleteAll();
        libraryRepository.deleteAll();
    }

    @Test
    @DisplayName("POST /api/libraries - 應能建立新的 Library")
    @WithMockUser(roles = "ADMIN")
    void shouldCreateLibrary() {
        // Given - 準備建立請求
        String requestBody = """
                {
                    "name": "spring-boot",
                    "displayName": "Spring Boot",
                    "description": "Spring Boot 框架文件",
                    "sourceType": "GITHUB",
                    "sourceUrl": "https://github.com/spring-projects/spring-boot",
                    "category": "backend",
                    "tags": ["java", "framework"]
                }
                """;

        // When & Then - 發送請求並驗證回應
        client.post().uri("/api/libraries")
                .contentType(MediaType.APPLICATION_JSON)
                .body(requestBody)
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.id").exists()
                .jsonPath("$.id").value(String.class, id ->
                        org.assertj.core.api.Assertions.assertThat(id).hasSize(13))  // TSID 長度為 13
                .jsonPath("$.name").isEqualTo("spring-boot")
                .jsonPath("$.displayName").isEqualTo("Spring Boot")
                .jsonPath("$.sourceType").isEqualTo("GITHUB")
                .jsonPath("$.category").isEqualTo("backend")
                .jsonPath("$.tags").isArray()
                .jsonPath("$.tags.length()").isEqualTo(2);
    }

    @Test
    @DisplayName("POST /api/libraries - 名稱格式不符應回傳 400")
    @WithMockUser(roles = "ADMIN")
    void shouldReturn400WhenNameInvalid() {
        // Given - 無效的名稱格式（包含大寫字母）
        String requestBody = """
                {
                    "name": "Spring-Boot",
                    "displayName": "Spring Boot",
                    "sourceType": "GITHUB"
                }
                """;

        // When & Then - 應回傳驗證錯誤
        client.post().uri("/api/libraries")
                .contentType(MediaType.APPLICATION_JSON)
                .body(requestBody)
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    @DisplayName("GET /api/libraries - 應能列出所有 Library")
    @WithMockUser
    void shouldListLibraries() {
        // Given - 預先建立測試資料
        Library lib1 = Library.create(
                idService.generateId(),
                "spring-boot",
                "Spring Boot",
                "Spring Boot 框架",
                SourceType.GITHUB,
                "https://github.com/spring-projects/spring-boot",
                "backend",
                List.of("java")
        );
        Library lib2 = Library.create(
                idService.generateId(),
                "react",
                "React",
                "React 前端框架",
                SourceType.GITHUB,
                "https://github.com/facebook/react",
                "frontend",
                List.of("javascript")
        );
        libraryRepository.saveAll(List.of(lib1, lib2));

        // When & Then - 列出所有
        client.get().uri("/api/libraries")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$").isArray()
                .jsonPath("$.length()").isEqualTo(2);
    }

    @Test
    @DisplayName("GET /api/libraries?category=backend - 應能依分類篩選")
    @WithMockUser
    void shouldFilterByCategory() {
        // Given - 預先建立不同分類的 Library
        Library backendLib = Library.create(
                idService.generateId(),
                "spring-boot",
                "Spring Boot",
                "Spring Boot 框架",
                SourceType.GITHUB,
                "https://github.com/spring-projects/spring-boot",
                "backend",
                List.of()
        );
        Library frontendLib = Library.create(
                idService.generateId(),
                "react",
                "React",
                "React 框架",
                SourceType.GITHUB,
                "https://github.com/facebook/react",
                "frontend",
                List.of()
        );
        libraryRepository.saveAll(List.of(backendLib, frontendLib));

        // When & Then - 篩選 backend 分類
        client.get().uri("/api/libraries?category=backend")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.length()").isEqualTo(1)
                .jsonPath("$[0].name").isEqualTo("spring-boot");
    }

    @Test
    @DisplayName("GET /api/libraries/{id} - 應能取得單一 Library")
    @WithMockUser
    void shouldGetLibraryById() {
        // Given - 預先建立測試資料
        Library library = Library.create(
                idService.generateId(),
                "spring-boot",
                "Spring Boot",
                "Spring Boot 框架",
                SourceType.GITHUB,
                "https://github.com/spring-projects/spring-boot",
                "backend",
                List.of("java", "framework")
        );
        library = libraryRepository.save(library);

        // When & Then - 依 ID 取得
        client.get().uri("/api/libraries/{id}", library.getId())
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.id").isEqualTo(library.getId())
                .jsonPath("$.name").isEqualTo("spring-boot")
                .jsonPath("$.displayName").isEqualTo("Spring Boot")
                .jsonPath("$.documentCount").isEqualTo(0)
                .jsonPath("$.chunkCount").isEqualTo(0);
    }

    @Test
    @DisplayName("GET /api/libraries/{id} - 不存在的 ID 應回傳 404")
    @WithMockUser
    void shouldReturn404WhenLibraryNotFound() {
        // When & Then - 查詢不存在的 ID
        client.get().uri("/api/libraries/{id}", "NONEXISTENT01")
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    @DisplayName("PUT /api/libraries/{id} - 應能更新 Library")
    @WithMockUser(roles = "ADMIN")
    void shouldUpdateLibrary() {
        // Given - 預先建立測試資料
        Library library = Library.create(
                idService.generateId(),
                "spring-boot",
                "Spring Boot",
                "舊的描述",
                SourceType.GITHUB,
                "https://github.com/spring-projects/spring-boot",
                "backend",
                List.of()
        );
        library = libraryRepository.save(library);

        String updateRequest = """
                {
                    "displayName": "Spring Boot (Updated)",
                    "description": "更新後的描述",
                    "category": "framework"
                }
                """;

        // When & Then - 更新並驗證
        client.put().uri("/api/libraries/{id}", library.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .body(updateRequest)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.displayName").isEqualTo("Spring Boot (Updated)")
                .jsonPath("$.description").isEqualTo("更新後的描述")
                .jsonPath("$.category").isEqualTo("framework");
    }

    @Test
    @DisplayName("DELETE /api/libraries/{id} - 應能刪除 Library")
    @WithMockUser(roles = "ADMIN")
    void shouldDeleteLibrary() {
        // Given - 預先建立測試資料
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

        // When - 刪除
        client.delete().uri("/api/libraries/{id}", library.getId())
                .exchange()
                .expectStatus().isNoContent();

        // Then - 確認已刪除
        client.get().uri("/api/libraries/{id}", library.getId())
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    @DisplayName("GET /api/libraries/{id}/versions - 應能列出版本")
    @WithMockUser
    void shouldListVersions() {
        // Given - 預先建立 Library 和版本
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

        // 建立版本
        var version1 = io.github.samzhu.documentation.platform.domain.model.LibraryVersion.create(
                idService.generateId(),
                library.getId(),
                "3.1.0",
                false
        );
        var version2 = io.github.samzhu.documentation.platform.domain.model.LibraryVersion.create(
                idService.generateId(),
                library.getId(),
                "3.2.0",
                true
        );
        libraryVersionRepository.saveAll(List.of(version1, version2));

        // When & Then - 列出版本
        client.get().uri("/api/libraries/{id}/versions", library.getId())
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$").isArray()
                .jsonPath("$.length()").isEqualTo(2);
    }

    @Test
    @DisplayName("POST /api/libraries - 必填欄位缺失應回傳 400")
    @WithMockUser(roles = "ADMIN")
    void shouldReturn400WhenRequiredFieldsMissing() {
        // Given - 缺少必填欄位
        String requestBody = """
                {
                    "name": "spring-boot"
                }
                """;

        // When & Then - 應回傳驗證錯誤
        client.post().uri("/api/libraries")
                .contentType(MediaType.APPLICATION_JSON)
                .body(requestBody)
                .exchange()
                .expectStatus().isBadRequest();
    }

    // 注意：權限測試在 OAuth2 模式下才有意義
    // 開發模式（test profile）允許所有請求，因此省略權限相關測試

    @Test
    @DisplayName("回應應包含 documentCount 和 chunkCount")
    @WithMockUser
    void shouldIncludeCountsInResponse() {
        // Given - 預先建立測試資料
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

        // When & Then - 確認回應包含統計欄位
        client.get().uri("/api/libraries/{id}", library.getId())
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.documentCount").exists()
                .jsonPath("$.chunkCount").exists();
    }
}
