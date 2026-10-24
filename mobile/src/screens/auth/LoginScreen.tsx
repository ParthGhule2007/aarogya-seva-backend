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
  ActivityIndicator,
} from 'react-native';
import { Ionicons } from '@expo/vector-icons';
import { useAuth, DEMO_CREDENTIALS } from '../../context/AuthContext';
import { useTheme } from '../../context/ThemeContext';
import { useLanguage } from '../../i18n/LanguageContext';
import { Button } from '../../components/common/Button';
import { Card } from '../../components/common/Card';
import { GoogleSignInButton } from '../../components/common/GoogleSignInButton';
import { UserRole } from '../../types/auth';

interface LoginScreenProps {
  navigation: any;
}

export const LoginScreen: React.FC<LoginScreenProps> = ({ navigation }) => {
  const { login, loginAsGuest, switchDemoRole, isLoading, error } = useAuth();
  const { colors } = useTheme();
  const { language, setLanguage, t } = useLanguage();

  const [username, setUsername] = useState('patient.ramesh');
  const [password, setPassword] = useState('Password@123');
  const [showPassword, setShowPassword] = useState(false);
  const [enteringRole, setEnteringRole] = useState<string | null>(null);

  const handleLogin = async () => {
    try {
      await login(username, password);
    } catch {
      // AuthContext handles automatic demo fallback seamlessly
    }
  };

  const handleFastGuestEntry = async (role: UserRole) => {
    setEnteringRole(role);
    try {
      await loginAsGuest(role);
    } finally {
      setEnteringRole(null);
    }
  };

  const handleSelectRoleChip = (u: string, p: string, r: UserRole) => {
    setUsername(u);
    setPassword(p);
  };

  return (
    <KeyboardAvoidingView
      behavior={Platform.OS === 'ios' ? 'padding' : undefined}
      style={{ flex: 1, backgroundColor: colors.bgApp }}
    >
      <ScrollView
        contentContainerStyle={styles.scrollContainer}
        keyboardShouldPersistTaps="handled"
        showsVerticalScrollIndicator={false}
      >
        {/* Top utility row: Language Toggle Pill */}
        <View style={styles.topBar}>
          <View style={styles.langPillContainer}>
            <TouchableOpacity
              style={[
                styles.langOption,
                language === 'en' && { backgroundColor: colors.primary },
              ]}
              onPress={() => setLanguage('en')}
            >
              <Text
                style={[
                  styles.langOptionText,
                  { color: language === 'en' ? '#FFFFFF' : colors.textSubtle },
                ]}
              >
                EN
              </Text>
            </TouchableOpacity>
            <TouchableOpacity
              style={[
                styles.langOption,
                language === 'hi' && { backgroundColor: colors.primary },
              ]}
              onPress={() => setLanguage('hi')}
            >
              <Text
                style={[
                  styles.langOptionText,
                  { color: language === 'hi' ? '#FFFFFF' : colors.textSubtle },
                ]}
              >
                हिंदी
              </Text>
            </TouchableOpacity>
            <TouchableOpacity
              style={[
                styles.langOption,
                language === 'mr' && { backgroundColor: colors.primary },
              ]}
              onPress={() => setLanguage('mr')}
            >
              <Text
                style={[
                  styles.langOptionText,
                  { color: language === 'mr' ? '#FFFFFF' : colors.textSubtle },
                ]}
              >
                मराठी
              </Text>
            </TouchableOpacity>
          </View>
        </View>

        {/* Branding Header */}
        <View style={styles.brandContainer}>
          <View style={[styles.logoCircle, { backgroundColor: colors.primaryLight }]}>
            <Ionicons name="medical" size={36} color={colors.primary} />
          </View>
          <Text style={[styles.appName, { color: colors.primary }]}>{t('brandName')}</Text>
          <Text style={[styles.appSubtitle, { color: colors.textSubtle }]}>
            {t('brandSubtitle')}
          </Text>
          <View style={[styles.statusBadge, { backgroundColor: '#ECFDF5', borderColor: '#A7F3D0' }]}>
            <View style={styles.statusDot} />
            <Text style={styles.statusBadgeText}>Services Online • Instant Demo Ready</Text>
          </View>
        </View>

        {/* FAST 1-TAP ROLE ACCESS (HERO SECTION) */}
        <Card style={[styles.quickAccessCard, { borderColor: colors.primary, borderWidth: 1.5 }]}>
          <View style={styles.quickAccessHeader}>
            <View style={{ flexDirection: 'row', alignItems: 'center', gap: 6 }}>
              <Ionicons name="flash" size={18} color="#D97706" />
              <Text style={[styles.quickAccessTitle, { color: colors.textMain }]}>
                1-Tap Instant Entry
              </Text>
            </View>
            <Text style={[styles.quickAccessSub, { color: colors.textSubtle }]}>
              No typing needed • Tap to open
            </Text>
          </View>

          <View style={styles.roleGrid}>
            {/* Patient Card */}
            <TouchableOpacity
              style={[
                styles.roleCard,
                { backgroundColor: colors.bgSubtle, borderColor: colors.borderLight },
              ]}
              onPress={() => handleFastGuestEntry('PATIENT')}
              activeOpacity={0.8}
              disabled={isLoading || enteringRole !== null}
            >
              <View style={[styles.roleIconWrap, { backgroundColor: '#E0F2FE' }]}>
                <Ionicons name="person" size={22} color="#0284C7" />
              </View>
              <View style={{ flex: 1 }}>
                <View style={{ flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between' }}>
                  <Text style={[styles.roleName, { color: colors.textMain }]}>Patient</Text>
                  <Text style={[styles.roleNameSub, { color: colors.primary }]}>Ramesh Patil</Text>
                </View>
                <Text style={[styles.roleDesc, { color: colors.textSubtle }]}>
                  Doses • Tele-Consult • Hospital Triage
                </Text>
              </View>
              {enteringRole === 'PATIENT' ? (
                <ActivityIndicator size="small" color={colors.primary} />
              ) : (
                <Ionicons name="arrow-forward-circle" size={22} color={colors.primary} />
              )}
            </TouchableOpacity>

            {/* Doctor Card */}
            <TouchableOpacity
              style={[
                styles.roleCard,
                { backgroundColor: colors.bgSubtle, borderColor: colors.borderLight },
              ]}
              onPress={() => handleFastGuestEntry('DOCTOR')}
              activeOpacity={0.8}
              disabled={isLoading || enteringRole !== null}
            >
              <View style={[styles.roleIconWrap, { backgroundColor: '#FEF3C7' }]}>
                <Ionicons name="fitness" size={22} color="#D97706" />
              </View>
              <View style={{ flex: 1 }}>
                <View style={{ flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between' }}>
                  <Text style={[styles.roleName, { color: colors.textMain }]}>Doctor</Text>
                  <Text style={[styles.roleNameSub, { color: '#D97706' }]}>Dr. Ananya Sharma</Text>
                </View>
                <Text style={[styles.roleDesc, { color: colors.textSubtle }]}>
                  OPD Queue • Live Video Consultation
                </Text>
              </View>
              {enteringRole === 'DOCTOR' ? (
                <ActivityIndicator size="small" color="#D97706" />
              ) : (
                <Ionicons name="arrow-forward-circle" size={22} color="#D97706" />
              )}
            </TouchableOpacity>

            {/* ASHA Card */}
            <TouchableOpacity
              style={[
                styles.roleCard,
                { backgroundColor: colors.bgSubtle, borderColor: colors.borderLight },
              ]}
              onPress={() => handleFastGuestEntry('ASHA')}
              activeOpacity={0.8}
              disabled={isLoading || enteringRole !== null}
            >
              <View style={[styles.roleIconWrap, { backgroundColor: '#FCE7F3' }]}>
                <Ionicons name="heart-half" size={22} color="#DB2777" />
              </View>
              <View style={{ flex: 1 }}>
                <View style={{ flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between' }}>
                  <Text style={[styles.roleName, { color: colors.textMain }]}>ASHA Worker</Text>
                  <Text style={[styles.roleNameSub, { color: '#DB2777' }]}>Sunita Tai</Text>
                </View>
                <Text style={[styles.roleDesc, { color: colors.textSubtle }]}>
                  Community Visits • SOS Alerts
                </Text>
              </View>
              {enteringRole === 'ASHA' ? (
                <ActivityIndicator size="small" color="#DB2777" />
              ) : (
                <Ionicons name="arrow-forward-circle" size={22} color="#DB2777" />
              )}
            </TouchableOpacity>
          </View>
        </Card>

        {/* OR SIGN IN WITH CREDENTIALS */}
        <Card style={styles.formCard}>
          <Text style={[styles.formTitle, { color: colors.textMain }]}>Or Sign In</Text>
          <Text style={[styles.formSubtitle, { color: colors.textSubtle }]}>
            Use your registered username & password
          </Text>

          {/* Quick Credential Chips */}
          <View style={styles.chipsRow}>
            {DEMO_CREDENTIALS.slice(0, 3).map((c) => {
              const active = username === c.username;
              return (
                <TouchableOpacity
                  key={c.role}
                  style={[
                    styles.credChip,
                    {
                      backgroundColor: active ? colors.primaryLight : colors.bgSubtle,
                      borderColor: active ? colors.primary : colors.borderLight,
                    },
                  ]}
                  onPress={() => handleSelectRoleChip(c.username, c.pass, c.role)}
                >
                  <Text
                    style={[
                      styles.credChipText,
                      { color: active ? colors.primary : colors.textMain },
                    ]}
                  >
                    {c.label.split(' ')[0]}
                  </Text>
                </TouchableOpacity>
              );
            })}
          </View>

          {error ? (
            <View style={[styles.errorBox, { backgroundColor: colors.riskHighBg, borderColor: colors.riskHighBorder }]}>
              <Text style={[styles.errorText, { color: colors.riskHigh }]}>{error}</Text>
            </View>
          ) : null}

          {/* Username Input */}
          <Text style={[styles.inputLabel, { color: colors.textMuted }]}>{t('username')}</Text>
          <View style={[styles.inputWrapper, { backgroundColor: colors.bgSubtle, borderColor: colors.borderLight }]}>
            <Ionicons name="person-outline" size={18} color={colors.textSubtle} style={styles.inputIcon} />
            <TextInput
              style={[styles.input, { color: colors.textMain }]}
              placeholder="Username or phone"
              placeholderTextColor={colors.textSubtle}
              autoCapitalize="none"
              value={username}
              onChangeText={setUsername}
            />
          </View>

          {/* Password Input */}
          <Text style={[styles.inputLabel, { color: colors.textMuted }]}>{t('password')}</Text>
          <View style={[styles.inputWrapper, { backgroundColor: colors.bgSubtle, borderColor: colors.borderLight }]}>
            <Ionicons name="lock-closed-outline" size={18} color={colors.textSubtle} style={styles.inputIcon} />
            <TextInput
              style={[styles.input, { color: colors.textMain }]}
              placeholder="Password"
              placeholderTextColor={colors.textSubtle}
              secureTextEntry={!showPassword}
              value={password}
              onChangeText={setPassword}
            />
            <TouchableOpacity onPress={() => setShowPassword(!showPassword)} style={styles.eyeBtn}>
              <Ionicons
                name={showPassword ? 'eye-off-outline' : 'eye-outline'}
                size={18}
                color={colors.textSubtle}
              />
            </TouchableOpacity>
          </View>

          <Button
            title={isLoading ? 'Signing In...' : 'Sign In to Account'}
            onPress={handleLogin}
            loading={isLoading}
            style={styles.loginBtn}
          />

          <View style={styles.dividerRow}>
            <View style={[styles.dividerLine, { backgroundColor: colors.borderLight }]} />
            <Text style={[styles.dividerText, { color: colors.textSubtle }]}>or</Text>
            <View style={[styles.dividerLine, { backgroundColor: colors.borderLight }]} />
          </View>

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
      </ScrollView>
    </KeyboardAvoidingView>
  );
};

const styles = StyleSheet.create({
  scrollContainer: {
    padding: 16,
    paddingTop: Platform.OS === 'android' ? 36 : 16,
    paddingBottom: 40,
  },
  topBar: {
    flexDirection: 'row',
    justifyContent: 'flex-end',
    marginBottom: 12,
  },
  langPillContainer: {
    flexDirection: 'row',
    backgroundColor: '#F3F4F6',
    borderRadius: 20,
    padding: 3,
  },
  langOption: {
    paddingHorizontal: 12,
    paddingVertical: 6,
    borderRadius: 16,
  },
  langOptionText: {
    fontSize: 12,
    fontWeight: '700',
  },
  brandContainer: {
    alignItems: 'center',
    marginBottom: 16,
  },
  logoCircle: {
    width: 68,
    height: 68,
    borderRadius: 34,
    alignItems: 'center',
    justifyContent: 'center',
    marginBottom: 8,
  },
  appName: {
    fontSize: 26,
    fontWeight: '900',
    letterSpacing: -0.5,
  },
  appSubtitle: {
    fontSize: 13,
    textAlign: 'center',
    marginTop: 2,
  },
  statusBadge: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 6,
    paddingHorizontal: 10,
    paddingVertical: 4,
    borderRadius: 12,
    borderWidth: 1,
    marginTop: 8,
  },
  statusDot: {
    width: 8,
    height: 8,
    borderRadius: 4,
    backgroundColor: '#10B981',
  },
  statusBadgeText: {
    fontSize: 11,
    fontWeight: '700',
    color: '#065F46',
  },
  quickAccessCard: {
    padding: 14,
    borderRadius: 16,
    marginBottom: 16,
  },
  quickAccessHeader: {
    marginBottom: 10,
  },
  quickAccessTitle: {
    fontSize: 15,
    fontWeight: '800',
  },
  quickAccessSub: {
    fontSize: 11,
    marginTop: 1,
  },
  roleGrid: {
    gap: 8,
  },
  roleCard: {
    flexDirection: 'row',
    alignItems: 'center',
    padding: 12,
    borderRadius: 12,
    borderWidth: 1,
    gap: 12,
  },
  roleIconWrap: {
    width: 42,
    height: 42,
    borderRadius: 21,
    alignItems: 'center',
    justifyContent: 'center',
  },
  roleName: {
    fontSize: 14,
    fontWeight: '800',
  },
  roleNameSub: {
    fontSize: 12,
    fontWeight: '700',
  },
  roleDesc: {
    fontSize: 11,
    marginTop: 2,
  },
  formCard: {
    padding: 16,
    borderRadius: 16,
  },
  formTitle: {
    fontSize: 18,
    fontWeight: '800',
  },
  formSubtitle: {
    fontSize: 12,
    marginTop: 2,
    marginBottom: 12,
  },
  chipsRow: {
    flexDirection: 'row',
    gap: 8,
    marginBottom: 12,
  },
  credChip: {
    flex: 1,
    paddingVertical: 7,
    borderRadius: 8,
    borderWidth: 1,
    alignItems: 'center',
  },
  credChipText: {
    fontSize: 12,
    fontWeight: '700',
  },
  errorBox: {
    padding: 10,
    borderRadius: 8,
    borderWidth: 1,
    marginBottom: 12,
  },
  errorText: {
    fontSize: 12,
    fontWeight: '600',
  },
  inputLabel: {
    fontSize: 12,
    fontWeight: '700',
    marginBottom: 4,
    marginTop: 4,
  },
  inputWrapper: {
    flexDirection: 'row',
    alignItems: 'center',
    height: 48,
    borderRadius: 10,
    borderWidth: 1,
    paddingHorizontal: 12,
    marginBottom: 8,
  },
  inputIcon: {
    marginRight: 8,
  },
  input: {
    flex: 1,
    height: '100%',
    fontSize: 14,
  },
  eyeBtn: {
    padding: 6,
  },
  loginBtn: {
    marginTop: 8,
    height: 48,
  },
  dividerRow: {
    flexDirection: 'row',
    alignItems: 'center',
    marginVertical: 12,
    gap: 8,
  },
  dividerLine: {
    flex: 1,
    height: 1,
  },
  dividerText: {
    fontSize: 12,
    fontWeight: '600',
  },
  registerLink: {
    marginTop: 10,
    alignItems: 'center',
  },
  registerLinkText: {
    fontSize: 13,
    fontWeight: '700',
  },
});
