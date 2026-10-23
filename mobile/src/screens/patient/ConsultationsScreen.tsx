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
  Linking,
  Alert,
  ActivityIndicator,
} from 'react-native';
import { Ionicons } from '@expo/vector-icons';
import * as Location from 'expo-location';
import { useAuth } from '../../context/AuthContext';
import { useTheme } from '../../context/ThemeContext';
import { useLanguage } from '../../i18n/LanguageContext';
import { consultationApi } from '../../api/consultationApi';
import { doctorApi } from '../../api/doctorApi';
import { communicationApi } from '../../api/communicationApi';
import { caseApi } from '../../api/caseApi';
import { symptomApi } from '../../api/symptomApi';
import { aiApi } from '../../api/aiApi';
import { referralApi } from '../../api/referralApi';
import { facilityApi } from '../../api/facilityApi';
import { FacilityRecommendationDto } from '../../types/facility';
import {
  ConsultationResponseDTO,
  ConsultationUrgency,
  ConsultationType,
} from '../../types/consultation';
import { AvailableDoctorResponseDTO } from '../../types/doctor';
import { ReferralResponseDto } from '../../types/referral';
import { AiAnalysisResponse } from '../../types/ai';
import { Header } from '../../components/common/Header';
import { Card } from '../../components/common/Card';
import { Button } from '../../components/common/Button';
import { StatusBadge } from '../../components/common/StatusBadge';
import { LoadingState } from '../../components/common/LoadingState';
import { EmptyState } from '../../components/common/EmptyState';
import { ErrorState } from '../../components/common/ErrorState';
import { EmergencySosButton } from '../../components/common/EmergencySosButton';
import { DemoModeBanner } from '../../components/common/DemoModeBanner';
import { IS_DEMO_MODE } from '../../config/apiConfig';

const SYMPTOM_CATALOG = [
  'High Fever',
  'Persistent Cough',
  'Shortness of Breath',
  'Chest Pain',
  'Severe Headache',
  'Abdominal Pain',
  'Vomiting / Nausea',
  'Diarrhea',
  'Joint Pain / Body Ache',
  'Skin Rash / Lesions',
];

const TIME_SLOTS = [
  'Morning (09:30 AM)',
  'Afternoon (02:00 PM)',
  'Evening (05:00 PM)',
];

const parseTriageDisplay = (msg?: string | null) => {
  if (!msg) return null;
  const isTriage = msg.includes('[AI_TRIAGE]') || msg.includes('[MANUAL]');
  if (!isTriage && !msg.includes('|')) {
    return { rawText: msg };
  }
  const clean = msg.replace(/\[(AI_TRIAGE|MANUAL)\]/g, '').trim();
  const parts = clean.split('|').map((s) => s.trim()).filter(Boolean);
  const out: Record<string, string> = {};
  let labeled = false;
  for (const p of parts) {
    const idx = p.indexOf(':');
    if (idx > 0) {
      const k = p.slice(0, idx).trim();
      const v = p.slice(idx + 1).trim();
      out[k] = v;
      labeled = true;
    }
  }
  if (!labeled) {
    if (parts[0]) out['Slot'] = parts[0];
    if (parts[1]) out['Condition'] = parts[1];
    if (parts[2]) out['Symptoms'] = parts[2];
    if (parts[3]) out['History'] = parts[3];
    if (parts[4]) out['Precautions'] = parts[4];
    if (parts[5]) out['Notes'] = parts.slice(5).join(' | ');
  }
  return out;
};

function computeDistanceKm(lat1: number, lon1: number, lat2?: number, lon2?: number): number {
  if (lat2 == null || lon2 == null) return 9999;
  const R = 6371;
  const dLat = ((lat2 - lat1) * Math.PI) / 180;
  const dLon = ((lon2 - lon1) * Math.PI) / 180;
  const a =
    Math.sin(dLat / 2) * Math.sin(dLat / 2) +
    Math.cos((lat1 * Math.PI) / 180) * Math.cos((lat2 * Math.PI) / 180) * Math.sin(dLon / 2) * Math.sin(dLon / 2);
  const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
  return R * c;
}

export const ConsultationsScreen: React.FC = () => {
  const { user, selectedPatientId } = useAuth();
  const { colors } = useTheme();
  const { t } = useLanguage();

  // Tab mode: Video Doctor or Hospital Appointment
  const [careMode, setCareMode] = useState<'VIDEO' | 'HOSPITAL'>('VIDEO');

  // Video Consultations state
  const [consultations, setConsultations] = useState<ConsultationResponseDTO[]>([]);
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [usedDemoData, setUsedDemoData] = useState(false);

  // Video Doctor Booking Modal
  const [isBookingModalOpen, setIsBookingModalOpen] = useState(false);
  const [availableDoctors, setAvailableDoctors] = useState<AvailableDoctorResponseDTO[]>([]);
  const [selectedDoctorId, setSelectedDoctorId] = useState<number | null>(null);
  const [diseaseComplaint, setDiseaseComplaint] = useState('');
  const [urgency, setUrgency] = useState<ConsultationUrgency>('MEDIUM');
  const [consultationType, setConsultationType] = useState<ConsultationType>('VIDEO');
  const [isBooking, setIsBooking] = useState(false);

  // Hospital Appointments state
  const [hospitalAppointments, setHospitalAppointments] = useState<ReferralResponseDto[]>([]);
  const [isHospitalModalOpen, setIsHospitalModalOpen] = useState(false);
  const [selectedSymptoms, setSelectedSymptoms] = useState<string[]>(['High Fever']);
  const [customSymptom, setCustomSymptom] = useState('');
  const [extraNotes, setExtraNotes] = useState('');

  // AI Triage & Hospital Appointment state
  const [bookingTab, setBookingTab] = useState<'AI' | 'SEARCH'>('AI');
  const [hospitalSearchQuery, setHospitalSearchQuery] = useState('');
  const [hospitalTypeFilter, setHospitalTypeFilter] = useState<string>('ALL');
  const [searchFacilities, setSearchFacilities] = useState<FacilityRecommendationDto[]>([]);
  const [loadingSearchFacilities, setLoadingSearchFacilities] = useState(false);
  const [manualReason, setManualReason] = useState('');
  const [isAnalyzingAi, setIsAnalyzingAi] = useState(false);
  const [triageCaseId, setTriageCaseId] = useState<string | null>(null);
  const [aiResult, setAiResult] = useState<AiAnalysisResponse | null>(null);
  const [selectedFacilityId, setSelectedFacilityId] = useState<string | null>(null);
  const [selectedSlot, setSelectedSlot] = useState<string>(TIME_SLOTS[0]);
  const [isBookingHospital, setIsBookingHospital] = useState(false);

  const getUserCoordinates = useCallback(async (): Promise<{ lat: number; lon: number; isDeviceGps: boolean }> => {
    try {
      const { status } = await Location.requestForegroundPermissionsAsync();
      if (status === 'granted') {
        let loc: Location.LocationObject | null = null;
        try {
          loc = await Location.getCurrentPositionAsync({ accuracy: Location.Accuracy.Balanced });
        } catch {
          loc = null;
        }
        if (!loc || !loc.coords) {
          loc = await Location.getLastKnownPositionAsync();
        }
        if (loc && loc.coords) {
          return { lat: loc.coords.latitude, lon: loc.coords.longitude, isDeviceGps: true };
        }
      }
    } catch (e) {
      console.warn('GPS location lookup error, falling back to profile coords:', e);
    }
    // Fallback to user location or default coordinates
    const lat = user?.latitude ?? 18.6750;
    const lon = user?.longitude ?? 73.8980;
    return { lat, lon, isDeviceGps: false };
  }, [user?.latitude, user?.longitude]);

  const fetchSearchFacilities = useCallback(async () => {
    setLoadingSearchFacilities(true);
    try {
      const coords = await getUserCoordinates();
      const res = await facilityApi.searchNearbyFacilities({
        latitude: coords.lat,
        longitude: coords.lon,
        radiusKm: 150,
        limit: 30,
      });
      if (Array.isArray(res) && res.length > 0) {
        const withDist = res.map((f) => ({
          ...f,
          distanceKm: f.distanceKm ?? computeDistanceKm(coords.lat, coords.lon, f.latitude, f.longitude),
        }));
        const sorted = withDist.sort((a, b) => (a.distanceKm ?? 0) - (b.distanceKm ?? 0));
        setSearchFacilities(sorted);
        if (!selectedFacilityId) setSelectedFacilityId(sorted[0].facilityId);
      } else {
        setSearchFacilities([]);
      }
    } catch (err) {
      console.warn('Failed to fetch facilities for search:', err);
      setSearchFacilities([]);
    } finally {
      setLoadingSearchFacilities(false);
    }
  }, [getUserCoordinates, selectedFacilityId]);

  const patientId = selectedPatientId || user?.patientId || user?.associatedEntityId || user?.id || '';

  const stripDemoPrefix = (name?: string) => (name || '').replace(/\[DEMO\]\s*/i, '').trim();

  const fetchConsultations = useCallback(async (silent = false) => {
    if (!silent) setError(null);
    setUsedDemoData(false);
    try {
      const data = await consultationApi.getConsultationsByUserId(patientId);
      setConsultations(Array.isArray(data) ? data : []);
    } catch (err: any) {
      console.error('Failed to load consultations:', err);
      if (!silent) {
        setError(err.message || 'Unable to connect to consultation service.');
      }
      if (IS_DEMO_MODE) {
        setUsedDemoData(true);
        setConsultations([
          {
            consultationId: 101,
            id: 101,
            userId: patientId,
            username: user?.displayName || 'Ramesh Patil',
            doctorId: 1,
            doctorName: 'Dr. Ananya Sharma',
            disease: 'Persistent fever and chills for 3 days',
            urgency: 'HIGH',
            consultationType: 'VIDEO',
            status: 'READY',
            meetingUrl: 'https://meet.jit.si/AarogyaSeva_Consultation_101',
            requestedAt: new Date(Date.now() - 3600000).toISOString(),
          },
        ]);
      }
    } finally {
      setLoading(false);
      setRefreshing(false);
    }
  }, [patientId, user?.displayName]);

  const fetchHospitalAppointments = useCallback(async () => {
    if (!patientId) return;
    try {
      const list = await referralApi.getReferralsByPatientId(patientId);
      setHospitalAppointments(Array.isArray(list) ? list : []);
    } catch (err: any) {
      console.warn('Failed to load patient hospital referrals:', err?.message);
    }
  }, [patientId]);

  useEffect(() => {
    fetchConsultations(false);
    fetchHospitalAppointments();
    const interval = setInterval(() => {
      fetchConsultations(true);
      fetchHospitalAppointments();
    }, 12000);
    return () => clearInterval(interval);
  }, [fetchConsultations, fetchHospitalAppointments]);

  const onRefresh = () => {
    setRefreshing(true);
    fetchConsultations(false);
    fetchHospitalAppointments();
  };

  const handleOpenBooking = async () => {
    setIsBookingModalOpen(true);
    try {
      const allDocs = await doctorApi.getAllDoctors();
      if (Array.isArray(allDocs) && allDocs.length > 0) {
        setAvailableDoctors(allDocs as AvailableDoctorResponseDTO[]);
        setSelectedDoctorId(allDocs[0].id);
      } else {
        const available = await doctorApi.getAvailableDoctors();
        setAvailableDoctors(available || []);
        if (available && available.length > 0) {
          setSelectedDoctorId(available[0].id || (available[0] as any).doctorId);
        }
      }
    } catch (err: any) {
      console.error('Failed to load available doctors:', err);
      Alert.alert('Doctor Directory Notice', err?.message || 'Unable to load doctors.');
      setAvailableDoctors([]);
    }
  };

  const handleCreateConsultation = async () => {
    if (!diseaseComplaint.trim()) {
      Alert.alert('Required', 'Please describe your chief symptoms or medical concern.');
      return;
    }
    if (!selectedDoctorId) {
      Alert.alert('Select Doctor', 'Please select a doctor for your consultation.');
      return;
    }

    const docId = selectedDoctorId;
    setIsBooking(true);
    try {
      const newConsult = await consultationApi.createConsultation({
        userId: patientId,
        username: user?.displayName || user?.fullName || 'Patient',
        doctorId: docId,
        disease: diseaseComplaint.trim(),
        urgency,
        consultationType,
      });

      setConsultations((prev) => [newConsult, ...prev]);
      setIsBookingModalOpen(false);
      setDiseaseComplaint('');
      Alert.alert('Success', `Consultation #${newConsult.consultationId || newConsult.id} requested! Status: ${newConsult.status}`);
    } catch (err: any) {
      console.error('Create consultation failed:', err);
      if (IS_DEMO_MODE) {
        setUsedDemoData(true);
        const mockCreated: ConsultationResponseDTO = {
          consultationId: Date.now(),
          id: Date.now(),
          userId: patientId,
          username: user?.displayName || 'Patient',
          doctorId: docId,
          doctorName: availableDoctors.find((d) => d.id === docId)?.name || 'Doctor',
          disease: diseaseComplaint.trim(),
          urgency,
          consultationType,
          status: 'PENDING',
          requestedAt: new Date().toISOString(),
        };
        setConsultations((prev) => [mockCreated, ...prev]);
        setIsBookingModalOpen(false);
        setDiseaseComplaint('');
      } else {
        Alert.alert('Consultation Request Failed', err?.message || 'Unable to create consultation request.');
      }
    } finally {
      setIsBooking(false);
    }
  };

  const handleJoinCall = async (item: ConsultationResponseDTO) => {
    let callUrl = item.meetingUrl || (item.roomId ? `https://meet.jit.si/${item.roomId}` : '');
    if (!callUrl) {
      try {
        const cid = item.consultationId || item.id || 0;
        const session = await communicationApi.getSessionByConsultationId(cid);
        if (session?.meetingUrl) {
          callUrl = session.meetingUrl;
        } else if (session?.roomId) {
          callUrl = `https://meet.jit.si/${session.roomId}`;
        }
      } catch (err: any) {
        console.warn('Could not fetch session by consultationId:', err?.message);
      }
    }
    if (!callUrl) {
      callUrl = `https://meet.jit.si/AarogyaSeva_Consultation_${item.consultationId || item.id}`;
    }
    Linking.openURL(callUrl).catch(() => {
      Alert.alert('Error', 'Unable to open video call URL.');
    });
  };

  // Toggle symptom chip
  const toggleSymptom = (sym: string) => {
    if (selectedSymptoms.includes(sym)) {
      if (selectedSymptoms.length > 1) {
        setSelectedSymptoms(selectedSymptoms.filter((s) => s !== sym));
      }
    } else {
      setSelectedSymptoms([...selectedSymptoms, sym]);
    }
  };

  const handleAddCustomSymptom = () => {
    if (!customSymptom.trim()) return;
    if (!selectedSymptoms.includes(customSymptom.trim())) {
      setSelectedSymptoms([...selectedSymptoms, customSymptom.trim()]);
    }
    setCustomSymptom('');
  };

  // Run AI Triage
  const handleRunAiTriage = async () => {
    if (selectedSymptoms.length === 0 && !customSymptom.trim()) {
      Alert.alert('Symptoms Required', 'Please select at least one symptom or type a symptom.');
      return;
    }
    setIsAnalyzingAi(true);
    setAiResult(null);
    try {
      const allSymptoms = [...selectedSymptoms];
      if (customSymptom.trim() && !allSymptoms.includes(customSymptom.trim())) {
        allSymptoms.push(customSymptom.trim());
      }

      const coords = await getUserCoordinates();
      const complaint = allSymptoms.join(', ') + (extraNotes.trim() ? ` - ${extraNotes.trim()}` : '');
      const newCase = await caseApi.createCase({
        patientId,
        intakeChannel: 'FORM',
        chiefComplaint: complaint.slice(0, 250),
        latitude: coords.lat,
        longitude: coords.lon,
        initialSymptoms: allSymptoms.map((s) => ({
          symptomName: s,
          severity: 'MODERATE',
          durationDays: 2,
        })),
      });

      const caseId = newCase.id;
      setTriageCaseId(caseId);

      // Record to symptoms catalog for AI service query
      try {
        await symptomApi.addSymptomsBatch(caseId, {
          symptoms: allSymptoms.map((s) => ({
            symptomName: s,
            severity: 'MODERATE',
            durationDays: 2,
          })),
        });
      } catch {
        // continue
      }

      const aiRes = await aiApi.runAiAnalysis(caseId);
      const rawFacilities = aiRes.recommendedFacilities || [];
      const withDist = rawFacilities.map((f: any) => ({
        ...f,
        distanceKm: f.distanceKm ?? computeDistanceKm(coords.lat, coords.lon, f.latitude, f.longitude),
      }));
      const sortedFacilities = withDist.sort(
        (a: any, b: any) => (a.distanceKm || 0) - (b.distanceKm || 0)
      );
      setAiResult({
        ...aiRes,
        recommendedFacilities: sortedFacilities,
      });
      if (sortedFacilities.length > 0) {
        setSelectedFacilityId(sortedFacilities[0].facilityId);
      }
    } catch (err: any) {
      console.error('AI Triage error:', err);
      Alert.alert('AI Triage Error', err?.message || 'Could not complete clinical triage analysis.');
    } finally {
      setIsAnalyzingAi(false);
    }
  };

  // Confirm Hospital Appointment
  const handleConfirmHospitalAppointment = async () => {
    if (!selectedFacilityId) {
      Alert.alert('Hospital Required', 'Please choose a hospital for your appointment.');
      return;
    }

    setIsBookingHospital(true);
    try {
      let effectiveCaseId = triageCaseId;
      if (!effectiveCaseId) {
        // Create clinical case record for manual booking flow
        const coords = await getUserCoordinates();
        const complaint = manualReason.trim() || selectedSymptoms.join(', ') || 'General Outpatient Examination';
        const newCase = await caseApi.createCase({
          patientId,
          intakeChannel: 'PATIENT_PORTAL',
          chiefComplaint: complaint,
          notes: extraNotes ? `Manual appointment booking • ${extraNotes}` : 'Direct manual patient appointment booking',
          latitude: coords.lat,
          longitude: coords.lon,
          initialSymptoms: [
            {
              symptomName: complaint.slice(0, 50),
              severity: 'MODERATE',
              durationDays: 1,
            },
          ],
        });
        effectiveCaseId = newCase.id;

        // Record symptoms and generate AI clinical assessment required by backend referral service
        try {
          await symptomApi.addSymptomsBatch(effectiveCaseId, {
            symptoms: [
              {
                symptomName: complaint.slice(0, 50),
                severity: 'MODERATE',
                durationDays: 1,
              },
            ],
          });
        } catch {
          // ignore if batch was captured via initialSymptoms
        }
        try {
          await aiApi.runAiAnalysis(effectiveCaseId);
        } catch (aiErr) {
          console.warn('Auto AI screening during manual booking:', aiErr);
        }
      }

      const diseaseName = aiResult?.possibleDiseases?.[0]?.category || manualReason.trim() || 'General Clinical Consultation';
      const urgency = (aiResult?.riskAssessment?.riskLevel as any) || (aiResult?.urgency?.level as any) || 'MEDIUM';
      const sourceTag = bookingTab === 'SEARCH' ? '[MANUAL]' : '[AI_TRIAGE]';
      const symptomsSummary = selectedSymptoms.join(', ');
      const precautionsSummary = (aiResult?.precautions || []).join('; ');
      const historySummary = user?.medicalHistory || 'None';

      const message = `${sourceTag} Slot: ${selectedSlot} | Condition: ${diseaseName} (${urgency}) | Symptoms: ${symptomsSummary} | History: ${historySummary}${precautionsSummary ? ' | Precautions: ' + precautionsSummary : ''}${extraNotes.trim() ? ' | Notes: ' + extraNotes.trim() : ''}`;

      const createdRef = await referralApi.createReferral({
        caseId: effectiveCaseId,
        patientId,
        facilityId: selectedFacilityId,
        urgencyLevel: urgency,
        reason: diseaseName,
        patientMessage: message,
      });

      Alert.alert(
        t('appointmentConfirmed'),
        `Appointment #${createdRef.referralToken || createdRef.id.slice(0, 8)} confirmed for ${selectedSlot}.\n\nFacility notified. View status under Hospital Appointments.`
      );
      setIsHospitalModalOpen(false);
      setAiResult(null);
      setTriageCaseId(null);
      setSelectedSymptoms(['High Fever']);
      setCustomSymptom('');
      setExtraNotes('');
      setManualReason('');
      await fetchHospitalAppointments();
    } catch (err: any) {
      console.error('Booking hospital appointment error:', err);
      Alert.alert('Booking Error', err?.message || 'Failed to confirm hospital appointment.');
    } finally {
      setIsBookingHospital(false);
    }
  };

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

        {/* Option Selector: Video Doctor vs Hospital Appointment */}
        <View style={[styles.tabSelectorContainer, { backgroundColor: colors.bgSubtle, borderColor: colors.borderLight }]}>
          <TouchableOpacity
            style={[
              styles.tabSelectorBtn,
              careMode === 'VIDEO' && { backgroundColor: colors.bgSurface, elevation: 2 },
            ]}
            onPress={() => setCareMode('VIDEO')}
          >
            <Ionicons
              name="videocam-outline"
              size={18}
              color={careMode === 'VIDEO' ? colors.primary : colors.textSubtle}
            />
            <Text
              style={[
                styles.tabSelectorText,
                { color: careMode === 'VIDEO' ? colors.primary : colors.textSubtle, fontWeight: careMode === 'VIDEO' ? '800' : '600' },
              ]}
            >
              {t('videoDoctorOption')}
            </Text>
          </TouchableOpacity>

          <TouchableOpacity
            style={[
              styles.tabSelectorBtn,
              careMode === 'HOSPITAL' && { backgroundColor: colors.bgSurface, elevation: 2 },
            ]}
            onPress={() => setCareMode('HOSPITAL')}
          >
            <Ionicons
              name="business-outline"
              size={18}
              color={careMode === 'HOSPITAL' ? colors.primary : colors.textSubtle}
            />
            <Text
              style={[
                styles.tabSelectorText,
                { color: careMode === 'HOSPITAL' ? colors.primary : colors.textSubtle, fontWeight: careMode === 'HOSPITAL' ? '800' : '600' },
              ]}
            >
              {t('hospitalAppointmentOption')}
            </Text>
          </TouchableOpacity>
        </View>

        {careMode === 'VIDEO' ? (
          <>
            <View style={styles.topRow}>
              <View>
                <Text style={[styles.heading, { color: colors.textMain }]}>{t('teleconsultations')}</Text>
                <Text style={[styles.subheading, { color: colors.textSubtle }]}>
                  Connect with government doctors & specialists
                </Text>
              </View>

              <Button
                title={t('requestDoctorBtn')}
                onPress={handleOpenBooking}
                variant="primary"
                style={styles.bookBtn}
              />
            </View>

            {error ? <ErrorState message={error} onRetry={() => fetchConsultations(false)} /> : null}

            {loading ? (
              <LoadingState message="Loading your consultation appointments..." />
            ) : consultations.length === 0 ? (
              <EmptyState
                icon="medkit-outline"
                title={t('noConsultHistory')}
                description={t('noConsultDesc')}
                actionTitle={t('bookFirstConsult')}
                onAction={handleOpenBooking}
              />
            ) : (
              consultations.map((item) => {
                const isReadyForCall =
                  item.status === 'READY' ||
                  item.status === 'IN_CALL' ||
                  item.status === 'ACCEPTED';

                return (
                  <Card key={item.consultationId || item.id} style={styles.consultCard}>
                    <View style={styles.cardHeader}>
                      <View style={{ flex: 1 }}>
                        <Text style={[styles.doctorTitle, { color: colors.textMain }]}>
                          {item.doctorName || `Doctor #${item.doctorId}`} (ID #{item.consultationId || item.id})
                        </Text>
                        <Text style={[styles.consultType, { color: colors.textSubtle }]}>
                          {item.consultationType} Consultation • {item.urgency} Urgency
                        </Text>
                      </View>
                      <StatusBadge status={item.status} />
                    </View>

                    <View style={[styles.complaintBox, { backgroundColor: colors.bgSubtle }]}>
                      <Text style={[styles.complaintLabel, { color: colors.textSubtle }]}>{t('chiefComplaint')}:</Text>
                      <Text style={[styles.complaintText, { color: colors.textMain }]}>{item.disease}</Text>
                    </View>

                    {/* Join Call Action */}
                    {isReadyForCall ? (
                      <View style={styles.callActionRow}>
                        <Button
                          title={t('joinVideoCall')}
                          onPress={() => handleJoinCall(item)}
                          variant="secondary"
                          style={styles.joinCallBtn}
                        />
                      </View>
                    ) : null}
                  </Card>
                );
              })
            )}
          </>
        ) : (
          /* HOSPITAL APPOINTMENTS TAB */
          <>
            <View style={styles.topRow}>
              <View>
                <Text style={[styles.heading, { color: colors.textMain }]}>{t('hospitalAppointments')}</Text>
                <Text style={[styles.subheading, { color: colors.textSubtle }]}>
                  AI-matched clinical referrals and slots
                </Text>
              </View>

              <Button
                title={t('bookHospitalVisitBtn')}
                onPress={() => {
                  setAiResult(null);
                  setTriageCaseId(null);
                  setIsHospitalModalOpen(true);
                }}
                variant="primary"
                style={styles.bookBtn}
              />
            </View>

            {hospitalAppointments.length === 0 ? (
              <EmptyState
                icon="business-outline"
                title="No Hospital Appointments"
                description="Book a hospital appointment with instant AI clinical triage, matched facilities, and preferred time slots."
                actionTitle="+ Book Hospital Appointment"
                onAction={() => {
                  setAiResult(null);
                  setTriageCaseId(null);
                  setIsHospitalModalOpen(true);
                }}
              />
            ) : (
              hospitalAppointments.map((appt) => (
                <Card key={appt.id} style={styles.consultCard}>
                  <View style={styles.cardHeader}>
                    <View style={{ flex: 1, paddingRight: 8 }}>
                      <Text style={[styles.doctorTitle, { color: colors.textMain }]}>
                        {appt.facilityName || 'Healthcare Facility'}
                      </Text>
                      <Text style={[styles.consultType, { color: colors.textSubtle }]}>
                        Referral #{appt.referralToken || appt.id.slice(0, 8)} • Urgency: {appt.urgencyLevel || 'MEDIUM'}
                      </Text>
                    </View>
                    <StatusBadge status={appt.status} />
                  </View>

                  <View style={[styles.complaintBox, { backgroundColor: colors.bgSubtle }]}>
                    <Text style={[styles.complaintLabel, { color: colors.textSubtle }]}>
                      Clinical Reason:
                    </Text>
                    <Text style={[styles.complaintText, { color: colors.textMain, flexWrap: 'wrap', flexShrink: 1 }]}>
                      {appt.reason || 'Medical Examination'}
                    </Text>
                    {(() => {
                      const parsed = parseTriageDisplay(appt.patientMessage);
                      if (!parsed) return null;
                      if (parsed.rawText) {
                        return (
                          <Text style={{ fontSize: 12, color: colors.primary, marginTop: 4, flexWrap: 'wrap', flexShrink: 1 }}>
                            {parsed.rawText}
                          </Text>
                        );
                      }
                      return (
                        <View style={{ marginTop: 6, gap: 2 }}>
                          {Object.entries(parsed).map(([k, v]) => (
                            <View key={k} style={{ flexDirection: 'row', flexWrap: 'wrap', alignItems: 'flex-start' }}>
                              <Text style={{ fontSize: 11, fontWeight: '700', color: colors.textMuted, width: 85 }}>{k}:</Text>
                              <Text style={{ fontSize: 12, color: colors.textMain, flex: 1, flexWrap: 'wrap', flexShrink: 1 }}>{v}</Text>
                            </View>
                          ))}
                        </View>
                      );
                    })()}
                    {appt.facilityResponse ? (
                      <Text style={{ fontSize: 12, color: '#059669', marginTop: 4, fontWeight: '600', flexWrap: 'wrap', flexShrink: 1 }}>
                        Facility Response: {appt.facilityResponse}
                      </Text>
                    ) : null}
                  </View>
                </Card>
              ))
            )}
          </>
        )}
      </ScrollView>

      {/* Book Video Doctor Modal */}
      <Modal visible={isBookingModalOpen} transparent animationType="slide" onRequestClose={() => setIsBookingModalOpen(false)}>
        <View style={styles.modalBackdrop}>
          <View style={[styles.modalCard, { backgroundColor: colors.bgSurface }]}>
            <ScrollView contentContainerStyle={{ paddingBottom: 20 }}>
              <View style={styles.modalHeader}>
                <Text style={[styles.modalTitle, { color: colors.textMain }]}>Book Teleconsultation</Text>
                <TouchableOpacity onPress={() => setIsBookingModalOpen(false)}>
                  <Ionicons name="close" size={22} color={colors.textSubtle} />
                </TouchableOpacity>
              </View>

              {/* Select Doctor */}
              <Text style={[styles.formLabel, { color: colors.textMuted }]}>Choose Doctor:</Text>
              {availableDoctors.map((doc) => (
                <TouchableOpacity
                  key={doc.id}
                  style={[
                    styles.docSelectCard,
                    {
                      backgroundColor: selectedDoctorId === doc.id ? colors.primaryLight : colors.bgSubtle,
                      borderColor: selectedDoctorId === doc.id ? colors.primary : colors.borderLight,
                    },
                  ]}
                  onPress={() => setSelectedDoctorId(doc.id)}
                >
                  <View>
                    <Text style={[styles.docSelectName, { color: colors.textMain }]}>{doc.name}</Text>
                    <Text style={[styles.docSelectSpec, { color: colors.textSubtle }]}>{doc.specialization}</Text>
                  </View>
                  {selectedDoctorId === doc.id && <Ionicons name="checkmark-circle" size={18} color={colors.primary} />}
                </TouchableOpacity>
              ))}

              {/* Symptoms Description */}
              <Text style={[styles.formLabel, { color: colors.textMuted, marginTop: 12 }]}>
                What symptoms are you experiencing?
              </Text>
              <TextInput
                style={[styles.textArea, { backgroundColor: colors.bgSubtle, borderColor: colors.borderLight, color: colors.textMain }]}
                placeholder="e.g. High fever, dry cough, dizziness for past 2 days"
                placeholderTextColor={colors.textSubtle}
                multiline
                numberOfLines={3}
                value={diseaseComplaint}
                onChangeText={setDiseaseComplaint}
              />

              {/* Urgency Selection */}
              <Text style={[styles.formLabel, { color: colors.textMuted, marginTop: 12 }]}>Urgency Level:</Text>
              <View style={styles.pillRow}>
                {(['LOW', 'MEDIUM', 'HIGH', 'EMERGENCY'] as ConsultationUrgency[]).map((u) => (
                  <TouchableOpacity
                    key={u}
                    style={[
                      styles.pill,
                      {
                        backgroundColor: urgency === u ? colors.primaryLight : colors.bgSubtle,
                        borderColor: urgency === u ? colors.primary : colors.borderLight,
                      },
                    ]}
                    onPress={() => setUrgency(u)}
                  >
                    <Text style={[styles.pillText, { color: urgency === u ? colors.primary : colors.textMain }]}>{u}</Text>
                  </TouchableOpacity>
                ))}
              </View>

              {/* Consultation Type */}
              <Text style={[styles.formLabel, { color: colors.textMuted, marginTop: 12 }]}>Mode of Care:</Text>
              <View style={styles.pillRow}>
                {(['VIDEO', 'AUDIO', 'CHAT'] as ConsultationType[]).map((m) => (
                  <TouchableOpacity
                    key={m}
                    style={[
                      styles.pill,
                      {
                        backgroundColor: consultationType === m ? colors.primaryLight : colors.bgSubtle,
                        borderColor: consultationType === m ? colors.primary : colors.borderLight,
                      },
                    ]}
                    onPress={() => setConsultationType(m)}
                  >
                    <Text style={[styles.pillText, { color: consultationType === m ? colors.primary : colors.textMain }]}>{m}</Text>
                  </TouchableOpacity>
                ))}
              </View>

              <Button
                title={isBooking ? 'Submitting...' : 'Confirm Appointment'}
                onPress={handleCreateConsultation}
                loading={isBooking}
                style={{ marginTop: 20 }}
              />
            </ScrollView>
          </View>
        </View>
      </Modal>

      {/* Book Hospital Appointment Modal with AI Triage */}
      <Modal visible={isHospitalModalOpen} transparent animationType="slide" onRequestClose={() => setIsHospitalModalOpen(false)}>
        <View style={styles.modalBackdrop}>
          <View style={[styles.modalCard, { backgroundColor: colors.bgSurface, maxHeight: '90%' }]}>
            <ScrollView contentContainerStyle={{ paddingBottom: 24 }}>
              <View style={styles.modalHeader}>
                <View>
                  <Text style={[styles.modalTitle, { color: colors.textMain }]}>Hospital Appointment</Text>
                  <Text style={{ fontSize: 12, color: colors.textSubtle }}>
                    AI Clinical Triage & Nearby Facility Matching
                  </Text>
                </View>
                <TouchableOpacity onPress={() => setIsHospitalModalOpen(false)}>
                  <Ionicons name="close" size={22} color={colors.textSubtle} />
                </TouchableOpacity>
              </View>

              {/* Segmented Control: Suggested by AI vs Search hospitals */}
              <View style={[styles.tabSelectorContainer, { backgroundColor: colors.bgSubtle, borderColor: colors.borderLight, marginVertical: 12 }]}>
                <TouchableOpacity
                  style={[
                    styles.tabSelectorBtn,
                    bookingTab === 'AI' && { backgroundColor: colors.bgSurface, elevation: 2 },
                  ]}
                  onPress={() => setBookingTab('AI')}
                >
                  <Ionicons
                    name="sparkles-outline"
                    size={16}
                    color={bookingTab === 'AI' ? colors.primary : colors.textSubtle}
                  />
                  <Text
                    style={[
                      styles.tabSelectorText,
                      { color: bookingTab === 'AI' ? colors.primary : colors.textSubtle, fontWeight: bookingTab === 'AI' ? '700' : '500' },
                    ]}
                  >
                    {t('aiTriageTab')}
                  </Text>
                </TouchableOpacity>

                <TouchableOpacity
                  style={[
                    styles.tabSelectorBtn,
                    bookingTab === 'SEARCH' && { backgroundColor: colors.bgSurface, elevation: 2 },
                  ]}
                  onPress={() => {
                    setBookingTab('SEARCH');
                    if (searchFacilities.length === 0) {
                      fetchSearchFacilities();
                    }
                  }}
                >
                  <Ionicons
                    name="search-outline"
                    size={16}
                    color={bookingTab === 'SEARCH' ? colors.primary : colors.textSubtle}
                  />
                  <Text
                    style={[
                      styles.tabSelectorText,
                      { color: bookingTab === 'SEARCH' ? colors.primary : colors.textSubtle, fontWeight: bookingTab === 'SEARCH' ? '700' : '500' },
                    ]}
                  >
                    {t('searchHospitals')}
                  </Text>
                </TouchableOpacity>
              </View>

              {bookingTab === 'SEARCH' ? (
                /* MANUAL HOSPITAL SEARCH TAB */
                <View style={{ marginTop: 4 }}>
                  <Text style={[styles.formLabel, { color: colors.textMuted }]}>Search Hospital by Name or Town:</Text>
                  <TextInput
                    style={[styles.customInput, { backgroundColor: colors.bgSubtle, borderColor: colors.borderLight, color: colors.textMain, marginBottom: 8 }]}
                    placeholder="e.g. Pune City, Rampur, District Hospital..."
                    placeholderTextColor={colors.textSubtle}
                    value={hospitalSearchQuery}
                    onChangeText={setHospitalSearchQuery}
                  />

                  {/* Filter chips: All, PHC, CHC, Hospital */}
                  <View style={{ flexDirection: 'row', gap: 6, marginBottom: 10 }}>
                    {['ALL', 'PHC', 'CHC', 'DISTRICT_HOSPITAL'].map((typeCode) => {
                      const isSel = hospitalTypeFilter === typeCode;
                      return (
                        <TouchableOpacity
                          key={typeCode}
                          style={{
                            backgroundColor: isSel ? colors.primaryLight : colors.bgSubtle,
                            borderColor: isSel ? colors.primary : colors.borderLight,
                            borderWidth: 1,
                            paddingHorizontal: 10,
                            paddingVertical: 5,
                            borderRadius: 14,
                          }}
                          onPress={() => setHospitalTypeFilter(typeCode)}
                        >
                          <Text style={{ fontSize: 11, fontWeight: isSel ? '700' : '500', color: isSel ? colors.primary : colors.textMain }}>
                            {typeCode === 'ALL' ? 'All Types' : typeCode.replace(/_/g, ' ')}
                          </Text>
                        </TouchableOpacity>
                      );
                    })}
                  </View>

                  {/* Facilities list sorted by distance */}
                  <Text style={[styles.formLabel, { color: colors.textMuted }]}>Select Facility (Nearest First):</Text>
                  {loadingSearchFacilities ? (
                    <ActivityIndicator size="small" color={colors.primary} style={{ marginVertical: 14 }} />
                  ) : (
                    (() => {
                      const filtered = searchFacilities
                        .filter((fac) => {
                          const q = hospitalSearchQuery.toLowerCase();
                          const matchesQuery =
                            !q ||
                            fac.name.toLowerCase().includes(q) ||
                            (fac.villageTown && fac.villageTown.toLowerCase().includes(q)) ||
                            (fac.district && fac.district.toLowerCase().includes(q));
                          const matchesType =
                            hospitalTypeFilter === 'ALL' || fac.facilityType === hospitalTypeFilter;
                          return matchesQuery && matchesType;
                        })
                        .sort((a, b) => (a.distanceKm ?? 0) - (b.distanceKm ?? 0));

                      if (filtered.length === 0) {
                        return (
                          <View style={{ padding: 12, backgroundColor: colors.bgSubtle, borderRadius: 8, borderWidth: 1, borderColor: colors.borderLight, marginVertical: 8 }}>
                            <Text style={{ fontSize: 13, color: colors.textSubtle, textAlign: 'center' }}>
                              No healthcare facilities found within search radius. Try adjusting filters or search term.
                            </Text>
                          </View>
                        );
                      }

                      return (
                        <View style={{ maxHeight: 220, marginBottom: 12 }}>
                          <ScrollView nestedScrollEnabled>
                            {filtered.map((fac) => {
                              const isSelected = selectedFacilityId === fac.facilityId;
                              return (
                                <TouchableOpacity
                                  key={fac.facilityId}
                                  style={[
                                    styles.docSelectCard,
                                    {
                                      backgroundColor: isSelected ? colors.primaryLight : colors.bgSubtle,
                                      borderColor: isSelected ? colors.primary : colors.borderLight,
                                    },
                                  ]}
                                  onPress={() => setSelectedFacilityId(fac.facilityId)}
                                >
                                  <View style={{ flex: 1 }}>
                                    <Text style={[styles.docSelectName, { color: colors.textMain }]}>{fac.name}</Text>
                                    <Text style={[styles.docSelectSpec, { color: colors.textSubtle }]}>
                                      {fac.facilityTypeDisplayName || fac.facilityType} • {fac.distanceKm !== undefined ? `${fac.distanceKm.toFixed(1)} km away` : ''} • {fac.villageTown || fac.district}
                                    </Text>
                                  </View>
                                  {isSelected && <Ionicons name="checkmark-circle" size={18} color={colors.primary} />}
                                </TouchableOpacity>
                              );
                            })}
                          </ScrollView>
                        </View>
                      );
                    })()
                  )}

                  {/* Reason for Appointment */}
                  <Text style={[styles.formLabel, { color: colors.textMuted, marginTop: 4 }]}>
                    {t('reasonForVisit')}:
                  </Text>
                  <TextInput
                    style={[styles.customInput, { backgroundColor: colors.bgSubtle, borderColor: colors.borderLight, color: colors.textMain, marginBottom: 10 }]}
                    placeholder="e.g. Chest discomfort, severe migraine, routine checkup"
                    placeholderTextColor={colors.textSubtle}
                    value={manualReason}
                    onChangeText={setManualReason}
                  />

                  {/* Preferred Slot */}
                  <Text style={[styles.formLabel, { color: colors.textMuted }]}>
                    {t('preferredTimeSlot')}:
                  </Text>
                  <View style={styles.pillRow}>
                    {TIME_SLOTS.map((slot) => (
                      <TouchableOpacity
                        key={slot}
                        style={[
                          styles.pill,
                          {
                            backgroundColor: selectedSlot === slot ? colors.primaryLight : colors.bgSubtle,
                            borderColor: selectedSlot === slot ? colors.primary : colors.borderLight,
                          },
                        ]}
                        onPress={() => setSelectedSlot(slot)}
                      >
                        <Text style={[styles.pillText, { color: selectedSlot === slot ? colors.primary : colors.textMain }]}>
                          {slot}
                        </Text>
                      </TouchableOpacity>
                    ))}
                  </View>

                  <Button
                    title={isBookingHospital ? 'Confirming...' : t('confirmAppointment')}
                    onPress={handleConfirmHospitalAppointment}
                    loading={isBookingHospital}
                    style={{ marginTop: 14 }}
                  />
                </View>
              ) : (
                /* AI CLINICAL TRIAGE TAB */
                <>
                  {/* STEP 1: SYMPTOMS SELECTION */}
                  <Text style={[styles.formLabel, { color: colors.textMuted }]}>
                    {t('selectSymptoms')}:
                  </Text>
                  <View style={styles.symptomGrid}>
                    {SYMPTOM_CATALOG.map((sym) => {
                      const isSelected = selectedSymptoms.includes(sym);
                      return (
                        <TouchableOpacity
                          key={sym}
                          style={[
                            styles.symptomChip,
                            {
                              backgroundColor: isSelected ? colors.primaryLight : colors.bgSubtle,
                              borderColor: isSelected ? colors.primary : colors.borderLight,
                            },
                          ]}
                          onPress={() => toggleSymptom(sym)}
                        >
                          <Ionicons
                            name={isSelected ? 'checkmark-circle' : 'add-circle-outline'}
                            size={15}
                            color={isSelected ? colors.primary : colors.textSubtle}
                          />
                          <Text
                            style={{
                              fontSize: 12,
                              fontWeight: isSelected ? '700' : '500',
                              color: isSelected ? colors.primary : colors.textMain,
                            }}
                          >
                            {sym}
                          </Text>
                        </TouchableOpacity>
                      );
                    })}
                  </View>

                  {/* Add Custom Symptom */}
                  <View style={styles.addCustomRow}>
                    <TextInput
                      style={[styles.customInput, { backgroundColor: colors.bgSubtle, borderColor: colors.borderLight, color: colors.textMain }]}
                      placeholder="Or type custom symptom..."
                      placeholderTextColor={colors.textSubtle}
                      value={customSymptom}
                      onChangeText={setCustomSymptom}
                    />
                    <TouchableOpacity
                      style={[styles.addCustomBtn, { backgroundColor: colors.primary }]}
                      onPress={handleAddCustomSymptom}
                    >
                      <Text style={{ color: '#fff', fontWeight: '700', fontSize: 13 }}>+ Add</Text>
                    </TouchableOpacity>
                  </View>

                  {/* Extra Notes */}
                  <Text style={[styles.formLabel, { color: colors.textMuted, marginTop: 12 }]}>
                    Additional Clinical Notes:
                  </Text>
                  <TextInput
                    style={[styles.textArea, { backgroundColor: colors.bgSubtle, borderColor: colors.borderLight, color: colors.textMain }]}
                    placeholder="Describe fever duration, SpO2, or specific discomfort..."
                    placeholderTextColor={colors.textSubtle}
                    multiline
                    numberOfLines={2}
                    value={extraNotes}
                    onChangeText={setExtraNotes}
                  />

                  {/* Run AI Triage Action */}
                  {!aiResult && (
                    <Button
                      title={isAnalyzingAi ? t('aiTriageAnalyzing') : 'Run AI Clinical Triage'}
                      onPress={handleRunAiTriage}
                      loading={isAnalyzingAi}
                      style={{ marginTop: 16 }}
                    />
                  )}

                  {/* STEP 2: AI TRIAGE RESULTS */}
                  {aiResult && (
                    <View style={styles.triageResultBox}>
                      <View style={[styles.aiHeaderCard, { backgroundColor: colors.bgSubtle, borderColor: colors.borderLight }]}>
                        <View style={{ flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between' }}>
                          <Text style={{ fontSize: 13, fontWeight: '800', color: colors.primary }}>
                            AI PREDICTED CONDITION
                          </Text>
                          <StatusBadge status={aiResult.riskAssessment?.riskLevel === 'HIGH' ? 'HIGH' : 'ACCEPTED'} />
                        </View>

                        <Text style={{ fontSize: 16, fontWeight: '800', color: colors.textMain, marginTop: 4 }}>
                          {aiResult.possibleDiseases?.[0]?.category || 'Acute Viral Infection'}
                        </Text>

                        <Text style={{ fontSize: 12, color: colors.textSubtle, marginTop: 2 }}>
                          Confidence: {Math.round((aiResult.confidence || 0.85) * 100)}% • Risk Score: {aiResult.riskAssessment?.riskScore || 25}/100
                        </Text>

                        {aiResult.precautions && aiResult.precautions.length > 0 && (
                          <View style={{ marginTop: 8, borderTopWidth: StyleSheet.hairlineWidth, borderTopColor: colors.borderLight, paddingTop: 6 }}>
                            <Text style={{ fontSize: 11, fontWeight: '700', color: colors.textSubtle }}>PRECAUTIONS:</Text>
                            <Text style={{ fontSize: 12, color: colors.textMain, marginTop: 2 }}>
                              • {aiResult.precautions.slice(0, 2).join('\n• ')}
                            </Text>
                          </View>
                        )}
                      </View>

                      {/* Choose Recommended Hospital */}
                      <Text style={[styles.formLabel, { color: colors.textMuted, marginTop: 14 }]}>
                        {t('chooseHospital')} (Nearest First):
                      </Text>
                      {(aiResult.recommendedFacilities && aiResult.recommendedFacilities.length > 0) ? (
                        aiResult.recommendedFacilities.map((fac: any, index: number) => {
                          const isSelected = selectedFacilityId === fac.facilityId;
                          return (
                            <TouchableOpacity
                              key={fac.facilityId}
                              style={[
                                styles.docSelectCard,
                                {
                                  backgroundColor: isSelected ? colors.primaryLight : colors.bgSubtle,
                                  borderColor: isSelected ? colors.primary : colors.borderLight,
                                },
                              ]}
                              onPress={() => setSelectedFacilityId(fac.facilityId)}
                            >
                              <View style={{ flex: 1 }}>
                                <View style={{ flexDirection: 'row', alignItems: 'center', gap: 6 }}>
                                  <Text style={[styles.docSelectName, { color: colors.textMain }]}>{fac.name}</Text>
                                  {index === 0 && (
                                    <View
                                      style={{
                                        backgroundColor: colors.primary,
                                        paddingHorizontal: 6,
                                        paddingVertical: 1,
                                        borderRadius: 4,
                                      }}
                                    >
                                      <Text style={{ color: '#fff', fontSize: 10, fontWeight: '700' }}>NEAREST</Text>
                                    </View>
                                  )}
                                </View>
                                <Text style={[styles.docSelectSpec, { color: colors.textSubtle }]}>
                                  {fac.facilityTypeDisplayName || 'Hospital'} • {fac.distanceKm != null ? `${fac.distanceKm.toFixed(1)} km away` : 'Nearby'}
                                </Text>
                              </View>
                              <Ionicons
                                name={isSelected ? "radio-button-on" : "radio-button-off"}
                                size={20}
                                color={isSelected ? colors.primary : colors.textSubtle}
                              />
                            </TouchableOpacity>
                          );
                        })
                      ) : (
                        <Text style={{ fontSize: 13, color: colors.textSubtle, marginVertical: 8 }}>
                          No operational hospitals found within 50 km. Try searching manually below.
                        </Text>
                      )}

                      {/* Slot Selection */}
                      <Text style={[styles.formLabel, { color: colors.textMuted, marginTop: 14 }]}>
                        Preferred Appointment Slot:
                      </Text>
                      <View style={styles.pillRow}>
                        {TIME_SLOTS.map((slot) => (
                          <TouchableOpacity
                            key={slot}
                            style={[
                              styles.pill,
                              {
                                backgroundColor: selectedSlot === slot ? colors.primaryLight : colors.bgSubtle,
                                borderColor: selectedSlot === slot ? colors.primary : colors.borderLight,
                              },
                            ]}
                            onPress={() => setSelectedSlot(slot)}
                          >
                            <Text style={[styles.pillText, { color: selectedSlot === slot ? colors.primary : colors.textMain }]}>
                              {slot}
                            </Text>
                          </TouchableOpacity>
                        ))}
                      </View>

                      <Button
                        title={isBookingHospital ? 'Confirming...' : t('bookAppointment')}
                        onPress={handleConfirmHospitalAppointment}
                        loading={isBookingHospital}
                        style={{ marginTop: 18 }}
                      />
                    </View>
                  )}
                </>
              )}
            </ScrollView>
          </View>
        </View>
      </Modal>

      <EmergencySosButton />
    </View>
  );
};

const styles = StyleSheet.create({
  container: {
    padding: 16,
    paddingBottom: 120,
  },
  tabSelectorContainer: {
    flexDirection: 'row',
    borderRadius: 12,
    borderWidth: 1,
    padding: 4,
    marginBottom: 16,
    gap: 4,
  },
  tabSelectorBtn: {
    flex: 1,
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    gap: 6,
    paddingVertical: 10,
    borderRadius: 8,
  },
  tabSelectorText: {
    fontSize: 13,
  },
  topRow: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    marginBottom: 16,
  },
  heading: {
    fontSize: 20,
    fontWeight: '800',
  },
  subheading: {
    fontSize: 12,
    marginTop: 2,
    maxWidth: 220,
  },
  bookBtn: {
    minHeight: 38,
    paddingVertical: 6,
    paddingHorizontal: 12,
  },
  consultCard: {
    padding: 14,
    marginBottom: 12,
  },
  cardHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'flex-start',
    marginBottom: 10,
  },
  doctorTitle: {
    fontSize: 16,
    fontWeight: '700',
  },
  consultType: {
    fontSize: 12,
    marginTop: 2,
  },
  complaintBox: {
    padding: 10,
    borderRadius: 8,
    marginTop: 4,
  },
  complaintLabel: {
    fontSize: 11,
    fontWeight: '700',
    textTransform: 'uppercase',
  },
  complaintText: {
    fontSize: 13,
    marginTop: 2,
  },
  callActionRow: {
    marginTop: 12,
  },
  joinCallBtn: {
    borderRadius: 10,
  },
  modalBackdrop: {
    flex: 1,
    backgroundColor: 'rgba(0,0,0,0.6)',
    justifyContent: 'flex-end',
  },
  modalCard: {
    borderTopLeftRadius: 24,
    borderTopRightRadius: 24,
    maxHeight: '88%',
    padding: 20,
  },
  modalHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: 16,
  },
  modalTitle: {
    fontSize: 18,
    fontWeight: '800',
  },
  formLabel: {
    fontSize: 13,
    fontWeight: '700',
    marginBottom: 6,
  },
  symptomGrid: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    gap: 8,
    marginBottom: 8,
  },
  symptomChip: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 4,
    paddingVertical: 6,
    paddingHorizontal: 10,
    borderRadius: 8,
    borderWidth: 1,
  },
  addCustomRow: {
    flexDirection: 'row',
    gap: 8,
    marginTop: 6,
  },
  customInput: {
    flex: 1,
    height: 42,
    borderRadius: 8,
    borderWidth: 1,
    paddingHorizontal: 10,
    fontSize: 13,
  },
  addCustomBtn: {
    paddingHorizontal: 14,
    justifyContent: 'center',
    alignItems: 'center',
    borderRadius: 8,
  },
  triageResultBox: {
    marginTop: 14,
  },
  aiHeaderCard: {
    padding: 12,
    borderRadius: 10,
    borderWidth: 1,
  },
  docSelectCard: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    padding: 10,
    borderRadius: 10,
    borderWidth: 1,
    marginBottom: 6,
  },
  docSelectName: {
    fontSize: 14,
    fontWeight: '700',
  },
  docSelectSpec: {
    fontSize: 12,
    marginTop: 2,
  },
  textArea: {
    borderRadius: 10,
    borderWidth: 1,
    padding: 12,
    fontSize: 14,
    minHeight: 60,
    textAlignVertical: 'top',
  },
  pillRow: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    gap: 8,
  },
  pill: {
    paddingVertical: 8,
    paddingHorizontal: 12,
    borderRadius: 8,
    borderWidth: 1,
  },
  pillText: {
    fontSize: 12,
    fontWeight: '700',
  },
});
