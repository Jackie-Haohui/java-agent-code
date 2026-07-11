package com.example.agent.memory;

/**
 * 角色定位：记忆分类枚举 (Cognitive Memory Types)
 * 业务意图：为 Agent 的记忆进行分级，以便在组装 Prompt 时采取不同的召回策略。
 */
public enum MemoryType {
    // 短期记忆：通常指当前会话上下文（在此架构中更多由 AgentSession 和 StepLog 承担，但预留此类型做拓展）
    SHORT_TERM,
    // 长期记忆：跨会话的持久化知识（如用户的永久设定、高优规则）
    LONG_TERM,
    // 情景记忆：记录“过去发生过的具体事件”（例如：用户在某天要求查过某个bug，系统最终给出了某个方案）
    EPISODIC,
    // 语义记忆：经过提炼的客观知识与概念（例如：系统总结出“该用户倾向于使用 Python 而不是 Java”）
    SEMANTIC
}