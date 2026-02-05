-- DocMCP Server 資料庫 Schema
-- 此檔案會在應用程式啟動時自動執行

-- 啟用必要的 PostgreSQL 擴充功能
CREATE EXTENSION IF NOT EXISTS vector;

-- 建立 libraries 表（函式庫主表）
-- 使用 VARCHAR(13) 儲存 TSID（時間排序唯一識別碼）
CREATE TABLE IF NOT EXISTS libraries (
    id VARCHAR(13) PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    display_name VARCHAR(200) NOT NULL,
    description TEXT,
    source_type VARCHAR(20) NOT NULL,
    source_url VARCHAR(500),
    category VARCHAR(50),
    tags TEXT[],
    version BIGINT DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE libraries IS '儲存函式庫/框架的基本資訊';
COMMENT ON COLUMN libraries.id IS 'TSID 格式（13 字元 Crockford Base32）';
COMMENT ON COLUMN libraries.name IS '函式庫唯一名稱，用於程式識別（如 spring-boot）';
COMMENT ON COLUMN libraries.display_name IS '函式庫顯示名稱，用於前端呈現（如 Spring Boot）';
COMMENT ON COLUMN libraries.description IS '函式庫描述說明';
COMMENT ON COLUMN libraries.source_type IS '來源類型: GITHUB, LOCAL, MANUAL';
COMMENT ON COLUMN libraries.source_url IS '來源網址（如 GitHub Repository URL）';
COMMENT ON COLUMN libraries.category IS '分類標籤（如 backend, frontend, devops）';
COMMENT ON COLUMN libraries.tags IS '標籤陣列，用於多維度分類與篩選';
COMMENT ON COLUMN libraries.version IS '樂觀鎖版本號，用於併發控制';
COMMENT ON COLUMN libraries.created_at IS '資料建立時間';
COMMENT ON COLUMN libraries.updated_at IS '資料最後更新時間';

-- 建立 library_versions 表（版本資訊表）
CREATE TABLE IF NOT EXISTS library_versions (
    id VARCHAR(13) PRIMARY KEY,
    library_id VARCHAR(13) NOT NULL REFERENCES libraries(id) ON DELETE CASCADE,
    version VARCHAR(50) NOT NULL,
    is_latest BOOLEAN DEFAULT FALSE,
    is_lts BOOLEAN DEFAULT FALSE,
    status VARCHAR(20) DEFAULT 'ACTIVE',
    docs_path VARCHAR(500),
    release_date DATE,
    entity_version BIGINT DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(library_id, version)
);

COMMENT ON TABLE library_versions IS '儲存每個函式庫的版本資訊';
COMMENT ON COLUMN library_versions.id IS 'TSID 格式（13 字元 Crockford Base32）';
COMMENT ON COLUMN library_versions.library_id IS '所屬函式庫 ID（外鍵關聯 libraries）';
COMMENT ON COLUMN library_versions.version IS '版本號（如 3.2.0、4.0.2）';
COMMENT ON COLUMN library_versions.is_latest IS '是否為最新版本';
COMMENT ON COLUMN library_versions.is_lts IS '是否為長期支援（Long-Term Support）版本';
COMMENT ON COLUMN library_versions.status IS '版本狀態: ACTIVE, DEPRECATED, EOL';
COMMENT ON COLUMN library_versions.docs_path IS '文件檔案存放路徑';
COMMENT ON COLUMN library_versions.release_date IS '版本發佈日期';
COMMENT ON COLUMN library_versions.entity_version IS '樂觀鎖版本號，用於併發控制';
COMMENT ON COLUMN library_versions.created_at IS '資料建立時間';
COMMENT ON COLUMN library_versions.updated_at IS '資料最後更新時間';

-- 建立 documents 表（文件表）
CREATE TABLE IF NOT EXISTS documents (
    id VARCHAR(13) PRIMARY KEY,
    version_id VARCHAR(13) NOT NULL REFERENCES library_versions(id) ON DELETE CASCADE,
    title VARCHAR(500) NOT NULL,
    path VARCHAR(1000) NOT NULL,
    content TEXT,
    content_hash VARCHAR(64),
    doc_type VARCHAR(50),
    metadata JSONB DEFAULT '{}',
    search_vector TSVECTOR,
    version BIGINT DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(version_id, path)
);

COMMENT ON TABLE documents IS '儲存文件內容和元資料';
COMMENT ON COLUMN documents.id IS 'TSID 格式（13 字元 Crockford Base32）';
COMMENT ON COLUMN documents.version_id IS '所屬函式庫版本 ID（外鍵關聯 library_versions）';
COMMENT ON COLUMN documents.title IS '文件標題';
COMMENT ON COLUMN documents.path IS '文件檔案路徑（相對於文件根目錄）';
COMMENT ON COLUMN documents.content IS '文件完整內容';
COMMENT ON COLUMN documents.content_hash IS '內容雜湊值（SHA-256），用於偵測文件變更';
COMMENT ON COLUMN documents.doc_type IS '文件類型（如 markdown, html, text）';
COMMENT ON COLUMN documents.metadata IS '額外元資料（JSONB 格式）';
COMMENT ON COLUMN documents.search_vector IS '全文檢索向量（tsvector），由標題與內容組合產生';
COMMENT ON COLUMN documents.version IS '樂觀鎖版本號，用於併發控制';
COMMENT ON COLUMN documents.created_at IS '資料建立時間';
COMMENT ON COLUMN documents.updated_at IS '資料最後更新時間';

-- 建立 document_chunks 表（文件區塊表，含向量嵌入）
CREATE TABLE IF NOT EXISTS document_chunks (
    id VARCHAR(13) PRIMARY KEY,
    document_id VARCHAR(13) NOT NULL REFERENCES documents(id) ON DELETE CASCADE,
    chunk_index INTEGER NOT NULL,
    content TEXT NOT NULL,
    embedding vector(768),
    token_count INTEGER,
    metadata JSONB DEFAULT '{}',
    version BIGINT DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(document_id, chunk_index)
);

COMMENT ON TABLE document_chunks IS '儲存分塊的文件內容與向量嵌入';
COMMENT ON COLUMN document_chunks.id IS 'TSID 格式（13 字元 Crockford Base32）';
COMMENT ON COLUMN document_chunks.document_id IS '所屬文件 ID（外鍵關聯 documents）';
COMMENT ON COLUMN document_chunks.chunk_index IS '區塊索引，從 0 開始，表示在原文件中的順序';
COMMENT ON COLUMN document_chunks.content IS '區塊文字內容（典型大小 500-1000 tokens）';
COMMENT ON COLUMN document_chunks.embedding IS '768 維度向量嵌入，用於語意搜尋（gemini-embedding-001）';
COMMENT ON COLUMN document_chunks.token_count IS '此區塊的 token 數量';
COMMENT ON COLUMN document_chunks.metadata IS '額外元資料（JSONB 格式，含 versionId 等篩選欄位）';
COMMENT ON COLUMN document_chunks.version IS '樂觀鎖版本號，用於併發控制';
COMMENT ON COLUMN document_chunks.created_at IS '資料建立時間';
COMMENT ON COLUMN document_chunks.updated_at IS '資料最後更新時間';

-- 建立 code_examples 表（程式碼範例表）
CREATE TABLE IF NOT EXISTS code_examples (
    id VARCHAR(13) PRIMARY KEY,
    document_id VARCHAR(13) NOT NULL REFERENCES documents(id) ON DELETE CASCADE,
    language VARCHAR(50) NOT NULL,
    code TEXT NOT NULL,
    description TEXT,
    start_line INTEGER,
    end_line INTEGER,
    metadata JSONB DEFAULT '{}',
    version BIGINT DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE code_examples IS '儲存從文件中擷取的程式碼範例';
COMMENT ON COLUMN code_examples.id IS 'TSID 格式（13 字元 Crockford Base32）';
COMMENT ON COLUMN code_examples.document_id IS '所屬文件 ID（外鍵關聯 documents）';
COMMENT ON COLUMN code_examples.language IS '程式語言（如 java, javascript, python）';
COMMENT ON COLUMN code_examples.code IS '程式碼內容';
COMMENT ON COLUMN code_examples.description IS '程式碼範例的說明描述';
COMMENT ON COLUMN code_examples.start_line IS '在原始文件中的起始行號';
COMMENT ON COLUMN code_examples.end_line IS '在原始文件中的結束行號';
COMMENT ON COLUMN code_examples.metadata IS '額外元資料（JSONB 格式）';
COMMENT ON COLUMN code_examples.version IS '樂觀鎖版本號，用於併發控制';
COMMENT ON COLUMN code_examples.created_at IS '資料建立時間';
COMMENT ON COLUMN code_examples.updated_at IS '資料最後更新時間';

-- 建立 sync_history 表（同步歷史記錄表）
CREATE TABLE IF NOT EXISTS sync_history (
    id VARCHAR(13) PRIMARY KEY,
    version_id VARCHAR(13) NOT NULL REFERENCES library_versions(id) ON DELETE CASCADE,
    status VARCHAR(20) NOT NULL,
    started_at TIMESTAMP WITH TIME ZONE NOT NULL,
    completed_at TIMESTAMP WITH TIME ZONE,
    documents_processed INTEGER DEFAULT 0,
    chunks_created INTEGER DEFAULT 0,
    error_message TEXT,
    metadata JSONB DEFAULT '{}',
    version BIGINT DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE sync_history IS '追蹤文件同步歷史記錄';
COMMENT ON COLUMN sync_history.id IS 'TSID 格式（13 字元 Crockford Base32）';
COMMENT ON COLUMN sync_history.version_id IS '同步的目標函式庫版本 ID（外鍵關聯 library_versions）';
COMMENT ON COLUMN sync_history.status IS '同步狀態: PENDING, RUNNING, SUCCESS, FAILED';
COMMENT ON COLUMN sync_history.started_at IS '同步作業開始時間';
COMMENT ON COLUMN sync_history.completed_at IS '同步作業完成時間（進行中為 NULL）';
COMMENT ON COLUMN sync_history.documents_processed IS '已處理的文件數量';
COMMENT ON COLUMN sync_history.chunks_created IS '已建立的文件區塊數量';
COMMENT ON COLUMN sync_history.error_message IS '同步失敗時的錯誤訊息';
COMMENT ON COLUMN sync_history.metadata IS '額外元資料（JSONB 格式）';
COMMENT ON COLUMN sync_history.version IS '樂觀鎖版本號，用於併發控制';
COMMENT ON COLUMN sync_history.created_at IS '資料建立時間';
COMMENT ON COLUMN sync_history.updated_at IS '資料最後更新時間';

-- 建立 api_keys 表（API 金鑰表）
CREATE TABLE IF NOT EXISTS api_keys (
    id VARCHAR(13) PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    key_hash VARCHAR(255) NOT NULL,
    key_prefix VARCHAR(20) NOT NULL UNIQUE,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    rate_limit INTEGER DEFAULT 1000,
    expires_at TIMESTAMP WITH TIME ZONE,
    last_used_at TIMESTAMP WITH TIME ZONE,
    version BIGINT DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100)
);

COMMENT ON TABLE api_keys IS '儲存 API 認證金鑰';
COMMENT ON COLUMN api_keys.id IS 'TSID 格式（13 字元 Crockford Base32）';
COMMENT ON COLUMN api_keys.name IS '金鑰識別名稱（唯一，用於辨識用途）';
COMMENT ON COLUMN api_keys.key_hash IS 'BCrypt 雜湊後的金鑰（永不儲存原始金鑰）';
COMMENT ON COLUMN api_keys.key_prefix IS '金鑰前綴（前 12 字元，如 dmcp_a1b2），用於快速查詢識別';
COMMENT ON COLUMN api_keys.status IS '金鑰狀態: ACTIVE, REVOKED, EXPIRED';
COMMENT ON COLUMN api_keys.rate_limit IS '每小時請求上限（預設 1000）';
COMMENT ON COLUMN api_keys.expires_at IS '金鑰到期時間（NULL 表示永不過期）';
COMMENT ON COLUMN api_keys.last_used_at IS '金鑰最後使用時間';
COMMENT ON COLUMN api_keys.version IS '樂觀鎖版本號，用於併發控制';
COMMENT ON COLUMN api_keys.created_at IS '資料建立時間';
COMMENT ON COLUMN api_keys.updated_at IS '資料最後更新時間';
COMMENT ON COLUMN api_keys.created_by IS '建立者識別資訊';

-- 建立索引以優化查詢效能

-- Libraries 索引
CREATE INDEX IF NOT EXISTS idx_libraries_name ON libraries(name);
CREATE INDEX IF NOT EXISTS idx_libraries_category ON libraries(category);
CREATE INDEX IF NOT EXISTS idx_libraries_source_type ON libraries(source_type);

-- Library versions 索引
CREATE INDEX IF NOT EXISTS idx_library_versions_library_id ON library_versions(library_id);
CREATE INDEX IF NOT EXISTS idx_library_versions_is_latest ON library_versions(is_latest) WHERE is_latest = TRUE;
CREATE INDEX IF NOT EXISTS idx_library_versions_is_lts ON library_versions(is_lts) WHERE is_lts = TRUE;
CREATE INDEX IF NOT EXISTS idx_library_versions_status ON library_versions(status);

-- Documents 索引
CREATE INDEX IF NOT EXISTS idx_documents_version_id ON documents(version_id);
CREATE INDEX IF NOT EXISTS idx_documents_doc_type ON documents(doc_type);
CREATE INDEX IF NOT EXISTS idx_documents_search_vector ON documents USING GIN(search_vector);

-- Document chunks 索引
CREATE INDEX IF NOT EXISTS idx_document_chunks_document_id ON document_chunks(document_id);

-- Document chunks metadata 索引（支援 VectorStore filter 機制）
CREATE INDEX IF NOT EXISTS idx_document_chunks_metadata ON document_chunks USING GIN(metadata);
CREATE INDEX IF NOT EXISTS idx_document_chunks_version_id ON document_chunks ((metadata->>'versionId'));

-- Code examples 索引
CREATE INDEX IF NOT EXISTS idx_code_examples_document_id ON code_examples(document_id);
CREATE INDEX IF NOT EXISTS idx_code_examples_language ON code_examples(language);

-- Sync history 索引
CREATE INDEX IF NOT EXISTS idx_sync_history_version_id ON sync_history(version_id);
CREATE INDEX IF NOT EXISTS idx_sync_history_status ON sync_history(status);

-- API keys 索引
CREATE INDEX IF NOT EXISTS idx_api_keys_key_prefix ON api_keys(key_prefix);
CREATE INDEX IF NOT EXISTS idx_api_keys_status ON api_keys(status);

-- 注意：tsvector 觸發器需要在資料庫層面另外設定
-- 因為 Spring SQL 初始化不支援 PostgreSQL 的 $$ 引用語法
-- 可透過 psql 手動執行以下 SQL：
--
-- CREATE OR REPLACE FUNCTION update_documents_search_vector()
-- RETURNS TRIGGER AS $$
-- BEGIN
--     NEW.search_vector :=
--         setweight(to_tsvector('english', COALESCE(NEW.title, '')), 'A') ||
--         setweight(to_tsvector('english', COALESCE(NEW.content, '')), 'B');
--     RETURN NEW;
-- END;
-- $$ LANGUAGE plpgsql;
--
-- CREATE TRIGGER trigger_documents_search_vector
--     BEFORE INSERT OR UPDATE OF title, content
--     ON documents
--     FOR EACH ROW
--     EXECUTE FUNCTION update_documents_search_vector();
