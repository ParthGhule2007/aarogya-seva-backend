import React, { useState } from 'react';
import {
  View,
  Text,
  TextInput,
  StyleSheet,
  ScrollView,
  TouchableOpacity,
  KeyboardAvoidingView,
  Platform,
  Alert,
} from 'react-native';
import { Ionicons } from '@expo/vector-icons';
import { useAuth, DEMO_CREDENTIALS } from '../../context/AuthContext';
import { useTheme } from '../../context/ThemeContext';
import { useLanguage } from '../../i18n/LanguageContext';
import { Button } from '../../components/common/Button';
import { Card } from '../../components/common/Card';
import { GoogleSignInButton } from '../../components/common/GoogleSignInButton';

interface LoginScreenProps {
  navigation: any;
}

export const LoginScreen: React.FC<LoginScreenProps> = ({ navigation }) => {
  const { login, switchDemoRole, isLoading, error } = useAuth();
  const { colors } = useTheme();
  const { language, setLanguage, t } = useLanguage();

  const [username, setUsername] = useState('patient.ramesh');
  const [password, setPassword] = useState('Password@123');

  const handleLogin = async () => {
    if (!username.trim() || !password.trim()) {
      Alert.alert('Required', 'Please enter username and password');
      return;
    }
    try {
      await login(username.trim(), password.trim());
    } catch (err: any) {
      Alert.alert('Sign In Notice', err.message || 'Unable to sign in. Please verify your connection.');
    }
  };

  const handleDemoSelect = async (demoUsername: string, demoPass: string, demoRole: any) => {
    setUsername(demoUsername);
    setPassword(demoPass);
    try {
      await switchDemoRole(demoRole);
    } catch {
      // handled inside switchDemoRole
    }
  };

  return (
    <KeyboardAvoidingView
      behavior={Platform.OS === 'ios' ? 'padding' : undefined}
      style={{ flex: 1, backgroundColor: colors.bgApp }}
    >
      <ScrollView contentContainerStyle={styles.scrollContainer} keyboardShouldPersistTaps="handled">
        {/* Top utility row: language */}
        <View style={styles.topBar}>
          <TouchableOpacity
            style={[styles.smallChip, { backgroundColor: colors.bgSurface, borderColor: colors.borderLight }]}
            onPress={() => setLanguage(language === 'en' ? 'hi' : language === 'hi' ? 'mr' : 'en')}
          >
            <View style={{ flexDirection: 'row', alignItems: 'center', gap: 4 }}>
              <Ionicons name="globe-outline" size={14} color={colors.textMain} />
              <Text style={[styles.chipText, { color: colors.textMain }]}>
                {language.toUpperCase()}
              </Text>
            </View>
          </TouchableOpacity>
        </View>

        {/* Branding header */}
        <View style={styles.brandContainer}>
          <View style={[styles.logoCircle, { backgroundColor: colors.primaryLight }]}>
            <Ionicons name="medical" size={32} color={colors.primary} />
          </View>
          <Text style={[styles.appName, { color: colors.primary }]}>{t('brandName')}</Text>
          <Text style={[styles.appSubtitle, { color: colors.textSubtle }]}>
            {t('brandSubtitle')}
          </Text>
        </View>

        {/* Main Sign In Card */}
        <Card style={styles.formCard}>
          <Text style={[styles.formTitle, { color: colors.textMain }]}>{t('login')}</Text>
          <Text style={[styles.formSubtitle, { color: colors.textSubtle }]}>
            {t('signInSubtitle')}
          </Text>

          {error ? (
            <View style={[styles.errorBox, { backgroundColor: colors.riskHighBg, borderColor: colors.riskHighBorder }]}>
              <Text style={[styles.errorText, { color: colors.riskHigh }]}>{error}</Text>
            </View>
          ) : null}

          {/* Username Input */}
          <Text style={[styles.inputLabel, { color: colors.textMuted }]}>{t('username')}</Text>
          <TextInput
            style={[styles.input, { backgroundColor: colors.bgSubtle, borderColor: colors.borderLight, color: colors.textMain }]}
            placeholder="Username, phone, or email"
            placeholderTextColor={colors.textSubtle}
            autoCapitalize="none"
            value={username}
            onChangeText={setUsername}
          />

          {/* Password Input */}
          <Text style={[styles.inputLabel, { color: colors.textMuted }]}>{t('password')}</Text>
          <TextInput
            style={[styles.input, { backgroundColor: colors.bgSubtle, borderColor: colors.borderLight, color: colors.textMain }]}
            placeholder="Password"
            placeholderTextColor={colors.textSubtle}
            secureTextEntry
            value={password}
            onChangeText={setPassword}
          />

          <Button
            title={isLoading ? t('loggingIn') : t('login')}
            onPress={handleLogin}
            loading={isLoading}
            style={styles.loginBtn}
          />

          <GoogleSignInButton navigation={navigation} />

          <TouchableOpacity
            style={styles.registerLink}
            onPress={() => navigation.navigate('Register')}
          >
            <Text style={[styles.registerLinkText, { color: colors.primary }]}>
              {t('dontHaveAccount')}
            </Text>
          </TouchableOpacity>
        </Card>

        {/* Quick-Fill Demo Logins */}
        <View style={styles.demoSection}>
          <View style={{ flexDirection: 'row', alignItems: 'center', gap: 6, marginBottom: 4 }}>
            <Ionicons name="flash-outline" size={16} color={colors.primary} />
            <Text style={[styles.demoSectionTitle, { color: colors.textSubtle }]}>
              QUICK-FILL DEMO CREDENTIALS
            </Text>
          </View>
          <Text style={[styles.demoNotice, { color: colors.textSubtle }]}>
            {t('demoModeNotice')}
          </Text>

          <View style={styles.demoGrid}>
            {DEMO_CREDENTIALS.map((cred) => (
              <TouchableOpacity
                key={cred.role}
                style={[
                  styles.demoButton,
                  {
                    backgroundColor: colors.bgSurface,
                    borderColor: colors.borderMedium,
                  },
                ]}
                onPress={() => handleDemoSelect(cred.username, cred.pass, cred.role)}
                activeOpacity={0.7}
              >
                <View style={styles.demoBadgeRow}>
                  <Text style={[styles.demoRoleBadge, { color: colors.primary }]}>
                    {cred.role}
                  </Text>
                </View>
                <Text style={[styles.demoLabel, { color: colors.textMain }]}>{cred.label}</Text>
                <Text style={[styles.demoSub, { color: colors.textSubtle }]}>
                  {cred.username}
                </Text>
              </TouchableOpacity>
            ))}
          </View>
        </View>
      </ScrollView>
    </KeyboardAvoidingView>
  );
};

const styles = StyleSheet.create({
  scrollContainer: {
    padding: 20,
    paddingTop: Platform.OS === 'android' ? 40 : 20,
    paddingBottom: 40,
  },
  topBar: {
    flexDirection: 'row',
    justifyContent: 'flex-end',
    gap: 8,
    marginBottom: 16,
  },
  smallChip: {
    paddingHorizontal: 10,
    paddingVertical: 6,
    borderRadius: 8,
    borderWidth: 1,
  },
  chipText: {
    fontSize: 12,
    fontWeight: '700',
  },
  brandContainer: {
    alignItems: 'center',
    marginBottom: 20,
  },
  logoCircle: {
    width: 64,
    height: 64,
    borderRadius: 32,
    alignItems: 'center',
    justifyContent: 'center',
    marginBottom: 8,
  },
  logoIcon: {
    fontSize: 32,
  },
  appName: {
    fontSize: 26,
    fontWeight: '900',
    letterSpacing: -0.5,
  },
  appSubtitle: {
    fontSize: 13,
    textAlign: 'center',
    marginTop: 4,
  },
  formCard: {
    padding: 20,
    borderRadius: 18,
  },
  formTitle: {
    fontSize: 20,
    fontWeight: '800',
  },
  formSubtitle: {
    fontSize: 13,
    marginTop: 2,
    marginBottom: 16,
  },
  errorBox: {
    padding: 10,
    borderRadius: 8,
    borderWidth: 1,
    marginBottom: 12,
  },
  errorText: {
    fontSize: 13,
    fontWeight: '600',
  },
  inputLabel: {
    fontSize: 13,
    fontWeight: '600',
    marginBottom: 6,
    marginTop: 8,
  },
  input: {
    height: 48,
    borderRadius: 10,
    borderWidth: 1,
    paddingHorizontal: 14,
    fontSize: 15,
  },
  loginBtn: {
    marginTop: 18,
  },
  registerLink: {
    marginTop: 14,
    alignItems: 'center',
  },
  registerLinkText: {
    fontSize: 13,
    fontWeight: '700',
  },
  demoSection: {
    marginTop: 24,
  },
  demoSectionTitle: {
    fontSize: 11,
    fontWeight: '800',
    letterSpacing: 0.8,
    marginBottom: 4,
  },
  demoNotice: {
    fontSize: 12,
    lineHeight: 16,
    marginBottom: 12,
  },
  demoGrid: {
    gap: 8,
  },
  demoButton: {
    padding: 12,
    borderRadius: 12,
    borderWidth: 1,
  },
  demoBadgeRow: {
    flexDirection: 'row',
    marginBottom: 2,
  },
  demoRoleBadge: {
    fontSize: 10,
    fontWeight: '800',
  },
  demoLabel: {
    fontSize: 14,
    fontWeight: '700',
  },
  demoSub: {
    fontSize: 12,
    marginTop: 2,
  },
});
