import React, { useState, useEffect, useRef } from 'react';
import {
  Video,
  PhoneOff,
  ExternalLink,
  AlertCircle,
  Clock,
  User,
  ShieldCheck,
  RefreshCw,
  Camera,
  Mic,
  Maximize2,
} from 'lucide-react';
import { consultationApi } from '../../api/consultationApi';
import { ConsultationResponseDTO } from '../../types/consultation';

export interface ActiveConsultationCallModalProps {
  consultation: ConsultationResponseDTO;
  role?: 'PATIENT' | 'DOCTOR';
  participantName?: string;
  onClose: () => void;
  onCompleted?: (consultation: ConsultationResponseDTO) => void;
}

export const ActiveConsultationCallModal: React.FC<ActiveConsultationCallModalProps> = ({
  consultation,
  role = 'PATIENT',
  participantName: initialParticipantName,
  onClose,
  onCompleted,
}) => {
  const [seconds, setSeconds] = useState(0);
  const [isEnding, setIsEnding] = useState(false);
  const [endError, setEndError] = useState<string | null>(null);
  const [iframeKey, setIframeKey] = useState(0);
  const [isLoadingCall, setIsLoadingCall] = useState(true);

  // Resolved display name
  const participantName =
    initialParticipantName ||
    (role === 'DOCTOR'
      ? consultation.doctorName || 'Doctor'
      : consultation.username || 'Patient');

  // Resolved Jitsi domain from environment variable or default
  const jitsiDomain = (
    (import.meta.env.VITE_JITSI_DOMAIN as string) || 'meet.jit.si'
  )
    .replace(/^https?:\/\//, '')
    .replace(/\/+$/, '');

  // Persistent room ID correlation key
  const roomId =
    consultation.roomId || `consultation-${consultation.id}`;

  // Formatted Jitsi URL with prejoin disabled and participant name passed
  const cleanBase = consultation.meetingUrl
    ? consultation.meetingUrl.split('#')[0]
    : `https://${jitsiDomain}/${roomId}`;

  const meetingUrlWithParams = `${cleanBase}#userInfo.displayName=${encodeURIComponent(
    participantName
  )}&config.prejoinPageEnabled=false&config.startWithAudioMuted=false&config.startWithVideoMuted=false&config.disableDeepLinking=true`;

  // Call duration counter
  useEffect(() => {
    const timer = setInterval(() => {
      setSeconds((prev) => prev + 1);
    }, 1000);
    return () => clearInterval(timer);
  }, []);

  const formatTime = (totalSeconds: number) => {
    const mins = Math.floor(totalSeconds / 60);
    const secs = totalSeconds % 60;
    return `${mins.toString().padStart(2, '0')}:${secs
      .toString()
      .padStart(2, '0')}`;
  };

  const handleReconnect = () => {
    setIsLoadingCall(true);
    setIframeKey((prev) => prev + 1);
  };

  const handleEndCall = async () => {
    const confirmMessage =
      role === 'DOCTOR'
        ? 'Do you want to end and complete this consultation? (आपण हा सल्ला संपवून पूर्ण करू इच्छिता का?)'
        : 'Do you want to leave and mark consultation complete? (आपण हा सल्ला संपवू इच्छिता का?)';

    if (!window.confirm(confirmMessage)) {
      return;
    }

    setIsEnding(true);
    setEndError(null);
    try {
      const updated = await consultationApi.completeConsultation(consultation.id);
      if (onCompleted) {
        onCompleted(updated);
      } else {
        onClose();
      }
    } catch (err: any) {
      console.error('Failed to complete consultation:', err);
      setEndError(
        err.message ||
          'Connection problem while ending consultation. Please try again.'
      );
      setIsEnding(false);
    }
  };

  return (
    <div
      role="dialog"
      aria-modal="true"
      aria-labelledby="active-call-title"
      style={{
        position: 'fixed',
        top: 0,
        left: 0,
        right: 0,
        bottom: 0,
        backgroundColor: 'rgba(15, 23, 42, 0.96)',
        zIndex: 250,
        display: 'flex',
        flexDirection: 'column',
      }}
    >
      {/* Top Header Bar */}
      <div
        style={{
          backgroundColor: '#0F172A',
          color: '#FFFFFF',
          padding: '0.85rem 1.5rem',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
          borderBottom: '2px solid #334155',
          flexWrap: 'wrap',
          gap: '1rem',
        }}
      >
        {/* Left Side: Participant Identity & Doctor/Patient Info */}
        <div style={{ display: 'flex', alignItems: 'center', gap: '0.85rem' }}>
          <div
            style={{
              width: '44px',
              height: '44px',
              borderRadius: '50%',
              backgroundColor:
                role === 'DOCTOR' ? '#0D9488' : 'var(--secondary, #059669)',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              color: '#FFFFFF',
              fontWeight: 800,
              fontSize: '1.1rem',
            }}
          >
            {role === 'DOCTOR' ? 'Dr' : 'Pt'}
          </div>
          <div>
            <h2
              id="active-call-title"
              style={{
                fontSize: '1.15rem',
                fontWeight: 800,
                margin: 0,
                color: '#FFFFFF',
              }}
            >
              {role === 'DOCTOR'
                ? `Patient: ${consultation.username || 'Patient Case #' + consultation.id}`
                : `Doctor: ${consultation.doctorName || 'Doctor #' + consultation.doctorId}`}
            </h2>
            <div style={{ fontSize: '0.85rem', color: '#94A3B8', display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
              <span>Joined as <strong>{participantName}</strong> ({role})</span>
              <span>•</span>
              <span>Room: {roomId.slice(0, 22)}...</span>
            </div>
          </div>
        </div>

        {/* Right Side: Timer, Reconnect, Fullscreen External Link */}
        <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem', flexWrap: 'wrap' }}>
          {/* Duration Badge */}
          <div
            style={{
              display: 'inline-flex',
              alignItems: 'center',
              gap: '0.4rem',
              backgroundColor: '#1E293B',
              padding: '0.35rem 0.75rem',
              borderRadius: '9999px',
              fontSize: '0.95rem',
              fontWeight: 700,
              color: '#38BDF8',
              border: '1px solid #334155',
            }}
          >
            <Clock size={16} />
            <span>{formatTime(seconds)}</span>
          </div>

          {/* Reconnect button */}
          <button
            onClick={handleReconnect}
            title="Reconnect video session"
            style={{
              background: '#1E293B',
              border: '1px solid #334155',
              color: '#F8FAFC',
              borderRadius: '8px',
              padding: '0.4rem 0.75rem',
              fontSize: '0.85rem',
              cursor: 'pointer',
              display: 'inline-flex',
              alignItems: 'center',
              gap: '0.35rem',
              fontWeight: 600,
            }}
          >
            <RefreshCw size={14} />
            <span>Reconnect</span>
          </button>

          {/* Open In New Window for browser restrictions / mobile */}
          <a
            href={meetingUrlWithParams}
            target="_blank"
            rel="noopener noreferrer"
            style={{
              color: '#FFFFFF',
              border: '1px solid #475569',
              background: 'rgba(255,255,255,0.08)',
              padding: '0.4rem 0.75rem',
              fontSize: '0.85rem',
              textDecoration: 'none',
              borderRadius: '8px',
              display: 'inline-flex',
              alignItems: 'center',
              gap: '0.35rem',
              fontWeight: 600,
            }}
          >
            <Maximize2 size={14} />
            <span>Full Screen Window</span>
          </a>
        </div>
      </div>

      {/* Permission & Inactivity Helper Banner */}
      <div
        style={{
          backgroundColor: '#1E293B',
          color: '#CBD5E1',
          padding: '0.5rem 1.5rem',
          fontSize: '0.85rem',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
          borderBottom: '1px solid #334155',
          flexWrap: 'wrap',
          gap: '0.5rem',
        }}
      >
        <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
          <ShieldCheck size={16} color="#10B981" />
          <span>
            Encrypted Rural Teleconsultation • Ensure camera & microphone access are enabled.
          </span>
        </div>
        <div style={{ display: 'flex', alignItems: 'center', gap: '1rem', fontSize: '0.8rem', color: '#94A3B8' }}>
          <span style={{ display: 'inline-flex', alignItems: 'center', gap: '0.25rem' }}>
            <Camera size={14} /> Camera Active
          </span>
          <span style={{ display: 'inline-flex', alignItems: 'center', gap: '0.25rem' }}>
            <Mic size={14} /> Mic Active
          </span>
        </div>
      </div>

      {endError && (
        <div
          className="alert-box alert-danger"
          style={{ margin: '0.75rem 1.5rem', padding: '0.75rem 1rem' }}
        >
          <AlertCircle size={20} />
          <span>{endError}</span>
        </div>
      )}

      {/* Main Jitsi Call Container */}
      <div style={{ flex: 1, position: 'relative', backgroundColor: '#000000', overflow: 'hidden' }}>
        {isLoadingCall && (
          <div
            style={{
              position: 'absolute',
              top: 0,
              left: 0,
              right: 0,
              bottom: 0,
              display: 'flex',
              flexDirection: 'column',
              alignItems: 'center',
              justifyContent: 'center',
              backgroundColor: '#0F172A',
              color: '#FFFFFF',
              zIndex: 10,
              gap: '1rem',
            }}
          >
            <div
              style={{
                width: '44px',
                height: '44px',
                borderRadius: '50%',
                border: '4px solid #38BDF8',
                borderTopColor: 'transparent',
                animation: 'spin 1s linear infinite',
              }}
            />
            <div style={{ fontSize: '1.15rem', fontWeight: 700 }}>
              Connecting to secure consultation room...
            </div>
            <div style={{ color: '#94A3B8', fontSize: '0.9rem' }}>
              कृपया थांबा, सुरक्षित व्हिडिओ रूम सुरू होत आहे...
            </div>
          </div>
        )}

        <iframe
          key={iframeKey}
          src={meetingUrlWithParams}
          title="Teleconsultation Video Session"
          style={{
            width: '100%',
            height: '100%',
            border: 'none',
            display: 'block',
          }}
          allow="camera; microphone; display-capture; autoplay; clipboard-write"
          onLoad={() => setIsLoadingCall(false)}
        />
      </div>

      {/* Bottom Controls Bar */}
      <div
        style={{
          backgroundColor: '#0F172A',
          padding: '1rem 1.5rem',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          gap: '1.5rem',
          borderTop: '2px solid #334155',
          flexWrap: 'wrap',
        }}
      >
        <button
          style={{
            backgroundColor: '#DC2626',
            color: '#FFFFFF',
            border: 'none',
            borderRadius: '9999px',
            padding: '0.85rem 2.25rem',
            fontSize: '1.1rem',
            fontWeight: 800,
            cursor: isEnding ? 'not-allowed' : 'pointer',
            display: 'flex',
            alignItems: 'center',
            gap: '0.75rem',
            boxShadow: '0 4px 15px rgba(220, 38, 38, 0.4)',
            transition: 'transform 0.1s ease',
          }}
          onClick={handleEndCall}
          disabled={isEnding}
          aria-label="End and Complete Consultation"
        >
          <PhoneOff size={24} />
          <span>
            {isEnding
              ? 'Completing Consultation...'
              : 'End & Complete Consultation • सल्ला संपवा'}
          </span>
        </button>

        <button
          onClick={onClose}
          style={{
            background: 'transparent',
            color: '#94A3B8',
            border: '1px solid #475569',
            borderRadius: '9999px',
            padding: '0.85rem 1.5rem',
            fontSize: '0.95rem',
            fontWeight: 600,
            cursor: 'pointer',
          }}
        >
          Minimize Window • विंडो लहान करा
        </button>
      </div>
    </div>
  );
};
