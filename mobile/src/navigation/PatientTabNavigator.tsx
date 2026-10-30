import React from 'react';
import { createBottomTabNavigator } from '@react-navigation/bottom-tabs';
import { Ionicons } from '@expo/vector-icons';
import { useTheme } from '../context/ThemeContext';
import { useLanguage } from '../i18n/LanguageContext';
import { PatientDashboardScreen } from '../screens/patient/PatientDashboardScreen';
import { ConsultationsScreen } from '../screens/patient/ConsultationsScreen';
import { TalkToAiScreen } from '../screens/patient/TalkToAiScreen';
import { ChatbotScreen } from '../screens/patient/ChatbotScreen';
import { FacilitiesScreen } from '../screens/patient/FacilitiesScreen';
import { SymptomScreeningScreen } from '../screens/patient/SymptomScreeningScreen';
import { MedicalRecordsScreen } from '../screens/patient/MedicalRecordsScreen';

const Tab = createBottomTabNavigator();

export const PatientTabNavigator: React.FC = () => {
  const { colors, isElderlyMode } = useTheme();
  const { t } = useLanguage();

  return (
    <Tab.Navigator
      screenOptions={{
        headerShown: false,
        tabBarStyle: {
          backgroundColor: colors.bgSurface,
          borderTopColor: colors.borderLight,
          height: isElderlyMode ? 68 : 60,
          paddingBottom: isElderlyMode ? 10 : 8,
          paddingTop: 6,
        },
        tabBarActiveTintColor: colors.primary,
        tabBarInactiveTintColor: colors.textSubtle,
        tabBarLabelStyle: {
          fontSize: isElderlyMode ? 12 : 11,
          fontWeight: '700',
        },
      }}
    >
      <Tab.Screen
        name="Dashboard"
        component={PatientDashboardScreen}
        options={{
          tabBarLabel: t('tabMeds') || 'Medicines',
          tabBarIcon: ({ color, size }) => (
            <Ionicons name="medkit" size={isElderlyMode ? 24 : 20} color={color} />
          ),
        }}
      />
      <Tab.Screen
        name="Consultations"
        component={ConsultationsScreen}
        options={{
          tabBarLabel: t('tabConsult') || 'Doctor Call',
          tabBarIcon: ({ color, size }) => (
            <Ionicons name="videocam" size={isElderlyMode ? 24 : 20} color={color} />
          ),
        }}
      />
      <Tab.Screen
        name="TalkToAi"
        component={TalkToAiScreen}
        options={{
          tabBarLabel: t('tabVoice') || 'Voice AI',
          tabBarIcon: ({ color, size }) => (
            <Ionicons name="call" size={isElderlyMode ? 24 : 20} color={color} />
          ),
        }}
      />
      <Tab.Screen
        name="Chatbot"
        component={ChatbotScreen}
        options={{
          tabBarLabel: t('tabChat') || 'Chatbot',
          tabBarIcon: ({ color, size }) => (
            <Ionicons name="chatbubble-ellipses" size={isElderlyMode ? 24 : 20} color={color} />
          ),
        }}
      />
      <Tab.Screen
        name="Facilities"
        component={FacilitiesScreen}
        options={{
          tabBarLabel: t('tabHospitals') || 'Hospitals',
          tabBarIcon: ({ color, size }) => (
            <Ionicons name="business" size={isElderlyMode ? 24 : 20} color={color} />
          ),
        }}
      />

      {/* Deep-linkable clinical screens accessible via OmniSearch & quick action cards */}
      <Tab.Screen
        name="Triage"
        component={SymptomScreeningScreen}
        options={{
          tabBarButton: () => null,
          tabBarLabel: 'Symptom Triage',
        }}
      />
      <Tab.Screen
        name="Records"
        component={MedicalRecordsScreen}
        options={{
          tabBarButton: () => null,
          tabBarLabel: 'Health Records',
        }}
      />
    </Tab.Navigator>
  );
};
