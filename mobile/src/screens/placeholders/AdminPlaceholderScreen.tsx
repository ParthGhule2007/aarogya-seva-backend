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
import { facilityApi } from '../../api/facilityApi';
import { referralApi } from '../../api/referralApi';
import { sosApi } from '../../api/sosApi';
import { surveillanceApi } from '../../api/surveillanceApi';
import { FacilityDto } from '../../types/facility';
import { ReferralDetailDto, ReferralResponseDto, ReferralStatus } from '../../types/referral';
import { SosStatusResponse, getSosSenderName } from '../../types/sos';
import { OutbreakZoneDto } from '../../types/surveillance';
import { Header } from '../../components/common/Header';
import { Card } from '../../components/common/Card';
import { Button } from '../../components/common/Button';
import { StatusBadge } from '../../components/common/StatusBadge';
import { LoadingState } from '../../components/common/LoadingState';
import { EmptyState } from '../../components/common/EmptyState';
import { ErrorState } from '../../components/common/ErrorState';

const DEFAULT_FACILITY_ID = 'c1000000-0000-0000-0000-000000000001';

function computeDistanceKm(lat1: number, lon1: number, lat2: number, lon2: number): number {
  const R = 6371;
  const dLat = ((lat2 - lat1) * Math.PI) / 180;
  const dLon = ((lon2 - lon1) * Math.PI) / 180;
  const a =
    Math.sin(dLat / 2) * Math.sin(dLat / 2) +
    Math.cos((lat1 * Math.PI) / 180) * Math.cos((lat2 * Math.PI) / 180) * Math.sin(dLon / 2) * Math.sin(dLon / 2);
  const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
  return R * c;
}

function formatRelativeTime(dateStr?: string): string {
  if (!dateStr) return '';
  const diffMs = Date.now() - new Date(dateStr).getTime();
  const mins = Math.floor(diffMs / 60000);
  if (mins < 1) return 'Just now';
  if (mins < 60) return `${mins}m ago`;
  const hours = Math.floor(mins / 60);
  if (hours < 24) return `${hours}h ago`;
  return `${Math.floor(hours / 24)}d ago`;
}

interface ParsedTriageInfo {
  slot?: string;
  condition?: string;
  symptoms?: string[];
  history?: string;
  precautions?: string;
  notes?: string;
  rawText?: string;
}

const parseTriageMessage = (msg?: string | null): ParsedTriageInfo => {
  if (!msg) return {};
  if (!msg.includes('[AI_TRIAGE]') && !msg.includes('[MANUAL]') && !msg.includes('|')) {
    return { rawText: msg };
  }

  const clean = msg.replace(/\[(AI_TRIAGE|MANUAL)\]/g, '').trim();
  const segments = clean.split('|').map((s) => s.trim()).filter(Boolean);
  if (segments.length === 0) return { rawText: msg };

  const parsed: ParsedTriageInfo = {};
  let labeledCount = 0;

  for (const seg of segments) {
    const colonIdx = seg.indexOf(':');
    if (colonIdx > 0) {
      const key = seg.slice(0, colonIdx).trim().toLowerCase();
      const val = seg.slice(colonIdx + 1).trim();
      if (key === 'slot') {
        parsed.slot = val;
        labeledCount++;
      } else if (key === 'condition') {
        parsed.condition = val;
        labeledCount++;
      } else if (key === 'symptoms' || key === 'symptom') {
        parsed.symptoms = val.split(',').map((s) => s.trim()).filter(Boolean);
        labeledCount++;
      } else if (key === 'history') {
        parsed.history = val;
        labeledCount++;
      } else if (key === 'precautions' || key === 'precaution') {
        parsed.precautions = val;
        labeledCount++;
      } else if (key === 'notes' || key === 'note') {
        parsed.notes = val;
        labeledCount++;
      }
    }
  }

  if (labeledCount === 0) {
    if (segments[0]) parsed.slot = segments[0];
    if (segments[1]) parsed.condition = segments[1];
    if (segments[2]) parsed.symptoms = segments[2].split(',').map((s) => s.trim()).filter(Boolean);
    if (segments[3]) parsed.history = segments[3];
    if (segments[4]) parsed.precautions = segments[4];
    if (segments[5]) parsed.notes = segments.slice(5).join(' | ');
  }

  return parsed;
};

export const AdminPlaceholderScreen: React.FC = () => {
  const { user } = useAuth();
  const { colors } = useTheme();

  const facilityId = user?.facilityId || user?.associatedEntityId || DEFAULT_FACILITY_ID;

  const [facility, setFacility] = useState<FacilityDto | null>(null);
  const [referrals, setReferrals] = useState<ReferralResponseDto[]>([]);
  const [referralDetails, setReferralDetails] = useState<Record<string, ReferralDetailDto>>({});
  const [sosQueue, setSosQueue] = useState<SosStatusResponse[]>([]);
  const [outbreakZones, setOutbreakZones] = useState<OutbreakZoneDto[]>([]);
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [actionId, setActionId] = useState<string | null>(null);

  const fetchAdminData = useCallback(
    async (silent = false) => {
      if (!silent) setError(null);
      try {
        const [facData, facReferrals, activeSos, facSos, zones] = await Promise.all([
          facilityApi.getFacilityById(facilityId).catch(() => null),
          referralApi.getReferralsByFacilityId(facilityId).catch(() => [] as ReferralResponseDto[]),
          sosApi.getActiveSosAlerts().catch(() => [] as SosStatusResponse[]),
          sosApi.getSosByFacility(facilityId).catch(() => [] as SosStatusResponse[]),
          surveillanceApi.getOutbreakZones().catch(() => [] as OutbreakZoneDto[]),
        ]);

        if (facData) setFacility(facData);
        setReferrals(Array.isArray(facReferrals) ? facReferrals : []);

        // Filter SOS alerts strictly relevant to THIS hospital:
        // 1. Alert specifically targeted at this facilityId
        // 2. Alert located within 25 km catchment radius of this facility
        // 3. Exclude RESOLVED alerts from active dispatch queue
        const relevantSosMap = new Map<string, SosStatusResponse>();
        (facSos || []).forEach((s) => {
          if (s?.sosId && s.status !== 'RESOLVED') relevantSosMap.set(s.sosId, s);
        });

        const facLat = facData?.latitude ?? (user?.role === 'FACILITY_ADMIN' ? 18.5204 : 28.5562);
        const facLon = facData?.longitude ?? (user?.role === 'FACILITY_ADMIN' ? 73.8567 : 77.1000);

        (activeSos || []).forEach((s) => {
          if (!s?.sosId || s.status === 'RESOLVED') return;
          if (s.facilityId === facilityId) {
            relevantSosMap.set(s.sosId, s);
          } else if (s.latitude != null && s.longitude != null && facLat != null && facLon != null) {
            const dist = computeDistanceKm(facLat, facLon, s.latitude, s.longitude);
            if (dist <= 25.0) {
              relevantSosMap.set(s.sosId, s);
            }
          }
        });

        setSosQueue(Array.from(relevantSosMap.values()));

        // Fetch detailed clinical dossier for active referrals
        if (Array.isArray(facReferrals) && facReferrals.length > 0) {
          Promise.all(
            facReferrals.slice(0, 10).map(async (r: ReferralResponseDto) => {
              try {
                const detail = await referralApi.getReferralById(r.id);
                return { id: r.id, detail };
              } catch {
                return null;
              }
            })
          ).then((results) => {
            const detailMap: Record<string, ReferralDetailDto> = {};
            results.forEach((item) => {
              if (item && item.detail) {
                detailMap[item.id] = item.detail;
              }
            });
            setReferralDetails((prev) => ({ ...prev, ...detailMap }));
          });
        }

        setOutbreakZones(Array.isArray(zones) ? zones : []);
      } catch (err: any) {
        console.error('Admin console load error:', err);
        if (!silent) {
          setError(err.message || 'Unable to load Facility Admin Console data.');
        }
      } finally {
        setLoading(false);
        setRefreshing(false);
      }
    },
    [facilityId]
  );

  useEffect(() => {
    fetchAdminData(false);
    const interval = setInterval(() => {
      fetchAdminData(true);
    }, 12000);
    return () => clearInterval(interval);
  }, [fetchAdminData]);

  const handleUpdateReferral = async (
    referralId: string,
    status: ReferralStatus,
    responseNote: string
  ) => {
    setActionId(referralId);
    try {
      const updated = await referralApi.updateReferralStatus(referralId, {
        status,
        facilityResponse: responseNote,
      });
      setReferrals((prev) =>
        prev.map((r) => (r.id === referralId ? updated : r))
      );
      Alert.alert(
        `Referral ${status}`,
        `Referral #${updated.referralToken || referralId.slice(0, 8)} updated to ${status}.`
      );
    } catch (err: any) {
      Alert.alert('Referral Update Error', err?.message || 'Could not update referral status.');
    } finally {
      setActionId(null);
    }
  };

  const handleResolveSos = async (sosId: string) => {
    setActionId(sosId);
    try {
      await sosApi.resolveSos(sosId);
      await fetchAdminData(true);
      Alert.alert('Emergency Resolved', `SOS alert #${sosId.slice(0, 8)} marked RESOLVED.`);
    } catch (err: any) {
      Alert.alert('SOS Error', err?.message || 'Failed to resolve SOS alert.');
    } finally {
      setActionId(null);
    }
  };

  const pendingReferrals = referrals.filter((r) => r.status === 'PENDING').length;
  const activeEmergencies = sosQueue.filter((s) => s.status !== 'RESOLVED').length;

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
              fetchAdminData(false);
            }}
            colors={[colors.primary]}
          />
        }
      >
        {/* Facility Capacity & Summary Card */}
        <Card style={styles.summaryCard}>
          <View style={styles.rowBetween}>
            <View style={{ flex: 1 }}>
              <View style={{ flexDirection: 'row', alignItems: 'center', gap: 6 }}>
                <Ionicons name="business" size={20} color={colors.primary} />
                <Text style={[styles.title, { color: colors.textMain }]}>
                  {facility?.name || facility?.facilityName || 'Facility Dashboard'}
                </Text>
              </View>
              <Text style={{ fontSize: 12, color: colors.textSubtle, marginTop: 2 }}>
                {facility?.facilityType || 'PHC'} • {facility?.villageTown || 'Aerocity'}, {facility?.district || 'New Delhi'}
              </Text>
              <Text style={{ fontSize: 11, color: colors.primary, marginTop: 2, fontWeight: '700' }}>
                Facility ID: {facilityId.slice(0, 13)}... • Live updates active
              </Text>
            </View>
            <StatusBadge status={facility?.isOperational !== false ? 'AVAILABLE' : 'OFFLINE'} />
          </View>

          <View style={[styles.statsRow, { backgroundColor: colors.bgSubtle }]}>
            <View style={styles.statItem}>
              <Text style={[styles.statVal, { color: colors.sosRed }]}>{activeEmergencies}</Text>
              <Text style={[styles.statLabel, { color: colors.textSubtle }]}>Active SOS</Text>
            </View>
            <View style={styles.statItem}>
              <Text style={[styles.statVal, { color: '#D97706' }]}>{pendingReferrals}</Text>
              <Text style={[styles.statLabel, { color: colors.textSubtle }]}>Pending Referrals</Text>
            </View>
            <View style={styles.statItem}>
              <Text style={[styles.statVal, { color: colors.primary }]}>{referrals.length}</Text>
              <Text style={[styles.statLabel, { color: colors.textSubtle }]}>Total Referrals</Text>
            </View>
            <View style={styles.statItem}>
              <Text style={[styles.statVal, { color: colors.riskLow }]}>
                {facility?.availableServices?.length || 4}
              </Text>
              <Text style={[styles.statLabel, { color: colors.textSubtle }]}>Active Units</Text>
            </View>
          </View>
        </Card>

        {error ? <ErrorState message={error} onRetry={() => fetchAdminData(false)} /> : null}

        {/* SECTION 1: EMERGENCY SOS DISPATCH QUEUE */}
        <View style={{ flexDirection: 'row', alignItems: 'center', gap: 6, marginBottom: 8 }}>
          <Ionicons name="warning" size={18} color={colors.sosRed} />
          <Text style={[styles.sectionTitle, { color: colors.sosRed, marginBottom: 0 }]}>
            Emergency SOS Dispatch Queue ({sosQueue.length})
          </Text>
        </View>
        {sosQueue.length === 0 ? (
          <Card style={styles.emptyCard}>
            <Text style={{ fontSize: 13, color: colors.textSubtle }}>
              No active SOS emergency calls in the dispatch queue.
            </Text>
          </Card>
        ) : (
          sosQueue.slice(0, 8).map((sos) => {
            const isResolved = sos.status === 'RESOLVED';
            const distKm =
              facility?.latitude != null && facility?.longitude != null && sos.latitude != null && sos.longitude != null
                ? computeDistanceKm(facility.latitude, facility.longitude, sos.latitude, sos.longitude)
                : null;
            const relativeTime = formatRelativeTime(sos.triggeredAt);

            return (
              <Card
                key={sos.sosId}
                style={[
                  styles.itemCard,
                  {
                    borderColor: isResolved ? colors.borderLight : colors.sosRed,
                    borderWidth: isResolved ? 1 : 2,
                    backgroundColor: isResolved ? colors.bgSurface : '#FEF2F2',
                  },
                ]}
              >
                <View style={styles.rowBetween}>
                  <View style={{ flex: 1, paddingRight: 8 }}>
                    <View style={{ flexDirection: 'row', alignItems: 'center', gap: 6 }}>
                      <Text style={{ fontWeight: '800', fontSize: 14, color: isResolved ? colors.textMain : '#B91C1C' }}>
                        Emergency SOS #{sos.sosId.slice(0, 8)}
                      </Text>
                      {distKm !== null && (
                        <View style={{ backgroundColor: '#FEE2E2', paddingHorizontal: 6, paddingVertical: 2, borderRadius: 4 }}>
                          <Text style={{ fontSize: 10, fontWeight: '700', color: '#B91C1C' }}>
                            {distKm.toFixed(1)} km away
                          </Text>
                        </View>
                      )}
                    </View>
                    <Text style={{ fontSize: 11, color: colors.textSubtle, marginTop: 2 }}>
                      {relativeTime} • {getSosSenderName(sos.message)} • Targets: {(sos.targets || []).join(', ')}
                    </Text>
                  </View>
                  <StatusBadge status={isResolved ? 'RESOLVED' : 'ACTIVE'} />
                </View>

                <Text style={{ fontSize: 13, color: colors.textMain, marginTop: 6, fontWeight: '500' }}>
                  {sos.message || 'Emergency triggered via Aarogya Seva'}
                </Text>

                <View style={{ flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', marginTop: 4 }}>
                  <Text style={{ fontSize: 11, color: colors.textSubtle }}>
                    Coordinates: {sos.latitude?.toFixed(4)}, {sos.longitude?.toFixed(4)}
                  </Text>
                  <Text style={{ fontSize: 11, color: (sos.recipientsNotified || 0) > 0 ? colors.primary : colors.textSubtle, fontWeight: '600' }}>
                    SMS/Push: {sos.recipientsNotified || 0} sent, {sos.recipientsFailed || 0} queued
                  </Text>
                </View>

                <View style={{ flexDirection: 'row', gap: 8, marginTop: 10 }}>
                  {sos.latitude && sos.longitude && (
                    <Button
                      title="GPS Map"
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
                      title="Resolve Emergency"
                      onPress={() => handleResolveSos(sos.sosId)}
                      loading={actionId === sos.sosId}
                      variant="primary"
                      style={{ flex: 1, minHeight: 36, paddingVertical: 6 }}
                    />
                  )}
                </View>
              </Card>
            );
          })
        )}

        {/* SECTION 2: INCOMING PATIENT REFERRALS */}
        <View style={{ flexDirection: 'row', alignItems: 'center', gap: 6, marginTop: 14, marginBottom: 8 }}>
          <Ionicons name="git-network-outline" size={18} color={colors.primary} />
          <Text style={[styles.sectionTitle, { color: colors.textMain, marginBottom: 0 }]}>
            Incoming Clinical Referrals ({referrals.length})
          </Text>
        </View>
        {loading ? (
          <LoadingState message="Loading facility referrals..." />
        ) : referrals.length === 0 ? (
          <EmptyState
            icon="git-network-outline"
            title="No Referrals Yet"
            description="Referrals created by ASHA workers or AI Triage for this facility will appear here."
          />
        ) : (
          referrals.map((ref) => {
            const busy = actionId === ref.id;
            const detail = referralDetails[ref.id];
            const patient = detail?.patient;
            const symptoms = detail?.symptoms || [];
            const ai = detail?.aiAssessment;
            const isManualBooking = ref.patientMessage?.includes('[MANUAL]');
            const parsedTriage = parseTriageMessage(ref.patientMessage);

            const displaySymptoms = symptoms.length > 0
              ? symptoms.map((s) => ({ name: s.symptomName, severity: s.severity || 'MILD' }))
              : (parsedTriage.symptoms || []).map((s) => ({ name: s, severity: 'REPORTED' }));

            return (
              <Card key={ref.id} style={styles.itemCard}>
                <View style={styles.rowBetween}>
                  <View style={{ flex: 1, paddingRight: 6 }}>
                    <View style={{ flexDirection: 'row', alignItems: 'center', gap: 6 }}>
                      <Text style={{ fontWeight: '800', fontSize: 15, color: colors.textMain }}>
                        {patient?.fullName || `Patient #${ref.patientId?.slice(0, 8)}`}
                      </Text>
                      <View
                        style={{
                          backgroundColor: isManualBooking ? colors.bgSubtle : colors.primaryLight,
                          paddingHorizontal: 6,
                          paddingVertical: 2,
                          borderRadius: 4,
                          borderWidth: 1,
                          borderColor: isManualBooking ? colors.borderLight : colors.primary,
                        }}
                      >
                        <Text
                          style={{
                            fontSize: 9,
                            fontWeight: '800',
                            color: isManualBooking ? colors.textSubtle : colors.primary,
                          }}
                        >
                          {isManualBooking ? 'MANUAL' : 'AI SUGGESTED'}
                        </Text>
                      </View>
                    </View>
                    <Text style={{ fontSize: 12, color: colors.textSubtle, marginTop: 2, flexWrap: 'wrap', flexShrink: 1 }}>
                      {patient?.age ? `${patient.age}y ${patient.gender || ''} • ` : ''}
                      {patient?.villageTown ? `${patient.villageTown}, ${patient.district || ''}` : `Patient ID: ${ref.patientId?.slice(0, 8)}...`}
                      {patient?.contactNumber ? ` • Phone: ${patient.contactNumber}` : ''}
                    </Text>
                  </View>
                  <StatusBadge status={ref.status} />
                </View>

                {/* Patient Symptoms (Shown as chips once) */}
                {displaySymptoms.length > 0 && (
                  <View style={{ flexDirection: 'row', flexWrap: 'wrap', gap: 6, marginTop: 8 }}>
                    {displaySymptoms.map((s, idx) => (
                      <View
                        key={idx}
                        style={{
                          backgroundColor: colors.bgSubtle,
                          paddingHorizontal: 8,
                          paddingVertical: 3,
                          borderRadius: 6,
                          borderWidth: 1,
                          borderColor: colors.borderLight,
                        }}
                      >
                        <Text style={{ fontSize: 11, color: colors.textMain, fontWeight: '600' }}>
                          {s.name} ({s.severity})
                        </Text>
                      </View>
                    ))}
                  </View>
                )}

                {/* AI Predicted Condition & Clinical Risk */}
                <View style={[styles.reasonBox, { backgroundColor: colors.bgSubtle, marginTop: 8 }]}>
                  {/* Diagnosis Row */}
                  <View style={{ marginBottom: 6 }}>
                    <Text style={{ fontSize: 12, fontWeight: '800', color: colors.primary, flexWrap: 'wrap', flexShrink: 1 }}>
                      AI DIAGNOSIS: {ai?.possibleDiseases?.[0]?.category || parsedTriage.condition || ref.reason || 'Medical Examination'}
                    </Text>
                  </View>

                  {/* Clinical Risk Row on separate line */}
                  <View style={{ flexDirection: 'row', alignItems: 'center', marginBottom: 8 }}>
                    <View
                      style={{
                        backgroundColor: (ai?.riskLevel === 'HIGH' || ref.urgencyLevel === 'HIGH') ? '#FEE2E2' : '#DCFCE7',
                        paddingHorizontal: 8,
                        paddingVertical: 3,
                        borderRadius: 4,
                        borderWidth: 1,
                        borderColor: (ai?.riskLevel === 'HIGH' || ref.urgencyLevel === 'HIGH') ? '#FCA5A5' : '#86EFAC',
                      }}
                    >
                      <Text
                        style={{
                          fontSize: 10,
                          fontWeight: '800',
                          color: (ai?.riskLevel === 'HIGH' || ref.urgencyLevel === 'HIGH') ? '#DC2626' : '#15803D',
                        }}
                      >
                        CLINICAL RISK: {ai?.riskLevel || ref.urgencyLevel || 'MEDIUM'}{ai?.riskScore ? ` (${ai.riskScore}/100)` : ''}
                      </Text>
                    </View>
                  </View>

                  {/* Parsed Triage Labelled Rows */}
                  <View style={{ marginTop: 2 }}>
                    {parsedTriage.slot ? (
                      <View style={styles.triageRow}>
                        <Text style={[styles.triageLabel, { color: colors.textMuted }]}>Slot:</Text>
                        <Text style={[styles.triageValue, { color: colors.textMain }]}>{parsedTriage.slot}</Text>
                      </View>
                    ) : null}
                    {parsedTriage.condition && parsedTriage.condition !== (ai?.possibleDiseases?.[0]?.category || ref.reason) ? (
                      <View style={styles.triageRow}>
                        <Text style={[styles.triageLabel, { color: colors.textMuted }]}>Condition:</Text>
                        <Text style={[styles.triageValue, { color: colors.textMain }]}>{parsedTriage.condition}</Text>
                      </View>
                    ) : null}
                    {parsedTriage.history ? (
                      <View style={styles.triageRow}>
                        <Text style={[styles.triageLabel, { color: colors.textMuted }]}>History:</Text>
                        <Text style={[styles.triageValue, { color: colors.textMain }]}>{parsedTriage.history}</Text>
                      </View>
                    ) : null}
                    {parsedTriage.precautions ? (
                      <View style={styles.triageRow}>
                        <Text style={[styles.triageLabel, { color: colors.textMuted }]}>Precautions:</Text>
                        <Text style={[styles.triageValue, { color: colors.textMain }]}>{parsedTriage.precautions}</Text>
                      </View>
                    ) : null}
                    {parsedTriage.notes ? (
                      <View style={styles.triageRow}>
                        <Text style={[styles.triageLabel, { color: colors.textMuted }]}>Notes:</Text>
                        <Text style={[styles.triageValue, { color: colors.textMain }]}>{parsedTriage.notes}</Text>
                      </View>
                    ) : null}
                    {parsedTriage.rawText ? (
                      <Text style={{ fontSize: 12, color: colors.textMain, marginTop: 4, lineHeight: 18, flexWrap: 'wrap', flexShrink: 1 }}>
                        {parsedTriage.rawText}
                      </Text>
                    ) : null}
                  </View>

                  {ref.facilityResponse && (
                    <Text style={{ fontSize: 12, color: colors.primary, marginTop: 6, fontWeight: '600', flexWrap: 'wrap', flexShrink: 1 }}>
                      Facility Response: {ref.facilityResponse}
                    </Text>
                  )}
                </View>

                {ref.status === 'PENDING' && (
                  <View style={{ flexDirection: 'row', gap: 8, marginTop: 10 }}>
                    <Button
                      title="Reject"
                      onPress={() =>
                        handleUpdateReferral(
                          ref.id,
                          'REJECTED',
                          'Referred to District Hospital due to specialist requirement'
                        )
                      }
                      disabled={busy}
                      variant="outline"
                      style={{ flex: 1, minHeight: 38, paddingVertical: 6 }}
                    />
                    <Button
                      title={busy ? 'Updating...' : 'Accept & Reserve Bed'}
                      onPress={() =>
                        handleUpdateReferral(
                          ref.id,
                          'ACCEPTED',
                          'Bed & medical officer reserved at Primary Health Centre'
                        )
                      }
                      loading={busy}
                      variant="primary"
                      style={{ flex: 2, minHeight: 38, paddingVertical: 6 }}
                    />
                  </View>
                )}

                {ref.status === 'ACCEPTED' && (
                  <Button
                    title="Mark Referral Completed"
                    onPress={() =>
                      handleUpdateReferral(
                        ref.id,
                        'COMPLETED',
                        'Patient examined and treated at facility'
                      )
                    }
                    loading={busy}
                    variant="secondary"
                    style={{ marginTop: 10, minHeight: 38, paddingVertical: 6 }}
                  />
                )}
              </Card>
            );
          })
        )}

        {/* SECTION 3: REGIONAL OUTBREAK SURVEILLANCE */}
        {outbreakZones.length > 0 && (
          <View style={{ marginTop: 14 }}>
            <View style={{ flexDirection: 'row', alignItems: 'center', gap: 6, marginBottom: 8 }}>
              <Ionicons name="map-outline" size={18} color={colors.primary} />
              <Text style={[styles.sectionTitle, { color: colors.textMain, marginBottom: 0 }]}>
                Regional Epidemiological Outbreak Zones
              </Text>
            </View>
            {outbreakZones.map((zone) => {
              const isRed = zone.zoneColor === 'RED';
              const isYellow = zone.zoneColor === 'YELLOW';
              const badgeBg = isRed ? '#FEE2E2' : isYellow ? '#FEF3C7' : '#DCFCE7';
              const badgeText = isRed ? '#B91C1C' : isYellow ? '#B45309' : '#15803D';

              return (
                <Card key={zone.zoneId || zone.areaId || zone.areaName} style={styles.itemCard}>
                  <View style={styles.rowBetween}>
                    <View style={{ flex: 1 }}>
                      <Text style={{ fontWeight: '800', fontSize: 14, color: colors.textMain }}>
                        {zone.areaName}
                      </Text>
                      <Text style={{ fontSize: 12, color: colors.textSubtle }}>
                        Active Cases: {zone.activeCases} • High Risk: {zone.highRiskCases}
                      </Text>
                    </View>
                    <View style={{ backgroundColor: badgeBg, paddingHorizontal: 10, paddingVertical: 4, borderRadius: 999 }}>
                      <Text style={{ color: badgeText, fontWeight: '800', fontSize: 11 }}>
                        ● {zone.zoneColor} ZONE
                      </Text>
                    </View>
                  </View>
                  {zone.topDiseases?.length > 0 && (
                    <Text style={{ fontSize: 12, fontWeight: '600', color: colors.textMain, marginTop: 4 }}>
                      {zone.topDiseases.slice(0, 3).join(', ')}
                    </Text>
                  )}
                </Card>
              );
            })}
          </View>
        )}
      </ScrollView>
    </View>
  );
};

const styles = StyleSheet.create({
  container: {
    padding: 14,
    paddingBottom: 80,
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
    fontSize: 17,
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
  itemCard: {
    padding: 14,
    marginBottom: 10,
  },
  reasonBox: {
    padding: 10,
    borderRadius: 8,
    marginTop: 8,
  },
  triageRow: {
    flexDirection: 'row',
    alignItems: 'flex-start',
    marginVertical: 2,
    flexWrap: 'wrap',
  },
  triageLabel: {
    fontSize: 11,
    fontWeight: '700',
    width: 85,
  },
  triageValue: {
    fontSize: 12,
    flex: 1,
    flexWrap: 'wrap',
    flexShrink: 1,
  },
});
