/**
 * 文件庫頁面
 * 顯示文件庫列表，管理刪除和同步
 */
import React, { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import api from '../services/api';
import {
  PlusIcon,
  GithubIcon,
  FolderIcon,
  SyncIcon,
  TrashIcon,
  ExternalLinkIcon,
  ChevronRightIcon
} from '../components/Icons';

export default function Libraries() {
  const [libraries, setLibraries] = useState([]);
  const [loading, setLoading] = useState(true);
  const [syncing, setSyncing] = useState({});

  useEffect(() => {
    loadLibraries();
  }, []);

  /**
   * 載入文件庫列表
   */
  const loadLibraries = async () => {
    try {
      setLoading(true);
      const data = await api.getLibraries();
      setLibraries(data || []);
    } catch (err) {
      console.error('載入文件庫失敗:', err);
    } finally {
      setLoading(false);
    }
  };

  /**
   * 刪除文件庫
   */
  const handleDelete = async (id) => {
    if (!confirm('確定要刪除此文件庫嗎？所有相關的文件和向量索引都會被移除。')) return;
    try {
      await api.deleteLibrary(id);
      loadLibraries();
    } catch (err) {
      alert('刪除失敗: ' + err.message);
    }
  };

  /**
   * 觸發同步
   */
  const handleSync = async (id) => {
    try {
      setSyncing(prev => ({ ...prev, [id]: true }));
      await api.triggerSync(id);
      alert('同步已開始，請稍後查看同步記錄');
    } catch (err) {
      alert('同步失敗: ' + err.message);
    } finally {
      setSyncing(prev => ({ ...prev, [id]: false }));
    }
  };

  /**
   * 取得來源類型圖標
   */
  const getSourceIcon = (sourceType) => {
    switch (sourceType) {
      case 'GITHUB':
        return <GithubIcon size={16} />;
      case 'LOCAL':
        return <FolderIcon size={16} />;
      default:
        return null;
    }
  };

  /**
   * 取得來源類型標籤
   */
  const getSourceLabel = (sourceType) => {
    switch (sourceType) {
      case 'GITHUB': return 'GitHub';
      case 'LOCAL': return '本地';
      default: return sourceType;
    }
  };

  if (loading) return <div className="loading">載入中...</div>;

  return (
    <div className="page libraries">
      <header className="page-header">
        <h2>文件庫</h2>
        {/* 新增按鈕改為連結到獨立頁面 */}
        <Link to="/libraries/new" className="btn btn-primary">
          <PlusIcon size={18} />
          新增文件庫
        </Link>
      </header>

      {/* 文件庫列表 */}
      {libraries.length === 0 ? (
        <div className="empty-state glass-card-static" style={{ textAlign: 'center', padding: '3rem' }}>
          <div className="empty-state-icon">
            <FolderIcon size={64} />
          </div>
          <h3>尚無文件庫</h3>
          <p>點擊上方按鈕建立您的第一個文件庫</p>
        </div>
      ) : (
        <div className="library-cards-grid">
          {libraries.map(lib => (
            <div key={lib.id} className="library-card glass-card">
              {/* 標題列 */}
              <div className="library-header">
                <Link to={`/libraries/${lib.id}`} className="library-name-link">
                  <h3 className="library-name">{lib.displayName || lib.name}</h3>
                </Link>
                <span className="badge badge-primary">
                  {getSourceIcon(lib.sourceType)}
                  <span style={{ marginLeft: '4px' }}>{getSourceLabel(lib.sourceType)}</span>
                </span>
              </div>

              {/* 名稱識別碼（灰色小字） */}
              <p className="text-sm text-muted mb-2" style={{ fontFamily: 'var(--font-mono)' }}>
                {lib.name}
              </p>

              {/* 描述 */}
              {lib.description && (
                <p className="library-desc">{lib.description}</p>
              )}

              {/* 來源連結 */}
              {lib.sourceUrl && (
                <div className="library-meta">
                  <a
                    href={lib.sourceUrl}
                    target="_blank"
                    rel="noopener noreferrer"
                    className="text-sm text-muted flex items-center gap-2"
                    style={{ wordBreak: 'break-all' }}
                  >
                    {lib.sourceUrl}
                    <ExternalLinkIcon size={14} />
                  </a>
                </div>
              )}

              {/* 標籤 */}
              {lib.tags && lib.tags.length > 0 && (
                <div className="tags mb-4">
                  {lib.tags.map((tag, idx) => (
                    <span key={idx} className="tag">{tag}</span>
                  ))}
                </div>
              )}

              {/* 統計 */}
              <div className="flex gap-4 text-sm text-muted mb-4">
                <span>{lib.documentCount || 0} 文件</span>
                <span>{lib.chunkCount || 0} 片段</span>
              </div>

              {/* 操作按鈕 */}
              <div className="card-actions">
                <Link to={`/libraries/${lib.id}`} className="btn btn-sm btn-ghost">
                  <ChevronRightIcon size={16} />
                  詳情
                </Link>
                <button
                  className={`btn btn-sm btn-sync ${syncing[lib.id] ? 'syncing' : ''}`}
                  onClick={() => handleSync(lib.id)}
                  disabled={syncing[lib.id]}
                >
                  <SyncIcon size={16} />
                  {syncing[lib.id] ? '同步中...' : '同步'}
                </button>
                <button
                  className="btn btn-sm btn-danger"
                  onClick={() => handleDelete(lib.id)}
                >
                  <TrashIcon size={16} />
                  刪除
                </button>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
