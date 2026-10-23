import React, { useState, useEffect } from 'react';
import { useAuth } from '../context/AuthContext';
import { facilityApi } from '../api/facilityApi';
import { referralApi } from '../api/referralApi';
import { sosApi } from '../api/sosApi';
import { FacilityDto } from '../types/facility';
import { ReferralDetailDto, ReferralResponseDto, ReferralStatus } from '../types/referral';
import { SosStatusResponse } from '../types/sos';
import { Button } from '../components/common/Button';
import { Badge } from '../components/common/Badge';
import { AlertBanner } from '../components/common/AlertBanner';
import { LoadingSpinner } from '../components/common/LoadingSpinner';
import { EmptyState } from '../components/common/EmptyState';
import { ReferralDetailModal } from '../components/referral/ReferralDetailModal';
import { ReferralActionModal } from '../components/referral/ReferralActionModal';
import { ReferralTimeline } from '../components/common/medical/ReferralTimeline';
import { StatusBadge } from '../components/common/medical/StatusBadge';
import {
  Building2,
  Clock,
  CheckCircle2,
  XCircle,
  Play,
  RotateCcw,
  Eye,
  Calendar,
  AlertTriangle,
  Phone,
  MapPin,
  CheckCheck,
  MessageSquare,
  ShieldCheck,
  Activity,
  Layers,
  ArrowRight
} from 'lucide-react';

export const FacilityDashboardPage: React.FC = () => {
  const { user, role } = useAuth();
  const [facilities, setFacilities] = useState<FacilityDto[]>([]);
  const [selectedFacility, setSelectedFacility] = useState<FacilityDto | null>(null);
  const [allFacilityReferrals, setAllFacilityReferrals] = useState<ReferralResponseDto[]>([]);
  const [referrals, setReferrals] = useState<ReferralResponseDto[]>([]);
  const [facilitySosEvents, setFacilitySosEvents] = useState<SosStatusResponse[]>([]);
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

  // When facility changes, load all referrals for KPI metrics & active tab
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
      const userAssignedId = user?.facilityId || user?.associatedEntityId;
      if (userAssignedId) {
        const assigned = data.find((f) => f.id === userAssignedId);
        if (assigned) {
          setSelectedFacility(assigned);
          return;
        }
      }
      if (data.length > 0 && !selectedFacility) {
        setSelectedFacility(data[0]);
      }
    } catch (err: any) {
      setBanner({ type: 'danger', message: `Could not load facility directory: ${err.message}` });
    } finally {
      setIsLoadingFacilities(false);
    }
  };

  const loadReferrals = async (facilityId: string, statusFilter: ReferralStatus | 'ALL') => {
    setIsLoadingReferrals(true);
    try {
      // 1. Always fetch full list for KPI counts
      const allData = await referralApi.getReferralsByFacilityId(facilityId);
      setAllFacilityReferrals(allData || []);

      // 2. Filter for active view
      if (statusFilter === 'ALL') {
        setReferrals(allData || []);
      } else {
        const filtered = (allData || []).filter((r) => r.status === statusFilter);
        setReferrals(filtered);
      }

      // 3. Also load any emergency SOS alerts dispatched to this facility
      sosApi
        .getSosByFacility(facilityId)
        .then((sosList) => setFacilitySosEvents((sosList || []).filter((s) => s.status !== 'RESOLVED')))
        .catch(() => {});
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
      message: `Referral #${updated.referralToken || updated.id.substring(0, 8)} transitioned to ${updated.status} successfully.`,
    });
    if (selectedFacility) {
      loadReferrals(selectedFacility.id, activeTab);
    }
  };

  // KPI Metrics calculated from allFacilityReferrals
  const pendingCount = allFacilityReferrals.filter((r) => r.status === 'PENDING').length;
  const acceptedCount = allFacilityReferrals.filter((r) => r.status === 'ACCEPTED').length;
  const inProgressCount = allFacilityReferrals.filter((r) => r.status === 'IN_PROGRESS').length;
  const completedCount = allFacilityReferrals.filter((r) => r.status === 'COMPLETED').length;
  const rejectedCount = allFacilityReferrals.filter((r) => r.status === 'REJECTED').length;
  const totalCount = allFacilityReferrals.length;

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '1.75rem', maxWidth: '1200px', margin: '0 auto' }}>
      {banner && (
        <AlertBanner
          type={banner.type}
          message={banner.message}
          onDismiss={() => setBanner(null)}
        />
      )}

      {/* Main Header */}
      <div
        style={{
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center',
          flexWrap: 'wrap',
          gap: '1rem',
          paddingBottom: '0.5rem',
        }}
      >
        <div>
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.6rem', marginBottom: '0.2rem' }}>
            <Building2 size={26} color="var(--primary)" />
            <h1 style={{ fontSize: '1.75rem', fontWeight: 800, color: 'var(--text-main)', margin: 0 }}>
              Healthcare Facility Portal • रुग्णालय पोर्टल
            </h1>
          </div>
          <p style={{ color: 'var(--text-muted)', fontSize: '0.95rem', margin: 0 }}>
            Review incoming patient referrals, evaluate AI clinical triage screening, and coordinate admissions.
          </p>
        </div>

        <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
          {selectedFacility && (
            <Button
              variant="outline"
              size="sm"
              icon={<RotateCcw size={15} />}
              onClick={() => loadReferrals(selectedFacility.id, activeTab)}
              isLoading={isLoadingReferrals}
            >
              Refresh Queue (रिफ्रेश)
            </Button>
          )}
        </div>
      </div>

      {/* Facility Selector & Active Centre Banner */}
      <div className="card" style={{ padding: '1.25rem', marginBottom: 0 }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: '0.75rem', marginBottom: '1rem' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
            <ShieldCheck size={20} color="var(--primary)" />
            <h3 style={{ fontSize: '1.15rem', fontWeight: 700, margin: 0 }}>
              Active Receiving Facility • नियुक्त आरोग्य केंद्र
            </h3>
          </div>
          {selectedFacility && (
            <div style={{ display: 'flex', gap: '0.5rem', alignItems: 'center' }}>
              <Badge variant="facilityType" value={selectedFacility.facilityType} />
              <span
                style={{
                  display: 'inline-flex',
                  alignItems: 'center',
                  gap: '0.3rem',
                  padding: '0.25rem 0.65rem',
                  borderRadius: 'var(--radius-full)',
                  fontSize: '0.8rem',
                  fontWeight: 700,
                  backgroundColor: selectedFacility.isOperational ? '#dcfce7' : '#fee2e2',
                  color: selectedFacility.isOperational ? '#15803d' : '#991b1b',
                }}
              >
                ✓ {selectedFacility.isOperational ? 'Operational (सुरू)' : 'Non-Operational (बंद)'}
              </span>
            </div>
          )}
        </div>

        {isLoadingFacilities ? (
          <LoadingSpinner size="sm" message="Loading facility directory..." />
        ) : (
          <div>
            <div style={{ marginBottom: '0.85rem' }}>
              <select
                className="form-select"
                style={{ width: '100%', minHeight: '44px', fontWeight: 600, fontSize: '0.95rem' }}
                disabled={role === 'FACILITY_ADMIN'}
                value={selectedFacility?.id || ''}
                onChange={(e) => {
                  const found = facilities.find((f) => f.id === e.target.value);
                  if (found) setSelectedFacility(found);
                }}
              >
                {facilities.map((f) => (
                  <option key={f.id} value={f.id}>
                    {f.name} ({f.facilityTypeDisplayName || f.facilityType}) — {f.villageTown}, {f.district}
                  </option>
                ))}
              </select>
            </div>

            {selectedFacility && (
              <div
                style={{
                  padding: '0.85rem 1rem',
                  backgroundColor: 'var(--bg-subtle)',
                  borderRadius: 'var(--radius-md)',
                  display: 'grid',
                  gridTemplateColumns: 'repeat(auto-fit, minmax(220px, 1fr))',
                  gap: '0.75rem',
                  fontSize: '0.88rem',
                  color: 'var(--text-main)',
                }}
              >
                <div style={{ display: 'flex', alignItems: 'center', gap: '0.4rem' }}>
                  <MapPin size={15} color="var(--primary)" style={{ flexShrink: 0 }} />
                  <span>
                    <strong>Location:</strong> {selectedFacility.addressLine1}, {selectedFacility.villageTown}, {selectedFacility.district} - {selectedFacility.pincode}
                  </span>
                </div>
                <div style={{ display: 'flex', alignItems: 'center', gap: '0.4rem' }}>
                  <Phone size={15} color="var(--primary)" style={{ flexShrink: 0 }} />
                  <span>
                    <strong>Facility Phone:</strong> {selectedFacility.contactNumber}
                  </span>
                </div>
              </div>
            )}
          </div>
        )}
      </div>

      {/* Overview KPI Metric Cards */}
      {facilitySosEvents.length > 0 && (
        <div
          style={{
            background: '#fef2f2',
            border: '2px solid #f87171',
            borderRadius: 'var(--radius-lg)',
            padding: '1rem 1.25rem',
          }}
        >
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.6rem', color: '#991b1b', fontWeight: 800, fontSize: '1.05rem', marginBottom: '0.5rem' }}>
            <AlertTriangle size={20} />
            <span>Active Incoming Emergency SOS Alerts ({facilitySosEvents.length})</span>
          </div>
          <div style={{ display: 'flex', flexDirection: 'column', gap: '0.5rem' }}>
            {facilitySosEvents.map((sos) => (
              <div
                key={sos.sosId}
                style={{
                  background: '#ffffff',
                  padding: '0.75rem 1rem',
                  borderRadius: 'var(--radius-md)',
                  border: '1px solid #fecaca',
                  display: 'flex',
                  justifyContent: 'space-between',
                  alignItems: 'center',
                  flexWrap: 'wrap',
                  gap: '0.5rem',
                }}
              >
                <div style={{ fontSize: '0.88rem' }}>
                  <strong>SOS #{sos.sosId.slice(0, 8).toUpperCase()}</strong> • Patient: {sos.patientId || 'Villager'} •{' '}
                  {sos.latitude != null && sos.longitude != null && (
                    <span>GPS: {sos.latitude.toFixed(4)}, {sos.longitude.toFixed(4)} • </span>
                  )}
                  <span>{sos.message || 'Emergency assistance requested'}</span>
                </div>
                <Button
                  variant="danger"
                  size="sm"
                  onClick={async () => {
                    await sosApi.resolveSos(sos.sosId);
                    if (selectedFacility) loadReferrals(selectedFacility.id, activeTab);
                  }}
                >
                  Mark SOS Resolved
                </Button>
              </div>
            ))}
          </div>
        </div>
      )}

      <div
        style={{
          display: 'grid',
          gridTemplateColumns: 'repeat(auto-fit, minmax(180px, 1fr))',
          gap: '1rem',
        }}
      >
        {/* Total Metric Card */}
        <div
          onClick={() => setActiveTab('ALL')}
          style={{
            padding: '1.1rem 1.25rem',
            backgroundColor: 'var(--bg-surface)',
            border: `2px solid ${activeTab === 'ALL' ? 'var(--primary)' : 'var(--border-light)'}`,
            borderRadius: 'var(--radius-lg)',
            cursor: 'pointer',
            transition: 'all 0.2s ease',
            boxShadow: 'var(--shadow-sm)',
          }}
        >
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '0.4rem' }}>
            <span style={{ fontSize: '0.82rem', fontWeight: 700, color: 'var(--text-subtle)', textTransform: 'uppercase' }}>
              Total Referrals
            </span>
            <Layers size={18} color="var(--primary)" />
          </div>
          <div style={{ fontSize: '1.75rem', fontWeight: 800, color: 'var(--text-main)' }}>
            {totalCount}
          </div>
          <div style={{ fontSize: '0.78rem', color: 'var(--text-muted)', marginTop: '0.2rem' }}>
            All patient requests
          </div>
        </div>

        {/* Pending Review Card */}
        <div
          onClick={() => setActiveTab('PENDING')}
          style={{
            padding: '1.1rem 1.25rem',
            backgroundColor: activeTab === 'PENDING' ? '#fef3c7' : 'var(--bg-surface)',
            border: `2px solid ${activeTab === 'PENDING' ? '#d97706' : pendingCount > 0 ? '#fde68a' : 'var(--border-light)'}`,
            borderRadius: 'var(--radius-lg)',
            cursor: 'pointer',
            transition: 'all 0.2s ease',
            boxShadow: 'var(--shadow-sm)',
          }}
        >
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '0.4rem' }}>
            <span style={{ fontSize: '0.82rem', fontWeight: 700, color: '#92400e', textTransform: 'uppercase' }}>
              Pending Review
            </span>
            <Clock size={18} color="#d97706" />
          </div>
          <div style={{ fontSize: '1.75rem', fontWeight: 800, color: pendingCount > 0 ? '#b45309' : 'var(--text-main)' }}>
            {pendingCount}
          </div>
          <div style={{ fontSize: '0.78rem', color: '#92400e', marginTop: '0.2rem', fontWeight: 600 }}>
            {pendingCount > 0 ? '⚠ Requires Staff Action' : 'All caught up'}
          </div>
        </div>

        {/* Accepted Care Card */}
        <div
          onClick={() => setActiveTab('ACCEPTED')}
          style={{
            padding: '1.1rem 1.25rem',
            backgroundColor: activeTab === 'ACCEPTED' ? '#ecfdf5' : 'var(--bg-surface)',
            border: `2px solid ${activeTab === 'ACCEPTED' ? '#059669' : 'var(--border-light)'}`,
            borderRadius: 'var(--radius-lg)',
            cursor: 'pointer',
            transition: 'all 0.2s ease',
            boxShadow: 'var(--shadow-sm)',
          }}
        >
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '0.4rem' }}>
            <span style={{ fontSize: '0.82rem', fontWeight: 700, color: '#065f46', textTransform: 'uppercase' }}>
              Accepted Care
            </span>
            <CheckCircle2 size={18} color="#059669" />
          </div>
          <div style={{ fontSize: '1.75rem', fontWeight: 800, color: '#047857' }}>
            {acceptedCount}
          </div>
          <div style={{ fontSize: '0.78rem', color: '#065f46', marginTop: '0.2rem' }}>
            Awaiting arrival / admit
          </div>
        </div>

        {/* Active In-Progress Card */}
        <div
          onClick={() => setActiveTab('IN_PROGRESS')}
          style={{
            padding: '1.1rem 1.25rem',
            backgroundColor: activeTab === 'IN_PROGRESS' ? '#f0fdfa' : 'var(--bg-surface)',
            border: `2px solid ${activeTab === 'IN_PROGRESS' ? 'var(--primary)' : 'var(--border-light)'}`,
            borderRadius: 'var(--radius-lg)',
            cursor: 'pointer',
            transition: 'all 0.2s ease',
            boxShadow: 'var(--shadow-sm)',
          }}
        >
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '0.4rem' }}>
            <span style={{ fontSize: '0.82rem', fontWeight: 700, color: '#115e59', textTransform: 'uppercase' }}>
              In Clinical Care
            </span>
            <Play size={18} color="var(--primary)" />
          </div>
          <div style={{ fontSize: '1.75rem', fontWeight: 800, color: 'var(--primary)' }}>
            {inProgressCount}
          </div>
          <div style={{ fontSize: '0.78rem', color: '#115e59', marginTop: '0.2rem' }}>
            Active triage & treatment
          </div>
        </div>

        {/* Completed Cases Card */}
        <div
          onClick={() => setActiveTab('COMPLETED')}
          style={{
            padding: '1.1rem 1.25rem',
            backgroundColor: activeTab === 'COMPLETED' ? '#f0fdf4' : 'var(--bg-surface)',
            border: `2px solid ${activeTab === 'COMPLETED' ? '#16a34a' : 'var(--border-light)'}`,
            borderRadius: 'var(--radius-lg)',
            cursor: 'pointer',
            transition: 'all 0.2s ease',
            boxShadow: 'var(--shadow-sm)',
          }}
        >
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '0.4rem' }}>
            <span style={{ fontSize: '0.82rem', fontWeight: 700, color: '#166534', textTransform: 'uppercase' }}>
              Completed
            </span>
            <CheckCheck size={18} color="#16a34a" />
          </div>
          <div style={{ fontSize: '1.75rem', fontWeight: 800, color: '#15803d' }}>
            {completedCount}
          </div>
          <div style={{ fontSize: '0.78rem', color: '#166534', marginTop: '0.2rem' }}>
            Triage & care resolved
          </div>
        </div>
      </div>

      {/* Referrals Queue & Management Section */}
      <div className="card" style={{ padding: '1.5rem', marginBottom: 0 }}>
        {/* Status Filter Tabs */}
        <div
          style={{
            display: 'flex',
            gap: '0.6rem',
            borderBottom: '2px solid var(--border-light)',
            paddingBottom: '0.85rem',
            marginBottom: '1.5rem',
            overflowX: 'auto',
          }}
        >
          {[
            { id: 'ALL', label: 'All Requests', count: totalCount },
            { id: 'PENDING', label: 'Pending Review (प्रलंबित)', count: pendingCount },
            { id: 'ACCEPTED', label: 'Accepted (स्वीकृत)', count: acceptedCount },
            { id: 'IN_PROGRESS', label: 'In Progress (उपचार सुरू)', count: inProgressCount },
            { id: 'COMPLETED', label: 'Completed (पूर्ण)', count: completedCount },
            { id: 'REJECTED', label: 'Rejected (नकार)', count: rejectedCount },
          ].map((tab) => {
            const isActive = activeTab === tab.id;
            return (
              <button
                key={tab.id}
                onClick={() => setActiveTab(tab.id as any)}
                style={{
                  padding: '0.6rem 1.1rem',
                  borderRadius: 'var(--radius-md)',
                  border: 'none',
                  backgroundColor: isActive ? 'var(--primary)' : 'var(--bg-subtle)',
                  color: isActive ? '#ffffff' : 'var(--text-main)',
                  fontWeight: 700,
                  fontSize: '0.9rem',
                  cursor: 'pointer',
                  display: 'inline-flex',
                  alignItems: 'center',
                  gap: '0.5rem',
                  whiteSpace: 'nowrap',
                  transition: 'all 0.15s ease',
                }}
              >
                <span>{tab.label}</span>
                <span
                  style={{
                    padding: '2px 7px',
                    borderRadius: 'var(--radius-full)',
                    backgroundColor: isActive
                      ? '#ffffff'
                      : tab.id === 'PENDING' && tab.count > 0
                      ? '#fed7aa'
                      : 'var(--border-light)',
                    color: isActive
                      ? 'var(--primary-dark)'
                      : tab.id === 'PENDING' && tab.count > 0
                      ? '#9a3412'
                      : 'var(--text-subtle)',
                    fontSize: '0.75rem',
                    fontWeight: 800,
                  }}
                >
                  {tab.count}
                </span>
              </button>
            );
          })}
        </div>

        {/* Referrals List Body */}
        {isLoadingReferrals ? (
          <LoadingSpinner message="Fetching referrals queue for selected facility..." />
        ) : referrals.length === 0 ? (
          <EmptyState
            title="No Referrals in this Category"
            description={`No referral requests matching '${activeTab.replace(/_/g, ' ')}' were found for ${selectedFacility?.name || 'this facility'}.`}
          />
        ) : (
          <div style={{ display: 'flex', flexDirection: 'column', gap: '1.25rem' }}>
            {referrals.map((ref) => {
              const isPending = ref.status === 'PENDING';
              const isAccepted = ref.status === 'ACCEPTED';
              const isInProgress = ref.status === 'IN_PROGRESS';
              const isCompleted = ref.status === 'COMPLETED';
              const isRejected = ref.status === 'REJECTED';

              const borderAccentColor = isPending
                ? '#d97706'
                : isAccepted
                ? '#059669'
                : isInProgress
                ? 'var(--primary)'
                : isCompleted
                ? '#16a34a'
                : '#dc2626';

              const refDisplayToken = ref.referralToken || `REF-${ref.id.substring(0, 8).toUpperCase()}`;

              return (
                <div
                  key={ref.id}
                  style={{
                    padding: '1.4rem 1.5rem',
                    backgroundColor: 'var(--bg-surface)',
                    border: '1.5px solid var(--border-light)',
                    borderLeft: `6px solid ${borderAccentColor}`,
                    borderRadius: 'var(--radius-lg)',
                    boxShadow: 'var(--shadow-sm)',
                    display: 'flex',
                    flexDirection: 'column',
                    gap: '1rem',
                    transition: 'box-shadow 0.2s ease',
                  }}
                >
                  {/* Card Header */}
                  <div
                    style={{
                      display: 'flex',
                      justifyContent: 'space-between',
                      alignItems: 'flex-start',
                      flexWrap: 'wrap',
                      gap: '0.75rem',
                    }}
                  >
                    <div>
                      <div style={{ display: 'flex', alignItems: 'center', gap: '0.6rem', marginBottom: '0.3rem' }}>
                        <h3 style={{ fontSize: '1.18rem', fontWeight: 800, color: 'var(--text-main)', margin: 0 }}>
                          Referral #{refDisplayToken}
                        </h3>
                        {isPending && (
                          <span
                            style={{
                              padding: '2px 8px',
                              borderRadius: 'var(--radius-full)',
                              backgroundColor: '#fef3c7',
                              border: '1px solid #fde68a',
                              color: '#b45309',
                              fontSize: '0.75rem',
                              fontWeight: 800,
                              textTransform: 'uppercase',
                            }}
                          >
                            Action Required
                          </span>
                        )}
                      </div>

                      <div style={{ fontSize: '0.84rem', color: 'var(--text-muted)', display: 'flex', alignItems: 'center', gap: '0.4rem' }}>
                        <Calendar size={14} color="var(--primary)" />
                        <span>
                          Requested: {ref.requestedAt ? new Date(ref.requestedAt).toLocaleString() : 'Recently'}
                        </span>
                      </div>
                    </div>

                    {/* Status & Priority Badges */}
                    <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', flexWrap: 'wrap' }}>
                      {ref.priority && (
                        <StatusBadge status={ref.priority} labelPrefix="Priority" />
                      )}
                      <StatusBadge status={ref.status} pulse={isPending || isInProgress} />
                    </div>
                  </div>

                  {/* Interactive 4-Stage Referral & Care Preparation Timeline */}
                  <ReferralTimeline
                    status={ref.status}
                    requestedAt={ref.requestedAt}
                    respondedAt={ref.respondedAt}
                    acceptedAt={ref.acceptedAt}
                    arrivedAt={ref.arrivedAt}
                    completedAt={ref.completedAt}
                    compact
                  />

                  {/* Patient Referral Reason Note */}
                  {ref.patientMessage && (
                    <div
                      style={{
                        padding: '0.85rem 1rem',
                        backgroundColor: 'var(--bg-subtle)',
                        borderRadius: 'var(--radius-md)',
                        border: '1px solid var(--border-light)',
                        display: 'flex',
                        alignItems: 'flex-start',
                        gap: '0.6rem',
                      }}
                    >
                      <MessageSquare size={16} color="var(--primary)" style={{ flexShrink: 0, marginTop: '3px' }} />
                      <div>
                        <span style={{ fontSize: '0.78rem', fontWeight: 800, color: 'var(--text-subtle)', textTransform: 'uppercase', display: 'block', marginBottom: '0.15rem' }}>
                          Patient Referral Reason & Intake Message
                        </span>
                        <p style={{ fontSize: '0.92rem', color: 'var(--text-main)', margin: 0, lineHeight: 1.5 }}>
                          "{ref.patientMessage}"
                        </p>
                      </div>
                    </div>
                  )}

                  {/* Facility Clinical Response Note */}
                  {ref.facilityResponse && (
                    <div
                      style={{
                        padding: '0.85rem 1rem',
                        backgroundColor: isRejected ? '#fff1f2' : '#f0fdf4',
                        border: `1px solid ${isRejected ? '#fecdd3' : '#bbf7d0'}`,
                        borderRadius: 'var(--radius-md)',
                        display: 'flex',
                        alignItems: 'flex-start',
                        gap: '0.6rem',
                      }}
                    >
                      {isRejected ? (
                        <AlertTriangle size={16} color="#e11d48" style={{ flexShrink: 0, marginTop: '3px' }} />
                      ) : (
                        <CheckCircle2 size={16} color="#16a34a" style={{ flexShrink: 0, marginTop: '3px' }} />
                      )}
                      <div>
                        <span
                          style={{
                            fontSize: '0.78rem',
                            fontWeight: 800,
                            color: isRejected ? '#9f1239' : '#166534',
                            textTransform: 'uppercase',
                            display: 'block',
                            marginBottom: '0.15rem',
                          }}
                        >
                          Recorded Facility Response
                        </span>
                        <p style={{ fontSize: '0.9rem', color: 'var(--text-main)', margin: 0, lineHeight: 1.5 }}>
                          "{ref.facilityResponse}"
                        </p>
                      </div>
                    </div>
                  )}

                  {/* Action Bar */}
                  <div
                    style={{
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'space-between',
                      paddingTop: '0.85rem',
                      borderTop: '1px solid var(--border-light)',
                      flexWrap: 'wrap',
                      gap: '0.75rem',
                    }}
                  >
                    {/* View Clinical Dossier Button */}
                    <Button
                      size="sm"
                      variant="outline"
                      icon={<Eye size={15} />}
                      onClick={() => handleOpenDetail(ref.id)}
                      isLoading={isLoadingDetail}
                    >
                      Review Clinical Dossier & AI (तपशील पाहा)
                    </Button>

                    {/* Status Action Buttons with adequate spacing & clear semantics */}
                    <div style={{ display: 'flex', gap: '0.75rem', flexWrap: 'wrap', alignItems: 'center' }}>
                      {isPending && (
                        <>
                          <Button
                            size="sm"
                            variant="danger"
                            icon={<XCircle size={15} />}
                            onClick={() => handleOpenAction(ref, 'REJECTED')}
                          >
                            Reject (नकार)
                          </Button>
                          <Button
                            size="sm"
                            variant="success"
                            icon={<CheckCircle2 size={15} />}
                            onClick={() => handleOpenAction(ref, 'ACCEPTED')}
                          >
                            Accept Referral (स्वीकारा)
                          </Button>
                        </>
                      )}

                      {isAccepted && (
                        <Button
                          size="sm"
                          variant="primary"
                          icon={<Play size={15} />}
                          onClick={() => handleOpenAction(ref, 'IN_PROGRESS')}
                        >
                          Start Clinical Care (उपचार सुरू)
                        </Button>
                      )}

                      {isInProgress && (
                        <Button
                          size="sm"
                          variant="success"
                          icon={<CheckCheck size={15} />}
                          onClick={() => handleOpenAction(ref, 'COMPLETED')}
                        >
                          Mark Completed (पूर्ण झाले)
                        </Button>
                      )}

                      {isCompleted && (
                        <span
                          style={{
                            display: 'inline-flex',
                            alignItems: 'center',
                            gap: '0.35rem',
                            color: '#15803d',
                            fontSize: '0.85rem',
                            fontWeight: 700,
                          }}
                        >
                          <CheckCheck size={16} />
                          Case Resolved
                        </span>
                      )}

                      {isRejected && (
                        <span
                          style={{
                            display: 'inline-flex',
                            alignItems: 'center',
                            gap: '0.35rem',
                            color: '#b91c1c',
                            fontSize: '0.85rem',
                            fontWeight: 700,
                          }}
                        >
                          <XCircle size={16} />
                          Referral Closed
                        </span>
                      )}
                    </div>
                  </div>
                </div>
              );
            })}
          </div>
        )}
      </div>

      {/* Clinical Review Detail Modal */}
      <ReferralDetailModal
        isOpen={isReviewOpen}
        onClose={() => setIsReviewOpen(false)}
        detail={reviewDetail}
      />

      {/* Status Action Transition Confirmation Modal */}
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
