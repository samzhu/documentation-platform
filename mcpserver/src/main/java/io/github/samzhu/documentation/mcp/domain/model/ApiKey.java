package io.github.samzhu.documentation.mcp.domain.model;

import io.github.samzhu.documentation.mcp.domain.enums.ApiKeyStatus;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.OffsetDateTime;
import java.util.Objects;

/**
 * API Key 唯讀實體
 * <p>
 * MCP Server 只負責驗證，不負責 CRUD。
 * 與 backend 共用 api_keys 表，透過 Spring Data JDBC 讀取。
 * </p>
 */
@Table("api_keys")
public class ApiKey {

    /** 唯一識別碼（TSID 格式，13 字元） */
    @Id
    private final String id;

    /** 識別名稱 */
    private final String name;

    /** 雜湊後的 Key（{bcrypt}$2a$10$...） */
    @Column("key_hash")
    private final String keyHash;

    /** Key 前綴（如 "dmcp_a1b2"） */
    @Column("key_prefix")
    private final String keyPrefix;

    /** 狀態 */
    private final ApiKeyStatus status;

    /** 每小時請求上限 */
    @Column("rate_limit")
    private final Integer rateLimit;

    /** 過期時間 */
    @Column("expires_at")
    private final OffsetDateTime expiresAt;

    /** 最後使用時間 */
    @Column("last_used_at")
    private final OffsetDateTime lastUsedAt;

    /** 建立者 */
    @Column("created_by")
    private final String createdBy;

    /** 建立時間 */
    @Column("created_at")
    private final OffsetDateTime createdAt;

    /** 更新時間 */
    @Column("updated_at")
    private final OffsetDateTime updatedAt;

    public ApiKey(String id, String name, String keyHash, String keyPrefix,
                  ApiKeyStatus status, Integer rateLimit, OffsetDateTime expiresAt,
                  OffsetDateTime lastUsedAt, String createdBy,
                  OffsetDateTime createdAt, OffsetDateTime updatedAt) {
        this.id = id;
        this.name = name;
        this.keyHash = keyHash;
        this.keyPrefix = keyPrefix;
        this.status = status;
        this.rateLimit = rateLimit;
        this.expiresAt = expiresAt;
        this.lastUsedAt = lastUsedAt;
        this.createdBy = createdBy;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getKeyHash() { return keyHash; }
    public String getKeyPrefix() { return keyPrefix; }
    public ApiKeyStatus getStatus() { return status; }
    public Integer getRateLimit() { return rateLimit; }
    public OffsetDateTime getExpiresAt() { return expiresAt; }
    public OffsetDateTime getLastUsedAt() { return lastUsedAt; }
    public String getCreatedBy() { return createdBy; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }

    /** 檢查是否已過期 */
    public boolean isExpired() {
        return expiresAt != null && OffsetDateTime.now().isAfter(expiresAt);
    }

    /** 檢查是否有效（狀態為 ACTIVE 且未過期） */
    public boolean isValid() {
        return status == ApiKeyStatus.ACTIVE && !isExpired();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ApiKey apiKey = (ApiKey) o;
        return Objects.equals(id, apiKey.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        // 排除 keyHash 避免洩漏
        return "ApiKey{id='%s', name='%s', status=%s}".formatted(id, name, status);
    }
}
