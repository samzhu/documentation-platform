/**
 * Documentation Platform - 主應用組件
 * 使用 React 19 + React Router 7 (HashRouter)
 * 設計風格：Apple Liquid Glass
 */
import React from 'react';
import { HashRouter, Routes, Route, useLocation } from 'react-router-dom';
import { AuthProvider, useAuth } from './hooks/useAuth';
import Sidebar from './components/Sidebar';
import Header from './components/Header';
import RequireAuth from './components/RequireAuth';

// 頁面組件
import Landing from './pages/Landing';
import Dashboard from './pages/Dashboard';
import Libraries from './pages/Libraries';
import LibraryForm from './pages/LibraryForm';
import LibraryDetail from './pages/LibraryDetail';
import DocumentList from './pages/DocumentList';
import DocumentDetail from './pages/DocumentDetail';
import Search from './pages/Search';
import SyncHistory from './pages/SyncHistory';
import SyncDetail from './pages/SyncDetail';
import ApiKeys from './pages/ApiKeys';
import Settings from './pages/Settings';
import Setup from './pages/Setup';
import Callback from './pages/Callback';

// 頁面標題對應表
const pageTitles = {
  '/': 'Home',
  '/dashboard': 'Dashboard',
  '/libraries': 'Libraries',
  '/libraries/new': 'New Library',
  '/search': 'Search',
  '/sync-history': 'Sync Status',
  '/api-keys': 'API Keys',
  '/settings': 'Settings',
  '/setup': 'Setup',
  '/documents': 'Documents',
  '/callback': 'Authentication',
};

/**
 * 應用內容組件
 * 包含側邊欄、頁首和主要路由
 */
function AppContent() {
  const { loading } = useAuth();
  const location = useLocation();

  // 判斷是否為全螢幕頁面（不顯示 Sidebar 和 Header）
  const isFullscreenPage = ['/', '/callback'].includes(location.pathname);

  /**
   * 取得目前頁面標題
   * 處理動態路由（如 /libraries/:id）
   */
  const getPageTitle = () => {
    // 處理 /libraries/:id/edit 動態路由
    if (location.pathname.match(/^\/libraries\/[^/]+\/edit$/)) {
      return 'Edit Library';
    }
    // 處理 /libraries/:id 動態路由
    if (location.pathname.startsWith('/libraries/') &&
        location.pathname !== '/libraries/new') {
      return 'Library Detail';
    }
    // 處理 /documents/:id 動態路由
    if (location.pathname.startsWith('/documents/')) {
      return 'Document Detail';
    }
    // 處理 /sync/:id 動態路由
    if (location.pathname.startsWith('/sync/')) {
      return 'Sync Detail';
    }
    return pageTitles[location.pathname] || 'Documentation Platform';
  };

  // 等待認證初始化完成
  if (loading) {
    return <div className="app-loading">Loading...</div>;
  }

  // 全螢幕頁面（Landing、Callback）：不顯示 Sidebar 和 Header
  if (isFullscreenPage) {
    return (
      <Routes>
        <Route path="/" element={<Landing />} />
        <Route path="/callback" element={<Callback />} />
      </Routes>
    );
  }

  // 一般頁面：顯示完整佈局
  return (
    <div className="app-container">
      {/* 側邊欄導航 */}
      <Sidebar />

      {/* 主內容區容器（包含 Header 和內容） */}
      <div className="app-main">
        {/* 頁首 */}
        <Header title={getPageTitle()} />

        {/* 內容區 */}
        <main className="app-content">
          <Routes>
            {/* 公開頁面 - 不需要認證 */}
            <Route path="/search" element={<Search />} />
            <Route path="/setup" element={<Setup />} />

            {/* 受保護頁面 - 需要認證 */}
            <Route path="/dashboard" element={
              <RequireAuth><Dashboard /></RequireAuth>
            } />
            <Route path="/libraries" element={
              <RequireAuth><Libraries /></RequireAuth>
            } />
            {/* 新增文件庫頁面（必須放在 /libraries/:id 之前） */}
            <Route path="/libraries/new" element={
              <RequireAuth><LibraryForm /></RequireAuth>
            } />
            {/* 編輯文件庫頁面 */}
            <Route path="/libraries/:id/edit" element={
              <RequireAuth><LibraryForm /></RequireAuth>
            } />
            <Route path="/libraries/:id" element={
              <RequireAuth><LibraryDetail /></RequireAuth>
            } />
            <Route path="/documents" element={
              <RequireAuth><DocumentList /></RequireAuth>
            } />
            <Route path="/documents/:id" element={
              <RequireAuth><DocumentDetail /></RequireAuth>
            } />
            <Route path="/sync-history" element={
              <RequireAuth><SyncHistory /></RequireAuth>
            } />
            <Route path="/sync/:id" element={
              <RequireAuth><SyncDetail /></RequireAuth>
            } />
            <Route path="/api-keys" element={
              <RequireAuth><ApiKeys /></RequireAuth>
            } />
            <Route path="/settings" element={
              <RequireAuth><Settings /></RequireAuth>
            } />
          </Routes>
        </main>
      </div>
    </div>
  );
}

/**
 * 主應用組件
 */
export default function App() {
  return (
    <HashRouter>
      <AuthProvider>
        <AppContent />
      </AuthProvider>
    </HashRouter>
  );
}
