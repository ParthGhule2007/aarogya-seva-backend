package com.healthcare.chatbotservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthcare.chatbotservice.dto.request.CreateConversationRequest;
import com.healthcare.chatbotservice.dto.request.SendMessageRequest;
import com.healthcare.chatbotservice.dto.response.ChatMessageResponse;
import com.healthcare.chatbotservice.dto.response.ConversationResponse;
import com.healthcare.chatbotservice.entity.MessageRole;
import com.healthcare.chatbotservice.exception.AiServiceException;
import com.healthcare.chatbotservice.exception.ConversationNotFoundException;
import com.healthcare.chatbotservice.exception.GlobalExceptionHandler;
import com.healthcare.chatbotservice.exception.UnauthorizedConversationAccessException;
import com.healthcare.chatbotservice.service.ChatService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ChatController.class)
@Import(GlobalExceptionHandler.class)
class ChatControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ChatService chatService;

    @Test
    @DisplayName("POST /api/v1/chat/conversations - Success returns 201 with conversationId")
    void testCreateConversation_success() throws Exception {
        UUID conversationId = UUID.randomUUID();
        CreateConversationRequest request = CreateConversationRequest.builder()
                .userId("user-123")
                .username("Rahul Sharma")
                .age(24)
                .build();

        when(chatService.createConversation(any(CreateConversationRequest.class)))
                .thenReturn(ConversationResponse.builder().conversationId(conversationId).build());

        mockMvc.perform(post("/api/v1/chat/conversations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Conversation created successfully"))
                .andExpect(jsonPath("$.data.conversationId").value(conversationId.toString()))
                .andExpect(jsonPath("$.timestamp").isNotEmpty());
    }

    @Test
    @DisplayName("POST /api/v1/chat/conversations - Validation failure with missing fields returns 400")
    void testCreateConversation_validationFailure() throws Exception {
        CreateConversationRequest invalidRequest = CreateConversationRequest.builder()
                .userId("")
                .username("")
                .age(-5)
                .build();

        mockMvc.perform(post("/api/v1/chat/conversations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.message").isNotEmpty())
                .andExpect(jsonPath("$.timestamp").isNotEmpty());
    }

    @Test
    @DisplayName("POST /api/v1/chat/conversations/{id}/messages - Success returns 200 with AI response")
    void testSendMessage_success() throws Exception {
        UUID conversationId = UUID.randomUUID();
        UUID messageId = UUID.randomUUID();
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);

        SendMessageRequest request = SendMessageRequest.builder()
                .userId("user-123")
                .message("I am having stomach pain")
                .build();

        ChatMessageResponse response = ChatMessageResponse.builder()
                .messageId(messageId)
                .role(MessageRole.ASSISTANT)
                .content("How long have you been experiencing the pain?")
                .createdAt(now)
                .build();

        when(chatService.sendMessage(eq(conversationId), any(SendMessageRequest.class)))
                .thenReturn(response);

        mockMvc.perform(post("/api/v1/chat/conversations/{conversationId}/messages", conversationId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Message processed successfully"))
                .andExpect(jsonPath("$.data.messageId").value(messageId.toString()))
                .andExpect(jsonPath("$.data.role").value("ASSISTANT"))
                .andExpect(jsonPath("$.data.content").value("How long have you been experiencing the pain?"))
                .andExpect(jsonPath("$.timestamp").isNotEmpty());
    }

    @Test
    @DisplayName("8. Empty message validation returns 400")
    void testSendMessage_emptyMessage_validationFailure() throws Exception {
        UUID conversationId = UUID.randomUUID();
        SendMessageRequest invalidRequest = SendMessageRequest.builder()
                .userId("user-123")
                .message("")
                .build();

        mockMvc.perform(post("/api/v1/chat/conversations/{conversationId}/messages", conversationId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.message").value("Validation failed: message cannot be empty"));
    }

    @Test
    @DisplayName("7. Conversation not found returns 404")
    void testSendMessage_conversationNotFound() throws Exception {
        UUID nonExistentId = UUID.randomUUID();
        SendMessageRequest request = SendMessageRequest.builder()
                .userId("user-123")
                .message("Hello")
                .build();

        when(chatService.sendMessage(eq(nonExistentId), any(SendMessageRequest.class)))
                .thenThrow(new ConversationNotFoundException(nonExistentId));

        mockMvc.perform(post("/api/v1/chat/conversations/{conversationId}/messages", nonExistentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("CONVERSATION_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Conversation not found with id: " + nonExistentId));
    }

    @Test
    @DisplayName("6. Unauthorized access returns 403")
    void testSendMessage_unauthorized() throws Exception {
        UUID conversationId = UUID.randomUUID();
        SendMessageRequest request = SendMessageRequest.builder()
                .userId("intruder")
                .message("Hello")
                .build();

        when(chatService.sendMessage(eq(conversationId), any(SendMessageRequest.class)))
                .thenThrow(new UnauthorizedConversationAccessException("intruder", conversationId));

        mockMvc.perform(post("/api/v1/chat/conversations/{conversationId}/messages", conversationId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("UNAUTHORIZED_CONVERSATION_ACCESS"))
                .andExpect(jsonPath("$.message").value(
                        String.format("User intruder is not authorized to access conversation %s", conversationId)));
    }

    @Test
    @DisplayName("10. AI service failure returns 502 Bad Gateway")
    void testSendMessage_aiServiceFailure() throws Exception {
        UUID conversationId = UUID.randomUUID();
        SendMessageRequest request = SendMessageRequest.builder()
                .userId("user-123")
                .message("Hello")
                .build();

        when(chatService.sendMessage(eq(conversationId), any(SendMessageRequest.class)))
                .thenThrow(new AiServiceException("AI service is unreachable"));

        mockMvc.perform(post("/api/v1/chat/conversations/{conversationId}/messages", conversationId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("AI_SERVICE_ERROR"))
                .andExpect(jsonPath("$.message").value("AI service is unreachable"));
    }

    @Test
    @DisplayName("11. GET /api/v1/chat/conversations/{id}/messages - Success returns 200 with chat history")
    void testGetConversationHistory_success() throws Exception {
        UUID conversationId = UUID.randomUUID();
        ChatMessageResponse msg1 = ChatMessageResponse.builder()
                .messageId(UUID.randomUUID())
                .role(MessageRole.USER)
                .content("I am having stomach pain")
                .createdAt(OffsetDateTime.now(ZoneOffset.UTC).minusMinutes(2))
                .build();

        ChatMessageResponse msg2 = ChatMessageResponse.builder()
                .messageId(UUID.randomUUID())
                .role(MessageRole.ASSISTANT)
                .content("How long have you been experiencing the pain?")
                .createdAt(OffsetDateTime.now(ZoneOffset.UTC).minusMinutes(1))
                .build();

        when(chatService.getConversationHistory(conversationId)).thenReturn(List.of(msg1, msg2));

        mockMvc.perform(get("/api/v1/chat/conversations/{conversationId}/messages", conversationId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Chat history fetched successfully"))
                .andExpect(jsonPath("$.data[0].role").value("USER"))
                .andExpect(jsonPath("$.data[0].content").value("I am having stomach pain"))
                .andExpect(jsonPath("$.data[1].role").value("ASSISTANT"))
                .andExpect(jsonPath("$.data[1].content").value("How long have you been experiencing the pain?"));
    }

    @Test
    @DisplayName("12. GET /api/v1/chat/conversations/user/{userId} - Success returns 200 with user conversations")
    void testGetUserConversations_success() throws Exception {
        UUID conversationId = UUID.randomUUID();
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        ConversationResponse conv = ConversationResponse.builder()
                .conversationId(conversationId)
                .createdAt(now.minusHours(1))
                .updatedAt(now)
                .build();

        when(chatService.getUserConversations("user-123")).thenReturn(List.of(conv));

        mockMvc.perform(get("/api/v1/chat/conversations/user/{userId}", "user-123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Conversations fetched successfully"))
                .andExpect(jsonPath("$.data[0].conversationId").value(conversationId.toString()))
                .andExpect(jsonPath("$.data[0].createdAt").isNotEmpty())
                .andExpect(jsonPath("$.data[0].updatedAt").isNotEmpty());
    }
}
