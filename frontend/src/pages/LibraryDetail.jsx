/**
 * 文件庫詳情頁面
 * 顯示文件庫資訊和版本管理
 * 實作多選版本列表與批次同步功能
 */
import React, { useState, useEffect } from 'react';
import { useParams, Link, useNavigate } from 'react-router-dom';
import api from '../services/api';
import {
  GithubIcon,
  FolderIcon,
  SyncIcon,
  TrashIcon,
  ExternalLinkIcon,
  ChevronRightIcon,
  TagIcon,
  FileTextIcon,
  EditIcon,
  InfoIcon
} from '../components/Icons';

export default function LibraryDetail() {
  const { id } = useParams();
  const navigate = useNavigate();

  // 狀態
  const [library, setLibrary] = useState(null);
  const [versions, setVersions] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [syncing, setSyncing] = useState({});

  // 多選同步 Modal 狀態
  const [showSyncModal, setShowSyncModal] = useState(false);
  const [releases, setReleases] = useState([]);
  const [loadingReleases, setLoadingReleases] = useState(false);
  const [selectedReleases, setSelectedReleases] = useState(new Set());
  const [defaultDocsPath, setDefaultDocsPath] = useState('docs');
  const [batchSyncing, setBatchSyncing] = useState(false);

  useEffect(() => {
    loadData();
  }, [id]);

  /**
   * 載入文件庫和版本資料
   */
  const loadData = async () => {
    try {
      setLoading(true);
      setError(null);
      const [libData, versionsData] = await Promise.all([
        api.getLibrary(id),
        api.getVersions(id)
      ]);
      setLibrary(libData);
      setVersions(versionsData || []);
    } catch (err) {
      console.error('載入資料失敗:', err);
      setError(err.message);
    } finally {
      setLoading(false);
    }
  };

  /**
   * 開啟同步版本 Modal 並載入 GitHub Releases
   */
  const handleOpenSyncModal = async () => {
    setShowSyncModal(true);
    setSelectedReleases(new Set());

    try {
      setLoadingReleases(true);
      const data = await api.getGitHubReleases(id);
      const releasesList = data?.releases || [];

      // 取得已存在的版本號
      const existingVersions = new Set(versions.map(v => v.version));

      // 標記已存在的版本
      const releasesWithStatus = releasesList.map(release => ({
        ...release,
        exists: existingVersions.has(release.version || release.tagName)
      }));

      setReleases(releasesWithStatus);

      // 設定預設文件路徑
      if (data?.defaultDocsPath) {
        setDefaultDocsPath(data.defaultDocsPath);
      }
    } catch (err) {
      console.error('載入 GitHub Releases 失敗:', err);
      alert('無法取得 GitHub Releases: ' + err.message);
      setReleases([]);
    } finally {
      setLoadingReleases(false);
    }
  };

  /**
   * 關閉同步 Modal
   */
  const handleCloseSyncModal = () => {
    setShowSyncModal(false);
    setSelectedReleases(new Set());
    setReleases([]);
  };

  /**
   * 切換單一版本選擇
   */
  const toggleReleaseSelection = (tagName) => {
    const newSelected = new Set(selectedReleases);
    if (newSelected.has(tagName)) {
      newSelected.delete(tagName);
    } else {
      newSelected.add(tagName);
    }
    setSelectedReleases(newSelected);
  };

  /**
   * 切換全選
   */
  const toggleSelectAll = () => {
    const selectableReleases = releases.filter(r => !r.exists);
    const allSelected = selectableReleases.every(r => selectedReleases.has(r.tagName));

    if (allSelected) {
      // 取消全選
      setSelectedReleases(new Set());
    } else {
      // 全選（只選可選擇的）
      setSelectedReleases(new Set(selectableReleases.map(r => r.tagName)));
    }
  };

  /**
   * 批次同步選中的版本
   */
  const handleBatchSync = async () => {
    if (selectedReleases.size === 0) {
      alert('請至少選擇一個版本');
      return;
    }

    try {
      setBatchSyncing(true);

      // 準備要同步的版本資料
      const versionsToSync = releases
        .filter(r => selectedReleases.has(r.tagName))
        .map(r => ({
          tagName: r.tagName,
          version: r.version || r.tagName,
          docsPath: r.docsPath || defaultDocsPath
        }));

      // 呼叫批次同步 API
      await api.batchSync(id, { versions: versionsToSync, defaultDocsPath });

      alert(`已開始同步 ${versionsToSync.length} 個版本，請稍後查看同步記錄`);
      handleCloseSyncModal();
      loadData();
    } catch (err) {
      alert('批次同步失敗: ' + err.message);
    } finally {
      setBatchSyncing(false);
    }
  };

  /**
   * 觸發單一版本同步
   */
  const handleSyncVersion = async (versionId) => {
    try {
      setSyncing(prev => ({ ...prev, [versionId]: true }));
      await api.triggerSync(id, versionId);
      alert('同步已開始，請稍後查看同步記錄');
    } catch (err) {
      alert('同步失敗: ' + err.message);
    } finally {
      setSyncing(prev => ({ ...prev, [versionId]: false }));
    }
  };

  /**
   * 刪除版本
   */
  const handleDeleteVersion = async (versionId) => {
    if (!confirm('確定要刪除此版本嗎？所有相關的文件和向量索引都會被移除。')) return;
    try {
      await api.deleteVersion(id, versionId);
      loadData();
    } catch (err) {
      alert('刪除版本失敗: ' + err.message);
    }
  };

  /**
   * 刪除文件庫
   */
  const handleDeleteLibrary = async () => {
    if (!confirm('確定要刪除此文件庫嗎？所有版本、文件和向量索引都會被移除。此操作無法復原。')) return;
    try {
      await api.deleteLibrary(id);
      navigate('/libraries');
    } catch (err) {
      alert('刪除文件庫失敗: ' + err.message);
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
   * 取得狀態徽章
   */
  const getStatusBadge = (status) => {
    switch (status) {
      case 'ACTIVE':
        return <span className="badge badge-success">Active</span>;
      case 'DEPRECATED':
        return <span className="badge badge-warning">Deprecated</span>;
      case 'EOL':
        return <span className="badge badge-danger">EOL</span>;
      default:
        return <span className="badge badge-default">{status}</span>;
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
      minute: '2-digit'
    });
  };

  /**
   * 格式化簡短日期（用於版本列表）
   */
  const formatShortDate = (dateStr) => {
    if (!dateStr) return '-';
    return new Date(dateStr).toLocaleDateString('zh-TW', {
      year: 'numeric',
      month: 'long',
      day: 'numeric'
    });
  };

  // 計算可選擇的版本數
  const selectableReleases = releases.filter(r => !r.exists);
  const allSelected = selectableReleases.length > 0 &&
    selectableReleases.every(r => selectedReleases.has(r.tagName));

  if (loading) {
    return <div className="loading">載入中...</div>;
  }

  if (error) {
    return (
      <div className="page">
        <div className="error glass-card-static" style={{ textAlign: 'center', padding: '2rem' }}>
          <h3>載入失敗</h3>
          <p>{error}</p>
          <button className="btn btn-primary mt-4" onClick={() => navigate('/libraries')}>
            返回文件庫列表
          </button>
        </div>
      </div>
    );
  }

  if (!library) {
    return (
      <div className="page">
        <div className="error glass-card-static" style={{ textAlign: 'center', padding: '2rem' }}>
          <h3>找不到文件庫</h3>
          <button className="btn btn-primary mt-4" onClick={() => navigate('/libraries')}>
            返回文件庫列表
          </button>
        </div>
      </div>
    );
  }

  return (
    <div className="page library-detail">
      {/* 麵包屑導航 */}
      <nav className="breadcrumb mb-4">
        <Link to="/libraries" className="breadcrumb-item">Libraries</Link>
        <ChevronRightIcon size={14} className="breadcrumb-separator" />
        <span className="breadcrumb-item active">{library.displayName || library.name}</span>
      </nav>

      {/* 文件庫資訊卡片 */}
      <div className="glass-card-static mb-6">
        {/* 卡片標題 */}
        <div className="info-card-header">
          <div>
            <h2 className="info-card-title">{library.displayName || library.name}</h2>
            <span className="info-card-subtitle" style={{ fontFamily: 'var(--font-mono)' }}>
              {library.name}
            </span>
          </div>
          <div className="info-card-actions">
            <button className="btn btn-sm" onClick={() => navigate(`/libraries/${id}/edit`)}>
              <EditIcon size={16} />
              Edit
            </button>
            <button className="btn btn-sm btn-danger" onClick={handleDeleteLibrary}>
              <TrashIcon size={16} />
              Delete
            </button>
          </div>
        </div>

        {/* 描述 */}
        {library.description && (
          <div className="info-card-body">
            <p className="info-card-description">{library.description}</p>
          </div>
        )}

        {/* 來源類型和分類（2 欄） */}
        <div className="info-grid-2">
          <div className="info-item">
            <span className="info-label">SOURCE TYPE</span>
            <span className="info-value flex items-center gap-2">
              {getSourceIcon(library.sourceType)}
              {library.sourceType === 'GITHUB' ? 'GitHub' : library.sourceType}
            </span>
          </div>
          <div className="info-item">
            <span className="info-label">CATEGORY</span>
            <span className="info-value">{library.category || '-'}</span>
          </div>
        </div>

        {/* 來源 URL（全寬） */}
        <div className="info-item-full">
          <span className="info-label">SOURCE URL</span>
          <div className="source-url-container mt-2">
            <a
              href={library.sourceUrl}
              target="_blank"
              rel="noopener noreferrer"
            >
              {library.sourceUrl}
              <ExternalLinkIcon size={14} />
            </a>
          </div>
        </div>

        {/* 標籤 */}
        {library.tags && library.tags.length > 0 && (
          <div className="info-tags-section">
            <span className="info-tags-label">Tags</span>
            <div className="tags mt-2">
              {library.tags.map((tag, idx) => (
                <span key={idx} className="tag">{tag}</span>
              ))}
            </div>
          </div>
        )}
      </div>

      {/* 版本區塊 */}
      <div className="versions-section">
        <div className="versions-header">
          <h3 className="versions-title">Versions</h3>
          {/* 單一按鈕：Sync Version（只有 GitHub 來源才顯示） */}
          {library.sourceType === 'GITHUB' && (
            <button className="btn btn-primary" onClick={handleOpenSyncModal}>
              <SyncIcon size={18} />
              Sync Version
            </button>
          )}
        </div>

        {/* 版本列表 */}
        <div className="glass-card-static">
          {versions.length === 0 ? (
            /* 版本空狀態 */
            <div className="versions-empty">
              <div className="versions-empty-icon">
                <TagIcon size={64} />
              </div>
              <h4>No versions yet</h4>
              <p>Click "Sync Version" to add a new version from GitHub</p>
            </div>
          ) : (
            /* 版本表格 */
            <div className="table-container">
              <table className="data-table">
                <thead>
                  <tr>
                    <th>版本</th>
                    <th>狀態</th>
                    <th>文件路徑</th>
                    <th>文件數</th>
                    <th>最後同步</th>
                    <th>操作</th>
                  </tr>
                </thead>
                <tbody>
                  {versions.map(version => (
                    <tr key={version.id}>
                      <td>
                        <div className="flex items-center gap-2">
                          <strong>{version.version}</strong>
                          {version.isLatest && (
                            <span className="badge badge-primary">Latest</span>
                          )}
                          {version.isLts && (
                            <span className="badge badge-info">LTS</span>
                          )}
                        </div>
                      </td>
                      <td>{getStatusBadge(version.status)}</td>
                      <td className="text-sm text-muted">{version.docsPath || 'docs'}</td>
                      <td>{version.documentCount || 0}</td>
                      <td className="text-sm">{formatDate(version.lastSyncAt)}</td>
                      <td>
                        <div className="flex gap-2">
                          <Link
                            to={`/documents?libraryId=${id}&versionId=${version.id}`}
                            className="btn btn-sm"
                          >
                            <FileTextIcon size={14} />
                            文件
                          </Link>
                          <button
                            className={`btn btn-sm btn-sync ${syncing[version.id] ? 'syncing' : ''}`}
                            onClick={() => handleSyncVersion(version.id)}
                            disabled={syncing[version.id]}
                          >
                            <SyncIcon size={14} />
                            {syncing[version.id] ? '同步中...' : '同步'}
                          </button>
                          <button
                            className="btn btn-sm btn-danger"
                            onClick={() => handleDeleteVersion(version.id)}
                          >
                            <TrashIcon size={14} />
                          </button>
                        </div>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>
      </div>

      {/* 多選版本同步 Modal */}
      {showSyncModal && (
        <div className="modal-overlay" onClick={handleCloseSyncModal}>
          <div className="modal modal-wide version-select-modal" onClick={e => e.stopPropagation()}>
            {/* Modal 標題 */}
            <div className="version-select-header">
              <div>
                <h3>從 GitHub 同步版本</h3>
                <p className="subtitle">選擇要同步的版本，系統將建立版本並下載文件</p>
              </div>
            </div>

            {/* 提示資訊 */}
            <div className="tips-box">
              <InfoIcon size={18} className="tips-icon" />
              <div className="tips-content">
                文件路徑依版本自動計算，不同版本可能有不同的路徑
              </div>
            </div>

            {loadingReleases ? (
              <div className="loading" style={{ padding: '2rem' }}>載入 Releases 中...</div>
            ) : releases.length === 0 ? (
              <div className="empty-state" style={{ padding: '2rem' }}>
                <p>找不到任何 Release</p>
              </div>
            ) : (
              <>
                {/* 版本列表 */}
                <div className="version-list">
                  {/* 全選列 */}
                  <div className="version-select-all">
                    <label>
                      <input
                        type="checkbox"
                        checked={allSelected}
                        onChange={toggleSelectAll}
                        disabled={selectableReleases.length === 0}
                      />
                      <span>全選</span>
                    </label>
                    <span className="version-select-count">
                      已選擇 {selectedReleases.size} 個
                    </span>
                  </div>

                  {/* 版本項目 */}
                  {releases.map(release => (
                    <div
                      key={release.tagName}
                      className={`version-item ${release.exists ? 'disabled' : ''}`}
                    >
                      <div className="version-checkbox">
                        <input
                          type="checkbox"
                          checked={selectedReleases.has(release.tagName)}
                          onChange={() => toggleReleaseSelection(release.tagName)}
                          disabled={release.exists}
                        />
                      </div>
                      <div className="version-content">
                        <div className="version-header">
                          <span className="version-name">
                            {release.version || release.tagName}
                          </span>
                          {release.exists && (
                            <span className="badge badge-default">已存在</span>
                          )}
                          <span className="version-date">
                            {formatShortDate(release.publishedAt || release.createdAt)}
                          </span>
                        </div>
                        <div className="version-release">
                          Release: {release.name || release.tagName}
                        </div>
                        <div className="version-path">
                          <FolderIcon size={12} />
                          {release.docsPath || defaultDocsPath}
                        </div>
                      </div>
                    </div>
                  ))}
                </div>

                {/* 操作按鈕 */}
                <div className="form-actions">
                  <button type="button" className="btn" onClick={handleCloseSyncModal}>
                    取消
                  </button>
                  <button
                    type="button"
                    className="btn btn-primary"
                    onClick={handleBatchSync}
                    disabled={selectedReleases.size === 0 || batchSyncing}
                  >
                    {batchSyncing ? '同步中...' : `同步選中版本 (${selectedReleases.size})`}
                  </button>
                </div>
              </>
            )}
          </div>
        </div>
      )}
    </div>
  );
}
