import React, { useState, useEffect } from 'react';
import { facilityApi } from '../api/facilityApi';
import { FacilityDto, FacilityType } from '../types/facility';
import { Badge } from '../components/common/Badge';
import { Button } from '../components/common/Button';
import { LoadingSpinner } from '../components/common/LoadingSpinner';
import { AlertBanner } from '../components/common/AlertBanner';
import { EmptyState } from '../components/common/EmptyState';
import { Building2, Search, MapPin, Phone, CheckCircle2, XCircle, Navigation } from 'lucide-react';

export const FacilitiesDirectoryPage: React.FC = () => {
  const [facilities, setFacilities] = useState<FacilityDto[]>([]);
  const [searchQuery, setSearchQuery] = useState('');
  const [selectedType, setSelectedType] = useState<string>('ALL');
  const [onlyOperational, setOnlyOperational] = useState(false);

  // Proximity testing
  const [customLat, setCustomLat] = useState<number>(18.5204);
  const [customLon, setCustomLon] = useState<number>(73.8567);
  const [radiusKm, setRadiusKm] = useState<number>(50);
  const [isSearchingNearby, setIsSearchingNearby] = useState(false);
  const [nearbyResults, setNearbyResults] = useState<any[] | null>(null);

  const [isLoading, setIsLoading] = useState(false);
  const [banner, setBanner] = useState<{ type: 'info' | 'success' | 'warning' | 'danger'; message: string } | null>(null);

  useEffect(() => {
    loadFacilities();
  }, []);

  const loadFacilities = async () => {
    setIsLoading(true);
    try {
      const data = await facilityApi.getAllFacilities();
      setFacilities(data);
    } catch (err: any) {
      setBanner({ type: 'danger', message: `Could not load facility directory: ${err.message}` });
    } finally {
      setIsLoading(false);
    }
  };

  const handleTestProximity = async () => {
    setIsSearchingNearby(true);
    try {
      const results = await facilityApi.searchNearbyFacilities(customLat, customLon, radiusKm);
      setNearbyResults(results);
      setBanner({
        type: 'info',
        message: `Discovered ${results.length} facilities within ${radiusKm} km of coordinates (${customLat}, ${customLon}).`,
      });
    } catch (err: any) {
      setBanner({ type: 'danger', message: `Geospatial query failed: ${err.message}` });
    } finally {
      setIsSearchingNearby(false);
    }
  };

  const handleToggleOperational = async (facilityId: string, currentStatus: boolean) => {
    try {
      await facilityApi.setOperationalStatus(facilityId, !currentStatus);
      setFacilities(facilities.map((f) => (f.id === facilityId ? { ...f, isOperational: !currentStatus } : f)));
      setBanner({ type: 'success', message: `Facility operational status updated to: ${!currentStatus}` });
    } catch (err: any) {
      setBanner({ type: 'danger', message: err.message || 'Failed to update facility operational status' });
    }
  };

  const filteredFacilities = facilities.filter((f) => {
    const matchesSearch =
      f.name.toLowerCase().includes(searchQuery.toLowerCase()) ||
      f.villageTown.toLowerCase().includes(searchQuery.toLowerCase()) ||
      f.district.toLowerCase().includes(searchQuery.toLowerCase());

    const matchesType = selectedType === 'ALL' || f.facilityType === selectedType;
    const matchesOperational = !onlyOperational || f.isOperational;

    return matchesSearch && matchesType && matchesOperational;
  });

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
      <div>
        <h2 style={{ fontSize: '1.75rem', fontWeight: 800 }}>Healthcare Facilities Directory (रुग्णालय निर्देशिका)</h2>
        <p style={{ color: 'var(--text-muted)' }}>
          Browse registered rural healthcare centers, Sub-Centres, PHCs, CHCs, and District Hospitals.
        </p>
      </div>

      {/* Proximity Distance Calculator Card */}
      <div className="card" style={{ backgroundColor: '#f0fdfa', border: '1px solid #99f6e4' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '0.6rem', marginBottom: '0.75rem' }}>
          <Navigation size={20} style={{ color: 'var(--primary)' }} />
          <h3 style={{ fontSize: '1.1rem', color: 'var(--primary-dark)' }}>
            Live PostGIS Geodesic Search (स्थानिक अंतर मोजा)
          </h3>
        </div>
        <p style={{ fontSize: '0.85rem', color: 'var(--text-muted)', marginBottom: '1rem' }}>
          Simulate GPS coordinates to query the spatial database for hospitals within range.
        </p>

        <div className="grid-3" style={{ alignItems: 'flex-end' }}>
          <div className="form-group" style={{ marginBottom: 0 }}>
            <label className="form-label">Latitude</label>
            <input
              type="number"
              step="any"
              className="form-input"
              value={customLat}
              onChange={(e) => setCustomLat(parseFloat(e.target.value))}
            />
          </div>

          <div className="form-group" style={{ marginBottom: 0 }}>
            <label className="form-label">Longitude</label>
            <input
              type="number"
              step="any"
              className="form-input"
              value={customLon}
              onChange={(e) => setCustomLon(parseFloat(e.target.value))}
            />
          </div>

          <div style={{ display: 'flex', gap: '0.5rem' }}>
            <Button
              type="button"
              variant="primary"
              isLoading={isSearchingNearby}
              onClick={handleTestProximity}
            >
              Search Radius ({radiusKm}km)
            </Button>
            {nearbyResults && (
              <Button type="button" variant="secondary" onClick={() => setNearbyResults(null)}>
                Clear
              </Button>
            )}
          </div>
        </div>

        {nearbyResults && (
          <div style={{ marginTop: '1rem', paddingTop: '1rem', borderTop: '1px solid #ccfbf1' }}>
            <div style={{ fontSize: '0.85rem', fontWeight: 700, marginBottom: '0.5rem' }}>
              Results within {radiusKm} km:
            </div>
            <div style={{ display: 'flex', flexWrap: 'wrap', gap: '0.5rem' }}>
              {nearbyResults.map((r, i) => (
                <div
                  key={i}
                  style={{
                    padding: '0.4rem 0.75rem',
                    backgroundColor: 'white',
                    borderRadius: 'var(--radius-sm)',
                    border: '1px solid var(--border-light)',
                    fontSize: '0.85rem',
                  }}
                >
                  <strong>{r.name}</strong> ({r.facilityType}) —{' '}
                  <span style={{ color: 'var(--primary-dark)', fontWeight: 700 }}>
                    {r.distanceKm?.toFixed(1)} km
                  </span>
                </div>
              ))}
            </div>
          </div>
        )}
      </div>

      {/* Directory Search & Filters */}
      <div className="card">
        <div className="grid-3" style={{ marginBottom: '1.25rem' }}>
          <div className="form-group" style={{ marginBottom: 0 }}>
            <label className="form-label">Search Facility Name / Village / District</label>
            <input
              type="text"
              className="form-input"
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              placeholder="e.g. Shirwal, Satara, PHC..."
            />
          </div>

          <div className="form-group" style={{ marginBottom: 0 }}>
            <label className="form-label">Facility Tier</label>
            <select
              className="form-select"
              value={selectedType}
              onChange={(e) => setSelectedType(e.target.value)}
            >
              <option value="ALL">All Facility Types</option>
              <option value="SUB_CENTRE">Sub Centre</option>
              <option value="PHC">Primary Health Centre (PHC)</option>
              <option value="CHC">Community Health Centre (CHC)</option>
              <option value="SUB_DISTRICT_HOSPITAL">Sub-District Hospital</option>
              <option value="DISTRICT_HOSPITAL">District Hospital</option>
              <option value="TERTIARY_HOSPITAL">Tertiary Hospital</option>
            </select>
          </div>

          <div style={{ display: 'flex', alignItems: 'center', height: '100%', paddingTop: '1.5rem' }}>
            <label style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', cursor: 'pointer', fontSize: '0.9rem' }}>
              <input
                type="checkbox"
                checked={onlyOperational}
                onChange={(e) => setOnlyOperational(e.target.checked)}
                style={{ width: '18px', height: '18px' }}
              />
              <span>Only Operational Facilities</span>
            </label>
          </div>
        </div>

        {/* Directory List */}
        {isLoading ? (
          <LoadingSpinner message="Loading facilities from database..." />
        ) : filteredFacilities.length === 0 ? (
          <EmptyState
            title="No Facilities Match Criteria"
            description="Try adjusting your search query or removing filters."
          />
        ) : (
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(320px, 1fr))', gap: '1.25rem' }}>
            {filteredFacilities.map((fac) => (
              <div
                key={fac.id}
                style={{
                  padding: '1.25rem',
                  backgroundColor: 'var(--bg-surface)',
                  border: '1px solid var(--border-light)',
                  borderRadius: 'var(--radius-md)',
                  display: 'flex',
                  flexDirection: 'column',
                  gap: '0.75rem',
                  boxShadow: 'var(--shadow-sm)',
                }}
              >
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
                  <h4 style={{ fontSize: '1.1rem', fontWeight: 700 }}>{fac.name}</h4>
                  <Badge variant="facilityType" value={fac.facilityType} />
                </div>

                <div style={{ fontSize: '0.85rem', color: 'var(--text-muted)', display: 'flex', flexDirection: 'column', gap: '0.25rem' }}>
                  <div style={{ display: 'flex', alignItems: 'center', gap: '0.4rem' }}>
                    <MapPin size={14} style={{ color: 'var(--primary)' }} />
                    <span>
                      {fac.addressLine1}, {fac.villageTown}, {fac.district} - {fac.pincode}
                    </span>
                  </div>

                  <div style={{ display: 'flex', alignItems: 'center', gap: '0.4rem' }}>
                    <Phone size={14} />
                    <span>{fac.contactNumber}</span>
                  </div>
                </div>

                <div style={{ display: 'flex', flexWrap: 'wrap', gap: '0.4rem', marginTop: 'auto' }}>
                  {fac.isOperational ? (
                    <span className="badge badge-low">
                      <CheckCircle2 size={12} />
                      <span>Operational</span>
                    </span>
                  ) : (
                    <span className="badge badge-critical">
                      <XCircle size={12} />
                      <span>Closed</span>
                    </span>
                  )}

                  {fac.isDemoData && <Badge variant="demo" />}
                </div>

                {fac.availableServices && fac.availableServices.length > 0 && (
                  <div style={{ marginTop: '0.5rem', paddingTop: '0.5rem', borderTop: '1px solid var(--border-light)' }}>
                    <div style={{ fontSize: '0.75rem', fontWeight: 700, color: 'var(--text-subtle)', marginBottom: '0.3rem' }}>
                      SERVICES ({fac.availableServices.length})
                    </div>
                    <div style={{ display: 'flex', flexWrap: 'wrap', gap: '0.3rem' }}>
                      {fac.availableServices.map((s, idx) => (
                        <span
                          key={idx}
                          style={{
                            fontSize: '0.7rem',
                            padding: '0.15rem 0.45rem',
                            backgroundColor: 'var(--bg-surface-subtle)',
                            borderRadius: 'var(--radius-sm)',
                            color: 'var(--text-muted)',
                            border: '1px solid var(--border-light)',
                          }}
                        >
                          {s.replace(/_/g, ' ')}
                        </span>
                      ))}
                    </div>
                  </div>
                )}

                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginTop: '0.5rem', paddingTop: '0.5rem', borderTop: '1px solid var(--border-light)' }}>
                  <span style={{ fontSize: '0.8rem', color: 'var(--text-subtle)' }}>Admin Control:</span>
                  <Button
                    size="sm"
                    variant={fac.isOperational ? 'secondary' : 'success'}
                    onClick={() => handleToggleOperational(fac.id, fac.isOperational)}
                  >
                    {fac.isOperational ? 'Set Inactive' : 'Set Operational'}
                  </Button>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
};
