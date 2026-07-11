package com.example.agent.eval;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class AgentEvaluationService {

    private final EvaluationRepository repository;

    public AgentEvaluationService(EvaluationRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public void recordEvaluation(
            UUID sessionId,
            boolean taskSuccess,
            double toolCallAccuracy,
            double stepReasonability,
            long latencyMs,
            int totalSteps,
            String failureReason
    ) {
        repository.save(
                EvaluationRecord.create(
                        sessionId,
                        taskSuccess,
                        toolCallAccuracy,
                        stepReasonability,
                        latencyMs,
                        totalSteps,
                        failureReason
                )
        );
    }

    public EvaluationStats getStats(String userId) {
        long totalSessions = repository.countByUserId(userId);
        if (totalSessions == 0) {
            return new EvaluationStats(0, 0, 0, 0);
        }
        double successRate = repository.successRateByUserId(userId);
        double avgSteps = repository.avgStepsByUserId(userId);
        double avgLatencyMs = repository.avgLatencyByUserId(userId);
        return new EvaluationStats(totalSessions, successRate, avgSteps, avgLatencyMs);
    }
}