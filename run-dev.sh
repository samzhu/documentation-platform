#!/bin/bash
# 快速啟動開發環境腳本
# 執行：./run-dev.sh

set -e

echo "🔨 建構前端..."
cd frontend
npm run build

echo "📦 複製前端到後端 static 目錄..."
rm -rf ../backend/src/main/resources/static/*
cp -r dist/* ../backend/src/main/resources/static/

echo "🚀 啟動後端應用..."
cd ../backend
./gradlew bootRun --no-daemon
