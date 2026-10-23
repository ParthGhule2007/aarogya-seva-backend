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

const Tab = createBottomTabNavigator();

export const PatientTabNavigator: React.FC = () => {
  const { colors } = useTheme();
  const { t } = useLanguage();

  return (
    <Tab.Navigator
      screenOptions={{
        headerShown: false,
        tabBarStyle: {
          backgroundColor: colors.bgSurface,
          borderTopColor: colors.borderLight,
          height: 60,
          paddingBottom: 8,
          paddingTop: 6,
        },
        tabBarActiveTintColor: colors.primary,
        tabBarInactiveTintColor: colors.textSubtle,
        tabBarLabelStyle: {
          fontSize: 11,
          fontWeight: '700',
        },
      }}
    >
      <Tab.Screen
        name="Dashboard"
        component={PatientDashboardScreen}
        options={{
          tabBarLabel: t('tabMeds'),
          tabBarIcon: ({ color, size }) => (
            <Ionicons name="medkit" size={20} color={color} />
          ),
        }}
      />
      <Tab.Screen
        name="Consultations"
        component={ConsultationsScreen}
        options={{
          tabBarLabel: t('tabConsult'),
          tabBarIcon: ({ color, size }) => (
            <Ionicons name="videocam" size={20} color={color} />
          ),
        }}
      />
      <Tab.Screen
        name="TalkToAi"
        component={TalkToAiScreen}
        options={{
          tabBarLabel: t('tabVoice'),
          tabBarIcon: ({ color, size }) => (
            <Ionicons name="call" size={20} color={color} />
          ),
        }}
      />
      <Tab.Screen
        name="Chatbot"
        component={ChatbotScreen}
        options={{
          tabBarLabel: t('tabChat'),
          tabBarIcon: ({ color, size }) => (
            <Ionicons name="chatbubble-ellipses" size={20} color={color} />
          ),
        }}
      />
      <Tab.Screen
        name="Facilities"
        component={FacilitiesScreen}
        options={{
          tabBarLabel: t('tabHospitals'),
          tabBarIcon: ({ color, size }) => (
            <Ionicons name="business" size={20} color={color} />
          ),
        }}
      />
    </Tab.Navigator>
  );
};
