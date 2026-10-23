import React, { useState } from 'react';
import { Send, CheckCircle2, ArrowLeft, Building2, AlertTriangle, ArrowRight, ShieldCheck } from 'lucide-react';
import { FacilityRecommendationDto } from '../../types/facility';
import { AiAnalysisResponse } from '../../types/ai';
import { referralApi } from '../../api/referralApi';
import { ReferralResponseDto } from '../../types/referral';
import { AlertBox } from '../common/AlertBox';

interface Step5CareRequestSentProps {
  caseId: string;
  facility: FacilityRecommendationDto;
  analysis: AiAnalysisResponse;
  onTrackRequests: () => void;
  onBack: () => void;
}

export const Step5CareRequestSent: React.FC<Step5CareRequestSentProps> = ({
  caseId,
  facility,
  analysis,
  onTrackRequests,
  onBack,
}) => {
  const [patientMessage, setPatientMessage] = useState(
    'I need medical evaluation for my symptoms.'
  );
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [createdReferral, setCreatedReferral] = useState<ReferralResponseDto | null>(null);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setErrorMessage(null);
    setIsSubmitting(true);

    try {
      const res = await referralApi.createReferral({
        caseId,
        facilityId: facility.facilityId,
        patientMessage: patientMessage.trim() || undefined,
      });

      setCreatedReferral(res);
    } catch (err: any) {
      setErrorMessage(err.message || 'Could not send care request. Please try again.');
    } finally {
      setIsSubmitting(false);
    }
  };

  // If request has been sent, show the clean success confirmation
  if (createdReferral) {
    return (
      <div className="card-simple" style={{ maxWidth: '640px', margin: '0 auto', textAlign: 'center', padding: '2.5rem 1.5rem' }}>
        <div
          style={{
            width: '72px',
            height: '72px',
            borderRadius: '50%',
            backgroundColor: 'var(--secondary-light)',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            margin: '0 auto 1.25rem auto',
          }}
        >
          <CheckCircle2 size={44} color="var(--secondary)" strokeWidth={2.5} />
        </div>

        <h2 style={{ fontSize: '1.8rem', fontWeight: 800, color: 'var(--secondary)', marginBottom: '0.4rem' }}>
          ✓ Request Sent • विनंती पाठवली गेली
        </h2>
        <p style={{ fontSize: '1.2rem', fontWeight: 600, color: 'var(--text-main)', marginBottom: '0.5rem' }}>
          Your request has been sent to <strong>{facility.name || facility.facilityName || 'Healthcare Centre'}</strong>.
        </p>
        <p style={{ fontSize: '1rem', color: 'var(--text-subtle)', marginBottom: '2rem' }}>
          आपली काळजी विनंती दवाखान्याकडे पोहोचली आहे. दवाखान्यातील डॉक्टर किंवा आरोग्य कर्मचारी यावर पुढील कार्यवाही करतील.
        </p>

        <div
          style={{
            backgroundColor: 'var(--bg-subtle)',
            borderRadius: 'var(--radius-md)',
            padding: '1.25rem',
            textAlign: 'left',
            marginBottom: '2rem',
            fontSize: '1rem',
          }}
        >
          <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '0.5rem' }}>
            <span style={{ color: 'var(--text-muted)' }}>Status:</span>
            <span className="status-pill pending">Waiting for Facility • दवाखान्याची वाट पाहत आहे</span>
          </div>
          <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '0.5rem' }}>
            <span style={{ color: 'var(--text-muted)' }}>Referral Reference:</span>
            <strong style={{ fontFamily: 'monospace', fontSize: '0.9rem' }}>{createdReferral.id}</strong>
          </div>
          <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '0.5rem' }}>
            <span style={{ color: 'var(--text-muted)' }}>Receiving Centre:</span>
            <strong>{facility.name}</strong>
          </div>
          <div style={{ display: 'flex', justifyContent: 'space-between' }}>
            <span style={{ color: 'var(--text-muted)' }}>Screening Risk:</span>
            <strong style={{ color: 'var(--risk-high)' }}>{analysis.riskLevel} ({analysis.urgencyLevel})</strong>
          </div>
        </div>

        <button
          className="btn-large btn-primary"
          style={{ width: '100%', fontSize: '1.25rem' }}
          onClick={onTrackRequests}
        >
          <span>TRACK MY REQUEST • माझी विनंती तपासा</span>
          <ArrowRight size={22} />
        </button>
      </div>
    );
  }

  // Confirmation Form before sending
  return (
    <form onSubmit={handleSubmit} className="card-simple" style={{ maxWidth: '640px', margin: '0 auto' }}>
      <h2 style={{ fontSize: '1.6rem', fontWeight: 800, color: 'var(--primary)', marginBottom: '0.4rem' }}>
        Step 5: Confirm Care Request
      </h2>
      <p style={{ color: 'var(--text-muted)', fontSize: '1.05rem', marginBottom: '1.5rem' }}>
        पुष्टी करा: दवाखान्याला विनंती पाठवा
      </p>

      {errorMessage && (
        <AlertBox type="danger">
          <strong>{errorMessage}</strong>
        </AlertBox>
      )}

      {/* Target Facility Summary */}
      <div
        style={{
          backgroundColor: 'var(--primary-light)',
          border: '2px solid var(--primary-border)',
          borderRadius: 'var(--radius-md)',
          padding: '1.25rem',
          marginBottom: '1.5rem',
        }}
      >
        <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem', marginBottom: '0.5rem' }}>
          <Building2 size={24} color="var(--primary)" />
          <strong style={{ fontSize: '1.25rem', color: 'var(--primary-hover)' }}>
            {facility.name}
          </strong>
        </div>
        <p style={{ fontSize: '1rem', color: 'var(--text-main)', margin: 0 }}>
          {facility.distanceKm ? `${facility.distanceKm.toFixed(1)} km away` : 'Nearby'} • {facility.facilityType || 'Healthcare Centre'}
        </p>
      </div>

      {/* Screening Summary */}
      <div
        style={{
          backgroundColor: 'var(--bg-subtle)',
          borderRadius: 'var(--radius-md)',
          padding: '1rem',
          marginBottom: '1.5rem',
        }}
      >
        <span style={{ fontSize: '0.85rem', color: 'var(--text-subtle)', fontWeight: 700, display: 'block', marginBottom: '0.25rem' }}>
          YOUR CLINICAL SCREENING:
        </span>
        <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
          <AlertTriangle size={20} color="var(--risk-high)" />
          <strong style={{ fontSize: '1.1rem', color: 'var(--risk-high)' }}>
            {analysis.riskLevel} RISK • {analysis.urgencyLevel}
          </strong>
        </div>
      </div>

      {/* Patient Message */}
      <div className="form-group">
        <label htmlFor="patient-message" className="form-label">
          Message for the Doctor / Facility (डॉक्टरांसाठी संदेश)
        </label>
        <textarea
          id="patient-message"
          className="form-textarea"
          rows={3}
          value={patientMessage}
          onChange={(e) => setPatientMessage(e.target.value)}
          placeholder="Describe any additional details or urgency..."
        />
      </div>

      {/* Action Buttons */}
      <div className="btn-row" style={{ display: 'flex', gap: '1rem', marginTop: '2rem' }}>
        <button
          type="button"
          className="btn-large btn-ghost"
          onClick={onBack}
          style={{ flex: 1 }}
          disabled={isSubmitting}
        >
          <ArrowLeft size={22} />
          <span>GO BACK • मागे जा</span>
        </button>

        <button
          type="submit"
          className="btn-large btn-primary"
          style={{ flex: 2 }}
          disabled={isSubmitting}
        >
          {isSubmitting ? (
            <span>Sending Request...</span>
          ) : (
            <>
              <Send size={22} />
              <span>SEND CARE REQUEST • विनंती पाठवा</span>
            </>
          )}
        </button>
      </div>
    </form>
  );
};
