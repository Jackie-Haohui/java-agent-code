package com.example.agent.workflow;

import org.springframework.stereotype.Service;

/**
 * 角色定位：工作流编排引擎 (Workflow Orchestrator)
 * 业务意图：作为整个 Workflow 模块的对外门面 (Facade)。
 * 它负责统筹 PlannerService 生成计划，并在未来负责根据 dependsOn 关系，推进 TaskStep 的状态流转。
 */
@Service
public class WorkflowEngine {

    private final PlannerService plannerService;

    public WorkflowEngine(PlannerService plannerService) {
        this.plannerService = plannerService;
    }

    /**
     * 触发“计划并执行”模式的入口
     * 架构备注：当前版本仅封装了创建计划的逻辑。随着项目演进，这里可以扩展一个 while 循环，
     * 遍历 TaskPlan 中的未完成步骤，并将每一步的 description 喂给 AgentLoop 去单独攻克。
     */
    public TaskPlan planAndExecute(String goal) {
        // 第一步：生成宏观蓝图
        return plannerService.createPlan(goal);

        // 预留的第二步：遍历 plan.getSteps() 并驱动执行 (目前由上层的 AgentLoop 承担了大部分职责)
    }
}