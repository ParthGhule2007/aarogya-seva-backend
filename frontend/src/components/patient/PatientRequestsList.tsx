import React, { useState, useEffect } from 'react';
import {
  ClipboardList,
  Building2,
  Calendar,
  CheckCircle2,
  Clock,
  AlertCircle,
  XCircle,
  ArrowRight,
  RefreshCw,
  Eye,
  User
} from 'lucide-react';
import { ReferralResponseDto, ReferralDetailDto } from '../../types/referral';
import { PatientDto } from '../../types/patient';
import { referralApi } from '../../api/referralApi';
import { patientApi } from '../../api/patientApi';
import { useQuery } from '../../api/queryClient';
import { AlertBox } from '../common/AlertBox';

interface PatientRequestsListProps {
  patientId?: string;
  onReReferCase: (caseId: string) => void;
  onStartNewScreening: () => void;
}

export const PatientRequestsList: React.FC<PatientRequestsListProps> = ({
  patientId,
  onReReferCase,
  onStartNewScreening,
}) => {
  // Stored patient in localStorage or state
  const [activePatientId, setActivePatientId] = useState<string>(
    patientId || localStorage.getItem('aarogya_patient_id') || ''
  );
  const [selectedReferralDetail, setSelectedReferralDetail] = useState<ReferralDetailDto | null>(null);
  const [isLoadingDetail, setIsLoadingDetail] = useState(false);

  // Fetch all patients for selection if none active
  const { data: allPatients } = useQuery<PatientDto[]>(
    async () => {
      const res = await patientApi.getAllPatients();
      if (res && res.length > 0 && !activePatientId) {
        // Automatically default to most recent patient
        const latest = res[res.length - 1];
        setActivePatientId(latest.id);
        localStorage.setItem('aarogya_patient_id', latest.id);
        localStorage.setItem('aarogya_patient_name', latest.fullName);
      }
      return res;
    }
  );

  useEffect(() => {
    if (patientId) {
      setActivePatientId(patientId);
      localStorage.setItem('aarogya_patient_id', patientId);
    }
  }, [patientId]);

  // Fetch referrals for active patient
  const {
    data: referrals,
    isLoading,
    isError,
    error,
    refetch,
  } = useQuery<ReferralResponseDto[]>(
    async () => {
      if (!activePatientId) return [];
      const res = await referralApi.getReferralsByPatientId(activePatientId);
      return res;
    },
    [activePatientId],
    !!activePatientId
  );

  const handlePatientSelectChange = (newId: string) => {
    setActivePatientId(newId);
    localStorage.setItem('aarogya_patient_id', newId);
    const p = allPatients?.find((item) => item.id === newId);
    if (p) {
      localStorage.setItem('aarogya_patient_name', p.fullName);
    }
  };

  const [detailError, setDetailError] = useState<string | null>(null);

  const handleViewDetail = async (referralId: string) => {
    setIsLoadingDetail(true);
    setDetailError(null);
    try {
      const res = await referralApi.getReferralById(referralId);
      setSelectedReferralDetail(res);
    } catch (err: any) {
      setDetailError(err.message || 'Failed to load details from server.');
    } finally {
      setIsLoadingDetail(false);
    }
  };

  const getStatusBadge = (status: string) => {
    switch (status) {
      case 'ACCEPTED':
        return (
          <span className="status-pill accepted">
            <CheckCircle2 size={16} /> Accepted • स्वीकारले
          </span>
        );
      case 'IN_PROGRESS':
        return (
          <span className="status-pill in-progress">
            <Clock size={16} /> In Progress • उपचार सुरू
          </span>
        );
      case 'COMPLETED':
        return (
          <span className="status-pill completed">
            <CheckCircle2 size={16} /> Completed • पूर्ण झाले
          </span>
        );
      case 'REJECTED':
        return (
          <span className="status-pill rejected">
            <XCircle size={16} /> Request Rejected • नाकारले
          </span>
        );
      default:
        return (
          <span className="status-pill pending">
            <Clock size={16} /> Waiting for Facility • वाट पाहत आहे
          </span>
        );
    }
  };

  const activePatientName =
    allPatients?.find((p) => p.id === activePatientId)?.fullName ||
    localStorage.getItem('aarogya_patient_name') ||
    '';

  return (
    <div style={{ maxWidth: '680px', margin: '0 auto' }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1.25rem', flexWrap: 'wrap', gap: '0.75rem' }}>
        <div>
          <h2 style={{ fontSize: '1.8rem', fontWeight: 800, color: 'var(--primary)' }}>
            My Requests • माझ्या विनंत्या
          </h2>
          <p style={{ color: 'var(--text-muted)', fontSize: '1rem' }}>
            Track the status of your care requests sent to nearby healthcare centres.
          </p>
        </div>

        {activePatientId && (
          <button
            className="btn-large btn-outline"
            style={{ width: 'auto', minHeight: '44px', padding: '0.4rem 0.8rem', fontSize: '0.9rem' }}
            onClick={() => refetch()}
            aria-label="Refresh requests"
          >
            <RefreshCw size={16} /> Refresh
          </button>
        )}
      </div>

      {/* Patient Switcher Dropdown */}
      {allPatients && allPatients.length > 0 && (
        <div
          style={{
            backgroundColor: 'var(--bg-surface)',
            border: '1px solid var(--border-light)',
            borderRadius: 'var(--radius-md)',
            padding: '0.75rem 1rem',
            marginBottom: '1.5rem',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
            flexWrap: 'wrap',
            gap: '0.5rem',
          }}
        >
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
            <User size={18} color="var(--primary)" />
            <label htmlFor="patient-select" style={{ fontSize: '0.95rem', fontWeight: 700 }}>
              Viewing Requests For:
            </label>
          </div>
          <select
            id="patient-select"
            className="form-select"
            style={{ minHeight: '40px', width: 'auto', padding: '0.3rem 0.6rem', fontSize: '0.95rem', fontWeight: 600 }}
            value={activePatientId}
            onChange={(e) => handlePatientSelectChange(e.target.value)}
          >
            {allPatients.map((p) => (
              <option key={p.id} value={p.id}>
                {p.fullName} ({p.age} yrs, {p.villageTown || p.village || 'Village'})
              </option>
            ))}
          </select>
        </div>
      )}

      {isLoading && (
        <div style={{ textAlign: 'center', padding: '3rem 1rem' }}>
          <div className="pulse-spinner" />
          <p style={{ fontWeight: 600, color: 'var(--text-muted)' }}>Loading your requests...</p>
        </div>
      )}

      {isError && (
        <AlertBox type="danger">
          <strong>{error?.message || 'Could not load requests from server.'}</strong>
        </AlertBox>
      )}

      {detailError && (
        <AlertBox type="danger">
          <strong>{detailError}</strong>
        </AlertBox>
      )}

      {!isLoading && (!referrals || referrals.length === 0) && (
        <div className="card-simple" style={{ textAlign: 'center', padding: '3rem 1.5rem' }}>
          <ClipboardList size={54} color="var(--border-medium)" style={{ margin: '0 auto 1rem auto' }} />
          <h3 style={{ fontSize: '1.3rem', fontWeight: 700, color: 'var(--text-main)', marginBottom: '0.5rem' }}>
            No Active Care Requests Found
          </h3>
          <p style={{ color: 'var(--text-subtle)', marginBottom: '1.5rem', fontSize: '1.05rem' }}>
            {activePatientName
              ? `No care requests found for ${activePatientName}. Start a screening to connect with a facility.`
              : 'You have not sent any healthcare requests yet, or your previous screening was completed.'}
          </p>
          <button className="btn-large btn-primary" onClick={onStartNewScreening}>
            <span>Start Health Screening • तपासणी सुरू करा</span>
            <ArrowRight size={20} />
          </button>
        </div>
      )}

      {/* Referrals List */}
      <div style={{ display: 'flex', flexDirection: 'column', gap: '1.25rem' }}>
        {referrals?.map((ref) => {
          const isRejected = ref.status === 'REJECTED';

          return (
            <div
              key={ref.id}
              className="card-simple"
              style={{
                marginBottom: 0,
                borderLeft: `6px solid ${
                  ref.status === 'ACCEPTED'
                    ? 'var(--secondary)'
                    : isRejected
                    ? 'var(--risk-high)'
                    : 'var(--accent-warm)'
                }`,
              }}
            >
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', flexWrap: 'wrap', gap: '0.75rem', marginBottom: '0.75rem' }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                  <Building2 size={24} color="var(--primary)" />
                  <strong style={{ fontSize: '1.25rem', color: 'var(--text-main)' }}>
                    {ref.facilityName || 'Healthcare Centre'}
                  </strong>
                </div>
                {getStatusBadge(ref.status)}
              </div>

              {/* Date & Facility Type */}
              <div style={{ display: 'flex', alignItems: 'center', gap: '1rem', color: 'var(--text-muted)', fontSize: '0.95rem', marginBottom: '0.75rem', flexWrap: 'wrap' }}>
                <span style={{ display: 'flex', alignItems: 'center', gap: '0.35rem' }}>
                  <Calendar size={16} />
                  {ref.requestedAt || ref.createdAt
                    ? new Date(ref.requestedAt || ref.createdAt || '').toLocaleDateString()
                    : 'Today'}
                </span>
                {ref.facilityType && (
                  <>
                    <span>•</span>
                    <span>Centre Type: <strong>{ref.facilityType}</strong></span>
                  </>
                )}
                {ref.statusDisplayName && (
                  <>
                    <span>•</span>
                    <span>Status: <strong>{ref.statusDisplayName}</strong></span>
                  </>
                )}
              </div>

              {/* Patient Message */}
              {ref.patientMessage && (
                <p style={{ fontSize: '1rem', color: 'var(--text-main)', backgroundColor: 'var(--bg-subtle)', padding: '0.75rem', borderRadius: 'var(--radius-sm)', marginBottom: '1rem' }}>
                  <strong>Your note:</strong> "{ref.patientMessage}"
                </p>
              )}

              {/* Rejection Handling */}
              {isRejected && (
                <div
                  style={{
                    backgroundColor: 'var(--risk-high-bg)',
                    border: '1px solid var(--risk-high-border)',
                    borderRadius: 'var(--radius-md)',
                    padding: '1rem',
                    marginBottom: '1rem',
                  }}
                >
                  <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', color: 'var(--risk-high)', fontWeight: 700, marginBottom: '0.25rem' }}>
                    <AlertCircle size={20} />
                    <span>Facility Could Not Accept Request</span>
                  </div>
                  <p style={{ fontSize: '0.95rem', color: 'var(--text-main)', margin: '0 0 0.75rem 0' }}>
                    Reason: {ref.facilityResponse || 'Centre is currently at full capacity.'}
                  </p>
                  <button
                    className="btn-large btn-primary"
                    style={{ fontSize: '1.05rem', minHeight: '46px', padding: '0.6rem 1.25rem' }}
                    onClick={() => onReReferCase(ref.caseId)}
                  >
                    <span>Choose Another Nearby Facility • दुसरा दवाखाना निवडा</span>
                    <ArrowRight size={20} />
                  </button>
                </div>
              )}

              {/* Facility Response Notes if Accepted */}
              {ref.status === 'ACCEPTED' && ref.facilityResponse && (
                <div
                  style={{
                    backgroundColor: 'var(--secondary-light)',
                    border: '1px solid var(--secondary)',
                    borderRadius: 'var(--radius-md)',
                    padding: '0.75rem 1rem',
                    marginBottom: '1rem',
                    fontSize: '0.95rem',
                  }}
                >
                  <strong style={{ color: 'var(--secondary)' }}>Doctor / Centre Response:</strong>
                  <p style={{ margin: '0.25rem 0 0 0', color: 'var(--text-main)' }}>
                    "{ref.facilityResponse}"
                  </p>
                </div>
              )}

              {/* Action Buttons */}
              <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '0.75rem', marginTop: '0.5rem' }}>
                <button
                  type="button"
                  className="btn-large btn-outline"
                  style={{ width: 'auto', minHeight: '44px', padding: '0.5rem 1rem', fontSize: '0.95rem' }}
                  onClick={() => handleViewDetail(ref.id)}
                >
                  <Eye size={18} />
                  <span>View Details • तपशील</span>
                </button>
              </div>
            </div>
          );
        })}
      </div>

      {/* Simple Details Modal */}
      {selectedReferralDetail && (
        <div
          style={{
            position: 'fixed',
            inset: 0,
            backgroundColor: 'rgba(15, 23, 42, 0.6)',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            zIndex: 60,
            padding: '1rem',
          }}
          role="dialog"
          aria-modal="true"
        >
          <div
            className="card-simple"
            style={{
              maxWidth: '560px',
              width: '100%',
              maxHeight: '90vh',
              overflowY: 'auto',
              borderRadius: 'var(--radius-lg)',
              padding: '1.75rem',
            }}
          >
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1rem' }}>
              <h3 style={{ fontSize: '1.3rem', fontWeight: 800, color: 'var(--primary)' }}>
                Care Request Details
              </h3>
              {getStatusBadge(selectedReferralDetail.referral?.status || 'PENDING')}
            </div>

            <div style={{ display: 'flex', flexDirection: 'column', gap: '0.75rem', fontSize: '1rem' }}>
              <div>
                <strong>Referral Reference:</strong>{' '}
                <span style={{ fontFamily: 'monospace', fontSize: '0.9rem' }}>{selectedReferralDetail.referral?.id}</span>
              </div>
              <div>
                <strong>Receiving Facility:</strong>{' '}
                {selectedReferralDetail.facility?.name ||
                  selectedReferralDetail.facility?.facilityName ||
                  selectedReferralDetail.referral?.facilityName ||
                  'Healthcare Centre'}
              </div>
              <div>
                <strong>Patient Name:</strong> {selectedReferralDetail.patient?.fullName || 'Patient'}
              </div>
              <div>
                <strong>Village / Town:</strong>{' '}
                {selectedReferralDetail.patient?.villageTown ||
                  selectedReferralDetail.patient?.village ||
                  'Shirwal'}
              </div>
              {selectedReferralDetail.aiAssessment && (
                <div style={{ backgroundColor: 'var(--bg-subtle)', padding: '0.75rem', borderRadius: 'var(--radius-sm)' }}>
                  <strong>Clinical Assessment:</strong>{' '}
                  <span style={{ color: 'var(--risk-high)', fontWeight: 700 }}>
                    {selectedReferralDetail.aiAssessment.riskLevel} Risk • {selectedReferralDetail.aiAssessment.urgencyLevel}
                  </span>
                  {selectedReferralDetail.aiAssessment.urgencyReason && (
                    <p style={{ margin: '0.25rem 0 0 0', fontSize: '0.9rem' }}>
                      {selectedReferralDetail.aiAssessment.urgencyReason}
                    </p>
                  )}
                </div>
              )}
              {selectedReferralDetail.referral?.patientMessage && (
                <div>
                  <strong>Patient Message:</strong> {selectedReferralDetail.referral.patientMessage}
                </div>
              )}
              {selectedReferralDetail.referral?.facilityResponse && (
                <div style={{ backgroundColor: 'var(--secondary-light)', padding: '0.75rem', borderRadius: 'var(--radius-sm)' }}>
                  <strong style={{ color: 'var(--secondary)' }}>Facility Response:</strong> {selectedReferralDetail.referral.facilityResponse}
                </div>
              )}
            </div>

            <div style={{ marginTop: '1.75rem', textAlign: 'right' }}>
              <button
                className="btn-large btn-primary"
                style={{ width: 'auto', minHeight: '44px', padding: '0.5rem 1.5rem' }}
                onClick={() => setSelectedReferralDetail(null)}
              >
                Close • बंद करा
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
