package io.github.samzhu.documentation.mcp.domain.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Objects;

/**
 * 程式碼範例唯讀實體
 * <p>
 * 從文件中萃取的程式碼範例，包含語言類型、程式碼內容及說明。
 * MCP Server 只負責查詢，不負責 CRUD。
 * </p>
 */
@Table("code_examples")
public class CodeExample {

    /** 唯一識別碼（TSID 格式，13 字元） */
    @Id
    private final String id;

    /** 所屬文件 ID（TSID 格式） */
    @Column("document_id")
    private final String documentId;

    /** 程式語言（如 java、javascript） */
    private final String language;

    /** 程式碼內容 */
    private final String code;

    /** 程式碼說明 */
    private final String description;

    /** 起始行號 */
    @Column("start_line")
    private final Integer startLine;

    /** 結束行號 */
    @Column("end_line")
    private final Integer endLine;

    /** 額外的元資料 */
    private final Map<String, Object> metadata;

    /** 建立時間 */
    @Column("created_at")
    private final OffsetDateTime createdAt;

    /** 更新時間 */
    @Column("updated_at")
    private final OffsetDateTime updatedAt;

    public CodeExample(String id, String documentId, String language, String code,
                       String description, Integer startLine, Integer endLine,
                       Map<String, Object> metadata, OffsetDateTime createdAt, OffsetDateTime updatedAt) {
        this.id = id;
        this.documentId = documentId;
        this.language = language;
        this.code = code;
        this.description = description;
        this.startLine = startLine;
        this.endLine = endLine;
        this.metadata = metadata;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public String getId() { return id; }
    public String getDocumentId() { return documentId; }
    public String getLanguage() { return language; }
    public String getCode() { return code; }
    public String getDescription() { return description; }
    public Integer getStartLine() { return startLine; }
    public Integer getEndLine() { return endLine; }
    public Map<String, Object> getMetadata() { return metadata; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CodeExample that = (CodeExample) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        // 排除 code 避免輸出過長
        return "CodeExample{id='%s', documentId='%s', language='%s', description='%s'}"
                .formatted(id, documentId, language, description);
    }
}
