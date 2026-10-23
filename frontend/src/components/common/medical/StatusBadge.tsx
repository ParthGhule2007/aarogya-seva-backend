import React from 'react';
import {
  AlertCircle,
  CheckCircle2,
  Clock,
  ShieldAlert,
  Activity,
  Building2,
} from 'lucide-react';

export interface StatusBadgeProps {
  status: string;
  variant?: 'risk' | 'urgency' | 'referral' | 'consultation' | 'facility' | 'neutral';
  pulse?: boolean;
  size?: 'sm' | 'md';
  labelPrefix?: string;
}

export const StatusBadge: React.FC<StatusBadgeProps> = ({
  status,
  variant = 'neutral',
  pulse = false,
  size = 'md',
  labelPrefix,
}) => {
  const normalized = (status || 'UNKNOWN').toUpperCase();

  let bg = '#f1f5f9';
  let fg = '#334155';
  let border = '#cbd5e1';
  let icon: React.ReactNode = <Clock size={size === 'sm' ? 11 : 13} />;

  if (
    normalized === 'LOW' ||
    normalized === 'ROUTINE' ||
    normalized === 'COMPLETED' ||
    normalized === 'ACCEPTED' ||
    normalized === 'CONFIRMED' ||
    normalized === 'TAKEN' ||
    normalized === 'CLOSED'
  ) {
    bg = 'var(--risk-low-bg)';
    fg = 'var(--risk-low)';
    border = 'var(--risk-low-border)';
    icon = <CheckCircle2 size={size === 'sm' ? 11 : 13} />;
  } else if (
    normalized === 'MODERATE' ||
    normalized === 'MEDIUM' ||
    normalized === 'PRIORITY' ||
    normalized === 'PENDING' ||
    normalized === 'SCHEDULED' ||
    normalized === 'REQUESTED' ||
    normalized === 'SYMPTOMS_RECORDED' ||
    normalized === 'PENDING_TRIAGE'
  ) {
    bg = 'var(--risk-moderate-bg)';
    fg = 'var(--risk-moderate)';
    border = 'var(--risk-moderate-border)';
    icon = <Clock size={size === 'sm' ? 11 : 13} />;
  } else if (
    normalized === 'IN_PROGRESS' ||
    normalized === 'TRIAGED' ||
    normalized === 'REFERRED'
  ) {
    bg = 'var(--primary-light)';
    fg = 'var(--primary-hover)';
    border = 'var(--primary-border)';
    icon = <Activity size={size === 'sm' ? 11 : 13} />;
  } else if (
    normalized === 'HIGH' ||
    normalized === 'URGENT' ||
    normalized === 'REJECTED' ||
    normalized === 'MISSED' ||
    normalized === 'CANCELLED'
  ) {
    bg = 'var(--risk-high-bg)';
    fg = 'var(--risk-high)';
    border = 'var(--risk-high-border)';
    icon = <AlertCircle size={size === 'sm' ? 11 : 13} />;
  } else if (normalized === 'CRITICAL' || normalized === 'EMERGENCY') {
    bg = 'var(--risk-critical-bg)';
    fg = 'var(--risk-critical)';
    border = 'var(--risk-critical-border)';
    icon = <ShieldAlert size={size === 'sm' ? 11 : 13} />;
  } else if (variant === 'facility') {
    bg = '#eff6ff';
    fg = '#1d4ed8';
    border = '#bfdbfe';
    icon = <Building2 size={size === 'sm' ? 11 : 13} />;
  }

  const displayLabel = normalized.replace(/_/g, ' ');

  return (
    <span
      style={{
        display: 'inline-flex',
        alignItems: 'center',
        gap: '0.35rem',
        padding: size === 'sm' ? '0.18rem 0.55rem' : '0.26rem 0.7rem',
        borderRadius: '9999px',
        fontSize: size === 'sm' ? '0.73rem' : '0.8rem',
        fontWeight: 800,
        backgroundColor: bg,
        color: fg,
        border: `1px solid ${border}`,
        letterSpacing: '0.02em',
        textTransform: 'uppercase',
        whiteSpace: 'nowrap',
      }}
    >
      {pulse ? (
        <span
          className="pulse-dot"
          style={{ backgroundColor: fg, color: fg }}
          aria-hidden="true"
        />
      ) : (
        icon
      )}
      <span>
        {labelPrefix ? `${labelPrefix}: ${displayLabel}` : displayLabel}
      </span>
    </span>
  );
};
