import { apiClient } from './client';
import {
  CreateCaseRequest,
  HealthcareCaseDto,
  PatientMedicalRecordDto,
  UpdateCaseStatusRequest,
} from '../types/case';

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

  getCaseMedicalRecord: (caseId: string) =>
    apiClient.get<PatientMedicalRecordDto>(`/api/v1/cases/${caseId}/medical-record`),

  getPatientMedicalRecords: (patientId: string) =>
    apiClient.get<PatientMedicalRecordDto[]>(`/api/v1/cases/patient/${patientId}/medical-records`),
};

