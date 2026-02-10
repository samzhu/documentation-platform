package io.github.samzhu.documentation.mcp.service;

import io.github.samzhu.documentation.mcp.config.SearchProperties;
import io.github.samzhu.documentation.mcp.domain.model.Document;
import io.github.samzhu.documentation.mcp.domain.model.Library;
import io.github.samzhu.documentation.mcp.domain.model.LibraryVersion;
import io.github.samzhu.documentation.mcp.domain.enums.SourceType;
import io.github.samzhu.documentation.mcp.domain.enums.VersionStatus;
import io.github.samzhu.documentation.mcp.repository.DocumentRepository;
import io.github.samzhu.documentation.mcp.repository.LibraryRepository;
import io.github.samzhu.documentation.mcp.repository.LibraryVersionRepository;
import io.github.samzhu.documentation.mcp.service.dto.SearchResultItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.document.DocumentMetadata;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static io.github.samzhu.documentation.mcp.infrastructure.vectorstore.DocumentChunkVectorStore.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * SearchService 單元測試
 * <p>
 * 使用純 Mockito mock 所有外部依賴（Repository、VectorStore），
 * 不啟動 Spring Context，聚焦測試搜尋邏輯本身。
 * </p>
 *
 * @see SearchService
 */
@ExtendWith(MockitoExtension.class)
class SearchServiceTests {

    @Mock
    private DocumentRepository documentRepository;

    @Mock
    private LibraryRepository libraryRepository;

    @Mock
    private LibraryVersionRepository versionRepository;

    @Mock
    private VectorStore vectorStore;

    private SearchService searchService;

    // 測試用常數
    private static final String LIBRARY_ID = "LIB0000000001";
    private static final String VERSION_ID = "VER0000000001";
    private static final String DOC_ID_1 = "DOC0000000001";
    private static final String DOC_ID_2 = "DOC0000000002";
    private static final String CHUNK_ID_1 = "CHK0000000001";
    private static final String CHUNK_ID_2 = "CHK0000000002";

    @BeforeEach
    void setUp() {
        // alpha=0.3 表示全文權重 30%，語意權重 70%；minSimilarity=0.5
        var searchProperties = new SearchProperties(
                new SearchProperties.Hybrid(0.3, 0.5),
                10, 20
        );
        searchService = new SearchService(
                documentRepository, libraryRepository, versionRepository,
                vectorStore, searchProperties
        );
    }

    // ========== 全文搜尋 ==========

    @Nested
    @DisplayName("全文搜尋")
    class FullTextSearchTests {

        @Test
        @DisplayName("空查詢回傳空列表")
        void emptyQueryReturnsEmpty() {
            assertThat(searchService.fullTextSearch(VERSION_ID, "", 10)).isEmpty();
            assertThat(searchService.fullTextSearch(VERSION_ID, null, 10)).isEmpty();
            assertThat(searchService.fullTextSearch(VERSION_ID, "   ", 10)).isEmpty();
        }

        @Test
        @DisplayName("委派至 DocumentRepository 並截斷內容")
        void delegatesToRepositoryAndTruncatesContent() {
            // 準備超過 500 字元的內容
            String longContent = "x".repeat(600);
            var doc = createDocument(DOC_ID_1, "標題一", "/path/one", longContent);

            given(documentRepository.fullTextSearch(VERSION_ID, "spring", 10))
                    .willReturn(List.of(doc));

            List<SearchResultItem> results = searchService.fullTextSearch(VERSION_ID, "spring", 10);

            assertThat(results).hasSize(1);
            assertThat(results.getFirst().documentId()).isEqualTo(DOC_ID_1);
            assertThat(results.getFirst().title()).isEqualTo("標題一");
            // 驗證內容被截斷為 500 字元 + "..."
            assertThat(results.getFirst().content()).hasSize(503);
            assertThat(results.getFirst().content()).endsWith("...");
            // 全文搜尋預設分數為 1.0
            assertThat(results.getFirst().score()).isEqualTo(1.0);
        }
    }

    // ========== 語意搜尋 ==========

    @Nested
    @DisplayName("語意搜尋")
    class SemanticSearchTests {

        @Test
        @DisplayName("空查詢回傳空列表")
        void emptyQueryReturnsEmpty() {
            assertThat(searchService.semanticSearch(VERSION_ID, "", 10, 0.5)).isEmpty();
            assertThat(searchService.semanticSearch(VERSION_ID, null, 10, 0.5)).isEmpty();
        }

        @Test
        @DisplayName("建構正確的 SearchRequest 並轉換結果")
        void buildsSearchRequestAndMapsResults() {
            // 準備 VectorStore 回傳的 Spring AI Document（含 metadata）
            var aiDoc = org.springframework.ai.document.Document.builder()
                    .id(CHUNK_ID_1)
                    .text("這是語意搜尋匹配的文件片段")
                    .metadata(Map.of(
                            METADATA_DOCUMENT_ID, DOC_ID_1,
                            METADATA_CHUNK_INDEX, 2,
                            METADATA_VERSION_ID, VERSION_ID
                    ))
                    .score(0.85)
                    .build();

            given(vectorStore.similaritySearch(any(SearchRequest.class)))
                    .willReturn(List.of(aiDoc));

            // 準備 DB Document（用於補充標題、路徑等資訊）
            var dbDoc = createDocument(DOC_ID_1, "Spring Boot 入門", "/getting-started", "完整內容...");
            given(documentRepository.findAllById(List.of(DOC_ID_1)))
                    .willReturn(List.of(dbDoc));

            List<SearchResultItem> results = searchService.semanticSearch(VERSION_ID, "如何開始使用", 10, 0.5);

            assertThat(results).hasSize(1);
            var result = results.getFirst();
            assertThat(result.documentId()).isEqualTo(DOC_ID_1);
            assertThat(result.chunkId()).isEqualTo(CHUNK_ID_1);
            assertThat(result.title()).isEqualTo("Spring Boot 入門");
            assertThat(result.content()).isEqualTo("這是語意搜尋匹配的文件片段");
            assertThat(result.score()).isEqualTo(0.85);
            assertThat(result.chunkIndex()).isEqualTo(2);
        }

        @Test
        @DisplayName("VectorStore 無結果時回傳空列表")
        void emptyVectorStoreResultsReturnsEmpty() {
            given(vectorStore.similaritySearch(any(SearchRequest.class)))
                    .willReturn(List.of());

            List<SearchResultItem> results = searchService.semanticSearch(VERSION_ID, "查詢", 10, 0.5);

            assertThat(results).isEmpty();
            // 不應查詢 DocumentRepository
            verify(documentRepository, never()).findAllById(any());
        }
    }

    // ========== 混合搜尋（RRF 演算法）==========

    @Nested
    @DisplayName("混合搜尋 RRF 演算法")
    class HybridSearchTests {

        @Test
        @DisplayName("空查詢回傳空列表")
        void emptyQueryReturnsEmpty() {
            assertThat(searchService.hybridSearch(VERSION_ID, "", 10)).isEmpty();
        }

        @Test
        @DisplayName("兩種搜尋都無結果時回傳空列表")
        void bothEmptyReturnsEmpty() {
            given(documentRepository.fullTextSearch(eq(VERSION_ID), anyString(), anyInt()))
                    .willReturn(List.of());
            given(vectorStore.similaritySearch(any(SearchRequest.class)))
                    .willReturn(List.of());

            assertThat(searchService.hybridSearch(VERSION_ID, "spring", 10)).isEmpty();
        }

        @Test
        @DisplayName("僅全文有結果時 fallback 至全文結果")
        void fallbackToFulltextWhenSemanticEmpty() {
            var doc = createDocument(DOC_ID_1, "標題", "/path", "內容");
            given(documentRepository.fullTextSearch(eq(VERSION_ID), anyString(), anyInt()))
                    .willReturn(List.of(doc));
            given(vectorStore.similaritySearch(any(SearchRequest.class)))
                    .willReturn(List.of());

            List<SearchResultItem> results = searchService.hybridSearch(VERSION_ID, "spring", 10);

            assertThat(results).hasSize(1);
            assertThat(results.getFirst().documentId()).isEqualTo(DOC_ID_1);
        }

        @Test
        @DisplayName("僅語意有結果時 fallback 至語意結果")
        void fallbackToSemanticWhenFulltextEmpty() {
            given(documentRepository.fullTextSearch(eq(VERSION_ID), anyString(), anyInt()))
                    .willReturn(List.of());

            var aiDoc = createAiDocument(CHUNK_ID_1, DOC_ID_1, "語意結果", 0.9);
            given(vectorStore.similaritySearch(any(SearchRequest.class)))
                    .willReturn(List.of(aiDoc));

            var dbDoc = createDocument(DOC_ID_1, "標題", "/path", "內容");
            given(documentRepository.findAllById(List.of(DOC_ID_1)))
                    .willReturn(List.of(dbDoc));

            List<SearchResultItem> results = searchService.hybridSearch(VERSION_ID, "如何開始", 10);

            assertThat(results).hasSize(1);
            assertThat(results.getFirst().chunkId()).isEqualTo(CHUNK_ID_1);
        }

        @Test
        @DisplayName("兩種都有結果時使用 RRF 融合並依分數排序")
        void fusesResultsWithRRFScoring() {
            // 全文搜尋結果：key 為 "doc:DOC_ID"
            var ftDoc1 = createDocument(DOC_ID_1, "文件一", "/one", "短內容");
            var ftDoc2 = createDocument(DOC_ID_2, "文件二", "/two", "短內容");
            given(documentRepository.fullTextSearch(eq(VERSION_ID), anyString(), anyInt()))
                    .willReturn(List.of(ftDoc1, ftDoc2));

            // 語意搜尋結果：key 為 "chunk:CHUNK_ID"
            // DOC_ID_2 的 chunk 排名第 1，DOC_ID_1 的 chunk 排名第 2
            var aiDoc2 = createAiDocument(CHUNK_ID_2, DOC_ID_2, "語意片段二", 0.95);
            var aiDoc1 = createAiDocument(CHUNK_ID_1, DOC_ID_1, "語意片段一", 0.80);
            given(vectorStore.similaritySearch(any(SearchRequest.class)))
                    .willReturn(List.of(aiDoc2, aiDoc1));

            given(documentRepository.findAllById(anyList()))
                    .willReturn(List.of(ftDoc1, ftDoc2));

            List<SearchResultItem> results = searchService.hybridSearch(VERSION_ID, "spring boot", 10);

            // 全文產生 2 筆 (doc:xxx)，語意產生 2 筆 (chunk:xxx) → RRF 合計 4 筆
            assertThat(results).hasSize(4);

            // alpha=0.3，語意權重 0.7 > 全文權重 0.3
            // 語意排名第 1 (chunk:CHUNK_ID_2) 的 RRF 分數最高
            assertThat(results.getFirst().chunkId()).isEqualTo(CHUNK_ID_2);
            assertThat(results.getFirst().documentId()).isEqualTo(DOC_ID_2);

            // 所有分數已正規化為 0-1 範圍
            assertThat(results).allSatisfy(item ->
                    assertThat(item.score()).isBetween(0.0, 1.0));
        }
    }

    // ========== 統一搜尋入口 ==========

    @Nested
    @DisplayName("搜尋統一入口")
    class SearchEntryTests {

        @Test
        @DisplayName("空查詢回傳空列表")
        void emptyQueryReturnsEmpty() {
            assertThat(searchService.search("lib", "1.0", "", "hybrid", 10)).isEmpty();
        }

        @Test
        @DisplayName("指定 libraryName 時搜尋單一函式庫")
        void searchInSpecificLibrary() {
            var library = createLibrary(LIBRARY_ID, "spring-boot");
            var version = createVersion(VERSION_ID, LIBRARY_ID, "3.2.0");

            given(libraryRepository.findByName("spring-boot")).willReturn(Optional.of(library));
            given(versionRepository.findByLibraryIdAndVersion(LIBRARY_ID, "3.2.0"))
                    .willReturn(Optional.of(version));

            // fulltext 模式
            given(documentRepository.fullTextSearch(eq(VERSION_ID), eq("query"), anyInt()))
                    .willReturn(List.of());

            List<SearchResultItem> results = searchService.search(
                    "spring-boot", "3.2.0", "query", "fulltext", 10);

            assertThat(results).isEmpty();
            verify(libraryRepository).findByName("spring-boot");
        }

        @Test
        @DisplayName("找不到 Library 時拋出 IllegalArgumentException")
        void libraryNotFoundThrowsException() {
            given(libraryRepository.findByName("nonexistent")).willReturn(Optional.empty());

            assertThatThrownBy(() ->
                    searchService.search("nonexistent", null, "query", "hybrid", 10))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("找不到函式庫");
        }

        @Test
        @DisplayName("未指定 libraryName 時搜尋所有函式庫的最新版本")
        void searchAcrossAllLibraries() {
            var lib1 = createLibrary(LIBRARY_ID, "spring-boot");
            var ver1 = createVersion(VERSION_ID, LIBRARY_ID, "3.2.0");

            given(libraryRepository.findAll()).willReturn(List.of(lib1));
            given(versionRepository.findLatestByLibraryId(LIBRARY_ID))
                    .willReturn(Optional.of(ver1));

            // fulltext 模式
            given(documentRepository.fullTextSearch(eq(VERSION_ID), eq("test"), anyInt()))
                    .willReturn(List.of());

            List<SearchResultItem> results = searchService.search(
                    null, null, "test", "fulltext", 10);

            assertThat(results).isEmpty();
            verify(libraryRepository).findAll();
        }

        @Test
        @DisplayName("limit 超過 maxLimit 時自動截斷")
        void limitIsCappedByMaxLimit() {
            var library = createLibrary(LIBRARY_ID, "spring-boot");
            var version = createVersion(VERSION_ID, LIBRARY_ID, "3.2.0");

            given(libraryRepository.findByName("spring-boot")).willReturn(Optional.of(library));
            given(versionRepository.findByLibraryIdAndVersion(LIBRARY_ID, "3.2.0"))
                    .willReturn(Optional.of(version));
            given(documentRepository.fullTextSearch(eq(VERSION_ID), anyString(), anyInt()))
                    .willReturn(List.of());

            // maxLimit 是 20，傳入 100 應被截斷
            searchService.search("spring-boot", "3.2.0", "query", "fulltext", 100);

            // 驗證傳給 repository 的 limit 是 20（被 maxLimit 截斷）
            verify(documentRepository).fullTextSearch(VERSION_ID, "query", 20);
        }
    }

    // ========== 測試輔助方法 ==========

    /** 建立測試用 Document entity */
    private Document createDocument(String id, String title, String path, String content) {
        return new Document(id, VERSION_ID, title, path, content, "hash", "markdown",
                Map.of(), OffsetDateTime.now(), OffsetDateTime.now());
    }

    /** 建立測試用 Spring AI Document（模擬 VectorStore 回傳結果） */
    private org.springframework.ai.document.Document createAiDocument(
            String chunkId, String documentId, String content, double score) {
        return org.springframework.ai.document.Document.builder()
                .id(chunkId)
                .text(content)
                .metadata(Map.of(
                        METADATA_DOCUMENT_ID, documentId,
                        METADATA_CHUNK_INDEX, 0,
                        METADATA_VERSION_ID, VERSION_ID
                ))
                .score(score)
                .build();
    }

    /** 建立測試用 Library entity */
    private Library createLibrary(String id, String name) {
        return new Library(id, name, name, "描述", SourceType.GITHUB,
                "https://github.com/test", "backend", List.of("java"),
                OffsetDateTime.now(), OffsetDateTime.now());
    }

    /** 建立測試用 LibraryVersion entity */
    private LibraryVersion createVersion(String id, String libraryId, String version) {
        return new LibraryVersion(id, libraryId, version, true, false,
                VersionStatus.ACTIVE, "/docs", null,
                OffsetDateTime.now(), OffsetDateTime.now());
    }
}
