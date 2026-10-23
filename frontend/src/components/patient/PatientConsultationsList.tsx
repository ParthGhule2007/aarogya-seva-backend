import React, { useState, useEffect } from 'react';
import {
  Stethoscope,
  Video,
  Phone,
  MessageSquare,
  Calendar,
  Clock,
  CheckCircle2,
  AlertCircle,
  XCircle,
  RefreshCw,
  ArrowRight,
  User,
  ShieldCheck,
  PlayCircle,
  ExternalLink,
} from 'lucide-react';
import { consultationApi } from '../../api/consultationApi';
import {
  ConsultationResponseDTO,
  ConsultationStatus,
  ConsultationType,
} from '../../types/consultation';
import { ActiveConsultationCallModal } from './ActiveConsultationCallModal';

interface PatientConsultationsListProps {
  onFindDoctors: () => void;
}

export const PatientConsultationsList: React.FC<PatientConsultationsListProps> = ({
  onFindDoctors,
}) => {
  const [patientId, setPatientId] = useState<string>(
    () => localStorage.getItem('aarogya_patient_id') || ''
  );
  const [patientName, _setPatientName] = useState<string>(
    () => localStorage.getItem('aarogya_patient_name') || ''
  );

  const [consultations, setConsultations] = useState<ConsultationResponseDTO[]>([]);
  const [loading, setLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);
  const [cancellingId, setCancellingId] = useState<number | null>(null);
  const [joiningId, setJoiningId] = useState<number | null>(null);
  const [simulatingId, setSimulatingId] = useState<number | null>(null);

  // Active Call Modal State
  const [activeCallConsultation, setActiveCallConsultation] = useState<ConsultationResponseDTO | null>(null);
  const [completedNotice, setCompletedNotice] = useState<string | null>(null);

  const fetchConsultations = async () => {
    if (!patientId) {
      setLoading(false);
      return;
    }
    setLoading(true);
    setError(null);
    try {
      const data = await consultationApi.getConsultationsByUserId(patientId);
      setConsultations(data || []);
    } catch (err: any) {
      console.error('Failed to fetch patient consultations:', err);
      setError(
        err.message ||
          'Connection problem: Unable to load your consultations. Please try again.'
      );
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchConsultations();
  }, [patientId]);

  const handleCancel = async (consultationId: number) => {
    if (!window.confirm('Do you want to cancel this consultation request? (आपणास ही विनंती रद्द करायची आहे का?)')) {
      return;
    }
    setCancellingId(consultationId);
    try {
      await consultationApi.cancelConsultation(consultationId, {
        cancelledBy: 'USER',
      });
      await fetchConsultations();
    } catch (err: any) {
      alert(`Could not cancel request: ${err.message || 'Please try again.'}`);
    } finally {
      setCancellingId(null);
    }
  };

  /**
   * Join consultation flow:
   * 1. If ACCEPTED -> transition to READY via markAsReady
   * 2. If READY -> transition to IN_CALL via startConsultation (starts communication session)
   * 3. Opens ActiveConsultationCallModal
   */
  const handleJoinConsultation = async (consultation: ConsultationResponseDTO) => {
    if (joiningId) return; // Prevent double-clicks
    setJoiningId(consultation.id);
    setCompletedNotice(null);

    try {
      let current = consultation;

      // Transition to READY if in ACCEPTED state
      if (current.status === 'ACCEPTED') {
        current = await consultationApi.markAsReady(current.id);
      }

      // Transition to IN_CALL if in READY state (or if already IN_CALL, reuse)
      if (current.status === 'READY') {
        current = await consultationApi.startConsultation(current.id);
      }

      setActiveCallConsultation(current);
      await fetchConsultations();
    } catch (err: any) {
      console.error('Failed to join consultation:', err);
      alert(
        err.message ||
          'Connection problem: Unable to connect to consultation call. Please try again.'
      );
    } finally {
      setJoiningId(null);
    }
  };

  /**
   * For testing & demonstration: simulate doctor accepting request
   */
  const handleSimulateDoctorAccept = async (consultationId: number) => {
    setSimulatingId(consultationId);
    try {
      await consultationApi.acceptConsultation(consultationId);
      await fetchConsultations();
    } catch (err: any) {
      alert(`Could not simulate acceptance: ${err.message || 'Please verify Communication Service is running.'}`);
    } finally {
      setSimulatingId(null);
    }
  };

  const handleCallCompleted = (completed: ConsultationResponseDTO) => {
    setActiveCallConsultation(null);
    setCompletedNotice(
      `✓ Consultation with ${completed.doctorName || 'Doctor'} has been completed successfully.`
    );
    fetchConsultations();
  };

  // Human-friendly status translation for rural / elderly patients
  const getStatusDisplay = (status: ConsultationStatus) => {
    switch (status) {
      case 'REQUESTED':
        return {
          label: 'Request Sent • विनंती पाठवली आहे',
          description: 'Waiting for the doctor to review your request.',
          badgeClass: 'status-pill pending',
          color: '#92400E',
          bg: '#FEF3C7',
          border: '#FCD34D',
        };
      case 'ACCEPTED':
        return {
          label: 'Doctor Accepted • डॉक्टरांनी स्वीकारले',
          description: 'Doctor has accepted. Your consultation room is ready to join.',
          badgeClass: 'status-pill accepted',
          color: '#166534',
          bg: '#DCFCE7',
          border: '#86EFAC',
        };
      case 'SCHEDULED':
        return {
          label: 'Scheduled • वेळ निश्चित झाली',
          description: 'Your appointment is booked. Please join at scheduled time.',
          badgeClass: 'status-pill in-progress',
          color: '#3730A3',
          bg: '#E0E7FF',
          border: '#A5B4FC',
        };
      case 'READY':
        return {
          label: 'Ready to Join • सामील होण्यासाठी तयार',
          description: 'Doctor is waiting for you. Please join now.',
          badgeClass: 'status-pill accepted',
          color: '#166534',
          bg: '#DCFCE7',
          border: '#86EFAC',
        };
      case 'IN_CALL':
        return {
          label: 'Call in Progress • सल्ला सुरू आहे',
          description: 'Call is currently active.',
          badgeClass: 'status-pill in-progress',
          color: '#3730A3',
          bg: '#E0E7FF',
          border: '#A5B4FC',
        };
      case 'COMPLETED':
        return {
          label: 'Completed • पूर्ण झाले',
          description: 'Medical consultation completed.',
          badgeClass: 'status-pill completed',
          color: '#334155',
          bg: '#F1F5F9',
          border: '#CBD5E1',
        };
      case 'REJECTED':
        return {
          label: 'Doctor Unavailable • डॉक्टर उपलब्ध नाहीत',
          description: 'Doctor could not take this consultation. Please pick another doctor.',
          badgeClass: 'status-pill rejected',
          color: '#991B1B',
          bg: '#FEE2E2',
          border: '#FCA5A5',
        };
      case 'CANCELLED':
        return {
          label: 'Cancelled • रद्द केले',
          description: 'This consultation was cancelled.',
          badgeClass: 'status-pill rejected',
          color: '#991B1B',
          bg: '#FEE2E2',
          border: '#FCA5A5',
        };
    }
  };

  const getTypeIcon = (type: ConsultationType) => {
    switch (type) {
      case 'VIDEO':
        return <Video size={18} color="var(--primary)" />;
      case 'AUDIO':
        return <Phone size={18} color="var(--secondary)" />;
      case 'CHAT':
        return <MessageSquare size={18} color="var(--accent-warm)" />;
    }
  };

  return (
    <div style={{ maxWidth: '850px', margin: '0 auto' }}>
      {/* Top Banner */}
      <div
        className="card-simple"
        style={{
          padding: '1.75rem',
          backgroundColor: 'var(--bg-surface)',
          border: '2px solid var(--primary-border)',
          borderRadius: 'var(--radius-lg)',
          marginBottom: '1.5rem',
        }}
      >
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', gap: '1rem', flexWrap: 'wrap' }}>
          <div>
            <h1 style={{ fontSize: '1.75rem', fontWeight: 800, color: 'var(--primary)', marginBottom: '0.25rem' }}>
              My Consultations • माझे डॉक्टर सल्ले
            </h1>
            <p style={{ fontSize: '1.05rem', color: 'var(--text-muted)', margin: 0 }}>
              {patientName ? `Consultations for ${patientName}` : 'Track status and join online medical consultations'}
            </p>
          </div>

          <div style={{ display: 'flex', gap: '0.75rem' }}>
            <button
              className="btn-large btn-outline"
              style={{ padding: '0.6rem 1.1rem', fontSize: '0.95rem' }}
              onClick={fetchConsultations}
              disabled={loading}
              aria-label="Refresh consultations"
            >
              <RefreshCw size={18} className={loading ? 'pulse-spinner' : ''} />
              <span>Refresh • ताजे करा</span>
            </button>

            <button
              className="btn-large btn-primary"
              style={{ padding: '0.6rem 1.1rem', fontSize: '0.95rem' }}
              onClick={onFindDoctors}
            >
              <Stethoscope size={18} />
              <span>Find Doctors • डॉक्टर शोधा</span>
            </button>
          </div>
        </div>
      </div>

      {/* Completion Notice Banner */}
      {completedNotice && (
        <div
          className="alert-box alert-success"
          style={{ padding: '1rem 1.25rem', marginBottom: '1.5rem' }}
        >
          <CheckCircle2 size={24} color="var(--secondary)" />
          <div>
            <strong style={{ fontSize: '1.05rem', display: 'block' }}>
              {completedNotice}
            </strong>
            <span style={{ fontSize: '0.95rem' }}>
              सल्ला यशस्वीरित्या पूर्ण झाला आहे. आवश्यक असल्यास आपण नवीन सल्ला मागू शकता.
            </span>
          </div>
        </div>
      )}

      {/* State 1: Loading */}
      {loading && (
        <div className="loading-overlay">
          <div className="pulse-spinner" />
          <h2 style={{ fontSize: '1.35rem', color: 'var(--primary)', marginBottom: '0.5rem' }}>
            Loading your consultations...
          </h2>
          <p style={{ color: 'var(--text-muted)' }}>
            माहिती लोड होत आहे • Fetching consultation records
          </p>
        </div>
      )}

      {/* State 2: Error with Retry */}
      {!loading && error && (
        <div className="alert-box alert-danger" style={{ flexDirection: 'column', gap: '0.75rem', padding: '1.5rem' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
            <AlertCircle size={28} />
            <strong style={{ fontSize: '1.15rem' }}>
              Connection problem • संपर्क समस्या
            </strong>
          </div>
          <p style={{ margin: 0 }}>{error}</p>
          <button
            className="btn-large btn-primary"
            style={{ alignSelf: 'flex-start', padding: '0.6rem 1.25rem' }}
            onClick={fetchConsultations}
          >
            <RefreshCw size={18} />
            <span>Try Again • पुन्हा प्रयत्न करा</span>
          </button>
        </div>
      )}

      {/* State 3: Empty State */}
      {!loading && !error && consultations.length === 0 && (
        <div
          className="card-simple"
          style={{
            textAlign: 'center',
            padding: '3rem 1.5rem',
            backgroundColor: 'var(--bg-surface)',
          }}
        >
          <Stethoscope size={56} color="var(--border-medium)" style={{ margin: '0 auto 1rem auto' }} />
          <h2 style={{ fontSize: '1.4rem', color: 'var(--text-main)', marginBottom: '0.5rem' }}>
            No Consultations Found
          </h2>
          <p style={{ fontSize: '1.05rem', color: 'var(--text-muted)', marginBottom: '1.5rem' }}>
            आपण अद्याप कोणत्याही डॉक्टरांशी सल्लामसलत विनंती केलेली नाही. आपण उपलब्ध प्रमाणित डॉक्टरांची यादी पाहून थेट विनंती पाठवू शकता.
          </p>
          <button
            className="btn-large btn-primary"
            style={{ margin: '0 auto' }}
            onClick={onFindDoctors}
          >
            <Stethoscope size={20} />
            <span>Find Available Doctors • उपलब्ध डॉक्टर पहा</span>
          </button>
        </div>
      )}

      {/* State 4: Consultations List */}
      {!loading && !error && consultations.length > 0 && (
        <div style={{ display: 'flex', flexDirection: 'column', gap: '1.25rem' }}>
          {consultations.map((c) => {
            const statusInfo = getStatusDisplay(c.status);
            const isCancelling = cancellingId === c.id;
            const isJoining = joiningId === c.id;
            const isSimulating = simulatingId === c.id;
            const canJoin = c.status === 'ACCEPTED' || c.status === 'READY' || c.status === 'IN_CALL';

            return (
              <div
                key={c.id}
                className="card-simple"
                style={{
                  padding: '1.5rem',
                  backgroundColor: 'var(--bg-surface)',
                  border: canJoin ? '3px solid var(--secondary)' : `2px solid ${statusInfo.border}`,
                  borderRadius: 'var(--radius-lg)',
                  boxShadow: canJoin ? 'var(--shadow-md)' : 'var(--shadow-sm)',
                }}
              >
                {/* Header: Doctor Info & Status Badge */}
                <div
                  style={{
                    display: 'flex',
                    alignItems: 'flex-start',
                    justifyContent: 'space-between',
                    gap: '1rem',
                    flexWrap: 'wrap',
                    marginBottom: '1rem',
                  }}
                >
                  <div style={{ display: 'flex', alignItems: 'center', gap: '1rem' }}>
                    <div
                      style={{
                        width: '52px',
                        height: '52px',
                        borderRadius: '50%',
                        backgroundColor: canJoin ? 'var(--secondary-light)' : 'var(--primary-light)',
                        display: 'flex',
                        alignItems: 'center',
                        justifyContent: 'center',
                        color: canJoin ? 'var(--secondary)' : 'var(--primary)',
                        fontWeight: 800,
                        fontSize: '1.25rem',
                      }}
                    >
                      Dr
                    </div>
                    <div>
                      <h2
                        style={{
                          fontSize: '1.35rem',
                          fontWeight: 800,
                          color: 'var(--text-main)',
                          marginBottom: '0.2rem',
                        }}
                      >
                        {c.doctorName || `Doctor #${c.doctorId}`}
                      </h2>
                      <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', flexWrap: 'wrap' }}>
                        <span
                          style={{
                            display: 'inline-flex',
                            alignItems: 'center',
                            gap: '0.35rem',
                            backgroundColor: 'var(--bg-subtle)',
                            padding: '0.2rem 0.6rem',
                            borderRadius: 'var(--radius-full)',
                            fontSize: '0.85rem',
                            fontWeight: 600,
                          }}
                        >
                          {getTypeIcon(c.consultationType)}
                          <span>{c.consultationType} Call</span>
                        </span>

                        <span
                          style={{
                            display: 'inline-flex',
                            alignItems: 'center',
                            gap: '0.35rem',
                            fontSize: '0.85rem',
                            color: 'var(--text-subtle)',
                          }}
                        >
                          <Clock size={14} />
                          {c.requestedAt ? new Date(c.requestedAt).toLocaleString() : 'Recently'}
                        </span>
                      </div>
                    </div>
                  </div>

                  {/* Status Badge */}
                  <div
                    style={{
                      display: 'inline-flex',
                      alignItems: 'center',
                      gap: '0.4rem',
                      backgroundColor: statusInfo.bg,
                      color: statusInfo.color,
                      border: `1.5px solid ${statusInfo.border}`,
                      padding: '0.4rem 0.85rem',
                      borderRadius: 'var(--radius-full)',
                      fontWeight: 700,
                      fontSize: '0.95rem',
                    }}
                  >
                    <span>{statusInfo.label}</span>
                  </div>
                </div>

                {/* Disease / Complaint Description */}
                <div
                  style={{
                    backgroundColor: 'var(--bg-subtle)',
                    padding: '1rem',
                    borderRadius: 'var(--radius-md)',
                    marginBottom: '1rem',
                  }}
                >
                  <strong style={{ fontSize: '0.95rem', color: 'var(--text-muted)', display: 'block', marginBottom: '0.25rem' }}>
                    Reported Concern • त्रास / लक्षणे:
                  </strong>
                  <p style={{ margin: 0, fontSize: '1.05rem', color: 'var(--text-main)' }}>
                    {c.disease}
                  </p>
                </div>

                {/* Primary Join Action (Section 6: Elderly / Rural Join Experience) */}
                {canJoin && (
                  <div
                    style={{
                      padding: '1.25rem',
                      backgroundColor: 'var(--secondary-light)',
                      border: '2px solid var(--secondary)',
                      borderRadius: 'var(--radius-md)',
                      marginBottom: '1rem',
                      display: 'flex',
                      flexDirection: 'column',
                      gap: '1rem',
                    }}
                  >
                    <div>
                      <strong style={{ fontSize: '1.15rem', color: 'var(--secondary)', display: 'block', marginBottom: '0.25rem' }}>
                        ✓ Doctor is ready • डॉक्टर तयार आहेत
                      </strong>
                      <span style={{ fontSize: '0.95rem', color: 'var(--text-main)' }}>
                        {c.status === 'IN_CALL'
                          ? 'Your consultation is in progress. Tap below to resume the call.'
                          : 'Your consultation session is prepared. Tap the green button below to connect.'}
                      </span>
                    </div>

                    <button
                      className="btn-large btn-success"
                      style={{
                        padding: '1.1rem 1.75rem',
                        fontSize: '1.25rem',
                        fontWeight: 800,
                        borderRadius: 'var(--radius-md)',
                        boxShadow: '0 4px 12px rgba(16, 112, 80, 0.3)',
                        width: '100%',
                      }}
                      onClick={() => handleJoinConsultation(c)}
                      disabled={isJoining}
                      aria-label="Join Consultation Call"
                    >
                      {isJoining ? (
                        <>
                          <div className="pulse-spinner" style={{ width: '24px', height: '24px', margin: 0 }} />
                          <span>Connecting...</span>
                        </>
                      ) : (
                        <>
                          <Video size={28} />
                          <span>
                            {c.status === 'IN_CALL'
                              ? 'RESUME CONSULTATION • कॉल पुन्हा जोडा'
                              : 'JOIN CONSULTATION • सल्ल्यामध्ये सामील व्हा'}
                          </span>
                          <ArrowRight size={24} />
                        </>
                      )}
                    </button>
                  </div>
                )}

                {/* State: REQUESTED Actions */}
                {c.status === 'REQUESTED' && (
                  <div
                    style={{
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'space-between',
                      flexWrap: 'wrap',
                      gap: '0.75rem',
                      marginTop: '0.5rem',
                    }}
                  >
                    {/* Testing simulation button */}
                    <button
                      className="btn-large btn-outline"
                      style={{
                        padding: '0.45rem 0.85rem',
                        fontSize: '0.85rem',
                        borderColor: 'var(--secondary)',
                        color: 'var(--secondary)',
                      }}
                      onClick={() => handleSimulateDoctorAccept(c.id)}
                      disabled={isSimulating}
                      title="Simulates doctor accepting this consultation via backend"
                    >
                      {isSimulating ? 'Accepting...' : 'Doctor Accepts (Test) • स्वीकारा (चाचणी)'}
                    </button>

                    <button
                      className="btn-large btn-outline"
                      style={{
                        padding: '0.5rem 1rem',
                        fontSize: '0.9rem',
                        color: 'var(--risk-high)',
                        borderColor: 'var(--risk-high-border)',
                      }}
                      onClick={() => handleCancel(c.id)}
                      disabled={isCancelling}
                    >
                      <XCircle size={16} />
                      <span>{isCancelling ? 'Cancelling...' : 'Cancel Request • विनंती रद्द करा'}</span>
                    </button>
                  </div>
                )}

                {/* State: COMPLETED Details */}
                {c.status === 'COMPLETED' && (
                  <div style={{ fontSize: '0.9rem', color: 'var(--text-subtle)', marginTop: '0.5rem' }}>
                    ✓ Consultation finished on {c.completedAt ? new Date(c.completedAt).toLocaleString() : 'completed'}.
                  </div>
                )}
              </div>
            );
          })}
        </div>
      )}

      {/* Active Call Modal */}
      {activeCallConsultation && (
        <ActiveConsultationCallModal
          consultation={activeCallConsultation}
          onClose={() => setActiveCallConsultation(null)}
          onCompleted={handleCallCompleted}
        />
      )}
    </div>
  );
};
