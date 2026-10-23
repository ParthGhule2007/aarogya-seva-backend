import { apiClient } from './client';
import {
  AuthResponse,
  AuthUser,
  ForgotPasswordRequest,
  LoginResponse,
  RegisterRequest,
  UpdateProfileRequest,
  UserDto,
} from '../types/auth';

/**
 * Normalizes backend AuthResponse, UserDto, or demo user mock objects into a standardized AuthUser
 */
export function normalizeAuthUser(raw: any): AuthUser {
  if (!raw) {
    return {
      username: '',
      email: '',
      role: 'PATIENT',
      displayName: 'Patient User',
    };
  }

  // Handle nested user object if present (e.g. AuthResponse.user)
  const userObj: Partial<UserDto> & Record<string, any> = raw.user || raw;

  const role = userObj.role || raw.role || 'PATIENT';
  const username = userObj.username || raw.username || '';
  const fullName = userObj.fullName || raw.displayName || raw.fullName || username;
  const phoneNumber = userObj.phoneNumber || raw.contactNumber || raw.phoneNumber || '';
  const email = userObj.email || raw.email || '';
  const associatedId = userObj.associatedEntityId || raw.associatedEntityId;

  const patientId = role === 'PATIENT' ? (raw.patientId || associatedId) : undefined;
  const doctorId = role === 'DOCTOR' ? (raw.doctorId || (typeof associatedId === 'number' ? String(associatedId) : associatedId)) : undefined;
  const facilityId = (role === 'ADMIN' || role === 'FACILITY_ADMIN') ? (raw.facilityId || associatedId) : undefined;

  return {
    id: userObj.id || raw.id,
    username,
    email,
    role,
    displayName: fullName,
    fullName,
    contactNumber: phoneNumber,
    phoneNumber,
    patientId: patientId || undefined,
    doctorId,
    facilityId,
    associatedEntityId: associatedId || undefined,
  };
}

export const authApi = {
  login: async (username: string, password: string): Promise<LoginResponse> => {
    return apiClient.post<LoginResponse>('/api/v1/auth/login', { username, password });
  },

  register: async (request: RegisterRequest): Promise<AuthResponse> => {
    const payload = {
      username: request.username,
      password: request.password,
      fullName: request.fullName || request.displayName || request.username,
      phoneNumber: request.phoneNumber || request.contactNumber || undefined,
      email: request.email || undefined,
      role: request.role || 'PATIENT',
      associatedEntityId: request.associatedEntityId || request.patientId || undefined,
    };
    return apiClient.post<AuthResponse>('/api/v1/auth/register', payload);
  },

  getMe: async (): Promise<AuthUser> => {
    const raw = await apiClient.get<any>('/api/v1/auth/me');
    return normalizeAuthUser(raw);
  },

  forgotPassword: async (request: ForgotPasswordRequest): Promise<void> => {
    const payload = {
      username: request.username,
      phoneOrEmail: request.phoneOrEmail || request.email || request.phoneNumber,
      newPassword: request.newPassword,
    };
    await apiClient.post<any>('/api/v1/auth/forgot-password', payload);
  },

  updateProfile: async (request: UpdateProfileRequest): Promise<AuthUser> => {
    const raw = await apiClient.put<any>('/api/v1/auth/profile', request);
    return normalizeAuthUser(raw);
  },

  updateLocation: async (latitude: number, longitude: number): Promise<AuthUser> => {
    const raw = await apiClient.patch<any>('/api/v1/auth/location', { latitude, longitude });
    return normalizeAuthUser(raw);
  },
};

