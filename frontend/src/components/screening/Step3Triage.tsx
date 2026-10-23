import React, { useState, useEffect } from 'react';
import { aiApi } from '../../api/aiApi';
import { useLanguage } from '../../context/LanguageContext';
import { AiAnalysisResponse } from '../../types/ai';
import {
  Brain,
  AlertTriangle,
  CheckCircle,
  ShieldAlert,
  HelpCircle,
  Activity,
  ArrowRight,
  RefreshCw,
  Building2,
  ListChecks,
} from 'lucide-react';

interface Step3TriageProps {
  caseId: string;
  aiAnalysis: AiAnalysisResponse | null;
  setAiAnalysis: (analysis: AiAnalysisResponse) => void;
  onNext: () => void;
  onBack: () => void;
}

export const Step3Triage: React.FC<Step3TriageProps> = ({
  caseId,
  aiAnalysis,
  setAiAnalysis,
  onNext,
  onBack,
}) => {
  const { language, t } = useLanguage();
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const runAnalysis = async () => {
    if (!caseId) return;
    setIsLoading(true);
    setError(null);

    try {
      const res = await aiApi.runAiAnalysis(caseId);
      if (res && res.riskAssessment?.riskLevel) {
        setAiAnalysis(res);
        setError(null);
      } else {
        throw new Error('AI screening returned incomplete triage data.');
      }
    } catch (err: any) {
      // If already analyzed, try fetching the latest recorded analysis
      try {
        const latest = await aiApi.getLatestAnalysis(caseId);
        if (latest && latest.riskAssessment?.riskLevel) {
          setAiAnalysis(latest);
          setError(null);
          return;
        }
      } catch {
        // ignore
      }

      const errMsg =
        err?.response?.data?.message ||
        err?.message ||
        'The clinical AI model was unable to complete triage. Please verify connection and retry.';
      setError(errMsg);
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    if (!aiAnalysis && caseId) {
      runAnalysis();
    }
  }, [caseId, aiAnalysis]);

  const rawRiskScore = aiAnalysis?.riskAssessment?.riskScore ?? aiAnalysis?.riskScore ?? 0;
  const riskScore = rawRiskScore <= 1.0 ? rawRiskScore * 100 : rawRiskScore;
  const riskLevel = aiAnalysis?.riskAssessment?.riskLevel || aiAnalysis?.riskLevel || 'LOW';
  const urgencyLevel = aiAnalysis?.urgency?.level || aiAnalysis?.urgencyLevel || 'ROUTINE';

  const getUrgencyBadge = (u: string) => {
    switch (u) {
      case 'EMERGENCY':
        return (
          <span className="badge badge-emergency" style={{ fontSize: '0.9rem', padding: '0.4rem 0.8rem' }}>
            EMERGENCY (आपातकालीन / तातडीचे)
          </span>
        );
      case 'URGENT':
      case 'PRIORITY':
        return (
          <span className="badge badge-urgent" style={{ fontSize: '0.9rem', padding: '0.4rem 0.8rem' }}>
            URGENT (अति आवश्यक / तातडीचे)
          </span>
        );
      default:
        return (
          <span className="badge badge-routine" style={{ fontSize: '0.9rem', padding: '0.4rem 0.8rem' }}>
            ROUTINE (सामान्य / नियमित)
          </span>
        );
    }
  };

  return (
    <div className="card" style={{ padding: '1.5rem' }}>
      {/* Header */}
      <div className="card-header" style={{ marginBottom: '1rem' }}>
        <div className="card-title" style={{ display: 'flex', alignItems: 'center', gap: '0.6rem' }}>
          <Brain size={24} color="var(--primary)" />
          <span style={{ fontSize: '1.25rem', fontWeight: 800 }}>{t('step3Title')}</span>
        </div>
        <span className="badge badge-routine" style={{ fontSize: '0.85rem', padding: '0.35rem 0.75rem' }}>
          Step 3 of 5
        </span>
      </div>

      <p style={{ color: 'var(--text-subtle)', marginBottom: '1.25rem', fontSize: '0.95rem' }}>
        {language === 'hi'
          ? 'क्लिनिकल एआई मॉडल द्वारा जोखिम मूल्यांकन और अस्पताल सिफारिश।'
          : language === 'mr'
          ? 'क्लिनिकल एआय मॉडेलद्वारे जोखीम मूल्यांकन आणि रुग्णालय शिफारस.'
          : 'Clinical AI risk assessment, differential diagnoses, and facility routing.'}
      </p>

      {/* Loading State */}
      {isLoading && (
        <div
          style={{
            textAlign: 'center',
            padding: '3.5rem 1rem',
            background: 'var(--bg-subtle)',
            borderRadius: 'var(--radius-md)',
            marginBottom: '1.5rem',
          }}
        >
          <RefreshCw
            size={40}
            color="var(--primary)"
            style={{ animation: 'spin 1s linear infinite', margin: '0 auto' }}
          />
          <div style={{ marginTop: '1.25rem', fontWeight: 800, fontSize: '1.15rem', color: 'var(--text-main)' }}>
            {language === 'hi'
              ? 'क्लिनिकल एआई मॉडल द्वारा लक्षणों का विश्लेषण हो रहा है...'
              : language === 'mr'
              ? 'क्लिनिकल एआय मॉडेलद्वारे लक्षणांचे विश्लेषण चालू आहे...'
              : 'Analyzing Case with Clinical AI Model...'}
          </div>
          <div style={{ fontSize: '0.9rem', color: 'var(--text-subtle)', marginTop: '0.35rem' }}>
            Evaluating symptom clusters, duration thresholds, and IPHS tele-triage rules.
          </div>
        </div>
      )}

      {/* Error Alert (No Hardcoded Fallback) */}
      {error && !isLoading && !aiAnalysis && (
        <div className="alert-box danger" style={{ marginBottom: '1.5rem' }}>
          <AlertTriangle size={24} />
          <div style={{ flex: 1 }}>
            <div style={{ fontWeight: 800, fontSize: '1rem', marginBottom: '0.25rem' }}>
              Clinical AI Screening Unavailable
            </div>
            <div style={{ fontSize: '0.9rem', marginBottom: '0.75rem' }}>{error}</div>
            <button
              type="button"
              className="btn btn-primary"
              style={{ minHeight: '40px', fontSize: '0.9rem', fontWeight: 700 }}
              onClick={runAnalysis}
            >
              <RefreshCw size={16} />
              <span>Retry AI Screening</span>
            </button>
          </div>
        </div>
      )}

      {/* Triage Results */}
      {aiAnalysis && !isLoading && (
        <div>
          {/* Top Triage Banner */}
          <div
            style={{
              background:
                urgencyLevel === 'EMERGENCY'
                  ? 'var(--risk-critical-bg)'
                  : urgencyLevel === 'URGENT' || urgencyLevel === 'PRIORITY'
                  ? 'var(--risk-moderate-bg)'
                  : 'var(--risk-low-bg)',
              border: `2px solid ${
                urgencyLevel === 'EMERGENCY'
                  ? 'var(--risk-critical-border)'
                  : urgencyLevel === 'URGENT' || urgencyLevel === 'PRIORITY'
                  ? 'var(--risk-moderate-border)'
                  : 'var(--risk-low-border)'
              }`,
              borderRadius: 'var(--radius-lg)',
              padding: '1.25rem',
              marginBottom: '1.5rem',
            }}
          >
            <div
              style={{
                display: 'flex',
                justifyContent: 'space-between',
                alignItems: 'center',
                flexWrap: 'wrap',
                gap: '0.75rem',
              }}
            >
              <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
                <ShieldAlert
                  size={32}
                  color={
                    urgencyLevel === 'EMERGENCY'
                      ? '#dc2626'
                      : urgencyLevel === 'URGENT' || urgencyLevel === 'PRIORITY'
                      ? '#d97706'
                      : '#15803d'
                  }
                />
                <div>
                  <div style={{ fontSize: '1.25rem', fontWeight: 800, color: 'var(--text-main)' }}>
                    Triage Classification: {urgencyLevel}
                  </div>
                  <div style={{ fontSize: '0.9rem', color: 'var(--text-subtle)', marginTop: '0.15rem' }}>
                    Risk Level: <strong style={{ color: 'var(--text-main)' }}>{riskLevel}</strong> (Risk Score: {riskScore.toFixed(0)}/100)
                    {aiAnalysis.modelVersion && (
                      <span style={{ marginLeft: '0.5rem', fontSize: '0.8rem', opacity: 0.85 }}>
                        • Engine: {aiAnalysis.modelVersion}
                      </span>
                    )}
                  </div>
                </div>
              </div>
              <div>{getUrgencyBadge(urgencyLevel)}</div>
            </div>

            {aiAnalysis.explanation && (
              <div
                style={{
                  marginTop: '0.85rem',
                  fontSize: '0.95rem',
                  color: 'var(--text-main)',
                  borderTop: '1px solid rgba(0,0,0,0.1)',
                  paddingTop: '0.65rem',
                  lineHeight: 1.5,
                }}
              >
                <strong>Clinical Physician Narrative:</strong> {aiAnalysis.explanation}
              </div>
            )}
          </div>

          {/* Suspected Differential Diagnoses */}
          {aiAnalysis.possibleDiseases && aiAnalysis.possibleDiseases.length > 0 && (
            <div className="card" style={{ padding: '1.25rem', marginBottom: '1.25rem', border: '1px solid var(--border-light)' }}>
              <div
                style={{
                  fontWeight: 800,
                  fontSize: '1rem',
                  marginBottom: '0.75rem',
                  display: 'flex',
                  alignItems: 'center',
                  gap: '0.5rem',
                }}
              >
                <Activity size={20} color="var(--primary)" />
                <span>Suspected Differential Diagnoses (AI Stratification):</span>
              </div>
              <div style={{ display: 'flex', flexDirection: 'column', gap: '0.5rem' }}>
                {aiAnalysis.possibleDiseases.map((d, i) => {
                  const prob = d.probability <= 1.0 ? d.probability * 100 : d.probability;
                  return (
                    <div
                      key={i}
                      style={{
                        display: 'flex',
                        justifyContent: 'space-between',
                        alignItems: 'center',
                        background: 'var(--bg-subtle)',
                        padding: '0.7rem 1rem',
                        borderRadius: 'var(--radius-sm)',
                        fontSize: '0.95rem',
                      }}
                    >
                      <span style={{ fontWeight: 700, color: 'var(--text-main)' }}>
                        {d.diseaseName || d.category}
                      </span>
                      <span
                        style={{
                          fontWeight: 800,
                          color: prob >= 70 ? 'var(--primary-hover)' : 'var(--text-subtle)',
                        }}
                      >
                        Probability: {prob.toFixed(0)}%
                      </span>
                    </div>
                  );
                })}
              </div>
            </div>
          )}

          {/* Precautions & Care Instructions */}
          {aiAnalysis.precautions && aiAnalysis.precautions.length > 0 && (
            <div className="card" style={{ padding: '1.25rem', marginBottom: '1.25rem', border: '1px solid var(--border-light)' }}>
              <div
                style={{
                  fontWeight: 800,
                  fontSize: '1rem',
                  marginBottom: '0.5rem',
                  display: 'flex',
                  alignItems: 'center',
                  gap: '0.5rem',
                }}
              >
                <ListChecks size={20} color="var(--secondary)" />
                <span>Recommended Precautions & Immediate Care:</span>
              </div>
              <ul style={{ paddingLeft: '1.25rem', fontSize: '0.95rem', color: 'var(--text-main)', lineHeight: 1.6 }}>
                {aiAnalysis.precautions.map((p, idx) => (
                  <li key={idx} style={{ marginBottom: '0.35rem' }}>{p}</li>
                ))}
              </ul>
            </div>
          )}

          {/* Recommended Facility Types */}
          {aiAnalysis.requiredFacilityTypes && aiAnalysis.requiredFacilityTypes.length > 0 && (
            <div
              style={{
                display: 'flex',
                alignItems: 'center',
                gap: '0.5rem',
                fontSize: '0.9rem',
                color: 'var(--text-subtle)',
                marginBottom: '1.5rem',
                padding: '0.75rem 1rem',
                background: 'var(--bg-subtle)',
                borderRadius: 'var(--radius-sm)',
              }}
            >
              <Building2 size={18} color="var(--primary)" />
              <span>
                Target Facility Capabilities (IPHS Standard):{' '}
                <strong style={{ color: 'var(--text-main)' }}>
                  {aiAnalysis.requiredFacilityTypes.join(', ')}
                </strong>
              </span>
            </div>
          )}
        </div>
      )}

      {/* Navigation Buttons */}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginTop: '1.5rem', gap: '1rem' }}>
        <button type="button" className="btn btn-outline" onClick={onBack}>
          <span>{t('back')}</span>
        </button>
        <button
          type="button"
          className="btn btn-primary"
          style={{ minWidth: '180px', minHeight: '48px', fontSize: '1rem', fontWeight: 700 }}
          disabled={!aiAnalysis}
          onClick={onNext}
        >
          <span>{t('next')} →</span>
          <ArrowRight size={18} />
        </button>
      </div>
    </div>
  );
};
