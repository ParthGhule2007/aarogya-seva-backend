export type ConsultationType = 'VIDEO' | 'AUDIO' | 'CHAT';

export type ConsultationUrgency = 'LOW' | 'MEDIUM' | 'HIGH' | 'EMERGENCY';

export type ConsultationStatus =
  | 'REQUESTED'
  | 'ACCEPTED'
  | 'SCHEDULED'
  | 'READY'
  | 'IN_CALL'
  | 'COMPLETED'
  | 'CANCELLED'
  | 'REJECTED';

export type CancelledBy = 'USER' | 'DOCTOR' | 'SYSTEM';

/**
 * Verified contract matching com.example.consultationservice.dto.ConsultationRequestDTO
 */
export interface ConsultationRequestDTO {
  userId: string; // Core Patient UUID passed as string
  username: string; // Patient Full Name
  doctorId: number; // Doctor Service ID (Long in Java)
  disease: string; // Chief complaint / illness description
  urgency: ConsultationUrgency; // 'LOW' | 'MEDIUM' | 'HIGH' | 'EMERGENCY'
  consultationType: ConsultationType; // 'VIDEO' | 'AUDIO' | 'CHAT'
}

/**
 * Verified contract matching com.example.consultationservice.dto.ConsultationResponseDTO
 */
export interface ConsultationResponseDTO {
  consultationId: number; // Java Long
  id: number; // Java Long (alias of consultationId)
  userId: string; // Core Patient UUID
  username: string; // Patient Full Name
  doctorId: number; // Doctor Service Long
  doctorName?: string; // Resolved by Consultation Service from Doctor Service
  disease: string;
  urgency: ConsultationUrgency;
  consultationType: ConsultationType;
  status: ConsultationStatus;
  requestedAt?: string;
  acceptedAt?: string;
  scheduledAt?: string;
  startedAt?: string;
  completedAt?: string;
  cancelledAt?: string;
  cancelledBy?: CancelledBy;
  roomId?: string;
  meetingUrl?: string;
  createdAt?: string;
  updatedAt?: string;
}

export interface CancelConsultationDTO {
  cancelledBy: CancelledBy;
}

export interface ScheduleConsultationDTO {
  scheduledAt: string;
}

export interface ConsultationErrorResponseDTO {
  timestamp?: string;
  status: number;
  error: string;
  message: string;
  path?: string;
}
