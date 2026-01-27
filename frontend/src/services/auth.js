/**
 * OAuth2 認證服務
 * 使用 oidc-client-ts 處理 Authorization Code + PKCE 流程
 */
import { UserManager, WebStorageStateStore } from 'oidc-client-ts';

// OAuth2 配置
const authConfig = {
  authority: 'https://auth-dev.omnihubs.cloud/',
  client_id: 'documentation-platform',
  redirect_uri: `${window.location.origin}/callback`,
  post_logout_redirect_uri: window.location.origin,
  response_type: 'code',
  scope: 'openid profile',
  automaticSilentRenew: true,
  userStore: new WebStorageStateStore({ store: window.localStorage }),
};

// 建立 UserManager 實例
let userManager = null;

try {
  userManager = new UserManager(authConfig);

  // 監聽 Token 過期事件
  userManager.events.addAccessTokenExpired(() => {
    console.warn('Access token 已過期');
  });

  // 監聽靜默更新失敗事件
  userManager.events.addSilentRenewError((error) => {
    console.error('靜默更新失敗:', error);
  });

  console.log('[Auth] OAuth2 認證已初始化');
} catch (error) {
  console.warn('[Auth] OAuth2 初始化失敗:', error.message);
}

/**
 * 認證服務 API
 */
const auth = {
  /**
   * 登入（重導向到 OAuth2 Server）
   */
  login: async () => {
    if (!userManager) {
      console.log('[Auth] 開發模式：登入功能禁用');
      return;
    }
    try {
      await userManager.signinRedirect();
    } catch (error) {
      console.error('登入失敗:', error);
      throw error;
    }
  },

  /**
   * 登出
   */
  logout: async () => {
    if (!userManager) {
      console.log('[Auth] 開發模式：登出功能禁用');
      return;
    }
    try {
      await userManager.signoutRedirect();
    } catch (error) {
      console.error('登出失敗:', error);
      await userManager.removeUser();
      window.location.href = '/';
    }
  },

  /**
   * 取得目前使用者
   */
  getUser: async () => {
    if (!userManager) return null;
    try {
      const user = await userManager.getUser();
      if (user && !user.expired) {
        return user;
      }
      return null;
    } catch (error) {
      console.error('取得使用者資訊失敗:', error);
      return null;
    }
  },

  /**
   * 取得 Access Token
   */
  getAccessToken: async () => {
    if (!userManager) return null;
    const user = await auth.getUser();
    return user?.access_token || null;
  },

  /**
   * 處理 OAuth2 回調
   */
  handleCallback: async () => {
    if (!userManager) return null;
    try {
      const user = await userManager.signinRedirectCallback();
      return user;
    } catch (error) {
      console.error('處理 OAuth2 回調失敗:', error);
      throw error;
    }
  },

  /**
   * 檢查是否已登入
   */
  isAuthenticated: async () => {
    const user = await auth.getUser();
    return user !== null;
  },

  /**
   * 靜默更新 Token
   */
  silentRenew: async () => {
    if (!userManager) return null;
    try {
      const user = await userManager.signinSilent();
      return user;
    } catch (error) {
      console.error('靜默更新失敗:', error);
      return null;
    }
  },
};

export default auth;
