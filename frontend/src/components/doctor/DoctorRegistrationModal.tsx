import React, { useState } from 'react';
import { useAuth } from '../../context/AuthContext';
import { useDoctor } from '../../hooks/useDoctor';
import { useLanguage } from '../../context/LanguageContext';
import { X, Stethoscope, Award, Building2, Calendar, FileText, CheckCircle, AlertCircle } from 'lucide-react';
import { DoctorRequestDTO } from '../../types/doctor';

interface DoctorRegistrationModalProps {
  isOpen: boolean;
  onClose: () => void;
  onSuccess?: () => void;
}

export const DoctorRegistrationModal: React.FC<DoctorRegistrationModalProps> = ({
  isOpen,
  onClose,
  onSuccess,
}) => {
  const { user } = useAuth();
  const { registerDoctorProfile, refreshCurrentDoctor } = useDoctor();
  const { language } = useLanguage();

  const [name, setName] = useState(user?.displayName || '');
  const [specialization, setSpecialization] = useState('General Medicine');
  const [licenseNumber, setLicenseNumber] = useState('');
  const [experienceYears, setExperienceYears] = useState<number>(3);
  const [qualification, setQualification] = useState('MBBS');
  const [hospitalName, setHospitalName] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [errorMsg, setErrorMsg] = useState<string | null>(null);

  if (!isOpen) return null;

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!name.trim()) {
      setErrorMsg('Doctor full name is required');
      return;
    }
    if (!licenseNumber.trim()) {
      setErrorMsg('Medical Council License Number is required');
      return;
    }
    if (!qualification.trim()) {
      setErrorMsg('Medical qualification is required');
      return;
    }
    if (!hospitalName.trim()) {
      setErrorMsg('Hospital or Clinic affiliation is required');
      return;
    }

    setIsSubmitting(true);
    setErrorMsg(null);

    try {
      const payload: Omit<DoctorRequestDTO, 'userId'> = {
        name: name.trim(),
        specialization: specialization.trim(),
        licenseNumber: licenseNumber.trim(),
        experienceYears: Number(experienceYears) || 0,
        qualification: qualification.trim(),
        hospitalName: hospitalName.trim(),
      };
      await registerDoctorProfile(payload);
      await refreshCurrentDoctor();
      if (onSuccess) onSuccess();
      onClose();
    } catch (err: any) {
      setErrorMsg(err.message || 'Failed to register doctor profile. Please verify your details.');
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div
      role="dialog"
      aria-modal="true"
      style={{
        position: 'fixed',
        inset: 0,
        zIndex: 9999,
        backgroundColor: 'rgba(0, 0, 0, 0.65)',
        backdropFilter: 'blur(4px)',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        padding: '1rem',
      }}
    >
      <div
        className="card"
        style={{
          width: '100%',
          maxWidth: '560px',
          maxHeight: '90vh',
          overflowY: 'auto',
          backgroundColor: '#ffffff',
          borderRadius: '16px',
          boxShadow: '0 25px 50px -12px rgba(0, 0, 0, 0.25)',
          padding: '1.75rem',
          margin: 0,
          position: 'relative',
        }}
      >
        {/* Header */}
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: '1.25rem' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
            <div
              style={{
                width: '46px',
                height: '46px',
                borderRadius: '12px',
                background: 'linear-gradient(135deg, #0d9488 0%, #0f766e 100%)',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                color: '#ffffff',
              }}
            >
              <Stethoscope size={24} />
            </div>
            <div>
              <h2 style={{ fontSize: '1.25rem', fontWeight: 800, color: 'var(--text-main, #0f172a)', margin: 0 }}>
                {language === 'hi' ? 'चिकित्सक प्रोफ़ाइल पंजीकरण' : language === 'mr' ? 'डॉक्टर प्रोफाइल नोंदणी' : 'Doctor Clinical Profile Registration'}
              </h2>
              <p style={{ fontSize: '0.825rem', color: 'var(--text-subtle, #64748b)', margin: '0.2rem 0 0' }}>
                Complete your medical credentials to receive teleconsultations
              </p>
            </div>
          </div>
          <button
            onClick={onClose}
            aria-label="Close"
            style={{
              background: 'transparent',
              border: 'none',
              cursor: 'pointer',
              color: 'var(--text-subtle, #64748b)',
              padding: '0.25rem',
              borderRadius: '8px',
            }}
          >
            <X size={20} />
          </button>
        </div>

        {errorMsg && (
          <div
            style={{
              display: 'flex',
              alignItems: 'center',
              gap: '0.5rem',
              backgroundColor: '#fef2f2',
              color: '#991b1b',
              border: '1px solid #fecaca',
              padding: '0.75rem 1rem',
              borderRadius: '8px',
              fontSize: '0.85rem',
              marginBottom: '1rem',
            }}
          >
            <AlertCircle size={18} style={{ flexShrink: 0 }} />
            <span>{errorMsg}</span>
          </div>
        )}

        <form onSubmit={handleSubmit} style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
          {/* Full Name */}
          <div>
            <label style={{ display: 'block', fontSize: '0.85rem', fontWeight: 700, marginBottom: '0.35rem', color: '#1e293b' }}>
              Full Name (with Dr. prefix) *
            </label>
            <input
              type="text"
              required
              value={name}
              onChange={(e) => setName(e.target.value)}
              placeholder="e.g. Dr. Rajesh Kulkarni"
              className="input-field"
              style={{
                width: '100%',
                padding: '0.65rem 0.85rem',
                borderRadius: '8px',
                border: '1px solid #cbd5e1',
                fontSize: '0.9rem',
              }}
            />
          </div>

          {/* Specialization & Experience */}
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '0.75rem' }}>
            <div>
              <label style={{ display: 'block', fontSize: '0.85rem', fontWeight: 700, marginBottom: '0.35rem', color: '#1e293b' }}>
                Specialization *
              </label>
              <select
                value={specialization}
                onChange={(e) => setSpecialization(e.target.value)}
                style={{
                  width: '100%',
                  padding: '0.65rem 0.85rem',
                  borderRadius: '8px',
                  border: '1px solid #cbd5e1',
                  fontSize: '0.9rem',
                  backgroundColor: '#ffffff',
                }}
              >
                <option value="General Medicine">General Medicine</option>
                <option value="Pediatrics">Pediatrics (बालरोग)</option>
                <option value="Cardiology">Cardiology (हृदयरोग)</option>
                <option value="Obstetrics & Gynecology">Gynecology (स्त्रीरोग)</option>
                <option value="Orthopedics">Orthopedics (अस्थिरोग)</option>
                <option value="Dermatology">Dermatology (त्वचारोग)</option>
                <option value="Emergency Medicine">Emergency Medicine</option>
              </select>
            </div>

            <div>
              <label style={{ display: 'block', fontSize: '0.85rem', fontWeight: 700, marginBottom: '0.35rem', color: '#1e293b' }}>
                Experience (Years) *
              </label>
              <input
                type="number"
                min="0"
                max="60"
                required
                value={experienceYears}
                onChange={(e) => setExperienceYears(parseInt(e.target.value, 10) || 0)}
                style={{
                  width: '100%',
                  padding: '0.65rem 0.85rem',
                  borderRadius: '8px',
                  border: '1px solid #cbd5e1',
                  fontSize: '0.9rem',
                }}
              />
            </div>
          </div>

          {/* License Number & Qualification */}
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '0.75rem' }}>
            <div>
              <label style={{ display: 'block', fontSize: '0.85rem', fontWeight: 700, marginBottom: '0.35rem', color: '#1e293b' }}>
                Medical License No. *
              </label>
              <input
                type="text"
                required
                value={licenseNumber}
                onChange={(e) => setLicenseNumber(e.target.value)}
                placeholder="e.g. MMC-2015-84920"
                style={{
                  width: '100%',
                  padding: '0.65rem 0.85rem',
                  borderRadius: '8px',
                  border: '1px solid #cbd5e1',
                  fontSize: '0.9rem',
                }}
              />
            </div>

            <div>
              <label style={{ display: 'block', fontSize: '0.85rem', fontWeight: 700, marginBottom: '0.35rem', color: '#1e293b' }}>
                Qualifications *
              </label>
              <input
                type="text"
                required
                value={qualification}
                onChange={(e) => setQualification(e.target.value)}
                placeholder="e.g. MBBS, MD, DNB"
                style={{
                  width: '100%',
                  padding: '0.65rem 0.85rem',
                  borderRadius: '8px',
                  border: '1px solid #cbd5e1',
                  fontSize: '0.9rem',
                }}
              />
            </div>
          </div>

          {/* Hospital / Clinic Affiliation */}
          <div>
            <label style={{ display: 'block', fontSize: '0.85rem', fontWeight: 700, marginBottom: '0.35rem', color: '#1e293b' }}>
              Hospital or Clinic Affiliation *
            </label>
            <input
              type="text"
              required
              value={hospitalName}
              onChange={(e) => setHospitalName(e.target.value)}
              placeholder="e.g. District Civil Hospital, Nashik"
              style={{
                width: '100%',
                padding: '0.65rem 0.85rem',
                borderRadius: '8px',
                border: '1px solid #cbd5e1',
                fontSize: '0.9rem',
              }}
            />
          </div>

          {/* Submit buttons */}
          <div style={{ display: 'flex', gap: '0.75rem', marginTop: '1rem' }}>
            <button
              type="button"
              onClick={onClose}
              disabled={isSubmitting}
              className="btn btn-outline"
              style={{ flex: 1, minHeight: '42px', borderRadius: '8px' }}
            >
              Cancel
            </button>
            <button
              type="submit"
              disabled={isSubmitting}
              className="btn btn-primary"
              style={{
                flex: 2,
                minHeight: '42px',
                borderRadius: '8px',
                background: 'linear-gradient(135deg, #0d9488 0%, #0f766e 100%)',
                color: '#ffffff',
                fontWeight: 700,
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                gap: '0.5rem',
              }}
            >
              {isSubmitting ? (
                <span>Registering Profile...</span>
              ) : (
                <>
                  <CheckCircle size={18} />
                  <span>Register Clinical Profile</span>
                </>
              )}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};
