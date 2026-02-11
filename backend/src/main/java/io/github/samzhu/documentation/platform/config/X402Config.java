package io.github.samzhu.documentation.platform.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * x402 贊助功能配置
 * <p>
 * 啟用 X402Properties 配置屬性綁定。
 * </p>
 */
@Configuration
@EnableConfigurationProperties(X402Properties.class)
public class X402Config {
}
