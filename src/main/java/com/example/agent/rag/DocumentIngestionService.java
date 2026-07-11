package com.example.agent.rag;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 角色定位：知识库摄入流水线 (Knowledge Ingestion Pipeline)
 * 链路定位：由 DocumentController (API) 触发，处理上传的文件数据。
 * 业务意图：大模型的上下文窗口有限，不能把一整本电子书直接塞进去。
 * 该服务负责将超长的文章“切片 (Chunking)”，然后把每个碎片分别向量化并存入库中。
 */
@Service
public class DocumentIngestionService {

    private final EmbeddingService embeddingService;
    private final MilvusVectorStoreService vectorStoreService;

    public DocumentIngestionService(
            EmbeddingService embeddingService,
            MilvusVectorStoreService vectorStoreService
    ) {
        this.embeddingService = embeddingService;
        this.vectorStoreService = vectorStoreService;
    }

    /**
     * 查询已入库的文档列表（委托给 MilvusVectorStoreService）。
     */
    public java.util.List<java.util.Map<String, Object>> listDocuments() {
        return vectorStoreService.listDocuments();
    }

    /**
     * 按 documentId 删除文档及其所有切片。
     */
    public long deleteDocument(String documentId) {
        return vectorStoreService.deleteByDocumentId(documentId);
    }

    /**
     * 核心摄入流水线：切片 -> 遍历向量化 -> 入库
     */
    public void ingest(String documentId, String title, String content) {
        if (content == null || content.isBlank()) {
            return;
        }

        List<String> chunks = split(content, 1200);

        for (String chunk : chunks) {
            if (chunk.isBlank()) {
                continue;
            }

            List<Double> vector = embeddingService.embed(chunk);

            vectorStoreService.insert(
                    UUID.randomUUID().toString(),
                    documentId,
                    title,
                    chunk,
                    vector
            );
        }
    }

    /**
     * 内部辅助方法：基础定长文本切分算法 (Fixed-size Chunking)
     * 架构备注：当前是简单的按字符数硬切分。在后续高级迭代中，可替换为基于句号或语义的智能切分 (Semantic Chunking)，
     * 并加入 Overlap (内容重叠) 防止关键信息在切分边界被截断。
     */
    private List<String> split(String content, int size) {
        List<String> chunks = new ArrayList<>();
        String normalized = content.replace("\r\n", "\n").replace('\r', '\n');

        int index = 0;
        while (index < normalized.length()) {
            int end = Math.min(index + size, normalized.length());
            chunks.add(normalized.substring(index, end));
            index = end;
        }

        return chunks;
    }
}