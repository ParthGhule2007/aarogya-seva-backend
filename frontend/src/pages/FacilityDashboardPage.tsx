import React, { useState, useEffect } from 'react';
import { facilityApi } from '../api/facilityApi';
import { referralApi } from '../api/referralApi';
import { FacilityDto } from '../types/facility';
import { ReferralDetailDto, ReferralResponseDto, ReferralStatus } from '../types/referral';
import { Button } from '../components/common/Button';
import { Badge } from '../components/common/Badge';
import { AlertBanner } from '../components/common/AlertBanner';
import { LoadingSpinner } from '../components/common/LoadingSpinner';
import { EmptyState } from '../components/common/EmptyState';
import { ReferralDetailModal } from '../components/referral/ReferralDetailModal';
import { ReferralActionModal } from '../components/referral/ReferralActionModal';
import {
  Building2,
  Clock,
  CheckCircle,
  XCircle,
  Play,
  RotateCcw,
  Eye,
  Calendar,
  AlertCircle,
} from 'lucide-react';

export const FacilityDashboardPage: React.FC = () => {
  const [facilities, setFacilities] = useState<FacilityDto[]>([]);
  const [selectedFacility, setSelectedFacility] = useState<FacilityDto | null>(null);
  const [referrals, setReferrals] = useState<ReferralResponseDto[]>([]);
  const [activeTab, setActiveTab] = useState<ReferralStatus | 'ALL'>('ALL');

  // Loading & Error states
  const [isLoadingFacilities, setIsLoadingFacilities] = useState(false);
  const [isLoadingReferrals, setIsLoadingReferrals] = useState(false);
  const [banner, setBanner] = useState<{ type: 'info' | 'success' | 'warning' | 'danger'; message: string } | null>(null);

  // Modals state
  const [reviewDetail, setReviewDetail] = useState<ReferralDetailDto | null>(null);
  const [isReviewOpen, setIsReviewOpen] = useState(false);
  const [isLoadingDetail, setIsLoadingDetail] = useState(false);

  const [actionReferral, setActionReferral] = useState<ReferralResponseDto | null>(null);
  const [targetAction, setTargetAction] = useState<ReferralStatus | null>(null);
  const [isActionOpen, setIsActionOpen] = useState(false);

  // Load facilities on mount
  useEffect(() => {
    loadFacilities();
  }, []);

  // When facility changes or tab changes, load referrals
  useEffect(() => {
    if (selectedFacility) {
      loadReferrals(selectedFacility.id, activeTab);
    }
  }, [selectedFacility, activeTab]);

  const loadFacilities = async () => {
    setIsLoadingFacilities(true);
    try {
      const data = await facilityApi.getAllFacilities();
      setFacilities(data);
      if (data.length > 0 && !selectedFacility) {
        setSelectedFacility(data[0]);
      }
    } catch (err: any) {
      setBanner({ type: 'danger', message: `Could not load facilities: ${err.message}` });
    } finally {
      setIsLoadingFacilities(false);
    }
  };

  const loadReferrals = async (facilityId: string, statusFilter: ReferralStatus | 'ALL') => {
    setIsLoadingReferrals(true);
    try {
      const filter = statusFilter === 'ALL' ? undefined : statusFilter;
      const data = await referralApi.getReferralsByFacilityId(facilityId, filter);
      setReferrals(data);
    } catch (err: any) {
      setBanner({ type: 'danger', message: `Could not load facility referrals: ${err.message}` });
    } finally {
      setIsLoadingReferrals(false);
    }
  };

  const handleOpenDetail = async (referralId: string) => {
    setIsLoadingDetail(true);
    try {
      const detail = await referralApi.getReferralById(referralId);
      setReviewDetail(detail);
      setIsReviewOpen(true);
    } catch (err: any) {
      setBanner({ type: 'danger', message: `Failed to fetch referral evaluation context: ${err.message}` });
    } finally {
      setIsLoadingDetail(false);
    }
  };

  const handleOpenAction = (referral: ReferralResponseDto, action: ReferralStatus) => {
    setActionReferral(referral);
    setTargetAction(action);
    setIsActionOpen(true);
  };

  const handleActionSuccess = (updated: ReferralResponseDto) => {
    setBanner({
      type: 'success',
      message: `Referral status updated to ${updated.status} successfully.`,
    });
    if (selectedFacility) {
      loadReferrals(selectedFacility.id, activeTab);
    }
  };

  // Counts
  const pendingCount = referrals.filter((r) => r.status === 'PENDING').length;

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '2rem' }}>
      {banner && (
        <AlertBanner
          type={banner.type}
          message={banner.message}
          onDismiss={() => setBanner(null)}
        />
      )}

      {/* Header */}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: '1rem' }}>
        <div>
          <h2 style={{ fontSize: '1.75rem', fontWeight: 800 }}>Healthcare Facility Portal (रुग्णालय डॅशबोर्ड)</h2>
          <p style={{ color: 'var(--text-muted)' }}>
            Review incoming patient referrals, examine AI clinical screening context, and manage care progression.
          </p>
        </div>

        <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
          {selectedFacility && (
            <Button
              variant="secondary"
              size="sm"
              icon={<RotateCcw size={14} />}
              onClick={() => loadReferrals(selectedFacility.id, activeTab)}
            >
              Refresh
            </Button>
          )}
        </div>
      </div>

      {/* Facility Switcher Card */}
      <div className="card">
        <div className="card-header">
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.6rem' }}>
            <Building2 size={22} style={{ color: 'var(--primary)' }} />
            <h3 style={{ fontSize: '1.2rem' }}>Select Healthcare Facility</h3>
          </div>
          {selectedFacility && (
            <div style={{ display: 'flex', gap: '0.5rem' }}>
              <Badge variant="facilityType" value={selectedFacility.facilityType} />
              {selectedFacility.isDemoData && <Badge variant="demo" />}
            </div>
          )}
        </div>

        {isLoadingFacilities ? (
          <LoadingSpinner size="sm" message="Loading facility directory..." />
        ) : (
          <div>
            <div className="form-group" style={{ marginBottom: '0.75rem' }}>
              <label className="form-label">Active Receiving Facility</label>
              <select
                className="form-select"
                value={selectedFacility?.id || ''}
                onChange={(e) => {
                  const found = facilities.find((f) => f.id === e.target.value);
                  if (found) setSelectedFacility(found);
                }}
              >
                {facilities.map((f) => (
                  <option key={f.id} value={f.id}>
                    {f.name} ({f.facilityType}) — {f.villageTown}, {f.district}
                  </option>
                ))}
              </select>
            </div>

            {selectedFacility && (
              <div
                style={{
                  padding: '0.85rem',
                  backgroundColor: 'var(--bg-surface-subtle)',
                  borderRadius: 'var(--radius-md)',
                  display: 'flex',
                  justifyContent: 'space-between',
                  alignItems: 'center',
                  flexWrap: 'wrap',
                  gap: '0.75rem',
                  fontSize: '0.85rem',
                }}
              >
                <div>
                  <strong>Location:</strong> {selectedFacility.addressLine1}, {selectedFacility.villageTown}, {selectedFacility.district} - {selectedFacility.pincode}
                </div>
                <div>
                  <strong>Phone:</strong> {selectedFacility.contactNumber}
                </div>
                <div>
                  <strong>Status:</strong>{' '}
                  <span style={{ color: selectedFacility.isOperational ? 'var(--success)' : 'var(--danger)', fontWeight: 700 }}>
                    {selectedFacility.isOperational ? 'Operational (सुरू)' : 'Non-Operational (बंद)'}
                  </span>
                </div>
              </div>
            )}
          </div>
        )}
      </div>

      {/* Referrals Tabs & List */}
      <div className="card">
        {/* Tab Filters */}
        <div
          style={{
            display: 'flex',
            gap: '0.5rem',
            borderBottom: '1px solid var(--border-light)',
            paddingBottom: '0.75rem',
            marginBottom: '1.25rem',
            overflowX: 'auto',
          }}
        >
          {(['ALL', 'PENDING', 'ACCEPTED', 'IN_PROGRESS', 'COMPLETED', 'REJECTED'] as const).map((tab) => (
            <button
              key={tab}
              onClick={() => setActiveTab(tab)}
              style={{
                padding: '0.5rem 1rem',
                borderRadius: 'var(--radius-md)',
                border: 'none',
                backgroundColor: activeTab === tab ? 'var(--primary)' : 'var(--bg-surface-subtle)',
                color: activeTab === tab ? 'white' : 'var(--text-muted)',
                fontWeight: 600,
                fontSize: '0.85rem',
                cursor: 'pointer',
                transition: 'all 0.15s ease',
              }}
            >
              {tab.replace(/_/g, ' ')}
              {tab === 'PENDING' && pendingCount > 0 && (
                <span
                  style={{
                    marginLeft: '6px',
                    padding: '2px 6px',
                    borderRadius: 'var(--radius-full)',
                    backgroundColor: activeTab === 'PENDING' ? 'white' : 'var(--danger)',
                    color: activeTab === 'PENDING' ? 'var(--primary-dark)' : 'white',
                    fontSize: '0.7rem',
                    fontWeight: 800,
                  }}
                >
                  {pendingCount}
                </span>
              )}
            </button>
          ))}
        </div>

        {/* Referrals List */}
        {isLoadingReferrals ? (
          <LoadingSpinner message="Fetching referrals for selected facility..." />
        ) : referrals.length === 0 ? (
          <EmptyState
            title="No Referrals Found"
            description={`No referral requests matching '${activeTab}' were found for ${selectedFacility?.name || 'this facility'}.`}
          />
        ) : (
          <div style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
            {referrals.map((ref) => (
              <div
                key={ref.id}
                style={{
                  padding: '1.25rem',
                  backgroundColor: 'var(--bg-surface)',
                  border: '1px solid var(--border-light)',
                  borderRadius: 'var(--radius-md)',
                  boxShadow: 'var(--shadow-sm)',
                  display: 'flex',
                  flexDirection: 'column',
                  gap: '0.75rem',
                }}
              >
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', flexWrap: 'wrap', gap: '0.5rem' }}>
                  <div>
                    <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', marginBottom: '0.2rem' }}>
                      <span style={{ fontWeight: 800, fontSize: '1.05rem' }}>Referral Request</span>
                      <span style={{ fontSize: '0.8rem', color: 'var(--text-subtle)' }}>({ref.id.substring(0, 8)}...)</span>
                    </div>
                    <div style={{ fontSize: '0.8rem', color: 'var(--text-muted)', display: 'flex', alignItems: 'center', gap: '0.35rem' }}>
                      <Calendar size={13} />
                      <span>Requested: {new Date(ref.requestedAt).toLocaleString()}</span>
                    </div>
                  </div>

                  <Badge variant="status" value={ref.status} />
                </div>

                {ref.patientMessage && (
                  <div
                    style={{
                      padding: '0.65rem 0.85rem',
                      backgroundColor: 'var(--bg-surface-subtle)',
                      borderRadius: 'var(--radius-sm)',
                      fontSize: '0.875rem',
                    }}
                  >
                    <strong>Patient Note:</strong> {ref.patientMessage}
                  </div>
                )}

                {ref.facilityResponse && (
                  <div
                    style={{
                      padding: '0.65rem 0.85rem',
                      backgroundColor: '#f0fdf4',
                      borderLeft: '3px solid var(--success)',
                      borderRadius: 'var(--radius-sm)',
                      fontSize: '0.875rem',
                    }}
                  >
                    <strong>Your Response:</strong> {ref.facilityResponse}
                  </div>
                )}

                {/* Action Buttons */}
                <div
                  style={{
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'space-between',
                    paddingTop: '0.75rem',
                    borderTop: '1px solid var(--border-light)',
                    flexWrap: 'wrap',
                    gap: '0.5rem',
                  }}
                >
                  <Button
                    size="sm"
                    variant="secondary"
                    icon={<Eye size={14} />}
                    onClick={() => handleOpenDetail(ref.id)}
                    isLoading={isLoadingDetail}
                  >
                    Review Patient & AI Clinical Assessment
                  </Button>

                  <div style={{ display: 'flex', gap: '0.5rem' }}>
                    {ref.status === 'PENDING' && (
                      <>
                        <Button
                          size="sm"
                          variant="danger"
                          icon={<XCircle size={14} />}
                          onClick={() => handleOpenAction(ref, 'REJECTED')}
                        >
                          Reject Referral (नकार)
                        </Button>
                        <Button
                          size="sm"
                          variant="success"
                          icon={<CheckCircle size={14} />}
                          onClick={() => handleOpenAction(ref, 'ACCEPTED')}
                        >
                          Accept Referral (स्वीकारा)
                        </Button>
                      </>
                    )}

                    {ref.status === 'ACCEPTED' && (
                      <Button
                        size="sm"
                        variant="primary"
                        icon={<Play size={14} />}
                        onClick={() => handleOpenAction(ref, 'IN_PROGRESS')}
                      >
                        Start Clinical Care (IN PROGRESS)
                      </Button>
                    )}

                    {ref.status === 'IN_PROGRESS' && (
                      <Button
                        size="sm"
                        variant="success"
                        icon={<CheckCircle size={14} />}
                        onClick={() => handleOpenAction(ref, 'COMPLETED')}
                      >
                        Mark Completed (पूर्ण करा)
                      </Button>
                    )}
                  </div>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>

      {/* Modals */}
      <ReferralDetailModal
        isOpen={isReviewOpen}
        onClose={() => setIsReviewOpen(false)}
        detail={reviewDetail}
      />

      <ReferralActionModal
        isOpen={isActionOpen}
        onClose={() => setIsActionOpen(false)}
        referral={actionReferral}
        targetAction={targetAction}
        onSuccess={handleActionSuccess}
      />
    </div>
  );
};
