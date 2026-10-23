import React, { useState, useEffect } from 'react';
import { patientApi } from '../api/patientApi';
import { caseApi } from '../api/caseApi';
import { symptomApi } from '../api/symptomApi';
import { aiApi } from '../api/aiApi';
import { referralApi } from '../api/referralApi';
import { PatientDto } from '../types/patient';
import { HealthcareCaseDto } from '../types/case';
import { SymptomDto } from '../types/symptom';
import { AiAnalysisResponse } from '../types/ai';
import { FacilityRecommendationDto } from '../types/facility';
import { ReferralResponseDto } from '../types/referral';
import { Button } from '../components/common/Button';
import { Badge } from '../components/common/Badge';
import { AlertBanner } from '../components/common/AlertBanner';
import { LoadingSpinner } from '../components/common/LoadingSpinner';
import { EmptyState } from '../components/common/EmptyState';
import { RegisterPatientModal } from '../components/patient/RegisterPatientModal';
import { CreateCaseModal } from '../components/case/CreateCaseModal';
import { AddSymptomModal } from '../components/symptom/AddSymptomModal';
import { AiAnalysisCard } from '../components/ai/AiAnalysisCard';
import { FacilityRecommendationCard } from '../components/facility/FacilityRecommendationCard';
import {
  UserPlus,
  PlusCircle,
  Activity,
  Sparkles,
  Send,
  RotateCcw,
  CheckCircle2,
  Clock,
  AlertTriangle,
  History,
  Trash2,
} from 'lucide-react';

export const PatientPortalPage: React.FC = () => {
  // Master lists
  const [patients, setPatients] = useState<PatientDto[]>([]);
  const [cases, setCases] = useState<HealthcareCaseDto[]>([]);

  // Selected entities
  const [selectedPatient, setSelectedPatient] = useState<PatientDto | null>(null);
  const [selectedCase, setSelectedCase] = useState<HealthcareCaseDto | null>(null);
  const [symptoms, setSymptoms] = useState<SymptomDto[]>([]);
  const [aiAnalysis, setAiAnalysis] = useState<AiAnalysisResponse | null>(null);
  const [selectedFacility, setSelectedFacility] = useState<FacilityRecommendationDto | null>(null);
  const [referrals, setReferrals] = useState<ReferralResponseDto[]>([]);

  // Patient message for referral
  const [patientMessage, setPatientMessage] = useState('Patient requesting urgent clinical evaluation');

  // Loading & Error states
  const [isLoadingPatients, setIsLoadingPatients] = useState(false);
  const [isLoadingCases, setIsLoadingCases] = useState(false);
  const [isAnalyzing, setIsAnalyzing] = useState(false);
  const [isSubmittingReferral, setIsSubmittingReferral] = useState(false);
  const [banner, setBanner] = useState<{ type: 'info' | 'success' | 'warning' | 'danger'; message: string } | null>(null);

  // Modals
  const [isRegisterOpen, setIsRegisterOpen] = useState(false);
  const [isCreateCaseOpen, setIsCreateCaseOpen] = useState(false);
  const [isAddSymptomOpen, setIsAddSymptomOpen] = useState(false);

  // Load patients on mount
  useEffect(() => {
    loadPatients();
  }, []);

  // When patient changes, load patient's cases
  useEffect(() => {
    if (selectedPatient) {
      loadCasesForPatient(selectedPatient.id);
    } else {
      setCases([]);
      setSelectedCase(null);
    }
  }, [selectedPatient]);

  // When case changes, load symptoms, existing AI analysis, and referral history
  useEffect(() => {
    if (selectedCase) {
      loadCaseData(selectedCase.id);
    } else {
      setSymptoms([]);
      setAiAnalysis(null);
      setSelectedFacility(null);
      setReferrals([]);
    }
  }, [selectedCase]);

  const loadPatients = async () => {
    setIsLoadingPatients(true);
    try {
      const data = await patientApi.getAllPatients();
      setPatients(data);
      if (data.length > 0 && !selectedPatient) {
        setSelectedPatient(data[0]);
      }
    } catch (err: any) {
      setBanner({ type: 'danger', message: `Could not load patients: ${err.message}` });
    } finally {
      setIsLoadingPatients(false);
    }
  };

  const loadCasesForPatient = async (patientId: string) => {
    setIsLoadingCases(true);
    try {
      const data = await caseApi.getCasesByPatientId(patientId);
      setCases(data);
      if (data.length > 0) {
        setSelectedCase(data[0]);
      } else {
        setSelectedCase(null);
      }
    } catch (err: any) {
      setBanner({ type: 'warning', message: `Failed to load cases: ${err.message}` });
    } finally {
      setIsLoadingCases(false);
    }
  };

  const loadCaseData = async (caseId: string) => {
    try {
      // 1. Load Symptoms
      const symList = await symptomApi.getSymptomsForCase(caseId);
      setSymptoms(symList);

      // 2. Load existing AI analysis if any
      try {
        const analysis = await aiApi.getLatestAnalysis(caseId);
        setAiAnalysis(analysis);
      } catch {
        setAiAnalysis(null);
      }

      // 3. Load referrals for this case
      try {
        const refs = await referralApi.getReferralsByCaseId(caseId);
        setReferrals(refs);
      } catch {
        setReferrals([]);
      }
    } catch (err: any) {
      setBanner({ type: 'warning', message: `Error loading case details: ${err.message}` });
    }
  };

  const handleRunAiScreening = async () => {
    if (!selectedCase) return;
    if (symptoms.length === 0) {
      setBanner({ type: 'warning', message: 'Please record at least one symptom before running AI screening.' });
      return;
    }

    setIsAnalyzing(true);
    setBanner(null);

    try {
      const result = await aiApi.runAiAnalysis(selectedCase.id);
      setAiAnalysis(result);
      setBanner({ type: 'success', message: 'AI clinical screening and facility discovery completed successfully.' });
      // Reload case status if it updated
      const updatedCase = await caseApi.getCaseById(selectedCase.id);
      setSelectedCase(updatedCase);
    } catch (err: any) {
      setBanner({ type: 'danger', message: err.message || 'AI Screening failed' });
    } finally {
      setIsAnalyzing(false);
    }
  };

  const handleSendReferral = async () => {
    if (!selectedCase || !selectedFacility) return;

    setIsSubmittingReferral(true);
    setBanner(null);

    try {
      const newReferral = await referralApi.createReferral({
        caseId: selectedCase.id,
        facilityId: selectedFacility.facilityId,
        patientMessage: patientMessage.trim(),
      });

      setReferrals([newReferral, ...referrals]);
      setSelectedFacility(null);
      setBanner({
        type: 'success',
        message: `Referral request submitted successfully to ${newReferral.facilityName} (Status: PENDING).`,
      });
    } catch (err: any) {
      setBanner({ type: 'danger', message: err.message || 'Failed to submit referral' });
    } finally {
      setIsSubmittingReferral(false);
    }
  };

  const handleDeleteSymptom = async (symptomId: string) => {
    try {
      await symptomApi.deleteSymptom(symptomId);
      setSymptoms(symptoms.filter((s) => s.id !== symptomId));
      setBanner({ type: 'info', message: 'Symptom removed.' });
    } catch (err: any) {
      setBanner({ type: 'danger', message: err.message || 'Failed to delete symptom' });
    }
  };

  // Determine current active referral
  const activeReferral = referrals.find((r) => r.status === 'PENDING' || r.status === 'ACCEPTED' || r.status === 'IN_PROGRESS');
  const latestReferral = referrals.length > 0 ? referrals[0] : null;

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '2rem' }}>
      {banner && (
        <AlertBanner
          type={banner.type}
          message={banner.message}
          onDismiss={() => setBanner(null)}
        />
      )}

      {/* Header section */}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: '1rem' }}>
        <div>
          <h2 style={{ fontSize: '1.75rem', fontWeight: 800 }}>Patient Care Navigation (रुग्ण सेवा व संदर्भ)</h2>
          <p style={{ color: 'var(--text-muted)' }}>
            Record clinical presentation, run AI screening, discover PostGIS facilities, and coordinate referrals.
          </p>
        </div>

        <div style={{ display: 'flex', gap: '0.75rem' }}>
          <Button
            variant="secondary"
            icon={<UserPlus size={16} />}
            onClick={() => setIsRegisterOpen(true)}
          >
            Register Patient (नवीन रुग्ण)
          </Button>

          {selectedPatient && (
            <Button
              variant="primary"
              icon={<PlusCircle size={16} />}
              onClick={() => setIsCreateCaseOpen(true)}
            >
              Open New Case (नवीन केस)
            </Button>
          )}
        </div>
      </div>

      {/* Step 1 & 2: Patient & Case Selector */}
      <div className="grid-2">
        {/* Patient Selection Card */}
        <div className="card">
          <div className="card-header">
            <span className="card-title">1. Patient Profile (रुग्ण)</span>
            {selectedPatient && (
              <span style={{ fontSize: '0.8rem', color: 'var(--text-subtle)' }}>
                ID: {selectedPatient.id.substring(0, 8)}...
              </span>
            )}
          </div>

          {isLoadingPatients ? (
            <LoadingSpinner size="sm" message="Loading patients..." />
          ) : patients.length === 0 ? (
            <EmptyState
              title="No Patients Registered"
              description="Register the first rural patient to start clinical intake."
              action={
                <Button size="sm" onClick={() => setIsRegisterOpen(true)}>
                  Register Patient
                </Button>
              }
            />
          ) : (
            <div>
              <div className="form-group">
                <label className="form-label">Select Patient</label>
                <select
                  className="form-select"
                  value={selectedPatient?.id || ''}
                  onChange={(e) => {
                    const found = patients.find((p) => p.id === e.target.value);
                    if (found) setSelectedPatient(found);
                  }}
                >
                  {patients.map((p) => (
                    <option key={p.id} value={p.id}>
                      {p.fullName} ({p.gender}, {p.age}y) — {p.villageTown}, {p.district}
                    </option>
                  ))}
                </select>
              </div>

              {selectedPatient && (
                <div
                  style={{
                    padding: '0.85rem',
                    backgroundColor: 'var(--bg-surface-subtle)',
                    borderRadius: 'var(--radius-md)',
                    fontSize: '0.85rem',
                  }}
                >
                  <div style={{ display: 'grid', gridTemplateColumns: 'repeat(2, 1fr)', gap: '0.4rem' }}>
                    <div><strong>Phone:</strong> {selectedPatient.contactNumber}</div>
                    <div><strong>Blood Group:</strong> {selectedPatient.bloodGroup || 'Unknown'}</div>
                    <div><strong>Village:</strong> {selectedPatient.villageTown}</div>
                    <div><strong>District:</strong> {selectedPatient.district}</div>
                  </div>
                </div>
              )}
            </div>
          )}
        </div>

        {/* Case Selection Card */}
        <div className="card">
          <div className="card-header">
            <span className="card-title">2. Healthcare Case (केस)</span>
            {selectedCase && (
              <Badge variant="neutral">{selectedCase.status}</Badge>
            )}
          </div>

          {isLoadingCases ? (
            <LoadingSpinner size="sm" message="Loading cases..." />
          ) : !selectedPatient ? (
            <p style={{ fontSize: '0.875rem', color: 'var(--text-subtle)' }}>Select a patient first.</p>
          ) : cases.length === 0 ? (
            <EmptyState
              title="No Active Consultation Case"
              description={`No healthcare cases found for ${selectedPatient.fullName}.`}
              action={
                <Button size="sm" onClick={() => setIsCreateCaseOpen(true)}>
                  Open Case
                </Button>
              }
            />
          ) : (
            <div>
              <div className="form-group">
                <label className="form-label">Select Consultation Case</label>
                <select
                  className="form-select"
                  value={selectedCase?.id || ''}
                  onChange={(e) => {
                    const found = cases.find((c) => c.id === e.target.value);
                    if (found) setSelectedCase(found);
                  }}
                >
                  {cases.map((c) => (
                    <option key={c.id} value={c.id}>
                      {c.caseNumber} — Priority: {c.priority} ({c.status})
                    </option>
                  ))}
                </select>
              </div>

              {selectedCase && (
                <div
                  style={{
                    padding: '0.85rem',
                    backgroundColor: 'var(--bg-surface-subtle)',
                    borderRadius: 'var(--radius-md)',
                    fontSize: '0.85rem',
                  }}
                >
                  <div style={{ display: 'grid', gridTemplateColumns: 'repeat(2, 1fr)', gap: '0.4rem' }}>
                    <div><strong>Case No:</strong> {selectedCase.caseNumber}</div>
                    <div><strong>Priority:</strong> {selectedCase.priority}</div>
                    <div><strong>Intake Modality:</strong> {selectedCase.intakeChannel}</div>
                    <div><strong>GPS Coords:</strong> {selectedCase.latitude || 'N/A'}, {selectedCase.longitude || 'N/A'}</div>
                  </div>
                  {selectedCase.chiefComplaint && (
                    <div style={{ marginTop: '0.4rem', color: 'var(--text-main)' }}>
                      <strong>Chief Complaint:</strong> {selectedCase.chiefComplaint}
                    </div>
                  )}
                  {selectedCase.notes && (
                    <div style={{ marginTop: '0.4rem', color: 'var(--text-muted)' }}>
                      <strong>Notes:</strong> {selectedCase.notes}
                    </div>
                  )}
                </div>
              )}
            </div>
          )}
        </div>
      </div>

      {/* Step 3: Symptoms Section */}
      {selectedCase && (
        <div className="card">
          <div className="card-header">
            <div>
              <h3 className="card-title">
                <Activity size={20} style={{ color: 'var(--warning)' }} />
                <span>3. Recorded Symptoms ({symptoms.length})</span>
              </h3>
              <p style={{ fontSize: '0.8rem', color: 'var(--text-subtle)' }}>
                Add clinical symptoms before running AI clinical screening
              </p>
            </div>

            <Button
              size="sm"
              variant="secondary"
              icon={<PlusCircle size={14} />}
              onClick={() => setIsAddSymptomOpen(true)}
            >
              Add Symptom (लक्षण जोडा)
            </Button>
          </div>

          {symptoms.length === 0 ? (
            <EmptyState
              title="No Symptoms Recorded"
              description="Click 'Add Symptom' to log complaints such as fever, cough, chest pain, or diarrhea."
              action={
                <Button size="sm" onClick={() => setIsAddSymptomOpen(true)}>
                  Add Symptom
                </Button>
              }
            />
          ) : (
            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(280px, 1fr))', gap: '0.85rem' }}>
              {symptoms.map((s) => (
                <div
                  key={s.id}
                  style={{
                    padding: '0.85rem',
                    backgroundColor: 'var(--bg-surface-subtle)',
                    border: '1px solid var(--border-light)',
                    borderRadius: 'var(--radius-md)',
                    display: 'flex',
                    justifyContent: 'space-between',
                    alignItems: 'flex-start',
                  }}
                >
                  <div>
                    <div style={{ fontWeight: 700, fontSize: '0.95rem' }}>{s.symptomName}</div>
                    <div style={{ display: 'flex', gap: '0.4rem', marginTop: '0.3rem', alignItems: 'center' }}>
                      <span
                        className="badge"
                        style={{
                          backgroundColor: s.severity === 'CRITICAL' ? 'var(--danger-bg)' : s.severity === 'SEVERE' ? '#ffedd5' : '#fef3c7',
                          color: s.severity === 'CRITICAL' ? 'var(--danger)' : s.severity === 'SEVERE' ? '#c2410c' : '#b45309',
                        }}
                      >
                        {s.severity}
                      </span>
                      <span style={{ fontSize: '0.8rem', color: 'var(--text-muted)' }}>
                        {s.durationDays} day(s)
                      </span>
                      {s.bodySite && (
                        <span style={{ fontSize: '0.8rem', color: 'var(--text-subtle)' }}>
                          • {s.bodySite}
                        </span>
                      )}
                    </div>
                    {s.description && (
                      <p style={{ fontSize: '0.8rem', color: 'var(--text-muted)', marginTop: '0.35rem' }}>
                        {s.description}
                      </p>
                    )}
                  </div>

                  <button
                    onClick={() => handleDeleteSymptom(s.id)}
                    style={{
                      background: 'transparent',
                      border: 'none',
                      color: 'var(--text-subtle)',
                      cursor: 'pointer',
                      padding: '4px',
                    }}
                    title="Remove symptom"
                  >
                    <Trash2 size={16} />
                  </button>
                </div>
              ))}
            </div>
          )}

          <div style={{ marginTop: '1.25rem', display: 'flex', justifyContent: 'flex-end' }}>
            <Button
              variant="primary"
              size="lg"
              icon={<Sparkles size={18} />}
              isLoading={isAnalyzing}
              disabled={symptoms.length === 0}
              onClick={handleRunAiScreening}
            >
              {aiAnalysis ? 'Re-run AI Clinical Screening' : 'Run AI Clinical Screening (AI तपासणी करा)'}
            </Button>
          </div>
        </div>
      )}

      {/* Step 4: AI Analysis Presentation */}
      {aiAnalysis && (
        <div>
          <AiAnalysisCard analysis={aiAnalysis} />
        </div>
      )}

      {/* Step 5: Recommended Facilities & Referral Sending */}
      {aiAnalysis && (
        <div className="card">
          <div className="card-header">
            <div>
              <h3 className="card-title">
                <span>4. Recommended Healthcare Facilities (जवळची रुग्णालये)</span>
              </h3>
              <p style={{ fontSize: '0.8rem', color: 'var(--text-subtle)' }}>
                {aiAnalysis.facilityMatchingNote || 'Nearby facilities matching required clinical tier and services'}
              </p>
            </div>
          </div>

          {/* Active Referral Notice */}
          {activeReferral && (
            <div className="alert alert-info" style={{ marginBottom: '1.25rem' }}>
              <div>
                <strong>Active Referral In Progress:</strong> This case currently has an active referral directed to{' '}
                <strong>{activeReferral.facilityName}</strong> with status{' '}
                <Badge variant="status" value={activeReferral.status} />.
                <div style={{ fontSize: '0.85rem', marginTop: '0.3rem' }}>
                  Per policy, only one active referral is permitted per case. You can track this referral below.
                </div>
              </div>
            </div>
          )}

          {/* If latest referral was rejected */}
          {latestReferral && latestReferral.status === 'REJECTED' && !activeReferral && (
            <div className="alert alert-warning" style={{ marginBottom: '1.25rem' }}>
              <div>
                <strong>Previous Referral to {latestReferral.facilityName} was REJECTED:</strong>
                {latestReferral.facilityResponse && (
                  <div style={{ marginTop: '0.2rem', fontStyle: 'italic' }}>
                    "{latestReferral.facilityResponse}"
                  </div>
                )}
                <div style={{ marginTop: '0.4rem', fontWeight: 600 }}>
                  ✓ You may now select another suggested facility below to submit a new referral for this SAME case.
                </div>
              </div>
            </div>
          )}

          {aiAnalysis.recommendedFacilities.length === 0 ? (
            <EmptyState
              title="No Matching Facilities Discovered"
              description="No operational facilities matching required services were found within search radius."
            />
          ) : (
            <div className="grid-2">
              {aiAnalysis.recommendedFacilities.map((facility) => (
                <FacilityRecommendationCard
                  key={facility.facilityId}
                  facility={facility}
                  isSelected={selectedFacility?.facilityId === facility.facilityId}
                  disabled={Boolean(activeReferral)}
                  onSelect={(f) => setSelectedFacility(f)}
                />
              ))}
            </div>
          )}

          {/* Send Referral Form */}
          {selectedFacility && !activeReferral && (
            <div
              style={{
                marginTop: '1.5rem',
                padding: '1.25rem',
                backgroundColor: 'var(--bg-surface-subtle)',
                borderRadius: 'var(--radius-md)',
                border: '1px solid var(--primary)',
              }}
            >
              <h4 style={{ fontSize: '1rem', fontWeight: 700, marginBottom: '0.5rem', color: 'var(--primary-dark)' }}>
                Confirm Referral Request to: {selectedFacility.name} ({selectedFacility.facilityType})
              </h4>
              <p style={{ fontSize: '0.85rem', color: 'var(--text-muted)', marginBottom: '0.85rem' }}>
                Distance: {selectedFacility.distanceKm?.toFixed(1)} km • {selectedFacility.villageTown}, {selectedFacility.district}
              </p>

              <div className="form-group">
                <label className="form-label">Message / Clinical Reason for Referral</label>
                <textarea
                  className="form-textarea"
                  rows={2}
                  value={patientMessage}
                  onChange={(e) => setPatientMessage(e.target.value)}
                  placeholder="e.g. Patient requires immediate evaluation for severe respiratory distress..."
                />
              </div>

              <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '0.75rem' }}>
                <Button variant="secondary" onClick={() => setSelectedFacility(null)}>
                  Cancel Selection
                </Button>
                <Button
                  variant="primary"
                  icon={<Send size={16} />}
                  isLoading={isSubmittingReferral}
                  onClick={handleSendReferral}
                >
                  Send Referral Request (संदर्भ पाठवा)
                </Button>
              </div>
            </div>
          )}
        </div>
      )}

      {/* Step 6: Referral History & Status Tracker */}
      {selectedCase && (
        <div className="card">
          <div className="card-header">
            <h3 className="card-title">
              <History size={20} style={{ color: 'var(--primary)' }} />
              <span>5. Case Referral History & Care Navigation ({referrals.length})</span>
            </h3>
            <Button size="sm" variant="secondary" icon={<RotateCcw size={14} />} onClick={() => loadCaseData(selectedCase.id)}>
              Refresh Status
            </Button>
          </div>

          {referrals.length === 0 ? (
            <EmptyState
              title="No Referral Requests"
              description="No referrals have been sent for this case yet. Run AI screening and select a facility above."
            />
          ) : (
            <div style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
              {referrals.map((ref) => (
                <div
                  key={ref.id}
                  style={{
                    padding: '1.25rem',
                    backgroundColor: 'var(--bg-surface)',
                    border: '1px solid var(--border-light)',
                    borderRadius: 'var(--radius-md)',
                    borderLeft: `5px solid ${
                      ref.status === 'COMPLETED'
                        ? 'var(--success)'
                        : ref.status === 'ACCEPTED' || ref.status === 'IN_PROGRESS'
                        ? 'var(--info)'
                        : ref.status === 'REJECTED'
                        ? 'var(--danger)'
                        : 'var(--warning)'
                    }`,
                  }}
                >
                  <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', flexWrap: 'wrap', gap: '0.5rem' }}>
                    <div>
                      <h4 style={{ fontSize: '1.05rem', fontWeight: 700 }}>{ref.facilityName}</h4>
                      <div style={{ fontSize: '0.8rem', color: 'var(--text-subtle)' }}>
                        Requested: {new Date(ref.requestedAt).toLocaleString()}
                      </div>
                    </div>

                    <Badge variant="status" value={ref.status} />
                  </div>

                  {ref.patientMessage && (
                    <div style={{ fontSize: '0.875rem', marginTop: '0.6rem', color: 'var(--text-main)' }}>
                      <strong>Patient Note:</strong> {ref.patientMessage}
                    </div>
                  )}

                  {ref.facilityResponse && (
                    <div
                      style={{
                        fontSize: '0.875rem',
                        marginTop: '0.5rem',
                        padding: '0.5rem 0.75rem',
                        backgroundColor: 'var(--bg-surface-subtle)',
                        borderRadius: 'var(--radius-sm)',
                        borderLeft: '3px solid var(--border-strong)',
                      }}
                    >
                      <strong>Facility Response:</strong> {ref.facilityResponse}
                    </div>
                  )}

                  <div style={{ display: 'flex', gap: '1rem', marginTop: '0.75rem', fontSize: '0.8rem', color: 'var(--text-subtle)' }}>
                    {ref.respondedAt && <div>Responded: {new Date(ref.respondedAt).toLocaleTimeString()}</div>}
                    {ref.acceptedAt && <div>Accepted: {new Date(ref.acceptedAt).toLocaleTimeString()}</div>}
                    {ref.completedAt && <div>Completed: {new Date(ref.completedAt).toLocaleTimeString()}</div>}
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>
      )}

      {/* Modals */}
      <RegisterPatientModal
        isOpen={isRegisterOpen}
        onClose={() => setIsRegisterOpen(false)}
        onSuccess={(newP) => {
          setPatients([newP, ...patients]);
          setSelectedPatient(newP);
          setBanner({ type: 'success', message: `Patient ${newP.fullName} registered successfully.` });
        }}
      />

      {selectedPatient && (
        <CreateCaseModal
          isOpen={isCreateCaseOpen}
          onClose={() => setIsCreateCaseOpen(false)}
          patient={selectedPatient}
          onSuccess={(newC) => {
            setCases([newC, ...cases]);
            setSelectedCase(newC);
            setBanner({ type: 'success', message: `Consultation Case ${newC.caseNumber} opened.` });
          }}
        />
      )}

      {selectedCase && (
        <AddSymptomModal
          isOpen={isAddSymptomOpen}
          onClose={() => setIsAddSymptomOpen(false)}
          caseId={selectedCase.id}
          onSuccess={(newS) => {
            setSymptoms([...symptoms, newS]);
            setBanner({ type: 'success', message: `Symptom '${newS.symptomName}' recorded.` });
          }}
        />
      )}
    </div>
  );
};
