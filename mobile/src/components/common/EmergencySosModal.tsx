import React, { useState } from 'react';
import {
  View,
  Text,
  StyleSheet,
  Modal,
  TouchableOpacity,
  Linking,
  ActivityIndicator,
  ScrollView,
  Alert,
} from 'react-native';
import { Ionicons } from '@expo/vector-icons';
import * as Location from 'expo-location';
import { useAuth } from '../../context/AuthContext';
import { useTheme } from '../../context/ThemeContext';
import { useLanguage } from '../../i18n/LanguageContext';
import { sosApi } from '../../api/sosApi';
import { TargetType, SosResponse } from '../../types/sos';
import { IS_DEMO_MODE } from '../../config/apiConfig';
import { DemoModeBanner } from './DemoModeBanner';

interface EmergencySosModalProps {
  visible: boolean;
  onClose: () => void;
  overridePatientId?: string | null;
}

export const EmergencySosModal: React.FC<EmergencySosModalProps> = ({
  visible,
  onClose,
  overridePatientId,
}) => {
  const { user, selectedPatientId } = useAuth();
  const { colors } = useTheme();
  const { t } = useLanguage();

  const [selectedTargets, setSelectedTargets] = useState<TargetType[]>([
    'CAREGIVER',
    'FACILITY',
    'NEARBY_PEOPLE',
  ]);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [sosResult, setSosResult] = useState<SosResponse | null>(null);
  const [statusMessage, setStatusMessage] = useState<string | null>(null);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [usedDemoData, setUsedDemoData] = useState(false);

  const toggleTarget = (target: TargetType) => {
    if (selectedTargets.includes(target)) {
      if (selectedTargets.length > 1) {
        setSelectedTargets(selectedTargets.filter((t) => t !== target));
      }
    } else {
      setSelectedTargets([...selectedTargets, target]);
    }
  };

  const handleCallEmergency = (number: string) => {
    Linking.openURL(`tel:${number}`).catch(() => {
      Alert.alert('Call Failed', `Unable to place direct call to ${number}. Please dial manually.`);
    });
  };

  const handleTriggerSos = async () => {
    setIsSubmitting(true);
    setStatusMessage('Acquiring rapid GPS location...');

    let latitude = 18.5204;
    let longitude = 73.8567; // Rural Maharashtra default fallback

    try {
      const { status } = await Location.requestForegroundPermissionsAsync();
      if (status === 'granted') {
        try {
          // Rapid GPS fix with 3-second timeout
          const pos = await Promise.race([
            Location.getCurrentPositionAsync({ accuracy: Location.Accuracy.Balanced }),
            new Promise<null>((resolve) => setTimeout(() => resolve(null), 3000)),
          ]);

          if (pos && pos.coords) {
            latitude = pos.coords.latitude;
            longitude = pos.coords.longitude;
          } else {
            // Fallback to last known position
            const lastKnown = await Location.getLastKnownPositionAsync();
            if (lastKnown?.coords) {
              latitude = lastKnown.coords.latitude;
              longitude = lastKnown.coords.longitude;
            }
          }
        } catch (locErr) {
          console.warn('GPS fix timed out or failed, using fallback location:', locErr);
        }
      }
    } catch (permErr) {
      console.warn('Location permission check failed:', permErr);
    }

    setStatusMessage('Broadcasting emergency SOS alert to responders...');
    setErrorMessage(null);
    setUsedDemoData(false);

    const patientId =
      overridePatientId ||
      selectedPatientId ||
      user?.patientId ||
      user?.associatedEntityId ||
      'b1000000-0000-0000-0000-000000000001';
    const idempotencyKey = `sos-${Date.now()}`;
    const senderName = user?.displayName || user?.fullName || 'Ramesh Patil';

    try {
      const response = await sosApi.triggerSos(
        {
          patientId,
          latitude,
          longitude,
          targets: selectedTargets,
          facilityId: selectedTargets.includes('FACILITY')
            ? (user?.facilityId || 'c1000000-0000-0000-0000-000000000001')
            : null,
          message: `Medical Emergency for ${senderName}`,
          patientName: senderName,
        },
        idempotencyKey
      );

      setSosResult(response);
      setStatusMessage('Alert dispatched successfully!');
    } catch (err: any) {
      console.error('SOS trigger failed:', err);
      if (IS_DEMO_MODE) {
        setUsedDemoData(true);
        setSosResult({
          sosId: `mock-sos-${Date.now()}`,
          status: 'TRIGGERED',
          triggeredAt: new Date().toISOString(),
          message: `Emergency SOS for ${senderName} dispatched successfully`,
          targets: selectedTargets,
          patientName: senderName,
        });
      } else {
        const msg = err?.message || 'Failed to dispatch SOS alert to server.';
        setErrorMessage(msg);
        setStatusMessage(null);
        Alert.alert('SOS Dispatch Error', msg);
      }
    } finally {
      setIsSubmitting(false);
    }
  };

  const resetAndClose = () => {
    setSosResult(null);
    setStatusMessage(null);
    setErrorMessage(null);
    setUsedDemoData(false);
    onClose();
  };

  return (
    <Modal visible={visible} transparent animationType="slide" onRequestClose={resetAndClose}>
      <View style={styles.backdrop}>
        <View style={[styles.modalCard, { backgroundColor: colors.bgSurface }]}>
          <ScrollView contentContainerStyle={styles.scrollContent}>
            {/* Header */}
            <DemoModeBanner visible={usedDemoData} />
            {errorMessage && (
              <View style={[styles.resultBox, { backgroundColor: '#FEF2F2', borderColor: '#FECACA', marginBottom: 12, marginTop: 0 }]}>
                <View style={{ flexDirection: 'row', alignItems: 'center', gap: 6 }}>
                  <Ionicons name="alert-circle-outline" size={16} color="#DC2626" />
                  <Text style={{ color: '#DC2626', fontWeight: '700', fontSize: 13, flex: 1 }}>
                    {errorMessage}
                  </Text>
                </View>
              </View>
            )}
            <View style={styles.headerRow}>
              <View style={{ flexDirection: 'row', alignItems: 'center', gap: 6 }}>
                <Ionicons name="warning" size={24} color={colors.sosRed} />
                <Text style={[styles.title, { color: colors.sosRed }]}>
                  {t('navSos')}
                </Text>
              </View>
              <TouchableOpacity onPress={resetAndClose} style={styles.closeBtn}>
                <Ionicons name="close" size={22} color={colors.textSubtle} />
              </TouchableOpacity>
            </View>

            {/* Direct Dial Emergency Speed Dials */}
            <Text style={[styles.sectionHeading, { color: colors.textMain }]}>
              {t('sosEmergencyCall')}
            </Text>
            <View style={styles.dialRow}>
              <TouchableOpacity
                style={[styles.dialButton, { backgroundColor: '#B91C1C' }]}
                onPress={() => handleCallEmergency('108')}
                activeOpacity={0.8}
              >
                <Ionicons name="medical" size={20} color="#FFFFFF" style={{ marginBottom: 4 }} />
                <Text style={styles.dialText}>{t('sos108')}</Text>
              </TouchableOpacity>

              <TouchableOpacity
                style={[styles.dialButton, { backgroundColor: '#1D4ED8' }]}
                onPress={() => handleCallEmergency('112')}
                activeOpacity={0.8}
              >
                <Ionicons name="shield-checkmark" size={20} color="#FFFFFF" style={{ marginBottom: 4 }} />
                <Text style={styles.dialText}>{t('sos112')}</Text>
              </TouchableOpacity>
            </View>

            {/* Success state */}
            {sosResult ? (
              <View style={[styles.resultBox, { backgroundColor: colors.riskLowBg, borderColor: colors.riskLowBorder }]}>
                <Text style={[styles.resultTitle, { color: colors.riskLow }]}>
                  {t('sosStatusDispatched')}
                </Text>
                <Text style={[styles.resultBody, { color: colors.textMain }]}>
                  {sosResult.message || 'Responders notified with your GPS coordinates.'}
                </Text>
                <Text style={[styles.resultSub, { color: colors.textSubtle }]}>
                  Recipients: {selectedTargets.join(', ')}
                </Text>

                <TouchableOpacity
                  style={[styles.actionBtn, { backgroundColor: colors.primary }]}
                  onPress={resetAndClose}
                >
                  <Text style={styles.btnText}>Done</Text>
                </TouchableOpacity>
              </View>
            ) : (
              <>
                {/* Multi-Target Checklist */}
                <Text style={[styles.sectionHeading, { color: colors.textMain, marginTop: 16 }]}>
                  Digital Broadcast Recipients
                </Text>

                <TouchableOpacity
                  style={[
                    styles.targetCard,
                    {
                      backgroundColor: selectedTargets.includes('CAREGIVER') ? colors.primaryLight : colors.bgSubtle,
                      borderColor: selectedTargets.includes('CAREGIVER') ? colors.primary : colors.borderLight,
                    },
                  ]}
                  onPress={() => toggleTarget('CAREGIVER')}
                  activeOpacity={0.7}
                >
                  <View style={{ flex: 1 }}>
                    <Text style={[styles.targetTitle, { color: colors.textMain }]}>{t('sosOptionC')}</Text>
                    <Text style={[styles.targetDesc, { color: colors.textSubtle }]}>{t('sosOptionCDesc')}</Text>
                  </View>
                  <Ionicons
                    name={selectedTargets.includes('CAREGIVER') ? 'checkbox' : 'square-outline'}
                    size={22}
                    color={colors.primary}
                  />
                </TouchableOpacity>

                <TouchableOpacity
                  style={[
                    styles.targetCard,
                    {
                      backgroundColor: selectedTargets.includes('NEARBY_PEOPLE') ? colors.primaryLight : colors.bgSubtle,
                      borderColor: selectedTargets.includes('NEARBY_PEOPLE') ? colors.primary : colors.borderLight,
                    },
                  ]}
                  onPress={() => toggleTarget('NEARBY_PEOPLE')}
                  activeOpacity={0.7}
                >
                  <View style={{ flex: 1 }}>
                    <Text style={[styles.targetTitle, { color: colors.textMain }]}>{t('sosOptionA')}</Text>
                    <Text style={[styles.targetDesc, { color: colors.textSubtle }]}>{t('sosOptionADesc')}</Text>
                  </View>
                  <Ionicons
                    name={selectedTargets.includes('NEARBY_PEOPLE') ? 'checkbox' : 'square-outline'}
                    size={22}
                    color={colors.primary}
                  />
                </TouchableOpacity>

                <TouchableOpacity
                  style={[
                    styles.targetCard,
                    {
                      backgroundColor: selectedTargets.includes('FACILITY') ? colors.primaryLight : colors.bgSubtle,
                      borderColor: selectedTargets.includes('FACILITY') ? colors.primary : colors.borderLight,
                    },
                  ]}
                  onPress={() => toggleTarget('FACILITY')}
                  activeOpacity={0.7}
                >
                  <View style={{ flex: 1 }}>
                    <Text style={[styles.targetTitle, { color: colors.textMain }]}>{t('sosOptionB')}</Text>
                    <Text style={[styles.targetDesc, { color: colors.textSubtle }]}>{t('sosOptionBDesc')}</Text>
                  </View>
                  <Ionicons
                    name={selectedTargets.includes('FACILITY') ? 'checkbox' : 'square-outline'}
                    size={22}
                    color={colors.primary}
                  />
                </TouchableOpacity>

                {/* Status indicator during submission */}
                {statusMessage && (
                  <View style={styles.statusBox}>
                    <ActivityIndicator size="small" color={colors.sosRed} />
                    <Text style={[styles.statusText, { color: colors.textMuted }]}>{statusMessage}</Text>
                  </View>
                )}

                {/* Big Red Trigger SOS Button */}
                <TouchableOpacity
                  style={[styles.triggerButton, { backgroundColor: colors.sosRed }]}
                  onPress={handleTriggerSos}
                  disabled={isSubmitting}
                  activeOpacity={0.8}
                >
                  {isSubmitting ? (
                    <ActivityIndicator color="#FFFFFF" />
                  ) : (
                    <Text style={styles.triggerButtonText}>{t('sosTriggerNow')}</Text>
                  )}
                </TouchableOpacity>
              </>
            )}
          </ScrollView>
        </View>
      </View>
    </Modal>
  );
};

const styles = StyleSheet.create({
  backdrop: {
    flex: 1,
    backgroundColor: 'rgba(0,0,0,0.6)',
    justifyContent: 'flex-end',
  },
  modalCard: {
    borderTopLeftRadius: 24,
    borderTopRightRadius: 24,
    maxHeight: '90%',
    paddingHorizontal: 20,
    paddingTop: 16,
    paddingBottom: 32,
  },
  scrollContent: {
    paddingBottom: 20,
  },
  headerRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: 16,
  },
  headerIcon: {
    fontSize: 24,
    marginRight: 8,
  },
  title: {
    fontSize: 20,
    fontWeight: '800',
  },
  closeBtn: {
    padding: 6,
  },
  sectionHeading: {
    fontSize: 13,
    fontWeight: '700',
    textTransform: 'uppercase',
    letterSpacing: 0.5,
    marginBottom: 10,
  },
  dialRow: {
    flexDirection: 'row',
    gap: 10,
    marginBottom: 12,
  },
  dialButton: {
    flex: 1,
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    paddingVertical: 14,
    paddingHorizontal: 12,
    borderRadius: 12,
    elevation: 2,
  },
  dialIcon: {
    fontSize: 20,
    marginRight: 6,
  },
  dialText: {
    color: '#FFFFFF',
    fontWeight: '700',
    fontSize: 13,
  },
  targetCard: {
    flexDirection: 'row',
    alignItems: 'center',
    padding: 12,
    borderRadius: 12,
    borderWidth: 1,
    marginBottom: 8,
  },
  targetTitle: {
    fontSize: 14,
    fontWeight: '700',
  },
  targetDesc: {
    fontSize: 12,
    marginTop: 2,
  },
  statusBox: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 8,
    marginVertical: 12,
    justifyContent: 'center',
  },
  statusText: {
    fontSize: 13,
  },
  triggerButton: {
    marginTop: 16,
    paddingVertical: 16,
    borderRadius: 14,
    alignItems: 'center',
    justifyContent: 'center',
    elevation: 4,
  },
  triggerButtonText: {
    color: '#FFFFFF',
    fontSize: 16,
    fontWeight: '800',
    letterSpacing: 0.5,
  },
  resultBox: {
    padding: 16,
    borderRadius: 12,
    borderWidth: 1,
    marginTop: 16,
  },
  resultTitle: {
    fontSize: 16,
    fontWeight: '700',
    marginBottom: 6,
  },
  resultBody: {
    fontSize: 14,
    marginBottom: 4,
  },
  resultSub: {
    fontSize: 12,
    marginBottom: 12,
  },
  actionBtn: {
    paddingVertical: 12,
    borderRadius: 8,
    alignItems: 'center',
  },
  btnText: {
    color: '#FFFFFF',
    fontWeight: '700',
  },
});
