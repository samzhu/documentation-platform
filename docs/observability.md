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
- 方案 B：直接送 `telemetry.googleapis.com`（僅 Traces）

## datasource-micrometer

- 版本透過 BOM 管理：`datasource-micrometer-bom:2.1.0`
- `datasource-micrometer-spring-boot`：JDBC 追蹤自動配置
- `datasource-micrometer-opentelemetry`：加上 OTel 語意規範（db.operation、db.statement）
