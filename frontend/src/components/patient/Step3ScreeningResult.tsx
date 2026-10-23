import React, { useState } from 'react';
import {
  AlertTriangle,
  CheckCircle2,
  AlertOctagon,
  Info,
  ShieldCheck,
  ChevronDown,
  ChevronUp,
  ArrowRight,
  ArrowLeft,
  Activity,
  HeartHandshake
} from 'lucide-react';
import { AiAnalysisResponse } from '../../types/ai';

interface Step3ScreeningResultProps {
  analysis: AiAnalysisResponse;
  onContinueToFacilities: () => void;
  onBack: () => void;
}

export const Step3ScreeningResult: React.FC<Step3ScreeningResultProps> = ({
  analysis,
  onContinueToFacilities,
  onBack,
}) => {
  const [showTechnicalDetails, setShowTechnicalDetails] = useState(false);

  const riskLevel = analysis.riskLevel || analysis.riskAssessment?.riskLevel || 'MODERATE';
  const riskScore = analysis.riskScore ?? analysis.riskAssessment?.riskScore ?? 50;
  const urgencyLevel = analysis.urgencyLevel || analysis.urgency?.level || 'PRIORITY';
  const urgencyReason =
    analysis.urgencyReason ||
    analysis.urgency?.reason ||
    analysis.triageRationale ||
    analysis.explanation ||
    'Your symptoms should be evaluated by a healthcare professional soon.';
  const diseases = analysis.probableDiseases || analysis.possibleDiseases || [];
  const precautions = analysis.precautions || [];
  const confidence = analysis.confidenceScore ?? analysis.confidence ?? 0.85;
  const modelName = analysis.modelName || analysis.modelVersion || 'Clinical AI Screening Engine';

  // Map risk level to human styling
  const getRiskDisplay = () => {
    switch (riskLevel) {
      case 'CRITICAL':
        return {
          icon: <AlertOctagon size={36} color="var(--risk-critical)" />,
          badgeClass: 'status-pill critical',
          label: 'CRITICAL RISK • अत्यंत गंभीर धोका',
          color: 'var(--risk-critical)',
          bg: 'var(--risk-critical-bg)',
          border: 'var(--risk-critical-border)',
        };
      case 'HIGH':
        return {
          icon: <AlertTriangle size={36} color="var(--risk-high)" />,
          badgeClass: 'status-pill high',
          label: 'HIGH RISK • मोठा धोका',
          color: 'var(--risk-high)',
          bg: 'var(--risk-high-bg)',
          border: 'var(--risk-high-border)',
        };
      case 'MODERATE':
        return {
          icon: <AlertTriangle size={36} color="var(--risk-moderate)" />,
          badgeClass: 'status-pill moderate',
          label: 'MODERATE RISK • मध्यम धोका',
          color: 'var(--risk-moderate)',
          bg: 'var(--risk-moderate-bg)',
          border: 'var(--risk-moderate-border)',
        };
      default:
        return {
          icon: <CheckCircle2 size={36} color="var(--risk-low)" />,
          badgeClass: 'status-pill low',
          label: 'LOW RISK • कमी धोका',
          color: 'var(--risk-low)',
          bg: 'var(--risk-low-bg)',
          border: 'var(--risk-low-border)',
        };
    }
  };

  const riskStyle = getRiskDisplay();

  return (
    <div className="card-simple" style={{ maxWidth: '680px', margin: '0 auto' }}>
      <h2 style={{ fontSize: '1.6rem', fontWeight: 800, color: 'var(--primary)', marginBottom: '0.4rem' }}>
        Step 3: Your Screening Result
      </h2>
      <p style={{ color: 'var(--text-muted)', fontSize: '1.05rem', marginBottom: '1.5rem' }}>
        तपासणी निकाल: आपल्या लक्षणांचे प्राथमिक विश्लेषण
      </p>

      {/* Main Risk Card */}
      <div
        style={{
          backgroundColor: riskStyle.bg,
          border: `2px solid ${riskStyle.border}`,
          borderRadius: 'var(--radius-lg)',
          padding: '1.5rem',
          marginBottom: '1.5rem',
        }}
      >
        <div style={{ display: 'flex', alignItems: 'center', gap: '1rem', marginBottom: '0.75rem' }}>
          {riskStyle.icon}
          <div>
            <span className={riskStyle.badgeClass} style={{ fontSize: '1.1rem', padding: '0.4rem 1rem' }}>
              {riskStyle.label}
            </span>
          </div>
        </div>

        <div style={{ marginTop: '0.75rem' }}>
          <strong style={{ fontSize: '1.15rem', color: 'var(--text-main)', display: 'block', marginBottom: '0.25rem' }}>
            Urgency Level: {urgencyLevel} (निकड: {urgencyLevel === 'URGENT' || urgencyLevel === 'EMERGENCY' ? 'तातडीने' : 'लवकर'})
          </strong>
          <p style={{ fontSize: '1.05rem', color: 'var(--text-main)' }}>
            {urgencyReason}
          </p>
        </div>
      </div>

      {/* Possible Health Conditions */}
      {diseases && diseases.length > 0 && (
        <div style={{ marginBottom: '1.5rem' }}>
          <h3 style={{ fontSize: '1.2rem', fontWeight: 700, color: 'var(--text-main)', marginBottom: '0.75rem' }}>
            Possible Health Conditions (संभाव्य आजार):
          </h3>
          <div style={{ display: 'flex', flexDirection: 'column', gap: '0.5rem' }}>
            {diseases.map((disease, i) => (
              <div
                key={i}
                style={{
                  backgroundColor: 'var(--bg-subtle)',
                  padding: '0.75rem 1rem',
                  borderRadius: 'var(--radius-md)',
                  border: '1px solid var(--border-light)',
                  display: 'flex',
                  justifyContent: 'space-between',
                  alignItems: 'center',
                }}
              >
                <strong style={{ fontSize: '1.05rem' }}>• {disease.diseaseName || disease.category}</strong>
                <span style={{ fontSize: '0.9rem', color: 'var(--text-subtle)', fontWeight: 600 }}>
                  Match: {(disease.probability * 100).toFixed(0)}%
                </span>
              </div>
            ))}
          </div>
        </div>
      )}

      {/* What You Should Do (Precautions) */}
      {precautions && precautions.length > 0 && (
        <div style={{ marginBottom: '1.5rem' }}>
          <h3 style={{ fontSize: '1.2rem', fontWeight: 700, color: 'var(--secondary)', marginBottom: '0.75rem' }}>
            What You Should Do Now (काय काळजी घ्यावी):
          </h3>
          <ul style={{ listStyle: 'none', display: 'flex', flexDirection: 'column', gap: '0.5rem' }}>
            {analysis.precautions.map((precaution, idx) => (
              <li
                key={idx}
                style={{
                  display: 'flex',
                  alignItems: 'flex-start',
                  gap: '0.6rem',
                  backgroundColor: 'var(--secondary-light)',
                  padding: '0.75rem 1rem',
                  borderRadius: 'var(--radius-md)',
                  border: '1px solid var(--secondary)',
                  fontSize: '1.05rem',
                  color: 'var(--text-main)',
                }}
              >
                <CheckCircle2 size={22} color="var(--secondary)" style={{ flexShrink: 0, marginTop: '2px' }} />
                <span>{precaution}</span>
              </li>
            ))}
          </ul>
        </div>
      )}

      {/* Collapsible Technical Details */}
      <div style={{ borderTop: '2px solid var(--border-light)', paddingTop: '1rem', marginBottom: '1.5rem' }}>
        <button
          type="button"
          className="btn-ghost"
          style={{
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
            width: '100%',
            padding: '0.75rem',
            borderRadius: 'var(--radius-md)',
            fontWeight: 700,
            fontSize: '1rem',
          }}
          onClick={() => setShowTechnicalDetails(!showTechnicalDetails)}
          aria-expanded={showTechnicalDetails}
        >
          <span>View Detailed Clinical Assessment (वैद्यकीय तपशील)</span>
          {showTechnicalDetails ? <ChevronUp size={20} /> : <ChevronDown size={20} />}
        </button>

        {showTechnicalDetails && (
          <div
            style={{
              backgroundColor: 'var(--bg-subtle)',
              padding: '1rem',
              borderRadius: 'var(--radius-md)',
              marginTop: '0.75rem',
              fontSize: '0.95rem',
            }}
          >
            <p><strong>Clinical Risk Score:</strong> {riskScore} / 100</p>
            <p><strong>Confidence:</strong> {`${(confidence * 100).toFixed(0)}%`}</p>
            <p><strong>Screening Engine:</strong> {modelName}</p>
            {analysis.requiredFacilityTypes && (
              <p><strong>Required Facility Tiers:</strong> {analysis.requiredFacilityTypes.join(', ')}</p>
            )}
            {analysis.recommendedServices && (
              <p><strong>Recommended Services:</strong> {analysis.recommendedServices.join(', ')}</p>
            )}
          </div>
        )}
      </div>

      {/* Clear Medical Disclaimer */}
      <div
        style={{
          backgroundColor: 'var(--bg-subtle)',
          border: '1px dashed var(--border-medium)',
          borderRadius: 'var(--radius-md)',
          padding: '0.85rem 1rem',
          marginBottom: '1.75rem',
          fontSize: '0.9rem',
          color: 'var(--text-subtle)',
        }}
      >
        <div style={{ display: 'flex', alignItems: 'center', gap: '0.4rem', fontWeight: 700, marginBottom: '0.2rem' }}>
          <ShieldCheck size={16} color="var(--primary)" />
          <span>Clinical Decision-Support Notice</span>
        </div>
        This screening helps assess urgency and locate appropriate healthcare. It is NOT a final medical diagnosis. Always consult a certified medical officer.
      </div>

      {/* Actions */}
      <div className="btn-row" style={{ display: 'flex', gap: '1rem' }}>
        <button
          type="button"
          className="btn-large btn-ghost"
          onClick={onBack}
          style={{ flex: 1 }}
        >
          <ArrowLeft size={22} />
          <span>Back • मागे</span>
        </button>

        <button
          type="button"
          className="btn-large btn-primary"
          style={{ flex: 2 }}
          onClick={onContinueToFacilities}
        >
          <span>FIND NEARBY HEALTHCARE • दवाखाना निवडा</span>
          <ArrowRight size={22} />
        </button>
      </div>
    </div>
  );
};
