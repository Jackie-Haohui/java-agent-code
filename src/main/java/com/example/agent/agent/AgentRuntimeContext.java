package com.example.agent.agent;

import com.example.agent.memory.MemoryRecord;
import com.example.agent.rag.RetrievedChunk;
import com.example.agent.state.AgentSession;
import com.example.agent.tool.ToolDefinition;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 角色定位：运行时上下文 (Runtime Context)
 * 链路定位：在单次 Agent 思考循环中，作为“状态总线”不断流转和更新，最终喂给大模型。
 * 业务意图：聚合所有影响大模型决策的环境数据。
 */
public class AgentRuntimeContext {

    // 当前任务的会话状态，包含任务目标(Goal)、当前步数等基本信息
    private AgentSession session;

    // 长期记忆：该用户过去执行过的相似任务经验或偏好
    private List<MemoryRecord> memories = new ArrayList<>();

    // RAG 检索结果：从 Milvus 向量库里搜出来的知识文档切片
    private List<RetrievedChunk> retrievedChunks = new ArrayList<>();

    // 可用工具清单：告诉大模型当前系统里有哪些手牌（如查天气、执行Shell等）可以打
    private List<ToolDefinition> availableTools = new ArrayList<>();

    // 观察日志：非常关键！用于记录每一步调用工具后的结果（或报错），让大模型知道上一步干得怎么样
    private List<String> observations = new ArrayList<>();

    // 用户随请求传入的结构化输入参数（如文件路径、代码片段等）
    private Map<String, Object> input = new HashMap<>();

    // 最近连续调用记录，用于循环检测：key=工具名，value=连续调用次数
    private final Map<String, Integer> consecutiveCalls = new HashMap<>();

    // 上一步的决策类型，用于检测连续相同操作
    private String lastDecisionKey;

    // 关联的对话 ID
    private UUID conversationId;

    // 当前会话的工作空间路径
    private String workspacePath;

    // 知识库文档摘要列表（文档名 + 切片数），用于告诉大模型知识库里有什么
    private String knowledgeBaseSummary;

    public AgentSession getSession() {
        return session;
    }

    public void setSession(AgentSession session) {
        this.session = session;
    }

    public List<MemoryRecord> getMemories() {
        return memories;
    }

    public void setMemories(List<MemoryRecord> memories) {
        this.memories = memories;
    }

    public List<RetrievedChunk> getRetrievedChunks() {
        return retrievedChunks;
    }

    public void setRetrievedChunks(List<RetrievedChunk> retrievedChunks) {
        this.retrievedChunks = retrievedChunks;
    }

    public List<ToolDefinition> getAvailableTools() {
        return availableTools;
    }

    public void setAvailableTools(List<ToolDefinition> availableTools) {
        this.availableTools = availableTools;
    }

    public List<String> getObservations() {
        return observations;
    }

    // 辅助方法：向观察日志中追加新结果，供下一轮循环读取
    public void addObservation(String observation) {
        this.observations.add(observation);
    }

    public Map<String, Object> getInput() {
        return input;
    }

    public void setInput(Map<String, Object> input) {
        this.input = input;
    }

    public void trackDecision(String decisionKey) {
        if (decisionKey == null) {
            consecutiveCalls.clear();
            lastDecisionKey = null;
            return;
        }
        if (decisionKey.equals(lastDecisionKey)) {
            consecutiveCalls.merge(decisionKey, 1, Integer::sum);
        } else {
            consecutiveCalls.clear();
            consecutiveCalls.put(decisionKey, 1);
            lastDecisionKey = decisionKey;
        }
    }

    public boolean isStuck(int threshold) {
        return consecutiveCalls.values().stream().anyMatch(count -> count >= threshold);
    }

    public String buildStepWarning(int currentStep, int maxSteps) {
        int remaining = maxSteps - currentStep;
        return "当前步数: " + currentStep + "/" + maxSteps
                + " (剩余 " + remaining + " 步)。请在剩余步数内尽快完成任务。";
    }

    public UUID getConversationId() { return conversationId; }

    public void setConversationId(UUID conversationId) { this.conversationId = conversationId; }

    public String getWorkspacePath() { return workspacePath; }

    public void setWorkspacePath(String workspacePath) { this.workspacePath = workspacePath; }

    public String getKnowledgeBaseSummary() { return knowledgeBaseSummary; }

    public void setKnowledgeBaseSummary(String knowledgeBaseSummary) { this.knowledgeBaseSummary = knowledgeBaseSummary; }
}