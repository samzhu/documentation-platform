import React, { createContext, useContext, useState, useEffect, useCallback } from 'react';
import { getConfig, getMe, logout as apiLogout } from '../services/api';

/**
 * 認證 Context
 * <p>
 * 提供全應用的認證狀態管理（無狀態 BFF 模式）。
 * Token 存於 HttpOnly Cookie，前端透過 /api/me 端點判斷登入狀態。
 * </p>
 */
const AuthContext = createContext(null);

/**
 * 認證 Provider 組件
 * <p>
 * 實作無狀態 BFF 認證流程：
 * - Token 由後端管理（HttpOnly Cookie）
 * - 前端透過 /api/me 端點判斷登入狀態
 * - 登入重導向到後端 OAuth2 端點
 * - 登出呼叫後端 /api/logout 清除 Cookie
 * </p>
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

        // 2. 如果 OAuth2 啟用，透過 /api/me 檢查認證狀態
        if (config.oauth2Enabled) {
          try {
            const userInfo = await getMe();
            setIsAuthenticated(true);
            setUser(userInfo);
          } catch (error) {
            // 401 或其他錯誤表示未登入
            setIsAuthenticated(false);
            setUser(null);
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
   * <p>
   * 登入成功後，後端會設置 HttpOnly Cookie 並重導向回前端。
   * </p>
   */
  const login = useCallback(() => {
    // 儲存當前頁面，登入後可返回
    sessionStorage.setItem('auth_redirect', window.location.hash || '#/dashboard');
    // 重導向到後端 OAuth2 授權端點
    window.location.href = '/oauth2/authorization/omnihubs';
  }, []);

  /**
   * 登出：呼叫後端 API 清除 Cookie 並重導向
   */
  const logout = useCallback(async () => {
    try {
      await apiLogout();
    } catch (error) {
      console.warn('[Auth] 登出 API 呼叫失敗:', error);
    }
    // 清除本地狀態
    setIsAuthenticated(false);
    setUser(null);
    // 重導向到首頁
    window.location.href = '/';
  }, []);

  /**
   * 重新檢查認證狀態
   * <p>
   * 用於登入後或需要刷新用戶資訊時。
   * </p>
   */
  const refreshAuth = useCallback(async () => {
    if (!oauth2Enabled) {
      return;
    }

    try {
      const userInfo = await getMe();
      setIsAuthenticated(true);
      setUser(userInfo);
    } catch (error) {
      // 401 或其他錯誤表示未登入
      setIsAuthenticated(false);
      setUser(null);
    }
  }, [oauth2Enabled]);

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
 * <p>
 * 必須在 AuthProvider 內使用
 * </p>
 */
export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
}
