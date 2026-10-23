import { request } from './client';
import { SERVICE_URLS } from '../config/apiConfig';
import { AiAnalysisResponse } from '../types/ai';

export const aiApi = {
  runAiAnalysis: (caseId: string) =>
    request<AiAnalysisResponse>(
      SERVICE_URLS.CORE,
      `/api/v1/cases/${encodeURIComponent(caseId)}/ai-analysis`,
      {
        method: 'POST',
        timeoutMs: 35000,
      }
    ),

  getLatestAnalysis: (caseId: string) =>
    request<AiAnalysisResponse>(
      SERVICE_URLS.CORE,
      `/api/v1/cases/${encodeURIComponent(caseId)}/ai-analysis`
    ),
};
