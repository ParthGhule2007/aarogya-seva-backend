import { request } from './client';
import { SERVICE_URLS } from '../config/apiConfig';
import {
  AvailableDoctorResponseDTO,
  DoctorResponseDTO,
  DoctorAvailabilityStatus,
} from '../types/doctor';

export const doctorApi = {
  getAllDoctors: (): Promise<DoctorResponseDTO[]> =>
    request<DoctorResponseDTO[]>(SERVICE_URLS.DOCTOR, '/api/doctors'),

  getAvailableDoctors: (): Promise<AvailableDoctorResponseDTO[]> =>
    request<AvailableDoctorResponseDTO[]>(SERVICE_URLS.DOCTOR, '/api/doctors/available'),

  getDoctorById: (doctorId: number): Promise<DoctorResponseDTO> =>
    request<DoctorResponseDTO>(SERVICE_URLS.DOCTOR, `/api/doctors/${doctorId}`),

  updateAvailability: (
    doctorId: number,
    status: DoctorAvailabilityStatus
  ): Promise<DoctorResponseDTO> =>
    request<DoctorResponseDTO>(
      SERVICE_URLS.DOCTOR,
      `/api/doctors/${doctorId}/availability?coordinated=true`,
      {
        method: 'PATCH',
        body: JSON.stringify({ availabilityStatus: status }),
      }
    ),
};
