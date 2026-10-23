import React, { useState } from 'react';
import { Modal } from '../common/Modal';
import { Button } from '../common/Button';
import { ReferralResponseDto, ReferralStatus } from '../../types/referral';
import { referralApi } from '../../api/referralApi';

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
        return 'Accept Patient Referral (रुग्ण स्वीकृती)';
      case 'REJECTED':
        return 'Reject Patient Referral (नकार नोंदवा)';
      case 'IN_PROGRESS':
        return 'Start Clinical Care (उपचार सुरू करा)';
      case 'COMPLETED':
        return 'Complete Referral Case (उपचार पूर्ण झाला)';
      default:
        return 'Update Referral Status';
    }
  };

  const getButtonVariant = () => {
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
      setError(err.message || 'Failed to update referral status');
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <Modal
      isOpen={isOpen}
      onClose={onClose}
      title={getActionTitle()}
      subtitle={`Referral for patient at ${referral.facilityName}`}
      maxWidth="540px"
    >
      {error && (
        <div className="alert alert-danger" style={{ marginBottom: '1rem' }}>
          {error}
        </div>
      )}

      <form onSubmit={handleSubmit}>
        <div style={{ marginBottom: '1.25rem' }}>
          <p style={{ fontSize: '0.9rem', color: 'var(--text-main)', marginBottom: '0.5rem' }}>
            Are you sure you want to transition this referral to{' '}
            <strong style={{ color: 'var(--primary-dark)' }}>{targetAction}</strong>?
          </p>
          {targetAction === 'REJECTED' && (
            <p style={{ fontSize: '0.85rem', color: 'var(--danger)', marginTop: '0.25rem' }}>
              Note: Rejecting this referral will liberate the healthcare case so the patient can seek care from another recommended facility.
            </p>
          )}
        </div>

        <div className="form-group">
          <label className="form-label">
            Facility Response / Clinical Note {targetAction === 'REJECTED' && <span className="required">*</span>}
          </label>
          <textarea
            required={targetAction === 'REJECTED'}
            className="form-textarea"
            rows={3}
            value={responseNote}
            onChange={(e) => setResponseNote(e.target.value)}
            placeholder={
              targetAction === 'ACCEPTED'
                ? 'e.g. Bed confirmed in Ward 2. Patient can proceed immediately for evaluation.'
                : targetAction === 'REJECTED'
                ? 'e.g. Oxygen beds full currently; recommend referring to Community Health Centre.'
                : targetAction === 'IN_PROGRESS'
                ? 'e.g. Patient arrived at triage, oxygen therapy and IV line initiated.'
                : 'e.g. Treatment completed, patient stabilized and discharged with prescription.'
            }
          />
        </div>

        <div className="modal-footer" style={{ margin: '1rem -1.5rem -1.5rem', padding: '1rem 1.5rem' }}>
          <Button type="button" variant="secondary" onClick={onClose}>
            Cancel
          </Button>
          <Button type="submit" variant={getButtonVariant()} isLoading={isLoading}>
            Confirm {targetAction}
          </Button>
        </div>
      </form>
    </Modal>
  );
};
