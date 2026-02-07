package io.github.samzhu.documentation.mcp.domain.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Objects;

/**
 * 文件唯讀實體
 * <p>
 * 儲存原始文件內容與元資料。每份文件屬於特定的函式庫版本。
 * MCP Server 只負責查詢，不負責 CRUD。
 * </p>
 */
@Table("documents")
public class Document {

    /** 唯一識別碼（TSID 格式，13 字元） */
    @Id
    private final String id;

    /** 所屬版本 ID（TSID 格式） */
    @Column("version_id")
    private final String versionId;

    /** 文件標題 */
    private final String title;

    /** 文件路徑 */
    private final String path;

    /** 文件內容 */
    private final String content;

    /** 內容雜湊值（用於偵測變更） */
    @Column("content_hash")
    private final String contentHash;

    /** 文件類型（如 markdown、html） */
    @Column("doc_type")
    private final String docType;

    /** 額外的元資料 */
    private final Map<String, Object> metadata;

    /** 建立時間 */
    @Column("created_at")
    private final OffsetDateTime createdAt;

    /** 更新時間 */
    @Column("updated_at")
    private final OffsetDateTime updatedAt;

    public Document(String id, String versionId, String title, String path,
                    String content, String contentHash, String docType,
                    Map<String, Object> metadata, OffsetDateTime createdAt, OffsetDateTime updatedAt) {
        this.id = id;
        this.versionId = versionId;
        this.title = title;
        this.path = path;
        this.content = content;
        this.contentHash = contentHash;
        this.docType = docType;
        this.metadata = metadata;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public String getId() { return id; }
    public String getVersionId() { return versionId; }
    public String getTitle() { return title; }
    public String getPath() { return path; }
    public String getContent() { return content; }
    public String getContentHash() { return contentHash; }
    public String getDocType() { return docType; }
    public Map<String, Object> getMetadata() { return metadata; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Document document = (Document) o;
        return Objects.equals(id, document.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Document{id='%s', versionId='%s', title='%s', path='%s', docType='%s'}"
                .formatted(id, versionId, title, path, docType);
    }
}
