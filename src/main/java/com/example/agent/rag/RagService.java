package com.example.agent.rag;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * 角色定位：RAG 检索门面服务 (RAG Facade)
 * 链路定位：被 AgentLoop 或 CodeSearchTool 调用。
 * 业务意图：对外屏蔽复杂的底层调用逻辑，把“文本转向量”和“去数据库查相似向量”这两步合并为一个直观的方法。
 */
@Service
public class RagService {

    private final EmbeddingService embeddingService;
    private final MilvusVectorStoreService vectorStoreService;

    public RagService(EmbeddingService embeddingService, MilvusVectorStoreService vectorStoreService) {
        this.embeddingService = embeddingService;
        this.vectorStoreService = vectorStoreService;
    }

    /**
     * 核心搜索流水线：
     * 1. 接收自然语言 query（如 "如何配置 Spring Boot 数据库"）
     * 2. 调用模型将 query 翻译成向量
     * 3. 去 Milvus 查最相似的 Top K 个文档片段
     */
    public List<RetrievedChunk> search(String query, int topK) {
        if (query == null || query.isBlank()) {
            return List.of();
        }

        List<Double> vector = embeddingService.embed(query);
        return vectorStoreService.search(vector, topK);
    }

    /**
     * 查询知识库中已入库的文档列表（去重，含切片数统计）。
     */
    public List<Map<String, Object>> listDocuments() {
        return vectorStoreService.listDocuments();
    }
}