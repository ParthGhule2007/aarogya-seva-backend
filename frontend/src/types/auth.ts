export type UserRole = 'PATIENT' | 'ASHA' | 'DOCTOR' | 'ADMIN' | 'CAREGIVER';

export interface UserDto {
  id?: string;
  username: string;
  fullName: string;
  phoneNumber?: string;
  email?: string;
  role: UserRole;
  associatedEntityId?: string;
}

export interface AuthResponse {
  token: string;
  tokenType?: string;
  expiresIn?: number;
  user?: UserDto;
  // Flattened aliases for backwards compatibility with demo mock accounts
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
}

export type LoginResponse = AuthResponse;

export interface RegisterRequest {
  username: string;
  password: string;
  fullName: string;
  displayName?: string; // fallback alias
  phoneNumber?: string;
  contactNumber?: string; // fallback alias
  email?: string;
  role?: UserRole;
  associatedEntityId?: string;
  patientId?: string;
  doctorId?: string;
  facilityId?: string;
}

