package io.github.samzhu.documentation.mcp.security;

import org.springaicommunity.mcp.security.server.apikey.ApiKeyEntity;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collections;
import java.util.List;

/**
 * 資料庫 API Key 實體
 * <p>
 * 橋接 api_keys 表與 mcp-server-security 的 ApiKeyEntity 介面。
 * Backend 使用 DelegatingPasswordEncoder，hash 已自帶算法前綴（{bcrypt}$2a$10$...），
 * getSecret() 直接回傳即可，不需額外處理。
 * </p>
 */
public class DatabaseApiKeyEntity implements ApiKeyEntity {

    private final String id;
    private String keyHash;  // 非 final，eraseCredentials() 需要清除
    private final String name;

    public DatabaseApiKeyEntity(String id, String keyHash, String name) {
        this.id = id;
        this.keyHash = keyHash;
        this.name = name;
    }

    @Override
    public String getId() {
        return id;
    }

    /**
     * 回傳密碼雜湊值
     * <p>
     * Backend 改用 DelegatingPasswordEncoder 後，hash 格式為 {bcrypt}$2a$10$...
     * mcp-server-security 內部的 DelegatingPasswordEncoder 可直接比對，無需前綴 hack。
     * </p>
     */
    @Override
    public String getSecret() {
        return keyHash;
    }

    public String getName() {
        return name;
    }

    @Override
    public List<GrantedAuthority> getAuthorities() {
        return Collections.emptyList();
    }

    @Override
    public void eraseCredentials() {
        this.keyHash = null;
    }

    @Override
    @SuppressWarnings("unchecked")
    public DatabaseApiKeyEntity copy() {
        return new DatabaseApiKeyEntity(id, keyHash, name);
    }
}
