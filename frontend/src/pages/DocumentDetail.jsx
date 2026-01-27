/**
 * 文件詳情頁面
 * 顯示單一文件的完整內容
 */
import React, { useState, useEffect } from 'react';
import { useParams, Link, useNavigate } from 'react-router-dom';
import api from '../services/api';
import {
  FileTextIcon,
  ChevronRightIcon,
  ExternalLinkIcon,
  CopyIcon,
  CheckIcon,
  LayersIcon,
  ClockIcon,
  TagIcon
} from '../components/Icons';

export default function DocumentDetail() {
  const { id } = useParams();
  const navigate = useNavigate();

  // 狀態
  const [document, setDocument] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [copied, setCopied] = useState(false);

  useEffect(() => {
    loadDocument();
  }, [id]);

  /**
   * 載入文件
   */
  const loadDocument = async () => {
    try {
      setLoading(true);
      setError(null);
      const data = await api.getDocument(id);
      setDocument(data);
    } catch (err) {
      console.error('載入文件失敗:', err);
      setError(err.message);
    } finally {
      setLoading(false);
    }
  };

  /**
   * 複製內容到剪貼簿
   */
  const handleCopyContent = async () => {
    try {
      await navigator.clipboard.writeText(document.content || '');
      setCopied(true);
      setTimeout(() => setCopied(false), 2000);
    } catch (err) {
      alert('複製失敗: ' + err.message);
    }
  };

  /**
   * 取得文件類型標籤
   */
  const getTypeLabel = (type) => {
    const types = {
      'MARKDOWN': 'Markdown',
      'HTML': 'HTML',
      'TEXT': '純文字',
      'RST': 'reStructuredText',
      'ASCIIDOC': 'AsciiDoc'
    };
    return types[type] || type;
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

  if (loading) {
    return <div className="loading">載入中...</div>;
  }

  if (error) {
    return (
      <div className="page">
        <div className="error glass-card-static" style={{ textAlign: 'center', padding: '2rem' }}>
          <h3>載入失敗</h3>
          <p>{error}</p>
          <button className="btn btn-primary mt-4" onClick={() => navigate(-1)}>
            返回
          </button>
        </div>
      </div>
    );
  }

  if (!document) {
    return (
      <div className="page">
        <div className="error glass-card-static" style={{ textAlign: 'center', padding: '2rem' }}>
          <h3>找不到文件</h3>
          <button className="btn btn-primary mt-4" onClick={() => navigate(-1)}>
            返回
          </button>
        </div>
      </div>
    );
  }

  return (
    <div className="page document-detail">
      {/* 麵包屑導航 */}
      <nav className="breadcrumb mb-4">
        <Link to="/libraries" className="breadcrumb-item">文件庫</Link>
        {document.libraryId && (
          <>
            <ChevronRightIcon size={14} className="breadcrumb-separator" />
            <Link to={`/libraries/${document.libraryId}`} className="breadcrumb-item">
              {document.libraryName || '文件庫'}
            </Link>
          </>
        )}
        {document.versionId && (
          <>
            <ChevronRightIcon size={14} className="breadcrumb-separator" />
            <Link
              to={`/documents?libraryId=${document.libraryId}&versionId=${document.versionId}`}
              className="breadcrumb-item"
            >
              {document.version || '版本'}
            </Link>
          </>
        )}
        <ChevronRightIcon size={14} className="breadcrumb-separator" />
        <span className="breadcrumb-item active">{document.title}</span>
      </nav>

      {/* 頁面標題 */}
      <header className="page-header">
        <div className="flex items-center gap-4">
          <div className="document-icon">
            <FileTextIcon size={32} />
          </div>
          <div>
            <h2>{document.title || '未命名文件'}</h2>
            <p className="text-muted mt-2">{document.path}</p>
          </div>
        </div>
        <button className="btn" onClick={handleCopyContent}>
          {copied ? (
            <>
              <CheckIcon size={18} />
              已複製
            </>
          ) : (
            <>
              <CopyIcon size={18} />
              複製內容
            </>
          )}
        </button>
      </header>

      {/* 文件資訊卡片 */}
      <div className="glass-card-static mb-6">
        <div className="document-meta-grid">
          <div className="meta-item">
            <span className="meta-label">
              <TagIcon size={14} />
              類型
            </span>
            <span className="meta-value">
              <span className="badge badge-default">{getTypeLabel(document.type)}</span>
            </span>
          </div>
          <div className="meta-item">
            <span className="meta-label">
              <LayersIcon size={14} />
              片段數
            </span>
            <span className="meta-value">{document.chunkCount || 0}</span>
          </div>
          <div className="meta-item">
            <span className="meta-label">
              <ClockIcon size={14} />
              更新時間
            </span>
            <span className="meta-value">{formatDate(document.updatedAt)}</span>
          </div>
        </div>

        {/* 相關連結 */}
        <div className="document-links mt-4">
          {document.sourceUrl && (
            <a
              href={document.sourceUrl}
              target="_blank"
              rel="noopener noreferrer"
              className="btn btn-sm"
            >
              <ExternalLinkIcon size={14} />
              查看原始文件
            </a>
          )}
          {document.libraryId && (
            <Link to={`/libraries/${document.libraryId}`} className="btn btn-sm">
              查看文件庫
            </Link>
          )}
          {document.versionId && (
            <Link
              to={`/documents?libraryId=${document.libraryId}&versionId=${document.versionId}`}
              className="btn btn-sm"
            >
              查看所有文件
            </Link>
          )}
        </div>
      </div>

      {/* 文件內容 */}
      <div className="section-header mb-4">
        <h3 className="section-title">文件內容</h3>
      </div>

      <div className="document-content-container glass-card-static">
        <pre className="document-content">
          <code>{document.content || '（無內容）'}</code>
        </pre>
      </div>
    </div>
  );
}
