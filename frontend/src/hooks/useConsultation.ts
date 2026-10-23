import React, { createContext, useContext, useState, useEffect, useCallback } from 'react';
import { consultationApi } from '../api/consultationApi';
import { ConsultationResponseDTO, ConsultationRequestDTO } from '../types/consultation';
import { useAuth } from '../context/AuthContext';

const ConsultationContext = createContext<ReturnType<typeof useConsultationInternal> | undefined>(undefined);

const useConsultationInternal = () => {
  const { user, selectedPatientId } = useAuth();
  const [consultations, setConsultations] = useState<ConsultationResponseDTO[]>([]);
  const [pendingQueue, setPendingQueue] = useState<ConsultationResponseDTO[]>([]);
  const [doctorConsultations, setDoctorConsultations] = useState<ConsultationResponseDTO[]>([]);
  const [activeCallConsultation, setActiveCallConsultation] = useState<ConsultationResponseDTO | null>(null);
  const [callRole, setCallRole] = useState<'PATIENT' | 'DOCTOR'>('PATIENT');
  const [participantName, setParticipantName] = useState<string>('');

  const [isLoading, setIsLoading] = useState<boolean>(false);
  const [error, setError] = useState<string | null>(null);

  const fetchUserConsultations = useCallback(async (userId?: string) => {
    const targetUserId = userId || selectedPatientId || user?.patientId || (user?.role === 'PATIENT' ? user?.id : undefined);
    if (!targetUserId) return [];
    setIsLoading(true);
    try {
      const data = await consultationApi.getConsultationsByUserId(targetUserId);
      setConsultations(data || []);
      return data;
    } catch (err: any) {
      console.warn('Failed to fetch user consultations:', err.message);
      return [];
    } finally {
      setIsLoading(false);
    }
  }, [selectedPatientId, user]);

  const fetchDoctorConsultations = useCallback(async (doctorId?: number) => {
    const targetDocId = doctorId || (user?.doctorId ? Number(user.doctorId) : undefined);
    if (!targetDocId) return [];
    setIsLoading(true);
    try {
      const data = await consultationApi.getConsultationsByDoctorId(targetDocId);
      setDoctorConsultations(data || []);
      const pending = (data || []).filter((c: ConsultationResponseDTO) => c.status === 'REQUESTED');
      setPendingQueue(pending);
      return data;
    } catch (err: any) {
      console.warn('Failed to fetch doctor consultations:', err.message);
      try {
        const pending = await consultationApi.getPendingConsultationsByDoctorId(targetDocId);
        setPendingQueue(pending || []);
        setDoctorConsultations(pending || []);
        return pending;
      } catch {
        return [];
      }
    } finally {
      setIsLoading(false);
    }
  }, [user]);

  const fetchDoctorQueue = useCallback(async (doctorId?: number) => {
    return fetchDoctorConsultations(doctorId);
  }, [fetchDoctorConsultations]);

  const bookConsultation = async (req: ConsultationRequestDTO): Promise<ConsultationResponseDTO> => {
    setIsLoading(true);
    setError(null);
    try {
      const created = await consultationApi.createConsultation(req);
      setConsultations((prev) => [created, ...prev]);
      return created;
    } catch (err: any) {
      setError(err.message || 'Booking consultation failed');
      throw err;
    } finally {
      setIsLoading(false);
    }
  };

  const acceptConsultation = async (consultationId: number) => {
    try {
      const updated = await consultationApi.acceptConsultation(consultationId);
      setPendingQueue((prev) => prev.filter((c) => c.id !== consultationId));
      setDoctorConsultations((prev) => prev.map((c) => (c.id === consultationId ? updated : c)));
      return updated;
    } catch (err: any) {
      setError(err.message || 'Failed to accept consultation');
      throw err;
    }
  };

  const rejectConsultation = async (consultationId: number) => {
    try {
      const updated = await consultationApi.rejectConsultation(consultationId);
      setPendingQueue((prev) => prev.filter((c) => c.id !== consultationId));
      setDoctorConsultations((prev) => prev.map((c) => (c.id === consultationId ? updated : c)));
      return updated;
    } catch (err: any) {
      setError(err.message || 'Failed to reject consultation');
      throw err;
    }
  };

  const scheduleConsultation = async (consultationId: number, scheduledAt: string) => {
    try {
      const updated = await consultationApi.scheduleConsultation(consultationId, scheduledAt);
      setPendingQueue((prev) => prev.filter((c) => c.id !== consultationId));
      setDoctorConsultations((prev) => prev.map((c) => (c.id === consultationId ? updated : c)));
      return updated;
    } catch (err: any) {
      setError(err.message || 'Failed to schedule consultation');
      throw err;
    }
  };

  const launchCall = async (consultation: ConsultationResponseDTO, role: 'PATIENT' | 'DOCTOR', name?: string) => {
    setIsLoading(true);
    try {
      let current = consultation;
      if (
        (role === 'DOCTOR' && (current.status === 'ACCEPTED' || current.status === 'READY' || current.status === 'SCHEDULED')) ||
        (role === 'PATIENT' && current.status === 'READY')
      ) {
        try {
          current = await consultationApi.startConsultation(current.id);
        } catch (e) {
          console.warn('Could not transition status to IN_CALL, proceeding with session:', e);
        }
      }

      setCallRole(role);
      setParticipantName(name || (role === 'DOCTOR' ? (current.doctorName || 'Doctor') : (current.username || 'Patient')));
      setActiveCallConsultation(current);
      return current;
    } catch (err: any) {
      setError(err.message || 'Failed to launch consultation call');
      throw err;
    } finally {
      setIsLoading(false);
    }
  };

  const startConsultation = async (consultationId: number, patientNameParam?: string, doctorNameParam?: string) => {
    const existing = doctorConsultations.find((c) => c.id === consultationId) ||
      consultations.find((c) => c.id === consultationId);
    if (existing) {
      return launchCall(existing, user?.role === 'DOCTOR' ? 'DOCTOR' : 'PATIENT', user?.role === 'DOCTOR' ? doctorNameParam : patientNameParam);
    } else {
      const fetched = await consultationApi.getConsultationById(consultationId);
      return launchCall(fetched, user?.role === 'DOCTOR' ? 'DOCTOR' : 'PATIENT', user?.role === 'DOCTOR' ? doctorNameParam : patientNameParam);
    }
  };

  const completeConsultation = async (consultationId: number) => {
    setIsLoading(true);
    try {
      const completed = await consultationApi.completeConsultation(consultationId);
      setActiveCallConsultation(null);
      if (user?.role === 'DOCTOR') {
        fetchDoctorConsultations();
      } else {
        fetchUserConsultations();
      }
      return completed;
    } catch (err: any) {
      setError(err.message || 'Failed to complete consultation');
      throw err;
    } finally {
      setIsLoading(false);
    }
  };

  const cancelConsultation = async (consultationId: number) => {
    try {
      const cancelled = await consultationApi.cancelConsultation(consultationId, {
        cancelledBy: user?.role === 'DOCTOR' ? 'DOCTOR' : 'USER',
      });
      setConsultations((prev) => prev.map((c) => (c.id === consultationId ? cancelled : c)));
      setDoctorConsultations((prev) => prev.map((c) => (c.id === consultationId ? cancelled : c)));
      setPendingQueue((prev) => prev.filter((c) => c.id !== consultationId));
      return cancelled;
    } catch (err: any) {
      setError(err.message || 'Failed to cancel consultation');
      throw err;
    }
  };

  const endCallSession = () => {
    setActiveCallConsultation(null);
  };

  useEffect(() => {
    if (user?.role === 'DOCTOR') {
      fetchDoctorConsultations();
    } else if (selectedPatientId || user?.patientId || user?.role === 'PATIENT' || user?.id) {
      fetchUserConsultations();
    }
  }, [user, selectedPatientId, fetchUserConsultations, fetchDoctorConsultations]);

  const activeCallSession = activeCallConsultation
    ? {
      consultationId: activeCallConsultation.id,
      meetingUrl:
        activeCallConsultation.meetingUrl ||
        `https://${(
          (import.meta.env.VITE_JITSI_DOMAIN as string) || 'meet.jit.si'
        )
          .replace(/^https?:\/\//, '')
          .replace(/\/+$/, '')}/${activeCallConsultation.roomId || 'consultation-' + activeCallConsultation.id
        }`,
      roomId: activeCallConsultation.roomId,
      doctorName: activeCallConsultation.doctorName,
      patientName: activeCallConsultation.username,
    }
    : null;

  return {
    consultations,
    pendingQueue,
    doctorConsultations,
    activeCallConsultation,
    activeCallSession,
    callRole,
    participantName,
    isLoading,
    error,
    fetchUserConsultations,
    fetchDoctorQueue,
    fetchDoctorConsultations,
    bookConsultation,
    acceptConsultation,
    rejectConsultation,
    scheduleConsultation,
    launchCall,
    startConsultation,
    completeConsultation,
    cancelConsultation,
    endCallSession,
    setActiveCallConsultation,
  };
};

export const ConsultationProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const value = useConsultationInternal();
  return React.createElement(ConsultationContext.Provider, { value }, children);
};

export const useConsultation = () => {
  const ctx = useContext(ConsultationContext);
  if (ctx) return ctx;
  return useConsultationInternal();
};

export default useConsultation;
