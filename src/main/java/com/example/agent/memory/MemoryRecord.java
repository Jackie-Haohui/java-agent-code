package com.example.agent.memory;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "memory_record")
public class MemoryRecord {

    @Id
    private UUID id;

    private String userId;

    @Enumerated(EnumType.STRING)
    private MemoryType memoryType;

    @Column(columnDefinition = "TEXT")
    private String content;

    private int importance;

    @Column(columnDefinition = "TEXT")
    private String embedding;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    public static MemoryRecord create(String userId, MemoryType type, String content, int importance) {
        MemoryRecord record = new MemoryRecord();
        record.id = UUID.randomUUID();
        record.userId = userId;
        record.memoryType = type;
        record.content = content;
        record.importance = importance;
        record.createdAt = LocalDateTime.now();
        record.updatedAt = LocalDateTime.now();
        return record;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public MemoryType getMemoryType() { return memoryType; }
    public void setMemoryType(MemoryType memoryType) { this.memoryType = memoryType; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public int getImportance() { return importance; }
    public void setImportance(int importance) { this.importance = importance; }

    public String getEmbedding() { return embedding; }
    public void setEmbedding(String embedding) { this.embedding = embedding; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
