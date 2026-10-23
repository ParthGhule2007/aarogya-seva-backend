import React, { useState, useEffect } from 'react';
import { useAuth } from '../../context/AuthContext';
import { useLanguage } from '../../context/LanguageContext';
import { referralApi } from '../../api/referralApi';
import { facilityApi } from '../../api/facilityApi';
import { ReferralResponseDto } from '../../types/referral';
import { HealthcareFacilityDto } from '../../types/facility';
import {
  Building2,
  FileCheck,
  CheckCircle,
  Clock,
  Search,
  AlertTriangle,
  Users,
  Activity,
  Bed,
  Phone,
  MapPin,
  RefreshCw,
} from 'lucide-react';

export const FacilityAdminWorkspace: React.FC = () => {
  const { user } = useAuth();
  const { t } = useLanguage();

  const [activeTab, setActiveTab] = useState<'referrals' | 'capacity' | 'directory'>('referrals');
  const [referrals, setReferrals] = useState<ReferralResponseDto[]>([]);
  const [facilities, setFacilities] = useState<HealthcareFacilityDto[]>([]);
  const [searchToken, setSearchToken] = useState('');
  const [isLoading, setIsLoading] = useState(false);

  // Facility ID fallback (e.g. Primary Health Centre 1)
  const facilityId = user?.facilityId || 'f1000000-0000-0000-0000-000000000001';

  const loadReferrals = async () => {
    setIsLoading(true);
    try {
      const data = await referralApi.getReferralsByFacilityId(facilityId);
      setReferrals(data || []);
    } catch {
      // fallback
    } finally {
      setIsLoading(false);
    }
  };

  const loadDirectory = async () => {
    try {
      const data = await facilityApi.searchNearbyFacilities({
        latitude: 18.5204,
        longitude: 73.8567,
        radiusKm: 50,
      });
      setFacilities(data || []);
    } catch {
      // fallback
    }
  };

  useEffect(() => {
    loadReferrals();
    loadDirectory();
  }, []);

  const handleUpdateStatus = async (referralId: string, newStatus: any) => {
    try {
      const updated = await referralApi.updateReferralStatus(referralId, { status: newStatus });
      setReferrals((prev) => prev.map((r) => (r.id === referralId ? updated : r)));
    } catch {
      // fallback
    }
  };

  const filteredReferrals = referrals.filter(
    (r) =>
      r.referralToken?.toLowerCase().includes(searchToken.toLowerCase()) ||
      r.id.toLowerCase().includes(searchToken.toLowerCase())
  );

  return (
    <div style={{ maxWidth: '1100px', margin: '0 auto' }}>
      {/* Header */}
      <div
        className="card"
        style={{
          background: 'linear-gradient(135deg, #0d9488 0%, #1e293b 100%)',
          color: '#ffffff',
          padding: '1.5rem',
        }}
      >
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: '1rem' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
            <Building2 size={32} color="#5eead4" />
            <div>
              <h2 style={{ fontSize: '1.4rem', fontWeight: 800 }}>
                {t('facOpsTitle')}
              </h2>
              <div style={{ fontSize: '0.85rem', opacity: 0.9 }}>
                Facility Code: PHC-PUNE-01 • Primary Health Centre & Emergency Station
              </div>
            </div>
          </div>

          <div style={{ display: 'flex', gap: '0.5rem' }}>
            <button
              className="btn btn-outline"
              style={{ minHeight: '38px', borderColor: 'rgba(255,255,255,0.4)', color: '#ffffff' }}
              onClick={loadReferrals}
            >
              <RefreshCw size={14} />
              <span>Refresh Inbound</span>
            </button>
          </div>
        </div>
      </div>

      {/* Metric Cards */}
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))', gap: '1rem', marginBottom: '1.5rem' }}>
        <div className="card" style={{ marginBottom: 0 }}>
          <div style={{ fontSize: '0.85rem', color: 'var(--text-subtle)', fontWeight: 600 }}>Inbound Referrals</div>
          <div style={{ fontSize: '1.8rem', fontWeight: 900, color: 'var(--primary)', margin: '0.25rem 0' }}>
            {referrals.length}
          </div>
          <div style={{ fontSize: '0.8rem', color: 'var(--text-muted)' }}>Tokens pending or admitted</div>
        </div>

        <div className="card" style={{ marginBottom: 0 }}>
          <div style={{ fontSize: '0.85rem', color: 'var(--text-subtle)', fontWeight: 600 }}>Emergency Status</div>
          <div style={{ fontSize: '1.8rem', fontWeight: 900, color: '#15803d', margin: '0.25rem 0' }}>
            READY
          </div>
          <div style={{ fontSize: '0.8rem', color: 'var(--text-muted)' }}>24x7 Ambulance & Triage active</div>
        </div>

        <div className="card" style={{ marginBottom: 0 }}>
          <div style={{ fontSize: '0.85rem', color: 'var(--text-subtle)', fontWeight: 600 }}>Network Facilities</div>
          <div style={{ fontSize: '1.8rem', fontWeight: 900, color: 'var(--secondary)', margin: '0.25rem 0' }}>
            {facilities.length}
          </div>
          <div style={{ fontSize: '0.8rem', color: 'var(--text-muted)' }}>PostGIS verified health centers</div>
        </div>
      </div>

      {/* Tabs */}
      <div className="tabs-nav">
        <button
          className={`tab-btn ${activeTab === 'referrals' ? 'active' : ''}`}
          onClick={() => setActiveTab('referrals')}
        >
          <FileCheck size={18} />
          <span>{t('facReferrals')}</span>
        </button>

        <button
          className={`tab-btn ${activeTab === 'directory' ? 'active' : ''}`}
          onClick={() => setActiveTab('directory')}
        >
          <Building2 size={18} />
          <span>PostGIS Facility Directory</span>
        </button>
      </div>

      {/* TAB 1: INBOUND REFERRALS */}
      {activeTab === 'referrals' && (
        <div>
          {/* Token Search Bar */}
          <div style={{ marginBottom: '1.25rem', position: 'relative', maxWidth: '420px' }}>
            <input
              type="text"
              className="form-control"
              style={{ paddingLeft: '2.5rem' }}
              placeholder="Search by Referral Token or Case ID..."
              value={searchToken}
              onChange={(e) => setSearchToken(e.target.value)}
            />
            <Search
              size={18}
              style={{ position: 'absolute', left: '0.75rem', top: '50%', transform: 'translateY(-50%)', color: 'var(--text-subtle)' }}
            />
          </div>

          {filteredReferrals.length === 0 ? (
            <div className="card" style={{ textAlign: 'center', padding: '3rem 1rem' }}>
              <FileCheck size={36} color="var(--primary)" style={{ margin: '0 auto 0.5rem' }} />
              <div style={{ fontWeight: 700 }}>No Inbound Referrals</div>
              <div style={{ fontSize: '0.85rem', color: 'var(--text-subtle)', marginTop: '0.25rem' }}>
                Referral tokens issued by patients or ASHA workers will appear here for verification.
              </div>
            </div>
          ) : (
            <div style={{ display: 'flex', flexDirection: 'column', gap: '0.75rem' }}>
              {filteredReferrals.map((ref) => (
                <div key={ref.id} className="card" style={{ marginBottom: 0 }}>
                  <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: '0.5rem' }}>
                    <div>
                      <div style={{ fontWeight: 800, fontSize: '1.05rem', color: 'var(--primary)' }}>
                        Token: {ref.referralToken || `REF-${ref.id.substring(0, 8).toUpperCase()}`}
                      </div>
                      <div style={{ fontSize: '0.85rem', color: 'var(--text-subtle)' }}>
                        Patient ID: {ref.patientId} • Urgency: <span className="badge badge-urgent">{ref.urgencyLevel}</span>
                      </div>
                    </div>

                    <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                      <span className="badge badge-routine">{ref.status}</span>
                      {ref.status === 'PENDING' && (
                        <button
                          className="btn btn-primary"
                          style={{ minHeight: '38px', fontSize: '0.8rem' }}
                          onClick={() => handleUpdateStatus(ref.id, 'CONFIRMED')}
                        >
                          <CheckCircle size={14} />
                          <span>{t('facConfirmReferral')}</span>
                        </button>
                      )}
                      {ref.status === 'CONFIRMED' && (
                        <button
                          className="btn btn-secondary"
                          style={{ minHeight: '38px', fontSize: '0.8rem' }}
                          onClick={() => handleUpdateStatus(ref.id, 'COMPLETED')}
                        >
                          <CheckCircle size={14} />
                          <span>{t('facCompleteReferral')}</span>
                        </button>
                      )}
                    </div>
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>
      )}

      {/* TAB 2: POSTGIS DIRECTORY */}
      {activeTab === 'directory' && (
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(320px, 1fr))', gap: '1rem' }}>
          {facilities.map((fac) => (
            <div key={fac.id} className="card" style={{ marginBottom: 0 }}>
              <div style={{ fontWeight: 800, fontSize: '1.05rem' }}>{fac.facilityName}</div>
              <div style={{ fontSize: '0.85rem', color: 'var(--primary)', fontWeight: 600, marginBottom: '0.5rem' }}>
                {fac.facilityType?.replace(/_/g, ' ')}
              </div>
              <div style={{ fontSize: '0.85rem', color: 'var(--text-muted)' }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: '0.4rem', marginBottom: '0.25rem' }}>
                  <MapPin size={14} color="var(--primary)" />
                  <span>{fac.address?.village || fac.address?.city}, {fac.address?.district}</span>
                </div>
                <div style={{ display: 'flex', alignItems: 'center', gap: '0.4rem' }}>
                  <Phone size={14} color="var(--primary)" />
                  <span>{fac.contactNumber || 'Contact Desk'}</span>
                </div>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
};
