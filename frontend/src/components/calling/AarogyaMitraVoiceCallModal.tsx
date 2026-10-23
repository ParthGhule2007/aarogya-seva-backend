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
  CheckCircle,
  Clock,
  Volume2,
  X,
  History,
  Info,
} from 'lucide-react';

interface Props {
  isOpen: boolean;
  onClose: () => void;
  defaultPhone?: string;
  defaultName?: string;
}

export const AarogyaMitraVoiceCallModal: React.FC<Props> = ({
  isOpen,
  onClose,
  defaultPhone = '',
  defaultName = '',
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
    if (isOpen && inputPhone) {
      fetchProfile(inputPhone);
      fetchHistory(inputPhone);
    }
  }, [isOpen, inputPhone, fetchProfile, fetchHistory]);

  if (!isOpen) return null;

  const handleTalkToAi = async () => {
    if (!inputPhone) return;
    try {
      await startTalkToAi({
        phoneNumber: inputPhone,
        language: selectedLang,
        chosenName: inputName,
        consentToSaveSummary: consent,
      });
    } catch {
      // Error is stored in hook
    }
  };

  const handleFollowUp = async () => {
    if (!inputPhone) return;
    try {
      await startFollowUp({
        phoneNumber: inputPhone,
        language: selectedLang,
      });
    } catch {
      // Error is stored in hook
    }
  };

  return (
    <div className="modal-overlay" onClick={onClose} role="dialog" aria-modal="true">
      <div
        className="modal-dialog"
        style={{ maxWidth: '640px', width: '92%' }}
        onClick={(e) => e.stopPropagation()}
      >
        {/* Header */}
        <div
          className="modal-header"
          style={{
            background: 'linear-gradient(135deg, #0d9488 0%, #059669 100%)',
            color: '#ffffff',
            borderTopLeftRadius: 'var(--radius-lg)',
            borderTopRightRadius: 'var(--radius-lg)',
          }}
        >
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
            <div
              style={{
                width: '42px',
                height: '42px',
                borderRadius: '50%',
                background: 'rgba(255, 255, 255, 0.2)',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
              }}
            >
              <PhoneCall size={22} color="#ffffff" />
            </div>
            <div>
              <h3 style={{ margin: 0, fontWeight: 800, fontSize: '1.25rem', color: '#ffffff' }}>
                आरोग्य मित्र — AI Voice Call
              </h3>
              <div style={{ fontSize: '0.85rem', color: 'rgba(255, 255, 255, 0.9)' }}>
                Real-time trilingual health consultation directly to your phone
              </div>
            </div>
          </div>
          <button
            className="icon-btn"
            style={{ color: '#ffffff' }}
            onClick={onClose}
            aria-label="Close dialog"
          >
            <X size={20} />
          </button>
        </div>

        <div className="modal-body" style={{ padding: '1.25rem 1.5rem' }}>
          {/* Error Banner */}
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
                padding: '1rem',
                marginBottom: '1.25rem',
              }}
            >
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                  <Volume2 size={20} color={isCallActive ? 'var(--primary)' : 'var(--text-subtle)'} />
                  <span style={{ fontWeight: 800, fontSize: '1rem' }}>
                    {activeSession.callType === 'FOLLOW_UP' ? 'Follow-up Check-in' : 'Aarogya Mitra Consultation'}
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
                  style={{ fontSize: '0.8rem', padding: '0.35rem 0.65rem' }}
                >
                  ● {activeSession.status}
                </span>
              </div>

              <div style={{ fontSize: '0.9rem', color: 'var(--text-subtle)', marginTop: '0.5rem' }}>
                {activeSession.message ||
                  (activeSession.status === 'RINGING'
                    ? 'Calling your phone... Please answer to speak with Aarogya Mitra.'
                    : activeSession.status === 'IN_PROGRESS'
                    ? 'Call connected! Speaking with AI voice assistant.'
                    : activeSession.status === 'COMPLETED'
                    ? 'Call ended successfully.'
                    : 'Call status: ' + activeSession.status)}
              </div>

              {activeSession.durationSeconds ? (
                <div style={{ fontSize: '0.8rem', color: 'var(--text-muted)', marginTop: '0.25rem' }}>
                  Duration: {activeSession.durationSeconds} seconds
                </div>
              ) : null}

              {isCallActive ? (
                <button
                  type="button"
                  className="btn btn-danger"
                  style={{ marginTop: '0.75rem', minHeight: '38px', fontSize: '0.85rem' }}
                  onClick={terminateCall}
                  disabled={isLoading}
                >
                  <PhoneOff size={16} />
                  <span>Hang Up / Cancel Call</span>
                </button>
              ) : (
                <button
                  type="button"
                  className="btn btn-outline"
                  style={{ marginTop: '0.75rem', minHeight: '36px', fontSize: '0.8rem' }}
                  onClick={clearActiveSession}
                >
                  Dismiss Call Card
                </button>
              )}
            </div>
          )}

          {/* Form Inputs (when no call is in progress) */}
          {!isCallActive && (
            <>
              {/* Language Selection */}
              <div className="form-group">
                <label className="form-label" style={{ fontWeight: 700 }}>
                  Spoken Language (भाषा चुनें):
                </label>
                <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: '0.5rem' }}>
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
                <div style={{ fontSize: '0.75rem', color: 'var(--text-subtle)', marginTop: '0.35rem' }}>
                  Aarogya Mitra naturally understands code-switching (Hinglish / Marathi-English) and interruptions.
                </div>
              </div>

              {/* Phone Number */}
              <div className="form-group">
                <label className="form-label" htmlFor="calling-phone" style={{ fontWeight: 700 }}>
                  Phone Number (फोन नंबर): *
                </label>
                <div style={{ position: 'relative' }}>
                  <input
                    id="calling-phone"
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
              </div>

              {/* Patient Name */}
              <div className="form-group">
                <label className="form-label" htmlFor="calling-name">
                  Caller / Patient Name (नाम - वैकल्पिक):
                </label>
                <input
                  id="calling-name"
                  type="text"
                  className="form-control"
                  placeholder="e.g. Ramesh Patil"
                  value={inputName}
                  onChange={(e) => setInputName(e.target.value)}
                />
              </div>

              {/* Consent Checkbox */}
              <div style={{ display: 'flex', alignItems: 'flex-start', gap: '0.5rem', margin: '0.75rem 0' }}>
                <input
                  type="checkbox"
                  id="consent-check"
                  checked={consent}
                  onChange={(e) => setConsent(e.target.checked)}
                  style={{ width: '18px', height: '18px', marginTop: '2px', accentColor: 'var(--primary)' }}
                />
                <label htmlFor="consent-check" style={{ fontSize: '0.85rem', color: 'var(--text-main)', cursor: 'pointer' }}>
                  Save a brief health concern note so I can request a follow-up check-in call later.
                  (फॉलो-अप चेक-इन के लिए संक्षिप्त सारांश सहेजने की सहमति दें)
                </label>
              </div>

              {/* Follow-up Profile Context Banner (if available) */}
              {profile && profile.firstCallCompleted && !profile.optedOut && (
                <div
                  style={{
                    background: 'rgba(5, 150, 105, 0.08)',
                    border: '1px solid var(--primary)',
                    borderRadius: 'var(--radius-md)',
                    padding: '0.75rem 1rem',
                    marginBottom: '1rem',
                  }}
                >
                  <div style={{ display: 'flex', alignItems: 'center', gap: '0.4rem', fontWeight: 700, fontSize: '0.85rem', color: 'var(--primary)' }}>
                    <ShieldCheck size={16} />
                    <span>Prior Consultation on File</span>
                  </div>
                  {profile.concernSummary && (
                    <div style={{ fontSize: '0.85rem', color: 'var(--text-main)', marginTop: '0.25rem' }}>
                      Previous concern: <em>"{profile.concernSummary}"</em>
                    </div>
                  )}
                </div>
              )}

              {/* Two Primary Action Buttons */}
              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '0.75rem', marginTop: '1.25rem' }}>
                {/* Button 1: Talk to AI */}
                <button
                  type="button"
                  className="btn btn-primary"
                  style={{ minHeight: '52px', fontSize: '1rem', fontWeight: 700 }}
                  onClick={handleTalkToAi}
                  disabled={isLoading || !inputPhone}
                >
                  <Sparkles size={18} />
                  <span>Talk to AI (कॉल शुरू करें)</span>
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
                      ? 'Requires a completed first consultation via Talk to AI'
                      : profile.optedOut
                      ? 'Patient has opted out'
                      : 'Request follow-up check-in'
                  }
                >
                  <PhoneForwarded size={18} />
                  <span>Follow-up (फॉलो-अप)</span>
                </button>
              </div>

              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginTop: '1rem' }}>
                <button
                  type="button"
                  onClick={() => setShowHistory(!showHistory)}
                  style={{
                    background: 'none',
                    border: 'none',
                    color: 'var(--primary)',
                    fontSize: '0.8rem',
                    cursor: 'pointer',
                    display: 'flex',
                    alignItems: 'center',
                    gap: '0.35rem',
                    padding: 0,
                  }}
                >
                  <History size={14} />
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

              {/* Call History Dropdown */}
              {showHistory && (
                <div style={{ marginTop: '0.75rem', maxHeight: '180px', overflowY: 'auto' }}>
                  {history.length === 0 ? (
                    <div style={{ fontSize: '0.8rem', color: 'var(--text-subtle)' }}>No past calls recorded.</div>
                  ) : (
                    history.map((h) => (
                      <div
                        key={h.sessionId}
                        style={{
                          fontSize: '0.8rem',
                          padding: '0.4rem 0',
                          borderBottom: '1px solid var(--border-light)',
                          display: 'flex',
                          justifyContent: 'space-between',
                        }}
                      >
                        <span>
                          {h.callType === 'FOLLOW_UP' ? 'Follow-up' : 'Talk to AI'} ({h.language?.toUpperCase()})
                        </span>
                        <span style={{ fontWeight: 600 }}>{h.status}</span>
                      </div>
                    ))
                  )}
                </div>
              )}
            </>
          )}
        </div>
      </div>
    </div>
  );
};
