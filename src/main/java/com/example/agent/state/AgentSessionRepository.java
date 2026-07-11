package com.example.agent.state;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AgentSessionRepository extends JpaRepository<AgentSession, UUID> {

    List<AgentSession> findByUserIdOrderByCreatedAtDesc(String userId);
}
