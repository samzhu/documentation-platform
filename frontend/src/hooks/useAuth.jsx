/**
 * 認證 Hook 和 Context
 * 目前認證功能已關閉，提供模擬的認證狀態
 */
import React, { createContext, useContext } from 'react';

// 建立認證 Context
const AuthContext = createContext(null);

/**
 * 認證 Provider 組件
 * 認證功能已關閉，直接提供已認證狀態
 */
export function AuthProvider({ children }) {
  const value = {
    user: null,
    loading: false,
    isAuthenticated: true,  // 認證功能關閉，視為已認證
    oauth2Enabled: false,   // OAuth2 已停用
    login: () => {},        // 空操作
    logout: () => {},       // 空操作
    refreshAuth: () => {},  // 空操作
  };

  return (
    <AuthContext.Provider value={value}>
      {children}
    </AuthContext.Provider>
  );
}

/**
 * 使用認證 Hook
 */
export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
}
