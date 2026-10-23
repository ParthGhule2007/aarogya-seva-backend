import { request } from './client';
import { SERVICE_URLS } from '../config/apiConfig';
import {
  FacilityDto,
  FacilityRecommendationDto,
  FacilityType,
  MedicalServiceCode,
} from '../types/facility';

export interface SearchNearbyOptions {
  latitude: number;
  longitude: number;
  radiusKm?: number;
  services?: MedicalServiceCode[];
  facilityTypes?: FacilityType[];
  limit?: number;
}

export const facilityApi = {
  getAllFacilities: () =>
    request<FacilityDto[]>(SERVICE_URLS.CORE, '/api/v1/facilities'),

  getFacilityById: (id: string) =>
    request<FacilityDto>(SERVICE_URLS.CORE, `/api/v1/facilities/${id}`),

  searchNearbyFacilities: (options: SearchNearbyOptions) => {
    const params = new URLSearchParams();
    params.append('latitude', options.latitude.toString());
    params.append('longitude', options.longitude.toString());
    params.append('radiusKm', (options.radiusKm ?? 30).toString());
    params.append('limit', (options.limit ?? 15).toString());

    if (options.services && options.services.length > 0) {
      options.services.forEach((s) => params.append('services', s));
    }
    if (options.facilityTypes && options.facilityTypes.length > 0) {
      options.facilityTypes.forEach((t) => params.append('facilityTypes', t));
    }

    return request<FacilityRecommendationDto[]>(
      SERVICE_URLS.CORE,
      `/api/v1/facilities/nearby?${params.toString()}`
    );
  },

  getNearbyFacilities: (latitude: number, longitude: number, radiusKm = 30) =>
    facilityApi.searchNearbyFacilities({ latitude, longitude, radiusKm }),

  createFacility: (payload: {
    name: string;
    facilityType: FacilityType;
    latitude: number;
    longitude: number;
    addressLine1: string;
    addressLine2?: string;
    villageTown: string;
    subDistrict?: string;
    district: string;
    state: string;
    pincode: string;
    contactNumber: string;
    emergencyNumber?: string;
    isOperational?: boolean;
  }) =>
    request<FacilityDto>(SERVICE_URLS.CORE, '/api/v1/facilities', {
      method: 'POST',
      body: JSON.stringify({
        isOperational: true,
        ...payload,
      }),
    }),
};
