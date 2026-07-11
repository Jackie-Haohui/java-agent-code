package com.example.agent.workflow;

import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 角色定位：规划师引擎 (The Planner / System 2 Thinking)
 * 链路定位：在 Agent 真正开始行动（调用工具）前被调用。
 * 业务意图：负责将模糊的、宏大的目标，拆解为切实可行的 TaskPlan。
 */
@Service
public class PlannerService {

    /**
     * 核心规划方法
     * 架构备注：当前初版代码这里硬编码了一个标准的 Agent 执行流（模板模式）。
     * 在未来的进阶迭代中，这里应当调用 LlmClient，利用特定的 Planner Prompt
     * 让大模型针对传入的 goal 动态生成 JSON 格式的步骤列表，从而实现真正的 AutoGPT 级别的动态规划。
     */
    public TaskPlan createPlan(String goal) {
        return new TaskPlan(
                goal,
                List.of(
                        // 步骤 1：分析意图，不依赖任何前置条件 (dependsOn 为 null)
                        new TaskStep(1, "理解目标", "分析用户目标、约束和期望输出", null),
                        // 步骤 2：获取上下文，必须在理解目标后进行
                        new TaskStep(2, "检索上下文", "从 RAG 和记忆中查找相关信息", "理解目标"),
                        // 步骤 3：工具调度策略
                        new TaskStep(3, "选择工具", "判断是否需要调用工具", "检索上下文"),
                        // 步骤 4：物理执行与观察
                        new TaskStep(4, "执行并观察", "执行工具并分析返回结果", "选择工具"),
                        // 步骤 5：归纳总结
                        new TaskStep(5, "生成答案", "整合上下文和工具结果形成最终输出", "执行并观察")
                )
        );
    }
}