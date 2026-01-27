/**
 * Button 組件使用範例
 * 此檔案展示 Button 組件的各種用法
 */
import React, { useState } from 'react';
import { Link } from 'react-router-dom';
import Button from './Button';

/**
 * 簡單的圖標組件示範
 */
const PlusIcon = () => (
  <svg width="16" height="16" viewBox="0 0 16 16" fill="currentColor">
    <path d="M8 2v12M2 8h12" stroke="currentColor" strokeWidth="2" strokeLinecap="round" />
  </svg>
);

const CheckIcon = () => (
  <svg width="16" height="16" viewBox="0 0 16 16" fill="currentColor">
    <path d="M3 8l3 3 7-7" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" fill="none" />
  </svg>
);

const TrashIcon = () => (
  <svg width="16" height="16" viewBox="0 0 16 16" fill="currentColor">
    <path d="M3 4h10M5 4V3a1 1 0 011-1h4a1 1 0 011 1v1m2 0v9a2 2 0 01-2 2H5a2 2 0 01-2-2V4h10z" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" fill="none" />
  </svg>
);

/**
 * Button 使用範例頁面
 */
export default function ButtonExamples() {
  const [loading, setLoading] = useState(false);

  const handleLoadingDemo = () => {
    setLoading(true);
    setTimeout(() => setLoading(false), 2000);
  };

  return (
    <div style={{ padding: '2rem', maxWidth: '1200px', margin: '0 auto' }}>
      <h1 style={{ color: 'var(--ink-black)', marginBottom: '2rem' }}>
        Button 組件使用範例
      </h1>

      {/* 基本變體 */}
      <section style={{ marginBottom: '3rem' }}>
        <h2 style={{ color: 'var(--ink-gray)', marginBottom: '1rem' }}>基本變體</h2>
        <div style={{ display: 'flex', gap: '1rem', flexWrap: 'wrap' }}>
          <Button variant="primary">Primary</Button>
          <Button variant="secondary">Secondary</Button>
          <Button variant="danger">Danger</Button>
          <Button variant="success">Success</Button>
          <Button variant="ghost">Ghost</Button>
          <Button variant="aurora">Aurora</Button>
          <Button variant="warning">Warning</Button>
          <Button variant="info">Info</Button>
        </div>
      </section>

      {/* Outline 變體 */}
      <section style={{ marginBottom: '3rem' }}>
        <h2 style={{ color: 'var(--ink-gray)', marginBottom: '1rem' }}>Outline 變體</h2>
        <div style={{ display: 'flex', gap: '1rem', flexWrap: 'wrap' }}>
          <Button variant="outline-primary">Outline Primary</Button>
          <Button variant="outline-danger">Outline Danger</Button>
          <Button variant="outline-success">Outline Success</Button>
          <Button variant="outline-warning">Outline Warning</Button>
          <Button variant="outline-info">Outline Info</Button>
        </div>
      </section>

      {/* 尺寸變體 */}
      <section style={{ marginBottom: '3rem' }}>
        <h2 style={{ color: 'var(--ink-gray)', marginBottom: '1rem' }}>尺寸變體</h2>
        <div style={{ display: 'flex', gap: '1rem', alignItems: 'center', flexWrap: 'wrap' }}>
          <Button variant="primary" size="sm">Small</Button>
          <Button variant="primary" size="md">Medium</Button>
          <Button variant="primary" size="lg">Large</Button>
        </div>
      </section>

      {/* 帶圖標的按鈕 */}
      <section style={{ marginBottom: '3rem' }}>
        <h2 style={{ color: 'var(--ink-gray)', marginBottom: '1rem' }}>帶圖標的按鈕</h2>
        <div style={{ display: 'flex', gap: '1rem', flexWrap: 'wrap' }}>
          <Button variant="primary" icon={<PlusIcon />}>
            新增
          </Button>
          <Button variant="success" icon={<CheckIcon />}>
            確認
          </Button>
          <Button variant="danger" icon={<TrashIcon />} iconPosition="right">
            刪除
          </Button>
          <Button variant="info" icon={<CheckIcon />}>
            查看詳情
          </Button>
        </div>
      </section>

      {/* 圓形按鈕 */}
      <section style={{ marginBottom: '3rem' }}>
        <h2 style={{ color: 'var(--ink-gray)', marginBottom: '1rem' }}>圓形按鈕</h2>
        <div style={{ display: 'flex', gap: '1rem', alignItems: 'center', flexWrap: 'wrap' }}>
          <Button variant="primary" circle icon={<PlusIcon />} />
          <Button variant="success" circle icon={<CheckIcon />} />
          <Button variant="danger" circle icon={<TrashIcon />} />
          <Button variant="aurora" circle icon={<PlusIcon />} size="lg" />
        </div>
      </section>

      {/* 狀態變體 */}
      <section style={{ marginBottom: '3rem' }}>
        <h2 style={{ color: 'var(--ink-gray)', marginBottom: '1rem' }}>按鈕狀態</h2>
        <div style={{ display: 'flex', gap: '1rem', flexWrap: 'wrap' }}>
          <Button variant="primary" disabled>
            Disabled
          </Button>
          <Button variant="primary" loading={loading} onClick={handleLoadingDemo}>
            {loading ? '處理中...' : '點擊測試載入'}
          </Button>
          <Button variant="success" loading>
            載入中
          </Button>
        </div>
      </section>

      {/* 作為連結使用 */}
      <section style={{ marginBottom: '3rem' }}>
        <h2 style={{ color: 'var(--ink-gray)', marginBottom: '1rem' }}>作為連結使用</h2>
        <div style={{ display: 'flex', gap: '1rem', flexWrap: 'wrap' }}>
          <Button as={Link} to="/" variant="primary">
            React Router Link
          </Button>
          <Button as="a" href="https://example.com" target="_blank" variant="info">
            外部連結
          </Button>
        </div>
      </section>

      {/* 按鈕群組 */}
      <section style={{ marginBottom: '3rem' }}>
        <h2 style={{ color: 'var(--ink-gray)', marginBottom: '1rem' }}>按鈕群組</h2>
        <div className="btn-group">
          <Button variant="outline-primary">選項 1</Button>
          <Button variant="outline-primary">選項 2</Button>
          <Button variant="outline-primary">選項 3</Button>
        </div>
      </section>

      {/* 實際使用場景 */}
      <section style={{ marginBottom: '3rem' }}>
        <h2 style={{ color: 'var(--ink-gray)', marginBottom: '1rem' }}>實際使用場景</h2>

        {/* 表單操作 */}
        <div style={{ marginBottom: '1.5rem' }}>
          <h3 style={{ color: 'var(--ink-cloud)', fontSize: '0.875rem', marginBottom: '0.5rem' }}>
            表單操作
          </h3>
          <div style={{ display: 'flex', gap: '0.5rem' }}>
            <Button variant="primary" icon={<CheckIcon />}>
              儲存
            </Button>
            <Button variant="secondary">
              取消
            </Button>
          </div>
        </div>

        {/* 危險操作 */}
        <div style={{ marginBottom: '1.5rem' }}>
          <h3 style={{ color: 'var(--ink-cloud)', fontSize: '0.875rem', marginBottom: '0.5rem' }}>
            危險操作
          </h3>
          <div style={{ display: 'flex', gap: '0.5rem' }}>
            <Button variant="outline-danger" size="sm" icon={<TrashIcon />}>
              刪除
            </Button>
            <Button variant="secondary" size="sm">
              取消
            </Button>
          </div>
        </div>

        {/* 通知操作 */}
        <div style={{ marginBottom: '1.5rem' }}>
          <h3 style={{ color: 'var(--ink-cloud)', fontSize: '0.875rem', marginBottom: '0.5rem' }}>
            通知與資訊
          </h3>
          <div style={{ display: 'flex', gap: '0.5rem' }}>
            <Button variant="warning" size="sm">
              警告
            </Button>
            <Button variant="info" size="sm">
              了解更多
            </Button>
            <Button variant="success" size="sm">
              完成
            </Button>
          </div>
        </div>

        {/* Hero 區塊 */}
        <div style={{ marginBottom: '1.5rem' }}>
          <h3 style={{ color: 'var(--ink-cloud)', fontSize: '0.875rem', marginBottom: '0.5rem' }}>
            Hero 區塊
          </h3>
          <div style={{ display: 'flex', gap: '1rem', alignItems: 'center' }}>
            <Button variant="aurora" size="lg" icon={<PlusIcon />}>
              開始使用
            </Button>
            <Button variant="outline-info" size="lg">
              了解更多
            </Button>
          </div>
        </div>
      </section>

      {/* 程式碼範例 */}
      <section style={{ marginBottom: '3rem' }}>
        <h2 style={{ color: 'var(--ink-gray)', marginBottom: '1rem' }}>程式碼範例</h2>
        <pre style={{
          background: 'var(--color-surface-elevated)',
          padding: '1rem',
          borderRadius: 'var(--radius-md)',
          overflow: 'auto',
          color: 'var(--ink-gray)',
          fontSize: '0.875rem'
        }}>
{`// 基本用法
<Button variant="primary">主要按鈕</Button>

// 帶圖標
<Button variant="success" icon={<CheckIcon />}>確認</Button>

// 載入狀態
<Button variant="primary" loading>處理中...</Button>

// 作為 Link 使用
<Button as={Link} to="/create" variant="aurora">建立文件</Button>

// 圓形按鈕
<Button variant="primary" circle icon={<PlusIcon />} />

// Outline 變體
<Button variant="outline-danger" size="sm">刪除</Button>`}
        </pre>
      </section>
    </div>
  );
}
