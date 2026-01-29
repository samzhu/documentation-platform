import { useAuth } from '../hooks/useAuth';

/**
 * 需要認證的頁面包裝器
 * OAuth2 啟用且未認證時，自動觸發登入流程
 */
export default function RequireAuth({ children }) {
  const { isAuthenticated, oauth2Enabled, loading, login } = useAuth();

  // 載入中，顯示 loading 狀態
  if (loading) {
    return (
      <div className="auth-loading">
        <div className="loading-spinner" />
        <p>載入中...</p>
      </div>
    );
  }

  // OAuth2 已啟用但未認證，觸發登入流程
  if (oauth2Enabled && !isAuthenticated) {
    // 觸發登入
    login();
    return (
      <div className="auth-loading">
        <div className="loading-spinner" />
        <p>正在導向登入頁面...</p>
      </div>
    );
  }

  // OAuth2 關閉或已認證，放行
  return children;
}
