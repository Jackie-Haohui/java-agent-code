package com.example.agent.state;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "agent_session")
public class AgentSession {

    @Id
    private UUID id;

    private String userId;

    @Column(columnDefinition = "TEXT")
    private String goal;

    private String status;

    private int currentStep;

    private int maxSteps;

    @Column(columnDefinition = "TEXT")
    private String summary;

    private UUID conversationId;

    private String workspacePath;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    public static AgentSession create(String userId, String goal, int maxSteps) {
        return create(userId, goal, maxSteps, null, null);
    }

    public static AgentSession create(String userId, String goal, int maxSteps, UUID conversationId, String workspacePath) {
        AgentSession session = new AgentSession();
        session.id = UUID.randomUUID();
        session.userId = userId;
        session.goal = goal;
        session.status = "RUNNING";
        session.currentStep = 0;
        session.maxSteps = maxSteps;
        session.conversationId = conversationId;
        session.workspacePath = workspacePath;
        session.createdAt = LocalDateTime.now();
        session.updatedAt = LocalDateTime.now();
        return session;
    }

    public void incrementStep() {
        this.currentStep++;
        this.updatedAt = LocalDateTime.now();
    }

    public void markCompleted() {
        this.status = "COMPLETED";
        this.updatedAt = LocalDateTime.now();
    }

    public void markFailed() {
        this.status = "FAILED";
        this.updatedAt = LocalDateTime.now();
    }

    public void markWaitingApproval() {
        this.status = "WAITING_APPROVAL";
        this.updatedAt = LocalDateTime.now();
    }

    public UUID getId() {
        return id;
    }

    public String getUserId() {
        return userId;
    }

    public String getGoal() {
        return goal;
    }

    public String getStatus() {
        return status;
    }

    public int getCurrentStep() {
        return currentStep;
    }

    public int getMaxSteps() {
        return maxSteps;
    }

    public String getSummary() {
        return summary;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setSummary(String summary) {
        this.summary = summary;
        this.updatedAt = LocalDateTime.now();
    }

    public UUID getConversationId() { return conversationId; }
    public void setConversationId(UUID conversationId) { this.conversationId = conversationId; }
    public String getWorkspacePath() { return workspacePath; }
    public void setWorkspacePath(String workspacePath) { this.workspacePath = workspacePath; }
}