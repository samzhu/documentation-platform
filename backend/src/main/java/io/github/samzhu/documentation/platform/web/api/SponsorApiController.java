package io.github.samzhu.documentation.platform.web.api;

import io.github.samzhu.documentation.platform.config.X402Properties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

import java.util.Base64;
import java.util.Map;
import java.util.Set;

/**
 * 贊助 REST API — 實作 x402 付款協議
 * <p>
 * 流程說明：
 * 1. 前端發送 POST /api/sponsor?amount=5（無付款 header）
 * 2. 後端回傳 HTTP 402 + PaymentRequired 資訊
 * 3. 前端 x402 client 自動簽署 EIP-3009 授權並重送請求
 * 4. 後端呼叫 Facilitator /settle 驗證並鏈上結算
 * 5. 成功後回傳 HTTP 200
 * </p>
 */
@RestController
@RequestMapping("/api/sponsor")
public class SponsorApiController {

    private static final Logger log = LoggerFactory.getLogger(SponsorApiController.class);

    /** 允許的贊助金額（美元） */
    private static final Set<Integer> ALLOWED_AMOUNTS = Set.of(1, 5, 10);

    /** USDC 精度：6 位小數 */
    private static final int USDC_DECIMALS = 6;

    private final X402Properties x402Props;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    /**
     * 建構函式
     *
     * @param x402Props x402 配置屬性
     * @param restClientBuilder RestClient 建構器
     * @param objectMapper Jackson 3.x ObjectMapper（Spring Boot 自動配置）
     */
    public SponsorApiController(
            X402Properties x402Props,
            RestClient.Builder restClientBuilder,
            ObjectMapper objectMapper) {
        this.x402Props = x402Props;
        this.restClient = restClientBuilder
                .baseUrl(x402Props.getFacilitatorUrl())
                .build();
        this.objectMapper = objectMapper;
    }

    /**
     * 贊助端點 — 處理 x402 付款流程
     * <p>
     * 支援 V1（X-PAYMENT）和 V2（PAYMENT-SIGNATURE）header 格式。
     * </p>
     *
     * @param amount 贊助金額（1、5 或 10 美元）
     * @param xPayment V1 付款 header（由 x402 client 自動附加）
     * @param paymentSignature V2 付款 header（由 x402 client 自動附加）
     * @return 402 PaymentRequired 或 200 成功回應
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> sponsor(
            @RequestParam int amount,
            @RequestHeader(value = "X-PAYMENT", required = false) String xPayment,
            @RequestHeader(value = "PAYMENT-SIGNATURE", required = false) String paymentSignature) {

        // 驗證金額是否合法
        if (!ALLOWED_AMOUNTS.contains(amount)) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "金額必須為 1、5 或 10 美元"));
        }

        // 取得實際的付款 header（V1 或 V2 皆可）
        String payment = xPayment != null ? xPayment : paymentSignature;

        if (payment == null || payment.isBlank()) {
            // 無付款 header → 回傳 402 PaymentRequired
            return buildPaymentRequired(amount);
        }

        // 有付款 header → 呼叫 Facilitator 結算
        return settlePayment(payment, amount);
    }

    /**
     * 建構 HTTP 402 PaymentRequired 回應
     * <p>
     * 包含 x402 協議所需的付款要求資訊，
     * 同時在 header 和 body 中提供（相容 V1 + V2）。
     * </p>
     *
     * @param amount 贊助金額（美元）
     * @return 402 回應
     */
    private ResponseEntity<Map<String, Object>> buildPaymentRequired(int amount) {
        // 將美元金額轉換為 USDC 最小單位（6 位小數）
        String maxAmountRequired = String.valueOf(amount * (long) Math.pow(10, USDC_DECIMALS));

        // 建構 accepts 物件（x402 協議格式）
        Map<String, Object> acceptsEntry = Map.of(
                "scheme", "exact",
                "network", x402Props.getNetwork(),
                "maxAmountRequired", maxAmountRequired,
                "resource", "/api/sponsor",
                "description", x402Props.getDescription(),
                "payTo", x402Props.getPayTo(),
                "asset", x402Props.getAsset(),
                "maxTimeoutSeconds", x402Props.getMaxTimeoutSeconds(),
                "extra", Map.of()
        );

        // 完整的 PaymentRequired 回應
        Map<String, Object> paymentRequired = Map.of(
                "x402Version", 1,
                "accepts", java.util.List.of(acceptsEntry)
        );

        // Base64 編碼（V2 header 格式）
        String jsonStr = objectMapper.writeValueAsString(paymentRequired);
        String base64Encoded = Base64.getEncoder().encodeToString(jsonStr.getBytes());

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-PAYMENT-REQUIRED", base64Encoded);

        log.info("贊助請求 - 金額: ${}, 回傳 402 PaymentRequired", amount);

        return ResponseEntity.status(HttpStatus.PAYMENT_REQUIRED)
                .headers(headers)
                .body(paymentRequired);
    }

    /**
     * 呼叫 Facilitator /settle 端點進行鏈上結算
     *
     * @param paymentHeader 前端傳來的付款 header（Base64 編碼）
     * @param amount 贊助金額（美元）
     * @return 結算結果
     */
    private ResponseEntity<Map<String, Object>> settlePayment(String paymentHeader, int amount) {
        try {
            // 解碼付款 payload
            String decodedPayment = new String(Base64.getDecoder().decode(paymentHeader));
            Object paymentPayload = objectMapper.readValue(decodedPayment, Object.class);

            // 建構結算請求所需的 paymentRequirements
            String maxAmountRequired = String.valueOf(amount * (long) Math.pow(10, USDC_DECIMALS));
            Map<String, Object> paymentRequirements = Map.of(
                    "scheme", "exact",
                    "network", x402Props.getNetwork(),
                    "maxAmountRequired", maxAmountRequired,
                    "resource", "/api/sponsor",
                    "description", x402Props.getDescription(),
                    "payTo", x402Props.getPayTo(),
                    "asset", x402Props.getAsset(),
                    "maxTimeoutSeconds", x402Props.getMaxTimeoutSeconds(),
                    "extra", Map.of()
            );

            // 建構 settle 請求 body
            ObjectNode settleBody = objectMapper.createObjectNode();
            settleBody.putPOJO("paymentPayload", paymentPayload);
            settleBody.putPOJO("paymentRequirements", paymentRequirements);

            log.info("贊助請求 - 金額: ${}, 開始呼叫 Facilitator 結算", amount);

            // 呼叫 Facilitator /settle
            String settleResponse = restClient.post()
                    .uri("/settle")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(objectMapper.writeValueAsString(settleBody))
                    .retrieve()
                    .body(String.class);

            // 解析結算回應
            @SuppressWarnings("unchecked")
            Map<String, Object> result = objectMapper.readValue(settleResponse, Map.class);

            Boolean success = (Boolean) result.get("success");

            if (Boolean.TRUE.equals(success)) {
                log.info("贊助成功 - 金額: ${}, 交易: {}", amount, result.get("transaction"));

                // 將結算回應 Base64 編碼後放入 header（V2 格式）
                String responseBase64 = Base64.getEncoder()
                        .encodeToString(settleResponse.getBytes());

                HttpHeaders headers = new HttpHeaders();
                headers.set("X-PAYMENT-RESPONSE", responseBase64);

                return ResponseEntity.ok()
                        .headers(headers)
                        .body(Map.of(
                                "success", true,
                                "message", "感謝您的贊助！",
                                "transaction", result.getOrDefault("transaction", "")
                        ));
            } else {
                // 結算失敗
                String errorReason = (String) result.getOrDefault("errorReason", "unknown");
                log.warn("贊助結算失敗 - 金額: ${}, 原因: {}", amount, errorReason);

                return ResponseEntity.status(HttpStatus.PAYMENT_REQUIRED)
                        .body(Map.of(
                                "success", false,
                                "error", "付款結算失敗：" + errorReason
                        ));
            }

        } catch (Exception e) {
            log.error("贊助處理異常 - 金額: ${}", amount, e);
            return ResponseEntity.status(HttpStatus.PAYMENT_REQUIRED)
                    .body(Map.of(
                            "success", false,
                            "error", "付款處理發生錯誤：" + e.getMessage()
                    ));
        }
    }
}
