package io.github.samzhu.documentation.platform.security;

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
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * OAuth2 登入成功處理器
 * <p>
 * 登入成功後：
 * 1. 從 OAuth2AuthorizedClientService 取得 access_token
 * 2. 產生一次性交換碼（60 秒有效）
 * 3. 重導向到前端 /#/callback?code=xxx
 * </p>
 */
@Component
@ConditionalOnProperty(name = "platform.features.oauth2", havingValue = "true")
public class OAuth2AuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    private static final Logger logger = LoggerFactory.getLogger(OAuth2AuthenticationSuccessHandler.class);

    /**
     * 交換碼有效期（秒）
     */
    private static final int EXCHANGE_CODE_EXPIRE_SECONDS = 60;

    /**
     * OAuth2 授權客戶端服務
     */
    private final OAuth2AuthorizedClientService authorizedClientService;

    /**
     * 一次性交換碼存儲（ConcurrentHashMap，生產環境建議使用 Redis）
     */
    private final ConcurrentHashMap<String, TokenInfo> exchangeCodes = new ConcurrentHashMap<>();

    public OAuth2AuthenticationSuccessHandler(OAuth2AuthorizedClientService authorizedClientService) {
        this.authorizedClientService = authorizedClientService;
    }

    /**
     * 登入成功處理邏輯
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

        // 2. 產生一次性交換碼
        String accessToken = authorizedClient.getAccessToken().getTokenValue();
        Instant expiresAt = Instant.now().plusSeconds(EXCHANGE_CODE_EXPIRE_SECONDS);
        String code = UUID.randomUUID().toString();

        exchangeCodes.put(code, new TokenInfo(accessToken, expiresAt));

        logger.info("登入成功，產生交換碼: code={}, principalName={}", code, principalName);

        // 3. 清理過期的交換碼
        cleanExpiredCodes();

        // 4. 重導向到前端 callback 頁面
        String redirectUrl = "/#/callback?code=" + code;
        response.sendRedirect(redirectUrl);
    }

    /**
     * 用交換碼換取 access_token
     * <p>
     * 此方法供 AuthController 呼叫。
     * </p>
     *
     * @param code 一次性交換碼
     * @return access_token，如果 code 無效或過期則返回 null
     */
    public String exchangeCodeForToken(String code) {
        TokenInfo tokenInfo = exchangeCodes.remove(code); // 一次性使用，取出後刪除

        if (tokenInfo == null) {
            logger.warn("交換碼不存在或已使用: code={}", code);
            return null;
        }

        if (Instant.now().isAfter(tokenInfo.expiresAt())) {
            logger.warn("交換碼已過期: code={}", code);
            return null;
        }

        logger.info("交換碼驗證成功: code={}", code);
        return tokenInfo.accessToken();
    }

    /**
     * 清理過期的交換碼
     */
    private void cleanExpiredCodes() {
        Instant now = Instant.now();
        exchangeCodes.entrySet().removeIf(entry -> now.isAfter(entry.getValue().expiresAt()));
    }

    /**
     * Token 資訊（包含 access_token 和過期時間）
     */
    private record TokenInfo(String accessToken, Instant expiresAt) {
    }
}
