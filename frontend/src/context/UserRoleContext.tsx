import React, { createContext, useContext, useState, ReactNode } from 'react';

export type UserRole = 'patient' | 'asha' | 'facility';

interface UserRoleContextType {
  role: UserRole;
  setRole: (role: UserRole) => void;
  patientSubTab: 'home' | 'screening' | 'requests' | 'doctors' | 'consultations';
  setPatientSubTab: (tab: 'home' | 'screening' | 'requests' | 'doctors' | 'consultations') => void;
  facilitySubTab: 'dashboard' | 'requests';
  setFacilitySubTab: (tab: 'dashboard' | 'requests') => void;
  ashaSubTab: 'dashboard' | 'patients' | 'screening' | 'requests';
  setAshaSubTab: (tab: 'dashboard' | 'patients' | 'screening' | 'requests') => void;
  isSosOpen: boolean;
  setIsSosOpen: (open: boolean) => void;
}

const UserRoleContext = createContext<UserRoleContextType | undefined>(undefined);

export const UserRoleProvider: React.FC<{ children: ReactNode }> = ({ children }) => {
  const [role, setRole] = useState<UserRole>('patient');
  const [patientSubTab, setPatientSubTab] = useState<'home' | 'screening' | 'requests' | 'doctors' | 'consultations'>('home');
  const [facilitySubTab, setFacilitySubTab] = useState<'dashboard' | 'requests'>('dashboard');
  const [ashaSubTab, setAshaSubTab] = useState<'dashboard' | 'patients' | 'screening' | 'requests'>('dashboard');
  const [isSosOpen, setIsSosOpen] = useState<boolean>(false);

  return (
    <UserRoleContext.Provider
      value={{
        role,
        setRole,
        patientSubTab,
        setPatientSubTab,
        facilitySubTab,
        setFacilitySubTab,
        ashaSubTab,
        setAshaSubTab,
        isSosOpen,
        setIsSosOpen,
      }}
    >
      {children}
    </UserRoleContext.Provider>
  );
};

export const useUserRole = (): UserRoleContextType => {
  const context = useContext(UserRoleContext);
  if (!context) {
    throw new Error('useUserRole must be used within a UserRoleProvider');
  }
  return context;
};
