/**
 * 系統設定頁面
 * 顯示 Feature Flags、同步設定、系統資訊
 */
import React, { useState, useEffect } from 'react';
import api from '../services/api';
import {
  SettingsIcon,
  CheckCircleIcon,
  XCircleIcon,
  SyncIcon,
  ServerIcon,
  RefreshIcon,
  InfoIcon,
  ClockIcon
} from '../components/Icons';

export default function Settings() {
  // 狀態
  const [settings, setSettings] = useState(null);
  const [config, setConfig] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    loadSettings();
  }, []);

  /**
   * 載入設定
   */
  const loadSettings = async () => {
    try {
      setLoading(true);
      setError(null);
      const [settingsData, configData] = await Promise.all([
        api.getSettings().catch(() => null),
        api.getConfig().catch(() => null)
      ]);
      setSettings(settingsData);
      setConfig(configData);
    } catch (err) {
      console.error('載入設定失敗:', err);
      setError(err.message);
    } finally {
      setLoading(false);
    }
  };

  /**
   * 取得功能狀態圖標
   */
  const getFeatureIcon = (enabled) => {
    if (enabled) {
      return <CheckCircleIcon size={18} className="feature-enabled" />;
    }
    return <XCircleIcon size={18} className="feature-disabled" />;
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
          <button className="btn btn-primary mt-4" onClick={loadSettings}>
            重試
          </button>
        </div>
      </div>
    );
  }

  // 預設值
  const featureFlags = settings?.featureFlags || config?.featureFlags || {};
  const syncSettings = settings?.syncSettings || {};
  const systemInfo = settings?.systemInfo || {};

  return (
    <div className="page settings">
      {/* 頁面標題 */}
      <header className="page-header">
        <h2>系統設定</h2>
        <button className="btn" onClick={loadSettings}>
          <RefreshIcon size={18} />
          重新整理
        </button>
      </header>

      {/* Feature Flags */}
      <section className="settings-section mb-6">
        <div className="section-header mb-4">
          <h3 className="section-title">
            <SettingsIcon size={20} className="mr-2" />
            Feature Flags
          </h3>
        </div>
        <div className="glass-card-static">
          <div className="feature-list">
            <div className="feature-item">
              <div className="feature-info">
                <span className="feature-name">OAuth2 認證</span>
                <span className="feature-desc">啟用 OAuth2 登入保護管理功能</span>
              </div>
              <div className="feature-status">
                {getFeatureIcon(featureFlags.oauth2Enabled || config?.oauth2Enabled)}
                <span>{featureFlags.oauth2Enabled || config?.oauth2Enabled ? '已啟用' : '已停用'}</span>
              </div>
            </div>

            <div className="feature-item">
              <div className="feature-info">
                <span className="feature-name">API Key 管理</span>
                <span className="feature-desc">允許建立和管理 API 金鑰</span>
              </div>
              <div className="feature-status">
                {getFeatureIcon(featureFlags.apiKeyEnabled !== false)}
                <span>{featureFlags.apiKeyEnabled !== false ? '已啟用' : '已停用'}</span>
              </div>
            </div>

            <div className="feature-item">
              <div className="feature-info">
                <span className="feature-name">語意搜尋</span>
                <span className="feature-desc">使用向量嵌入進行語意搜尋</span>
              </div>
              <div className="feature-status">
                {getFeatureIcon(featureFlags.semanticSearchEnabled !== false)}
                <span>{featureFlags.semanticSearchEnabled !== false ? '已啟用' : '已停用'}</span>
              </div>
            </div>

            <div className="feature-item">
              <div className="feature-info">
                <span className="feature-name">自動同步</span>
                <span className="feature-desc">定期自動同步文件庫</span>
              </div>
              <div className="feature-status">
                {getFeatureIcon(featureFlags.autoSyncEnabled)}
                <span>{featureFlags.autoSyncEnabled ? '已啟用' : '已停用'}</span>
              </div>
            </div>
          </div>
        </div>
      </section>

      {/* 同步設定 */}
      <section className="settings-section mb-6">
        <div className="section-header mb-4">
          <h3 className="section-title">
            <SyncIcon size={20} className="mr-2" />
            同步設定
          </h3>
        </div>
        <div className="glass-card-static">
          <div className="settings-grid">
            <div className="setting-item">
              <div className="setting-label">
                <ClockIcon size={16} />
                Cron Schedule
              </div>
              <div className="setting-value">
                <code>{syncSettings.cronSchedule || '0 2 * * *'}</code>
                <span className="text-muted ml-2">（預設每天凌晨 2 點）</span>
              </div>
            </div>

            <div className="setting-item">
              <div className="setting-label">
                <InfoIcon size={16} />
                同步超時
              </div>
              <div className="setting-value">
                {syncSettings.timeoutMinutes || 30} 分鐘
              </div>
            </div>

            <div className="setting-item">
              <div className="setting-label">
                <InfoIcon size={16} />
                最大重試次數
              </div>
              <div className="setting-value">
                {syncSettings.maxRetries || 3} 次
              </div>
            </div>
          </div>
        </div>
      </section>

      {/* 系統資訊 */}
      <section className="settings-section">
        <div className="section-header mb-4">
          <h3 className="section-title">
            <ServerIcon size={20} className="mr-2" />
            系統資訊
          </h3>
        </div>
        <div className="glass-card-static">
          <div className="system-info-grid">
            <div className="info-card">
              <div className="info-card-label">應用程式版本</div>
              <div className="info-card-value">{systemInfo.appVersion || '1.0.0'}</div>
            </div>

            <div className="info-card">
              <div className="info-card-label">Java 版本</div>
              <div className="info-card-value">{systemInfo.javaVersion || 'N/A'}</div>
            </div>

            <div className="info-card">
              <div className="info-card-label">作業系統</div>
              <div className="info-card-value">{systemInfo.osName || 'N/A'}</div>
            </div>

            <div className="info-card">
              <div className="info-card-label">Spring Boot</div>
              <div className="info-card-value">{systemInfo.springBootVersion || 'N/A'}</div>
            </div>

            <div className="info-card">
              <div className="info-card-label">資料庫</div>
              <div className="info-card-value">{systemInfo.database || 'PostgreSQL + pgvector'}</div>
            </div>

            <div className="info-card">
              <div className="info-card-label">嵌入模型</div>
              <div className="info-card-value">{systemInfo.embeddingModel || 'gemini-embedding-001'}</div>
            </div>
          </div>
        </div>
      </section>
    </div>
  );
}
