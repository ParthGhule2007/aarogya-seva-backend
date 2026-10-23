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
  Calendar,
  Phone,
  MessageSquare,
  ArrowRight,
  ShieldAlert,
} from 'lucide-react';
import { ConsultationResponseDTO } from '../../types/consultation';
import { DoctorRegistrationModal } from './DoctorRegistrationModal';
import { ActiveConsultationCallModal } from '../patient/ActiveConsultationCallModal';
import { caseApi } from '../../api/caseApi';
import { PatientMedicalRecordDto } from '../../types/case';
import { VitalsGridDisplay } from '../common/medical/VitalCard';
import { RiskGauge } from '../common/medical/RiskGauge';
import { SymptomChip } from '../common/medical/SymptomChip';
import { StatusBadge } from '../common/medical/StatusBadge';

export const DoctorWorkspace: React.FC = () => {
  const { user } = useAuth();
  const {
    currentDoctor,
    availability,
    updateAvailability,
    isLoading: isDocLoading,
    refreshCurrentDoctor,
  } = useDoctor();

  const {
    pendingQueue,
    doctorConsultations,
    activeCallConsultation,
    callRole,
    participantName,
    fetchDoctorConsultations,
    acceptConsultation,
    rejectConsultation,
    scheduleConsultation,
    launchCall,
    completeConsultation,
    cancelConsultation,
    setActiveCallConsultation,
    isLoading: isQueueLoading,
  } = useConsultation();

  const { t } = useLanguage();

  const [activeTab, setActiveTab] = useState<'PENDING' | 'ACTIVE' | 'HISTORY'>('PENDING');
  const [selectedCase, setSelectedCase] = useState<ConsultationResponseDTO | null>(null);
  const [isRegModalOpen, setIsRegModalOpen] = useState(false);
  const [isAccepting, setIsAccepting] = useState<number | null>(null);
  const [isRejecting, setIsRejecting] = useState<number | null>(null);
  const [scheduleDateTime, setScheduleDateTime] = useState<string>('');
  const [schedulingId, setSchedulingId] = useState<number | null>(null);
  const [actionError, setActionError] = useState<string | null>(null);
  const [patientRecords, setPatientRecords] = useState<PatientMedicalRecordDto[]>([]);
  const [isLoadingRecords, setIsLoadingRecords] = useState<boolean>(false);

  // Load clinical history when a consultation case is selected
  useEffect(() => {
    const pId = selectedCase?.userId;
    if (!pId || !pId.includes('-')) {
      setPatientRecords([]);
      return;
    }
    setIsLoadingRecords(true);
    caseApi
      .getPatientMedicalRecords(pId)
      .then((records) => setPatientRecords(records || []))
      .catch(() => setPatientRecords([]))
      .finally(() => setIsLoadingRecords(false));
  }, [selectedCase?.userId]);

  const docId =
    currentDoctor?.doctorId ||
    currentDoctor?.id ||
    (user?.doctorId ? Number(user.doctorId) : undefined);

  // Poll doctor consultations every 6 seconds for live incoming requests
  useEffect(() => {
    if (!docId) return;
    fetchDoctorConsultations(docId);
    const interval = setInterval(() => {
      fetchDoctorConsultations(docId);
    }, 6000);
    return () => clearInterval(interval);
  }, [fetchDoctorConsultations, docId]);

  const handleStatusChange = async (e: React.ChangeEvent<HTMLSelectElement>) => {
    const status = e.target.value as DoctorAvailabilityStatus;
    try {
      await updateAvailability(status);
    } catch {
      // error handled in hook
    }
  };

  const getUrgencyBadge = (u: string) => {
    switch (u) {
      case 'EMERGENCY':
        return <span className="badge badge-emergency">EMERGENCY • आणीबाणी</span>;
      case 'HIGH':
      case 'URGENT':
        return <span className="badge badge-urgent">HIGH PRIORITY • तातडीचे</span>;
      default:
        return <span className="badge badge-routine">ROUTINE • सामान्य</span>;
    }
  };

  const getTypeIcon = (type: string) => {
    switch (type) {
      case 'VIDEO':
        return <Video size={16} color="var(--primary)" />;
      case 'AUDIO':
        return <Phone size={16} color="#0d9488" />;
      default:
        return <MessageSquare size={16} color="#d97706" />;
    }
  };

  // Categorize consultations
  const pendingRequests = doctorConsultations.filter(
    (c: ConsultationResponseDTO) => c.status === 'REQUESTED'
  );

  const activeConsultations = doctorConsultations.filter(
    (c: ConsultationResponseDTO) =>
      c.status === 'ACCEPTED' ||
      c.status === 'SCHEDULED' ||
      c.status === 'READY' ||
      c.status === 'IN_CALL'
  );

  const historyConsultations = doctorConsultations.filter(
    (c: ConsultationResponseDTO) =>
      c.status === 'COMPLETED' ||
      c.status === 'CANCELLED' ||
      c.status === 'REJECTED'
  );

  const handleAccept = async (cId: number) => {
    setIsAccepting(cId);
    setActionError(null);
    try {
      await acceptConsultation(cId);
      if (docId) await fetchDoctorConsultations(docId);
      setActiveTab('ACTIVE');
    } catch (err: any) {
      setActionError(
        err.message || 'Failed to accept consultation request. Please verify service availability.'
      );
    } finally {
      setIsAccepting(null);
    }
  };

  const handleReject = async (cId: number) => {
    if (!window.confirm('Are you sure you want to decline this consultation request? (आपण ही विनंती नाकारू इच्छिता का?)')) {
      return;
    }
    setIsRejecting(cId);
    setActionError(null);
    try {
      await rejectConsultation(cId);
      if (docId) await fetchDoctorConsultations(docId);
    } catch (err: any) {
      setActionError(err.message || 'Failed to decline consultation request.');
    } finally {
      setIsRejecting(null);
    }
  };

  const handleScheduleSubmit = async (cId: number) => {
    if (!scheduleDateTime) {
      alert('Please select a future appointment date and time.');
      return;
    }
    try {
      await scheduleConsultation(cId, scheduleDateTime);
      setSchedulingId(null);
      setScheduleDateTime('');
      if (docId) await fetchDoctorConsultations(docId);
    } catch (err: any) {
      alert(`Scheduling failed: ${err.message || 'Please pick a future time.'}`);
    }
  };

  const handleLaunchVideoCall = async (c: ConsultationResponseDTO) => {
    setActionError(null);
    try {
      const docName =
        currentDoctor?.doctorName ||
        currentDoctor?.name ||
        user?.displayName ||
        'Doctor';
      await launchCall(c, 'DOCTOR', docName);
      await fetchDoctorConsultations(docId);
    } catch (err: any) {
      setActionError(err.message || 'Failed to launch consultation video call.');
    }
  };

  return (
    <div style={{ maxWidth: '1150px', margin: '0 auto', display: 'flex', flexDirection: 'column', gap: '1.25rem' }}>
      {/* Doctor Header & Availability Bar */}
      <div
        className="card"
        style={{
          background: 'linear-gradient(135deg, #0d9488 0%, #0f766e 100%)',
          color: '#ffffff',
          padding: '1.5rem',
          borderRadius: 'var(--radius-lg, 12px)',
          boxShadow: 'var(--shadow-md)',
        }}
      >
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: '1rem' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.85rem' }}>
            <div
              style={{
                width: '56px',
                height: '56px',
                borderRadius: '50%',
                background: 'rgba(255, 255, 255, 0.2)',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
              }}
            >
              <Stethoscope size={30} color="#ffffff" />
            </div>
            <div>
              <h2 style={{ fontSize: '1.45rem', fontWeight: 800, margin: 0 }}>
                {currentDoctor?.doctorName || currentDoctor?.name || user?.displayName || user?.fullName || 'Clinical Doctor'}
              </h2>
              <div style={{ fontSize: '0.9rem', opacity: 0.95, marginTop: '0.2rem' }}>
                {currentDoctor?.specialization || 'Medical Officer'} • {currentDoctor?.licenseNumber ? `License: ${currentDoctor.licenseNumber}` : (docId ? `Doctor ID #${docId}` : 'Doctor Profile Pending Registration')}
              </div>
            </div>
          </div>

          <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem', flexWrap: 'wrap' }}>
            <button
              onClick={() => setIsRegModalOpen(true)}
              className="btn btn-outline"
              style={{
                color: '#ffffff',
                borderColor: 'rgba(255, 255, 255, 0.5)',
                background: 'rgba(255, 255, 255, 0.12)',
                minHeight: '36px',
                padding: '0 0.85rem',
                fontSize: '0.85rem',
                fontWeight: 600,
                borderRadius: '8px',
              }}
            >
              <Stethoscope size={15} />
              <span>{currentDoctor?.licenseNumber ? 'Edit Credentials' : 'Register Doctor Profile'}</span>
            </button>

            {/* Live Availability Toggle */}
            <div
              style={{
                display: 'flex',
                alignItems: 'center',
                gap: '0.5rem',
                background: 'rgba(0,0,0,0.3)',
                padding: '0.4rem 0.85rem',
                borderRadius: '8px',
                border: '1px solid rgba(255,255,255,0.2)',
              }}
            >
              <div
                style={{
                  width: '12px',
                  height: '12px',
                  borderRadius: '50%',
                  background:
                    availability === 'AVAILABLE'
                      ? '#4ade80'
                      : availability === 'BUSY'
                      ? '#facc15'
                      : '#ef4444',
                }}
              />
              <span style={{ fontSize: '0.85rem', fontWeight: 700 }}>Duty Status:</span>
              <select
                style={{
                  background: 'transparent',
                  color: '#ffffff',
                  border: '1px solid rgba(255,255,255,0.4)',
                  borderRadius: '4px',
                  padding: '0.2rem 0.4rem',
                  fontSize: '0.85rem',
                  fontWeight: 600,
                  cursor: 'pointer',
                }}
                value={availability}
                onChange={handleStatusChange}
                disabled={isDocLoading}
              >
                <option value="AVAILABLE" style={{ color: '#000000' }}>● Available (ड्यूटीवर)</option>
                <option value="BUSY" style={{ color: '#000000' }}>● In Consultation (व्यस्त)</option>
                <option value="OFFLINE" style={{ color: '#000000' }}>● Offline (ऑफ़लाइन)</option>
              </select>
            </div>
          </div>
        </div>
      </div>

      {actionError && (
        <div className="alert-box alert-danger" style={{ padding: '0.75rem 1.25rem' }}>
          <AlertTriangle size={20} />
          <span>{actionError}</span>
        </div>
      )}

      {/* Clinical KPI Summary Strip */}
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(210px, 1fr))', gap: '0.9rem' }}>
        <div className="stat-card-modern" onClick={() => setActiveTab('PENDING')} style={{ cursor: 'pointer' }}>
          <div style={{ fontSize: '0.78rem', color: 'var(--text-subtle)', fontWeight: 700, textTransform: 'uppercase' }}>
            Incoming Triage Queue • नवीन विनंत्या
          </div>
          <div style={{ fontSize: '1.8rem', fontWeight: 900, color: '#d97706', margin: '0.2rem 0' }}>
            {pendingRequests.length}
          </div>
          <div style={{ fontSize: '0.78rem', color: 'var(--text-muted)' }}>
            Awaiting physician acceptance
          </div>
        </div>

        <div className="stat-card-modern" onClick={() => setActiveTab('ACTIVE')} style={{ cursor: 'pointer' }}>
          <div style={{ fontSize: '0.78rem', color: 'var(--text-subtle)', fontWeight: 700, textTransform: 'uppercase' }}>
            Active Tele-Sessions • चालू सल्लामसलत
          </div>
          <div style={{ fontSize: '1.8rem', fontWeight: 900, color: 'var(--primary)', margin: '0.2rem 0' }}>
            {activeConsultations.length}
          </div>
          <div style={{ fontSize: '0.78rem', color: 'var(--text-muted)' }}>
            Ready for WebRTC video/audio call
          </div>
        </div>

        <div className="stat-card-modern" onClick={() => setActiveTab('HISTORY')} style={{ cursor: 'pointer' }}>
          <div style={{ fontSize: '0.78rem', color: 'var(--text-subtle)', fontWeight: 700, textTransform: 'uppercase' }}>
            Completed Cases • पूर्ण झालेले
          </div>
          <div style={{ fontSize: '1.8rem', fontWeight: 900, color: '#15803d', margin: '0.2rem 0' }}>
            {historyConsultations.length}
          </div>
          <div style={{ fontSize: '0.78rem', color: 'var(--text-muted)' }}>
            Archived in clinical registry
          </div>
        </div>
      </div>

      {/* Navigation Tabs */}
      <div style={{ display: 'flex', gap: '0.75rem', borderBottom: '2px solid var(--border-light)', paddingBottom: '0.25rem', flexWrap: 'wrap' }}>
        <button
          onClick={() => setActiveTab('PENDING')}
          style={{
            padding: '0.65rem 1.25rem',
            borderRadius: '8px 8px 0 0',
            border: 'none',
            background: activeTab === 'PENDING' ? 'var(--primary, #0f766e)' : 'transparent',
            color: activeTab === 'PENDING' ? '#ffffff' : 'var(--text-main)',
            fontWeight: 700,
            fontSize: '0.95rem',
            cursor: 'pointer',
            display: 'flex',
            alignItems: 'center',
            gap: '0.5rem',
          }}
        >
          <Clock size={16} />
          <span>Incoming Requests ({pendingRequests.length})</span>
        </button>

        <button
          onClick={() => setActiveTab('ACTIVE')}
          style={{
            padding: '0.65rem 1.25rem',
            borderRadius: '8px 8px 0 0',
            border: 'none',
            background: activeTab === 'ACTIVE' ? 'var(--primary, #0f766e)' : 'transparent',
            color: activeTab === 'ACTIVE' ? '#ffffff' : 'var(--text-main)',
            fontWeight: 700,
            fontSize: '0.95rem',
            cursor: 'pointer',
            display: 'flex',
            alignItems: 'center',
            gap: '0.5rem',
          }}
        >
          <Video size={16} />
          <span>Upcoming & Active ({activeConsultations.length})</span>
        </button>

        <button
          onClick={() => setActiveTab('HISTORY')}
          style={{
            padding: '0.65rem 1.25rem',
            borderRadius: '8px 8px 0 0',
            border: 'none',
            background: activeTab === 'HISTORY' ? 'var(--primary, #0f766e)' : 'transparent',
            color: activeTab === 'HISTORY' ? '#ffffff' : 'var(--text-main)',
            fontWeight: 700,
            fontSize: '0.95rem',
            cursor: 'pointer',
            display: 'flex',
            alignItems: 'center',
            gap: '0.5rem',
          }}
        >
          <FileText size={16} />
          <span>History & Completed ({historyConsultations.length})</span>
        </button>

        <button
          onClick={() => fetchDoctorConsultations(docId)}
          className="btn btn-outline"
          style={{
            marginLeft: 'auto',
            minHeight: '34px',
            padding: '0 0.75rem',
            fontSize: '0.8rem',
            display: 'flex',
            alignItems: 'center',
            gap: '0.4rem',
          }}
        >
          <RefreshCw size={14} className={isQueueLoading ? 'pulse-spinner' : ''} />
          <span>Refresh Live</span>
        </button>
      </div>

      {/* Main Workstation Layout */}
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(350px, 1fr))', gap: '1.5rem' }}>
        {/* Left Column: Consultation List */}
        <div>
          {/* Tab 1: PENDING REQUESTS */}
          {activeTab === 'PENDING' && (
            <div style={{ display: 'flex', flexDirection: 'column', gap: '0.85rem' }}>
              {pendingRequests.length === 0 ? (
                <div className="card" style={{ textAlign: 'center', padding: '3rem 1.5rem' }}>
                  <CheckCircle size={44} color="#059669" style={{ margin: '0 auto 0.75rem' }} />
                  <div style={{ fontWeight: 800, fontSize: '1.15rem' }}>Triage Queue Clear</div>
                  <div style={{ fontSize: '0.9rem', color: 'var(--text-muted)', marginTop: '0.35rem' }}>
                    No pending patient consultation requests waiting for review.
                  </div>
                </div>
              ) : (
                pendingRequests.map((item: ConsultationResponseDTO) => (
                  <div
                    key={item.id}
                    className="card"
                    style={{
                      padding: '1.25rem',
                      border: `2px solid ${selectedCase?.id === item.id ? 'var(--primary)' : 'var(--border-light)'}`,
                      borderRadius: '10px',
                      cursor: 'pointer',
                      boxShadow: 'var(--shadow-sm)',
                    }}
                    onClick={() => setSelectedCase(item)}
                  >
                    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: '0.5rem' }}>
                      <div>
                        <div style={{ fontWeight: 800, fontSize: '1.1rem', color: 'var(--text-main)' }}>
                          {item.username || 'Patient Case'}
                        </div>
                        <div style={{ fontSize: '0.8rem', color: 'var(--text-subtle)', display: 'flex', alignItems: 'center', gap: '0.4rem' }}>
                          <span>Case #{item.id}</span>
                          <span>•</span>
                          <span style={{ display: 'inline-flex', alignItems: 'center', gap: '0.2rem' }}>
                            {getTypeIcon(item.consultationType)} {item.consultationType}
                          </span>
                          <span>•</span>
                          <span>{item.requestedAt ? new Date(item.requestedAt).toLocaleTimeString() : 'Recently'}</span>
                        </div>
                      </div>
                      {getUrgencyBadge(item.urgency)}
                    </div>

                    <div style={{ background: 'var(--bg-subtle, #f8fafc)', padding: '0.75rem', borderRadius: '6px', marginBottom: '0.85rem' }}>
                      <strong style={{ fontSize: '0.85rem', color: 'var(--text-muted)' }}>Chief Complaint / त्रास:</strong>
                      <div style={{ fontSize: '0.95rem', color: 'var(--text-main)', marginTop: '0.2rem' }}>
                        {item.disease}
                      </div>
                    </div>

                    {/* Inline scheduling form if opened */}
                    {schedulingId === item.id ? (
                      <div style={{ display: 'flex', flexDirection: 'column', gap: '0.5rem', marginBottom: '0.75rem', background: '#eff6ff', padding: '0.75rem', borderRadius: '6px' }}>
                        <label style={{ fontSize: '0.85rem', fontWeight: 700, color: '#1e3a8a' }}>
                          Select Appointment Time (भेटण्याची वेळ निवडा):
                        </label>
                        <input
                          type="datetime-local"
                          value={scheduleDateTime}
                          onChange={(e) => setScheduleDateTime(e.target.value)}
                          style={{ padding: '0.4rem', borderRadius: '4px', border: '1px solid #bfdbfe' }}
                        />
                        <div style={{ display: 'flex', gap: '0.5rem' }}>
                          <button
                            className="btn btn-primary"
                            style={{ flex: 1, minHeight: '32px', fontSize: '0.8rem' }}
                            onClick={() => handleScheduleSubmit(item.id)}
                          >
                            Confirm Schedule
                          </button>
                          <button
                            className="btn btn-outline"
                            style={{ minHeight: '32px', fontSize: '0.8rem' }}
                            onClick={() => setSchedulingId(null)}
                          >
                            Cancel
                          </button>
                        </div>
                      </div>
                    ) : null}

                    {/* Action buttons */}
                    <div style={{ display: 'flex', gap: '0.5rem', flexWrap: 'wrap' }}>
                      <button
                        className="btn btn-primary"
                        style={{ flex: 2, minHeight: '38px', fontSize: '0.85rem', fontWeight: 700 }}
                        onClick={(e) => {
                          e.stopPropagation();
                          handleAccept(item.id);
                        }}
                        disabled={isAccepting === item.id}
                      >
                        <CheckCircle size={15} />
                        <span>{isAccepting === item.id ? 'Accepting...' : 'Accept Now • स्वीकारा'}</span>
                      </button>

                      <button
                        className="btn btn-outline"
                        style={{ minHeight: '38px', fontSize: '0.85rem' }}
                        onClick={(e) => {
                          e.stopPropagation();
                          setSchedulingId(item.id);
                        }}
                      >
                        <Calendar size={15} />
                        <span>Schedule</span>
                      </button>

                      <button
                        className="btn btn-outline"
                        style={{ minHeight: '38px', fontSize: '0.85rem', color: '#dc2626', borderColor: '#fca5a5' }}
                        onClick={(e) => {
                          e.stopPropagation();
                          handleReject(item.id);
                        }}
                        disabled={isRejecting === item.id}
                        title="Decline request"
                      >
                        <XCircle size={15} />
                      </button>
                    </div>
                  </div>
                ))
              )}
            </div>
          )}

          {/* Tab 2: UPCOMING & ACTIVE CONSULTATIONS */}
          {activeTab === 'ACTIVE' && (
            <div style={{ display: 'flex', flexDirection: 'column', gap: '0.85rem' }}>
              {activeConsultations.length === 0 ? (
                <div className="card" style={{ textAlign: 'center', padding: '3rem 1.5rem' }}>
                  <Video size={44} color="#64748b" style={{ margin: '0 auto 0.75rem' }} />
                  <div style={{ fontWeight: 800, fontSize: '1.15rem' }}>No Active Consultations</div>
                  <div style={{ fontSize: '0.9rem', color: 'var(--text-muted)', marginTop: '0.35rem' }}>
                    Accepted or scheduled consultations will appear here with launch buttons.
                  </div>
                </div>
              ) : (
                activeConsultations.map((item: ConsultationResponseDTO) => (
                  <div
                    key={item.id}
                    className="card"
                    style={{
                      padding: '1.25rem',
                      border: `2px solid ${item.status === 'IN_CALL' ? '#0d9488' : 'var(--border-light)'}`,
                      borderRadius: '10px',
                      cursor: 'pointer',
                      boxShadow: 'var(--shadow-sm)',
                    }}
                    onClick={() => setSelectedCase(item)}
                  >
                    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: '0.5rem' }}>
                      <div>
                        <div style={{ fontWeight: 800, fontSize: '1.1rem', color: 'var(--text-main)' }}>
                          {item.username || 'Patient Case'}
                        </div>
                        <div style={{ fontSize: '0.8rem', color: 'var(--text-subtle)' }}>
                          Case #{item.id} • Room: {item.roomId ? item.roomId.slice(0, 18) + '...' : 'Created'}
                        </div>
                      </div>
                      <span
                        className="badge"
                        style={{
                          background: item.status === 'IN_CALL' ? '#dcfce7' : '#e0e7ff',
                          color: item.status === 'IN_CALL' ? '#166534' : '#3730a3',
                          fontWeight: 700,
                        }}
                      >
                        {item.status}
                      </span>
                    </div>

                    <div style={{ fontSize: '0.9rem', color: 'var(--text-main)', marginBottom: '0.75rem' }}>
                      <strong>Complaint:</strong> {item.disease}
                    </div>

                    {item.scheduledAt && (
                      <div style={{ fontSize: '0.85rem', color: '#1d4ed8', marginBottom: '0.75rem', display: 'flex', alignItems: 'center', gap: '0.35rem' }}>
                        <Calendar size={14} />
                        <span>Scheduled for: {new Date(item.scheduledAt).toLocaleString()}</span>
                      </div>
                    )}

                    {/* Launch call button */}
                    <div style={{ display: 'flex', gap: '0.5rem', flexWrap: 'wrap' }}>
                      <button
                        className="btn btn-primary"
                        style={{
                          flex: 2,
                          minHeight: '40px',
                          fontSize: '0.9rem',
                          fontWeight: 800,
                          backgroundColor: '#0d9488',
                        }}
                        onClick={(e) => {
                          e.stopPropagation();
                          handleLaunchVideoCall(item);
                        }}
                      >
                        <Video size={17} />
                        <span>Launch Video Call • कॉल सुरू करा</span>
                      </button>

                      <button
                        className="btn btn-outline"
                        style={{ minHeight: '40px', fontSize: '0.85rem' }}
                        onClick={(e) => {
                          e.stopPropagation();
                          completeConsultation(item.id);
                        }}
                      >
                        <CheckCircle size={15} />
                        <span>Complete</span>
                      </button>
                    </div>
                  </div>
                ))
              )}
            </div>
          )}

          {/* Tab 3: HISTORY & COMPLETED */}
          {activeTab === 'HISTORY' && (
            <div style={{ display: 'flex', flexDirection: 'column', gap: '0.85rem' }}>
              {historyConsultations.length === 0 ? (
                <div className="card" style={{ textAlign: 'center', padding: '3rem 1.5rem', color: 'var(--text-muted)' }}>
                  <FileText size={44} style={{ margin: '0 auto 0.75rem' }} />
                  <div style={{ fontWeight: 700 }}>No Past Records</div>
                  <div>Completed and historical consultations will be archived here.</div>
                </div>
              ) : (
                historyConsultations.map((item: ConsultationResponseDTO) => (
                  <div
                    key={item.id}
                    className="card"
                    style={{
                      padding: '1.25rem',
                      borderRadius: '10px',
                      cursor: 'pointer',
                    }}
                    onClick={() => setSelectedCase(item)}
                  >
                    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '0.4rem' }}>
                      <div style={{ fontWeight: 800, fontSize: '1rem' }}>{item.username}</div>
                      <span className="badge badge-routine">{item.status}</span>
                    </div>
                    <div style={{ fontSize: '0.85rem', color: 'var(--text-muted)' }}>
                      <strong>Complaint:</strong> {item.disease}
                    </div>
                    <div style={{ fontSize: '0.8rem', color: 'var(--text-subtle)', marginTop: '0.35rem' }}>
                      {item.completedAt ? `Completed on ${new Date(item.completedAt).toLocaleString()}` : `Case #${item.id}`}
                    </div>
                  </div>
                ))
              )}
            </div>
          )}
        </div>

        {/* Right Column: Selected Case Details & Clinical Action Center */}
        <div>
          <div className="card-header" style={{ padding: '0 0 0.5rem 0' }}>
            <div className="card-title">
              <FileText size={20} color="var(--primary)" />
              <span>{t('docCaseReview') || 'Patient Clinical Review'}</span>
            </div>
          </div>

          {selectedCase ? (
            <div className="card" style={{ padding: '1.5rem', borderRadius: '12px' }}>
              <div style={{ borderBottom: '1px solid var(--border-light)', paddingBottom: '0.75rem', marginBottom: '1rem' }}>
                <div style={{ fontSize: '1.25rem', fontWeight: 800, color: 'var(--text-main)' }}>
                  {selectedCase.username}
                </div>
                <div style={{ fontSize: '0.85rem', color: 'var(--text-subtle)', marginTop: '0.2rem' }}>
                  Consultation Case #{selectedCase.id} • Status: <span className="badge badge-routine">{selectedCase.status}</span>
                </div>
              </div>

              <div style={{ display: 'flex', flexDirection: 'column', gap: '0.85rem', fontSize: '0.9rem' }}>
                <div>
                  <strong style={{ color: 'var(--text-muted)', display: 'block', marginBottom: '0.2rem' }}>
                    Chief Complaint & Symptoms:
                  </strong>
                  <div style={{ background: 'var(--bg-subtle, #f8fafc)', padding: '0.75rem', borderRadius: '6px' }}>
                    {selectedCase.disease}
                  </div>
                </div>

                <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '0.75rem' }}>
                  <div style={{ background: 'var(--bg-subtle, #f8fafc)', padding: '0.6rem', borderRadius: '6px' }}>
                    <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>Urgency:</span>
                    <div style={{ fontWeight: 700 }}>{selectedCase.urgency}</div>
                  </div>
                  <div style={{ background: 'var(--bg-subtle, #f8fafc)', padding: '0.6rem', borderRadius: '6px' }}>
                    <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>Mode:</span>
                    <div style={{ fontWeight: 700 }}>{selectedCase.consultationType} Call</div>
                  </div>
                </div>

                {selectedCase.roomId && (
                  <div>
                    <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>Room Identifier:</span>
                    <div style={{ fontSize: '0.8rem', fontFamily: 'monospace', wordBreak: 'break-all' }}>
                      {selectedCase.roomId}
                    </div>
                  </div>
                )}

                {/* Patient Clinical History & AI Triage Records */}
                <div style={{ borderTop: '1px solid var(--border-light)', paddingTop: '0.75rem', marginTop: '0.25rem' }}>
                  <strong style={{ color: 'var(--text-muted)', display: 'block', marginBottom: '0.4rem' }}>
                    Clinical History & AI Triage Records:
                  </strong>
                  {isLoadingRecords ? (
                    <div style={{ fontSize: '0.8rem', color: 'var(--text-subtle)' }}>Loading patient clinical records...</div>
                  ) : patientRecords.length === 0 ? (
                    <div style={{ fontSize: '0.8rem', color: 'var(--text-subtle)' }}>
                      No prior clinical cases found for patient ID {selectedCase.userId}.
                    </div>
                  ) : (
                    <div style={{ display: 'flex', flexDirection: 'column', gap: '0.75rem', maxHeight: '340px', overflowY: 'auto' }}>
                      {patientRecords.slice(0, 3).map((rec, idx) => (
                        <div
                          key={rec.caseDetails?.id || idx}
                          style={{
                            background: 'var(--bg-subtle, #f8fafc)',
                            padding: '0.75rem',
                            borderRadius: '8px',
                            border: '1px solid var(--border-light)',
                            fontSize: '0.82rem',
                            display: 'flex',
                            flexDirection: 'column',
                            gap: '0.5rem',
                          }}
                        >
                          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: '0.4rem' }}>
                            <strong style={{ color: 'var(--primary)' }}>
                              {rec.caseDetails?.caseNumber || rec.caseDetails?.id?.slice(0, 8)} • {rec.caseDetails?.chiefComplaint}
                            </strong>
                            {rec.caseDetails?.status && <StatusBadge status={rec.caseDetails.status} size="sm" />}
                          </div>

                          {rec.caseDetails?.vitals && (
                            <VitalsGridDisplay vitals={rec.caseDetails.vitals} compact />
                          )}

                          {rec.caseDetails?.symptoms && rec.caseDetails.symptoms.length > 0 && (
                            <div style={{ display: 'flex', gap: '0.35rem', flexWrap: 'wrap' }}>
                              {rec.caseDetails.symptoms.map((s, sIdx) => (
                                <SymptomChip
                                  key={sIdx}
                                  name={s.symptomName}
                                  severity={s.severity}
                                  durationDays={s.durationDays}
                                />
                              ))}
                            </div>
                          )}

                          {rec.aiAssessment && (
                            <RiskGauge
                              riskScore={rec.aiAssessment.riskScore}
                              riskLevel={rec.aiAssessment.riskLevel}
                              urgencyLevel={rec.aiAssessment.urgencyLevel}
                              confidence={rec.aiAssessment.confidence}
                              compact
                            />
                          )}
                        </div>
                      ))}
                    </div>
                  )}
                </div>
              </div>

              {/* Action buttons */}
              <div style={{ display: 'flex', flexDirection: 'column', gap: '0.65rem', marginTop: '1.5rem' }}>
                {(selectedCase.status === 'ACCEPTED' || selectedCase.status === 'READY' || selectedCase.status === 'IN_CALL') && (
                  <button
                    className="btn btn-primary"
                    style={{ minHeight: '44px', fontWeight: 800, backgroundColor: '#0d9488' }}
                    onClick={() => handleLaunchVideoCall(selectedCase)}
                  >
                    <Video size={18} />
                    <span>Launch Video Call with Patient</span>
                  </button>
                )}

                {selectedCase.status === 'REQUESTED' && (
                  <button
                    className="btn btn-primary"
                    style={{ minHeight: '44px', fontWeight: 700 }}
                    onClick={() => handleAccept(selectedCase.id)}
                    disabled={isAccepting === selectedCase.id}
                  >
                    <CheckCircle size={18} />
                    <span>Accept Patient Request</span>
                  </button>
                )}

                {(selectedCase.status === 'ACCEPTED' || selectedCase.status === 'IN_CALL') && (
                  <button
                    className="btn btn-secondary"
                    style={{ minHeight: '42px' }}
                    onClick={() => completeConsultation(selectedCase.id)}
                  >
                    <CheckCircle size={18} />
                    <span>Complete Consultation Case</span>
                  </button>
                )}
              </div>
            </div>
          ) : (
            <div className="card" style={{ textAlign: 'center', padding: '3.5rem 1.5rem', color: 'var(--text-subtle)' }}>
              <User size={40} color="var(--border-medium)" style={{ margin: '0 auto 0.75rem' }} />
              <div>Select a consultation from the list to review patient symptoms, triage notes, and launch call.</div>
            </div>
          )}
        </div>
      </div>

      {/* Embedded/Fullscreen Call Modal when call is active */}
      {activeCallConsultation && (
        <ActiveConsultationCallModal
          consultation={activeCallConsultation}
          role="DOCTOR"
          participantName={currentDoctor?.doctorName || currentDoctor?.name || user?.displayName || 'Doctor'}
          onClose={() => setActiveCallConsultation(null)}
          onCompleted={(updated) => {
            setActiveCallConsultation(null);
            fetchDoctorConsultations(docId);
          }}
        />
      )}

      {/* Doctor Registration Modal */}
      <DoctorRegistrationModal
        isOpen={isRegModalOpen}
        onClose={() => setIsRegModalOpen(false)}
        onSuccess={() => {
          refreshCurrentDoctor();
          fetchDoctorConsultations(docId);
        }}
      />
    </div>
  );
};
