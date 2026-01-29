# OAuth2 Login Stateless 架構實作總結

## 實作完成時間
2026-01-28

## 實作內容

### 1. 後端實作（Spring Boot）

#### 1.1 CookieAuthorizationRequestRepository
- **檔案**: `backend/src/main/java/.../security/CookieAuthorizationRequestRepository.java`
- **功能**: 將 OAuth2 授權請求存入 Cookie（非 Session），實現無狀態存儲
- **特點**:
  - 序列化/反序列化 OAuth2AuthorizationRequest
  - Cookie 有效期 3 分鐘
  - HttpOnly Cookie 提升安全性

#### 1.2 OAuth2AuthenticationSuccessHandler
- **檔案**: `backend/src/main/java/.../security/OAuth2AuthenticationSuccessHandler.java`
- **功能**: OAuth2 登入成功後的處理邏輯
- **流程**:
  1. 從 OAuth2AuthorizedClientService 取得 access_token
  2. 產生一次性交換碼（60 秒有效）
  3. 重導向到前端 `/#/callback?code=xxx`
- **安全性**:
  - 交換碼一次性使用
  - 60 秒自動過期
  - ConcurrentHashMap 儲存（生產環境建議 Redis）

#### 1.3 AuthController
- **檔案**: `backend/src/main/java/.../web/api/AuthController.java`
- **端點**: `POST /api/auth/exchange`
- **功能**: 用一次性交換碼換取 access_token
- **請求格式**:
  ```json
  { "code": "xxx" }
  ```
- **回應格式**:
  ```json
  {
    "access_token": "...",
    "token_type": "Bearer"
  }
  ```

#### 1.4 SecurityConfig 修改
- **檔案**: `backend/src/main/java/.../config/SecurityConfig.java`
- **主要變更**:
  - 加入 `SessionCreationPolicy.STATELESS`（不依賴 Session）
  - 加入 `/api/auth/**` 公開端點
  - 使用 `CookieAuthorizationRequestRepository` 存儲授權請求
  - 使用 `OAuth2AuthenticationSuccessHandler` 處理登入成功
  - 維持 Resource Server JWT 驗證功能

### 2. 前端實作（React）

#### 2.1 auth.js 修改
- **檔案**: `frontend/src/services/auth.js`
- **主要變更**:
  - `handleCallback()`: 支援 Stateless 模式（檢測 URL code 參數）
  - `getAccessToken()`: 優先從 localStorage 取得 token
  - `isAuthenticated()`: 檢查 localStorage 中的 token
  - `logout()`: 清除 localStorage token
- **向下相容**: 保留 oidc-client-ts 模式支援

#### 2.2 Callback.jsx
- **檔案**: `frontend/src/pages/Callback.jsx`
- **功能**: 已存在，使用 `auth.handleCallback()` 處理回調
- **流程**:
  1. 從 URL 取得 code
  2. 呼叫 `/api/auth/exchange` 換取 token
  3. 儲存到 localStorage
  4. 導向首頁

#### 2.3 App.jsx
- **檔案**: `frontend/src/App.jsx`
- **路由**: `/callback` 路由已存在（第 96 行）

## 認證流程圖

```
使用者訪問受保護 API
        ↓
後端檢測未認證 → 重導向 Auth Server
        ↓
使用者登入 Auth Server
        ↓
Auth Server 回調 /login/oauth2/code/omnihubs
        ↓
後端用 code 換取 access_token
        ↓
產生一次性交換碼（60秒有效）
        ↓
重導向前端 /#/callback?code=xxx
        ↓
前端 Callback 頁面取得 code
        ↓
呼叫 POST /api/auth/exchange
        ↓
後端驗證交換碼 → 返回 access_token
        ↓
前端儲存 token 到 localStorage
        ↓
導向首頁，後續請求帶 Authorization: Bearer <token>
```

## 端點認證規則

| 端點 | 認證要求 | 說明 |
|------|---------|------|
| `/` `/index.html` `/assets/**` | 公開 | 前端 SPA 靜態資源 |
| `/actuator/**` | 公開 | 健康檢查 |
| `/api/search/**` | 公開 | 搜尋 API |
| `/api/dashboard/stats` | 公開 | Dashboard 統計 |
| `/api/config` | 公開 | 前端配置 |
| `/api/auth/**` | 公開 | Token 交換端點 |
| `/login/**` `/oauth2/**` | 公開 | OAuth2 登入相關 |
| `/api/libraries/**` | 需認證 | 函式庫管理 |
| `/api/api-keys/**` | 需認證 | API Key 管理 |
| 其他 `/api/**` | 需認證 | 所有其他 API |

## 測試步驟

### 1. 確認配置
```bash
# 編輯 backend/config/application-secrets.properties
# 確認已填入：
# platform-oauth2-client-id=your-client-id
# platform-oauth2-client-secret=your-client-secret
```

### 2. 啟動後端
```bash
cd backend
./gradlew bootRun --args='--spring.profiles.active=local,dev'
```

### 3. 測試登入流程
1. 瀏覽器訪問 `http://localhost:8080/api/libraries`
2. 自動重導向到 Auth Server 登入頁
3. 登入後重導向到 `http://localhost:8080/#/callback?code=xxx`
4. 前端自動用 code 換取 token
5. 儲存 token 後導向首頁
6. 檢查 localStorage 是否有 `access_token`

### 4. 測試 API 呼叫
```bash
# 取得 token（從瀏覽器 localStorage）
TOKEN="your-token-here"

# 測試受保護端點
curl -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/libraries
# 預期: 200 OK + JSON 資料
```

## 安全注意事項

1. **交換碼安全**
   - 一次性使用
   - 60 秒自動過期
   - 存儲在記憶體（生產環境建議 Redis）

2. **HTTPS 必要性**
   - 生產環境必須使用 HTTPS
   - Cookie 應設定 Secure flag

3. **Token 儲存**
   - 目前使用 localStorage
   - 有 XSS 風險，可考慮 httpOnly Cookie 或 memory-only

4. **Client Secret**
   - 確保 `application-secrets.properties` 在 `.gitignore`
   - 不要提交到版本控制

## 已知限制

1. **交換碼儲存**
   - 目前使用 ConcurrentHashMap（記憶體）
   - 不支援多實例部署
   - 生產環境建議改用 Redis

2. **Token 刷新**
   - 目前未實作 refresh_token 機制
   - Token 過期後需重新登入

3. **Token 驗證**
   - 前端未驗證 token 過期時間
   - 建議加入 token 過期檢查

## 後續改進建議

1. **Redis 整合**
   - 將交換碼存儲改為 Redis
   - 支援分散式部署

2. **Refresh Token**
   - 實作 refresh_token 機制
   - 自動更新過期 token

3. **Token 安全性**
   - 考慮改用 httpOnly Cookie
   - 或實作 memory-only storage

4. **監控與日誌**
   - 加入交換碼使用統計
   - 記錄異常登入行為

## 編譯狀態

✅ Java 編譯成功
- 所有新增的類別編譯通過
- 有一個 deprecation 警告（CookieAuthorizationRequestRepository）

## 參考資料

- [Spring Security OAuth2 Login Advanced](https://docs.spring.io/spring-security/reference/servlet/oauth2/login/advanced.html)
- [Stateless OAuth2 with Spring Boot](https://www.jessym.com/articles/stateless-oauth2-social-logins-with-spring-boot)
