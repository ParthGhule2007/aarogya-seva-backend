import { apiClient } from './client';
import { Medication, ScheduledDose, OcrExtractionResult } from '../types/medicine';

export const medicationApi = {
  getTodaySchedule: async (patientId: string): Promise<ScheduledDose[]> => {
    return apiClient.get<ScheduledDose[]>(`/api/v1/medications/today/${patientId}`);
  },

  getMedications: async (patientId: string): Promise<Medication[]> => {
    return apiClient.get<Medication[]>(`/api/v1/medications/patient/${patientId}`);
  },

  addMedication: async (medication: Partial<Medication>): Promise<Medication> => {
    return apiClient.post<Medication>('/api/v1/medications', medication);
  },

  extractOcr: async (imageFile: File): Promise<OcrExtractionResult> => {
    const formData = new FormData();
    formData.append('image', imageFile);
    return apiClient.upload<OcrExtractionResult>('/api/v1/medications/extract-ocr', formData);
  },

  markTaken: async (eventId: string): Promise<any> => {
    return apiClient.post(`/api/v1/medication-events/${eventId}/taken`);
  },

  markMissed: async (eventId: string): Promise<any> => {
    return apiClient.post(`/api/v1/medication-events/${eventId}/missed`);
  },

  markSkip: async (eventId: string): Promise<any> => {
    return apiClient.post(`/api/v1/medication-events/${eventId}/skip`);
  },
};
