package com.example.agent.state;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "agent_step_log")
public class AgentStepLog {

    @Id
    private UUID id;

    private UUID sessionId;

    private int stepIndex;

    private String decisionType;

    @Column(columnDefinition = "TEXT")
    private String modelOutput;

    private String toolName;

    @Column(columnDefinition = "TEXT")
    private String toolArguments;

    @Column(columnDefinition = "TEXT")
    private String toolResult;

    @Column(columnDefinition = "TEXT")
    private String errorMessage;

    private LocalDateTime createdAt;

    public static AgentStepLog create(
            UUID sessionId,
            int stepIndex,
            String decisionType,
            String modelOutput,
            String toolName,
            String toolArguments,
            String toolResult,
            String errorMessage
    ) {
        AgentStepLog log = new AgentStepLog();
        log.id = UUID.randomUUID();
        log.sessionId = sessionId;
        log.stepIndex = stepIndex;
        log.decisionType = decisionType;
        log.modelOutput = modelOutput;
        log.toolName = toolName;
        log.toolArguments = toolArguments;
        log.toolResult = toolResult;
        log.errorMessage = errorMessage;
        log.createdAt = LocalDateTime.now();
        return log;
    }

    public UUID getId() {
        return id;
    }

    public UUID getSessionId() {
        return sessionId;
    }

    public int getStepIndex() {
        return stepIndex;
    }

    public String getDecisionType() {
        return decisionType;
    }

    public String getModelOutput() {
        return modelOutput;
    }

    public String getToolName() {
        return toolName;
    }

    public String getToolArguments() {
        return toolArguments;
    }

    public String getToolResult() {
        return toolResult;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}