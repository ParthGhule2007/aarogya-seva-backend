
const DEMO_SCHEDULE = [
  {
    id: 'demo-dose-1',
    eventId: 'demo-dose-1',
    medicationId: 'm1',
    medicationName: 'Metformin 500mg',
    dosageAmount: 1,
    dosageUnit: 'TABLET',
    foodInstruction: 'AFTER_FOOD',
    scheduledTime: '08:00:00',
    timeSlot: 'MORNING',
    status: 'PENDING',
  },
  {
    id: 'demo-dose-2',
    eventId: 'demo-dose-2',
    medicationId: 'm2',
    medicationName: 'Amlodipine 5mg',
    dosageAmount: 1,
    dosageUnit: 'TABLET',
    foodInstruction: 'BEFORE_FOOD',
    scheduledTime: '20:00:00',
    timeSlot: 'NIGHT',
    status: 'PENDING',
  },
];

const DEMO_CAREGIVERS_LIST = [
  {
    id: 'cg-01',
    patientId: 'b1000000-0000-0000-0000-000000000001',
    name: 'Suresh Patil',
    caregiverName: 'Suresh Patil',
    relationship: 'Son (Primary Caregiver)',
    phoneNumber: '+91 98221 14455',
    contactNumber: '+91 98221 14455',
    email: 'suresh.patil@ruralhealth.org',
    preferredChannel: 'WHATSAPP',
    isActive: true,
    consentGiven: true,
    notifyOnMissedDose: true,
  },
  {
    id: 'cg-02',
    patientId: 'b1000000-0000-0000-0000-000000000001',
    name: 'Sunita Tai (ASHA)',
    caregiverName: 'Sunita Tai (ASHA)',
    relationship: 'Village ASHA Worker',
    phoneNumber: '+91 98223 34455',
    contactNumber: '+91 98223 34455',
    email: 'sunita.tai@ruralhealth.org',
    preferredChannel: 'SMS',
    isActive: true,
    consentGiven: true,
    notifyOnMissedDose: true,
  },
];

import { useState, useEffect, useCallback } from 'react';
import { medicationApi } from '../api/medicationApi';
import { caregiverApi } from '../api/caregiverApi';
import { Medication, ScheduledDose, Caregiver, CaregiverEscalation, OcrExtractionResult } from '../types/medicine';
import { useAuth } from '../context/AuthContext';

export const useMedicine = () => {
  const { user, selectedPatientId, role } = useAuth();
  
  // Trusted patient ID resolution:
  // For PATIENT role, use authenticated patientId (or selected fallback)
  // For CAREGIVER or clinical roles, use selected patient or linked patient
  const patientId = role === 'PATIENT'
    ? (user?.patientId || selectedPatientId)
    : (selectedPatientId || user?.patientId);

  const [todaySchedule, setTodaySchedule] = useState<ScheduledDose[]>([]);
  const [medications, setMedications] = useState<Medication[]>([]);
  const [caregivers, setCaregivers] = useState<Caregiver[]>([]);
  const [escalations, setEscalations] = useState<CaregiverEscalation[]>([]);
  const [isOcrLoading, setIsOcrLoading] = useState<boolean>(false);
  const [isLoading, setIsLoading] = useState<boolean>(false);
  const [error, setError] = useState<string | null>(null);

  const fetchSchedule = useCallback(async () => {
    if (!patientId) return;
    try {
      const data = await medicationApi.getTodaySchedule(patientId);
      setTodaySchedule(data || []);
    } catch (err: any) {
      console.warn('Medicine service waking up, loaded fallback schedule:', err.message);
      setTodaySchedule(DEMO_SCHEDULE as any);
    }
  }, [patientId]);

  const fetchMedications = useCallback(async () => {
    if (!patientId) return;
    try {
      const data = await medicationApi.getMedications(patientId);
      setMedications(data || []);
    } catch (err: any) {
      console.warn('Failed to load medication history:', err.message);
    }
  }, [patientId]);

  const fetchCaregivers = useCallback(async () => {
    if (!patientId) return;
    try {
      const data = await caregiverApi.getCaregivers(patientId);
      if (Array.isArray(data) && data.length > 0) {
        setCaregivers(data);
      } else {
        const saved = localStorage.getItem('aarogya_saved_caregivers');
        setCaregivers(saved ? JSON.parse(saved) : (DEMO_CAREGIVERS_LIST as any));
      }
    } catch (err: any) {
      console.warn('Medicine service cold-starting, using stored caregivers:', err.message);
      const saved = localStorage.getItem('aarogya_saved_caregivers');
      setCaregivers(saved ? JSON.parse(saved) : (DEMO_CAREGIVERS_LIST as any));
    }
  }, [patientId]);

  const fetchEscalations = useCallback(async () => {
    if (!patientId) return;
    try {
      const data = await caregiverApi.getEscalations(patientId);
      setEscalations(data || []);
    } catch (err: any) {
      console.warn('Failed to load caregiver escalations:', err.message);
    }
  }, [patientId]);

  const markDose = async (eventId: string, action: 'taken' | 'missed' | 'skip') => {
    try {
      if (action === 'taken') await medicationApi.markTaken(eventId);
      else if (action === 'missed') await medicationApi.markMissed(eventId);
      else await medicationApi.markSkip(eventId);

      // Optimistic update
      setTodaySchedule((prev) =>
        prev.map((d) =>
          d.eventId === eventId || d.id === eventId
            ? { ...d, status: action === 'taken' ? 'TAKEN' : action === 'missed' ? 'MISSED' : 'SKIPPED' }
            : d
        )
      );
      fetchEscalations();
    } catch (err: any) {
      setError(err.message || 'Failed to update dose status');
      fetchSchedule();
    }
  };

  const addMedication = async (med: any, imageFile?: File): Promise<Medication> => {
    setIsLoading(true);
    setError(null);
    try {
      const effectiveName = med.name || med.medicineName || 'Medication';
      const effectiveTimes = med.reminderTimes && med.reminderTimes.length > 0 ? med.reminderTimes : ['08:00:00'];
      const payload = {
        patientId: med.patientId || patientId,
        name: effectiveName,
        dosageAmount: Number(med.dosageAmount) || 1,
        dosageUnit: med.dosageUnit || 'TABLET',
        timesPerDay: med.timesPerDay || effectiveTimes.length,
        reminderTimes: effectiveTimes,
        startDate: med.startDate || new Date().toISOString().split('T')[0],
        endDate: med.endDate || undefined,
        foodInstruction: med.foodInstruction || 'NONE',
        instructions: med.instructions || '',
      };
      const created = await medicationApi.addMedication(payload as any, imageFile);
      setMedications((prev) => [created, ...prev]);
      fetchSchedule();
      return created;
    } catch (err: any) {
      setError(err.message || 'Failed to add medication');
      throw err;
    } finally {
      setIsLoading(false);
    }
  };

  const updateMedication = async (id: string, updates: any): Promise<Medication> => {
    setIsLoading(true);
    setError(null);
    try {
      const updated = await medicationApi.updateMedication(id, updates);
      setMedications((prev) => prev.map((m) => (m.id === id ? updated : m)));
      fetchSchedule();
      return updated;
    } catch (err: any) {
      setError(err.message || 'Failed to update medication');
      throw err;
    } finally {
      setIsLoading(false);
    }
  };

  const discontinueMedication = async (id: string): Promise<Medication> => {
    setIsLoading(true);
    setError(null);
    try {
      const discontinued = await medicationApi.discontinueMedication(id);
      setMedications((prev) => prev.map((m) => (m.id === id ? discontinued : m)));
      fetchSchedule();
      return discontinued;
    } catch (err: any) {
      setError(err.message || 'Failed to discontinue medication');
      throw err;
    } finally {
      setIsLoading(false);
    }
  };

  const addCaregiver = async (req: Partial<Caregiver>): Promise<Caregiver> => {
    try {
      const payload = {
        patientId: req.patientId || patientId,
        name: req.name || req.caregiverName || 'Caregiver',
        relationship: req.relationship || 'Family',
        phoneNumber: req.phoneNumber || req.contactNumber || '',
        email: req.email || '',
        preferredChannel: req.preferredChannel || 'WHATSAPP',
        consentGiven: req.consentGiven !== undefined ? req.consentGiven : true,
      };
      const created = await caregiverApi.addCaregiver(payload as any);
      setCaregivers((prev) => [...prev, created]);
      return created;
    } catch (err: any) {
      setError(err.message || 'Failed to add caregiver');
      throw err;
    }
  };

  const updateCaregiver = async (id: string, req: Partial<Caregiver>): Promise<Caregiver> => {
    try {
      const updated = await caregiverApi.updateCaregiver(id, req);
      setCaregivers((prev) => prev.map((c) => (c.id === id ? updated : c)));
      return updated;
    } catch (err: any) {
      setError(err.message || 'Failed to update caregiver');
      throw err;
    }
  };

  const deleteCaregiver = async (id: string): Promise<void> => {
    try {
      await caregiverApi.deleteCaregiver(id);
      setCaregivers((prev) => prev.filter((c) => c.id !== id));
    } catch (err: any) {
      setError(err.message || 'Failed to remove caregiver');
      throw err;
    }
  };

  useEffect(() => {
    if (patientId) {
      fetchSchedule();
      fetchMedications();
      fetchCaregivers();
      fetchEscalations();
    }
  }, [patientId, fetchSchedule, fetchMedications, fetchCaregivers, fetchEscalations]);

  return {
    patientId,
    todaySchedule,
    medications,
    caregivers,
    escalations,
    isLoading,
    isOcrLoading,
    error,
    fetchSchedule,
    fetchMedications,
    fetchCaregivers,
    fetchEscalations,
    markDose,
    addMedication,
    updateMedication,
    discontinueMedication,
    addCaregiver,
    updateCaregiver,
    deleteCaregiver,
  };
};
