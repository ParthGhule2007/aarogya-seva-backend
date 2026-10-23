import { request } from './client';
import { SERVICE_URLS } from '../config/apiConfig';
import {
  CreateConversationRequest,
  ConversationResponse,
  SendMessageRequest,
  ChatMessageResponse,
} from '../types/chat';

export const chatApi = {
  createConversation: async (req: CreateConversationRequest): Promise<ConversationResponse> => {
    return request<ConversationResponse>(
      SERVICE_URLS.CHATBOT,
      '/api/v1/chat/conversations',
      {
        method: 'POST',
        body: JSON.stringify(req),
      }
    );
  },

  sendMessage: async (
    conversationId: string,
    req: SendMessageRequest
  ): Promise<ChatMessageResponse> => {
    return request<ChatMessageResponse>(
      SERVICE_URLS.CHATBOT,
      `/api/v1/chat/conversations/${encodeURIComponent(conversationId)}/messages`,
      {
        method: 'POST',
        body: JSON.stringify(req),
        timeoutMs: 35000,
      }
    );
  },

  getMessages: async (conversationId: string): Promise<ChatMessageResponse[]> => {
    return request<ChatMessageResponse[]>(
      SERVICE_URLS.CHATBOT,
      `/api/v1/chat/conversations/${encodeURIComponent(conversationId)}/messages`
    );
  },
};
