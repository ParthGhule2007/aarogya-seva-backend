import React from 'react';
import { Bot, MessageSquare, AlertCircle, ArrowLeft, PenTool } from 'lucide-react';

interface ChatbotModalProps {
  isOpen: boolean;
  onClose: () => void;
  onUseForm: () => void;
}

export const ChatbotModal: React.FC<ChatbotModalProps> = ({
  isOpen,
  onClose,
  onUseForm,
}) => {
  if (!isOpen) return null;

  return (
    <div
      style={{
        position: 'fixed',
        inset: 0,
        backgroundColor: 'rgba(15, 23, 42, 0.65)',
        backdropFilter: 'blur(4px)',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        zIndex: 50,
        padding: '1rem',
      }}
      role="dialog"
      aria-modal="true"
      aria-labelledby="chatbot-title"
    >
      <div
        className="card-simple"
        style={{
          maxWidth: '520px',
          width: '100%',
          backgroundColor: 'var(--bg-surface)',
          borderRadius: 'var(--radius-lg)',
          boxShadow: 'var(--shadow-lg)',
          padding: '2rem 1.5rem',
          textAlign: 'center',
          border: '2px solid var(--primary-border)',
        }}
      >
        <div
          style={{
            width: '64px',
            height: '64px',
            borderRadius: '50%',
            backgroundColor: 'var(--primary-light)',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            margin: '0 auto 1rem auto',
          }}
        >
          <Bot size={36} color="var(--primary)" />
        </div>

        <h3 id="chatbot-title" style={{ fontSize: '1.4rem', fontWeight: 800, color: 'var(--primary)', marginBottom: '0.4rem' }}>
          Talk to Aarogya • आरोग्य संवाद
        </h3>
        <p style={{ fontSize: '1rem', color: 'var(--text-subtle)', marginBottom: '1.25rem' }}>
          Voice & Conversational AI Clinical Assistant
        </p>

        {/* Development Notice */}
        <div
          style={{
            backgroundColor: 'var(--accent-warm-light)',
            border: '2px solid var(--accent-warm)',
            borderRadius: 'var(--radius-md)',
            padding: '1rem',
            textAlign: 'left',
            marginBottom: '1.5rem',
            display: 'flex',
            gap: '0.75rem',
          }}
        >
          <AlertCircle size={24} color="var(--accent-warm)" style={{ flexShrink: 0 }} />
          <div>
            <strong style={{ display: 'block', color: 'var(--accent-warm)', fontSize: '1.05rem', marginBottom: '0.2rem' }}>
              Chatbot Under Active Development
            </strong>
            <p style={{ fontSize: '0.95rem', color: 'var(--text-main)', margin: 0 }}>
              The interactive voice and chatbot service is currently being integrated with rural language models.
              Please use our <strong>Simple Form</strong> to submit your symptoms right now.
            </p>
          </div>
        </div>

        {/* Mock Chat Preview Previewing Future Capability */}
        <div
          style={{
            backgroundColor: 'var(--bg-subtle)',
            border: '1px solid var(--border-light)',
            borderRadius: 'var(--radius-md)',
            padding: '1rem',
            textAlign: 'left',
            marginBottom: '1.5rem',
            fontSize: '0.95rem',
          }}
        >
          <div style={{ display: 'flex', gap: '0.5rem', marginBottom: '0.75rem' }}>
            <span style={{ fontWeight: 700, color: 'var(--primary)' }}>Aarogya:</span>
            <span>"Namaste! What health problem are you having today?"</span>
          </div>
          <div style={{ display: 'flex', gap: '0.5rem', color: 'var(--text-muted)' }}>
            <span style={{ fontWeight: 700 }}>You:</span>
            <span>"I have had a high fever and cough for 3 days."</span>
          </div>
        </div>

        {/* Action Buttons */}
        <div style={{ display: 'flex', flexDirection: 'column', gap: '0.75rem' }}>
          <button
            className="btn-large btn-primary"
            onClick={() => {
              onClose();
              onUseForm();
            }}
          >
            <PenTool size={20} />
            <span>Use Simple Form Instead • फॉर्म वापरा</span>
          </button>

          <button
            className="btn-large btn-ghost"
            onClick={onClose}
          >
            <ArrowLeft size={20} />
            <span>Back • मागे जा</span>
          </button>
        </div>
      </div>
    </div>
  );
};
