package com.example.agent.api.dto;

import jakarta.validation.constraints.NotBlank;

public class CreateConversationRequest {

    @NotBlank
    private String userId;

    private String title;

    private String workspacePath;

    public String getUserId() { return userId; }
    public String getTitle() { return title; }
    public String getWorkspacePath() { return workspacePath; }
    public void setUserId(String userId) { this.userId = userId; }
    public void setTitle(String title) { this.title = title; }
    public void setWorkspacePath(String workspacePath) { this.workspacePath = workspacePath; }
}
