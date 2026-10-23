package com.healthcare.chatbotservice;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthcare.chatbotservice.dto.request.CreateConversationRequest;
import com.healthcare.chatbotservice.dto.request.SendMessageRequest;
import com.healthcare.chatbotservice.entity.ChatConversation;
import com.healthcare.chatbotservice.entity.ChatMessage;
import com.healthcare.chatbotservice.entity.MessageRole;
import com.healthcare.chatbotservice.repository.ChatConversationRepository;
import com.healthcare.chatbotservice.repository.ChatMessageRepository;
import com.healthcare.chatbotservice.service.AiModelService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ChatbotServiceIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ChatConversationRepository conversationRepository;

    @Autowired
    private ChatMessageRepository messageRepository;

    @MockBean
    private AiModelService aiModelService;

    @BeforeEach
    void cleanDatabase() {
        messageRepository.deleteAll();
        conversationRepository.deleteAll();
    }

    @Test
    @DisplayName("Complete end-to-end integration test: create conversation, send message, check database and fetch history")
    void testEndToEndChatFlow() throws Exception {
        // 1. Create conversation
        CreateConversationRequest createReq = CreateConversationRequest.builder()
                .userId("user-abc-123")
                .username("Rahul Sharma")
                .age(24)
                .build();

        MvcResult createResult = mockMvc.perform(post("/api/v1/chat/conversations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.conversationId").isNotEmpty())
                .andReturn();

        Map<?, ?> createResponseMap = objectMapper.readValue(createResult.getResponse().getContentAsString(), Map.class);
        Map<?, ?> dataMap = (Map<?, ?>) createResponseMap.get("data");
        UUID conversationId = UUID.fromString((String) dataMap.get("conversationId"));

        // Verify conversation saved in DB with correct snapshot
        ChatConversation conversation = conversationRepository.findById(conversationId).orElseThrow();
        assertThat(conversation.getUserId()).isEqualTo("user-abc-123");
        assertThat(conversation.getUsername()).isEqualTo("Rahul Sharma");
        assertThat(conversation.getAge()).isEqualTo(24);

        // 2. Mock AI model response
        when(aiModelService.getAiResponse(eq(24), eq("I have severe stomach pain"), any()))
                .thenReturn("How many hours has the stomach pain persisted?");

        // 3. Send message
        SendMessageRequest messageReq = SendMessageRequest.builder()
                .userId("user-abc-123")
                .message("I have severe stomach pain")
                .build();

        mockMvc.perform(post("/api/v1/chat/conversations/{conversationId}/messages", conversationId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(messageReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.role").value("ASSISTANT"))
                .andExpect(jsonPath("$.data.content").value("How many hours has the stomach pain persisted?"));

        // Verify both USER and ASSISTANT messages saved in DB
        List<ChatMessage> messagesInDb = messageRepository.findByConversationIdOrderByCreatedAtAsc(conversationId);
        assertThat(messagesInDb).hasSize(2);
        assertThat(messagesInDb.get(0).getRole()).isEqualTo(MessageRole.USER);
        assertThat(messagesInDb.get(0).getContent()).isEqualTo("I have severe stomach pain");
        assertThat(messagesInDb.get(1).getRole()).isEqualTo(MessageRole.ASSISTANT);
        assertThat(messagesInDb.get(1).getContent()).isEqualTo("How many hours has the stomach pain persisted?");

        // 4. Retrieve chat history via GET endpoint
        mockMvc.perform(get("/api/v1/chat/conversations/{conversationId}/messages", conversationId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].role").value("USER"))
                .andExpect(jsonPath("$.data[1].role").value("ASSISTANT"));

        // 5. Retrieve user's conversations
        mockMvc.perform(get("/api/v1/chat/conversations/user/{userId}", "user-abc-123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].conversationId").value(conversationId.toString()));
    }
}
