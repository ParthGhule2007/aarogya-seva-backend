import { apiClient } from './client';
import { Medication, ScheduledDose, CreateMedicationPayload, UpdateMedicationPayload, computeTimeSlot } from '../types/medicine';

export const medicationApi = {
  getTodaySchedule: async (patientId: string): Promise<ScheduledDose[]> => {
    const doses = await apiClient.get<ScheduledDose[]>(`/api/v1/medications/today/${patientId}`);
    return (doses || []).map((d) => ({
      ...d,
      eventId: d.eventId || d.id,
      medicineName: d.medicineName || d.medicationName,
      timeSlot: d.timeSlot || computeTimeSlot(d.scheduledTime),
    }));
  },

  getMedications: async (patientId: string): Promise<Medication[]> => {
    return apiClient.get<Medication[]>(`/api/v1/medications/patient/${patientId}`);
  },

  getMedicationById: async (id: string): Promise<Medication> => {
    return apiClient.get<Medication>(`/api/v1/medications/${id}`);
  },

  getHistory: async (patientId: string): Promise<ScheduledDose[]> => {
    const doses = await apiClient.get<ScheduledDose[]>(`/api/v1/medications/history/${patientId}`);
    return (doses || []).map((d) => ({
      ...d,
      eventId: d.eventId || d.id,
      medicineName: d.medicineName || d.medicationName,
      timeSlot: d.timeSlot || computeTimeSlot(d.scheduledTime),
    }));
  },

  addMedication: async (payload: CreateMedicationPayload, imageFile?: File): Promise<Medication> => {
    if (imageFile) {
      const formData = new FormData();
      formData.append('metadata', JSON.stringify(payload));
      formData.append('image', imageFile);
      return apiClient.upload<Medication>('/api/v1/medications', formData);
    }
    return apiClient.post<Medication>('/api/v1/medications', payload);
  },

  updateMedication: async (id: string, updates: UpdateMedicationPayload): Promise<Medication> => {
    return apiClient.put<Medication>(`/api/v1/medications/${id}`, updates);
  },

  discontinueMedication: async (id: string): Promise<Medication> => {
    return apiClient.put<Medication>(`/api/v1/medications/${id}/discontinue`);
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
