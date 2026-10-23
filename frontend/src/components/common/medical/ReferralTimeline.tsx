import React from 'react';
import { ReferralStatus } from '../../../types/referral';
import {
  Send,
  CheckCircle2,
  Activity,
  Award,
  XCircle,
} from 'lucide-react';

export interface ReferralTimelineProps {
  status: ReferralStatus | string;
  requestedAt?: string;
  respondedAt?: string;
  acceptedAt?: string;
  arrivedAt?: string;
  completedAt?: string;
  compact?: boolean;
}

export const ReferralTimeline: React.FC<ReferralTimelineProps> = ({
  status,
  requestedAt,
  respondedAt,
  acceptedAt,
  arrivedAt,
  completedAt,
  compact = false,
}) => {
  const norm = (status || 'PENDING').toUpperCase();

  if (norm === 'REJECTED' || norm === 'CANCELLED') {
    return (
      <div
        style={{
          padding: '0.75rem 1rem',
          borderRadius: 'var(--radius-md)',
          backgroundColor: '#fef2f2',
          border: '1px solid #fca5a5',
          display: 'flex',
          alignItems: 'center',
          gap: '0.65rem',
          color: '#991b1b',
          fontSize: '0.85rem',
          fontWeight: 700,
        }}
      >
        <XCircle size={18} color="#dc2626" style={{ flexShrink: 0 }} />
        <div>
          <span>
            Referral {norm === 'REJECTED' ? 'Declined by Facility • रुग्णालयाने नकार दिला' : 'Cancelled'}
          </span>
          {respondedAt && (
            <div style={{ fontSize: '0.75rem', fontWeight: 500, color: '#7f1d1d' }}>
              Updated: {new Date(respondedAt).toLocaleString()}
            </div>
          )}
        </div>
      </div>
    );
  }

  const activeStage =
    norm === 'COMPLETED'
      ? 4
      : norm === 'IN_PROGRESS'
      ? 3
      : norm === 'ACCEPTED' || norm === 'CONFIRMED'
      ? 2
      : 1;

  const steps = [
    {
      index: 1,
      title: 'Referral Issued',
      bilingual: 'संदर्भ जारी',
      time: requestedAt ? new Date(requestedAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }) : 'Issued',
      icon: <Send size={14} />,
    },
    {
      index: 2,
      title: 'Facility Accepted',
      bilingual: 'रुग्णालय स्वीकृती',
      time: acceptedAt || respondedAt
        ? new Date((acceptedAt || respondedAt)!).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
        : 'Awaiting',
      icon: <CheckCircle2 size={14} />,
    },
    {
      index: 3,
      title: 'Care & Bed Prep',
      bilingual: 'उपचार सुरू',
      time: arrivedAt
        ? new Date(arrivedAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
        : activeStage >= 3
        ? 'Active'
        : 'Pending',
      icon: <Activity size={14} />,
    },
    {
      index: 4,
      title: 'Case Completed',
      bilingual: 'उपचार पूर्ण',
      time: completedAt
        ? new Date(completedAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
        : activeStage === 4
        ? 'Done'
        : 'Pending',
      icon: <Award size={14} />,
    },
  ];

  return (
    <div className="referral-timeline-track" role="list" aria-label="Referral Progression Timeline">
      {steps.map((step) => {
        const isDone = step.index < activeStage || activeStage === 4;
        const isCurrent = step.index === activeStage && activeStage < 4;

        const bg = isDone
          ? '#f0fdf4'
          : isCurrent
          ? '#f0fdfa'
          : '#f8fafc';
        const border = isDone
          ? '#86efac'
          : isCurrent
          ? '#5eead4'
          : 'var(--border-light)';
        const color = isDone
          ? '#15803d'
          : isCurrent
          ? '#0f766e'
          : 'var(--text-subtle)';

        return (
          <div
            key={step.index}
            role="listitem"
            style={{
              padding: compact ? '0.5rem 0.65rem' : '0.65rem 0.8rem',
              borderRadius: 'var(--radius-md)',
              backgroundColor: bg,
              border: `1.5px solid ${border}`,
              display: 'flex',
              alignItems: 'center',
              gap: '0.55rem',
              transition: 'all 0.2s ease',
            }}
          >
            <div
              style={{
                width: compact ? '26px' : '30px',
                height: compact ? '26px' : '30px',
                borderRadius: '50%',
                backgroundColor: isDone || isCurrent ? color : '#e2e8f0',
                color: isDone || isCurrent ? '#ffffff' : '#64748b',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                flexShrink: 0,
                fontWeight: 800,
                fontSize: '0.75rem',
              }}
            >
              {step.icon}
            </div>
            <div style={{ minWidth: 0 }}>
              <div
                style={{
                  fontSize: compact ? '0.75rem' : '0.8rem',
                  fontWeight: 800,
                  color: isDone || isCurrent ? 'var(--text-main)' : 'var(--text-muted)',
                  whiteSpace: 'nowrap',
                  overflow: 'hidden',
                  textOverflow: 'ellipsis',
                }}
              >
                {step.title}
              </div>
              {!compact && (
                <div style={{ fontSize: '0.68rem', color: 'var(--text-subtle)', fontWeight: 600 }}>
                  {step.bilingual} • {step.time}
                </div>
              )}
            </div>
          </div>
        );
      })}
    </div>
  );
};
