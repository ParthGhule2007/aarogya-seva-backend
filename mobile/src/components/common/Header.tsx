import React, { useState } from 'react';
import { View, Text, StyleSheet, TouchableOpacity, Modal, Switch } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { StatusBar } from 'expo-status-bar';
import { Ionicons } from '@expo/vector-icons';
import { useNavigation } from '@react-navigation/native';
import { useAuth, DEMO_CREDENTIALS } from '../../context/AuthContext';
import { useTheme } from '../../context/ThemeContext';
import { useLanguage, SupportedLanguage } from '../../i18n/LanguageContext';
import { useLocationSharing } from '../../context/LocationSharingContext';
import { UserRole } from '../../types/auth';
import { OmniSearchModal } from './OmniSearchModal';
import { EmergencySosModal } from './EmergencySosModal';

export const Header: React.FC = () => {
  const insets = useSafeAreaInsets();
  const navigation = useNavigation<any>();
  const { user, role, switchDemoRole, logout } = useAuth();
  const { colors, isElderlyMode, toggleElderlyMode, fontScale } = useTheme();
  const { language, setLanguage, t } = useLanguage();
  const { isSharingEnabled, setIsSharingEnabled } = useLocationSharing();

  const [showRoleModal, setShowRoleModal] = useState(false);
  const [showLangModal, setShowLangModal] = useState(false);
  const [showSettingsModal, setShowSettingsModal] = useState(false);
  const [showSearchModal, setShowSearchModal] = useState(false);
  const [showSosModal, setShowSosModal] = useState(false);

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
        <View style={{ flex: 1 }}>
          <View style={{ flexDirection: 'row', alignItems: 'center', gap: 6 }}>
            <Ionicons name="medical" size={20} color={colors.primary} />
            <Text style={[styles.brandTitle, { color: colors.primary, fontSize: (isElderlyMode ? 20 : 17) * fontScale }]}>
              {t('brandName')}
            </Text>
          </View>
          <Text style={[styles.brandSubtitle, { color: colors.textSubtle, fontSize: (isElderlyMode ? 13 : 11) * fontScale }]} numberOfLines={1}>
            {user?.displayName || t('brandSubtitle')}
          </Text>
        </View>

        {/* Action controls */}
        <View style={styles.controlsRow}>
          {/* Senior Mode Toggle Button */}
          <TouchableOpacity
            style={[
              styles.seniorToggleBtn,
              {
                backgroundColor: isElderlyMode ? '#FEF3C7' : colors.bgSubtle,
                borderColor: isElderlyMode ? '#D97706' : colors.borderMedium,
              },
            ]}
            onPress={toggleElderlyMode}
            activeOpacity={0.7}
            accessibilityLabel="Toggle Elderly Mode"
          >
            <Text style={{ fontSize: 13 }}>👵</Text>
            <Text style={[styles.seniorToggleText, { color: isElderlyMode ? '#92400E' : colors.textMuted }]}>
              {isElderlyMode ? 'वृद्ध Mode ON' : 'वृद्ध'}
            </Text>
          </TouchableOpacity>

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

      {/* Prominent Universal OmniSearch Trigger Bar */}
      <TouchableOpacity
        onPress={() => setShowSearchModal(true)}
        activeOpacity={0.8}
        style={[
          styles.omniSearchBar,
          {
            backgroundColor: colors.bgSubtle,
            borderColor: colors.borderMedium,
            height: isElderlyMode ? 52 : 44,
          },
        ]}
      >
        <Ionicons name="search" size={isElderlyMode ? 22 : 18} color={colors.primary} style={{ marginRight: 8 }} />
        <Text style={[styles.omniSearchPlaceholder, { color: colors.textSubtle, fontSize: (isElderlyMode ? 15 : 13) * fontScale }]}>
          {language === 'hi'
            ? 'यहाँ खोजें: डॉक्टर, दवाइयाँ, 108 SOS, अस्पताल...'
            : language === 'mr'
            ? 'येथे शोधा: डॉक्टर, औषधे, 108 SOS, रुग्णालय...'
            : 'Search anything: Doctors, medicines, 108 SOS, PHCs...'}
        </Text>
        <View style={[styles.micIndicator, { backgroundColor: colors.primaryLight }]}>
          <Ionicons name="mic" size={15} color={colors.primary} />
        </View>
      </TouchableOpacity>

      {/* Universal OmniSearch Modal */}
      <OmniSearchModal
        visible={showSearchModal}
        onClose={() => setShowSearchModal(false)}
        navigation={navigation}
        onOpenSos={() => setShowSosModal(true)}
        onOpenTriage={() => navigation.navigate('Triage' as any)}
        onOpenRecords={() => navigation.navigate('Records' as any)}
      />

      {/* Emergency SOS Modal */}
      <EmergencySosModal visible={showSosModal} onClose={() => setShowSosModal(false)} />

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
                    borderColor: role === cred.role ? colors.primary : colors.borderLight,
                    backgroundColor: role === cred.role ? colors.primaryLight : colors.bgApp,
                  },
                ]}
                onPress={() => {
                  switchDemoRole(cred.role);
                  setShowRoleModal(false);
                }}
              >
                <View>
                  <Text style={[styles.roleOptionTitle, { color: colors.textMain }]}>
                    {getRoleLabel(cred.role)}
                  </Text>
                  <Text style={[styles.roleOptionSub, { color: colors.textSubtle }]}>
                    {cred.label} ({cred.sublabel})
                  </Text>
                </View>
                {role === cred.role && (
                  <Ionicons name="checkmark-circle" size={20} color={colors.primary} />
                )}
              </TouchableOpacity>
            ))}

            <TouchableOpacity
              style={[styles.modalCloseButton, { backgroundColor: colors.bgSubtle }]}
              onPress={() => setShowRoleModal(false)}
            >
              <Text style={{ color: colors.textMain, fontWeight: '600' }}>Cancel</Text>
            </TouchableOpacity>
          </View>
        </TouchableOpacity>
      </Modal>

      {/* Language Switcher Modal */}
      <Modal visible={showLangModal} transparent animationType="fade" onRequestClose={() => setShowLangModal(false)}>
        <TouchableOpacity style={styles.modalBackdrop} activeOpacity={1} onPress={() => setShowLangModal(false)}>
          <View style={[styles.modalCard, { backgroundColor: colors.bgSurface }]}>
            <Text style={[styles.modalTitle, { color: colors.textMain }]}>Choose Language / भाषा चुनें</Text>
            <Text style={[styles.modalSubtitle, { color: colors.textSubtle }]}>
              Select your preferred Indic regional language:
            </Text>

            {[
              { code: 'hi' as SupportedLanguage, label: 'हिन्दी (Hindi)' },
              { code: 'mr' as SupportedLanguage, label: 'मराठी (Marathi)' },
              { code: 'en' as SupportedLanguage, label: 'English' },
            ].map((l) => (
              <TouchableOpacity
                key={l.code}
                style={[
                  styles.roleOption,
                  {
                    borderColor: language === l.code ? colors.primary : colors.borderLight,
                    backgroundColor: language === l.code ? colors.primaryLight : colors.bgApp,
                  },
                ]}
                onPress={() => {
                  setLanguage(l.code);
                  setShowLangModal(false);
                }}
              >
                <Text style={[styles.roleOptionTitle, { color: colors.textMain }]}>{l.label}</Text>
                {language === l.code && (
                  <Ionicons name="checkmark-circle" size={20} color={colors.primary} />
                )}
              </TouchableOpacity>
            ))}

            <TouchableOpacity
              style={[styles.modalCloseButton, { backgroundColor: colors.bgSubtle }]}
              onPress={() => setShowLangModal(false)}
            >
              <Text style={{ color: colors.textMain, fontWeight: '600' }}>Close</Text>
            </TouchableOpacity>
          </View>
        </TouchableOpacity>
      </Modal>

      {/* Settings Modal */}
      <Modal visible={showSettingsModal} transparent animationType="fade" onRequestClose={() => setShowSettingsModal(false)}>
        <TouchableOpacity style={styles.modalBackdrop} activeOpacity={1} onPress={() => setShowSettingsModal(false)}>
          <View style={[styles.modalCard, { backgroundColor: colors.bgSurface }]}>
            <Text style={[styles.modalTitle, { color: colors.textMain }]}>Settings & Preferences</Text>
            
            {/* Elderly mode switch in settings */}
            <View style={[styles.settingRow, { borderColor: colors.borderLight }]}>
              <View style={{ flex: 1, paddingRight: 10 }}>
                <Text style={[styles.settingLabel, { color: colors.textMain }]}>
                  👵 Senior Citizen / Elderly Mode
                </Text>
                <Text style={[styles.settingSub, { color: colors.textSubtle }]}>
                  Giant high-contrast buttons, large text, and voice audio readouts
                </Text>
              </View>
              <Switch
                value={isElderlyMode}
                onValueChange={toggleElderlyMode}
                trackColor={{ false: colors.borderMedium, true: colors.primary }}
                thumbColor="#FFFFFF"
              />
            </View>

            {/* Location Sharing */}
            <View style={[styles.settingRow, { borderColor: colors.borderLight }]}>
              <View style={{ flex: 1, paddingRight: 10 }}>
                <Text style={[styles.settingLabel, { color: colors.textMain }]}>
                  5km SOS Geo-Fencing
                </Text>
                <Text style={[styles.settingSub, { color: colors.textSubtle }]}>
                  Share live GPS with nearby community ASHA workers and ambulances
                </Text>
              </View>
              <Switch
                value={isSharingEnabled}
                onValueChange={setIsSharingEnabled}
                trackColor={{ false: colors.borderMedium, true: colors.primary }}
                thumbColor="#FFFFFF"
              />
            </View>

            <TouchableOpacity
              style={[styles.modalCloseButton, { backgroundColor: colors.bgSubtle, marginTop: 14 }]}
              onPress={() => setShowSettingsModal(false)}
            >
              <Text style={{ color: colors.textMain, fontWeight: '600' }}>Done</Text>
            </TouchableOpacity>
          </View>
        </TouchableOpacity>
      </Modal>
    </View>
  );
};

const styles = StyleSheet.create({
  headerContainer: {
    paddingHorizontal: 16,
    paddingBottom: 10,
    borderBottomWidth: 1,
    gap: 10,
  },
  brandRow: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
  },
  brandTitle: {
    fontWeight: '800',
    letterSpacing: -0.2,
  },
  brandSubtitle: {
    marginTop: 1,
    fontWeight: '500',
  },
  controlsRow: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 6,
  },
  chipButton: {
    height: 32,
    paddingHorizontal: 8,
    borderRadius: 8,
    borderWidth: 1,
    alignItems: 'center',
    justifyContent: 'center',
  },
  chipText: {
    fontSize: 11,
    fontWeight: '700',
  },
  seniorToggleBtn: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 3,
    height: 32,
    paddingHorizontal: 8,
    borderRadius: 8,
    borderWidth: 1,
  },
  seniorToggleText: {
    fontSize: 11,
    fontWeight: '800',
  },
  roleBadge: {
    height: 32,
    paddingHorizontal: 10,
    borderRadius: 8,
    borderWidth: 1,
    alignItems: 'center',
    justifyContent: 'center',
  },
  roleBadgeText: {
    fontSize: 12,
    fontWeight: '700',
  },
  omniSearchBar: {
    flexDirection: 'row',
    alignItems: 'center',
    borderRadius: 14,
    borderWidth: 1.5,
    paddingHorizontal: 12,
  },
  omniSearchPlaceholder: {
    flex: 1,
    fontWeight: '500',
  },
  micIndicator: {
    width: 26,
    height: 26,
    borderRadius: 13,
    alignItems: 'center',
    justifyContent: 'center',
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
