/**
 * 頁首組件
 * <p>
 * 顯示頁面標題、通知按鈕、使用者頭像、登出按鈕。
 * 採用 Apple Liquid Glass 設計風格。
 * </p>
 */
import React, { useState, useRef, useEffect } from 'react';
import { useAuth } from '../hooks/useAuth';

/**
 * 通知鈴鐺圖標（20x20 尺寸）
 */
function BellIcon() {
  return (
    <svg
      width="20"
      height="20"
      fill="none"
      stroke="currentColor"
      viewBox="0 0 24 24"
      strokeWidth="1.75"
    >
      <path
        strokeLinecap="round"
        strokeLinejoin="round"
        d="M14.857 17.082a23.848 23.848 0 005.454-1.31A8.967 8.967 0 0118 9.75v-.7V9A6 6 0 006 9v.75a8.967 8.967 0 01-2.312 6.022c1.733.64 3.56 1.085 5.455 1.31m5.714 0a24.255 24.255 0 01-5.714 0m5.714 0a3 3 0 11-5.714 0"
      />
    </svg>
  );
}

/**
 * 登出圖標
 */
function LogoutIcon() {
  return (
    <svg
      width="16"
      height="16"
      fill="none"
      stroke="currentColor"
      viewBox="0 0 24 24"
      strokeWidth="2"
    >
      <path
        strokeLinecap="round"
        strokeLinejoin="round"
        d="M17 16l4-4m0 0l-4-4m4 4H7m6 4v1a3 3 0 01-3 3H6a3 3 0 01-3-3V7a3 3 0 013-3h4a3 3 0 013 3v1"
      />
    </svg>
  );
}

/**
 * Header 組件
 * @param {Object} props - 組件屬性
 * @param {string} props.title - 頁面標題
 */
export default function Header({ title }) {
  const { user, oauth2Enabled, logout } = useAuth();
  const [showDropdown, setShowDropdown] = useState(false);
  const dropdownRef = useRef(null);

  // 點擊外部關閉下拉選單
  useEffect(() => {
    const handleClickOutside = (event) => {
      if (dropdownRef.current && !dropdownRef.current.contains(event.target)) {
        setShowDropdown(false);
      }
    };

    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, []);

  // 取得用戶名稱首字母
  const getInitial = () => {
    if (user?.name) {
      return user.name.charAt(0).toUpperCase();
    }
    if (user?.email) {
      return user.email.charAt(0).toUpperCase();
    }
    return 'U';
  };

  // 處理登出
  const handleLogout = async () => {
    setShowDropdown(false);
    await logout();
  };

  return (
    <header className="header">
      {/* 頂部高光效果層 */}
      <div className="header-highlight" />

      <div className="header-content">
        {/* 頁面標題（左側） */}
        <h1 className="header-title">{title}</h1>

        {/* 右側操作區 */}
        <div className="header-actions">
          {/* 通知按鈕 */}
          <button className="header-icon-btn" aria-label="Notifications">
            <BellIcon />
          </button>

          {/* 使用者頭像與下拉選單 */}
          {oauth2Enabled && (
            <div className="header-user-menu" ref={dropdownRef}>
              <button
                className="header-avatar"
                onClick={() => setShowDropdown(!showDropdown)}
                aria-label="使用者選單"
              >
                <span>{getInitial()}</span>
              </button>

              {/* 下拉選單 */}
              {showDropdown && (
                <div className="header-dropdown">
                  {user && (
                    <div className="header-dropdown-user">
                      <span className="header-dropdown-name">{user.name || '使用者'}</span>
                      {user.email && (
                        <span className="header-dropdown-email">{user.email}</span>
                      )}
                    </div>
                  )}
                  <div className="header-dropdown-divider" />
                  <button
                    className="header-dropdown-item"
                    onClick={handleLogout}
                  >
                    <LogoutIcon />
                    <span>登出</span>
                  </button>
                </div>
              )}
            </div>
          )}

          {/* OAuth2 關閉時顯示預設頭像 */}
          {!oauth2Enabled && (
            <div className="header-avatar">
              <span>A</span>
            </div>
          )}
        </div>
      </div>
    </header>
  );
}
