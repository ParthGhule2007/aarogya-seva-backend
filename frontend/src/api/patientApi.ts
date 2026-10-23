import { apiClient } from './client';
import { CreatePatientRequest, PatientDto, UpdatePatientRequest } from '../types/patient';

export const patientApi = {
  getAllPatients: (): Promise<PatientDto[]> =>
    apiClient.get<PatientDto[]>('/api/v1/patients'),

  getPatientById: (id: string): Promise<PatientDto> =>
    apiClient.get<PatientDto>(`/api/v1/patients/${id}`),

  registerPatient: (data: CreatePatientRequest): Promise<PatientDto> =>
    apiClient.post<PatientDto>('/api/v1/patients', data),

  createPatient: (data: CreatePatientRequest): Promise<PatientDto> =>
    apiClient.post<PatientDto>('/api/v1/patients', data),

  updatePatient: (id: string, data: UpdatePatientRequest): Promise<PatientDto> =>
    apiClient.put<PatientDto>(`/api/v1/patients/${id}`, data),

  searchPatientsByName: (name: string): Promise<PatientDto[]> =>
    apiClient.get<PatientDto[]>(`/api/v1/patients/search?query=${encodeURIComponent(name)}`),

  getPatientByContact: (phone: string): Promise<PatientDto> =>
    apiClient.get<PatientDto>(`/api/v1/patients/by-contact?contactNumber=${encodeURIComponent(phone)}`),
};
