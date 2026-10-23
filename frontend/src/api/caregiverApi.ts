import { apiClient } from './client';
import { Caregiver, CaregiverEscalation } from '../types/medicine';

export const caregiverApi = {
  getCaregivers: async (patientId: string): Promise<Caregiver[]> => {
    return apiClient.get<Caregiver[]>(`/api/v1/caregivers/patient/${patientId}`);
  },

  addCaregiver: async (caregiver: Partial<Caregiver>): Promise<Caregiver> => {
    return apiClient.post<Caregiver>('/api/v1/caregivers', caregiver);
  },

  updateCaregiver: async (id: string, caregiver: Partial<Caregiver>): Promise<Caregiver> => {
    return apiClient.put<Caregiver>(`/api/v1/caregivers/${id}`, caregiver);
  },

  deleteCaregiver: async (id: string): Promise<void> => {
    return apiClient.delete<void>(`/api/v1/caregivers/${id}`);
  },

  getEscalations: async (patientId: string): Promise<CaregiverEscalation[]> => {
    return apiClient.get<CaregiverEscalation[]>(`/api/v1/caregivers/escalations/patient/${patientId}`);
  },
};
