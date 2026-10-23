import React from 'react';
import { RiskLevel, UrgencyLevel } from '../../../types/ai';
import { ShieldAlert, AlertTriangle, CheckCircle2, Sparkles } from 'lucide-react';

export interface RiskGaugeProps {
  riskScore?: number; // 0..1 or 0..100
  riskLevel?: RiskLevel | string;
  urgencyLevel?: UrgencyLevel | string;
  confidence?: number; // 0..1 or 0..100
  compact?: boolean;
}

export const RiskGauge: React.FC<RiskGaugeProps> = ({
  riskScore,
  riskLevel = 'MODERATE',
  urgencyLevel,
  confidence,
  compact = false,
}) => {
  const normalizedLevel = (riskLevel || 'MODERATE').toUpperCase();

  // Compute 0..100 percentage from real riskScore or level fallback
  let scorePct = 50;
  if (typeof riskScore === 'number' && !Number.isNaN(riskScore)) {
    scorePct = riskScore <= 1 ? Math.round(riskScore * 100) : Math.round(riskScore);
  } else {
    if (normalizedLevel === 'LOW') scorePct = 24;
    else if (normalizedLevel === 'MODERATE') scorePct = 54;
    else if (normalizedLevel === 'HIGH') scorePct = 78;
    else if (normalizedLevel === 'CRITICAL') scorePct = 94;
  }
  scorePct = Math.max(5, Math.min(100, scorePct));

  let confidencePct: number | null = null;
  if (typeof confidence === 'number' && !Number.isNaN(confidence)) {
    confidencePct = confidence <= 1 ? Math.round(confidence * 100) : Math.round(confidence);
  }

  const getTheme = () => {
    switch (normalizedLevel) {
      case 'LOW':
        return {
          color: '#15803d',
          trackBg: '#dcfce7',
          cardBg: '#f0fdf4',
          borderColor: '#86efac',
          bilingual: 'Low Risk • कमी धोका',
          icon: <CheckCircle2 size={compact ? 16 : 20} color="#15803d" />,
        };
      case 'HIGH':
        return {
          color: '#c2410c',
          trackBg: '#ffedd5',
          cardBg: '#fff7ed',
          borderColor: '#fdba74',
          bilingual: 'High Risk • जास्त धोका',
          icon: <AlertTriangle size={compact ? 16 : 20} color="#c2410c" />,
        };
      case 'CRITICAL':
        return {
          color: '#b91c1c',
          trackBg: '#fee2e2',
          cardBg: '#fef2f2',
          borderColor: '#fca5a5',
          bilingual: 'Critical • अतिशय गंभीर',
          icon: <ShieldAlert size={compact ? 16 : 20} color="#b91c1c" />,
        };
      default:
        return {
          color: '#b45309',
          trackBg: '#fef3c7',
          cardBg: '#fffbeb',
          borderColor: '#fcd34d',
          bilingual: 'Moderate Risk • मध्यम धोका',
          icon: <AlertTriangle size={compact ? 16 : 20} color="#b45309" />,
        };
    }
  };

  const theme = getTheme();

  // SVG Semi-circle gauge geometry
  const radius = compact ? 46 : 64;
  const strokeWidth = compact ? 10 : 13;
  const circumference = Math.PI * radius;
  const dashOffset = circumference - (scorePct / 100) * circumference;
  const svgWidth = (radius + strokeWidth) * 2;
  const svgHeight = radius + strokeWidth + 12;

  return (
    <div
      style={{
        backgroundColor: theme.cardBg,
        border: `1.5px solid ${theme.borderColor}`,
        borderRadius: 'var(--radius-lg)',
        padding: compact ? '0.85rem 1rem' : '1.25rem 1.35rem',
        display: 'flex',
        flexDirection: compact ? 'row' : 'column',
        alignItems: 'center',
        justifyContent: 'space-between',
        gap: compact ? '1rem' : '0.75rem',
      }}
      role="region"
      aria-label={`Clinical Risk Assessment: ${normalizedLevel} (${scorePct}%)`}
    >
      {/* Semi-Circular Arc Gauge */}
      <div style={{ position: 'relative', display: 'flex', flexDirection: 'column', alignItems: 'center' }}>
        <svg
          width={svgWidth}
          height={svgHeight}
          viewBox={`0 0 ${svgWidth} ${svgHeight}`}
          style={{ overflow: 'visible' }}
        >
          {/* Background Track Arc */}
          <path
            d={`M ${strokeWidth} ${radius + strokeWidth / 2} A ${radius} ${radius} 0 0 1 ${svgWidth - strokeWidth} ${radius + strokeWidth / 2}`}
            fill="none"
            stroke={theme.trackBg}
            strokeWidth={strokeWidth}
            strokeLinecap="round"
          />
          {/* Foreground Value Arc */}
          <path
            d={`M ${strokeWidth} ${radius + strokeWidth / 2} A ${radius} ${radius} 0 0 1 ${svgWidth - strokeWidth} ${radius + strokeWidth / 2}`}
            fill="none"
            stroke={theme.color}
            strokeWidth={strokeWidth}
            strokeLinecap="round"
            strokeDasharray={circumference}
            strokeDashoffset={dashOffset}
            style={{ transition: 'stroke-dashoffset 0.7s cubic-bezier(0.16, 1, 0.3, 1)' }}
          />
        </svg>

        <div
          style={{
            position: 'absolute',
            bottom: compact ? '2px' : '6px',
            display: 'flex',
            flexDirection: 'column',
            alignItems: 'center',
            lineHeight: 1.1,
          }}
        >
          <span
            style={{
              fontSize: compact ? '1.2rem' : '1.65rem',
              fontWeight: 900,
              color: theme.color,
            }}
          >
            {scorePct}%
          </span>
          <span
            style={{
              fontSize: '0.68rem',
              fontWeight: 800,
              color: 'var(--text-subtle)',
              textTransform: 'uppercase',
              letterSpacing: '0.04em',
            }}
          >
            Risk Index
          </span>
        </div>
      </div>

      {/* Clinical Labels & Confidence */}
      <div style={{ flex: 1, textAlign: compact ? 'left' : 'center', width: '100%' }}>
        <div
          style={{
            display: 'inline-flex',
            alignItems: 'center',
            gap: '0.4rem',
            padding: '0.25rem 0.75rem',
            borderRadius: '9999px',
            backgroundColor: '#ffffff',
            border: `1px solid ${theme.borderColor}`,
            fontWeight: 800,
            fontSize: compact ? '0.82rem' : '0.9rem',
            color: theme.color,
            marginBottom: '0.35rem',
          }}
        >
          {theme.icon}
          <span>{theme.bilingual}</span>
        </div>

        {urgencyLevel && (
          <div style={{ fontSize: '0.8rem', fontWeight: 700, color: 'var(--text-main)', marginTop: '0.2rem' }}>
            Triage Priority: <strong style={{ color: theme.color }}>{urgencyLevel}</strong>
          </div>
        )}

        {confidencePct !== null && (
          <div style={{ marginTop: '0.55rem' }}>
            <div
              style={{
                display: 'flex',
                justifyContent: 'space-between',
                fontSize: '0.73rem',
                fontWeight: 700,
                color: 'var(--text-muted)',
                marginBottom: '0.2rem',
              }}
            >
              <span style={{ display: 'inline-flex', alignItems: 'center', gap: '0.25rem' }}>
                <Sparkles size={11} color={theme.color} /> AI Confidence
              </span>
              <span>{confidencePct}%</span>
            </div>
            <div
              style={{
                width: '100%',
                height: '6px',
                backgroundColor: '#ffffff',
                borderRadius: '9999px',
                overflow: 'hidden',
                border: `1px solid ${theme.borderColor}`,
              }}
            >
              <div
                style={{
                  width: `${Math.min(100, Math.max(10, confidencePct))}%`,
                  height: '100%',
                  backgroundColor: theme.color,
                  borderRadius: '9999px',
                }}
              />
            </div>
          </div>
        )}
      </div>
    </div>
  );
};
