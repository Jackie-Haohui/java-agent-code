package com.example.agent.rag;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 角色定位：文本向量化引擎 (Text Embedder)
 * 链路定位：被 DocumentIngestionService (存数据时) 和 RagService (查数据时) 强依赖。
 * 业务意图：将人类可读的自然语言文本，转化为计算机和向量数据库能理解的高维浮点数数组（如 1536 维的向量）。
 */
@Service
public class EmbeddingService {

    private static final int MAX_EMBEDDING_INPUT_LENGTH = 2000;

    private final EmbeddingModel embeddingModel;

    public EmbeddingService(EmbeddingModel embeddingModel) {
        this.embeddingModel = embeddingModel;
    }

    public List<Double> embed(String text) {
        String safeText = normalizeForEmbedding(text);
        float[] vector = embeddingModel.embed(safeText);
        return toDoubleList(vector);
    }

    private String normalizeForEmbedding(String text) {
        if (text == null) {
            return " ";
        }

        String normalized = text.replaceAll("\\s+", " ").trim();
        if (normalized.isEmpty()) {
            return " ";
        }

        if (normalized.length() <= MAX_EMBEDDING_INPUT_LENGTH) {
            return normalized;
        }

        return normalized.substring(0, MAX_EMBEDDING_INPUT_LENGTH);
    }

    private List<Double> toDoubleList(float[] vector) {
        return java.util.stream.IntStream.range(0, vector.length)
                .mapToObj(i -> (double) vector[i])
                .toList();
    }
}
