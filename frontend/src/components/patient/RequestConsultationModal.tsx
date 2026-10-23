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
} from 'lucide-react';
import { consultationApi } from '../../api/consultationApi';
import { patientApi } from '../../api/patientApi';
import { AvailableDoctorResponseDTO, DoctorResponseDTO } from '../../types/doctor';
import {
  ConsultationRequestDTO,
  ConsultationResponseDTO,
  ConsultationType,
  ConsultationUrgency,
} from '../../types/consultation';
import { PatientDto } from '../../types/patient';

interface RequestConsultationModalProps {
  doctor: AvailableDoctorResponseDTO | DoctorResponseDTO;
  onClose: () => void;
  onSuccess: (consultation: ConsultationResponseDTO) => void;
}

export const RequestConsultationModal: React.FC<RequestConsultationModalProps> = ({
  doctor,
  onClose,
  onSuccess,
}) => {
  // Patient Identity (UUID from Core)
  const [patientId, setPatientId] = useState<string>(
    () => localStorage.getItem('aarogya_patient_id') || ''
  );
  const [patientName, setPatientName] = useState<string>(
    () => localStorage.getItem('aarogya_patient_name') || ''
  );
  const [patientPhone, setPatientPhone] = useState<string>('');

  // Fallback patient list if need to pick
  const [availablePatients, setAvailablePatients] = useState<PatientDto[]>([]);
  const [isRegisteringPatient, setIsRegisteringPatient] = useState(false);

  // Form Fields for ConsultationRequestDTO
  const [disease, setDisease] = useState<string>('');
  const [urgency, setUrgency] = useState<ConsultationUrgency>('MEDIUM');
  const [consultationType, setConsultationType] = useState<ConsultationType>('VIDEO');

  // Submission State
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

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

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setErrorMessage(null);

    if (!patientId) {
      setErrorMessage(
        'Please enter or select your patient name first (कृपया रुग्णाचे नाव निवडा किंवा नोंदवा).'
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
        doctorId: doctor.id, // Doctor Service Long
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
        backgroundColor: 'rgba(15, 23, 42, 0.7)',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        zIndex: 110,
        padding: '1rem',
      }}
    >
      <div
        style={{
          backgroundColor: 'var(--bg-surface)',
          borderRadius: 'var(--radius-lg)',
          maxWidth: '620px',
          width: '100%',
          maxHeight: '92vh',
          overflowY: 'auto',
          padding: '2rem',
          boxShadow: 'var(--shadow-lg)',
          position: 'relative',
          border: '2px solid var(--border-light)',
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
            background: 'var(--bg-subtle)',
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
              backgroundColor: 'var(--primary-light)',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              color: 'var(--primary)',
            }}
          >
            <Stethoscope size={30} />
          </div>
          <div>
            <h2
              id="consultation-modal-title"
              style={{ fontSize: '1.5rem', fontWeight: 800, color: 'var(--primary)', marginBottom: '0.2rem' }}
            >
              Request Consultation • सल्लामसलत विनंती
            </h2>
            <p style={{ fontSize: '1rem', color: 'var(--text-muted)', margin: 0 }}>
              Consulting with <strong>{doctor.name}</strong> ({doctor.specialization})
            </p>
          </div>
        </div>

        {errorMessage && (
          <div
            className="alert-box alert-danger"
            style={{ padding: '1rem', marginBottom: '1.25rem' }}
          >
            <AlertCircle size={22} />
            <div style={{ fontSize: '0.95rem' }}>{errorMessage}</div>
          </div>
        )}

        <form onSubmit={handleSubmit} style={{ display: 'flex', flexDirection: 'column', gap: '1.25rem' }}>
          {/* Patient Profile Selection */}
          <div
            style={{
              backgroundColor: 'var(--bg-subtle)',
              padding: '1rem 1.25rem',
              borderRadius: 'var(--radius-md)',
              border: '1px solid var(--border-light)',
            }}
          >
            <label
              style={{
                display: 'block',
                fontWeight: 700,
                fontSize: '1rem',
                color: 'var(--text-main)',
                marginBottom: '0.5rem',
              }}
            >
              Patient Name • रुग्णाचे नाव
            </label>

            {patientId ? (
              <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                  <User size={20} color="var(--primary)" />
                  <strong style={{ fontSize: '1.1rem' }}>{patientName}</strong>
                </div>
                {availablePatients.length > 1 && (
                  <select
                    className="input-field"
                    value={patientId}
                    onChange={handlePatientSelect}
                    style={{ width: 'auto', padding: '0.35rem 0.75rem', fontSize: '0.9rem' }}
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
                <p style={{ fontSize: '0.9rem', color: 'var(--text-muted)', marginBottom: '0.5rem' }}>
                  Enter your name to initiate consultation:
                </p>
                <div style={{ display: 'flex', gap: '0.5rem', flexWrap: 'wrap' }}>
                  <input
                    type="text"
                    className="input-field"
                    placeholder="Full Name (e.g. Ramesh Patil)"
                    value={patientName}
                    onChange={(e) => setPatientName(e.target.value)}
                    style={{ flex: 1, minWidth: '160px' }}
                  />
                  <input
                    type="tel"
                    className="input-field"
                    placeholder="Mobile (10 digits)"
                    value={patientPhone}
                    onChange={(e) => setPatientPhone(e.target.value)}
                    style={{ width: '150px' }}
                  />
                  <button
                    type="button"
                    className="btn-large btn-outline"
                    onClick={handleQuickRegisterPatient}
                    disabled={isRegisteringPatient}
                    style={{ padding: '0.5rem 1rem', fontSize: '0.9rem' }}
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
                fontSize: '1.05rem',
                color: 'var(--text-main)',
                marginBottom: '0.4rem',
              }}
            >
              Describe Your Health Problem • त्रास / लक्षणे सांगा *
            </label>
            <textarea
              id="consultation-disease"
              className="input-field"
              rows={3}
              placeholder="e.g. High fever for 2 days, chest congestion and severe headache..."
              value={disease}
              onChange={(e) => setDisease(e.target.value)}
              style={{
                width: '100%',
                padding: '0.85rem',
                fontSize: '1rem',
                fontFamily: 'inherit',
                resize: 'vertical',
              }}
            />
          </div>

          {/* Consultation Type Selection */}
          <div>
            <label
              style={{
                display: 'block',
                fontWeight: 700,
                fontSize: '1.05rem',
                color: 'var(--text-main)',
                marginBottom: '0.5rem',
              }}
            >
              How do you want to consult? • कसा सल्ला घ्यायचा आहे?
            </label>

            <div
              style={{
                display: 'grid',
                gridTemplateColumns: 'repeat(auto-fit, minmax(140px, 1fr))',
                gap: '0.75rem',
              }}
            >
              <button
                type="button"
                onClick={() => setConsultationType('VIDEO')}
                style={{
                  padding: '1rem 0.75rem',
                  borderRadius: 'var(--radius-md)',
                  border:
                    consultationType === 'VIDEO'
                      ? '3px solid var(--primary)'
                      : '2px solid var(--border-light)',
                  backgroundColor:
                    consultationType === 'VIDEO'
                      ? 'var(--primary-light)'
                      : 'var(--bg-surface)',
                  cursor: 'pointer',
                  display: 'flex',
                  flexDirection: 'column',
                  alignItems: 'center',
                  gap: '0.5rem',
                  fontWeight: 700,
                  color: consultationType === 'VIDEO' ? 'var(--primary)' : 'var(--text-main)',
                }}
              >
                <Video size={26} />
                <span>Video Call</span>
                <span style={{ fontSize: '0.8rem', fontWeight: 500, color: 'var(--text-subtle)' }}>
                  व्हिडिओ कॉल
                </span>
              </button>

              <button
                type="button"
                onClick={() => setConsultationType('AUDIO')}
                style={{
                  padding: '1rem 0.75rem',
                  borderRadius: 'var(--radius-md)',
                  border:
                    consultationType === 'AUDIO'
                      ? '3px solid var(--primary)'
                      : '2px solid var(--border-light)',
                  backgroundColor:
                    consultationType === 'AUDIO'
                      ? 'var(--primary-light)'
                      : 'var(--bg-surface)',
                  cursor: 'pointer',
                  display: 'flex',
                  flexDirection: 'column',
                  alignItems: 'center',
                  gap: '0.5rem',
                  fontWeight: 700,
                  color: consultationType === 'AUDIO' ? 'var(--primary)' : 'var(--text-main)',
                }}
              >
                <Phone size={26} />
                <span>Phone Call</span>
                <span style={{ fontSize: '0.8rem', fontWeight: 500, color: 'var(--text-subtle)' }}>
                  फोन कॉल
                </span>
              </button>

              <button
                type="button"
                onClick={() => setConsultationType('CHAT')}
                style={{
                  padding: '1rem 0.75rem',
                  borderRadius: 'var(--radius-md)',
                  border:
                    consultationType === 'CHAT'
                      ? '3px solid var(--primary)'
                      : '2px solid var(--border-light)',
                  backgroundColor:
                    consultationType === 'CHAT'
                      ? 'var(--primary-light)'
                      : 'var(--bg-surface)',
                  cursor: 'pointer',
                  display: 'flex',
                  flexDirection: 'column',
                  alignItems: 'center',
                  gap: '0.5rem',
                  fontWeight: 700,
                  color: consultationType === 'CHAT' ? 'var(--primary)' : 'var(--text-main)',
                }}
              >
                <MessageSquare size={26} />
                <span>Chat Message</span>
                <span style={{ fontSize: '0.8rem', fontWeight: 500, color: 'var(--text-subtle)' }}>
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
                fontSize: '1.05rem',
                color: 'var(--text-main)',
                marginBottom: '0.5rem',
              }}
            >
              Urgency Level • तातडीचे प्रमाण
            </label>

            <div
              style={{
                display: 'grid',
                gridTemplateColumns: 'repeat(auto-fit, minmax(120px, 1fr))',
                gap: '0.5rem',
              }}
            >
              {(
                [
                  { key: 'LOW', label: 'Routine • सामान्य', border: 'var(--risk-low-border)', bg: 'var(--risk-low-bg)' },
                  { key: 'MEDIUM', label: 'Moderate • मध्यम', border: 'var(--risk-moderate-border)', bg: 'var(--risk-moderate-bg)' },
                  { key: 'HIGH', label: 'Urgent • तातडीचे', border: 'var(--risk-high-border)', bg: 'var(--risk-high-bg)' },
                  { key: 'EMERGENCY', label: 'Emergency • आणीबाणी', border: 'var(--risk-critical-border)', bg: 'var(--risk-critical-bg)' },
                ] as const
              ).map((opt) => (
                <button
                  key={opt.key}
                  type="button"
                  onClick={() => setUrgency(opt.key)}
                  style={{
                    padding: '0.75rem 0.5rem',
                    borderRadius: 'var(--radius-md)',
                    border:
                      urgency === opt.key ? `3px solid var(--primary)` : `1.5px solid var(--border-light)`,
                    backgroundColor: urgency === opt.key ? opt.bg : 'var(--bg-surface)',
                    fontWeight: 700,
                    fontSize: '0.9rem',
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
          <div style={{ display: 'flex', gap: '1rem', marginTop: '1rem' }}>
            <button
              type="button"
              className="btn-large btn-outline"
              style={{ flex: 1 }}
              onClick={onClose}
              disabled={isSubmitting}
            >
              Cancel • रद्द करा
            </button>

            <button
              type="submit"
              className="btn-large btn-primary"
              style={{ flex: 2 }}
              disabled={isSubmitting}
            >
              {isSubmitting ? (
                <>
                  <div className="pulse-spinner" style={{ width: '22px', height: '22px', margin: 0 }} />
                  <span>Sending Request...</span>
                </>
              ) : (
                <>
                  <CheckCircle2 size={22} />
                  <span>Send Request • विनंती पाठवा</span>
                </>
              )}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};
