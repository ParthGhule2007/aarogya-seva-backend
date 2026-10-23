import React, { useState, useEffect } from 'react';
import { doctorApi } from '../../api/doctorApi';
import { AvailableDoctorResponseDTO, DoctorResponseDTO } from '../../types/doctor';
import { ConsultationResponseDTO } from '../../types/consultation';
import { RequestConsultationModal } from './RequestConsultationModal';
import {
  UserCheck,
  Stethoscope,
  Building2,
  Award,
  Clock,
  Search,
  RefreshCw,
  AlertCircle,
  CheckCircle2,
  X,
  FileBadge2,
  Calendar,
  Sparkles,
  ArrowRight,
  Video,
} from 'lucide-react';

interface AvailableDoctorsListProps {
  onDoctorSelected?: (doctor: AvailableDoctorResponseDTO) => void;
  onNavigateBack?: () => void;
  onNavigateToConsultations?: () => void;
}

export const AvailableDoctorsList: React.FC<AvailableDoctorsListProps> = ({
  onDoctorSelected,
  onNavigateBack,
  onNavigateToConsultations,
}) => {
  const [doctors, setDoctors] = useState<AvailableDoctorResponseDTO[]>([]);
  const [loading, setLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);
  const [searchQuery, setSearchQuery] = useState<string>('');
  const [selectedSpecialization, setSelectedSpecialization] = useState<string>('ALL');

  // Selected doctor ID (Long in Java, number in TS)
  const [selectedDoctorId, setSelectedDoctorId] = useState<number | null>(() => {
    const saved = localStorage.getItem('aarogya_selected_doctor_id');
    return saved ? parseInt(saved, 10) : null;
  });

  // Doctor Details Modal State
  const [detailModalOpen, setDetailModalOpen] = useState<boolean>(false);
  const [loadingDetail, setLoadingDetail] = useState<boolean>(false);
  const [detailError, setDetailError] = useState<string | null>(null);
  const [doctorDetail, setDoctorDetail] = useState<DoctorResponseDTO | null>(null);

  // Request Consultation Modal State (Phase C)
  const [requestingDoctor, setRequestingDoctor] = useState<AvailableDoctorResponseDTO | DoctorResponseDTO | null>(null);
  const [recentConsultation, setRecentConsultation] = useState<ConsultationResponseDTO | null>(null);

  const fetchAvailableDoctors = async () => {
    setLoading(true);
    setError(null);
    try {
      const data = await doctorApi.getAvailableDoctors();
      setDoctors(data);
    } catch (err: any) {
      console.error('Failed to load available doctors:', err);
      setError(
        err.message ||
          'Failed to connect to Doctor Service. Please verify service is running on port 8086.'
      );
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchAvailableDoctors();
  }, []);

  const handleSelectDoctor = (doctor: AvailableDoctorResponseDTO) => {
    setSelectedDoctorId(doctor.id);
    localStorage.setItem('aarogya_selected_doctor_id', doctor.id.toString());
    localStorage.setItem('aarogya_selected_doctor_name', doctor.name);
    localStorage.setItem('aarogya_selected_doctor_spec', doctor.specialization);
    if (onDoctorSelected) {
      onDoctorSelected(doctor);
    }
  };

  const handleViewDetails = async (doctorId: number) => {
    setDetailModalOpen(true);
    setLoadingDetail(true);
    setDetailError(null);
    setDoctorDetail(null);
    try {
      const detail = await doctorApi.getDoctorById(doctorId);
      setDoctorDetail(detail);
    } catch (err: any) {
      console.error(`Failed to load doctor profile ${doctorId}:`, err);
      setDetailError(err.message || 'Failed to load doctor profile details.');
    } finally {
      setLoadingDetail(false);
    }
  };

  const handleClearSelectedDoctor = () => {
    setSelectedDoctorId(null);
    localStorage.removeItem('aarogya_selected_doctor_id');
    localStorage.removeItem('aarogya_selected_doctor_name');
    localStorage.removeItem('aarogya_selected_doctor_spec');
  };

  const handleConsultationSuccess = (consultation: ConsultationResponseDTO) => {
    setRecentConsultation(consultation);
    setRequestingDoctor(null);
    if (onNavigateToConsultations) {
      onNavigateToConsultations();
    }
  };

  // Derive distinct specializations for filter dropdown
  const specializations = Array.from(
    new Set(doctors.map((d) => d.specialization).filter(Boolean))
  );

  // Filtered doctor list
  const filteredDoctors = doctors.filter((doctor) => {
    const matchesSearch =
      doctor.name.toLowerCase().includes(searchQuery.toLowerCase()) ||
      doctor.specialization.toLowerCase().includes(searchQuery.toLowerCase()) ||
      (doctor.hospitalName &&
        doctor.hospitalName.toLowerCase().includes(searchQuery.toLowerCase()));

    const matchesSpec =
      selectedSpecialization === 'ALL' ||
      doctor.specialization === selectedSpecialization;

    return matchesSearch && matchesSpec;
  });

  const selectedDoctorObj = doctors.find((d) => d.id === selectedDoctorId);

  return (
    <div style={{ maxWidth: '850px', margin: '0 auto' }}>
      {/* Top Banner & Header */}
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
        <div style={{ display: 'flex', alignItems: 'center', gap: '1rem', flexWrap: 'wrap' }}>
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
            <Stethoscope size={32} />
          </div>
          <div style={{ flex: 1 }}>
            <h1
              style={{
                fontSize: '1.75rem',
                fontWeight: 800,
                color: 'var(--primary)',
                marginBottom: '0.25rem',
              }}
            >
              Available Doctors • उपलब्ध डॉक्टर
            </h1>
            <p style={{ fontSize: '1.05rem', color: 'var(--text-muted)' }}>
              Verified medical practitioners online for consultation & advice.
              <br />
              <span style={{ fontSize: '0.95rem', color: 'var(--text-subtle)' }}>
                थेट ऑनलाइन सल्ला आणि उपचारासाठी उपलब्ध प्रमाणित डॉक्टर निवडा.
              </span>
            </p>
          </div>
          <div style={{ display: 'flex', gap: '0.5rem' }}>
            <button
              className="btn-large btn-outline"
              style={{ padding: '0.6rem 1.1rem', fontSize: '0.95rem' }}
              onClick={fetchAvailableDoctors}
              disabled={loading}
              aria-label="Refresh doctor list"
            >
              <RefreshCw size={18} className={loading ? 'pulse-spinner' : ''} />
              <span>Refresh • ताजे करा</span>
            </button>
            {onNavigateToConsultations && (
              <button
                className="btn-large btn-outline"
                style={{ padding: '0.6rem 1.1rem', fontSize: '0.95rem' }}
                onClick={onNavigateToConsultations}
              >
                <Video size={18} />
                <span>My Consultations • माझे सल्ले</span>
              </button>
            )}
          </div>
        </div>

        {/* Selected Doctor Active Banner */}
        {selectedDoctorId && (
          <div
            style={{
              marginTop: '1.25rem',
              padding: '1.1rem 1.25rem',
              backgroundColor: 'var(--secondary-light)',
              border: '2px solid var(--secondary)',
              borderRadius: 'var(--radius-md)',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'space-between',
              gap: '1rem',
              flexWrap: 'wrap',
            }}
          >
            <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
              <CheckCircle2 size={26} color="var(--secondary)" />
              <div>
                <strong style={{ fontSize: '1.1rem', color: 'var(--secondary-hover)' }}>
                  Selected Doctor • निवडलेले डॉक्टर:
                </strong>{' '}
                <span style={{ fontSize: '1.1rem', fontWeight: 700 }}>
                  {selectedDoctorObj
                    ? selectedDoctorObj.name
                    : localStorage.getItem('aarogya_selected_doctor_name') ||
                      `Doctor #${selectedDoctorId}`}
                </span>
                <div style={{ fontSize: '0.9rem', color: 'var(--text-muted)' }}>
                  Specialization:{' '}
                  {selectedDoctorObj?.specialization ||
                    localStorage.getItem('aarogya_selected_doctor_spec') ||
                    'Specialist'}
                </div>
              </div>
            </div>

            <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem', flexWrap: 'wrap' }}>
              {selectedDoctorObj && (
                <button
                  className="btn-large btn-primary"
                  style={{ padding: '0.6rem 1.2rem', fontSize: '0.95rem' }}
                  onClick={() => setRequestingDoctor(selectedDoctorObj)}
                >
                  <Video size={18} />
                  <span>Request Consultation • सल्ला विनंती</span>
                </button>
              )}

              <button
                onClick={handleClearSelectedDoctor}
                style={{
                  background: 'transparent',
                  border: '1px solid var(--border-medium)',
                  borderRadius: 'var(--radius-sm)',
                  padding: '0.45rem 0.75rem',
                  fontSize: '0.85rem',
                  cursor: 'pointer',
                  color: 'var(--text-muted)',
                }}
              >
                Change
              </button>
            </div>
          </div>
        )}
      </div>

      {/* Recent Consultation Alert */}
      {recentConsultation && (
        <div
          className="alert-box alert-success"
          style={{
            padding: '1.25rem',
            marginBottom: '1.5rem',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
            flexWrap: 'wrap',
            gap: '1rem',
          }}
        >
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
            <CheckCircle2 size={28} color="var(--secondary)" />
            <div>
              <strong style={{ fontSize: '1.1rem', display: 'block' }}>
                ✓ Consultation Request Sent • सल्लामसलत विनंती पाठवली गेली!
              </strong>
              <span style={{ fontSize: '0.95rem' }}>
                Your request has been submitted to Dr.{' '}
                {recentConsultation.doctorName || selectedDoctorObj?.name}.
              </span>
            </div>
          </div>
          {onNavigateToConsultations && (
            <button
              className="btn-large btn-secondary"
              style={{ padding: '0.6rem 1.2rem', fontSize: '0.95rem' }}
              onClick={onNavigateToConsultations}
            >
              <span>View My Consultations • माझे सल्ले पहा</span>
              <ArrowRight size={18} />
            </button>
          )}
        </div>
      )}

      {/* Search and Filter Controls */}
      <div
        style={{
          display: 'grid',
          gridTemplateColumns: 'repeat(auto-fit, minmax(240px, 1fr))',
          gap: '1rem',
          marginBottom: '1.5rem',
        }}
      >
        <div style={{ position: 'relative' }}>
          <Search
            size={20}
            color="var(--text-subtle)"
            style={{
              position: 'absolute',
              left: '12px',
              top: '50%',
              transform: 'translateY(-50%)',
            }}
          />
          <input
            type="text"
            className="input-field"
            placeholder="Search by doctor name or hospital..."
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            style={{ paddingLeft: '2.5rem', height: '52px', fontSize: '1.05rem' }}
          />
        </div>

        <div>
          <select
            className="input-field"
            value={selectedSpecialization}
            onChange={(e) => setSelectedSpecialization(e.target.value)}
            style={{ height: '52px', fontSize: '1.05rem' }}
          >
            <option value="ALL">All Specializations • सर्व शाखा</option>
            {specializations.map((spec) => (
              <option key={spec} value={spec}>
                {spec}
              </option>
            ))}
          </select>
        </div>
      </div>

      {/* State 1: Loading */}
      {loading && (
        <div className="loading-overlay">
          <div className="pulse-spinner" />
          <h2 style={{ fontSize: '1.35rem', color: 'var(--primary)', marginBottom: '0.5rem' }}>
            Connecting to Doctor Service...
          </h2>
          <p style={{ color: 'var(--text-muted)' }}>
            उपलब्ध डॉक्टरांची यादी आणत आहे • Fetching real-time doctor availability
          </p>
        </div>
      )}

      {/* State 2: Error with Retry */}
      {!loading && error && (
        <div
          className="alert-box alert-danger"
          style={{ flexDirection: 'column', gap: '0.75rem', padding: '1.5rem' }}
        >
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
            <AlertCircle size={28} />
            <strong style={{ fontSize: '1.15rem' }}>
              Error loading doctors • डॉक्टर माहिती मिळवण्यात त्रुटी
            </strong>
          </div>
          <p style={{ fontSize: '1rem', margin: '0.25rem 0' }}>{error}</p>
          <button
            className="btn-large btn-primary"
            style={{ alignSelf: 'flex-start', padding: '0.6rem 1.25rem', marginTop: '0.5rem' }}
            onClick={fetchAvailableDoctors}
          >
            <RefreshCw size={18} />
            <span>Try Again • पुन्हा प्रयत्न करा</span>
          </button>
        </div>
      )}

      {/* State 3: Empty State */}
      {!loading && !error && filteredDoctors.length === 0 && (
        <div
          className="card-simple"
          style={{
            textAlign: 'center',
            padding: '3rem 1.5rem',
            backgroundColor: 'var(--bg-surface)',
          }}
        >
          <UserCheck size={56} color="var(--border-medium)" style={{ margin: '0 auto 1rem auto' }} />
          <h2 style={{ fontSize: '1.4rem', color: 'var(--text-main)', marginBottom: '0.5rem' }}>
            No Doctors Currently Available
          </h2>
          <p style={{ fontSize: '1.05rem', color: 'var(--text-muted)', marginBottom: '1.5rem' }}>
            सध्या कोणताही डॉक्टर उपलब्ध नाही. कृपया थोड्या वेळाने पुन्हा तपासा किंवा नजीकच्या प्राथमिक आरोग्य केंद्राशी संपर्क साधा.
          </p>
          <button
            className="btn-large btn-outline"
            style={{ margin: '0 auto' }}
            onClick={fetchAvailableDoctors}
          >
            <RefreshCw size={20} />
            <span>Check Again • पुन्हा तपासा</span>
          </button>
        </div>
      )}

      {/* State 4: Doctor Cards Grid */}
      {!loading && !error && filteredDoctors.length > 0 && (
        <div style={{ display: 'flex', flexDirection: 'column', gap: '1.25rem' }}>
          {filteredDoctors.map((doctor) => {
            const isSelected = selectedDoctorId === doctor.id;

            return (
              <div
                key={doctor.id}
                className="card-simple"
                style={{
                  padding: '1.5rem',
                  backgroundColor: 'var(--bg-surface)',
                  border: isSelected
                    ? '3px solid var(--secondary)'
                    : '2px solid var(--border-light)',
                  borderRadius: 'var(--radius-lg)',
                  boxShadow: isSelected ? 'var(--shadow-md)' : 'var(--shadow-sm)',
                  transition: 'all 0.2s ease',
                  position: 'relative',
                }}
              >
                {/* Top Row: Doctor Name & Availability Badge */}
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
                        width: '54px',
                        height: '54px',
                        borderRadius: '50%',
                        backgroundColor: isSelected
                          ? 'var(--secondary-light)'
                          : 'var(--primary-light)',
                        display: 'flex',
                        alignItems: 'center',
                        justifyContent: 'center',
                        color: isSelected ? 'var(--secondary)' : 'var(--primary)',
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
                        {doctor.name}
                      </h2>
                      <div
                        style={{
                          display: 'inline-flex',
                          alignItems: 'center',
                          gap: '0.35rem',
                          backgroundColor: 'var(--primary-light)',
                          color: 'var(--primary)',
                          padding: '0.25rem 0.65rem',
                          borderRadius: 'var(--radius-full)',
                          fontSize: '0.9rem',
                          fontWeight: 700,
                        }}
                      >
                        <Stethoscope size={15} />
                        <span>{doctor.specialization}</span>
                      </div>
                    </div>
                  </div>

                  {/* Status Indicator Badge */}
                  <div
                    style={{
                      display: 'inline-flex',
                      alignItems: 'center',
                      gap: '0.5rem',
                      backgroundColor: 'var(--risk-low-bg)',
                      border: '1.5px solid var(--risk-low-border)',
                      color: 'var(--risk-low)',
                      padding: '0.35rem 0.85rem',
                      borderRadius: 'var(--radius-full)',
                      fontWeight: 700,
                      fontSize: '0.95rem',
                    }}
                  >
                    <span
                      style={{
                        width: '10px',
                        height: '10px',
                        borderRadius: '50%',
                        backgroundColor: 'var(--secondary)',
                        display: 'inline-block',
                      }}
                    />
                    <span>AVAILABLE • उपलब्ध</span>
                  </div>
                </div>

                {/* Details Meta Grid */}
                <div
                  style={{
                    display: 'grid',
                    gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))',
                    gap: '0.75rem',
                    backgroundColor: 'var(--bg-subtle)',
                    padding: '1rem',
                    borderRadius: 'var(--radius-md)',
                    marginBottom: '1.25rem',
                    fontSize: '0.95rem',
                  }}
                >
                  {doctor.hospitalName && (
                    <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                      <Building2 size={18} color="var(--primary)" />
                      <span>
                        <strong>Hospital:</strong> {doctor.hospitalName}
                      </span>
                    </div>
                  )}

                  {doctor.qualification && (
                    <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                      <Award size={18} color="var(--primary)" />
                      <span>
                        <strong>Degree:</strong> {doctor.qualification}
                      </span>
                    </div>
                  )}

                  {doctor.experienceYears !== undefined && (
                    <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                      <Clock size={18} color="var(--primary)" />
                      <span>
                        <strong>Experience:</strong> {doctor.experienceYears} Years
                      </span>
                    </div>
                  )}
                </div>

                {/* Action Buttons Row */}
                <div
                  style={{
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'space-between',
                    gap: '1rem',
                    flexWrap: 'wrap',
                  }}
                >
                  <button
                    className="btn-large btn-outline"
                    style={{
                      flex: 1,
                      minWidth: '160px',
                      padding: '0.75rem 1rem',
                      fontSize: '0.95rem',
                    }}
                    onClick={() => handleViewDetails(doctor.id)}
                    aria-label={`View full profile of ${doctor.name}`}
                  >
                    <span>Full Profile • माहिती</span>
                  </button>

                  <button
                    className="btn-large btn-primary"
                    style={{
                      flex: 1.5,
                      minWidth: '220px',
                      padding: '0.75rem 1.25rem',
                      fontSize: '1rem',
                    }}
                    onClick={() => {
                      handleSelectDoctor(doctor);
                      setRequestingDoctor(doctor);
                    }}
                    aria-label={`Request consultation with ${doctor.name}`}
                  >
                    <Video size={20} />
                    <span>Request Consultation • सल्ला घ्या</span>
                  </button>
                </div>
              </div>
            );
          })}
        </div>
      )}

      {/* Doctor Details Modal (GET /api/doctors/{doctorId}) */}
      {detailModalOpen && (
        <div
          role="dialog"
          aria-modal="true"
          aria-labelledby="doctor-modal-title"
          style={{
            position: 'fixed',
            top: 0,
            left: 0,
            right: 0,
            bottom: 0,
            backgroundColor: 'rgba(15, 23, 42, 0.65)',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            zIndex: 100,
            padding: '1rem',
          }}
        >
          <div
            style={{
              backgroundColor: 'var(--bg-surface)',
              borderRadius: 'var(--radius-lg)',
              maxWidth: '580px',
              width: '100%',
              padding: '2rem',
              boxShadow: 'var(--shadow-lg)',
              maxHeight: '90vh',
              overflowY: 'auto',
              position: 'relative',
              border: '2px solid var(--border-light)',
            }}
          >
            {/* Close Button */}
            <button
              onClick={() => setDetailModalOpen(false)}
              aria-label="Close Doctor Details"
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

            {loadingDetail && (
              <div style={{ textAlign: 'center', padding: '2.5rem 0' }}>
                <div className="pulse-spinner" />
                <p style={{ color: 'var(--text-muted)' }}>Loading doctor profile details...</p>
              </div>
            )}

            {detailError && (
              <div className="alert-box alert-danger">
                <AlertCircle size={22} />
                <span>{detailError}</span>
              </div>
            )}

            {!loadingDetail && doctorDetail && (
              <div>
                <div style={{ display: 'flex', alignItems: 'center', gap: '1rem', marginBottom: '1.25rem' }}>
                  <div
                    style={{
                      width: '64px',
                      height: '64px',
                      borderRadius: '50%',
                      backgroundColor: 'var(--primary-light)',
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'center',
                      color: 'var(--primary)',
                      fontWeight: 800,
                      fontSize: '1.5rem',
                    }}
                  >
                    Dr
                  </div>
                  <div>
                    <h2
                      id="doctor-modal-title"
                      style={{ fontSize: '1.6rem', fontWeight: 800, color: 'var(--primary)' }}
                    >
                      {doctorDetail.name}
                    </h2>
                    <span
                      style={{
                        fontSize: '1rem',
                        fontWeight: 600,
                        color: 'var(--secondary)',
                      }}
                    >
                      {doctorDetail.specialization}
                    </span>
                  </div>
                </div>

                <div
                  style={{
                    display: 'flex',
                    flexDirection: 'column',
                    gap: '0.85rem',
                    backgroundColor: 'var(--bg-subtle)',
                    padding: '1.25rem',
                    borderRadius: 'var(--radius-md)',
                    marginBottom: '1.5rem',
                    fontSize: '1rem',
                  }}
                >
                  <div>
                    <strong>Medical License:</strong>{' '}
                    <code
                      style={{
                        background: 'var(--bg-surface)',
                        padding: '0.2rem 0.5rem',
                        borderRadius: '4px',
                        border: '1px solid var(--border-light)',
                      }}
                    >
                      {doctorDetail.licenseNumber}
                    </code>
                  </div>

                  <div>
                    <strong>Qualification:</strong> {doctorDetail.qualification || 'MBBS'}
                  </div>

                  <div>
                    <strong>Hospital / Facility:</strong>{' '}
                    {doctorDetail.hospitalName || 'Affiliated Healthcare Center'}
                  </div>

                  <div>
                    <strong>Experience:</strong>{' '}
                    {doctorDetail.experienceYears ? `${doctorDetail.experienceYears} Years` : 'Experienced'}
                  </div>

                  <div>
                    <strong>Availability:</strong>{' '}
                    <span
                      style={{
                        color:
                          doctorDetail.availabilityStatus === 'AVAILABLE'
                            ? 'var(--risk-low)'
                            : 'var(--risk-moderate)',
                        fontWeight: 700,
                      }}
                    >
                      AVAILABLE • उपलब्ध
                    </span>
                  </div>
                </div>

                <div style={{ display: 'flex', gap: '1rem' }}>
                  <button
                    className="btn-large btn-outline"
                    style={{ flex: 1 }}
                    onClick={() => setDetailModalOpen(false)}
                  >
                    Close • बंद करा
                  </button>

                  <button
                    className="btn-large btn-primary"
                    style={{ flex: 2 }}
                    onClick={() => {
                      handleSelectDoctor(doctorDetail);
                      setDetailModalOpen(false);
                      setRequestingDoctor(doctorDetail);
                    }}
                  >
                    <Video size={22} />
                    <span>Request Consultation • सल्ला घ्या</span>
                  </button>
                </div>
              </div>
            )}
          </div>
        </div>
      )}

      {/* Phase C: Request Consultation Modal */}
      {requestingDoctor && (
        <RequestConsultationModal
          doctor={requestingDoctor}
          onClose={() => setRequestingDoctor(null)}
          onSuccess={handleConsultationSuccess}
        />
      )}
    </div>
  );
};
