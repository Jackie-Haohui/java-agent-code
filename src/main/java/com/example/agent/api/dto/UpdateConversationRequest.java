package com.example.agent.api.dto;

public class UpdateConversationRequest {

    private String title;

    private String workspacePath;

    public String getTitle() { return title; }
    public String getWorkspacePath() { return workspacePath; }
    public void setTitle(String title) { this.title = title; }
    public void setWorkspacePath(String workspacePath) { this.workspacePath = workspacePath; }
}
