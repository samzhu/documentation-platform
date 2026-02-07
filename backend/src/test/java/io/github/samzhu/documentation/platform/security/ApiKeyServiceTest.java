package io.github.samzhu.documentation.platform.security;

import io.github.samzhu.documentation.platform.domain.enums.ApiKeyStatus;
import io.github.samzhu.documentation.platform.domain.model.ApiKey;
import io.github.samzhu.documentation.platform.repository.ApiKeyRepository;
import io.github.samzhu.documentation.platform.service.IdService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * ApiKeyService 單元測試
 * <p>
 * 測試 API Key 服務的生成、驗證、撤銷等功能。
 * </p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ApiKeyService 單元測試")
class ApiKeyServiceTest {

    @Mock
    private IdService idService;

    @Mock
    private ApiKeyRepository apiKeyRepository;

    private final PasswordEncoder passwordEncoder = PasswordEncoderFactories.createDelegatingPasswordEncoder();

    private ApiKeyService apiKeyService;

    @BeforeEach
    void setUp() {
        apiKeyService = new ApiKeyService(idService, apiKeyRepository, passwordEncoder);
    }

    @Test
    @DisplayName("應生成有效的 API Key - 當輸入有效時")
    void shouldGenerateApiKey_whenValidInput() {
        // Given
        String mockId = "0HZXJ8KYPKA9E";
        String name = "test-key";
        String createdBy = "admin";

        when(idService.generateId()).thenReturn(mockId);
        when(apiKeyRepository.findByName(name)).thenReturn(Optional.empty());

        ArgumentCaptor<ApiKey> apiKeyCaptor = ArgumentCaptor.forClass(ApiKey.class);
        when(apiKeyRepository.save(apiKeyCaptor.capture())).thenAnswer(i -> i.getArgument(0));

        // When
        ApiKeyService.GeneratedApiKey result = apiKeyService.generateKey(name, createdBy);

        // Then
        assertThat(result.id()).isEqualTo(mockId);
        assertThat(result.name()).isEqualTo(name);
        assertThat(result.rawKey()).startsWith("dmcp_");
        assertThat(result.keyPrefix()).hasSize(12);
        assertThat(result.keyPrefix()).startsWith("dmcp_");

        ApiKey savedApiKey = apiKeyCaptor.getValue();
        assertThat(savedApiKey.getId()).isEqualTo(mockId);
        assertThat(savedApiKey.getName()).isEqualTo(name);
        assertThat(savedApiKey.getStatus()).isEqualTo(ApiKeyStatus.ACTIVE);
        assertThat(savedApiKey.getRateLimit()).isEqualTo(1000);
    }

    @Test
    @DisplayName("應生成 dmcp_ 前綴的 Key - 當生成 API Key 時")
    void shouldGenerateKeyWithDmcpPrefix_whenGeneratingApiKey() {
        // Given
        when(idService.generateId()).thenReturn("0HZXJ8KYPKA9E");
        when(apiKeyRepository.findByName(anyString())).thenReturn(Optional.empty());
        when(apiKeyRepository.save(any(ApiKey.class))).thenAnswer(i -> i.getArgument(0));

        // When
        ApiKeyService.GeneratedApiKey result = apiKeyService.generateKey("test", "admin");

        // Then
        assertThat(result.rawKey()).startsWith("dmcp_");
        assertThat(result.rawKey().length()).isGreaterThan(12);
    }

    @Test
    @DisplayName("應拋出例外 - 當名稱已存在時")
    void shouldThrowException_whenNameAlreadyExists() {
        // Given
        String existingName = "existing-key";
        ApiKey existingKey = ApiKey.create(
                "0HZXJ8KYPKA9E", existingName, "hash", "dmcp_abc",
                1000, null, "admin"
        );
        when(apiKeyRepository.findByName(existingName)).thenReturn(Optional.of(existingKey));

        // When & Then
        assertThatThrownBy(() -> apiKeyService.generateKey(existingName, "admin"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("API Key 名稱已存在");
    }

    @Test
    @DisplayName("應生成含過期時間的 API Key - 當設定過期時間時")
    void shouldGenerateApiKeyWithExpiration_whenExpirationSet() {
        // Given
        OffsetDateTime expiresAt = OffsetDateTime.now().plusDays(30);
        when(idService.generateId()).thenReturn("0HZXJ8KYPKA9E");
        when(apiKeyRepository.findByName(anyString())).thenReturn(Optional.empty());

        ArgumentCaptor<ApiKey> apiKeyCaptor = ArgumentCaptor.forClass(ApiKey.class);
        when(apiKeyRepository.save(apiKeyCaptor.capture())).thenAnswer(i -> i.getArgument(0));

        // When
        apiKeyService.generateKey("test", "admin", expiresAt, 500);

        // Then
        ApiKey savedApiKey = apiKeyCaptor.getValue();
        assertThat(savedApiKey.getExpiresAt()).isEqualTo(expiresAt);
        assertThat(savedApiKey.getRateLimit()).isEqualTo(500);
    }

    @Test
    @DisplayName("應回傳有效的 API Key - 當 Key 有效時")
    void shouldReturnApiKey_whenKeyIsValid() {
        // Given
        String rawKey = "dmcp_abcdefghijklmnopqrstuvwxyz123456";
        String keyPrefix = rawKey.substring(0, 12);
        // 使用 DelegatingPasswordEncoder 產生正確格式的 hash（含 {bcrypt} 前綴）
        String encodedHash = passwordEncoder.encode(rawKey);

        ApiKey apiKey = ApiKey.create(
                "0HZXJ8KYPKA9E", "test", encodedHash, keyPrefix,
                1000, null, "admin"
        );

        when(apiKeyRepository.findByKeyPrefix(keyPrefix)).thenReturn(Optional.of(apiKey));

        // When
        Optional<ApiKey> result = apiKeyService.validateKey(rawKey);

        // Then - 使用正確的 hash 應驗證成功
        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo("0HZXJ8KYPKA9E");
    }

    @Test
    @DisplayName("應回傳空 Optional - 當 Key 不存在時")
    void shouldReturnEmpty_whenKeyNotFound() {
        // Given
        String rawKey = "dmcp_nonexistent";
        when(apiKeyRepository.findByKeyPrefix(anyString())).thenReturn(Optional.empty());

        // When
        Optional<ApiKey> result = apiKeyService.validateKey(rawKey);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("應回傳空 Optional - 當 Key 格式無效時")
    void shouldReturnEmpty_whenKeyFormatInvalid() {
        // When
        Optional<ApiKey> result1 = apiKeyService.validateKey(null);
        Optional<ApiKey> result2 = apiKeyService.validateKey("invalid_key");
        Optional<ApiKey> result3 = apiKeyService.validateKey("wrong_prefix");

        // Then
        assertThat(result1).isEmpty();
        assertThat(result2).isEmpty();
        assertThat(result3).isEmpty();
    }

    @Test
    @DisplayName("應回傳空 Optional - 當 Key 已撤銷時")
    void shouldReturnEmpty_whenKeyIsRevoked() {
        // Given
        String rawKey = "dmcp_abcdefghijklmnopqrstuvwxyz123456";
        String keyPrefix = rawKey.substring(0, 12);

        ApiKey revokedKey = new ApiKey(
                "0HZXJ8KYPKA9E", "test", "hash", keyPrefix,
                ApiKeyStatus.REVOKED, 1000, null, null,
                "admin", null, null, null
        );

        when(apiKeyRepository.findByKeyPrefix(keyPrefix)).thenReturn(Optional.of(revokedKey));

        // When
        Optional<ApiKey> result = apiKeyService.validateKey(rawKey);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("應回傳空 Optional - 當 Key 已過期時")
    void shouldReturnEmpty_whenKeyIsExpired() {
        // Given
        String rawKey = "dmcp_abcdefghijklmnopqrstuvwxyz123456";
        String keyPrefix = rawKey.substring(0, 12);

        OffsetDateTime pastDate = OffsetDateTime.now().minusDays(1);
        ApiKey expiredKey = ApiKey.create(
                "0HZXJ8KYPKA9E", "test", "hash", keyPrefix,
                1000, pastDate, "admin"
        );

        when(apiKeyRepository.findByKeyPrefix(keyPrefix)).thenReturn(Optional.of(expiredKey));

        // When
        Optional<ApiKey> result = apiKeyService.validateKey(rawKey);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("應更新最後使用時間 - 當呼叫 updateLastUsed 時")
    void shouldUpdateLastUsed_whenCalled() {
        // Given
        ApiKey apiKey = ApiKey.create(
                "0HZXJ8KYPKA9E", "test", "hash", "dmcp_abc",
                1000, null, "admin"
        );

        ArgumentCaptor<ApiKey> apiKeyCaptor = ArgumentCaptor.forClass(ApiKey.class);
        when(apiKeyRepository.save(apiKeyCaptor.capture())).thenAnswer(i -> i.getArgument(0));

        // When
        apiKeyService.updateLastUsed(apiKey);

        // Then
        ApiKey savedApiKey = apiKeyCaptor.getValue();
        assertThat(savedApiKey.getLastUsedAt()).isNotNull();
        assertThat(savedApiKey.getLastUsedAt()).isBeforeOrEqualTo(OffsetDateTime.now());
    }

    @Test
    @DisplayName("應撤銷 API Key - 當呼叫 revokeKey 時")
    void shouldRevokeApiKey_whenRevokeKeyCalled() {
        // Given
        String keyId = "0HZXJ8KYPKA9E";
        ApiKey apiKey = ApiKey.create(
                keyId, "test", "hash", "dmcp_abc",
                1000, null, "admin"
        );

        when(apiKeyRepository.findById(keyId)).thenReturn(Optional.of(apiKey));

        ArgumentCaptor<ApiKey> apiKeyCaptor = ArgumentCaptor.forClass(ApiKey.class);
        when(apiKeyRepository.save(apiKeyCaptor.capture())).thenAnswer(i -> i.getArgument(0));

        // When
        apiKeyService.revokeKey(keyId);

        // Then
        ApiKey savedApiKey = apiKeyCaptor.getValue();
        assertThat(savedApiKey.getStatus()).isEqualTo(ApiKeyStatus.REVOKED);
    }

    @Test
    @DisplayName("應拋出例外 - 當撤銷不存在的 Key 時")
    void shouldThrowException_whenRevokingNonExistentKey() {
        // Given
        String keyId = "nonexistent";
        when(apiKeyRepository.findById(keyId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> apiKeyService.revokeKey(keyId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("找不到 API Key");
    }

    @Test
    @DisplayName("應回傳所有 API Keys - 當呼叫 listKeys 時")
    void shouldReturnAllApiKeys_whenListKeysCalled() {
        // Given
        List<ApiKey> mockKeys = List.of(
                ApiKey.create("id1", "key1", "hash1", "dmcp_abc", 1000, null, "admin"),
                ApiKey.create("id2", "key2", "hash2", "dmcp_def", 1000, null, "admin")
        );
        when(apiKeyRepository.findAllOrderByCreatedAtDesc()).thenReturn(mockKeys);

        // When
        List<ApiKey> result = apiKeyService.listKeys();

        // Then
        assertThat(result).hasSize(2);
    }

    @Test
    @DisplayName("應回傳有效的 API Keys - 當呼叫 listActiveKeys 時")
    void shouldReturnActiveApiKeys_whenListActiveKeysCalled() {
        // Given
        List<ApiKey> activeKeys = List.of(
                ApiKey.create("id1", "key1", "hash1", "dmcp_abc", 1000, null, "admin")
        );
        when(apiKeyRepository.findByStatus(ApiKeyStatus.ACTIVE)).thenReturn(activeKeys);

        // When
        List<ApiKey> result = apiKeyService.listActiveKeys();

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getStatus()).isEqualTo(ApiKeyStatus.ACTIVE);
    }

    @Test
    @DisplayName("應回傳 API Key - 當根據 ID 查詢時")
    void shouldReturnApiKey_whenGetByIdCalled() {
        // Given
        String keyId = "0HZXJ8KYPKA9E";
        ApiKey apiKey = ApiKey.create(
                keyId, "test", "hash", "dmcp_abc",
                1000, null, "admin"
        );
        when(apiKeyRepository.findById(keyId)).thenReturn(Optional.of(apiKey));

        // When
        Optional<ApiKey> result = apiKeyService.getKeyById(keyId);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(keyId);
    }

    @Test
    @DisplayName("應生成長度正確的原始 Key - 當生成 API Key 時")
    void shouldGenerateCorrectLengthRawKey_whenGeneratingApiKey() {
        // Given
        when(idService.generateId()).thenReturn("0HZXJ8KYPKA9E");
        when(apiKeyRepository.findByName(anyString())).thenReturn(Optional.empty());
        when(apiKeyRepository.save(any(ApiKey.class))).thenAnswer(i -> i.getArgument(0));

        // When
        ApiKeyService.GeneratedApiKey result = apiKeyService.generateKey("test", "admin");

        // Then
        // dmcp_ (5) + 32 字元 = 37 字元
        assertThat(result.rawKey().length()).isEqualTo(37);
    }
}
