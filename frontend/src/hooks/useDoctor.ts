import { useState, useEffect, useCallback } from 'react';
import { doctorApi } from '../api/doctorApi';
import { AvailableDoctorResponseDTO, DoctorResponseDTO, DoctorRequestDTO, DoctorAvailabilityStatus } from '../types/doctor';
import { useAuth } from '../context/AuthContext';

export const useDoctor = () => {
  const { user } = useAuth();
  const [availableDoctors, setAvailableDoctors] = useState<AvailableDoctorResponseDTO[]>([]);
  const [allDoctors, setAllDoctors] = useState<DoctorResponseDTO[]>([]);
  const [currentDoctor, setCurrentDoctor] = useState<DoctorResponseDTO | null>(null);
  const [availability, setAvailability] = useState<DoctorAvailabilityStatus>('AVAILABLE');
  const [isLoading, setIsLoading] = useState<boolean>(false);
  const [error, setError] = useState<string | null>(null);

  const fetchAvailableDoctors = useCallback(async () => {
    setIsLoading(true);
    setError(null);
    try {
      const data = await doctorApi.getAvailableDoctors();
      setAvailableDoctors(data || []);
      return data;
    } catch (err: any) {
      console.warn('Failed to fetch available doctors:', err.message);
      return [];
    } finally {
      setIsLoading(false);
    }
  }, []);

  const fetchAllDoctors = useCallback(async () => {
    setIsLoading(true);
    try {
      const data = await doctorApi.getAllDoctors();
      setAllDoctors(data || []);
      return data;
    } catch (err: any) {
      console.warn('Failed to fetch all doctors:', err.message);
      return [];
    } finally {
      setIsLoading(false);
    }
  }, []);

  const fetchCurrentDoctorProfile = useCallback(async () => {
    if (!user || user.role !== 'DOCTOR') return;
    try {
      // 1. First try lookup by user ID (UUID string or numeric)
      if (user.id) {
        try {
          const byUser = await doctorApi.getDoctorByUserId(user.id);
          if (byUser) {
            setCurrentDoctor(byUser);
            if (byUser.availabilityStatus) {
              setAvailability(byUser.availabilityStatus as DoctorAvailabilityStatus);
            }
            return byUser;
          }
        } catch {
          // May not be registered by userId yet
        }
      }

      // 2. Lookup by explicit doctorId if attached to token/session
      if (user.doctorId) {
        try {
          const byDocId = await doctorApi.getDoctorById(Number(user.doctorId));
          if (byDocId) {
            setCurrentDoctor(byDocId);
            if (byDocId.availabilityStatus) {
              setAvailability(byDocId.availabilityStatus as DoctorAvailabilityStatus);
            }
            return byDocId;
          }
        } catch {
          // Fall through
        }
      }

      return null;
    } catch (err: any) {
      console.warn('Failed to fetch current doctor profile:', err.message);
      return null;
    }
  }, [user]);

  const registerDoctorProfile = async (formData: Omit<DoctorRequestDTO, 'userId'>) => {
    if (!user) throw new Error('User must be logged in to register doctor profile');
    setIsLoading(true);
    setError(null);
    try {
      const payload: DoctorRequestDTO = {
        userId: String(user.id),
        ...formData,
      };
      const created = await doctorApi.createDoctor(payload);
      setCurrentDoctor(created);
      if (created.availabilityStatus) {
        setAvailability(created.availabilityStatus as DoctorAvailabilityStatus);
      }
      return created;
    } catch (err: any) {
      setError(err.message || 'Failed to register doctor profile');
      throw err;
    } finally {
      setIsLoading(false);
    }
  };

  const updateAvailability = async (newStatus: DoctorAvailabilityStatus) => {
    const docId = currentDoctor?.doctorId || currentDoctor?.id || (user?.doctorId ? Number(user.doctorId) : undefined);
    if (!docId) {
      throw new Error('No doctor profile found for current user');
    }
    setIsLoading(true);
    try {
      const updated = await doctorApi.updateAvailability(docId, newStatus);
      setAvailability(newStatus);
      setCurrentDoctor(updated);
      return updated;
    } catch (err: any) {
      setError(err.message || 'Failed to update availability status');
      throw err;
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    fetchAvailableDoctors();
    if (user?.role === 'DOCTOR') {
      fetchCurrentDoctorProfile();
    }
  }, [user, fetchAvailableDoctors, fetchCurrentDoctorProfile]);

  return {
    availableDoctors,
    allDoctors,
    currentDoctor,
    availability,
    isLoading,
    error,
    fetchAvailableDoctors,
    fetchAllDoctors,
    updateAvailability,
    registerDoctorProfile,
    refreshCurrentDoctor: fetchCurrentDoctorProfile,
  };
};

