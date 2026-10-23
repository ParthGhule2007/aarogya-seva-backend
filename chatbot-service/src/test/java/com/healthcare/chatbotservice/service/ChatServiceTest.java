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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatServiceTest {

    @Mock
    private ChatConversationRepository conversationRepository;

    @Mock
    private ChatMessageRepository messageRepository;

    @Mock
    private AiModelService aiModelService;

    @Captor
    private ArgumentCaptor<ChatConversation> conversationCaptor;

    @Captor
    private ArgumentCaptor<ChatMessage> messageCaptor;

    @Captor
    private ArgumentCaptor<List<AiChatRequest.AiHistoryItem>> historyCaptor;

    private ChatService chatService;

    private final int MAX_HISTORY = 20;

    @BeforeEach
    void setUp() {
        chatService = new ChatService(conversationRepository, messageRepository, aiModelService, MAX_HISTORY);
    }

    @Test
    @DisplayName("1. Conversation creation saves conversation and returns conversationId")
    void testCreateConversation_success() {
        CreateConversationRequest request = CreateConversationRequest.builder()
                .userId("user-123")
                .username("Rahul Sharma")
                .age(24)
                .build();

        UUID generatedId = UUID.randomUUID();
        when(conversationRepository.save(any(ChatConversation.class))).thenAnswer(invocation -> {
            ChatConversation conv = invocation.getArgument(0);
            return ChatConversation.builder()
                    .id(generatedId)
                    .userId(conv.getUserId())
                    .username(conv.getUsername())
                    .age(conv.getAge())
                    .createdAt(conv.getCreatedAt())
                    .updatedAt(conv.getUpdatedAt())
                    .build();
        });

        ConversationResponse response = chatService.createConversation(request);

        assertThat(response).isNotNull();
        assertThat(response.getConversationId()).isEqualTo(generatedId);

        verify(conversationRepository).save(conversationCaptor.capture());
        ChatConversation captured = conversationCaptor.getValue();
        assertThat(captured.getUserId()).isEqualTo("user-123");
        assertThat(captured.getUsername()).isEqualTo("Rahul Sharma");
        assertThat(captured.getAge()).isEqualTo(24);
        assertThat(captured.getCreatedAt()).isNotNull();
        assertThat(captured.getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("2, 3, 4, 5. Send message: USER message and AI response saved correctly without pending status")
    void testSendMessage_success() {
        UUID convId = UUID.randomUUID();
        String userId = "user-123";

        ChatConversation conversation = ChatConversation.builder()
                .id(convId)
                .userId(userId)
                .username("Rahul Sharma")
                .age(24)
                .createdAt(OffsetDateTime.now(ZoneOffset.UTC))
                .updatedAt(OffsetDateTime.now(ZoneOffset.UTC))
                .build();

        when(conversationRepository.findById(convId)).thenReturn(Optional.of(conversation));
        when(messageRepository.findByConversationIdOrderByCreatedAtDesc(eq(convId), any(Pageable.class)))
                .thenReturn(new PageImpl<>(Collections.emptyList()));
        when(aiModelService.getAiResponse(eq(24), eq("I am having stomach pain"), any()))
                .thenReturn("How long have you been experiencing the pain?");

        when(messageRepository.save(any(ChatMessage.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(conversationRepository.save(any(ChatConversation.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SendMessageRequest request = SendMessageRequest.builder()
                .userId(userId)
                .message("I am having stomach pain")
                .build();

        ChatMessageResponse response = chatService.sendMessage(convId, request);

        // Verify response returned to frontend
        assertThat(response).isNotNull();
        assertThat(response.getRole()).isEqualTo(MessageRole.ASSISTANT);
        assertThat(response.getContent()).isEqualTo("How long have you been experiencing the pain?");

        // Verify two messages were saved: 1 USER, 1 ASSISTANT
        verify(messageRepository, times(2)).save(messageCaptor.capture());
        List<ChatMessage> savedMessages = messageCaptor.getAllValues();

        ChatMessage userMessage = savedMessages.get(0);
        assertThat(userMessage.getRole()).isEqualTo(MessageRole.USER);
        assertThat(userMessage.getContent()).isEqualTo("I am having stomach pain");
        assertThat(userMessage.getConversationId()).isEqualTo(convId);

        ChatMessage aiMessage = savedMessages.get(1);
        assertThat(aiMessage.getRole()).isEqualTo(MessageRole.ASSISTANT);
        assertThat(aiMessage.getContent()).isEqualTo("How long have you been experiencing the pain?");
        assertThat(aiMessage.getConversationId()).isEqualTo(convId);

        // Verify conversation updated_at was updated
        verify(conversationRepository).save(conversation);
    }

    @Test
    @DisplayName("6. Conversation ownership validation throws UnauthorizedConversationAccessException")
    void testSendMessage_unauthorizedAccess() {
        UUID convId = UUID.randomUUID();
        ChatConversation conversation = ChatConversation.builder()
                .id(convId)
                .userId("owner-user")
                .username("Owner")
                .age(30)
                .build();

        when(conversationRepository.findById(convId)).thenReturn(Optional.of(conversation));

        SendMessageRequest request = SendMessageRequest.builder()
                .userId("intruder-user")
                .message("Hello")
                .build();

        assertThatThrownBy(() -> chatService.sendMessage(convId, request))
                .isInstanceOf(UnauthorizedConversationAccessException.class)
                .hasMessageContaining("intruder-user");

        verify(messageRepository, never()).save(any());
        verify(aiModelService, never()).getAiResponse(any(), any(), any());
    }

    @Test
    @DisplayName("7. Conversation not found throws ConversationNotFoundException")
    void testSendMessage_conversationNotFound() {
        UUID nonExistentId = UUID.randomUUID();
        when(conversationRepository.findById(nonExistentId)).thenReturn(Optional.empty());

        SendMessageRequest request = SendMessageRequest.builder()
                .userId("user-123")
                .message("Hello")
                .build();

        assertThatThrownBy(() -> chatService.sendMessage(nonExistentId, request))
                .isInstanceOf(ConversationNotFoundException.class)
                .hasMessageContaining(nonExistentId.toString());

        verify(messageRepository, never()).save(any());
        verify(aiModelService, never()).getAiResponse(any(), any(), any());
    }

    @Test
    @DisplayName("Chat history passed to AI respects chronological order and limit")
    void testSendMessage_historyPassedToAi() {
        UUID convId = UUID.randomUUID();
        String userId = "user-123";

        ChatConversation conversation = ChatConversation.builder()
                .id(convId)
                .userId(userId)
                .username("Rahul")
                .age(24)
                .build();

        ChatMessage prev1 = ChatMessage.builder()
                .id(UUID.randomUUID())
                .conversationId(convId)
                .role(MessageRole.USER)
                .content("First message")
                .createdAt(OffsetDateTime.now(ZoneOffset.UTC).minusMinutes(5))
                .build();

        ChatMessage prev2 = ChatMessage.builder()
                .id(UUID.randomUUID())
                .conversationId(convId)
                .role(MessageRole.ASSISTANT)
                .content("First response")
                .createdAt(OffsetDateTime.now(ZoneOffset.UTC).minusMinutes(4))
                .build();

        // Repository returns desc order (latest first: prev2, prev1)
        when(conversationRepository.findById(convId)).thenReturn(Optional.of(conversation));
        when(messageRepository.findByConversationIdOrderByCreatedAtDesc(eq(convId), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(prev2, prev1)));

        when(aiModelService.getAiResponse(eq(24), eq("Second message"), historyCaptor.capture()))
                .thenReturn("Second response");
        when(messageRepository.save(any(ChatMessage.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SendMessageRequest request = SendMessageRequest.builder()
                .userId(userId)
                .message("Second message")
                .build();

        chatService.sendMessage(convId, request);

        // Verify history passed to AI was reversed to chronological order (prev1, then prev2)
        List<AiChatRequest.AiHistoryItem> capturedHistory = historyCaptor.getValue();
        assertThat(capturedHistory).hasSize(2);
        assertThat(capturedHistory.get(0).getContent()).isEqualTo("First message");
        assertThat(capturedHistory.get(0).getRole()).isEqualTo("USER");
        assertThat(capturedHistory.get(1).getContent()).isEqualTo("First response");
        assertThat(capturedHistory.get(1).getRole()).isEqualTo("ASSISTANT");
    }

    @Test
    @DisplayName("11. Chat history retrieval in chronological order")
    void testGetConversationHistory_success() {
        UUID convId = UUID.randomUUID();
        ChatMessage msg1 = ChatMessage.builder()
                .id(UUID.randomUUID())
                .conversationId(convId)
                .role(MessageRole.USER)
                .content("I am having stomach pain")
                .createdAt(OffsetDateTime.now(ZoneOffset.UTC).minusMinutes(2))
                .build();

        ChatMessage msg2 = ChatMessage.builder()
                .id(UUID.randomUUID())
                .conversationId(convId)
                .role(MessageRole.ASSISTANT)
                .content("How long have you been experiencing the pain?")
                .createdAt(OffsetDateTime.now(ZoneOffset.UTC).minusMinutes(1))
                .build();

        when(conversationRepository.existsById(convId)).thenReturn(true);
        when(messageRepository.findByConversationIdOrderByCreatedAtAsc(convId)).thenReturn(List.of(msg1, msg2));

        List<ChatMessageResponse> history = chatService.getConversationHistory(convId);

        assertThat(history).hasSize(2);
        assertThat(history.get(0).getRole()).isEqualTo(MessageRole.USER);
        assertThat(history.get(0).getContent()).isEqualTo("I am having stomach pain");
        assertThat(history.get(1).getRole()).isEqualTo(MessageRole.ASSISTANT);
        assertThat(history.get(1).getContent()).isEqualTo("How long have you been experiencing the pain?");
    }

    @Test
    @DisplayName("Chat history retrieval for non-existent conversation throws ConversationNotFoundException")
    void testGetConversationHistory_notFound() {
        UUID nonExistentId = UUID.randomUUID();
        when(conversationRepository.existsById(nonExistentId)).thenReturn(false);

        assertThatThrownBy(() -> chatService.getConversationHistory(nonExistentId))
                .isInstanceOf(ConversationNotFoundException.class);
    }

    @Test
    @DisplayName("12. User conversation retrieval returns conversations belonging to user")
    void testGetUserConversations_success() {
        String userId = "user-123";
        ChatConversation c1 = ChatConversation.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .username("Rahul")
                .age(24)
                .createdAt(OffsetDateTime.now(ZoneOffset.UTC).minusHours(1))
                .updatedAt(OffsetDateTime.now(ZoneOffset.UTC))
                .build();

        when(conversationRepository.findByUserIdOrderByUpdatedAtDesc(userId)).thenReturn(List.of(c1));

        List<ConversationResponse> conversations = chatService.getUserConversations(userId);

        assertThat(conversations).hasSize(1);
        assertThat(conversations.get(0).getConversationId()).isEqualTo(c1.getId());
        assertThat(conversations.get(0).getCreatedAt()).isEqualTo(c1.getCreatedAt());
        assertThat(conversations.get(0).getUpdatedAt()).isEqualTo(c1.getUpdatedAt());
    }
}
