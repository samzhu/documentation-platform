package io.github.samzhu.documentation.platform.config;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.security.oauth2.client.autoconfigure.OAuth2ClientAutoConfiguration;
import org.springframework.boot.security.oauth2.server.resource.autoconfigure.servlet.OAuth2ResourceServerAutoConfiguration;
import org.springframework.context.annotation.Configuration;

/**
 * OAuth2 自動配置排除器
 * <p>
 * 當 platform.features.oauth2=false(預設)時,排除 OAuth2 相關的自動配置,
 * 避免嘗試連接 Auth Server 的 .well-known/openid-configuration。
 * </p>
 * <p>
 * 使用方式:
 * - 功能測試: platform.features.oauth2=false(預設,無需連接 Auth Server)
 * - OAuth2 整合測試: platform.features.oauth2=true(需在白名單 IP)
 * </p>
 *
 * @see <a href="https://github.com/fineconstant/spring-boot-autoconfigure-exclude">spring-boot-autoconfigure-exclude</a>
 */
@Configuration
@ConditionalOnProperty(
        name = "platform.features.oauth2",
        havingValue = "false",
        matchIfMissing = true  // 預設排除(功能測試優先)
)
@EnableAutoConfiguration(exclude = {
        OAuth2ClientAutoConfiguration.class,
        OAuth2ResourceServerAutoConfiguration.class
})
public class OAuth2AutoConfigurationExcluder {
    // 此配置類僅用於條件性排除自動配置,無需任何方法
}
