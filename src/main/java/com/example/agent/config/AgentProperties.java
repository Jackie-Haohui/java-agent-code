package com.example.agent.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Agent 核心配置类 (System Guardrails)
 * 负责将 application.yml 中的 agent.* 配置映射为 Spring Bean。
 * 作为全局参数，为 Agent 的主循环、工具执行和检索模块设定运行边界。
 */
@Component
@ConfigurationProperties(prefix = "agent")
public class AgentProperties {

    // 1. 循环限制：Agent（如 ReAct 模式）单次任务最大思考与执行步数，防止死循环
    private int maxSteps = 12;

    // 2. 超时保护：工具（如 Shell/网络请求）执行的最大超时时长，防止阻塞主线程
    private long toolTimeoutMs = 15000;

    // 3. 检索控制：RAG 召回时，最多向大模型 Prompt 中注入多少条相关文档切片
    private int ragTopK = 6;

    // 4. 安全开关：触发高危工具时，是否挂起 Agent 并强制要求人类审批 (HITL)
    private boolean requireApprovalForSensitiveTools = true;

    // 5. 工作空间路径：Agent 文件操作的默认根目录
    private String workspacePath = "workspace";

    // 6. 自动提交：Agent 修改文件后是否自动 git commit
    private boolean autoCommit = false;

    // ==========================================
    // Getters and Setters
    // ==========================================

    public int getMaxSteps() {
        return maxSteps;
    }

    public void setMaxSteps(int maxSteps) {
        this.maxSteps = maxSteps;
    }

    public long getToolTimeoutMs() {
        return toolTimeoutMs;
    }

    public void setToolTimeoutMs(long toolTimeoutMs) {
        this.toolTimeoutMs = toolTimeoutMs;
    }

    public int getRagTopK() {
        return ragTopK;
    }

    public void setRagTopK(int ragTopK) {
        this.ragTopK = ragTopK;
    }

    public boolean isRequireApprovalForSensitiveTools() {
        return requireApprovalForSensitiveTools;
    }

    public void setRequireApprovalForSensitiveTools(boolean requireApprovalForSensitiveTools) {
        this.requireApprovalForSensitiveTools = requireApprovalForSensitiveTools;
    }

    public String getWorkspacePath() {
        return workspacePath;
    }

    public void setWorkspacePath(String workspacePath) {
        this.workspacePath = workspacePath;
    }

    public boolean isAutoCommit() {
        return autoCommit;
    }

    public void setAutoCommit(boolean autoCommit) {
        this.autoCommit = autoCommit;
    }
}