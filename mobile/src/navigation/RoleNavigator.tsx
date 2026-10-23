import React from 'react';
import { useAuth } from '../context/AuthContext';
import { PatientTabNavigator } from './PatientTabNavigator';
import { AshaPlaceholderScreen } from '../screens/placeholders/AshaPlaceholderScreen';
import { DoctorPlaceholderScreen } from '../screens/placeholders/DoctorPlaceholderScreen';
import { CaregiverPlaceholderScreen } from '../screens/placeholders/CaregiverPlaceholderScreen';
import { AdminPlaceholderScreen } from '../screens/placeholders/AdminPlaceholderScreen';

export const RoleNavigator: React.FC = () => {
  const { role } = useAuth();

  switch (role) {
    case 'ASHA':
      return <AshaPlaceholderScreen />;
    case 'DOCTOR':
      return <DoctorPlaceholderScreen />;
    case 'CAREGIVER':
      return <CaregiverPlaceholderScreen />;
    case 'ADMIN':
    case 'FACILITY_ADMIN':
      return <AdminPlaceholderScreen />;
    case 'PATIENT':
    default:
      return <PatientTabNavigator />;
  }
};
