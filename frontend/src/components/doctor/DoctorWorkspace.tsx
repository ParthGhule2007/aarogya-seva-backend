import React, { useState, useEffect } from 'react';
import { useAuth } from '../../context/AuthContext';
import { useDoctor } from '../../hooks/useDoctor';
import { useConsultation } from '../../hooks/useConsultation';
import { useLanguage } from '../../context/LanguageContext';
import { DoctorAvailabilityStatus } from '../../types/doctor';
import {
  Stethoscope,
  Video,
  CheckCircle,
  XCircle,
  Clock,
  User,
  Activity,
  FileText,
  AlertTriangle,
  RefreshCw,
} from 'lucide-react';
import { ConsultationResponseDTO } from '../../types/consultation';
import { DoctorRegistrationModal } from './DoctorRegistrationModal';

export const DoctorWorkspace: React.FC = () => {
  const { user } = useAuth();
  const { currentDoctor, availability, updateAvailability, isLoading: isDocLoading, refreshCurrentDoctor } = useDoctor();
  const {
    pendingQueue,
    fetchDoctorQueue,
    acceptConsultation,
    rejectConsultation,
    startConsultation,
    completeConsultation,
    isLoading: isQueueLoading,
  } = useConsultation();
  const { t } = useLanguage();

  const [selectedCase, setSelectedCase] = useState<ConsultationResponseDTO | null>(null);
  const [isRegModalOpen, setIsRegModalOpen] = useState(false);

  useEffect(() => {
    fetchDoctorQueue();
  }, [fetchDoctorQueue]);

  const handleStatusChange = async (e: React.ChangeEvent<HTMLSelectElement>) => {
    const status = e.target.value as DoctorAvailabilityStatus;
    try {
      await updateAvailability(status);
    } catch {
      // error handled
    }
  };

  const getUrgencyBadge = (u: string) => {
    switch (u) {
      case 'EMERGENCY':
        return <span className="badge badge-emergency">EMERGENCY</span>;
      case 'HIGH':
      case 'URGENT':
        return <span className="badge badge-urgent">HIGH PRIORITY</span>;
      default:
        return <span className="badge badge-routine">ROUTINE</span>;
    }
  };

  return (
    <div style={{ maxWidth: '1100px', margin: '0 auto' }}>
      {/* Doctor Header & Availability Bar */}
      <div
        className="card"
        style={{
          background: 'linear-gradient(135deg, #0d9488 0%, #0f766e 100%)',
          color: '#ffffff',
          padding: '1.5rem',
        }}
      >
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: '1rem' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
            <div
              style={{
                width: '54px',
                height: '54px',
                borderRadius: 'var(--radius-full)',
                background: 'rgba(255, 255, 255, 0.2)',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
              }}
            >
              <Stethoscope size={28} color="#ffffff" />
            </div>
            <div>
              <h2 style={{ fontSize: '1.4rem', fontWeight: 800 }}>
                {currentDoctor?.doctorName || currentDoctor?.name || user?.displayName || 'Dr. Ananya Sharma, MBBS'}
              </h2>
              <div style={{ fontSize: '0.85rem', opacity: 0.9 }}>
                {currentDoctor?.specialization || 'General Medicine'} • {currentDoctor?.licenseNumber ? `License: ${currentDoctor.licenseNumber}` : 'Clinical Profile Pending'}
              </div>
            </div>
          </div>

          <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem', flexWrap: 'wrap' }}>
            <button
              onClick={() => setIsRegModalOpen(true)}
              className="btn btn-outline"
              style={{
                color: '#ffffff',
                borderColor: 'rgba(255, 255, 255, 0.4)',
                background: 'rgba(255, 255, 255, 0.1)',
                minHeight: '34px',
                padding: '0 0.75rem',
                fontSize: '0.85rem',
                fontWeight: 600,
              }}
            >
              <Stethoscope size={15} />
              <span>{currentDoctor?.licenseNumber ? 'Update Profile' : 'Register Doctor Profile'}</span>
            </button>

            {/* Real Availability Toggle */}
            <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', background: 'rgba(0,0,0,0.25)', padding: '0.5rem 0.85rem', borderRadius: 'var(--radius-md)' }}>
              <div
                style={{
                  width: '12px',
                  height: '12px',
                  borderRadius: 'var(--radius-full)',
                  background: availability === 'AVAILABLE' ? '#4ade80' : availability === 'BUSY' ? '#facc15' : '#ef4444',
                }}
              />
              <span style={{ fontSize: '0.85rem', fontWeight: 700 }}>Status:</span>
              <select
                style={{
                  background: 'transparent',
                  color: '#ffffff',
                  border: '1px solid rgba(255,255,255,0.4)',
                  borderRadius: 'var(--radius-sm)',
                  padding: '0.2rem 0.4rem',
                  fontSize: '0.85rem',
                  fontWeight: 600,
                  cursor: 'pointer',
                }}
                value={availability}
                onChange={handleStatusChange}
                disabled={isDocLoading}
              >
                <option value="AVAILABLE" style={{ color: '#000000' }}>● Available (ड्यूटी पर)</option>
                <option value="BUSY" style={{ color: '#000000' }}>● In Consultation (व्यस्त)</option>
                <option value="OFFLINE" style={{ color: '#000000' }}>● Offline (ऑफ़लाइन)</option>
              </select>
            </div>
          </div>
        </div>
      </div>

      {/* Profile Registration Banner if doctor has not completed profile */}
      {!currentDoctor?.licenseNumber && (
        <div
          style={{
            backgroundColor: '#eff6ff',
            border: '1px solid #bfdbfe',
            borderRadius: '12px',
            padding: '1rem 1.25rem',
            marginBottom: '1.5rem',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
            gap: '1rem',
            flexWrap: 'wrap',
          }}
        >
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
            <AlertTriangle size={24} color="#2563eb" />
            <div>
              <div style={{ fontWeight: 700, color: '#1e3a8a', fontSize: '0.95rem' }}>
                Complete your clinical credentials
              </div>
              <div style={{ color: '#3b82f6', fontSize: '0.85rem' }}>
                Register your medical license, qualification, and hospital affiliation to start accepting patient consultations.
              </div>
            </div>
          </div>
          <button
            onClick={() => setIsRegModalOpen(true)}
            className="btn btn-primary"
            style={{
              padding: '0.45rem 1rem',
              fontSize: '0.85rem',
              fontWeight: 700,
              backgroundColor: '#2563eb',
              color: '#ffffff',
            }}
          >
            Complete Registration Now
          </button>
        </div>
      )}

      {/* Main Workstation Layout */}
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(340px, 1fr))', gap: '1.5rem' }}>
        {/* Left Column: Triage Consultation Queue */}
        <div>
          <div className="card-header" style={{ padding: '0 0 0.5rem 0' }}>
            <div className="card-title">
              <Clock size={20} color="var(--primary)" />
              <span>{t('docQueueTitle')}</span>
            </div>
            <button
              className="btn btn-outline"
              style={{ minHeight: '34px', padding: '0 0.5rem', fontSize: '0.8rem' }}
              onClick={() => fetchDoctorQueue()}
            >
              <RefreshCw size={14} />
              <span>Refresh</span>
            </button>
          </div>

          {isQueueLoading ? (
            <div style={{ padding: '2rem', textAlign: 'center', color: 'var(--text-subtle)' }}>
              Loading queue...
            </div>
          ) : pendingQueue.length === 0 ? (
            <div className="card" style={{ textAlign: 'center', padding: '2.5rem 1rem' }}>
              <CheckCircle size={36} color="var(--primary)" style={{ margin: '0 auto 0.5rem' }} />
              <div style={{ fontWeight: 700 }}>Queue Clear</div>
              <div style={{ fontSize: '0.85rem', color: 'var(--text-subtle)', marginTop: '0.25rem' }}>
                No pending consultation requests at this time.
              </div>
            </div>
          ) : (
            <div style={{ display: 'flex', flexDirection: 'column', gap: '0.75rem' }}>
              {pendingQueue.map((item) => (
                <div
                  key={item.id}
                  className="card"
                  style={{
                    marginBottom: 0,
                    border: `2px solid ${selectedCase?.id === item.id ? 'var(--primary)' : 'var(--border-light)'}`,
                    cursor: 'pointer',
                  }}
                  onClick={() => setSelectedCase(item)}
                >
                  <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: '0.4rem' }}>
                    <div>
                      <div style={{ fontWeight: 800, fontSize: '1rem' }}>
                        {item.username || 'Patient Case'}
                      </div>
                      <div style={{ fontSize: '0.8rem', color: 'var(--text-subtle)' }}>
                        Case #{item.id} • {item.consultationType} Call
                      </div>
                    </div>
                    {getUrgencyBadge(item.urgency)}
                  </div>

                  <div style={{ fontSize: '0.85rem', color: 'var(--text-muted)', marginBottom: '0.75rem' }}>
                    <strong>Complaint:</strong> {item.disease}
                  </div>

                  <div style={{ display: 'flex', gap: '0.5rem', flexWrap: 'wrap' }}>
                    {item.status === 'REQUESTED' && (
                      <>
                        <button
                          className="btn btn-primary"
                          style={{ flex: 1, minHeight: '38px', fontSize: '0.8rem' }}
                          onClick={(e) => {
                            e.stopPropagation();
                            acceptConsultation(item.id);
                          }}
                        >
                          <CheckCircle size={14} />
                          <span>Accept</span>
                        </button>
                        <button
                          className="btn btn-outline"
                          style={{ minHeight: '38px', fontSize: '0.8rem', color: '#dc2626' }}
                          onClick={(e) => {
                            e.stopPropagation();
                            rejectConsultation(item.id);
                          }}
                        >
                          <XCircle size={14} />
                        </button>
                      </>
                    )}

                    {(item.status === 'ACCEPTED' || item.status === 'READY' || item.status === 'IN_CALL') && (
                      <button
                        className="btn btn-primary"
                        style={{ width: '100%', minHeight: '40px', fontSize: '0.85rem', fontWeight: 700 }}
                        onClick={(e) => {
                          e.stopPropagation();
                          startConsultation(item.id, item.username, currentDoctor?.doctorName || currentDoctor?.name || user?.displayName);
                        }}
                      >
                        <Video size={16} />
                        <span>Launch Video Call</span>
                      </button>
                    )}
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>

        {/* Right Column: Case Clinical Review & Notes */}
        <div>
          <div className="card-header" style={{ padding: '0 0 0.5rem 0' }}>
            <div className="card-title">
              <FileText size={20} color="var(--primary)" />
              <span>{t('docCaseReview')}</span>
            </div>
          </div>

          {selectedCase ? (
            <div className="card">
              <div style={{ borderBottom: '1px solid var(--border-light)', paddingBottom: '0.75rem', marginBottom: '0.75rem' }}>
                <div style={{ fontSize: '1.2rem', fontWeight: 800 }}>
                  {selectedCase.username}
                </div>
                <div style={{ fontSize: '0.85rem', color: 'var(--text-subtle)' }}>
                  Consultation #{selectedCase.id} • Status: <span className="badge badge-routine">{selectedCase.status}</span>
                </div>
              </div>

              <div style={{ marginBottom: '1rem', fontSize: '0.9rem' }}>
                <div style={{ fontWeight: 700, marginBottom: '0.25rem' }}>Chief Complaint:</div>
                <div style={{ background: 'var(--bg-subtle)', padding: '0.75rem', borderRadius: 'var(--radius-sm)' }}>
                  {selectedCase.disease}
                </div>
              </div>

              {/* Action buttons */}
              <div style={{ display: 'flex', flexDirection: 'column', gap: '0.5rem', marginTop: '1.5rem' }}>
                <button
                  className="btn btn-primary"
                  style={{ minHeight: '44px', fontWeight: 700 }}
                  onClick={() => startConsultation(selectedCase.id, selectedCase.username, currentDoctor?.doctorName || currentDoctor?.name || user?.displayName)}
                >
                  <Video size={18} />
                  <span>{t('docStartCall')}</span>
                </button>

                <button
                  className="btn btn-secondary"
                  style={{ minHeight: '44px' }}
                  onClick={() => completeConsultation(selectedCase.id)}
                >
                  <CheckCircle size={18} />
                  <span>{t('docComplete')}</span>
                </button>
              </div>
            </div>
          ) : (
            <div className="card" style={{ textAlign: 'center', padding: '3rem 1rem', color: 'var(--text-subtle)' }}>
              <User size={36} color="var(--border-medium)" style={{ margin: '0 auto 0.5rem' }} />
              <div>Select a consultation from the queue to review patient details and clinical findings.</div>
            </div>
          )}
        </div>
      </div>

      <DoctorRegistrationModal
        isOpen={isRegModalOpen}
        onClose={() => setIsRegModalOpen(false)}
        onSuccess={() => refreshCurrentDoctor()}
      />
    </div>
  );
};
