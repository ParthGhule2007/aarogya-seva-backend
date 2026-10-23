import { apiClient } from './client';
import { AiAnalysisResponse } from '../types/ai';

export const aiApi = {
  runAiAnalysis: (caseId: string) =>
    apiClient.post<AiAnalysisResponse>(`/api/v1/cases/${caseId}/ai-analysis`),

  getLatestAnalysis: (caseId: string) =>
    apiClient.get<AiAnalysisResponse>(`/api/v1/cases/${caseId}/ai-analysis`),
};
