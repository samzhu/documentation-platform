/**
 * 頁首組件
 * 顯示頁面標題、通知按鈕、使用者頭像
 * 採用 Apple Liquid Glass 設計風格
 */
import React from 'react';

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
 * Header 組件
 * @param {Object} props - 組件屬性
 * @param {string} props.title - 頁面標題
 */
export default function Header({ title }) {
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
          {/* 使用者頭像 */}
          <div className="header-avatar">
            <span>A</span>
          </div>
        </div>
      </div>
    </header>
  );
}
