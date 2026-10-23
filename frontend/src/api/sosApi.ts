import { apiClient } from './client';
import {
  CreateSosRequest,
  SosResponse,
  SosStatusResponse,
  DeviceRegistrationRequest,
  DeviceRegistrationResponse,
} from '../types/sos';

export const sosApi = {
  /**
   * Trigger an Emergency SOS alert.
   * Sends patient location, chosen recipients, optional facility ID, and idempotency key.
   */
  triggerSos: async (
    request: CreateSosRequest,
    idempotencyKey?: string
  ): Promise<SosResponse> => {
    const headers: Record<string, string> = {
      'X-User-Id': request.patientId,
      'X-User-Role': 'PATIENT',
    };

    if (idempotencyKey) {
      headers['Idempotency-Key'] = idempotencyKey;
    }

    return apiClient.post<SosResponse>('/api/v1/sos', request, {
      headers,
    });
  },

  /**
   * Retrieve real-time status of an SOS alert.
   */
  getSosStatus: async (sosId: string): Promise<SosStatusResponse> => {
    return apiClient.get<SosStatusResponse>(`/api/v1/sos/${sosId}`);
  },

  /**
   * Mark an SOS alert as RESOLVED.
   */
  resolveSos: async (sosId: string): Promise<SosStatusResponse> => {
    return apiClient.post<SosStatusResponse>(`/api/v1/sos/${sosId}/resolve`);
  },

  /**
   * Register or update an FCM push device token and emergency alert opt-in preference.
   */
  registerDevice: async (
    request: DeviceRegistrationRequest
  ): Promise<DeviceRegistrationResponse> => {
    const headers: Record<string, string> = {
      'X-User-Id': request.userId,
      'X-User-Role': request.userType,
    };

    return apiClient.post<DeviceRegistrationResponse>(
      '/api/v1/sos/devices/register',
      request,
      { headers }
    );
  },

  /**
   * Deactivate an FCM device token.
   */
  deactivateDevice: async (token: string): Promise<void> => {
    return apiClient.delete<void>(`/api/v1/sos/devices/${encodeURIComponent(token)}`);
  },

  /**
   * Retrieve SOS events targeted at a specific healthcare facility.
   */
  getSosByFacility: async (facilityId: string): Promise<SosStatusResponse[]> => {
    return apiClient.get<SosStatusResponse[]>(`/api/v1/sos/facility/${encodeURIComponent(facilityId)}`);
  },

  /**
   * Retrieve SOS events triggered by a specific patient.
   */
  getSosByPatient: async (patientId: string): Promise<SosStatusResponse[]> => {
    return apiClient.get<SosStatusResponse[]>(`/api/v1/sos/patient/${encodeURIComponent(patientId)}`);
  },
};
