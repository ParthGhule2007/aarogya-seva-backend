import { apiClient } from './client';
import { AvailableDoctorResponseDTO, DoctorResponseDTO, DoctorRequestDTO, DoctorAvailabilityStatus } from '../types/doctor';

export const doctorApi = {
  /**
   * GET /api/doctors
   */
  getAllDoctors: (): Promise<DoctorResponseDTO[]> =>
    apiClient.get<DoctorResponseDTO[]>('/api/doctors'),

  /**
   * GET /api/doctors/available
   * Discovers all ACTIVE and AVAILABLE doctors for consultation.
   */
  getAvailableDoctors: (): Promise<AvailableDoctorResponseDTO[]> =>
    apiClient.get<AvailableDoctorResponseDTO[]>('/api/doctors/available'),

  /**
   * GET /api/doctors/{doctorId}
   * Retrieves full profile details for a doctor by Long doctorId.
   */
  getDoctorById: (doctorId: number): Promise<DoctorResponseDTO> =>
    apiClient.get<DoctorResponseDTO>(`/api/doctors/${doctorId}`),

  /**
   * POST /api/doctors
   * Registers a clinical doctor profile for a user account.
   */
  createDoctor: (data: DoctorRequestDTO): Promise<DoctorResponseDTO> =>
    apiClient.post<DoctorResponseDTO>('/api/doctors', data),

  /**
   * GET /api/doctors/user/{userId}
   * Retrieves doctor profile by string or numeric userId.
   */
  getDoctorByUserId: (userId: string | number): Promise<DoctorResponseDTO> =>
    apiClient.get<DoctorResponseDTO>(`/api/doctors/user/${userId}`),

  /**
   * PATCH /api/doctors/{doctorId}/availability
   */
  updateAvailability: (doctorId: number, status: DoctorAvailabilityStatus): Promise<DoctorResponseDTO> =>
    apiClient.patch<DoctorResponseDTO>(`/api/doctors/${doctorId}/availability`, { availabilityStatus: status }),
};
