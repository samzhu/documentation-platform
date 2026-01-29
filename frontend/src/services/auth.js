/**
 * 認證服務 - Stateless 模式
 * Token 儲存在 localStorage
 */

/**
 * 取得 Access Token
 */
export function getAccessToken() {
  return localStorage.getItem('access_token');
}

/**
 * 處理 OAuth2 回調
 * 從 URL 取得交換碼，呼叫後端 API 換取 access_token
 */
export async function handleCallback() {
  // 從 URL 取得交換碼（支援 query string 和 hash 兩種方式）
  const urlParams = new URLSearchParams(window.location.search);
  const hashParams = new URLSearchParams(window.location.hash.split('?')[1] || '');
  const code = urlParams.get('code') || hashParams.get('code');

  if (!code) {
    throw new Error('缺少授權碼');
  }

  // 用交換碼換取 access_token
  const response = await fetch('/api/auth/exchange', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ code }),
  });

  if (!response.ok) {
    const error = await response.json();
    throw new Error(error.error_description || '交換 Token 失敗');
  }

  const data = await response.json();

  // 儲存 Token 到 localStorage
  localStorage.setItem('access_token', data.access_token);

  console.log('[Auth] Stateless 登入成功');
  return data;
}

/**
 * 檢查是否已認證
 */
export function isAuthenticated() {
  return !!localStorage.getItem('access_token');
}

/**
 * 登出
 */
export function logout() {
  localStorage.removeItem('access_token');
}

export default {
  getAccessToken,
  handleCallback,
  isAuthenticated,
  logout,
};
