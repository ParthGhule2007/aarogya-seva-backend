import React from 'react';
import { Check, Loader2, AlertCircle } from 'lucide-react';

interface LoadingOverlayProps {
  title?: string;
  subtitle?: string;
  error?: string | null;
  onRetry?: () => void;
}

export const LoadingOverlay: React.FC<LoadingOverlayProps> = ({
  title = 'Screening in Progress • तपासणी सुरू आहे',
  subtitle = 'Analyzing your symptoms with clinical AI • आपल्या लक्षणांचे विश्लेषण केले जात आहे...',
  error,
  onRetry,
}) => {
  return (
    <div className="loading-overlay" role="status" aria-live="polite">
      {error ? (
        <div style={{ maxWidth: '420px', margin: '0 auto' }}>
          <AlertCircle size={56} style={{ color: 'var(--risk-high)', margin: '0 auto 1rem auto' }} />
          <h2 style={{ fontSize: '1.4rem', color: 'var(--risk-high)', marginBottom: '0.5rem' }}>
            Screening Could Not Complete
          </h2>
          <p style={{ color: 'var(--text-muted)', marginBottom: '1.5rem', fontSize: '1rem' }}>
            {error}
          </p>
          {onRetry && (
            <button className="btn-large btn-primary" onClick={onRetry}>
              Try Again • पुन्हा प्रयत्न करा
            </button>
          )}
        </div>
      ) : (
        <>
          <div className="pulse-spinner" />
          <h2 style={{ fontSize: '1.5rem', fontWeight: 800, color: 'var(--primary)', marginBottom: '0.4rem' }}>
            {title}
          </h2>
          <p style={{ color: 'var(--text-muted)', fontSize: '1.05rem', marginBottom: '1.5rem' }}>
            {subtitle}
          </p>

          <ul className="progress-steps-list">
            <li className="done">
              <Check size={20} color="var(--secondary)" strokeWidth={3} />
              <span>Symptoms recorded (लक्षणे नोंदवली)</span>
            </li>
            <li className="done">
              <Check size={20} color="var(--secondary)" strokeWidth={3} />
              <span>Clinical intelligence engaged (वैद्यकीय विश्लेषण सुरू)</span>
            </li>
            <li className="current">
              <Loader2 size={20} color="var(--primary)" className="spin-fast" />
              <span>Finding suitable healthcare (जवळचे दवाखाने शोधत आहे)</span>
            </li>
          </ul>

          <p style={{ marginTop: '1.5rem', fontSize: '0.9rem', color: 'var(--text-subtle)' }}>
            Please wait a moment... (कृपया थोडा वेळ थांबा...)
          </p>
        </>
      )}
    </div>
  );
};
