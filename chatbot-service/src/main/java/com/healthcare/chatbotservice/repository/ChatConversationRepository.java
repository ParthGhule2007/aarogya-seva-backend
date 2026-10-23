package com.healthcare.chatbotservice.repository;

import com.healthcare.chatbotservice.entity.ChatConversation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ChatConversationRepository extends JpaRepository<ChatConversation, UUID> {

    List<ChatConversation> findByUserIdOrderByUpdatedAtDesc(String userId);
}
