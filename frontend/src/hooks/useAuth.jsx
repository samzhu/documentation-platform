import React, { createContext, useContext, useState, useEffect, useCallback } from 'react';
import { getConfig } from '../services/api';

/**
 * 認證 Context
 * 提供全應用的認證狀態管理
 */
const AuthContext = createContext(null);

/**
 * 認證 Provider 組件
 * 實作真正的 OAuth2 Stateless 認證流程
 */
export function AuthProvider({ children }) {
  const [loading, setLoading] = useState(true);
  const [oauth2Enabled, setOauth2Enabled] = useState(false);
  const [isAuthenticated, setIsAuthenticated] = useState(false);
  const [user, setUser] = useState(null);

  // 初始化：取得配置和檢查認證狀態
  useEffect(() => {
    const initAuth = async () => {
      try {
        // 1. 從後端取得 OAuth2 啟用狀態
        const config = await getConfig();
        setOauth2Enabled(config.oauth2Enabled || false);

        // 2. 檢查 localStorage 是否有 token
        const token = localStorage.getItem('access_token');
        if (token) {
          setIsAuthenticated(true);
          // 可選：解析 JWT 取得 user info
          try {
            const payload = JSON.parse(atob(token.split('.')[1]));
            setUser({
              sub: payload.sub,
              name: payload.name,
              email: payload.email,
            });
          } catch (e) {
            // Token 格式錯誤，清除它
            console.warn('Token 格式錯誤，已清除:', e);
            localStorage.removeItem('access_token');
            setIsAuthenticated(false);
          }
        }
      } catch (error) {
        console.error('初始化認證失敗:', error);
        // 如果無法取得配置，假設 OAuth2 關閉
        setOauth2Enabled(false);
      } finally {
        setLoading(false);
      }
    };

    initAuth();
  }, []);

  /**
   * 登入：重導向到後端 OAuth2 授權端點
   */
  const login = useCallback(() => {
    // 儲存當前頁面，登入後可返回
    sessionStorage.setItem('auth_redirect', window.location.hash || '#/dashboard');
    // 重導向到後端 OAuth2 授權端點
    window.location.href = '/oauth2/authorization/omnihubs';
  }, []);

  /**
   * 登出：清除 token 並重導向
   */
  const logout = useCallback(() => {
    localStorage.removeItem('access_token');
    setIsAuthenticated(false);
    setUser(null);
    window.location.href = '/';
  }, []);

  /**
   * 重新檢查認證狀態
   * 用於登入後或 token 更新時
   */
  const refreshAuth = useCallback(() => {
    const token = localStorage.getItem('access_token');
    setIsAuthenticated(!!token);
    if (!token) {
      setUser(null);
    } else {
      try {
        const payload = JSON.parse(atob(token.split('.')[1]));
        setUser({
          sub: payload.sub,
          name: payload.name,
          email: payload.email,
        });
      } catch (e) {
        console.warn('Token 格式錯誤:', e);
        setIsAuthenticated(false);
        setUser(null);
      }
    }
  }, []);

  const value = {
    user,
    loading,
    isAuthenticated,
    oauth2Enabled,
    login,
    logout,
    refreshAuth,
  };

  return (
    <AuthContext.Provider value={value}>
      {children}
    </AuthContext.Provider>
  );
}

/**
 * 使用認證 Hook
 * 必須在 AuthProvider 內使用
 */
export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
}
