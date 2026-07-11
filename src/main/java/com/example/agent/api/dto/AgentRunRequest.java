package com.example.agent.api.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.Map;
import java.util.UUID;

/**
 * 角色: Agent 任务触发载体 (Task Input)
 * 链路: Client -> AgentController -> AgentLoop
 * 作用: 封装外部用户的初始请求，包含用户标识、核心目标(Prompt)以及可能的结构化输入(Input)。
 */
public class AgentRunRequest {

    @NotBlank
    private String userId;

    @NotBlank
    private String goal;

    // 支持透传额外的结构化参数供 AgentPromptBuilder 渲染上下文
    private Map<String, Object> input;

    // 关联的对话 ID
    private UUID conversationId;

    // 工作空间路径，覆盖对话级别设置
    private String workspacePath;

    public String getUserId() { return userId; }
    public String getGoal() { return goal; }
    public Map<String, Object> getInput() { return input; }
    public UUID getConversationId() { return conversationId; }
    public String getWorkspacePath() { return workspacePath; }
    public void setUserId(String userId) { this.userId = userId; }
    public void setGoal(String goal) { this.goal = goal; }
    public void setInput(Map<String, Object> input) { this.input = input; }
    public void setConversationId(UUID conversationId) { this.conversationId = conversationId; }
    public void setWorkspacePath(String workspacePath) { this.workspacePath = workspacePath; }
}