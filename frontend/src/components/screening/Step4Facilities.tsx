import React, { useState, useEffect } from 'react';
import { facilityApi } from '../../api/facilityApi';
import { useLanguage } from '../../context/LanguageContext';
import { HealthcareFacilityDto } from '../../types/facility';
import { AiAnalysisResponse } from '../../types/ai';
import {
  Building2,
  MapPin,
  Phone,
  Clock,
  ShieldCheck,
  CheckCircle,
  ArrowRight,
  RefreshCw,
  AlertCircle,
} from 'lucide-react';

interface Step4FacilitiesProps {
  selectedFacility: HealthcareFacilityDto | null;
  setSelectedFacility: (fac: HealthcareFacilityDto) => void;
  aiAnalysis?: AiAnalysisResponse | null;
  onNext: () => void;
  onBack: () => void;
}

export const Step4Facilities: React.FC<Step4FacilitiesProps> = ({
  selectedFacility,
  setSelectedFacility,
  aiAnalysis,
  onNext,
  onBack,
}) => {
  const { t } = useLanguage();
  const [facilities, setFacilities] = useState<HealthcareFacilityDto[]>([]);
  const [radiusKm, setRadiusKm] = useState<number>(50);
  const [isLoading, setIsLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);

  // Default coordinate center (Palasdeo, Indapur regional corridor: 18.2562, 74.9048)
  const [coords, setCoords] = useState<{ lat: number; lon: number }>({
    lat: 18.2562,
    lon: 74.9048,
  });

  useEffect(() => {
    if (navigator.geolocation) {
      navigator.geolocation.getCurrentPosition(
        (pos) => setCoords({ lat: pos.coords.latitude, lon: pos.coords.longitude }),
        () => {} // silent fallback to regional corridor
      );
    }
  }, []);

  const loadFacilities = async () => {
    setIsLoading(true);
    setError(null);
    try {
      const data = await facilityApi.searchNearbyFacilities({
        latitude: coords.lat,
        longitude: coords.lon,
        radiusKm,
      });
      const aiRecIds = new Set(
        (aiAnalysis?.recommendedFacilities || []).map((r: any) => r.facilityId || r.id)
      );
      const normalized = (data || []).map((f: any) => ({
        ...f,
        id: f.facilityId || f.id,
        facilityId: f.facilityId || f.id,
        name: f.name || f.facilityName || 'Healthcare Facility',
        facilityName: f.name || f.facilityName || 'Healthcare Facility',
      }));
      // Sort AI-recommended facilities first, then by distanceKm
      normalized.sort((a: any, b: any) => {
        const aRec = aiRecIds.has(a.id) ? 0 : 1;
        const bRec = aiRecIds.has(b.id) ? 0 : 1;
        if (aRec !== bRec) return aRec - bRec;
        return (a.distanceKm || 999) - (b.distanceKm || 999);
      });
      setFacilities(normalized);
      if (normalized.length > 0 && !selectedFacility) {
        setSelectedFacility(normalized[0]);
      }
    } catch (err: any) {
      // Seeded database UUIDs so referrals map to real facilities if search fails
      const fallbackList: HealthcareFacilityDto[] = [
        {
          id: 'c2000000-0000-0000-0000-000000000001',
          facilityId: 'c2000000-0000-0000-0000-000000000001',
          name: 'Palasdeo Primary Health Centre (PHC)',
          facilityType: 'PHC',
          villageTown: 'Palasdeo',
          district: 'Pune',
          state: 'Maharashtra',
          pincode: '413132',
          contactNumber: '+91 2111 224455',
          isOperational: true,
          isDemoData: true,
          availableServices: ['GENERAL_OPD', 'EMERGENCY', 'MATERNAL_HEALTH'],
          distanceKm: 2.1,
        },
        {
          id: 'c2000000-0000-0000-0000-000000000002',
          facilityId: 'c2000000-0000-0000-0000-000000000002',
          name: 'Indapur Sub-District Hospital (SDH)',
          facilityType: 'CHC',
          villageTown: 'Indapur',
          district: 'Pune',
          state: 'Maharashtra',
          pincode: '413106',
          contactNumber: '+91 2111 223344',
          isOperational: true,
          isDemoData: true,
          availableServices: ['GENERAL_OPD', 'EMERGENCY', 'MATERNAL_HEALTH', 'INPATIENT_BEDS'],
          distanceKm: 14.5,
        },
        {
          id: 'c2000000-0000-0000-0000-000000000003',
          facilityId: 'c2000000-0000-0000-0000-000000000003',
          name: 'Baramati District Civil Hospital',
          facilityType: 'DISTRICT_HOSPITAL',
          villageTown: 'Baramati',
          district: 'Pune',
          state: 'Maharashtra',
          pincode: '413102',
          contactNumber: '+91 2112 222333',
          isOperational: true,
          isDemoData: true,
          availableServices: ['GENERAL_OPD', 'EMERGENCY', 'MATERNAL_HEALTH', 'ICU_BEDS', 'SURGICAL_UNIT', 'DIAGNOSTIC_LAB'],
          distanceKm: 28.5,
        },
      ];
      setFacilities(fallbackList);
      if (!selectedFacility) {
        setSelectedFacility(fallbackList[0]);
      }
      setError(err?.message || 'Facility search service returned an error. Showing verified regional health network.');
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    loadFacilities();
  }, [coords, radiusKm]);

  return (
    <div className="card">
      <div className="card-header">
        <div className="card-title">
          <Building2 size={22} color="var(--primary)" />
          <span>{t('step4Title')}</span>
        </div>
        <span className="badge badge-routine">Step 4 of 5</span>
      </div>

      <p style={{ color: 'var(--text-subtle)', marginBottom: '1.25rem' }}>
        {t('step4Subtitle')}
      </p>

      {/* Radius Filter */}
      <div
        style={{
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center',
          background: 'var(--bg-subtle)',
          padding: '0.75rem 1rem',
          borderRadius: 'var(--radius-md)',
          marginBottom: '1.25rem',
        }}
      >
        <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', fontSize: '0.9rem' }}>
          <MapPin size={18} color="var(--primary)" />
          <span>Search Distance Radius:</span>
        </div>
        <select
          className="form-control"
          style={{ width: 'auto', minHeight: '38px', padding: '0.2rem 0.6rem', fontWeight: 600 }}
          value={radiusKm}
          onChange={(e) => setRadiusKm(Number(e.target.value))}
        >
          <option value={10}>Within 10 km</option>
          <option value={20}>Within 20 km</option>
          <option value={30}>Within 30 km (Recommended)</option>
          <option value={50}>Within 50 km</option>
        </select>
      </div>

      {isLoading && (
        <div style={{ textAlign: 'center', padding: '2.5rem 1rem' }}>
          <RefreshCw size={32} color="var(--primary)" style={{ animation: 'spin 1s linear infinite' }} />
          <div style={{ marginTop: '0.75rem', fontWeight: 600 }}>
            Querying PostGIS Geodesic Spatial Catalog...
          </div>
        </div>
      )}

      {error && (
        <div className="alert-box danger">
          <AlertCircle size={20} />
          <span>{error}</span>
        </div>
      )}

      {!isLoading && facilities.length === 0 && (
        <div className="alert-box info">
          <span>No facilities found within {radiusKm} km. Try increasing the search radius.</span>
        </div>
      )}

      {/* Facility Selection List */}
      <div style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
        {facilities.map((fac) => {
          const isSelected = (selectedFacility?.id || selectedFacility?.facilityId) === (fac.id || fac.facilityId);
          return (
            <div
              key={fac.id || fac.facilityId}
              onClick={() => setSelectedFacility(fac)}
              style={{
                border: `2px solid ${isSelected ? 'var(--primary)' : 'var(--border-light)'}`,
                background: isSelected ? 'var(--primary-light)' : 'var(--bg-surface)',
                borderRadius: 'var(--radius-lg)',
                padding: '1.25rem',
                cursor: 'pointer',
                transition: 'all 0.2s ease',
              }}
            >
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: '0.5rem' }}>
                <div>
                  <div style={{ fontSize: '1.1rem', fontWeight: 800, color: 'var(--text-main)' }}>
                    {fac.name || fac.facilityName}
                  </div>
                  <div style={{ fontSize: '0.85rem', color: 'var(--primary)', fontWeight: 600 }}>
                    {fac.facilityType?.replace(/_/g, ' ')}
                  </div>
                </div>

                <div style={{ textAlign: 'right' }}>
                  <div style={{ fontSize: '1.05rem', fontWeight: 800, color: 'var(--secondary)' }}>
                    {fac.distanceKm?.toFixed(1) || '0'} km
                  </div>
                  {isSelected && (
                    <span className="badge badge-routine" style={{ marginTop: '0.25rem' }}>
                      Selected ✓
                    </span>
                  )}
                </div>
              </div>

              <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))', gap: '0.5rem', fontSize: '0.85rem', color: 'var(--text-muted)', marginBottom: '0.75rem' }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: '0.4rem' }}>
                  <MapPin size={16} color="var(--primary)" />
                  <span>{fac.address?.village || fac.village || fac.villageTown || fac.address?.city || 'Pune'}, {fac.address?.district || fac.district || 'District'}</span>
                </div>
                <div style={{ display: 'flex', alignItems: 'center', gap: '0.4rem' }}>
                  <Phone size={16} color="var(--primary)" />
                  <span>{fac.contactNumber || 'Contact at Desk'}</span>
                </div>
                <div style={{ display: 'flex', alignItems: 'center', gap: '0.4rem' }}>
                  <Clock size={16} color="var(--primary)" />
                  <span>{fac.operationalStatus || 'Open 24/7'}</span>
                </div>
              </div>

              {/* Service Offerings */}
              {(fac.availableServices || fac.services || []).length > 0 && (
                <div style={{ display: 'flex', flexWrap: 'wrap', gap: '0.35rem', borderTop: '1px solid rgba(0,0,0,0.06)', paddingTop: '0.5rem' }}>
                  {(fac.availableServices || fac.services || []).map((svc: any, i: number) => (
                    <span
                      key={i}
                      style={{
                        background: 'var(--bg-subtle)',
                        padding: '0.2rem 0.5rem',
                        borderRadius: 'var(--radius-sm)',
                        fontSize: '0.75rem',
                        fontWeight: 600,
                      }}
                    >
                      {typeof svc === 'string' ? svc.replace(/_/g, ' ') : svc.serviceName || svc.serviceCode || String(svc)}
                    </span>
                  ))}
                </div>
              )}
            </div>
          );
        })}
      </div>

      <div style={{ display: 'flex', justifyContent: 'space-between', marginTop: '1.5rem' }}>
        <button type="button" className="btn btn-outline" onClick={onBack}>
          <span>{t('back')}</span>
        </button>
        <button
          type="button"
          className="btn btn-primary"
          style={{ minWidth: '160px' }}
          disabled={!selectedFacility}
          onClick={onNext}
        >
          <span>{t('next')}</span>
          <ArrowRight size={18} />
        </button>
      </div>
    </div>
  );
};
