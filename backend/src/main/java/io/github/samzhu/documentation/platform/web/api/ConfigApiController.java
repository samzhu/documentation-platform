package io.github.samzhu.documentation.platform.web.api;

import io.github.samzhu.documentation.platform.config.FeatureFlags;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 配置 REST API
 * <p>
 * 提供前端所需的配置資訊，例如認證模式。
 * </p>
 */
@RestController
@RequestMapping("/api/config")
public class ConfigApiController {

    private final FeatureFlags featureFlags;

    /**
     * 建構函式
     *
     * @param featureFlags 功能開關配置
     */
    public ConfigApiController(FeatureFlags featureFlags) {
        this.featureFlags = featureFlags;
    }

    /**
     * 取得前端配置
     * <p>
     * 回傳前端所需的配置，例如是否啟用 OAuth2 認證。
     * 此端點為公開端點，不需要認證。
     * </p>
     *
     * @return 前端配置
     */
    @GetMapping
    public ConfigResponse getConfig() {
        return new ConfigResponse(featureFlags.isOauth2());
    }

    /**
     * 前端配置回應
     *
     * @param oauth2Enabled 是否啟用 OAuth2 認證
     */
    public record ConfigResponse(boolean oauth2Enabled) {}
}
