package com.healthcare.chatbotservice.service;

import com.healthcare.chatbotservice.dto.request.AiChatRequest;
import com.healthcare.chatbotservice.dto.request.CreateConversationRequest;
import com.healthcare.chatbotservice.dto.request.SendMessageRequest;
import com.healthcare.chatbotservice.dto.response.ChatMessageResponse;
import com.healthcare.chatbotservice.dto.response.ConversationResponse;
import com.healthcare.chatbotservice.entity.ChatConversation;
import com.healthcare.chatbotservice.entity.ChatMessage;
import com.healthcare.chatbotservice.entity.MessageRole;
import com.healthcare.chatbotservice.exception.ConversationNotFoundException;
import com.healthcare.chatbotservice.exception.UnauthorizedConversationAccessException;
import com.healthcare.chatbotservice.repository.ChatConversationRepository;
import com.healthcare.chatbotservice.repository.ChatMessageRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ChatService {

    private static final Logger log = LoggerFactory.getLogger(ChatService.class);

    private final ChatConversationRepository conversationRepository;
    private final ChatMessageRepository messageRepository;
    private final AiModelService aiModelService;
    private final int maxHistoryMessages;

    public ChatService(ChatConversationRepository conversationRepository,
                       ChatMessageRepository messageRepository,
                       AiModelService aiModelService,
                       @Value("${ai.max-history-messages:20}") int maxHistoryMessages) {
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.aiModelService = aiModelService;
        this.maxHistoryMessages = maxHistoryMessages;
    }

    /**
     * Creates a new conversation snapshot.
     */
    @Transactional
    public ConversationResponse createConversation(CreateConversationRequest request) {
        log.info("Creating conversation for user: {}", request.getUserId());

        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        ChatConversation conversation = ChatConversation.builder()
                .id(UUID.randomUUID())
                .userId(request.getUserId())
                .username(request.getUsername())
                .age(request.getAge())
                .createdAt(now)
                .updatedAt(now)
                .build();

        ChatConversation saved = conversationRepository.save(conversation);
        return ConversationResponse.builder()
                .conversationId(saved.getId())
                .build();
    }

    /**
     * Handles sending a message, calling the external AI service, saving both messages,
     * and returning the AI response.
     */
    @Transactional
    public ChatMessageResponse sendMessage(UUID conversationId, SendMessageRequest request) {
        log.info("Processing message for conversation: {} by user: {}", conversationId, request.getUserId());

        // 1. Validate that the conversation exists
        ChatConversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ConversationNotFoundException(conversationId));

        // 2. Verify that the conversation belongs to the supplied userId
        if (!conversation.getUserId().equals(request.getUserId())) {
            log.warn("Security alert: User {} attempted to access conversation {} owned by {}",
                    request.getUserId(), conversationId, conversation.getUserId());
            throw new UnauthorizedConversationAccessException(request.getUserId(), conversationId);
        }

        // 3. Retrieve recent conversation history for AI context (before saving current message)
        List<ChatMessage> recentMessagesDesc = messageRepository
                .findByConversationIdOrderByCreatedAtDesc(conversationId, PageRequest.of(0, maxHistoryMessages))
                .getContent();

        List<ChatMessage> historyChronological = new ArrayList<>(recentMessagesDesc);
        Collections.reverse(historyChronological);

        List<AiChatRequest.AiHistoryItem> aiHistory = historyChronological.stream()
                .map(msg -> new AiChatRequest.AiHistoryItem(msg.getRole().name(), msg.getContent()))
                .collect(Collectors.toList());

        // 4. Save the user's message in chat_messages with role = USER
        OffsetDateTime userMessageTime = OffsetDateTime.now(ZoneOffset.UTC);
        ChatMessage userMessage = ChatMessage.builder()
                .id(UUID.randomUUID())
                .conversationId(conversationId)
                .role(MessageRole.USER)
                .content(request.getMessage())
                .createdAt(userMessageTime)
                .build();
        messageRepository.save(userMessage);

        // 5. Call external AI Model Service with age, current message, and conversation history
        String aiResponseText = aiModelService.getAiResponse(
                conversation.getAge(),
                request.getMessage(),
                aiHistory
        );

        // 6. Save the AI response in chat_messages with role = ASSISTANT
        OffsetDateTime aiMessageTime = OffsetDateTime.now(ZoneOffset.UTC);
        ChatMessage aiMessage = ChatMessage.builder()
                .id(UUID.randomUUID())
                .conversationId(conversationId)
                .role(MessageRole.ASSISTANT)
                .content(aiResponseText)
                .createdAt(aiMessageTime)
                .build();
        ChatMessage savedAiMessage = messageRepository.save(aiMessage);

        // 7. Update conversation updated_at timestamp
        conversation.setUpdatedAt(aiMessageTime);
        conversationRepository.save(conversation);

        // 8. Return AI response to the frontend
        return ChatMessageResponse.builder()
                .messageId(savedAiMessage.getId())
                .role(savedAiMessage.getRole())
                .content(savedAiMessage.getContent())
                .createdAt(savedAiMessage.getCreatedAt())
                .build();
    }

    /**
     * Retrieves all messages for a given conversation in chronological order.
     */
    @Transactional(readOnly = true)
    public List<ChatMessageResponse> getConversationHistory(UUID conversationId) {
        log.info("Fetching chat history for conversation: {}", conversationId);

        if (!conversationRepository.existsById(conversationId)) {
            throw new ConversationNotFoundException(conversationId);
        }

        return messageRepository.findByConversationIdOrderByCreatedAtAsc(conversationId).stream()
                .map(msg -> ChatMessageResponse.builder()
                        .messageId(msg.getId())
                        .role(msg.getRole())
                        .content(msg.getContent())
                        .createdAt(msg.getCreatedAt())
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * Retrieves all conversations belonging to a user.
     */
    @Transactional(readOnly = true)
    public List<ConversationResponse> getUserConversations(String userId) {
        log.info("Fetching conversations for user: {}", userId);

        return conversationRepository.findByUserIdOrderByUpdatedAtDesc(userId).stream()
                .map(conv -> ConversationResponse.builder()
                        .conversationId(conv.getId())
                        .createdAt(conv.getCreatedAt())
                        .updatedAt(conv.getUpdatedAt())
                        .build())
                .collect(Collectors.toList());
    }
}
