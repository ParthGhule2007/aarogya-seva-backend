package com.healthcare.chatbotservice.controller;

import com.healthcare.chatbotservice.dto.request.CreateConversationRequest;
import com.healthcare.chatbotservice.dto.request.SendMessageRequest;
import com.healthcare.chatbotservice.dto.response.ApiResponse;
import com.healthcare.chatbotservice.dto.response.ChatMessageResponse;
import com.healthcare.chatbotservice.dto.response.ConversationResponse;
import com.healthcare.chatbotservice.service.ChatService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/chat/conversations")
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    /**
     * POST /api/v1/chat/conversations
     * Create a new chat conversation snapshot.
     */
    @PostMapping
    public ResponseEntity<ApiResponse<ConversationResponse>> createConversation(
            @Valid @RequestBody CreateConversationRequest request) {
        ConversationResponse response = chatService.createConversation(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Conversation created successfully", response));
    }

    /**
     * POST /api/v1/chat/conversations/{conversationId}/messages
     * Send user message and receive AI response.
     */
    @PostMapping("/{conversationId}/messages")
    public ResponseEntity<ApiResponse<ChatMessageResponse>> sendMessage(
            @PathVariable UUID conversationId,
            @Valid @RequestBody SendMessageRequest request) {
        ChatMessageResponse response = chatService.sendMessage(conversationId, request);
        return ResponseEntity.ok(ApiResponse.success("Message processed successfully", response));
    }

    /**
     * GET /api/v1/chat/conversations/{conversationId}/messages
     * Get conversation chat history.
     */
    @GetMapping("/{conversationId}/messages")
    public ResponseEntity<ApiResponse<List<ChatMessageResponse>>> getConversationHistory(
            @PathVariable UUID conversationId) {
        List<ChatMessageResponse> history = chatService.getConversationHistory(conversationId);
        return ResponseEntity.ok(ApiResponse.success("Chat history fetched successfully", history));
    }

    /**
     * GET /api/v1/chat/conversations/user/{userId}
     * Get all conversations belonging to a user.
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<ApiResponse<List<ConversationResponse>>> getUserConversations(
            @PathVariable String userId) {
        List<ConversationResponse> conversations = chatService.getUserConversations(userId);
        return ResponseEntity.ok(ApiResponse.success("Conversations fetched successfully", conversations));
    }
}
