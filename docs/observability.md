# 可觀測性筆記（Spring Boot 4.0.2 + OpenTelemetry）

## spring-boot-starter-opentelemetry 已包含的傳遞依賴

| 依賴 | 版本 | 用途 |
|------|------|------|
| `micrometer-tracing-bridge-otel` | 1.6.2 | Micrometer Observation → OTel Traces |
| `micrometer-registry-otlp` | 1.16.2 | Micrometer Metrics → OTLP HTTP |
| `opentelemetry-exporter-otlp` | 1.55.0 | OTel SDK → OTLP gRPC/HTTP |
| `spring-boot-opentelemetry` | 4.0.2 | OTel SDK 自動配置 |

## 不需要額外宣告的依賴

- ❌ `micrometer-registry-otlp`（已包含，冗餘）
- ❌ `micrometer-tracing-bridge-brave`（與 bridge-otel **互斥**）
- ❌ `spring-boot-micrometer-tracing-brave`（Brave 自動配置，不需要）

## 正確的最小依賴組合

```groovy
implementation 'org.springframework.boot:spring-boot-starter-actuator'
implementation 'org.springframework.boot:spring-boot-starter-opentelemetry'
implementation 'net.ttddyy.observation:datasource-micrometer-spring-boot'
implementation 'net.ttddyy.observation:datasource-micrometer-opentelemetry'
```

## OTLP 傳輸協定限制

| 訊號 | 協定 | 端點 | 原因 |
|------|------|------|------|
| Traces | gRPC | `:4317` | OTel SDK 原生支援 |
| Logs | gRPC | `:4317` | OTel SDK 原生支援 |
| **Metrics** | **HTTP** | **`:4318/v1/metrics`** | Micrometer 僅實作 HTTP |

gRPC Metrics 支援追蹤：https://github.com/micrometer-metrics/micrometer/issues/5040

## Cloud Run 部署

- 沒有內建 OTLP collector
- 方案 A（推薦）：Sidecar collector（`otelcol-google`），三種訊號都支援
- 方案 B：直接送 `telemetry.googleapis.com`（見下方限制）

## Console Logging 策略

- Production（`application.yaml`）：`logging.console.enabled: false`
  - 關閉 stdout，避免 Cloud Run 擷取產生無 traceId 的 `textPayload` 重複 logs
  - 所有 logs 僅走 OTLP → Collector → Cloud Logging（自動帶 trace 關聯）
- Dev（`config/application-dev.yaml`）：`logging.console.enabled: true`
  - 本地無 Collector，需要 console 輸出
- 屬性來源：Spring Boot 4.0.0-M2 新增（[Issue #46592](https://github.com/spring-projects/spring-boot/issues/46592)）

## GCP telemetry.googleapis.com — OTLP 原生端點

> 評估日期：2026-02 ｜ 結論：**暫不採用，維持 Collector Sidecar**

### 背景

Google 2026-03-23 起自動為現有 GCP 專案啟用 `telemetry.googleapis.com` API，
與 Cloud Logging / Trace / Monitoring 現有 API 綁定。僅 API 啟用，不影響現有服務。

### 支援狀況

| Signal | 直接 OTLP | 狀態 | 備註 |
|--------|-----------|------|------|
| Traces | ✅ | GA | endpoint: `https://telemetry.googleapis.com` |
| Metrics | ✅ | **Pre-GA** | 配額 60,000 req/min，計費走 Prometheus SKU |
| **Logs** | ❌ | **不支援** | 無 `/v1/logs` 端點 |

### 直接送（無 Collector）的限制

1. **Logs 不支援** — 我們依賴 OTLP Logs pipeline 做 trace 關聯，這是最大阻礙
2. **認證需 `opentelemetry-gcp-auth-extension`**（仍為 alpha `1.52.0-alpha`），處理 ADC token 動態刷新
3. **HTTP exporter 不建議** — Google 建議 SDK 直送只用 gRPC（token 刷新問題），但 Micrometer Metrics OTLP 僅支援 HTTP
4. **Metrics Pre-GA** — 可能有 breaking changes

### 重新評估條件

以下條件皆滿足時，可考慮移除 Collector Sidecar 改為直送：

- [ ] `telemetry.googleapis.com` 支援 OTLP Logs
- [ ] `opentelemetry-gcp-auth-extension` 脫離 alpha（≥ 1.0）
- [ ] Micrometer Metrics OTLP 支援 gRPC（[Issue #5040](https://github.com/micrometer-metrics/micrometer/issues/5040)）

### 參考連結

- [Telemetry API overview](https://docs.cloud.google.com/stackdriver/docs/reference/telemetry/overview)
- [OTLP Metrics overview](https://docs.cloud.google.com/stackdriver/docs/otlp-metrics/overview)
- [Migrate to OTLP endpoints](https://docs.cloud.google.com/stackdriver/docs/instrumentation/migrate-to-otlp-endpoints)
- [Cloud Run OTel Collector](https://docs.cloud.google.com/stackdriver/docs/instrumentation/opentelemetry-collector-cloud-run)

## datasource-micrometer

- 版本透過 BOM 管理：`datasource-micrometer-bom:2.1.0`
- `datasource-micrometer-spring-boot`：JDBC 追蹤自動配置
- `datasource-micrometer-opentelemetry`：加上 OTel 語意規範（db.operation、db.statement）
