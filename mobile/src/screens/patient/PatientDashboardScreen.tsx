import React, { useState, useEffect, useCallback } from 'react';
import {
  View,
  Text,
  StyleSheet,
  ScrollView,
  RefreshControl,
  TouchableOpacity,
  Modal,
  TextInput,
  Alert,
  ActivityIndicator,
} from 'react-native';
import { Ionicons } from '@expo/vector-icons';
import * as ImagePicker from 'expo-image-picker';
import * as ImageManipulator from 'expo-image-manipulator';
import { useAuth } from '../../context/AuthContext';
import { useTheme } from '../../context/ThemeContext';
import { useLanguage } from '../../i18n/LanguageContext';
import { medicineApi } from '../../api/medicineApi';
import { ScheduledDose, TimeSlot, computeTimeSlot, formatDosage } from '../../types/medicine';
import { Header } from '../../components/common/Header';
import { Card } from '../../components/common/Card';
import { Button } from '../../components/common/Button';
import { StatusBadge } from '../../components/common/StatusBadge';
import { LoadingState } from '../../components/common/LoadingState';
import { EmptyState } from '../../components/common/EmptyState';
import { ErrorState } from '../../components/common/ErrorState';
import { EmergencySosButton } from '../../components/common/EmergencySosButton';
import { DemoModeBanner } from '../../components/common/DemoModeBanner';
import { NearbySosAlertsModal } from '../../components/sos/NearbySosAlertsModal';
import { IS_DEMO_MODE } from '../../config/apiConfig';

const FALLBACK_PATIENT_ID = 'b1000000-0000-0000-0000-000000000001';

interface PatientDashboardScreenProps {
  navigation: any;
}

export const PatientDashboardScreen: React.FC<PatientDashboardScreenProps> = ({ navigation }) => {
  const { user, selectedPatientId } = useAuth();
  const { colors } = useTheme();
  const { t } = useLanguage();

  const [doses, setDoses] = useState<ScheduledDose[]>([]);
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [actionInProgressId, setActionInProgressId] = useState<string | null>(null);
  const [usedDemoData, setUsedDemoData] = useState(false);

  // Add / Edit Medication Modal state
  const [isAddMedOpen, setIsAddMedOpen] = useState(false);
  const [editingMedicationId, setEditingMedicationId] = useState<string | null>(null);
  const [medName, setMedName] = useState('');
  const [medTimes, setMedTimes] = useState<TimeSlot[]>(['MORNING']);
  const [isAddingMed, setIsAddingMed] = useState(false);
  const [isScanningOcr, setIsScanningOcr] = useState(false);
  const [ocrNotice, setOcrNotice] = useState<string | null>(null);
  const [ocrDetails, setOcrDetails] = useState<{
    brand?: string;
    strength?: string;
    rawText?: string;
    confidence?: number;
    candidates?: string[];
  } | null>(null);
  const [isNearbySosOpen, setIsNearbySosOpen] = useState(false);

  const patientId = selectedPatientId || user?.patientId || user?.associatedEntityId || FALLBACK_PATIENT_ID;

  const fetchSchedule = useCallback(async () => {
    setError(null);
    setUsedDemoData(false);
    try {
      // Fetch both active patient medications (/api/v1/medications/patient/{patientId})
      // and today's scheduled dose events (/api/v1/medications/today/{patientId})
      const [todayDoses, meds] = await Promise.all([
        medicineApi.getTodaySchedule(patientId),
        medicineApi.getMedications(patientId),
      ]);

      if (Array.isArray(todayDoses) && todayDoses.length > 0) {
        const enriched = todayDoses.map((d: any) => ({
          ...d,
          eventId: d.eventId || d.id,
          timeSlot: d.timeSlot || computeTimeSlot(d.scheduledTime),
        }));
        setDoses(enriched);
      } else if (Array.isArray(meds) && meds.length > 0) {
        const generatedDoses: ScheduledDose[] = [];
        meds.forEach((m) => {
          (m.reminderTimes || ['08:00:00']).forEach((time, index) => {
            generatedDoses.push({
              id: `${m.id}-${index}`,
              eventId: m.id,
              medicationId: m.id,
              medicationName: m.name || m.medicineName || 'Prescribed Medicine',
              dosageAmount: m.dosageAmount,
              dosageUnit: m.dosageUnit,
              foodInstruction: m.foodInstruction,
              scheduledTime: time,
              timeSlot: computeTimeSlot(time),
              status: 'PENDING',
            });
          });
        });
        setDoses(generatedDoses);
      } else {
        setDoses([]);
      }
    } catch (err: any) {
      console.error('Failed to load medication schedule:', err);
      setError(err.message || 'Unable to load today’s medication schedule.');
      if (IS_DEMO_MODE) {
        setUsedDemoData(true);
        setDoses([
          {
            id: 'demo-dose-1',
            eventId: 'demo-dose-1',
            medicationId: 'm1',
            medicationName: 'Metformin 500mg',
            dosageAmount: 1,
            dosageUnit: 'TABLET',
            foodInstruction: 'AFTER_FOOD',
            scheduledTime: '08:00:00',
            timeSlot: 'MORNING',
            status: 'PENDING',
          },
        ]);
      } else {
        setDoses([]);
      }
    } finally {
      setLoading(false);
      setRefreshing(false);
    }
  }, [patientId]);

  useEffect(() => {
    fetchSchedule();
  }, [fetchSchedule]);

  const onRefresh = () => {
    setRefreshing(true);
    fetchSchedule();
  };

  const handleMarkTaken = async (dose: ScheduledDose) => {
    setActionInProgressId(dose.id);
    const previousStatus = dose.status;
    setDoses((prev) =>
      prev.map((d) => (d.id === dose.id ? { ...d, status: 'TAKEN' as const } : d))
    );

    try {
      await medicineApi.markTaken(dose.eventId || dose.id);
    } catch (err: any) {
      console.error('Mark taken failed:', err);
      setDoses((prev) =>
        prev.map((d) => (d.id === dose.id ? { ...d, status: previousStatus } : d))
      );
      Alert.alert('Error Marking Taken', err?.message || 'Failed to update dose status on server.');
    } finally {
      setActionInProgressId(null);
    }
  };

  const handleMarkMissed = async (dose: ScheduledDose) => {
    setActionInProgressId(dose.id);
    const previousStatus = dose.status;
    setDoses((prev) =>
      prev.map((d) => (d.id === dose.id ? { ...d, status: 'MISSED' as const } : d))
    );

    try {
      await medicineApi.markMissed(dose.eventId || dose.id);
      Alert.alert(
        'Dose Marked Missed',
        `${dose.medicationName} marked as MISSED. Consecutive missed doses automatically alert your Caregiver.`
      );
    } catch (err: any) {
      console.error('Mark missed failed:', err);
      setDoses((prev) =>
        prev.map((d) => (d.id === dose.id ? { ...d, status: previousStatus } : d))
      );
      Alert.alert('Error Marking Missed', err?.message || 'Failed to update dose status on server.');
    } finally {
      setActionInProgressId(null);
    }
  };

  const handleSeedDemoMedications = async () => {
    setLoading(true);
    const todayStr = new Date().toISOString().split('T')[0];
    try {
      await medicineApi.addMedication({
        patientId,
        name: 'Metformin 500mg',
        dosageAmount: 1,
        dosageUnit: 'TABLET',
        timesPerDay: 2,
        reminderTimes: ['08:00:00', '20:00:00'],
        startDate: todayStr,
        foodInstruction: 'AFTER_FOOD',
        instructions: 'Take after meals with water',
      });
      await medicineApi.addMedication({
        patientId,
        name: 'Amlodipine 5mg',
        dosageAmount: 1,
        dosageUnit: 'TABLET',
        timesPerDay: 1,
        reminderTimes: ['13:00:00'],
        startDate: todayStr,
        foodInstruction: 'BEFORE_FOOD',
        instructions: 'Daily blood pressure control',
      });
      await fetchSchedule();
    } catch (err: any) {
      Alert.alert('Notice', err.message || 'Could not seed sample prescriptions.');
      setLoading(false);
    }
  };

  const handleAddMedication = async () => {
    if (!medName.trim()) {
      Alert.alert('Required', 'Please enter a medicine name.');
      return;
    }
    setIsAddingMed(true);
    const slotMap: Record<TimeSlot, string> = {
      MORNING: '08:00:00',
      AFTERNOON: '13:00:00',
      EVENING: '18:00:00',
      NIGHT: '21:00:00',
    };
    const reminderTimes = medTimes.map((s) => slotMap[s]);
    try {
      if (editingMedicationId) {
        await medicineApi.updateMedication(editingMedicationId, {
          name: medName.trim(),
          reminderTimes,
          timesPerDay: reminderTimes.length,
        });
        Alert.alert('Success', 'Medicine reminder updated successfully.');
      } else {
        await medicineApi.addMedication({
          patientId,
          name: medName.trim(),
          dosageAmount: 1,
          dosageUnit: 'TABLET',
          timesPerDay: reminderTimes.length,
          reminderTimes,
          startDate: new Date().toISOString().split('T')[0],
          foodInstruction: 'AFTER_FOOD',
        });
        Alert.alert('Success', 'Medicine reminder added successfully.');
      }
      setIsAddMedOpen(false);
      setEditingMedicationId(null);
      setMedName('');
      setOcrNotice(null);
      setOcrDetails(null);
      await fetchSchedule();
    } catch (err: any) {
      Alert.alert('Error', err.message || 'Unable to save medicine reminder.');
    } finally {
      setIsAddingMed(false);
    }
  };

  const handleStartEdit = (dose: ScheduledDose) => {
    setEditingMedicationId(dose.medicationId);
    setMedName(dose.medicationName);
    setMedTimes([dose.timeSlot || 'MORNING']);
    setOcrNotice(null);
    setOcrDetails(null);
    setIsAddMedOpen(true);
  };

  const handleDeleteMedication = (dose: ScheduledDose) => {
    Alert.alert(
      t('medDeleteConfirmTitle'),
      `${t('medDeleteConfirmMsg')}\n\n• ${dose.medicationName}`,
      [
        { text: t('cancel'), style: 'cancel' },
        {
          text: t('medDelete'),
          style: 'destructive',
          onPress: async () => {
            // Optimistically update local state immediately
            setDoses((prev: ScheduledDose[]) => prev.filter((d) => d.medicationId !== dose.medicationId));
            try {
              await medicineApi.deleteMedication(dose.medicationId);
              await fetchSchedule();
              Alert.alert('Deleted', `${dose.medicationName} removed.`);
            } catch (err: any) {
              await fetchSchedule();
              Alert.alert('Delete Failed', err?.message || 'Could not delete medication.');
            }
          },
        },
      ]
    );
  };

  const prepareOcrImage = async (asset: ImagePicker.ImagePickerAsset): Promise<string> => {
    try {
      const actions: ImageManipulator.Action[] = [];
      if (asset.width && asset.width > 1600) {
        actions.push({ resize: { width: 1600 } });
      } else if (!asset.width) {
        actions.push({ resize: { width: 1600 } });
      }
      const manipulated = await ImageManipulator.manipulateAsync(
        asset.uri,
        actions,
        { compress: 0.8, format: ImageManipulator.SaveFormat.JPEG }
      );
      return manipulated.uri;
    } catch {
      return asset.uri;
    }
  };

  const handleScanMedicine = async () => {
    Alert.alert(
      t('medScanBtn'),
      'Select prescription or packaging image source for OCR recognition:',
      [
        {
          text: 'Camera',
          onPress: async () => {
            try {
              const { status } = await ImagePicker.requestCameraPermissionsAsync();
              if (status !== 'granted') {
                Alert.alert('Permission Denied', 'Camera permission is required.');
                return;
              }
              const res = await ImagePicker.launchCameraAsync({
                quality: 0.8,
                allowsEditing: false,
              });
              if (!res.canceled && res.assets && res.assets[0]) {
                const optimizedUri = await prepareOcrImage(res.assets[0]);
                await runOcr(optimizedUri);
              }
            } catch (err: any) {
              Alert.alert('Camera Error', err?.message || 'Could not open camera.');
            }
          },
        },
        {
          text: 'Gallery',
          onPress: async () => {
            try {
              const res = await ImagePicker.launchImageLibraryAsync({
                quality: 0.8,
                allowsEditing: false,
              });
              if (!res.canceled && res.assets && res.assets[0]) {
                const optimizedUri = await prepareOcrImage(res.assets[0]);
                await runOcr(optimizedUri);
              }
            } catch (err: any) {
              Alert.alert('Gallery Error', err?.message || 'Could not pick image.');
            }
          },
        },
        { text: t('cancel'), style: 'cancel' },
      ]
    );
  };

  const runOcr = async (uri: string) => {
    setIsScanningOcr(true);
    setOcrNotice(null);
    try {
      const resp = await medicineApi.extractOcr(uri);
      const detected = resp.detectedName || (resp.brand ? `${resp.brand} ${resp.strength || ''}`.trim() : resp.candidateNames?.[0] || '');
      if (detected) {
        setMedName(detected);
      }
      setOcrDetails({
        brand: resp.brand,
        strength: resp.strength,
        rawText: resp.rawExtractedText,
        confidence: resp.confidence,
        candidates: resp.candidateNames,
      });

      if (detected) {
        const confPct = Math.round((resp.confidence || 0.8) * 100);
        setOcrNotice(`Recognized: ${detected} (Confidence: ${confPct}%)`);
      } else if (resp.rawExtractedText) {
        setOcrNotice(resp.rawExtractedText.slice(0, 100));
      } else {
        setOcrNotice('No text detected. Please enter name manually.');
      }
    } catch (err: any) {
      Alert.alert('OCR Scanning Notice', err?.message || 'Could not extract text from image.');
    } finally {
      setIsScanningOcr(false);
    }
  };

  const toggleTimeSlot = (slot: TimeSlot) => {
    if (medTimes.includes(slot)) {
      if (medTimes.length > 1) setMedTimes(medTimes.filter((s) => s !== slot));
    } else {
      setMedTimes([...medTimes, slot]);
    }
  };

  const getTimeSlotTitle = (slot: TimeSlot): string => {
    switch (slot) {
      case 'MORNING': return t('medSlotMorning');
      case 'AFTERNOON': return t('medSlotAfternoon');
      case 'EVENING': return t('medSlotEvening');
      case 'NIGHT': return t('medSlotNight');
    }
  };

  const timeSlots: TimeSlot[] = ['MORNING', 'AFTERNOON', 'EVENING', 'NIGHT'];

  return (
    <View style={{ flex: 1, backgroundColor: colors.bgApp }}>
      <Header />

      <ScrollView
        contentContainerStyle={styles.container}
        refreshControl={
          <RefreshControl
            refreshing={refreshing}
            onRefresh={onRefresh}
            colors={[colors.primary]}
            tintColor={colors.primary}
          />
        }
      >
        <DemoModeBanner visible={usedDemoData} />

        {/* Welcome Banner */}
        <View style={styles.welcomeBanner}>
          <Text style={[styles.greetingText, { color: colors.textMain }]}>
            Namaste, {user?.displayName || 'Patient'}
          </Text>
          <Text style={[styles.subGreeting, { color: colors.textSubtle }]}>
            Rural Health Workstation • Patient ID: {patientId.slice(0, 8)}...
          </Text>
        </View>

        {/* Nearby Emergency SOS Banner */}
        <TouchableOpacity
          style={{
            backgroundColor: '#FEF2F2',
            borderWidth: 1.5,
            borderColor: '#F87171',
            borderRadius: 12,
            padding: 12,
            marginBottom: 16,
            flexDirection: 'row',
            alignItems: 'center',
            justifyContent: 'space-between',
          }}
          onPress={() => setIsNearbySosOpen(true)}
          activeOpacity={0.8}
        >
          <View style={{ flexDirection: 'row', alignItems: 'center', gap: 10, flex: 1 }}>
            <View
              style={{
                width: 36,
                height: 36,
                borderRadius: 8,
                backgroundColor: '#FEE2E2',
                alignItems: 'center',
                justifyContent: 'center',
              }}
            >
              <Ionicons name="warning-outline" size={20} color="#DC2626" />
            </View>
            <View style={{ flex: 1 }}>
              <Text style={{ fontSize: 13, fontWeight: '700', color: '#B91C1C' }}>
                Active Emergencies Around You
              </Text>
              <Text style={{ fontSize: 11, color: '#991B1B', marginTop: 1 }}>
                Tap to check genuine live SOS alerts triggered around your location
              </Text>
            </View>
          </View>
          <Ionicons name="chevron-forward" size={18} color="#DC2626" />
        </TouchableOpacity>

        {/* Quick Action Navigation Grid */}
        <View style={styles.actionGrid}>
          {/* Talk to AI */}
          <TouchableOpacity
            style={[styles.actionCard, { backgroundColor: colors.bgSurface, borderColor: colors.borderLight }]}
            onPress={() => navigation.navigate('TalkToAi')}
            activeOpacity={0.8}
          >
            <View style={[styles.actionIconBox, { backgroundColor: colors.primaryLight }]}>
              <Ionicons name="call" size={22} color={colors.primary} />
            </View>
            <Text style={[styles.actionTitle, { color: colors.textMain }]}>{t('quickTalkToAi')}</Text>
            <Text style={[styles.actionSubtitle, { color: colors.textSubtle }]}>
              {t('voiceCallSub')}
            </Text>
          </TouchableOpacity>

          {/* AI Chatbot */}
          <TouchableOpacity
            style={[styles.actionCard, { backgroundColor: colors.bgSurface, borderColor: colors.borderLight }]}
            onPress={() => navigation.navigate('Chatbot')}
            activeOpacity={0.8}
          >
            <View style={[styles.actionIconBox, { backgroundColor: '#EDE9FE' }]}>
              <Ionicons name="chatbubble-ellipses" size={22} color="#7C3AED" />
            </View>
            <Text style={[styles.actionTitle, { color: colors.textMain }]}>{t('quickAiChat')}</Text>
            <Text style={[styles.actionSubtitle, { color: colors.textSubtle }]}>
              {t('symptomBotSub')}
            </Text>
          </TouchableOpacity>

          {/* Doctor Consult */}
          <TouchableOpacity
            style={[styles.actionCard, { backgroundColor: colors.bgSurface, borderColor: colors.borderLight }]}
            onPress={() => navigation.navigate('Consultations')}
            activeOpacity={0.8}
          >
            <View style={[styles.actionIconBox, { backgroundColor: colors.secondaryLight }]}>
              <Ionicons name="videocam" size={22} color={colors.secondary} />
            </View>
            <Text style={[styles.actionTitle, { color: colors.textMain }]}>{t('quickConsult')}</Text>
            <Text style={[styles.actionSubtitle, { color: colors.textSubtle }]}>
              {t('videoDoctorSub')}
            </Text>
          </TouchableOpacity>

          {/* Nearby Facilities */}
          <TouchableOpacity
            style={[styles.actionCard, { backgroundColor: colors.bgSurface, borderColor: colors.borderLight }]}
            onPress={() => navigation.navigate('Facilities')}
            activeOpacity={0.8}
          >
            <View style={[styles.actionIconBox, { backgroundColor: colors.accentWarmLight }]}>
              <Ionicons name="business" size={22} color={colors.accentWarm} />
            </View>
            <Text style={[styles.actionTitle, { color: colors.textMain }]}>{t('quickHospitals')}</Text>
            <Text style={[styles.actionSubtitle, { color: colors.textSubtle }]}>
              {t('phcZonesSub')}
            </Text>
          </TouchableOpacity>
        </View>

        {/* Today's Medication Section */}
        <View style={styles.sectionHeaderRow}>
          <View style={{ flex: 1, paddingRight: 8 }}>
            <View style={{ flexDirection: 'row', alignItems: 'center', gap: 6 }}>
              <Ionicons name="medkit-outline" size={20} color={colors.primary} />
              <Text style={[styles.sectionTitle, { color: colors.textMain }]} numberOfLines={1}>
                {t('medTodaySchedule')}
              </Text>
            </View>
            <Text style={[styles.sectionSub, { color: colors.textSubtle }]} numberOfLines={1}>
              {doses.filter((d) => d.status === 'TAKEN').length} of {doses.length} doses taken
            </Text>
          </View>
          <Button
            title={t('medAddBtn')}
            onPress={() => {
              setEditingMedicationId(null);
              setMedName('');
              setOcrNotice(null);
              setIsAddMedOpen(true);
            }}
            variant="outline"
            style={styles.addMedBtn}
          />
        </View>

        {error ? (
          <ErrorState message={error} onRetry={fetchSchedule} />
        ) : null}

        {loading ? (
          <LoadingState message="Loading your daily prescriptions..." />
        ) : doses.length === 0 ? (
          <EmptyState
            icon="medical-outline"
            title="No Doses Scheduled for Today"
            description="No active medication reminders found for today. Tap below to load sample prescriptions or add a medicine."
            actionTitle="+ Load Sample Prescriptions"
            onAction={handleSeedDemoMedications}
          />
        ) : (
          timeSlots.map((slot) => {
            const slotDoses = doses.filter((d) => d.timeSlot === slot);
            if (slotDoses.length === 0) return null;

            return (
              <View key={slot} style={styles.slotBlock}>
                <Text style={[styles.slotTitle, { color: colors.primaryHover }]}>
                  {getTimeSlotTitle(slot)}
                </Text>

                {slotDoses.map((dose) => {
                  const isTaken = dose.status === 'TAKEN';
                  const isPendingAction = actionInProgressId === dose.id;

                  return (
                    <Card key={dose.id} style={styles.doseCard}>
                      <View style={styles.doseRow}>
                        <View style={{ flex: 1, paddingRight: 8 }}>
                          <Text style={[styles.doseName, { color: colors.textMain }]}>
                            {dose.medicationName}
                          </Text>
                          <Text style={[styles.doseDetails, { color: colors.textSubtle }]}>
                            {formatDosage(dose.dosageAmount, dose.dosageUnit)}
                            {dose.foodInstruction ? ` • ${dose.foodInstruction.replace('_', ' ')}` : ''}
                            {dose.scheduledTime ? ` • ${dose.scheduledTime.slice(0, 5)}` : ''}
                          </Text>
                        </View>

                        <View style={{ flexDirection: 'row', alignItems: 'center', gap: 6 }}>
                          <StatusBadge status={dose.status} />
                          {/* Edit Button */}
                          <TouchableOpacity
                            onPress={() => handleStartEdit(dose)}
                            style={{ padding: 4 }}
                            accessibilityLabel={t('medEdit')}
                          >
                            <Ionicons name="create-outline" size={18} color={colors.textSubtle} />
                          </TouchableOpacity>
                          {/* Delete Button */}
                          <TouchableOpacity
                            onPress={() => handleDeleteMedication(dose)}
                            style={{ padding: 4 }}
                            accessibilityLabel={t('medDelete')}
                          >
                            <Ionicons name="trash-outline" size={18} color="#EF4444" />
                          </TouchableOpacity>
                        </View>
                      </View>

                      {/* One-tap Take / Missed Buttons */}
                      {dose.status === 'PENDING' ? (
                        <View style={styles.doseActionRow}>
                          <Button
                            title={t('markMissed')}
                            onPress={() => handleMarkMissed(dose)}
                            disabled={isPendingAction}
                            variant="outline"
                            style={[styles.takeBtn, { marginRight: 8, borderColor: '#FCA5A5' }]}
                          />
                          <Button
                            title={isPendingAction ? t('saving') : t('markTaken')}
                            onPress={() => handleMarkTaken(dose)}
                            loading={isPendingAction}
                            variant="primary"
                            style={styles.takeBtn}
                          />
                        </View>
                      ) : isTaken ? (
                        <View style={styles.takenNoticeRow}>
                          <Text style={[styles.takenNotice, { color: colors.riskLow }]}>
                            {t('completedOnSchedule')}
                          </Text>
                        </View>
                      ) : (
                        <View style={styles.takenNoticeRow}>
                          <Text style={[styles.takenNotice, { color: '#DC2626' }]}>
                            {t('doseRecordedAs')} {dose.status}
                          </Text>
                        </View>
                      )}
                    </Card>
                  );
                })}
              </View>
            );
          })
        )}
      </ScrollView>

      {/* Add / Edit Medicine Modal */}
      <Modal visible={isAddMedOpen} transparent animationType="slide" onRequestClose={() => setIsAddMedOpen(false)}>
        <View style={styles.modalBackdrop}>
          <View style={[styles.modalCard, { backgroundColor: colors.bgSurface }]}>
            <View style={styles.modalHeader}>
              <Text style={[styles.modalTitle, { color: colors.textMain }]}>
                {editingMedicationId ? t('medEditTitle') : t('medAddTitle')}
              </Text>
              <TouchableOpacity onPress={() => setIsAddMedOpen(false)}>
                <Ionicons name="close" size={22} color={colors.textSubtle} />
              </TouchableOpacity>
            </View>

            {/* OCR Scan Button */}
            <TouchableOpacity
              style={[
                styles.ocrScanButton,
                { backgroundColor: colors.primaryLight, borderColor: colors.primary },
              ]}
              onPress={handleScanMedicine}
              disabled={isScanningOcr}
            >
              {isScanningOcr ? (
                <View style={{ flexDirection: 'row', alignItems: 'center', gap: 8 }}>
                  <ActivityIndicator size="small" color={colors.primary} />
                  <Text style={{ fontSize: 13, fontWeight: '700', color: colors.primary }}>
                    {t('medOcrAnalyzing')}
                  </Text>
                </View>
              ) : (
                <View style={{ flexDirection: 'row', alignItems: 'center', gap: 8 }}>
                  <Ionicons name="camera-outline" size={18} color={colors.primary} />
                  <Text style={{ fontSize: 13, fontWeight: '700', color: colors.primary }}>
                    {t('medScanBtn')} (OCR)
                  </Text>
                </View>
              )}
            </TouchableOpacity>

            {ocrNotice ? (
              <View style={[styles.ocrNoticeBox, { backgroundColor: colors.bgSubtle, borderColor: colors.borderLight }]}>
                <Ionicons name="information-circle-outline" size={16} color={colors.primary} />
                <Text style={{ fontSize: 12, color: colors.textMain, flex: 1 }}>{ocrNotice}</Text>
              </View>
            ) : null}

            {ocrDetails ? (
              <View style={[styles.ocrDetailCard, { backgroundColor: colors.bgSubtle, borderColor: colors.borderLight }]}>
                <View style={{ flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center' }}>
                  <Text style={{ fontSize: 11, fontWeight: '700', color: colors.primary }}>
                    OCR PACKAGING EXTRACTION
                  </Text>
                  {ocrDetails.confidence ? (
                    <Text style={{ fontSize: 11, fontWeight: '700', color: colors.textMuted }}>
                      {Math.round(ocrDetails.confidence * 100)}% Match
                    </Text>
                  ) : null}
                </View>

                {ocrDetails.brand ? (
                  <View style={{ flexDirection: 'row', marginTop: 4 }}>
                    <Text style={{ fontSize: 11, fontWeight: '700', color: colors.textMuted, width: 65 }}>Brand:</Text>
                    <Text style={{ fontSize: 12, color: colors.textMain, fontWeight: '600', flex: 1 }}>{ocrDetails.brand}</Text>
                  </View>
                ) : null}

                {ocrDetails.strength ? (
                  <View style={{ flexDirection: 'row', marginTop: 2 }}>
                    <Text style={{ fontSize: 11, fontWeight: '700', color: colors.textMuted, width: 65 }}>Strength:</Text>
                    <Text style={{ fontSize: 12, color: colors.textMain, fontWeight: '600', flex: 1 }}>{ocrDetails.strength}</Text>
                  </View>
                ) : null}

                {ocrDetails.candidates && ocrDetails.candidates.length > 1 ? (
                  <View style={{ marginTop: 6 }}>
                    <Text style={{ fontSize: 10, color: colors.textSubtle, marginBottom: 4 }}>Detected Candidates (tap to use):</Text>
                    <View style={{ flexDirection: 'row', flexWrap: 'wrap', gap: 4 }}>
                      {ocrDetails.candidates.slice(0, 4).map((c, idx) => (
                        <TouchableOpacity
                          key={idx}
                          onPress={() => setMedName(c)}
                          style={{
                            backgroundColor: colors.bgSurface,
                            borderWidth: 1,
                            borderColor: colors.borderLight,
                            paddingHorizontal: 6,
                            paddingVertical: 2,
                            borderRadius: 4,
                          }}
                        >
                          <Text style={{ fontSize: 10, color: colors.textMain }}>{c}</Text>
                        </TouchableOpacity>
                      ))}
                    </View>
                  </View>
                ) : null}

                {ocrDetails.rawText ? (
                  <View style={{ marginTop: 6, borderTopWidth: 1, borderTopColor: colors.borderLight, paddingTop: 4 }}>
                    <Text style={{ fontSize: 10, color: colors.textSubtle }}>Raw Scanned Text:</Text>
                    <Text style={{ fontSize: 11, color: colors.textMuted, marginTop: 2, flexWrap: 'wrap' }} numberOfLines={3}>
                      {ocrDetails.rawText}
                    </Text>
                  </View>
                ) : null}
              </View>
            ) : null}

            <Text style={[styles.formLabel, { color: colors.textMuted, marginTop: 12 }]}>Medicine Name:</Text>
            <TextInput
              style={[styles.input, { backgroundColor: colors.bgSubtle, borderColor: colors.borderLight, color: colors.textMain }]}
              placeholder="e.g. Paracetamol 500mg"
              placeholderTextColor={colors.textSubtle}
              value={medName}
              onChangeText={setMedName}
            />

            <Text style={[styles.formLabel, { color: colors.textMuted, marginTop: 12 }]}>Schedule Slots:</Text>
            <View style={styles.slotPillRow}>
              {timeSlots.map((s) => (
                <TouchableOpacity
                  key={s}
                  style={[
                    styles.slotPill,
                    {
                      backgroundColor: medTimes.includes(s) ? colors.primaryLight : colors.bgSubtle,
                      borderColor: medTimes.includes(s) ? colors.primary : colors.borderLight,
                    },
                  ]}
                  onPress={() => toggleTimeSlot(s)}
                >
                  <Text style={{ fontSize: 12, fontWeight: '700', color: medTimes.includes(s) ? colors.primary : colors.textMain }}>
                    {s}
                  </Text>
                </TouchableOpacity>
              ))}
            </View>

            <Button
              title={isAddingMed ? t('saving') : editingMedicationId ? t('save') : 'Save Medicine Reminder'}
              onPress={handleAddMedication}
              loading={isAddingMed}
              style={{ marginTop: 18 }}
            />
          </View>
        </View>
      </Modal>

      {/* Floating Emergency SOS Button */}
      <EmergencySosButton />

      {/* Nearby Active SOS Emergencies Modal */}
      <NearbySosAlertsModal
        visible={isNearbySosOpen}
        onClose={() => setIsNearbySosOpen(false)}
      />
    </View>
  );
};

const styles = StyleSheet.create({
  container: {
    padding: 16,
    paddingBottom: 120,
  },
  welcomeBanner: {
    marginBottom: 16,
  },
  greetingText: {
    fontSize: 22,
    fontWeight: '800',
  },
  subGreeting: {
    fontSize: 13,
    marginTop: 2,
  },
  actionGrid: {
    flexDirection: 'row',
    gap: 10,
    marginBottom: 20,
  },
  actionCard: {
    flex: 1,
    padding: 12,
    borderRadius: 14,
    borderWidth: 1,
    alignItems: 'center',
    elevation: 2,
  },
  actionIconBox: {
    width: 44,
    height: 44,
    borderRadius: 22,
    alignItems: 'center',
    justifyContent: 'center',
    marginBottom: 8,
  },
  actionIcon: {
    fontSize: 22,
  },
  actionTitle: {
    fontSize: 13,
    fontWeight: '700',
    textAlign: 'center',
  },
  actionSubtitle: {
    fontSize: 10,
    marginTop: 2,
    textAlign: 'center',
  },
  sectionHeaderRow: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    marginBottom: 12,
  },
  addMedBtn: {
    minHeight: 34,
    paddingVertical: 5,
    paddingHorizontal: 10,
    flexShrink: 0,
  },
  modalBackdrop: {
    flex: 1,
    backgroundColor: 'rgba(0,0,0,0.6)',
    justifyContent: 'flex-end',
  },
  modalCard: {
    borderTopLeftRadius: 24,
    borderTopRightRadius: 24,
    padding: 20,
  },
  modalHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: 14,
  },
  modalTitle: {
    fontSize: 18,
    fontWeight: '800',
  },
  formLabel: {
    fontSize: 13,
    fontWeight: '600',
    marginBottom: 6,
  },
  input: {
    height: 48,
    borderRadius: 10,
    borderWidth: 1,
    paddingHorizontal: 14,
    fontSize: 15,
  },
  slotPillRow: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    gap: 8,
  },
  slotPill: {
    paddingVertical: 8,
    paddingHorizontal: 12,
    borderRadius: 8,
    borderWidth: 1,
  },
  sectionTitle: {
    fontSize: 18,
    fontWeight: '800',
  },
  sectionSub: {
    fontSize: 13,
    marginTop: 2,
  },
  slotBlock: {
    marginBottom: 16,
  },
  slotTitle: {
    fontSize: 14,
    fontWeight: '800',
    textTransform: 'uppercase',
    letterSpacing: 0.5,
    marginBottom: 8,
    marginLeft: 4,
  },
  doseCard: {
    padding: 14,
    marginBottom: 10,
  },
  doseRow: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
  },
  doseName: {
    fontSize: 16,
    fontWeight: '700',
  },
  doseDetails: {
    fontSize: 13,
    marginTop: 4,
  },
  doseActionRow: {
    marginTop: 12,
    flexDirection: 'row',
    justifyContent: 'flex-end',
  },
  takeBtn: {
    minHeight: 40,
    paddingVertical: 8,
    paddingHorizontal: 20,
    borderRadius: 10,
  },
  takenNoticeRow: {
    marginTop: 8,
    paddingTop: 8,
    borderTopWidth: StyleSheet.hairlineWidth,
    borderTopColor: '#E2E8F0',
  },
  takenNotice: {
    fontSize: 13,
    fontWeight: '700',
  },
  ocrScanButton: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    paddingVertical: 10,
    paddingHorizontal: 14,
    borderRadius: 10,
    borderWidth: 1,
    borderStyle: 'dashed',
    marginBottom: 8,
  },
  ocrNoticeBox: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 8,
    padding: 10,
    borderRadius: 8,
    borderWidth: 1,
    marginBottom: 8,
  },
  ocrDetailCard: {
    padding: 10,
    borderRadius: 8,
    borderWidth: 1,
    marginTop: 4,
    marginBottom: 8,
  },
});
