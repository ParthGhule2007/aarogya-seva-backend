export type SymptomSeverity = 'MILD' | 'MODERATE' | 'SEVERE' | 'CRITICAL';

export interface SymptomDto {
  id: string;
  caseId: string;
  symptomName: string;
  severity: SymptomSeverity;
  durationDays: number;
  description?: string;
  bodySite?: string;
  createdAt?: string;
}

export interface CreateSymptomRequest {
  symptomName: string;
  severity: SymptomSeverity;
  durationDays: number;
  description?: string;
  bodySite?: string;
}

export interface BatchCreateSymptomsRequest {
  symptoms: CreateSymptomRequest[];
}
