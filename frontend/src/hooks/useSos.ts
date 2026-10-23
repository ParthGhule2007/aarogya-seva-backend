import { useState, useEffect, useCallback, useRef } from 'react';
import { sosApi } from '../api/sosApi';
import { TargetType, CreateSosRequest, SosResponse, SosStatusResponse } from '../types/sos';
import { useAuth } from '../context/AuthContext';

export const useSos = () => {
  const { user, selectedPatientId } = useAuth();
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

  const pollIntervalRef = useRef<NodeJS.Timeout | null>(null);

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
        setCurrentCoords({
          latitude: pos.coords.latitude,
          longitude: pos.coords.longitude,
        });
        setIsLocating(false);
      },
      (err) => {
        setLocationError(
          err.code === 1
            ? 'Location access denied. Please enable GPS permissions.'
            : 'Unable to retrieve your location.'
        );
        // Fallback default coordinates (Maharashtra rural district)
        setCurrentCoords({ latitude: 18.5204, longitude: 73.8567 });
        setIsLocating(false);
      },
      { timeout: 10000, enableHighAccuracy: true }
    );
  }, []);

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
      }, 5000);
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
      facilityId,
      message: message || 'Emergency assistance requested via Aarogya Seva SOS',
    };

    try {
      const res = await sosApi.triggerSos(req, idempotencyKey);
      const initialStatus: SosStatusResponse = {
        sosId: res.sosId,
        status: res.status,
        triggeredAt: res.triggeredAt,
        targets: res.targets,
        totalRecipients: targets.length,
        successfulNotifications: 1,
        failedNotifications: 0,
      };
      setActiveSos(initialStatus);
      sessionStorage.setItem('aarogya_active_sos', JSON.stringify(initialStatus));
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
    detectLocation,
    triggerSos,
    resolveSos,
    clearActiveSos,
  };
};
