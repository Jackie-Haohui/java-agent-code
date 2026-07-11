package com.example.agent.api.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.Map;

public class ChatRequest {

    @NotBlank
    private String userId;

    @NotBlank
    private String message;

    private Map<String, Object> input;

    public String getUserId() { return userId; }
    public String getMessage() { return message; }
    public Map<String, Object> getInput() { return input; }
    public void setUserId(String userId) { this.userId = userId; }
    public void setMessage(String message) { this.message = message; }
    public void setInput(Map<String, Object> input) { this.input = input; }
}
