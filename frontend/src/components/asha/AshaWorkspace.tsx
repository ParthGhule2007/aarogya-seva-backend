import React, { useState, useEffect } from 'react';
import { useAuth } from '../../context/AuthContext';
import { usePatient } from '../../hooks/usePatient';
import { useLanguage } from '../../context/LanguageContext';
import { useSos } from '../../hooks/useSos';
import { ScreeningWizard } from '../screening/ScreeningWizard';
import {
  Users,
  UserPlus,
  Heart,
  Search,
  MapPin,
  Phone,
  AlertTriangle,
  CheckCircle,
  FileText,
  Calendar,
  ShieldAlert,
  PhoneCall,
} from 'lucide-react';
import { PatientDto, CreatePatientRequest } from '../../types/patient';
import { AarogyaMitraVoiceCallCard } from '../calling/AarogyaMitraVoiceCallCard';

export const AshaWorkspace: React.FC = () => {
  const {
    selectedPatientId,
    setSelectedPatientId,
    selectedPatientName,
    setSelectedPatientName,
  } = useAuth();
  const { patients, fetchAllPatients, registerPatient, isLoading } = usePatient();
  const { t } = useLanguage();
  const { setIsSosModalOpen } = useSos();

  const [activeTab, setActiveTab] = useState<'register' | 'assisted' | 'assistedSos' | 'voiceCall'>('register');
  const [searchFilter, setSearchFilter] = useState('');
  const [isRegisterModalOpen, setIsRegisterModalOpen] = useState(false);

  // New patient form
  const [fullName, setFullName] = useState('');
  const [gender, setGender] = useState<'MALE' | 'FEMALE' | 'OTHER'>('FEMALE');
  const [contactNumber, setContactNumber] = useState('');
  const [village, setVillage] = useState('Palasdeo');
  const [district, setDistrict] = useState('Pune');
  const [isSubmitting, setIsSubmitting] = useState(false);

  useEffect(() => {
    fetchAllPatients();
  }, [fetchAllPatients]);

  const handleSelectPatient = (p: PatientDto) => {
    setSelectedPatientId(p.id);
    setSelectedPatientName(p.fullName);
  };

  const handleRegisterSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!fullName || !contactNumber) return;
    setIsSubmitting(true);
    try {
      const req: CreatePatientRequest = {
        fullName,
        gender,
        dateOfBirth: '1985-05-15',
        contactNumber,
        address: {
          village,
          district,
          state: 'Maharashtra',
          pincode: '413132',
          gpsLocation: {
            latitude: 18.5204,
            longitude: 73.8567,
          },
        },
        emergencyContact: {
          contactName: 'Local Caregiver',
          contactNumber,
          relationship: 'Family',
        },
      };
      const created = await registerPatient(req);
      setSelectedPatientId(created.id);
      setSelectedPatientName(created.fullName);
      setIsRegisterModalOpen(false);
      setFullName('');
      setContactNumber('');
    } catch {
      // Error handled in hook
    } finally {
      setIsSubmitting(false);
    }
  };

  const filteredPatients = patients.filter(
    (p) =>
      p.fullName.toLowerCase().includes(searchFilter.toLowerCase()) ||
      p.contactNumber.includes(searchFilter) ||
      (p.address?.village || p.village || p.villageTown || '').toLowerCase().includes(searchFilter.toLowerCase())
  );

  return (
    <div style={{ maxWidth: '1100px', margin: '0 auto' }}>
      {/* Active Selected Patient Banner (Mandatory Boundary) */}
      <div
        style={{
          background: selectedPatientId ? 'var(--primary-light)' : '#fef3c7',
          border: `2px solid ${selectedPatientId ? 'var(--primary-border)' : '#fde68a'}`,
          borderRadius: 'var(--radius-lg)',
          padding: '1rem 1.25rem',
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center',
          marginBottom: '1.5rem',
          flexWrap: 'wrap',
          gap: '0.75rem',
        }}
      >
        <div style={{ display: 'flex', alignItems: 'center', gap: '0.6rem' }}>
          <Users size={22} color={selectedPatientId ? 'var(--primary)' : '#b45309'} />
          <div>
            <div style={{ fontWeight: 800, fontSize: '1.05rem', color: selectedPatientId ? 'var(--primary-hover)' : '#92400e' }}>
              {selectedPatientId ? `${t('ashaSelectedPatient')} ${selectedPatientName}` : t('ashaSelectPatientFirst')}
            </div>
            <div style={{ fontSize: '0.8rem', color: 'var(--text-subtle)' }}>
              {selectedPatientId
                ? `Patient ID: ${selectedPatientId} • Ready for assisted field screening & triage`
                : 'Select a patient from the register below before initiating assisted actions.'}
            </div>
          </div>
        </div>

        {selectedPatientId && (
          <button
            className="btn btn-outline"
            style={{ minHeight: '38px', fontSize: '0.8rem' }}
            onClick={() => {
              setSelectedPatientId(null);
              setSelectedPatientName(null);
            }}
          >
            Switch Patient
          </button>
        )}
      </div>

      {/* Tabs */}
      <div className="tabs-nav">
        <button
          className={`tab-btn ${activeTab === 'register' ? 'active' : ''}`}
          onClick={() => setActiveTab('register')}
        >
          <Users size={18} />
          <span>{t('ashaVillageTitle')}</span>
        </button>

        <button
          className={`tab-btn ${activeTab === 'assisted' ? 'active' : ''}`}
          onClick={() => {
            if (!selectedPatientId && patients.length > 0) {
              handleSelectPatient(patients[0]);
            }
            setActiveTab('assisted');
          }}
        >
          <Heart size={18} />
          <span>{t('ashaAssistedTriage')}</span>
        </button>

        <button
          className={`tab-btn ${activeTab === 'assistedSos' ? 'active' : ''}`}
          onClick={() => setActiveTab('assistedSos')}
        >
          <ShieldAlert size={18} />
          <span>{t('ashaAssistedSos')}</span>
        </button>

        <button
          className={`tab-btn ${activeTab === 'voiceCall' ? 'active' : ''}`}
          onClick={() => {
            if (!selectedPatientId && patients.length > 0) {
              handleSelectPatient(patients[0]);
            }
            setActiveTab('voiceCall');
          }}
        >
          <PhoneCall size={18} />
          <span>{t('navVoiceCall')}</span>
        </button>
      </div>

      {/* TAB 1: VILLAGE PATIENT REGISTER */}
      {activeTab === 'register' && (
        <div>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1.25rem', flexWrap: 'wrap', gap: '0.75rem' }}>
            {/* Search Filter */}
            <div style={{ position: 'relative', flex: 1, maxWidth: '400px' }}>
              <input
                type="text"
                className="form-control"
                style={{ paddingLeft: '2.5rem' }}
                placeholder="Filter by name, village, or mobile..."
                value={searchFilter}
                onChange={(e) => setSearchFilter(e.target.value)}
              />
              <Search
                size={18}
                style={{ position: 'absolute', left: '0.75rem', top: '50%', transform: 'translateY(-50%)', color: 'var(--text-subtle)' }}
              />
            </div>

            <button className="btn btn-primary" onClick={() => setIsRegisterModalOpen(true)}>
              <UserPlus size={18} />
              <span>{t('ashaRegisterPatient')}</span>
            </button>
          </div>

          {/* Patients Register Table / Cards */}
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(320px, 1fr))', gap: '1rem' }}>
            {filteredPatients.map((p) => {
              const isSelected = selectedPatientId === p.id;
              return (
                <div
                  key={p.id}
                  className="card"
                  style={{
                    marginBottom: 0,
                    border: `2px solid ${isSelected ? 'var(--primary)' : 'var(--border-light)'}`,
                    background: isSelected ? 'var(--primary-light)' : 'var(--bg-surface)',
                  }}
                >
                  <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: '0.5rem' }}>
                    <div>
                      <div style={{ fontWeight: 800, fontSize: '1.1rem' }}>{p.fullName}</div>
                      <div style={{ fontSize: '0.8rem', color: 'var(--text-subtle)' }}>
                        Gender: {p.gender} • DOB: {p.dateOfBirth || 'Adult'}
                      </div>
                    </div>
                    {isSelected ? (
                      <span className="badge badge-routine">Active ✓</span>
                    ) : (
                      <button
                        className="btn btn-outline"
                        style={{ minHeight: '34px', padding: '0 0.6rem', fontSize: '0.8rem' }}
                        onClick={() => handleSelectPatient(p)}
                      >
                        Select
                      </button>
                    )}
                  </div>

                  <div style={{ fontSize: '0.85rem', color: 'var(--text-muted)', marginBottom: '0.75rem' }}>
                    <div style={{ display: 'flex', alignItems: 'center', gap: '0.4rem', marginBottom: '0.25rem' }}>
                      <Phone size={14} color="var(--primary)" />
                      <span>{p.contactNumber}</span>
                    </div>
                    <div style={{ display: 'flex', alignItems: 'center', gap: '0.4rem' }}>
                      <MapPin size={14} color="var(--primary)" />
                      <span>Village: {p.address?.village || p.village || p.villageTown || 'Local'}, {p.address?.district || p.district || 'Pune'}</span>
                    </div>
                  </div>

                  <div style={{ display: 'flex', gap: '0.5rem', borderTop: '1px solid var(--border-light)', paddingTop: '0.75rem' }}>
                    <button
                      className="btn btn-outline"
                      style={{ flex: 1, minHeight: '38px', fontSize: '0.8rem' }}
                      onClick={() => {
                        handleSelectPatient(p);
                        setActiveTab('assisted');
                      }}
                    >
                      <Heart size={14} />
                      <span>Screening</span>
                    </button>
                    <button
                      className="btn btn-outline"
                      style={{ minHeight: '38px', fontSize: '0.8rem', color: '#dc2626', borderColor: '#fca5a5' }}
                      onClick={() => {
                        handleSelectPatient(p);
                        setIsSosModalOpen(true);
                      }}
                    >
                      <AlertTriangle size={14} />
                      <span>SOS</span>
                    </button>
                  </div>
                </div>
              );
            })}
          </div>
        </div>
      )}

      {/* TAB 2: ASSISTED SCREENING WIZARD */}
      {activeTab === 'assisted' && (
        <div>
          {!selectedPatientId ? (
            <div className="alert-box warning">
              <AlertTriangle size={20} />
              <span>Please select a patient from the register before running assisted triage.</span>
            </div>
          ) : (
            <ScreeningWizard onComplete={() => setActiveTab('register')} />
          )}
        </div>
      )}

      {/* TAB 3: ASSISTED SOS DISPATCH */}
      {activeTab === 'assistedSos' && (
        <div className="card" style={{ maxWidth: '600px', margin: '0 auto', textAlign: 'center', padding: '2rem' }}>
          <ShieldAlert size={48} color="#dc2626" style={{ margin: '0 auto 1rem' }} />
          <h3 style={{ fontSize: '1.3rem', fontWeight: 800 }}>Assisted Emergency SOS Trigger</h3>
          <p style={{ color: 'var(--text-subtle)', fontSize: '0.9rem', marginBottom: '1.5rem' }}>
            Dispatch community responders, PostGIS hospital alert, and family notifications on behalf of:{' '}
            <strong>{selectedPatientName || 'Active Villager'}</strong>
          </p>

          <button
            className="btn btn-danger"
            style={{ width: '100%', minHeight: '54px', fontSize: '1.1rem', fontWeight: 800 }}
            onClick={() => setIsSosModalOpen(true)}
          >
            <AlertTriangle size={22} />
            <span>Open Emergency SOS Dispatcher</span>
          </button>
        </div>
      )}

      {/* TAB 4: ASHA ASSISTED AI VOICE CALL */}
      {activeTab === 'voiceCall' && (
        <div style={{ maxWidth: '850px', margin: '0 auto' }}>
          <AarogyaMitraVoiceCallCard
            defaultPhone={patients.find((p) => p.id === selectedPatientId)?.contactNumber || ''}
            defaultName={selectedPatientName || ''}
          />
        </div>
      )}

      {/* Register Villager Modal */}
      {isRegisterModalOpen && (
        <div className="modal-overlay" onClick={() => setIsRegisterModalOpen(false)}>
          <div className="modal-dialog" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <h3 style={{ fontWeight: 700 }}>{t('ashaRegisterPatient')}</h3>
              <button className="icon-btn" onClick={() => setIsRegisterModalOpen(false)}>✕</button>
            </div>
            <form onSubmit={handleRegisterSubmit}>
              <div className="modal-body">
                <div className="form-group">
                  <label className="form-label">Full Name (पूरा नाम): *</label>
                  <input
                    type="text"
                    className="form-control"
                    placeholder="e.g. Shakuntala Devi"
                    value={fullName}
                    onChange={(e) => setFullName(e.target.value)}
                    required
                  />
                </div>

                <div className="form-group">
                  <label className="form-label">Gender (लिंग): *</label>
                  <select
                    className="form-control"
                    value={gender}
                    onChange={(e) => setGender(e.target.value as any)}
                  >
                    <option value="FEMALE">Female (महिला)</option>
                    <option value="MALE">Male (पुरुष)</option>
                    <option value="OTHER">Other</option>
                  </select>
                </div>

                <div className="form-group">
                  <label className="form-label">Mobile Contact Number: *</label>
                  <input
                    type="tel"
                    className="form-control"
                    placeholder="e.g. 9822012345"
                    value={contactNumber}
                    onChange={(e) => setContactNumber(e.target.value)}
                    required
                  />
                </div>

                <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '0.75rem' }}>
                  <div className="form-group">
                    <label className="form-label">Village (गाँव):</label>
                    <input
                      type="text"
                      className="form-control"
                      value={village}
                      onChange={(e) => setVillage(e.target.value)}
                    />
                  </div>

                  <div className="form-group">
                    <label className="form-label">District (ज़िला):</label>
                    <input
                      type="text"
                      className="form-control"
                      value={district}
                      onChange={(e) => setDistrict(e.target.value)}
                    />
                  </div>
                </div>
              </div>

              <div className="modal-footer">
                <button type="button" className="btn btn-outline" onClick={() => setIsRegisterModalOpen(false)}>
                  Cancel
                </button>
                <button type="submit" className="btn btn-primary" disabled={isSubmitting || !fullName || !contactNumber}>
                  {isSubmitting ? 'Registering...' : 'Register Villager'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};
