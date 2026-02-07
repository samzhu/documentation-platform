package io.github.samzhu.documentation.mcp.repository;

import io.github.samzhu.documentation.mcp.domain.model.LibraryVersion;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 函式庫版本唯讀資料存取介面
 * <p>
 * 提供 MCP Tools 所需的版本查詢方法。
 * </p>
 */
@Repository
public interface LibraryVersionRepository extends CrudRepository<LibraryVersion, String> {

    /**
     * 查找指定函式庫的最新版本
     *
     * @param libraryId 函式庫 ID（TSID 格式）
     * @return 最新版本（若存在）
     */
    @Query("SELECT * FROM library_versions WHERE library_id = :libraryId AND is_latest = true")
    Optional<LibraryVersion> findLatestByLibraryId(@Param("libraryId") String libraryId);

    /**
     * 查找指定函式庫的特定版本
     *
     * @param libraryId 函式庫 ID（TSID 格式）
     * @param version   版本號
     * @return 版本資訊（若存在）
     */
    @Query("SELECT * FROM library_versions WHERE library_id = :libraryId AND version = :version")
    Optional<LibraryVersion> findByLibraryIdAndVersion(
            @Param("libraryId") String libraryId,
            @Param("version") String version
    );

    /**
     * 取得指定函式庫的所有版本
     *
     * @param libraryId 函式庫 ID（TSID 格式）
     * @return 版本列表（依建立時間降序排列）
     */
    @Query("SELECT * FROM library_versions WHERE library_id = :libraryId ORDER BY created_at DESC")
    List<LibraryVersion> findByLibraryId(@Param("libraryId") String libraryId);
}
