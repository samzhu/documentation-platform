package io.github.samzhu.documentation.platform.config;

import io.github.samzhu.documentation.platform.security.ConcurrencyLimitInterceptor;
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
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 安全配置
 * <p>
 * 支援兩種模式：
 * - OAuth2 模式（platform.features.oauth2=true）：使用 OAuth2 Resource Server + Client
 * - 開發模式（platform.features.oauth2=false）：允許所有請求，方便本地開發
 * </p>
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig implements WebMvcConfigurer {

    private final ConcurrencyLimitInterceptor concurrencyLimitInterceptor;

    public SecurityConfig(ConcurrencyLimitInterceptor concurrencyLimitInterceptor) {
        this.concurrencyLimitInterceptor = concurrencyLimitInterceptor;
    }

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
     * API 端點的安全配置（OAuth2 Resource Server）
     * <p>
     * 使用 JWT 驗證保護 API 端點，公開端點（搜尋、統計）不需認證。
     * </p>
     */
    @Bean
    @Order(1)
    @ConditionalOnProperty(name = "platform.features.oauth2", havingValue = "true")
    public SecurityFilterChain apiSecurityFilterChainOAuth2(HttpSecurity http) throws Exception {
        http
                .securityMatcher("/api/**")
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // 公開端點（搜尋、統計、配置）
                        .requestMatchers("/api/search/**", "/api/dashboard/stats", "/api/config").permitAll()
                        // 其他 API 需要認證
                        .anyRequest().authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
                );

        return http.build();
    }

    /**
     * Web UI 的安全配置（OAuth2 Client）
     * <p>
     * 使用 OAuth2 登入保護管理介面，靜態資源和健康檢查端點公開存取。
     * </p>
     */
    @Bean
    @Order(2)
    @ConditionalOnProperty(name = "platform.features.oauth2", havingValue = "true")
    public SecurityFilterChain webSecurityFilterChainOAuth2(HttpSecurity http) throws Exception {
        http
                .securityMatcher("/**")
                .authorizeHttpRequests(auth -> auth
                        // 靜態資源
                        .requestMatchers("/", "/index.html", "/css/**", "/js/**", "/images/**", "/favicon.ico").permitAll()
                        // OAuth2 登入回調
                        .requestMatchers("/login/**", "/oauth2/**", "/callback").permitAll()
                        // Actuator 端點
                        .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                        // 其他請求需要認證
                        .anyRequest().authenticated()
                )
                .oauth2Login(oauth2 -> oauth2
                        .defaultSuccessUrl("/", true)
                )
                .logout(logout -> logout
                        .logoutSuccessUrl("/")
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

    /**
     * 註冊併發限制攔截器
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(concurrencyLimitInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns("/api/api-keys/**");  // API Key 管理端點不限制
    }
}
