import { useState, useEffect, useCallback, useRef } from 'react';
import { callingApi } from '../api/callingApi';
import {
  CallResponse,
  FollowUpProfile,
  TalkToAiRequest,
  FollowUpCallRequest,
  CallStatus,
} from '../types/calling';

const TERMINAL_STATUSES: CallStatus[] = [
  'COMPLETED',
  'BUSY',
  'NO_ANSWER',
  'CANCELLED',
  'FAILED',
];

export const useCalling = (initialPhoneNumber?: string) => {
  const [phoneNumber, setPhoneNumber] = useState<string>(initialPhoneNumber || '');
  const [activeSession, setActiveSession] = useState<CallResponse | null>(null);
  const [profile, setProfile] = useState<FollowUpProfile | null>(null);
  const [history, setHistory] = useState<CallResponse[]>([]);
  const [isLoading, setIsLoading] = useState<boolean>(false);
  const [error, setError] = useState<string | null>(null);

  const pollTimerRef = useRef<any>(null);

  const stopPolling = useCallback(() => {
    if (pollTimerRef.current) {
      clearInterval(pollTimerRef.current);
      pollTimerRef.current = null;
    }
  }, []);

  const fetchProfile = useCallback(async (phone: string) => {
    if (!phone) return;
    try {
      const p = await callingApi.getProfile(phone);
      setProfile(p);
    } catch {
      // Profile may not exist yet if patient hasn't called before
      setProfile(null);
    }
  }, []);

  const fetchHistory = useCallback(async (phone: string) => {
    if (!phone) return;
    try {
      const list = await callingApi.getCallHistory(phone);
      setHistory(list || []);
    } catch {
      setHistory([]);
    }
  }, []);

  const pollStatus = useCallback(
    (sessionId: string) => {
      stopPolling();
      pollTimerRef.current = setInterval(async () => {
        try {
          const updated = await callingApi.getCallStatus(sessionId);
          setActiveSession(updated);

          if (TERMINAL_STATUSES.includes(updated.status)) {
            stopPolling();
            if (phoneNumber) {
              fetchProfile(phoneNumber);
              fetchHistory(phoneNumber);
            }
          }
        } catch (err: any) {
          console.warn('Call status polling error:', err.message);
        }
      }, 3000);
    },
    [stopPolling, phoneNumber, fetchProfile, fetchHistory]
  );

  const startTalkToAi = async (req: TalkToAiRequest): Promise<CallResponse> => {
    setIsLoading(true);
    setError(null);
    try {
      const session = await callingApi.talkToAi(req);
      setActiveSession(session);
      setPhoneNumber(req.phoneNumber);
      pollStatus(session.sessionId);
      return session;
    } catch (err: any) {
      const msg = err.message || 'Failed to initiate AI call';
      setError(msg);
      throw err;
    } finally {
      setIsLoading(false);
    }
  };

  const startFollowUp = async (req: FollowUpCallRequest): Promise<CallResponse> => {
    setIsLoading(true);
    setError(null);
    try {
      const session = await callingApi.followUpCall(req);
      setActiveSession(session);
      setPhoneNumber(req.phoneNumber);
      pollStatus(session.sessionId);
      return session;
    } catch (err: any) {
      const msg = err.message || 'Failed to initiate follow-up call';
      setError(msg);
      throw err;
    } finally {
      setIsLoading(false);
    }
  };

  const terminateCall = async (): Promise<void> => {
    if (!activeSession) return;
    setIsLoading(true);
    try {
      const res = await callingApi.terminateCall(activeSession.sessionId);
      setActiveSession(res);
      stopPolling();
    } catch (err: any) {
      setError(err.message || 'Failed to terminate call');
    } finally {
      setIsLoading(false);
    }
  };

  const optOut = async (reason?: string): Promise<void> => {
    if (!phoneNumber) return;
    setIsLoading(true);
    try {
      const updated = await callingApi.optOut({ phoneNumber, reason });
      setProfile(updated);
    } catch (err: any) {
      setError(err.message || 'Failed to register opt-out');
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    if (initialPhoneNumber) {
      setPhoneNumber(initialPhoneNumber);
      fetchProfile(initialPhoneNumber);
      fetchHistory(initialPhoneNumber);
    }
  }, [initialPhoneNumber, fetchProfile, fetchHistory]);

  useEffect(() => {
    return () => stopPolling();
  }, [stopPolling]);

  return {
    phoneNumber,
    setPhoneNumber,
    activeSession,
    profile,
    history,
    isLoading,
    isCallActive: !!activeSession && !TERMINAL_STATUSES.includes(activeSession.status),
    error,
    startTalkToAi,
    startFollowUp,
    terminateCall,
    fetchProfile,
    fetchHistory,
    optOut,
    clearActiveSession: () => {
      stopPolling();
      setActiveSession(null);
    },
  };
};
