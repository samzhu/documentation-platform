package io.github.samzhu.documentation.platform.web.api;

import io.github.samzhu.documentation.platform.security.ApiKeyAuthenticationFilter;
import io.github.samzhu.documentation.platform.security.ApiKeyService;
import io.github.samzhu.documentation.platform.web.dto.ApiKeyDto;
import io.github.samzhu.documentation.platform.web.dto.CreateApiKeyRequest;
import io.github.samzhu.documentation.platform.web.dto.GeneratedApiKeyDto;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * API Key 管理 REST API
 * <p>
 * 提供 API Key 的建立、列表、撤銷功能。
 * 所有端點需要已認證的 API Key。
 * </p>
 * <p>
 * 重要注意事項：
 * <ul>
 *   <li>原始 Key 只在建立時顯示一次，之後無法再次查看</li>
 *   <li>Key 使用 BCrypt 雜湊儲存，確保安全性</li>
 *   <li>撤銷後的 Key 無法恢復使用</li>
 * </ul>
 * </p>
 */
@RestController
@RequestMapping("/api/api-keys")
public class ApiKeyController {

    private final ApiKeyService apiKeyService;

    /**
     * 建構函式
     *
     * @param apiKeyService API Key 服務
     */
    public ApiKeyController(ApiKeyService apiKeyService) {
        this.apiKeyService = apiKeyService;
    }

    /**
     * 建立新的 API Key
     * <p>
     * 產生新的 API Key，格式為 dmcp_ 前綴加上 Base64 編碼的隨機數。
     * 原始 Key 只會在此回應中顯示一次，請妥善保管。
     * </p>
     *
     * @param request   建立請求（包含名稱、過期時間、速率限制）
     * @param principal 當前認證的 API Key（用於記錄建立者）
     * @return 包含原始金鑰的結果（HTTP 201 Created）
     */
    @PostMapping
    public ResponseEntity<GeneratedApiKeyDto> createKey(
            @RequestBody @Valid CreateApiKeyRequest request,
            @AuthenticationPrincipal ApiKeyAuthenticationFilter.ApiKeyPrincipal principal) {

        // 取得建立者名稱，若未認證則使用 "system"
        String createdBy = principal != null ? principal.name() : "system";

        var generated = apiKeyService.generateKey(
                request.name(),
                createdBy,
                request.expiresAt(),
                request.rateLimit()
        );

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(GeneratedApiKeyDto.from(generated));
    }

    /**
     * 列出所有 API Key
     * <p>
     * 回傳所有 API Key 的基本資訊，不含敏感資料（如 key_hash）。
     * </p>
     *
     * @return API Key 列表（不含敏感資訊）
     */
    @GetMapping
    public List<ApiKeyDto> listKeys() {
        return apiKeyService.listKeys().stream()
                .map(ApiKeyDto::from)
                .toList();
    }

    /**
     * 取得單一 API Key
     * <p>
     * 依 ID 取得特定 API Key 的資訊。
     * </p>
     *
     * @param id 金鑰 ID（TSID 格式，13 字元）
     * @return API Key（不含敏感資訊），若不存在則回傳 404
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiKeyDto> getKey(@PathVariable String id) {
        return apiKeyService.getKeyById(id)
                .map(ApiKeyDto::from)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * 撤銷 API Key（DELETE 方式）
     * <p>
     * 將 API Key 狀態設為 REVOKED，使其無法再用於認證。
     * 此操作不可逆，撤銷後的 Key 無法恢復。
     * </p>
     *
     * @param id 金鑰 ID（TSID 格式，13 字元）
     * @return HTTP 204 No Content
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> revokeKey(@PathVariable String id) {
        apiKeyService.revokeKey(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * 撤銷 API Key（POST 方式）
     * <p>
     * 將 API Key 狀態設為 REVOKED，使其無法再用於認證。
     * 此操作不可逆，撤銷後的 Key 無法恢復。
     * 提供 POST 端點以支援前端 API 呼叫。
     * </p>
     *
     * @param id 金鑰 ID（TSID 格式，13 字元）
     * @return 撤銷後的 API Key
     */
    @PostMapping("/{id}/revoke")
    public ApiKeyDto revokeKeyPost(@PathVariable String id) {
        apiKeyService.revokeKey(id);
        return apiKeyService.getKeyById(id)
                .map(ApiKeyDto::from)
                .orElseThrow(() -> new IllegalArgumentException("API Key 不存在: " + id));
    }
}
