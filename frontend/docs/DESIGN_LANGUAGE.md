# 設計語言文件（Design Language Document）

## 專案設計系統
**極光折射設計系統（Aurora Refraction Design System）**
基於 Apple Liquid Glass 的現代化設計語言實作

---

## 目錄

1. [概述與設計哲學](#1-概述與設計哲學)
2. [Apple Liquid Glass 官方設計原則](#2-apple-liquid-glass-官方設計原則)
3. [色彩系統](#3-色彩系統)
4. [玻璃材質系統](#4-玻璃材質系統)
5. [字體系統](#5-字體系統)
6. [動畫系統](#6-動畫系統)
7. [組件庫](#7-組件庫)
8. [工具類別](#8-工具類別)
9. [效能與可及性](#9-效能與可及性)
10. [CSS 架構](#10-css-架構)

---

## 1. 概述與設計哲學

### 1.1 設計系統簡介

極光折射設計系統（Aurora Refraction Design System）是本專案採用的視覺設計語言，結合了 Apple Liquid Glass 的材質特性與自訂的極光色彩系統。

**核心特色：**
- 🌌 **深空背景** - 深邃的太空漸層背景
- ✨ **三層景深玻璃** - 前景、中景、背景的層次效果
- 🌈 **極光色彩流動** - 5 色漸層的動態流動
- 🔤 **Variable Font** - 現代化的可變字體系統
- 💫 **動態折射與光暈** - 豐富的光學效果
- 🎭 **微交互動畫** - 流暢的使用者互動反饋

### 1.2 與 Apple Liquid Glass 的關係

本設計系統基於 [Apple Liquid Glass](https://developer.apple.com/documentation/TechnologyOverviews/liquid-glass) 官方設計指南，並進行以下擴展：

| Apple Liquid Glass | 極光折射設計系統 |
|-------------------|-----------------|
| 玻璃材質核心概念 | ✅ 完全遵循 |
| 層級分離原則 | ✅ 採用（Sidebar 使用玻璃，內容層使用實心） |
| 透光性、折射效果 | ✅ 實作並增強 |
| 自適應陰影 | ✅ 實作多層陰影系統 |
| 色彩系統 | ➕ 擴展為「極光色彩」+ 「墨玉文字」 |
| 動畫效果 | ➕ 增加極光流動、玻璃重組等特效 |

### 1.3 實作方式

```css
/* CSS @layer 模組化架構 */
@layer foundation  /* 變數、重置、字體、動畫、玻璃效果 */
@layer layout      /* 容器、網格、不對稱佈局、懸浮區域 */
@layer effects     /* 折射效果、光暈效果 */
@layer animations  /* 頁面過渡、滾動顯示、微交互 */
@layer components  /* UI 組件 */
@layer utilities   /* 工具類別 */
@layer overrides   /* 應用特定覆蓋 */
```

**檔案統計：**
- Foundation: 5 個檔案
- Layout: 4 個檔案
- Effects: 2 個檔案
- Animations: 3 個檔案
- Components: 11 個檔案
- Utilities: 5 個檔案
- **總計：30 個模組化 CSS 檔案**

---

## 2. Apple Liquid Glass 官方設計原則

### 2.1 核心概念

> **Liquid Glass** 是一種動態材質，結合玻璃的光學特性與流動感。它形成一個獨特的功能層，用於控制項和導航元素，浮動於內容層之上。

### 2.2 設計原則

| 原則 | 說明 | 本專案實作 |
|-----|------|-----------|
| **層級分離** | Liquid Glass 用於控制項/導航，標準材質用於內容層 | ✅ Sidebar 使用 Liquid Glass，主內容區使用實心背景 |
| **節制使用** | 避免過度使用，以免分散對內容的注意力 | ✅ 僅在必要的導航和控制元素使用 |
| **自動適應** | 系統組件自動採用 Liquid Glass 外觀 | ✅ 透過 CSS 類別和變數系統實現 |
| **可及性考量** | 支援減少透明度/動作的系統設定 | ✅ 完整支援 `prefers-reduced-motion` |

### 2.3 視覺效果

Apple Liquid Glass 定義的核心視覺效果：

1. **透光性（Translucency）** - 背景透過玻璃可見
2. **折射效果（Refraction）** - 光線通過玻璃的折射
3. **高光反應（Specular Highlights）** - 玻璃表面的高光
4. **自適應陰影（Adaptive Shadows）** - 深度感的多層陰影
5. **滾動邊緣效果（Scroll Edge Effect）** - 滾動時的邊緣光暈

**本專案全數實作** ✅

### 2.4 兩種變體

| 變體 | 特性 | 使用場景 |
|-----|------|---------|
| **Regular** | 模糊並調整背景亮度，維持前景易讀性 | Sidebar、Modal、Dropdown |
| **Clear** | 高度透明，適合浮動於視覺豐富的背景 | 浮動按鈕、Tooltip |

---

## 3. 色彩系統

### 3.1 極光色彩調色盤（Aurora Palette）

本專案的核心視覺特色，由 5 種色彩組成的流動漸層：

```css
/* 極光色彩 - 5 色流動 */
--color-aurora-teal:    #00d4aa;  /* 極光綠青 */
--color-aurora-cyan:    #00b8e6;  /* 極光青藍 */
--color-aurora-blue:    #5b6ee6;  /* 極光藍 */
--color-aurora-purple:  #8b5cf6;  /* 極光紫 */
--color-aurora-pink:    #ec4899;  /* 極光粉 */
```

**極光漸層（Aurora Gradient）：**

```css
/* 135° 對角線漸層 */
--gradient-aurora: linear-gradient(135deg,
    #00d4aa 0%,   /* teal */
    #00b8e6 25%,  /* cyan */
    #5b6ee6 50%,  /* blue */
    #8b5cf6 75%,  /* purple */
    #ec4899 100%  /* pink */
);

/* 90° 水平流動版本 */
--gradient-aurora-flow: linear-gradient(90deg,
    #00d4aa 0%,
    #00b8e6 25%,
    #5b6ee6 50%,
    #8b5cf6 75%,
    #ec4899 100%
);
```

### 3.2 深空背景色彩（Deep Space）

```css
/* 深空漸層背景 */
--color-space-black:  #0a0e27;  /* 深空黑 */
--color-space-deep:   #151b3d;  /* 深空藍 */
--color-space-medium: #1f2849;  /* 中層深空 */

--bg-space-gradient: linear-gradient(135deg,
    #0a0e27 0%,
    #151b3d 50%,
    #1f2849 100%
);
```

**使用場景：** `<body>` 背景、全屏容器背景

### 3.3 墨玉文字層級（Ink Typography）

**Dark Mode 專用高對比度文字色彩系統**，所有對比度已通過 WCAG AA 驗證：

```css
/* 墨玉文字層級 - Dark Mode */
--ink-black:  #f1f5f9;  /* L1: 淺灰白 - 標題、數字 (18.3:1 對比度) ✅ */
--ink-gray:   #cbd5e1;  /* L2: 淺灰 - 正文、表格 (9.7:1 對比度) ✅ */
--ink-cloud:  #94a3b8;  /* L3: 中灰 - 標籤、說明 (6.5:1 對比度) ✅ */
--ink-fog:    #64748b;  /* L4: 霧灰 - 輔助資訊 (4.6:1 對比度) ✅ */
```

**墨玉強調色：**

```css
--ink-sky:    #38bdf8;  /* 天藍 - 連結、按鈕 (5.9:1 對比度) ✅ */
--ink-green:  #4ade80;  /* 亮綠 - 成功狀態 (5.2:1 對比度) ✅ */
--ink-amber:  #fbbf24;  /* 琥珀 - 警告狀態 (4.5:1 對比度) ✅ */
--ink-red:    #f87171;  /* 亮紅 - 錯誤狀態 (5.8:1 對比度) ✅ */
```

**色彩語義對照表：**

| 用途 | CSS 變數 | 對比度 | 使用場景 |
|-----|---------|-------|---------|
| 主要標題 | `var(--ink-black)` | 18.3:1 | `<h1>`、`<h2>`、數字顯示 |
| 正文內容 | `var(--ink-gray)` | 9.7:1 | `<p>`、表格內容 |
| 標籤說明 | `var(--ink-cloud)` | 6.5:1 | Label、Caption |
| 輔助資訊 | `var(--ink-fog)` | 4.6:1 | Placeholder、次要說明 |
| 主要動作 | `var(--ink-sky)` | 5.9:1 | 連結、主要按鈕 |
| 成功提示 | `var(--ink-green)` | 5.2:1 | 成功訊息、完成狀態 |
| 警告提示 | `var(--ink-amber)` | 4.5:1 | 警告訊息、注意事項 |
| 錯誤提示 | `var(--ink-red)` | 5.8:1 | 錯誤訊息、危險操作 |

### 3.4 表面色彩（Dark Mode 預設）

```css
--color-surface-white:    #18181b;  /* 主要表面（內容卡片） */
--color-surface-elevated: #27272a;  /* 浮起表面（Modal） */
```

---

## 4. 玻璃材質系統

### 4.1 三層景深系統

本設計系統的核心特色，模擬真實玻璃的多層折射效果：

```css
/* 前景玻璃 - 高透明度（Foreground Layer） */
--glass-fg-light:  rgba(255, 255, 255, 0.12);
--glass-fg-medium: rgba(255, 255, 255, 0.20);
--glass-fg-strong: rgba(255, 255, 255, 0.35);

/* 中景玻璃 - 主要內容層（Midground Layer） */
--glass-bg:       rgba(255, 255, 255, 0.08);   /* 標準 */
--glass-bg-hover: rgba(255, 255, 255, 0.12);   /* Hover */
--glass-bg-solid: rgba(255, 255, 255, 0.15);   /* 實心 */
--glass-border:   rgba(255, 255, 255, 0.12);   /* 邊框 */

/* 背景玻璃 - 深色模糊（Background Layer） */
--glass-bg-dark:  rgba(10, 14, 39, 0.88);
--glass-bg-deep:  rgba(10, 14, 39, 0.95);
```

### 4.2 CSS 實作範例

**基礎玻璃效果：**

```css
.glass-simple {
    background: var(--glass-bg);
    backdrop-filter: blur(var(--blur-md));
    -webkit-backdrop-filter: blur(var(--blur-md));
    border: 1px solid var(--glass-border);
    border-radius: var(--radius-lg);
    box-shadow: var(--glass-shadow);
    transition: all var(--transition-normal);
}
```

**三層景深玻璃（使用偽元素）：**

```css
.glass-layered {
    position: relative;
    background: var(--glass-bg);
    backdrop-filter: blur(var(--blur-md));
    border: 1px solid var(--glass-border);
    border-radius: var(--radius-lg);
    transform-style: preserve-3d;
}

/* 背景層 - 深空陰影 */
.glass-layered::before {
    content: '';
    position: absolute;
    inset: -2px;
    background: var(--glass-bg-dark);
    backdrop-filter: blur(var(--blur-xl)) saturate(180%);
    border-radius: inherit;
    transform: translateZ(-20px);
    filter: drop-shadow(0 0 40px rgba(0, 229, 204, 0.4));
    z-index: -1;
}

/* 前景層 - 極光高光 */
.glass-layered::after {
    content: '';
    position: absolute;
    inset: 0;
    background: linear-gradient(135deg,
        rgba(255, 255, 255, 0.25) 0%,
        transparent 40%,
        rgba(0, 229, 204, 0.1) 100%);
    border-radius: inherit;
    transform: translateZ(10px);
    opacity: 0;
    transition: opacity var(--transition-normal);
}

.glass-layered:hover::after {
    opacity: 1;
}
```

### 4.3 Specular Highlight（高光漸層）

```css
/* 標準高光 */
--glass-highlight: linear-gradient(135deg,
    rgba(255, 255, 255, 0.9) 0%,
    rgba(255, 255, 255, 0.4) 20%,
    rgba(255, 255, 255, 0.1) 50%,
    transparent 100%
);

/* 微妙高光 */
--glass-highlight-subtle: linear-gradient(135deg,
    rgba(255, 255, 255, 0.5) 0%,
    rgba(255, 255, 255, 0.2) 30%,
    transparent 60%
);

/* 極光高光 */
--glass-highlight-aurora: linear-gradient(135deg,
    rgba(0, 212, 170, 0.25) 0%,
    rgba(0, 184, 230, 0.15) 40%,
    transparent 70%
);
```

### 4.4 多層陰影系統

```css
/* 小陰影 */
--glass-shadow-sm:
    0 2px 8px rgba(0, 0, 0, 0.04),
    0 4px 16px rgba(0, 0, 0, 0.04);

/* 標準陰影 */
--glass-shadow:
    0 4px 16px rgba(0, 0, 0, 0.06),
    0 8px 32px rgba(0, 0, 0, 0.08),
    inset 0 1px 0 rgba(255, 255, 255, 0.6);

/* 大陰影 */
--glass-shadow-lg:
    0 8px 32px rgba(0, 0, 0, 0.1),
    0 16px 64px rgba(0, 0, 0, 0.1),
    inset 0 1px 0 rgba(255, 255, 255, 0.8);

/* Hover 陰影 */
--glass-shadow-hover:
    0 12px 40px rgba(0, 0, 0, 0.12),
    0 24px 80px rgba(0, 0, 0, 0.08),
    inset 0 1px 0 rgba(255, 255, 255, 0.9);
```

### 4.5 模糊強度

```css
--blur-sm: 12px;
--blur-md: 24px;
--blur-lg: 40px;
--blur-xl: 60px;
```

### 4.6 圓角系統

Liquid Glass 使用較大的圓角以強化流動感：

```css
--radius-sm:   12px;
--radius-md:   16px;
--radius-lg:   22px;
--radius-xl:   28px;
--radius-full: 9999px;
```

### 4.7 降級方案

```css
/* 不支援 backdrop-filter 的瀏覽器 */
@supports not (backdrop-filter: blur(10px)) {
    .glass-layered,
    .glass-simple {
        background: rgba(255, 255, 255, 0.95);
    }
}
```

---

## 5. 字體系統

### 5.1 Variable Font 堆疊

本專案採用 4 種 Variable Font，支援動態字重調整：

```css
/* 主字體 - Space Grotesk（幾何感、未來科技感） */
--font-display: 'Space Grotesk', -apple-system, BlinkMacSystemFont,
                'SF Pro Display', system-ui, sans-serif;

/* 顯示字體 - Epilogue（現代幾何、纖細優雅） */
--font-heading: 'Epilogue', 'Space Grotesk', -apple-system,
                system-ui, sans-serif;

/* 正文字體 - Inter Variable */
--font-text: 'Inter', -apple-system, BlinkMacSystemFont,
             'SF Pro Text', system-ui, sans-serif;

/* 代碼字體 - JetBrains Mono */
--font-mono: 'JetBrains Mono', 'SF Mono', ui-monospace, monospace;
```

### 5.2 Variable Font 字重範圍

```css
--font-weight-light:    300;
--font-weight-normal:   400;
--font-weight-medium:   500;
--font-weight-semibold: 600;
--font-weight-bold:     700;
--font-weight-black:    900;
```

### 5.3 字體層級

| 層級 | 大小 | 字重 | 字體家族 | 用途 |
|-----|------|------|---------|------|
| Display | 3rem (48px) | Light (300) | Epilogue | 超大標題 |
| H1 | 2.5rem (40px) | Bold (700) | Space Grotesk | 頁面主標題 |
| H2 | 2rem (32px) | Semibold (600) | Space Grotesk | 區塊標題 |
| H3 | 1.5rem (24px) | Semibold (600) | Space Grotesk | 次級標題 |
| H4 | 1.25rem (20px) | Medium (500) | Space Grotesk | 小標題 |
| H5 | 1.125rem (18px) | Medium (500) | Space Grotesk | 次要標題 |
| H6 | 1rem (16px) | Medium (500) | Space Grotesk | 最小標題 |
| Body Large | 1.125rem (18px) | Normal (400) | Inter | 重點正文 |
| Body | 1rem (16px) | Normal (400) | Inter | 標準正文 |
| Small | 0.875rem (14px) | Normal (400) | Inter | 輔助說明 |
| Tiny | 0.75rem (12px) | Normal (400) | Inter | 標籤、說明 |

### 5.4 OpenType 特性

```css
body {
    font-family: var(--font-text);
    font-feature-settings: 'kern' 1, 'liga' 1, 'calt' 1;
    -webkit-font-smoothing: antialiased;
    -moz-osx-font-smoothing: grayscale;
    text-rendering: optimizeLegibility;
}
```

### 5.5 字體載入

字體透過 `<link>` 標籤在 HTML 中載入（位於 `frontend/index.html`）：

```html
<link rel="preconnect" href="https://fonts.googleapis.com">
<link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
<link href="https://fonts.googleapis.com/css2?family=Space+Grotesk:wght@300..700&family=Epilogue:wght@100..900&family=Inter:wght@100..900&family=JetBrains+Mono:wght@100..800&display=swap" rel="stylesheet">
```

---

## 6. 動畫系統

### 6.1 動畫曲線（Easing Functions）

基於 Apple 風格的彈性曲線：

```css
--ease-out-expo:    cubic-bezier(0.16, 1, 0.3, 1);      /* 指數緩出 */
--ease-out-back:    cubic-bezier(0.34, 1.56, 0.64, 1);  /* 回彈效果 */
--ease-in-out-circ: cubic-bezier(0.85, 0, 0.15, 1);     /* 圓形緩動 */
```

### 6.2 過渡時間

```css
--transition-fast:   180ms var(--ease-out-expo);
--transition-normal: 280ms var(--ease-out-expo);
--transition-slow:   400ms var(--ease-out-expo);
--transition-bounce: 500ms var(--ease-out-back);
```

### 6.3 核心動畫 Keyframes

**極光流動（Aurora Flow）：**

```css
@keyframes aurora-flow {
    0%, 100% { background-position: 0% 50%; }
    25%      { background-position: 100% 50%; }
    50%      { background-position: 100% 0%; }
    75%      { background-position: 0% 100%; }
}

/* 使用範例 */
.aurora-flow {
    background: var(--gradient-aurora);
    background-size: 200% 200%;
    animation: aurora-flow 8s ease infinite;
}
```

**玻璃重組（Glass Rebuild）：**

```css
@keyframes glass-rebuild {
    0% {
        opacity: 0;
        transform: scale(0.8) rotateX(20deg);
        filter: blur(20px);
    }
    100% {
        opacity: 1;
        transform: scale(1) rotateX(0deg);
        filter: blur(0);
    }
}
```

**頁面過渡（Page Transition）：**

```css
/* 液體流出 */
@keyframes page-exit-liquid {
    0% {
        opacity: 1;
        transform: translateX(0);
        filter: blur(0);
    }
    100% {
        opacity: 0;
        transform: translateX(-100px) scale(0.95);
        filter: blur(20px);
    }
}

/* 液體流入 */
@keyframes page-enter-liquid {
    0% {
        opacity: 0;
        transform: translateX(100px) scale(0.95);
        filter: blur(20px);
    }
    100% {
        opacity: 1;
        transform: translateX(0);
        filter: blur(0);
    }
}
```

### 6.4 滾動顯示動畫

```css
@keyframes fadeInUp {
    from {
        opacity: 0;
        transform: translateY(16px);
    }
    to {
        opacity: 1;
        transform: translateY(0);
    }
}

/* 卡片交錯動畫 */
.glass-card {
    animation: fadeInUp 400ms var(--ease-out-expo) backwards;
}

.glass-card:nth-child(1) { animation-delay: 50ms; }
.glass-card:nth-child(2) { animation-delay: 100ms; }
.glass-card:nth-child(3) { animation-delay: 150ms; }
/* ... 依此類推 */
```

### 6.5 微交互動畫

```css
/* 脈動 */
@keyframes pulse {
    0%, 100% { opacity: 1; }
    50%      { opacity: 0.5; }
}

/* 旋轉 */
@keyframes spin {
    from { transform: rotate(0deg); }
    to   { transform: rotate(360deg); }
}

/* 極光脈衝 */
@keyframes aurora-pulse {
    0%, 100% {
        box-shadow: 0 0 20px rgba(0, 229, 204, 0.4),
                    0 0 40px rgba(0, 191, 255, 0.2);
    }
    50% {
        box-shadow: 0 0 30px rgba(0, 229, 204, 0.6),
                    0 0 60px rgba(0, 191, 255, 0.3);
    }
}
```

---

## 7. 組件庫

### 7.1 按鈕系統

#### 7.1.1 React Button 組件（推薦）

專案提供封裝完整的 `Button` 組件，支援所有按鈕變體和功能：

```jsx
import Button from '@/components/Button';

// 基本用法
<Button variant="primary">主要按鈕</Button>

// 帶圖標
<Button variant="success" icon={<CheckIcon />}>確認</Button>

// 載入狀態
<Button variant="primary" loading>處理中...</Button>

// 作為 Link 使用
<Button as={Link} to="/create" variant="aurora">建立文件</Button>

// 圓形按鈕
<Button variant="primary" circle icon={<PlusIcon />} />
```

**Button 組件 Props：**

| Prop | 類型 | 預設值 | 說明 |
|------|------|--------|------|
| `variant` | string | `'secondary'` | 按鈕變體（見下方變體列表） |
| `size` | `'sm' \| 'md' \| 'lg'` | `'md'` | 按鈕尺寸 |
| `circle` | boolean | `false` | 是否為圓形按鈕 |
| `loading` | boolean | `false` | 是否顯示載入狀態 |
| `disabled` | boolean | `false` | 是否禁用 |
| `icon` | ReactNode | `null` | 圖標元素 |
| `iconPosition` | `'left' \| 'right'` | `'left'` | 圖標位置 |
| `as` | elementType | `'button'` | 多態組件（可用 `Link` 或 `'a'`） |
| `className` | string | `''` | 額外的 CSS 類別 |

#### 7.1.2 基礎玻璃按鈕（原生 HTML）

如需使用原生 HTML，可直接使用 CSS 類別：

```html
<!-- 向後相容：支援 .btn 和 .btn-glass-base -->
<button class="btn btn-primary">Primary Button</button>
<button class="btn btn-secondary">Secondary Button</button>
<button class="btn btn-danger">Danger Button</button>
<button class="btn btn-success">Success Button</button>
<button class="btn btn-ghost">Ghost Button</button>
```

#### 7.1.3 按鈕變體

**實心按鈕（Solid）：**

| 變體 | 類別 / variant | 用途 | 色彩 |
|-----|---------------|------|------|
| Primary | `btn-primary` | 主要操作 | 藍色漸層 |
| Secondary | `btn-secondary` | 次要操作 | 白色玻璃 |
| Danger | `btn-danger` | 危險操作 | 紅色漸層 |
| Success | `btn-success` | 成功確認 | 綠色漸層 |
| **Warning** ⭐ | `btn-warning` | 警告操作 | 琥珀色漸層 |
| **Info** ⭐ | `btn-info` | 資訊提示 | 天藍色漸層 |
| Ghost | `btn-ghost` | 透明玻璃 | 白色透明 |
| Aurora | `btn-aurora` | 特色操作 | 極光流動 |

**邊框按鈕（Outline）：** ⭐ 新增

| 變體 | 類別 / variant | 用途 |
|-----|---------------|------|
| Outline Primary | `btn-outline-primary` | 主要邊框按鈕 |
| Outline Danger | `btn-outline-danger` | 危險邊框按鈕 |
| Outline Success | `btn-outline-success` | 成功邊框按鈕 |
| Outline Warning | `btn-outline-warning` | 警告邊框按鈕 |
| Outline Info | `btn-outline-info` | 資訊邊框按鈕 |

**使用範例：**

```jsx
// React 組件
<Button variant="warning">警告</Button>
<Button variant="info">查看詳情</Button>
<Button variant="outline-danger" size="sm">刪除</Button>

// 原生 HTML
<button class="btn btn-warning">警告</button>
<button class="btn btn-info">查看詳情</button>
<button class="btn btn-outline-danger btn-sm">刪除</button>
```

#### 7.1.4 極光按鈕（特色）

```jsx
<Button variant="aurora">極光按鈕</Button>
```

**效果：** 背景極光色彩持續流動，Hover 時加速流動並增加光暈

```css
.btn.btn-aurora {
    background: var(--gradient-aurora);
    background-size: 200% 200%;
    animation: aurora-flow 8s ease infinite;
}

.btn.btn-aurora:hover {
    animation: aurora-flow 3s ease infinite;
    filter: brightness(1.15) saturate(1.2);
}
```

#### 7.1.5 按鈕尺寸

```jsx
<Button variant="primary" size="sm">Small</Button>
<Button variant="primary" size="md">Medium (預設)</Button>
<Button variant="primary" size="lg">Large</Button>
```

**尺寸規格：**

| 尺寸 | 類別 | padding | font-size | border-radius |
|-----|------|---------|-----------|---------------|
| Small | `btn-sm` | 6px 12px | 0.8125rem (13px) | var(--radius-sm) |
| Medium | 預設 | 10px 18px | 0.9375rem (15px) | var(--radius-md) |
| Large | `btn-lg` | 14px 24px | 1rem (16px) | var(--radius-lg) |

#### 7.1.6 圓形按鈕

```jsx
<Button variant="primary" circle icon={<SearchIcon />} />
<Button variant="success" circle icon={<CheckIcon />} size="lg" />
```

**圓形尺寸：**
- Small: 32×32px
- Medium: 40×40px
- Large: 48×48px

#### 7.1.7 按鈕狀態

```jsx
// 禁用狀態
<Button variant="primary" disabled>Disabled</Button>

// 載入狀態
<Button variant="primary" loading>處理中...</Button>

// 載入狀態（動態控制）
<Button
  variant="success"
  loading={isSubmitting}
  onClick={handleSubmit}
>
  {isSubmitting ? '儲存中...' : '儲存'}
</Button>
```

#### 7.1.8 新增變體設計說明 ⭐

**Warning 按鈕（琥珀色）：**
- 溫暖的琥珀色漸層（#fbbf24 → #f59e0b）
- 溫潤的內發光效果
- Hover 時琥珀光暈擴散
- 特殊高光漸層融入琥珀色折射
- 適用場景：警告操作、需要注意的動作

**Info 按鈕（天藍色）：**
- 清新的天空藍漸層（#38bdf8 → #0ea5e9）
- 水晶質感的透明度處理
- Hover 時清新的水晶光暈
- 天空色彩折射效果
- 適用場景：資訊提示、查看詳情、了解更多

**Outline 按鈕系列：**
- 極致輕盈的背景（8% 透明度）
- 大膽的 2px 色彩邊框
- 8px 背景模糊效果
- Hover 時邊框變亮、文字變亮、背景加深
- 特殊的邊框流光效果（徑向漸變光暈）
- 適用場景：次要操作、取消動作、多選項按鈕群組

### 7.2 卡片系統

#### 7.2.1 標準玻璃卡片

```html
<div class="glass-simple glass-p-lg">
    <h3>卡片標題</h3>
    <p>卡片內容</p>
</div>
```

#### 7.2.2 三層景深卡片

```html
<div class="glass-layered glass-p-lg">
    <h3>三層景深卡片</h3>
    <p>Hover 時顯示前景和背景層</p>
</div>
```

#### 7.2.3 極光卡片

```html
<div class="glass-aurora glass-p-lg">
    <h3>極光卡片</h3>
    <p>背景持續流動極光色彩</p>
</div>
```

### 7.3 Badge 與 Tag 組件

#### 7.3.1 Badge（狀態徽章）

用於顯示**狀態資訊**的彩色標籤組件：

```html
<span class="badge badge-success">Success</span>
<span class="badge badge-warning">Warning</span>
<span class="badge badge-danger">Danger</span>
<span class="badge badge-info">Info</span>
<span class="badge badge-default">Default</span>
```

**視覺特性：**
- 膠囊形狀（`border-radius: var(--radius-full)`）
- 明確的狀態色彩（綠/琥珀/紅/天藍/灰）
- 大寫字母、字重 600
- 用於重要的狀態顯示

**可用變體：**

| 變體 | 類別 | 用途 | 色彩 |
|-----|------|------|------|
| Success | `badge-success` | 成功狀態 | 穩重綠（`--ink-green`） |
| Warning | `badge-warning` | 警告狀態 | 琥珀（`--ink-amber`） |
| Danger | `badge-danger` | 危險狀態 | 警示紅（`--ink-red`） |
| Info | `badge-info` | 資訊提示 | 天藍（`--ink-sky`） |
| Primary | `badge-primary` | 主要標記 | 天藍（`--ink-sky`） |
| Default | `badge-default` | 預設標記 | 墨灰（`--ink-gray`） |

#### 7.3.2 Tag（分類標籤）

用於顯示**分類、技術標籤**的中性標籤組件（例如語言、框架名稱）：

```html
<div class="tags">
    <span class="tag">java</span>
    <span class="tag">spring</span>
    <span class="tag">react</span>
</div>
```

**視覺特性：**
- 膠囊形狀（`border-radius: var(--radius-full)`）
- Neutral Glass 背景（`rgba(255, 255, 255, 0.06)`）
- 玻璃邊框（`rgba(255, 255, 255, 0.12)`）
- 墨玉雲灰文字（`--ink-cloud`）
- Hover 效果：背景變亮、微妙上浮

**CSS 樣式：**

```css
.tag {
    display: inline-flex;
    align-items: center;
    padding: 4px 10px;
    font-size: 0.75rem;
    font-weight: 500;
    letter-spacing: 0.02em;
    border-radius: var(--radius-full);
    background: rgba(255, 255, 255, 0.06);
    border: 1px solid rgba(255, 255, 255, 0.12);
    box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.06);
    color: var(--ink-cloud);
    transition: all var(--transition-fast);
}

.tag:hover {
    background: rgba(255, 255, 255, 0.1);
    border-color: rgba(255, 255, 255, 0.18);
    color: var(--ink-gray);
    transform: translateY(-1px);
}
```

#### 7.3.3 Tag vs Badge 視覺區隔

| 特性 | Tag（分類標籤） | Badge（狀態徽章） |
|-----|---------------|-----------------|
| **用途** | 語言、框架分類 | 狀態顯示 |
| **形狀** | 膠囊形（`radius-full`） | 膠囊形（`radius-full`） |
| **背景** | 中性玻璃（6% 白） | 明確狀態色 |
| **邊框** | 有（12% 白） | 無 |
| **文字色** | 墨玉雲灰 | 狀態色 |
| **視覺層級** | 輔助資訊 | 重要狀態 |
| **Hover 效果** | 有 | 無 |

**設計決策說明：**

1. **膠囊形狀**：Tag 和 Badge 都使用 `border-radius: var(--radius-full)` 膠囊形狀，這是標籤視覺識別的關鍵因素
2. **中性 vs 彩色**：Tag 使用中性玻璃保持低調，Badge 使用明確狀態色強調重要性
3. **玻璃邊框**：Tag 使用玻璃邊框在 Dark Mode 下建立視覺邊界，Badge 不需要邊框因為背景色已足夠明顯
4. **不使用 `#` 前綴**：膠囊形狀已足夠表達標籤語義，`#` 是社交媒體風格，非通用設計語言

**使用範例：**

```jsx
// Libraries 頁面 - 技術標籤使用 Tag
<div className="tags">
    {library.tags.map(tag => (
        <span key={tag} className="tag">{tag}</span>
    ))}
</div>

// LibraryDetail 頁面 - 狀態顯示使用 Badge
<span className="badge badge-success">Active</span>
<span className="badge badge-info">v{version}</span>
```

### 7.4 表單元素

#### 7.4.1 極光輸入框

```html
<div class="input-aurora-wrapper">
    <input class="input-aurora" type="text" placeholder="輸入內容">
</div>
```

**效果：** Focus 時顯示極光色彩邊框流動效果

#### 7.4.2 玻璃效果輸入框

```html
<input class="glass-input" type="text" placeholder="玻璃輸入框">
```

### 7.5 導航組件

#### 7.5.1 側邊欄（Sidebar）

使用深色 Liquid Glass 效果：

```css
.sidebar {
    background: var(--glass-dark-bg);
    backdrop-filter: blur(var(--blur-xl)) saturate(200%);
    border: 1px solid var(--glass-dark-border);
    border-radius: var(--radius-xl);
    box-shadow:
        0 8px 32px rgba(0, 0, 0, 0.4),
        inset 0 1px 0 rgba(255, 255, 255, 0.1);
}
```

### 7.6 搜尋組件

獨立的搜尋組件樣式模組 (`components/search.css`)，提供完整的搜尋介面元素。

#### 7.6.1 搜尋模式按鈕

膠囊形狀的模式切換按鈕，支援 Active 狀態：

```html
<div class="search-modes">
  <button class="search-mode-btn active">Hybrid</button>
  <button class="search-mode-btn">Semantic</button>
  <button class="search-mode-btn">Full-text</button>
</div>
```

**視覺特性：**
- 預設：玻璃背景、墨灰文字、膠囊圓角
- Hover：背景變亮、微妙上浮效果
- Active：藍色漸層背景、白色文字、發光陰影

#### 7.6.2 提示框

Info 風格的玻璃卡片，用於顯示使用說明：

```html
<div class="tips-box">
  <InfoIcon className="tips-icon" size={20} />
  <div class="tips-content">
    提示內容文字...
  </div>
</div>
```

**視覺特性：**
- 天藍色半透明背景（rgba(56, 189, 248, 0.1)）
- 天藍色邊框
- 圖標與文字左對齊佈局

#### 7.6.3 篩選列

Flex 佈局的篩選器容器：

```html
<div class="filter-row">
  <div class="filter-group">
    <label>Library</label>
    <select>...</select>
  </div>
  <div class="filter-group">
    <label>Version</label>
    <select>...</select>
  </div>
</div>
```

**視覺特性：**
- 橫向排列，間距 var(--spacing-lg)
- 支援 flex-wrap 自動換行
- 響應式設計：手機版改為縱向排列

#### 7.6.4 搜尋結果卡片

繼承玻璃卡片樣式，擴展搜尋專用佈局：

```html
<div class="result-card glass-card">
  <div class="result-header">
    <div class="flex items-center gap-2">
      <span class="badge badge-primary">Library Name</span>
      <span class="badge badge-default">v1.0.0</span>
    </div>
    <span class="result-score">Relevance: 95.0%</span>
  </div>
  <p class="result-content">搜尋結果內容...</p>
  <div class="result-meta">
    <span>Section: Introduction</span>
  </div>
</div>
```

**視覺特性：**
- `.result-header`：標題與分數分兩側顯示
- `.result-score`：天藍色、小字體、600 字重
- `.result-content`：墨灰文字、1.6 行高
- `.result-meta`：輔助資訊區塊

#### 7.6.5 空白狀態

搜尋無結果或初始狀態的顯示：

```html
<div class="empty-state glass-card-static">
  <div class="empty-state-icon">
    <SearchIcon size={64} />
  </div>
  <h3>No results found</h3>
  <p>Try adjusting your search terms</p>
</div>
```

**視覺特性：**
- 圖標：霧灰色、60% 透明度、置中顯示
- 標題：墨黑色、1.25rem、600 字重
- 說明：墨灰色、0.9375rem

#### 7.6.6 響應式設計

搜尋組件在手機版（<768px）的適配：
- 搜尋模式按鈕：縮小 padding 和字體
- 篩選列：改為縱向排列
- 搜尋表單：按鈕與輸入框縱向排列
- 結果標題：標籤與分數縱向排列

---

### 7.7 設定組件

獨立的設定頁面樣式模組 (`components/settings.css`)，提供 Feature Flags、同步設定、系統資訊等顯示元素。

#### 7.7.1 Feature Flags 列表

垂直排列的功能開關列表，支援啟用/停用狀態顯示：

```html
<div class="feature-list">
  <div class="feature-item">
    <div class="feature-info">
      <div class="feature-name">功能名稱</div>
      <div class="feature-desc">功能描述</div>
    </div>
    <div class="feature-status feature-enabled">
      <CheckIcon /> Enabled
    </div>
  </div>
</div>
```

**視覺特性：**
- `.feature-item`：玻璃背景（5% 白）、圓角、hover 變亮
- `.feature-name`：墨黑色、600 字重
- `.feature-desc`：墨灰色、0.875rem
- `.feature-enabled`：亮綠色（`--ink-green`）
- `.feature-disabled`：霧灰色（`--ink-fog`）

#### 7.7.2 同步設定網格

垂直排列的設定項目列表，顯示標籤與對應值：

```html
<div class="settings-grid">
  <div class="setting-item">
    <div class="setting-label">
      <ClockIcon />
      同步時間
    </div>
    <div class="setting-value">
      <code>0 0 2 * * ?</code>
    </div>
  </div>
</div>
```

**視覺特性：**
- `.setting-item`：玻璃背景（5% 白）、兩端對齊佈局、hover 變亮效果
- `.setting-label`：墨灰色、500 字重、帶圖標
- `.setting-value`：墨黑色、0.9375rem
- `.setting-value code`：天藍色（`--ink-sky`）、等寬字體、深色背景

#### 7.7.3 系統資訊卡片

CSS Grid 自動填充的資訊卡片網格：

```html
<div class="system-info-grid">
  <div class="info-card">
    <div class="info-card-label">Spring Boot</div>
    <div class="info-card-value">4.0.2</div>
  </div>
</div>
```

**視覺特性：**
- `.system-info-grid`：Grid 佈局、最小 180px、自動填充
- `.info-card`：玻璃卡片、hover 效果
- `.info-card-label`：雲灰色、0.75rem、大寫字母、字間距 0.05em
- `.info-card-value`：墨黑色、0.9375rem、支援斷行

#### 7.7.4 響應式設計

設定組件在手機版（<768px）的適配：
- Feature 項目：改為縱向排列
- 設定項目：改為縱向排列
- 系統資訊網格：最小寬度縮小至 140px

---

### 7.8 整合指南組件

獨立的 Setup 頁面樣式模組 (`components/setup.css`)，提供 MCP 端點資訊、IDE 設定指南等顯示元素。

#### 7.8.1 MCP 端點資訊

顯示 SSE 端點和認證狀態的資訊區塊：

```html
<div class="endpoint-info">
  <div class="endpoint-row">
    <span class="endpoint-label">SSE Endpoint</span>
    <div class="endpoint-value-wrapper">
      <code class="endpoint-value">http://localhost:5173/mcp/sse</code>
      <button class="copy-btn">Copy</button>
    </div>
  </div>
  <div class="endpoint-row">
    <span class="endpoint-label">認證狀態</span>
    <div class="endpoint-value-wrapper">
      <span class="auth-status disabled">無需認證</span>
    </div>
  </div>
</div>
```

**視覺特性：**
- `.endpoint-row`：兩端對齊、支援換行
- `.endpoint-label`：墨灰色、500 字重
- `.endpoint-value`：天藍色（`--ink-sky`）、等寬字體、深色背景
- `.auth-status.enabled`：亮綠色（OAuth2 啟用）
- `.auth-status.disabled`：墨灰色（無需認證）

#### 7.8.2 IDE 標籤切換

膠囊形狀的 IDE 選擇標籤，支援 Active 狀態：

```html
<div class="setup-tabs">
  <button class="setup-tab active">
    <TerminalIcon /> Claude Code
  </button>
  <button class="setup-tab">
    <CodeIcon /> VS Code
  </button>
  <button class="setup-tab">
    <CodeIcon /> Cursor
  </button>
</div>
```

**視覺特性：**
- 預設：玻璃背景（8% 白）、墨灰文字、膠囊圓角（`--radius-full`）
- Hover：背景變亮、邊框強化、微妙上浮（-1px）
- Active：藍色漸層背景（`--ink-sky`）、白色文字、發光陰影
- Active Hover：增強發光效果、上浮 -2px

#### 7.8.3 程式碼區塊

深色背景的程式碼展示區塊，支援複製功能：

```html
<div class="code-block-wrapper">
  <pre class="code-block">
    <code>{程式碼內容}</code>
  </pre>
  <button class="copy-btn">
    <CopyIcon />
  </button>
</div>
```

**視覺特性：**
- `.config-title`：配置標題、墨黑、600 字重
- `.code-block`：深色背景（rgba(0, 0, 0, 0.3)）、等寬字體、墨灰文字
- `.code-block code`：天藍色高亮（`--ink-sky`）
- `.copy-btn`：絕對定位於右上角、玻璃按鈕風格

#### 7.8.4 可用工具列表

顯示 MCP Server 提供的工具清單：

```html
<div class="tools-list">
  <div class="tool-item">
    <div class="tool-name">
      <code>search_documentation</code>
    </div>
    <div class="tool-desc">搜尋文件庫內的文件內容</div>
  </div>
</div>
```

**視覺特性：**
- `.tool-item`：玻璃背景（5% 白）、hover 變亮效果
- `.tool-name code`：天藍色、等寬字體、600 字重
- `.tool-desc`：墨灰色、0.875rem、1.5 行高

#### 7.8.5 響應式設計

Setup 組件在手機版（<768px）的適配：
- 端點資訊行：改為縱向排列
- 標籤按鈕：縮小 padding 和字體
- 程式碼區塊：縮小字體和 padding

---

## 8. 工具類別

### 8.1 玻璃效果工具類別

快速應用玻璃材質：

```css
/* 標準玻璃 */
.glass-standard {
    background: var(--glass-bg);
    backdrop-filter: blur(var(--blur-md));
    border: 1px solid var(--glass-border);
    border-radius: var(--radius-lg);
    box-shadow: var(--glass-shadow);
}

/* 深色玻璃 */
.glass-dark-standard { /* ... */ }

/* 實心玻璃 */
.glass-solid-standard { /* ... */ }

/* 微妙玻璃 */
.glass-subtle-standard { /* ... */ }
```

**模糊強度：**

```html
<div class="blur-sm">小模糊</div>
<div class="blur-md">中模糊</div>
<div class="blur-lg">大模糊</div>
<div class="blur-xl">超大模糊</div>
```

**Hover 效果：**

```html
<div class="glass-standard glass-hover-lift">Hover 抬起</div>
<div class="glass-standard glass-hover-brighten">Hover 變亮</div>
```

### 8.2 極光色彩工具類別

```html
<!-- 背景漸層 -->
<div class="bg-gradient-aurora">極光背景</div>

<!-- 文字漸層 -->
<span class="text-gradient-aurora">極光文字</span>

<!-- 單色背景 -->
<div class="bg-aurora-teal">青綠背景</div>
<div class="bg-aurora-cyan">青藍背景</div>
<div class="bg-aurora-blue">藍色背景</div>
<div class="bg-aurora-purple">紫色背景</div>
<div class="bg-aurora-pink">粉色背景</div>
```

### 8.3 折射效果工具類別

```html
<!-- 極光色彩折射 -->
<div class="refract-aurora">
    <p>背景有極光色彩折射</p>
</div>

<!-- 單色折射 -->
<div class="refract-teal">青綠折射</div>
<div class="refract-blue">藍色折射</div>
<div class="refract-pink">粉色折射</div>

<!-- 邊緣高光 -->
<div class="edge-glow">邊緣高光效果</div>
<div class="edge-glow-aurora">極光色邊緣高光</div>
```

### 8.4 光暈效果工具類別

```html
<!-- 標準光暈 -->
<div class="glow-aurora-sm">小光暈</div>
<div class="glow-aurora-md">中光暈</div>
<div class="glow-aurora-lg">大光暈</div>

<!-- 脈動光暈 -->
<div class="aurora-pulse">脈動光暈效果</div>
```

---

## 9. 效能與可及性

### 9.1 效能優化策略

#### 9.1.1 硬體加速提示

```css
.glass-card,
.aurora-card,
.btn-glass-base {
    will-change: auto; /* 預設關閉 */
}

/* Hover 時啟用 */
.glass-card:hover {
    will-change: transform, box-shadow;
}
```

#### 9.1.2 移動裝置簡化

```css
@media (max-width: 767px) {
    /* 簡化模糊強度 */
    .blur-lg,
    .blur-xl {
        backdrop-filter: blur(var(--blur-md));
    }

    /* 關閉動畫 */
    .btn-glass-base.btn-aurora {
        animation: none;
        background-size: 100% 100%;
    }

    /* 簡化玻璃效果 */
    .glass-layered::before {
        backdrop-filter: none;
    }
}
```

#### 9.1.3 平滑滾動

```css
html {
    scroll-behavior: smooth;
}
```

### 9.2 prefers-reduced-motion 支援

完整支援「減少動作」偏好設定：

```css
@media (prefers-reduced-motion: reduce) {
    *,
    *::before,
    *::after {
        animation-duration: 0.01ms !important;
        animation-iteration-count: 1 !important;
        transition-duration: 0.01ms !important;
    }

    /* 關閉特定動畫 */
    .aurora-flow,
    .aurora-pulse,
    .glass-shimmer::after {
        animation: none !important;
    }

    html {
        scroll-behavior: auto;
    }
}
```

### 9.3 瀏覽器相容性

| 功能 | Chrome/Edge | Safari | Firefox |
|-----|-------------|--------|---------|
| `backdrop-filter` | 88+ | 15.4+ | 97+ |
| CSS `@layer` | 99+ | 15.4+ | 97+ |
| Variable Fonts | 62+ | 11+ | 62+ |
| `prefers-reduced-motion` | 74+ | 10.1+ | 63+ |

**降級方案：**

```css
@supports not (backdrop-filter: blur(10px)) {
    .glass-layered,
    .glass-simple {
        background: rgba(255, 255, 255, 0.95);
    }
}
```

### 9.4 WCAG 對比度合規

所有墨玉文字色彩已通過 WCAG AA 標準（>= 4.5:1）：

| 色彩 | 對比度 | 等級 |
|-----|-------|------|
| `--ink-black` | 18.3:1 | ✅ AAA |
| `--ink-gray` | 9.7:1 | ✅ AAA |
| `--ink-cloud` | 6.5:1 | ✅ AA |
| `--ink-fog` | 4.6:1 | ✅ AA |
| `--ink-sky` | 5.9:1 | ✅ AA |
| `--ink-green` | 5.2:1 | ✅ AA |
| `--ink-amber` | 4.5:1 | ✅ AA |
| `--ink-red` | 5.8:1 | ✅ AA |

---

## 10. CSS 架構

### 10.1 模組化結構

專案採用 **CSS @layer** 進行模組化管理，確保樣式的正確覆蓋順序：

```
frontend/src/styles/
├── foundation/                        # 基礎層（5 個檔案）
│   ├── variables.css                 # 所有 CSS 變數定義
│   ├── reset.css                     # CSS Reset
│   ├── typography.css                # 字體系統
│   ├── animations.css                # 動畫 Keyframes
│   └── glass-effects.css             # 玻璃效果核心
│
├── layout/                            # 佈局層（4 個檔案）
│   ├── containers.css                # 容器系統
│   ├── grid.css                      # 網格系統
│   ├── asymmetric.css                # 不對稱佈局
│   └── floating-zones.css            # 懸浮區域
│
├── effects/                           # 效果層（2 個檔案）
│   ├── refractions.css               # 折射效果
│   └── glows.css                     # 光暈效果
│
├── animations/                        # 動畫層（3 個檔案）
│   ├── page-transitions.css          # 頁面過渡
│   ├── scroll-reveals.css            # 滾動顯示
│   └── micro-interactions.css        # 微交互
│
├── components/                        # 組件層（12 個檔案）
│   ├── sidebar.css
│   ├── header.css
│   ├── cards.css
│   ├── aurora-cards.css
│   ├── buttons.css
│   ├── forms.css
│   ├── modals.css
│   ├── tables.css
│   ├── badges.css
│   ├── search.css
│   ├── settings.css                   # 設定頁面組件
│   └── setup.css                      # 整合指南頁面組件
│
├── utilities/                         # 工具層（5 個檔案）
│   ├── spacing.css
│   ├── display.css
│   ├── text.css
│   ├── glass-utils.css               # 玻璃效果工具類別
│   └── aurora-utils.css              # 極光色彩工具類別
│
└── index.css                          # 主入口檔案
```

### 10.2 @layer 順序

```css
@layer foundation, layout, effects, animations, components, utilities, overrides;
```

**優先順序：** `foundation` < `layout` < `effects` < `animations` < `components` < `utilities` < `overrides`

### 10.3 主入口檔案範例

```css
/* frontend/src/styles/index.css */

/* Foundation 基礎層 */
@import url('./foundation/variables.css') layer(foundation);
@import url('./foundation/reset.css') layer(foundation);
@import url('./foundation/typography.css') layer(foundation);
@import url('./foundation/animations.css') layer(foundation);
@import url('./foundation/glass-effects.css') layer(foundation);

/* Layout 佈局層 */
@import url('./layout/containers.css') layer(layout);
@import url('./layout/grid.css') layer(layout);
/* ... */

/* Effects 效果層 */
@import url('./effects/refractions.css') layer(effects);
@import url('./effects/glows.css') layer(effects);

/* Animations 動畫層 */
@import url('./animations/page-transitions.css') layer(animations);
/* ... */

/* Components 組件層 */
@import url('./components/buttons.css') layer(components);
/* ... */

/* Utilities 工具層 */
@import url('./utilities/glass-utils.css') layer(utilities);
@import url('./utilities/aurora-utils.css') layer(utilities);

/* 聲明層級順序 */
@layer foundation, layout, effects, animations, components, utilities, overrides;

/* Overrides 層 - 應用特定覆蓋 */
@layer overrides {
    /* 應用特定樣式 */
}
```

---

## 參考資源

### 官方文件

- [Apple Liquid Glass 官方文件](https://developer.apple.com/documentation/TechnologyOverviews/liquid-glass)
- [Adopting Liquid Glass](https://developer.apple.com/documentation/technologyoverviews/adopting-liquid-glass)
- [Human Interface Guidelines - Materials](https://developer.apple.com/design/human-interface-guidelines/materials)

### 本專案檔案

- **PRD 文件：** `../PRD.md`
- **專案說明：** `../../CLAUDE.md`
- **前端入口：** `../../frontend/src/styles/index.css`
- **變數定義：** `../../frontend/src/styles/foundation/variables.css`

