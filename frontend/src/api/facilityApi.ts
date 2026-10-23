import { apiClient } from './client';
import { FacilityDto, FacilityRecommendationDto, FacilityType, MedicalServiceCode } from '../types/facility';

export interface CreateFacilityRequest {
  name: string;
  facilityType: FacilityType;
  latitude: number;
  longitude: number;
  addressLine1: string;
  addressLine2?: string;
  villageTown: string;
  subDistrict: string;
  district: string;
  state: string;
  pincode: string;
  contactNumber: string;
  emergencyNumber?: string;
  isOperational?: boolean;
  availableServices?: MedicalServiceCode[];
}

export interface UpdateFacilityRequest {
  name?: string;
  facilityType?: FacilityType;
  latitude?: number;
  longitude?: number;
  addressLine1?: string;
  addressLine2?: string;
  villageTown?: string;
  subDistrict?: string;
  district?: string;
  state?: string;
  pincode?: string;
  contactNumber?: string;
  emergencyNumber?: string;
  isOperational?: boolean;
  availableServices?: MedicalServiceCode[];
}

export interface SearchNearbyOptions {
  latitude: number;
  longitude: number;
  radiusKm?: number;
  services?: MedicalServiceCode[];
  facilityTypes?: FacilityType[];
  limit?: number;
}

export const facilityApi = {
  getAllFacilities: () => apiClient.get<FacilityDto[]>('/api/v1/facilities'),

  getFacilityById: (id: string) => apiClient.get<FacilityDto>(`/api/v1/facilities/${id}`),

  searchNearbyFacilities: (
    latOrOptions: number | SearchNearbyOptions,
    longitude?: number,
    radiusKm: number = 50,
    services?: MedicalServiceCode[],
    facilityTypes?: FacilityType[],
    limit: number = 10
  ) => {
    let lat: number;
    let lon: number;
    let rad: number = radiusKm;
    let srv = services;
    let types = facilityTypes;
    let lim = limit;

    if (typeof latOrOptions === 'object') {
      lat = latOrOptions.latitude;
      lon = latOrOptions.longitude;
      rad = latOrOptions.radiusKm ?? 50;
      srv = latOrOptions.services;
      types = latOrOptions.facilityTypes;
      lim = latOrOptions.limit ?? 10;
    } else {
      lat = latOrOptions;
      lon = longitude!;
    }

    const params = new URLSearchParams();
    params.append('latitude', lat.toString());
    params.append('longitude', lon.toString());
    params.append('radiusKm', rad.toString());
    params.append('limit', lim.toString());

    if (srv && srv.length > 0) {
      srv.forEach((s) => params.append('services', s));
    }
    if (types && types.length > 0) {
      types.forEach((t) => params.append('facilityTypes', t));
    }

    return apiClient.get<FacilityRecommendationDto[]>(`/api/v1/facilities/nearby?${params.toString()}`);
  },

  getNearbyFacilities: (
    latitude: number,
    longitude: number,
    radiusKm: number = 50
  ) => {
    const params = new URLSearchParams();
    params.append('latitude', latitude.toString());
    params.append('longitude', longitude.toString());
    params.append('radiusKm', radiusKm.toString());
    return apiClient.get<FacilityRecommendationDto[]>(`/api/v1/facilities/nearby?${params.toString()}`);
  },

  createFacility: (data: CreateFacilityRequest) =>
    apiClient.post<FacilityDto>('/api/v1/facilities', data),

  updateFacility: (id: string, data: UpdateFacilityRequest) =>
    apiClient.put<FacilityDto>(`/api/v1/facilities/${id}`, data),

  setOperationalStatus: (id: string, operational: boolean) =>
    apiClient.patch<void>(`/api/v1/facilities/${id}/status?operational=${operational}`),

  deleteFacility: (id: string) =>
    apiClient.delete<void>(`/api/v1/facilities/${id}`),
};
