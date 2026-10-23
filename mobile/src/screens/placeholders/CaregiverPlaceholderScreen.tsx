import React, { useState, useEffect, useCallback } from 'react';
import {
  View,
  Text,
  StyleSheet,
  ScrollView,
  RefreshControl,
  Linking,
  Alert,
} from 'react-native';
import { Ionicons } from '@expo/vector-icons';
import { useAuth } from '../../context/AuthContext';
import { useTheme } from '../../context/ThemeContext';
import { medicineApi } from '../../api/medicineApi';
import { caregiverApi } from '../../api/caregiverApi';
import { sosApi } from '../../api/sosApi';
import { ScheduledDose, CaregiverEscalation, Caregiver, formatDosage } from '../../types/medicine';
import { SosStatusResponse, getSosSenderName } from '../../types/sos';
import { Header } from '../../components/common/Header';
import { Card } from '../../components/common/Card';
import { Button } from '../../components/common/Button';
import { StatusBadge } from '../../components/common/StatusBadge';
import { LoadingState } from '../../components/common/LoadingState';
import { EmptyState } from '../../components/common/EmptyState';
import { ErrorState } from '../../components/common/ErrorState';
import { EmergencySosButton } from '../../components/common/EmergencySosButton';

const FALLBACK_PATIENT_ID = 'b1000000-0000-0000-0000-000000000001';

export const CaregiverPlaceholderScreen: React.FC = () => {
  const { user, selectedPatientId } = useAuth();
  const { colors } = useTheme();

  const patientId = selectedPatientId || user?.patientId || user?.associatedEntityId || FALLBACK_PATIENT_ID;

  const [doses, setDoses] = useState<ScheduledDose[]>([]);
  const [escalations, setEscalations] = useState<CaregiverEscalation[]>([]);
  const [caregivers, setCaregivers] = useState<Caregiver[]>([]);
  const [sosAlerts, setSosAlerts] = useState<SosStatusResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [resolvingId, setResolvingId] = useState<string | null>(null);

  const fetchCaregiverData = useCallback(
    async (silent = false) => {
      if (!silent) setError(null);
      try {
        const [todaySchedule, escList, cgList, patientSos] = await Promise.all([
          medicineApi.getTodaySchedule(patientId),
          caregiverApi.getEscalations(patientId),
          caregiverApi.getCaregivers(patientId),
          sosApi.getSosByPatient(patientId),
        ]);

        setDoses(Array.isArray(todaySchedule) ? todaySchedule : []);
        setEscalations(Array.isArray(escList) ? escList : []);
        setCaregivers(Array.isArray(cgList) ? cgList : []);
        setSosAlerts(Array.isArray(patientSos) ? patientSos : []);
      } catch (err: any) {
        console.error('Caregiver dashboard load error:', err);
        if (!silent) {
          setError(err.message || 'Unable to load Caregiver Companion data.');
        }
      } finally {
        setLoading(false);
        setRefreshing(false);
      }
    },
    [patientId]
  );

  useEffect(() => {
    fetchCaregiverData(false);
    const interval = setInterval(() => {
      fetchCaregiverData(true);
    }, 12000);
    return () => clearInterval(interval);
  }, [fetchCaregiverData]);

  const handleEnsureCaregiverRegistered = async () => {
    try {
      const created = await caregiverApi.addCaregiver({
        patientId,
        name: user?.displayName || 'Suresh Patil',
        relationship: 'Son / Primary Caregiver',
        phoneNumber: '9876543211',
        email: 'suresh.patil@ruralhealth.org',
        preferredChannel: 'SMS',
      });
      setCaregivers((prev) => [created, ...prev]);
      Alert.alert('Caregiver Linked ✓', `${created.name} linked to patient ${patientId.slice(0, 8)}...`);
    } catch (err: any) {
      Alert.alert('Caregiver Link Error', err?.message || 'Unable to register caregiver.');
    }
  };

  const handleResolveSos = async (sosId: string) => {
    setResolvingId(sosId);
    try {
      await sosApi.resolveSos(sosId);
      await fetchCaregiverData(true);
      Alert.alert('SOS Resolved ✓', `Emergency alert ${sosId.slice(0, 8)} marked RESOLVED.`);
    } catch (err: any) {
      Alert.alert('Resolve Failed', err?.message || 'Could not resolve SOS alert.');
    } finally {
      setResolvingId(null);
    }
  };

  const takenCount = doses.filter((d) => d.status === 'TAKEN').length;
  const missedCount = doses.filter((d) => d.status === 'MISSED').length;
  const adherencePct = doses.length > 0 ? Math.round((takenCount / doses.length) * 100) : 100;

  return (
    <View style={{ flex: 1, backgroundColor: colors.bgApp }}>
      <Header />

      <ScrollView
        contentContainerStyle={styles.container}
        refreshControl={
          <RefreshControl
            refreshing={refreshing}
            onRefresh={() => {
              setRefreshing(true);
              fetchCaregiverData(false);
            }}
            colors={[colors.primary]}
          />
        }
      >
        {/* Caregiver Header Card */}
        <Card style={styles.summaryCard}>
          <View style={styles.rowBetween}>
            <View style={{ flex: 1 }}>
              <View style={{ flexDirection: 'row', alignItems: 'center', gap: 6 }}>
                <Ionicons name="people-outline" size={20} color={colors.primary} />
                <Text style={[styles.title, { color: colors.textMain }]}>
                  Caregiver Companion
                </Text>
              </View>
              <Text style={{ fontSize: 12, color: colors.textSubtle, marginTop: 2 }}>
                Monitoring Patient ID: {patientId.slice(0, 8)}...
              </Text>
              <Text style={{ fontSize: 11, color: colors.primary, marginTop: 2, fontWeight: '700' }}>
                Auto-polling every 12s • {caregivers.length} registered caregiver(s)
              </Text>
            </View>
            {caregivers.length === 0 && (
              <Button
                title="+ Link Self"
                onPress={handleEnsureCaregiverRegistered}
                variant="outline"
                style={{ minHeight: 34, paddingVertical: 4, paddingHorizontal: 10 }}
              />
            )}
          </View>

          <View style={[styles.statsRow, { backgroundColor: colors.bgSubtle }]}>
            <View style={styles.statItem}>
              <Text style={[styles.statVal, { color: colors.primary }]}>{adherencePct}%</Text>
              <Text style={[styles.statLabel, { color: colors.textSubtle }]}>Adherence</Text>
            </View>
            <View style={styles.statItem}>
              <Text style={[styles.statVal, { color: colors.riskLow }]}>{takenCount}/{doses.length}</Text>
              <Text style={[styles.statLabel, { color: colors.textSubtle }]}>Taken Today</Text>
            </View>
            <View style={styles.statItem}>
              <Text style={[styles.statVal, { color: '#DC2626' }]}>{missedCount}</Text>
              <Text style={[styles.statLabel, { color: colors.textSubtle }]}>Missed Today</Text>
            </View>
            <View style={styles.statItem}>
              <Text style={[styles.statVal, { color: '#D97706' }]}>{escalations.length}</Text>
              <Text style={[styles.statLabel, { color: colors.textSubtle }]}>Escalations</Text>
            </View>
          </View>
        </Card>

        {error ? <ErrorState message={error} onRetry={() => fetchCaregiverData(false)} /> : null}

        {/* SECTION 1: EMERGENCY SOS ALERTS */}
        <View style={{ flexDirection: 'row', alignItems: 'center', gap: 6, marginBottom: 8 }}>
          <Ionicons name="warning" size={18} color={colors.sosRed} />
          <Text style={[styles.sectionTitle, { color: colors.sosRed, marginBottom: 0 }]}>
            Patient Emergency SOS Alerts ({sosAlerts.length})
          </Text>
        </View>
        {sosAlerts.length === 0 ? (
          <Card style={styles.emptyCard}>
            <Text style={{ fontSize: 13, color: colors.textSubtle }}>
              No SOS emergency alerts triggered by the patient.
            </Text>
          </Card>
        ) : (
          sosAlerts.slice(0, 5).map((sos) => {
            const isResolved = sos.status === 'RESOLVED';
            return (
              <Card
                key={sos.sosId}
                style={[
                  styles.alertCard,
                  {
                    borderColor: isResolved ? colors.borderLight : colors.sosRed,
                    borderWidth: isResolved ? 1 : 2,
                    backgroundColor: isResolved ? colors.bgSurface : '#FEF2F2',
                  },
                ]}
              >
                <View style={styles.rowBetween}>
                  <Text style={{ fontWeight: '800', fontSize: 14, color: isResolved ? colors.textMain : '#B91C1C' }}>
                    Emergency SOS • {getSosSenderName(sos.message)}
                  </Text>
                  <StatusBadge status={sos.status} />
                </View>
                <Text style={{ fontSize: 13, color: colors.textMain, marginTop: 4 }}>
                  {sos.message || 'Emergency SOS triggered by patient'}
                </Text>
                <Text style={{ fontSize: 12, color: colors.textSubtle, marginTop: 2 }}>
                  Coordinates: {sos.latitude?.toFixed(4)}, {sos.longitude?.toFixed(4)} • Targets: {(sos.targets || []).join(', ')}
                </Text>
                <Text style={{ fontSize: 11, color: colors.textSubtle, marginTop: 2 }}>
                  Triggered: {new Date(sos.triggeredAt).toLocaleString()}
                </Text>

                <View style={{ flexDirection: 'row', gap: 8, marginTop: 10 }}>
                  {sos.latitude && sos.longitude && (
                    <Button
                      title="Open Map"
                      onPress={() =>
                        Linking.openURL(
                          `https://www.google.com/maps/search/?api=1&query=${sos.latitude},${sos.longitude}`
                        )
                      }
                      variant="outline"
                      style={{ flex: 1, minHeight: 36, paddingVertical: 6 }}
                    />
                  )}
                  {!isResolved && (
                    <Button
                      title="Resolve SOS"
                      onPress={() => handleResolveSos(sos.sosId)}
                      loading={resolvingId === sos.sosId}
                      variant="primary"
                      style={{ flex: 1, minHeight: 36, paddingVertical: 6 }}
                    />
                  )}
                </View>
              </Card>
            );
          })
        )}

        {/* SECTION 2: MISSED-DOSE ESCALATIONS */}
        <View style={{ flexDirection: 'row', alignItems: 'center', gap: 6, marginTop: 14, marginBottom: 8 }}>
          <Ionicons name="alert-circle-outline" size={18} color="#D97706" />
          <Text style={[styles.sectionTitle, { color: colors.textMain, marginBottom: 0 }]}>
            Missed-Dose Escalation History ({escalations.length})
          </Text>
        </View>
        {loading ? (
          <LoadingState message="Checking missed-dose escalations..." />
        ) : escalations.length === 0 ? (
          <Card style={styles.emptyCard}>
            <Text style={{ fontSize: 13, color: colors.textSubtle }}>
              No consecutive missed-dose escalations recorded yet. When the patient misses 2+ consecutive doses, an escalation alert is logged here.
            </Text>
          </Card>
        ) : (
          escalations.map((esc) => (
            <Card
              key={esc.id}
              style={[
                styles.alertCard,
                { borderColor: '#F59E0B', borderWidth: 1.5, backgroundColor: '#FFFBEB' },
              ]}
            >
              <View style={styles.rowBetween}>
                <Text style={{ fontWeight: '800', fontSize: 14, color: '#92400E' }}>
                  {esc.medicationName || 'Prescribed Medicine'}
                </Text>
                <View style={{ backgroundColor: '#FEF3C7', paddingHorizontal: 8, paddingVertical: 3, borderRadius: 6 }}>
                  <Text style={{ fontSize: 11, fontWeight: '800', color: '#B45309' }}>
                    {esc.consecutiveMissedCount} Missed Streak
                  </Text>
                </View>
              </View>
              {esc.messageContent && (
                <Text style={{ fontSize: 12, color: '#78350F', marginTop: 4 }}>
                  {esc.messageContent}
                </Text>
              )}
              <Text style={{ fontSize: 11, color: '#92400E', marginTop: 4 }}>
                Channel: {esc.channel || 'SMS'} • Recipient: {esc.recipient || 'Caregiver'} • Status: {esc.deliveryStatus || 'SENT'}
              </Text>
              {esc.createdAt && (
                <Text style={{ fontSize: 11, color: colors.textSubtle, marginTop: 2 }}>
                  Logged: {new Date(esc.createdAt).toLocaleString()}
                </Text>
              )}
            </Card>
          ))
        )}

        {/* SECTION 3: LINKED PATIENT TODAY'S SCHEDULE */}
        <View style={{ flexDirection: 'row', alignItems: 'center', gap: 6, marginTop: 14, marginBottom: 8 }}>
          <Ionicons name="medkit-outline" size={18} color={colors.primary} />
          <Text style={[styles.sectionTitle, { color: colors.textMain, marginBottom: 0 }]}>
            Linked Patient's Today Schedule ({doses.length})
          </Text>
        </View>
        {doses.length === 0 ? (
          <EmptyState
            icon="medkit-outline"
            title="No Doses Scheduled Today"
            description="No scheduled doses found for the linked patient today."
          />
        ) : (
          doses.map((d) => (
            <Card key={d.id} style={styles.doseCard}>
              <View style={styles.rowBetween}>
                <View style={{ flex: 1 }}>
                  <Text style={{ fontWeight: '700', fontSize: 15, color: colors.textMain }}>
                    {d.medicationName}
                  </Text>
                  <Text style={{ fontSize: 12, color: colors.textSubtle, marginTop: 2 }}>
                    {formatDosage(d.dosageAmount, d.dosageUnit)} • Scheduled: {d.scheduledTime?.slice(0, 5)}
                  </Text>
                </View>
                <StatusBadge status={d.status} />
              </View>
            </Card>
          ))
        )}
      </ScrollView>

      <EmergencySosButton />
    </View>
  );
};

const styles = StyleSheet.create({
  container: {
    padding: 14,
    paddingBottom: 110,
  },
  summaryCard: {
    padding: 16,
    marginBottom: 14,
  },
  rowBetween: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
  },
  title: {
    fontSize: 18,
    fontWeight: '800',
  },
  statsRow: {
    flexDirection: 'row',
    justifyContent: 'space-around',
    paddingVertical: 12,
    borderRadius: 10,
    marginTop: 12,
  },
  statItem: {
    alignItems: 'center',
  },
  statVal: {
    fontSize: 18,
    fontWeight: '800',
  },
  statLabel: {
    fontSize: 11,
    marginTop: 2,
  },
  sectionTitle: {
    fontSize: 16,
    fontWeight: '800',
    marginBottom: 8,
  },
  emptyCard: {
    padding: 14,
    marginBottom: 10,
  },
  alertCard: {
    padding: 14,
    marginBottom: 10,
  },
  doseCard: {
    padding: 12,
    marginBottom: 8,
  },
});
