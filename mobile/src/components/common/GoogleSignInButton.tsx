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
  const { loginWithToken } = useAuth();
  const [loading, setLoading] = useState(false);

  const isExpoGo = Constants.executionEnvironment === ExecutionEnvironment.StoreClient;

  const handlePress = async () => {
    if (isExpoGo) {
      Alert.alert(
        'Google Sign-In Unavailable in Expo Go',
        'Native Google Sign-In requires native Google Play Services binaries. It is available in the installed EAS build or APK.'
      );
      return;
    }

    setLoading(true);
    try {
      // Lazy-require so Expo Go never crashes on bundle load
      const { GoogleSignin } = require('@react-native-google-signin/google-signin');

      GoogleSignin.configure({
        webClientId: GOOGLE_WEB_CLIENT_ID || undefined,
        offlineAccess: false,
      });

      await GoogleSignin.hasPlayServices({ showPlayServicesUpdateDialog: true });
      const signInResult = await GoogleSignin.signIn();
      const idToken = signInResult?.data?.idToken || signInResult?.idToken;

      if (!idToken) {
        throw new Error('Google Sign-In failed to retrieve a valid ID token.');
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
        console.warn('Google Sign-In error:', err);
        if (onError) {
          onError(err);
        } else {
          Alert.alert('Google Sign-In', err.message || 'Unable to complete Google Sign-In.');
        }
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
            opacity: isExpoGo ? 0.65 : 1,
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
      {isExpoGo && (
        <Text style={[styles.note, { color: colors.textSubtle }]}>
          Available in the installed app
        </Text>
      )}
    </View>
  );
};

const styles = StyleSheet.create({
  container: {
    marginVertical: 8,
    alignItems: 'center',
    width: '100%',
  },
  button: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    height: 48,
    borderRadius: 10,
    borderWidth: 1,
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
    fontWeight: '600',
  },
  note: {
    fontSize: 11,
    marginTop: 4,
    textAlign: 'center',
  },
});
