import React, { useState, useEffect, useCallback } from 'react';
import {
  View,
  Text,
  StyleSheet,
  ScrollView,
  RefreshControl,
  TextInput,
  TouchableOpacity,
  Modal,
  Alert,
  ActivityIndicator,
} from 'react-native';
import * as Location from 'expo-location';
import { useAuth } from '../../context/AuthContext';
import { useTheme } from '../../context/ThemeContext';
import { patientApi } from '../../api/patientApi';
import { caseApi } from '../../api/caseApi';
import { symptomApi } from '../../api/symptomApi';
import { aiApi } from '../../api/aiApi';
import { referralApi } from '../../api/referralApi';
import { facilityApi } from '../../api/facilityApi';
import { surveillanceApi } from '../../api/surveillanceApi';
import { PatientDto, Gender } from '../../types/patient';
import { HealthcareCaseDto } from '../../types/case';
import { SymptomSeverity, CreateSymptomRequest } from '../../types/symptom';
import { AiAnalysisResponse } from '../../types/ai';
import { FacilityRecommendationDto } from '../../types/facility';
import { OutbreakZoneDto } from '../../types/surveillance';
import { Header } from '../../components/common/Header';
import { Card } from '../../components/common/Card';
import { Button } from '../../components/common/Button';
import { StatusBadge } from '../../components/common/StatusBadge';
import { LoadingState } from '../../components/common/LoadingState';
import { ErrorState } from '../../components/common/ErrorState';
import { EmergencySosModal } from '../../components/common/EmergencySosModal';

type AshaTab = 'PATIENTS' | 'TRIAGE' | 'ZONES';

export const AshaPlaceholderScreen: React.FC = () => {
  const { user, setSelectedPatientId } = useAuth();
  const { colors } = useTheme();

  const [activeTab, setActiveTab] = useState<AshaTab>('PATIENTS');
  const [patients, setPatients] = useState<PatientDto[]>([]);
  const [searchQuery, setSearchQuery] = useState('');
  const [selectedPatient, setSelectedPatient] = useState<PatientDto | null>(null);
  const [patientCases, setPatientCases] = useState<HealthcareCaseDto[]>([]);
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [error, setError] = useState<string | null>(null);

  // Location
  const [coords, setCoords] = useState({ latitude: 18.5204, longitude: 73.8567 });

  // Register Patient Modal
  const [isRegOpen, setIsRegOpen] = useState(false);
  const [regName, setRegName] = useState('');
  const [regGender, setRegGender] = useState<Gender>('FEMALE');
  const [regAge, setRegAge] = useState('34');
  const [regPhone, setRegPhone] = useState('');
  const [regVillage, setRegVillage] = useState('Shirwal');
  const [regDistrict, setRegDistrict] = useState('Satara');
  const [isRegistering, setIsRegistering] = useState(false);

  // Case + Symptoms + AI Triage state
  const [chiefComplaint, setChiefComplaint] = useState('');
  const [clinicalNotes, setClinicalNotes] = useState('');
  const [symptoms, setSymptoms] = useState<CreateSymptomRequest[]>([
    { symptomName: 'High Fever', severity: 'SEVERE', durationDays: 4 },
    { symptomName: 'Chills and Sweating', severity: 'MODERATE', durationDays: 3 },
  ]);
  const [newSymName, setNewSymName] = useState('');
  const [newSymSev, setNewSymSev] = useState<SymptomSeverity>('MODERATE');
  const [newSymDays, setNewSymDays] = useState('3');
  const [activeCase, setActiveCase] = useState<HealthcareCaseDto | null>(null);
  const [aiResult, setAiResult] = useState<AiAnalysisResponse | null>(null);
  const [isRunningTriage, setIsRunningTriage] = useState(false);
  const [isCreatingReferral, setIsCreatingReferral] = useState(false);
  const [referralSuccessMsg, setReferralSuccessMsg] = useState<string | null>(null);

  // Nearby facilities & Outbreak zones
  const [nearbyFacilities, setNearbyFacilities] = useState<FacilityRecommendationDto[]>([]);
  const [outbreakZones, setOutbreakZones] = useState<OutbreakZoneDto[]>([]);

  // SOS on behalf of patient
  const [sosModalOpen, setSosModalOpen] = useState(false);
  const [sosPatientId, setSosPatientId] = useState<string | null>(null);

  const acquireLocation = useCallback(async () => {
    try {
      const { status } = await Location.requestForegroundPermissionsAsync();
      if (status === 'granted') {
        const pos = await Location.getCurrentPositionAsync({ accuracy: Location.Accuracy.Balanced });
        if (pos?.coords) {
          setCoords({ latitude: pos.coords.latitude, longitude: pos.coords.longitude });
          return { latitude: pos.coords.latitude, longitude: pos.coords.longitude };
        }
      }
    } catch {
      // use default coords
    }
    return coords;
  }, [coords]);

  const fetchPatients = useCallback(async (query?: string) => {
    setError(null);
    try {
      const q = (query ?? searchQuery).trim();
      const data = q
        ? await patientApi.searchPatientsByName(q)
        : await patientApi.getAllPatients();
      const list = Array.isArray(data) ? data : [];
      setPatients(list);
      if (!selectedPatient && list.length > 0) {
        setSelectedPatient(list[0]);
        setSelectedPatientId(list[0].id);
      }
    } catch (err: any) {
      console.error('ASHA patient directory fetch error:', err);
      setError(err.message || 'Failed to load patient directory.');
    } finally {
      setLoading(false);
      setRefreshing(false);
    }
  }, [searchQuery, selectedPatient, setSelectedPatientId]);

  const fetchFacilitiesAndZones = useCallback(async () => {
    const loc = await acquireLocation();
    try {
      const [facs, zones] = await Promise.all([
        facilityApi.searchNearbyFacilities({
          latitude: loc.latitude,
          longitude: loc.longitude,
          radiusKm: 50,
          limit: 15,
        }),
        surveillanceApi.getOutbreakZones(loc.latitude, loc.longitude),
      ]);
      setNearbyFacilities(Array.isArray(facs) ? facs : []);
      setOutbreakZones(Array.isArray(zones) ? zones : []);
    } catch (err: any) {
      console.error('ASHA facilities/zones error:', err);
    }
  }, [acquireLocation]);

  useEffect(() => {
    fetchPatients('');
    fetchFacilitiesAndZones();
  }, []);

  useEffect(() => {
    if (selectedPatient?.id) {
      caseApi
        .getCasesByPatientId(selectedPatient.id)
        .then((cases) => setPatientCases(Array.isArray(cases) ? cases : []))
        .catch(() => setPatientCases([]));
    }
  }, [selectedPatient?.id]);

  const handleRegisterPatient = async () => {
    if (!regName.trim() || !regPhone.trim()) {
      Alert.alert('Required', 'Please enter patient full name and 10-digit contact number.');
      return;
    }
    setIsRegistering(true);
    try {
      const loc = await acquireLocation();
      const ageNum = parseInt(regAge, 10) || 30;
      const birthYear = new Date().getFullYear() - ageNum;
      const dob = `${birthYear}-06-15`;

      const created = await patientApi.registerPatient({
        fullName: regName.trim(),
        gender: regGender,
        age: ageNum,
        dateOfBirth: dob,
        contactNumber: regPhone.trim(),
        villageTown: regVillage.trim() || 'Shirwal',
        subDistrict: regDistrict.trim() || 'Khandala',
        district: regDistrict.trim() || 'Satara',
        state: 'Maharashtra',
        pincode: '412801',
        latitude: loc.latitude,
        longitude: loc.longitude,
      });

      setPatients((prev) => [created, ...prev]);
      setSelectedPatient(created);
      setSelectedPatientId(created.id);
      setIsRegOpen(false);
      setRegName('');
      setRegPhone('');
      Alert.alert('Patient Registered ✓', `${created.fullName} registered with ID ${created.id.slice(0, 8)}...`);
    } catch (err: any) {
      Alert.alert('Registration Failed', err?.message || 'Unable to register patient.');
    } finally {
      setIsRegistering(false);
    }
  };

  const handleAddSymptomRow = () => {
    if (!newSymName.trim()) return;
    setSymptoms((prev) => [
      ...prev,
      {
        symptomName: newSymName.trim(),
        severity: newSymSev,
        durationDays: parseInt(newSymDays, 10) || 1,
      },
    ]);
    setNewSymName('');
  };

  const handleRemoveSymptom = (idx: number) => {
    setSymptoms((prev) => prev.filter((_, i) => i !== idx));
  };

  const handleCreateCaseAndRunTriage = async () => {
    if (!selectedPatient) {
      Alert.alert('Select Patient', 'Please select or register a patient first.');
      return;
    }
    if (symptoms.length === 0) {
      Alert.alert('Symptoms Required', 'Please add at least one symptom for clinical triage.');
      return;
    }

    setIsRunningTriage(true);
    setAiResult(null);
    setReferralSuccessMsg(null);
    setError(null);

    try {
      const loc = await acquireLocation();
      const complaintText =
        chiefComplaint.trim() ||
        symptoms.map((s) => `${s.symptomName} (${s.severity}, ${s.durationDays}d)`).join(', ');

      // 1. Create Case (POST /api/v1/cases)
      const createdCase = await caseApi.createCase({
        patientId: selectedPatient.id,
        intakeChannel: 'COMMUNITY_HEALTH_WORKER',
        chiefComplaint: complaintText,
        notes: clinicalNotes.trim() || `ASHA field screening by ${user?.displayName || 'Sunita Tai'}`,
        latitude: loc.latitude,
        longitude: loc.longitude,
      });
      setActiveCase(createdCase);

      // 2. Record Symptoms in both symptoms table (/symptoms/batch) and case-symptoms (/case-symptoms)
      await symptomApi.addSymptomsBatch(createdCase.id, symptoms);
      try {
        await symptomApi.addCaseSymptoms(createdCase.id, symptoms);
      } catch {
        // optional secondary table
      }

      // 3. Run Real AI Triage (POST /api/v1/cases/{id}/ai-analysis -> Hugging Face Gradio Space)
      const analysis = await aiApi.runAiAnalysis(createdCase.id);
      setAiResult(analysis);

      // Refresh patient cases & outbreak zones
      const updatedCases = await caseApi.getCasesByPatientId(selectedPatient.id);
      setPatientCases(Array.isArray(updatedCases) ? updatedCases : []);
      fetchFacilitiesAndZones();
    } catch (err: any) {
      console.error('ASHA intake/triage failed:', err);
      const msg = err?.message || 'Clinical AI triage failed.';
      setError(msg);
      Alert.alert('AI Triage Error', msg);
    } finally {
      setIsRunningTriage(false);
    }
  };

  const handleCreateReferral = async (facilityId: string, facilityName: string) => {
    if (!activeCase || !selectedPatient) {
      Alert.alert('Case Required', 'Please create a case and run AI triage before referring.');
      return;
    }
    setIsCreatingReferral(true);
    try {
      const urgencyLevel =
        aiResult?.urgency?.level || aiResult?.urgencyLevel || 'PRIORITY';
      const ref = await referralApi.createReferral({
        caseId: activeCase.id,
        patientId: selectedPatient.id,
        facilityId,
        urgencyLevel,
        reason:
          aiResult?.explanation ||
          aiResult?.triageRationale ||
          `ASHA AI Triage Referral (${aiResult?.riskAssessment?.riskLevel || 'HIGH'} risk)`,
        patientMessage: `Referred to ${facilityName} for clinical evaluation.`,
      });
      const tokenMsg = `Referral #${ref.referralToken || ref.id.slice(0, 8)} created for ${facilityName} (Status: ${ref.status})`;
      setReferralSuccessMsg(tokenMsg);
      Alert.alert('Referral Sent 🏥', tokenMsg);
    } catch (err: any) {
      Alert.alert('Referral Error', err?.message || 'Failed to create referral.');
    } finally {
      setIsCreatingReferral(false);
    }
  };

  return (
    <View style={{ flex: 1, backgroundColor: colors.bgApp }}>
      <Header />

      {/* Top Role Bar & Tabs */}
      <View style={[styles.tabBar, { backgroundColor: colors.bgSurface, borderColor: colors.borderLight }]}>
        {(
          [
            { key: 'PATIENTS', label: '👥 Village Patients' },
            { key: 'TRIAGE', label: '🧠 Intake & AI Triage' },
            { key: 'ZONES', label: '🏥 PHC & Outbreak' },
          ] as { key: AshaTab; label: string }[]
        ).map((t) => (
          <TouchableOpacity
            key={t.key}
            style={[
              styles.tabItem,
              activeTab === t.key && { backgroundColor: colors.primary },
            ]}
            onPress={() => setActiveTab(t.key)}
          >
            <Text
              style={[
                styles.tabText,
                { color: activeTab === t.key ? '#FFFFFF' : colors.textMain },
              ]}
            >
              {t.label}
            </Text>
          </TouchableOpacity>
        ))}
      </View>

      <ScrollView
        contentContainerStyle={styles.container}
        refreshControl={
          <RefreshControl
            refreshing={refreshing}
            onRefresh={() => {
              setRefreshing(true);
              fetchPatients();
              fetchFacilitiesAndZones();
            }}
            colors={[colors.primary]}
          />
        }
      >
        {error ? <ErrorState message={error} onRetry={() => fetchPatients()} /> : null}

        {/* TAB 1: VILLAGE PATIENTS */}
        {activeTab === 'PATIENTS' && (
          <View>
            <View style={styles.rowBetween}>
              <View>
                <Text style={[styles.sectionTitle, { color: colors.textMain }]}>
                  ASHA Patient Directory
                </Text>
                <Text style={[styles.sectionSub, { color: colors.textSubtle }]}>
                  Search village citizens, register new patients, or trigger SOS
                </Text>
              </View>
              <Button
                title="+ Register Patient"
                onPress={() => setIsRegOpen(true)}
                variant="primary"
                style={{ minHeight: 38, paddingVertical: 6, paddingHorizontal: 12 }}
              />
            </View>

            {/* Search Input */}
            <View style={styles.searchRow}>
              <TextInput
                style={[styles.searchInput, { backgroundColor: colors.bgSurface, borderColor: colors.borderLight, color: colors.textMain }]}
                placeholder="Search patient by name (GET /api/v1/patients/search)..."
                placeholderTextColor={colors.textSubtle}
                value={searchQuery}
                onChangeText={setSearchQuery}
                onSubmitEditing={() => fetchPatients(searchQuery)}
              />
              <Button
                title="Search"
                onPress={() => fetchPatients(searchQuery)}
                variant="secondary"
                style={{ minHeight: 44, paddingHorizontal: 14 }}
              />
            </View>

            {loading ? (
              <LoadingState message="Loading village patient register..." />
            ) : (
              patients.map((p) => {
                const isSelected = selectedPatient?.id === p.id;
                return (
                  <Card
                    key={p.id}
                    style={[
                      styles.patientCard,
                      isSelected ? { borderColor: colors.primary, borderWidth: 2 } : {},
                    ]}
                  >
                    <View style={styles.rowBetween}>
                      <View style={{ flex: 1 }}>
                        <Text style={[styles.patientName, { color: colors.textMain }]}>
                          {p.fullName} ({p.gender}, {p.age || 35}y)
                        </Text>
                        <Text style={[styles.patientMeta, { color: colors.textSubtle }]}>
                          📞 {p.contactNumber} • 📍 {p.villageTown || p.address?.village || 'Shirwal'}, {p.district || p.address?.district || 'Satara'}
                        </Text>
                        <Text style={{ fontSize: 11, color: colors.textSubtle, marginTop: 2 }}>
                          Patient ID: {p.id}
                        </Text>
                      </View>
                    </View>

                    <View style={styles.patientActions}>
                      <Button
                        title="🧠 New Case & AI Triage"
                        onPress={() => {
                          setSelectedPatient(p);
                          setSelectedPatientId(p.id);
                          setActiveTab('TRIAGE');
                        }}
                        variant="primary"
                        style={{ flex: 1, minHeight: 38, paddingVertical: 6 }}
                      />
                      <Button
                        title="🚨 Trigger SOS"
                        onPress={() => {
                          setSosPatientId(p.id);
                          setSosModalOpen(true);
                        }}
                        variant="danger"
                        style={{ minHeight: 38, paddingVertical: 6, paddingHorizontal: 12 }}
                      />
                    </View>
                  </Card>
                );
              })
            )}
          </View>
        )}

        {/* TAB 2: CLINICAL CASE INTAKE & AI TRIAGE */}
        {activeTab === 'TRIAGE' && (
          <View>
            <Card style={styles.intakeCard}>
              <Text style={[styles.sectionTitle, { color: colors.textMain }]}>
                Assisted Clinical Intake & AI Triage
              </Text>
              <Text style={[styles.sectionSub, { color: colors.textSubtle, marginBottom: 12 }]}>
                Selected Patient:{' '}
                <Text style={{ fontWeight: '800', color: colors.primary }}>
                  {selectedPatient
                    ? `${selectedPatient.fullName} (${selectedPatient.age || 35}y ${selectedPatient.gender})`
                    : 'None — select a patient in Tab 1'}
                </Text>
              </Text>

              <Text style={[styles.label, { color: colors.textMuted }]}>Chief Complaint:</Text>
              <TextInput
                style={[styles.input, { backgroundColor: colors.bgSubtle, borderColor: colors.borderLight, color: colors.textMain }]}
                placeholder="e.g. High fever with chills and body ache for 4 days"
                placeholderTextColor={colors.textSubtle}
                value={chiefComplaint}
                onChangeText={setChiefComplaint}
              />

              <Text style={[styles.label, { color: colors.textMuted, marginTop: 10 }]}>
                Recorded Symptoms ({symptoms.length}):
              </Text>
              {symptoms.map((s, idx) => (
                <View
                  key={idx}
                  style={[styles.symRow, { backgroundColor: colors.bgSubtle, borderColor: colors.borderLight }]}
                >
                  <View style={{ flex: 1 }}>
                    <Text style={{ fontWeight: '700', color: colors.textMain, fontSize: 13 }}>
                      • {s.symptomName}
                    </Text>
                    <Text style={{ fontSize: 11, color: colors.textSubtle }}>
                      Severity: {s.severity} • Duration: {s.durationDays} days
                    </Text>
                  </View>
                  <TouchableOpacity onPress={() => handleRemoveSymptom(idx)}>
                    <Text style={{ color: '#DC2626', fontWeight: '800', fontSize: 14 }}>✕</Text>
                  </TouchableOpacity>
                </View>
              ))}

              {/* Add Symptom Inline */}
              <View style={[styles.addSymBox, { borderColor: colors.borderLight }]}>
                <Text style={{ fontSize: 12, fontWeight: '700', color: colors.textMuted, marginBottom: 6 }}>
                  + Add Symptom
                </Text>
                <View style={{ flexDirection: 'row', gap: 6, marginBottom: 6 }}>
                  <TextInput
                    style={[styles.input, { flex: 2, height: 40, backgroundColor: colors.bgSubtle, borderColor: colors.borderLight, color: colors.textMain }]}
                    placeholder="Symptom name (e.g. Cough)"
                    placeholderTextColor={colors.textSubtle}
                    value={newSymName}
                    onChangeText={setNewSymName}
                  />
                  <TextInput
                    style={[styles.input, { flex: 1, height: 40, backgroundColor: colors.bgSubtle, borderColor: colors.borderLight, color: colors.textMain }]}
                    placeholder="Days"
                    placeholderTextColor={colors.textSubtle}
                    keyboardType="numeric"
                    value={newSymDays}
                    onChangeText={setNewSymDays}
                  />
                </View>
                <View style={{ flexDirection: 'row', gap: 6, alignItems: 'center' }}>
                  {(['MILD', 'MODERATE', 'SEVERE', 'CRITICAL'] as SymptomSeverity[]).map((sev) => (
                    <TouchableOpacity
                      key={sev}
                      style={[
                        styles.sevChip,
                        {
                          backgroundColor: newSymSev === sev ? colors.primaryLight : colors.bgSubtle,
                          borderColor: newSymSev === sev ? colors.primary : colors.borderLight,
                        },
                      ]}
                      onPress={() => setNewSymSev(sev)}
                    >
                      <Text style={{ fontSize: 10, fontWeight: '700', color: newSymSev === sev ? colors.primary : colors.textMain }}>
                        {sev}
                      </Text>
                    </TouchableOpacity>
                  ))}
                  <Button
                    title="+ Add"
                    onPress={handleAddSymptomRow}
                    variant="outline"
                    style={{ minHeight: 34, paddingVertical: 4, paddingHorizontal: 10 }}
                  />
                </View>
              </View>

              <Text style={[styles.label, { color: colors.textMuted, marginTop: 10 }]}>
                ASHA Field Observations / Vitals Notes:
              </Text>
              <TextInput
                style={[styles.input, { backgroundColor: colors.bgSubtle, borderColor: colors.borderLight, color: colors.textMain }]}
                placeholder="e.g. BP 130/85, Temp 102F, platelet drop suspected"
                placeholderTextColor={colors.textSubtle}
                value={clinicalNotes}
                onChangeText={setClinicalNotes}
              />

              <Button
                title={
                  isRunningTriage
                    ? 'Running Hugging Face Clinical AI Triage...'
                    : '🧠 Create Case & Run Live AI Triage'
                }
                onPress={handleCreateCaseAndRunTriage}
                loading={isRunningTriage}
                variant="primary"
                style={{ marginTop: 14 }}
              />
            </Card>

            {/* AI TRIAGE RESULT */}
            {aiResult && (
              <Card style={[styles.aiCard, { borderColor: colors.primary, borderWidth: 1.5 }]}>
                <View style={styles.rowBetween}>
                  <View>
                    <Text style={[styles.sectionTitle, { color: colors.primary }]}>
                      🩺 AI Clinical Triage Result
                    </Text>
                    <Text style={{ fontSize: 11, color: colors.textSubtle }}>
                      Case #{activeCase?.caseNumber || aiResult.caseId.slice(0, 8)} • Model: {aiResult.modelVersion}
                    </Text>
                  </View>
                  <StatusBadge
                    status={
                      aiResult.riskAssessment?.riskLevel ||
                      aiResult.riskLevel ||
                      'HIGH'
                    }
                  />
                </View>

                <View style={[styles.scoreRow, { backgroundColor: colors.bgSubtle }]}>
                  <Text style={{ fontSize: 13, fontWeight: '700', color: colors.textMain }}>
                    Risk Score: {Math.round((aiResult.riskAssessment?.riskScore ?? aiResult.riskScore ?? 0.75) * 100)}%
                  </Text>
                  <Text style={{ fontSize: 13, fontWeight: '700', color: colors.secondary }}>
                    Urgency: {aiResult.urgency?.level || aiResult.urgencyLevel || 'URGENT'}
                  </Text>
                </View>

                {(aiResult.explanation || aiResult.triageRationale) && (
                  <Text style={{ fontSize: 13, color: colors.textMain, marginVertical: 8, lineHeight: 19 }}>
                    {aiResult.explanation || aiResult.triageRationale}
                  </Text>
                )}

                {/* Possible Diseases */}
                {aiResult.possibleDiseases && aiResult.possibleDiseases.length > 0 && (
                  <View style={{ marginTop: 6 }}>
                    <Text style={{ fontSize: 12, fontWeight: '800', color: colors.textMuted, marginBottom: 4 }}>
                      DIFFERENTIAL DIAGNOSIS / PROBABLE CONDITIONS:
                    </Text>
                    {aiResult.possibleDiseases.map((d, i) => (
                      <Text key={i} style={{ fontSize: 13, color: colors.textMain, marginBottom: 2 }}>
                        • {d.category || d.diseaseName} ({Math.round((d.probability || 0) * 100)}%)
                      </Text>
                    ))}
                  </View>
                )}

                {/* Precautions */}
                {aiResult.precautions && aiResult.precautions.length > 0 && (
                  <View style={{ marginTop: 8 }}>
                    <Text style={{ fontSize: 12, fontWeight: '800', color: colors.textMuted, marginBottom: 4 }}>
                      RECOMMENDED PRECAUTIONS:
                    </Text>
                    {aiResult.precautions.map((p, i) => (
                      <Text key={i} style={{ fontSize: 12, color: colors.textSubtle, marginBottom: 2 }}>
                        ✓ {p}
                      </Text>
                    ))}
                  </View>
                )}

                {referralSuccessMsg && (
                  <View style={{ backgroundColor: '#DCFCE7', padding: 10, borderRadius: 8, marginTop: 10 }}>
                    <Text style={{ color: '#15803D', fontWeight: '700', fontSize: 13 }}>
                      ✓ {referralSuccessMsg}
                    </Text>
                  </View>
                )}

                {/* Recommended Facilities & One-Tap Referral */}
                <Text style={{ fontSize: 13, fontWeight: '800', color: colors.textMain, marginTop: 14, marginBottom: 8 }}>
                  🏥 Recommended Referral Facilities (PostGIS Matched):
                </Text>

                {(aiResult.recommendedFacilities && aiResult.recommendedFacilities.length > 0
                  ? aiResult.recommendedFacilities
                  : nearbyFacilities
                )
                  .slice(0, 4)
                  .map((fac) => {
                    const fid = fac.facilityId || fac.id || 'f1000000-0000-0000-0000-000000000001';
                    return (
                      <View
                        key={fid}
                        style={[styles.refFacRow, { backgroundColor: colors.bgSubtle, borderColor: colors.borderLight }]}
                      >
                        <View style={{ flex: 1 }}>
                          <Text style={{ fontWeight: '700', color: colors.textMain, fontSize: 14 }}>
                            {fac.name}
                          </Text>
                          <Text style={{ fontSize: 12, color: colors.textSubtle }}>
                            {fac.facilityType} • {fac.villageTown}, {fac.district}{' '}
                            {fac.distanceKm != null ? `(${fac.distanceKm.toFixed(1)} km)` : ''}
                          </Text>
                        </View>
                        <Button
                          title="Refer Patient"
                          onPress={() => handleCreateReferral(fid, fac.name)}
                          loading={isCreatingReferral}
                          variant="secondary"
                          style={{ minHeight: 36, paddingVertical: 6, paddingHorizontal: 12 }}
                        />
                      </View>
                    );
                  })}

                {/* Also allow direct referral to Seeded Primary Health Centre (c1000000-0000-0000-0000-000000000001) */}
                <Button
                  title="🏥 Refer to Palasdeo Primary Health Centre (PHC)"
                  onPress={() =>
                    handleCreateReferral(
                      'c1000000-0000-0000-0000-000000000001',
                      'Palasdeo Primary Health Centre (PHC)'
                    )
                  }
                  loading={isCreatingReferral}
                  variant="outline"
                  style={{ marginTop: 8 }}
                />
              </Card>
            )}
          </View>
        )}

        {/* TAB 3: NEARBY FACILITIES & OUTBREAK ZONES */}
        {activeTab === 'ZONES' && (
          <View>
            <Text style={[styles.sectionTitle, { color: colors.textMain, marginBottom: 4 }]}>
              🗺️ Regional Outbreak Surveillance Zones
            </Text>
            <Text style={[styles.sectionSub, { color: colors.textSubtle, marginBottom: 12 }]}>
              Real-time RED / YELLOW / GREEN epidemiological classification
            </Text>

            {outbreakZones.map((zone) => {
              const isRed = zone.zoneColor === 'RED';
              const isYellow = zone.zoneColor === 'YELLOW';
              const badgeBg = isRed ? '#FEE2E2' : isYellow ? '#FEF3C7' : '#DCFCE7';
              const badgeText = isRed ? '#B91C1C' : isYellow ? '#B45309' : '#15803D';

              return (
                <Card key={zone.zoneId || zone.areaId || zone.areaName} style={styles.patientCard}>
                  <View style={styles.rowBetween}>
                    <View style={{ flex: 1 }}>
                      <Text style={[styles.patientName, { color: colors.textMain }]}>
                        {zone.areaName}
                      </Text>
                      <Text style={{ fontSize: 12, color: colors.textSubtle }}>
                        Active Cases: {zone.activeCases} • High Risk: {zone.highRiskCases}
                        {zone.distanceKm != null ? ` • ${zone.distanceKm} km away` : ''}
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
                      🦠 {zone.topDiseases.slice(0, 3).join(', ')}
                    </Text>
                  )}
                  <Text style={{ fontSize: 12, color: colors.textSubtle, marginTop: 4 }}>
                    {zone.advisoryMessage || zone.advisory}
                  </Text>
                </Card>
              );
            })}

            <Text style={[styles.sectionTitle, { color: colors.textMain, marginTop: 16, marginBottom: 8 }]}>
              🏥 Nearby Healthcare Facilities (Sorted by Distance)
            </Text>
            {nearbyFacilities.map((f) => (
              <Card key={f.facilityId || f.id} style={styles.patientCard}>
                <View style={styles.rowBetween}>
                  <View style={{ flex: 1 }}>
                    <Text style={[styles.patientName, { color: colors.textMain }]}>{f.name}</Text>
                    <Text style={{ fontSize: 12, color: colors.textSubtle }}>
                      📍 {f.villageTown}, {f.district} • {f.distanceKm != null ? `${f.distanceKm.toFixed(1)} km` : 'Nearby'}
                    </Text>
                  </View>
                  <StatusBadge status={f.facilityType} />
                </View>
              </Card>
            ))}
          </View>
        )}
      </ScrollView>

      {/* Register Patient Modal */}
      <Modal visible={isRegOpen} transparent animationType="slide" onRequestClose={() => setIsRegOpen(false)}>
        <View style={styles.modalBackdrop}>
          <View style={[styles.modalCard, { backgroundColor: colors.bgSurface }]}>
            <View style={styles.rowBetween}>
              <Text style={[styles.sectionTitle, { color: colors.textMain }]}>Register Rural Patient</Text>
              <TouchableOpacity onPress={() => setIsRegOpen(false)}>
                <Text style={{ fontSize: 18, color: colors.textSubtle }}>✕</Text>
              </TouchableOpacity>
            </View>

            <Text style={[styles.label, { color: colors.textMuted, marginTop: 10 }]}>Full Name:</Text>
            <TextInput
              style={[styles.input, { backgroundColor: colors.bgSubtle, borderColor: colors.borderLight, color: colors.textMain }]}
              placeholder="e.g. Kavita Jadhav"
              placeholderTextColor={colors.textSubtle}
              value={regName}
              onChangeText={setRegName}
            />

            <View style={{ flexDirection: 'row', gap: 10, marginTop: 8 }}>
              <View style={{ flex: 1 }}>
                <Text style={[styles.label, { color: colors.textMuted }]}>Age:</Text>
                <TextInput
                  style={[styles.input, { backgroundColor: colors.bgSubtle, borderColor: colors.borderLight, color: colors.textMain }]}
                  keyboardType="numeric"
                  value={regAge}
                  onChangeText={setRegAge}
                />
              </View>
              <View style={{ flex: 2 }}>
                <Text style={[styles.label, { color: colors.textMuted }]}>Contact Number:</Text>
                <TextInput
                  style={[styles.input, { backgroundColor: colors.bgSubtle, borderColor: colors.borderLight, color: colors.textMain }]}
                  keyboardType="phone-pad"
                  placeholder="9876501234"
                  placeholderTextColor={colors.textSubtle}
                  value={regPhone}
                  onChangeText={setRegPhone}
                />
              </View>
            </View>

            <Text style={[styles.label, { color: colors.textMuted, marginTop: 8 }]}>Gender:</Text>
            <View style={{ flexDirection: 'row', gap: 8 }}>
              {(['FEMALE', 'MALE', 'OTHER'] as Gender[]).map((g) => (
                <TouchableOpacity
                  key={g}
                  style={[
                    styles.sevChip,
                    {
                      flex: 1,
                      alignItems: 'center',
                      paddingVertical: 8,
                      backgroundColor: regGender === g ? colors.primaryLight : colors.bgSubtle,
                      borderColor: regGender === g ? colors.primary : colors.borderLight,
                    },
                  ]}
                  onPress={() => setRegGender(g)}
                >
                  <Text style={{ fontWeight: '700', fontSize: 12, color: regGender === g ? colors.primary : colors.textMain }}>
                    {g}
                  </Text>
                </TouchableOpacity>
              ))}
            </View>

            <View style={{ flexDirection: 'row', gap: 10, marginTop: 8 }}>
              <View style={{ flex: 1 }}>
                <Text style={[styles.label, { color: colors.textMuted }]}>Village/Town:</Text>
                <TextInput
                  style={[styles.input, { backgroundColor: colors.bgSubtle, borderColor: colors.borderLight, color: colors.textMain }]}
                  value={regVillage}
                  onChangeText={setRegVillage}
                />
              </View>
              <View style={{ flex: 1 }}>
                <Text style={[styles.label, { color: colors.textMuted }]}>District:</Text>
                <TextInput
                  style={[styles.input, { backgroundColor: colors.bgSubtle, borderColor: colors.borderLight, color: colors.textMain }]}
                  value={regDistrict}
                  onChangeText={setRegDistrict}
                />
              </View>
            </View>

            <Button
              title={isRegistering ? 'Registering...' : 'Save & Select Patient'}
              onPress={handleRegisterPatient}
              loading={isRegistering}
              style={{ marginTop: 16 }}
            />
          </View>
        </View>
      </Modal>

      <EmergencySosModal
        visible={sosModalOpen}
        onClose={() => setSosModalOpen(false)}
        overridePatientId={sosPatientId}
      />
    </View>
  );
};

const styles = StyleSheet.create({
  container: {
    padding: 14,
    paddingBottom: 90,
  },
  tabBar: {
    flexDirection: 'row',
    padding: 6,
    borderBottomWidth: 1,
    gap: 6,
  },
  tabItem: {
    flex: 1,
    paddingVertical: 9,
    borderRadius: 8,
    alignItems: 'center',
  },
  tabText: {
    fontSize: 12,
    fontWeight: '800',
  },
  rowBetween: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
  },
  sectionTitle: {
    fontSize: 17,
    fontWeight: '800',
  },
  sectionSub: {
    fontSize: 12,
    marginTop: 2,
  },
  searchRow: {
    flexDirection: 'row',
    gap: 8,
    marginVertical: 12,
  },
  searchInput: {
    flex: 1,
    height: 44,
    borderWidth: 1,
    borderRadius: 10,
    paddingHorizontal: 12,
    fontSize: 13,
  },
  patientCard: {
    padding: 14,
    marginBottom: 10,
  },
  patientName: {
    fontSize: 15,
    fontWeight: '800',
  },
  patientMeta: {
    fontSize: 12,
    marginTop: 3,
  },
  patientActions: {
    flexDirection: 'row',
    gap: 8,
    marginTop: 10,
  },
  intakeCard: {
    padding: 14,
    marginBottom: 14,
  },
  label: {
    fontSize: 12,
    fontWeight: '700',
    marginBottom: 4,
  },
  input: {
    height: 44,
    borderWidth: 1,
    borderRadius: 8,
    paddingHorizontal: 12,
    fontSize: 14,
  },
  symRow: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    padding: 10,
    borderRadius: 8,
    borderWidth: 1,
    marginBottom: 6,
  },
  addSymBox: {
    borderWidth: 1,
    borderStyle: 'dashed',
    borderRadius: 10,
    padding: 10,
    marginTop: 6,
  },
  sevChip: {
    paddingHorizontal: 8,
    paddingVertical: 5,
    borderRadius: 6,
    borderWidth: 1,
  },
  aiCard: {
    padding: 16,
    marginBottom: 16,
  },
  scoreRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    padding: 10,
    borderRadius: 8,
    marginTop: 10,
  },
  refFacRow: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    padding: 10,
    borderRadius: 8,
    borderWidth: 1,
    marginBottom: 8,
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
});
