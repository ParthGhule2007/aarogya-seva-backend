export type TargetType = 'CAREGIVER' | 'FACILITY' | 'NEARBY_PEOPLE';

export type SosEventStatus =
  | 'TRIGGERED'
  | 'PROCESSING'
  | 'NOTIFICATIONS_SENT'
  | 'PARTIALLY_SENT'
  | 'FAILED'
  | 'RESOLVED';

export type UserType = 'PATIENT' | 'CAREGIVER' | 'ASHA' | 'FACILITY' | 'DOCTOR';

export interface CreateSosRequest {
  patientId: string;
  latitude: number;
  longitude: number;
  targets: TargetType[];
  facilityId?: string | null;
  message?: string;
  patientName?: string;
}

export interface SosResponse {
  sosId: string;
  status: SosEventStatus;
  triggeredAt: string;
  message: string;
  targets: TargetType[];
  patientName?: string;
}

export interface SosStatusResponse {
  sosId: string;
  patientId?: string;
  patientName?: string;
  latitude?: number;
  longitude?: number;
  facilityId?: string | null;
  message?: string | null;
  status: SosEventStatus;
  triggeredAt: string;
  resolvedAt?: string | null;
  targets: TargetType[];
  totalRecipients: number;
  successfulNotifications: number;
  failedNotifications: number;
  recipientsNotified?: number;
  recipientsFailed?: number;
}

export interface DeviceRegistrationRequest {
  userId: string;
  fcmToken: string;
  userType: UserType;
  emergencyAlertOptIn: boolean;
  latitude?: number;
  longitude?: number;
}

export interface DeviceRegistrationResponse {
  deviceId: string;
  userId: string;
  userType: UserType;
  emergencyAlertOptIn: boolean;
  active: boolean;
  message?: string;
  registeredAt?: string;
  updatedAt?: string;
}

export interface NearbyAlertDto {
  sosId: string;
  patientId: string;
  latitude: number;
  longitude: number;
  distanceKm: number;
  message: string;
  triggeredAt: string;
  status: SosEventStatus | string;
}

export interface UpdateLocationPayload {
  userId: string;
  latitude: number;
  longitude: number;
  emergencyAlertOptIn: boolean;
  userType?: UserType;
}

/**
 * Resolves the display name of the SOS sender / patient from the message or alert data.
 * Always prefers the human name over raw UUIDs or patient IDs.
 */
export function getSosSenderName(message?: string | null, fallback?: string): string {
  if (message) {
    const match = message.match(/(?:Emergency for|Emergency SOS for|triggered by|Emergency:\s*|Patient:\s*)([A-Za-z\s.'-]+?)(?:\s+via|\s+needs|\s*$|\.)/i);
    if (match && match[1]?.trim()) {
      const extracted = match[1].trim();
      if (extracted.toLowerCase() !== 'patient' && extracted.length >= 2) {
        return extracted;
      }
    }
  }
  return fallback || 'Ramesh Patil';
}
