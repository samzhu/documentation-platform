#!/bin/bash
# =============================================================================
# Liquibase Docker 指令包裝腳本
# =============================================================================
# 使用 Liquibase 5.0 Docker Official Image 執行資料庫版本管理
# PostgreSQL driver 透過 lpm 自動安裝
# =============================================================================

set -e

# ----- 預設參數（可透過環境變數覆蓋）-----
# DB_HOST 預設為 host.docker.internal（Docker Desktop 提供的特殊 DNS）
# 因為 Liquibase 跑在 Docker 容器內，localhost 指的是容器自己
# host.docker.internal 會指向 Mac host，再透過 port mapping 連到 pgvector 容器
DB_HOST="${DB_HOST:-host.docker.internal}"
DB_PORT="${DB_PORT:-5432}"
DB_NAME="${DB_NAME:-mydatabase}"
DB_USER="${DB_USER:-myuser}"
DB_PASS="${DB_PASS:-secret}"
CHANGELOG_DIR="${CHANGELOG_DIR:-$(pwd)/src/main/resources/db/changelog}"
LIQUIBASE_IMAGE="${LIQUIBASE_IMAGE:-liquibase:latest}"

# ----- JDBC URL -----
JDBC_URL="jdbc:postgresql://${DB_HOST}:${DB_PORT}/${DB_NAME}"

# ----- 共用 Docker 指令 -----
run_liquibase() {
  docker run --rm \
    -v "${CHANGELOG_DIR}:/liquibase/changelog" \
    --entrypoint bash \
    "${LIQUIBASE_IMAGE}" \
    -c "lpm add postgresql --global && liquibase --url=${JDBC_URL} --username=${DB_USER} --password=${DB_PASS} $*"
}

# ----- 指令說明 -----
show_help() {
  cat << 'EOF'
使用方式: ./liquibase.sh <command> [options]

Commands:
  generate    從現有資料庫產生 changelog（反向工程）
  update      套用尚未執行的 changeset
  status      顯示尚未執行的 changeset 清單
  validate    驗證 changelog 語法正確性
  rollback    回滾到指定 tag
  release     釋放被鎖住的 DATABASECHANGELOGLOCK
  diff        比較兩個資料庫的差異
  snapshot    快照目前資料庫狀態

環境變數:
  DB_HOST       資料庫主機（預設: host.docker.internal）
  DB_PORT       資料庫埠號（預設: 5432）
  DB_NAME       資料庫名稱（預設: mydatabase）
  DB_USER       使用者名稱（預設: myuser）
  DB_PASS       使用者密碼（預設: secret）
  CHANGELOG_DIR changelog 目錄（預設: src/main/resources/db/changelog）

範例:
  ./liquibase.sh generate
  ./liquibase.sh update
  ./liquibase.sh status
  ./liquibase.sh rollback v0.0.1
  ./liquibase.sh release
  DB_NAME=documentation DB_USER=documentation-platform ./liquibase.sh status
EOF
}

# ----- 主程式 -----
case "${1}" in
  generate)
    echo ">>> 從資料庫產生 changelog: ${JDBC_URL}"
    mkdir -p "${CHANGELOG_DIR}/raw"
    run_liquibase "generate-changelog --changelog-file=/liquibase/changelog/raw/full-generated.yaml"
    echo ">>> 已產生: ${CHANGELOG_DIR}/raw/full-generated.yaml"
    ;;
  update)
    echo ">>> 套用 changelog: ${JDBC_URL}"
    run_liquibase "update --changelog-file=/liquibase/changelog/db.changelog-master.yaml"
    ;;
  status)
    echo ">>> 檢查未執行的 changeset: ${JDBC_URL}"
    run_liquibase "status --changelog-file=/liquibase/changelog/db.changelog-master.yaml"
    ;;
  validate)
    echo ">>> 驗證 changelog 語法"
    run_liquibase "validate --changelog-file=/liquibase/changelog/db.changelog-master.yaml"
    ;;
  rollback)
    if [ -z "$2" ]; then
      echo "錯誤：請指定 tag，例如: ./liquibase.sh rollback v0.0.1"
      exit 1
    fi
    echo ">>> 回滾到 tag: $2"
    run_liquibase "rollback --changelog-file=/liquibase/changelog/db.changelog-master.yaml --tag=$2"
    ;;
  release)
    echo ">>> 釋放 DATABASECHANGELOGLOCK"
    run_liquibase "release-locks"
    ;;
  diff)
    echo ">>> 快照目前資料庫狀態"
    run_liquibase "diff"
    ;;
  snapshot)
    echo ">>> 快照目前資料庫狀態"
    run_liquibase "snapshot"
    ;;
  *)
    show_help
    ;;
esac
