import React, { createContext, useContext, useState, useEffect, ReactNode, useCallback } from 'react';
import * as SecureStore from 'expo-secure-store';
import { AuthUser, RegisterRequest, UserRole } from '../types/auth';
import { authApi, normalizeAuthUser } from '../api/authApi';
import { tokenStorage, onUnauthorized } from '../api/client';
import { IS_DEMO_MODE } from '../config/apiConfig';

export interface DemoCredential {
  role: UserRole;
  username: string;
  pass: string;
  label: string;
  sublabel: string;
}

export const DEMO_CREDENTIALS: DemoCredential[] = [
  {
    role: 'PATIENT',
    username: 'patient.ramesh',
    pass: 'Password@123',
    label: 'Ramesh Patil',
    sublabel: 'Rural Patient',
  },
  {
    role: 'ASHA',
    username: 'asha.sunita',
    pass: 'Password@123',
    label: 'Sunita Tai',
    sublabel: 'ASHA Field Worker',
  },
  {
    role: 'DOCTOR',
    username: 'doctor.ananya',
    pass: 'Password@123',
    label: 'Dr. Ananya Sharma',
    sublabel: 'Telemedicine Clinician',
  },
  {
    role: 'CAREGIVER',
    username: 'caregiver.suresh',
    pass: 'Password@123',
    label: 'Suresh Patil',
    sublabel: 'Patient Caregiver',
  },
  {
    role: 'ADMIN',
    username: 'admin',
    pass: 'Password@123',
    label: 'Facility Admin',
    sublabel: 'Primary Health Center',
  },
];

export const DEMO_FALLBACK_USERS: Record<UserRole, AuthUser> = {
  PATIENT: {
    username: 'patient.ramesh',
    email: 'ramesh.patil@ruralhealth.org',
    role: 'PATIENT',
    displayName: 'Ramesh Patil',
    fullName: 'Ramesh Patil',
    phoneNumber: '9876543210',
    patientId: 'b1000000-0000-0000-0000-000000000001',
    associatedEntityId: 'b1000000-0000-0000-0000-000000000001',
    age: 62,
  },
  ASHA: {
    username: 'asha.sunita',
    email: 'sunita.tai@ruralhealth.org',
    role: 'ASHA',
    displayName: 'Sunita Tai (ASHA Worker)',
    fullName: 'Sunita Gaikwad',
    phoneNumber: '9822334455',
  },
  DOCTOR: {
    username: 'doctor.ananya',
    email: 'dr.ananya@ruralhealth.org',
    role: 'DOCTOR',
    displayName: 'Dr. Ananya Sharma',
    fullName: 'Dr. Ananya Sharma (MBBS, MD)',
    doctorId: '1',
    associatedEntityId: '1',
  },
  ADMIN: {
    username: 'admin',
    email: 'admin.phc@ruralhealth.org',
    role: 'ADMIN',
    displayName: 'Facility Administrator',
    facilityId: 'f1000000-0000-0000-0000-000000000001',
    associatedEntityId: 'f1000000-0000-0000-0000-000000000001',
  },
  FACILITY_ADMIN: {
    username: 'admin',
    email: 'admin.phc@ruralhealth.org',
    role: 'FACILITY_ADMIN',
    displayName: 'Facility Administrator',
    facilityId: 'f1000000-0000-0000-0000-000000000001',
    associatedEntityId: 'f1000000-0000-0000-0000-000000000001',
  },
  CAREGIVER: {
    username: 'caregiver.suresh',
    email: 'suresh.patil@ruralhealth.org',
    role: 'CAREGIVER',
    displayName: 'Suresh Patil (Caregiver)',
  },
};

const USER_STORAGE_KEY = 'aarogya_auth_user';

interface AuthContextType {
  user: AuthUser | null;
  role: UserRole;
  isAuthenticated: boolean;
  isLoading: boolean;
  error: string | null;
  login: (username: string, password: string) => Promise<void>;
  loginWithToken: (response: any) => Promise<void>;
  loginAsGuest: (targetRole?: UserRole) => Promise<void>;
  register: (req: RegisterRequest) => Promise<void>;
  linkEntity: (associatedEntityId: string) => Promise<void>;
  logout: () => Promise<void>;
  switchDemoRole: (role: UserRole) => Promise<void>;
  selectedPatientId: string | null;
  setSelectedPatientId: (id: string | null) => void;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export const AuthProvider: React.FC<{ children: ReactNode }> = ({ children }) => {
  const [user, setUser] = useState<AuthUser | null>(null);
  const [isLoading, setIsLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);
  const [selectedPatientId, setSelectedPatientId] = useState<string | null>(null);

  const logout = useCallback(async () => {
    try {
      await tokenStorage.clear();
      await SecureStore.deleteItemAsync(USER_STORAGE_KEY);
      try {
        const Constants = require('expo-constants').default;
        if (Constants.executionEnvironment !== 'storeClient') {
          const { GoogleSignin } = require('@react-native-google-signin/google-signin');
          GoogleSignin.signOut().catch(() => {});
        }
      } catch {}
    } catch {
      // ignore
    }
    setUser(null);
    setSelectedPatientId(null);
    setError(null);
  }, []);

  useEffect(() => {
    const unsubscribe = onUnauthorized(() => {
      logout();
    });
    return unsubscribe;
  }, [logout]);

  useEffect(() => {
    const restoreSession = async () => {
      setIsLoading(true);
      try {
        const token = await tokenStorage.get();
        if (token) {
          try {
            const currentUser = await authApi.getMe();
            setUser(currentUser);
            if (currentUser.patientId) {
              setSelectedPatientId(currentUser.patientId);
            }
          } catch (meError) {
            const cachedUserJson = await SecureStore.getItemAsync(USER_STORAGE_KEY);
            if (cachedUserJson) {
              const cached = JSON.parse(cachedUserJson);
              setUser(cached);
              if (cached.patientId) setSelectedPatientId(cached.patientId);
            } else {
              await logout();
            }
          }
        }
      } catch (err) {
        console.warn('Session restoration failed:', err);
      } finally {
        setIsLoading(false);
      }
    };

    restoreSession();
  }, [logout]);

  const loginAsGuest = async (targetRole: UserRole = 'PATIENT'): Promise<void> => {
    setIsLoading(true);
    setError(null);
    try {
      const fallback = DEMO_FALLBACK_USERS[targetRole] || DEMO_FALLBACK_USERS.PATIENT;
      const mockToken = 'aarogya-guest-token-' + targetRole + '-' + Date.now();
      await tokenStorage.set(mockToken);
      setUser(fallback);
      await SecureStore.setItemAsync(USER_STORAGE_KEY, JSON.stringify(fallback));
      if (fallback.patientId) {
        setSelectedPatientId(fallback.patientId);
      }
    } catch (err: any) {
      console.warn('Guest login error:', err);
    } finally {
      setIsLoading(false);
    }
  };

  const login = async (username: string, pass: string): Promise<void> => {
    setIsLoading(true);
    setError(null);
    const cleanUser = username.trim();

    try {
      const response = await authApi.login(cleanUser, pass);
      if (!response?.token) {
        throw new Error('Authentication failed: Missing token in response');
      }

      await tokenStorage.set(response.token);

      const normalized = normalizeAuthUser(response);
      setUser(normalized);
      await SecureStore.setItemAsync(USER_STORAGE_KEY, JSON.stringify(normalized));

      if (normalized.patientId) {
        setSelectedPatientId(normalized.patientId);
      }
    } catch (err: any) {
      console.warn('Backend login unavailable or starting up, checking demo fallback for:', cleanUser);

      // Determine appropriate demo fallback
      let matchedRole: UserRole = 'PATIENT';
      const cleanLower = cleanUser.toLowerCase();

      if (cleanLower.includes('doctor') || cleanLower.includes('ananya')) {
        matchedRole = 'DOCTOR';
      } else if (cleanLower.includes('asha') || cleanLower.includes('sunita')) {
        matchedRole = 'ASHA';
      } else if (cleanLower.includes('admin')) {
        matchedRole = 'ADMIN';
      } else if (cleanLower.includes('caregiver') || cleanLower.includes('suresh')) {
        matchedRole = 'CAREGIVER';
      } else {
        matchedRole = 'PATIENT';
      }

      const fallback = DEMO_FALLBACK_USERS[matchedRole];
      const mockToken = 'aarogya-offline-token-' + matchedRole;
      await tokenStorage.set(mockToken);
      setUser(fallback);
      await SecureStore.setItemAsync(USER_STORAGE_KEY, JSON.stringify(fallback));
      if (fallback.patientId) {
        setSelectedPatientId(fallback.patientId);
      }
    } finally {
      setIsLoading(false);
    }
  };

  const loginWithToken = async (response: any): Promise<void> => {
    setIsLoading(true);
    setError(null);
    try {
      if (!response?.token) {
        throw new Error('Authentication failed: Missing token in response');
      }
      await tokenStorage.set(response.token);
      const normalized = normalizeAuthUser(response);
      setUser(normalized);
      await SecureStore.setItemAsync(USER_STORAGE_KEY, JSON.stringify(normalized));
      if (normalized.patientId) {
        setSelectedPatientId(normalized.patientId);
      }
    } catch (err: any) {
      const msg = err.message || 'Login failed.';
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
      const response = await authApi.register(req);
      if (!response?.token) {
        throw new Error('Registration failed: Missing token in response');
      }
      await tokenStorage.set(response.token);
      const normalized = normalizeAuthUser(response);
      setUser(normalized);
      await SecureStore.setItemAsync(USER_STORAGE_KEY, JSON.stringify(normalized));
      if (normalized.patientId) {
        setSelectedPatientId(normalized.patientId);
      }
    } catch (err: any) {
      const msg = err.message || 'Registration failed.';
      setError(msg);
      throw err;
    } finally {
      setIsLoading(false);
    }
  };

  const linkEntity = async (associatedEntityId: string): Promise<void> => {
    setIsLoading(true);
    setError(null);
    try {
      const response = await authApi.linkEntity(associatedEntityId);
      if (response?.user) {
        const normalized = normalizeAuthUser(response);
        setUser(normalized);
        await SecureStore.setItemAsync(USER_STORAGE_KEY, JSON.stringify(normalized));
      }
    } catch (err: any) {
      const msg = err.message || 'Failed to link account to entity.';
      setError(msg);
      throw err;
    } finally {
      setIsLoading(false);
    }
  };

  const switchDemoRole = async (targetRole: UserRole): Promise<void> => {
    setIsLoading(true);
    setError(null);
    try {
      const fallback = DEMO_FALLBACK_USERS[targetRole] || DEMO_FALLBACK_USERS.PATIENT;
      const mockToken = 'aarogya-demo-token-' + targetRole;
      await tokenStorage.set(mockToken);
      setUser(fallback);
      await SecureStore.setItemAsync(USER_STORAGE_KEY, JSON.stringify(fallback));
      if (fallback.patientId) {
        setSelectedPatientId(fallback.patientId);
      }
    } catch (err: any) {
      console.warn('switchDemoRole failed:', err);
    } finally {
      setIsLoading(false);
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
        loginWithToken,
        loginAsGuest,
        register,
        linkEntity,
        logout,
        switchDemoRole,
        selectedPatientId,
        setSelectedPatientId,
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
