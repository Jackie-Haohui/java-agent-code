package com.example.agent.hitl;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 角色定位：人工审批单据实体 (Approval Ticket Entity)
 * 链路定位：被 AgentLoop 创建 -> 被前端 UI 读取 -> 被人类管理员(通过 AgentController) 处理。
 * 业务意图：持久化记录“是谁、在什么任务中、试图调用什么敏感工具、传了什么参数”。
 */
@Entity
@Table(name = "human_approval")
public class HumanApproval {

    @Id
    private UUID id;

    // 关联的 Agent 任务会话 ID，方便在审批通过/拒绝后，系统知道该去唤醒哪个挂起的 Session
    private UUID sessionId;

    // 大模型意图调用的敏感工具名称（如 "shell_command"）
    private String toolName;

    // 大模型意图传递给该工具的具体参数（以 JSON 字符串形式存储）
    @Column(columnDefinition = "TEXT")
    private String toolArguments;

    // 当前这笔审批单的状态
    @Enumerated(EnumType.STRING)
    private HumanApprovalStatus status;

    // 创建审批单时的理由，或者管理员在审批通过/拒绝时填写的反馈原因。
    // 这点极其重要：管理员填写的“拒绝理由”最终会作为 Observation 喂给大模型，纠正它的错误思维！
    @Column(columnDefinition = "TEXT")
    private String reason;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    /**
     * 工厂方法：在 AgentLoop 的 TOOL_CALL 分支中，如果权限服务报告需要审批，则调用此方法生成工单。
     */
    public static HumanApproval create(UUID sessionId, String toolName, String toolArguments, String reason) {
        HumanApproval approval = new HumanApproval();
        approval.id = UUID.randomUUID();
        approval.sessionId = sessionId;
        approval.toolName = toolName;
        approval.toolArguments = toolArguments;
        approval.status = HumanApprovalStatus.PENDING; // 初始状态挂起待办
        approval.reason = reason;
        approval.createdAt = LocalDateTime.now();
        approval.updatedAt = LocalDateTime.now();
        return approval;
    }

    /**
     * 领域行为方法：管理员同意操作
     * @param reason 同意时的附言（可选）
     */
    public void approve(String reason) {
        this.status = HumanApprovalStatus.APPROVED;
        this.reason = reason;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 领域行为方法：管理员拒绝操作
     * @param reason 拒绝原因（必填，以指导大模型后续行为）
     */
    public void reject(String reason) {
        this.status = HumanApprovalStatus.REJECTED;
        this.reason = reason;
        this.updatedAt = LocalDateTime.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getSessionId() {
        return sessionId;
    }

    public String getToolName() {
        return toolName;
    }

    public String getToolArguments() {
        return toolArguments;
    }

    public HumanApprovalStatus getStatus() {
        return status;
    }

    public String getReason() {
        return reason;
    }
}