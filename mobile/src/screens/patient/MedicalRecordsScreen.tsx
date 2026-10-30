import React, { useState } from 'react';
import {
  View,
  Text,
  StyleSheet,
  ScrollView,
  TouchableOpacity,
  Share,
  Alert,
} from 'react-native';
import { Ionicons } from '@expo/vector-icons';
import { useTheme } from '../../context/ThemeContext';
import { useLanguage } from '../../i18n/LanguageContext';
import { useAuth } from '../../context/AuthContext';
import { Card } from '../../components/common/Card';
import { StatusBadge } from '../../components/common/StatusBadge';
import { Header } from '../../components/common/Header';

interface VitalItem {
  name: string;
  indicName: string;
  value: string;
  unit: string;
  status: 'NORMAL' | 'ELEVATED' | 'OPTIMAL';
  icon: keyof typeof Ionicons.glyphMap;
  color: string;
  recordedAt: string;
}

export const MedicalRecordsScreen: React.FC<{ navigation: any }> = ({ navigation }) => {
  const { colors, isElderlyMode, fontScale, touchTargetMin } = useTheme();
  const { language, t } = useLanguage();
  const { user } = useAuth();

  const [activeTab, setActiveTab] = useState<'VITALS' | 'ABHA' | 'PRESCRIPTIONS'>('VITALS');

  const vitals: VitalItem[] = [
    {
      name: 'Blood Pressure',
      indicName: 'रक्तचाप (BP)',
      value: '122 / 80',
      unit: 'mmHg',
      status: 'OPTIMAL',
      icon: 'heart',
      color: '#059669',
      recordedAt: 'Today, 08:30 AM',
    },
    {
      name: 'Pulse / Heart Rate',
      indicName: 'हृदय गति (Heart Rate)',
      value: '72',
      unit: 'BPM',
      status: 'NORMAL',
      icon: 'pulse',
      color: '#0284C7',
      recordedAt: 'Today, 08:30 AM',
    },
    {
      name: 'Blood Oxygen (SpO2)',
      indicName: 'ऑक्सीजन स्तर',
      value: '98',
      unit: '%',
      status: 'OPTIMAL',
      icon: 'water',
      color: '#0D9488',
      recordedAt: 'Yesterday, 07:15 PM',
    },
    {
      name: 'Blood Glucose (Fasting)',
      indicName: 'शुगर स्तर (उपवास)',
      value: '96',
      unit: 'mg/dL',
      status: 'NORMAL',
      icon: 'fitness',
      color: '#D97706',
      recordedAt: '28 Oct 2026',
    },
    {
      name: 'Body Temperature',
      indicName: 'शरीर का तापमान',
      value: '98.4',
      unit: '°F',
      status: 'NORMAL',
      icon: 'thermometer',
      color: '#7C3AED',
      recordedAt: 'Today, 08:30 AM',
    },
  ];

  const pastPrescriptions = [
    {
      id: 'rx-1',
      doctorName: 'Dr. Rajesh Sharma (MBBS, MD)',
      date: '26 Oct 2026',
      diagnosis: 'Acute Seasonal Bronchitis (मौसमी ब्रोंकाइटिस)',
      medicines: [
        { name: 'Amoxicillin 500mg', dose: '1 capsule • 3 times daily • After food', duration: '5 days' },
        { name: 'Paracetamol 650mg', dose: '1 tablet • As needed for fever', duration: '3 days' },
        { name: 'Cetirizine 10mg', dose: '1 tablet at bedtime', duration: '5 days' },
      ],
    },
    {
      id: 'rx-2',
      doctorName: 'Dr. Anjali Deshmukh (Pediatrics)',
      date: '14 Oct 2026',
      diagnosis: 'Mild Gastroenteritis (पेट संक्रमण)',
      medicines: [
        { name: 'ORS Electrolyte Solution', dose: '1 liter throughout day', duration: '3 days' },
        { name: 'Zinc Sulphate 20mg', dose: '1 tablet once daily', duration: '14 days' },
      ],
    },
  ];

  const handleSharePass = () => {
    Share.share({
      message: `Aarogya Seva Digital Health Pass\nPatient: ${user?.displayName || 'Ramesh Patil'}\nABHA Number: 91-4521-8890-1234\nLatest BP: 122/80 mmHg | SpO2: 98%`,
      title: 'Aarogya Seva Health Card',
    });
  };

  return (
    <View style={[styles.screen, { backgroundColor: colors.bgApp }]}>
      <Header />
      <ScrollView contentContainerStyle={styles.scrollContent}>
        {/* Navigation Switcher Pills */}
        <View style={styles.tabPillsRow}>
          {[
            { id: 'VITALS', label: '📊 Vitals (वाइटल्स)', icon: 'speedometer' },
            { id: 'ABHA', label: '🆔 ABHA Card (स्वास्थ्य पत्र)', icon: 'card' },
            { id: 'PRESCRIPTIONS', label: '📋 Prescriptions (पर्चे)', icon: 'document-text' },
          ].map((tab) => {
            const isSel = activeTab === tab.id;
            return (
              <TouchableOpacity
                key={tab.id}
                onPress={() => setActiveTab(tab.id as any)}
                style={[
                  styles.tabPill,
                  {
                    backgroundColor: isSel ? colors.primary : colors.bgSurface,
                    borderColor: isSel ? colors.primary : colors.borderLight,
                    minHeight: touchTargetMin,
                  },
                ]}
              >
                <Text
                  style={[
                    styles.tabPillText,
                    {
                      color: isSel ? '#FFFFFF' : colors.textMain,
                      fontWeight: isSel ? '800' : '600',
                      fontSize: (isElderlyMode ? 14 : 12) * fontScale,
                    },
                  ]}
                >
                  {tab.label}
                </Text>
              </TouchableOpacity>
            );
          })}
        </View>

        {/* TAB 1: VITALS */}
        {activeTab === 'VITALS' && (
          <View style={styles.sectionGap}>
            <View style={styles.sectionHeader}>
              <Text style={[styles.sectionTitle, { color: colors.textMain, fontSize: 16 * fontScale }]}>
                {language === 'hi' ? 'दैनिक स्वास्थ्य मापदंड (Daily Vitals)' : 'Monitored Health Vitals'}
              </Text>
              <TouchableOpacity
                onPress={() => Alert.alert('Sync Vitals', 'Vitals synced from PHC Health Kiosk')}
                style={[styles.syncBtn, { backgroundColor: colors.primaryLight }]}
              >
                <Ionicons name="sync" size={14} color={colors.primary} />
                <Text style={[styles.syncText, { color: colors.primary }]}>Sync</Text>
              </TouchableOpacity>
            </View>

            {vitals.map((v, i) => (
              <Card key={i} style={[styles.vitalCard, { borderColor: `${v.color}35`, minHeight: touchTargetMin }]}>
                <View style={[styles.vitalIconBox, { backgroundColor: `${v.color}15` }]}>
                  <Ionicons name={v.icon} size={24} color={v.color} />
                </View>
                <View style={styles.vitalContent}>
                  <View style={styles.vitalRowTop}>
                    <Text
                      style={[styles.vitalName, { color: colors.textMain, fontSize: (isElderlyMode ? 16 : 14) * fontScale }]}
                    >
                      {v.indicName}
                    </Text>
                    <View style={[styles.statusBadge, { backgroundColor: '#DCFCE7' }]}>
                      <Text style={[styles.statusBadgeText, { color: '#15803D' }]}>{v.status}</Text>
                    </View>
                  </View>
                  <View style={styles.vitalValRow}>
                    <Text style={[styles.vitalVal, { color: colors.textMain, fontSize: (isElderlyMode ? 26 : 22) * fontScale }]}>
                      {v.value}
                    </Text>
                    <Text style={[styles.vitalUnit, { color: colors.textSubtle }]}>{v.unit}</Text>
                  </View>
                  <Text style={[styles.vitalTime, { color: colors.textSubtle }]}>Recorded: {v.recordedAt}</Text>
                </View>
              </Card>
            ))}
          </View>
        )}

        {/* TAB 2: ABHA CARD */}
        {activeTab === 'ABHA' && (
          <View style={styles.sectionGap}>
            <View style={[styles.abhaCard, { backgroundColor: '#07162C', borderColor: colors.primary }]}>
              {/* Card Header */}
              <View style={styles.abhaHeader}>
                <View>
                  <Text style={styles.abhaGovt}>NATIONAL HEALTH AUTHORITY • GOVERNMENT OF INDIA</Text>
                  <Text style={styles.abhaTitle}>ABHA Digital Health Card (आयुष्मान भारत)</Text>
                </View>
                <Ionicons name="shield-checkmark" size={28} color="#00E5B9" />
              </View>

              {/* Photo & Patient Details */}
              <View style={styles.abhaBody}>
                <View style={styles.abhaPhotoPlaceholder}>
                  <Ionicons name="person" size={44} color="#94A3B8" />
                </View>
                <View style={styles.abhaDetails}>
                  <Text style={styles.abhaName}>{user?.displayName || 'Ramesh D. Patil'}</Text>
                  <Text style={styles.abhaSub}>DOB: 12-05-1978 • Male</Text>
                  <Text style={styles.abhaNumber}>ABHA No: 91-4521-8890-1234</Text>
                  <Text style={styles.abhaAddress}>Dist: Pune, Maharashtra • Pin: 411038</Text>
                </View>
              </View>

              {/* QR Code Bar */}
              <View style={styles.abhaFooter}>
                <View style={styles.qrSimulation}>
                  <Ionicons name="qr-code" size={44} color="#FFFFFF" />
                  <Text style={styles.qrText}>Scan for Instant Emergency EHR Access</Text>
                </View>
                <TouchableOpacity onPress={handleSharePass} style={styles.shareBtn}>
                  <Ionicons name="share-social" size={16} color="#00E5B9" />
                  <Text style={styles.shareBtnText}>Share Pass</Text>
                </TouchableOpacity>
              </View>
            </View>
          </View>
        )}

        {/* TAB 3: PRESCRIPTIONS */}
        {activeTab === 'PRESCRIPTIONS' && (
          <View style={styles.sectionGap}>
            <Text style={[styles.sectionTitle, { color: colors.textMain, fontSize: 16 * fontScale }]}>
              {language === 'hi' ? 'डॉक्टर के पर्चे व दवाइयाँ' : 'Doctor Prescriptions & Records'}
            </Text>

            {pastPrescriptions.map((rx) => (
              <Card key={rx.id} style={styles.rxCard}>
                <View style={styles.rxHeader}>
                  <View style={{ flex: 1 }}>
                    <Text style={[styles.rxDoc, { color: colors.textMain }]}>{rx.doctorName}</Text>
                    <Text style={[styles.rxDiagnosis, { color: colors.primary }]}>{rx.diagnosis}</Text>
                  </View>
                  <Text style={[styles.rxDate, { color: colors.textSubtle }]}>{rx.date}</Text>
                </View>

                <View style={styles.rxMedsList}>
                  {rx.medicines.map((m, idx) => (
                    <View key={idx} style={[styles.rxMedItem, { borderColor: colors.borderLight }]}>
                      <Ionicons name="medkit" size={16} color="#059669" />
                      <View style={{ flex: 1 }}>
                        <Text style={[styles.rxMedName, { color: colors.textMain }]}>{m.name}</Text>
                        <Text style={[styles.rxMedDose, { color: colors.textMuted }]}>{m.dose}</Text>
                      </View>
                      <View style={styles.rxDurationBadge}>
                        <Text style={styles.rxDurationText}>{m.duration}</Text>
                      </View>
                    </View>
                  ))}
                </View>

                <TouchableOpacity
                  onPress={() => navigation.navigate('Dashboard')}
                  style={[styles.setReminderBtn, { backgroundColor: colors.primaryLight }]}
                >
                  <Ionicons name="notifications" size={16} color={colors.primary} />
                  <Text style={[styles.setReminderText, { color: colors.primary }]}>
                    View Active Reminders in Medicine Tracker
                  </Text>
                </TouchableOpacity>
              </Card>
            ))}
          </View>
        )}
      </ScrollView>
    </View>
  );
};

const styles = StyleSheet.create({
  screen: {
    flex: 1,
  },
  scrollContent: {
    padding: 16,
    paddingBottom: 40,
    gap: 14,
  },
  tabPillsRow: {
    flexDirection: 'row',
    gap: 6,
    flexWrap: 'wrap',
  },
  tabPill: {
    flex: 1,
    paddingVertical: 10,
    paddingHorizontal: 12,
    borderRadius: 12,
    borderWidth: 1.5,
    alignItems: 'center',
    justifyContent: 'center',
  },
  tabPillText: {
    letterSpacing: 0.2,
  },
  sectionGap: {
    gap: 12,
  },
  sectionHeader: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
  },
  sectionTitle: {
    fontWeight: '800',
  },
  syncBtn: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 4,
    paddingHorizontal: 10,
    paddingVertical: 4,
    borderRadius: 99,
  },
  syncText: {
    fontSize: 12,
    fontWeight: '700',
  },
  vitalCard: {
    flexDirection: 'row',
    alignItems: 'center',
    padding: 14,
    borderRadius: 16,
    borderWidth: 1.5,
    gap: 14,
  },
  vitalIconBox: {
    width: 48,
    height: 48,
    borderRadius: 14,
    alignItems: 'center',
    justifyContent: 'center',
  },
  vitalContent: {
    flex: 1,
  },
  vitalRowTop: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
  },
  vitalName: {
    fontWeight: '700',
  },
  statusBadge: {
    paddingHorizontal: 8,
    paddingVertical: 2,
    borderRadius: 6,
  },
  statusBadgeText: {
    fontSize: 10,
    fontWeight: '800',
  },
  vitalValRow: {
    flexDirection: 'row',
    alignItems: 'baseline',
    gap: 6,
    marginVertical: 2,
  },
  vitalVal: {
    fontWeight: '900',
  },
  vitalUnit: {
    fontSize: 13,
    fontWeight: '600',
  },
  vitalTime: {
    fontSize: 11,
  },
  abhaCard: {
    borderRadius: 20,
    borderWidth: 2,
    padding: 16,
    gap: 14,
    shadowColor: '#00E5B9',
    shadowOffset: { width: 0, height: 4 },
    shadowOpacity: 0.2,
    shadowRadius: 10,
    elevation: 8,
  },
  abhaHeader: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    borderBottomWidth: 1,
    borderBottomColor: 'rgba(255,255,255,0.15)',
    paddingBottom: 8,
  },
  abhaGovt: {
    color: '#94A3B8',
    fontSize: 9,
    fontWeight: '700',
    letterSpacing: 0.8,
  },
  abhaTitle: {
    color: '#00E5B9',
    fontSize: 14,
    fontWeight: '800',
    marginTop: 2,
  },
  abhaBody: {
    flexDirection: 'row',
    gap: 14,
    alignItems: 'center',
  },
  abhaPhotoPlaceholder: {
    width: 72,
    height: 84,
    borderRadius: 10,
    backgroundColor: 'rgba(255,255,255,0.08)',
    borderWidth: 1,
    borderColor: 'rgba(255,255,255,0.2)',
    alignItems: 'center',
    justifyContent: 'center',
  },
  abhaDetails: {
    flex: 1,
    gap: 2,
  },
  abhaName: {
    color: '#FFFFFF',
    fontSize: 16,
    fontWeight: '800',
  },
  abhaSub: {
    color: '#94A3B8',
    fontSize: 11,
  },
  abhaNumber: {
    color: '#38BDF8',
    fontSize: 13,
    fontWeight: '800',
    marginTop: 4,
    letterSpacing: 0.5,
  },
  abhaAddress: {
    color: '#64748B',
    fontSize: 10,
    marginTop: 2,
  },
  abhaFooter: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    borderTopWidth: 1,
    borderTopColor: 'rgba(255,255,255,0.15)',
    paddingTop: 10,
  },
  qrSimulation: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 10,
  },
  qrText: {
    color: '#94A3B8',
    fontSize: 10,
    maxWidth: 150,
  },
  shareBtn: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 6,
    paddingHorizontal: 12,
    paddingVertical: 6,
    borderRadius: 99,
    borderWidth: 1,
    borderColor: '#00E5B9',
  },
  shareBtnText: {
    color: '#00E5B9',
    fontSize: 11,
    fontWeight: '700',
  },
  rxCard: {
    padding: 14,
    gap: 12,
  },
  rxHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    borderBottomWidth: 1,
    borderBottomColor: '#F1F5F9',
    paddingBottom: 8,
  },
  rxDoc: {
    fontSize: 14,
    fontWeight: '700',
  },
  rxDiagnosis: {
    fontSize: 12,
    fontWeight: '600',
    marginTop: 2,
  },
  rxDate: {
    fontSize: 11,
  },
  rxMedsList: {
    gap: 8,
  },
  rxMedItem: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 10,
    padding: 8,
    borderRadius: 8,
    borderWidth: 1,
  },
  rxMedName: {
    fontSize: 13,
    fontWeight: '700',
  },
  rxMedDose: {
    fontSize: 11,
    marginTop: 1,
  },
  rxDurationBadge: {
    backgroundColor: '#F1F5F9',
    paddingHorizontal: 6,
    paddingVertical: 2,
    borderRadius: 4,
  },
  rxDurationText: {
    fontSize: 10,
    fontWeight: '700',
    color: '#475569',
  },
  setReminderBtn: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    gap: 6,
    paddingVertical: 8,
    borderRadius: 10,
  },
  setReminderText: {
    fontSize: 12,
    fontWeight: '700',
  },
});
