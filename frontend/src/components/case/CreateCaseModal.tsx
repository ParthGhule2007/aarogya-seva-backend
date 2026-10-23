import React, { useState } from 'react';
import { Modal } from '../common/Modal';
import { Button } from '../common/Button';
import { CreateCaseRequest, HealthcareCaseDto, IntakeChannel } from '../../types/case';
import { PatientDto } from '../../types/patient';
import { caseApi } from '../../api/caseApi';

interface CreateCaseModalProps {
  isOpen: boolean;
  onClose: () => void;
  patient: PatientDto;
  onSuccess: (newCase: HealthcareCaseDto) => void;
}

export const CreateCaseModal: React.FC<CreateCaseModalProps> = ({
  isOpen,
  onClose,
  patient,
  onSuccess,
}) => {
  const [formData, setFormData] = useState({
    intakeChannel: 'FORM' as IntakeChannel,
    latitude: patient.latitude || 18.5204,
    longitude: patient.longitude || 73.8567,
    chiefComplaint: '',
    notes: '',
  });

  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setIsLoading(true);
    setError(null);

    const payload: CreateCaseRequest = {
      patientId: patient.id,
      intakeChannel: formData.intakeChannel,
      latitude: Number(formData.latitude),
      longitude: Number(formData.longitude),
      chiefComplaint: formData.chiefComplaint.trim() || undefined,
      notes: formData.notes.trim() || undefined,
    };

    try {
      const created = await caseApi.createCase(payload);
      onSuccess(created);
      onClose();
    } catch (err: any) {
      setError(err.message || 'Failed to open healthcare case');
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <Modal
      isOpen={isOpen}
      onClose={onClose}
      title="Open Healthcare Case (केस नोंदणी)"
      subtitle={`Creating consultation intake for ${patient.fullName} (${patient.villageTown}, ${patient.district})`}
      maxWidth="600px"
    >
      {error && (
        <div className="alert alert-danger" style={{ marginBottom: '1rem' }}>
          {error}
        </div>
      )}

      <form onSubmit={handleSubmit}>
        <div className="form-group">
          <label className="form-label">
            Intake Modality <span className="required">*</span>
          </label>
          <select
            className="form-select"
            value={formData.intakeChannel}
            onChange={(e) => setFormData({ ...formData, intakeChannel: e.target.value as IntakeChannel })}
          >
            <option value="FORM">Web Form / Tablet (वेब फॉर्म)</option>
            <option value="CHAT">Chatbot Assistant (संभाषण)</option>
            <option value="VOICE">Voice Triage (आवाज हेल्पलाइन)</option>
            <option value="COMMUNITY_HEALTH_WORKER">ASHA / Health Worker (आशा कार्यकर्ती)</option>
          </select>
        </div>

        <div className="form-group">
          <label className="form-label">Chief Complaint / Primary Concern</label>
          <input
            type="text"
            className="form-input"
            value={formData.chiefComplaint}
            onChange={(e) => setFormData({ ...formData, chiefComplaint: e.target.value })}
            placeholder="e.g. High fever with severe breathlessness since 3 days"
          />
        </div>

        <div className="grid-2">
          <div className="form-group">
            <label className="form-label">
              Consultation Latitude <span className="required">*</span>
            </label>
            <input
              type="number"
              step="any"
              required
              className="form-input"
              value={formData.latitude}
              onChange={(e) => setFormData({ ...formData, latitude: parseFloat(e.target.value) || 0 })}
            />
            <span className="form-hint">Used by PostGIS to locate closest health facilities</span>
          </div>

          <div className="form-group">
            <label className="form-label">
              Consultation Longitude <span className="required">*</span>
            </label>
            <input
              type="number"
              step="any"
              required
              className="form-input"
              value={formData.longitude}
              onChange={(e) => setFormData({ ...formData, longitude: parseFloat(e.target.value) || 0 })}
            />
          </div>
        </div>

        <div className="form-group">
          <label className="form-label">Clinical / Intake Notes</label>
          <textarea
            className="form-textarea"
            rows={3}
            value={formData.notes}
            onChange={(e) => setFormData({ ...formData, notes: e.target.value })}
            placeholder="e.g. Patient walked in with complaints of persistent fever and severe breathlessness..."
          />
        </div>

        <div className="modal-footer" style={{ margin: '1rem -1.5rem -1.5rem', padding: '1rem 1.5rem' }}>
          <Button type="button" variant="secondary" onClick={onClose}>
            Cancel
          </Button>
          <Button type="submit" variant="primary" isLoading={isLoading}>
            Create Case
          </Button>
        </div>
      </form>
    </Modal>
  );
};
