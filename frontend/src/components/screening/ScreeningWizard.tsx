import React, { useState } from 'react';
import { useAuth } from '../../context/AuthContext';
import { useLanguage } from '../../context/LanguageContext';
import { caseApi } from '../../api/caseApi';
import { symptomApi } from '../../api/symptomApi';
import { Step1Patient } from './Step1Patient';
import { Step2Symptoms, SymptomsFormData } from './Step2Symptoms';
import { Step3Triage } from './Step3Triage';
import { Step4Facilities } from './Step4Facilities';
import { Step5Referral } from './Step5Referral';
import { HealthcareFacilityDto } from '../../types/facility';
import { AiAnalysisResponse } from '../../types/ai';
import { CreateSymptomRequest, SymptomSeverity } from '../../types/symptom';
import { IntakeChannel } from '../../types/case';
import { User, Activity, Brain, Building2, QrCode } from 'lucide-react';

interface ScreeningWizardProps {
  onComplete?: () => void;
}

export const ScreeningWizard: React.FC<ScreeningWizardProps> = ({ onComplete }) => {
  const { role, selectedPatientId, selectedPatientName, user } = useAuth();
  const { t } = useLanguage();

  const [currentStep, setCurrentStep] = useState<number>(1);
  const [createdCaseId, setCreatedCaseId] = useState<string | null>(null);

  const [formData, setFormData] = useState<SymptomsFormData>({
    chiefComplaint: '',
    selectedSymptoms: [],
    severity: 'MODERATE',
    durationDays: 2,
    notes: '',
    vitals: {},
  });

  const [isSubmitting, setIsSubmitting] = useState<boolean>(false);
  const [submitError, setSubmitError] = useState<string | null>(null);

  const [aiAnalysis, setAiAnalysis] = useState<AiAnalysisResponse | null>(null);
  const [selectedFacility, setSelectedFacility] = useState<HealthcareFacilityDto | null>(null);

  // Transition from Step 2 to Step 3: Create Case & Attach Symptoms in Core API
  const handleStep2Next = async () => {
    const patientId = selectedPatientId || user?.patientId;
    if (!patientId) {
      setSubmitError('No patient profile selected or associated with this session. Please return to Step 1 and complete patient registration.');
      return;
    }
    setIsSubmitting(true);
    setSubmitError(null);

    try {
      // 1. Gather and deduplicate all selected symptoms + chief complaint
      const cleanSym = (s: string) => s.split(' (')[0].trim();
      const uniqueSymptoms: string[] = [];

      // Collect from selectedSymptoms chips first
      for (const s of formData.selectedSymptoms) {
        const cleaned = cleanSym(s);
        if (cleaned && !uniqueSymptoms.some((x) => x.toLowerCase() === cleaned.toLowerCase())) {
          uniqueSymptoms.push(cleaned);
        }
      }

      // Add chief complaint if not already included
      if (formData.chiefComplaint && formData.chiefComplaint.trim()) {
        const cleanedComplaint = cleanSym(formData.chiefComplaint);
        if (cleanedComplaint && !uniqueSymptoms.some((x) => x.toLowerCase() === cleanedComplaint.toLowerCase())) {
          uniqueSymptoms.push(cleanedComplaint);
        }
      }

      if (uniqueSymptoms.length === 0) {
        uniqueSymptoms.push('General Malaise');
      }

      const primaryComplaint = formData.chiefComplaint.trim() || uniqueSymptoms.join(', ');

      // 2. Open Healthcare Case in Core backend
      const channel: IntakeChannel = role === 'ASHA' ? 'COMMUNITY_HEALTH_WORKER' : 'FORM';
      const hasVitals = Boolean(
        formData.vitals && (
          formData.vitals.temperatureFahrenheit != null ||
          formData.vitals.systolicBp != null ||
          formData.vitals.diastolicBp != null ||
          formData.vitals.pulseBpm != null
        )
      );

      const createdCase = await caseApi.createCase({
        patientId,
        intakeChannel: channel,
        chiefComplaint: primaryComplaint,
        initialChiefComplaint: primaryComplaint,
        notes: formData.notes || undefined,
        latitude: 18.2562,
        longitude: 74.9048,
        vitals: hasVitals ? formData.vitals : undefined,
      });

      setCreatedCaseId(createdCase.id);

      // 3. Normalize severity to valid SymptomSeverity enum
      const severityEnum: SymptomSeverity =
        typeof formData.severity === 'string'
          ? (formData.severity as SymptomSeverity)
          : Number(formData.severity) <= 3
          ? 'MILD'
          : Number(formData.severity) <= 6
          ? 'MODERATE'
          : Number(formData.severity) <= 8
          ? 'SEVERE'
          : 'CRITICAL';

      // 4. Batch submit symptoms with strictly valid schema
      const symptomsPayload: CreateSymptomRequest[] = uniqueSymptoms.map((symName, idx) => ({
        symptomName: symName,
        severity: severityEnum,
        durationDays: formData.durationDays > 0 ? formData.durationDays : 1,
        description: idx === 0 && formData.notes ? formData.notes : undefined,
      }));

      await symptomApi.addSymptomsBatch(createdCase.id, symptomsPayload);

      // Advance to AI clinical screening
      setCurrentStep(3);
    } catch (err: any) {
      console.error('Failed to initialize clinical case or record symptoms:', err);
      const msg =
        err?.response?.data?.message ||
        err?.message ||
        'Failed to record symptoms and create clinical case. Please check connection and try again.';
      setSubmitError(msg);
    } finally {
      setIsSubmitting(false);
    }
  };

  const stepsMeta = [
    { num: 1, label: 'Patient', icon: <User size={18} /> },
    { num: 2, label: 'Symptoms', icon: <Activity size={18} /> },
    { num: 3, label: 'AI Triage', icon: <Brain size={18} /> },
    { num: 4, label: 'Facilities', icon: <Building2 size={18} /> },
    { num: 5, label: 'Referral QR', icon: <QrCode size={18} /> },
  ];

  return (
    <div style={{ maxWidth: '900px', margin: '0 auto' }}>
      {/* 5-Step Visual Progress Bar */}
      <div className="step-indicator">
        {stepsMeta.map((s) => {
          const isActive = currentStep === s.num;
          const isCompleted = currentStep > s.num;
          return (
            <div
              key={s.num}
              className={`step-item ${isActive ? 'active' : ''} ${isCompleted ? 'completed' : ''}`}
            >
              <div className="step-circle">
                {isCompleted ? '✓' : s.num}
              </div>
              <div className="step-label">{s.label}</div>
            </div>
          );
        })}
      </div>

      {/* Step Components */}
      {currentStep === 1 && (
        <Step1Patient onNext={() => setCurrentStep(2)} />
      )}

      {currentStep === 2 && (
        <Step2Symptoms
          formData={formData}
          setFormData={setFormData}
          onNext={handleStep2Next}
          onBack={() => setCurrentStep(1)}
          isSubmitting={isSubmitting}
          submitError={submitError}
        />
      )}

      {currentStep === 3 && createdCaseId && (
        <Step3Triage
          caseId={createdCaseId}
          aiAnalysis={aiAnalysis}
          setAiAnalysis={setAiAnalysis}
          onNext={() => setCurrentStep(4)}
          onBack={() => setCurrentStep(2)}
        />
      )}

      {currentStep === 4 && (
        <Step4Facilities
          selectedFacility={selectedFacility}
          setSelectedFacility={setSelectedFacility}
          aiAnalysis={aiAnalysis}
          onNext={() => setCurrentStep(5)}
          onBack={() => setCurrentStep(3)}
        />
      )}

      {currentStep === 5 && createdCaseId && selectedFacility && (
        <Step5Referral
          caseId={createdCaseId}
          patientId={selectedPatientId || user?.patientId || ''}
          patientName={selectedPatientName || user?.displayName || 'Patient'}
          facility={selectedFacility}
          aiAnalysis={aiAnalysis}
          onFinish={() => {
            if (onComplete) onComplete();
            else setCurrentStep(1);
          }}
        />
      )}
    </div>
  );
};
