package io.github.samzhu.documentation.platform.repository;

import io.github.samzhu.documentation.platform.TestcontainersConfiguration;
import io.github.samzhu.documentation.platform.domain.enums.SourceType;
import io.github.samzhu.documentation.platform.domain.model.Document;
import io.github.samzhu.documentation.platform.domain.model.DocumentChunk;
import io.github.samzhu.documentation.platform.domain.model.Library;
import io.github.samzhu.documentation.platform.domain.model.LibraryVersion;
import io.github.samzhu.documentation.platform.service.IdService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * DocumentChunk Repository 整合測試
 * <p>
 * 使用 Testcontainers 啟動真實 PostgreSQL（含 pgvector）資料庫進行測試。
 * 驗證向量搜尋和區塊查詢功能。
 * </p>
 */
@Tag("integration")
@SpringBootTest
@Import(TestcontainersConfiguration.class)
@Transactional
class DocumentChunkRepositoryIntegrationTest {

    @MockitoBean
    EmbeddingModel embeddingModel;

    @Autowired
    DocumentChunkRepository documentChunkRepository;

    @Autowired
    DocumentRepository documentRepository;

    @Autowired
    LibraryVersionRepository libraryVersionRepository;

    @Autowired
    LibraryRepository libraryRepository;

    @Autowired
    IdService idService;

    private Document testDocument;
    private LibraryVersion testVersion;

    @BeforeEach
    void setUp() {
        // 清除所有測試資料
        documentChunkRepository.deleteAll();
        documentRepository.deleteAll();
        libraryVersionRepository.deleteAll();
        libraryRepository.deleteAll();

        // 建立測試用 Library、Version、Document
        Library library = Library.create(
                idService.generateId(),
                "spring-boot",
                "Spring Boot",
                "Spring Boot 框架",
                SourceType.GITHUB,
                "https://github.com/spring-projects/spring-boot",
                "backend",
                List.of()
        );
        library = libraryRepository.save(library);

        testVersion = LibraryVersion.create(
                idService.generateId(),
                library.getId(),
                "3.2.0",
                true
        );
        testVersion = libraryVersionRepository.save(testVersion);

        testDocument = Document.create(
                idService.generateId(),
                testVersion.getId(),
                "Getting Started",
                "/docs/getting-started.md",
                "Full document content here",
                "hash123",
                "markdown"
        );
        testDocument = documentRepository.save(testDocument);
    }

    @Test
    @DisplayName("應能儲存並查詢 DocumentChunk")
    void shouldSaveAndFindChunk() {
        // Given - 準備區塊資料（含 768 維向量）
        float[] embedding = createTestEmbedding(768, 0.1f);
        DocumentChunk chunk = DocumentChunk.create(
                idService.generateId(),
                testDocument.getId(),
                0,
                "This is the first chunk of the document",
                embedding,
                50
        );

        // When - 儲存
        DocumentChunk saved = documentChunkRepository.save(chunk);

        // Then - 驗證儲存結果
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getDocumentId()).isEqualTo(testDocument.getId());
        assertThat(saved.getChunkIndex()).isEqualTo(0);
        assertThat(saved.getContent()).isEqualTo("This is the first chunk of the document");
        assertThat(saved.getTokenCount()).isEqualTo(50);
    }

    @Test
    @DisplayName("應能依文件 ID 查詢所有區塊並依索引排序")
    void shouldFindByDocumentIdOrderByChunkIndex() {
        // Given - 建立多個區塊（順序打亂）
        DocumentChunk chunk2 = DocumentChunk.create(
                idService.generateId(),
                testDocument.getId(),
                2,
                "Third chunk",
                createTestEmbedding(768, 0.3f),
                30
        );
        DocumentChunk chunk0 = DocumentChunk.create(
                idService.generateId(),
                testDocument.getId(),
                0,
                "First chunk",
                createTestEmbedding(768, 0.1f),
                30
        );
        DocumentChunk chunk1 = DocumentChunk.create(
                idService.generateId(),
                testDocument.getId(),
                1,
                "Second chunk",
                createTestEmbedding(768, 0.2f),
                30
        );

        documentChunkRepository.saveAll(List.of(chunk2, chunk0, chunk1));

        // When - 查詢並排序
        List<DocumentChunk> chunks = documentChunkRepository.findByDocumentIdOrderByChunkIndex(testDocument.getId());

        // Then - 應依 chunk_index 排序
        assertThat(chunks).hasSize(3);
        assertThat(chunks.get(0).getChunkIndex()).isEqualTo(0);
        assertThat(chunks.get(1).getChunkIndex()).isEqualTo(1);
        assertThat(chunks.get(2).getChunkIndex()).isEqualTo(2);
        assertThat(chunks.get(0).getContent()).isEqualTo("First chunk");
    }

    @Test
    @DisplayName("應能取得文件的第一個區塊")
    void shouldFindFirstByDocumentId() {
        // Given - 建立多個區塊
        DocumentChunk chunk0 = DocumentChunk.create(
                idService.generateId(),
                testDocument.getId(),
                0,
                "First chunk",
                createTestEmbedding(768, 0.1f),
                30
        );
        DocumentChunk chunk1 = DocumentChunk.create(
                idService.generateId(),
                testDocument.getId(),
                1,
                "Second chunk",
                createTestEmbedding(768, 0.2f),
                30
        );

        documentChunkRepository.saveAll(List.of(chunk0, chunk1));

        // When - 取得第一個區塊
        DocumentChunk first = documentChunkRepository.findFirstByDocumentId(testDocument.getId());

        // Then - 應為 index 0 的區塊
        assertThat(first).isNotNull();
        assertThat(first.getChunkIndex()).isEqualTo(0);
        assertThat(first.getContent()).isEqualTo("First chunk");
    }

    @Test
    @DisplayName("向量搜尋應找到最相似的區塊")
    void shouldFindSimilarChunks() {
        // Given - 建立多個區塊，使用不同的向量
        // 區塊 1：向量值都是 0.1（與查詢向量相似度高）
        DocumentChunk similarChunk = DocumentChunk.create(
                idService.generateId(),
                testDocument.getId(),
                0,
                "Similar content about Spring",
                createTestEmbedding(768, 0.1f),
                30
        );

        // 區塊 2：向量值都是 0.9（與查詢向量相似度低）
        DocumentChunk differentChunk = DocumentChunk.create(
                idService.generateId(),
                testDocument.getId(),
                1,
                "Different content about something else",
                createTestEmbedding(768, 0.9f),
                30
        );

        documentChunkRepository.saveAll(List.of(similarChunk, differentChunk));

        // 查詢向量：值都是 0.1（與 similarChunk 最接近）
        String queryEmbedding = formatEmbeddingForQuery(createTestEmbedding(768, 0.1f));

        // When - 執行向量搜尋
        List<DocumentChunk> results = documentChunkRepository.findSimilarChunks(
                testVersion.getId(),
                queryEmbedding,
                10
        );

        // Then - 最相似的應排在前面
        assertThat(results).hasSize(2);
        // 第一個結果應該是 similarChunk（向量值最接近 0.1）
        assertThat(results.get(0).getContent()).isEqualTo("Similar content about Spring");
    }

    @Test
    @DisplayName("向量搜尋應遵守 limit 限制")
    void shouldRespectLimitInSimilarChunks() {
        // Given - 建立多個區塊
        for (int i = 0; i < 10; i++) {
            DocumentChunk chunk = DocumentChunk.create(
                    idService.generateId(),
                    testDocument.getId(),
                    i,
                    "Chunk content " + i,
                    createTestEmbedding(768, 0.1f + i * 0.05f),
                    30
            );
            documentChunkRepository.save(chunk);
        }

        String queryEmbedding = formatEmbeddingForQuery(createTestEmbedding(768, 0.1f));

        // When - 限制只回傳 3 個結果
        List<DocumentChunk> results = documentChunkRepository.findSimilarChunks(
                testVersion.getId(),
                queryEmbedding,
                3
        );

        // Then - 應只回傳 3 個結果
        assertThat(results).hasSize(3);
    }

    @Test
    @DisplayName("應能排除指定文件進行相似度搜尋")
    void shouldFindSimilarChunksExcludingDocument() {
        // Given - 建立另一個文件的區塊
        Document otherDocument = Document.create(
                idService.generateId(),
                testVersion.getId(),
                "Other Document",
                "/docs/other.md",
                "Other content",
                "otherhash",
                "markdown"
        );
        otherDocument = documentRepository.save(otherDocument);

        // 為兩個文件各建立一個區塊
        DocumentChunk chunk1 = DocumentChunk.create(
                idService.generateId(),
                testDocument.getId(),
                0,
                "Chunk from test document",
                createTestEmbedding(768, 0.1f),
                30
        );
        DocumentChunk chunk2 = DocumentChunk.create(
                idService.generateId(),
                otherDocument.getId(),
                0,
                "Chunk from other document",
                createTestEmbedding(768, 0.15f),
                30
        );

        documentChunkRepository.saveAll(List.of(chunk1, chunk2));

        String queryEmbedding = formatEmbeddingForQuery(createTestEmbedding(768, 0.1f));

        // When - 搜尋時排除 testDocument
        List<DocumentChunk> results = documentChunkRepository.findSimilarChunksExcludingDocument(
                testDocument.getId(),
                queryEmbedding,
                10
        );

        // Then - 只應找到 otherDocument 的區塊
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getContent()).isEqualTo("Chunk from other document");
    }

    @Test
    @DisplayName("應能統計指定 Library 的區塊數量")
    void shouldCountByLibraryId() {
        // Given - 取得 Library
        Library library = libraryRepository.findByName("spring-boot").orElseThrow();

        // 建立另一個版本
        LibraryVersion version2 = LibraryVersion.create(
                idService.generateId(),
                library.getId(),
                "3.1.0",
                false
        );
        version2 = libraryVersionRepository.save(version2);

        Document doc2 = Document.create(
                idService.generateId(),
                version2.getId(),
                "Doc 2",
                "/docs/doc2.md",
                "Content 2",
                "hash2",
                "markdown"
        );
        doc2 = documentRepository.save(doc2);

        // 為兩個版本的文件各建立區塊
        DocumentChunk chunk1 = DocumentChunk.create(
                idService.generateId(),
                testDocument.getId(),
                0,
                "Chunk 1",
                createTestEmbedding(768, 0.1f),
                30
        );
        DocumentChunk chunk2 = DocumentChunk.create(
                idService.generateId(),
                testDocument.getId(),
                1,
                "Chunk 2",
                createTestEmbedding(768, 0.2f),
                30
        );
        DocumentChunk chunk3 = DocumentChunk.create(
                idService.generateId(),
                doc2.getId(),
                0,
                "Chunk 3",
                createTestEmbedding(768, 0.3f),
                30
        );

        documentChunkRepository.saveAll(List.of(chunk1, chunk2, chunk3));

        // When - 統計該 Library 的區塊數量
        long count = documentChunkRepository.countByLibraryId(library.getId());

        // Then - 應為 3
        assertThat(count).isEqualTo(3);
    }

    @Test
    @DisplayName("embedding 為 null 的區塊不應出現在向量搜尋結果中")
    void shouldExcludeNullEmbeddingsFromSearch() {
        // Given - 建立一個有 embedding 和一個沒有 embedding 的區塊
        DocumentChunk chunkWithEmbedding = DocumentChunk.create(
                idService.generateId(),
                testDocument.getId(),
                0,
                "Chunk with embedding",
                createTestEmbedding(768, 0.1f),
                30
        );
        DocumentChunk chunkWithoutEmbedding = DocumentChunk.create(
                idService.generateId(),
                testDocument.getId(),
                1,
                "Chunk without embedding",
                null,
                30
        );

        documentChunkRepository.saveAll(List.of(chunkWithEmbedding, chunkWithoutEmbedding));

        String queryEmbedding = formatEmbeddingForQuery(createTestEmbedding(768, 0.1f));

        // When - 執行向量搜尋
        List<DocumentChunk> results = documentChunkRepository.findSimilarChunks(
                testVersion.getId(),
                queryEmbedding,
                10
        );

        // Then - 只應找到有 embedding 的區塊
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getContent()).isEqualTo("Chunk with embedding");
    }

    @Test
    @DisplayName("儲存後應自動設定樂觀鎖版本號")
    void shouldSetVersionOnSave() {
        // Given - 準備區塊資料
        DocumentChunk chunk = DocumentChunk.create(
                idService.generateId(),
                testDocument.getId(),
                0,
                "Test chunk",
                createTestEmbedding(768, 0.1f),
                30
        );

        // version 初始為 null（新實體）
        assertThat(chunk.getVersion()).isNull();

        // When - 儲存
        DocumentChunk saved = documentChunkRepository.save(chunk);

        // Then - 樂觀鎖版本號應由 Spring Data JDBC 自動設定
        assertThat(saved.getVersion()).isNotNull();
    }

    /**
     * 建立測試用的向量（指定維度和基準值）
     *
     * @param dimensions 維度
     * @param baseValue  基準值
     * @return 測試向量
     */
    private float[] createTestEmbedding(int dimensions, float baseValue) {
        float[] embedding = new float[dimensions];
        Arrays.fill(embedding, baseValue);
        return embedding;
    }

    /**
     * 將 float[] 轉換為 PostgreSQL vector 格式的字串
     *
     * @param embedding 向量陣列
     * @return 格式化的字串，如 "[0.1,0.1,...]"
     */
    private String formatEmbeddingForQuery(float[] embedding) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < embedding.length; i++) {
            if (i > 0) {
                sb.append(",");
            }
            sb.append(embedding[i]);
        }
        sb.append("]");
        return sb.toString();
    }
}
