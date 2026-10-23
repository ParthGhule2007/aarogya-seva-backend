export type FacilityType =
  | 'SUB_CENTRE'
  | 'PHC'
  | 'CHC'
  | 'SUB_DISTRICT_HOSPITAL'
  | 'DISTRICT_HOSPITAL'
  | 'TERTIARY_HOSPITAL';

export type MedicalServiceCode =
  | 'GENERAL_OPD'
  | 'EMERGENCY'
  | 'MATERNAL_HEALTH'
  | 'PEDIATRIC_CARE'
  | 'INPATIENT_BEDS'
  | 'ICU_BEDS'
  | 'SURGICAL_UNIT'
  | 'DIAGNOSTIC_LAB'
  | 'RADIOLOGY_XRAY'
  | 'BLOOD_STORAGE_BANK'
  | 'TELEMEDICINE';

export interface FacilityDto {
  id: string;
  name: string;
  facilityName?: string;
  facilityType: FacilityType;
  facilityTypeDisplayName?: string;
  latitude?: number;
  longitude?: number;
  addressLine1?: string;
  addressLine2?: string;
  villageTown: string;
  subDistrict: string;
  district: string;
  state: string;
  pincode: string;
  address?: {
    village?: string;
    city?: string;
    district?: string;
    state?: string;
    pincode?: string;
  };
  contactNumber: string;
  emergencyNumber?: string;
  isOperational: boolean;
  operationalStatus?: string;
  isDemoData: boolean;
  availableServices: MedicalServiceCode[];
  services?: any[];
  distanceKm?: number;
  createdAt?: string;
  updatedAt?: string;
}

export interface FacilityRecommendationDto {
  id?: string;
  facilityId: string;
  name: string;
  facilityName?: string;
  facilityType: FacilityType;
  facilityTypeDisplayName?: string;
  addressSummary?: string;
  villageTown: string;
  village?: string;
  district: string;
  state: string;
  pincode: string;
  address?: {
    village?: string;
    city?: string;
    district?: string;
    state?: string;
    pincode?: string;
  };
  latitude?: number;
  longitude?: number;
  distanceKm?: number;
  contactNumber: string;
  emergencyNumber?: string;
  isOperational: boolean;
  operationalStatus?: string;
  isDemoData: boolean;
  availableServices: MedicalServiceCode[];
  services?: any[];
}

export type HealthcareFacilityDto = FacilityRecommendationDto;
