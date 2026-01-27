/**
 * 文件列表頁面
 * 顯示特定版本的所有文件
 */
import React, { useState, useEffect } from 'react';
import { useSearchParams, Link } from 'react-router-dom';
import api from '../services/api';
import {
  FileTextIcon,
  ChevronRightIcon,
  SearchIcon,
  FolderIcon,
  EyeIcon,
  RefreshIcon
} from '../components/Icons';

export default function DocumentList() {
  const [searchParams] = useSearchParams();
  const libraryId = searchParams.get('libraryId');
  const versionId = searchParams.get('versionId');

  // 狀態
  const [documents, setDocuments] = useState([]);
  const [library, setLibrary] = useState(null);
  const [version, setVersion] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  // 搜尋篩選
  const [searchTerm, setSearchTerm] = useState('');

  useEffect(() => {
    loadData();
  }, [libraryId, versionId]);

  /**
   * 載入資料
   */
  const loadData = async () => {
    try {
      setLoading(true);
      setError(null);

      // 載入文件庫資訊
      if (libraryId) {
        const libData = await api.getLibrary(libraryId);
        setLibrary(libData);

        // 載入版本列表找出當前版本
        if (versionId) {
          const versionsData = await api.getVersions(libraryId);
          const currentVersion = versionsData?.find(v => v.id === versionId);
          setVersion(currentVersion);
        }
      }

      // 載入文件列表
      const params = {};
      if (versionId) params.versionId = versionId;
      if (libraryId && !versionId) params.libraryId = libraryId;

      const docsData = await api.getDocuments(params);
      setDocuments(docsData || []);
    } catch (err) {
      console.error('載入文件失敗:', err);
      setError(err.message);
    } finally {
      setLoading(false);
    }
  };

  /**
   * 篩選文件
   */
  const filteredDocuments = documents.filter(doc => {
    if (!searchTerm) return true;
    const term = searchTerm.toLowerCase();
    return (
      doc.title?.toLowerCase().includes(term) ||
      doc.path?.toLowerCase().includes(term) ||
      doc.content?.toLowerCase().includes(term)
    );
  });

  /**
   * 依路徑分組文件
   */
  const groupedDocuments = filteredDocuments.reduce((acc, doc) => {
    const pathParts = doc.path?.split('/') || [''];
    const folder = pathParts.length > 1 ? pathParts.slice(0, -1).join('/') : '根目錄';
    if (!acc[folder]) acc[folder] = [];
    acc[folder].push(doc);
    return acc;
  }, {});

  /**
   * 取得文件類型標籤
   */
  const getTypeLabel = (type) => {
    const types = {
      'MARKDOWN': 'MD',
      'HTML': 'HTML',
      'TEXT': 'TXT',
      'RST': 'RST',
      'ASCIIDOC': 'ADOC'
    };
    return types[type] || type;
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
          <Link to="/libraries" className="btn btn-primary mt-4">
            返回文件庫列表
          </Link>
        </div>
      </div>
    );
  }

  return (
    <div className="page document-list">
      {/* 麵包屑導航 */}
      <nav className="breadcrumb mb-4">
        <Link to="/libraries" className="breadcrumb-item">文件庫</Link>
        {library && (
          <>
            <ChevronRightIcon size={14} className="breadcrumb-separator" />
            <Link to={`/libraries/${library.id}`} className="breadcrumb-item">
              {library.name}
            </Link>
          </>
        )}
        {version && (
          <>
            <ChevronRightIcon size={14} className="breadcrumb-separator" />
            <span className="breadcrumb-item active">{version.version}</span>
          </>
        )}
      </nav>

      {/* 頁面標題 */}
      <header className="page-header">
        <div>
          <h2>文件列表</h2>
          <p className="text-muted mt-2">
            {library?.name}
            {version && ` - ${version.version}`}
            {` (共 ${documents.length} 份文件)`}
          </p>
        </div>
        <button className="btn" onClick={loadData}>
          <RefreshIcon size={18} />
          重新整理
        </button>
      </header>

      {/* 搜尋框 */}
      <div className="search-box mb-6">
        <span className="search-icon">
          <SearchIcon size={20} />
        </span>
        <input
          type="text"
          placeholder="搜尋文件標題或路徑..."
          value={searchTerm}
          onChange={e => setSearchTerm(e.target.value)}
        />
      </div>

      {/* 文件列表 */}
      {filteredDocuments.length === 0 ? (
        <div className="empty-state glass-card-static" style={{ textAlign: 'center', padding: '3rem' }}>
          <div className="empty-state-icon">
            <FileTextIcon size={64} />
          </div>
          <h3>尚無文件</h3>
          <p>
            {searchTerm
              ? '找不到符合條件的文件'
              : '此版本尚未同步任何文件，請先執行同步操作'
            }
          </p>
        </div>
      ) : (
        <div className="document-groups space-y-6">
          {Object.entries(groupedDocuments).map(([folder, docs]) => (
            <div key={folder} className="document-group">
              {/* 資料夾標題 */}
              <div className="folder-header flex items-center gap-2 mb-4">
                <FolderIcon size={18} className="text-muted" />
                <span className="folder-name">{folder}</span>
                <span className="badge badge-default">{docs.length}</span>
              </div>

              {/* 文件表格 */}
              <div className="table-container glass-card-static">
                <table className="data-table">
                  <thead>
                    <tr>
                      <th>標題</th>
                      <th>路徑</th>
                      <th>類型</th>
                      <th>操作</th>
                    </tr>
                  </thead>
                  <tbody>
                    {docs.map(doc => (
                      <tr key={doc.id}>
                        <td>
                          <div className="flex items-center gap-2">
                            <FileTextIcon size={16} className="text-muted" />
                            <strong>{doc.title || '未命名文件'}</strong>
                          </div>
                        </td>
                        <td className="text-sm text-muted">{doc.path}</td>
                        <td>
                          <span className="badge badge-default">
                            {getTypeLabel(doc.type)}
                          </span>
                        </td>
                        <td>
                          <Link
                            to={`/documents/${doc.id}`}
                            className="btn btn-sm"
                          >
                            <EyeIcon size={14} />
                            查看
                          </Link>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </div>
          ))}
        </div>
      )}

      {/* 顯示篩選結果 */}
      {searchTerm && filteredDocuments.length > 0 && (
        <div className="text-sm text-muted mt-4">
          顯示 {filteredDocuments.length} / {documents.length} 份文件
        </div>
      )}
    </div>
  );
}
