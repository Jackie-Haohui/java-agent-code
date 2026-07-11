package com.example.agent.agent;

import java.util.Map;

/**
 * 角色定位：决策指令载体 (Decision Payload)
 * 链路定位：大模型 JSON 输出 -> Jackson 解析 -> 转换为此对象交由 AgentLoop 执行
 * 业务意图：标准化大模型的输出结构。
 */
public class AgentDecision {

    // 大模型决定的下一步走向（枚举）
    private AgentDecisionType type;

    // 思考过程的文字说明，如果是 FINAL_ANSWER，这里就是给用户的最终回答；如果是 RAG_SEARCH，这里就是搜索关键词
    private String content;

    // 目标工具名称 (仅当 type 为 TOOL_CALL 时有效)
    private String toolName;

    // 目标工具的入参，以 K-V 形式存储，后续交给 ToolExecutor 执行 (仅当 type 为 TOOL_CALL 时有效)
    private Map<String, Object> toolArguments;

    public AgentDecision() {
    }

    public AgentDecision(AgentDecisionType type, String content, String toolName, Map<String, Object> toolArguments) {
        this.type = type;
        this.content = content;
        this.toolName = toolName;
        this.toolArguments = toolArguments;
    }

    public AgentDecisionType getType() {
        return type;
    }

    public String getContent() {
        return content;
    }

    public String getToolName() {
        return toolName;
    }

    public Map<String, Object> getToolArguments() {
        return toolArguments;
    }

    public void setType(AgentDecisionType type) {
        this.type = type;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public void setToolName(String toolName) {
        this.toolName = toolName;
    }

    public void setToolArguments(Map<String, Object> toolArguments) {
        this.toolArguments = toolArguments;
    }
}