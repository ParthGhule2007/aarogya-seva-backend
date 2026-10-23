export type CommunicationType = 'VIDEO' | 'AUDIO' | 'CHAT';

export type CommunicationStatus = 'CREATED' | 'ACTIVE' | 'ENDED' | 'CANCELLED';

/**
 * Verified contract matching com.example.communicationservice.dto.CommunicationSessionResponseDTO
 */
export interface CommunicationSessionResponseDTO {
  id: number;
  consultationId: number;
  userId: string;
  doctorId: number;
  roomId: string;
  meetingUrl: string;
  communicationType: CommunicationType;
  status: CommunicationStatus;
  createdAt?: string;
  startedAt?: string;
  endedAt?: string;
  updatedAt?: string;
}

export interface CreateCommunicationSessionDTO {
  consultationId: number;
  userId: string;
  doctorId: number;
  communicationType: CommunicationType;
}
