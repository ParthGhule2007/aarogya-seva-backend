import React from 'react';
import { AiAnalysisResponse } from '../../types/ai';
import { Badge } from '../common/Badge';
import { Activity, AlertTriangle, CheckSquare, Stethoscope, Sparkles, Building2 } from 'lucide-react';

interface AiAnalysisCardProps {
  analysis: AiAnalysisResponse;
}

export const AiAnalysisCard: React.FC<AiAnalysisCardProps> = ({ analysis }) => {
  const riskScore = analysis.riskAssessment?.riskScore || 0;
  const riskLevel = analysis.riskAssessment?.riskLevel || 'LOW';

  // Calculate risk color
  const getRiskColor = (score: number) => {
    if (score >= 80) return 'var(--danger)';
    if (score >= 60) return '#ea580c'; // orange
    if (score >= 40) return 'var(--warning)';
    return 'var(--success)';
  };

  return (
    <div className="card" style={{ borderLeft: `6px solid ${getRiskColor(riskScore)}` }}>
      <div className="card-header" style={{ paddingBottom: '0.75rem', borderBottom: '1px solid var(--border-light)' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
          <div
            style={{
              width: '42px',
              height: '42px',
              borderRadius: 'var(--radius-md)',
              backgroundColor: 'var(--primary-light)',
              color: 'var(--primary-dark)',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
            }}
          >
            <Sparkles size={22} />
          </div>
          <div>
            <h3 style={{ fontSize: '1.25rem', fontWeight: 800 }}>AI Clinical Screening Intelligence</h3>
            <p style={{ fontSize: '0.8rem', color: 'var(--text-subtle)' }}>
              Model: {analysis.modelVersion} • Confidence: {(analysis.confidence * 100).toFixed(0)}%
            </p>
          </div>
        </div>

        <div style={{ display: 'flex', gap: '0.5rem', flexWrap: 'wrap' }}>
          <Badge variant="risk" value={riskLevel} />
          <Badge variant="urgency" value={analysis.urgency?.level} />
        </div>
      </div>

      <div style={{ marginTop: '1.25rem' }}>
        {/* Risk & Urgency Overview */}
        <div className="grid-2" style={{ marginBottom: '1.25rem' }}>
          <div
            style={{
              padding: '1rem',
              backgroundColor: 'var(--bg-surface-subtle)',
              borderRadius: 'var(--radius-md)',
              border: '1px solid var(--border-light)',
            }}
          >
            <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '0.5rem' }}>
              <span style={{ fontSize: '0.85rem', fontWeight: 700, color: 'var(--text-muted)' }}>
                CLINICAL RISK SCORE
              </span>
              <span style={{ fontSize: '1.25rem', fontWeight: 800, color: getRiskColor(riskScore) }}>
                {riskScore.toFixed(1)} / 100
              </span>
            </div>
            <div
              style={{
                width: '100%',
                height: '8px',
                backgroundColor: 'var(--border-light)',
                borderRadius: '4px',
                overflow: 'hidden',
              }}
            >
              <div
                style={{
                  width: `${Math.min(100, Math.max(0, riskScore))}%`,
                  height: '100%',
                  backgroundColor: getRiskColor(riskScore),
                  borderRadius: '4px',
                  transition: 'width 0.5s ease',
                }}
              />
            </div>
          </div>

          <div
            style={{
              padding: '1rem',
              backgroundColor: 'var(--bg-surface-subtle)',
              borderRadius: 'var(--radius-md)',
              border: '1px solid var(--border-light)',
            }}
          >
            <div style={{ fontSize: '0.85rem', fontWeight: 700, color: 'var(--text-muted)', marginBottom: '0.35rem' }}>
              TRIAGE URGENCY RATIONALE
            </div>
            <p style={{ fontSize: '0.9rem', color: 'var(--text-main)', fontWeight: 500 }}>
              {analysis.urgency?.reason || 'Evaluation based on recorded symptom pattern.'}
            </p>
          </div>
        </div>

        {/* Possible Disease Categories */}
        {analysis.possibleDiseases && analysis.possibleDiseases.length > 0 && (
          <div style={{ marginBottom: '1.25rem' }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', marginBottom: '0.6rem' }}>
              <Activity size={18} style={{ color: 'var(--primary)' }} />
              <h4 style={{ fontSize: '0.95rem' }}>Identified Potential Disease Categories</h4>
            </div>
            <div style={{ display: 'flex', flexDirection: 'column', gap: '0.5rem' }}>
              {analysis.possibleDiseases.map((d, i) => (
                <div
                  key={i}
                  style={{
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'space-between',
                    padding: '0.65rem 0.85rem',
                    backgroundColor: 'var(--bg-surface)',
                    border: '1px solid var(--border-light)',
                    borderRadius: 'var(--radius-md)',
                  }}
                >
                  <span style={{ fontWeight: 600, fontSize: '0.9rem' }}>{d.category}</span>
                  <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
                    <div
                      style={{
                        width: '100px',
                        height: '6px',
                        backgroundColor: 'var(--border-light)',
                        borderRadius: '3px',
                        overflow: 'hidden',
                      }}
                    >
                      <div
                        style={{
                          width: `${(d.probability * 100).toFixed(0)}%`,
                          height: '100%',
                          backgroundColor: 'var(--primary)',
                        }}
                      />
                    </div>
                    <span style={{ fontSize: '0.85rem', fontWeight: 700, color: 'var(--text-muted)', minWidth: '40px' }}>
                      {(d.probability * 100).toFixed(0)}%
                    </span>
                  </div>
                </div>
              ))}
            </div>
          </div>
        )}

        {/* Required Facility Types & Recommended Services */}
        <div className="grid-2" style={{ marginBottom: '1.25rem' }}>
          <div>
            <div style={{ display: 'flex', alignItems: 'center', gap: '0.4rem', marginBottom: '0.5rem' }}>
              <Building2 size={16} style={{ color: 'var(--secondary)' }} />
              <span style={{ fontSize: '0.85rem', fontWeight: 700, color: 'var(--text-muted)' }}>
                REQUIRED FACILITY TIERS (AI DETERMINED)
              </span>
            </div>
            <div style={{ display: 'flex', flexWrap: 'wrap', gap: '0.4rem' }}>
              {analysis.requiredFacilityTypes && analysis.requiredFacilityTypes.length > 0 ? (
                analysis.requiredFacilityTypes.map((t, idx) => (
                  <span
                    key={idx}
                    style={{
                      padding: '0.35rem 0.75rem',
                      borderRadius: 'var(--radius-sm)',
                      backgroundColor: 'var(--secondary-light)',
                      color: 'var(--secondary)',
                      fontWeight: 700,
                      fontSize: '0.85rem',
                    }}
                  >
                    {t.replace(/_/g, ' ')}
                  </span>
                ))
              ) : (
                <span style={{ fontSize: '0.85rem', color: 'var(--text-subtle)' }}>Any registered health facility</span>
              )}
            </div>
          </div>

          <div>
            <div style={{ display: 'flex', alignItems: 'center', gap: '0.4rem', marginBottom: '0.5rem' }}>
              <Stethoscope size={16} style={{ color: 'var(--primary)' }} />
              <span style={{ fontSize: '0.85rem', fontWeight: 700, color: 'var(--text-muted)' }}>
                RECOMMENDED MEDICAL SERVICES
              </span>
            </div>
            <div style={{ display: 'flex', flexWrap: 'wrap', gap: '0.4rem' }}>
              {analysis.recommendedServices && analysis.recommendedServices.length > 0 ? (
                analysis.recommendedServices.map((s, idx) => (
                  <span
                    key={idx}
                    style={{
                      padding: '0.35rem 0.65rem',
                      borderRadius: 'var(--radius-sm)',
                      backgroundColor: 'var(--primary-light)',
                      color: 'var(--primary-dark)',
                      fontWeight: 600,
                      fontSize: '0.8rem',
                    }}
                  >
                    {s.replace(/_/g, ' ')}
                  </span>
                ))
              ) : (
                <span style={{ fontSize: '0.85rem', color: 'var(--text-subtle)' }}>General Consultation</span>
              )}
            </div>
          </div>
        </div>

        {/* Clinical Precautions */}
        {analysis.precautions && analysis.precautions.length > 0 && (
          <div>
            <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', marginBottom: '0.5rem' }}>
              <CheckSquare size={16} style={{ color: 'var(--warning)' }} />
              <span style={{ fontSize: '0.85rem', fontWeight: 700, color: 'var(--text-muted)' }}>
                IMMEDIATE CLINICAL PRECAUTIONS
              </span>
            </div>
            <ul style={{ listStyleType: 'none', paddingLeft: 0, display: 'flex', flexDirection: 'column', gap: '0.35rem' }}>
              {analysis.precautions.map((p, idx) => (
                <li
                  key={idx}
                  style={{
                    display: 'flex',
                    alignItems: 'flex-start',
                    gap: '0.5rem',
                    fontSize: '0.875rem',
                    color: 'var(--text-main)',
                  }}
                >
                  <span style={{ color: 'var(--primary)', fontWeight: 700 }}>•</span>
                  <span>{p}</span>
                </li>
              ))}
            </ul>
          </div>
        )}
      </div>
    </div>
  );
};
