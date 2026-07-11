package com.example.agent.api;

import com.example.agent.api.dto.DocumentUploadRequest;
import com.example.agent.rag.DocumentIngestionService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/**
 * 角色: RAG 知识管理网关 (Knowledge Ingestion Gateway)
 * 作用: 对外暴露长文本摄入接口。
 */
@RestController
@RequestMapping("/api/documents")
public class DocumentController {

    private final DocumentIngestionService documentIngestionService;

    public DocumentController(DocumentIngestionService documentIngestionService) {
        this.documentIngestionService = documentIngestionService;
    }

    /**
     * JSON 文本摄入：接收 documentId + title + content → 分块 → 向量化 → 写入 Milvus
     */
    @PostMapping("/ingest")
    public void ingest(@Valid @RequestBody DocumentUploadRequest request) {
        documentIngestionService.ingest(
                request.getDocumentId(),
                request.getTitle(),
                request.getContent()
        );
    }

    /**
     * 查询已入库的文档列表（从 Milvus 按 document_id 去重统计）
     */
    @GetMapping("/list")
    public java.util.List<java.util.Map<String, Object>> listDocuments() {
        return documentIngestionService.listDocuments();
    }

    /**
     * 删除已入库文档及其所有切片。
     */
    @DeleteMapping("/{documentId:.+}")
    public Map<String, Object> deleteDocument(@PathVariable String documentId) {
        long deleted = documentIngestionService.deleteDocument(documentId);
        return Map.of("success", deleted > 0, "documentId", documentId, "deletedChunks", deleted);
    }

    /**
     * 文件上传摄入：接收 MultipartFile → 读取文本内容 → 向量化 → 写入 Milvus
     */
    @PostMapping("/ingest/file")
    public Map<String, Object> ingestFile(@RequestParam("file") MultipartFile file) {
        try {
            String filename = file.getOriginalFilename();
            String docId = filename != null ? filename.replaceAll("[^a-zA-Z0-9._-]", "_") : "uploaded_doc";
            String content = new String(file.getBytes(), java.nio.charset.StandardCharsets.UTF_8);
            String title = filename != null ? filename : "Untitled";

            documentIngestionService.ingest(docId, title, content);

            int chunkCount = (int) Math.ceil(content.length() / 1200.0);
            return Map.of(
                    "success", true,
                    "documentId", docId,
                    "title", title,
                    "chunks", chunkCount,
                    "message", "已成功摄入 " + chunkCount + " 个文本块到向量数据库"
            );
        } catch (Exception e) {
            return Map.of("success", false, "message", "摄入失败: " + e.getMessage());
        }
    }
}