import React, { useState } from 'react';
import { Modal } from '../common/Modal';
import { Button } from '../common/Button';
import { CreateSymptomRequest, SymptomDto, SymptomSeverity } from '../../types/symptom';
import { symptomApi } from '../../api/symptomApi';

interface AddSymptomModalProps {
  isOpen: boolean;
  onClose: () => void;
  caseId: string;
  onSuccess: (newSymptom: SymptomDto) => void;
}

const COMMON_SYMPTOMS = [
  { name: 'High Grade Fever (तीव्र ताप)', site: 'WHOLE_BODY', defaultSeverity: 'SEVERE' as SymptomSeverity },
  { name: 'Dry Persistent Cough (खोकला)', site: 'CHEST', defaultSeverity: 'MODERATE' as SymptomSeverity },
  { name: 'Severe Dyspnea / Breathlessness (दम लागणे)', site: 'CHEST', defaultSeverity: 'CRITICAL' as SymptomSeverity },
  { name: 'Acute Chest Pain (छातीत दुखणे)', site: 'CHEST', defaultSeverity: 'CRITICAL' as SymptomSeverity },
  { name: 'Watery Diarrhea (अतिसार / जुलाब)', site: 'ABDOMEN', defaultSeverity: 'SEVERE' as SymptomSeverity },
  { name: 'Throbbing Headache (डोकेदुखी)', site: 'HEAD', defaultSeverity: 'MODERATE' as SymptomSeverity },
  { name: 'Persistent Vomiting (उलटी)', site: 'ABDOMEN', defaultSeverity: 'SEVERE' as SymptomSeverity },
  { name: 'Extreme Fatigue / Weakness (अशक्तपणा)', site: 'WHOLE_BODY', defaultSeverity: 'MODERATE' as SymptomSeverity },
];

export const AddSymptomModal: React.FC<AddSymptomModalProps> = ({
  isOpen,
  onClose,
  caseId,
  onSuccess,
}) => {
  const [formData, setFormData] = useState<CreateSymptomRequest>({
    symptomName: '',
    severity: 'SEVERE',
    durationDays: 3,
    description: '',
    bodySite: 'CHEST',
  });

  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const handleQuickSelect = (item: (typeof COMMON_SYMPTOMS)[0]) => {
    setFormData({
      ...formData,
      symptomName: item.name.split(' (')[0],
      bodySite: item.site,
      severity: item.defaultSeverity,
    });
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!formData.symptomName.trim()) {
      setError('Please provide a symptom name');
      return;
    }

    setIsLoading(true);
    setError(null);

    try {
      const created = await symptomApi.addSymptom(caseId, formData);
      onSuccess(created);
      onClose();
    } catch (err: any) {
      setError(err.message || 'Failed to record symptom');
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <Modal
      isOpen={isOpen}
      onClose={onClose}
      title="Record Patient Symptom (लक्षण नोंदवा)"
      subtitle="Select a common rural health complaint or specify custom clinical symptoms"
      maxWidth="620px"
    >
      {error && (
        <div className="alert alert-danger" style={{ marginBottom: '1rem' }}>
          {error}
        </div>
      )}

      <div style={{ marginBottom: '1.25rem' }}>
        <div style={{ fontSize: '0.8rem', fontWeight: 700, color: 'var(--text-subtle)', marginBottom: '0.5rem', textTransform: 'uppercase' }}>
          Quick Select Common Rural Symptoms
        </div>
        <div style={{ display: 'flex', flexWrap: 'wrap', gap: '0.4rem' }}>
          {COMMON_SYMPTOMS.map((item, idx) => (
            <button
              key={idx}
              type="button"
              onClick={() => handleQuickSelect(item)}
              style={{
                padding: '0.35rem 0.65rem',
                fontSize: '0.8rem',
                borderRadius: 'var(--radius-sm)',
                border: '1px solid var(--border-light)',
                backgroundColor: 'var(--bg-surface-subtle)',
                color: 'var(--text-main)',
                cursor: 'pointer',
                transition: 'all 0.15s ease',
              }}
              onMouseEnter={(e) => (e.currentTarget.style.borderColor = 'var(--primary)')}
              onMouseLeave={(e) => (e.currentTarget.style.borderColor = 'var(--border-light)')}
            >
              + {item.name}
            </button>
          ))}
        </div>
      </div>

      <form onSubmit={handleSubmit}>
        <div className="form-group">
          <label className="form-label">
            Symptom Name <span className="required">*</span>
          </label>
          <input
            type="text"
            required
            className="form-input"
            value={formData.symptomName}
            onChange={(e) => setFormData({ ...formData, symptomName: e.target.value })}
            placeholder="e.g. High Grade Fever with Chills"
          />
        </div>

        <div className="grid-3">
          <div className="form-group">
            <label className="form-label">
              Severity <span className="required">*</span>
            </label>
            <select
              className="form-select"
              value={formData.severity}
              onChange={(e) => setFormData({ ...formData, severity: e.target.value as SymptomSeverity })}
            >
              <option value="MILD">Mild (कमी)</option>
              <option value="MODERATE">Moderate (मध्यम)</option>
              <option value="SEVERE">Severe (तीव्र)</option>
              <option value="CRITICAL">Critical (अति तीव्र)</option>
            </select>
          </div>

          <div className="form-group">
            <label className="form-label">
              Duration (Days) <span className="required">*</span>
            </label>
            <input
              type="number"
              min={1}
              max={365}
              required
              className="form-input"
              value={formData.durationDays}
              onChange={(e) => setFormData({ ...formData, durationDays: parseInt(e.target.value) || 1 })}
            />
          </div>

          <div className="form-group">
            <label className="form-label">Body Site</label>
            <input
              type="text"
              className="form-input"
              value={formData.bodySite || ''}
              onChange={(e) => setFormData({ ...formData, bodySite: e.target.value })}
              placeholder="e.g. CHEST, THROAT"
            />
          </div>
        </div>

        <div className="form-group">
          <label className="form-label">Detailed Clinical Description</label>
          <textarea
            className="form-textarea"
            rows={2}
            value={formData.description || ''}
            onChange={(e) => setFormData({ ...formData, description: e.target.value })}
            placeholder="Continuous fever with evening spike and associated breathlessness..."
          />
        </div>

        <div className="modal-footer" style={{ margin: '1rem -1.5rem -1.5rem', padding: '1rem 1.5rem' }}>
          <Button type="button" variant="secondary" onClick={onClose}>
            Cancel
          </Button>
          <Button type="submit" variant="primary" isLoading={isLoading}>
            Add Symptom
          </Button>
        </div>
      </form>
    </Modal>
  );
};
