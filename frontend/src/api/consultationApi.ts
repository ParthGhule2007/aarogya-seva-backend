import { apiClient } from './client';
import {
  ConsultationRequestDTO,
  ConsultationResponseDTO,
  CancelConsultationDTO,
} from '../types/consultation';

export const consultationApi = {
  /**
   * POST /api/consultations
   * Requests a new consultation with a doctor.
   */
  createConsultation: (
    request: ConsultationRequestDTO
  ): Promise<ConsultationResponseDTO> =>
    apiClient.post<ConsultationResponseDTO>('/api/consultations', request),

  /**
   * GET /api/consultations/{id}
   * Retrieves consultation details by Consultation Long ID.
   */
  getConsultationById: (id: number): Promise<ConsultationResponseDTO> =>
    apiClient.get<ConsultationResponseDTO>(`/api/consultations/${id}`),

  /**
   * GET /api/consultations/user/{userId}
   * Retrieves all consultations for a patient/user by Core Patient UUID (as string).
   */
  getConsultationsByUserId: (
    userId: string
  ): Promise<ConsultationResponseDTO[]> =>
    apiClient.get<ConsultationResponseDTO[]>(
      `/api/consultations/user/${encodeURIComponent(userId)}`
    ),

  /**
   * GET /api/consultations/doctor/{doctorId}/pending
   * Retrieves pending consultation requests for a doctor.
   */
  getPendingConsultationsByDoctorId: (
    doctorId: number
  ): Promise<ConsultationResponseDTO[]> =>
    apiClient.get<ConsultationResponseDTO[]>(
      `/api/consultations/doctor/${doctorId}/pending`
    ),

  /**
   * GET /api/consultations/doctor/{doctorId}
   * Retrieves all consultations for a doctor (pending, scheduled, active, completed).
   */
  getConsultationsByDoctorId: (
    doctorId: number
  ): Promise<ConsultationResponseDTO[]> =>
    apiClient.get<ConsultationResponseDTO[]>(
      `/api/consultations/doctor/${doctorId}`
    ),

  /**
   * PUT /api/consultations/{id}/schedule
   * Schedules a consultation for a specific date and time.
   */
  scheduleConsultation: (
    id: number,
    scheduledAt: string
  ): Promise<ConsultationResponseDTO> =>
    apiClient.put<ConsultationResponseDTO>(`/api/consultations/${id}/schedule`, {
      scheduledAt,
    }),

  /**
   * PUT /api/consultations/{id}/cancel
   * Patient cancels an active consultation.
   */
  cancelConsultation: (
    id: number,
    cancelDto: CancelConsultationDTO
  ): Promise<ConsultationResponseDTO> =>
    apiClient.put<ConsultationResponseDTO>(
      `/api/consultations/${id}/cancel`,
      cancelDto
    ),

  /**
   * PUT /api/consultations/{id}/accept
   * Doctor accepts consultation.
   */
  acceptConsultation: (id: number): Promise<ConsultationResponseDTO> =>
    apiClient.put<ConsultationResponseDTO>(`/api/consultations/${id}/accept`),

  /**
   * PUT /api/consultations/{id}/reject
   * Doctor rejects consultation.
   */
  rejectConsultation: (id: number): Promise<ConsultationResponseDTO> =>
    apiClient.put<ConsultationResponseDTO>(`/api/consultations/${id}/reject`),

  /**
   * PUT /api/consultations/{id}/ready
   * Transitions consultation to READY state.
   */
  markAsReady: (id: number): Promise<ConsultationResponseDTO> =>
    apiClient.put<ConsultationResponseDTO>(`/api/consultations/${id}/ready`),

  /**
   * PUT /api/consultations/{id}/start
   * Starts consultation call (READY -> IN_CALL).
   */
  startConsultation: (id: number): Promise<ConsultationResponseDTO> =>
    apiClient.put<ConsultationResponseDTO>(`/api/consultations/${id}/start`),

  /**
   * PUT /api/consultations/{id}/complete
   * Ends consultation (IN_CALL -> COMPLETED).
   */
  completeConsultation: (id: number): Promise<ConsultationResponseDTO> =>
    apiClient.put<ConsultationResponseDTO>(`/api/consultations/${id}/complete`),
};
