export type Gender = 'MALE' | 'FEMALE' | 'OTHER';

export interface PatientDto {
  id: string;
  fullName: string;
  gender: Gender;
  dateOfBirth?: string;
  age?: number;
  contactNumber: string;
  emergencyContactName?: string;
  emergencyContactNumber?: string;
  emergencyContact?: {
    contactName?: string;
    contactNumber?: string;
    relationship?: string;
  };
  addressLine1?: string;
  addressLine2?: string;
  villageTown?: string;
  village?: string;
  subDistrict?: string;
  district?: string;
  state?: string;
  pincode?: string;
  address?: {
    village?: string;
    subDistrict?: string;
    district?: string;
    state?: string;
    pincode?: string;
    gpsLocation?: {
      latitude?: number;
      longitude?: number;
    };
  };
  latitude?: number | null;
  longitude?: number | null;
  bloodGroup?: string;
  createdAt?: string;
  updatedAt?: string;
}

export interface CreatePatientRequest {
  fullName: string;
  gender: Gender;
  dateOfBirth?: string;
  age?: number;
  contactNumber: string;
  emergencyContactName?: string;
  emergencyContactNumber?: string;
  emergencyContact?: {
    contactName?: string;
    contactNumber?: string;
    relationship?: string;
  };
  addressLine1?: string;
  addressLine2?: string;
  villageTown?: string;
  village?: string;
  subDistrict?: string;
  district?: string;
  state?: string;
  pincode?: string;
  address?: {
    village?: string;
    subDistrict?: string;
    district?: string;
    state?: string;
    pincode?: string;
    gpsLocation?: {
      latitude?: number;
      longitude?: number;
    };
  };
  latitude?: number;
  longitude?: number;
  bloodGroup?: string;
}

export interface UpdatePatientRequest {
  fullName?: string;
  contactNumber?: string;
  emergencyContactName?: string;
  emergencyContactNumber?: string;
  addressLine1?: string;
  addressLine2?: string;
  villageTown?: string;
  subDistrict?: string;
  district?: string;
  state?: string;
  pincode?: string;
  latitude?: number;
  longitude?: number;
  bloodGroup?: string;
}
