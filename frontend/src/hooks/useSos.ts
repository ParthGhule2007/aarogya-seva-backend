import React, { createContext, useContext, useState, useEffect, useCallback, useRef } from 'react';
import { sosApi } from '../api/sosApi';
import { authApi } from '../api/authApi';
import { TargetType, CreateSosRequest, SosResponse, SosStatusResponse, UserType } from '../types/sos';
import { useAuth } from '../context/AuthContext';

export interface SosContextValue {
  isSosModalOpen: boolean;
  setIsSosModalOpen: (open: boolean) => void;
  activeSos: SosStatusResponse | null;
  currentCoords: { latitude: number; longitude: number } | null;
  locationError: string | null;
  isLocating: boolean;
  isTriggering: boolean;
  error: string | null;
  emergencyAlertOptIn: boolean;
  setEmergencyAlertOptIn: (optIn: boolean) => void;
  detectLocation: () => void;
  syncDeviceAndLocation: (lat: number, lon: number, optIn?: boolean) => Promise<void>;
  triggerSos: (targets: TargetType[], facilityId?: string | null, message?: string) => Promise<SosResponse>;
  resolveSos: (sosId: string) => Promise<void>;
  clearActiveSos: () => void;
}

const SosContext = createContext<SosContextValue | undefined>(undefined);

const useSosInternal = (): SosContextValue => {
  const { user, role, selectedPatientId } = useAuth();
  const patientId = selectedPatientId || user?.patientId || 'b1000000-0000-0000-0000-000000000001';

  const [isSosModalOpen, setIsSosModalOpen] = useState<boolean>(false);
  const [activeSos, setActiveSos] = useState<SosStatusResponse | null>(() => {
    try {
      const saved = sessionStorage.getItem('aarogya_active_sos');
      return saved ? JSON.parse(saved) : null;
    } catch {
      return null;
    }
  });

  const [currentCoords, setCurrentCoords] = useState<{ latitude: number; longitude: number } | null>(null);
  const [locationError, setLocationError] = useState<string | null>(null);
  const [isLocating, setIsLocating] = useState<boolean>(false);
  const [isTriggering, setIsTriggering] = useState<boolean>(false);
  const [error, setError] = useState<string | null>(null);
  const [emergencyAlertOptIn, setEmergencyAlertOptIn] = useState<boolean>(() => {
    try {
      const saved = localStorage.getItem('aarogya_sos_opt_in');
      return saved !== 'false';
    } catch {
      return true;
    }
  });

  const pollIntervalRef = useRef<ReturnType<typeof setInterval> | null>(null);

  const syncDeviceAndLocation = useCallback(
    async (lat: number, lon: number, optIn: boolean = emergencyAlertOptIn) => {
      try {
        localStorage.setItem('aarogya_sos_opt_in', String(optIn));
        setEmergencyAlertOptIn(optIn);
      } catch {
        // Ignore storage errors
      }

      const activeUserId = user?.patientId || selectedPatientId || user?.id || patientId;
      const mappedType: UserType =
        role === 'ASHA'
          ? 'ASHA'
          : role === 'DOCTOR'
          ? 'DOCTOR'
          : role === 'CAREGIVER'
          ? 'CAREGIVER'
          : role === 'ADMIN' || role === 'FACILITY_ADMIN'
          ? 'FACILITY'
          : 'PATIENT';

      try {
        await sosApi.registerDevice({
          userId: activeUserId,
          fcmToken: `web-fcm-token-${activeUserId}`,
          userType: mappedType,
          emergencyAlertOptIn: optIn,
          latitude: lat,
          longitude: lon,
        });
      } catch (err: any) {
        console.warn('Could not register SOS device location:', err.message);
      }

      if (user) {
        try {
          await authApi.updateLocation(lat, lon);
        } catch {
          // Core API location sync is best-effort
        }
      }
    },
    [emergencyAlertOptIn, patientId, role, selectedPatientId, user]
  );

  // Request GPS coordinates
  const detectLocation = useCallback(() => {
    setIsLocating(true);
    setLocationError(null);
    if (!navigator.geolocation) {
      setLocationError('Geolocation is not supported by your browser.');
      setIsLocating(false);
      return;
    }

    navigator.geolocation.getCurrentPosition(
      (pos) => {
        const coords = {
          latitude: pos.coords.latitude,
          longitude: pos.coords.longitude,
        };
        setCurrentCoords(coords);
        setIsLocating(false);
        syncDeviceAndLocation(coords.latitude, coords.longitude);
      },
      (err) => {
        setLocationError(
          err.code === 1
            ? 'Location access denied. Using regional default coordinates (18.5204, 73.8567).'
            : 'Unable to retrieve live GPS. Using regional default coordinates (18.5204, 73.8567).'
        );
        const fallback = { latitude: 18.5204, longitude: 73.8567 };
        setCurrentCoords(fallback);
        setIsLocating(false);
      },
      { timeout: 10000, enableHighAccuracy: true }
    );
  }, [syncDeviceAndLocation]);

  // Poll active SOS status
  const pollSosStatus = useCallback(async (sosId: string) => {
    try {
      const status = await sosApi.getSosStatus(sosId);
      setActiveSos(status);
      sessionStorage.setItem('aarogya_active_sos', JSON.stringify(status));
      if (status.status === 'RESOLVED') {
        if (pollIntervalRef.current) clearInterval(pollIntervalRef.current);
      }
    } catch (err: any) {
      console.warn('Failed to poll SOS status:', err.message);
    }
  }, []);

  useEffect(() => {
    if (activeSos && activeSos.status !== 'RESOLVED') {
      pollIntervalRef.current = setInterval(() => {
        pollSosStatus(activeSos.sosId);
      }, 4000);
      return () => {
        if (pollIntervalRef.current) clearInterval(pollIntervalRef.current);
      };
    }
  }, [activeSos, pollSosStatus]);

  const triggerSos = async (
    targets: TargetType[],
    facilityId?: string | null,
    message?: string
  ): Promise<SosResponse> => {
    setIsTriggering(true);
    setError(null);

    const lat = currentCoords?.latitude || 18.5204;
    const lon = currentCoords?.longitude || 73.8567;
    const idempotencyKey = `sos-${Date.now()}-${Math.random().toString(36).substring(2, 9)}`;

    const req: CreateSosRequest = {
      patientId,
      latitude: lat,
      longitude: lon,
      targets,
      facilityId: targets.includes('FACILITY') ? facilityId : null,
      message: message || 'Emergency assistance requested via Aarogya Seva SOS',
    };

    try {
      const res = await sosApi.triggerSos(req, idempotencyKey);
      // Immediately fetch authoritative status from backend so recipient counts are accurate
      try {
        const realStatus = await sosApi.getSosStatus(res.sosId);
        setActiveSos(realStatus);
        sessionStorage.setItem('aarogya_active_sos', JSON.stringify(realStatus));
      } catch {
        const initialStatus: SosStatusResponse = {
          sosId: res.sosId,
          patientId,
          latitude: lat,
          longitude: lon,
          facilityId: req.facilityId,
          message: req.message,
          status: res.status,
          triggeredAt: res.triggeredAt,
          targets: res.targets,
          totalRecipients: 0,
          successfulNotifications: 0,
          failedNotifications: 0,
        };
        setActiveSos(initialStatus);
        sessionStorage.setItem('aarogya_active_sos', JSON.stringify(initialStatus));
      }
      return res;
    } catch (err: any) {
      setError(err.message || 'Failed to dispatch SOS alert. Please use direct emergency dial 108/112.');
      throw err;
    } finally {
      setIsTriggering(false);
    }
  };

  const resolveSos = async (sosId: string): Promise<void> => {
    try {
      const res = await sosApi.resolveSos(sosId);
      setActiveSos(res);
      sessionStorage.removeItem('aarogya_active_sos');
      if (pollIntervalRef.current) clearInterval(pollIntervalRef.current);
    } catch (err: any) {
      setError(err.message || 'Failed to resolve emergency incident');
      throw err;
    }
  };

  const clearActiveSos = () => {
    setActiveSos(null);
    sessionStorage.removeItem('aarogya_active_sos');
    if (pollIntervalRef.current) clearInterval(pollIntervalRef.current);
  };

  return {
    isSosModalOpen,
    setIsSosModalOpen,
    activeSos,
    currentCoords,
    locationError,
    isLocating,
    isTriggering,
    error,
    emergencyAlertOptIn,
    setEmergencyAlertOptIn,
    detectLocation,
    syncDeviceAndLocation,
    triggerSos,
    resolveSos,
    clearActiveSos,
  };
};

export const SosProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const value = useSosInternal();
  return React.createElement(SosContext.Provider, { value }, children);
};

export const useSos = (): SosContextValue => {
  const context = useContext(SosContext);
  if (context) {
    return context;
  }
  return useSosInternal();
};
