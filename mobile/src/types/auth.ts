export type UserRole = 'PATIENT' | 'ASHA' | 'DOCTOR' | 'ADMIN' | 'FACILITY_ADMIN' | 'CAREGIVER';

export interface UserDto {
  id?: string;
  username: string;
  fullName: string;
  phoneNumber?: string;
  email?: string;
  role: UserRole;
  associatedEntityId?: string;
  age?: number;
  dob?: string;
  heightCm?: number;
  weightKg?: number;
  medicalHistory?: string;
}

export interface AuthResponse {
  token: string;
  tokenType?: string;
  expiresIn?: number;
  user?: UserDto;
  id?: string;
  username?: string;
  email?: string;
  role?: UserRole;
  displayName?: string;
  fullName?: string;
  contactNumber?: string;
  phoneNumber?: string;
  patientId?: string;
  doctorId?: string;
  facilityId?: string;
  associatedEntityId?: string;
  age?: number;
  dob?: string;
  heightCm?: number;
  weightKg?: number;
  medicalHistory?: string;
  latitude?: number;
  longitude?: number;
}

export interface AuthUser {
  id?: string;
  username: string;
  email: string;
  role: UserRole;
  displayName: string;
  fullName?: string;
  contactNumber?: string;
  phoneNumber?: string;
  patientId?: string;
  doctorId?: string;
  facilityId?: string;
  associatedEntityId?: string;
  age?: number;
  dob?: string;
  heightCm?: number;
  weightKg?: number;
  medicalHistory?: string;
  latitude?: number;
  longitude?: number;
}

export type LoginResponse = AuthResponse;

export interface RegisterRequest {
  username: string;
  password: string;
  fullName: string;
  displayName?: string;
  phoneNumber?: string;
  contactNumber?: string;
  email?: string;
  role?: UserRole;
  associatedEntityId?: string;
  patientId?: string;
  doctorId?: string;
  facilityId?: string;
  gender?: string;
  age?: number;
  heightCm?: number;
  weightKg?: number;
  medicalHistory?: string;
  villageTown?: string;
  district?: string;
  subDistrict?: string;
  state?: string;
  pincode?: string;
  specialization?: string;
  licenseNumber?: string;
  experienceYears?: number;
  qualification?: string;
  hospitalName?: string;
  linkedPatientPhone?: string;
  linkedPatientUsername?: string;
}
