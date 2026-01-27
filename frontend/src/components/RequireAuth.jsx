/**
 * 需要認證的頁面包裝器
 * 目前認證功能已關閉，直接返回子組件
 */
import React from 'react';

export default function RequireAuth({ children }) {
  // 認證功能已關閉，直接返回子組件
  return children;
}
