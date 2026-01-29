import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';
import path from 'path';

/**
 * Vite 配置
 * - 開發模式：代理 API 請求到 Spring Boot 後端
 * - 建構模式：輸出到 Spring Boot 的 static 目錄
 */
export default defineConfig({
  plugins: [react()],

  // 開發伺服器配置
  server: {
    port: 5173,
    // 代理 API 請求到 Spring Boot 後端
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
      '/actuator': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
      // OAuth2 認證相關端點代理
      '/oauth2': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
      '/login/oauth2': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },

  // 建構配置
  build: {
    // 使用預設的 dist 目錄，手動複製到 Spring Boot static 目錄
    // 這樣可以保持乾淨的建構流程
    // 不產生 source map（生產環境）
    sourcemap: false,
    // 資源內聯閾值
    assetsInlineLimit: 4096,
    // Rollup 選項
    rollupOptions: {
      output: {
        // 資源檔案命名
        assetFileNames: 'assets/[name]-[hash][extname]',
        chunkFileNames: 'assets/[name]-[hash].js',
        entryFileNames: 'assets/[name]-[hash].js',
      },
    },
  },

  // 解析配置
  resolve: {
    alias: {
      '@': path.resolve(__dirname, 'src'),
    },
  },
});
