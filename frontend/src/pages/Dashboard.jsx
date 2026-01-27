/**
 * 儀表板頁面
 * 顯示系統統計概覽、最近同步活動、快速操作
 * 設計對齊 docmcp-server 原始設計
 */
import React, { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import api from '../services/api';

/**
 * Libraries 圖標（藍色）
 */
function LibrariesIcon() {
  return (
    <svg fill="none" stroke="currentColor" viewBox="0 0 24 24" width="26" height="26">
      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2"
            d="M19 11H5m14 0a2 2 0 012 2v6a2 2 0 01-2 2H5a2 2 0 01-2-2v-6a2 2 0 012-2m14 0V9a2 2 0 00-2-2M5 11V9a2 2 0 012-2m0 0V5a2 2 0 012-2h6a2 2 0 012 2v2M7 7h10"/>
    </svg>
  );
}

/**
 * Documents 圖標（綠色）
 */
function DocumentsIcon() {
  return (
    <svg fill="none" stroke="currentColor" viewBox="0 0 24 24" width="26" height="26">
      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2"
            d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z"/>
    </svg>
  );
}

/**
 * Chunks 圖標（黃色/橙色）
 */
function ChunksIcon() {
  return (
    <svg fill="none" stroke="currentColor" viewBox="0 0 24 24" width="26" height="26">
      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2"
            d="M4 7v10c0 2.21 3.582 4 8 4s8-1.79 8-4V7M4 7c0 2.21 3.582 4 8 4s8-1.79 8-4M4 7c0-2.21 3.582-4 8-4s8 1.79 8 4m0 5c0 2.21-3.582 4-8 4s-8-1.79-8-4"/>
    </svg>
  );
}

/**
 * 同步圖標（空狀態用）
 */
function SyncEmptyIcon() {
  return (
    <svg fill="none" stroke="currentColor" viewBox="0 0 24 24" width="72" height="72">
      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2"
            d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15"/>
    </svg>
  );
}

/**
 * 加號圖標（Add Library）
 */
function PlusIcon() {
  return (
    <svg fill="none" stroke="currentColor" viewBox="0 0 24 24" width="26" height="26">
      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M12 4v16m8-8H4"/>
    </svg>
  );
}

/**
 * 搜尋圖標（Test Search）
 */
function SearchIcon() {
  return (
    <svg fill="none" stroke="currentColor" viewBox="0 0 24 24" width="26" height="26">
      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2"
            d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z"/>
    </svg>
  );
}

export default function Dashboard() {
  const [stats, setStats] = useState(null);
  const [recentSyncs, setRecentSyncs] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    loadData();
  }, []);

  /**
   * 載入統計資料和最近同步記錄
   */
  const loadData = async () => {
    try {
      setLoading(true);
      const [statsData, syncData] = await Promise.all([
        api.getStats(),
        api.getSyncHistory()
      ]);
      setStats(statsData);
      // 只取最近 5 筆同步記錄
      setRecentSyncs((syncData || []).slice(0, 5));
    } catch (err) {
      setError('無法載入資料');
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  /**
   * 格式化日期
   */
  const formatDate = (dateStr) => {
    if (!dateStr) return '-';
    return new Date(dateStr).toLocaleString('en-US', {
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit'
    });
  };

  /**
   * 取得狀態徽章
   */
  const getStatusBadge = (status) => {
    switch (status) {
      case 'SUCCESS':
        return <span className="badge badge-success">Success</span>;
      case 'RUNNING':
        return <span className="badge badge-info">Running</span>;
      case 'FAILED':
        return <span className="badge badge-danger">Failed</span>;
      case 'PENDING':
        return <span className="badge badge-warning">Pending</span>;
      default:
        return <span className="badge badge-secondary">{status}</span>;
    }
  };

  if (loading) return <div className="loading">Loading...</div>;
  if (error) return <div className="error">{error}</div>;

  return (
    <div className="page dashboard">
      {/* 統計概覽卡片 */}
      <div className="stats-grid mb-6">
        {/* Libraries 統計 */}
        <div className="stat-card">
          <div className="stat-icon blue">
            <LibrariesIcon />
          </div>
          <div>
            <p className="stat-value">{stats?.libraryCount || 0}</p>
            <p className="stat-label">Libraries</p>
          </div>
        </div>

        {/* Documents 統計 */}
        <div className="stat-card">
          <div className="stat-icon green">
            <DocumentsIcon />
          </div>
          <div>
            <p className="stat-value">{stats?.documentCount || 0}</p>
            <p className="stat-label">Documents</p>
          </div>
        </div>

        {/* Chunks 統計 */}
        <div className="stat-card">
          <div className="stat-icon yellow">
            <ChunksIcon />
          </div>
          <div>
            <p className="stat-value">{stats?.chunkCount || 0}</p>
            <p className="stat-label">Chunks</p>
          </div>
        </div>
      </div>

      {/* 最近同步活動 */}
      <div className="glass-card-static mb-6">
        <div className="flex items-center justify-between mb-4">
          <h2 className="text-lg font-semibold text-primary">Recent Sync Activity</h2>
          <Link to="/sync-history" className="text-sm text-accent hover:underline">View All</Link>
        </div>

        {recentSyncs.length === 0 ? (
          /* 空狀態 */
          <div className="empty-state">
            <SyncEmptyIcon />
            <p className="font-medium">No sync activity yet</p>
            <p className="text-sm mt-2">Start by adding a library and triggering a sync</p>
          </div>
        ) : (
          /* 同步記錄表格 */
          <table className="table">
            <thead>
              <tr>
                <th>Version ID</th>
                <th>Status</th>
                <th>Documents</th>
                <th>Chunks</th>
                <th>Started</th>
              </tr>
            </thead>
            <tbody>
              {recentSyncs.map(sync => (
                <tr key={sync.id}>
                  <td className="font-mono text-sm">
                    {sync.versionId ? sync.versionId.substring(0, 12) : '-'}
                  </td>
                  <td>{getStatusBadge(sync.status)}</td>
                  <td>{sync.documentCount || sync.documentsProcessed || 0}</td>
                  <td>{sync.chunkCount || sync.chunksCreated || 0}</td>
                  <td>{formatDate(sync.startedAt)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>

      {/* 快速操作 */}
      <div className="action-cards-grid">
        {/* 新增 Library */}
        <Link to="/libraries/new" className="action-card">
          <div className="action-card-icon bg-indigo-100 text-accent">
            <PlusIcon />
          </div>
          <div className="action-card-content">
            <h3>Add Library</h3>
            <p>Register a new documentation library</p>
          </div>
        </Link>

        {/* 測試搜尋 */}
        <Link to="/search" className="action-card">
          <div className="action-card-icon bg-green-100 text-success">
            <SearchIcon />
          </div>
          <div className="action-card-content">
            <h3>Test Search</h3>
            <p>Try full-text and semantic search</p>
          </div>
        </Link>
      </div>
    </div>
  );
}
