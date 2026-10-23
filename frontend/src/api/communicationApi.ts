import { apiClient } from './client';
import {
  CommunicationSessionResponseDTO,
  CreateCommunicationSessionDTO,
} from '../types/communication';

export const communicationApi = {
  /**
   * GET /api/communications/sessions/{id}
   * Retrieves a communication session by session ID.
   */
  getSessionById: (id: number): Promise<CommunicationSessionResponseDTO> =>
    apiClient.get<CommunicationSessionResponseDTO>(`/api/communications/sessions/${id}`),

  /**
   * GET /api/communications/sessions/consultation/{consultationId}
   * Retrieves the active communication session created by Consultation Service.
   */
  getSessionByConsultationId: (
    consultationId: number
  ): Promise<CommunicationSessionResponseDTO> =>
    apiClient.get<CommunicationSessionResponseDTO>(
      `/api/communications/sessions/consultation/${consultationId}`
    ),

  /**
   * PUT /api/communications/sessions/{id}/start
   * Starts a communication session (CREATED -> ACTIVE).
   */
  startSession: (id: number): Promise<CommunicationSessionResponseDTO> =>
    apiClient.put<CommunicationSessionResponseDTO>(`/api/communications/sessions/${id}/start`),

  /**
   * PUT /api/communications/sessions/{id}/end
   * Ends an active communication session (ACTIVE -> ENDED).
   */
  endSession: (id: number): Promise<CommunicationSessionResponseDTO> =>
    apiClient.put<CommunicationSessionResponseDTO>(`/api/communications/sessions/${id}/end`),

  /**
   * PUT /api/communications/sessions/{id}/cancel
   * Cancels a communication session.
   */
  cancelSession: (id: number): Promise<CommunicationSessionResponseDTO> =>
    apiClient.put<CommunicationSessionResponseDTO>(`/api/communications/sessions/${id}/cancel`),
};
