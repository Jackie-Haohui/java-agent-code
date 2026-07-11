package com.example.agent.api.dto;

import com.example.agent.conversation.ConversationMessage;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class ConversationMessageDto {

    private UUID id;
    private UUID conversationId;
    private UUID sessionId;
    private String role;
    private String content;
    private List<ToolCallDto> toolCalls;
    private LocalDateTime createdAt;

    public ConversationMessageDto(ConversationMessage msg) {
        this(msg, new ObjectMapper());
    }

    public ConversationMessageDto(ConversationMessage msg, ObjectMapper om) {
        this.id = msg.getId();
        this.conversationId = msg.getConversationId();
        this.sessionId = msg.getSessionId();
        this.role = msg.getRole();
        this.content = msg.getContent();
        this.createdAt = msg.getCreatedAt();
        if (msg.getToolCallsJson() != null && !msg.getToolCallsJson().isBlank()) {
            try {
                this.toolCalls = om.readValue(msg.getToolCallsJson(), new TypeReference<List<ToolCallDto>>() {});
            } catch (Exception e) {
                this.toolCalls = Collections.emptyList();
            }
        } else {
            this.toolCalls = Collections.emptyList();
        }
    }

    public UUID getId() { return id; }
    public UUID getConversationId() { return conversationId; }
    public UUID getSessionId() { return sessionId; }
    public String getRole() { return role; }
    public String getContent() { return content; }
    public List<ToolCallDto> getToolCalls() { return toolCalls; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
