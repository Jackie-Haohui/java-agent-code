package com.example.agent.memory;

import com.example.agent.llm.LlmClient;
import com.example.agent.rag.EmbeddingService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class MemoryService {

    private static final Logger log = LoggerFactory.getLogger(MemoryService.class);

    private final MemoryRepository memoryRepository;
    private final EmbeddingService embeddingService;
    private final LlmClient llmClient;
    private final ObjectMapper objectMapper;

    public MemoryService(MemoryRepository memoryRepository,
                         EmbeddingService embeddingService,
                         LlmClient llmClient,
                         ObjectMapper objectMapper) {
        this.memoryRepository = memoryRepository;
        this.embeddingService = embeddingService;
        this.llmClient = llmClient;
        this.objectMapper = objectMapper;
    }

    /**
     * 直接保存用户原始问题（不做 LLM 总结），确保对话历史可追溯。
     */
    @Transactional
    public void saveUserMessage(String userId, String message) {
        if (message == null || message.isBlank()) {
            return;
        }
        String truncated = message.length() > 500 ? message.substring(0, 500) : message;
        MemoryRecord record = MemoryRecord.create(userId, MemoryType.EPISODIC, "用户提问：" + truncated, 7);
        try {
            List<Double> vec = embeddingService.embed(truncated);
            record.setEmbedding(objectMapper.writeValueAsString(vec));
        } catch (Exception e) {
            log.warn("用户消息向量化失败: {}", e.getMessage());
        }
        memoryRepository.save(record);
    }

    /**
     * 语义记忆召回：用 query 向量化后与所有记忆做余弦相似度匹配。
     * 综合得分 = 语义相似度(70%) + 重要性(30%)。
     */
    public List<MemoryRecord> findRelevantMemories(String userId, String query) {
        List<MemoryRecord> all = memoryRepository.findByUserId(userId);
        if (all.isEmpty()) {
            return List.of();
        }

        List<Double> queryEmbedding;
        try {
            queryEmbedding = embeddingService.embed(query);
        } catch (Exception e) {
            log.warn("查询向量化失败，降级为重要性排序: {}", e.getMessage());
            return all.stream()
                    .sorted((a, b) -> Integer.compare(b.getImportance(), a.getImportance()))
                    .limit(10)
                    .toList();
        }

        record ScoredMemory(MemoryRecord memory, double score) {}

        // 对"用户提问"类型的记忆加权，让对话历史类问题更容易命中
        List<ScoredMemory> scored = new ArrayList<>();
        for (MemoryRecord m : all) {
            double similarity = 0.0;
            if (m.getEmbedding() != null && !m.getEmbedding().isBlank()) {
                try {
                    List<Double> memEmbedding = deserializeEmbedding(m.getEmbedding());
                    similarity = cosineSimilarity(queryEmbedding, memEmbedding);
                } catch (Exception e) {
                    // embedding 数据损坏，忽略
                }
            }
            // 用户原始提问记忆：提高重要性权重（40% 重要性 + 60% 相似度），让对话回溯更容易命中
            boolean isUserQuestion = m.getContent() != null && m.getContent().startsWith("用户提问：");
            double importanceWeight = isUserQuestion ? 0.4 : 0.3;
            double combined = similarity * (1.0 - importanceWeight) + (m.getImportance() / 10.0) * importanceWeight;
            scored.add(new ScoredMemory(m, combined));
        }

        scored.sort((a, b) -> Double.compare(b.score, a.score));
        return scored.stream()
                .limit(10)
                .map(ScoredMemory::memory)
                .toList();
    }

    /**
     * 记忆反思与沉淀：用 LLM 提炼摘要、自动评估重要性，向量化后持久化。
     */
    @Transactional
    public void extractAndSaveMemories(String userId, String goal, String finalAnswer) {
        if (finalAnswer == null || finalAnswer.length() < 20) {
            return;
        }

        String truncated = finalAnswer.length() > 800 ? finalAnswer.substring(0, 800) : finalAnswer;

        String summary;
        int importance;
        try {
            Map<String, Object> result = summarizeAndRate(goal, truncated);
            summary = (String) result.getOrDefault("summary", truncated.substring(0, Math.min(truncated.length(), 100)));
            importance = ((Number) result.getOrDefault("importance", 3)).intValue();
            importance = Math.max(1, Math.min(10, importance));
        } catch (Exception e) {
            log.warn("LLM 总结/评估失败，使用硬截断兜底: {}", e.getMessage());
            summary = truncated.length() <= 100 ? truncated : truncated.substring(0, 100);
            importance = 3;
        }

        String content = "目标：" + goal + "。结果：" + summary;

        String embeddingJson = null;
        try {
            List<Double> vec = embeddingService.embed(summary);
            embeddingJson = objectMapper.writeValueAsString(vec);
        } catch (Exception e) {
            log.warn("记忆向量化失败，仅保存文本: {}", e.getMessage());
        }

        MemoryRecord record = MemoryRecord.create(userId, MemoryType.EPISODIC, content, importance);
        record.setEmbedding(embeddingJson);
        memoryRepository.save(record);
    }

    /**
     * 调用 LLM 对执行记录进行总结 + 重要性评分，返回 JSON。
     */
    private Map<String, Object> summarizeAndRate(String goal, String answer) throws Exception {
        String prompt = """
                请对以下 Agent 执行记录进行总结和重要性评估，返回纯 JSON（不要 markdown 代码块）：

                用户目标：%s
                Agent 执行结果摘要：%s

                返回格式：
                {"summary": "一句话总结核心经验和关键结论（不超过100字）", "importance": 1-10 的整数}

                重要性评估标准：
                1-3: 日常普通操作
                4-6: 包含可复用的知识或模式
                7-8: 解决了复杂问题或有重要发现
                9-10: 关键经验教训，必须牢记
                """.formatted(goal, answer);

        String response = llmClient.complete(prompt).content();
        String json = extractJson(response);
        return objectMapper.readValue(json, new TypeReference<>() {});
    }

    private String extractJson(String llmOutput) {
        String trimmed = llmOutput.trim();
        int start = trimmed.indexOf('{');
        int end = trimmed.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return trimmed.substring(start, end + 1);
        }
        return trimmed;
    }

    private double cosineSimilarity(List<Double> a, List<Double> b) {
        if (a.size() != b.size()) return 0.0;
        double dot = 0.0, normA = 0.0, normB = 0.0;
        for (int i = 0; i < a.size(); i++) {
            double va = a.get(i);
            double vb = b.get(i);
            dot += va * vb;
            normA += va * va;
            normB += vb * vb;
        }
        if (normA == 0.0 || normB == 0.0) return 0.0;
        return dot / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    private List<Double> deserializeEmbedding(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (Exception e) {
            throw new RuntimeException("Failed to deserialize embedding", e);
        }
    }
}
