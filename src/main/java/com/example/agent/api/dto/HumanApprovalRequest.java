package com.example.agent.api.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

/**
 * 角色: 人机协作审批指令 (HITL Resolve Command)
 * 链路: Client -> AgentController -> HumanApprovalService -> AgentStateService
 * 作用: 当 Agent 触发高危工具被挂起时，用于接收管理员的“同意”或“拒绝”指令，从而恢复 Agent 执行流。
 */
public class HumanApprovalRequest {

    @NotNull
    private UUID approvalId;

    // 是否允许执行该敏感工具
    private boolean approved;

    // 拒绝或同意的补充理由，可反馈给大模型作为 Memory 供其纠正行为
    private String reason;

    public UUID getApprovalId() { return approvalId; }
    public boolean isApproved() { return approved; }
    public String getReason() { return reason; }
    public void setApprovalId(UUID approvalId) { this.approvalId = approvalId; }
    public void setApproved(boolean approved) { this.approved = approved; }
    public void setReason(String reason) { this.reason = reason; }
}