package com.example.agent.agent;

/**
 * 角色定位：单步执行结果 (Step Snapshot)
 * 业务意图：记录某一个独立执行步骤结束后的瞬时状态快照。
 */
public class AgentStepResult {

    // 整个任务是否彻底完结
    private boolean finished;

    // 当前是否正处于挂起状态，等待人工审批 (HITL)
    private boolean waitingForHuman;

    // 如果任务完结，给用户的最终答复
    private String finalAnswer;

    public AgentStepResult(boolean finished, boolean waitingForHuman, String finalAnswer) {
        this.finished = finished;
        this.waitingForHuman = waitingForHuman;
        this.finalAnswer = finalAnswer;
    }

    public boolean isFinished() {
        return finished;
    }

    public boolean isWaitingForHuman() {
        return waitingForHuman;
    }

    public String getFinalAnswer() {
        return finalAnswer;
    }
}