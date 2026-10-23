import React from 'react';
import { Check } from 'lucide-react';

interface Step {
  stepNumber: number;
  label: string;
  hindiLabel: string;
}

const STEPS: Step[] = [
  { stepNumber: 1, label: 'Start', hindiLabel: 'सुरुवात' },
  { stepNumber: 2, label: 'Symptoms', hindiLabel: 'लक्षणे' },
  { stepNumber: 3, label: 'Screening', hindiLabel: 'तपासणी' },
  { stepNumber: 4, label: 'Healthcare', hindiLabel: 'दवाखाना' },
  { stepNumber: 5, label: 'Request', hindiLabel: 'विनंती' },
];

interface StepIndicatorProps {
  currentStep: number;
}

export const StepIndicator: React.FC<StepIndicatorProps> = ({ currentStep }) => {
  return (
    <div className="step-indicator-bar" aria-label="Screening Progress">
      <ol className="step-indicator-list">
        {STEPS.map((s) => {
          const isDone = s.stepNumber < currentStep;
          const isActive = s.stepNumber === currentStep;

          return (
            <li
              key={s.stepNumber}
              className={`step-item ${isActive ? 'active' : ''} ${isDone ? 'completed' : ''}`}
            >
              <div className="step-circle" aria-current={isActive ? 'step' : undefined}>
                {isDone ? <Check size={18} strokeWidth={3} /> : s.stepNumber}
              </div>
              <div>
                <span>{s.label}</span>
                <span style={{ display: 'block', fontSize: '0.75rem', fontWeight: 500, color: 'var(--text-subtle)' }}>
                  {s.hindiLabel}
                </span>
              </div>
            </li>
          );
        })}
      </ol>
    </div>
  );
};
