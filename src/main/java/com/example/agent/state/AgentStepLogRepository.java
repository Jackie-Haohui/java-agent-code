package com.example.agent.state;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AgentStepLogRepository extends JpaRepository<AgentStepLog, UUID> {

    List<AgentStepLog> findBySessionIdOrderByStepIndexAsc(UUID sessionId);
}