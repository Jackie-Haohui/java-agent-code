package com.example.agent.conversation;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface ConversationRepository extends JpaRepository<Conversation, UUID> {

    List<Conversation> findByUserIdAndStatusOrderByUpdatedAtDesc(String userId, String status);

    List<Conversation> findByUserIdOrderByUpdatedAtDesc(String userId);
}
