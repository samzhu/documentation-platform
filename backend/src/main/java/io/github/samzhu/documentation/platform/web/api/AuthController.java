package io.github.samzhu.documentation.platform.web.api;

import io.github.samzhu.documentation.platform.security.OAuth2AuthenticationSuccessHandler;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 認證相關 API
 * <p>
 * 提供 OAuth2 登入後的 Token 交換功能。
 * </p>
 */
@RestController
@RequestMapping("/api/auth")
@ConditionalOnProperty(name = "platform.features.oauth2", havingValue = "true")
public class AuthController {

    private final OAuth2AuthenticationSuccessHandler successHandler;

    public AuthController(OAuth2AuthenticationSuccessHandler successHandler) {
        this.successHandler = successHandler;
    }

    /**
     * 用一次性交換碼換取 access_token
     * <p>
     * POST /api/auth/exchange
     * Body: { "code": "xxx" }
     * Response: { "access_token": "...", "token_type": "Bearer" }
     * </p>
     *
     * @param request 包含 code 的請求體
     * @return access_token 或錯誤訊息
     */
    @PostMapping("/exchange")
    public ResponseEntity<?> exchangeToken(@RequestBody Map<String, String> request) {
        String code = request.get("code");

        if (code == null || code.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "invalid_request",
                    "error_description", "缺少 code 參數"
            ));
        }

        String accessToken = successHandler.exchangeCodeForToken(code);

        if (accessToken == null) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "invalid_code",
                    "error_description", "交換碼無效或已過期"
            ));
        }

        return ResponseEntity.ok(Map.of(
                "access_token", accessToken,
                "token_type", "Bearer"
        ));
    }
}
