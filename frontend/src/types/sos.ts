/**
 * TypeScript definitions for Aarogya Seva SOS Emergency Service.
 * Matches backend contracts in com.ruralhealthcare.sos.
 */

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
}

export interface SosResponse {
  sosId: string;
  status: SosEventStatus;
  triggeredAt: string;
  message: string;
  targets: TargetType[];
}

export interface SosStatusResponse {
  sosId: string;
  patientId?: string;
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
