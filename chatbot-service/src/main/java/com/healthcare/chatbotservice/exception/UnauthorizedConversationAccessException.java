package com.healthcare.chatbotservice.exception;

import java.util.UUID;

public class UnauthorizedConversationAccessException extends RuntimeException {

    public UnauthorizedConversationAccessException(String userId, UUID conversationId) {
        super(String.format("User %s is not authorized to access conversation %s", userId, conversationId));
    }

    public UnauthorizedConversationAccessException(String message) {
        super(message);
    }
}
