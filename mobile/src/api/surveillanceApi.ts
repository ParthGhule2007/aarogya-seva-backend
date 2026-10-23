import { request } from './client';
import { SERVICE_URLS } from '../config/apiConfig';
import { OutbreakZoneDto } from '../types/surveillance';

export const surveillanceApi = {
  getOutbreakZones: (latitude?: number, longitude?: number): Promise<OutbreakZoneDto[]> => {
    const params = new URLSearchParams();
    if (latitude !== undefined && latitude !== null) {
      params.append('latitude', latitude.toString());
    }
    if (longitude !== undefined && longitude !== null) {
      params.append('longitude', longitude.toString());
    }
    const query = params.toString();
    const endpoint = query
      ? `/api/v1/surveillance/outbreak-zones?${query}`
      : '/api/v1/surveillance/outbreak-zones';
    return request<OutbreakZoneDto[]>(SERVICE_URLS.CORE, endpoint);
  },
};
