export type MessageRole = 'USER' | 'ASSISTANT' | 'SYSTEM';

export interface CreateConversationRequest {
  userId: string;
  username: string;
  age: number;
}

export interface ConversationResponse {
  conversationId: string;
  createdAt: string;
  updatedAt?: string;
}

export interface SendMessageRequest {
  userId: string;
  message: string;
}

export interface ChatMessageResponse {
  messageId: string;
  role: MessageRole;
  content: string;
  createdAt: string;
}
