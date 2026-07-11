package com.example.agent.workflow;

import java.util.List;

/**
 * 角色定位：任务执行蓝图 (Execution Blueprint)
 * 链路定位：由 PlannerService 生成，交给 WorkflowEngine 或 AgentLoop 执行。
 * 业务意图：承载针对当前用户目标的全局行动计划。
 */
public class TaskPlan {

    // 用户的全局原始目标
    private String goal;

    // 拆解出来的具体步骤列表
    private List<TaskStep> steps;

    public TaskPlan() {
    }

    public TaskPlan(String goal, List<TaskStep> steps) {
        this.goal = goal;
        this.steps = steps;
    }

    public String getGoal() {
        return goal;
    }

    public List<TaskStep> getSteps() {
        return steps;
    }
}