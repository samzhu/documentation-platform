package io.github.samzhu.documentation.mcp.repository;

import io.github.samzhu.documentation.mcp.domain.model.CodeExample;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 程式碼範例唯讀資料存取介面
 * <p>
 * 提供 MCP Tools 所需的程式碼範例查詢方法。
 * </p>
 */
@Repository
public interface CodeExampleRepository extends CrudRepository<CodeExample, String> {

    /**
     * 取得指定文件的所有程式碼範例
     *
     * @param documentId 文件 ID（TSID 格式）
     * @return 程式碼範例列表（依起始行號排序）
     */
    @Query("SELECT * FROM code_examples WHERE document_id = :documentId ORDER BY start_line")
    List<CodeExample> findByDocumentId(@Param("documentId") String documentId);
}
