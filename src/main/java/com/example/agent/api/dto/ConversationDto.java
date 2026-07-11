package com.example.agent.api.dto;

import com.example.agent.conversation.Conversation;
import java.time.LocalDateTime;
import java.util.UUID;

public class ConversationDto {

    private UUID id;
    private String userId;
    private String title;
    private String workspacePath;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public ConversationDto(Conversation c) {
        this.id = c.getId();
        this.userId = c.getUserId();
        this.title = c.getTitle();
        this.workspacePath = c.getWorkspacePath();
        this.status = c.getStatus();
        this.createdAt = c.getCreatedAt();
        this.updatedAt = c.getUpdatedAt();
    }

    public UUID getId() { return id; }
    public String getUserId() { return userId; }
    public String getTitle() { return title; }
    public String getWorkspacePath() { return workspacePath; }
    public String getStatus() { return status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
