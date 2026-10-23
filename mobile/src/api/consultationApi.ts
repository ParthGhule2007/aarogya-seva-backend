import { request } from './client';
import { SERVICE_URLS } from '../config/apiConfig';
import {
  ConsultationRequestDTO,
  ConsultationResponseDTO,
  CancelConsultationDTO,
} from '../types/consultation';

export const consultationApi = {
  createConsultation: (
    req: ConsultationRequestDTO
  ): Promise<ConsultationResponseDTO> =>
    request<ConsultationResponseDTO>(
      SERVICE_URLS.CONSULTATION,
      '/api/consultations',
      {
        method: 'POST',
        body: JSON.stringify(req),
      }
    ),

  getConsultationById: (id: number): Promise<ConsultationResponseDTO> =>
    request<ConsultationResponseDTO>(
      SERVICE_URLS.CONSULTATION,
      `/api/consultations/${id}`
    ),

  getConsultationsByUserId: (
    userId: string
  ): Promise<ConsultationResponseDTO[]> =>
    request<ConsultationResponseDTO[]>(
      SERVICE_URLS.CONSULTATION,
      `/api/consultations/user/${encodeURIComponent(userId)}`
    ),

  getPendingConsultationsByDoctorId: (
    doctorId: number
  ): Promise<ConsultationResponseDTO[]> =>
    request<ConsultationResponseDTO[]>(
      SERVICE_URLS.CONSULTATION,
      `/api/consultations/doctor/${doctorId}/pending`
    ),

  getConsultationsByDoctorId: (
    doctorId: number
  ): Promise<ConsultationResponseDTO[]> =>
    request<ConsultationResponseDTO[]>(
      SERVICE_URLS.CONSULTATION,
      `/api/consultations/doctor/${doctorId}`
    ),

  cancelConsultation: (
    id: number,
    cancelDto: CancelConsultationDTO
  ): Promise<ConsultationResponseDTO> =>
    request<ConsultationResponseDTO>(
      SERVICE_URLS.CONSULTATION,
      `/api/consultations/${id}/cancel`,
      {
        method: 'PUT',
        body: JSON.stringify(cancelDto),
      }
    ),

  acceptConsultation: (id: number): Promise<ConsultationResponseDTO> =>
    request<ConsultationResponseDTO>(
      SERVICE_URLS.CONSULTATION,
      `/api/consultations/${id}/accept`,
      { method: 'PUT' }
    ),

  rejectConsultation: (id: number): Promise<ConsultationResponseDTO> =>
    request<ConsultationResponseDTO>(
      SERVICE_URLS.CONSULTATION,
      `/api/consultations/${id}/reject`,
      { method: 'PUT' }
    ),

  markAsReady: (id: number): Promise<ConsultationResponseDTO> =>
    request<ConsultationResponseDTO>(
      SERVICE_URLS.CONSULTATION,
      `/api/consultations/${id}/ready`,
      { method: 'PUT' }
    ),

  startConsultation: (id: number): Promise<ConsultationResponseDTO> =>
    request<ConsultationResponseDTO>(
      SERVICE_URLS.CONSULTATION,
      `/api/consultations/${id}/start`,
      { method: 'PUT' }
    ),

  completeConsultation: (id: number): Promise<ConsultationResponseDTO> =>
    request<ConsultationResponseDTO>(
      SERVICE_URLS.CONSULTATION,
      `/api/consultations/${id}/complete`,
      { method: 'PUT' }
    ),
};
