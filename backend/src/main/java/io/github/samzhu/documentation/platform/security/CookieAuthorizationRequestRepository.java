package io.github.samzhu.documentation.platform.security;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.oauth2.client.web.AuthorizationRequestRepository;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.SerializationUtils;

import java.util.Base64;

/**
 * 無狀態的 OAuth2 授權請求存儲
 * <p>
 * 將 OAuth2AuthorizationRequest 序列化後存入加密 Cookie，
 * 避免使用 Session，支援水平擴展。
 * </p>
 */
@Component
@ConditionalOnProperty(name = "platform.features.oauth2", havingValue = "true")
public class CookieAuthorizationRequestRepository implements AuthorizationRequestRepository<OAuth2AuthorizationRequest> {

    /**
     * Cookie 名稱
     */
    private static final String COOKIE_NAME = "oauth2_auth_request";

    /**
     * Cookie 過期時間（秒）
     */
    private static final int COOKIE_EXPIRE_SECONDS = 180; // 3 分鐘

    /**
     * 載入授權請求
     * <p>
     * 從 Cookie 中反序列化 OAuth2AuthorizationRequest。
     * </p>
     */
    @Override
    public OAuth2AuthorizationRequest loadAuthorizationRequest(HttpServletRequest request) {
        return getCookie(request)
                .map(this::deserialize)
                .orElse(null);
    }

    /**
     * 儲存授權請求
     * <p>
     * 將 OAuth2AuthorizationRequest 序列化後存入 Cookie。
     * </p>
     */
    @Override
    public void saveAuthorizationRequest(
            OAuth2AuthorizationRequest authorizationRequest,
            HttpServletRequest request,
            HttpServletResponse response) {

        if (authorizationRequest == null) {
            removeAuthorizationRequest(request, response);
            return;
        }

        String value = serialize(authorizationRequest);
        Cookie cookie = new Cookie(COOKIE_NAME, value);
        cookie.setPath("/");
        cookie.setHttpOnly(true);
        cookie.setMaxAge(COOKIE_EXPIRE_SECONDS);
        response.addCookie(cookie);
    }

    /**
     * 移除授權請求（並返回移除前的值）
     * <p>
     * OAuth2 回調成功後會呼叫此方法清除 Cookie。
     * </p>
     */
    @Override
    public OAuth2AuthorizationRequest removeAuthorizationRequest(
            HttpServletRequest request,
            HttpServletResponse response) {

        OAuth2AuthorizationRequest authorizationRequest = loadAuthorizationRequest(request);

        Cookie cookie = new Cookie(COOKIE_NAME, "");
        cookie.setPath("/");
        cookie.setHttpOnly(true);
        cookie.setMaxAge(0);
        response.addCookie(cookie);

        return authorizationRequest;
    }

    /**
     * 從請求中取得 Cookie
     */
    private java.util.Optional<Cookie> getCookie(HttpServletRequest request) {
        if (request.getCookies() == null) {
            return java.util.Optional.empty();
        }

        for (Cookie cookie : request.getCookies()) {
            if (COOKIE_NAME.equals(cookie.getName())) {
                return java.util.Optional.of(cookie);
            }
        }

        return java.util.Optional.empty();
    }

    /**
     * 序列化 OAuth2AuthorizationRequest
     */
    private String serialize(OAuth2AuthorizationRequest authorizationRequest) {
        byte[] bytes = SerializationUtils.serialize(authorizationRequest);
        return Base64.getUrlEncoder().encodeToString(bytes);
    }

    /**
     * 反序列化 OAuth2AuthorizationRequest
     */
    private OAuth2AuthorizationRequest deserialize(Cookie cookie) {
        byte[] bytes = Base64.getUrlDecoder().decode(cookie.getValue());
        return (OAuth2AuthorizationRequest) SerializationUtils.deserialize(bytes);
    }
}
