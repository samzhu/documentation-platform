/**
 * API 金鑰頁面
 * 管理 API 金鑰的建立和撤銷
 */
import React, { useState, useEffect } from 'react';
import api from '../services/api';
import {
  KeyIcon,
  PlusIcon,
  CheckCircleIcon,
  XCircleIcon,
  CopyIcon,
  TrashIcon,
  AlertTriangleIcon,
  ClockIcon,
  ChevronDownIcon,
  ChevronUpIcon
} from '../components/Icons';

export default function ApiKeys() {
  const [apiKeys, setApiKeys] = useState([]);
  const [loading, setLoading] = useState(true);
  const [showForm, setShowForm] = useState(false);
  const [formData, setFormData] = useState({ name: '', expiresIn: 30 });
  const [newKeyResult, setNewKeyResult] = useState(null);
  const [copiedField, setCopiedField] = useState(null);
  const [showExample, setShowExample] = useState(false);
  const [stats, setStats] = useState({ total: 0, active: 0, revoked: 0, expired: 0 });

  useEffect(() => {
    loadApiKeys();
  }, []);

  /**
   * 載入 API 金鑰列表
   */
  const loadApiKeys = async () => {
    try {
      setLoading(true);
      const data = await api.getApiKeys();
      const keys = data || [];
      setApiKeys(keys);

      // 計算統計
      const now = new Date();
      const newStats = keys.reduce((acc, key) => {
        acc.total++;
        if (key.status === 'ACTIVE') {
          if (key.expiresAt && new Date(key.expiresAt) < now) {
            acc.expired++;
          } else {
            acc.active++;
          }
        } else if (key.status === 'REVOKED') {
          acc.revoked++;
        }
        return acc;
      }, { total: 0, active: 0, revoked: 0, expired: 0 });

      setStats(newStats);
    } catch (err) {
      console.error('載入 API 金鑰失敗:', err);
    } finally {
      setLoading(false);
    }
  };

  /**
   * 建立 API 金鑰
   */
  const handleCreate = async (e) => {
    e.preventDefault();
    try {
      const result = await api.createApiKey(formData);
      // 保存完整結果，包含 mcpKey 和 rawKey
      setNewKeyResult(result);
      setShowForm(false);
      setFormData({ name: '', expiresIn: 30 });
      loadApiKeys();
    } catch (err) {
      alert('建立失敗: ' + err.message);
    }
  };

  /**
   * 撤銷 API 金鑰
   */
  const handleRevoke = async (id) => {
    if (!confirm('確定要撤銷此 API 金鑰嗎？此操作無法復原，使用此金鑰的服務將無法繼續存取。')) return;
    try {
      await api.revokeApiKey(id);
      loadApiKeys();
    } catch (err) {
      alert('撤銷失敗: ' + err.message);
    }
  };

  /**
   * 複製指定欄位的值
   * @param {string} text 要複製的文字
   * @param {string} field 欄位名稱（用於區分複製狀態）
   */
  const copyToClipboard = async (text, field) => {
    try {
      await navigator.clipboard.writeText(text);
      setCopiedField(field);
      setTimeout(() => setCopiedField(null), 2000);
    } catch (err) {
      alert('複製失敗，請手動複製');
    }
  };

  /**
   * 格式化日期
   */
  const formatDate = (dateStr) => {
    if (!dateStr) return '永不過期';
    return new Date(dateStr).toLocaleDateString('zh-TW', {
      year: 'numeric',
      month: '2-digit',
      day: '2-digit'
    });
  };

  /**
   * 檢查是否過期
   */
  const isExpired = (expiresAt) => {
    if (!expiresAt) return false;
    return new Date(expiresAt) < new Date();
  };

  /**
   * 取得狀態標籤
   */
  const getStatusBadge = (key) => {
    if (key.status === 'REVOKED') {
      return (
        <span className="status-badge status-failed">
          <XCircleIcon size={14} />
          <span style={{ marginLeft: '4px' }}>已撤銷</span>
        </span>
      );
    }

    if (isExpired(key.expiresAt)) {
      return (
        <span className="status-badge" style={{ background: 'var(--color-warning-bg)', color: '#d97706' }}>
          <ClockIcon size={14} />
          <span style={{ marginLeft: '4px' }}>已過期</span>
        </span>
      );
    }

    return (
      <span className="status-badge status-success">
        <CheckCircleIcon size={14} />
        <span style={{ marginLeft: '4px' }}>啟用中</span>
      </span>
    );
  };

  if (loading) return <div className="loading">載入中...</div>;

  return (
    <div className="page api-keys">
      <header className="page-header">
        <h2>API 金鑰</h2>
        <button className="btn btn-primary" onClick={() => setShowForm(true)}>
          <PlusIcon size={18} />
          建立金鑰
        </button>
      </header>

      {/* 統計卡片 */}
      <div className="stats-grid mb-6">
        <div className="stat-card">
          <div className="stat-icon blue">
            <KeyIcon size={24} />
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
            <div className="stat-value">{stats.active}</div>
            <div className="stat-label">啟用中</div>
          </div>
        </div>

        <div className="stat-card">
          <div className="stat-icon red">
            <XCircleIcon size={24} />
          </div>
          <div className="stat-content">
            <div className="stat-value">{stats.revoked}</div>
            <div className="stat-label">已撤銷</div>
          </div>
        </div>

        <div className="stat-card">
          <div className="stat-icon yellow">
            <ClockIcon size={24} />
          </div>
          <div className="stat-content">
            <div className="stat-value">{stats.expired}</div>
            <div className="stat-label">已過期</div>
          </div>
        </div>
      </div>

      {/* 新金鑰顯示（只顯示一次） */}
      {newKeyResult && (
        <div className="warning-box mb-6" style={{ background: 'var(--color-warning-bg)', border: '1px solid rgba(255, 159, 10, 0.3)' }}>
          <AlertTriangleIcon className="warning-icon" size={24} />
          <div className="warning-content" style={{ flex: 1 }}>
            <h4 style={{ marginBottom: '8px', color: 'var(--color-text-primary)' }}>新金鑰已建立</h4>
            <p style={{ marginBottom: '12px' }}>請立即複製金鑰，關閉後將無法再次查看！</p>

            {/* MCP 客戶端用金鑰（主要） */}
            <div style={{ marginBottom: '12px' }}>
              <label className="text-xs text-muted" style={{ display: 'block', marginBottom: '4px' }}>
                MCP 客戶端用金鑰（設定於 X-API-Key Header）
              </label>
              <div className="key-display">
                <code style={{ flex: 1, wordBreak: 'break-all' }}>{newKeyResult.mcpKey}</code>
                <button className="btn btn-sm" onClick={() => copyToClipboard(newKeyResult.mcpKey, 'mcpKey')}>
                  <CopyIcon size={16} />
                  {copiedField === 'mcpKey' ? '已複製' : '複製'}
                </button>
              </div>
            </div>

            {/* 原始金鑰（次要） */}
            <div style={{ marginBottom: '12px' }}>
              <label className="text-xs text-muted" style={{ display: 'block', marginBottom: '4px' }}>
                原始金鑰
              </label>
              <div className="key-display">
                <code style={{ flex: 1, wordBreak: 'break-all' }}>{newKeyResult.rawKey}</code>
                <button className="btn btn-sm" onClick={() => copyToClipboard(newKeyResult.rawKey, 'rawKey')}>
                  <CopyIcon size={16} />
                  {copiedField === 'rawKey' ? '已複製' : '複製'}
                </button>
              </div>
            </div>

            {/* 可展開的 MCP 客戶端設定範例 */}
            <div style={{ marginBottom: '12px' }}>
              <button
                className="btn btn-sm"
                onClick={() => setShowExample(!showExample)}
                style={{ display: 'flex', alignItems: 'center', gap: '4px' }}
              >
                {showExample ? <ChevronUpIcon size={14} /> : <ChevronDownIcon size={14} />}
                MCP 客戶端設定範例
              </button>
              {showExample && (
                <pre style={{
                  marginTop: '8px',
                  padding: '12px',
                  borderRadius: '8px',
                  background: 'var(--color-bg-secondary)',
                  fontSize: '13px',
                  lineHeight: '1.5',
                  overflow: 'auto',
                  whiteSpace: 'pre'
                }}>{`{
  "mcpServers": {
    "documentation": {
      "url": "https://your-mcp-server.example.com/mcp",
      "headers": {
        "X-API-Key": "${newKeyResult.mcpKey}"
      }
    }
  }
}`}</pre>
              )}
            </div>

            <button
              className="btn mt-4"
              onClick={() => { setNewKeyResult(null); setShowExample(false); }}
            >
              我已安全保存
            </button>
          </div>
        </div>
      )}

      {/* 建立表單 Modal */}
      {showForm && (
        <div className="modal-overlay" onClick={() => setShowForm(false)}>
          <div className="modal" onClick={e => e.stopPropagation()}>
            <h3>建立 API 金鑰</h3>
            <form onSubmit={handleCreate}>
              <div className="form-group">
                <label>名稱 *</label>
                <input
                  type="text"
                  value={formData.name}
                  onChange={e => setFormData({ ...formData, name: e.target.value })}
                  placeholder="例如: Production API"
                  required
                />
                <p className="text-xs text-muted mt-2">
                  為此金鑰取一個易於辨識的名稱
                </p>
              </div>

              <div className="form-group">
                <label>有效期限</label>
                <select
                  value={formData.expiresIn}
                  onChange={e => setFormData({ ...formData, expiresIn: parseInt(e.target.value) })}
                >
                  <option value="7">7 天</option>
                  <option value="30">30 天</option>
                  <option value="90">90 天</option>
                  <option value="180">180 天</option>
                  <option value="365">1 年</option>
                  <option value="0">永不過期</option>
                </select>
                <p className="text-xs text-muted mt-2">
                  建議設定適當的過期時間以提高安全性
                </p>
              </div>

              <div className="form-actions">
                <button type="button" className="btn" onClick={() => setShowForm(false)}>
                  取消
                </button>
                <button type="submit" className="btn btn-primary">
                  建立
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* API 金鑰列表 */}
      <div className="table-container glass-card-static">
        <table className="data-table">
          <thead>
            <tr>
              <th>名稱</th>
              <th>金鑰前綴</th>
              <th>狀態</th>
              <th>建立日期</th>
              <th>到期日</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            {apiKeys.length === 0 ? (
              <tr>
                <td colSpan="6" className="empty-state">
                  <KeyIcon size={24} style={{ display: 'inline', marginRight: '8px', verticalAlign: 'middle' }} />
                  尚無 API 金鑰，點擊上方按鈕建立
                </td>
              </tr>
            ) : (
              apiKeys.map(key => (
                <tr key={key.id}>
                  <td>
                    <strong>{key.name}</strong>
                  </td>
                  <td>
                    <code className="text-sm">{key.keyPrefix}...</code>
                  </td>
                  <td>{getStatusBadge(key)}</td>
                  <td className="text-sm">{formatDate(key.createdAt)}</td>
                  <td className="text-sm">
                    {key.expiresAt ? (
                      <span style={{ color: isExpired(key.expiresAt) ? 'var(--color-error)' : 'inherit' }}>
                        {formatDate(key.expiresAt)}
                      </span>
                    ) : (
                      <span className="text-muted">永不過期</span>
                    )}
                  </td>
                  <td>
                    {key.status === 'ACTIVE' && !isExpired(key.expiresAt) && (
                      <button
                        className="btn btn-sm btn-danger"
                        onClick={() => handleRevoke(key.id)}
                      >
                        <TrashIcon size={14} />
                        撤銷
                      </button>
                    )}
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>
    </div>
  );
}
