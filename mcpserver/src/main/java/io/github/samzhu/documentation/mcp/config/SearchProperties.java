package io.github.samzhu.documentation.mcp.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 搜尋相關配置屬性
 * <p>
 * 從 platform.search.* 讀取混合搜尋參數。
 * </p>
 *
 * @param hybrid       混合搜尋參數
 * @param defaultLimit 預設回傳筆數上限
 * @param maxLimit     最大回傳筆數上限
 */
@ConfigurationProperties(prefix = "platform.search")
public record SearchProperties(Hybrid hybrid, int defaultLimit, int maxLimit) {

    /**
     * 混合搜尋參數
     *
     * @param alpha         關鍵字搜尋權重（0-1），(1 - alpha) 為語意搜尋權重
     * @param minSimilarity 語意搜尋的最低相似度閾值
     */
    public record Hybrid(double alpha, double minSimilarity) {}
}
