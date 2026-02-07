package io.github.samzhu.documentation.mcp.security;

import io.github.samzhu.documentation.mcp.domain.model.ApiKey;
import io.github.samzhu.documentation.mcp.repository.ApiKeyRepository;
import org.springframework.lang.Nullable;
import org.springaicommunity.mcp.security.server.apikey.ApiKeyEntityRepository;
import org.springframework.stereotype.Component;

/**
 * 從共用資料庫讀取 API Key，供 mcp-server-security 認證用
 * <p>
 * 認證流程：
 * 1. MCP 客戶端傳送 X-API-Key: {id}.{rawKey}
 * 2. mcp-server-security 用 "." 分割取得 id
 * 3. 呼叫 findByKeyId(id) 從 DB 撈出記錄
 * 4. 比對 rawKey 與 DB 中的 hash
 * </p>
 */
@Component
public class DatabaseApiKeyEntityRepository
        implements ApiKeyEntityRepository<DatabaseApiKeyEntity> {

    private final ApiKeyRepository apiKeyRepository;

    public DatabaseApiKeyEntityRepository(ApiKeyRepository apiKeyRepository) {
        this.apiKeyRepository = apiKeyRepository;
    }

    @Override
    public @Nullable DatabaseApiKeyEntity findByKeyId(String keyId) {
        return apiKeyRepository.findById(keyId)
                .filter(ApiKey::isValid)  // 狀態 ACTIVE 且未過期
                .map(apiKey -> new DatabaseApiKeyEntity(
                        apiKey.getId(),
                        apiKey.getKeyHash(),
                        apiKey.getName()
                ))
                .orElse(null);
    }
}
