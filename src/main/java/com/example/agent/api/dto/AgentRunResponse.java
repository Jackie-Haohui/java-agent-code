package com.example.agent.api.dto;

import java.util.List;
import java.util.UUID;

/**
 * 角色: Agent 任务结果快照 (Execution Snapshot)
 * 链路: AgentLoop -> AgentController -> Client
 * 作用: 返回大模型的最终回答，同时包含 SessionId 用于上下文追踪，以及 ToolCalls 数组用于审计 Agent 的思考与行动过程。
 */
public class AgentRunResponse {

    private UUID sessionId;
    private String status;
    private String finalAnswer;
    private List<ToolCallDto> toolCalls;
    private UUID pendingApprovalId;
    private String pendingToolName;
    private String pendingToolArguments;
    private UUID conversationId;

    public AgentRunResponse(UUID sessionId, String status, String finalAnswer, List<ToolCallDto> toolCalls) {
        this.sessionId = sessionId;
        this.status = status;
        this.finalAnswer = finalAnswer;
        this.toolCalls = toolCalls;
    }

    public AgentRunResponse(UUID sessionId, String status, String finalAnswer, List<ToolCallDto> toolCalls, UUID pendingApprovalId) {
        this(sessionId, status, finalAnswer, toolCalls);
        this.pendingApprovalId = pendingApprovalId;
    }

    public AgentRunResponse(UUID sessionId, String status, String finalAnswer, List<ToolCallDto> toolCalls, UUID pendingApprovalId, String pendingToolName, String pendingToolArguments) {
        this(sessionId, status, finalAnswer, toolCalls, pendingApprovalId);
        this.pendingToolName = pendingToolName;
        this.pendingToolArguments = pendingToolArguments;
    }

    public UUID getSessionId() { return sessionId; }
    public String getStatus() { return status; }
    public String getFinalAnswer() { return finalAnswer; }
    public List<ToolCallDto> getToolCalls() { return toolCalls; }
    public UUID getPendingApprovalId() { return pendingApprovalId; }
    public String getPendingToolName() { return pendingToolName; }
    public String getPendingToolArguments() { return pendingToolArguments; }
    public UUID getConversationId() { return conversationId; }
    public void setConversationId(UUID conversationId) { this.conversationId = conversationId; }
}