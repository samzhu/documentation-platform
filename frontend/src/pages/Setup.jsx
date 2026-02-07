/**
 * 整合指南頁面
 * 提供 MCP 端點資訊和各種 IDE 設定範例
 */
import React, { useState, useEffect } from 'react';
import api from '../services/api';
import {
  TerminalIcon,
  CodeIcon,
  CopyIcon,
  CheckIcon,
  LinkIcon,
  ServerIcon,
  ToolIcon,
  CheckCircleIcon,
  XCircleIcon,
  InfoIcon
} from '../components/Icons';

export default function Setup() {
  // 狀態
  const [config, setConfig] = useState(null);
  const [loading, setLoading] = useState(true);
  const [activeTab, setActiveTab] = useState('claude-code');
  const [copied, setCopied] = useState({});

  useEffect(() => {
    loadConfig();
  }, []);

  /**
   * 載入配置
   */
  const loadConfig = async () => {
    try {
      setLoading(true);
      const data = await api.getConfig();
      setConfig(data);
    } catch (err) {
      console.error('載入配置失敗:', err);
    } finally {
      setLoading(false);
    }
  };

  /**
   * 複製到剪貼簿
   */
  const handleCopy = async (text, key) => {
    try {
      await navigator.clipboard.writeText(text);
      setCopied(prev => ({ ...prev, [key]: true }));
      setTimeout(() => setCopied(prev => ({ ...prev, [key]: false })), 2000);
    } catch (err) {
      alert('複製失敗: ' + err.message);
    }
  };

  // MCP 端點 URL（STATELESS Streamable HTTP）
  const baseUrl = window.location.origin;
  const mcpEndpoint = `${baseUrl}/mcp`;

  // 各種設定範例（Streamable HTTP，需搭配 X-API-Key 認證）
  const claudeCodeCli = `claude mcp add docmcp --url "${mcpEndpoint}" --header "X-API-Key: <your-api-key>"`;

  const claudeCodeJson = `{
  "mcpServers": {
    "docmcp": {
      "url": "${mcpEndpoint}",
      "headers": {
        "X-API-Key": "<your-api-key>"
      }
    }
  }
}`;

  const vscodeSettings = `{
  "mcpServers": {
    "docmcp": {
      "url": "${mcpEndpoint}",
      "headers": {
        "X-API-Key": "<your-api-key>"
      }
    }
  }
}`;

  const cursorConfig = `{
  "mcpServers": {
    "docmcp": {
      "url": "${mcpEndpoint}",
      "headers": {
        "X-API-Key": "<your-api-key>"
      }
    }
  }
}`;

  // 可用工具列表
  const availableTools = [
    {
      name: 'search_documents',
      description: '搜尋技術文件（支援全文、語意、混合三種模式）'
    },
    {
      name: 'list_libraries',
      description: '列出所有可用的文件庫'
    },
    {
      name: 'list_library_versions',
      description: '列出指定文件庫的所有版本'
    },
    {
      name: 'get_document',
      description: '取得指定文件的完整內容（含程式碼範例）'
    },
    {
      name: 'list_documents',
      description: '列出指定版本下的所有文件路徑'
    }
  ];

  if (loading) {
    return <div className="loading">載入中...</div>;
  }

  return (
    <div className="page setup">
      {/* 頁面標題 */}
      <header className="page-header">
        <h2>整合指南</h2>
      </header>

      {/* MCP 端點資訊 */}
      <section className="setup-section mb-6">
        <div className="section-header mb-4">
          <h3 className="section-title">
            <ServerIcon size={20} className="mr-2" />
            MCP 端點
          </h3>
        </div>

        <div className="glass-card-static">
          <div className="endpoint-info">
            <div className="endpoint-row">
              <span className="endpoint-label">MCP Endpoint</span>
              <div className="endpoint-value-wrapper">
                <code className="endpoint-value">{mcpEndpoint}</code>
                <button
                  className="btn btn-sm copy-btn"
                  onClick={() => handleCopy(mcpEndpoint, 'mcp')}
                >
                  {copied.mcp ? <CheckIcon size={14} /> : <CopyIcon size={14} />}
                </button>
              </div>
            </div>

            <div className="endpoint-row mt-4">
              <span className="endpoint-label">認證狀態</span>
              <div className="endpoint-value-wrapper">
                {config?.oauth2Enabled ? (
                  <span className="auth-status enabled">
                    <CheckCircleIcon size={16} />
                    OAuth2 已啟用 - 需要 Bearer Token
                  </span>
                ) : (
                  <span className="auth-status disabled">
                    <XCircleIcon size={16} />
                    無需認證
                  </span>
                )}
              </div>
            </div>
          </div>
        </div>
      </section>

      {/* 快速設定 */}
      <section className="setup-section mb-6">
        <div className="section-header mb-4">
          <h3 className="section-title">
            <CodeIcon size={20} className="mr-2" />
            快速設定
          </h3>
        </div>

        {/* 標籤切換 */}
        <div className="setup-tabs mb-4">
          <button
            className={`setup-tab ${activeTab === 'claude-code' ? 'active' : ''}`}
            onClick={() => setActiveTab('claude-code')}
          >
            <TerminalIcon size={16} />
            Claude Code
          </button>
          <button
            className={`setup-tab ${activeTab === 'vscode' ? 'active' : ''}`}
            onClick={() => setActiveTab('vscode')}
          >
            <CodeIcon size={16} />
            VS Code
          </button>
          <button
            className={`setup-tab ${activeTab === 'cursor' ? 'active' : ''}`}
            onClick={() => setActiveTab('cursor')}
          >
            <CodeIcon size={16} />
            Cursor
          </button>
        </div>

        {/* Claude Code 設定 */}
        {activeTab === 'claude-code' && (
          <div className="glass-card-static">
            <h4 className="config-title mb-4">方法 1: CLI 指令</h4>
            <div className="code-block-wrapper">
              <pre className="code-block">
                <code>{claudeCodeCli}</code>
              </pre>
              <button
                className="btn btn-sm copy-btn"
                onClick={() => handleCopy(claudeCodeCli, 'cli')}
              >
                {copied.cli ? <CheckIcon size={14} /> : <CopyIcon size={14} />}
              </button>
            </div>

            <h4 className="config-title mb-4 mt-6">方法 2: .mcp.json 設定檔</h4>
            <p className="text-muted mb-4">
              在專案根目錄建立 <code>.mcp.json</code> 檔案：
            </p>
            <div className="code-block-wrapper">
              <pre className="code-block">
                <code>{claudeCodeJson}</code>
              </pre>
              <button
                className="btn btn-sm copy-btn"
                onClick={() => handleCopy(claudeCodeJson, 'json')}
              >
                {copied.json ? <CheckIcon size={14} /> : <CopyIcon size={14} />}
              </button>
            </div>
          </div>
        )}

        {/* VS Code 設定 */}
        {activeTab === 'vscode' && (
          <div className="glass-card-static">
            <h4 className="config-title mb-4">settings.json</h4>
            <p className="text-muted mb-4">
              在 VS Code 設定中加入以下配置：
            </p>
            <div className="code-block-wrapper">
              <pre className="code-block">
                <code>{vscodeSettings}</code>
              </pre>
              <button
                className="btn btn-sm copy-btn"
                onClick={() => handleCopy(vscodeSettings, 'vscode')}
              >
                {copied.vscode ? <CheckIcon size={14} /> : <CopyIcon size={14} />}
              </button>
            </div>
          </div>
        )}

        {/* Cursor 設定 */}
        {activeTab === 'cursor' && (
          <div className="glass-card-static">
            <h4 className="config-title mb-4">~/.cursor/mcp.json</h4>
            <p className="text-muted mb-4">
              在家目錄下建立或編輯 <code>~/.cursor/mcp.json</code> 檔案：
            </p>
            <div className="code-block-wrapper">
              <pre className="code-block">
                <code>{cursorConfig}</code>
              </pre>
              <button
                className="btn btn-sm copy-btn"
                onClick={() => handleCopy(cursorConfig, 'cursor')}
              >
                {copied.cursor ? <CheckIcon size={14} /> : <CopyIcon size={14} />}
              </button>
            </div>
          </div>
        )}
      </section>

      {/* 可用工具 */}
      <section className="setup-section">
        <div className="section-header mb-4">
          <h3 className="section-title">
            <ToolIcon size={20} className="mr-2" />
            可用工具
          </h3>
        </div>

        <div className="glass-card-static">
          <div className="tools-list">
            {availableTools.map((tool, index) => (
              <div key={index} className="tool-item">
                <div className="tool-name">
                  <code>{tool.name}</code>
                </div>
                <div className="tool-desc">{tool.description}</div>
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* 提示訊息 */}
      <div className="tips-box mt-6">
        <span className="tips-icon">
          <InfoIcon size={20} />
        </span>
        <div className="tips-content">
          <strong>提示：</strong>請先在「API 金鑰」頁面建立金鑰，取得 MCP 客戶端用金鑰（格式：{`{id}.{secret}`}），
          填入上方設定範例的 X-API-Key 欄位即可使用。
        </div>
      </div>
    </div>
  );
}
