/**
 * 文件庫表單頁面
 * 支援新增和編輯模式
 */
import React, { useState, useEffect } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import api from '../services/api';
import {
  ChevronRightIcon
} from '../components/Icons';

export default function LibraryForm() {
  const navigate = useNavigate();
  const { id } = useParams(); // 如果有 id 表示編輯模式
  const isEditMode = Boolean(id);

  const [loading, setLoading] = useState(false);
  const [initialLoading, setInitialLoading] = useState(isEditMode);

  // 表單資料
  const [formData, setFormData] = useState({
    name: '',           // 識別碼（小寫字母、數字、連字號）
    displayName: '',    // 顯示名稱（必填）
    description: '',    // 描述
    sourceType: 'GITHUB', // 來源類型
    sourceUrl: '',      // 來源 URL
    category: '',       // 分類
    tags: ''            // 標籤（逗號分隔）
  });

  /**
   * 編輯模式時載入現有資料
   */
  useEffect(() => {
    if (isEditMode) {
      loadLibraryData();
    }
  }, [id]);

  /**
   * 載入文件庫資料
   */
  const loadLibraryData = async () => {
    try {
      setInitialLoading(true);
      const library = await api.getLibrary(id);
      setFormData({
        name: library.name || '',
        displayName: library.displayName || '',
        description: library.description || '',
        sourceType: library.sourceType || 'GITHUB',
        sourceUrl: library.sourceUrl || '',
        category: library.category || '',
        tags: library.tags ? library.tags.join(', ') : ''
      });
    } catch (err) {
      console.error('載入文件庫資料失敗:', err);
      alert('載入失敗: ' + err.message);
      navigate('/libraries');
    } finally {
      setInitialLoading(false);
    }
  };

  /**
   * 處理表單提交
   */
  const handleSubmit = async (e) => {
    e.preventDefault();

    try {
      setLoading(true);

      // 準備提交資料
      const payload = {
        ...formData,
        tags: formData.tags
          ? formData.tags.split(',').map(t => t.trim()).filter(Boolean)
          : []
      };

      if (isEditMode) {
        // 編輯模式：更新文件庫
        await api.updateLibrary(id, payload);
        navigate(`/libraries/${id}`);
      } else {
        // 新增模式：建立文件庫
        await api.createLibrary(payload);
        navigate('/libraries');
      }
    } catch (err) {
      alert((isEditMode ? '更新' : '建立') + '失敗: ' + err.message);
    } finally {
      setLoading(false);
    }
  };

  /**
   * 處理表單欄位變更
   */
  const handleChange = (field) => (e) => {
    setFormData(prev => ({
      ...prev,
      [field]: e.target.value
    }));
  };

  // 編輯模式載入中
  if (initialLoading) {
    return <div className="loading">載入中...</div>;
  }

  return (
    <div className="page form-page">
      {/* 麵包屑導航 */}
      <nav className="breadcrumb mb-4">
        <Link to="/libraries" className="breadcrumb-item">Libraries</Link>
        <ChevronRightIcon size={14} className="breadcrumb-separator" />
        {isEditMode && formData.displayName && (
          <>
            <Link to={`/libraries/${id}`} className="breadcrumb-item">
              {formData.displayName}
            </Link>
            <ChevronRightIcon size={14} className="breadcrumb-separator" />
          </>
        )}
        <span className="breadcrumb-item active">{isEditMode ? 'Edit' : 'New'}</span>
      </nav>

      {/* 頁面標題 */}
      <header className="form-page-header">
        <h2>{isEditMode ? 'Edit Library' : 'Add New Library'}</h2>
      </header>

      {/* 表單卡片 */}
      <div className="form-card">
        <form onSubmit={handleSubmit}>
          {/* Name 欄位 - 必填，只能小寫字母、數字、連字號（編輯模式不可修改） */}
          <div className="form-group">
            <label>Name *</label>
            <input
              type="text"
              value={formData.name}
              onChange={handleChange('name')}
              placeholder="e.g., spring-boot"
              pattern="^[a-z0-9-]+$"
              required
              disabled={isEditMode}
              className={isEditMode ? 'input-disabled' : ''}
            />
            <small className="form-hint">
              {isEditMode
                ? 'Name cannot be changed after creation.'
                : 'Only lowercase letters, numbers, and hyphens. This will be used as the unique identifier.'}
            </small>
          </div>

          {/* Display Name 欄位 - 必填 */}
          <div className="form-group">
            <label>Display Name *</label>
            <input
              type="text"
              value={formData.displayName}
              onChange={handleChange('displayName')}
              placeholder="e.g., Spring Boot"
              required
            />
          </div>

          {/* Description 欄位 - 選填 */}
          <div className="form-group">
            <label>Description</label>
            <textarea
              value={formData.description}
              onChange={handleChange('description')}
              placeholder="Brief description of this documentation library..."
              rows="3"
            />
          </div>

          {/* Source Type 欄位 - 必填 */}
          <div className="form-group">
            <label>Source Type *</label>
            <select
              value={formData.sourceType}
              onChange={handleChange('sourceType')}
              required
            >
              <option value="GITHUB">GitHub Repository</option>
              <option value="LOCAL">本地路徑</option>
            </select>
          </div>

          {/* Source URL 欄位 - 選填 */}
          <div className="form-group">
            <label>Source URL</label>
            <input
              type="url"
              value={formData.sourceUrl}
              onChange={handleChange('sourceUrl')}
              placeholder="https://github.com/owner/repo"
            />
            <small className="url-hint">
              GitHub repository URL for synchronization
            </small>
          </div>

          {/* Category 欄位 - 選填 */}
          <div className="form-group">
            <label>Category</label>
            <input
              type="text"
              value={formData.category}
              onChange={handleChange('category')}
              placeholder="e.g., backend, frontend, database"
            />
          </div>

          {/* Tags 欄位 - 選填 */}
          <div className="form-group">
            <label>Tags</label>
            <input
              type="text"
              value={formData.tags}
              onChange={handleChange('tags')}
              placeholder="java, spring, framework (comma-separated)"
            />
          </div>

          {/* 表單操作按鈕 */}
          <div className="form-actions">
            <Link to={isEditMode ? `/libraries/${id}` : '/libraries'} className="btn">
              Cancel
            </Link>
            <button
              type="submit"
              className="btn btn-primary"
              disabled={loading}
            >
              {loading
                ? (isEditMode ? 'Saving...' : 'Creating...')
                : (isEditMode ? 'Save Changes' : 'Create Library')}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
