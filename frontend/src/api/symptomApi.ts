import { apiClient } from './client';
import { BatchCreateSymptomsRequest, CreateSymptomRequest, SymptomDto } from '../types/symptom';

export const symptomApi = {
  getSymptomsForCase: (caseId: string): Promise<SymptomDto[]> =>
    apiClient.get<SymptomDto[]>(`/api/v1/cases/${caseId}/symptoms`),

  getSymptomById: (symptomId: string): Promise<SymptomDto> =>
    apiClient.get<SymptomDto>(`/api/v1/symptoms/${symptomId}`),

  addSymptom: (caseId: string, data: CreateSymptomRequest): Promise<SymptomDto> =>
    apiClient.post<SymptomDto>(`/api/v1/cases/${caseId}/symptoms`, data),

  addSymptomsBatch: (
    caseId: string,
    data: BatchCreateSymptomsRequest | CreateSymptomRequest[]
  ): Promise<SymptomDto[]> => {
    const payload = Array.isArray(data) ? { symptoms: data } : data;
    return apiClient.post<SymptomDto[]>(`/api/v1/cases/${caseId}/symptoms/batch`, payload);
  },

  updateSymptom: (symptomId: string, data: Partial<CreateSymptomRequest>): Promise<SymptomDto> =>
    apiClient.put<SymptomDto>(`/api/v1/symptoms/${symptomId}`, data),

  deleteSymptom: (symptomId: string): Promise<void> =>
    apiClient.delete<void>(`/api/v1/symptoms/${symptomId}`),
};
