import React, { useState } from 'react';
import { View, Text, StyleSheet, TouchableOpacity, Modal, Switch } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { StatusBar } from 'expo-status-bar';
import { Ionicons } from '@expo/vector-icons';
import { useAuth, DEMO_CREDENTIALS } from '../../context/AuthContext';
import { useTheme } from '../../context/ThemeContext';
import { useLanguage, SupportedLanguage } from '../../i18n/LanguageContext';
import { useLocationSharing } from '../../context/LocationSharingContext';
import { UserRole } from '../../types/auth';

export const Header: React.FC = () => {
  const insets = useSafeAreaInsets();
  const { user, role, switchDemoRole, logout } = useAuth();
  const { colors } = useTheme();
  const { language, setLanguage, t } = useLanguage();
  const { isSharingEnabled, setIsSharingEnabled } = useLocationSharing();
  const [showRoleModal, setShowRoleModal] = useState(false);
  const [showLangModal, setShowLangModal] = useState(false);
  const [showSettingsModal, setShowSettingsModal] = useState(false);

  const getRoleLabel = (r: UserRole): string => {
    switch (r) {
      case 'PATIENT': return t('rolePatient');
      case 'ASHA': return t('roleAsha');
      case 'DOCTOR': return t('roleDoctor');
      case 'CAREGIVER': return t('roleCaregiver');
      case 'ADMIN':
      case 'FACILITY_ADMIN': return t('roleFacility');
      default: return r;
    }
  };

  return (
    <View
      style={[
        styles.headerContainer,
        {
          backgroundColor: colors.bgSurface,
          borderBottomColor: colors.borderLight,
          paddingTop: Math.max(insets.top, 8) + 4,
        },
      ]}
    >
      <StatusBar style="dark" />
      {/* Brand & User info */}
      <View style={styles.brandRow}>
        <View>
          <Text style={[styles.brandTitle, { color: colors.primary }]}>{t('brandName')}</Text>
          <Text style={[styles.brandSubtitle, { color: colors.textSubtle }]}>
            {user?.displayName || t('brandSubtitle')}
          </Text>
        </View>

        {/* Action controls */}
        <View style={styles.controlsRow}>
          {/* Language Switcher */}
          <TouchableOpacity
            style={[styles.chipButton, { backgroundColor: colors.bgSubtle, borderColor: colors.borderMedium }]}
            onPress={() => setShowLangModal(true)}
            activeOpacity={0.7}
          >
            <Text style={[styles.chipText, { color: colors.textMain }]}>
              {language === 'hi' ? 'हिन्दी' : language === 'mr' ? 'मराठी' : 'EN'}
            </Text>
          </TouchableOpacity>

          {/* Settings / Location Sharing */}
          <TouchableOpacity
            style={[styles.chipButton, { backgroundColor: colors.bgSubtle, borderColor: colors.borderMedium }]}
            onPress={() => setShowSettingsModal(true)}
            activeOpacity={0.7}
            accessibilityLabel="Settings"
          >
            <Ionicons name="settings-outline" size={17} color={colors.textMain} />
          </TouchableOpacity>

          {/* Role Badge / Switcher */}
          <TouchableOpacity
            style={[styles.roleBadge, { backgroundColor: colors.primaryLight, borderColor: colors.primaryBorder }]}
            onPress={() => setShowRoleModal(true)}
            activeOpacity={0.7}
          >
            <Text style={[styles.roleBadgeText, { color: colors.primary }]}>
              {getRoleLabel(role)}
            </Text>
          </TouchableOpacity>

          {/* Sign out */}
          <TouchableOpacity
            style={[styles.chipButton, { backgroundColor: colors.bgSubtle, borderColor: colors.borderMedium }]}
            onPress={logout}
            activeOpacity={0.7}
          >
            <Ionicons name="log-out-outline" size={18} color={colors.sosRed} />
          </TouchableOpacity>
        </View>
      </View>

      {/* Role Switcher Modal */}
      <Modal visible={showRoleModal} transparent animationType="fade" onRequestClose={() => setShowRoleModal(false)}>
        <TouchableOpacity style={styles.modalBackdrop} activeOpacity={1} onPress={() => setShowRoleModal(false)}>
          <View style={[styles.modalCard, { backgroundColor: colors.bgSurface }]}>
            <Text style={[styles.modalTitle, { color: colors.textMain }]}>Demo Role Switcher</Text>
            <Text style={[styles.modalSubtitle, { color: colors.textSubtle }]}>
              Select an authorized clinical or patient profile:
            </Text>

            {DEMO_CREDENTIALS.map((cred) => (
              <TouchableOpacity
                key={cred.role}
                style={[
                  styles.roleOption,
                  {
                    backgroundColor: cred.role === role ? colors.primaryLight : colors.bgSubtle,
                    borderColor: cred.role === role ? colors.primary : colors.borderLight,
                  },
                ]}
                onPress={() => {
                  setShowRoleModal(false);
                  switchDemoRole(cred.role);
                }}
              >
                <View>
                  <Text style={[styles.roleOptionTitle, { color: colors.textMain }]}>
                    {cred.label} ({cred.role})
                  </Text>
                  <Text style={[styles.roleOptionSub, { color: colors.textSubtle }]}>
                    {cred.sublabel} • @{cred.username}
                  </Text>
                </View>
                {cred.role === role && <Text style={{ color: colors.primary, fontWeight: '700' }}>✓</Text>}
              </TouchableOpacity>
            ))}

            <TouchableOpacity
              style={[styles.modalCloseButton, { backgroundColor: colors.bgSubtle }]}
              onPress={() => setShowRoleModal(false)}
            >
              <Text style={{ color: colors.textMain, fontWeight: '600' }}>{t('cancel')}</Text>
            </TouchableOpacity>
          </View>
        </TouchableOpacity>
      </Modal>

      {/* Language Switcher Modal */}
      <Modal visible={showLangModal} transparent animationType="fade" onRequestClose={() => setShowLangModal(false)}>
        <TouchableOpacity style={styles.modalBackdrop} activeOpacity={1} onPress={() => setShowLangModal(false)}>
          <View style={[styles.modalCard, { backgroundColor: colors.bgSurface }]}>
            <Text style={[styles.modalTitle, { color: colors.textMain }]}>{t('language')}</Text>

            {[
              { code: 'en' as SupportedLanguage, label: 'English' },
              { code: 'hi' as SupportedLanguage, label: 'हिन्दी (Hindi)' },
              { code: 'mr' as SupportedLanguage, label: 'मराठी (Marathi)' },
            ].map((item) => (
              <TouchableOpacity
                key={item.code}
                style={[
                  styles.roleOption,
                  {
                    backgroundColor: language === item.code ? colors.primaryLight : colors.bgSubtle,
                    borderColor: language === item.code ? colors.primary : colors.borderLight,
                  },
                ]}
                onPress={() => {
                  setLanguage(item.code);
                  setShowLangModal(false);
                }}
              >
                <Text style={[styles.roleOptionTitle, { color: colors.textMain }]}>{item.label}</Text>
                {language === item.code && <Text style={{ color: colors.primary, fontWeight: '700' }}>✓</Text>}
              </TouchableOpacity>
            ))}
          </View>
        </TouchableOpacity>
      </Modal>

      {/* Emergency & Location Settings Modal */}
      <Modal visible={showSettingsModal} transparent animationType="fade" onRequestClose={() => setShowSettingsModal(false)}>
        <TouchableOpacity style={styles.modalBackdrop} activeOpacity={1} onPress={() => setShowSettingsModal(false)}>
          <View style={[styles.modalCard, { backgroundColor: colors.bgSurface }]}>
            <View style={{ flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between', marginBottom: 12 }}>
              <Text style={[styles.modalTitle, { color: colors.textMain }]}>Settings</Text>
              <TouchableOpacity onPress={() => setShowSettingsModal(false)}>
                <Ionicons name="close" size={22} color={colors.textSubtle} />
              </TouchableOpacity>
            </View>

            <View style={[styles.settingRow, { borderBottomColor: colors.borderLight }]}>
              <View style={{ flex: 1, paddingRight: 10 }}>
                <Text style={[styles.settingLabel, { color: colors.textMain }]}>
                  Share my location for emergency alerts
                </Text>
                <Text style={[styles.settingSub, { color: colors.textSubtle }]}>
                  When active, your location is securely updated every 30s so you can be reached or help someone in an emergency within 5 km.
                </Text>
              </View>
              <Switch
                value={isSharingEnabled}
                onValueChange={(val) => setIsSharingEnabled(val)}
                trackColor={{ false: colors.bgSubtle, true: colors.primaryLight }}
                thumbColor={isSharingEnabled ? colors.primary : '#94A3B8'}
              />
            </View>

            <View style={{ marginTop: 14 }}>
              <Text style={{ fontSize: 12, color: colors.textSubtle }}>
                Status: {isSharingEnabled ? '● Location sharing active (Opted In)' : '○ Location sharing paused'}
              </Text>
            </View>
          </View>
        </TouchableOpacity>
      </Modal>
    </View>
  );
};

const styles = StyleSheet.create({
  headerContainer: {
    paddingHorizontal: 16,
    paddingTop: 8,
    paddingBottom: 12,
    borderBottomWidth: 1,
  },
  brandRow: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
  },
  brandTitle: {
    fontSize: 18,
    fontWeight: '800',
    letterSpacing: -0.3,
  },
  brandSubtitle: {
    fontSize: 12,
    marginTop: 2,
    maxWidth: 160,
  },
  controlsRow: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 6,
  },
  chipButton: {
    minWidth: 36,
    height: 36,
    borderRadius: 8,
    borderWidth: 1,
    alignItems: 'center',
    justifyContent: 'center',
    paddingHorizontal: 6,
  },
  chipText: {
    fontSize: 12,
    fontWeight: '700',
  },
  iconText: {
    fontSize: 14,
  },
  roleBadge: {
    paddingHorizontal: 10,
    height: 36,
    borderRadius: 8,
    borderWidth: 1,
    justifyContent: 'center',
    alignItems: 'center',
  },
  roleBadgeText: {
    fontSize: 12,
    fontWeight: '700',
  },
  modalBackdrop: {
    flex: 1,
    backgroundColor: 'rgba(0,0,0,0.5)',
    justifyContent: 'center',
    alignItems: 'center',
    padding: 20,
  },
  modalCard: {
    width: '100%',
    maxWidth: 360,
    borderRadius: 16,
    padding: 20,
    elevation: 8,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 4 },
    shadowOpacity: 0.25,
    shadowRadius: 10,
  },
  modalTitle: {
    fontSize: 18,
    fontWeight: '700',
    marginBottom: 4,
  },
  modalSubtitle: {
    fontSize: 13,
    marginBottom: 16,
  },
  roleOption: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    padding: 12,
    borderRadius: 10,
    borderWidth: 1,
    marginBottom: 8,
  },
  roleOptionTitle: {
    fontSize: 14,
    fontWeight: '600',
  },
  roleOptionSub: {
    fontSize: 12,
    marginTop: 2,
  },
  modalCloseButton: {
    marginTop: 10,
    padding: 12,
    borderRadius: 8,
    alignItems: 'center',
  },
  settingRow: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    paddingVertical: 14,
    borderBottomWidth: StyleSheet.hairlineWidth,
  },
  settingLabel: {
    fontSize: 14,
    fontWeight: '700',
  },
  settingSub: {
    fontSize: 12,
    marginTop: 4,
    lineHeight: 16,
  },
});
