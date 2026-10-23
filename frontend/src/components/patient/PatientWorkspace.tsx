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

export const PatientWorkspace: React.FC = () => {
  const { user, selectedPatientId } = useAuth();
  const { patient, cases } = usePatient();
  const { consultations, bookConsultation, startConsultation } = useConsultation();
  const { availableDoctors } = useDoctor();
  const { t } = useLanguage();
  const { setIsSosModalOpen } = useSos();

  const [activeTab, setActiveTab] = useState<'overview' | 'screening' | 'medicines' | 'consultations' | 'voiceCall'>('overview');
  const [isBookModalOpen, setIsBookModalOpen] = useState(false);
  const [selectedDoctorId, setSelectedDoctorId] = useState<number>(1);
  const [consultationChiefComplaint, setConsultationChiefComplaint] = useState('');
  const [isBooking, setIsBooking] = useState(false);

  const handleBookConsultationSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!consultationChiefComplaint.trim()) return;
    setIsBooking(true);
    try {
      const selectedDoc = availableDoctors.find((d) => (d.doctorId || d.id) === selectedDoctorId);
      const req: ConsultationRequestDTO = {
        userId: selectedPatientId || user?.patientId || 'b1000000-0000-0000-0000-000000000001',
        username: user?.displayName || 'Patient',
        doctorId: selectedDoctorId,
        disease: consultationChiefComplaint,
        urgency: 'MEDIUM',
        consultationType: 'VIDEO',
      };
      await bookConsultation(req);
      setIsBookModalOpen(false);
      setConsultationChiefComplaint('');
      setActiveTab('consultations');
    } catch {
      // Error handled in hook
    } finally {
      setIsBooking(false);
    }
  };

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
        <div style={{ maxWidth: '1000px', margin: '0 auto' }}>
          {/* Welcome Health Card */}
          <div
            style={{
              background: 'linear-gradient(135deg, #0d9488 0%, #059669 100%)',
              color: '#ffffff',
              borderRadius: 'var(--radius-lg)',
              padding: '1.75rem',
              marginBottom: '1.5rem',
              boxShadow: 'var(--shadow-md)',
            }}
          >
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: '1rem' }}>
              <div>
                <div style={{ fontSize: '0.9rem', opacity: 0.9 }}>Welcome to Aarogya Seva</div>
                <h2 style={{ fontSize: '1.6rem', fontWeight: 900, margin: '0.25rem 0' }}>
                  {user?.displayName || 'Ramesh Patil'}
                </h2>
                <div style={{ fontSize: '0.9rem', opacity: 0.9 }}>
                  Village: {patient?.address?.village || patient?.villageTown || 'Palasdeo'}, District: {patient?.address?.district || patient?.district || 'Pune'}
                </div>
              </div>

              <div style={{ display: 'flex', gap: '0.5rem', flexWrap: 'wrap' }}>
                <button
                  className="btn"
                  style={{ background: '#ffffff', color: '#0d9488', fontWeight: 700 }}
                  onClick={() => setActiveTab('screening')}
                >
                  <Heart size={18} />
                  <span>Start 5-Step Triage</span>
                </button>

                <button
                  className="btn btn-outline"
                  style={{
                    backgroundColor: 'rgba(255, 255, 255, 0.15)',
                    color: '#ffffff',
                    borderColor: 'rgba(255, 255, 255, 0.4)',
                    fontWeight: 700,
                  }}
                  onClick={() => setActiveTab('voiceCall')}
                >
                  <PhoneCall size={18} />
                  <span>AI Voice Call</span>
                </button>

                <button
                  className="btn btn-danger"
                  style={{ fontWeight: 800 }}
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
            <div className="card" style={{ marginBottom: 0 }}>
              <div style={{ fontSize: '0.85rem', color: 'var(--text-subtle)', fontWeight: 600 }}>Active Care Cases</div>
              <div style={{ fontSize: '1.8rem', fontWeight: 900, color: 'var(--primary)', margin: '0.25rem 0' }}>
                {cases.length}
              </div>
              <div style={{ fontSize: '0.8rem', color: 'var(--text-muted)' }}>Recorded with Core health service</div>
            </div>

            <div className="card" style={{ marginBottom: 0 }}>
              <div style={{ fontSize: '0.85rem', color: 'var(--text-subtle)', fontWeight: 600 }}>Telemedicine Calls</div>
              <div style={{ fontSize: '1.8rem', fontWeight: 900, color: 'var(--secondary)', margin: '0.25rem 0' }}>
                {consultations.length}
              </div>
              <div style={{ fontSize: '0.8rem', color: 'var(--text-muted)' }}>Doctor consultations scheduled</div>
            </div>

            <div className="card" style={{ marginBottom: 0 }}>
              <div style={{ fontSize: '0.85rem', color: 'var(--text-subtle)', fontWeight: 600 }}>Available Doctors</div>
              <div style={{ fontSize: '1.8rem', fontWeight: 900, color: '#f59e0b', margin: '0.25rem 0' }}>
                {availableDoctors.length}
              </div>
              <div style={{ fontSize: '0.8rem', color: 'var(--text-muted)' }}>Online for instant telemedicine</div>
            </div>
          </div>

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
                  <span className="badge badge-routine">{consultations[0].status}</span>
                  {(consultations[0].status === 'READY' || consultations[0].status === 'ACCEPTED' || consultations[0].status === 'IN_CALL') && (
                    <button
                      className="btn btn-primary"
                      style={{ minHeight: '40px', fontSize: '0.85rem' }}
                      onClick={() => startConsultation(consultations[0].id, user?.displayName, consultations[0].doctorName)}
                    >
                      <Video size={16} />
                      <span>Join Call</span>
                    </button>
                  )}
                </div>
              </div>
            )}
          </div>
        </div>
      )}

      {/* TAB 2: 5-STEP CARE FLOW */}
      {activeTab === 'screening' && (
        <ScreeningWizard onComplete={() => setActiveTab('overview')} />
      )}

      {/* TAB 3: MEDICINE CABINET */}
      {activeTab === 'medicines' && <MedicineCabinet />}

      {/* TAB 4: CONSULTATIONS */}
      {activeTab === 'consultations' && (
        <div style={{ maxWidth: '1000px', margin: '0 auto' }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1.5rem', flexWrap: 'wrap', gap: '0.5rem' }}>
            <div>
              <h2 style={{ fontSize: '1.3rem', fontWeight: 800 }}>Telemedicine Consultations</h2>
              <p style={{ fontSize: '0.85rem', color: 'var(--text-subtle)' }}>
                Connect with verified MBBS doctors and specialists over encrypted WebRTC video.
              </p>
            </div>
            <button className="btn btn-primary" onClick={() => setIsBookModalOpen(true)}>
              <Plus size={18} />
              <span>Book New Consultation</span>
            </button>
          </div>

          <div style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
            {consultations.length === 0 ? (
              <div className="card" style={{ textAlign: 'center', padding: '3rem 1rem' }}>
                <Video size={36} color="var(--primary)" style={{ margin: '0 auto 0.75rem' }} />
                <div style={{ fontWeight: 700 }}>No Telemedicine Consultations</div>
                <div style={{ fontSize: '0.85rem', color: 'var(--text-subtle)', marginTop: '0.25rem' }}>
                  Book your first video call consultation with an available medical officer.
                </div>
              </div>
            ) : (
              consultations.map((c) => (
                <div key={c.id} className="card" style={{ marginBottom: 0 }}>
                  <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: '0.5rem' }}>
                    <div>
                      <div style={{ fontWeight: 800, fontSize: '1.1rem' }}>
                        {c.disease}
                      </div>
                      <div style={{ fontSize: '0.85rem', color: 'var(--text-subtle)' }}>
                        Consultation #{c.id} • Mode: {c.consultationType} • Urgency: {c.urgency}
                      </div>
                    </div>

                    <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                      <span className="badge badge-routine">{c.status}</span>
                      {(c.status === 'READY' || c.status === 'ACCEPTED' || c.status === 'IN_CALL') && (
                        <button
                          className="btn btn-primary"
                          style={{ minHeight: '40px' }}
                          onClick={() => startConsultation(c.id, user?.displayName, c.doctorName)}
                        >
                          <Video size={16} />
                          <span>Join Video Consultation</span>
                        </button>
                      )}
                    </div>
                  </div>
                </div>
              ))
            )}
          </div>
        </div>
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
        <div className="modal-overlay" onClick={() => setIsBookModalOpen(false)}>
          <div className="modal-dialog" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <h3 style={{ fontWeight: 700 }}>Book Telemedicine Consultation</h3>
              <button className="icon-btn" onClick={() => setIsBookModalOpen(false)}>✕</button>
            </div>
            <form onSubmit={handleBookConsultationSubmit}>
              <div className="modal-body">
                <div className="form-group">
                  <label className="form-label">Select Available Doctor: *</label>
                  <select
                    className="form-control"
                    value={selectedDoctorId}
                    onChange={(e) => setSelectedDoctorId(Number(e.target.value))}
                  >
                    {availableDoctors.length === 0 ? (
                      <option value={1}>Dr. Ananya Sharma (General Medicine) - Online</option>
                    ) : (
                      availableDoctors.map((doc) => (
                        <option key={doc.doctorId || doc.id} value={doc.doctorId || doc.id}>
                          {doc.doctorName || doc.name} ({doc.specialization}) - OPD Fee: ₹{doc.consultationFee || 200}
                        </option>
                      ))
                    )}
                  </select>
                </div>

                <div className="form-group">
                  <label className="form-label">Chief Complaint / Symptoms to Discuss: *</label>
                  <textarea
                    className="form-control"
                    rows={3}
                    placeholder="e.g. Persistent fever and body ache for 3 days"
                    value={consultationChiefComplaint}
                    onChange={(e) => setConsultationChiefComplaint(e.target.value)}
                    required
                  />
                </div>
              </div>

              <div className="modal-footer">
                <button type="button" className="btn btn-outline" onClick={() => setIsBookModalOpen(false)}>
                  Cancel
                </button>
                <button type="submit" className="btn btn-primary" disabled={isBooking || !consultationChiefComplaint.trim()}>
                  {isBooking ? 'Requesting...' : 'Confirm & Request Consultation'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};
