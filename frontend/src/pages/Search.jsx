/**
 * 搜尋頁面
 * 語意搜尋介面，支援多種搜尋模式
 */
import React, { useState, useEffect } from 'react';
import api from '../services/api';
import {
  SearchIcon,
  InfoIcon,
  FileTextIcon,
  ExternalLinkIcon
} from '../components/Icons';

// 搜尋模式配置
const searchModes = [
  { value: 'hybrid', label: 'Hybrid', desc: 'Combines semantic and full-text search (Recommended)' },
  { value: 'semantic', label: 'Semantic', desc: 'Vector similarity based search' },
  { value: 'fulltext', label: 'Full-text', desc: 'Traditional keyword matching' }
];

export default function Search() {
  const [query, setQuery] = useState('');
  const [mode, setMode] = useState('hybrid');
  const [libraryId, setLibraryId] = useState('');
  const [versionId, setVersionId] = useState('');
  const [libraries, setLibraries] = useState([]);
  const [versions, setVersions] = useState([]);
  const [loadingVersions, setLoadingVersions] = useState(false);
  const [results, setResults] = useState([]);
  const [searching, setSearching] = useState(false);
  const [hasSearched, setHasSearched] = useState(false);

  useEffect(() => {
    loadLibraries();
  }, []);

  /**
   * 當選擇文件庫時載入版本列表
   */
  useEffect(() => {
    if (libraryId) {
      loadVersions(libraryId);
    } else {
      setVersions([]);
      setVersionId('');
    }
  }, [libraryId]);

  /**
   * 載入文件庫列表（用於篩選）
   */
  const loadLibraries = async () => {
    try {
      const data = await api.getLibraries();
      setLibraries(data || []);
    } catch (err) {
      console.error('載入文件庫失敗:', err);
    }
  };

  /**
   * 載入指定文件庫的版本列表
   */
  const loadVersions = async (libId) => {
    try {
      setLoadingVersions(true);
      setVersionId(''); // 重置版本選擇
      const data = await api.getVersions(libId);
      setVersions(data || []);
    } catch (err) {
      console.error('載入版本失敗:', err);
      setVersions([]);
    } finally {
      setLoadingVersions(false);
    }
  };

  /**
   * 執行搜尋
   */
  const handleSearch = async (e) => {
    e.preventDefault();
    if (!query.trim()) return;

    try {
      setSearching(true);
      setHasSearched(true);

      // 取得選中版本的 version 字串（API 使用 version 而非 versionId）
      const selectedVersion = versionId
        ? versions.find(v => v.id === versionId)?.version
        : undefined;

      const data = await api.search({
        query: query.trim(),
        mode,
        libraryId: libraryId || undefined,
        version: selectedVersion,
        limit: 20
      });
      setResults(data.results || []);
    } catch (err) {
      alert('搜尋失敗: ' + err.message);
    } finally {
      setSearching(false);
    }
  };

  /**
   * 格式化分數顯示
   */
  const formatScore = (score) => {
    if (score === undefined || score === null) return '-';
    return `${(score * 100).toFixed(1)}%`;
  };

  /**
   * 高亮顯示匹配文字
   */
  const highlightContent = (content, maxLength = 300) => {
    if (!content) return '';
    if (content.length > maxLength) {
      return content.substring(0, maxLength) + '...';
    }
    return content;
  };

  return (
    <div className="page search">
      <header className="page-header">
        <h2>Search</h2>
      </header>

      {/* 提示框 */}
      <div className="tips-box">
        <InfoIcon className="tips-icon" size={20} />
        <div className="tips-content">
          Enter natural language queries to search indexed documents. Supports Hybrid, Semantic, and Full-text search modes.
        </div>
      </div>

      {/* 搜尋模式選擇 */}
      <div className="search-modes mb-4">
        {searchModes.map(m => (
          <button
            key={m.value}
            className={`search-mode-btn ${mode === m.value ? 'active' : ''}`}
            onClick={() => setMode(m.value)}
            title={m.desc}
          >
            {m.label}
          </button>
        ))}
      </div>

      {/* 篩選列 */}
      <div className="filter-row">
        <div className="filter-group">
          <label>Library</label>
          <select
            value={libraryId}
            onChange={e => setLibraryId(e.target.value)}
          >
            <option value="">All Libraries</option>
            {libraries.map(lib => (
              <option key={lib.id} value={lib.id}>
                {lib.displayName || lib.name}
              </option>
            ))}
          </select>
        </div>

        {/* 版本選擇 - 只有選擇文件庫後才顯示 */}
        {libraryId && (
          <div className="filter-group">
            <label>Version</label>
            <select
              value={versionId}
              onChange={e => setVersionId(e.target.value)}
              disabled={loadingVersions}
            >
              <option value="">
                {loadingVersions ? 'Loading...' : 'All Versions'}
              </option>
              {versions.map(ver => (
                <option key={ver.id} value={ver.id}>
                  {ver.version}
                  {ver.isLatest ? ' (Latest)' : ''}
                </option>
              ))}
            </select>
          </div>
        )}
      </div>

      {/* 搜尋表單 */}
      <form className="search-form mb-6" onSubmit={handleSearch}>
        <div className="search-box">
          <SearchIcon className="search-icon" size={20} />
          <input
            type="text"
            placeholder="Enter search query..."
            value={query}
            onChange={e => setQuery(e.target.value)}
          />
        </div>
        <button type="submit" className="btn btn-primary" disabled={searching || !query.trim()}>
          {searching ? 'Searching...' : 'Search'}
        </button>
      </form>

      {/* 搜尋結果 */}
      {hasSearched && (
        <>
          <div className="results-count">
            {results.length} result{results.length !== 1 ? 's' : ''} found
          </div>

          <div className="search-results space-y-4">
            {results.length === 0 ? (
              <div className="empty-state glass-card-static" style={{ textAlign: 'center', padding: '2rem' }}>
                <div className="empty-state-icon">
                  <SearchIcon size={48} />
                </div>
                <h3>No results found</h3>
                <p>Try adjusting your search terms or selecting a different search mode</p>
              </div>
            ) : (
              results.map((result, index) => (
                <div key={index} className="result-card glass-card">
                  <div className="result-header">
                    <div className="flex items-center gap-2 flex-wrap">
                      <span className="badge badge-primary">{result.libraryName}</span>
                      {result.version && (
                        <span className="badge badge-default">{result.version}</span>
                      )}
                      {result.documentPath && (
                        <span className="text-xs text-muted">
                          <FileTextIcon size={12} style={{ display: 'inline', marginRight: '4px' }} />
                          {result.documentPath}
                        </span>
                      )}
                    </div>
                    <span className="result-score">
                      Relevance: {formatScore(result.score)}
                    </span>
                  </div>

                  <p className="result-content">
                    {highlightContent(result.content)}
                  </p>

                  {result.metadata && (
                    <div className="result-meta text-xs text-muted mt-2">
                      {result.metadata.section && (
                        <span>Section: {result.metadata.section}</span>
                      )}
                    </div>
                  )}

                  {result.sourceUrl && (
                    <a
                      href={result.sourceUrl}
                      target="_blank"
                      rel="noopener noreferrer"
                      className="text-sm text-muted flex items-center gap-2 mt-2"
                    >
                      View source <ExternalLinkIcon size={14} />
                    </a>
                  )}
                </div>
              ))
            )}
          </div>
        </>
      )}

      {/* 初始狀態 */}
      {!hasSearched && (
        <div className="empty-state glass-card-static" style={{ textAlign: 'center', padding: '3rem' }}>
          <div className="empty-state-icon">
            <SearchIcon size={64} />
          </div>
          <h3>Start Searching</h3>
          <p>Enter keywords to search indexed documentation content</p>
        </div>
      )}
    </div>
  );
}
