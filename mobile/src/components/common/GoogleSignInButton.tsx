import React, { useState } from 'react';
import { View, Text, TouchableOpacity, StyleSheet, ActivityIndicator, Alert } from 'react-native';
import { Ionicons } from '@expo/vector-icons';
import Constants, { ExecutionEnvironment } from 'expo-constants';
import { useTheme } from '../../context/ThemeContext';
import { authApi } from '../../api/authApi';
import { useAuth } from '../../context/AuthContext';
import { GOOGLE_WEB_CLIENT_ID } from '../../config/apiConfig';

interface GoogleSignInButtonProps {
  onSignInSuccess?: (googleUser: any) => Promise<void> | void;
  onRegistrationRequired?: (data: { idToken: string; email: string; fullName: string }) => void;
  onError?: (err: any) => void;
  navigation?: any;
}

export const GoogleSignInButton: React.FC<GoogleSignInButtonProps> = ({
  onSignInSuccess,
  onRegistrationRequired,
  onError,
  navigation,
}) => {
  const { colors } = useTheme();
  const { loginWithToken, loginAsGuest } = useAuth();
  const [loading, setLoading] = useState(false);

  const isExpoGo = Constants.executionEnvironment === ExecutionEnvironment.StoreClient;

  const handlePress = async () => {
    if (isExpoGo) {
      Alert.alert(
        'Google Sign-In',
        'Native Google Sign-In requires standalone APK. Would you like to enter as Guest Patient now?',
        [
          { text: 'Cancel', style: 'cancel' },
          { text: '⚡ Continue as Guest', onPress: () => loginAsGuest('PATIENT') },
        ]
      );
      return;
    }

    setLoading(true);
    try {
      const { GoogleSignin } = require('@react-native-google-signin/google-signin');

      GoogleSignin.configure({
        webClientId: GOOGLE_WEB_CLIENT_ID || undefined,
        offlineAccess: false,
      });

      await GoogleSignin.hasPlayServices({ showPlayServicesUpdateDialog: true });
      const signInResult = await GoogleSignin.signIn();
      const idToken = signInResult?.data?.idToken || signInResult?.idToken;

      if (!idToken) {
        throw new Error('Google Sign-In did not return an ID token.');
      }

      if (onSignInSuccess) {
        await onSignInSuccess({ idToken, user: signInResult?.data?.user || signInResult?.user });
        return;
      }

      const res = await authApi.googleAuth({ idToken });

      if (res?.registrationRequired) {
        if (onRegistrationRequired) {
          onRegistrationRequired({
            idToken,
            email: res.email || '',
            fullName: res.fullName || '',
          });
        } else if (navigation) {
          navigation.navigate('Register', {
            googleIdToken: idToken,
            googleEmail: res.email || '',
            googleFullName: res.fullName || '',
          });
        }
      } else {
        await loginWithToken(res);
      }
    } catch (err: any) {
      if (err.code !== 'SIGN_IN_CANCELLED') {
        console.warn('Google Sign-In note:', err);
        Alert.alert(
          'Google Sign-In Notice',
          'Google Play services signature is pending for this preview build.\n\nWould you like to enter immediately as Guest Patient?',
          [
            { text: 'Cancel', style: 'cancel' },
            {
              text: '⚡ Enter as Guest Patient',
              style: 'default',
              onPress: () => loginAsGuest('PATIENT'),
            },
          ]
        );
      }
    } finally {
      setLoading(false);
    }
  };

  return (
    <View style={styles.container}>
      <TouchableOpacity
        style={[
          styles.button,
          {
            backgroundColor: colors.bgSurface,
            borderColor: colors.borderLight,
            opacity: isExpoGo ? 0.85 : 1,
          },
        ]}
        onPress={handlePress}
        disabled={loading}
        activeOpacity={0.8}
      >
        {loading ? (
          <ActivityIndicator size="small" color={colors.primary} />
        ) : (
          <View style={styles.innerRow}>
            <Ionicons name="logo-google" size={18} color="#EA4335" />
            <Text style={[styles.text, { color: colors.textMain }]}>Continue with Google</Text>
          </View>
        )}
      </TouchableOpacity>
    </View>
  );
};

const styles = StyleSheet.create({
  container: {
    marginVertical: 6,
    alignItems: 'center',
    width: '100%',
  },
  button: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    height: 50,
    borderRadius: 12,
    borderWidth: 1.5,
    width: '100%',
    paddingHorizontal: 16,
  },
  innerRow: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 10,
  },
  text: {
    fontSize: 15,
    fontWeight: '700',
  },
});
