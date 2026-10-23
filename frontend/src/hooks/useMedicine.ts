import { useState, useEffect, useCallback } from 'react';
import { medicationApi } from '../api/medicationApi';
import { caregiverApi } from '../api/caregiverApi';
import { Medication, ScheduledDose, Caregiver, CaregiverEscalation, OcrExtractionResult } from '../types/medicine';
import { useAuth } from '../context/AuthContext';

export const useMedicine = () => {
  const { user, selectedPatientId } = useAuth();
  const patientId = selectedPatientId || user?.patientId || 'b1000000-0000-0000-0000-000000000001';

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
      console.warn('Failed to load today medication schedule:', err.message);
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
      setCaregivers(data || []);
    } catch (err: any) {
      console.warn('Failed to load caregivers:', err.message);
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
          d.eventId === eventId
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

  const addMedication = async (med: Partial<Medication>): Promise<Medication> => {
    setIsLoading(true);
    try {
      const effectiveName = med.name || med.medicineName || 'Medication';
      const effectiveTimes = med.reminderTimes && med.reminderTimes.length > 0 ? med.reminderTimes : ['08:00:00'];
      const payload = {
        patientId: med.patientId || patientId,
        name: effectiveName,
        dosageAmount: med.dosageAmount || 1,
        dosageUnit: med.dosageUnit || 'TABLET',
        timesPerDay: med.timesPerDay || effectiveTimes.length,
        reminderTimes: effectiveTimes,
        startDate: med.startDate || new Date().toISOString().split('T')[0],
        endDate: med.endDate,
        foodInstruction: med.foodInstruction || 'NONE',
        instructions: med.instructions || '',
      };
      const created = await medicationApi.addMedication(payload as any);
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

  const scanPillPhoto = async (file: File): Promise<OcrExtractionResult> => {
    setIsOcrLoading(true);
    setError(null);
    try {
      const result = await medicationApi.extractOcr(file);
      return result;
    } catch (err: any) {
      setError(err.message || 'OCR extraction failed');
      throw err;
    } finally {
      setIsOcrLoading(false);
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
      };
      const created = await caregiverApi.addCaregiver(payload as any);
      setCaregivers((prev) => [...prev, created]);
      return created;
    } catch (err: any) {
      setError(err.message || 'Failed to add caregiver');
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
    scanPillPhoto,
    addCaregiver,
    deleteCaregiver,
  };
};
