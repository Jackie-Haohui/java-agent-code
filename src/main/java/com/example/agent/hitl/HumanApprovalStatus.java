package com.example.agent.hitl;

/**
 * 角色定位：人工审批状态流转机 (Approval State Machine)
 * 业务意图：定义一条审批记录的生命周期。
 */
public enum HumanApprovalStatus {
    // 审批单已创建，当前 AgentSession 已被挂起，等待人类介入
    PENDING,
    // 人类管理员审核后同意了该高危操作，Agent 可在下次唤醒时继续执行该工具
    APPROVED,
    // 人类管理员拒绝了该操作，Agent 唤醒后将被告知“操作被拒绝”，需重新规划路线
    REJECTED
}