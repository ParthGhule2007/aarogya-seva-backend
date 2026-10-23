import React, { useState, useEffect } from 'react';
import {
  Stethoscope,
  Video,
  Phone,
  MessageSquare,
  AlertCircle,
  CheckCircle2,
  X,
  User,
  Clock,
  Sparkles,
  ArrowRight,
  Building2,
} from 'lucide-react';
import { consultationApi } from '../../api/consultationApi';
import { doctorApi } from '../../api/doctorApi';
import { patientApi } from '../../api/patientApi';
import { AvailableDoctorResponseDTO, DoctorResponseDTO } from '../../types/doctor';
import {
  ConsultationRequestDTO,
  ConsultationResponseDTO,
  ConsultationType,
  ConsultationUrgency,
} from '../../types/consultation';
import { PatientDto } from '../../types/patient';
import { useAuth } from '../../context/AuthContext';

interface RequestConsultationModalProps {
  doctor?: AvailableDoctorResponseDTO | DoctorResponseDTO;
  onClose: () => void;
  onSuccess: (consultation: ConsultationResponseDTO) => void;
}

export const RequestConsultationModal: React.FC<RequestConsultationModalProps> = ({
  doctor: initialDoctor,
  onClose,
  onSuccess,
}) => {
  const { user, selectedPatientId, selectedPatientName } = useAuth();

  // Patient Identity (UUID from Core or Auth)
  const [patientId, setPatientId] = useState<string>(
    () =>
      selectedPatientId ||
      user?.patientId ||
      (user?.role === 'PATIENT' ? user?.id : '') ||
      localStorage.getItem('aarogya_patient_id') ||
      ''
  );
  const [patientName, setPatientName] = useState<string>(
    () =>
      selectedPatientName ||
      user?.displayName ||
      user?.fullName ||
      localStorage.getItem('aarogya_patient_name') ||
      ''
  );
  const [patientPhone, setPatientPhone] = useState<string>('');

  // Fallback patient list if need to pick
  const [availablePatients, setAvailablePatients] = useState<PatientDto[]>([]);
  const [isRegisteringPatient, setIsRegisteringPatient] = useState(false);

  // Doctors list for doctor & specialty selection
  const [availableDoctors, setAvailableDoctors] = useState<AvailableDoctorResponseDTO[]>([]);
  const [selectedDoctorId, setSelectedDoctorId] = useState<number>(() => {
    if (initialDoctor) return initialDoctor.id;
    return 0;
  });

  // Form Fields for ConsultationRequestDTO
  const [disease, setDisease] = useState<string>('');
  const [urgency, setUrgency] = useState<ConsultationUrgency>('MEDIUM');
  const [consultationType, setConsultationType] = useState<ConsultationType>('VIDEO');

  // Submission State
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  // Fetch available doctors on mount
  useEffect(() => {
    doctorApi
      .getAvailableDoctors()
      .then((docs) => {
        if (docs && docs.length > 0) {
          setAvailableDoctors(docs);
          if (!initialDoctor) {
            setSelectedDoctorId(docs[0].id);
          }
        }
      })
      .catch((err) => console.warn('Could not load available doctors:', err));
  }, [initialDoctor]);

  // On mount, if no patientId in localStorage, try to fetch existing patients from Core
  useEffect(() => {
    if (!patientId) {
      patientApi
        .getAllPatients()
        .then((pts) => {
          if (pts && pts.length > 0) {
            setAvailablePatients(pts);
            const latest = pts[pts.length - 1];
            setPatientId(latest.id);
            setPatientName(latest.fullName);
            localStorage.setItem('aarogya_patient_id', latest.id);
            localStorage.setItem('aarogya_patient_name', latest.fullName);
          }
        })
        .catch((err) => console.warn('Could not auto-fetch patients:', err));
    }
  }, [patientId]);

  const handlePatientSelect = (e: React.ChangeEvent<HTMLSelectElement>) => {
    const selected = availablePatients.find((p) => p.id === e.target.value);
    if (selected) {
      setPatientId(selected.id);
      setPatientName(selected.fullName);
      localStorage.setItem('aarogya_patient_id', selected.id);
      localStorage.setItem('aarogya_patient_name', selected.fullName);
    }
  };

  const handleQuickRegisterPatient = async () => {
    if (!patientName.trim()) {
      setErrorMessage('Please enter your full name (कृपया आपले नाव प्रविष्ट करा).');
      return;
    }
    const cleanPhone = patientPhone.replace(/[^0-9]/g, '');
    const formattedPhone = cleanPhone.length === 10 ? `+91${cleanPhone}` : patientPhone.trim();
    if (!formattedPhone.match(/^(\+91)?[0-9]{10}$/)) {
      setErrorMessage('Please enter a valid 10-digit mobile number (कृपया 10 अंकी मोबाइल नंबर टाका).');
      return;
    }

    setIsRegisteringPatient(true);
    setErrorMessage(null);
    try {
      const created = await patientApi.createPatient({
        fullName: patientName.trim(),
        age: 35,
        gender: 'OTHER',
        contactNumber: formattedPhone,
        villageTown: 'Rural Center',
        subDistrict: 'District Sub-division',
        district: 'Pune',
        state: 'Maharashtra',
        pincode: '411001',
      });
      setPatientId(created.id);
      localStorage.setItem('aarogya_patient_id', created.id);
      localStorage.setItem('aarogya_patient_name', created.fullName);
    } catch (err: any) {
      setErrorMessage(err.message || 'Could not register patient profile. Please try again.');
    } finally {
      setIsRegisteringPatient(false);
    }
  };

  const currentDoctor =
    availableDoctors.find((d) => d.id === selectedDoctorId) ||
    initialDoctor ||
    null;

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setErrorMessage(null);

    if (!patientId) {
      setErrorMessage(
        'Please enter or select your patient name first (कृपया रुग्णाचे नाव निवडा किंवा नोंदवा).'
      );
      return;
    }

    if (!selectedDoctorId || !currentDoctor) {
      setErrorMessage(
        'Please select an available doctor for teleconsultation (कृपया उपलब्ध डॉक्टर निवडा).'
      );
      return;
    }

    if (!disease.trim()) {
      setErrorMessage(
        'Please describe your health concern or symptoms (कृपया आपल्या त्रासाचे वर्णन करा).'
      );
      return;
    }

    setIsSubmitting(true);
    try {
      const payload: ConsultationRequestDTO = {
        userId: patientId, // Core UUID passed as String
        username: patientName || 'Patient',
        doctorId: selectedDoctorId, // Doctor Service Long
        disease: disease.trim(),
        urgency,
        consultationType,
      };

      const result = await consultationApi.createConsultation(payload);
      onSuccess(result);
    } catch (err: any) {
      console.error('Failed to create consultation request:', err);
      setErrorMessage(
        err.message ||
          'Failed to send consultation request. Please check if Doctor and Consultation services are active.'
      );
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div
      role="dialog"
      aria-modal="true"
      aria-labelledby="consultation-modal-title"
      style={{
        position: 'fixed',
        top: 0,
        left: 0,
        right: 0,
        bottom: 0,
        backgroundColor: 'rgba(15, 23, 42, 0.75)',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        zIndex: 220,
        padding: '1rem',
      }}
    >
      <div
        style={{
          backgroundColor: 'var(--bg-surface, #FFFFFF)',
          borderRadius: '14px',
          maxWidth: '640px',
          width: '100%',
          maxHeight: '92vh',
          overflowY: 'auto',
          padding: '2rem',
          boxShadow: '0 20px 25px -5px rgba(0, 0, 0, 0.2)',
          position: 'relative',
          border: '2px solid var(--border-light, #E2E8F0)',
        }}
      >
        {/* Close Button */}
        <button
          onClick={onClose}
          aria-label="Close dialog"
          style={{
            position: 'absolute',
            top: '1.25rem',
            right: '1.25rem',
            background: 'var(--bg-subtle, #F1F5F9)',
            border: 'none',
            borderRadius: '50%',
            width: '36px',
            height: '36px',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            cursor: 'pointer',
          }}
        >
          <X size={20} color="var(--text-muted)" />
        </button>

        {/* Doctor Header Banner */}
        <div style={{ display: 'flex', alignItems: 'center', gap: '1rem', marginBottom: '1.5rem' }}>
          <div
            style={{
              width: '56px',
              height: '56px',
              borderRadius: '50%',
              backgroundColor: 'var(--primary-light, #CCFBF1)',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              color: 'var(--primary, #0F766E)',
            }}
          >
            <Stethoscope size={30} />
          </div>
          <div>
            <h2
              id="consultation-modal-title"
              style={{ fontSize: '1.45rem', fontWeight: 800, color: 'var(--primary, #0F766E)', margin: 0 }}
            >
              Book Teleconsultation • डॉक्टर सल्ला बुक करा
            </h2>
            <p style={{ fontSize: '0.95rem', color: 'var(--text-muted, #64748B)', margin: '0.2rem 0 0 0' }}>
              Connect over secure video or phone with verified MBBS specialist
            </p>
          </div>
        </div>

        {errorMessage && (
          <div
            className="alert-box alert-danger"
            style={{ padding: '0.85rem 1rem', marginBottom: '1.25rem', borderRadius: '8px' }}
          >
            <AlertCircle size={20} />
            <div style={{ fontSize: '0.9rem' }}>{errorMessage}</div>
          </div>
        )}

        <form onSubmit={handleSubmit} style={{ display: 'flex', flexDirection: 'column', gap: '1.25rem' }}>
          {/* Doctor & Specialty Selection */}
          <div
            style={{
              backgroundColor: 'var(--bg-subtle, #F8FAFC)',
              padding: '1rem 1.25rem',
              borderRadius: '10px',
              border: '1px solid var(--border-light, #E2E8F0)',
            }}
          >
            <label
              style={{
                display: 'block',
                fontWeight: 700,
                fontSize: '0.95rem',
                color: 'var(--text-main, #0F172A)',
                marginBottom: '0.4rem',
              }}
            >
              Select Doctor / Specialty • डॉक्टर व विशेष विभाग निवडा:
            </label>

            {availableDoctors.length > 0 ? (
              <select
                className="input-field"
                value={selectedDoctorId}
                onChange={(e) => setSelectedDoctorId(parseInt(e.target.value, 10))}
                style={{ width: '100%', padding: '0.55rem', fontSize: '0.95rem', fontWeight: 600 }}
              >
                {availableDoctors.map((doc) => (
                  <option key={doc.id} value={doc.id}>
                    {doc.name} — {doc.specialization} ({doc.hospitalName || 'District Hospital'})
                  </option>
                ))}
              </select>
            ) : currentDoctor ? (
              <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', fontWeight: 700, color: '#0F766E' }}>
                <Stethoscope size={18} />
                <span>{currentDoctor.name} ({currentDoctor.specialization})</span>
              </div>
            ) : (
              <div
                style={{
                  display: 'flex',
                  alignItems: 'center',
                  gap: '0.65rem',
                  padding: '0.75rem',
                  backgroundColor: '#FEF3C7',
                  color: '#92400E',
                  borderRadius: '8px',
                  fontSize: '0.9rem',
                }}
              >
                <AlertCircle size={20} style={{ flexShrink: 0 }} />
                <div>
                  <strong>No Doctors Currently Available (सध्या कोणतेही डॉक्टर उपलब्ध नाहीत)</strong>
                  <p style={{ margin: '0.15rem 0 0 0', fontSize: '0.85rem' }}>
                    All doctors are currently busy or offline. Please check back shortly.
                  </p>
                </div>
              </div>
            )}
          </div>

          {/* Patient Profile Selection */}
          <div
            style={{
              backgroundColor: 'var(--bg-subtle, #F8FAFC)',
              padding: '1rem 1.25rem',
              borderRadius: '10px',
              border: '1px solid var(--border-light, #E2E8F0)',
            }}
          >
            <label
              style={{
                display: 'block',
                fontWeight: 700,
                fontSize: '0.95rem',
                color: 'var(--text-main, #0F172A)',
                marginBottom: '0.4rem',
              }}
            >
              Patient Name • रुग्णाचे नाव
            </label>

            {patientId ? (
              <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', flexWrap: 'wrap', gap: '0.5rem' }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                  <User size={20} color="var(--primary, #0F766E)" />
                  <strong style={{ fontSize: '1.05rem' }}>{patientName || 'Registered Patient'}</strong>
                </div>
                {availablePatients.length > 1 && (
                  <select
                    className="input-field"
                    value={patientId}
                    onChange={handlePatientSelect}
                    style={{ width: 'auto', padding: '0.3rem 0.6rem', fontSize: '0.85rem' }}
                  >
                    {availablePatients.map((p) => (
                      <option key={p.id} value={p.id}>
                        Switch: {p.fullName}
                      </option>
                    ))}
                  </select>
                )}
              </div>
            ) : (
              <div>
                <p style={{ fontSize: '0.85rem', color: 'var(--text-muted)', marginBottom: '0.4rem' }}>
                  Enter your name to register patient profile:
                </p>
                <div style={{ display: 'flex', gap: '0.5rem', flexWrap: 'wrap' }}>
                  <input
                    type="text"
                    className="input-field"
                    placeholder="Full Name (e.g. Ramesh Patil)"
                    value={patientName}
                    onChange={(e) => setPatientName(e.target.value)}
                    style={{ flex: 1, minWidth: '150px' }}
                  />
                  <input
                    type="tel"
                    className="input-field"
                    placeholder="Mobile (10 digits)"
                    value={patientPhone}
                    onChange={(e) => setPatientPhone(e.target.value)}
                    style={{ width: '140px' }}
                  />
                  <button
                    type="button"
                    className="btn btn-outline"
                    onClick={handleQuickRegisterPatient}
                    disabled={isRegisteringPatient}
                    style={{ padding: '0.4rem 0.8rem', fontSize: '0.85rem' }}
                  >
                    {isRegisteringPatient ? 'Saving...' : 'Set Patient'}
                  </button>
                </div>
              </div>
            )}
          </div>

          {/* Health Concern / Problem */}
          <div>
            <label
              htmlFor="consultation-disease"
              style={{
                display: 'block',
                fontWeight: 700,
                fontSize: '1rem',
                color: 'var(--text-main, #0F172A)',
                marginBottom: '0.4rem',
              }}
            >
              Describe Symptoms & Health Concern • त्रास / लक्षणे सांगा *
            </label>
            <textarea
              id="consultation-disease"
              className="input-field"
              rows={3}
              placeholder="उदा. गेल्या २ दिवसांपासून ताप, खोकला आणि छातीत कफ आहे... (e.g. High fever for 2 days with chest pain)"
              value={disease}
              onChange={(e) => setDisease(e.target.value)}
              style={{
                width: '100%',
                padding: '0.75rem',
                fontSize: '0.95rem',
                fontFamily: 'inherit',
                borderRadius: '8px',
              }}
            />
          </div>

          {/* Consultation Type Selection */}
          <div>
            <label
              style={{
                display: 'block',
                fontWeight: 700,
                fontSize: '1rem',
                color: 'var(--text-main, #0F172A)',
                marginBottom: '0.4rem',
              }}
            >
              How do you want to consult? • कसा सल्ला घ्यायचा आहे?
            </label>

            <div
              style={{
                display: 'grid',
                gridTemplateColumns: 'repeat(auto-fit, minmax(130px, 1fr))',
                gap: '0.65rem',
              }}
            >
              <button
                type="button"
                onClick={() => setConsultationType('VIDEO')}
                style={{
                  padding: '0.85rem 0.65rem',
                  borderRadius: '10px',
                  border:
                    consultationType === 'VIDEO'
                      ? '3px solid var(--primary, #0F766E)'
                      : '1.5px solid var(--border-light, #CBD5E1)',
                  backgroundColor:
                    consultationType === 'VIDEO'
                      ? '#F0FDFA'
                      : '#FFFFFF',
                  cursor: 'pointer',
                  display: 'flex',
                  flexDirection: 'column',
                  alignItems: 'center',
                  gap: '0.35rem',
                  fontWeight: 700,
                  color: consultationType === 'VIDEO' ? 'var(--primary, #0F766E)' : 'var(--text-main)',
                }}
              >
                <Video size={24} color={consultationType === 'VIDEO' ? '#0F766E' : '#64748B'} />
                <span>Video Call</span>
                <span style={{ fontSize: '0.75rem', fontWeight: 500, color: 'var(--text-subtle)' }}>
                  व्हिडिओ कॉल
                </span>
              </button>

              <button
                type="button"
                onClick={() => setConsultationType('AUDIO')}
                style={{
                  padding: '0.85rem 0.65rem',
                  borderRadius: '10px',
                  border:
                    consultationType === 'AUDIO'
                      ? '3px solid var(--primary, #0F766E)'
                      : '1.5px solid var(--border-light, #CBD5E1)',
                  backgroundColor:
                    consultationType === 'AUDIO'
                      ? '#F0FDFA'
                      : '#FFFFFF',
                  cursor: 'pointer',
                  display: 'flex',
                  flexDirection: 'column',
                  alignItems: 'center',
                  gap: '0.35rem',
                  fontWeight: 700,
                  color: consultationType === 'AUDIO' ? 'var(--primary, #0F766E)' : 'var(--text-main)',
                }}
              >
                <Phone size={24} color={consultationType === 'AUDIO' ? '#0F766E' : '#64748B'} />
                <span>Phone Call</span>
                <span style={{ fontSize: '0.75rem', fontWeight: 500, color: 'var(--text-subtle)' }}>
                  फोन कॉल
                </span>
              </button>

              <button
                type="button"
                onClick={() => setConsultationType('CHAT')}
                style={{
                  padding: '0.85rem 0.65rem',
                  borderRadius: '10px',
                  border:
                    consultationType === 'CHAT'
                      ? '3px solid var(--primary, #0F766E)'
                      : '1.5px solid var(--border-light, #CBD5E1)',
                  backgroundColor:
                    consultationType === 'CHAT'
                      ? '#F0FDFA'
                      : '#FFFFFF',
                  cursor: 'pointer',
                  display: 'flex',
                  flexDirection: 'column',
                  alignItems: 'center',
                  gap: '0.35rem',
                  fontWeight: 700,
                  color: consultationType === 'CHAT' ? 'var(--primary, #0F766E)' : 'var(--text-main)',
                }}
              >
                <MessageSquare size={24} color={consultationType === 'CHAT' ? '#0F766E' : '#64748B'} />
                <span>Chat Message</span>
                <span style={{ fontSize: '0.75rem', fontWeight: 500, color: 'var(--text-subtle)' }}>
                  चॅट संदेश
                </span>
              </button>
            </div>
          </div>

          {/* Urgency Selection */}
          <div>
            <label
              style={{
                display: 'block',
                fontWeight: 700,
                fontSize: '1rem',
                color: 'var(--text-main, #0F172A)',
                marginBottom: '0.4rem',
              }}
            >
              Urgency Level • तातडीचे प्रमाण
            </label>

            <div
              style={{
                display: 'grid',
                gridTemplateColumns: 'repeat(auto-fit, minmax(110px, 1fr))',
                gap: '0.5rem',
              }}
            >
              {(
                [
                  { key: 'LOW', label: 'Routine • सामान्य', bg: '#F0FDF4', color: '#166534' },
                  { key: 'MEDIUM', label: 'Moderate • मध्यम', bg: '#EFF6FF', color: '#1E40AF' },
                  { key: 'HIGH', label: 'Urgent • तातडीचे', bg: '#FEF3C7', color: '#92400E' },
                  { key: 'EMERGENCY', label: 'Emergency • आणीबाणी', bg: '#FEE2E2', color: '#991B1B' },
                ] as const
              ).map((opt) => (
                <button
                  key={opt.key}
                  type="button"
                  onClick={() => setUrgency(opt.key)}
                  style={{
                    padding: '0.65rem 0.4rem',
                    borderRadius: '8px',
                    border:
                      urgency === opt.key
                        ? `3px solid var(--primary, #0F766E)`
                        : `1.5px solid var(--border-light, #CBD5E1)`,
                    backgroundColor: urgency === opt.key ? opt.bg : '#FFFFFF',
                    color: urgency === opt.key ? opt.color : 'inherit',
                    fontWeight: 700,
                    fontSize: '0.85rem',
                    cursor: 'pointer',
                    textAlign: 'center',
                  }}
                >
                  {opt.label}
                </button>
              ))}
            </div>
          </div>

          {/* Action Buttons */}
          <div style={{ display: 'flex', gap: '0.85rem', marginTop: '0.75rem' }}>
            <button
              type="button"
              className="btn btn-outline"
              style={{ flex: 1, minHeight: '44px' }}
              onClick={onClose}
              disabled={isSubmitting}
            >
              Cancel • रद्द करा
            </button>

            <button
              type="submit"
              className="btn btn-primary"
              style={{ flex: 2, minHeight: '44px', fontWeight: 800, backgroundColor: 'var(--primary, #0F766E)' }}
              disabled={isSubmitting || !currentDoctor || !selectedDoctorId}
            >
              {isSubmitting ? (
                <>
                  <div className="pulse-spinner" style={{ width: '20px', height: '20px', margin: 0 }} />
                  <span>Submitting Request...</span>
                </>
              ) : (
                <>
                  <CheckCircle2 size={20} />
                  <span>Send Consultation Request • विनंती पाठवा</span>
                </>
              )}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};
