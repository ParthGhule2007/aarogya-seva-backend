import React from 'react';
import { Check, X } from 'lucide-react';
import { SymptomSeverity } from '../../../types/symptom';

export interface SymptomChipProps {
  name: string;
  bilingualLabel?: string;
  severity?: SymptomSeverity | string;
  durationDays?: number;
  selected?: boolean;
  onClick?: () => void;
  onRemove?: () => void;
  icon?: React.ReactNode;
}

export const SymptomChip: React.FC<SymptomChipProps> = ({
  name,
  bilingualLabel,
  severity,
  durationDays,
  selected = false,
  onClick,
  onRemove,
  icon,
}) => {
  const normSev = (severity || '').toUpperCase();
  const sevColor =
    normSev === 'CRITICAL' || normSev === 'SEVERE'
      ? { bg: '#fee2e2', fg: '#991b1b', border: '#fca5a5' }
      : normSev === 'MODERATE'
      ? { bg: '#fef3c7', fg: '#92400e', border: '#fde68a' }
      : normSev === 'MILD'
      ? { bg: '#dcfce7', fg: '#166534', border: '#86efac' }
      : null;

  return (
    <div
      role={onClick ? 'button' : undefined}
      tabIndex={onClick ? 0 : undefined}
      onClick={onClick}
      onKeyDown={(e) => {
        if (onClick && (e.key === 'Enter' || e.key === ' ')) {
          e.preventDefault();
          onClick();
        }
      }}
      className={`symptom-chip-modern ${selected ? 'selected' : ''}`}
      style={{
        cursor: onClick ? 'pointer' : 'default',
      }}
    >
      {icon && <span style={{ display: 'inline-flex', color: 'var(--primary)' }}>{icon}</span>}
      {selected && !icon && <Check size={14} color="var(--primary)" />}

      <div style={{ display: 'flex', flexDirection: 'column', lineHeight: 1.2 }}>
        <span style={{ fontWeight: 700, fontSize: '0.85rem' }}>{name}</span>
        {bilingualLabel && (
          <span style={{ fontSize: '0.72rem', color: 'var(--text-subtle)', fontWeight: 600 }}>
            {bilingualLabel}
          </span>
        )}
      </div>

      {sevColor && (
        <span
          style={{
            padding: '0.1rem 0.45rem',
            borderRadius: '9999px',
            fontSize: '0.68rem',
            fontWeight: 800,
            backgroundColor: sevColor.bg,
            color: sevColor.fg,
            border: `1px solid ${sevColor.border}`,
          }}
        >
          {normSev}
          {durationDays ? ` • ${durationDays}d` : ''}
        </span>
      )}

      {onRemove && (
        <button
          type="button"
          onClick={(e) => {
            e.stopPropagation();
            onRemove();
          }}
          aria-label={`Remove ${name}`}
          style={{
            background: 'transparent',
            border: 'none',
            padding: '2px',
            display: 'inline-flex',
            alignItems: 'center',
            cursor: 'pointer',
            color: 'var(--text-subtle)',
          }}
        >
          <X size={14} />
        </button>
      )}
    </div>
  );
};
