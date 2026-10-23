import { request } from './client';
import { SERVICE_URLS } from '../config/apiConfig';
import {
  CreateSosRequest,
  NearbyAlertDto,
  SosResponse,
  SosStatusResponse,
  UpdateLocationPayload,
} from '../types/sos';

export const sosApi = {
  /**
   * Trigger an Emergency SOS alert.
   */
  triggerSos: async (req: CreateSosRequest, idempotencyKey?: string): Promise<SosResponse> => {
    const headers: Record<string, string> = {};
    if (idempotencyKey) {
      headers['Idempotency-Key'] = idempotencyKey;
    }

    return request<SosResponse>(
      SERVICE_URLS.SOS,
      '/api/v1/sos',
      {
        method: 'POST',
        headers,
        body: JSON.stringify(req),
      }
    );
  },

  /**
   * Get SOS status.
   */
  getSosStatus: async (sosId: string): Promise<SosStatusResponse> => {
    return request<SosStatusResponse>(
      SERVICE_URLS.SOS,
      `/api/v1/sos/${sosId}`
    );
  },

  /**
   * Resolve SOS.
   */
  resolveSos: async (sosId: string): Promise<SosStatusResponse> => {
    return request<SosStatusResponse>(
      SERVICE_URLS.SOS,
      `/api/v1/sos/${sosId}/resolve`,
      { method: 'POST' }
    );
  },

  /**
   * Get all active (non-resolved) SOS alerts across the platform.
   */
  getActiveSosAlerts: async (): Promise<SosStatusResponse[]> => {
    return request<SosStatusResponse[]>(
      SERVICE_URLS.SOS,
      '/api/v1/sos/active'
    );
  },

  /**
   * Get SOS alerts for a specific patient.
   */
  getSosByPatient: async (patientId: string): Promise<SosStatusResponse[]> => {
    return request<SosStatusResponse[]>(
      SERVICE_URLS.SOS,
      `/api/v1/sos/patient/${encodeURIComponent(patientId)}`
    );
  },

  /**
   * Get SOS alerts targeting a specific facility.
   */
  getSosByFacility: async (facilityId: string): Promise<SosStatusResponse[]> => {
    return request<SosStatusResponse[]>(
      SERVICE_URLS.SOS,
      `/api/v1/sos/facility/${encodeURIComponent(facilityId)}`
    );
  },

  /**
   * Update user live location and emergency alert opt-in preference.
   */
  updateLocation: async (payload: UpdateLocationPayload): Promise<any> => {
    return request<any>(
      SERVICE_URLS.SOS,
      '/api/v1/devices/location',
      {
        method: 'POST',
        body: JSON.stringify(payload),
      }
    );
  },

  /**
   * Get active nearby emergency alerts for a user (within 5 km and created < 10 min ago).
   */
  getUserAlerts: async (userId: string): Promise<NearbyAlertDto[]> => {
    return request<NearbyAlertDto[]>(
      SERVICE_URLS.SOS,
      `/api/v1/sos/alerts/user/${encodeURIComponent(userId)}`
    );
  },
};
