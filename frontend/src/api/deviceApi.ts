import { apiClient } from './client';
import { DeviceRegistrationRequest, DeviceRegistrationResponse } from '../types/sos';

export const deviceApi = {
  registerDevice: async (req: DeviceRegistrationRequest): Promise<DeviceRegistrationResponse> => {
    return apiClient.post<DeviceRegistrationResponse>('/api/v1/sos/devices/register', req);
  },

  deactivateDevice: async (token: string): Promise<void> => {
    return apiClient.delete<void>(`/api/v1/sos/devices/${encodeURIComponent(token)}`);
  },
};
