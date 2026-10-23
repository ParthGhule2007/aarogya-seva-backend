export type DoctorAvailabilityStatus = 'AVAILABLE' | 'BUSY' | 'OFFLINE';

export type DoctorStatus = 'ACTIVE' | 'INACTIVE';

/**
 * Sanitized response from GET /api/doctors/available
 */
export interface AvailableDoctorResponseDTO {
  id: number; // Doctor ID is Long in Java
  doctorId?: number;
  userId: string | number; // User Service User ID is String / UUID in Java
  name: string;
  doctorName?: string;
  specialization: string;
  experienceYears?: number;
  qualification?: string;
  hospitalName?: string;
  consultationFee?: number;
  availabilityStatus: DoctorAvailabilityStatus;
}

/**
 * Full doctor profile response from GET /api/doctors/{doctorId}
 */
export interface DoctorResponseDTO {
  id: number; // Doctor ID is Long in Java
  doctorId?: number;
  userId: string | number; // User ID is String / UUID in Java
  name: string;
  doctorName?: string;
  specialization: string;
  licenseNumber: string;
  experienceYears?: number;
  qualification?: string;
  hospitalName?: string;
  consultationFee?: number;
  status: DoctorStatus;
  availabilityStatus: DoctorAvailabilityStatus;
  createdAt?: string;
  updatedAt?: string;
}

export interface DoctorRequestDTO {
  userId: string;
  name: string;
  specialization: string;
  licenseNumber: string;
  experienceYears: number;
  qualification: string;
  hospitalName: string;
}

export interface DoctorErrorResponseDTO {
  timestamp?: string;
  status: number;
  error: string;
  message: string;
  path?: string;
  validationErrors?: Record<string, string>;
}
