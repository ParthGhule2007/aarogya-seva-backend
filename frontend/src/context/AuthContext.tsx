import React, { createContext, useContext, useState, useEffect, ReactNode, useCallback } from 'react';
import { AuthUser, RegisterRequest, UserRole } from '../types/auth';
import { authApi, normalizeAuthUser } from '../api/authApi';
import { doctorApi } from '../api/doctorApi';
import { tokenStorage } from '../api/client';

interface AuthContextType {
  user: AuthUser | null;
  role: UserRole;
  isAuthenticated: boolean;
  isLoading: boolean;
  error: string | null;
  login: (username: string, password: string) => Promise<void>;
  register: (req: RegisterRequest) => Promise<void>;
  logout: () => void;
  switchDemoRole: (role: UserRole) => Promise<void>;
  selectedPatientId: string | null;
  setSelectedPatientId: (id: string | null) => void;
  selectedPatientName: string | null;
  setSelectedPatientName: (name: string | null) => void;
}


const DEMO_CREDENTIALS: Record<UserRole, { username: string; pass: string }> = {
  PATIENT: { username: 'patient.ramesh', pass: 'Password@123' },
  ASHA: { username: 'asha.sunita', pass: 'Password@123' },
  DOCTOR: { username: 'doctor.ananya', pass: 'Password@123' },
  ADMIN: { username: 'admin', pass: 'Password@123' },
  FACILITY_ADMIN: { username: 'facility.indapur', pass: 'Password@123' },
  CAREGIVER: { username: 'caregiver.suresh', pass: 'Password@123' },
};

const DEMO_USERS: Record<UserRole, AuthUser> = {
  PATIENT: {
    username: 'patient.ramesh',
    email: 'ramesh.patil@ruralhealth.org',
    role: 'PATIENT',
    displayName: 'Ramesh Patil',
    patientId: 'b1000000-0000-0000-0000-000000000001',
  },
  ASHA: {
    username: 'asha.sunita',
    email: 'sunita.tai@ruralhealth.org',
    role: 'ASHA',
    displayName: 'Sunita Tai (ASHA Worker)',
  },
  DOCTOR: {
    username: 'doctor.ananya',
    email: 'dr.ananya@ruralhealth.org',
    role: 'DOCTOR',
    displayName: 'Dr. Ananya Sharma',
    doctorId: '1',
  },
  ADMIN: {
    username: 'admin',
    email: 'admin.phc@ruralhealth.org',
    role: 'ADMIN',
    displayName: 'Facility Administrator',
    facilityId: 'c2000000-0000-0000-0000-000000000002',
    associatedEntityId: 'c2000000-0000-0000-0000-000000000002',
  },
  FACILITY_ADMIN: {
    username: 'facility.indapur',
    email: 'facility.indapur@ruralhealth.org',
    role: 'FACILITY_ADMIN',
    displayName: 'Indapur SDH Administrator',
    facilityId: 'c2000000-0000-0000-0000-000000000002',
    associatedEntityId: 'c2000000-0000-0000-0000-000000000002',
  },
  CAREGIVER: {
    username: 'caregiver.suresh',
    email: 'suresh.patil@ruralhealth.org',
    role: 'CAREGIVER',
    displayName: 'Suresh Patil (Caregiver)',
  },
};

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export const AuthProvider: React.FC<{ children: ReactNode }> = ({ children }) => {
  const [user, setUser] = useState<AuthUser | null>(null);
  const [isLoading, setIsLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);

  // ASHA or Admin selected patient boundary, or authenticated patient ID
  const [selectedPatientId, setSelectedPatientId] = useState<string | null>(() => {
    try {
      return localStorage.getItem('aarogya_selected_patient_id') || null;
    } catch {
      return null;
    }
  });

  const [selectedPatientName, setSelectedPatientName] = useState<string | null>(() => {
    try {
      return localStorage.getItem('aarogya_selected_patient_name') || null;
    } catch {
      return null;
    }
  });

  useEffect(() => {
    if (selectedPatientId) {
      localStorage.setItem('aarogya_selected_patient_id', selectedPatientId);
    } else {
      localStorage.removeItem('aarogya_selected_patient_id');
    }
  }, [selectedPatientId]);

  useEffect(() => {
    if (selectedPatientName) {
      localStorage.setItem('aarogya_selected_patient_name', selectedPatientName);
    } else {
      localStorage.removeItem('aarogya_selected_patient_name');
    }
  }, [selectedPatientName]);

  const restoreSession = useCallback(async () => {
    const token = tokenStorage.get();
    if (!token) {
      setIsLoading(false);
      return;
    }
    try {
      const me = await authApi.getMe();
      if (me.role === 'DOCTOR' && me.id && !me.doctorId) {
        try {
          const doc = await doctorApi.getDoctorByUserId(me.id);
          if (doc) {
            me.doctorId = String(doc.doctorId || doc.id);
          }
        } catch {
          // Profile may be registered later
        }
      }
      setUser(me);
      if (me.role === 'PATIENT') {
        if (me.patientId) {
          setSelectedPatientId(me.patientId);
          setSelectedPatientName(me.displayName);
        } else {
          setSelectedPatientId(null);
          setSelectedPatientName(null);
        }
      }
    } catch (err: any) {
      console.warn('Session restoration failed or expired:', err.message);
      tokenStorage.clear();
      setUser(null);
      setSelectedPatientId(null);
      setSelectedPatientName(null);
    } finally {
      setIsLoading(false);
    }
  }, []);

  useEffect(() => {
    restoreSession();

    const handleUnauthorized = () => {
      tokenStorage.clear();
      setUser(null);
      setSelectedPatientId(null);
      setSelectedPatientName(null);
    };

    window.addEventListener('aarogya:unauthorized', handleUnauthorized);
    return () => window.removeEventListener('aarogya:unauthorized', handleUnauthorized);
  }, [restoreSession]);

  const login = async (username: string, pass: string): Promise<void> => {
    setIsLoading(true);
    setError(null);
    try {
      const res = await authApi.login(username, pass);
      if (res.token) {
        tokenStorage.set(res.token);
      }
      const authUser = normalizeAuthUser(res);
      if (authUser.role === 'DOCTOR' && authUser.id && !authUser.doctorId) {
        try {
          const doc = await doctorApi.getDoctorByUserId(authUser.id);
          if (doc) {
            authUser.doctorId = String(doc.doctorId || doc.id);
          }
        } catch {
          // Profile may be registered later
        }
      }
      setUser(authUser);
      if (authUser.role === 'PATIENT' && authUser.patientId) {
        setSelectedPatientId(authUser.patientId);
        setSelectedPatientName(authUser.displayName);
      } else {
        setSelectedPatientId(null);
        setSelectedPatientName(null);
      }
    } catch (err: any) {
      const msg = err.message || 'Login failed';
      setError(msg);
      throw err;
    } finally {
      setIsLoading(false);
    }
  };

  const register = async (req: RegisterRequest): Promise<void> => {
    setIsLoading(true);
    setError(null);
    try {
      const res = await authApi.register(req);
      if (res.token) {
        tokenStorage.set(res.token);
      }
      const authUser = normalizeAuthUser(res);
      setUser(authUser);
      if (authUser.role === 'PATIENT' && authUser.patientId) {
        setSelectedPatientId(authUser.patientId);
        setSelectedPatientName(authUser.displayName);
      } else {
        setSelectedPatientId(null);
        setSelectedPatientName(null);
      }
    } catch (err: any) {
      const msg = err.message || 'Registration failed';
      setError(msg);
      throw err;
    } finally {
      setIsLoading(false);
    }
  };

  const logout = () => {
    tokenStorage.clear();
    setUser(null);
    setSelectedPatientId(null);
    setSelectedPatientName(null);
  };

  const switchDemoRole = async (targetRole: UserRole): Promise<void> => {
    const cred = DEMO_CREDENTIALS[targetRole];
    if (!cred) return;
    try {
      await login(cred.username, cred.pass);
    } catch (err: any) {
      // Demo fallback when backend service is offline
      const fallback = DEMO_USERS[targetRole];
      setUser(fallback);
      if (fallback.role === 'PATIENT' && fallback.patientId) {
        setSelectedPatientId(fallback.patientId);
        setSelectedPatientName(fallback.displayName);
      } else if (fallback.role === 'ASHA') {
        setSelectedPatientId('b1000000-0000-0000-0000-000000000001');
        setSelectedPatientName('Ramesh Patil (Villager)');
      } else {
        setSelectedPatientId(null);
        setSelectedPatientName(null);
      }
    }
  };

  const currentRole: UserRole = user?.role || 'PATIENT';

  return (
    <AuthContext.Provider
      value={{
        user,
        role: currentRole,
        isAuthenticated: !!user,
        isLoading,
        error,
        login,
        register,
        logout,
        switchDemoRole,
        selectedPatientId,
        setSelectedPatientId,
        selectedPatientName,
        setSelectedPatientName,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
};

export const useAuth = (): AuthContextType => {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
};
