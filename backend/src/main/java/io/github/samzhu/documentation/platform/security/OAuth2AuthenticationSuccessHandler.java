package io.github.samzhu.documentation.platform.security;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * OAuth2 登入成功處理器（無狀態 BFF 模式）
 * <p>
 * 登入成功後：
 * 1. 從 OAuth2AuthorizedClientService 取得 access_token
 * 2. 將 Token 存入 HttpOnly Cookie（前端 JavaScript 無法讀取）
 * 3. 重導向到前端 /#/dashboard
 * </p>
 * <p>
 * 安全特性：
 * - HttpOnly：防止 XSS 攻擊竊取 Token
 * - Secure：僅透過 HTTPS 傳輸（開發環境可覆寫）
 * - SameSite=Lax：基本 CSRF 防護
 * </p>
 */
@Component
@ConditionalOnProperty(name = "platform.features.oauth2", havingValue = "true")
public class OAuth2AuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    private static final Logger logger = LoggerFactory.getLogger(OAuth2AuthenticationSuccessHandler.class);

    /**
     * Token Cookie 名稱
     */
    public static final String TOKEN_COOKIE_NAME = "platform_token";

    /**
     * Token Cookie 有效期（秒）- 預設 1 小時
     */
    private static final int TOKEN_COOKIE_MAX_AGE = 3600;

    /**
     * OAuth2 授權客戶端服務
     */
    private final OAuth2AuthorizedClientService authorizedClientService;

    public OAuth2AuthenticationSuccessHandler(OAuth2AuthorizedClientService authorizedClientService) {
        this.authorizedClientService = authorizedClientService;
    }

    /**
     * 登入成功處理邏輯
     * <p>
     * 將 access_token 存入 HttpOnly Cookie，然後重導向到前端。
     * </p>
     */
    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication) throws IOException {

        if (!(authentication instanceof OAuth2AuthenticationToken oauth2Token)) {
            logger.error("Authentication 不是 OAuth2AuthenticationToken 類型");
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Invalid authentication type");
            return;
        }

        // 1. 取得 OAuth2 授權客戶端
        String registrationId = oauth2Token.getAuthorizedClientRegistrationId();
        String principalName = oauth2Token.getName();

        OAuth2AuthorizedClient authorizedClient = authorizedClientService.loadAuthorizedClient(
                registrationId,
                principalName
        );

        if (authorizedClient == null || authorizedClient.getAccessToken() == null) {
            logger.error("無法取得 access_token: registrationId={}, principalName={}", registrationId, principalName);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Failed to retrieve access token");
            return;
        }

        // 2. 取得 access_token
        String accessToken = authorizedClient.getAccessToken().getTokenValue();

        // 3. 設置 HttpOnly Cookie
        Cookie tokenCookie = new Cookie(TOKEN_COOKIE_NAME, accessToken);
        tokenCookie.setHttpOnly(true);  // 防止 XSS 攻擊讀取 Token
        tokenCookie.setSecure(isSecureRequest(request));  // HTTPS 環境下啟用
        tokenCookie.setPath("/");  // 全站可用
        tokenCookie.setMaxAge(TOKEN_COOKIE_MAX_AGE);  // 1 小時有效期

        // 設置 SameSite=Lax 防止 CSRF（透過 Header 方式）
        // 注意：Jakarta Servlet API 原生不支援 SameSite，需透過 response header
        String cookieValue = String.format("%s=%s; Path=/; Max-Age=%d; HttpOnly; SameSite=Lax%s",
                TOKEN_COOKIE_NAME,
                accessToken,
                TOKEN_COOKIE_MAX_AGE,
                isSecureRequest(request) ? "; Secure" : "");
        response.setHeader("Set-Cookie", cookieValue);

        logger.info("登入成功，Token 已存入 HttpOnly Cookie: principalName={}", principalName);

        // 4. 重導向到前端 Dashboard
        response.sendRedirect("/#/dashboard");
    }

    /**
     * 判斷是否為安全連線（HTTPS）
     * <p>
     * 考慮反向代理情況（X-Forwarded-Proto header）
     * </p>
     */
    private boolean isSecureRequest(HttpServletRequest request) {
        // 檢查直接連線是否為 HTTPS
        if (request.isSecure()) {
            return true;
        }
        // 檢查反向代理 header
        String forwardedProto = request.getHeader("X-Forwarded-Proto");
        return "https".equalsIgnoreCase(forwardedProto);
    }

    /**
     * 清除 Token Cookie（用於登出）
     */
    public static void clearTokenCookie(HttpServletResponse response) {
        String cookieValue = String.format("%s=; Path=/; Max-Age=0; HttpOnly; SameSite=Lax",
                TOKEN_COOKIE_NAME);
        response.setHeader("Set-Cookie", cookieValue);
    }
}
