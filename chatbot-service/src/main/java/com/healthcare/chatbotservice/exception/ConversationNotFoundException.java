package com.healthcare.chatbotservice.exception;

import java.util.UUID;

public class ConversationNotFoundException extends RuntimeException {

    public ConversationNotFoundException(UUID conversationId) {
        super("Conversation not found with id: " + conversationId);
    }

    public ConversationNotFoundException(String message) {
        super(message);
    }
}
