import { PatientDto } from './patient';
import { HealthcareCaseDto } from './case';
import { SymptomDto } from './symptom';
import { FacilityDto, FacilityType, MedicalServiceCode } from './facility';
import { DiseasePrediction, RiskLevel, UrgencyLevel } from './ai';

export type ReferralStatus =
  | 'PENDING'
  | 'ACCEPTED'
  | 'CONFIRMED'
  | 'REJECTED'
  | 'IN_PROGRESS'
  | 'COMPLETED'
  | 'CANCELLED';

export interface ReferralResponseDto {
  id: string;
  referralToken?: string;
  qrCodePayload?: string;
  caseId: string;
  patientId: string;
  facilityId: string;
  facilityName: string;
  name?: string;
  facilityType?: string;
  aiPredictionId?: string;
  status: ReferralStatus;
  statusDisplayName?: string;
  priority?: string;
  urgencyLevel?: string;
  patientMessage?: string;
  facilityResponse?: string;
  requestedAt: string;
  respondedAt?: string;
  acceptedAt?: string;
  arrivedAt?: string;
  completedAt?: string;
  createdAt?: string;
  updatedAt?: string;
}

export interface AiAssessmentSummaryDto {
  aiPredictionId: string;
  modelVersion: string;
  riskScore: number;
  riskLevel: RiskLevel;
  urgencyLevel: UrgencyLevel;
  urgencyReason?: string;
  confidence: number;
  explanation?: string;
  possibleDiseases: DiseasePrediction[];
  precautions: string[];
  recommendedServices: MedicalServiceCode[];
  requiredFacilityTypes: FacilityType[];
  processedAt?: string;
}

export interface ReferralDetailDto {
  referral: ReferralResponseDto;
  patient?: PatientDto;
  healthcareCase?: HealthcareCaseDto;
  symptoms: SymptomDto[];
  aiAssessment?: AiAssessmentSummaryDto;
  facility?: FacilityDto;
}

export interface CreateReferralRequest {
  caseId: string;
  patientId?: string;
  facilityId: string;
  urgencyLevel?: string;
  reason?: string;
  patientMessage?: string;
}

export interface UpdateReferralStatusRequest {
  status: ReferralStatus;
  facilityResponse?: string;
}
