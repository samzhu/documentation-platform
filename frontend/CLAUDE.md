# Frontend CLAUDE.md

前端專案 AI 輔助開發指引。

## 技術堆疊

| 項目 | 版本 |
|------|------|
| React | 19.0.0 |
| Vite | 6.0.3 |
| React Router | 7.1.3 |

## 開發指令

```bash
npm install        # 安裝依賴
npm run dev        # 開發模式（port 5173，API 代理到 8080）
npm run build      # 建構 → 輸出到 backend/src/main/resources/static
```

## 設計系統

**Apple Liquid Glass + 極光折射設計系統**

- 詳細規範：`docs/DESIGN_LANGUAGE.md`
- Sidebar 使用 Liquid Glass，內容區使用實心背景
- Dark Mode 為預設

## CSS 架構

```
@layer foundation → layout → effects → animations → components → utilities
```

**目錄結構：**
```
src/styles/
├── foundation/    # 變數、重設、字體、玻璃效果
├── layout/        # 容器、網格
├── effects/       # 折射、光暈
├── animations/    # 頁面過渡、微交互
├── components/    # 按鈕、表單、卡片
├── pages/         # 頁面專屬樣式
└── utilities/     # 間距、顯示、文字
```

## 專案結構

```
frontend/
├── CLAUDE.md          # 本檔案
├── docs/
│   ├── README.md      # 技術文件索引
│   └── DESIGN_LANGUAGE.md
├── src/
│   ├── components/    # 共用組件（6 個）
│   ├── pages/         # 頁面組件（13 個）
│   ├── hooks/         # useAuth
│   ├── services/      # api.js, auth.js
│   └── styles/        # CSS 模組
├── package.json
└── vite.config.js
```

## 開發規範

1. **樣式**：遵循 @layer 層級，使用 DESIGN_LANGUAGE.md 的變數
2. **API**：使用 `services/api.js` 統一處理
3. **認證**：使用 `useAuth` Hook
4. **路由保護**：使用 `RequireAuth` 包裝
5. **組件大小**：保持 < 300 行
