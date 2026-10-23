import { SymptomSeverity } from './symptom';

export type CaseStatus =
  | 'CREATED'
  | 'SYMPTOMS_RECORDED'
  | 'PENDING_TRIAGE'
  | 'TRIAGED'
  | 'REFERRED'
  | 'CLOSED'
  | 'CANCELLED';

export type CasePriority = 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';

export type IntakeChannel =
  | 'FORM'
  | 'CHAT'
  | 'VOICE'
  | 'COMMUNITY_HEALTH_WORKER'
  | 'PATIENT_PORTAL'
  | 'ASHA_WORKER'
  | 'FIELD_CLINIC';

export interface CaseSymptomDto {
  symptomName: string;
  severity: string;
  durationDays: number;
}

export interface HealthcareCaseDto {
  id: string;
  caseNumber: string;
  patientId: string;
  patientName?: string;
  patientContactNumber?: string;
  status: CaseStatus;
  priority: CasePriority;
  intakeChannel: IntakeChannel;
  intakeChannelDescription?: string;
  chiefComplaint?: string;
  notes?: string;
  latitude?: number | null;
  longitude?: number | null;
  symptoms?: CaseSymptomDto[];
  createdAt?: string;
  updatedAt?: string;
}

export interface SymptomInputDto {
  symptomName: string;
  severity: SymptomSeverity;
  durationDays: number;
  description?: string;
  bodySite?: string;
}

export interface CreateCaseRequest {
  patientId: string;
  intakeChannel?: IntakeChannel;
  intakeSource?: string;
  latitude?: number;
  longitude?: number;
  chiefComplaint?: string;
  initialChiefComplaint?: string;
  notes?: string;
  initialSymptoms?: SymptomInputDto[];
}

export interface UpdateCaseStatusRequest {
  status: CaseStatus;
  priority?: CasePriority;
  notes?: string;
}
