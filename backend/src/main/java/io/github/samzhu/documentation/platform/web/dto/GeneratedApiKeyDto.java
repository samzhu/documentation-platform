package io.github.samzhu.documentation.platform.web.dto;

import io.github.samzhu.documentation.platform.security.ApiKeyService;

/**
 * 生成的 API Key 結果 DTO
 * <p>
 * 包含原始金鑰，只會在建立時顯示一次。
 * </p>
 *
 * @param id        金鑰 ID
 * @param name      識別名稱
 * @param rawKey    原始金鑰（只會顯示一次，請妥善保存）
 * @param keyPrefix 金鑰前綴
 * @param mcpKey    MCP 客戶端用金鑰（{id}.{rawKey}），可直接設定於 X-API-Key Header
 */
public record GeneratedApiKeyDto(
        String id,
        String name,
        String rawKey,
        String keyPrefix,
        String mcpKey
) {
    /**
     * 從 GeneratedApiKey 轉換
     */
    public static GeneratedApiKeyDto from(ApiKeyService.GeneratedApiKey generated) {
        return new GeneratedApiKeyDto(
                generated.id(),
                generated.name(),
                generated.rawKey(),
                generated.keyPrefix(),
                generated.id() + "." + generated.rawKey()
        );
    }
}
