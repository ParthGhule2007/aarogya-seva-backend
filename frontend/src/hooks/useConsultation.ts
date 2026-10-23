import { useState, useEffect, useCallback } from 'react';
import { consultationApi } from '../api/consultationApi';
import { communicationApi } from '../api/communicationApi';
import { ConsultationResponseDTO, ConsultationRequestDTO } from '../types/consultation';
import { CommunicationSessionResponseDTO } from '../types/communication';
import { useAuth } from '../context/AuthContext';

export const useConsultation = () => {
  const { user, selectedPatientId } = useAuth();
  const [consultations, setConsultations] = useState<ConsultationResponseDTO[]>([]);
  const [pendingQueue, setPendingQueue] = useState<ConsultationResponseDTO[]>([]);
  const [activeCallSession, setActiveCallSession] = useState<{
    consultationId: number;
    meetingUrl: string;
    roomId?: string;
    doctorName?: string;
    patientName?: string;
  } | null>(null);

  const [isLoading, setIsLoading] = useState<boolean>(false);
  const [error, setError] = useState<string | null>(null);

  const fetchUserConsultations = useCallback(async (userId?: string) => {
    const targetUserId = userId || selectedPatientId || user?.patientId;
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

  const fetchDoctorQueue = useCallback(async (doctorId?: number) => {
    const targetDocId = doctorId || (user?.doctorId ? Number(user.doctorId) : 1);
    setIsLoading(true);
    try {
      const data = await consultationApi.getPendingConsultationsByDoctorId(targetDocId);
      setPendingQueue(data || []);
      return data;
    } catch (err: any) {
      console.warn('Failed to fetch doctor consultation queue:', err.message);
      return [];
    } finally {
      setIsLoading(false);
    }
  }, [user]);

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
      setPendingQueue((prev) => prev.map((c) => (c.id === consultationId ? updated : c)));
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
      return updated;
    } catch (err: any) {
      setError(err.message || 'Failed to reject consultation');
      throw err;
    }
  };

  const startConsultation = async (consultationId: number, patientName?: string, doctorName?: string) => {
    setIsLoading(true);
    try {
      // Transition consultation to IN_CALL
      const updated = await consultationApi.startConsultation(consultationId);
      
      // Query communication session URL
      let meetingUrl = updated.meetingUrl || `https://meet.jit.si/AarogyaSeva_Room_${consultationId}`;
      let roomId = updated.roomId || `AarogyaSeva_Room_${consultationId}`;

      try {
        const commSession = await communicationApi.getSessionByConsultationId(consultationId);
        if (commSession && commSession.meetingUrl) {
          meetingUrl = commSession.meetingUrl;
          roomId = commSession.roomId;
        }
      } catch {
        // use fallback meetingUrl
      }

      setActiveCallSession({
        consultationId,
        meetingUrl,
        roomId,
        doctorName: doctorName || updated.doctorName,
        patientName: patientName || updated.username,
      });

      return updated;
    } catch (err: any) {
      setError(err.message || 'Failed to launch consultation call');
      throw err;
    } finally {
      setIsLoading(false);
    }
  };

  const completeConsultation = async (consultationId: number) => {
    setIsLoading(true);
    try {
      const completed = await consultationApi.completeConsultation(consultationId);
      setActiveCallSession(null);
      if (user?.role === 'DOCTOR') {
        fetchDoctorQueue();
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
      const cancelled = await consultationApi.cancelConsultation(consultationId, { cancelledBy: user?.role === 'DOCTOR' ? 'DOCTOR' : 'USER' });
      setConsultations((prev) => prev.map((c) => (c.id === consultationId ? cancelled : c)));
      return cancelled;
    } catch (err: any) {
      setError(err.message || 'Failed to cancel consultation');
      throw err;
    }
  };

  const endCallSession = () => {
    setActiveCallSession(null);
  };

  useEffect(() => {
    if (user?.role === 'DOCTOR') {
      fetchDoctorQueue();
    } else if (selectedPatientId || user?.patientId) {
      fetchUserConsultations();
    }
  }, [user, selectedPatientId, fetchUserConsultations, fetchDoctorQueue]);

  return {
    consultations,
    pendingQueue,
    activeCallSession,
    isLoading,
    error,
    fetchUserConsultations,
    fetchDoctorQueue,
    bookConsultation,
    acceptConsultation,
    rejectConsultation,
    startConsultation,
    completeConsultation,
    cancelConsultation,
    endCallSession,
  };
};
