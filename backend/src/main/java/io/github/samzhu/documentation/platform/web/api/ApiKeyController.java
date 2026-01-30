package io.github.samzhu.documentation.platform.web.api;

import io.github.samzhu.documentation.platform.security.ApiKeyService;
import io.github.samzhu.documentation.platform.web.dto.ApiKeyDto;
import io.github.samzhu.documentation.platform.web.dto.CreateApiKeyRequest;
import io.github.samzhu.documentation.platform.web.dto.GeneratedApiKeyDto;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
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
 * 此專案只負責 API Key 的 CRUD 管理，實際認證由 MCP Server 處理。
 * </p>
 */
@RestController
@RequestMapping("/api/api-keys")
public class ApiKeyController {

    private final ApiKeyService apiKeyService;

    public ApiKeyController(ApiKeyService apiKeyService) {
        this.apiKeyService = apiKeyService;
    }

    /**
     * 建立新的 API Key
     * <p>
     * 產生新的 API Key，格式為 dmcp_ 前綴加上 Base64 編碼的隨機數。
     * 原始 Key 只會在此回應中顯示一次，請妥善保管。
     * </p>
     */
    @PostMapping
    public ResponseEntity<GeneratedApiKeyDto> createKey(
            @RequestBody @Valid CreateApiKeyRequest request,
            Authentication authentication) {

        // 從 JWT 取得建立者名稱
        String createdBy = getUsername(authentication);

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
     */
    @GetMapping
    public List<ApiKeyDto> listKeys() {
        return apiKeyService.listKeys().stream()
                .map(ApiKeyDto::from)
                .toList();
    }

    /**
     * 取得單一 API Key
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
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> revokeKey(@PathVariable String id) {
        apiKeyService.revokeKey(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * 撤銷 API Key（POST 方式）
     */
    @PostMapping("/{id}/revoke")
    public ApiKeyDto revokeKeyPost(@PathVariable String id) {
        apiKeyService.revokeKey(id);
        return apiKeyService.getKeyById(id)
                .map(ApiKeyDto::from)
                .orElseThrow(() -> new IllegalArgumentException("API Key 不存在: " + id));
    }

    /**
     * 從 Authentication 取得用戶名稱
     */
    private String getUsername(Authentication authentication) {
        if (authentication == null) {
            return "system";
        }
        if (authentication.getPrincipal() instanceof Jwt jwt) {
            // 優先使用 name，其次 preferred_username，最後 sub
            String name = jwt.getClaimAsString("name");
            if (name != null && !name.isBlank()) {
                return name;
            }
            String username = jwt.getClaimAsString("preferred_username");
            if (username != null && !username.isBlank()) {
                return username;
            }
            return jwt.getSubject();
        }
        return authentication.getName();
    }
}
