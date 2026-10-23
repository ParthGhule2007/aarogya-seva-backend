import { FacilityRecommendationDto, FacilityType, MedicalServiceCode } from './facility';

export type RiskLevel = 'LOW' | 'MODERATE' | 'HIGH' | 'CRITICAL';
export type UrgencyLevel = 'ROUTINE' | 'PRIORITY' | 'URGENT' | 'EMERGENCY';
export type ProcessingStatus = 'PENDING' | 'COMPLETED' | 'FAILED';

export interface RiskAssessment {
  riskScore: number;
  riskLevel: RiskLevel;
}

export interface UrgencyAssessment {
  level: UrgencyLevel;
  reason?: string;
}

export interface DiseasePrediction {
  category: string;
  probability: number;
  diseaseName?: string;
}

export interface AiAnalysisResponse {
  id?: string;
  caseId: string;
  modelVersion: string;
  modelName?: string;
  processingStatus: ProcessingStatus;
  riskAssessment: RiskAssessment;
  possibleDiseases: DiseasePrediction[];
  probableDiseases?: DiseasePrediction[];
  precautions: string[];
  recommendedServices: MedicalServiceCode[];
  requiredFacilityTypes: FacilityType[];
  urgency: UrgencyAssessment;
  confidence: number;
  confidenceScore?: number;
  explanation?: string;
  triageRationale?: string;
  recommendedFacilities: FacilityRecommendationDto[];
  facilityMatchingNote?: string;
  processedAt?: string;

  // Flattened aliases for easy access
  riskLevel?: RiskLevel;
  riskScore?: number;
  urgencyLevel?: UrgencyLevel;
  urgencyReason?: string;
}
