import { request } from './client';
import { SERVICE_URLS } from '../config/apiConfig';
import {
  CreateReferralRequest,
  ReferralDetailDto,
  ReferralResponseDto,
  ReferralStatus,
  UpdateReferralStatusRequest,
} from '../types/referral';

export const referralApi = {
  createReferral: (data: CreateReferralRequest): Promise<ReferralResponseDto> =>
    request<ReferralResponseDto>(SERVICE_URLS.CORE, '/api/v1/referrals', {
      method: 'POST',
      body: JSON.stringify(data),
    }),

  getReferralById: (id: string): Promise<ReferralDetailDto> =>
    request<ReferralDetailDto>(SERVICE_URLS.CORE, `/api/v1/referrals/${encodeURIComponent(id)}`),

  getReferralsByCaseId: (caseId: string): Promise<ReferralResponseDto[]> =>
    request<ReferralResponseDto[]>(
      SERVICE_URLS.CORE,
      `/api/v1/referrals/case/${encodeURIComponent(caseId)}`
    ),

  getReferralsByPatientId: (patientId: string): Promise<ReferralResponseDto[]> =>
    request<ReferralResponseDto[]>(
      SERVICE_URLS.CORE,
      `/api/v1/referrals/patient/${encodeURIComponent(patientId)}`
    ),

  getReferralsByFacilityId: (
    facilityId: string,
    status?: ReferralStatus
  ): Promise<ReferralResponseDto[]> => {
    const endpoint = status
      ? `/api/v1/referrals/facility/${encodeURIComponent(facilityId)}?status=${status}`
      : `/api/v1/referrals/facility/${encodeURIComponent(facilityId)}`;
    return request<ReferralResponseDto[]>(SERVICE_URLS.CORE, endpoint);
  },

  updateReferralStatus: (
    id: string,
    data: UpdateReferralStatusRequest
  ): Promise<ReferralResponseDto> =>
    request<ReferralResponseDto>(
      SERVICE_URLS.CORE,
      `/api/v1/referrals/${encodeURIComponent(id)}/status`,
      {
        method: 'PATCH',
        body: JSON.stringify(data),
      }
    ),
};
