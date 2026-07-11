package com.example.agent.conversation;

import com.example.agent.api.dto.ToolCallDto;
import com.example.agent.workspace.ProjectScanner;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

@Service
public class ConversationService {

    private static final Logger log = LoggerFactory.getLogger(ConversationService.class);

    private final ConversationRepository conversationRepository;
    private final ConversationMessageRepository messageRepository;
    private final ProjectScanner projectScanner;
    private final ObjectMapper objectMapper;

    public ConversationService(
            ConversationRepository conversationRepository,
            ConversationMessageRepository messageRepository,
            ProjectScanner projectScanner,
            ObjectMapper objectMapper
    ) {
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.projectScanner = projectScanner;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public Conversation createConversation(String userId, String title, String workspacePath) {
        if (workspacePath != null && !workspacePath.isBlank()) {
            Path path = Path.of(workspacePath).toAbsolutePath().normalize();
            if (!Files.exists(path) || !Files.isDirectory(path)) {
                throw new IllegalArgumentException("工作空间路径不存在或不是目录: " + workspacePath);
            }
        }
        String finalTitle = (title == null || title.isBlank())
                ? "New Conversation"
                : title;
        Conversation conversation = conversationRepository.save(Conversation.create(userId, finalTitle, workspacePath));

        // 如果有工作空间路径，自动扫描项目并保存为系统消息
        if (workspacePath != null && !workspacePath.isBlank()) {
            try {
                ProjectScanner.ProjectSummary summary = projectScanner.scan(workspacePath);
                String sysMsgContent = "【项目扫描结果】\n" + summary.summary();
                messageRepository.save(ConversationMessage.create(conversation.getId(), null, "SYSTEM", sysMsgContent, null));
                log.info("项目扫描完成: {} -> {} 个文件, 类型: {}", workspacePath, summary.allFiles().size(), summary.projectType());
            } catch (Exception e) {
                log.warn("项目扫描失败: {}", e.getMessage());
            }
        }

        return conversation;
    }

    public Conversation getConversation(UUID conversationId) {
        return conversationRepository.findById(conversationId)
                .orElseThrow(() -> new IllegalArgumentException("Conversation not found: " + conversationId));
    }

    public List<Conversation> listConversations(String userId) {
        return conversationRepository.findByUserIdAndStatusOrderByUpdatedAtDesc(userId, "ACTIVE");
    }

    @Transactional
    public ConversationMessage addUserMessage(UUID conversationId, String content) {
        Conversation conversation = getConversation(conversationId);
        conversation.touch();
        conversationRepository.save(conversation);
        return messageRepository.save(ConversationMessage.create(conversationId, null, "USER", content, null));
    }

    @Transactional
    public ConversationMessage addAgentMessage(UUID conversationId, UUID sessionId, String content, List<ToolCallDto> toolCalls) {
        Conversation conversation = getConversation(conversationId);
        conversation.touch();
        conversationRepository.save(conversation);

        String toolCallsJson = null;
        if (toolCalls != null && !toolCalls.isEmpty()) {
            try {
                toolCallsJson = objectMapper.writeValueAsString(toolCalls);
            } catch (JsonProcessingException ignored) {
            }
        }

        return messageRepository.save(ConversationMessage.create(conversationId, sessionId, "AGENT", content, toolCallsJson));
    }

    @Transactional
    public ConversationMessage addSystemMessage(UUID conversationId, String content) {
        return messageRepository.save(ConversationMessage.create(conversationId, null, "SYSTEM", content, null));
    }

    public List<ConversationMessage> getMessages(UUID conversationId) {
        return messageRepository.findByConversationIdOrderByCreatedAtAsc(conversationId);
    }

    @Transactional
    public Conversation updateConversation(UUID conversationId, String title, String workspacePath) {
        Conversation conversation = getConversation(conversationId);
        if (title != null && !title.isBlank()) {
            conversation.updateTitle(title);
        }
        if (workspacePath != null && !workspacePath.isBlank()) {
            Path path = Path.of(workspacePath).toAbsolutePath().normalize();
            if (!Files.exists(path) || !Files.isDirectory(path)) {
                throw new IllegalArgumentException("工作空间路径不存在或不是目录: " + workspacePath);
            }
            conversation.updateWorkspacePath(workspacePath);
        }
        return conversationRepository.save(conversation);
    }

    @Transactional
    public void archiveConversation(UUID conversationId) {
        Conversation conversation = getConversation(conversationId);
        conversation.archive();
        conversationRepository.save(conversation);
    }
}
