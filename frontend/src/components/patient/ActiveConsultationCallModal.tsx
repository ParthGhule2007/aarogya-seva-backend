import React, { useState, useEffect } from 'react';
import {
  Video,
  Phone,
  PhoneOff,
  ExternalLink,
  AlertCircle,
  Clock,
  User,
  ShieldCheck,
  CheckCircle2,
} from 'lucide-react';
import { consultationApi } from '../../api/consultationApi';
import { ConsultationResponseDTO } from '../../types/consultation';

interface ActiveConsultationCallModalProps {
  consultation: ConsultationResponseDTO;
  onClose: () => void;
  onCompleted: (consultation: ConsultationResponseDTO) => void;
}

export const ActiveConsultationCallModal: React.FC<ActiveConsultationCallModalProps> = ({
  consultation,
  onClose,
  onCompleted,
}) => {
  const [seconds, setSeconds] = useState(0);
  const [isEnding, setIsEnding] = useState(false);
  const [endError, setEndError] = useState<string | null>(null);

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
    return `${mins.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}`;
  };

  const handleEndCall = async () => {
    if (!window.confirm('Do you want to end and complete this consultation? (आपण हा सल्ला संपवू इच्छिता का?)')) {
      return;
    }
    setIsEnding(true);
    setEndError(null);
    try {
      // Calls Consultation Service PUT /api/consultations/{id}/complete
      // Which completes consultation, releases doctor in Doctor Service, and ends session in Communication Service
      const updated = await consultationApi.completeConsultation(consultation.id);
      onCompleted(updated);
    } catch (err: any) {
      console.error('Failed to complete consultation:', err);
      setEndError(
        err.message || 'Connection problem while ending consultation. Please try again.'
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
        backgroundColor: 'rgba(15, 23, 42, 0.92)',
        zIndex: 200,
        display: 'flex',
        flexDirection: 'column',
      }}
    >
      {/* Top Header Bar */}
      <div
        style={{
          backgroundColor: '#0F172A',
          color: '#FFFFFF',
          padding: '1rem 1.5rem',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
          borderBottom: '2px solid #334155',
          flexWrap: 'wrap',
          gap: '1rem',
        }}
      >
        <div style={{ display: 'flex', alignItems: 'center', gap: '1rem' }}>
          <div
            style={{
              width: '46px',
              height: '46px',
              borderRadius: '50%',
              backgroundColor: 'var(--secondary)',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              color: '#FFFFFF',
              fontWeight: 800,
              fontSize: '1.2rem',
            }}
          >
            Dr
          </div>
          <div>
            <h2
              id="active-call-title"
              style={{ fontSize: '1.25rem', fontWeight: 800, margin: 0, color: '#FFFFFF' }}
            >
              {consultation.doctorName || `Doctor #${consultation.doctorId}`}
            </h2>
            <div style={{ fontSize: '0.9rem', color: '#94A3B8' }}>
              Consultation in Progress • सल्ला सुरू आहे
            </div>
          </div>
        </div>

        <div style={{ display: 'flex', alignItems: 'center', gap: '1rem' }}>
          <div
            style={{
              display: 'inline-flex',
              alignItems: 'center',
              gap: '0.4rem',
              backgroundColor: '#1E293B',
              padding: '0.4rem 0.85rem',
              borderRadius: 'var(--radius-full)',
              fontSize: '1rem',
              fontWeight: 700,
              color: '#38BDF8',
            }}
          >
            <Clock size={16} />
            <span>{formatTime(seconds)}</span>
          </div>

          {consultation.meetingUrl && (
            <a
              href={consultation.meetingUrl}
              target="_blank"
              rel="noopener noreferrer"
              className="btn-large btn-outline"
              style={{
                color: '#FFFFFF',
                borderColor: '#475569',
                padding: '0.5rem 0.85rem',
                fontSize: '0.85rem',
                textDecoration: 'none',
              }}
            >
              <ExternalLink size={16} />
              <span>Open in Full Screen • पूर्ण पडद्यावर</span>
            </a>
          )}
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

      {/* Main Call View Area */}
      <div style={{ flex: 1, position: 'relative', backgroundColor: '#000000' }}>
        {consultation.meetingUrl ? (
          <iframe
            src={consultation.meetingUrl}
            title="Teleconsultation Video Session"
            style={{
              width: '100%',
              height: '100%',
              border: 'none',
              display: 'block',
            }}
            allow="camera; microphone; display-capture; autoplay"
          />
        ) : (
          <div
            style={{
              color: '#FFFFFF',
              textAlign: 'center',
              padding: '3rem 1.5rem',
              maxWidth: '480px',
              margin: '0 auto',
            }}
          >
            <AlertCircle size={48} color="#F87171" style={{ margin: '0 auto 1rem auto' }} />
            <h3 style={{ fontSize: '1.3rem', marginBottom: '0.5rem' }}>
              Connection Problem • संपर्क समस्या
            </h3>
            <p style={{ color: '#94A3B8' }}>
              The consultation meeting room could not be loaded. Please try again.
            </p>
          </div>
        )}
      </div>

      {/* Bottom Control Bar */}
      <div
        style={{
          backgroundColor: '#0F172A',
          padding: '1.25rem 1.5rem',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          gap: '1.5rem',
          borderTop: '2px solid #334155',
        }}
      >
        <button
          className="btn-large"
          style={{
            backgroundColor: '#DC2626',
            color: '#FFFFFF',
            border: 'none',
            borderRadius: 'var(--radius-full)',
            padding: '1rem 2.5rem',
            fontSize: '1.2rem',
            fontWeight: 800,
            cursor: 'pointer',
            display: 'flex',
            alignItems: 'center',
            gap: '0.75rem',
            boxShadow: '0 4px 15px rgba(220, 38, 38, 0.4)',
          }}
          onClick={handleEndCall}
          disabled={isEnding}
          aria-label="End Consultation"
        >
          <PhoneOff size={26} />
          <span>
            {isEnding
              ? 'Ending Consultation...'
              : 'End Consultation • सल्ला संपवा'}
          </span>
        </button>
      </div>
    </div>
  );
};
