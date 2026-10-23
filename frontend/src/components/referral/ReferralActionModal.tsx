import React, { useState } from 'react';
import { Modal } from '../common/Modal';
import { Button } from '../common/Button';
import { ReferralResponseDto, ReferralStatus } from '../../types/referral';
import { referralApi } from '../../api/referralApi';
import {
  CheckCircle2,
  XCircle,
  Play,
  CheckCheck,
  AlertTriangle,
  ArrowRight,
  Building2,
  Calendar,
  FileText
} from 'lucide-react';

interface ReferralActionModalProps {
  isOpen: boolean;
  onClose: () => void;
  referral: ReferralResponseDto | null;
  targetAction: ReferralStatus | null;
  onSuccess: (updated: ReferralResponseDto) => void;
}

export const ReferralActionModal: React.FC<ReferralActionModalProps> = ({
  isOpen,
  onClose,
  referral,
  targetAction,
  onSuccess,
}) => {
  if (!referral || !targetAction) return null;

  const [responseNote, setResponseNote] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const getActionTitle = () => {
    switch (targetAction) {
      case 'ACCEPTED':
        return 'Accept Patient Referral • रुग्ण स्वीकृती';
      case 'REJECTED':
        return 'Reject Patient Referral • नकार नोंदवा';
      case 'IN_PROGRESS':
        return 'Start Clinical Care • उपचार सुरू करा';
      case 'COMPLETED':
        return 'Complete Referral Case • उपचार पूर्ण';
      default:
        return 'Update Referral Status';
    }
  };

  const getActionSubtitle = () => {
    const refId = referral.referralToken || `REF-${referral.id.substring(0, 8).toUpperCase()}`;
    return `${refId} • Receiving Centre: ${referral.facilityName || 'Current Facility'}`;
  };

  const getButtonVariant = (): 'primary' | 'secondary' | 'success' | 'danger' | 'neutral' => {
    switch (targetAction) {
      case 'ACCEPTED':
      case 'COMPLETED':
        return 'success';
      case 'REJECTED':
        return 'danger';
      case 'IN_PROGRESS':
        return 'primary';
      default:
        return 'primary';
    }
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setIsLoading(true);
    setError(null);

    try {
      const updated = await referralApi.updateReferralStatus(referral.id, {
        status: targetAction,
        facilityResponse: responseNote.trim() || undefined,
      });
      onSuccess(updated);
      onClose();
    } catch (err: any) {
      setError(err.message || 'Failed to update referral status. Please try again.');
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <Modal
      isOpen={isOpen}
      onClose={onClose}
      title={getActionTitle()}
      subtitle={getActionSubtitle()}
      maxWidth="580px"
    >
      {error && (
        <div
          style={{
            padding: '0.85rem 1rem',
            backgroundColor: '#fee2e2',
            border: '1px solid #fca5a5',
            borderRadius: 'var(--radius-md)',
            color: '#991b1b',
            fontSize: '0.9rem',
            marginBottom: '1.25rem',
            display: 'flex',
            alignItems: 'center',
            gap: '0.5rem',
          }}
        >
          <AlertTriangle size={18} />
          <span>{error}</span>
        </div>
      )}

      <form onSubmit={handleSubmit} style={{ display: 'flex', flexDirection: 'column', gap: '1.25rem' }}>
        {/* Referral Summary Card */}
        <div
          style={{
            backgroundColor: 'var(--bg-subtle)',
            borderRadius: 'var(--radius-md)',
            padding: '1rem',
            border: '1px solid var(--border-light)',
            display: 'flex',
            flexDirection: 'column',
            gap: '0.6rem',
          }}
        >
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: '0.5rem' }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: '0.4rem', fontSize: '0.85rem', color: 'var(--text-muted)' }}>
              <Building2 size={15} color="var(--primary)" />
              <span>{referral.facilityName}</span>
            </div>
            <div style={{ display: 'flex', alignItems: 'center', gap: '0.4rem', fontSize: '0.85rem', color: 'var(--text-subtle)' }}>
              <Calendar size={14} />
              <span>{referral.requestedAt ? new Date(referral.requestedAt).toLocaleString() : 'Recently'}</span>
            </div>
          </div>

          <div
            style={{
              display: 'flex',
              alignItems: 'center',
              gap: '0.6rem',
              paddingTop: '0.5rem',
              borderTop: '1px solid var(--border-light)',
              fontSize: '0.9rem',
            }}
          >
            <span style={{ color: 'var(--text-muted)', fontWeight: 600 }}>Status Transition:</span>
            <span
              style={{
                padding: '0.2rem 0.6rem',
                borderRadius: 'var(--radius-full)',
                backgroundColor: 'var(--border-light)',
                color: 'var(--text-main)',
                fontWeight: 700,
                fontSize: '0.8rem',
              }}
            >
              {referral.status}
            </span>
            <ArrowRight size={16} color="var(--text-subtle)" />
            <span
              style={{
                padding: '0.2rem 0.6rem',
                borderRadius: 'var(--radius-full)',
                backgroundColor:
                  targetAction === 'ACCEPTED'
                    ? '#dcfce7'
                    : targetAction === 'REJECTED'
                    ? '#fee2e2'
                    : targetAction === 'IN_PROGRESS'
                    ? '#ccfbf1'
                    : '#e0e7ff',
                color:
                  targetAction === 'ACCEPTED'
                    ? '#166534'
                    : targetAction === 'REJECTED'
                    ? '#991b1b'
                    : targetAction === 'IN_PROGRESS'
                    ? '#115e59'
                    : '#3730a3',
                fontWeight: 800,
                fontSize: '0.82rem',
              }}
            >
              {targetAction}
            </span>
          </div>
        </div>

        {/* Action Guidance Callout */}
        {targetAction === 'ACCEPTED' && (
          <div
            style={{
              padding: '0.9rem 1rem',
              backgroundColor: '#ecfdf5',
              border: '1px solid #a7f3d0',
              borderRadius: 'var(--radius-md)',
              color: '#065f46',
              fontSize: '0.875rem',
              display: 'flex',
              alignItems: 'flex-start',
              gap: '0.65rem',
              lineHeight: 1.5,
            }}
          >
            <CheckCircle2 size={18} color="#059669" style={{ flexShrink: 0, marginTop: '2px' }} />
            <div>
              <strong>Accepting Patient Referral:</strong> You are confirming admission capacity at {referral.facilityName}. The referring clinician, patient, and ASHA worker will receive immediate notification with your arrival guidance.
            </div>
          </div>
        )}

        {targetAction === 'REJECTED' && (
          <div
            style={{
              padding: '0.9rem 1rem',
              backgroundColor: '#fff1f2',
              border: '1px solid #fecdd3',
              borderRadius: 'var(--radius-md)',
              color: '#9f1239',
              fontSize: '0.875rem',
              display: 'flex',
              alignItems: 'flex-start',
              gap: '0.65rem',
              lineHeight: 1.5,
            }}
          >
            <AlertTriangle size={18} color="#e11d48" style={{ flexShrink: 0, marginTop: '2px' }} />
            <div>
              <strong>Rejection Impact Notice:</strong> Rejecting this request frees the active case so the patient can be promptly navigated to another facility. A specific reason note is <strong>mandatory</strong>.
            </div>
          </div>
        )}

        {targetAction === 'IN_PROGRESS' && (
          <div
            style={{
              padding: '0.9rem 1rem',
              backgroundColor: '#f0fdfa',
              border: '1px solid #99f6e4',
              borderRadius: 'var(--radius-md)',
              color: '#134e4a',
              fontSize: '0.875rem',
              display: 'flex',
              alignItems: 'flex-start',
              gap: '0.65rem',
              lineHeight: 1.5,
            }}
          >
            <Play size={18} color="var(--primary)" style={{ flexShrink: 0, marginTop: '2px' }} />
            <div>
              <strong>Commence Active Care:</strong> Record that the patient has presented at the centre and active triage/treatment is underway.
            </div>
          </div>
        )}

        {targetAction === 'COMPLETED' && (
          <div
            style={{
              padding: '0.9rem 1rem',
              backgroundColor: '#ecfdf5',
              border: '1px solid #a7f3d0',
              borderRadius: 'var(--radius-md)',
              color: '#065f46',
              fontSize: '0.875rem',
              display: 'flex',
              alignItems: 'flex-start',
              gap: '0.65rem',
              lineHeight: 1.5,
            }}
          >
            <CheckCheck size={18} color="#059669" style={{ flexShrink: 0, marginTop: '2px' }} />
            <div>
              <strong>Finalize Referral Case:</strong> Mark that all clinical interventions and medical evaluations have successfully concluded.
            </div>
          </div>
        )}

        {/* Facility Clinical Response / Note Input */}
        <div>
          <label
            htmlFor="facility-response-note"
            style={{
              display: 'block',
              fontSize: '0.92rem',
              fontWeight: 700,
              color: 'var(--text-main)',
              marginBottom: '0.4rem',
            }}
          >
            Facility Response & Clinical Instructions{' '}
            {targetAction === 'REJECTED' ? (
              <span style={{ color: 'var(--danger)', fontSize: '0.8rem' }}>(Mandatory • आवश्यक)</span>
            ) : (
              <span style={{ color: 'var(--text-subtle)', fontWeight: 500, fontSize: '0.8rem' }}>(Optional • ऐच्छिक)</span>
            )}
          </label>
          <textarea
            id="facility-response-note"
            required={targetAction === 'REJECTED'}
            className="form-textarea"
            rows={3}
            style={{ width: '100%', resize: 'vertical', minHeight: '85px' }}
            value={responseNote}
            onChange={(e) => setResponseNote(e.target.value)}
            placeholder={
              targetAction === 'ACCEPTED'
                ? 'e.g., Bed allocated in Ward 2. Patient should bring existing medication strip and Aadhar card upon arrival.'
                : targetAction === 'REJECTED'
                ? 'e.g., ICU / Oxygen beds currently at 100% capacity; redirect patient to Shirwal Community Health Centre.'
                : targetAction === 'IN_PROGRESS'
                ? 'e.g., Patient admitted to observation bay; vitals stabilized and IV fluids started.'
                : 'e.g., Clinical evaluation complete, medication regimen prescribed and discharge summary provided.'
            }
          />
          <p style={{ fontSize: '0.78rem', color: 'var(--text-subtle)', marginTop: '0.35rem' }}>
            {targetAction === 'ACCEPTED'
              ? 'This response will be visible to the referring doctor and the patient.'
              : targetAction === 'REJECTED'
              ? 'Provide a clear clinical or capacity reason so alternative care can be scheduled.'
              : 'Add clinical notes or bed/department updates.'}
          </p>
        </div>

        {/* Modal Action Buttons with proper spacing */}
        <div
          style={{
            display: 'flex',
            justifyContent: 'flex-end',
            alignItems: 'center',
            gap: '0.85rem',
            paddingTop: '1rem',
            borderTop: '1px solid var(--border-light)',
            marginTop: '0.5rem',
          }}
        >
          <Button
            type="button"
            variant="neutral"
            onClick={onClose}
            disabled={isLoading}
          >
            Cancel (रद्द करा)
          </Button>
          <Button
            type="submit"
            variant={getButtonVariant()}
            isLoading={isLoading}
            disabled={isLoading || (targetAction === 'REJECTED' && !responseNote.trim())}
          >
            {targetAction === 'ACCEPTED' && <CheckCircle2 size={16} />}
            {targetAction === 'REJECTED' && <XCircle size={16} />}
            {targetAction === 'IN_PROGRESS' && <Play size={16} />}
            {targetAction === 'COMPLETED' && <CheckCheck size={16} />}
            <span>Confirm {targetAction}</span>
          </Button>
        </div>
      </form>
    </Modal>
  );
};
