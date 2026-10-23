import { apiClient } from './client';
import {
  CreateReferralRequest,
  ReferralDetailDto,
  ReferralResponseDto,
  ReferralStatus,
  UpdateReferralStatusRequest,
} from '../types/referral';

export const referralApi = {
  createReferral: (data: CreateReferralRequest): Promise<ReferralResponseDto> =>
    apiClient.post<ReferralResponseDto>('/api/v1/referrals', data),

  getReferralById: (id: string): Promise<ReferralDetailDto> =>
    apiClient.get<ReferralDetailDto>(`/api/v1/referrals/${id}`),

  getReferralsByCaseId: (caseId: string): Promise<ReferralResponseDto[]> =>
    apiClient.get<ReferralResponseDto[]>(`/api/v1/referrals/case/${caseId}`),

  getReferralsByCase: (caseId: string): Promise<ReferralResponseDto[]> =>
    apiClient.get<ReferralResponseDto[]>(`/api/v1/referrals/case/${caseId}`),

  getReferralsByPatientId: (patientId: string): Promise<ReferralResponseDto[]> =>
    apiClient.get<ReferralResponseDto[]>(`/api/v1/referrals/patient/${patientId}`),

  getReferralsByPatient: (patientId: string): Promise<ReferralResponseDto[]> =>
    apiClient.get<ReferralResponseDto[]>(`/api/v1/referrals/patient/${patientId}`),

  getReferralsByFacilityId: (facilityId: string, status?: ReferralStatus): Promise<ReferralResponseDto[]> => {
    const endpoint = status
      ? `/api/v1/referrals/facility/${facilityId}?status=${status}`
      : `/api/v1/referrals/facility/${facilityId}`;
    return apiClient.get<ReferralResponseDto[]>(endpoint);
  },

  getReferralsByFacility: (facilityId: string, status?: ReferralStatus): Promise<ReferralResponseDto[]> => {
    const endpoint = status
      ? `/api/v1/referrals/facility/${facilityId}?status=${status}`
      : `/api/v1/referrals/facility/${facilityId}`;
    return apiClient.get<ReferralResponseDto[]>(endpoint);
  },

  updateReferralStatus: (id: string, data: UpdateReferralStatusRequest): Promise<ReferralResponseDto> =>
    apiClient.patch<ReferralResponseDto>(`/api/v1/referrals/${id}/status`, data),
};
