package com.example.agent.workflow;

/**
 * 角色定位：Agent 工作流模式枚举 (Agentic Workflow Patterns)
 * 业务意图：定义了业界最先进的几种大模型复合编排模式。这些模式决定了 Agent 面对复杂任务时采取何种策略。
 * （注：这里的定义非常契合 Anthropic 和 Andrew Ng 提出的大模型设计模式）
 */
public enum WorkflowType {
    // 提示词链：将前一个 LLM 的输出直接作为下一个 LLM 的输入，流水线作业
    PROMPT_CHAINING,
    // 意图路由：前端一个网关 LLM 负责识别意图，然后分发给后端不同领域的专业小 Agent
    ROUTING,
    // 并行执行：将任务拆分后，同时调用多个工具或 LLM 处理，最后由一个聚合节点合并结果（Map-Reduce）
    PARALLELIZATION,
    // 调度者-工作者模式：一个主干 LLM 负责发号施令，多个子 LLM 负责干脏活累活
    ORCHESTRATOR_WORKERS,
    // 评估者-优化者模式：一个 LLM 负责生成内容，另一个专门负责挑刺和打分，循环优化直到达标
    EVALUATOR_OPTIMIZER,
    // 计划与执行模式：面对长线任务，先让 LLM 拆解出 DAG（有向无环图）任务书，然后一步步线性或并行执行
    PLAN_AND_EXECUTE
}