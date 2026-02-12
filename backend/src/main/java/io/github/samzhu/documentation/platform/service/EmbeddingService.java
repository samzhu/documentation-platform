package io.github.samzhu.documentation.platform.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 嵌入服務
 * <p>
 * 封裝 EmbeddingModel 的呼叫，提供文字向量化功能。
 * 支援單一文字和批次處理。
 * </p>
 */
@Service
public class EmbeddingService {

    private static final Logger log = LoggerFactory.getLogger(EmbeddingService.class);

    private final EmbeddingModel embeddingModel;

    public EmbeddingService(EmbeddingModel embeddingModel) {
        this.embeddingModel = embeddingModel;
    }

    /**
     * 將文字轉換為向量嵌入
     *
     * @param text 要轉換的文字
     * @return 768 維度的浮點數陣列
     */
    public float[] embed(String text) {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("文字不得為空");
        }
        log.debug("呼叫嵌入模型，文字長度: {} 字元", text.length());
        try {
            float[] result = embeddingModel.embed(text);
            log.debug("嵌入完成，向量維度: {}", result.length);
            return result;
        } catch (Exception e) {
            log.error("呼叫嵌入模型失敗，文字長度: {} 字元", text.length(), e);
            throw e;
        }
    }

    /**
     * 批次轉換文字為向量嵌入
     *
     * @param texts 要轉換的文字列表
     * @return 向量嵌入列表
     */
    public List<float[]> embedBatch(List<String> texts) {
        if (texts == null || texts.isEmpty()) {
            throw new IllegalArgumentException("文字列表不得為空");
        }
        log.info("批次呼叫嵌入模型，數量: {}", texts.size());
        try {
            List<float[]> results = embeddingModel.embed(texts);
            log.info("批次嵌入完成，數量: {}", results.size());
            return results;
        } catch (Exception e) {
            log.error("批次呼叫嵌入模型失敗，數量: {}", texts.size(), e);
            throw e;
        }
    }

    /**
     * 取得嵌入維度
     *
     * @return 向量維度
     */
    public int getDimensions() {
        return embeddingModel.dimensions();
    }

    /**
     * 將 float[] 轉換為 PostgreSQL vector 字串格式
     *
     * @param embedding 向量陣列
     * @return PostgreSQL vector 格式字串，如 "[0.1, 0.2, ...]"
     */
    public String toVectorString(float[] embedding) {
        if (embedding == null || embedding.length == 0) {
            throw new IllegalArgumentException("向量不得為空");
        }
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
