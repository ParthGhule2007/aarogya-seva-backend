import { request } from './client';
import { SERVICE_URLS } from '../config/apiConfig';
import { AuthResponse, AuthUser, LoginResponse, RegisterRequest, UserDto } from '../types/auth';

export function normalizeAuthUser(raw: any): AuthUser {
  if (!raw) {
    return {
      username: '',
      email: '',
      role: 'PATIENT',
      displayName: 'Patient User',
    };
  }

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
    age: userObj.age || raw.age || raw.patientProfile?.age || raw.profile?.age || undefined,
  };
}

export const authApi = {
  login: async (username: string, password: string): Promise<LoginResponse> => {
    return request<LoginResponse>(SERVICE_URLS.CORE, '/api/v1/auth/login', {
      method: 'POST',
      body: JSON.stringify({ username, password }),
    });
  },

  register: async (req: RegisterRequest): Promise<AuthResponse> => {
    return request<AuthResponse>(SERVICE_URLS.CORE, '/api/v1/auth/register', {
      method: 'POST',
      body: JSON.stringify(req),
    });
  },

  linkEntity: async (associatedEntityId: string): Promise<AuthResponse> => {
    return request<AuthResponse>(SERVICE_URLS.CORE, '/api/v1/auth/link-entity', {
      method: 'PUT',
      body: JSON.stringify({ associatedEntityId }),
    });
  },

  getMe: async (): Promise<AuthUser> => {
    const raw = await request<any>(SERVICE_URLS.CORE, '/api/v1/auth/me');
    return normalizeAuthUser(raw);
  },

  googleAuth: async (payload: { idToken: string; role?: string; [key: string]: any }): Promise<any> => {
    return request<any>(SERVICE_URLS.OAUTH, '/api/v1/auth/google', {
      method: 'POST',
      body: JSON.stringify(payload),
    });
  },
};
