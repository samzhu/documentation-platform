/**
 * 認證服務（無狀態 BFF 模式）
 * <p>
 * Token 存於 HttpOnly Cookie，由瀏覽器自動管理。
 * 前端不直接處理 Token，只透過 /api/me 端點判斷登入狀態。
 * </p>
 */

import { getMe, logout as apiLogout } from './api';

/**
 * 檢查是否已認證
 * <p>
 * 透過呼叫 /api/me 端點判斷。
 * 如果 Cookie 中有有效的 Token，後端會返回用戶資訊。
 * </p>
 *
 * @returns {Promise<Object|null>} 用戶資訊或 null
 */
export async function checkAuth() {
  try {
    const user = await getMe();
    return user;
  } catch (error) {
    // 401 或其他錯誤表示未登入
    return null;
  }
}

/**
 * 登入
 * <p>
 * 重導向到後端 OAuth2 授權端點。
 * 登入成功後，後端會設置 HttpOnly Cookie 並重導向回前端。
 * </p>
 */
export function login() {
  // 儲存當前頁面，登入後可返回
  sessionStorage.setItem('auth_redirect', window.location.hash || '#/dashboard');
  // 重導向到後端 OAuth2 授權端點
  window.location.href = '/oauth2/authorization/omnihubs';
}

/**
 * 登出
 * <p>
 * 呼叫後端 /api/logout 清除 Cookie，然後重導向到首頁。
 * </p>
 */
export async function logout() {
  try {
    await apiLogout();
  } catch (error) {
    console.warn('[Auth] 登出 API 呼叫失敗:', error);
  }
  // 無論 API 成功與否，都重導向到首頁
  window.location.href = '/';
}

/**
 * 取得登入後的重導向路徑
 */
export function getRedirectPath() {
  const redirect = sessionStorage.getItem('auth_redirect') || '#/dashboard';
  sessionStorage.removeItem('auth_redirect');
  return redirect.replace('#', '') || '/dashboard';
}

export default {
  checkAuth,
  login,
  logout,
  getRedirectPath,
};
