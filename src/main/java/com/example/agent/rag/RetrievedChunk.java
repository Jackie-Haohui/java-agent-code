package com.example.agent.rag;

/**
 * 角色定位：检索召回片段载体 (Retrieval Result Payload)
 * 链路定位：MilvusVectorStoreService -> RagService -> AgentPromptBuilder
 * 业务意图：封装从向量数据库中搜索出来的最相似的文档切片，最终拼接到大模型的 Prompt 中作为上下文。
 */
public record RetrievedChunk(
        // 文档所属的源文件 ID（比如来源于哪份 PDF 或 Markdown）
        String documentId,
        // 文档的标题
        String title,
        // 切片后的纯文本内容（真正喂给大模型的知识）
        String content,
        // 相似度得分：数值越说明该切片与用户的提问越相关
        double score
) {
}