package io.github.samzhu.documentation.platform.repository;

import io.github.samzhu.documentation.platform.domain.model.Document;
import org.springframework.data.jdbc.repository.query.Modifying;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 文件資料存取介面
 * <p>
 * 提供文件的 CRUD 操作及全文搜尋功能。
 * ID 類型為 TSID 字串。
 * </p>
 */
@Repository
public interface DocumentRepository extends CrudRepository<Document, String> {

    /**
     * 取得指定版本的所有文件
     *
     * @param versionId 版本 ID（TSID 格式）
     * @return 文件列表
     */
    @Query("SELECT * FROM documents WHERE version_id = :versionId")
    List<Document> findByVersionId(@Param("versionId") String versionId);

    /**
     * 取得指定版本的所有文件（依路徑排序）
     *
     * @param versionId 版本 ID（TSID 格式）
     * @return 依路徑排序的文件列表
     */
    @Query("SELECT * FROM documents WHERE version_id = :versionId ORDER BY path ASC")
    List<Document> findByVersionIdOrderByPathAsc(@Param("versionId") String versionId);

    /**
     * 根據版本 ID 和路徑查找文件
     *
     * @param versionId 版本 ID（TSID 格式）
     * @param path      文件路徑
     * @return 文件（若存在）
     */
    @Query("SELECT * FROM documents WHERE version_id = :versionId AND path = :path")
    Optional<Document> findByVersionIdAndPath(
            @Param("versionId") String versionId,
            @Param("path") String path
    );

    /**
     * 全文搜尋文件（動態條件）
     * <p>
     * 使用 PostgreSQL 的 tsvector 進行全文搜尋。
     * libraryId 和 versionId 為可選篩選條件，傳 null 表示不篩選。
     * </p>
     *
     * @param libraryId 函式庫 ID（可選，null 表示所有函式庫）
     * @param versionId 版本 ID（可選，null 表示所有版本）
     * @param query     搜尋關鍵字
     * @param limit     最大回傳筆數
     * @return 符合條件的文件列表（依相關性排序）
     */
    @Query("""
            SELECT d.* FROM documents d
            JOIN library_versions lv ON d.version_id = lv.id
            WHERE d.search_vector @@ plainto_tsquery('english', :query)
            AND (:libraryId IS NULL OR lv.library_id = :libraryId)
            AND (:versionId IS NULL OR d.version_id = :versionId)
            ORDER BY ts_rank(d.search_vector, plainto_tsquery('english', :query)) DESC
            LIMIT :limit
            """)
    List<Document> fullTextSearch(
            @Param("libraryId") String libraryId,
            @Param("versionId") String versionId,
            @Param("query") String query,
            @Param("limit") int limit
    );

    /**
     * 更新指定文件的全文搜尋向量
     * <p>
     * 使用 PostgreSQL to_tsvector 產生 tsvector，
     * 標題權重為 'A'，內容權重為 'B'。
     * </p>
     *
     * @param id      文件 ID（TSID 格式）
     * @param title   文件標題
     * @param content 文件內容
     */
    @Modifying
    @Query("""
            UPDATE documents SET search_vector =
                setweight(to_tsvector('english', COALESCE(:title, '')), 'A') ||
                setweight(to_tsvector('english', COALESCE(:content, '')), 'B')
            WHERE id = :id
            """)
    void updateSearchVector(@Param("id") String id,
                            @Param("title") String title,
                            @Param("content") String content);

    /**
     * 批次回填所有尚未建立搜尋向量的文件
     *
     * @return 更新的文件數量
     */
    @Modifying
    @Query("""
            UPDATE documents SET search_vector =
                setweight(to_tsvector('english', COALESCE(title, '')), 'A') ||
                setweight(to_tsvector('english', COALESCE(content, '')), 'B')
            WHERE search_vector IS NULL
            """)
    int backfillSearchVectors();

    /**
     * 統計指定 Library 的文件數量（透過 LibraryVersion JOIN）
     *
     * @param libraryId 函式庫 ID（TSID 格式）
     * @return 文件數量
     */
    @Query("""
            SELECT COUNT(d.id) FROM documents d
            JOIN library_versions lv ON d.version_id = lv.id
            WHERE lv.library_id = :libraryId
            """)
    long countByLibraryId(@Param("libraryId") String libraryId);
}
