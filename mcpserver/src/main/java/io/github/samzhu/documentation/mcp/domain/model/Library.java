package io.github.samzhu.documentation.mcp.domain.model;

import io.github.samzhu.documentation.mcp.domain.enums.SourceType;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;

/**
 * 函式庫唯讀實體
 * <p>
 * 代表一個可被索引的技術函式庫或框架，例如 Spring Boot、React 等。
 * MCP Server 只負責查詢，不負責 CRUD。
 * </p>
 */
@Table("libraries")
public class Library {

    /** 唯一識別碼（TSID 格式，13 字元） */
    @Id
    private final String id;

    /** 函式庫名稱（唯一，用於 API 查詢） */
    private final String name;

    /** 顯示名稱 */
    @Column("display_name")
    private final String displayName;

    /** 函式庫描述 */
    private final String description;

    /** 來源類型（GITHUB、LOCAL、MANUAL） */
    @Column("source_type")
    private final SourceType sourceType;

    /** 來源網址（如 GitHub repo URL） */
    @Column("source_url")
    private final String sourceUrl;

    /** 分類（如 backend、frontend） */
    private final String category;

    /** 標籤列表 */
    private final List<String> tags;

    /** 建立時間 */
    @Column("created_at")
    private final OffsetDateTime createdAt;

    /** 更新時間 */
    @Column("updated_at")
    private final OffsetDateTime updatedAt;

    public Library(String id, String name, String displayName, String description,
                   SourceType sourceType, String sourceUrl, String category,
                   List<String> tags, OffsetDateTime createdAt, OffsetDateTime updatedAt) {
        this.id = id;
        this.name = name;
        this.displayName = displayName;
        this.description = description;
        this.sourceType = sourceType;
        this.sourceUrl = sourceUrl;
        this.category = category;
        this.tags = tags;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getDisplayName() { return displayName; }
    public String getDescription() { return description; }
    public SourceType getSourceType() { return sourceType; }
    public String getSourceUrl() { return sourceUrl; }
    public String getCategory() { return category; }
    public List<String> getTags() { return tags; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Library library = (Library) o;
        return Objects.equals(id, library.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Library{id='%s', name='%s', displayName='%s', category='%s'}"
                .formatted(id, name, displayName, category);
    }
}
