package io.github.samzhu.documentation.mcp.service;

import io.github.samzhu.documentation.mcp.domain.model.CodeExample;
import io.github.samzhu.documentation.mcp.domain.model.Document;
import io.github.samzhu.documentation.mcp.domain.model.Library;
import io.github.samzhu.documentation.mcp.domain.model.LibraryVersion;
import io.github.samzhu.documentation.mcp.repository.CodeExampleRepository;
import io.github.samzhu.documentation.mcp.repository.DocumentRepository;
import io.github.samzhu.documentation.mcp.repository.LibraryRepository;
import io.github.samzhu.documentation.mcp.repository.LibraryVersionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * 函式庫查詢服務
 * <p>
 * 封裝「libraryName → libraryId → versionId」的通用解析邏輯，
 * 提供 MCP Tools 統一的查詢入口。
 * </p>
 */
@Service
@Transactional(readOnly = true)
public class LibraryQueryService {

    private static final Logger log = LoggerFactory.getLogger(LibraryQueryService.class);

    private final LibraryRepository libraryRepository;
    private final LibraryVersionRepository versionRepository;
    private final DocumentRepository documentRepository;
    private final CodeExampleRepository codeExampleRepository;

    public LibraryQueryService(LibraryRepository libraryRepository,
                               LibraryVersionRepository versionRepository,
                               DocumentRepository documentRepository,
                               CodeExampleRepository codeExampleRepository) {
        this.libraryRepository = libraryRepository;
        this.versionRepository = versionRepository;
        this.documentRepository = documentRepository;
        this.codeExampleRepository = codeExampleRepository;
    }

    /**
     * 根據名稱查找函式庫
     *
     * @param name 函式庫名稱
     * @return 函式庫
     * @throws IllegalArgumentException 找不到時拋出
     */
    public Library findLibraryByName(String name) {
        return libraryRepository.findByName(name)
                .orElseThrow(() -> new IllegalArgumentException("找不到函式庫: " + name));
    }

    /**
     * 取得所有函式庫，可選擇按分類篩選
     *
     * @param category 分類（null 表示全部）
     * @return 函式庫列表
     */
    public List<Library> findAllLibraries(String category) {
        if (category != null && !category.isBlank()) {
            return libraryRepository.findByCategory(category);
        }
        return libraryRepository.findAll();
    }

    /**
     * 取得指定函式庫的所有版本
     *
     * @param libraryName 函式庫名稱
     * @return 版本列表
     */
    public List<LibraryVersion> findVersionsByLibraryName(String libraryName) {
        Library library = findLibraryByName(libraryName);
        return versionRepository.findByLibraryId(library.getId());
    }

    /**
     * 解析版本 ID
     * <p>
     * 若 version 為 null，取最新版本；否則取指定版本。
     * </p>
     *
     * @param libraryId 函式庫 ID
     * @param version   版本號（可為 null）
     * @return 版本 ID
     * @throws IllegalArgumentException 找不到版本時拋出
     */
    public String resolveVersionId(String libraryId, String version) {
        Optional<LibraryVersion> versionOpt;
        if (version != null && !version.isBlank()) {
            versionOpt = versionRepository.findByLibraryIdAndVersion(libraryId, version);
        } else {
            versionOpt = versionRepository.findLatestByLibraryId(libraryId);
        }
        return versionOpt
                .map(LibraryVersion::getId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "找不到版本: libraryId=%s, version=%s".formatted(libraryId, version)));
    }

    /**
     * 取得指定函式庫版本下的文件列表
     *
     * @param libraryName 函式庫名稱
     * @param version     版本號（null 表示最新版）
     * @return 文件列表
     */
    public List<Document> findDocuments(String libraryName, String version) {
        Library library = findLibraryByName(libraryName);
        String versionId = resolveVersionId(library.getId(), version);
        return documentRepository.findByVersionIdOrderByPathAsc(versionId);
    }

    /**
     * 取得指定文件
     *
     * @param libraryName 函式庫名稱
     * @param version     版本號
     * @param path        文件路徑
     * @return 文件
     * @throws IllegalArgumentException 找不到文件時拋出
     */
    public Document findDocument(String libraryName, String version, String path) {
        Library library = findLibraryByName(libraryName);
        String versionId = resolveVersionId(library.getId(), version);
        return documentRepository.findByVersionIdAndPath(versionId, path)
                .orElseThrow(() -> new IllegalArgumentException(
                        "找不到文件: library=%s, version=%s, path=%s".formatted(libraryName, version, path)));
    }

    /**
     * 取得指定文件的程式碼範例
     *
     * @param documentId 文件 ID
     * @return 程式碼範例列表
     */
    public List<CodeExample> findCodeExamples(String documentId) {
        return codeExampleRepository.findByDocumentId(documentId);
    }

    /**
     * 統計指定函式庫的文件數量
     *
     * @param libraryId 函式庫 ID
     * @return 文件數量
     */
    public long countDocuments(String libraryId) {
        return documentRepository.countByLibraryId(libraryId);
    }
}
