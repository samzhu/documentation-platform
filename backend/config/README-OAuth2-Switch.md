# OAuth2 認證開關使用說明

## 概述

在 `local,dev` 開發環境中,提供 OAuth2 認證開關,讓工程師可以選擇：
1. **功能測試模式**(預設)：關閉 OAuth2,所有 API 直接訪問
2. **OAuth2 整合測試模式**：啟用 OAuth2,測試完整認證流程

## 配置方式

### 功能測試模式(預設)

**檔案**: `config/application-dev.yaml`

```yaml
platform:
  features:
    oauth2: false  # 預設值
```

**行為**:
- ✅ 不會嘗試連接 Auth Server
- ✅ 所有 API 直接訪問(無認證)
- ✅ 適合本機功能開發與測試
- ✅ 不需要在 Auth Server 白名單內

### OAuth2 整合測試模式

**檔案**: `config/application-dev.yaml`

```yaml
platform:
  features:
    oauth2: true  # 手動改為 true
```

**行為**:
- ⚠️ 會連接 Auth Server 的 .well-known/openid-configuration
- ⚠️ 需在 Auth Server 的 IP 白名單內
- ✅ 測試完整 OAuth2 登入流程

## 實作原理

### 1. OAuth2AutoConfigurationExcluder

條件性排除 Spring Boot 的 OAuth2 自動配置：

```java
@Configuration
@ConditionalOnProperty(
    name = "platform.features.oauth2",
    havingValue = "false",
    matchIfMissing = true
)
@EnableAutoConfiguration(exclude = {
    OAuth2ClientAutoConfiguration.class,
    OAuth2ResourceServerAutoConfiguration.class
})
public class OAuth2AutoConfigurationExcluder {
    // 排除 OAuth2 自動配置
}
```

### 2. 條件性 Bean

所有 OAuth2 相關的 Bean 都使用 `@ConditionalOnProperty` 條件性載入：

- `OAuth2AuthenticationSuccessHandler`
- `CookieAuthorizationRequestRepository`
- `AuthController`
- `SecurityConfig.securityFilterChainOAuth2()`

### 3. SecurityConfig

提供兩個 SecurityFilterChain：
- `securityFilterChainDev()`: `oauth2=false` 時生效,允許所有請求
- `securityFilterChainOAuth2()`: `oauth2=true` 時生效,啟用 OAuth2 認證

## 驗證步驟

### 1. 功能測試模式驗證

```bash
cd backend
./gradlew bootRun --args='--spring.profiles.active=local,dev'
```

**預期結果**:
- ✅ 啟動成功,不會嘗試連接 Auth Server
- ✅ 日誌中沒有 OAuth2 相關錯誤

### 2. 測試 API 訪問

```bash
# 測試受保護端點(應該可以直接訪問)
curl http://localhost:8080/api/libraries
# 預期: 200 OK

curl http://localhost:8080/actuator/health
# 預期: 200 OK,狀態為 UP
```

### 3. OAuth2 整合測試(可選)

修改 `config/application-dev.yaml`:

```yaml
platform:
  features:
    oauth2: true
```

重新啟動:

```bash
./gradlew bootRun --args='--spring.profiles.active=local,dev'
```

**預期結果**:
- ⚠️ 如果不在白名單內,會看到 403 錯誤
- ✅ 如果在白名單內,會成功連接 Auth Server

## 安全注意事項

1. **僅限開發環境**: `config/application-dev.yaml` 不會包進 Docker Image
2. **預設安全**: `matchIfMissing = true` 確保未設定時預設關閉 OAuth2
3. **明確標示**: 配置檔中清楚標示開關用途

## 參考資料

- [Spring Boot - Auto-configuration](https://docs.spring.io/spring-boot/reference/using/auto-configuration.html)
- [GitHub - spring-boot-autoconfigure-exclude](https://github.com/fineconstant/spring-boot-autoconfigure-exclude)
- [Baeldung - Disable Security for a Profile](https://www.baeldung.com/spring-security-disable-profile)
