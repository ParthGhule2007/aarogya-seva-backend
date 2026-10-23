import React, { useState } from 'react';
import { useConsultation } from '../../hooks/useConsultation';
import { useAuth } from '../../context/AuthContext';
import { PhoneOff, Video, VideoOff, Mic, MicOff, Wifi, User, Stethoscope, Maximize2 } from 'lucide-react';

export const JitsiCallModal: React.FC = () => {
  const { activeCallSession, completeConsultation, endCallSession } = useConsultation();
  const { user } = useAuth();
  const [isAudioOnly, setIsAudioOnly] = useState(false);
  const [isCompleting, setIsCompleting] = useState(false);

  if (!activeCallSession) return null;

  const handleEndCall = async () => {
    setIsCompleting(true);
    try {
      if (user?.role === 'DOCTOR') {
        await completeConsultation(activeCallSession.consultationId);
      } else {
        endCallSession();
      }
    } catch {
      endCallSession();
    } finally {
      setIsCompleting(false);
    }
  };

  // Construct Jitsi URL with config params
  const rawUrl = activeCallSession.meetingUrl;
  const callUrl = isAudioOnly
    ? `${rawUrl}#config.startWithVideoMuted=true&config.resolution=180`
    : rawUrl;

  return (
    <div className="modal-overlay" style={{ padding: 0 }} role="dialog" aria-modal="true">
      <div
        className="modal-dialog"
        style={{
          maxWidth: '100vw',
          width: '100vw',
          height: '100vh',
          maxHeight: '100vh',
          borderRadius: 0,
          border: 'none',
          display: 'flex',
          flexDirection: 'column',
          background: '#0f172a',
        }}
      >
        {/* Call Header */}
        <div
          style={{
            padding: '0.75rem 1.25rem',
            background: '#1e293b',
            color: '#ffffff',
            display: 'flex',
            justifyContent: 'space-between',
            alignItems: 'center',
            borderBottom: '1px solid #334155',
          }}
        >
          <div style={{ display: 'flex', alignItems: 'center', gap: '1rem' }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: '0.4rem', fontWeight: 700 }}>
              <Stethoscope size={20} color="#2dd4bf" />
              <span>Dr. Consultation #{activeCallSession.consultationId}</span>
            </div>
            <div style={{ fontSize: '0.85rem', color: '#94a3b8' }}>
              Patient: <strong>{activeCallSession.patientName || 'Verified Patient'}</strong>
            </div>
          </div>

          <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
            {/* Low Bandwidth Audio-Only Mode */}
            <button
              className="btn btn-outline"
              style={{
                minHeight: '40px',
                padding: '0 0.85rem',
                fontSize: '0.85rem',
                borderColor: isAudioOnly ? '#2dd4bf' : '#64748b',
                color: isAudioOnly ? '#2dd4bf' : '#ffffff',
              }}
              onClick={() => setIsAudioOnly(!isAudioOnly)}
              title="Toggle low bandwidth mode"
            >
              <Wifi size={16} />
              <span>{isAudioOnly ? 'Audio-Only Mode (Low Bandwidth)' : 'Normal Mode'}</span>
            </button>

            {/* End Call Button */}
            <button
              className="btn btn-danger"
              style={{ minHeight: '40px', padding: '0 1rem', fontWeight: 700 }}
              onClick={handleEndCall}
              disabled={isCompleting}
            >
              <PhoneOff size={18} />
              <span>{isCompleting ? 'Ending Call...' : 'End Call'}</span>
            </button>
          </div>
        </div>

        {/* Video Call iFrame */}
        <div style={{ flex: 1, position: 'relative', width: '100%', height: '100%' }}>
          <iframe
            src={callUrl}
            title="Telemedicine Consultation Call"
            style={{ width: '100%', height: '100%', border: 'none' }}
            allow="camera; microphone; display-capture; autoplay; clipboard-write"
          />
        </div>
      </div>
    </div>
  );
};
