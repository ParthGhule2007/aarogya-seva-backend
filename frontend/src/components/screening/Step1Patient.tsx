import React, { useState } from 'react';
import { useAuth } from '../../context/AuthContext';
import { usePatient } from '../../hooks/usePatient';
import { useLanguage } from '../../context/LanguageContext';
import { User, MapPin, Phone, Search, CheckCircle, AlertCircle, Loader2 } from 'lucide-react';
import { PatientDto } from '../../types/patient';
import { RegisterPatientModal } from '../patient/RegisterPatientModal';

interface Step1PatientProps {
  onNext: () => void;
}

export const Step1Patient: React.FC<Step1PatientProps> = ({ onNext }) => {
  const { user, role, selectedPatientId, setSelectedPatientId, setSelectedPatientName } = useAuth();
  const { patient, patients, searchPatients, isLoading } = usePatient();
  const { t } = useLanguage();

  const [searchQuery, setSearchQuery] = useState('');
  const [searchResults, setSearchResults] = useState<PatientDto[]>([]);
  const [_hasSearched, setHasSearched] = useState(false);
  const [isRegisterModalOpen, setIsRegisterModalOpen] = useState(false);

  const handleSearch = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!searchQuery.trim()) return;
    const res = await searchPatients(searchQuery);
    setSearchResults(res);
    setHasSearched(true);
  };

  const handleSelectPatient = (p: PatientDto) => {
    setSelectedPatientId(p.id);
    setSelectedPatientName(p.fullName);
  };

  const handlePatientRegistered = (newPatient: PatientDto) => {
    setSelectedPatientId(newPatient.id);
    setSelectedPatientName(newPatient.fullName);
    setIsRegisterModalOpen(false);
  };

  return (
    <div className="card">
      <div className="card-header">
        <div className="card-title">
          <User size={22} color="var(--primary)" />
          <span>{t('step1Title')}</span>
        </div>
        <span className="badge badge-routine">Step 1 of 5</span>
      </div>

      <p style={{ color: 'var(--text-subtle)', marginBottom: '1.25rem' }}>
        {t('step1Subtitle')}
      </p>

      {/* If ASHA Worker or Admin, show search & selector */}
      {role !== 'PATIENT' && (
        <div style={{ marginBottom: '1.5rem', padding: '1rem', background: 'var(--bg-subtle)', borderRadius: 'var(--radius-md)' }}>
          <label className="form-label">Search Patient Roster by Name or Mobile Number:</label>
          <form onSubmit={handleSearch} style={{ display: 'flex', gap: '0.5rem', marginBottom: '1rem' }}>
            <input
              type="text"
              className="form-control"
              placeholder="e.g. Ramesh or 9876543210"
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
            />
            <button type="submit" className="btn btn-primary" disabled={isLoading}>
              <Search size={18} />
              <span>Search</span>
            </button>
          </form>

          {/* Quick List of registered villagers */}
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(240px, 1fr))', gap: '0.5rem' }}>
            {(searchResults.length > 0 ? searchResults : patients.slice(0, 4)).map((p) => (
              <div
                key={p.id}
                onClick={() => handleSelectPatient(p)}
                style={{
                  padding: '0.75rem',
                  borderRadius: 'var(--radius-sm)',
                  border: `2px solid ${selectedPatientId === p.id ? 'var(--primary)' : 'var(--border-light)'}`,
                  background: selectedPatientId === p.id ? 'var(--primary-light)' : 'var(--bg-surface)',
                  cursor: 'pointer',
                  display: 'flex',
                  justifyContent: 'space-between',
                  alignItems: 'center',
                }}
              >
                <div>
                  <div style={{ fontWeight: 700, fontSize: '0.95rem' }}>{p.fullName}</div>
                  <div style={{ fontSize: '0.8rem', color: 'var(--text-subtle)' }}>
                    Village: {p.address?.village || p.village || p.villageTown || 'Local'} • Ph: {p.contactNumber}
                  </div>
                </div>
                {selectedPatientId === p.id && <CheckCircle size={18} color="var(--primary)" />}
              </div>
            ))}
          </div>
        </div>
      )}

      {/* Loading indicator */}
      {isLoading && (
        <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', padding: '1.25rem', color: 'var(--primary)', fontWeight: 600 }}>
          <Loader2 size={20} className="spin" />
          <span>Loading patient clinical profile...</span>
        </div>
      )}

      {/* Selected Patient Overview */}
      {!isLoading && selectedPatientId && patient ? (
        <div
          style={{
            border: '2px solid var(--primary-border)',
            background: 'var(--primary-light)',
            borderRadius: 'var(--radius-md)',
            padding: '1.25rem',
            marginBottom: '1.5rem',
          }}
        >
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '0.5rem' }}>
            <div style={{ fontSize: '1.1rem', fontWeight: 800, color: 'var(--primary-hover)' }}>
              {patient.fullName}
            </div>
            <span className="badge badge-routine">{patient.gender || 'Verified'}</span>
          </div>

          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(180px, 1fr))', gap: '0.75rem', fontSize: '0.9rem' }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: '0.4rem' }}>
              <Phone size={16} color="var(--primary)" />
              <span>Contact: {patient.contactNumber}</span>
            </div>
            <div style={{ display: 'flex', alignItems: 'center', gap: '0.4rem' }}>
              <MapPin size={16} color="var(--primary)" />
              <span>Village: {patient.address?.village || patient.village || patient.villageTown || 'Local'}, {patient.address?.district || patient.district || 'Pune'}</span>
            </div>
            <div>DOB / Age: {patient.dateOfBirth || patient.age || 'Adult'}</div>
            <div>Emergency: {patient.emergencyContact?.contactNumber || patient.emergencyContactNumber || '108'}</div>
          </div>
        </div>
      ) : !isLoading && role === 'PATIENT' ? (
        <div
          style={{
            border: '2px solid var(--risk-high, #ef4444)',
            background: 'rgba(239, 68, 68, 0.05)',
            borderRadius: 'var(--radius-md)',
            padding: '1.25rem',
            marginBottom: '1.5rem',
          }}
        >
          <div style={{ display: 'flex', alignItems: 'flex-start', gap: '0.75rem' }}>
            <AlertCircle size={24} color="var(--risk-high, #ef4444)" style={{ flexShrink: 0, marginTop: '2px' }} />
            <div style={{ flex: 1 }}>
              <h4 style={{ margin: '0 0 0.35rem 0', fontWeight: 800, color: 'var(--risk-high, #ef4444)' }}>
                Patient Medical Profile Missing (रुग्ण वैद्यकीय प्रोफाइल आढळले नाही)
              </h4>
              <p style={{ margin: '0 0 0.85rem 0', fontSize: '0.9rem', color: 'var(--text-main)', lineHeight: 1.5 }}>
                {user?.displayName
                  ? `You are logged in as "${user.displayName}", but no clinical patient profile is registered for your account.`
                  : 'No clinical patient profile is registered for your authenticated account.'}
                {' '}Please complete your patient details to start clinical screening.
              </p>
              <button
                type="button"
                className="btn btn-primary"
                onClick={() => setIsRegisterModalOpen(true)}
                style={{ display: 'inline-flex', alignItems: 'center', gap: '0.4rem' }}
              >
                <User size={16} />
                <span>Complete Patient Profile (रुग्ण नोंदणी पूर्ण करा)</span>
              </button>
            </div>
          </div>
        </div>
      ) : !isLoading && (
        <div className="alert-box warning">
          <AlertCircle size={20} />
          <span>No patient profile currently active. Please select a patient from the roster above to proceed.</span>
        </div>
      )}

      <div style={{ display: 'flex', justifyContent: 'flex-end', marginTop: '1.5rem' }}>
        <button
          className="btn btn-primary"
          style={{ minWidth: '160px' }}
          disabled={!selectedPatientId || !patient}
          onClick={onNext}
        >
          <span>{t('next')}</span>
        </button>
      </div>

      {/* Patient Profile Registration Modal */}
      <RegisterPatientModal
        isOpen={isRegisterModalOpen}
        onClose={() => setIsRegisterModalOpen(false)}
        onSuccess={handlePatientRegistered}
        initialFullName={user?.displayName || user?.fullName || ''}
        initialContactNumber={user?.contactNumber || user?.phoneNumber || ''}
      />
    </div>
  );
};
