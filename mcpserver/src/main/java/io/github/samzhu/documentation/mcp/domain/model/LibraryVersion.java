package io.github.samzhu.documentation.mcp.domain.model;

import io.github.samzhu.documentation.mcp.domain.enums.VersionStatus;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Objects;

/**
 * 函式庫版本唯讀實體
 * <p>
 * 每個函式庫可以有多個版本，文件會關聯到特定版本。
 * MCP Server 只負責查詢，不負責 CRUD。
 * </p>
 */
@Table("library_versions")
public class LibraryVersion {

    /** 唯一識別碼（TSID 格式，13 字元） */
    @Id
    private final String id;

    /** 所屬函式庫 ID（TSID 格式） */
    @Column("library_id")
    private final String libraryId;

    /** 版本號（如 3.2.0） */
    private final String version;

    /** 是否為最新版本 */
    @Column("is_latest")
    private final Boolean isLatest;

    /** 是否為 LTS（Long-Term Support）版本 */
    @Column("is_lts")
    private final Boolean isLts;

    /** 版本狀態 */
    private final VersionStatus status;

    /** 文件路徑 */
    @Column("docs_path")
    private final String docsPath;

    /** 發布日期 */
    @Column("release_date")
    private final LocalDate releaseDate;

    /** 建立時間 */
    @Column("created_at")
    private final OffsetDateTime createdAt;

    /** 更新時間 */
    @Column("updated_at")
    private final OffsetDateTime updatedAt;

    public LibraryVersion(String id, String libraryId, String version, Boolean isLatest,
                          Boolean isLts, VersionStatus status, String docsPath,
                          LocalDate releaseDate, OffsetDateTime createdAt, OffsetDateTime updatedAt) {
        this.id = id;
        this.libraryId = libraryId;
        this.version = version;
        this.isLatest = isLatest;
        this.isLts = isLts;
        this.status = status;
        this.docsPath = docsPath;
        this.releaseDate = releaseDate;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public String getId() { return id; }
    public String getLibraryId() { return libraryId; }
    public String getVersion() { return version; }
    public Boolean getIsLatest() { return isLatest; }
    public Boolean getIsLts() { return isLts; }
    public VersionStatus getStatus() { return status; }
    public String getDocsPath() { return docsPath; }
    public LocalDate getReleaseDate() { return releaseDate; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LibraryVersion that = (LibraryVersion) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "LibraryVersion{id='%s', libraryId='%s', version='%s', isLatest=%s, status=%s}"
                .formatted(id, libraryId, version, isLatest, status);
    }
}
