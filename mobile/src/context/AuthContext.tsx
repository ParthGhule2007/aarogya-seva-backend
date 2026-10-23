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

const DEMO_FALLBACK_USERS: Record<UserRole, AuthUser> = {
  PATIENT: {
    username: 'patient.ramesh',
    email: 'ramesh.patil@ruralhealth.org',
    role: 'PATIENT',
    displayName: 'Ramesh Patil',
    patientId: 'b1000000-0000-0000-0000-000000000001',
    associatedEntityId: 'b1000000-0000-0000-0000-000000000001',
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
      // Safe lazy signout from Google if installed app
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

  // Listen to 401 Unauthorized from central HTTP client
  useEffect(() => {
    const unsubscribe = onUnauthorized(() => {
      logout();
    });
    return unsubscribe;
  }, [logout]);

  // Restore session on app start
  useEffect(() => {
    const restoreSession = async () => {
      setIsLoading(true);
      try {
        const token = await tokenStorage.get();
        if (token) {
          try {
            // Attempt to fetch current user profile from /me
            const currentUser = await authApi.getMe();
            setUser(currentUser);
            if (currentUser.patientId) {
              setSelectedPatientId(currentUser.patientId);
            }
          } catch (meError) {
            // If /me fails but cached user is stored, restore cached user
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

  const login = async (username: string, pass: string): Promise<void> => {
    setIsLoading(true);
    setError(null);
    try {
      const response = await authApi.login(username.trim(), pass);
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
      const msg = err.message || 'Login failed. Please verify credentials.';
      setError(msg);
      throw err;
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
      if (response?.token) {
        await tokenStorage.set(response.token);
      }
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
      if (response?.token) {
        await tokenStorage.set(response.token);
      }
      const normalized = normalizeAuthUser(response);
      setUser(normalized);
      await SecureStore.setItemAsync(USER_STORAGE_KEY, JSON.stringify(normalized));
      if (normalized.patientId) setSelectedPatientId(normalized.patientId);
    } catch (err: any) {
      setError(err.message || 'Entity linking failed.');
      throw err;
    } finally {
      setIsLoading(false);
    }
  };

  const switchDemoRole = async (targetRole: UserRole): Promise<void> => {
    const cred = DEMO_CREDENTIALS.find((c) => c.role === targetRole);
    if (!cred) return;

    try {
      await login(cred.username, cred.pass);
    } catch (err: any) {
      if (IS_DEMO_MODE) {
        console.warn('DEMO MODE: Live login failed, using local demo profile:', err);
        const fallback = DEMO_FALLBACK_USERS[targetRole];
        setUser(fallback);
        if (fallback.patientId) {
          setSelectedPatientId(fallback.patientId);
        }
        return;
      }
      throw err;
    }
  };

  const role: UserRole = user?.role || 'PATIENT';
  const isAuthenticated = !!user;

  return (
    <AuthContext.Provider
      value={{
        user,
        role,
        isAuthenticated,
        isLoading,
        error,
        login,
        loginWithToken,
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
