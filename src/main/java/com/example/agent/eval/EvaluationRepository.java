package com.example.agent.eval;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface EvaluationRepository extends JpaRepository<EvaluationRecord, UUID> {

    List<EvaluationRecord> findBySessionIdIn(List<UUID> sessionIds);

    @Query(value = "SELECT COALESCE(AVG(e.total_steps), 0) FROM evaluation_record e JOIN agent_session s ON e.session_id = s.id WHERE s.user_id = :userId", nativeQuery = true)
    double avgStepsByUserId(@Param("userId") String userId);

    @Query(value = "SELECT COALESCE(AVG(e.latency_ms), 0) FROM evaluation_record e JOIN agent_session s ON e.session_id = s.id WHERE s.user_id = :userId", nativeQuery = true)
    double avgLatencyByUserId(@Param("userId") String userId);

    @Query(value = "SELECT COALESCE(CAST(SUM(CASE WHEN e.task_success THEN 1 ELSE 0 END) AS double) / NULLIF(COUNT(*), 0), 0) FROM evaluation_record e JOIN agent_session s ON e.session_id = s.id WHERE s.user_id = :userId", nativeQuery = true)
    double successRateByUserId(@Param("userId") String userId);

    @Query(value = "SELECT COUNT(*) FROM evaluation_record e JOIN agent_session s ON e.session_id = s.id WHERE s.user_id = :userId", nativeQuery = true)
    long countByUserId(@Param("userId") String userId);
}