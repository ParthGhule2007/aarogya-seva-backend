import React from 'react';

export type BodySystemRegion = 'ALL' | 'HEAD' | 'CHEST' | 'ABDOMEN' | 'LIMBS' | 'GENERAL';

export interface BodyMapProps {
  activeRegion: BodySystemRegion;
  onSelectRegion: (region: BodySystemRegion) => void;
  selectedSymptomIds: string[];
  onToggleSymptom?: (symptomId: string) => void;
}

export const BODY_REGION_SYMPTOMS: Record<Exclude<BodySystemRegion, 'ALL'>, string[]> = {
  HEAD: ['headache', 'dizziness', 'cold_sore_throat'],
  CHEST: ['cough', 'shortness_of_breath', 'chest_pain'],
  ABDOMEN: ['abdominal_pain', 'vomiting', 'diarrhea'],
  LIMBS: ['body_ache'],
  GENERAL: ['fever', 'skin_rash'],
};

export const BODY_REGION_LABELS: Record<
  BodySystemRegion,
  { title: string; bilingual: string; description: string; emoji: string }
> = {
  ALL: {
    title: 'Whole Body (All)',
    bilingual: 'संपूर्ण शरीर (सर्व)',
    description: 'Showing all common rural health screening symptoms',
    emoji: '👤',
  },
  HEAD: {
    title: 'Head, ENT & Eyes',
    bilingual: 'डोके, घसा व डोळे',
    description: 'Headache, Dizziness, Cold / Sore Throat',
    emoji: '🤕',
  },
  CHEST: {
    title: 'Chest & Breathing',
    bilingual: 'छाती, खोकला व दम',
    description: 'Cough, Shortness of Breath, Chest Pain',
    emoji: '🫁',
  },
  ABDOMEN: {
    title: 'Stomach & Digestion',
    bilingual: 'पोटदुखी, उलटी व जुलाब',
    description: 'Abdominal Pain, Vomiting, Diarrhea',
    emoji: '🤢',
  },
  LIMBS: {
    title: 'Arms, Legs & Muscles',
    bilingual: 'हातपाय, सांधे व अंगदुखी',
    description: 'Joint Pain, Body Ache, Weakness',
    emoji: '🦵',
  },
  GENERAL: {
    title: 'Fever, Skin & Fatigue',
    bilingual: 'ताप, थंडी व पुरळ',
    description: 'Fever, Skin Rash, Fatigue, Shivering',
    emoji: '🌡️',
  },
};

export const BodyMap: React.FC<BodyMapProps> = ({
  activeRegion,
  onSelectRegion,
  selectedSymptomIds,
  onToggleSymptom,
}) => {
  const countRegionMatches = (region: Exclude<BodySystemRegion, 'ALL'>) => {
    const ids = BODY_REGION_SYMPTOMS[region];
    return selectedSymptomIds.filter((id) => ids.includes(id)).length;
  };

  const getRegionFill = (region: Exclude<BodySystemRegion, 'ALL'>) => {
    const isSelected = activeRegion === region;
    const count = countRegionMatches(region);
    if (isSelected) return '#0d9488';
    if (count > 0) return '#f59e0b';
    return '#cbd5e1';
  };

  const getRegionStroke = (region: Exclude<BodySystemRegion, 'ALL'>) => {
    const isSelected = activeRegion === region;
    const count = countRegionMatches(region);
    if (isSelected) return '#115e59';
    if (count > 0) return '#b45309';
    return '#64748b';
  };

  return (
    <div className="body-map-container" style={{
      backgroundColor: 'var(--bg-surface)',
      border: '2px solid var(--border-light)',
      borderRadius: 'var(--radius-lg)',
      padding: '1.25rem',
      marginBottom: '1.25rem',
      boxShadow: 'var(--shadow-sm)',
    }}>
      <div
        style={{
          width: '100%',
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center',
          marginBottom: '0.85rem',
          flexWrap: 'wrap',
          gap: '0.5rem',
        }}
      >
        <div>
          <span
            style={{
              fontSize: '0.75rem',
              fontWeight: 800,
              color: 'var(--primary)',
              textTransform: 'uppercase',
              letterSpacing: '0.06em',
              display: 'block',
            }}
          >
            Touch Where You Feel Discomfort • कुठे त्रास होतोय ते निवडा
          </span>
          <strong style={{ fontSize: '1rem', color: 'var(--text-main)', display: 'flex', alignItems: 'center', gap: '0.4rem' }}>
            <span>{BODY_REGION_LABELS[activeRegion].emoji}</span>
            <span>{BODY_REGION_LABELS[activeRegion].title}</span>
            <span style={{ fontSize: '0.85rem', color: 'var(--text-subtle)', fontWeight: 500 }}>
              ({BODY_REGION_LABELS[activeRegion].bilingual})
            </span>
          </strong>
        </div>

        {activeRegion !== 'ALL' && (
          <button
            type="button"
            onClick={() => onSelectRegion('ALL')}
            className="btn btn-outline"
            style={{
              padding: '0.25rem 0.75rem',
              borderRadius: '9999px',
              fontSize: '0.78rem',
              fontWeight: 700,
              minHeight: '32px',
            }}
          >
            Show All (सर्व दाखवा)
          </button>
        )}
      </div>

      {/* Interactive Anatomical SVG + Region Pills */}
      <div
        style={{
          display: 'grid',
          gridTemplateColumns: 'minmax(130px, 150px) 1fr',
          gap: '1.25rem',
          alignItems: 'center',
          width: '100%',
        }}
      >
        {/* Human Silhouette SVG with Pulsing Markers */}
        <div style={{ display: 'flex', justifyContent: 'center', position: 'relative' }}>
          <svg
            width="140"
            height="240"
            viewBox="0 0 140 240"
            role="img"
            aria-label="Interactive anatomical body selector"
            style={{ filter: 'drop-shadow(0 4px 6px rgba(0,0,0,0.06))' }}
          >
            {/* Systemic Aura Ring */}
            <circle
              cx="70"
              cy="115"
              r="66"
              fill={activeRegion === 'GENERAL' ? 'rgba(13, 148, 136, 0.12)' : 'none'}
              stroke={activeRegion === 'GENERAL' ? '#0d9488' : '#e2e8f0'}
              strokeWidth="2.5"
              strokeDasharray="5 4"
              style={{ cursor: 'pointer' }}
              onClick={() => onSelectRegion(activeRegion === 'GENERAL' ? 'ALL' : 'GENERAL')}
            />

            {/* HEAD & NECK */}
            <g
              className="body-region-svg"
              style={{ cursor: 'pointer', transition: 'transform 0.15s ease' }}
              onClick={() => onSelectRegion(activeRegion === 'HEAD' ? 'ALL' : 'HEAD')}
            >
              <title>Head, Neuro & ENT (डोके व घसा)</title>
              <circle
                cx="70"
                cy="30"
                r="20"
                fill={getRegionFill('HEAD')}
                stroke={getRegionStroke('HEAD')}
                strokeWidth="2.5"
              />
              <rect
                x="63"
                y="50"
                width="14"
                height="10"
                rx="4"
                fill={getRegionFill('HEAD')}
                stroke={getRegionStroke('HEAD')}
                strokeWidth="2"
              />
            </g>

            {/* CHEST / THORAX */}
            <g
              className="body-region-svg"
              style={{ cursor: 'pointer', transition: 'transform 0.15s ease' }}
              onClick={() => onSelectRegion(activeRegion === 'CHEST' ? 'ALL' : 'CHEST')}
            >
              <title>Chest & Respiratory (छाती व श्वसन)</title>
              <path
                d="M 44 62 Q 70 58 96 62 L 92 104 L 48 104 Z"
                fill={getRegionFill('CHEST')}
                stroke={getRegionStroke('CHEST')}
                strokeWidth="2.5"
              />
            </g>

            {/* ABDOMEN / GI */}
            <g
              className="body-region-svg"
              style={{ cursor: 'pointer', transition: 'transform 0.15s ease' }}
              onClick={() => onSelectRegion(activeRegion === 'ABDOMEN' ? 'ALL' : 'ABDOMEN')}
            >
              <title>Abdomen & Digestive (पोट व पचन)</title>
              <path
                d="M 48 106 L 92 106 L 88 144 Q 70 150 52 144 Z"
                fill={getRegionFill('ABDOMEN')}
                stroke={getRegionStroke('ABDOMEN')}
                strokeWidth="2.5"
              />
            </g>

            {/* LIMBS (ARMS & LEGS) */}
            <g
              className="body-region-svg"
              style={{ cursor: 'pointer', transition: 'transform 0.15s ease' }}
              onClick={() => onSelectRegion(activeRegion === 'LIMBS' ? 'ALL' : 'LIMBS')}
            >
              <title>Arms, Legs & Joints (हातापायांचे सांधे)</title>
              {/* Left Arm */}
              <rect
                x="25"
                y="64"
                width="15"
                height="68"
                rx="7"
                fill={getRegionFill('LIMBS')}
                stroke={getRegionStroke('LIMBS')}
                strokeWidth="2"
              />
              {/* Right Arm */}
              <rect
                x="100"
                y="64"
                width="15"
                height="68"
                rx="7"
                fill={getRegionFill('LIMBS')}
                stroke={getRegionStroke('LIMBS')}
                strokeWidth="2"
              />
              {/* Left Leg */}
              <rect
                x="50"
                y="148"
                width="17"
                height="80"
                rx="8"
                fill={getRegionFill('LIMBS')}
                stroke={getRegionStroke('LIMBS')}
                strokeWidth="2"
              />
              {/* Right Leg */}
              <rect
                x="73"
                y="148"
                width="17"
                height="80"
                rx="8"
                fill={getRegionFill('LIMBS')}
                stroke={getRegionStroke('LIMBS')}
                strokeWidth="2"
              />
            </g>
          </svg>
        </div>

        {/* Region Selector Pills */}
        <div style={{ display: 'flex', flexDirection: 'column', gap: '0.45rem' }}>
          {(['ALL', 'HEAD', 'CHEST', 'ABDOMEN', 'LIMBS', 'GENERAL'] as BodySystemRegion[]).map(
            (region) => {
              const isSelected = activeRegion === region;
              const matchCount =
                region === 'ALL'
                  ? selectedSymptomIds.length
                  : countRegionMatches(region);

              return (
                <button
                  key={region}
                  type="button"
                  onClick={() => onSelectRegion(region)}
                  style={{
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'space-between',
                    padding: '0.55rem 0.85rem',
                    borderRadius: 'var(--radius-md)',
                    border: isSelected
                      ? '2px solid var(--primary)'
                      : '1px solid var(--border-light)',
                    backgroundColor: isSelected ? 'var(--primary-light)' : 'var(--bg-surface)',
                    color: isSelected ? 'var(--primary-hover)' : 'var(--text-main)',
                    fontWeight: isSelected ? 800 : 600,
                    fontSize: '0.85rem',
                    cursor: 'pointer',
                    textAlign: 'left',
                    transition: 'all 0.15s ease',
                  }}
                >
                  <div style={{ display: 'flex', alignItems: 'center', gap: '0.45rem' }}>
                    <span>{BODY_REGION_LABELS[region].emoji}</span>
                    <div>
                      <div>{BODY_REGION_LABELS[region].title}</div>
                      <div style={{ fontSize: '0.72rem', color: 'var(--text-subtle)' }}>
                        {BODY_REGION_LABELS[region].bilingual}
                      </div>
                    </div>
                  </div>
                  {matchCount > 0 && (
                    <span
                      style={{
                        padding: '0.15rem 0.55rem',
                        borderRadius: '9999px',
                        fontSize: '0.75rem',
                        fontWeight: 800,
                        backgroundColor: isSelected ? 'var(--primary)' : '#fef3c7',
                        color: isSelected ? '#ffffff' : '#92400e',
                      }}
                    >
                      {matchCount} active
                    </span>
                  )}
                </button>
              );
            }
          )}
        </div>
      </div>
    </div>
  );
};
