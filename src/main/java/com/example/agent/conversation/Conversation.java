package com.example.agent.conversation;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "conversation")
public class Conversation {

    @Id
    private UUID id;

    private String userId;

    private String title;

    @Column(columnDefinition = "TEXT")
    private String workspacePath;

    private String status;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    public static Conversation create(String userId, String title, String workspacePath) {
        Conversation c = new Conversation();
        c.id = UUID.randomUUID();
        c.userId = userId;
        c.title = (title == null || title.isBlank()) ? "New Conversation" : title;
        c.workspacePath = workspacePath;
        c.status = "ACTIVE";
        c.createdAt = LocalDateTime.now();
        c.updatedAt = LocalDateTime.now();
        return c;
    }

    public void updateTitle(String title) {
        this.title = title;
        this.updatedAt = LocalDateTime.now();
    }

    public void updateWorkspacePath(String workspacePath) {
        this.workspacePath = workspacePath;
        this.updatedAt = LocalDateTime.now();
    }

    public void archive() {
        this.status = "ARCHIVED";
        this.updatedAt = LocalDateTime.now();
    }

    public void touch() {
        this.updatedAt = LocalDateTime.now();
    }

    public UUID getId() { return id; }
    public String getUserId() { return userId; }
    public String getTitle() { return title; }
    public String getWorkspacePath() { return workspacePath; }
    public String getStatus() { return status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
