import { apiClient } from './client';
import { CreateCaseRequest, HealthcareCaseDto, UpdateCaseStatusRequest } from '../types/case';

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
};
