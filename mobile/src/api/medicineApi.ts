import { request, uploadMultipart } from './client';
import { SERVICE_URLS } from '../config/apiConfig';
import {
  Medication,
  ScheduledDose,
  CreateMedicationPayload,
  UpdateMedicationPayload,
  OcrExtractionResponse,
} from '../types/medicine';

export const medicineApi = {
  /**
   * Retrieves today's scheduled dose events for a patient.
   */
  getTodaySchedule: async (patientId: string): Promise<ScheduledDose[]> => {
    return request<ScheduledDose[]>(
      SERVICE_URLS.MEDICINE,
      `/api/v1/medications/today/${patientId}`
    );
  },

  /**
   * Retrieves all active medications configured for a patient.
   */
  getMedications: async (patientId: string): Promise<Medication[]> => {
    return request<Medication[]>(
      SERVICE_URLS.MEDICINE,
      `/api/v1/medications/patient/${patientId}`
    );
  },

  /**
   * Creates a new medicine reminder without photo (JSON).
   */
  addMedication: async (payload: CreateMedicationPayload): Promise<Medication> => {
    return request<Medication>(
      SERVICE_URLS.MEDICINE,
      '/api/v1/medications',
      {
        method: 'POST',
        body: JSON.stringify(payload),
      }
    );
  },

  /**
   * Updates an existing medication reminder.
   */
  updateMedication: async (id: string, payload: UpdateMedicationPayload): Promise<Medication> => {
    return request<Medication>(
      SERVICE_URLS.MEDICINE,
      `/api/v1/medications/${id}`,
      {
        method: 'PUT',
        body: JSON.stringify(payload),
      }
    );
  },

  /**
   * Deactivates / deletes a medication schedule.
   */
  deleteMedication: async (id: string): Promise<void> => {
    return request<void>(
      SERVICE_URLS.MEDICINE,
      `/api/v1/medications/${id}`,
      {
        method: 'DELETE',
      }
    );
  },

  /**
   * Extracts text and medicine names from photo via OCR.
   * Uses XMLHttpRequest-based uploadMultipart to support Expo SDK 57 / React Native file attachments.
   */
  extractOcr: async (fileUri: string, mimeType = 'image/jpeg'): Promise<OcrExtractionResponse> => {
    const formData = new FormData();
    const rawFilename = fileUri.split('/').pop() || 'prescription.jpg';
    const filename = rawFilename.includes('.') ? rawFilename : `${rawFilename}.jpg`;

    // React Native FormData expects { uri, name, type } cast as any
    formData.append('image', {
      uri: fileUri,
      name: filename,
      type: mimeType || 'image/jpeg',
    } as any);

    return uploadMultipart<OcrExtractionResponse>(
      SERVICE_URLS.MEDICINE,
      '/api/v1/medications/extract-ocr',
      formData,
      45000
    );
  },

  /**
   * One-tap mark dose as TAKEN via Medication Event Controller.
   */
  markTaken: async (eventId: string): Promise<any> => {
    return request<any>(
      SERVICE_URLS.MEDICINE,
      `/api/v1/medication-events/${eventId}/taken`,
      { method: 'POST' }
    );
  },

  /**
   * Mark dose as MISSED.
   */
  markMissed: async (eventId: string): Promise<any> => {
    return request<any>(
      SERVICE_URLS.MEDICINE,
      `/api/v1/medication-events/${eventId}/missed`,
      { method: 'POST' }
    );
  },

  /**
   * Mark dose as SKIPPED.
   */
  markSkip: async (eventId: string): Promise<any> => {
    return request<any>(
      SERVICE_URLS.MEDICINE,
      `/api/v1/medication-events/${eventId}/skip`,
      { method: 'POST' }
    );
  },
};
