package io.github.samzhu.documentation.platform.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

/**
 * Token Cookie 認證過濾器
 * <p>
 * 從 HttpOnly Cookie 讀取 Token，轉換為 Authorization Header，
 * 讓 OAuth2 Resource Server 可以正常驗證 JWT。
 * </p>
 * <p>
 * 工作原理：
 * 1. 檢查請求是否已有 Authorization Header（優先使用）
 * 2. 若無，從 Cookie 讀取 platform_token
 * 3. 若有 Token，包裝請求並加入 Authorization: Bearer {token}
 * </p>
 */
public class TokenCookieAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(TokenCookieAuthenticationFilter.class);

    /**
     * Token Cookie 名稱（與 OAuth2AuthenticationSuccessHandler 一致）
     */
    private static final String TOKEN_COOKIE_NAME = OAuth2AuthenticationSuccessHandler.TOKEN_COOKIE_NAME;

    /**
     * Authorization Header 名稱
     */
    private static final String AUTHORIZATION_HEADER = "Authorization";

    /**
     * Bearer Token 前綴
     */
    private static final String BEARER_PREFIX = "Bearer ";

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        // 如果已有 Authorization Header，直接放行（Header 優先於 Cookie）
        if (request.getHeader(AUTHORIZATION_HEADER) != null) {
            filterChain.doFilter(request, response);
            return;
        }

        // 從 Cookie 取得 Token
        String token = getTokenFromCookie(request);

        if (token != null && !token.isBlank()) {
            // 包裝請求，加入 Authorization Header
            HttpServletRequest wrappedRequest = new BearerTokenRequestWrapper(request, token);
            logger.debug("從 Cookie 讀取 Token 並轉換為 Authorization Header");
            filterChain.doFilter(wrappedRequest, response);
        } else {
            // 無 Token，直接放行（可能是公開端點）
            filterChain.doFilter(request, response);
        }
    }

    /**
     * 從 Cookie 中取得 Token
     *
     * @param request HTTP 請求
     * @return Token 字串，若不存在則返回 null
     */
    private String getTokenFromCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }

        for (Cookie cookie : cookies) {
            if (TOKEN_COOKIE_NAME.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }

    /**
     * 包裝 HttpServletRequest，加入 Authorization Header
     * <p>
     * 讓後續的 OAuth2 Resource Server Filter 可以讀取到 Bearer Token。
     * </p>
     */
    private static class BearerTokenRequestWrapper extends HttpServletRequestWrapper {

        private final String bearerToken;

        public BearerTokenRequestWrapper(HttpServletRequest request, String token) {
            super(request);
            this.bearerToken = BEARER_PREFIX + token;
        }

        @Override
        public String getHeader(String name) {
            if (AUTHORIZATION_HEADER.equalsIgnoreCase(name)) {
                return bearerToken;
            }
            return super.getHeader(name);
        }

        @Override
        public Enumeration<String> getHeaders(String name) {
            if (AUTHORIZATION_HEADER.equalsIgnoreCase(name)) {
                return Collections.enumeration(List.of(bearerToken));
            }
            return super.getHeaders(name);
        }

        @Override
        public Enumeration<String> getHeaderNames() {
            // 加入 Authorization 到 header 名稱列表
            List<String> headerNames = Collections.list(super.getHeaderNames());
            if (!headerNames.contains(AUTHORIZATION_HEADER)) {
                headerNames.add(AUTHORIZATION_HEADER);
            }
            return Collections.enumeration(headerNames);
        }
    }
}
