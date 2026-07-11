package com.example.agent.agent;

/**
 * 决策指令枚举 (Decision Router)
 * 定义了大模型思考后可能做出的所有动作类型，用于指导 AgentLoop 的下一步走向。
 */
public enum AgentDecisionType {
    FINAL_ANSWER,         // 已经得出结论，结束当前任务
    TOOL_CALL,            // 需要调用外部工具
    RAG_SEARCH,           // 现有信息不足，需要检索知识库
    NEED_HUMAN_APPROVAL,  // 遇到敏感操作或不确定边界，主动请求人类介入
    REPLAN,               // 执行受阻，需要重新规划步骤
    ERROR                 // 模型输出格式错误或解析异常
}