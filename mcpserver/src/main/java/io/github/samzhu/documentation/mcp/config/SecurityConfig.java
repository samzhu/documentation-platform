package io.github.samzhu.documentation.mcp.config;

import io.github.samzhu.documentation.mcp.security.DatabaseApiKeyEntityRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

import static org.springaicommunity.mcp.security.server.config.McpApiKeyConfigurer.mcpServerApiKey;

/**
 * MCP Server 安全配置
 * <p>
 * 使用 mcp-server-security 的 API Key 認證機制。
 * Actuator 健康檢查端點公開，其餘需 API Key 認證。
 * </p>
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            DatabaseApiKeyEntityRepository apiKeyRepository) throws Exception {
        return http
                .authorizeHttpRequests(auth -> auth
                        // Actuator 健康檢查端點公開（Cloud Run 需要）
                        .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                        .anyRequest().authenticated())
                .with(mcpServerApiKey(), apiKey ->
                        apiKey.apiKeyRepository(apiKeyRepository))
                .build();
    }
}
