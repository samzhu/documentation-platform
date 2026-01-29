/**
 * API 服務
 * 提供與後端 REST API 通訊的方法
 */
import auth from './auth';

const BASE_URL = '/api';

/**
 * 通用 HTTP 請求方法
 * 自動加入 OAuth2 Bearer Token（如果已登入）
 */
async function request(endpoint, options = {}) {
  const url = `${BASE_URL}${endpoint}`;

  // 取得 Access Token（如果已登入）
  const token = await auth.getAccessToken();

  const config = {
    headers: {
      'Content-Type': 'application/json',
      // 如果有 Token，加入 Authorization Header
      ...(token && { 'Authorization': `Bearer ${token}` }),
      ...options.headers,
    },
    ...options,
  };

  try {
    const response = await fetch(url, config);

    // 處理 401 未授權：Token 過期或無效
    if (response.status === 401) {
      console.warn('[API] Token 過期或無效，清除認證狀態');
      localStorage.removeItem('access_token');
      // 重導向到首頁觸發重新登入
      window.location.href = '/#/';
      throw new Error('認證已過期，請重新登入');
    }

    // 處理無內容回應
    if (response.status === 204) {
      return null;
    }

    // 解析 JSON 回應
    const data = await response.json();

    // 檢查 HTTP 錯誤
    if (!response.ok) {
      throw new Error(data.message || `HTTP 錯誤: ${response.status}`);
    }

    return data;
  } catch (error) {
    console.error(`API 請求失敗: ${endpoint}`, error);
    throw error;
  }
}

// =====================
// Libraries API（文件庫）
// =====================

/**
 * 取得所有文件庫列表
 */
export async function getLibraries() {
  return request('/libraries');
}

/**
 * 取得單一文件庫詳情
 */
export async function getLibrary(id) {
  return request(`/libraries/${id}`);
}

/**
 * 建立新文件庫
 */
export async function createLibrary(library) {
  return request('/libraries', {
    method: 'POST',
    body: JSON.stringify(library),
  });
}

/**
 * 更新文件庫
 */
export async function updateLibrary(id, library) {
  return request(`/libraries/${id}`, {
    method: 'PUT',
    body: JSON.stringify(library),
  });
}

/**
 * 刪除文件庫
 */
export async function deleteLibrary(id) {
  return request(`/libraries/${id}`, {
    method: 'DELETE',
  });
}

// =====================
// Versions API（版本）
// =====================

/**
 * 取得文件庫的所有版本
 */
export async function getVersions(libraryId) {
  return request(`/libraries/${libraryId}/versions`);
}

/**
 * 建立新版本
 */
export async function createVersion(libraryId, version) {
  return request(`/libraries/${libraryId}/versions`, {
    method: 'POST',
    body: JSON.stringify(version),
  });
}

// =====================
// Sync API（同步）
// =====================

/**
 * 觸發文件庫同步
 */
export async function triggerSync(libraryId, versionId = null) {
  const endpoint = versionId
    ? `/sync/libraries/${libraryId}/versions/${versionId}`
    : `/sync/libraries/${libraryId}`;
  return request(endpoint, {
    method: 'POST',
  });
}

/**
 * 取得同步歷史記錄
 */
export async function getSyncHistory(params = {}) {
  const queryString = new URLSearchParams(params).toString();
  const endpoint = queryString ? `/sync/history?${queryString}` : '/sync/history';
  return request(endpoint);
}

// =====================
// Search API（搜尋）
// =====================

/**
 * 執行語意搜尋
 */
export async function search(params) {
  const queryString = new URLSearchParams({
    q: params.query,
    ...(params.libraryId && { libraryId: params.libraryId }),
    ...(params.version && { version: params.version }),
    ...(params.mode && { mode: params.mode }),
    ...(params.limit && { limit: params.limit.toString() }),
  }).toString();
  return request(`/search?${queryString}`);
}

// =====================
// API Keys API（API 金鑰）
// =====================

/**
 * 取得所有 API 金鑰
 */
export async function getApiKeys() {
  return request('/api-keys');
}

/**
 * 建立新 API 金鑰
 */
export async function createApiKey(apiKey) {
  return request('/api-keys', {
    method: 'POST',
    body: JSON.stringify(apiKey),
  });
}

/**
 * 撤銷 API 金鑰
 */
export async function revokeApiKey(id) {
  return request(`/api-keys/${id}/revoke`, {
    method: 'POST',
  });
}

// =====================
// Dashboard API（儀表板）
// =====================

/**
 * 取得儀表板統計資料
 */
export async function getStats() {
  return request('/dashboard/stats');
}

// =====================
// Config API（配置）
// =====================

/**
 * 取得前端配置
 * 包含 OAuth2 啟用狀態等資訊
 */
export async function getConfig() {
  return request('/config');
}

/**
 * 取得系統設定
 * 包含 Feature Flags、同步設定、系統資訊
 */
export async function getSettings() {
  return request('/config/settings');
}

// =====================
// Documents API（文件）
// =====================

/**
 * 取得文件列表
 */
export async function getDocuments(params = {}) {
  const queryString = new URLSearchParams(params).toString();
  const endpoint = queryString ? `/documents?${queryString}` : '/documents';
  return request(endpoint);
}

/**
 * 取得單一文件詳情
 */
export async function getDocument(id) {
  return request(`/documents/${id}`);
}

// =====================
// Sync Detail API（同步詳情）
// =====================

/**
 * 取得單一同步記錄詳情
 */
export async function getSyncDetail(id) {
  return request(`/sync/${id}`);
}

// =====================
// Versions API（版本擴充）
// =====================

/**
 * 更新版本
 */
export async function updateVersion(libraryId, versionId, version) {
  return request(`/libraries/${libraryId}/versions/${versionId}`, {
    method: 'PUT',
    body: JSON.stringify(version),
  });
}

/**
 * 刪除版本
 */
export async function deleteVersion(libraryId, versionId) {
  return request(`/libraries/${libraryId}/versions/${versionId}`, {
    method: 'DELETE',
  });
}

/**
 * 從 GitHub Releases 取得可用版本
 */
export async function getGitHubReleases(libraryId) {
  return request(`/libraries/${libraryId}/github-releases`);
}

/**
 * 同步 GitHub Release 版本
 */
export async function syncGitHubRelease(libraryId, releaseTag, docsPath = 'docs') {
  return request(`/libraries/${libraryId}/sync-release`, {
    method: 'POST',
    body: JSON.stringify({ releaseTag, docsPath }),
  });
}

/**
 * 批次同步多個 GitHub Release 版本
 * @param {string} libraryId - 文件庫 ID
 * @param {Object} data - 批次同步資料
 * @param {Array} data.versions - 版本列表（每個版本包含 tagName, version, docsPath）
 * @param {string} data.defaultDocsPath - 預設文件路徑
 */
export async function batchSync(libraryId, data) {
  return request(`/libraries/${libraryId}/batch-sync`, {
    method: 'POST',
    body: JSON.stringify(data),
  });
}

// 匯出所有 API 方法
const api = {
  getLibraries,
  getLibrary,
  createLibrary,
  updateLibrary,
  deleteLibrary,
  getVersions,
  createVersion,
  updateVersion,
  deleteVersion,
  getGitHubReleases,
  syncGitHubRelease,
  batchSync,
  triggerSync,
  getSyncHistory,
  getSyncDetail,
  search,
  getApiKeys,
  createApiKey,
  revokeApiKey,
  getStats,
  getConfig,
  getSettings,
  getDocuments,
  getDocument,
};

export default api;
