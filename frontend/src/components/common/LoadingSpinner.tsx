import React from 'react';

interface LoadingSpinnerProps {
  message?: string;
  size?: 'sm' | 'md' | 'lg';
}

export const LoadingSpinner: React.FC<LoadingSpinnerProps> = ({
  message = 'Loading data from healthcare servers...',
  size = 'md',
}) => {
  const dimension = size === 'sm' ? '20px' : size === 'lg' ? '48px' : '32px';

  return (
    <div
      style={{
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        justifyContent: 'center',
        padding: '3rem 1.5rem',
        textAlign: 'center',
      }}
    >
      <div
        className="spinner"
        style={{
          width: dimension,
          height: dimension,
          borderColor: 'rgba(13, 148, 136, 0.2)',
          borderTopColor: 'var(--primary)',
          marginBottom: '1rem',
        }}
      />
      {message && <p style={{ fontSize: '0.9rem', color: 'var(--text-muted)' }}>{message}</p>}
    </div>
  );
};
