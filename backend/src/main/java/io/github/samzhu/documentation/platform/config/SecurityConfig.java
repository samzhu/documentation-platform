package io.github.samzhu.documentation.platform.config;

import io.github.samzhu.documentation.platform.security.CookieAuthorizationRequestRepository;
import io.github.samzhu.documentation.platform.security.OAuth2AuthenticationSuccessHandler;
import io.github.samzhu.documentation.platform.security.TokenCookieAuthenticationFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;

/**
 * 安全配置（無狀態 BFF 模式）
 * <p>
 * 支援兩種模式：
 * - OAuth2 模式（platform.features.oauth2=true）：使用 OAuth2 Login + Resource Server + HttpOnly Cookie
 * - 開發模式（platform.features.oauth2=false）：允許所有請求，方便本地開發
 * </p>
 * <p>
 * OAuth2 模式安全特性：
 * - Token 存於 HttpOnly Cookie，前端 JavaScript 無法讀取
 * - TokenCookieAuthenticationFilter 從 Cookie 讀取 Token，轉換為 Authorization Header
 * - OAuth2 Resource Server 驗證 JWT 簽名
 * </p>
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Autowired(required = false)
    private CookieAuthorizationRequestRepository cookieAuthRequestRepo;

    @Autowired(required = false)
    private OAuth2AuthenticationSuccessHandler oAuth2SuccessHandler;

    /**
     * 密碼編碼器
     * <p>
     * 使用 BCrypt 進行 API Key 雜湊（供 MCP Server 驗證使用）
     * </p>
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // ==================== OAuth2 模式（生產環境） ====================

    /**
     * OAuth2 安全配置（無狀態 BFF 架構）
     * <p>
     * 結合 OAuth2 Login 和 Resource Server：
     * - Token 存於 HttpOnly Cookie（防止 XSS）
     * - TokenCookieAuthenticationFilter 從 Cookie 讀取 Token，轉換為 Authorization Header
     * - OAuth2 Resource Server 驗證 JWT
     * </p>
     */
    @Bean
    @Order(1)
    @ConditionalOnProperty(name = "platform.features.oauth2", havingValue = "true")
    public SecurityFilterChain securityFilterChainOAuth2(HttpSecurity http) throws Exception {
        http
                // 停用 CSRF（使用 SameSite Cookie 防護）
                .csrf(csrf -> csrf.disable())

                // ✅ Stateless：不依賴 Session
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                // 授權規則
                .authorizeHttpRequests(auth -> auth
                        // 靜態資源（前端 SPA）- 公開
                        .requestMatchers("/", "/index.html", "/assets/**", "/favicon.ico").permitAll()
                        // Actuator 端點 - 公開（Cloud Run 健康檢查）
                        .requestMatchers("/actuator/**").permitAll()
                        // 公開 API 端點
                        .requestMatchers("/api/search/**", "/api/dashboard/stats", "/api/config").permitAll()
                        // ✅ 用戶資訊端點 - 需認證（用於前端判斷登入狀態）
                        .requestMatchers("/api/me").authenticated()
                        // ✅ 登出端點 - 需認證
                        .requestMatchers("/api/logout").authenticated()
                        // OAuth2 登入相關端點 - 公開
                        .requestMatchers("/login/**", "/oauth2/**").permitAll()
                        // 其他請求需要認證
                        .anyRequest().authenticated()
                )

                // ✅ 加入 TokenCookieAuthenticationFilter
                // 在 BearerTokenAuthenticationFilter 之前執行，從 Cookie 讀取 Token 轉換為 Authorization Header
                .addFilterBefore(
                        new TokenCookieAuthenticationFilter(),
                        BearerTokenAuthenticationFilter.class
                )

                // OAuth2 Login：未認證時自動重導向 Auth Server
                .oauth2Login(oauth2 -> oauth2
                        // ✅ 使用 Cookie 存儲授權請求（無狀態）
                        .authorizationEndpoint(endpoint -> endpoint
                                .authorizationRequestRepository(cookieAuthRequestRepo)
                        )
                        // ✅ 自定義成功處理器（將 Token 存入 HttpOnly Cookie）
                        .successHandler(oAuth2SuccessHandler)
                )

                // OAuth2 Resource Server：驗證 Bearer Token（從 Cookie 轉換而來）
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
                );

        return http.build();
    }

    /**
     * JWT claims 轉換為 Spring Security 權限
     * <p>
     * 從 JWT 的 roles claim 中提取權限，並加上 ROLE_ 前綴。
     * </p>
     */
    private JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter converter = new JwtGrantedAuthoritiesConverter();
        converter.setAuthoritiesClaimName("roles");
        converter.setAuthorityPrefix("ROLE_");

        JwtAuthenticationConverter jwtConverter = new JwtAuthenticationConverter();
        jwtConverter.setJwtGrantedAuthoritiesConverter(converter);
        return jwtConverter;
    }

    // ==================== 開發模式（本地開發） ====================

    /**
     * 開發模式的安全配置
     * <p>
     * 允許所有請求，方便本地開發。不使用 OAuth2 認證。
     * </p>
     */
    @Bean
    @Order(1)
    @ConditionalOnProperty(name = "platform.features.oauth2", havingValue = "false", matchIfMissing = true)
    public SecurityFilterChain securityFilterChainDev(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        .anyRequest().permitAll()
                );

        return http.build();
    }

}
