/**
 * 同步詳情頁面
 * 顯示單一同步記錄的詳細資訊
 */
import React, { useState, useEffect } from 'react';
import { useParams, Link, useNavigate } from 'react-router-dom';
import api from '../services/api';
import {
  SyncIcon,
  ChevronRightIcon,
  CheckCircleIcon,
  XCircleIcon,
  BoltIcon,
  ClockIcon,
  FileTextIcon,
  LayersIcon,
  AlertTriangleIcon,
  ExternalLinkIcon,
  RefreshIcon
} from '../components/Icons';

export default function SyncDetail() {
  const { id } = useParams();
  const navigate = useNavigate();

  // 狀態
  const [syncRecord, setSyncRecord] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    loadSyncDetail();
  }, [id]);

  /**
   * 載入同步記錄詳情
   */
  const loadSyncDetail = async () => {
    try {
      setLoading(true);
      setError(null);
      const data = await api.getSyncDetail(id);
      setSyncRecord(data);
    } catch (err) {
      console.error('載入同步記錄失敗:', err);
      setError(err.message);
    } finally {
      setLoading(false);
    }
  };

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
  const formatDuration = (startedAt, finishedAt) => {
    if (!startedAt) return '-';
    const start = new Date(startedAt);
    const end = finishedAt ? new Date(finishedAt) : new Date();
    const diffMs = end - start;
    const diffSec = Math.floor(diffMs / 1000);

    if (diffSec < 60) return `${diffSec} 秒`;
    if (diffSec < 3600) return `${Math.floor(diffSec / 60)} 分 ${diffSec % 60} 秒`;
    return `${Math.floor(diffSec / 3600)} 時 ${Math.floor((diffSec % 3600) / 60)} 分`;
  };

  /**
   * 取得狀態圖標和樣式
   */
  const getStatusInfo = (status) => {
    switch (status) {
      case 'SUCCESS':
        return {
          icon: <CheckCircleIcon size={24} />,
          label: '同步成功',
          className: 'status-success-card',
          iconClass: 'green'
        };
      case 'FAILED':
        return {
          icon: <XCircleIcon size={24} />,
          label: '同步失敗',
          className: 'status-failed-card',
          iconClass: 'red'
        };
      case 'RUNNING':
        return {
          icon: <BoltIcon size={24} />,
          label: '執行中',
          className: 'status-running-card',
          iconClass: 'blue'
        };
      default:
        return {
          icon: <SyncIcon size={24} />,
          label: status,
          className: '',
          iconClass: 'blue'
        };
    }
  };

  if (loading) {
    return <div className="loading">載入中...</div>;
  }

  if (error) {
    return (
      <div className="page">
        <div className="error glass-card-static" style={{ textAlign: 'center', padding: '2rem' }}>
          <h3>載入失敗</h3>
          <p>{error}</p>
          <button className="btn btn-primary mt-4" onClick={() => navigate('/sync-history')}>
            返回同步記錄
          </button>
        </div>
      </div>
    );
  }

  if (!syncRecord) {
    return (
      <div className="page">
        <div className="error glass-card-static" style={{ textAlign: 'center', padding: '2rem' }}>
          <h3>找不到同步記錄</h3>
          <button className="btn btn-primary mt-4" onClick={() => navigate('/sync-history')}>
            返回同步記錄
          </button>
        </div>
      </div>
    );
  }

  const statusInfo = getStatusInfo(syncRecord.status);

  return (
    <div className="page sync-detail">
      {/* 麵包屑導航 */}
      <nav className="breadcrumb mb-4">
        <Link to="/sync-history" className="breadcrumb-item">同步記錄</Link>
        <ChevronRightIcon size={14} className="breadcrumb-separator" />
        <span className="breadcrumb-item active">同步詳情</span>
      </nav>

      {/* 頁面標題 */}
      <header className="page-header">
        <div className="flex items-center gap-4">
          <div className={`sync-status-icon stat-icon ${statusInfo.iconClass}`}>
            {statusInfo.icon}
          </div>
          <div>
            <h2>{statusInfo.label}</h2>
            <p className="text-muted mt-2">
              {syncRecord.libraryName}
              {syncRecord.version && ` - ${syncRecord.version}`}
            </p>
          </div>
        </div>
        <button className="btn" onClick={loadSyncDetail}>
          <RefreshIcon size={18} />
          重新整理
        </button>
      </header>

      {/* 基本資訊 */}
      <div className="stats-grid mb-6">
        <div className="stat-card">
          <div className="stat-icon blue">
            <ClockIcon size={24} />
          </div>
          <div className="stat-content">
            <div className="stat-label">開始時間</div>
            <div className="stat-value text-sm">{formatDate(syncRecord.startedAt)}</div>
          </div>
        </div>

        <div className="stat-card">
          <div className="stat-icon green">
            <ClockIcon size={24} />
          </div>
          <div className="stat-content">
            <div className="stat-label">完成時間</div>
            <div className="stat-value text-sm">
              {syncRecord.status === 'RUNNING' ? '執行中...' : formatDate(syncRecord.finishedAt)}
            </div>
          </div>
        </div>

        <div className="stat-card">
          <div className="stat-icon yellow">
            <SyncIcon size={24} />
          </div>
          <div className="stat-content">
            <div className="stat-label">執行時間</div>
            <div className="stat-value text-sm">
              {formatDuration(syncRecord.startedAt, syncRecord.finishedAt)}
            </div>
          </div>
        </div>

        <div className="stat-card">
          <div className="stat-icon purple">
            <FileTextIcon size={24} />
          </div>
          <div className="stat-content">
            <div className="stat-value">{syncRecord.documentCount || 0}</div>
            <div className="stat-label">處理文件數</div>
          </div>
        </div>
      </div>

      {/* 處理結果 */}
      <div className="glass-card-static mb-6">
        <h3 className="section-title mb-4">處理結果</h3>
        <div className="sync-results-grid">
          <div className="result-item">
            <span className="result-icon">
              <FileTextIcon size={20} />
            </span>
            <div className="result-content">
              <div className="result-value">{syncRecord.documentCount || 0}</div>
              <div className="result-label">Documents Processed</div>
            </div>
          </div>
          <div className="result-item">
            <span className="result-icon">
              <LayersIcon size={20} />
            </span>
            <div className="result-content">
              <div className="result-value">{syncRecord.chunkCount || 0}</div>
              <div className="result-label">Chunks Created</div>
            </div>
          </div>
        </div>
      </div>

      {/* 錯誤訊息（如果有） */}
      {syncRecord.status === 'FAILED' && syncRecord.errorMessage && (
        <div className="warning-box mb-6">
          <span className="warning-icon">
            <AlertTriangleIcon size={20} />
          </span>
          <div className="warning-content">
            <strong>錯誤訊息：</strong>
            <pre className="error-message mt-2">{syncRecord.errorMessage}</pre>
          </div>
        </div>
      )}

      {/* 相關連結 */}
      <div className="glass-card-static">
        <h3 className="section-title mb-4">相關連結</h3>
        <div className="flex gap-4">
          {syncRecord.libraryId && (
            <Link to={`/libraries/${syncRecord.libraryId}`} className="btn">
              <ExternalLinkIcon size={18} />
              查看文件庫
            </Link>
          )}
          {syncRecord.libraryId && syncRecord.versionId && (
            <Link
              to={`/documents?libraryId=${syncRecord.libraryId}&versionId=${syncRecord.versionId}`}
              className="btn"
            >
              <FileTextIcon size={18} />
              查看文件
            </Link>
          )}
        </div>
      </div>
    </div>
  );
}
