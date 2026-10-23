import { request } from './client';
import { SERVICE_URLS } from '../config/apiConfig';
import { BatchCreateSymptomsRequest, CreateSymptomRequest, SymptomDto } from '../types/symptom';

export const symptomApi = {
  getSymptomsForCase: (caseId: string): Promise<SymptomDto[]> =>
    request<SymptomDto[]>(SERVICE_URLS.CORE, `/api/v1/cases/${encodeURIComponent(caseId)}/symptoms`),

  getSymptomById: (symptomId: string): Promise<SymptomDto> =>
    request<SymptomDto>(SERVICE_URLS.CORE, `/api/v1/symptoms/${encodeURIComponent(symptomId)}`),

  addSymptom: (caseId: string, data: CreateSymptomRequest): Promise<SymptomDto> =>
    request<SymptomDto>(SERVICE_URLS.CORE, `/api/v1/cases/${encodeURIComponent(caseId)}/symptoms`, {
      method: 'POST',
      body: JSON.stringify(data),
    }),

  addSymptomsBatch: (
    caseId: string,
    data: BatchCreateSymptomsRequest | CreateSymptomRequest[]
  ): Promise<SymptomDto[]> => {
    const payload = Array.isArray(data) ? { symptoms: data } : data;
    return request<SymptomDto[]>(
      SERVICE_URLS.CORE,
      `/api/v1/cases/${encodeURIComponent(caseId)}/symptoms/batch`,
      {
        method: 'POST',
        body: JSON.stringify(payload),
      }
    );
  },

  addCaseSymptoms: (caseId: string, symptoms: CreateSymptomRequest[]): Promise<any> =>
    request<any>(
      SERVICE_URLS.CORE,
      `/api/v1/cases/${encodeURIComponent(caseId)}/case-symptoms`,
      {
        method: 'POST',
        body: JSON.stringify({ symptoms }),
      }
    ),
};
