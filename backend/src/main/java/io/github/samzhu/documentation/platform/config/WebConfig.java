package io.github.samzhu.documentation.platform.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC 配置
 * <p>
 * 處理 SPA 路由和靜態資源配置。
 * 前端使用 React CDN 版本，所有前端路由都導向 index.html。
 * </p>
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    /**
     * 配置靜態資源處理
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 靜態資源路徑映射
        registry.addResourceHandler("/**")
                .addResourceLocations("classpath:/static/");
    }

    /**
     * 配置視圖控制器
     * <p>
     * SPA fallback: 根路徑導向 index.html
     * </p>
     */
    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addViewController("/").setViewName("forward:/index.html");
    }
}
