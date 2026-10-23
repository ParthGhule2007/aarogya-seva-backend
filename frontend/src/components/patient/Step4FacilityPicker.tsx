import React, { useState } from 'react';
import { Building2, MapPin, CheckCircle, AlertCircle, ArrowRight, ArrowLeft, Phone, Stethoscope } from 'lucide-react';
import { FacilityRecommendationDto } from '../../types/facility';
import { AlertBox } from '../common/AlertBox';

interface Step4FacilityPickerProps {
  facilities: FacilityRecommendationDto[];
  onSelectFacility: (facility: FacilityRecommendationDto) => void;
  onBack: () => void;
}

export const Step4FacilityPicker: React.FC<Step4FacilityPickerProps> = ({
  facilities,
  onSelectFacility,
  onBack,
}) => {
  const [selectedFacilityId, setSelectedFacilityId] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);

  const selectedFacility = facilities.find((f) => f.facilityId === selectedFacilityId);

  const handleContinue = () => {
    if (!selectedFacility) {
      setError('Please tap on a facility to select where you would like care (कृपया एक दवाखाना निवडा)');
      return;
    }
    onSelectFacility(selectedFacility);
  };

  return (
    <div className="card-simple" style={{ maxWidth: '680px', margin: '0 auto' }}>
      <h2 style={{ fontSize: '1.6rem', fontWeight: 800, color: 'var(--primary)', marginBottom: '0.4rem' }}>
        Step 4: Healthcare Facility Selection
      </h2>
      <p style={{ color: 'var(--text-muted)', fontSize: '1.05rem', marginBottom: '1.25rem' }}>
        दवाखाना: योग्य आरोग्य केंद्र निवडा
      </p>

      {facilities.length > 0 ? (
        <p style={{ fontSize: '1.05rem', color: 'var(--text-main)', marginBottom: '1.25rem' }}>
          {facilities.every(f => f.distanceKm && f.distanceKm > 50)
            ? <span>Showing <strong>{facilities.length}</strong> regional fallback facility(ies) matching required clinical care (regional transit required):</span>
            : <span>We found <strong>{facilities.length}</strong> healthcare facilities matching your required clinical services:</span>}
        </p>
      ) : (
        <AlertBox type="warning">
          <strong>No matching operational facilities found within search radius.</strong>
        </AlertBox>
      )}

      {error && (
        <AlertBox type="warning">
          <strong>{error}</strong>
        </AlertBox>
      )}

      {/* Facilities List */}
      <div style={{ display: 'flex', flexDirection: 'column', gap: '1rem', marginBottom: '2rem' }}>
        {facilities.map((facility) => {
          const isSelected = facility.facilityId === selectedFacilityId;

          return (
            <div
              key={facility.facilityId}
              className={`card-simple card-interactive ${isSelected ? 'selected' : ''}`}
              style={{
                marginBottom: 0,
                padding: '1.25rem',
                borderWidth: isSelected ? '3px' : '2px',
                borderColor: isSelected ? 'var(--primary)' : 'var(--border-medium)',
              }}
              onClick={() => {
                setSelectedFacilityId(facility.facilityId);
                setError(null);
              }}
              role="radio"
              aria-checked={isSelected}
              tabIndex={0}
              onKeyDown={(e) => {
                if (e.key === ' ' || e.key === 'Enter') {
                  setSelectedFacilityId(facility.facilityId);
                  setError(null);
                }
              }}
            >
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', gap: '0.5rem' }}>
                <div style={{ display: 'flex', alignItems: 'flex-start', gap: '0.75rem' }}>
                  <Building2 size={28} color="var(--primary)" style={{ flexShrink: 0, marginTop: '2px' }} />
                  <div>
                    <h3 style={{ fontSize: '1.25rem', fontWeight: 800, color: 'var(--text-main)', marginBottom: '0.2rem' }}>
                      {facility.name}
                    </h3>
                    <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem', flexWrap: 'wrap', fontSize: '0.95rem' }}>
                      <span style={{ fontWeight: 600, color: 'var(--primary)' }}>
                        {facility.facilityType || 'Healthcare Centre'}
                      </span>
                      <span>•</span>
                      <span style={{ display: 'flex', alignItems: 'center', gap: '0.25rem', color: 'var(--text-main)', fontWeight: 700 }}>
                        <MapPin size={16} color="var(--accent-warm)" />
                        {facility.distanceKm !== undefined && facility.distanceKm !== null
                          ? `${facility.distanceKm.toFixed(1)} km away`
                          : 'Distance unavailable'}
                      </span>
                      {facility.distanceKm !== undefined && facility.distanceKm !== null && (
                        facility.distanceKm > 50 ? (
                          <span style={{
                            fontSize: '0.75rem',
                            fontWeight: 700,
                            padding: '0.15rem 0.5rem',
                            borderRadius: '4px',
                            backgroundColor: '#FEF3C7',
                            color: '#92400E',
                            border: '1px solid #FCD34D'
                          }}>
                            Regional Fallback ({facility.distanceKm > 100 ? 'Distant' : 'Extended'})
                          </span>
                        ) : (
                          <span style={{
                            fontSize: '0.75rem',
                            fontWeight: 700,
                            padding: '0.15rem 0.5rem',
                            borderRadius: '4px',
                            backgroundColor: '#D1FAE5',
                            color: '#065F46',
                            border: '1px solid #6EE7B7'
                          }}>
                            Nearby Local (≤ 50 km)
                          </span>
                        )
                      )}
                    </div>
                  </div>
                </div>

                {/* Selection Radio Visual */}
                <div
                  style={{
                    width: '24px',
                    height: '24px',
                    borderRadius: '50%',
                    border: `2px solid ${isSelected ? 'var(--primary)' : 'var(--border-medium)'}`,
                    backgroundColor: isSelected ? 'var(--primary)' : 'transparent',
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                    flexShrink: 0,
                  }}
                >
                  {isSelected && <div style={{ width: '8px', height: '8px', borderRadius: '50%', backgroundColor: '#fff' }} />}
                </div>
              </div>

              {/* Status & Badges */}
              <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', marginTop: '0.75rem', flexWrap: 'wrap' }}>
                <span className="status-pill accepted" style={{ fontSize: '0.85rem', padding: '0.2rem 0.6rem' }}>
                  ✓ {facility.isOperational ? 'Operational' : 'Inactive'}
                </span>

                {facility.isDemoData && (
                  <span style={{ fontSize: '0.75rem', color: 'var(--text-subtle)', background: 'var(--bg-subtle)', padding: '0.2rem 0.5rem', borderRadius: '4px' }}>
                    Demo Data
                  </span>
                )}

                {facility.contactNumber && (
                  <span style={{ display: 'flex', alignItems: 'center', gap: '0.25rem', fontSize: '0.85rem', color: 'var(--text-muted)' }}>
                    <Phone size={14} /> {facility.contactNumber}
                  </span>
                )}
              </div>

              {/* Available Services */}
              {facility.availableServices && facility.availableServices.length > 0 && (
                <div style={{ marginTop: '0.75rem' }}>
                  <span style={{ fontSize: '0.85rem', fontWeight: 700, color: 'var(--text-subtle)', display: 'block', marginBottom: '0.3rem' }}>
                    Services:
                  </span>
                  <div style={{ display: 'flex', flexWrap: 'wrap', gap: '0.35rem' }}>
                    {facility.availableServices.map((srv, idx) => (
                      <span
                        key={idx}
                        style={{
                          backgroundColor: 'var(--primary-light)',
                          color: 'var(--primary)',
                          fontSize: '0.85rem',
                          fontWeight: 600,
                          padding: '0.2rem 0.6rem',
                          borderRadius: 'var(--radius-sm)',
                        }}
                      >
                        {srv}
                      </span>
                    ))}
                  </div>
                </div>
              )}
            </div>
          );
        })}
      </div>

      {/* Navigation Buttons */}
      <div className="btn-row" style={{ display: 'flex', gap: '1rem' }}>
        <button
          type="button"
          className="btn-large btn-ghost"
          onClick={onBack}
          style={{ flex: 1 }}
        >
          <ArrowLeft size={22} />
          <span>Back • मागे</span>
        </button>

        <button
          type="button"
          className="btn-large btn-primary"
          style={{ flex: 2 }}
          onClick={handleContinue}
          disabled={!selectedFacilityId}
        >
          <span>REQUEST CARE • काळजीची विनंती करा</span>
          <ArrowRight size={22} />
        </button>
      </div>
    </div>
  );
};
