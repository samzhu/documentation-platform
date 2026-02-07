package io.github.samzhu.documentation.mcp.domain.enums;

/**
 * API Key 狀態（與 backend 共用相同列舉值）
 */
public enum ApiKeyStatus {
    /** 有效 */
    ACTIVE,
    /** 已撤銷 */
    REVOKED,
    /** 已過期 */
    EXPIRED
}
