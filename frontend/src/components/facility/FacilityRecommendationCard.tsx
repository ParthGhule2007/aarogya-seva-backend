import React from 'react';
import { FacilityRecommendationDto } from '../../types/facility';
import { Badge } from '../common/Badge';
import { Button } from '../common/Button';
import { MapPin, Phone, Send, CheckCircle2, XCircle } from 'lucide-react';

interface FacilityRecommendationCardProps {
  facility: FacilityRecommendationDto;
  isSelected?: boolean;
  onSelect: (facility: FacilityRecommendationDto) => void;
  disabled?: boolean;
}

export const FacilityRecommendationCard: React.FC<FacilityRecommendationCardProps> = ({
  facility,
  isSelected = false,
  onSelect,
  disabled = false,
}) => {
  return (
    <div
      className="card"
      style={{
        border: isSelected ? '2px solid var(--primary)' : '1px solid var(--border-light)',
        boxShadow: isSelected ? '0 0 0 4px var(--primary-light)' : 'var(--shadow-sm)',
        transition: 'all 0.2s ease',
        backgroundColor: facility.isOperational ? 'var(--bg-surface)' : 'var(--bg-surface-subtle)',
      }}
    >
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: '0.75rem' }}>
        <div>
          <h4 style={{ fontSize: '1.1rem', fontWeight: 700, marginBottom: '0.25rem' }}>
            {facility.name}
          </h4>
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.4rem', fontSize: '0.85rem', color: 'var(--text-muted)' }}>
            <MapPin size={14} style={{ color: 'var(--primary)' }} />
            <span>
              {facility.villageTown}, {facility.district} ({facility.state})
            </span>
          </div>
        </div>

        <div style={{ textAlign: 'right' }}>
          {facility.distanceKm !== undefined && facility.distanceKm !== null ? (
            <>
              <div style={{ fontSize: '1.15rem', fontWeight: 800, color: 'var(--primary-dark)' }}>
                {facility.distanceKm.toFixed(1)} km
              </div>
              <span style={{
                fontSize: '0.75rem',
                fontWeight: 600,
                color: facility.distanceKm > 50 ? '#92400E' : 'var(--text-subtle)'
              }}>
                {facility.distanceKm > 50 ? 'Regional Fallback' : 'Geodesic Distance'}
              </span>
            </>
          ) : (
            <span style={{ fontSize: '0.75rem', color: 'var(--text-subtle)' }}>Distance unavailable</span>
          )}
        </div>
      </div>

      <div style={{ display: 'flex', flexWrap: 'wrap', gap: '0.4rem', marginBottom: '0.85rem' }}>
        <Badge variant="facilityType" value={facility.facilityType} />

        {facility.distanceKm !== undefined && facility.distanceKm !== null && (
          facility.distanceKm > 50 ? (
            <span className="badge badge-warning" style={{ fontSize: '0.75rem', fontWeight: 700 }}>
              Regional ({facility.distanceKm > 100 ? '>100 km' : '>50 km'})
            </span>
          ) : (
            <span className="badge badge-low" style={{ fontSize: '0.75rem', fontWeight: 700 }}>
              Nearby Local
            </span>
          )
        )}

        {facility.isOperational ? (
          <span className="badge badge-low">
            <CheckCircle2 size={12} />
            <span>Operational (सुरू आहे)</span>
          </span>
        ) : (
          <span className="badge badge-critical">
            <XCircle size={12} />
            <span>Closed (बंद आहे)</span>
          </span>
        )}

        {facility.isDemoData && <Badge variant="demo" />}
      </div>

      {facility.availableServices && facility.availableServices.length > 0 && (
        <div style={{ marginBottom: '1rem' }}>
          <div style={{ fontSize: '0.75rem', fontWeight: 700, color: 'var(--text-subtle)', marginBottom: '0.35rem', textTransform: 'uppercase' }}>
            Available Services
          </div>
          <div style={{ display: 'flex', flexWrap: 'wrap', gap: '0.35rem' }}>
            {facility.availableServices.map((service, idx) => (
              <span
                key={idx}
                style={{
                  fontSize: '0.75rem',
                  padding: '0.2rem 0.5rem',
                  borderRadius: 'var(--radius-sm)',
                  backgroundColor: 'var(--bg-surface-subtle)',
                  color: 'var(--text-muted)',
                  border: '1px solid var(--border-light)',
                }}
              >
                {service.replace(/_/g, ' ')}
              </span>
            ))}
          </div>
        </div>
      )}

      <div
        style={{
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
          paddingTop: '0.75rem',
          borderTop: '1px solid var(--border-light)',
        }}
      >
        <div style={{ display: 'flex', alignItems: 'center', gap: '0.4rem', fontSize: '0.85rem', color: 'var(--text-muted)' }}>
          <Phone size={14} />
          <span>{facility.contactNumber || 'Contact not listed'}</span>
        </div>

        <Button
          type="button"
          size="sm"
          variant={isSelected ? 'success' : 'primary'}
          icon={<Send size={14} />}
          disabled={disabled || !facility.isOperational}
          onClick={() => onSelect(facility)}
        >
          {isSelected ? 'Selected' : 'Refer Patient'}
        </Button>
      </div>
    </div>
  );
};
