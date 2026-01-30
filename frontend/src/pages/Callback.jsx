/**
 * OAuth2 回調頁面（無狀態 BFF 模式）
 * <p>
 * 登入成功後，後端已設置 HttpOnly Cookie 並重導向到此頁面。
 * 此頁面只需刷新認證狀態並重導向到目標頁面。
 * </p>
 */
import React, { useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../hooks/useAuth';

export default function Callback() {
  const navigate = useNavigate();
  const { refreshAuth } = useAuth();

  useEffect(() => {
    handleCallback();
  }, []);

  /**
   * 處理 OAuth2 回調
   * <p>
   * 登入成功後，Cookie 已由後端設置。
   * 只需刷新認證狀態並重導向到目標頁面。
   * </p>
   */
  const handleCallback = async () => {
    try {
      // 刷新認證狀態（從 /api/me 取得用戶資訊）
      await refreshAuth();

      // 取得儲存的重導向路徑，預設為 /dashboard
      const redirect = sessionStorage.getItem('auth_redirect') || '#/dashboard';
      sessionStorage.removeItem('auth_redirect');
      navigate(redirect.replace('#', '') || '/dashboard');
    } catch (err) {
      console.error('OAuth2 回調處理失敗:', err);
      // 錯誤時也重導向到 dashboard（認證狀態會由 RequireAuth 處理）
      navigate('/dashboard');
    }
  };

  // 處理中顯示載入狀態
  return (
    <div className="callback-page">
      <div className="loading">登入處理中...</div>
    </div>
  );
}
