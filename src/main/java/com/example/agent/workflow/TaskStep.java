package com.example.agent.workflow;

/**
 * 角色定位：任务执行节点 (Execution DAG Node)
 * 链路定位：被 TaskPlan 聚合，最终交由 WorkflowEngine 逐个调度执行。
 * 业务意图：复杂任务拆解后的最小执行单元。支持定义依赖关系，为未来的“并发执行”或“重试机制”打下基础。
 */
public class TaskStep {

    // 步骤的顺序序号
    private int index;

    // 步骤的简短名称
    private String name;

    // 步骤的具体操作说明，这个 description 最终会作为 System Prompt 的一部分喂给负责执行的 LLM
    private String description;

    // 依赖的前置步骤名称（实现 DAG 有向无环图的核心字段）。如果为 null，说明可以立即执行
    private String dependsOn;

    // 状态位：该步骤是否已经成功执行完毕
    private boolean completed;

    public TaskStep() {
    }

    public TaskStep(int index, String name, String description, String dependsOn) {
        this.index = index;
        this.name = name;
        this.description = description;
        this.dependsOn = dependsOn;
        this.completed = false; // 初始化时默认为未完成
    }

    public int getIndex() {
        return index;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getDependsOn() {
        return dependsOn;
    }

    public boolean isCompleted() {
        return completed;
    }

    // 状态流转：当该步骤的工具调用或 LLM 生成成功结束后，调用此方法将其标记为完成
    public void markCompleted() {
        this.completed = true;
    }
}