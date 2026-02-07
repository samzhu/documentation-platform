package io.github.samzhu.documentation.mcp.repository;

import io.github.samzhu.documentation.mcp.domain.model.ApiKey;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

/**
 * API Key 資料存取（唯讀）
 * <p>
 * MCP Server 只讀取 api_keys 表進行認證驗證，
 * 金鑰的 CRUD 由 backend 負責。
 * findById(String id) 由 CrudRepository 提供。
 * </p>
 */
@Repository
public interface ApiKeyRepository extends CrudRepository<ApiKey, String> {
}
