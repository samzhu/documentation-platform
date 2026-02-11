package io.github.samzhu.documentation.platform.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * x402 贊助功能配置屬性
 * <p>
 * 定義 x402 協議所需的錢包地址、網路、合約地址等參數。
 * 配置前綴: platform.x402
 * </p>
 */
@ConfigurationProperties(prefix = "platform.x402")
public class X402Properties {

    /**
     * 收款錢包地址（Base Sepolia）
     */
    private String payTo;

    /**
     * USDC 合約地址（Base Mainnet）
     */
    private String asset = "0x833589fCD6eDb6E08f4c7C32D4f71b54bdA02913";

    /**
     * 區塊鏈網路識別碼
     */
    private String network = "base";

    /**
     * Coinbase Facilitator 服務 URL
     */
    private String facilitatorUrl = "https://x402.org/facilitator";

    /**
     * 付款描述文字
     */
    private String description = "Sponsor Documentation Platform";

    /**
     * 簽名有效期（秒）
     */
    private int maxTimeoutSeconds = 60;

    public String getPayTo() {
        return payTo;
    }

    public void setPayTo(String payTo) {
        this.payTo = payTo;
    }

    public String getAsset() {
        return asset;
    }

    public void setAsset(String asset) {
        this.asset = asset;
    }

    public String getNetwork() {
        return network;
    }

    public void setNetwork(String network) {
        this.network = network;
    }

    public String getFacilitatorUrl() {
        return facilitatorUrl;
    }

    public void setFacilitatorUrl(String facilitatorUrl) {
        this.facilitatorUrl = facilitatorUrl;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getMaxTimeoutSeconds() {
        return maxTimeoutSeconds;
    }

    public void setMaxTimeoutSeconds(int maxTimeoutSeconds) {
        this.maxTimeoutSeconds = maxTimeoutSeconds;
    }
}
