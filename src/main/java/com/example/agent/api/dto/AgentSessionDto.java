package com.example.agent.api.dto;

import com.example.agent.state.AgentSession;

import java.time.LocalDateTime;
import java.util.UUID;

public class AgentSessionDto {

    private UUID id;
    private String userId;
    private String goal;
    private String status;
    private int currentStep;
    private int maxSteps;
    private String summary;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public AgentSessionDto(AgentSession session) {
        this.id = session.getId();
        this.userId = session.getUserId();
        this.goal = session.getGoal();
        this.status = session.getStatus();
        this.currentStep = session.getCurrentStep();
        this.maxSteps = session.getMaxSteps();
        this.summary = session.getSummary();
        this.createdAt = session.getCreatedAt();
        this.updatedAt = session.getUpdatedAt();
    }

    public UUID getId() { return id; }
    public String getUserId() { return userId; }
    public String getGoal() { return goal; }
    public String getStatus() { return status; }
    public int getCurrentStep() { return currentStep; }
    public int getMaxSteps() { return maxSteps; }
    public String getSummary() { return summary; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
