export type DoctorAvailabilityStatus = 'AVAILABLE' | 'BUSY' | 'OFFLINE';

export type DoctorStatus = 'ACTIVE' | 'INACTIVE';

export interface AvailableDoctorResponseDTO {
  id: number; // Doctor ID is Long in Java
  doctorId?: number;
  userId: string | number;
  name: string;
  doctorName?: string;
  specialization: string;
  experienceYears?: number;
  qualification?: string;
  hospitalName?: string;
  consultationFee?: number;
  availabilityStatus: DoctorAvailabilityStatus;
}

export interface DoctorResponseDTO {
  id: number;
  doctorId?: number;
  userId: string | number;
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
