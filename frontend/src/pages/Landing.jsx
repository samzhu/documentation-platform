import { useNavigate } from 'react-router-dom';
import { useAuth } from '../hooks/useAuth';
// CSS 透過 index.css @import 引入，確保 CSS layer 順序正確

/**
 * Landing Page - 首頁
 * 沉浸式深空體驗 + Apple Liquid Glass 材質設計
 * 提供專案介紹和「開始使用」入口
 * 不需要認證即可訪問
 */
export default function Landing() {
  const navigate = useNavigate();
  const { isAuthenticated, oauth2Enabled, login } = useAuth();

  // 處理「開始使用」按鈕點擊
  const handleGetStarted = () => {
    if (!oauth2Enabled || isAuthenticated) {
      // OAuth2 關閉或已登入，直接進入 Dashboard
      navigate('/dashboard');
    } else {
      // 需要登入，觸發 OAuth2 流程
      login();
    }
  };

  return (
    <div className="landing-page">
      {/* 深空背景層 - 極光光球裝飾 */}
      <div className="landing-background">
        <div className="aurora-orb aurora-orb-1" />
        <div className="aurora-orb aurora-orb-2" />
        <div className="aurora-orb aurora-orb-3" />
        <div className="grid-overlay" />
      </div>

      {/* 內容層 */}
      <div className="landing-content">
        {/* Hero 區塊 */}
        <section className="landing-hero">
          <h1 className="hero-title">
            <span className="hero-title-line">Documentation</span>
            <span className="hero-title-line hero-title-accent">Platform</span>
          </h1>
          <p className="hero-subtitle">
            統一管理您的技術文件，支援 GitHub 同步、語意搜尋、版本管理
          </p>
          <button
            className="btn btn-aurora btn-lg hero-cta"
            onClick={handleGetStarted}
          >
            開始使用
            <ArrowIcon />
          </button>
        </section>

        {/* 功能卡片區塊 */}
        <section className="landing-features">
          <div className="feature-card glass-layered">
            <div className="feature-icon">
              <GitHubIcon />
            </div>
            <h3>GitHub 同步</h3>
            <p>自動從 GitHub Repository 同步文件，支援多版本管理</p>
          </div>
          <div className="feature-card glass-layered">
            <div className="feature-icon">
              <SearchIcon />
            </div>
            <h3>語意搜尋</h3>
            <p>基於 AI 的語意搜尋，快速找到相關文件</p>
          </div>
          <div className="feature-card glass-layered">
            <div className="feature-icon">
              <ApiIcon />
            </div>
            <h3>API 整合</h3>
            <p>提供 REST API，輕鬆整合到您的工作流程</p>
          </div>
        </section>
      </div>
    </div>
  );
}

/**
 * 箭頭圖示 - CTA 按鈕用
 */
function ArrowIcon() {
  return (
    <svg
      width="20"
      height="20"
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth="2"
      strokeLinecap="round"
      strokeLinejoin="round"
    >
      <line x1="5" y1="12" x2="19" y2="12" />
      <polyline points="12 5 19 12 12 19" />
    </svg>
  );
}

/**
 * GitHub 圖示 - 功能卡片用
 */
function GitHubIcon() {
  return (
    <svg
      width="24"
      height="24"
      viewBox="0 0 24 24"
      fill="currentColor"
    >
      <path d="M12 0c-6.626 0-12 5.373-12 12 0 5.302 3.438 9.8 8.207 11.387.599.111.793-.261.793-.577v-2.234c-3.338.726-4.033-1.416-4.033-1.416-.546-1.387-1.333-1.756-1.333-1.756-1.089-.745.083-.729.083-.729 1.205.084 1.839 1.237 1.839 1.237 1.07 1.834 2.807 1.304 3.492.997.107-.775.418-1.305.762-1.604-2.665-.305-5.467-1.334-5.467-5.931 0-1.311.469-2.381 1.236-3.221-.124-.303-.535-1.524.117-3.176 0 0 1.008-.322 3.301 1.23.957-.266 1.983-.399 3.003-.404 1.02.005 2.047.138 3.006.404 2.291-1.552 3.297-1.23 3.297-1.23.653 1.653.242 2.874.118 3.176.77.84 1.235 1.911 1.235 3.221 0 4.609-2.807 5.624-5.479 5.921.43.372.823 1.102.823 2.222v3.293c0 .319.192.694.801.576 4.765-1.589 8.199-6.086 8.199-11.386 0-6.627-5.373-12-12-12z"/>
    </svg>
  );
}

/**
 * 搜尋圖示 - 功能卡片用
 */
function SearchIcon() {
  return (
    <svg
      width="24"
      height="24"
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth="2"
      strokeLinecap="round"
      strokeLinejoin="round"
    >
      <circle cx="11" cy="11" r="8" />
      <line x1="21" y1="21" x2="16.65" y2="16.65" />
    </svg>
  );
}

/**
 * API 圖示 - 功能卡片用
 */
function ApiIcon() {
  return (
    <svg
      width="24"
      height="24"
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth="2"
      strokeLinecap="round"
      strokeLinejoin="round"
    >
      <path d="M4 17l6-6-6-6" />
      <path d="M12 19h8" />
    </svg>
  );
}
