# 前端技術文件索引

## 技術堆疊

| 項目 | 版本 | 說明 |
|------|------|------|
| **React** | 19.0.0 | UI 框架 |
| **Vite** | 6.0.3 | 建構工具 |
| **React Router** | 7.1.3 | 路由管理 |
| **設計風格** | Apple Liquid Glass | Sidebar 使用 Liquid Glass，內容區使用實心白色 |

## 文件清單

| 文件 | 說明 |
|------|------|
| [DESIGN_LANGUAGE.md](./DESIGN_LANGUAGE.md) | 設計語言規範 |

## 快速指令

### 開發模式

```bash
cd frontend
npm install
npm run dev
```

訪問 `http://localhost:5173`（API 自動代理到後端 8080）

### 建構部署

```bash
cd frontend
npm run build
```

輸出到 `backend/src/main/resources/static/`

## 專案結構

```
frontend/
├── src/
│   ├── components/          # 共用組件
│   │   ├── Button.jsx       # 按鈕組件
│   │   ├── Button.examples.jsx  # 按鈕範例
│   │   ├── Header.jsx       # 頁面標題組件
│   │   ├── Icons.jsx        # 圖示組件
│   │   ├── RequireAuth.jsx  # 路由認證保護
│   │   └── Sidebar.jsx      # 側邊欄導航
│   ├── pages/               # 頁面組件（13 個）
│   │   ├── Dashboard.jsx    # 儀表板
│   │   ├── Libraries.jsx    # 文件庫列表
│   │   ├── LibraryDetail.jsx    # 文件庫詳情
│   │   ├── LibraryForm.jsx  # 文件庫表單
│   │   ├── DocumentList.jsx # 文件列表
│   │   ├── DocumentDetail.jsx   # 文件詳情
│   │   ├── SyncHistory.jsx  # 同步歷史
│   │   ├── SyncDetail.jsx   # 同步詳情
│   │   ├── ApiKeys.jsx      # API Key 管理
│   │   ├── Settings.jsx     # 系統設定
│   │   ├── Setup.jsx        # 初始設定
│   │   ├── Search.jsx       # 搜尋頁面
│   │   └── Callback.jsx     # OAuth 回調
│   ├── hooks/               # Custom Hooks
│   │   └── useAuth.js       # 認證狀態管理
│   ├── services/            # API 與認證服務
│   │   ├── api.js           # API 呼叫封裝
│   │   └── auth.js          # 認證服務
│   ├── styles/              # CSS 樣式（模組化）
│   │   ├── index.css        # 主樣式入口
│   │   ├── foundation/      # 基礎層（變數、重設、字體、動畫、玻璃效果）
│   │   ├── layout/          # 佈局層（容器、網格）
│   │   ├── effects/         # 效果層（折射、光暈）
│   │   ├── animations/      # 動畫層（頁面過渡、滾動、微交互）
│   │   ├── components/      # 組件層（按鈕、表單、卡片等）
│   │   ├── pages/           # 頁面層（專屬樣式）
│   │   └── utilities/       # 工具層（間距、顯示、文字）
│   ├── App.jsx              # 根組件
│   └── main.jsx             # 應用入口
├── package.json
├── vite.config.js           # Vite 配置（輸出到 static）
└── index.html
```

## 開發注意事項

1. **樣式開發**：遵循 @layer 層級結構（foundation → layout → effects → animations → components → utilities）
2. **API 呼叫**：使用 `services/api.js` 統一處理
3. **認證處理**：使用 `useAuth` Hook 管理登入狀態
4. **路由保護**：使用 `RequireAuth` 包裝需要認證的頁面
5. **建構流程**：開發時使用 Vite dev server，部署前執行 `npm run build`
6. **設計系統**：採用 Apple Liquid Glass + 極光折射設計語言，詳見 DESIGN_LANGUAGE.md

## 相關連結

- [專案 PRD](../../docs/PRD.md)
- [設計語言規範](./DESIGN_LANGUAGE.md)
