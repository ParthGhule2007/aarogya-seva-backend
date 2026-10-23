import { request } from './client';
import { SERVICE_URLS } from '../config/apiConfig';
import { CreateCaseRequest, HealthcareCaseDto, UpdateCaseStatusRequest } from '../types/case';

export const caseApi = {
  getAllCases: () =>
    request<HealthcareCaseDto[]>(SERVICE_URLS.CORE, '/api/v1/cases'),

  getCaseById: (id: string) =>
    request<HealthcareCaseDto>(SERVICE_URLS.CORE, `/api/v1/cases/${encodeURIComponent(id)}`),

  getCaseByNumber: (caseNumber: string) =>
    request<HealthcareCaseDto>(
      SERVICE_URLS.CORE,
      `/api/v1/cases/by-number/${encodeURIComponent(caseNumber)}`
    ),

  getCasesByPatientId: (patientId: string) =>
    request<HealthcareCaseDto[]>(
      SERVICE_URLS.CORE,
      `/api/v1/cases/patient/${encodeURIComponent(patientId)}`
    ),

  createCase: (data: CreateCaseRequest) =>
    request<HealthcareCaseDto>(SERVICE_URLS.CORE, '/api/v1/cases', {
      method: 'POST',
      body: JSON.stringify(data),
    }),

  updateCaseStatus: (id: string, data: UpdateCaseStatusRequest) =>
    request<HealthcareCaseDto>(SERVICE_URLS.CORE, `/api/v1/cases/${encodeURIComponent(id)}/status`, {
      method: 'PATCH',
      body: JSON.stringify(data),
    }),
};
