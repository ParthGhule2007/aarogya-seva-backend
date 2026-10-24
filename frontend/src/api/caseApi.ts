import { apiClient } from './client';
import {
  CreateCaseRequest,
  HealthcareCaseDto,
  PatientMedicalRecordDto,
  UpdateCaseStatusRequest,
} from '../types/case';
import { referralApi } from './referralApi';
import { ReferralResponseDto } from '../types/referral';

export const caseApi = {
  getAllCases: () => apiClient.get<HealthcareCaseDto[]>('/api/v1/cases'),

  getCaseById: (id: string) => apiClient.get<HealthcareCaseDto>(`/api/v1/cases/${id}`),

  getCaseByNumber: (caseNumber: string) =>
    apiClient.get<HealthcareCaseDto>(`/api/v1/cases/by-number/${encodeURIComponent(caseNumber)}`),

  getCasesByPatientId: (patientId: string) =>
    apiClient.get<HealthcareCaseDto[]>(`/api/v1/cases/patient/${patientId}`),

  createCase: (data: CreateCaseRequest) =>
    apiClient.post<HealthcareCaseDto>('/api/v1/cases', data),

  updateCaseStatus: (id: string, data: UpdateCaseStatusRequest) =>
    apiClient.patch<HealthcareCaseDto>(`/api/v1/cases/${id}/status`, data),

  getCaseMedicalRecord: async (caseId: string): Promise<PatientMedicalRecordDto> => {
    try {
      const record = await apiClient.get<PatientMedicalRecordDto>(`/api/v1/cases/${caseId}/medical-record`);
      if (record && record.caseDetails) {
        return record;
      }
    } catch {
      // Fallback
    }

    const [caseDetails, referrals] = await Promise.all([
      apiClient.get<HealthcareCaseDto>(`/api/v1/cases/${caseId}`),
      referralApi.getReferralsByCaseId(caseId).catch(() => [] as ReferralResponseDto[]),
    ]);

    return {
      caseDetails,
      referrals: referrals || [],
    };
  },

  getPatientMedicalRecords: async (patientId: string): Promise<PatientMedicalRecordDto[]> => {
    try {
      // Try direct backend endpoint first if available
      const direct = await apiClient.get<PatientMedicalRecordDto[]>(
        `/api/v1/cases/patient/${patientId}/medical-records`
      );
      if (Array.isArray(direct) && direct.length > 0) {
        return direct;
      }
    } catch {
      // Fallback to assembling authoritative cases & referrals from Core DB
    }

    const [cases, referrals] = await Promise.all([
      apiClient.get<HealthcareCaseDto[]>(`/api/v1/cases/patient/${patientId}`),
      referralApi.getReferralsByPatientId(patientId).catch(() => [] as ReferralResponseDto[]),
    ]);

    const referralMap = new Map<string, ReferralResponseDto[]>();
    (referrals || []).forEach((ref) => {
      if (!ref.caseId) return;
      const list = referralMap.get(ref.caseId) || [];
      list.push(ref);
      referralMap.set(ref.caseId, list);
    });

    const assembled: PatientMedicalRecordDto[] = (cases || []).map((c) => ({
      caseDetails: c,
      referrals: referralMap.get(c.id) || [],
    }));

    // Sort cases so newest appears first
    assembled.sort((a, b) => {
      const timeA = new Date(a.caseDetails?.createdAt || 0).getTime();
      const timeB = new Date(b.caseDetails?.createdAt || 0).getTime();
      return timeB - timeA;
    });

    return assembled;
  },
};

