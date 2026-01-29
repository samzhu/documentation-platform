package io.github.samzhu.documentation.platform.repository;

import io.github.samzhu.documentation.platform.TestcontainersConfiguration;
import io.github.samzhu.documentation.platform.domain.enums.ApiKeyStatus;
import io.github.samzhu.documentation.platform.domain.model.ApiKey;
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

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ApiKey Repository 整合測試
 * <p>
 * 使用 Testcontainers 啟動真實 PostgreSQL 資料庫進行測試。
 * 驗證 API Key 的查詢、狀態篩選等功能。
 * </p>
 */
@Tag("integration")
@SpringBootTest
@Import(TestcontainersConfiguration.class)
@Transactional
class ApiKeyRepositoryIntegrationTest {

    @MockitoBean
    EmbeddingModel embeddingModel;

    @Autowired
    ApiKeyRepository apiKeyRepository;

    @Autowired
    IdService idService;

    @BeforeEach
    void setUp() {
        // 清除所有測試資料
        apiKeyRepository.deleteAll();
    }

    @Test
    @DisplayName("應能儲存並查詢 ApiKey")
    void shouldSaveAndFindApiKey() {
        // Given - 準備 API Key 資料
        ApiKey apiKey = ApiKey.create(
                idService.generateId(),
                "Test API Key",
                "$2a$10$TestHashForApiKey",
                "dmcp_abc123",
                1000,
                null,
                "admin"
        );

        // When - 儲存
        ApiKey saved = apiKeyRepository.save(apiKey);

        // Then - 驗證儲存結果
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getName()).isEqualTo("Test API Key");
        assertThat(saved.getKeyPrefix()).isEqualTo("dmcp_abc123");
        assertThat(saved.getStatus()).isEqualTo(ApiKeyStatus.ACTIVE);
        assertThat(saved.getRateLimit()).isEqualTo(1000);
    }

    @Test
    @DisplayName("應能依 Key 前綴查詢 ApiKey")
    void shouldFindByKeyPrefix() {
        // Given - 建立 API Key
        ApiKey apiKey = ApiKey.create(
                idService.generateId(),
                "Production Key",
                "$2a$10$TestHashForApiKey",
                "dmcp_prod12",
                5000,
                null,
                "admin"
        );
        apiKeyRepository.save(apiKey);

        // When - 依前綴查詢
        Optional<ApiKey> found = apiKeyRepository.findByKeyPrefix("dmcp_prod12");

        // Then - 應找到對應的 API Key
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("Production Key");
        assertThat(found.get().getRateLimit()).isEqualTo(5000);
    }

    @Test
    @DisplayName("查詢不存在的 Key 前綴應回傳空")
    void shouldReturnEmptyWhenKeyPrefixNotFound() {
        // When - 查詢不存在的前綴
        Optional<ApiKey> found = apiKeyRepository.findByKeyPrefix("dmcp_nonexist");

        // Then - 應為空
        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("應能依狀態查詢 ApiKey 列表")
    void shouldFindByStatus() {
        // Given - 建立不同狀態的 API Key
        ApiKey activeKey1 = ApiKey.create(
                idService.generateId(),
                "Active Key 1",
                "$2a$10$TestHash1",
                "dmcp_active1",
                1000,
                null,
                "admin"
        );
        ApiKey activeKey2 = ApiKey.create(
                idService.generateId(),
                "Active Key 2",
                "$2a$10$TestHash2",
                "dmcp_active2",
                1000,
                null,
                "admin"
        );

        // 建立已撤銷的 Key
        ApiKey revokedKey = new ApiKey(
                idService.generateId(),
                "Revoked Key",
                "$2a$10$TestHash3",
                "dmcp_revoke",
                ApiKeyStatus.REVOKED,
                1000,
                null,
                null,
                "admin",
                null,
                null,
                null
        );

        apiKeyRepository.saveAll(List.of(activeKey1, activeKey2, revokedKey));

        // When - 查詢 ACTIVE 狀態的 Key
        List<ApiKey> activeKeys = apiKeyRepository.findByStatus(ApiKeyStatus.ACTIVE);

        // Then - 應找到 2 個 ACTIVE Key
        assertThat(activeKeys).hasSize(2);
        assertThat(activeKeys).allMatch(key -> key.getStatus() == ApiKeyStatus.ACTIVE);
    }

    @Test
    @DisplayName("應能依名稱查詢 ApiKey")
    void shouldFindByName() {
        // Given - 建立 API Key
        ApiKey apiKey = ApiKey.create(
                idService.generateId(),
                "My Unique Key Name",
                "$2a$10$TestHash",
                "dmcp_unique",
                1000,
                null,
                "admin"
        );
        apiKeyRepository.save(apiKey);

        // When - 依名稱查詢
        Optional<ApiKey> found = apiKeyRepository.findByName("My Unique Key Name");

        // Then - 應找到對應的 API Key
        assertThat(found).isPresent();
        assertThat(found.get().getKeyPrefix()).isEqualTo("dmcp_unique");
    }

    @Test
    @DisplayName("查詢不存在的名稱應回傳空")
    void shouldReturnEmptyWhenNameNotFound() {
        // When - 查詢不存在的名稱
        Optional<ApiKey> found = apiKeyRepository.findByName("Non Existent Name");

        // Then - 應為空
        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("應能查詢所有 ApiKey")
    void shouldFindAllOrderByCreatedAtDesc() {
        // Given - 建立多個 API Key
        ApiKey key1 = ApiKey.create(
                idService.generateId(),
                "First Key",
                "$2a$10$TestHash1",
                "dmcp_first1",
                1000,
                null,
                "admin"
        );
        ApiKey key2 = ApiKey.create(
                idService.generateId(),
                "Second Key",
                "$2a$10$TestHash2",
                "dmcp_second",
                1000,
                null,
                "admin"
        );
        ApiKey key3 = ApiKey.create(
                idService.generateId(),
                "Third Key",
                "$2a$10$TestHash3",
                "dmcp_third1",
                1000,
                null,
                "admin"
        );

        apiKeyRepository.saveAll(List.of(key1, key2, key3));

        // When - 查詢所有
        List<ApiKey> allKeys = apiKeyRepository.findAllOrderByCreatedAtDesc();

        // Then - 應找到 3 個 Key
        assertThat(allKeys).hasSize(3);
        assertThat(allKeys)
                .extracting(ApiKey::getName)
                .containsExactlyInAnyOrder("First Key", "Second Key", "Third Key");
    }

    @Test
    @DisplayName("應能統計指定狀態的 ApiKey 數量")
    void shouldCountByStatus() {
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
        ApiKey activeKey3 = ApiKey.create(
                idService.generateId(),
                "Active Key 3",
                "$2a$10$TestHash3",
                "dmcp_activ3",
                1000,
                null,
                "admin"
        );

        ApiKey revokedKey = new ApiKey(
                idService.generateId(),
                "Revoked Key",
                "$2a$10$TestHash4",
                "dmcp_revok1",
                ApiKeyStatus.REVOKED,
                1000,
                null,
                null,
                "admin",
                null,
                null,
                null
        );

        apiKeyRepository.saveAll(List.of(activeKey1, activeKey2, activeKey3, revokedKey));

        // When - 統計 ACTIVE 狀態的數量
        long activeCount = apiKeyRepository.countByStatus(ApiKeyStatus.ACTIVE);
        long revokedCount = apiKeyRepository.countByStatus(ApiKeyStatus.REVOKED);

        // Then - 應正確統計
        assertThat(activeCount).isEqualTo(3);
        assertThat(revokedCount).isEqualTo(1);
    }

    @Test
    @DisplayName("應能正確儲存和讀取過期時間")
    void shouldSaveAndRetrieveExpiresAt() {
        // Given - 建立帶有過期時間的 API Key
        OffsetDateTime expiresAt = OffsetDateTime.now().plusDays(30);
        ApiKey apiKey = ApiKey.create(
                idService.generateId(),
                "Expiring Key",
                "$2a$10$TestHash",
                "dmcp_expire",
                1000,
                expiresAt,
                "admin"
        );

        // When - 儲存並重新查詢
        apiKeyRepository.save(apiKey);
        Optional<ApiKey> found = apiKeyRepository.findByKeyPrefix("dmcp_expire");

        // Then - 過期時間應正確保存
        assertThat(found).isPresent();
        assertThat(found.get().getExpiresAt()).isNotNull();
        // 比較時間（允許毫秒級誤差）
        assertThat(found.get().getExpiresAt().toEpochSecond())
                .isCloseTo(expiresAt.toEpochSecond(), org.assertj.core.api.Assertions.within(1L));
    }

    @Test
    @DisplayName("撤銷功能應正確更新狀態")
    void shouldRevokeApiKey() {
        // Given - 建立 API Key
        ApiKey apiKey = ApiKey.create(
                idService.generateId(),
                "Key to Revoke",
                "$2a$10$TestHash",
                "dmcp_torevk",
                1000,
                null,
                "admin"
        );
        ApiKey saved = apiKeyRepository.save(apiKey);

        // When - 撤銷
        ApiKey revoked = saved.revoke();
        apiKeyRepository.save(revoked);

        // Then - 狀態應更新為 REVOKED
        Optional<ApiKey> found = apiKeyRepository.findByKeyPrefix("dmcp_torevk");
        assertThat(found).isPresent();
        assertThat(found.get().getStatus()).isEqualTo(ApiKeyStatus.REVOKED);
        assertThat(found.get().isValid()).isFalse();
    }

    @Test
    @DisplayName("儲存後應自動設定樂觀鎖版本號")
    void shouldSetVersionOnSave() {
        // Given - 準備 API Key 資料
        ApiKey apiKey = ApiKey.create(
                idService.generateId(),
                "Test Key",
                "$2a$10$TestHash",
                "dmcp_tstkey",
                1000,
                null,
                "admin"
        );

        // version 初始為 null（新實體）
        assertThat(apiKey.getVersion()).isNull();

        // When - 儲存
        ApiKey saved = apiKeyRepository.save(apiKey);

        // Then - 樂觀鎖版本號應由 Spring Data JDBC 自動設定
        assertThat(saved.getVersion()).isNotNull();
    }

    @Test
    @DisplayName("isExpired 方法應正確判斷過期狀態")
    void shouldCorrectlyDetermineExpiredStatus() {
        // Given - 建立已過期和未過期的 API Key
        ApiKey expiredKey = ApiKey.create(
                idService.generateId(),
                "Expired Key",
                "$2a$10$TestHash1",
                "dmcp_expird",
                1000,
                OffsetDateTime.now().minusDays(1), // 已過期
                "admin"
        );
        ApiKey validKey = ApiKey.create(
                idService.generateId(),
                "Valid Key",
                "$2a$10$TestHash2",
                "dmcp_validk",
                1000,
                OffsetDateTime.now().plusDays(30), // 未過期
                "admin"
        );

        apiKeyRepository.saveAll(List.of(expiredKey, validKey));

        // When - 重新查詢
        Optional<ApiKey> foundExpired = apiKeyRepository.findByKeyPrefix("dmcp_expird");
        Optional<ApiKey> foundValid = apiKeyRepository.findByKeyPrefix("dmcp_validk");

        // Then - 應正確判斷過期狀態
        assertThat(foundExpired).isPresent();
        assertThat(foundExpired.get().isExpired()).isTrue();
        assertThat(foundExpired.get().isValid()).isFalse();

        assertThat(foundValid).isPresent();
        assertThat(foundValid.get().isExpired()).isFalse();
        assertThat(foundValid.get().isValid()).isTrue();
    }
}
