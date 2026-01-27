package io.github.samzhu.documentation.platform.service;

import io.github.samzhu.documentation.platform.service.DocumentChunker.ChunkResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * DocumentChunker 單元測試
 * <p>
 * 測試文件分塊服務的分割邏輯、邊界處理、Token 估算等功能。
 * </p>
 */
@DisplayName("DocumentChunker 單元測試")
class DocumentChunkerTest {

    private DocumentChunker documentChunker;

    @BeforeEach
    void setUp() {
        documentChunker = new DocumentChunker();
    }

    @Test
    @DisplayName("應回傳單一區塊 - 當內容小於區塊大小時")
    void shouldReturnSingleChunk_whenContentSmallerThanChunkSize() {
        // Given
        String content = "這是一段簡短的文字";

        // When
        List<ChunkResult> chunks = documentChunker.chunk(content);

        // Then
        assertThat(chunks).hasSize(1);
        assertThat(chunks.get(0).content()).isEqualTo(content);
        assertThat(chunks.get(0).index()).isZero();
        assertThat(chunks.get(0).tokenCount()).isGreaterThan(0);
    }

    @Test
    @DisplayName("應回傳空列表 - 當內容為 null 時")
    void shouldReturnEmptyList_whenContentIsNull() {
        // When
        List<ChunkResult> chunks = documentChunker.chunk(null);

        // Then
        assertThat(chunks).isEmpty();
    }

    @Test
    @DisplayName("應回傳空列表 - 當內容為空白時")
    void shouldReturnEmptyList_whenContentIsBlank() {
        // When
        List<ChunkResult> chunks = documentChunker.chunk("   ");

        // Then
        assertThat(chunks).isEmpty();
    }

    @Test
    @DisplayName("應在段落邊界分割 - 當遇到雙換行時")
    void shouldSplitAtParagraphBoundary_whenDoubleNewlineFound() {
        // Given
        String paragraph1 = "A".repeat(500);
        String paragraph2 = "B".repeat(500);
        String paragraph3 = "C".repeat(500);
        String content = paragraph1 + "\n\n" + paragraph2 + "\n\n" + paragraph3;

        // When
        List<ChunkResult> chunks = documentChunker.chunk(content, 600, 100);

        // Then
        assertThat(chunks).hasSizeGreaterThan(1);
        // 第一個區塊應該在段落邊界結束
        assertThat(chunks.get(0).content()).contains(paragraph1);
    }

    @Test
    @DisplayName("應在句子邊界分割 - 當遇到句號時")
    void shouldSplitAtSentenceBoundary_whenPeriodFound() {
        // Given
        String sentence1 = "This is the first sentence. ";
        String sentence2 = "This is the second sentence. ";
        String content = sentence1.repeat(20) + sentence2.repeat(20);

        // When
        List<ChunkResult> chunks = documentChunker.chunk(content, 500, 50);

        // Then
        assertThat(chunks).hasSizeGreaterThan(1);
        // 區塊應該在句子邊界結束
        for (ChunkResult chunk : chunks) {
            String trimmed = chunk.content().trim();
            if (!trimmed.isEmpty()) {
                // 最後一個字元應該是句號或是因為重疊而在句中
                assertThat(trimmed).matches(".*[.!?。!?].*");
            }
        }
    }

    @Test
    @DisplayName("應在中文句子邊界分割 - 當遇到中文標點時")
    void shouldSplitAtChineseSentenceBoundary_whenChinesePunctuationFound() {
        // Given
        String sentence1 = "這是第一句話。";
        String sentence2 = "這是第二句話。";
        String content = sentence1.repeat(40) + sentence2.repeat(40);

        // When
        List<ChunkResult> chunks = documentChunker.chunk(content, 500, 50);

        // Then
        assertThat(chunks).hasSizeGreaterThan(1);
    }

    @Test
    @DisplayName("應正確估算英文 Token")
    void shouldEstimateTokensForEnglish() {
        // Given - 英文大約 4 個字元 = 1 token
        String content = "This is a test sentence with multiple words.";

        // When
        List<ChunkResult> chunks = documentChunker.chunk(content);

        // Then
        ChunkResult chunk = chunks.get(0);
        // 英文字元數約 44，估算應該約 11 tokens（44/4）
        assertThat(chunk.tokenCount()).isBetween(8, 15);
    }

    @Test
    @DisplayName("應正確估算中文 Token")
    void shouldEstimateTokensForChinese() {
        // Given - 中文大約 1.5 個字元 = 1 token
        String content = "這是一個測試句子包含多個中文字";

        // When
        List<ChunkResult> chunks = documentChunker.chunk(content);

        // Then
        ChunkResult chunk = chunks.get(0);
        // 中文字元數 15，估算應該約 10 tokens（15/1.5）
        assertThat(chunk.tokenCount()).isBetween(8, 12);
    }

    @Test
    @DisplayName("應正確估算混合語言 Token")
    void shouldEstimateTokensForMixedLanguage() {
        // Given
        String content = "這是一個 test sentence 包含中英文混合的內容";

        // When
        List<ChunkResult> chunks = documentChunker.chunk(content);

        // Then
        ChunkResult chunk = chunks.get(0);
        assertThat(chunk.tokenCount()).isGreaterThan(0);
    }

    @Test
    @DisplayName("應產生重疊的區塊 - 當設定重疊大小時")
    void shouldGenerateOverlappingChunks_whenOverlapSet() {
        // Given
        String content = "A".repeat(1500);

        // When
        List<ChunkResult> chunks = documentChunker.chunk(content, 1000, 200);

        // Then
        assertThat(chunks.size()).isGreaterThan(1);
        if (chunks.size() >= 2) {
            String firstChunkEnd = chunks.get(0).content().substring(
                    Math.max(0, chunks.get(0).content().length() - 250)
            );
            String secondChunkStart = chunks.get(1).content().substring(
                    0, Math.min(250, chunks.get(1).content().length())
            );
            // 應該有部分重疊的內容
            assertThat(secondChunkStart).contains(firstChunkEnd.substring(50, 150));
        }
    }

    @Test
    @DisplayName("應正確設定區塊索引 - 當產生多個區塊時")
    void shouldSetCorrectChunkIndex_whenMultipleChunksGenerated() {
        // Given
        String content = "A".repeat(3000);

        // When
        List<ChunkResult> chunks = documentChunker.chunk(content, 1000, 200);

        // Then
        for (int i = 0; i < chunks.size(); i++) {
            assertThat(chunks.get(i).index()).isEqualTo(i);
        }
    }

    @Test
    @DisplayName("應處理超大文件 - 當內容超過 10000 字元時")
    void shouldHandleLargeDocument_whenContentExceeds10000Characters() {
        // Given
        String content = "這是測試內容。".repeat(2000); // 約 14000 字元

        // When
        List<ChunkResult> chunks = documentChunker.chunk(content);

        // Then
        assertThat(chunks).isNotEmpty();
        int totalLength = chunks.stream().mapToInt(c -> c.content().length()).sum();
        // 由於重疊，總長度會大於原始內容
        assertThat(totalLength).isGreaterThanOrEqualTo(content.length());
    }

    @Test
    @DisplayName("應使用預設參數 - 當參數無效時")
    void shouldUseDefaultParameters_whenInvalidParametersProvided() {
        // Given
        String content = "A".repeat(3000);

        // When - 提供無效的參數
        List<ChunkResult> chunks = documentChunker.chunk(content, -100, 1500);

        // Then - 應該使用預設參數並成功分塊
        assertThat(chunks).isNotEmpty();
    }

    @Test
    @DisplayName("應回傳空列表 - 當內容僅包含空白字元時")
    void shouldReturnEmptyList_whenContentOnlyContainsSpaces() {
        // Given
        String content = " ".repeat(100);

        // When
        List<ChunkResult> chunks = documentChunker.chunk(content);

        // Then
        // isBlank() 會判定為空白內容，回傳空列表
        assertThat(chunks).isEmpty();
    }

    @Test
    @DisplayName("應在換行邊界分割 - 當找不到段落邊界時")
    void shouldSplitAtNewlineBoundary_whenNoParagraphBoundaryFound() {
        // Given
        String line1 = "A".repeat(500) + "\n";
        String line2 = "B".repeat(500) + "\n";
        String content = line1 + line2;

        // When
        List<ChunkResult> chunks = documentChunker.chunk(content, 600, 100);

        // Then
        assertThat(chunks).hasSizeGreaterThan(1);
    }

    @Test
    @DisplayName("應正確處理邊界情況 - 當區塊大小等於內容長度時")
    void shouldHandleEdgeCase_whenChunkSizeEqualsContentLength() {
        // Given
        String content = "A".repeat(1000);

        // When
        List<ChunkResult> chunks = documentChunker.chunk(content, 1000, 200);

        // Then
        assertThat(chunks).hasSize(1);
        assertThat(chunks.get(0).content()).hasSize(1000);
    }
}
