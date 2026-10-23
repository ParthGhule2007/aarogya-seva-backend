import React, { useState, useEffect, useRef } from 'react';
import {
  AlertTriangle,
  MapPin,
  Users,
  Building2,
  HeartHandshake,
  CheckCircle2,
  XCircle,
  Clock,
  ShieldAlert,
  PhoneCall,
  RefreshCw,
  X,
  AlertCircle,
  Check,
  BellRing,
} from 'lucide-react';
import { sosApi } from '../../api/sosApi';
import { facilityApi } from '../../api/facilityApi';
import { patientApi } from '../../api/patientApi';
import { requestFcmToken } from '../../config/firebase';
import { TargetType, SosStatusResponse, CreateSosRequest } from '../../types/sos';
import { FacilityRecommendationDto } from '../../types/facility';
import { PatientDto } from '../../types/patient';

interface EmergencySosModalProps {
  isOpen: boolean;
  onClose: () => void;
  preselectedPatientId?: string;
}

export const EmergencySosModal: React.FC<EmergencySosModalProps> = ({
  isOpen,
  onClose,
  preselectedPatientId,
}) => {
  // 1. Patient identity
  const [patientId, setPatientId] = useState<string>(
    preselectedPatientId || localStorage.getItem('aarogya_patient_id') || ''
  );
  const [patientName, setPatientName] = useState<string>(
    localStorage.getItem('aarogya_patient_name') || ''
  );
  const [availablePatients, setAvailablePatients] = useState<PatientDto[]>([]);

  // 2. Geolocation state
  const [coords, setCoords] = useState<{ lat: number; lng: number } | null>(null);
  const [locationStatus, setLocationStatus] = useState<
    'detecting' | 'detected' | 'denied' | 'error'
  >('detecting');
  const [locationError, setLocationError] = useState<string | null>(null);

  // 3. Targets selection
  const [selectedTargets, setSelectedTargets] = useState<TargetType[]>([
    'CAREGIVER',
    'FACILITY',
    'NEARBY_PEOPLE',
  ]);

  // 4. Facility selection
  const [nearbyFacilities, setNearbyFacilities] = useState<FacilityRecommendationDto[]>([]);
  const [selectedFacilityId, setSelectedFacilityId] = useState<string | null>(null);
  const [isLoadingFacilities, setIsLoadingFacilities] = useState<boolean>(false);

  // 5. Emergency Message
  const [emergencyMessage, setEmergencyMessage] = useState<string>(
    'Emergency medical assistance needed immediately. • त्वरित वैद्यकीय मदतीची गरज आहे.'
  );

  // 6. Submission & Active SOS state
  const [isSubmitting, setIsSubmitting] = useState<boolean>(false);
  const [submitError, setSubmitError] = useState<string | null>(null);
  const [activeSosId, setActiveSosId] = useState<string | null>(null);
  const [sosStatus, setSosStatus] = useState<SosStatusResponse | null>(null);
  const [isResolving, setIsResolving] = useState<boolean>(false);
  const [fcmRegistered, setFcmRegistered] = useState<boolean>(false);

  // Poll timer reference
  const pollTimerRef = useRef<number | null>(null);

  // Auto register device push token with SOS backend
  const registerDeviceToken = async (userId: string, currentCoords?: { lat: number; lng: number } | null) => {
    try {
      const token = await requestFcmToken();
      if (token) {
        await sosApi.registerDevice({
          userId,
          fcmToken: token,
          userType: 'PATIENT',
          emergencyAlertOptIn: true,
          latitude: currentCoords?.lat,
          longitude: currentCoords?.lng,
        });
        setFcmRegistered(true);
        console.log('[FCM] Successfully registered device push token with SOS Service');
      }
    } catch (e) {
      console.warn('[FCM] Browser push registration note:', e);
    }
  };

  // On open: detect location and load patients if needed
  useEffect(() => {
    if (!isOpen) {
      if (pollTimerRef.current) {
        clearInterval(pollTimerRef.current);
        pollTimerRef.current = null;
      }
      return;
    }

    // Load patients if no preselected patient ID
    if (!patientId) {
      patientApi.getAllPatients().then((list) => {
        setAvailablePatients(list);
        if (list.length > 0) {
          setPatientId(list[0].id);
          setPatientName(list[0].fullName);
          localStorage.setItem('aarogya_patient_id', list[0].id);
          localStorage.setItem('aarogya_patient_name', list[0].fullName);
          registerDeviceToken(list[0].id, coords);
        }
      }).catch(console.error);
    } else {
      registerDeviceToken(patientId, coords);
    }

    // Trigger device geolocation
    detectLocation();
  }, [isOpen]);

  // Detect browser/device current location
  const detectLocation = () => {
    setLocationStatus('detecting');
    setLocationError(null);

    if (!('geolocation' in navigator)) {
      setLocationStatus('error');
      setLocationError('Geolocation is not supported by your browser / device.');
      return;
    }

    navigator.geolocation.getCurrentPosition(
      (position) => {
        const lat = Number(position.coords.latitude.toFixed(6));
        const lng = Number(position.coords.longitude.toFixed(6));
        setCoords({ lat, lng });
        setLocationStatus('detected');
        // Fetch nearby facilities for facility picker
        fetchFacilities(lat, lng);
      },
      (err) => {
        console.warn('Geolocation error:', err);
        setLocationStatus('denied');
        setLocationError(
          'Location access was denied or unavailable. Emergency alert requires device coordinates so help can find you. (स्थान परवानगी नाकारली गेली आहे. मदतीसाठी स्थान आवश्यक आहे.)'
        );
      },
      {
        enableHighAccuracy: true,
        timeout: 10000,
        maximumAge: 0,
      }
    );
  };

  // Fetch facilities for selection
  const fetchFacilities = async (lat: number, lng: number) => {
    setIsLoadingFacilities(true);
    try {
      const facilities = await facilityApi.getNearbyFacilities(lat, lng, 50);
      if (facilities && facilities.length > 0) {
        setNearbyFacilities(facilities);
        if (!selectedFacilityId) {
          setSelectedFacilityId(facilities[0].facilityId);
        }
      } else {
        // Fallback to all facilities
        const all = await facilityApi.getAllFacilities();
        const mapped: FacilityRecommendationDto[] = all.map((f) => ({
          facilityId: f.id,
          name: f.name || f.facilityName || 'Healthcare Facility',
          facilityType: f.facilityType,
          villageTown: f.villageTown,
          district: f.district,
          state: f.state,
          pincode: f.pincode,
          contactNumber: f.contactNumber,
          isOperational: f.isOperational,
          isDemoData: f.isDemoData,
          availableServices: f.availableServices,
        }));
        setNearbyFacilities(mapped);
        if (mapped.length > 0 && !selectedFacilityId) {
          setSelectedFacilityId(mapped[0].facilityId);
        }
      }
    } catch (e) {
      console.warn('Could not load facilities:', e);
    } finally {
      setIsLoadingFacilities(false);
    }
  };

  // Poll SOS status
  const startStatusPolling = (sosId: string) => {
    if (pollTimerRef.current) {
      clearInterval(pollTimerRef.current);
    }

    const checkStatus = async () => {
      try {
        const status = await sosApi.getSosStatus(sosId);
        setSosStatus(status);
        if (status.status !== 'PROCESSING') {
          // Terminal state reached, slow down polling
          if (pollTimerRef.current) {
            clearInterval(pollTimerRef.current);
            pollTimerRef.current = null;
          }
        }
      } catch (err) {
        console.error('Error polling SOS status:', err);
      }
    };

    // Initial check
    checkStatus();
    // Poll every 2.5 seconds
    pollTimerRef.current = window.setInterval(checkStatus, 2500);
  };

  // Toggle target checkbox
  const handleToggleTarget = (target: TargetType) => {
    setSelectedTargets((prev) => {
      if (prev.includes(target)) {
        return prev.filter((t) => t !== target);
      } else {
        return [...prev, target];
      }
    });
  };

  // Trigger SOS handler
  const handleTriggerSos = async () => {
    setSubmitError(null);

    // Validation
    if (!patientId) {
      setSubmitError('Please select or register a patient profile first.');
      return;
    }

    if (!coords) {
      setSubmitError('Device location is required to trigger emergency SOS.');
      return;
    }

    if (selectedTargets.length === 0) {
      setSubmitError('Please select at least one recipient group (Caregiver, Facility, or Nearby Helpers).');
      return;
    }

    if (selectedTargets.includes('FACILITY') && !selectedFacilityId) {
      setSubmitError('Please select exactly one nearby facility to receive the SOS.');
      return;
    }

    setIsSubmitting(true);

    try {
      const idempotencyKey = 'sos-' + crypto.randomUUID();
      const request: CreateSosRequest = {
        patientId,
        latitude: coords.lat,
        longitude: coords.lng,
        targets: selectedTargets,
        facilityId: selectedTargets.includes('FACILITY') ? selectedFacilityId : null,
        message: emergencyMessage,
      };

      const res = await sosApi.triggerSos(request, idempotencyKey);
      setActiveSosId(res.sosId);
      // Start polling status
      startStatusPolling(res.sosId);
    } catch (err: any) {
      console.error('Failed to trigger SOS:', err);
      setSubmitError(err.message || 'Failed to dispatch SOS alert. Please try again or dial 108 directly.');
    } finally {
      setIsSubmitting(false);
    }
  };

  // Resolve active SOS
  const handleResolveSos = async () => {
    if (!activeSosId) return;
    setIsResolving(true);
    try {
      const res = await sosApi.resolveSos(activeSosId);
      setSosStatus(res);
      if (pollTimerRef.current) {
        clearInterval(pollTimerRef.current);
        pollTimerRef.current = null;
      }
    } catch (err: any) {
      alert(`Could not resolve SOS: ${err.message || 'Please try again.'}`);
    } finally {
      setIsResolving(false);
    }
  };

  // Translate technical status to rural-friendly bilingual messaging
  const renderStatusDetails = (status: SosStatusResponse) => {
    switch (status.status) {
      case 'PROCESSING':
      case 'TRIGGERED':
        return {
          title: 'Sending Emergency Alerts...',
          titleMr: 'आणीबाणीचे इशारे पाठवत आहे...',
          color: '#C05621',
          bg: '#FEF3C7',
          border: '#FCD34D',
          desc: 'Your emergency distress signal is currently being transmitted to chosen responders.',
        };
      case 'NOTIFICATIONS_SENT':
        return {
          title: 'Emergency Alerts Sent!',
          titleMr: 'आणीबाणीचे इशारे यशस्वीरित्या पाठवले गेले आहेत!',
          color: '#166534',
          bg: '#DCFCE7',
          border: '#86EFAC',
          desc: 'Responders have been notified. Stay calm, help is on the way.',
        };
      case 'PARTIALLY_SENT':
        return {
          title: 'Some Emergency Alerts Sent',
          titleMr: 'काही इशारे पाठवले गेले आहेत.',
          color: '#92400E',
          bg: '#FEF3C7',
          border: '#FCD34D',
          desc: 'Some responders were notified. Please stay safe and contact emergency services (108) if needed.',
        };
      case 'FAILED':
        return {
          title: 'Alert Dispatch Failed',
          titleMr: 'इशारे पाठवता आले नाहीत.',
          color: '#991B1B',
          bg: '#FEE2E2',
          border: '#FCA5A5',
          desc: 'We could not reach the selected contacts. Please call 108 (Ambulance) or 112 directly!',
        };
      case 'RESOLVED':
        return {
          title: 'Emergency Resolved',
          titleMr: 'आणीबाणी मिटली आहे (सुरक्षित).',
          color: '#1E293B',
          bg: '#F1F5F9',
          border: '#CBD5E1',
          desc: 'This emergency incident has been marked as resolved.',
        };
      default:
        return {
          title: 'SOS Status: ' + status.status,
          titleMr: 'स्थिती: ' + status.status,
          color: '#334155',
          bg: '#F8FAFC',
          border: '#CBD5E1',
          desc: '',
        };
    }
  };

  if (!isOpen) return null;

  return (
    <div
      style={{
        position: 'fixed',
        inset: 0,
        backgroundColor: 'rgba(0, 0, 0, 0.75)',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        zIndex: 9999,
        padding: '1rem',
        backdropFilter: 'blur(4px)',
      }}
    >
      <div
        style={{
          backgroundColor: '#FFFFFF',
          borderRadius: '1.25rem',
          maxWidth: '640px',
          width: '100%',
          maxHeight: '92vh',
          overflowY: 'auto',
          boxShadow: '0 25px 50px -12px rgba(185, 28, 28, 0.35)',
          border: '3px solid #DC2626',
        }}
      >
        {/* Header */}
        <div
          style={{
            backgroundColor: '#DC2626',
            color: '#FFFFFF',
            padding: '1.25rem 1.5rem',
            borderTopLeftRadius: '1rem',
            borderTopRightRadius: '1rem',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
          }}
        >
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
            <div
              style={{
                backgroundColor: '#FFFFFF',
                borderRadius: '50%',
                width: '44px',
                height: '44px',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
              }}
            >
              <ShieldAlert size={28} color="#DC2626" />
            </div>
            <div>
              <h2 style={{ margin: 0, fontSize: '1.5rem', fontWeight: 800, letterSpacing: '0.5px' }}>
                EMERGENCY SOS • आणीबाणी
              </h2>
              <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', marginTop: '0.2rem' }}>
                <p style={{ margin: 0, fontSize: '0.9rem', opacity: 0.95 }}>
                  Aarogya Seva Instant Distress System
                </p>
                {fcmRegistered ? (
                  <span style={{ fontSize: '0.75rem', backgroundColor: '#15803D', color: '#FFFFFF', padding: '2px 8px', borderRadius: '9999px', fontWeight: 700 }}>
                    ✓ FCM Push Enabled
                  </span>
                ) : (
                  <span style={{ fontSize: '0.75rem', backgroundColor: 'rgba(255,255,255,0.25)', color: '#FFFFFF', padding: '2px 8px', borderRadius: '9999px', fontWeight: 600 }}>
                    FCM Push Connected
                  </span>
                )}
              </div>
            </div>
          </div>

          <button
            onClick={onClose}
            style={{
              background: 'transparent',
              border: 'none',
              color: '#FFFFFF',
              cursor: 'pointer',
              padding: '0.5rem',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
            }}
            aria-label="Close"
          >
            <X size={26} />
          </button>
        </div>

        {/* Modal Body */}
        <div style={{ padding: '1.5rem' }}>
          {/* Active SOS Tracker View */}
          {activeSosId && sosStatus ? (
            <div>
              {/* Status Banner */}
              {(() => {
                const s = renderStatusDetails(sosStatus);
                return (
                  <div
                    style={{
                      backgroundColor: s.bg,
                      border: `2px solid ${s.border}`,
                      borderRadius: '1rem',
                      padding: '1.5rem',
                      textAlign: 'center',
                      marginBottom: '1.5rem',
                    }}
                  >
                    <div
                      style={{
                        fontSize: '1.5rem',
                        fontWeight: 800,
                        color: s.color,
                        marginBottom: '0.25rem',
                      }}
                    >
                      {s.title}
                    </div>
                    <div
                      style={{
                        fontSize: '1.15rem',
                        fontWeight: 700,
                        color: s.color,
                        marginBottom: '0.5rem',
                      }}
                    >
                      {s.titleMr}
                    </div>
                    <p style={{ margin: 0, fontSize: '0.95rem', color: '#475569' }}>
                      {s.desc}
                    </p>
                    {sosStatus.status === 'PROCESSING' && (
                      <div
                        style={{
                          marginTop: '1rem',
                          display: 'flex',
                          alignItems: 'center',
                          justifyContent: 'center',
                          gap: '0.5rem',
                          color: '#C05621',
                          fontWeight: 600,
                        }}
                      >
                        <RefreshCw size={20} className="animate-spin" />
                        <span>Transmitting to emergency network...</span>
                      </div>
                    )}
                  </div>
                );
              })()}

              {/* Delivery Breakdown Card */}
              <div
                style={{
                  backgroundColor: '#F8FAFC',
                  border: '1px solid #E2E8F0',
                  borderRadius: '0.75rem',
                  padding: '1.25rem',
                  marginBottom: '1.5rem',
                }}
              >
                <h4 style={{ margin: '0 0 0.75rem 0', fontSize: '1.05rem', color: '#1E293B' }}>
                  Target Delivery Report • इशारा पाठवण्याचा तपशील:
                </h4>
                <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '0.75rem' }}>
                  <div
                    style={{
                      backgroundColor: '#FFFFFF',
                      padding: '0.75rem',
                      borderRadius: '0.5rem',
                      border: '1px solid #E2E8F0',
                    }}
                  >
                    <div style={{ fontSize: '0.85rem', color: '#64748B' }}>Total Targets</div>
                    <div style={{ fontSize: '1.35rem', fontWeight: 800, color: '#1E293B' }}>
                      {sosStatus.totalRecipients}
                    </div>
                  </div>

                  <div
                    style={{
                      backgroundColor: '#FFFFFF',
                      padding: '0.75rem',
                      borderRadius: '0.5rem',
                      border: '1px solid #E2E8F0',
                    }}
                  >
                    <div style={{ fontSize: '0.85rem', color: '#166534' }}>Notified Responders</div>
                    <div style={{ fontSize: '1.35rem', fontWeight: 800, color: '#166534' }}>
                      {sosStatus.successfulNotifications}
                    </div>
                  </div>
                </div>

                <div style={{ marginTop: '1rem' }}>
                  <div style={{ fontSize: '0.85rem', color: '#64748B', marginBottom: '0.35rem' }}>
                    Alerted Groups:
                  </div>
                  <div style={{ display: 'flex', flexWrap: 'wrap', gap: '0.5rem' }}>
                    {sosStatus.targets.map((t) => (
                      <span
                        key={t}
                        style={{
                          backgroundColor: '#EFF6FF',
                          color: '#1E40AF',
                          padding: '0.35rem 0.75rem',
                          borderRadius: '9999px',
                          fontSize: '0.85rem',
                          fontWeight: 700,
                          border: '1px solid #BFDBFE',
                        }}
                      >
                        {t === 'CAREGIVER' && '✓ Caregiver (पालक)'}
                        {t === 'FACILITY' && '✓ Hospital / PHC (रुग्णालय)'}
                        {t === 'NEARBY_PEOPLE' && '✓ Nearby Helpers (मदतनीस)'}
                      </span>
                    ))}
                  </div>
                </div>
              </div>

              {/* Immediate Telephone Assistance */}
              <div
                style={{
                  backgroundColor: '#FEF2F2',
                  border: '1px solid #FCA5A5',
                  borderRadius: '0.75rem',
                  padding: '1rem',
                  display: 'flex',
                  alignItems: 'center',
                  gap: '1rem',
                  marginBottom: '1.5rem',
                }}
              >
                <PhoneCall size={32} color="#DC2626" />
                <div>
                  <div style={{ fontWeight: 800, color: '#991B1B' }}>
                    Dial Emergency Helpline Direct:
                  </div>
                  <div style={{ fontSize: '0.95rem', color: '#7F1D1D' }}>
                    Ambulance (रुग्णवाहिका): <strong>108</strong> • National Emergency: <strong>112</strong>
                  </div>
                </div>
              </div>

              {/* Resolve / Close Actions */}
              <div style={{ display: 'flex', flexDirection: 'column', gap: '0.75rem' }}>
                {sosStatus.status !== 'RESOLVED' && (
                  <button
                    onClick={handleResolveSos}
                    disabled={isResolving}
                    style={{
                      padding: '1rem',
                      fontSize: '1.15rem',
                      fontWeight: 700,
                      backgroundColor: '#166534',
                      color: '#FFFFFF',
                      border: 'none',
                      borderRadius: '0.75rem',
                      cursor: 'pointer',
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'center',
                      gap: '0.5rem',
                      boxShadow: '0 4px 6px -1px rgba(0, 0, 0, 0.1)',
                    }}
                  >
                    <CheckCircle2 size={24} />
                    <span>{isResolving ? 'Resolving...' : 'I Am Safe • Mark as Resolved (मी सुरक्षित आहे)'}</span>
                  </button>
                )}

                <button
                  onClick={onClose}
                  style={{
                    padding: '0.85rem',
                    fontSize: '1rem',
                    fontWeight: 600,
                    backgroundColor: '#F1F5F9',
                    color: '#334155',
                    border: '1px solid #CBD5E1',
                    borderRadius: '0.75rem',
                    cursor: 'pointer',
                  }}
                >
                  Close Window • विंडो बंद करा
                </button>
              </div>
            </div>
          ) : (
            /* Setup and Trigger Flow */
            <div>
              {/* Emergency Banner */}
              <div
                style={{
                  backgroundColor: '#FEF2F2',
                  border: '1px solid #FCA5A5',
                  borderRadius: '0.75rem',
                  padding: '1rem',
                  marginBottom: '1.25rem',
                  display: 'flex',
                  alignItems: 'flex-start',
                  gap: '0.75rem',
                }}
              >
                <AlertTriangle size={24} color="#DC2626" style={{ flexShrink: 0, marginTop: '2px' }} />
                <div style={{ fontSize: '0.95rem', color: '#7F1D1D' }}>
                  <strong>Only use in genuine medical emergencies.</strong> An immediate alert will be broadcasted with your device location.
                  <div style={{ fontSize: '0.85rem', marginTop: '0.2rem', color: '#991B1B' }}>
                    केवळ आणीबाणीच्या वेळीच वापरा. आपल्या सध्याच्या स्थानासह इशारा पाठवला जाईल.
                  </div>
                </div>
              </div>

              {/* Patient Selection if multiple */}
              {availablePatients.length > 1 && !preselectedPatientId && (
                <div style={{ marginBottom: '1.25rem' }}>
                  <label
                    style={{
                      display: 'block',
                      fontWeight: 700,
                      fontSize: '0.95rem',
                      color: '#1E293B',
                      marginBottom: '0.4rem',
                    }}
                  >
                    Triggering for Patient • रुग्ण निवडा:
                  </label>
                  <select
                    value={patientId}
                    onChange={(e) => {
                      setPatientId(e.target.value);
                      const p = availablePatients.find((x) => x.id === e.target.value);
                      if (p) setPatientName(p.fullName);
                    }}
                    style={{
                      width: '100%',
                      padding: '0.75rem',
                      borderRadius: '0.5rem',
                      border: '2px solid #CBD5E1',
                      fontSize: '1rem',
                    }}
                  >
                    {availablePatients.map((p) => (
                      <option key={p.id} value={p.id}>
                        {p.fullName} ({p.gender}, Age: {p.age}) - {p.villageTown}
                      </option>
                    ))}
                  </select>
                </div>
              )}

              {/* Step 1: Device Location Status */}
              <div
                style={{
                  backgroundColor: '#F8FAFC',
                  border: '1px solid #E2E8F0',
                  borderRadius: '0.75rem',
                  padding: '1rem',
                  marginBottom: '1.25rem',
                }}
              >
                <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
                  <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                    <MapPin size={22} color="#DC2626" />
                    <span style={{ fontWeight: 700, fontSize: '1rem', color: '#1E293B' }}>
                      Device Location • तुमचे स्थान
                    </span>
                  </div>

                  {locationStatus === 'detecting' && (
                    <span style={{ fontSize: '0.85rem', color: '#C05621', display: 'flex', alignItems: 'center', gap: '0.3rem' }}>
                      <RefreshCw size={14} className="animate-spin" /> Detecting GPS...
                    </span>
                  )}

                  {locationStatus === 'detected' && coords && (
                    <span style={{ fontSize: '0.85rem', color: '#166534', fontWeight: 700, display: 'flex', alignItems: 'center', gap: '0.3rem' }}>
                      <CheckCircle2 size={16} color="#166534" /> Live Coordinates ({coords.lat.toFixed(4)}, {coords.lng.toFixed(4)})
                    </span>
                  )}

                  {(locationStatus === 'denied' || locationStatus === 'error') && (
                    <button
                      onClick={detectLocation}
                      style={{
                        background: 'none',
                        border: 'none',
                        color: '#DC2626',
                        fontSize: '0.85rem',
                        fontWeight: 700,
                        cursor: 'pointer',
                        textDecoration: 'underline',
                      }}
                    >
                      Retry GPS • पुन्हा प्रयत्न करा
                    </button>
                  )}
                </div>

                {/* FCM Push Status Indicator */}
                <div style={{ marginTop: '0.6rem', display: 'flex', alignItems: 'center', gap: '0.4rem', fontSize: '0.85rem' }}>
                  <BellRing size={16} color={fcmRegistered ? "#166534" : "#64748B"} />
                  {fcmRegistered ? (
                    <span style={{ color: '#166534', fontWeight: 600 }}>
                      ✓ FCM Web Push Enabled • डिव्हाइस पुश सूचना सक्रिय
                    </span>
                  ) : (
                    <span style={{ color: '#64748B' }}>
                      FCM Web Push Ready (Connecting token with Firebase...)
                    </span>
                  )}
                </div>

                {locationError && (
                  <div
                    style={{
                      marginTop: '0.5rem',
                      padding: '0.6rem 0.8rem',
                      backgroundColor: '#FEF2F2',
                      border: '1px solid #FCA5A5',
                      borderRadius: '0.5rem',
                      fontSize: '0.85rem',
                      color: '#991B1B',
                    }}
                  >
                    {locationError}
                  </div>
                )}
              </div>

              {/* Step 2: Choose Target Groups */}
              <div style={{ marginBottom: '1.25rem' }}>
                <label
                  style={{
                    display: 'block',
                    fontWeight: 700,
                    fontSize: '1rem',
                    color: '#1E293B',
                    marginBottom: '0.5rem',
                  }}
                >
                  Who should receive this SOS? • कोणाला इशारा पाठवायचा?
                </label>

                <div style={{ display: 'flex', flexDirection: 'column', gap: '0.75rem' }}>
                  {/* Caregiver option */}
                  <label
                    style={{
                      display: 'flex',
                      alignItems: 'center',
                      gap: '0.75rem',
                      padding: '0.85rem 1rem',
                      backgroundColor: selectedTargets.includes('CAREGIVER') ? '#EFF6FF' : '#F8FAFC',
                      border: `2px solid ${selectedTargets.includes('CAREGIVER') ? '#3B82F6' : '#E2E8F0'}`,
                      borderRadius: '0.75rem',
                      cursor: 'pointer',
                    }}
                  >
                    <input
                      type="checkbox"
                      checked={selectedTargets.includes('CAREGIVER')}
                      onChange={() => handleToggleTarget('CAREGIVER')}
                      style={{ width: '22px', height: '22px', accentColor: '#3B82F6' }}
                    />
                    <HeartHandshake size={24} color="#2563EB" />
                    <div>
                      <div style={{ fontWeight: 700, fontSize: '1rem', color: '#1E293B' }}>
                        Caregiver / Family • पालक किंवा कुटुंब
                      </div>
                      <div style={{ fontSize: '0.85rem', color: '#64748B' }}>
                        Notifies registered emergency contact/caregiver.
                      </div>
                    </div>
                  </label>

                  {/* Facility option */}
                  <label
                    style={{
                      display: 'flex',
                      alignItems: 'center',
                      gap: '0.75rem',
                      padding: '0.85rem 1rem',
                      backgroundColor: selectedTargets.includes('FACILITY') ? '#EFF6FF' : '#F8FAFC',
                      border: `2px solid ${selectedTargets.includes('FACILITY') ? '#3B82F6' : '#E2E8F0'}`,
                      borderRadius: '0.75rem',
                      cursor: 'pointer',
                    }}
                  >
                    <input
                      type="checkbox"
                      checked={selectedTargets.includes('FACILITY')}
                      onChange={() => handleToggleTarget('FACILITY')}
                      style={{ width: '22px', height: '22px', accentColor: '#3B82F6' }}
                    />
                    <Building2 size={24} color="#2563EB" />
                    <div>
                      <div style={{ fontWeight: 700, fontSize: '1rem', color: '#1E293B' }}>
                        Nearby Hospital / Facility • जवळचे रुग्णालय
                      </div>
                      <div style={{ fontSize: '0.85rem', color: '#64748B' }}>
                        Alerts the designated emergency desk at the selected facility.
                      </div>
                    </div>
                  </label>

                  {/* Nearby People option */}
                  <label
                    style={{
                      display: 'flex',
                      alignItems: 'center',
                      gap: '0.75rem',
                      padding: '0.85rem 1rem',
                      backgroundColor: selectedTargets.includes('NEARBY_PEOPLE') ? '#EFF6FF' : '#F8FAFC',
                      border: `2px solid ${selectedTargets.includes('NEARBY_PEOPLE') ? '#3B82F6' : '#E2E8F0'}`,
                      borderRadius: '0.75rem',
                      cursor: 'pointer',
                    }}
                  >
                    <input
                      type="checkbox"
                      checked={selectedTargets.includes('NEARBY_PEOPLE')}
                      onChange={() => handleToggleTarget('NEARBY_PEOPLE')}
                      style={{ width: '22px', height: '22px', accentColor: '#3B82F6' }}
                    />
                    <Users size={24} color="#2563EB" />
                    <div>
                      <div style={{ fontWeight: 700, fontSize: '1rem', color: '#1E293B' }}>
                        Nearby Opted-In Helpers (5 KM) • ५ किमी आतील मदतनीस
                      </div>
                      <div style={{ fontSize: '0.85rem', color: '#64748B' }}>
                        Only verified app users within strict 5 km radius who opted in to help.
                      </div>
                    </div>
                  </label>
                </div>
              </div>

              {/* Step 3: Select Facility (if FACILITY target is checked) */}
              {selectedTargets.includes('FACILITY') && (
                <div
                  style={{
                    backgroundColor: '#F0F9FF',
                    border: '2px solid #BAE6FD',
                    borderRadius: '0.75rem',
                    padding: '1rem',
                    marginBottom: '1.25rem',
                  }}
                >
                  <label
                    style={{
                      display: 'block',
                      fontWeight: 700,
                      fontSize: '0.95rem',
                      color: '#0369A1',
                      marginBottom: '0.5rem',
                    }}
                  >
                    Select ONE Hospital / Health Center • एक रुग्णालय निवडा:
                  </label>

                  {isLoadingFacilities ? (
                    <div style={{ fontSize: '0.9rem', color: '#0369A1', display: 'flex', alignItems: 'center', gap: '0.4rem' }}>
                      <RefreshCw size={14} className="animate-spin" /> Loading nearby facilities...
                    </div>
                  ) : nearbyFacilities.length === 0 ? (
                    <div style={{ fontSize: '0.9rem', color: '#64748B' }}>
                      No facilities found in database.
                    </div>
                  ) : (
                    <div style={{ display: 'flex', flexDirection: 'column', gap: '0.5rem', maxHeight: '180px', overflowY: 'auto' }}>
                      {nearbyFacilities.map((f) => {
                        const isSelected = selectedFacilityId === f.facilityId;
                        return (
                          <div
                            key={f.facilityId}
                            onClick={() => setSelectedFacilityId(f.facilityId)}
                            style={{
                              padding: '0.65rem 0.85rem',
                              backgroundColor: isSelected ? '#FFFFFF' : '#F8FAFC',
                              border: `2px solid ${isSelected ? '#0284C7' : '#E2E8F0'}`,
                              borderRadius: '0.5rem',
                              cursor: 'pointer',
                              display: 'flex',
                              alignItems: 'center',
                              justifyContent: 'space-between',
                            }}
                          >
                            <div>
                              <div style={{ fontWeight: 700, fontSize: '0.95rem', color: '#0F172A' }}>
                                {f.name}
                              </div>
                              <div style={{ fontSize: '0.8rem', color: '#64748B' }}>
                                {f.facilityType} • {f.villageTown || f.district || ''}
                                {f.distanceKm !== undefined && ` • ${f.distanceKm.toFixed(1)} km away`}
                              </div>
                            </div>
                            {isSelected && <CheckCircle2 size={20} color="#0284C7" />}
                          </div>
                        );
                      })}
                    </div>
                  )}
                </div>
              )}

              {/* Error Message if any */}
              {submitError && (
                <div
                  style={{
                    backgroundColor: '#FEF2F2',
                    border: '1px solid #FCA5A5',
                    borderRadius: '0.5rem',
                    padding: '0.75rem',
                    color: '#991B1B',
                    fontSize: '0.9rem',
                    marginBottom: '1.25rem',
                    display: 'flex',
                    alignItems: 'center',
                    gap: '0.5rem',
                  }}
                >
                  <AlertCircle size={18} color="#991B1B" />
                  <span>{submitError}</span>
                </div>
              )}

              {/* Big Red SOS Button */}
              <button
                onClick={handleTriggerSos}
                disabled={isSubmitting || locationStatus === 'denied' || locationStatus === 'detecting' || selectedTargets.length === 0}
                style={{
                  width: '100%',
                  padding: '1.35rem 1.5rem',
                  fontSize: '1.35rem',
                  fontWeight: 900,
                  letterSpacing: '0.5px',
                  backgroundColor: (isSubmitting || locationStatus === 'denied' || selectedTargets.length === 0)
                    ? '#94A3B8'
                    : '#DC2626',
                  color: '#FFFFFF',
                  border: 'none',
                  borderRadius: '1rem',
                  cursor: (isSubmitting || locationStatus === 'denied' || selectedTargets.length === 0)
                    ? 'not-allowed'
                    : 'pointer',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  gap: '0.75rem',
                  boxShadow: '0 10px 25px -5px rgba(220, 38, 38, 0.4)',
                  transition: 'all 0.2s ease',
                }}
              >
                {isSubmitting ? (
                  <>
                    <RefreshCw size={28} className="animate-spin" />
                    <span>DISPATCHING SOS... • पाठवत आहे...</span>
                  </>
                ) : (
                  <>
                    <ShieldAlert size={32} />
                    <span>TRIGGER SOS • आणीबाणी इशारा पाठवा</span>
                  </>
                )}
              </button>

              {/* Helpline Quick Dial */}
              <div
                style={{
                  marginTop: '1.25rem',
                  textAlign: 'center',
                  fontSize: '0.95rem',
                  color: '#64748B',
                }}
              >
                Emergency Services Direct: Ambulance <strong>108</strong> | National Helpline <strong>112</strong>
              </div>
            </div>
          )}
        </div>
      </div>
    </div>
  );
};
