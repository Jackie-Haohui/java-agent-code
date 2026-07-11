package com.example.agent.hitl;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * 角色定位：人工审批单数据访问层 (Approval Ticket DAO)
 * 业务意图：提供基于 JPA 的底层持久化操作，支持按会话和状态查询审批单。
 */
public interface HumanApprovalRepository extends JpaRepository<HumanApproval, UUID> {

    HumanApproval findBySessionIdAndStatus(UUID sessionId, HumanApprovalStatus status);

    Optional<HumanApproval> findTopBySessionIdOrderByCreatedAtDesc(UUID sessionId);
}