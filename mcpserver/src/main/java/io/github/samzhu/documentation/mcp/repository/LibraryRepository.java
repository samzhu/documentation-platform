package io.github.samzhu.documentation.mcp.repository;

import io.github.samzhu.documentation.mcp.domain.model.Library;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 函式庫唯讀資料存取介面
 * <p>
 * 提供 MCP Tools 所需的函式庫查詢方法。
 * </p>
 */
@Repository
public interface LibraryRepository extends CrudRepository<Library, String> {

    /**
     * 根據名稱查找函式庫
     *
     * @param name 函式庫名稱
     * @return 函式庫（若存在）
     */
    Optional<Library> findByName(String name);

    /**
     * 根據分類查找函式庫列表
     *
     * @param category 分類名稱
     * @return 符合分類的函式庫列表
     */
    List<Library> findByCategory(String category);

    /**
     * 取得所有函式庫列表
     *
     * @return 所有函式庫
     */
    @Override
    List<Library> findAll();
}
