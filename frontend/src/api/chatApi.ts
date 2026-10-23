import { apiClient } from './client';
import { CreateConversationRequest, ConversationResponse, SendMessageRequest, ChatMessageResponse } from '../types/chat';

export const chatApi = {
  createConversation: async (req: CreateConversationRequest): Promise<ConversationResponse> => {
    return apiClient.post<ConversationResponse>('/api/v1/chat/conversations', req);
  },

  sendMessage: async (conversationId: string, req: SendMessageRequest): Promise<ChatMessageResponse> => {
    return apiClient.post<ChatMessageResponse>(`/api/v1/chat/conversations/${conversationId}/messages`, req);
  },

  getMessages: async (conversationId: string): Promise<ChatMessageResponse[]> => {
    return apiClient.get<ChatMessageResponse[]>(`/api/v1/chat/conversations/${conversationId}/messages`);
  },
};
