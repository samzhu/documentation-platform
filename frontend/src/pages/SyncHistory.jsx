/**
 * 同步記錄頁面
 * 顯示文件庫同步歷史和統計
 */
import React, { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import api from '../services/api';
import {
  SyncIcon,
  CheckCircleIcon,
  XCircleIcon,
  BoltIcon,
  RefreshIcon,
  ClockIcon,
  EyeIcon
} from '../components/Icons';

export default function SyncHistory() {
  const [history, setHistory] = useState([]);
  const [libraries, setLibraries] = useState([]);
  const [loading, setLoading] = useState(true);
  const [stats, setStats] = useState({ total: 0, success: 0, failed: 0, running: 0 });

  // 篩選條件
  const [filterLibrary, setFilterLibrary] = useState('');
  const [filterStatus, setFilterStatus] = useState('');

  useEffect(() => {
    loadData();
  }, []);

  /**
   * 載入同步記錄和文件庫列表
   */
  const loadData = async () => {
    try {
      setLoading(true);
      const [syncData, libData] = await Promise.all([
        api.getSyncHistory(),
        api.getLibraries()
      ]);

      const records = syncData || [];
      setHistory(records);
      setLibraries(libData || []);

      // 計算統計
      const newStats = records.reduce((acc, r) => {
        acc.total++;
        if (r.status === 'SUCCESS') acc.success++;
        if (r.status === 'FAILED') acc.failed++;
        if (r.status === 'RUNNING') acc.running++;
        return acc;
      }, { total: 0, success: 0, failed: 0, running: 0 });

      setStats(newStats);
    } catch (err) {
      console.error('載入同步記錄失敗:', err);
    } finally {
      setLoading(false);
    }
  };

  /**
   * 篩選後的記錄
   */
  const filteredHistory = history.filter(record => {
    if (filterLibrary && record.libraryId !== filterLibrary) return false;
    if (filterStatus && record.status !== filterStatus) return false;
    return true;
  });

  /**
   * 格式化日期
   */
  const formatDate = (dateStr) => {
    if (!dateStr) return '-';
    return new Date(dateStr).toLocaleString('zh-TW', {
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit',
      second: '2-digit'
    });
  };

  /**
   * 計算持續時間
   */
  const formatDuration = (startedAt, completedAt) => {
    if (!startedAt || !completedAt) return '-';
    const start = new Date(startedAt);
    const end = new Date(completedAt);
    const diffMs = end - start;
    const diffSec = Math.floor(diffMs / 1000);

    if (diffSec < 60) return `${diffSec}s`;
    if (diffSec < 3600) return `${Math.floor(diffSec / 60)}m ${diffSec % 60}s`;
    return `${Math.floor(diffSec / 3600)}h ${Math.floor((diffSec % 3600) / 60)}m`;
  };

  /**
   * 取得狀態圖標和樣式
   */
  const getStatusBadge = (status) => {
    switch (status) {
      case 'SUCCESS':
        return (
          <span className="status-badge status-success">
            <CheckCircleIcon size={14} />
            <span style={{ marginLeft: '4px' }}>成功</span>
          </span>
        );
      case 'FAILED':
        return (
          <span className="status-badge status-failed">
            <XCircleIcon size={14} />
            <span style={{ marginLeft: '4px' }}>失敗</span>
          </span>
        );
      case 'RUNNING':
        return (
          <span className="status-badge status-running">
            <BoltIcon size={14} />
            <span style={{ marginLeft: '4px' }}>執行中</span>
          </span>
        );
      default:
        return <span className="status-badge">{status}</span>;
    }
  };

  if (loading) return <div className="loading">載入中...</div>;

  return (
    <div className="page sync-history">
      <header className="page-header">
        <h2>同步記錄</h2>
        <button className="btn" onClick={loadData}>
          <RefreshIcon size={18} />
          重新整理
        </button>
      </header>

      {/* 統計卡片 */}
      <div className="stats-grid mb-6">
        <div className="stat-card">
          <div className="stat-icon blue">
            <SyncIcon size={24} />
          </div>
          <div className="stat-content">
            <div className="stat-value">{stats.total}</div>
            <div className="stat-label">總計</div>
          </div>
        </div>

        <div className="stat-card">
          <div className="stat-icon green">
            <CheckCircleIcon size={24} />
          </div>
          <div className="stat-content">
            <div className="stat-value">{stats.success}</div>
            <div className="stat-label">成功</div>
          </div>
        </div>

        <div className="stat-card">
          <div className="stat-icon red">
            <XCircleIcon size={24} />
          </div>
          <div className="stat-content">
            <div className="stat-value">{stats.failed}</div>
            <div className="stat-label">失敗</div>
          </div>
        </div>

        <div className="stat-card">
          <div className="stat-icon yellow">
            <BoltIcon size={24} />
          </div>
          <div className="stat-content">
            <div className="stat-value">{stats.running}</div>
            <div className="stat-label">執行中</div>
          </div>
        </div>
      </div>

      {/* 篩選列 */}
      <div className="filter-row">
        <div className="filter-group">
          <label>文件庫</label>
          <select
            value={filterLibrary}
            onChange={e => setFilterLibrary(e.target.value)}
          >
            <option value="">全部</option>
            {libraries.map(lib => (
              <option key={lib.id} value={lib.id}>{lib.name}</option>
            ))}
          </select>
        </div>

        <div className="filter-group">
          <label>狀態</label>
          <select
            value={filterStatus}
            onChange={e => setFilterStatus(e.target.value)}
          >
            <option value="">全部</option>
            <option value="SUCCESS">成功</option>
            <option value="FAILED">失敗</option>
            <option value="RUNNING">執行中</option>
          </select>
        </div>
      </div>

      {/* 同步記錄表格 */}
      <div className="table-container glass-card-static">
        <table className="data-table">
          <thead>
            <tr>
              <th>文件庫</th>
              <th>狀態</th>
              <th>開始時間</th>
              <th>持續時間</th>
              <th>文件數</th>
              <th>片段數</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            {filteredHistory.length === 0 ? (
              <tr>
                <td colSpan="7" className="empty-state">
                  <ClockIcon size={24} style={{ display: 'inline', marginRight: '8px', verticalAlign: 'middle' }} />
                  尚無同步記錄
                </td>
              </tr>
            ) : (
              filteredHistory.map(record => (
                <tr key={record.id}>
                  <td>
                    <strong>{record.libraryName || '-'}</strong>
                    {record.version && (
                      <span className="text-sm text-muted ml-2">v{record.version}</span>
                    )}
                  </td>
                  <td>{getStatusBadge(record.status)}</td>
                  <td className="text-sm">{formatDate(record.startedAt)}</td>
                  <td className="text-sm">{formatDuration(record.startedAt, record.completedAt)}</td>
                  <td>{record.documentsProcessed || 0}</td>
                  <td>{record.chunksCreated || 0}</td>
                  <td>
                    <Link to={`/sync/${record.id}`} className="btn btn-sm">
                      <EyeIcon size={14} />
                      Details
                    </Link>
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>

      {/* 顯示篩選後的數量 */}
      {(filterLibrary || filterStatus) && (
        <div className="text-sm text-muted mt-4">
          顯示 {filteredHistory.length} / {history.length} 筆記錄
        </div>
      )}
    </div>
  );
}
