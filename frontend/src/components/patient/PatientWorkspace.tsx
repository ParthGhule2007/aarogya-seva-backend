import React, { useState } from 'react';
import { useAuth } from '../../context/AuthContext';
import { usePatient } from '../../hooks/usePatient';
import { useConsultation } from '../../hooks/useConsultation';
import { useDoctor } from '../../hooks/useDoctor';
import { useLanguage } from '../../context/LanguageContext';
import { useSos } from '../../hooks/useSos';
import { ScreeningWizard } from '../screening/ScreeningWizard';
import { MedicineCabinet } from '../medicine/MedicineCabinet';
import {
  Activity,
  Heart,
  Pill,
  Video,
  Calendar,
  AlertTriangle,
  Stethoscope,
  Plus,
  ArrowRight,
  ShieldCheck,
  CheckCircle,
  Clock,
  User,
  PhoneCall,
} from 'lucide-react';
import { ConsultationRequestDTO } from '../../types/consultation';
import { AarogyaMitraVoiceCallCard } from '../calling/AarogyaMitraVoiceCallCard';
import { PatientConsultationsList } from './PatientConsultationsList';
import { RequestConsultationModal } from './RequestConsultationModal';
import { PatientMedicalRecords } from './PatientMedicalRecords';
import { FileText, MapPin, Droplet, Phone, TrendingUp, Sparkles } from 'lucide-react';
import { VitalsGridDisplay } from '../common/medical/VitalCard';
import { StatusBadge } from '../common/medical/StatusBadge';
import { SymptomChip } from '../common/medical/SymptomChip';
import { MobileBottomNav } from '../common/MobileBottomNav';

export const PatientWorkspace: React.FC = () => {
  const { user, selectedPatientId } = useAuth();
  const { patient, cases } = usePatient();
  const { consultations, startConsultation } = useConsultation();
  const { availableDoctors } = useDoctor();
  const { t } = useLanguage();
  const { setIsSosModalOpen } = useSos();

  const [activeTab, setActiveTab] = useState<'overview' | 'screening' | 'records' | 'medicines' | 'consultations' | 'voiceCall'>('overview');
  const [isBookModalOpen, setIsBookModalOpen] = useState(false);

  return (
    <div>
      {/* Sub-Navigation Tabs */}
      <div className="tabs-nav">
        <button
          className={`tab-btn ${activeTab === 'overview' ? 'active' : ''}`}
          onClick={() => setActiveTab('overview')}
        >
          <Activity size={18} />
          <span>{t('navOverview')}</span>
        </button>

        <button
          className={`tab-btn ${activeTab === 'screening' ? 'active' : ''}`}
          onClick={() => setActiveTab('screening')}
        >
          <Heart size={18} />
          <span>{t('navCareFlow')}</span>
        </button>

        <button
          className={`tab-btn ${activeTab === 'records' ? 'active' : ''}`}
          onClick={() => setActiveTab('records')}
        >
          <FileText size={18} />
          <span>Medical History (नोंदवही)</span>
        </button>

        <button
          className={`tab-btn ${activeTab === 'medicines' ? 'active' : ''}`}
          onClick={() => setActiveTab('medicines')}
        >
          <Pill size={18} />
          <span>{t('navMedicines')}</span>
        </button>

        <button
          className={`tab-btn ${activeTab === 'consultations' ? 'active' : ''}`}
          onClick={() => setActiveTab('consultations')}
        >
          <Video size={18} />
          <span>{t('navConsultations')}</span>
        </button>

        <button
          className={`tab-btn ${activeTab === 'voiceCall' ? 'active' : ''}`}
          onClick={() => setActiveTab('voiceCall')}
        >
          <PhoneCall size={18} />
          <span>{t('navVoiceCall')}</span>
        </button>
      </div>

      {/* TAB 1: OVERVIEW */}
      {activeTab === 'overview' && (
        <div style={{ maxWidth: '1040px', margin: '0 auto' }}>
          {/* Premium Patient Health Hero Card */}
          <div className="hero-banner-modern" style={{ marginBottom: '1.5rem' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', flexWrap: 'wrap', gap: '1.25rem', position: 'relative', zIndex: 1 }}>
              <div>
                <div style={{ display: 'inline-flex', alignItems: 'center', gap: '0.4rem', padding: '0.22rem 0.7rem', borderRadius: '9999px', backgroundColor: 'rgba(255,255,255,0.14)', fontSize: '0.78rem', fontWeight: 700, marginBottom: '0.5rem' }}>
                  <Sparkles size={13} />
                  <span>Rural Digital Health Pass • आरोग्य सेवा ओळखपत्र</span>
                </div>
                <h2 style={{ fontSize: '1.75rem', fontWeight: 900, margin: '0.15rem 0 0.45rem 0', letterSpacing: '-0.02em' }}>
                  {patient?.fullName || user?.displayName || 'Ramesh Patil'}
                </h2>
                <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem', flexWrap: 'wrap', fontSize: '0.88rem', opacity: 0.95 }}>
                  <span style={{ display: 'inline-flex', alignItems: 'center', gap: '0.3rem' }}>
                    <MapPin size={15} />
                    {patient?.address?.village || patient?.villageTown || 'Palasdeo'}, {patient?.address?.district || patient?.district || 'Pune'}
                  </span>
                  {patient?.age && (
                    <span>• {patient.age} yrs ({patient.gender})</span>
                  )}
                  {patient?.bloodGroup && (
                    <span style={{ display: 'inline-flex', alignItems: 'center', gap: '0.25rem', padding: '0.1rem 0.55rem', borderRadius: '9999px', backgroundColor: 'rgba(239, 68, 68, 0.28)', border: '1px solid rgba(252, 165, 165, 0.5)', fontWeight: 800 }}>
                      <Droplet size={13} /> {patient.bloodGroup}
                    </span>
                  )}
                  {(patient?.contactNumber || user?.contactNumber) && (
                    <span style={{ display: 'inline-flex', alignItems: 'center', gap: '0.25rem' }}>
                      <Phone size={14} /> {patient?.contactNumber || user?.contactNumber}
                    </span>
                  )}
                </div>
              </div>

              <div style={{ display: 'flex', gap: '0.55rem', flexWrap: 'wrap' }}>
                <button
                  className="btn"
                  style={{ background: '#ffffff', color: '#0f766e', fontWeight: 800, boxShadow: '0 4px 12px rgba(0,0,0,0.15)' }}
                  onClick={() => setActiveTab('screening')}
                >
                  <Heart size={18} />
                  <span>Start 5-Step Triage</span>
                </button>

                <button
                  className="btn btn-outline"
                  style={{
                    backgroundColor: 'rgba(255, 255, 255, 0.14)',
                    color: '#ffffff',
                    borderColor: 'rgba(255, 255, 255, 0.38)',
                    fontWeight: 700,
                  }}
                  onClick={() => setActiveTab('records')}
                >
                  <FileText size={18} />
                  <span>Medical History</span>
                </button>

                <button
                  className="btn btn-outline"
                  style={{
                    backgroundColor: 'rgba(255, 255, 255, 0.14)',
                    color: '#ffffff',
                    borderColor: 'rgba(255, 255, 255, 0.38)',
                    fontWeight: 700,
                  }}
                  onClick={() => setActiveTab('voiceCall')}
                >
                  <PhoneCall size={18} />
                  <span>AI Voice Call</span>
                </button>

                <button
                  className="btn btn-danger"
                  style={{ fontWeight: 800, boxShadow: '0 4px 14px rgba(220, 38, 38, 0.4)' }}
                  onClick={() => setIsSosModalOpen(true)}
                >
                  <AlertTriangle size={18} />
                  <span>Emergency SOS</span>
                </button>
              </div>
            </div>
          </div>

          {/* Quick Metrics Grid */}
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(220px, 1fr))', gap: '1rem', marginBottom: '1.5rem' }}>
            <div className="stat-card-modern" onClick={() => setActiveTab('records')} style={{ cursor: 'pointer' }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                <span style={{ fontSize: '0.82rem', color: 'var(--text-subtle)', fontWeight: 700, textTransform: 'uppercase' }}>
                  Active Care Cases • रुग्ण प्रकरणे
                </span>
                <Activity size={18} color="var(--primary)" />
              </div>
              <div style={{ fontSize: '1.9rem', fontWeight: 900, color: 'var(--primary)', margin: '0.3rem 0' }}>
                {cases.length}
              </div>
              <div style={{ fontSize: '0.8rem', color: 'var(--text-muted)' }}>
                Recorded in longitudinal EHR
              </div>
            </div>

            <div className="stat-card-modern" onClick={() => setActiveTab('consultations')} style={{ cursor: 'pointer' }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                <span style={{ fontSize: '0.82rem', color: 'var(--text-subtle)', fontWeight: 700, textTransform: 'uppercase' }}>
                  Telemedicine Calls • डॉक्टर सल्ला
                </span>
                <Video size={18} color="var(--secondary)" />
              </div>
              <div style={{ fontSize: '1.9rem', fontWeight: 900, color: 'var(--secondary)', margin: '0.3rem 0' }}>
                {consultations.length}
              </div>
              <div style={{ fontSize: '0.8rem', color: 'var(--text-muted)' }}>
                Video / audio sessions scheduled
              </div>
            </div>

            <div className="stat-card-modern" onClick={() => setIsBookModalOpen(true)} style={{ cursor: 'pointer' }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                <span style={{ fontSize: '0.82rem', color: 'var(--text-subtle)', fontWeight: 700, textTransform: 'uppercase' }}>
                  Available Doctors • उपलब्ध डॉक्टर
                </span>
                <Stethoscope size={18} color="#f59e0b" />
              </div>
              <div style={{ fontSize: '1.9rem', fontWeight: 900, color: '#d97706', margin: '0.3rem 0' }}>
                {availableDoctors.length}
              </div>
              <div style={{ fontSize: '0.8rem', color: 'var(--text-muted)' }}>
                Online for instant rural consultation
              </div>
            </div>
          </div>

          {/* Latest Recorded Vitals & Symptom Trends */}
          {cases.length > 0 && (
            <div className="card" style={{ marginBottom: '1.5rem' }}>
              <div className="card-header">
                <div className="card-title">
                  <TrendingUp size={20} color="var(--primary)" />
                  <span>Latest Vitals & Clinical Case Trend • ताजे शारीरिक मापदंड</span>
                </div>
                <StatusBadge status={cases[0].priority || cases[0].status} />
              </div>

              {cases[0].vitals ? (
                <div style={{ marginBottom: '1rem' }}>
                  <VitalsGridDisplay vitals={cases[0].vitals} />
                </div>
              ) : (
                <div style={{ fontSize: '0.85rem', color: 'var(--text-subtle)', marginBottom: '0.85rem' }}>
                  Case <strong>{cases[0].caseNumber}</strong> recorded on{' '}
                  {cases[0].createdAt ? new Date(cases[0].createdAt).toLocaleDateString() : 'Recently'}.
                </div>
              )}

              {/* Recent Case Timeline & Symptom Chips */}
              <div
                style={{
                  display: 'grid',
                  gridTemplateColumns: 'repeat(auto-fit, minmax(260px, 1fr))',
                  gap: '0.75rem',
                  paddingTop: '0.75rem',
                  borderTop: '1px solid var(--border-light)',
                }}
              >
                {cases.slice(0, 3).map((c) => (
                  <div
                    key={c.id}
                    style={{
                      padding: '0.75rem 0.9rem',
                      borderRadius: 'var(--radius-md)',
                      backgroundColor: 'var(--bg-subtle)',
                      border: '1px solid var(--border-light)',
                      display: 'flex',
                      flexDirection: 'column',
                      gap: '0.4rem',
                    }}
                  >
                    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                      <strong style={{ fontSize: '0.86rem', color: 'var(--text-main)' }}>
                        {c.caseNumber}
                      </strong>
                      <StatusBadge status={c.status} size="sm" />
                    </div>
                    {c.chiefComplaint && (
                      <div style={{ fontSize: '0.82rem', color: 'var(--text-muted)' }}>
                        {c.chiefComplaint}
                      </div>
                    )}
                    {c.symptoms && c.symptoms.length > 0 && (
                      <div style={{ display: 'flex', gap: '0.35rem', flexWrap: 'wrap', marginTop: '0.2rem' }}>
                        {c.symptoms.slice(0, 3).map((sym, i) => (
                          <SymptomChip
                            key={i}
                            name={sym.symptomName}
                            severity={sym.severity}
                            durationDays={sym.durationDays}
                          />
                        ))}
                      </div>
                    )}
                  </div>
                ))}
              </div>
            </div>
          )}

          {/* Recent Consultation or Case Banner */}
          <div className="card">
            <div className="card-header">
              <div className="card-title">
                <Stethoscope size={20} color="var(--primary)" />
                <span>Next Scheduled Doctor Consultation</span>
              </div>
              <button
                className="btn btn-outline"
                style={{ minHeight: '36px', fontSize: '0.85rem' }}
                onClick={() => setIsBookModalOpen(true)}
              >
                <Plus size={16} />
                <span>Book Doctor</span>
              </button>
            </div>

            {consultations.length === 0 ? (
              <div style={{ padding: '1rem 0', color: 'var(--text-subtle)', fontSize: '0.9rem' }}>
                No active consultation requests. Click 'Book Doctor' to schedule a video call with a specialist.
              </div>
            ) : (
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: '0.75rem' }}>
                <div>
                  <div style={{ fontWeight: 800, fontSize: '1.05rem' }}>
                    Consultation #{consultations[0].id} • {consultations[0].disease}
                  </div>
                  <div style={{ fontSize: '0.85rem', color: 'var(--text-subtle)' }}>
                    Type: {consultations[0].consultationType} • Urgency: {consultations[0].urgency}
                  </div>
                </div>

                <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                  <StatusBadge status={consultations[0].status} pulse={consultations[0].status === 'READY' || consultations[0].status === 'IN_CALL'} />
                  {(consultations[0].status === 'READY' || consultations[0].status === 'IN_CALL') && (
                    <button
                      className="btn btn-primary"
                      style={{ minHeight: '40px', fontSize: '0.85rem' }}
                      onClick={() => startConsultation(consultations[0].id, user?.displayName, consultations[0].doctorName)}
                    >
                      <Video size={16} />
                      <span>Join Call</span>
                    </button>
                  )}
                  {consultations[0].status === 'ACCEPTED' && (
                    <span style={{ fontSize: '0.85rem', color: 'var(--text-subtle)', fontStyle: 'italic' }}>
                      Doctor accepted. Waiting for doctor to launch call...
                    </span>
                  )}
                </div>
              </div>
            )}
          </div>
        </div>
      )}

      {/* TAB 2: 5-STEP CARE FLOW */}
      {activeTab === 'screening' && (
        <ScreeningWizard onComplete={() => setActiveTab('records')} />
      )}

      {/* TAB: PERMANENT MEDICAL RECORDS */}
      {activeTab === 'records' && <PatientMedicalRecords />}

      {/* TAB 3: MEDICINE CABINET */}
      {activeTab === 'medicines' && <MedicineCabinet />}

      {/* TAB 4: CONSULTATIONS */}
      {activeTab === 'consultations' && (
        <PatientConsultationsList onFindDoctors={() => setIsBookModalOpen(true)} />
      )}

      {/* TAB 5: AI VOICE CALL (AAROGYA MITRA) */}
      {activeTab === 'voiceCall' && (
        <div style={{ maxWidth: '850px', margin: '0 auto' }}>
          <AarogyaMitraVoiceCallCard
            defaultPhone={patient?.contactNumber || user?.contactNumber}
            defaultName={patient?.fullName || user?.displayName}
          />
        </div>
      )}

      {/* Book Consultation Modal */}
      {isBookModalOpen && (
        <RequestConsultationModal
          onClose={() => setIsBookModalOpen(false)}
          onSuccess={() => {
            setIsBookModalOpen(false);
            setActiveTab('consultations');
          }}
        />
      )}

      {/* Mobile Bottom Navigation Bar for Thumb Ease */}
      <MobileBottomNav activeTab={activeTab} onTabChange={(tab) => setActiveTab(tab)} />
    </div>
  );
};
