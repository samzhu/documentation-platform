/**
 * OAuth2 回調頁面
 * 處理 OAuth2 認證回調
 */
import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../hooks/useAuth';
import auth from '../services/auth';

export default function Callback() {
  const navigate = useNavigate();
  const { refreshAuth } = useAuth();
  const [error, setError] = useState(null);

  useEffect(() => {
    handleCallback();
  }, []);

  /**
   * 處理 OAuth2 回調
   */
  const handleCallback = async () => {
    try {
      await auth.handleCallback();
      await refreshAuth();

      // 取得儲存的重導向路徑，預設為 /dashboard
      const redirect = sessionStorage.getItem('auth_redirect') || '#/dashboard';
      sessionStorage.removeItem('auth_redirect');
      navigate(redirect.replace('#', '') || '/dashboard');
    } catch (err) {
      console.error('OAuth2 回調處理失敗:', err);
      setError('登入失敗，請重試');
    }
  };

  // 錯誤狀態
  if (error) {
    return (
      <div className="callback-page">
        <div className="glass-card">
          <h2>登入失敗</h2>
          <p>{error}</p>
          <button className="btn btn-primary" onClick={() => navigate('/')}>
            返回首頁
          </button>
        </div>
      </div>
    );
  }

  // 處理中
  return (
    <div className="callback-page">
      <div className="loading">處理登入中...</div>
    </div>
  );
}
