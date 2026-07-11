package com.example.agent.conversation;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "conversation_message")
public class ConversationMessage {

    @Id
    private UUID id;

    private UUID conversationId;

    private UUID sessionId;

    private String role;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Column(columnDefinition = "TEXT")
    private String toolCallsJson;

    private LocalDateTime createdAt;

    public static ConversationMessage create(UUID conversationId, UUID sessionId, String role, String content, String toolCallsJson) {
        ConversationMessage msg = new ConversationMessage();
        msg.id = UUID.randomUUID();
        msg.conversationId = conversationId;
        msg.sessionId = sessionId;
        msg.role = role;
        msg.content = content;
        msg.toolCallsJson = toolCallsJson;
        msg.createdAt = LocalDateTime.now();
        return msg;
    }

    public UUID getId() { return id; }
    public UUID getConversationId() { return conversationId; }
    public UUID getSessionId() { return sessionId; }
    public String getRole() { return role; }
    public String getContent() { return content; }
    public String getToolCallsJson() { return toolCallsJson; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
