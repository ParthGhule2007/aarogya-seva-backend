import React from 'react';
import { Inbox } from 'lucide-react';

interface EmptyStateProps {
  icon?: React.ReactNode;
  title: string;
  description: string;
  action?: React.ReactNode;
}

export const EmptyState: React.FC<EmptyStateProps> = ({
  icon = <Inbox size={48} style={{ color: 'var(--text-subtle)' }} />,
  title,
  description,
  action,
}) => {
  return (
    <div
      style={{
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        justifyContent: 'center',
        padding: '3.5rem 1.5rem',
        textAlign: 'center',
        backgroundColor: 'var(--bg-surface-subtle)',
        borderRadius: 'var(--radius-lg)',
        border: '1px dashed var(--border-strong)',
      }}
    >
      <div style={{ marginBottom: '1rem' }}>{icon}</div>
      <h4 style={{ fontSize: '1.1rem', marginBottom: '0.4rem', color: 'var(--text-main)' }}>{title}</h4>
      <p style={{ fontSize: '0.875rem', color: 'var(--text-muted)', maxWidth: '420px', marginBottom: action ? '1.25rem' : '0' }}>
        {description}
      </p>
      {action}
    </div>
  );
};
