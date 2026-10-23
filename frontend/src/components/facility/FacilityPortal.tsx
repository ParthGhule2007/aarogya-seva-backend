import React, { useState } from 'react';
import {
  Building2,
  Clock,
  CheckCircle2,
  XCircle,
  AlertTriangle,
  Eye,
  RefreshCw,
  Phone,
  User,
  Activity,
  ArrowRight
} from 'lucide-react';
import { FacilityDto } from '../../types/facility';
import { ReferralResponseDto, ReferralDetailDto, ReferralStatus } from '../../types/referral';
import { facilityApi } from '../../api/facilityApi';
import { referralApi } from '../../api/referralApi';
import { useQuery } from '../../api/queryClient';
import { ReferralDetailModal } from '../referral/ReferralDetailModal';
import { ReferralActionModal } from '../referral/ReferralActionModal';
import { AlertBox } from '../common/AlertBox';

export const FacilityPortal: React.FC = () => {
  const [selectedFacilityId, setSelectedFacilityId] = useState<string>('');
  const [activeStatusTab, setActiveStatusTab] = useState<ReferralStatus>('PENDING');

  // Modals
  const [detailModalOpen, setDetailModalOpen] = useState(false);
  const [selectedReferralDetail, setSelectedReferralDetail] = useState<ReferralDetailDto | null>(null);
  const [isLoadingDetail, setIsLoadingDetail] = useState(false);

  const [actionModalOpen, setActionModalOpen] = useState(false);
  const [targetReferral, setTargetReferral] = useState<ReferralResponseDto | null>(null);
  const [targetAction, setTargetAction] = useState<ReferralStatus | null>(null);

  // 1. Fetch available facilities to choose current workplace
  const { data: facilities, isLoading: isLoadingFacilities } = useQuery<FacilityDto[]>(
    async () => {
      const res = await facilityApi.getAllFacilities();
      if (res && res.length > 0 && !selectedFacilityId) {
        setSelectedFacilityId(res[0].id);
      }
      return res;
    }
  );

  // 2. Fetch referrals for the chosen facility
  const {
    data: referrals,
    isLoading: isLoadingReferrals,
    refetch: refetchReferrals,
  } = useQuery<ReferralResponseDto[]>(
    async () => {
      if (!selectedFacilityId) return [];
      const res = await referralApi.getReferralsByFacilityId(selectedFacilityId);
      return res;
    },
    [selectedFacilityId],
    !!selectedFacilityId
  );

  const activeFacility = facilities?.find((f) => f.id === selectedFacilityId);

  // Filter referrals by status tab
  const filteredReferrals = referrals?.filter((r) => r.status === activeStatusTab) || [];

  const handleOpenDetail = async (referralId: string) => {
    setIsLoadingDetail(true);
    try {
      const res = await referralApi.getReferralById(referralId);
      setSelectedReferralDetail(res);
      setDetailModalOpen(true);
    } catch (err: any) {
      alert(err.message || 'Could not load referral clinical details');
    } finally {
      setIsLoadingDetail(false);
    }
  };

  const handleOpenAction = (referral: ReferralResponseDto, action: ReferralStatus) => {
    setTargetReferral(referral);
    setTargetAction(action);
    setActionModalOpen(true);
  };

  const handleActionSuccess = (_updated: ReferralResponseDto) => {
    refetchReferrals();
  };

  return (
    <div style={{ maxWidth: '1000px', margin: '0 auto' }}>
      {/* Header Banner & Facility Switcher */}
      <div
        className="card-simple"
        style={{
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center',
          flexWrap: 'wrap',
          gap: '1rem',
          backgroundColor: 'var(--bg-surface)',
          marginBottom: '1.5rem',
        }}
      >
        <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
          <div
            style={{
              width: '52px',
              height: '52px',
              borderRadius: 'var(--radius-md)',
              backgroundColor: 'var(--primary-light)',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
            }}
          >
            <Building2 size={30} color="var(--primary)" />
          </div>
          <div>
            <h1 style={{ fontSize: '1.4rem', fontWeight: 800, color: 'var(--primary)' }}>
              Healthcare Facility Portal • रुग्णालय पोर्टल
            </h1>
            <p style={{ fontSize: '0.95rem', color: 'var(--text-muted)' }}>
              Review incoming patient care requests, evaluate clinical AI screening, and triage admissions.
            </p>
          </div>
        </div>

        {/* Facility Selector */}
        <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
          <label htmlFor="facility-selector" style={{ fontWeight: 700, fontSize: '0.95rem' }}>
            Current Centre:
          </label>
          <select
            id="facility-selector"
            className="form-select"
            style={{ minHeight: '44px', width: 'auto', fontWeight: 600 }}
            value={selectedFacilityId}
            onChange={(e) => setSelectedFacilityId(e.target.value)}
          >
            {facilities?.map((f) => (
              <option key={f.id} value={f.id}>
                {f.name || f.facilityName || 'Centre'} ({f.facilityType || 'Centre'})
              </option>
            ))}
          </select>
        </div>
      </div>

      {activeFacility && (
        <div
          style={{
            backgroundColor: 'var(--bg-subtle)',
            borderRadius: 'var(--radius-md)',
            padding: '0.75rem 1rem',
            marginBottom: '1.5rem',
            display: 'flex',
            justifyContent: 'space-between',
            alignItems: 'center',
            fontSize: '0.95rem',
          }}
        >
          <div>
            <strong>Operational Status:</strong>{' '}
            <span style={{ color: (activeFacility.isOperational ?? true) ? 'var(--secondary)' : 'var(--risk-high)', fontWeight: 700 }}>
              ✓ {activeFacility.isOperational ? 'OPERATIONAL' : (activeFacility.operationalStatus || 'INACTIVE')}
            </span>{' '}
            • <strong>District:</strong> {activeFacility.district || 'Satara'}
          </div>
          <button
            className="btn-large btn-outline"
            style={{ width: 'auto', minHeight: '38px', padding: '0.3rem 0.75rem', fontSize: '0.85rem' }}
            onClick={() => refetchReferrals()}
          >
            <RefreshCw size={14} /> Refresh Requests
          </button>
        </div>
      )}

      {/* Referral Status Filter Tabs */}
      <div
        style={{
          display: 'flex',
          gap: '0.5rem',
          flexWrap: 'wrap',
          marginBottom: '1.5rem',
          borderBottom: '2px solid var(--border-light)',
          paddingBottom: '0.5rem',
        }}
      >
        {(['PENDING', 'ACCEPTED', 'IN_PROGRESS', 'COMPLETED', 'REJECTED'] as ReferralStatus[]).map((tab) => {
          const count = referrals?.filter((r) => r.status === tab).length || 0;
          const isActive = activeStatusTab === tab;

          return (
            <button
              key={tab}
              className={`nav-item ${isActive ? 'active' : ''}`}
              style={{ fontSize: '0.95rem', padding: '0.5rem 0.9rem' }}
              onClick={() => setActiveStatusTab(tab)}
            >
              <span>
                {tab === 'PENDING' && 'Pending Requests'}
                {tab === 'ACCEPTED' && 'Accepted Care'}
                {tab === 'IN_PROGRESS' && 'In Progress'}
                {tab === 'COMPLETED' && 'Completed'}
                {tab === 'REJECTED' && 'Rejected'}
              </span>
              <span
                style={{
                  backgroundColor: isActive ? 'var(--primary)' : 'var(--bg-subtle)',
                  color: isActive ? '#fff' : 'var(--text-muted)',
                  borderRadius: '10px',
                  padding: '0.1rem 0.45rem',
                  fontSize: '0.8rem',
                  fontWeight: 700,
                  marginLeft: '0.3rem',
                }}
              >
                {count}
              </span>
            </button>
          );
        })}
      </div>

      {isLoadingReferrals && (
        <div style={{ textAlign: 'center', padding: '3rem' }}>
          <div className="pulse-spinner" />
          <p>Loading incoming requests...</p>
        </div>
      )}

      {/* Empty State */}
      {!isLoadingReferrals && filteredReferrals.length === 0 && (
        <div className="card-simple" style={{ textAlign: 'center', padding: '3rem 1.5rem' }}>
          <Clock size={48} color="var(--border-medium)" style={{ margin: '0 auto 1rem auto' }} />
          <h3 style={{ fontSize: '1.25rem', fontWeight: 700, marginBottom: '0.3rem' }}>
            No {activeStatusTab.toLowerCase()} requests
          </h3>
          <p style={{ color: 'var(--text-subtle)', fontSize: '0.95rem' }}>
            There are currently no patient care requests matching this status for {activeFacility?.name || 'this centre'}.
          </p>
        </div>
      )}

      {/* Referrals Cards List */}
      <div style={{ display: 'flex', flexDirection: 'column', gap: '1.25rem' }}>
        {filteredReferrals.map((referral) => (
          <div
            key={referral.id}
            className="card-simple"
            style={{
              marginBottom: 0,
              padding: '1.5rem',
              borderLeft: `6px solid ${
                referral.priority === 'CRITICAL'
                  ? 'var(--risk-high)'
                  : referral.status === 'REJECTED'
                  ? 'var(--risk-high)'
                  : referral.status === 'ACCEPTED' || referral.status === 'COMPLETED'
                  ? 'var(--secondary)'
                  : 'var(--primary)'
              }`,
            }}
          >
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', flexWrap: 'wrap', gap: '0.75rem', marginBottom: '0.75rem' }}>
              <div>
                <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', marginBottom: '0.2rem' }}>
                  <User size={20} color="var(--primary)" />
                  <h3 style={{ fontSize: '1.3rem', fontWeight: 800, color: 'var(--text-main)' }}>
                    Referral Request #{referral.id.substring(0, 8)}
                  </h3>
                </div>
                <div style={{ fontSize: '0.9rem', color: 'var(--text-muted)' }}>
                  Requested:{' '}
                  {referral.requestedAt || referral.createdAt
                    ? new Date(referral.requestedAt || referral.createdAt || '').toLocaleString()
                    : 'Recently'}
                </div>
              </div>

              {/* Status & Priority Badge */}
              <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', flexWrap: 'wrap' }}>
                {referral.priority && (
                  <span
                    className="badge"
                    style={{
                      backgroundColor:
                        referral.priority === 'CRITICAL'
                          ? 'var(--risk-high-bg)'
                          : referral.priority === 'HIGH'
                          ? '#ffedd5'
                          : 'var(--bg-subtle)',
                      color:
                        referral.priority === 'CRITICAL'
                          ? 'var(--risk-high)'
                          : referral.priority === 'HIGH'
                          ? '#c2410c'
                          : 'var(--text-main)',
                      fontWeight: 700,
                    }}
                  >
                    Priority: {referral.priority}
                  </span>
                )}
                <span className={`status-pill ${referral.status.toLowerCase()}`}>
                  {referral.statusDisplayName || referral.status}
                </span>
              </div>
            </div>

            {/* Patient Message */}
            {referral.patientMessage && (
              <div
                style={{
                  backgroundColor: 'var(--bg-subtle)',
                  padding: '0.85rem 1rem',
                  borderRadius: 'var(--radius-sm)',
                  marginBottom: '1rem',
                  fontSize: '0.95rem',
                }}
              >
                <strong>Patient Message:</strong> "{referral.patientMessage}"
              </div>
            )}

            {/* Response Notes if already accepted/rejected */}
            {referral.facilityResponse && (
              <div
                style={{
                  backgroundColor: referral.status === 'REJECTED' ? 'var(--risk-high-bg)' : 'var(--secondary-light)',
                  padding: '0.75rem 1rem',
                  borderRadius: 'var(--radius-sm)',
                  marginBottom: '1rem',
                  fontSize: '0.95rem',
                }}
              >
                <strong>Clinical Response:</strong> "{referral.facilityResponse}"
              </div>
            )}

            {/* Action Bar */}
            <div
              style={{
                display: 'flex',
                justifyContent: 'space-between',
                alignItems: 'center',
                flexWrap: 'wrap',
                gap: '0.75rem',
                marginTop: '1rem',
                borderTop: '1px solid var(--border-light)',
                paddingTop: '1rem',
              }}
            >
              <button
                className="btn-large btn-outline"
                style={{ width: 'auto', minHeight: '42px', padding: '0.4rem 1rem', fontSize: '0.95rem' }}
                onClick={() => handleOpenDetail(referral.id)}
              >
                <Eye size={18} />
                <span>Review Full Clinical Context</span>
              </button>

              <div style={{ display: 'flex', gap: '0.5rem', flexWrap: 'wrap' }}>
                {referral.status === 'PENDING' && (
                  <>
                    <button
                      className="btn-large btn-secondary"
                      style={{ width: 'auto', minHeight: '42px', padding: '0.4rem 1.25rem', fontSize: '0.95rem' }}
                      onClick={() => handleOpenAction(referral, 'ACCEPTED')}
                    >
                      <CheckCircle2 size={18} />
                      <span>Accept Request</span>
                    </button>

                    <button
                      className="btn-large btn-ghost"
                      style={{ width: 'auto', minHeight: '42px', padding: '0.4rem 1rem', fontSize: '0.95rem', color: 'var(--risk-high)' }}
                      onClick={() => handleOpenAction(referral, 'REJECTED')}
                    >
                      <XCircle size={18} />
                      <span>Reject</span>
                    </button>
                  </>
                )}

                {referral.status === 'ACCEPTED' && (
                  <button
                    className="btn-large btn-primary"
                    style={{ width: 'auto', minHeight: '42px', padding: '0.4rem 1.25rem', fontSize: '0.95rem' }}
                    onClick={() => handleOpenAction(referral, 'IN_PROGRESS')}
                  >
                    <span>Start Clinical Care →</span>
                  </button>
                )}

                {referral.status === 'IN_PROGRESS' && (
                  <button
                    className="btn-large btn-secondary"
                    style={{ width: 'auto', minHeight: '42px', padding: '0.4rem 1.25rem', fontSize: '0.95rem' }}
                    onClick={() => handleOpenAction(referral, 'COMPLETED')}
                  >
                    <CheckCircle2 size={18} />
                    <span>Mark Complete ✓</span>
                  </button>
                )}
              </div>
            </div>
          </div>
        ))}
      </div>

      {/* Clinical Review Detail Modal */}
      <ReferralDetailModal
        isOpen={detailModalOpen}
        onClose={() => setDetailModalOpen(false)}
        detail={selectedReferralDetail}
      />

      {/* Status Action Transition Modal */}
      <ReferralActionModal
        isOpen={actionModalOpen}
        onClose={() => setActionModalOpen(false)}
        referral={targetReferral}
        targetAction={targetAction}
        onSuccess={handleActionSuccess}
      />
    </div>
  );
};
