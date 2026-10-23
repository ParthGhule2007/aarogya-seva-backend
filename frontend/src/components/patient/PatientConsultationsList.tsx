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
  Plus,
  Building2,
  ChevronRight,
  Info,
} from 'lucide-react';
import { consultationApi } from '../../api/consultationApi';
import {
  ConsultationResponseDTO,
  ConsultationStatus,
  ConsultationType,
} from '../../types/consultation';
import { ActiveConsultationCallModal } from './ActiveConsultationCallModal';
import { RequestConsultationModal } from './RequestConsultationModal';
import { useAuth } from '../../context/AuthContext';
import { useLanguage } from '../../context/LanguageContext';

interface PatientConsultationsListProps {
  onFindDoctors?: () => void;
}

export const PatientConsultationsList: React.FC<PatientConsultationsListProps> = ({
  onFindDoctors,
}) => {
  const { user, selectedPatientId, selectedPatientName } = useAuth();
  const { language } = useLanguage();

  // Resolved persistent patient identity
  const patientId =
    selectedPatientId ||
    user?.patientId ||
    (user?.role === 'PATIENT' ? user?.id : undefined) ||
    localStorage.getItem('aarogya_patient_id') ||
    '';

  const patientName =
    selectedPatientName ||
    user?.displayName ||
    user?.fullName ||
    localStorage.getItem('aarogya_patient_name') ||
    'Patient';

  const [consultations, setConsultations] = useState<ConsultationResponseDTO[]>([]);
  const [loading, setLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);
  const [cancellingId, setCancellingId] = useState<number | null>(null);
  const [joiningId, setJoiningId] = useState<number | null>(null);

  // Filter tabs: ACTIVE by default so cancelled cards do not clutter the active dashboard
  const [filterTab, setFilterTab] = useState<'ACTIVE' | 'HISTORY' | 'ALL'>('ACTIVE');

  // Active Call Modal State
  const [activeCallConsultation, setActiveCallConsultation] =
    useState<ConsultationResponseDTO | null>(null);
  const [completedNotice, setCompletedNotice] = useState<string | null>(null);

  // Book Consultation Modal State
  const [isBookModalOpen, setIsBookModalOpen] = useState(false);

  const fetchConsultations = async (silent = false) => {
    if (!patientId) {
      setLoading(false);
      return;
    }
    if (!silent) setLoading(true);
    setError(null);
    try {
      const data = await consultationApi.getConsultationsByUserId(patientId);
      setConsultations(data || []);
    } catch (err: any) {
      console.error('Failed to fetch patient consultations:', err);
      if (!silent) {
        setError(
          err.message ||
            'Connection problem: Unable to load your consultations. Please verify backend services are active.'
        );
      }
    } finally {
      if (!silent) setLoading(false);
    }
  };

  useEffect(() => {
    fetchConsultations();
  }, [patientId]);

  // Live Auto-Polling: Refresh every 5s while any consultation is active or pending
  useEffect(() => {
    const hasActiveOrPending = consultations.some(
      (c) =>
        c.status === 'REQUESTED' ||
        c.status === 'ACCEPTED' ||
        c.status === 'SCHEDULED' ||
        c.status === 'READY' ||
        c.status === 'IN_CALL'
    );

    if (!hasActiveOrPending && consultations.length > 0) return;

    const interval = setInterval(() => {
      fetchConsultations(true);
    }, 5000);

    return () => clearInterval(interval);
  }, [consultations, patientId]);

  const handleCancel = async (consultationId: number) => {
    if (
      !window.confirm(
        'Do you want to cancel this consultation request? (आपणास ही विनंती रद्द करायची आहे का?)'
      )
    ) {
      return;
    }
    setCancellingId(consultationId);
    try {
      await consultationApi.cancelConsultation(consultationId, {
        cancelledBy: 'USER',
      });
      await fetchConsultations(true);
    } catch (err: any) {
      alert(`Could not cancel request: ${err.message || 'Please try again.'}`);
    } finally {
      setCancellingId(null);
    }
  };

  /**
   * Join consultation flow:
   * Only permitted when backend confirms session is ready (READY or IN_CALL)
   */
  const handleJoinConsultation = async (consultation: ConsultationResponseDTO) => {
    if (joiningId) return;
    setJoiningId(consultation.id);
    setCompletedNotice(null);

    try {
      let current = consultation;
      if (current.status === 'READY') {
        try {
          current = await consultationApi.startConsultation(current.id);
        } catch (e) {
          console.warn('Could not transition status to IN_CALL, proceeding with session:', e);
        }
      }

      setActiveCallConsultation(current);
      await fetchConsultations(true);
    } catch (err: any) {
      console.error('Failed to join consultation:', err);
      alert(
        err.message ||
          'Unable to launch video session. Please ensure your camera and microphone permissions are granted.'
      );
    } finally {
      setJoiningId(null);
    }
  };

  const getStatusDisplay = (status: ConsultationStatus) => {
    switch (status) {
      case 'REQUESTED':
        return {
          label: 'Waiting for Doctor • डॉक्टरांच्या प्रतिसादाची वाट पहात आहे',
          badgeText: 'WAITING FOR DOCTOR',
          friendlyMessage: 'Your request has been sent to the doctor. Please wait for acceptance.',
          friendlyMarathi: 'आपली विनंती डॉक्टरांकडे पाठवली आहे. डॉक्टर स्वीकारतील तेव्हा येथे दिसेल.',
          nextAction: 'Waiting for doctor acceptance. This page updates automatically every 5 seconds.',
          nextActionMarathi: 'डॉक्टर स्वीकारण्याची वाट पहात आहे. हे पान आपोआप ताजे होत राहील.',
          badgeClass: 'status-pill pending',
          color: '#B45309',
          bg: '#FEF3C7',
          border: '#FCD34D',
        };
      case 'ACCEPTED':
        return {
          label: 'Accepted by Doctor • डॉक्टरांनी विनंती स्वीकारली',
          badgeText: 'ACCEPTED',
          friendlyMessage: 'Doctor has accepted your consultation! Preparing your session.',
          friendlyMarathi: 'डॉक्टरांनी विनंती स्वीकारली आहे! डॉक्टर कॉल सुरू करत आहेत, कृपया थांबा.',
          nextAction: 'Waiting for doctor to launch the call. Join button will activate once call starts.',
          nextActionMarathi: 'डॉक्टर कॉल सुरू करत आहेत. डॉक्टर सुरू करताच येथे हिरवे बटण येईल.',
          badgeClass: 'status-pill accepted',
          color: '#0F766E',
          bg: '#CCFBF1',
          border: '#5EEAD4',
        };
      case 'SCHEDULED':
        return {
          label: 'Appointment Scheduled • वेळ निश्चित झाली',
          badgeText: 'SCHEDULED',
          friendlyMessage: 'Your consultation appointment time has been scheduled.',
          friendlyMarathi: 'आपली भेट वेळ निश्चित झाली आहे. वेळेवर सल्ल्यामध्ये सामील व्हा.',
          nextAction: 'Please be ready at your scheduled appointment time.',
          nextActionMarathi: 'कृपया आपल्या निश्चित केलेल्या वेळेवर तयार रहा.',
          badgeClass: 'status-pill in-progress',
          color: '#4338CA',
          bg: '#E0E7FF',
          border: '#A5B4FC',
        };
      case 'READY':
        return {
          label: 'Video Call Ready • व्हिडिओ सल्ला तयार आहे',
          badgeText: 'VIDEO CALL READY',
          friendlyMessage: 'Your doctor has prepared the room! Tap Join Video Consultation now.',
          friendlyMarathi: 'डॉक्टर सल्ल्यासाठी तयार आहेत! कृपया खालील हिरव्या बटणावर दाबून लगेच सामील व्हा.',
          nextAction: 'Tap Join Video Consultation below to enter the live room.',
          nextActionMarathi: 'थेट खोलीत जाण्यासाठी खालील बटण दाबा.',
          badgeClass: 'status-pill accepted',
          color: '#15803D',
          bg: '#DCFCE7',
          border: '#86EFAC',
        };
      case 'IN_CALL':
        return {
          label: 'Doctor Started Call • व्हिडिओ सल्ला सुरू झाला आहे',
          badgeText: 'IN CONSULTATION',
          friendlyMessage: 'Your doctor has started the live video consultation! Tap Join below.',
          friendlyMarathi: 'डॉक्टरांनी व्हिडिओ कॉल सुरू केला आहे! त्वरित खालील बटण दाबून सामील व्हा.',
          nextAction: 'Live video call is active. Tap Join Video Call to speak with the doctor.',
          nextActionMarathi: 'कॉल सुरू आहे. डॉक्टरांशी बोलण्यासाठी खाली सामील व्हा.',
          badgeClass: 'status-pill in-progress',
          color: '#15803D',
          bg: '#DCFCE7',
          border: '#86EFAC',
        };
      case 'COMPLETED':
        return {
          label: 'Completed • सल्ला यशस्वी पूर्ण झाला',
          badgeText: 'COMPLETED',
          friendlyMessage: 'Your consultation has been completed successfully.',
          friendlyMarathi: 'सल्ला यशस्वीरित्या पूर्ण झाला आहे. आवश्यक असल्यास आपण नवीन सल्ला मागू शकता.',
          nextAction: 'Consultation completed. Doctor prescriptions and notes are recorded in your history.',
          nextActionMarathi: 'सल्ला पूर्ण झाला आहे.',
          badgeClass: 'status-pill completed',
          color: '#334155',
          bg: '#F1F5F9',
          border: '#CBD5E1',
        };
      case 'REJECTED':
        return {
          label: 'Doctor Unavailable • डॉक्टर सध्या उपलब्ध नाहीत',
          badgeText: 'DECLINED',
          friendlyMessage: 'The doctor could not take this request at this time. Please book another doctor.',
          friendlyMarathi: 'डॉक्टर सध्या उपलब्ध नाहीत. कृपया इतर उपलब्ध डॉक्टरांशी सल्ला बुक करा.',
          nextAction: 'You can book another available specialist right away.',
          nextActionMarathi: 'आपण लगेच इतर उपलब्ध डॉक्टरांशी सल्ला बुक करू शकता.',
          badgeClass: 'status-pill rejected',
          color: '#991B1B',
          bg: '#FEE2E2',
          border: '#FCA5A5',
        };
      case 'CANCELLED':
        return {
          label: 'Cancelled • सल्ला रद्द केला',
          badgeText: 'CANCELLED',
          friendlyMessage: 'This consultation was cancelled.',
          friendlyMarathi: 'हा सल्ला रद्द करण्यात आला आहे.',
          nextAction: 'Request was cancelled. You can book a new consultation anytime.',
          nextActionMarathi: 'ही विनंती रद्द झाली आहे.',
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
        return <Video size={18} color="var(--primary, #0d9488)" />;
      case 'AUDIO':
        return <Phone size={18} color="#059669" />;
      case 'CHAT':
        return <MessageSquare size={18} color="#d97706" />;
    }
  };

  // Group consultations into Active vs Past/Cancelled
  const activeConsultations = consultations.filter(
    (c) =>
      c.status === 'REQUESTED' ||
      c.status === 'ACCEPTED' ||
      c.status === 'SCHEDULED' ||
      c.status === 'READY' ||
      c.status === 'IN_CALL'
  );

  const historyConsultations = consultations.filter(
    (c) =>
      c.status === 'COMPLETED' ||
      c.status === 'CANCELLED' ||
      c.status === 'REJECTED'
  );

  const displayedConsultations =
    filterTab === 'ACTIVE'
      ? activeConsultations
      : filterTab === 'HISTORY'
      ? historyConsultations
      : consultations;

  return (
    <div style={{ maxWidth: '880px', margin: '0 auto', display: 'flex', flexDirection: 'column', gap: '1.25rem' }}>
      {/* Top Banner with Book New Consultation Action */}
      <div
        className="card-simple"
        style={{
          padding: '1.5rem',
          backgroundColor: '#FFFFFF',
          border: '2px solid var(--border-light, #E2E8F0)',
          borderRadius: '12px',
          boxShadow: 'var(--shadow-sm)',
        }}
      >
        <div
          style={{
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
            gap: '1rem',
            flexWrap: 'wrap',
          }}
        >
          <div>
            <h1
              style={{
                fontSize: '1.65rem',
                fontWeight: 800,
                color: 'var(--primary, #0F766E)',
                marginBottom: '0.25rem',
              }}
            >
              My Doctor Consultations • माझे डॉक्टर सल्ले
            </h1>
            <p style={{ fontSize: '0.95rem', color: 'var(--text-muted, #64748B)', margin: 0 }}>
              Live teleconsultations with certified clinical doctors • Patient: <strong>{patientName}</strong>
            </p>
          </div>

          <div style={{ display: 'flex', gap: '0.65rem', flexWrap: 'wrap' }}>
            <button
              className="btn btn-outline"
              style={{
                padding: '0.55rem 1rem',
                fontSize: '0.9rem',
                minHeight: '44px',
              }}
              onClick={() => fetchConsultations()}
              disabled={loading}
              aria-label="Refresh consultations"
            >
              <RefreshCw size={16} className={loading ? 'pulse-spinner' : ''} />
              <span>Refresh • ताजे करा</span>
            </button>

            <button
              className="btn btn-primary"
              style={{
                padding: '0.55rem 1.25rem',
                fontSize: '0.95rem',
                fontWeight: 700,
                minHeight: '44px',
                backgroundColor: 'var(--primary, #0F766E)',
              }}
              onClick={() => setIsBookModalOpen(true)}
            >
              <Plus size={18} />
              <span>Book Consultation • नवीन सल्ला</span>
            </button>
          </div>
        </div>

        {/* Tab Navigation to cleanly separate Active vs Cancelled/Past */}
        <div
          style={{
            display: 'flex',
            gap: '0.5rem',
            marginTop: '1.25rem',
            borderTop: '1px solid var(--border-light, #E2E8F0)',
            paddingTop: '1rem',
          }}
        >
          <button
            onClick={() => setFilterTab('ACTIVE')}
            style={{
              padding: '0.45rem 1rem',
              borderRadius: '8px',
              border: filterTab === 'ACTIVE' ? '2px solid #0F766E' : '1px solid #CBD5E1',
              backgroundColor: filterTab === 'ACTIVE' ? '#F0FDFA' : '#FFFFFF',
              color: filterTab === 'ACTIVE' ? '#0F766E' : '#475569',
              fontWeight: 700,
              fontSize: '0.9rem',
              cursor: 'pointer',
              display: 'flex',
              alignItems: 'center',
              gap: '0.35rem',
            }}
          >
            <span>Active Consultations ({activeConsultations.length})</span>
            <span style={{ fontSize: '0.8rem', opacity: 0.8 }}>• चालू सल्ले</span>
          </button>

          <button
            onClick={() => setFilterTab('HISTORY')}
            style={{
              padding: '0.45rem 1rem',
              borderRadius: '8px',
              border: filterTab === 'HISTORY' ? '2px solid #0F766E' : '1px solid #CBD5E1',
              backgroundColor: filterTab === 'HISTORY' ? '#F0FDFA' : '#FFFFFF',
              color: filterTab === 'HISTORY' ? '#0F766E' : '#475569',
              fontWeight: 700,
              fontSize: '0.9rem',
              cursor: 'pointer',
              display: 'flex',
              alignItems: 'center',
              gap: '0.35rem',
            }}
          >
            <span>Past & Cancelled ({historyConsultations.length})</span>
            <span style={{ fontSize: '0.8rem', opacity: 0.8 }}>• मागील व रद्द</span>
          </button>

          <button
            onClick={() => setFilterTab('ALL')}
            style={{
              padding: '0.45rem 0.85rem',
              borderRadius: '8px',
              border: filterTab === 'ALL' ? '2px solid #0F766E' : '1px solid #CBD5E1',
              backgroundColor: filterTab === 'ALL' ? '#F0FDFA' : '#FFFFFF',
              color: filterTab === 'ALL' ? '#0F766E' : '#475569',
              fontWeight: 600,
              fontSize: '0.9rem',
              cursor: 'pointer',
            }}
          >
            <span>All ({consultations.length})</span>
          </button>
        </div>
      </div>

      {/* Completion Notice Banner */}
      {completedNotice && (
        <div
          className="alert-box alert-success"
          style={{ padding: '1rem 1.25rem', borderRadius: '10px' }}
        >
          <CheckCircle2 size={24} color="#059669" />
          <div>
            <strong style={{ fontSize: '1.05rem', display: 'block' }}>
              {completedNotice}
            </strong>
            <span style={{ fontSize: '0.9rem' }}>
              सल्ला यशस्वीरित्या पूर्ण झाला आहे. आवश्यक असल्यास आपण नवीन सल्ला मागू शकता.
            </span>
          </div>
        </div>
      )}

      {/* State 1: Loading */}
      {loading && (
        <div
          className="card"
          style={{
            textAlign: 'center',
            padding: '3rem 1.5rem',
            display: 'flex',
            flexDirection: 'column',
            alignItems: 'center',
            gap: '1rem',
          }}
        >
          <div className="pulse-spinner" />
          <h2 style={{ fontSize: '1.25rem', color: 'var(--primary, #0F766E)', margin: 0 }}>
            Loading your consultations...
          </h2>
          <p style={{ color: 'var(--text-muted)', margin: 0 }}>
            माहिती लोड होत आहे • Connecting to Consultation Service
          </p>
        </div>
      )}

      {/* State 2: Error with Retry */}
      {!loading && error && (
        <div
          className="alert-box alert-danger"
          style={{ flexDirection: 'column', gap: '0.75rem', padding: '1.5rem', borderRadius: '10px' }}
        >
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
            <AlertCircle size={28} />
            <strong style={{ fontSize: '1.15rem' }}>
              Connection problem • संपर्क समस्या
            </strong>
          </div>
          <p style={{ margin: 0, fontSize: '0.95rem' }}>{error}</p>
          <div>
            <button
              className="btn btn-primary"
              style={{ padding: '0.55rem 1.25rem', fontSize: '0.9rem' }}
              onClick={() => fetchConsultations()}
            >
              <RefreshCw size={16} />
              <span>Retry Connection • पुन्हा प्रयत्न करा</span>
            </button>
          </div>
        </div>
      )}

      {/* State 3: Empty List */}
      {!loading && !error && displayedConsultations.length === 0 && (
        <div
          className="card-simple"
          style={{
            padding: '3rem 1.5rem',
            textAlign: 'center',
            backgroundColor: '#FFFFFF',
            border: '2px dashed var(--border-light, #CBD5E1)',
            borderRadius: '12px',
          }}
        >
          <Stethoscope
            size={56}
            color="var(--primary, #0F766E)"
            style={{ margin: '0 auto 1rem auto' }}
          />
          <h2
            style={{
              fontSize: '1.35rem',
              color: 'var(--text-main, #0F172A)',
              marginBottom: '0.4rem',
            }}
          >
            {filterTab === 'ACTIVE'
              ? 'No Active Consultations Right Now'
              : filterTab === 'HISTORY'
              ? 'No Past Consultations on Record'
              : 'No Consultations Booked Yet'}
          </h2>
          <p
            style={{
              fontSize: '1rem',
              color: 'var(--text-muted, #64748B)',
              marginBottom: '1.5rem',
              maxWidth: '520px',
              margin: '0 auto 1.5rem auto',
            }}
          >
            {filterTab === 'ACTIVE'
              ? 'आपला कोणताही सल्ला सध्या चालू किंवा प्रलंबित नाही. डॉक्टरांशी सल्ला घेण्यासाठी खालील बटण दाबा.'
              : 'आपल्या खात्यावर अद्याप मागील सल्ले नोंदवलेले नाहीत.'}
          </p>
          {filterTab === 'ACTIVE' && (
            <button
              className="btn btn-primary"
              style={{
                margin: '0 auto',
                padding: '0.75rem 1.5rem',
                fontSize: '1rem',
                fontWeight: 700,
                minHeight: '48px',
              }}
              onClick={() => setIsBookModalOpen(true)}
            >
              <Plus size={20} />
              <span>Book Doctor Consultation Now • डॉक्टर सल्ला बुक करा</span>
            </button>
          )}
        </div>
      )}

      {/* State 4: Consultations Cards */}
      {!loading && !error && displayedConsultations.length > 0 && (
        <div style={{ display: 'flex', flexDirection: 'column', gap: '1.25rem' }}>
          {displayedConsultations.map((c) => {
            const statusInfo = getStatusDisplay(c.status);
            const isCancelling = cancellingId === c.id;
            const isJoining = joiningId === c.id;

            // Strict backend session readiness: Only READY or IN_CALL permits patient join
            const isCallReady = c.status === 'READY' || c.status === 'IN_CALL';

            const canCancel =
              c.status === 'REQUESTED' ||
              c.status === 'ACCEPTED' ||
              c.status === 'SCHEDULED';

            return (
              <div
                key={c.id}
                className="card-simple"
                style={{
                  padding: '1.5rem',
                  backgroundColor: '#FFFFFF',
                  border: isCallReady
                    ? '3px solid #10B981'
                    : c.status === 'ACCEPTED'
                    ? '2px solid #0F766E'
                    : `2px solid ${statusInfo.border}`,
                  borderRadius: '12px',
                  boxShadow: isCallReady
                    ? '0 6px 18px rgba(16, 185, 129, 0.25)'
                    : 'var(--shadow-sm)',
                  display: 'flex',
                  flexDirection: 'column',
                  gap: '1rem',
                }}
              >
                {/* Live Call Banner when doctor starts video consultation */}
                {isCallReady && (
                  <div
                    style={{
                      background: 'linear-gradient(135deg, #059669 0%, #047857 100%)',
                      color: '#FFFFFF',
                      padding: '0.85rem 1.25rem',
                      borderRadius: '8px',
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'space-between',
                      flexWrap: 'wrap',
                      gap: '0.75rem',
                      animation: 'pulse 2s infinite',
                    }}
                  >
                    <div style={{ display: 'flex', alignItems: 'center', gap: '0.65rem' }}>
                      <Video size={24} color="#FFFFFF" />
                      <div>
                        <strong style={{ fontSize: '1.05rem', display: 'block' }}>
                          Your doctor has started the video consultation!
                        </strong>
                        <span style={{ fontSize: '0.85rem', opacity: 0.95 }}>
                          डॉक्टर उपलब्ध आहेत! थेट व्हिडिओ सल्ल्यामध्ये त्वरित सामील व्हा.
                        </span>
                      </div>
                    </div>

                    <button
                      className="btn"
                      style={{
                        backgroundColor: '#FFFFFF',
                        color: '#047857',
                        fontWeight: 800,
                        fontSize: '0.95rem',
                        padding: '0.55rem 1.25rem',
                        border: 'none',
                        minHeight: '44px',
                        boxShadow: '0 2px 8px rgba(0,0,0,0.15)',
                      }}
                      onClick={() => handleJoinConsultation(c)}
                      disabled={isJoining}
                    >
                      {isJoining ? (
                        <>
                          <RefreshCw size={18} className="pulse-spinner" />
                          <span>Connecting...</span>
                        </>
                      ) : (
                        <>
                          <Video size={18} />
                          <span>Join Video Call Now • सामील व्हा</span>
                        </>
                      )}
                    </button>
                  </div>
                )}

                {/* Header: Doctor Info & Lifecycle Status */}
                <div
                  style={{
                    display: 'flex',
                    alignItems: 'flex-start',
                    justifyContent: 'space-between',
                    gap: '1rem',
                    flexWrap: 'wrap',
                  }}
                >
                  <div style={{ display: 'flex', alignItems: 'center', gap: '0.85rem' }}>
                    <div
                      style={{
                        width: '52px',
                        height: '52px',
                        borderRadius: '50%',
                        backgroundColor: isCallReady ? '#DCFCE7' : 'var(--primary-light, #CCFBF1)',
                        display: 'flex',
                        alignItems: 'center',
                        justifyContent: 'center',
                        color: isCallReady ? '#166534' : 'var(--primary, #0F766E)',
                        border: '2px solid rgba(0,0,0,0.06)',
                      }}
                    >
                      <Stethoscope size={26} />
                    </div>
                    <div>
                      <h3
                        style={{
                          fontSize: '1.25rem',
                          fontWeight: 800,
                          color: 'var(--text-main, #0F172A)',
                          margin: 0,
                        }}
                      >
                        {c.doctorName || `Doctor #${c.doctorId}`}
                      </h3>
                      <div
                        style={{
                          fontSize: '0.9rem',
                          color: 'var(--text-muted, #64748B)',
                          marginTop: '0.15rem',
                        }}
                      >
                        Consultation #{c.id} • Assigned Specialist
                      </div>
                    </div>
                  </div>

                  {/* Status Pill Badge */}
                  <div
                    style={{
                      padding: '0.4rem 0.85rem',
                      borderRadius: '9999px',
                      backgroundColor: statusInfo.bg,
                      color: statusInfo.color,
                      border: `1.5px solid ${statusInfo.border}`,
                      fontWeight: 800,
                      fontSize: '0.85rem',
                      display: 'flex',
                      alignItems: 'center',
                      gap: '0.4rem',
                    }}
                  >
                    <span>{statusInfo.label}</span>
                  </div>
                </div>

                {/* Details Box: Mode, Symptoms, Urgency */}
                <div
                  style={{
                    backgroundColor: 'var(--bg-subtle, #F8FAFC)',
                    padding: '1rem 1.25rem',
                    borderRadius: '10px',
                    border: '1px solid var(--border-light, #E2E8F0)',
                    display: 'grid',
                    gridTemplateColumns: 'repeat(auto-fit, minmax(220px, 1fr))',
                    gap: '0.85rem',
                  }}
                >
                  <div>
                    <span style={{ fontSize: '0.8rem', color: 'var(--text-muted)', display: 'block' }}>
                      Reason for Consultation • तक्रार / लक्षणे
                    </span>
                    <strong style={{ fontSize: '0.95rem', color: 'var(--text-main)' }}>
                      {c.disease || 'General Medical Consultation'}
                    </strong>
                  </div>

                  <div>
                    <span style={{ fontSize: '0.8rem', color: 'var(--text-muted)', display: 'block' }}>
                      Preferred Mode & Urgency • माध्यम व निकड
                    </span>
                    <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', marginTop: '0.15rem' }}>
                      <span style={{ display: 'inline-flex', alignItems: 'center', gap: '0.35rem', fontWeight: 600 }}>
                        {getTypeIcon(c.consultationType)}
                        {c.consultationType || 'VIDEO'}
                      </span>
                      <span>•</span>
                      <span
                        style={{
                          fontWeight: 700,
                          fontSize: '0.85rem',
                          color:
                            c.urgency === 'EMERGENCY'
                              ? '#DC2626'
                              : c.urgency === 'HIGH'
                              ? '#EA580C'
                              : '#059669',
                        }}
                      >
                        {c.urgency || 'MEDIUM'}
                      </span>
                    </div>
                  </div>

                  <div>
                    <span style={{ fontSize: '0.8rem', color: 'var(--text-muted)', display: 'block' }}>
                      Requested Time • विनंती वेळ
                    </span>
                    <span style={{ fontSize: '0.9rem', color: 'var(--text-main)', fontWeight: 600 }}>
                      {c.requestedAt ? new Date(c.requestedAt).toLocaleString() : 'Recent'}
                    </span>
                  </div>

                  {c.scheduledAt && (
                    <div style={{ gridColumn: '1 / -1' }}>
                      <span style={{ fontSize: '0.8rem', color: '#4338CA', fontWeight: 700, display: 'block' }}>
                        Confirmed Appointment Time • भेटीची निश्चित वेळ
                      </span>
                      <strong style={{ fontSize: '1.05rem', color: '#3730A3' }}>
                        {new Date(c.scheduledAt).toLocaleString()}
                      </strong>
                    </div>
                  )}
                </div>

                {/* Clear Next Action & Instructions */}
                <div
                  style={{
                    backgroundColor: statusInfo.bg,
                    border: `1px solid ${statusInfo.border}`,
                    borderRadius: '8px',
                    padding: '0.75rem 1rem',
                    fontSize: '0.9rem',
                    color: statusInfo.color,
                    display: 'flex',
                    alignItems: 'center',
                    gap: '0.65rem',
                  }}
                >
                  <Info size={18} style={{ flexShrink: 0 }} />
                  <div>
                    <strong>Next Step: </strong>
                    <span>{statusInfo.nextAction}</span>
                    <span style={{ display: 'block', fontSize: '0.82rem', marginTop: '0.15rem', opacity: 0.9 }}>
                      {statusInfo.nextActionMarathi}
                    </span>
                  </div>
                </div>

                {/* Actions Row */}
                <div
                  style={{
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'space-between',
                    gap: '0.75rem',
                    flexWrap: 'wrap',
                    borderTop: '1px solid var(--border-light, #E2E8F0)',
                    paddingTop: '0.85rem',
                  }}
                >
                  <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                    {isCallReady ? (
                      <button
                        className="btn btn-primary"
                        style={{
                          backgroundColor: '#10B981',
                          borderColor: '#059669',
                          fontWeight: 800,
                          minHeight: '44px',
                          padding: '0.55rem 1.25rem',
                          fontSize: '0.95rem',
                        }}
                        onClick={() => handleJoinConsultation(c)}
                        disabled={isJoining}
                      >
                        <Video size={18} />
                        <span>Join Video Consultation • सल्ल्यामध्ये सामील व्हा</span>
                      </button>
                    ) : c.status === 'ACCEPTED' ? (
                      <div
                        style={{
                          padding: '0.5rem 1rem',
                          backgroundColor: '#CCFBF1',
                          color: '#0F766E',
                          borderRadius: '8px',
                          fontWeight: 700,
                          fontSize: '0.9rem',
                          display: 'flex',
                          alignItems: 'center',
                          gap: '0.5rem',
                        }}
                      >
                        <div className="pulse-spinner" style={{ width: '14px', height: '14px', borderWidth: '2px' }} />
                        <span>Waiting for Doctor to Start Call • डॉक्टर सुरू करत आहेत...</span>
                      </div>
                    ) : null}
                  </div>

                  <div>
                    {canCancel && (
                      <button
                        className="btn btn-outline"
                        style={{
                          borderColor: '#DC2626',
                          color: '#DC2626',
                          padding: '0.45rem 0.85rem',
                          fontSize: '0.85rem',
                          minHeight: '38px',
                        }}
                        onClick={() => handleCancel(c.id)}
                        disabled={isCancelling}
                      >
                        {isCancelling ? 'Cancelling...' : 'Cancel Request • विनंती रद्द करा'}
                      </button>
                    )}

                    {(c.status === 'COMPLETED' || c.status === 'CANCELLED' || c.status === 'REJECTED') && (
                      <button
                        className="btn btn-outline"
                        style={{
                          padding: '0.45rem 0.85rem',
                          fontSize: '0.85rem',
                          minHeight: '38px',
                        }}
                        onClick={() => setIsBookModalOpen(true)}
                      >
                        <span>Book Again • पुन्हा बुक करा</span>
                      </button>
                    )}
                  </div>
                </div>
              </div>
            );
          })}
        </div>
      )}

      {/* Request Consultation Modal */}
      {isBookModalOpen && (
        <RequestConsultationModal
          onClose={() => setIsBookModalOpen(false)}
          onSuccess={(newConsultation) => {
            setIsBookModalOpen(false);
            setFilterTab('ACTIVE');
            fetchConsultations(true);
          }}
        />
      )}

      {/* Active Consultation Call Modal */}
      {activeCallConsultation && (
        <ActiveConsultationCallModal
          consultation={activeCallConsultation}
          role="PATIENT"
          participantName={patientName}
          onClose={() => {
            setActiveCallConsultation(null);
            fetchConsultations(true);
          }}
          onCompleted={(updated) => {
            setActiveCallConsultation(null);
            setCompletedNotice(
              `Consultation #${updated.id} completed. (सल्ला यशस्वीरित्या पूर्ण झाला आहे.)`
            );
            fetchConsultations(true);
          }}
        />
      )}
    </div>
  );
};
