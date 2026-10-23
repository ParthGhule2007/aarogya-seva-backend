import { request } from './client';
import { SERVICE_URLS } from '../config/apiConfig';
import { Caregiver, CaregiverEscalation, CreateCaregiverPayload } from '../types/medicine';

export const caregiverApi = {
  getCaregivers: async (patientId: string): Promise<Caregiver[]> =>
    request<Caregiver[]>(
      SERVICE_URLS.MEDICINE,
      `/api/v1/caregivers/patient/${encodeURIComponent(patientId)}`
    ),

  addCaregiver: async (caregiver: CreateCaregiverPayload): Promise<Caregiver> =>
    request<Caregiver>(SERVICE_URLS.MEDICINE, '/api/v1/caregivers', {
      method: 'POST',
      body: JSON.stringify(caregiver),
    }),

  deleteCaregiver: async (id: string): Promise<void> =>
    request<void>(SERVICE_URLS.MEDICINE, `/api/v1/caregivers/${encodeURIComponent(id)}`, {
      method: 'DELETE',
    }),

  getEscalations: async (patientId: string): Promise<CaregiverEscalation[]> =>
    request<CaregiverEscalation[]>(
      SERVICE_URLS.MEDICINE,
      `/api/v1/caregivers/escalations/patient/${encodeURIComponent(patientId)}`
    ),
};
