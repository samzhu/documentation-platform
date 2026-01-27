package io.github.samzhu.documentation.platform.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.embedding.EmbeddingModel;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * EmbeddingService 單元測試
 * <p>
 * 測試嵌入服務的向量轉換、批次處理、格式轉換等功能。
 * </p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("EmbeddingService 單元測試")
class EmbeddingServiceTest {

    @Mock
    private EmbeddingModel embeddingModel;

    private EmbeddingService embeddingService;

    @BeforeEach
    void setUp() {
        embeddingService = new EmbeddingService(embeddingModel);
    }

    @Test
    @DisplayName("應回傳嵌入向量 - 當文字有效時")
    void shouldReturnEmbedding_whenValidText() {
        // Given
        float[] expectedEmbedding = createMockEmbedding(768);
        when(embeddingModel.embed(anyString())).thenReturn(expectedEmbedding);

        // When
        float[] result = embeddingService.embed("測試文字");

        // Then
        assertThat(result).hasSize(768);
        assertThat(result).isEqualTo(expectedEmbedding);
    }

    @Test
    @DisplayName("應拋出例外 - 當文字為 null 時")
    void shouldThrowException_whenTextIsNull() {
        // When & Then
        assertThatThrownBy(() -> embeddingService.embed(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("文字不得為空");
    }

    @Test
    @DisplayName("應拋出例外 - 當文字為空白時")
    void shouldThrowException_whenTextIsBlank() {
        // When & Then
        assertThatThrownBy(() -> embeddingService.embed("   "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("文字不得為空");
    }

    @Test
    @DisplayName("應拋出例外 - 當文字為空字串時")
    void shouldThrowException_whenTextIsEmpty() {
        // When & Then
        assertThatThrownBy(() -> embeddingService.embed(""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("文字不得為空");
    }

    @Test
    @DisplayName("應回傳嵌入向量列表 - 當批次文字有效時")
    void shouldReturnEmbeddings_whenValidTexts() {
        // Given
        List<String> texts = Arrays.asList("文字1", "文字2", "文字3");
        List<float[]> expectedEmbeddings = Arrays.asList(
                createMockEmbedding(768),
                createMockEmbedding(768),
                createMockEmbedding(768)
        );
        when(embeddingModel.embed(anyList())).thenReturn(expectedEmbeddings);

        // When
        List<float[]> result = embeddingService.embedBatch(texts);

        // Then
        assertThat(result).hasSize(3);
        for (float[] embedding : result) {
            assertThat(embedding).hasSize(768);
        }
    }

    @Test
    @DisplayName("應拋出例外 - 當批次文字為 null 時")
    void shouldThrowException_whenTextsIsNull() {
        // When & Then
        assertThatThrownBy(() -> embeddingService.embedBatch(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("文字列表不得為空");
    }

    @Test
    @DisplayName("應拋出例外 - 當批次文字為空列表時")
    void shouldThrowException_whenTextsIsEmpty() {
        // When & Then
        assertThatThrownBy(() -> embeddingService.embedBatch(List.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("文字列表不得為空");
    }

    @Test
    @DisplayName("應回傳嵌入維度 - 當呼叫 getDimensions 時")
    void shouldReturnDimensions_whenGetDimensionsCalled() {
        // Given
        when(embeddingModel.dimensions()).thenReturn(768);

        // When
        int dimensions = embeddingService.getDimensions();

        // Then
        assertThat(dimensions).isEqualTo(768);
    }

    @Test
    @DisplayName("應轉換為 PostgreSQL vector 格式 - 當向量有效時")
    void shouldConvertToPostgresFormat_whenValidEmbedding() {
        // Given
        float[] embedding = {0.1f, 0.2f, 0.3f};

        // When
        String vectorString = embeddingService.toVectorString(embedding);

        // Then
        assertThat(vectorString).isEqualTo("[0.1,0.2,0.3]");
    }

    @Test
    @DisplayName("應轉換為 PostgreSQL vector 格式 - 當向量包含負數時")
    void shouldConvertToPostgresFormat_whenEmbeddingContainsNegativeValues() {
        // Given
        float[] embedding = {-0.5f, 0.0f, 0.5f};

        // When
        String vectorString = embeddingService.toVectorString(embedding);

        // Then
        assertThat(vectorString).isEqualTo("[-0.5,0.0,0.5]");
    }

    @Test
    @DisplayName("應正確轉換大型向量 - 當向量為 768 維時")
    void shouldConvertLargeVector_whenEmbeddingIs768Dimensions() {
        // Given
        float[] embedding = createMockEmbedding(768);

        // When
        String vectorString = embeddingService.toVectorString(embedding);

        // Then
        assertThat(vectorString).startsWith("[");
        assertThat(vectorString).endsWith("]");
        assertThat(vectorString.split(",")).hasSize(768);
    }

    @Test
    @DisplayName("應拋出例外 - 當向量為 null 時")
    void shouldThrowException_whenEmbeddingIsNull() {
        // When & Then
        assertThatThrownBy(() -> embeddingService.toVectorString(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("向量不得為空");
    }

    @Test
    @DisplayName("應拋出例外 - 當向量為空陣列時")
    void shouldThrowException_whenEmbeddingIsEmpty() {
        // When & Then
        assertThatThrownBy(() -> embeddingService.toVectorString(new float[0]))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("向量不得為空");
    }

    /**
     * 建立模擬的嵌入向量
     *
     * @param dimensions 維度
     * @return 填充隨機值的向量
     */
    private float[] createMockEmbedding(int dimensions) {
        float[] embedding = new float[dimensions];
        for (int i = 0; i < dimensions; i++) {
            embedding[i] = (float) (Math.random() * 2 - 1); // -1.0 到 1.0 之間
        }
        return embedding;
    }
}
