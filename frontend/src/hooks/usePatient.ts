import { useState, useEffect, useCallback } from 'react';
import { patientApi } from '../api/patientApi';
import { caseApi } from '../api/caseApi';
import { PatientDto, CreatePatientRequest } from '../types/patient';
import { HealthcareCaseDto } from '../types/case';
import { useAuth } from '../context/AuthContext';

const DEMO_PATIENTS: PatientDto[] = [
  {
    id: 'b1000000-0000-0000-0000-000000000001',
    fullName: 'Ramesh Patil',
    gender: 'MALE',
    age: 48,
    contactNumber: '9822012345',
    dateOfBirth: '1978-04-12',
    address: {
      village: 'Palasdeo',
      subDistrict: 'Indapur',
      district: 'Pune',
      state: 'Maharashtra',
      pincode: '413132',
    },
    emergencyContact: {
      contactName: 'Suresh Patil (Son)',
      contactNumber: '9822099999',
      relationship: 'Son',
    },
  },
  {
    id: 'b2000000-0000-0000-0000-000000000002',
    fullName: 'Shantabai Shinde',
    gender: 'FEMALE',
    age: 62,
    contactNumber: '9822054321',
    dateOfBirth: '1964-08-20',
    address: {
      village: 'Shelgaon',
      district: 'Pune',
    },
  },
  {
    id: 'b3000000-0000-0000-0000-000000000003',
    fullName: 'Ganesh Jadhav',
    gender: 'MALE',
    age: 35,
    contactNumber: '9822077889',
    dateOfBirth: '1991-01-10',
    address: {
      village: 'Bhigwan',
      district: 'Pune',
    },
  },
];

export const usePatient = () => {
  const { selectedPatientId } = useAuth();
  const [patient, setPatient] = useState<PatientDto | null>(null);
  const [cases, setCases] = useState<HealthcareCaseDto[]>([]);
  const [patients, setPatients] = useState<PatientDto[]>(DEMO_PATIENTS);
  const [isLoading, setIsLoading] = useState<boolean>(false);
  const [error, setError] = useState<string | null>(null);

  const fetchPatient = useCallback(async (id: string) => {
    if (!id) {
      setPatient(null);
      setError(null);
      return null;
    }
    setIsLoading(true);
    setError(null);
    try {
      const data = await patientApi.getPatientById(id);
      setPatient(data);
      return data;
    } catch (err: any) {
      // Offline fallback only if id is explicitly recognized in DEMO_PATIENTS
      const found = DEMO_PATIENTS.find((p) => p.id === id);
      if (found) {
        setPatient(found);
        return found;
      }
      // Never silently fallback to DEMO_PATIENTS[0]!
      setPatient(null);
      const errMsg = err?.response?.data?.message || err?.message || `Patient profile not found for ID: ${id}`;
      setError(errMsg);
      return null;
    } finally {
      setIsLoading(false);
    }
  }, []);

  const fetchCases = useCallback(async (patientId: string) => {
    try {
      const data = await caseApi.getCasesByPatientId(patientId);
      setCases(data || []);
      return data;
    } catch {
      setCases([]);
      return [];
    }
  }, []);

  const fetchAllPatients = useCallback(async () => {
    setIsLoading(true);
    setError(null);
    try {
      const data = await patientApi.getAllPatients();
      setPatients(data && data.length > 0 ? data : DEMO_PATIENTS);
      return data;
    } catch {
      setPatients(DEMO_PATIENTS);
      return DEMO_PATIENTS;
    } finally {
      setIsLoading(false);
    }
  }, []);

  const searchPatients = async (query: string) => {
    setIsLoading(true);
    try {
      const data = await patientApi.searchPatientsByName(query);
      return data && data.length > 0
        ? data
        : DEMO_PATIENTS.filter(
            (p) =>
              p.fullName.toLowerCase().includes(query.toLowerCase()) ||
              p.contactNumber.includes(query)
          );
    } catch {
      return DEMO_PATIENTS.filter(
        (p) =>
          p.fullName.toLowerCase().includes(query.toLowerCase()) ||
          p.contactNumber.includes(query)
      );
    } finally {
      setIsLoading(false);
    }
  };

  const registerPatient = async (req: CreatePatientRequest): Promise<PatientDto> => {
    setIsLoading(true);
    setError(null);
    try {
      const created = await patientApi.registerPatient(req);
      setPatients((prev) => [created, ...prev]);
      return created;
    } catch (err: any) {
      const errMsg = err?.response?.data?.message || err?.message || 'Failed to register patient in Core service';
      setError(errMsg);
      throw new Error(errMsg);
    } finally {
      setIsLoading(false);
    }
  };

  const createCase = async (chiefComplaint: string, notes: string = '', intakeSource: any = 'PATIENT_PORTAL') => {
    if (!selectedPatientId) throw new Error('No patient selected');
    try {
      const created = await caseApi.createCase({
        patientId: selectedPatientId,
        chiefComplaint,
        initialChiefComplaint: chiefComplaint,
        notes,
        intakeSource,
      });
      setCases((prev) => [created, ...prev]);
      return created;
    } catch (err: any) {
      const errMsg = err?.response?.data?.message || err?.message || 'Failed to create clinical case in Core service';
      setError(errMsg);
      throw new Error(errMsg);
    }
  };

  useEffect(() => {
    if (selectedPatientId) {
      fetchPatient(selectedPatientId);
      fetchCases(selectedPatientId);
    } else {
      setPatient(null);
      setCases([]);
      setError(null);
    }
  }, [selectedPatientId, fetchPatient, fetchCases]);

  return {
    patient,
    cases,
    patients,
    isLoading,
    error,
    fetchPatient,
    fetchCases,
    fetchAllPatients,
    searchPatients,
    registerPatient,
    createCase,
  };
};
