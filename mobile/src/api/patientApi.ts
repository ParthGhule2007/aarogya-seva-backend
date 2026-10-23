import { request } from './client';
import { SERVICE_URLS } from '../config/apiConfig';
import { CreatePatientRequest, PatientDto, UpdatePatientRequest } from '../types/patient';

export const patientApi = {
  getAllPatients: (): Promise<PatientDto[]> =>
    request<PatientDto[]>(SERVICE_URLS.CORE, '/api/v1/patients'),

  getPatientById: (id: string): Promise<PatientDto> =>
    request<PatientDto>(SERVICE_URLS.CORE, `/api/v1/patients/${encodeURIComponent(id)}`),

  registerPatient: (data: CreatePatientRequest): Promise<PatientDto> =>
    request<PatientDto>(SERVICE_URLS.CORE, '/api/v1/patients', {
      method: 'POST',
      body: JSON.stringify(data),
    }),

  createPatient: (data: CreatePatientRequest): Promise<PatientDto> =>
    request<PatientDto>(SERVICE_URLS.CORE, '/api/v1/patients', {
      method: 'POST',
      body: JSON.stringify(data),
    }),

  updatePatient: (id: string, data: UpdatePatientRequest): Promise<PatientDto> =>
    request<PatientDto>(SERVICE_URLS.CORE, `/api/v1/patients/${encodeURIComponent(id)}`, {
      method: 'PUT',
      body: JSON.stringify(data),
    }),

  searchPatientsByName: (name: string): Promise<PatientDto[]> =>
    request<PatientDto[]>(
      SERVICE_URLS.CORE,
      `/api/v1/patients/search?query=${encodeURIComponent(name)}`
    ),

  getPatientByContact: (phone: string): Promise<PatientDto> =>
    request<PatientDto>(
      SERVICE_URLS.CORE,
      `/api/v1/patients/by-contact?contactNumber=${encodeURIComponent(phone)}`
    ),
};
