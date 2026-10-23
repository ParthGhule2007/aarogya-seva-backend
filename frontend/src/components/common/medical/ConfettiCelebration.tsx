import React, { useEffect } from 'react';

interface ConfettiCelebrationProps {
  active: boolean;
  onComplete?: () => void;
  message?: string;
  subMessage?: string;
}

export const ConfettiCelebration: React.FC<ConfettiCelebrationProps> = ({
  active,
  onComplete,
  message = 'Well done! / शाबाश! / उत्तम!',
  subMessage = 'Your health step is recorded successfully.',
}) => {
  useEffect(() => {
    if (!active) return;

    const timer = setTimeout(() => {
      if (onComplete) onComplete();
    }, 2800);

    return () => clearTimeout(timer);
  }, [active, onComplete]);

  if (!active) return null;

  return (
    <div
      style={{
        position: 'fixed',
        top: 0,
        left: 0,
        right: 0,
        bottom: 0,
        zIndex: 9999,
        pointerEvents: 'none',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        padding: '1rem',
      }}
    >
      {/* Visual Confetti Particles */}
      <div className="confetti-container" aria-hidden="true">
        {Array.from({ length: 32 }).map((_, i) => (
          <span
            key={i}
            className={`confetti-particle p-${i % 8}`}
            style={{
              left: `${Math.random() * 95}%`,
              animationDelay: `${Math.random() * 0.4}s`,
              backgroundColor: [
                '#10b981',
                '#0d9488',
                '#f59e0b',
                '#3b82f6',
                '#ec4899',
                '#8b5cf6',
                '#14b8a6',
              ][i % 7],
            }}
          />
        ))}
      </div>

      {/* Pop-up Celebration Toast */}
      <div
        style={{
          pointerEvents: 'auto',
          backgroundColor: 'rgba(255, 255, 255, 0.98)',
          backdropFilter: 'blur(12px)',
          border: '2px solid #10b981',
          boxShadow: '0 20px 40px rgba(16, 185, 129, 0.25)',
          borderRadius: '1.25rem',
          padding: '1.5rem 2rem',
          textAlign: 'center',
          animation: 'celebration-pop 0.4s cubic-bezier(0.175, 0.885, 0.32, 1.275)',
          maxWidth: '380px',
        }}
      >
        <div style={{ fontSize: '2.5rem', marginBottom: '0.4rem', animation: 'bounce-heart 1s infinite' }}>
          🎉
        </div>
        <h3 style={{ margin: 0, fontSize: '1.25rem', fontWeight: 800, color: '#065f46' }}>
          {message}
        </h3>
        <p style={{ margin: '0.35rem 0 0 0', fontSize: '0.88rem', color: '#047857' }}>
          {subMessage}
        </p>
      </div>
    </div>
  );
};
