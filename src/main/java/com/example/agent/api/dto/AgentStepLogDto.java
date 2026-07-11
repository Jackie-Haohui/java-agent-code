package com.example.agent.api.dto;

import com.example.agent.state.AgentStepLog;

import java.time.LocalDateTime;
import java.util.UUID;

public class AgentStepLogDto {

    private UUID id;
    private UUID sessionId;
    private int stepIndex;
    private String decisionType;
    private String modelOutput;
    private String toolName;
    private String toolArguments;
    private String toolResult;
    private String errorMessage;
    private LocalDateTime createdAt;

    public AgentStepLogDto(AgentStepLog log) {
        this.id = log.getId();
        this.sessionId = log.getSessionId();
        this.stepIndex = log.getStepIndex();
        this.decisionType = log.getDecisionType();
        this.modelOutput = log.getModelOutput();
        this.toolName = log.getToolName();
        this.toolArguments = log.getToolArguments();
        this.toolResult = log.getToolResult();
        this.errorMessage = log.getErrorMessage();
        this.createdAt = log.getCreatedAt();
    }

    public UUID getId() { return id; }
    public UUID getSessionId() { return sessionId; }
    public int getStepIndex() { return stepIndex; }
    public String getDecisionType() { return decisionType; }
    public String getModelOutput() { return modelOutput; }
    public String getToolName() { return toolName; }
    public String getToolArguments() { return toolArguments; }
    public String getToolResult() { return toolResult; }
    public String getErrorMessage() { return errorMessage; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
