export type ConsultationType = 'VIDEO' | 'AUDIO' | 'CHAT';

export type ConsultationUrgency = 'LOW' | 'MEDIUM' | 'HIGH' | 'EMERGENCY';

export type ConsultationStatus =
  | 'PENDING'
  | 'REQUESTED'
  | 'ACCEPTED'
  | 'SCHEDULED'
  | 'READY'
  | 'IN_CALL'
  | 'COMPLETED'
  | 'CANCELLED'
  | 'REJECTED';

export type CancelledBy = 'USER' | 'DOCTOR' | 'SYSTEM';

export interface ConsultationRequestDTO {
  userId: string; // Core Patient UUID passed as string
  username: string; // Patient Full Name
  doctorId: number; // Doctor Service ID (Long in Java)
  disease: string; // Chief complaint / illness description
  urgency: ConsultationUrgency; // 'LOW' | 'MEDIUM' | 'HIGH' | 'EMERGENCY'
  consultationType: ConsultationType; // 'VIDEO' | 'AUDIO' | 'CHAT'
}

export interface ConsultationResponseDTO {
  consultationId: number; // Java Long
  id: number; // alias
  userId: string;
  username: string;
  doctorId: number;
  doctorName?: string;
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
