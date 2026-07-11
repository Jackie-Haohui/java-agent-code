package com.example.agent.api.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 角色: 知识库入库请求 (Knowledge Ingestion Request)
 * 链路: Client -> DocumentController -> DocumentIngestionService -> Milvus
 * 作用: 接收外部上传的文档数据，用于后续的 RAG (增强检索) 向量化切分。
 */
public class DocumentUploadRequest {

    @NotBlank
    private String documentId;

    @NotBlank
    private String title;

    @NotBlank
    private String content;

    public String getDocumentId() { return documentId; }
    public String getTitle() { return title; }
    public String getContent() { return content; }
    public void setDocumentId(String documentId) { this.documentId = documentId; }
    public void setTitle(String title) { this.title = title; }
    public void setContent(String content) { this.content = content; }
}