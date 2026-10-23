import { request } from './client';
import { SERVICE_URLS } from '../config/apiConfig';
import { DeviceRegistrationRequest, DeviceRegistrationResponse } from '../types/sos';

export const deviceApi = {
  /**
   * Device registration stub for FCM push tokens (used in production standalone build).
   */
  registerDevice: async (
    req: DeviceRegistrationRequest
  ): Promise<DeviceRegistrationResponse> => {
    return request<DeviceRegistrationResponse>(
      SERVICE_URLS.SOS,
      '/api/v1/devices/register',
      {
        method: 'POST',
        body: JSON.stringify(req),
      }
    );
  },
};
