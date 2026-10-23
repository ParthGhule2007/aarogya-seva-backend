import React, { useState, useEffect } from 'react';
import { useCalling } from '../../hooks/useCalling';
import { useLanguage } from '../../context/LanguageContext';
import { useAuth } from '../../context/AuthContext';
import { VoiceLanguage } from '../../types/calling';
import {
  Phone,
  PhoneCall,
  PhoneForwarded,
  PhoneOff,
  Sparkles,
  ShieldCheck,
  AlertCircle,
  Volume2,
  History,
  Info,
} from 'lucide-react';

interface Props {
  defaultPhone?: string;
  defaultName?: string;
  onCallInitiated?: () => void;
}

export const AarogyaMitraVoiceCallCard: React.FC<Props> = ({
  defaultPhone = '',
  defaultName = '',
  onCallInitiated,
}) => {
  const { user } = useAuth();
  const { language } = useLanguage();

  const [inputPhone, setInputPhone] = useState(defaultPhone || user?.contactNumber || '');
  const [inputName, setInputName] = useState(defaultName || user?.displayName || '');
  const [selectedLang, setSelectedLang] = useState<VoiceLanguage>(
    language === 'mr' ? 'mr' : language === 'hi' ? 'hi' : 'en'
  );
  const [consent, setConsent] = useState(true);
  const [showHistory, setShowHistory] = useState(false);

  const {
    activeSession,
    profile,
    history,
    isLoading,
    isCallActive,
    error,
    startTalkToAi,
    startFollowUp,
    terminateCall,
    fetchProfile,
    fetchHistory,
    optOut,
    clearActiveSession,
  } = useCalling(inputPhone);

  useEffect(() => {
    if (inputPhone) {
      fetchProfile(inputPhone);
      fetchHistory(inputPhone);
    }
  }, [inputPhone, fetchProfile, fetchHistory]);

  const handleTalkToAi = async () => {
    if (!inputPhone) return;
    try {
      await startTalkToAi({
        phoneNumber: inputPhone,
        language: selectedLang,
        chosenName: inputName,
        consentToSaveSummary: consent,
      });
      if (onCallInitiated) onCallInitiated();
    } catch {
      // Error handled in hook
    }
  };

  const handleFollowUp = async () => {
    if (!inputPhone) return;
    try {
      await startFollowUp({
        phoneNumber: inputPhone,
        language: selectedLang,
      });
      if (onCallInitiated) onCallInitiated();
    } catch {
      // Error handled in hook
    }
  };

  return (
    <div className="card" style={{ padding: '1.75rem', borderRadius: 'var(--radius-lg)' }}>
      {/* Title */}
      <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem', marginBottom: '1.25rem' }}>
        <div
          style={{
            width: '46px',
            height: '46px',
            borderRadius: '50%',
            background: 'linear-gradient(135deg, #0d9488 0%, #059669 100%)',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            color: '#ffffff',
          }}
        >
          <PhoneCall size={24} />
        </div>
        <div>
          <h2 style={{ fontSize: '1.3rem', fontWeight: 800, margin: 0 }}>
            आरोग्य मित्र — AI Voice Healthcare Call
          </h2>
          <p style={{ fontSize: '0.85rem', color: 'var(--text-subtle)', margin: '0.2rem 0 0' }}>
            Speak directly to our AI voice health assistant in English, Hindi, or Marathi over a standard phone call.
          </p>
        </div>
      </div>

      {/* Error alert */}
      {error && (
        <div className="alert-box danger" style={{ marginBottom: '1rem' }}>
          <AlertCircle size={20} />
          <div style={{ fontSize: '0.9rem' }}>{error}</div>
        </div>
      )}

      {/* Active Call Live Status Monitor */}
      {activeSession && (
        <div
          style={{
            background: isCallActive ? 'rgba(13, 148, 136, 0.08)' : 'var(--surface-subtle)',
            border: `2px solid ${isCallActive ? 'var(--primary)' : 'var(--border-light)'}`,
            borderRadius: 'var(--radius-md)',
            padding: '1.25rem',
            marginBottom: '1.5rem',
          }}
        >
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: '0.5rem' }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
              <Volume2 size={22} color={isCallActive ? 'var(--primary)' : 'var(--text-subtle)'} />
              <span style={{ fontWeight: 800, fontSize: '1.05rem' }}>
                {activeSession.callType === 'FOLLOW_UP' ? 'Follow-up Check-in Call' : 'Aarogya Mitra Consultation Call'}
              </span>
            </div>
            <span
              className={`badge ${
                activeSession.status === 'IN_PROGRESS'
                  ? 'badge-routine'
                  : activeSession.status === 'RINGING' || activeSession.status === 'INITIATED'
                  ? 'badge-urgent'
                  : 'badge-emergency'
              }`}
              style={{ fontSize: '0.85rem', padding: '0.35rem 0.75rem' }}
            >
              ● {activeSession.status}
            </span>
          </div>

          <div style={{ fontSize: '0.95rem', color: 'var(--text-subtle)', marginTop: '0.5rem' }}>
            {activeSession.message ||
              (activeSession.status === 'RINGING'
                ? 'Your phone is ringing! Please answer the call to speak with Aarogya Mitra.'
                : activeSession.status === 'IN_PROGRESS'
                ? 'Call connected! The AI assistant is listening and conversing.'
                : activeSession.status === 'COMPLETED'
                ? 'Consultation call successfully finished.'
                : 'Session status: ' + activeSession.status)}
          </div>

          {activeSession.durationSeconds ? (
            <div style={{ fontSize: '0.85rem', color: 'var(--text-muted)', marginTop: '0.35rem' }}>
              Call duration: {activeSession.durationSeconds} seconds
            </div>
          ) : null}

          {isCallActive ? (
            <button
              type="button"
              className="btn btn-danger"
              style={{ marginTop: '1rem', minHeight: '44px', fontWeight: 700 }}
              onClick={terminateCall}
              disabled={isLoading}
            >
              <PhoneOff size={18} />
              <span>Hang Up / End Call</span>
            </button>
          ) : (
            <button
              type="button"
              className="btn btn-outline"
              style={{ marginTop: '0.85rem', minHeight: '38px', fontSize: '0.85rem' }}
              onClick={clearActiveSession}
            >
              Dismiss Status
            </button>
          )}
        </div>
      )}

      {/* Main form when no call active */}
      {!isCallActive && (
        <>
          {/* Language Selector */}
          <div className="form-group">
            <label className="form-label" style={{ fontWeight: 700 }}>
              Preferred Voice Language (बातचीत की भाषा चुनें):
            </label>
            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(140px, 1fr))', gap: '0.5rem' }}>
              {[
                { code: 'hi', label: 'हिन्दी (Hindi)' },
                { code: 'mr', label: 'मराठी (Marathi)' },
                { code: 'en', label: 'English' },
              ].map((l) => (
                <button
                  key={l.code}
                  type="button"
                  onClick={() => setSelectedLang(l.code as VoiceLanguage)}
                  className={`btn ${selectedLang === l.code ? 'btn-primary' : 'btn-outline'}`}
                  style={{ minHeight: '44px', fontWeight: selectedLang === l.code ? 700 : 500 }}
                >
                  {l.label}
                </button>
              ))}
            </div>
          </div>

          {/* Phone Number */}
          <div className="form-group">
            <label className="form-label" htmlFor="voice-phone" style={{ fontWeight: 700 }}>
              Destination Phone Number (फ़ोन नंबर जिसपर कॉल आएगा): *
            </label>
            <div style={{ position: 'relative' }}>
              <input
                id="voice-phone"
                type="tel"
                className="form-control"
                placeholder="e.g. 9876543210 or +919876543210"
                value={inputPhone}
                onChange={(e) => {
                  setInputPhone(e.target.value);
                  fetchProfile(e.target.value);
                }}
                required
              />
              <Phone
                size={18}
                style={{
                  position: 'absolute',
                  right: '12px',
                  top: '50%',
                  transform: 'translateY(-50%)',
                  color: 'var(--text-subtle)',
                  pointerEvents: 'none',
                }}
              />
            </div>
            <div style={{ fontSize: '0.8rem', color: 'var(--text-subtle)', marginTop: '0.25rem' }}>
              You will receive an actual phone call from Aarogya Mitra on this phone number.
            </div>
          </div>

          {/* Patient Name */}
          <div className="form-group">
            <label className="form-label" htmlFor="voice-name">
              Patient Name (मरीज़ का नाम):
            </label>
            <input
              id="voice-name"
              type="text"
              className="form-control"
              placeholder="e.g. Ramesh Patil"
              value={inputName}
              onChange={(e) => setInputName(e.target.value)}
            />
          </div>

          {/* Consent Checkbox */}
          <div style={{ display: 'flex', alignItems: 'flex-start', gap: '0.65rem', margin: '1rem 0' }}>
            <input
              type="checkbox"
              id="consent-voice"
              checked={consent}
              onChange={(e) => setConsent(e.target.checked)}
              style={{ width: '18px', height: '18px', marginTop: '2px', accentColor: 'var(--primary)' }}
            />
            <label htmlFor="consent-voice" style={{ fontSize: '0.85rem', color: 'var(--text-main)', cursor: 'pointer' }}>
              Save a confidential concern summary so I can request follow-up check-ins.
              (फॉलो-अप चेक-इन के लिए संक्षिप्त सारांश सहेजने की सहमति दें)
            </label>
          </div>

          {/* Follow-up Context Banner */}
          {profile && profile.firstCallCompleted && !profile.optedOut && (
            <div
              style={{
                background: 'rgba(5, 150, 105, 0.08)',
                border: '1px solid var(--primary)',
                borderRadius: 'var(--radius-md)',
                padding: '0.85rem 1rem',
                marginBottom: '1rem',
              }}
            >
              <div style={{ display: 'flex', alignItems: 'center', gap: '0.4rem', fontWeight: 700, fontSize: '0.85rem', color: 'var(--primary)' }}>
                <ShieldCheck size={16} />
                <span>Verified Patient Consultation on File</span>
              </div>
              {profile.concernSummary && (
                <div style={{ fontSize: '0.85rem', color: 'var(--text-main)', marginTop: '0.35rem' }}>
                  Recorded concern: <em>"{profile.concernSummary}"</em>
                </div>
              )}
            </div>
          )}

          {/* Two Buttons */}
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(220px, 1fr))', gap: '0.75rem', marginTop: '1.25rem' }}>
            {/* Button 1: Talk to AI */}
            <button
              type="button"
              className="btn btn-primary"
              style={{ minHeight: '52px', fontSize: '1rem', fontWeight: 800 }}
              onClick={handleTalkToAi}
              disabled={isLoading || !inputPhone}
            >
              <Sparkles size={18} />
              <span>Button 1: Talk to AI (कॉल शुरू करें)</span>
            </button>

            {/* Button 2: Follow-up Call */}
            <button
              type="button"
              className="btn btn-outline"
              style={{
                minHeight: '52px',
                fontSize: '0.95rem',
                fontWeight: 700,
                borderColor: 'var(--primary)',
              }}
              onClick={handleFollowUp}
              disabled={
                isLoading ||
                !inputPhone ||
                !profile ||
                !profile.firstCallCompleted ||
                profile.optedOut
              }
              title={
                !profile || !profile.firstCallCompleted
                  ? 'Complete an initial Talk to AI consultation first'
                  : profile.optedOut
                  ? 'Patient opted out'
                  : 'Place follow-up check-in'
              }
            >
              <PhoneForwarded size={18} />
              <span>Button 2: Follow-up Call (फॉलो-अप)</span>
            </button>
          </div>

          {/* History & Opt-out footer */}
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginTop: '1.5rem', flexWrap: 'wrap', gap: '0.5rem' }}>
            <button
              type="button"
              onClick={() => setShowHistory(!showHistory)}
              style={{
                background: 'none',
                border: 'none',
                color: 'var(--primary)',
                fontSize: '0.85rem',
                cursor: 'pointer',
                display: 'flex',
                alignItems: 'center',
                gap: '0.4rem',
                padding: 0,
                fontWeight: 600,
              }}
            >
              <History size={16} />
              <span>{showHistory ? 'Hide Call History' : 'View Past Calls'}</span>
            </button>

            {profile && !profile.optedOut && (
              <button
                type="button"
                onClick={() => optOut('Requested via web portal')}
                style={{
                  background: 'none',
                  border: 'none',
                  color: 'var(--text-muted)',
                  fontSize: '0.8rem',
                  cursor: 'pointer',
                  padding: 0,
                }}
              >
                Opt-out from calls
              </button>
            )}
          </div>

          {/* History */}
          {showHistory && (
            <div style={{ marginTop: '1rem', borderTop: '1px solid var(--border-light)', paddingTop: '0.75rem' }}>
              <div style={{ fontWeight: 700, fontSize: '0.9rem', marginBottom: '0.5rem' }}>Past Voice Calls:</div>
              {history.length === 0 ? (
                <div style={{ fontSize: '0.85rem', color: 'var(--text-subtle)' }}>No past calls recorded for this phone number.</div>
              ) : (
                <div style={{ display: 'flex', flexDirection: 'column', gap: '0.5rem' }}>
                  {history.map((h) => (
                    <div
                      key={h.sessionId}
                      style={{
                        padding: '0.65rem 0.85rem',
                        background: 'var(--bg-subtle)',
                        borderRadius: 'var(--radius-sm)',
                        display: 'flex',
                        justifyContent: 'space-between',
                        alignItems: 'center',
                        fontSize: '0.85rem',
                      }}
                    >
                      <div>
                        <strong>{h.callType === 'FOLLOW_UP' ? 'Follow-up Call' : 'Consultation Call'}</strong>
                        <span style={{ color: 'var(--text-subtle)', marginLeft: '0.5rem' }}>
                          ({h.language?.toUpperCase()})
                        </span>
                      </div>
                      <span className="badge badge-routine" style={{ fontSize: '0.75rem' }}>
                        {h.status}
                      </span>
                    </div>
                  ))}
                </div>
              )}
            </div>
          )}
        </>
      )}
    </div>
  );
};
