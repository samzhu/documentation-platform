package io.github.samzhu.documentation.platform.support;

import io.github.samzhu.documentation.platform.domain.enums.ApiKeyStatus;
import io.github.samzhu.documentation.platform.domain.enums.SourceType;
import io.github.samzhu.documentation.platform.domain.enums.SyncStatus;
import io.github.samzhu.documentation.platform.domain.enums.VersionStatus;
import io.github.samzhu.documentation.platform.domain.model.*;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * 測試資料工廠
 * <p>
 * 提供測試資料建立的輔助方法，統一管理測試物件的建立邏輯。
 * </p>
 */
public class TestDataFactory {

    /**
     * 建立測試用的 Library
     *
     * @param id   Library ID
     * @param name Library 名稱
     * @return Library 實例
     */
    public static Library createLibrary(String id, String name) {
        return Library.create(
                id,
                name,
                "Test Library for " + name,
                "Test description for " + name,
                SourceType.GITHUB,
                "https://github.com/test-org/" + name,
                "backend",
                List.of("test", "library")
        );
    }

    /**
     * 建立測試用的 LibraryVersion
     *
     * @param id        Version ID
     * @param libraryId 所屬 Library ID
     * @param version   版本號
     * @return LibraryVersion 實例
     */
    public static LibraryVersion createLibraryVersion(String id, String libraryId, String version) {
        return LibraryVersion.create(
                id,
                libraryId,
                version,
                false
        );
    }

    /**
     * 建立測試用的 Document
     *
     * @param id        Document ID
     * @param versionId Version ID
     * @param title     文件標題
     * @return Document 實例
     */
    public static Document createDocument(String id, String versionId, String title) {
        return Document.create(
                id,
                versionId,
                title,
                "/docs/test.md",
                "Test content for " + title,
                "hash123",
                "markdown"
        );
    }

    /**
     * 建立測試用的 DocumentChunk
     *
     * @param id         Chunk ID
     * @param documentId Document ID
     * @param index      Chunk 索引
     * @param content    內容
     * @return DocumentChunk 實例
     */
    public static DocumentChunk createDocumentChunk(String id, String documentId, int index, String content) {
        return DocumentChunk.create(
                id,
                documentId,
                index,
                content,
                new float[768], // 768 維度的零向量
                100
        );
    }

    /**
     * 建立測試用的 DocumentChunk（含自定義 embedding）
     *
     * @param id         Chunk ID
     * @param documentId Document ID
     * @param index      Chunk 索引
     * @param content    內容
     * @param embedding  向量嵌入
     * @return DocumentChunk 實例
     */
    public static DocumentChunk createDocumentChunk(String id, String documentId, int index,
                                                     String content, float[] embedding) {
        return DocumentChunk.create(
                id,
                documentId,
                index,
                content,
                embedding,
                estimateTokenCount(content)
        );
    }

    /**
     * 建立測試用的 ApiKey
     *
     * @param id        Key ID
     * @param name      名稱
     * @param keyPrefix Key 前綴
     * @return ApiKey 實例
     */
    public static ApiKey createApiKey(String id, String name, String keyPrefix) {
        return ApiKey.create(
                id,
                name,
                "$2a$10$dummyHashForTesting",
                keyPrefix,
                1000,
                null,
                "admin"
        );
    }

    /**
     * 建立測試用的 ApiKey（含過期時間）
     *
     * @param id        Key ID
     * @param name      名稱
     * @param keyPrefix Key 前綴
     * @param expiresAt 過期時間
     * @return ApiKey 實例
     */
    public static ApiKey createApiKey(String id, String name, String keyPrefix, OffsetDateTime expiresAt) {
        return ApiKey.create(
                id,
                name,
                "$2a$10$dummyHashForTesting",
                keyPrefix,
                1000,
                expiresAt,
                "admin"
        );
    }

    /**
     * 建立測試用的已撤銷 ApiKey
     *
     * @param id        Key ID
     * @param name      名稱
     * @param keyPrefix Key 前綴
     * @return 已撤銷的 ApiKey 實例
     */
    public static ApiKey createRevokedApiKey(String id, String name, String keyPrefix) {
        return new ApiKey(
                id,
                name,
                "$2a$10$dummyHashForTesting",
                keyPrefix,
                ApiKeyStatus.REVOKED,
                1000,
                null,
                null,
                "admin",
                null,
                null,
                null
        );
    }

    /**
     * 建立測試用的 SyncHistory
     *
     * @param id        History ID
     * @param versionId Version ID
     * @return SyncHistory 實例
     */
    public static SyncHistory createSyncHistory(String id, String versionId) {
        return SyncHistory.createPending(id, versionId);
    }

    /**
     * 建立測試用的 CodeExample
     *
     * @param id         Example ID
     * @param documentId Document ID
     * @param language   程式語言
     * @param code       程式碼
     * @return CodeExample 實例
     */
    public static CodeExample createCodeExample(String id, String documentId, String language, String code) {
        return CodeExample.create(
                id,
                documentId,
                language,
                code,
                "Example code"
        );
    }

    /**
     * 建立測試用的 TSID
     *
     * @param seed 種子值（用於產生可預測的 ID）
     * @return 13 字元的 TSID 字串
     */
    public static String createTestId(int seed) {
        // 產生固定格式的測試 ID
        String base = "0000000000000" + seed;
        return base.substring(base.length() - 13);
    }

    /**
     * 建立測試用的 ID 列表
     *
     * @param count 數量
     * @return ID 列表
     */
    public static List<String> createTestIds(int count) {
        return java.util.stream.IntStream.range(0, count)
                .mapToObj(TestDataFactory::createTestId)
                .toList();
    }

    /**
     * 建立測試用的 768 維度向量
     *
     * @return 768 維度的浮點數陣列
     */
    public static float[] createTestEmbedding() {
        return createTestEmbedding(768);
    }

    /**
     * 建立測試用的指定維度向量
     *
     * @param dimensions 維度
     * @return 指定維度的浮點數陣列
     */
    public static float[] createTestEmbedding(int dimensions) {
        float[] embedding = new float[dimensions];
        for (int i = 0; i < dimensions; i++) {
            // 使用索引產生可預測的值
            embedding[i] = (float) Math.sin(i * 0.1);
        }
        return embedding;
    }

    /**
     * 估算 Token 數量（簡化版）
     *
     * @param text 文字
     * @return 估算的 Token 數量
     */
    private static int estimateTokenCount(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        // 簡單估算：平均 4 字元 = 1 token
        return (int) Math.ceil(text.length() / 4.0);
    }

    /**
     * 建立測試用的過去時間
     *
     * @param daysAgo 幾天前
     * @return OffsetDateTime
     */
    public static OffsetDateTime pastDateTime(int daysAgo) {
        return OffsetDateTime.now().minusDays(daysAgo);
    }

    /**
     * 建立測試用的未來時間
     *
     * @param daysLater 幾天後
     * @return OffsetDateTime
     */
    public static OffsetDateTime futureDateTime(int daysLater) {
        return OffsetDateTime.now().plusDays(daysLater);
    }
}
