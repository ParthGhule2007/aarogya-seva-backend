import React, { useState, useEffect } from 'react';
import { useSos } from '../../hooks/useSos';
import { useLanguage } from '../../context/LanguageContext';
import { useAuth } from '../../context/AuthContext';
import { facilityApi } from '../../api/facilityApi';
import { caregiverApi } from '../../api/caregiverApi';
import { TargetType } from '../../types/sos';
import { HealthcareFacilityDto } from '../../types/facility';
import { Caregiver } from '../../types/medicine';
import {
  X,
  AlertTriangle,
  PhoneCall,
  MapPin,
  Users,
  Building2,
  HeartHandshake,
  CheckCircle,
  Clock,
  Radio,
  Navigation,
} from 'lucide-react';

export const EmergencySosModal: React.FC = () => {
  const {
    isSosModalOpen,
    setIsSosModalOpen,
    activeSos,
    currentCoords,
    locationError,
    isLocating,
    isTriggering,
    error,
    detectLocation,
    triggerSos,
    resolveSos,
    clearActiveSos,
  } = useSos();

  const { t } = useLanguage();
  const { user, selectedPatientId } = useAuth();
  const patientId = selectedPatientId || user?.patientId || 'b1000000-0000-0000-0000-000000000001';

  const [selectedTargets, setSelectedTargets] = useState<TargetType[]>(['NEARBY_PEOPLE', 'FACILITY', 'CAREGIVER']);
  const [selectedFacilityId, setSelectedFacilityId] = useState<string>('');
  const [sosMessage, setSosMessage] = useState<string>('');
  const [nearbyFacilities, setNearbyFacilities] = useState<HealthcareFacilityDto[]>([]);
  const [caregiverContacts, setCaregiverContacts] = useState<Caregiver[]>([]);

  useEffect(() => {
    if (isSosModalOpen) {
      detectLocation();
    }
  }, [isSosModalOpen, detectLocation]);

  // Load nearby facilities once coords are available
  useEffect(() => {
    if (currentCoords) {
      facilityApi
        .searchNearbyFacilities({
          latitude: currentCoords.latitude,
          longitude: currentCoords.longitude,
          radiusKm: 25,
        })
        .then((res) => {
          if (res && res.length > 0) {
            setNearbyFacilities(res);
            setSelectedFacilityId(res[0].id || res[0].facilityId);
          }
        })
        .catch(() => {});
    }
  }, [currentCoords]);

  // Load caregivers (strictly filter by consentGiven && active)
  useEffect(() => {
    if (patientId) {
      caregiverApi
        .getCaregivers(patientId)
        .then((data) => {
          const consented = (data || []).filter((c) => c.consentGiven !== false && c.active !== false);
          setCaregiverContacts(consented);
        })
        .catch(() => {});
    }
  }, [patientId]);

  if (!isSosModalOpen) return null;

  const toggleTarget = (target: TargetType) => {
    if (selectedTargets.includes(target)) {
      if (selectedTargets.length === 1) return; // Keep at least one target
      setSelectedTargets(selectedTargets.filter((t) => t !== target));
    } else {
      setSelectedTargets([...selectedTargets, target]);
    }
  };

  const handleTrigger = async () => {
    try {
      await triggerSos(
        selectedTargets,
        selectedFacilityId || null,
        sosMessage || undefined
      );
    } catch {
      // Handled in useSos
    }
  };

  return (
    <div className="modal-overlay" onClick={() => setIsSosModalOpen(false)} role="dialog" aria-modal="true">
      <div className="modal-dialog wide" onClick={(e) => e.stopPropagation()}>
        {/* Header */}
        <div className="modal-header" style={{ background: '#dc2626', color: '#ffffff' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.6rem' }}>
            <AlertTriangle size={24} />
            <h2 style={{ fontSize: '1.3rem', fontWeight: 700, color: '#ffffff' }}>
              {t('navSos')}
            </h2>
          </div>
          <button
            className="icon-btn"
            style={{ color: '#ffffff', borderColor: 'rgba(255,255,255,0.4)', background: 'transparent' }}
            onClick={() => setIsSosModalOpen(false)}
            aria-label="Close"
          >
            <X size={20} />
          </button>
        </div>

        <div className="modal-body">
          {/* Direct Emergency Call Banner */}
          <div
            style={{
              background: '#fef2f2',
              border: '2px solid #fecaca',
              borderRadius: 'var(--radius-md)',
              padding: '1rem',
              marginBottom: '1.5rem',
            }}
          >
            <div style={{ fontSize: '0.85rem', fontWeight: 700, color: '#991b1b', marginBottom: '0.5rem', display: 'flex', alignItems: 'center', gap: '0.4rem' }}>
              <PhoneCall size={16} />
              <span>{t('sosEmergencyCall')} (No Internet / GPS Required)</span>
            </div>
            <div style={{ display: 'flex', gap: '0.75rem', flexWrap: 'wrap' }}>
              <a
                href="tel:108"
                className="btn btn-danger"
                style={{ flex: 1, minHeight: '48px', textDecoration: 'none' }}
              >
                <PhoneCall size={18} />
                <span>{t('sos108')}</span>
              </a>
              <a
                href="tel:112"
                className="btn btn-outline"
                style={{ flex: 1, minHeight: '48px', borderColor: '#dc2626', color: '#dc2626', textDecoration: 'none' }}
              >
                <PhoneCall size={18} />
                <span>{t('sos112')}</span>
              </a>
            </div>
          </div>

          {/* If Active SOS Exists, Show Live Incident Progress */}
          {activeSos && (
            <div
              style={{
                background: '#f0fdf4',
                border: '2px solid #86efac',
                borderRadius: 'var(--radius-lg)',
                padding: '1.25rem',
                marginBottom: '1.5rem',
              }}
            >
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '0.75rem' }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                  <Radio size={20} color="#15803d" className="pulse-icon" />
                  <span style={{ fontWeight: 700, fontSize: '1.1rem', color: '#15803d' }}>
                    {activeSos.status === 'RESOLVED' ? t('sosStatusResolved') : t('sosStatusDispatched')}
                  </span>
                </div>
                <span className="badge badge-emergency" style={{ fontSize: '0.85rem' }}>
                  {activeSos.status}
                </span>
              </div>

              <div style={{ fontSize: '0.9rem', color: '#166534', marginBottom: '0.75rem' }}>
                Incident ID: <strong>{activeSos.sosId}</strong> • Dispatched: {new Date(activeSos.triggeredAt).toLocaleTimeString()}
              </div>

              <div style={{ fontSize: '0.85rem', color: '#166534', marginBottom: '0.75rem', background: '#dcfce7', padding: '0.5rem 0.75rem', borderRadius: 'var(--radius-sm)' }}>
                <strong>Recipient Dispatch Summary:</strong> {activeSos.totalRecipients} total •{' '}
                <span style={{ color: '#15803d', fontWeight: 700 }}>{activeSos.successfulNotifications} sent</span>
                {activeSos.failedNotifications > 0 && (
                  <span style={{ color: '#b91c1c', fontWeight: 700 }}> • {activeSos.failedNotifications} failed</span>
                )}
              </div>

              <div style={{ display: 'flex', gap: '0.5rem', flexWrap: 'wrap', marginBottom: '1rem' }}>
                {activeSos.targets.map((tgt) => (
                  <span key={tgt} style={{ background: '#dcfce7', padding: '0.3rem 0.6rem', borderRadius: 'var(--radius-sm)', fontSize: '0.8rem', fontWeight: 600 }}>
                    🎯 {tgt}
                  </span>
                ))}
              </div>

              {activeSos.status !== 'RESOLVED' ? (
                <button
                  className="btn btn-secondary"
                  style={{ width: '100%' }}
                  onClick={() => resolveSos(activeSos.sosId)}
                >
                  <CheckCircle size={18} />
                  <span>{t('sosResolveBtn')}</span>
                </button>
              ) : (
                <button
                  className="btn btn-outline"
                  style={{ width: '100%' }}
                  onClick={clearActiveSos}
                >
                  Close & Dismiss Incident
                </button>
              )}
            </div>
          )}

          {/* SOS Dispatch Form */}
          {!activeSos && (
            <>
              {error && (
                <div className="alert-box danger">
                  <AlertTriangle size={20} />
                  <span>{error}</span>
                </div>
              )}

              {/* Location Status */}
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
                <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                  <MapPin size={18} color="var(--primary)" />
                  <span style={{ fontSize: '0.9rem' }}>
                    {currentCoords
                      ? `GPS: ${currentCoords.latitude.toFixed(4)}, ${currentCoords.longitude.toFixed(4)}`
                      : isLocating
                      ? 'Acquiring GPS location...'
                      : locationError || 'GPS not active'}
                  </span>
                </div>
                <button
                  type="button"
                  className="btn btn-outline"
                  style={{ minHeight: '36px', padding: '0 0.6rem', fontSize: '0.8rem' }}
                  onClick={detectLocation}
                  disabled={isLocating}
                >
                  <Navigation size={14} />
                  <span>Update GPS</span>
                </button>
              </div>

              {/* Three Help Options */}
              <div style={{ marginBottom: '1.25rem' }}>
                <label className="form-label">Select SOS Help Options:</label>
                <div style={{ display: 'flex', flexDirection: 'column', gap: '0.75rem' }}>
                  {/* Option A: People within 5 km */}
                  <div
                    onClick={() => toggleTarget('NEARBY_PEOPLE')}
                    style={{
                      border: `2px solid ${selectedTargets.includes('NEARBY_PEOPLE') ? 'var(--primary)' : 'var(--border-light)'}`,
                      background: selectedTargets.includes('NEARBY_PEOPLE') ? 'var(--primary-light)' : 'var(--bg-surface)',
                      padding: '0.85rem 1rem',
                      borderRadius: 'var(--radius-md)',
                      cursor: 'pointer',
                      display: 'flex',
                      alignItems: 'center',
                      gap: '0.75rem',
                      transition: 'all 0.15s ease',
                    }}
                  >
                    <input
                      type="checkbox"
                      checked={selectedTargets.includes('NEARBY_PEOPLE')}
                      onChange={() => {}}
                      style={{ width: '20px', height: '20px', accentColor: 'var(--primary)' }}
                    />
                    <Users size={22} color="var(--primary)" />
                    <div>
                      <div style={{ fontWeight: 700 }}>{t('sosOptionA')}</div>
                      <div style={{ fontSize: '0.85rem', color: 'var(--text-subtle)' }}>
                        {t('sosOptionADesc')}
                      </div>
                    </div>
                  </div>

                  {/* Option B: Nearby Hospitals */}
                  <div
                    onClick={() => toggleTarget('FACILITY')}
                    style={{
                      border: `2px solid ${selectedTargets.includes('FACILITY') ? 'var(--primary)' : 'var(--border-light)'}`,
                      background: selectedTargets.includes('FACILITY') ? 'var(--primary-light)' : 'var(--bg-surface)',
                      padding: '0.85rem 1rem',
                      borderRadius: 'var(--radius-md)',
                      cursor: 'pointer',
                      display: 'flex',
                      alignItems: 'center',
                      gap: '0.75rem',
                      transition: 'all 0.15s ease',
                    }}
                  >
                    <input
                      type="checkbox"
                      checked={selectedTargets.includes('FACILITY')}
                      onChange={() => {}}
                      style={{ width: '20px', height: '20px', accentColor: 'var(--primary)' }}
                    />
                    <Building2 size={22} color="var(--secondary)" />
                    <div style={{ flex: 1 }}>
                      <div style={{ fontWeight: 700 }}>{t('sosOptionB')}</div>
                      <div style={{ fontSize: '0.85rem', color: 'var(--text-subtle)' }}>
                        {t('sosOptionBDesc')}
                      </div>
                      {selectedTargets.includes('FACILITY') && nearbyFacilities.length > 0 && (
                        <select
                          className="form-control"
                          style={{ marginTop: '0.5rem', minHeight: '40px', fontSize: '0.85rem' }}
                          value={selectedFacilityId}
                          onClick={(e) => e.stopPropagation()}
                          onChange={(e) => setSelectedFacilityId(e.target.value)}
                        >
                          {nearbyFacilities.map((fac) => (
                            <option key={fac.id || fac.facilityId} value={fac.id || fac.facilityId}>
                              {fac.facilityName || fac.name} ({fac.distanceKm?.toFixed(1) || '0'} km away)
                            </option>
                          ))}
                        </select>
                      )}
                    </div>
                  </div>

                  {/* Option C: Family & Caregivers */}
                  <div
                    onClick={() => toggleTarget('CAREGIVER')}
                    style={{
                      border: `2px solid ${selectedTargets.includes('CAREGIVER') ? 'var(--primary)' : 'var(--border-light)'}`,
                      background: selectedTargets.includes('CAREGIVER') ? 'var(--primary-light)' : 'var(--bg-surface)',
                      padding: '0.85rem 1rem',
                      borderRadius: 'var(--radius-md)',
                      cursor: 'pointer',
                      display: 'flex',
                      alignItems: 'center',
                      gap: '0.75rem',
                      transition: 'all 0.15s ease',
                    }}
                  >
                    <input
                      type="checkbox"
                      checked={selectedTargets.includes('CAREGIVER')}
                      onChange={() => {}}
                      style={{ width: '20px', height: '20px', accentColor: 'var(--primary)' }}
                    />
                    <HeartHandshake size={22} color="var(--accent-warm)" />
                    <div>
                      <div style={{ fontWeight: 700 }}>{t('sosOptionC')}</div>
                      <div style={{ fontSize: '0.85rem', color: 'var(--text-subtle)' }}>
                        {t('sosOptionCDesc')}
                      </div>
                      {caregiverContacts.length > 0 && (
                        <div style={{ fontSize: '0.8rem', color: 'var(--text-muted)', marginTop: '0.25rem' }}>
                          Contacts: {caregiverContacts.map((c) => `${c.name || c.caregiverName} (${c.relationship})`).join(', ')}
                        </div>
                      )}
                    </div>
                  </div>
                </div>
              </div>

              {/* Message / Landmark Note */}
              <div className="form-group">
                <label className="form-label" htmlFor="sos-msg">
                  Emergency Notes / Landmark:
                </label>
                <input
                  id="sos-msg"
                  type="text"
                  className="form-control"
                  placeholder="e.g. Near village primary school, severe chest pain"
                  value={sosMessage}
                  onChange={(e) => setSosMessage(e.target.value)}
                />
              </div>

              {/* Trigger Button */}
              <button
                type="button"
                className="btn btn-danger"
                style={{ width: '100%', minHeight: '54px', fontSize: '1.1rem', fontWeight: 800 }}
                onClick={handleTrigger}
                disabled={isTriggering}
              >
                <AlertTriangle size={22} />
                <span>{isTriggering ? 'Dispatching Emergency Alert...' : t('sosTriggerNow')}</span>
              </button>
            </>
          )}
        </div>
      </div>
    </div>
  );
};
