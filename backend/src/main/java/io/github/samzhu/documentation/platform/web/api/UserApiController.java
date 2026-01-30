package io.github.samzhu.documentation.platform.web.api;

import io.github.samzhu.documentation.platform.security.OAuth2AuthenticationSuccessHandler;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 用戶相關 API（無狀態 BFF 模式）
 * <p>
 * 提供以下端點：
 * - GET /api/me：取得當前用戶資訊（用於前端判斷登入狀態）
 * - POST /api/logout：登出（清除 Token Cookie）
 * </p>
 */
@RestController
@RequestMapping("/api")
@ConditionalOnProperty(name = "platform.features.oauth2", havingValue = "true")
public class UserApiController {

    /**
     * 取得當前用戶資訊
     * <p>
     * GET /api/me
     * <p>
     * 用途：
     * - 前端啟動時呼叫此端點判斷是否已登入
     * - 取得用戶基本資訊（從 JWT claims 提取）
     * </p>
     *
     * @param authentication Spring Security 認證物件（由 Filter 自動注入）
     * @return 用戶資訊或 401 未授權
     */
    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> getCurrentUser(Authentication authentication) {
        // 未認證（無 Token 或 Token 無效）
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).build();
        }

        Map<String, Object> userInfo = new HashMap<>();

        // 從 JWT 提取用戶資訊
        if (authentication.getPrincipal() instanceof Jwt jwt) {
            userInfo.put("sub", jwt.getSubject());
            userInfo.put("name", jwt.getClaimAsString("name"));
            userInfo.put("email", jwt.getClaimAsString("email"));

            // 可選：提取其他常見 claims
            if (jwt.hasClaim("picture")) {
                userInfo.put("picture", jwt.getClaimAsString("picture"));
            }
            if (jwt.hasClaim("preferred_username")) {
                userInfo.put("preferred_username", jwt.getClaimAsString("preferred_username"));
            }
        } else {
            // 非 JWT 認證（備用）
            userInfo.put("sub", authentication.getName());
            userInfo.put("name", authentication.getName());
        }

        userInfo.put("authenticated", true);

        return ResponseEntity.ok(userInfo);
    }

    /**
     * 登出
     * <p>
     * POST /api/logout
     * <p>
     * 清除 Token Cookie，讓用戶登出。
     * </p>
     *
     * @param response HTTP 回應（用於設置 Cookie）
     * @return 登出成功訊息
     */
    @PostMapping("/logout")
    public ResponseEntity<Map<String, Object>> logout(HttpServletResponse response) {
        // 清除 Token Cookie
        OAuth2AuthenticationSuccessHandler.clearTokenCookie(response);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "登出成功"
        ));
    }
}
