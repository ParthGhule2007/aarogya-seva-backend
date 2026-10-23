import React, { useState } from 'react';
import { PenTool, Bot, Plus, Trash2, ArrowRight, ArrowLeft } from 'lucide-react';
import { symptomApi } from '../../api/symptomApi';
import { SymptomInputDto } from '../../types/case';
import { AlertBox } from '../common/AlertBox';
import { ChatbotModal } from './ChatbotModal';

interface SymptomRow {
  symptomName: string;
  severity: 'MILD' | 'MODERATE' | 'SEVERE' | 'CRITICAL';
  durationDays: number;
  description?: string;
}

const COMMON_SYMPTOM_PRESETS = [
  { name: 'High Grade Fever', label: 'Fever (ताप)', severity: 'SEVERE' as const, duration: 3 },
  { name: 'Persistent Cough', label: 'Cough (खोकला)', severity: 'MODERATE' as const, duration: 4 },
  { name: 'Breathing Difficulty', label: 'Breathing Issue (श्वास त्रास)', severity: 'SEVERE' as const, duration: 2 },
  { name: 'Chest Pain', label: 'Chest Pain (छातीत कळ)', severity: 'SEVERE' as const, duration: 1 },
  { name: 'Watery Diarrhea', label: 'Diarrhea / Loose Motion (जुलाब)', severity: 'MODERATE' as const, duration: 2 },
  { name: 'Severe Headache', label: 'Headache (डोकेदुखी)', severity: 'MODERATE' as const, duration: 2 },
  { name: 'Stomach Pain', label: 'Stomach Pain (पोटदुखी)', severity: 'MODERATE' as const, duration: 2 },
  { name: 'Extreme Weakness', label: 'Weakness / Fatigue (अशक्तपणा)', severity: 'MILD' as const, duration: 5 },
];

interface Step2SymptomInputProps {
  caseId: string;
  onSymptomsSubmitted: (symptoms: SymptomInputDto[]) => void;
  onBack: () => void;
}

export const Step2SymptomInput: React.FC<Step2SymptomInputProps> = ({
  caseId,
  onSymptomsSubmitted,
  onBack,
}) => {
  const [method, setMethod] = useState<'form' | 'chatbot'>('form');
  const [isChatbotOpen, setIsChatbotOpen] = useState(false);

  const [symptoms, setSymptoms] = useState<SymptomRow[]>([
    { symptomName: '', severity: 'MODERATE', durationDays: 3, description: '' },
  ]);

  const [isSubmitting, setIsSubmitting] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  // Quick preset chip click
  const handleAddPreset = (preset: typeof COMMON_SYMPTOM_PRESETS[0]) => {
    // If the first symptom row is blank, replace it; otherwise append
    if (symptoms.length === 1 && !symptoms[0].symptomName.trim()) {
      setSymptoms([
        {
          symptomName: preset.name,
          severity: preset.severity,
          durationDays: preset.duration,
          description: `Patient reported ${preset.name}`,
        },
      ]);
    } else {
      // Check if already added
      if (!symptoms.some((s) => s.symptomName.toLowerCase() === preset.name.toLowerCase())) {
        setSymptoms((prev) => [
          ...prev,
          {
            symptomName: preset.name,
            severity: preset.severity,
            durationDays: preset.duration,
            description: `Patient reported ${preset.name}`,
          },
        ]);
      }
    }
  };

  const handleUpdateRow = (index: number, field: keyof SymptomRow, value: any) => {
    setSymptoms((prev) => {
      const updated = [...prev];
      updated[index] = { ...updated[index], [field]: value };
      return updated;
    });
  };

  const handleAddRow = () => {
    setSymptoms((prev) => [
      ...prev,
      { symptomName: '', severity: 'MODERATE', durationDays: 2, description: '' },
    ]);
  };

  const handleRemoveRow = (index: number) => {
    if (symptoms.length <= 1) return;
    setSymptoms((prev) => prev.filter((_, i) => i !== index));
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setErrorMessage(null);

    const validSymptoms = symptoms.filter((s) => s.symptomName.trim().length > 0);
    if (validSymptoms.length === 0) {
      setErrorMessage('Please enter at least one symptom (कृपया किमान एक लक्षण प्रविष्ट करा)');
      return;
    }

    setIsSubmitting(true);
    try {
      const dtos: SymptomInputDto[] = validSymptoms.map((s) => ({
        symptomName: s.symptomName.trim(),
        severity: s.severity,
        durationDays: Number(s.durationDays) || 1,
        description: s.description?.trim() || undefined,
      }));

      // Submit batch to backend API: POST /api/v1/cases/{caseId}/symptoms/batch
      await symptomApi.addSymptomsBatch(caseId, dtos);

      onSymptomsSubmitted(dtos);
    } catch (err: any) {
      setErrorMessage(err.message || 'Failed to record symptoms in backend.');
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="card-simple" style={{ maxWidth: '680px', margin: '0 auto' }}>
      <h2 style={{ fontSize: '1.6rem', fontWeight: 800, color: 'var(--primary)', marginBottom: '0.4rem' }}>
        Step 2: Tell Us What is Wrong
      </h2>
      <p style={{ color: 'var(--text-muted)', fontSize: '1.05rem', marginBottom: '1.5rem' }}>
        लक्षणे: आपल्याला काय त्रास होत आहे ते सांगा
      </p>

      {/* Method Selection */}
      <div style={{ marginBottom: '1.75rem' }}>
        <label className="form-label">How would you like to tell us? (कसे सांगू इच्छिता?)</label>
        <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '0.75rem' }}>
          <button
            type="button"
            className={`btn-large ${method === 'form' ? 'btn-primary' : 'btn-outline'}`}
            onClick={() => setMethod('form')}
          >
            <PenTool size={22} />
            <span>Fill a Simple Form • सोपा फॉर्म</span>
          </button>

          <button
            type="button"
            className="btn-large btn-outline"
            onClick={() => setIsChatbotOpen(true)}
          >
            <Bot size={22} />
            <span>Talk to Aarogya • बोलून सांगा</span>
          </button>
        </div>
      </div>

      {/* Common Quick Ailment Chips */}
      <div style={{ marginBottom: '1.5rem' }}>
        <label className="form-label">Quick Select Common Problems (वारंवार होणारे त्रास):</label>
        <div className="chip-grid">
          {COMMON_SYMPTOM_PRESETS.map((preset) => {
            const isSelected = symptoms.some(
              (s) => s.symptomName.toLowerCase() === preset.name.toLowerCase()
            );
            return (
              <button
                key={preset.name}
                type="button"
                className={`chip-option ${isSelected ? 'selected' : ''}`}
                onClick={() => handleAddPreset(preset)}
                aria-pressed={isSelected}
              >
                {preset.label}
              </button>
            );
          })}
        </div>
      </div>

      {errorMessage && (
        <AlertBox type="danger">
          <strong>{errorMessage}</strong>
        </AlertBox>
      )}

      {/* Multi-Symptom Form */}
      <form onSubmit={handleSubmit}>
        <div style={{ display: 'flex', flexDirection: 'column', gap: '1.25rem' }}>
          {symptoms.map((symptom, idx) => (
            <div
              key={idx}
              style={{
                backgroundColor: 'var(--bg-subtle)',
                border: '2px solid var(--border-medium)',
                borderRadius: 'var(--radius-md)',
                padding: '1.25rem',
              }}
            >
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '0.75rem' }}>
                <strong style={{ fontSize: '1.1rem', color: 'var(--primary)' }}>
                  Problem #{idx + 1}
                </strong>
                {symptoms.length > 1 && (
                  <button
                    type="button"
                    style={{
                      background: 'none',
                      border: 'none',
                      color: 'var(--risk-high)',
                      cursor: 'pointer',
                      display: 'flex',
                      alignItems: 'center',
                      gap: '0.25rem',
                      fontWeight: 600,
                    }}
                    onClick={() => handleRemoveRow(idx)}
                    aria-label={`Remove problem ${idx + 1}`}
                  >
                    <Trash2 size={18} />
                    <span>Remove</span>
                  </button>
                )}
              </div>

              {/* Symptom Name */}
              <div className="form-group">
                <label className="form-label">
                  What problem are you having? (काय त्रास होत आहे?)
                </label>
                <input
                  type="text"
                  className="form-input"
                  placeholder="e.g. High fever with chills, persistent dry cough"
                  value={symptom.symptomName}
                  onChange={(e) => handleUpdateRow(idx, 'symptomName', e.target.value)}
                  required
                />
              </div>

              {/* Severity Chips */}
              <div className="form-group">
                <label className="form-label">How severe is it? (त्रास किती तीव्र आहे?)</label>
                <div style={{ display: 'grid', gridTemplateColumns: 'repeat(4, 1fr)', gap: '0.5rem' }}>
                  {(['MILD', 'MODERATE', 'SEVERE', 'CRITICAL'] as const).map((sev) => (
                    <button
                      key={sev}
                      type="button"
                      className={`chip-option ${symptom.severity === sev ? 'selected' : ''}`}
                      onClick={() => handleUpdateRow(idx, 'severity', sev)}
                      aria-pressed={symptom.severity === sev}
                    >
                      {sev === 'MILD' && 'Mild (कमी)'}
                      {sev === 'MODERATE' && 'Moderate (मध्यम)'}
                      {sev === 'SEVERE' && 'Severe (तीव्र)'}
                      {sev === 'CRITICAL' && 'Critical (गंभीर)'}
                    </button>
                  ))}
                </div>
              </div>

              {/* Duration */}
              <div className="form-group" style={{ marginBottom: 0 }}>
                <label className="form-label">How many days? (किती दिवसांपासून?)</label>
                <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
                  <input
                    type="number"
                    min="1"
                    max="365"
                    className="form-input"
                    style={{ maxWidth: '120px' }}
                    value={symptom.durationDays}
                    onChange={(e) => handleUpdateRow(idx, 'durationDays', Number(e.target.value))}
                    required
                  />
                  <span style={{ fontSize: '1.05rem', fontWeight: 600 }}>Days (दिवस)</span>
                </div>
              </div>
            </div>
          ))}
        </div>

        {/* Add Another Symptom */}
        <div style={{ marginTop: '1rem', textAlign: 'left' }}>
          <button
            type="button"
            className="btn-large btn-outline"
            style={{ width: 'auto' }}
            onClick={handleAddRow}
          >
            <Plus size={20} />
            <span>+ Add Another Symptom • आणखी त्रास जोडा</span>
          </button>
        </div>

        {/* Navigation Controls */}
        <div className="btn-row" style={{ marginTop: '2rem', display: 'flex', gap: '1rem' }}>
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
            type="submit"
            className="btn-large btn-primary"
            style={{ flex: 2 }}
            disabled={isSubmitting}
          >
            {isSubmitting ? (
              <span>Submitting Symptoms...</span>
            ) : (
              <>
                <span>START CLINICAL SCREENING • तपासणी करा</span>
                <ArrowRight size={22} />
              </>
            )}
          </button>
        </div>
      </form>

      {/* Chatbot Modal */}
      <ChatbotModal
        isOpen={isChatbotOpen}
        onClose={() => setIsChatbotOpen(false)}
        onUseForm={() => {
          setIsChatbotOpen(false);
          setMethod('form');
        }}
      />
    </div>
  );
};
