import { request } from './client';
import { SERVICE_URLS } from '../config/apiConfig';
import {
  CommunicationSessionResponseDTO,
  CreateCommunicationSessionDTO,
} from '../types/communication';

export const communicationApi = {
  createSession: (
    data: CreateCommunicationSessionDTO
  ): Promise<CommunicationSessionResponseDTO> =>
    request<CommunicationSessionResponseDTO>(
      SERVICE_URLS.COMMUNICATION,
      '/api/communications/sessions',
      {
        method: 'POST',
        body: JSON.stringify(data),
      }
    ),

  getSessionById: (id: number): Promise<CommunicationSessionResponseDTO> =>
    request<CommunicationSessionResponseDTO>(
      SERVICE_URLS.COMMUNICATION,
      `/api/communications/sessions/${id}`
    ),

  getSessionByConsultationId: (
    consultationId: number
  ): Promise<CommunicationSessionResponseDTO> =>
    request<CommunicationSessionResponseDTO>(
      SERVICE_URLS.COMMUNICATION,
      `/api/communications/sessions/consultation/${consultationId}`
    ),

  startSession: (id: number): Promise<CommunicationSessionResponseDTO> =>
    request<CommunicationSessionResponseDTO>(
      SERVICE_URLS.COMMUNICATION,
      `/api/communications/sessions/${id}/start`,
      { method: 'PUT' }
    ),

  endSession: (id: number): Promise<CommunicationSessionResponseDTO> =>
    request<CommunicationSessionResponseDTO>(
      SERVICE_URLS.COMMUNICATION,
      `/api/communications/sessions/${id}/end`,
      { method: 'PUT' }
    ),

  cancelSession: (id: number): Promise<CommunicationSessionResponseDTO> =>
    request<CommunicationSessionResponseDTO>(
      SERVICE_URLS.COMMUNICATION,
      `/api/communications/sessions/${id}/cancel`,
      { method: 'PUT' }
    ),
};
