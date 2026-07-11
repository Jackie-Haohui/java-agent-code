package com.example.agent.hitl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * 角色定位：人机协同审批总线 (HITL Coordinator Service)
 * 链路定位：被 AgentLoop（创建工单）和 AgentController（处理工单）两端调用。
 * 业务意图：封装审批流程的业务事务边界，解耦底层的持久化细节。
 */
@Service
public class HumanApprovalService {

    private final HumanApprovalRepository repository;

    public HumanApprovalService(HumanApprovalRepository repository) {
        this.repository = repository;
    }

    /**
     * 核心操作 A：触发人机拦截 (Trigger HITL)
     * 链路：AgentLoop 发现敏感工具调用 -> 挂起当前线程 -> 调用此方法持久化待办工单。
     */
    @Transactional
    public HumanApproval createApproval(UUID sessionId, String toolName, String toolArguments, String reason) {
        return repository.save(
                HumanApproval.create(sessionId, toolName, toolArguments, reason)
        );
    }

    /**
     * 核心操作 B：人类决断反馈 (Resolve HITL)
     * 链路：管理员在前端 UI 点击同意/拒绝 -> 发送 HTTP 请求到 AgentController -> 调用此方法完成工单状态扭转。
     * 架构备注：当前实现仅修改了单据状态。在一个完整的全自动异步 Agent 架构中，这里通常还会发布一个 Spring Event 或 Message Queue 消息，用来异步唤醒那个被挂起的 AgentSession 恢复运行。
     */
    @Transactional
    public void resolveApproval(UUID approvalId, boolean approved, String reason) {
        // 1. 查找对应的挂起工单
        HumanApproval approval = repository.findById(approvalId)
                .orElseThrow(() -> new IllegalArgumentException("Approval not found: " + approvalId));

        // 2. 根据管理员的决策，触发领域模型的状态变更
        if (approved) {
            approval.approve(reason);
        } else {
            approval.reject(reason);
        }

        // 3. 落库保存
        repository.save(approval);
    }
}