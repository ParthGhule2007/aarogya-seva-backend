import React from 'react';
import { Thermometer, Heart, Activity, Wind, Droplets } from 'lucide-react';
import { VitalsDto } from '../../../types/case';

export interface VitalCardProps {
  label: string;
  bilingualLabel?: string;
  value: string | number;
  unit: string;
  type: 'temp' | 'bp' | 'pulse' | 'spo2' | 'resp';
  status?: 'normal' | 'elevated' | 'critical';
  normalRangeText?: string;
}

export const evaluateVitalStatus = (
  type: VitalCardProps['type'],
  rawVal?: number,
  secondaryVal?: number
): { status: 'normal' | 'elevated' | 'critical'; rangeText: string; pct: number } => {
  if (rawVal == null || Number.isNaN(rawVal)) {
    return { status: 'normal', rangeText: 'Standard Range', pct: 50 };
  }
  switch (type) {
    case 'temp': {
      const status = rawVal >= 102 || rawVal < 95 ? 'critical' : rawVal >= 99.5 ? 'elevated' : 'normal';
      const pct = Math.min(100, Math.max(10, Math.round(((rawVal - 95) / 10) * 100)));
      return { status, rangeText: 'Normal: 97°F – 99°F', pct };
    }
    case 'bp': {
      const dia = secondaryVal || 80;
      const status =
        rawVal >= 160 || rawVal < 90 || dia >= 100
          ? 'critical'
          : rawVal >= 130 || dia >= 85
          ? 'elevated'
          : 'normal';
      const pct = Math.min(100, Math.max(15, Math.round(((rawVal - 80) / 100) * 100)));
      return { status, rangeText: 'Normal: ~120/80 mmHg', pct };
    }
    case 'pulse': {
      const status = rawVal >= 120 || rawVal < 50 ? 'critical' : rawVal > 100 || rawVal < 60 ? 'elevated' : 'normal';
      const pct = Math.min(100, Math.max(15, Math.round(((rawVal - 40) / 110) * 100)));
      return { status, rangeText: 'Normal: 60 – 100 BPM', pct };
    }
    case 'spo2': {
      const status = rawVal < 92 ? 'critical' : rawVal < 95 ? 'elevated' : 'normal';
      const pct = Math.min(100, Math.max(10, rawVal));
      return { status, rangeText: 'Normal: 95% – 100%', pct };
    }
    case 'resp': {
      const status = rawVal > 24 || rawVal < 10 ? 'critical' : rawVal > 20 ? 'elevated' : 'normal';
      const pct = Math.min(100, Math.max(15, Math.round((rawVal / 32) * 100)));
      return { status, rangeText: 'Normal: 12 – 20 /min', pct };
    }
  }
};

export const VitalCard: React.FC<VitalCardProps> = ({
  label,
  bilingualLabel,
  value,
  unit,
  type,
  status = 'normal',
  normalRangeText,
}) => {
  const getIcon = () => {
    switch (type) {
      case 'temp':
        return <Thermometer size={17} />;
      case 'bp':
        return <Activity size={17} />;
      case 'pulse':
        return <Heart size={17} />;
      case 'spo2':
        return <Droplets size={17} />;
      case 'resp':
        return <Wind size={17} />;
    }
  };

  const palette =
    status === 'critical'
      ? { fg: '#b91c1c', bg: '#fef2f2', border: '#fca5a5', badge: 'Critical • गंभीर' }
      : status === 'elevated'
      ? { fg: '#b45309', bg: '#fffbeb', border: '#fde68a', badge: 'Attention • लक्ष द्या' }
      : { fg: '#0f766e', bg: '#f0fdfa', border: '#99f6e4', badge: 'Normal • सामान्य' };

  return (
    <div
      className="vital-card-modern"
      style={{
        borderColor: status === 'normal' ? 'var(--border-light)' : palette.border,
        backgroundColor: status === 'normal' ? 'var(--bg-surface)' : palette.bg,
      }}
    >
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', gap: '0.4rem' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '0.4rem' }}>
          <div
            style={{
              width: '30px',
              height: '30px',
              borderRadius: '8px',
              backgroundColor: palette.bg,
              color: palette.fg,
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              border: `1px solid ${palette.border}`,
              flexShrink: 0,
            }}
          >
            {getIcon()}
          </div>
          <div>
            <div style={{ fontSize: '0.78rem', fontWeight: 800, color: 'var(--text-main)', lineHeight: 1.2 }}>
              {label}
            </div>
            {bilingualLabel && (
              <div style={{ fontSize: '0.68rem', color: 'var(--text-subtle)', fontWeight: 600 }}>
                {bilingualLabel}
              </div>
            )}
          </div>
        </div>

        <span
          style={{
            fontSize: '0.66rem',
            fontWeight: 800,
            padding: '0.12rem 0.45rem',
            borderRadius: '9999px',
            backgroundColor: '#ffffff',
            color: palette.fg,
            border: `1px solid ${palette.border}`,
            whiteSpace: 'nowrap',
          }}
        >
          {palette.badge}
        </span>
      </div>

      <div style={{ display: 'flex', alignItems: 'baseline', gap: '0.3rem', marginTop: '0.25rem' }}>
        <span style={{ fontSize: '1.3rem', fontWeight: 900, color: palette.fg }}>{value}</span>
        <span style={{ fontSize: '0.76rem', fontWeight: 700, color: 'var(--text-subtle)' }}>{unit}</span>
      </div>

      {normalRangeText && (
        <div style={{ fontSize: '0.68rem', color: 'var(--text-subtle)', fontWeight: 600 }}>
          {normalRangeText}
        </div>
      )}
    </div>
  );
};

export const VitalsGridDisplay: React.FC<{ vitals?: VitalsDto; compact?: boolean }> = ({ vitals }) => {
  if (!vitals) return null;

  const hasAny =
    vitals.temperatureFahrenheit != null ||
    vitals.systolicBp != null ||
    vitals.diastolicBp != null ||
    vitals.pulseBpm != null ||
    vitals.spo2Percent != null ||
    vitals.respiratoryRate != null;

  if (!hasAny) return null;

  const tempEval = evaluateVitalStatus('temp', vitals.temperatureFahrenheit);
  const bpEval = evaluateVitalStatus('bp', vitals.systolicBp, vitals.diastolicBp);
  const pulseEval = evaluateVitalStatus('pulse', vitals.pulseBpm);
  const spo2Eval = evaluateVitalStatus('spo2', vitals.spo2Percent);
  const respEval = evaluateVitalStatus('resp', vitals.respiratoryRate);

  return (
    <div
      style={{
        display: 'grid',
        gridTemplateColumns: 'repeat(auto-fit, minmax(165px, 1fr))',
        gap: '0.75rem',
      }}
    >
      {vitals.temperatureFahrenheit != null && (
        <VitalCard
          type="temp"
          label="Temperature"
          bilingualLabel="शरीर तापमान"
          value={vitals.temperatureFahrenheit}
          unit="°F"
          status={tempEval.status}
          normalRangeText={tempEval.rangeText}
        />
      )}
      {(vitals.systolicBp != null || vitals.diastolicBp != null) && (
        <VitalCard
          type="bp"
          label="Blood Pressure"
          bilingualLabel="रक्तदाब (BP)"
          value={`${vitals.systolicBp || '—'}/${vitals.diastolicBp || '—'}`}
          unit="mmHg"
          status={bpEval.status}
          normalRangeText={bpEval.rangeText}
        />
      )}
      {vitals.pulseBpm != null && (
        <VitalCard
          type="pulse"
          label="Heart Rate"
          bilingualLabel="नाडीचे ठोके"
          value={vitals.pulseBpm}
          unit="BPM"
          status={pulseEval.status}
          normalRangeText={pulseEval.rangeText}
        />
      )}
      {vitals.spo2Percent != null && (
        <VitalCard
          type="spo2"
          label="Oxygen (SpO2)"
          bilingualLabel="ऑक्सिजन पातळी"
          value={vitals.spo2Percent}
          unit="%"
          status={spo2Eval.status}
          normalRangeText={spo2Eval.rangeText}
        />
      )}
      {vitals.respiratoryRate != null && (
        <VitalCard
          type="resp"
          label="Resp. Rate"
          bilingualLabel="श्वसन दर"
          value={vitals.respiratoryRate}
          unit="/min"
          status={respEval.status}
          normalRangeText={respEval.rangeText}
        />
      )}
    </div>
  );
};
