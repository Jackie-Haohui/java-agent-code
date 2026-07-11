package com.example.agent.eval;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "evaluation_record")
public class EvaluationRecord {

    @Id
    private UUID id;

    private UUID sessionId;

    private Boolean taskSuccess;

    private Double toolCallAccuracy;

    private Double stepReasonability;

    private Long latencyMs;

    private Integer totalSteps;

    @Column(columnDefinition = "TEXT")
    private String failureReason;

    private LocalDateTime createdAt;

    public static EvaluationRecord create(
            UUID sessionId,
            Boolean taskSuccess,
            Double toolCallAccuracy,
            Double stepReasonability,
            Long latencyMs,
            Integer totalSteps,
            String failureReason
    ) {
        EvaluationRecord record = new EvaluationRecord();
        record.id = UUID.randomUUID();
        record.sessionId = sessionId;
        record.taskSuccess = taskSuccess;
        record.toolCallAccuracy = toolCallAccuracy;
        record.stepReasonability = stepReasonability;
        record.latencyMs = latencyMs;
        record.totalSteps = totalSteps;
        record.failureReason = failureReason;
        record.createdAt = LocalDateTime.now();
        return record;
    }
}