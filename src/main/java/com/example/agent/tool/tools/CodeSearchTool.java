package com.example.agent.tool.tools;

import com.example.agent.rag.RagService;
import com.example.agent.rag.RetrievedChunk;
import com.example.agent.tool.AgentTool;
import com.example.agent.tool.ToolDefinition;
import com.example.agent.tool.ToolExecutionResult;
import com.example.agent.tool.ToolParameterSchema;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 角色定位：外挂知识库检索引擎 (RAG Tool)
 * 业务意图：当大模型主动判定自己需要查询本地私有代码或文档时，会调用此工具，发起向 Milvus 的向量搜索。
 */
@Component
public class CodeSearchTool implements AgentTool {

    private final RagService ragService;
    private final ObjectMapper objectMapper;

    public CodeSearchTool(RagService ragService, ObjectMapper objectMapper) {
        this.ragService = ragService;
        this.objectMapper = objectMapper;
    }

    @Override
    public ToolDefinition definition() {
        return new ToolDefinition(
                "code_search",
                "在向量数据库中检索相关代码、文档和知识片段。",
                new ToolParameterSchema(
                        "object",
                        Map.of(
                                "query", Map.of("type", "string", "description", "检索关键词或问题"),
                                "topK", Map.of("type", "integer", "description", "返回数量")
                        ),
                        new String[]{"query"} // 要求大模型必须提供 query 参数
                ),
                false // 检索动作无破坏性，不需要人工审批
        );
    }

    @Override
    public ToolExecutionResult execute(Map<String, Object> arguments) {
        long start = System.currentTimeMillis();
        String query = String.valueOf(arguments.get("query"));
        // 容错处理：如果大模型没传 topK，默认给 5
        int topK = arguments.get("topK") == null ? 5 : Integer.parseInt(String.valueOf(arguments.get("topK")));

        try {
            // 调用底层的向量数据库服务进行近似查询
            List<RetrievedChunk> chunks = ragService.search(query, topK);
            // 把召回的片段列表序列化为 JSON 文本喂给大模型
            return ToolExecutionResult.success(
                    definition().name(),
                    objectMapper.writeValueAsString(chunks),
                    System.currentTimeMillis() - start
            );
        } catch (Exception e) {
            return ToolExecutionResult.failure(
                    definition().name(),
                    "CODE_SEARCH_FAILED",
                    e.getMessage(),
                    System.currentTimeMillis() - start
            );
        }
    }
}