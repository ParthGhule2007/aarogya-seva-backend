import React, { useState } from 'react';
import { StepIndicator } from '../common/StepIndicator';
import { Step1WhoAndLocation } from './Step1WhoAndLocation';
import { Step2SymptomInput } from './Step2SymptomInput';
import { Step3ScreeningResult } from './Step3ScreeningResult';
import { Step4FacilityPicker } from './Step4FacilityPicker';
import { Step5CareRequestSent } from './Step5CareRequestSent';
import { LoadingOverlay } from '../common/LoadingOverlay';
import { PatientDto } from '../../types/patient';
import { HealthcareCaseDto, SymptomInputDto } from '../../types/case';
import { AiAnalysisResponse } from '../../types/ai';
import { FacilityRecommendationDto } from '../../types/facility';
import { aiApi } from '../../api/aiApi';
import { facilityApi } from '../../api/facilityApi';

interface ScreeningWizardProps {
  onComplete: () => void;
  reReferCaseId?: string | null;
}

export const ScreeningWizard: React.FC<ScreeningWizardProps> = ({
  onComplete,
  reReferCaseId,
}) => {
  const [currentStep, setCurrentStep] = useState<number>(1);

  // Screening Session Context
  const [activePatient, setActivePatient] = useState<PatientDto | null>(null);
  const [activeCase, setActiveCase] = useState<HealthcareCaseDto | null>(null);
  const [activeCoords, setActiveCoords] = useState<{ lat: number; lng: number }>({ lat: 18.0534, lng: 74.0211 });
  const [_recordedSymptoms, setRecordedSymptoms] = useState<SymptomInputDto[]>([]);
  const [aiAnalysis, setAiAnalysis] = useState<AiAnalysisResponse | null>(null);
  const [recommendedFacilities, setRecommendedFacilities] = useState<FacilityRecommendationDto[]>([]);
  const [selectedFacility, setSelectedFacility] = useState<FacilityRecommendationDto | null>(null);

  // AI Screening Processing State
  const [isAiProcessing, setIsAiProcessing] = useState(false);
  const [aiError, setAiError] = useState<string | null>(null);

  // If re-referring an existing case that was rejected
  React.useEffect(() => {
    if (reReferCaseId) {
      loadExistingCaseForReReferral(reReferCaseId);
    }
  }, [reReferCaseId]);

  const populateAiAnalysis = (res: AiAnalysisResponse): AiAnalysisResponse => ({
    ...res,
    riskLevel: res.riskLevel || res.riskAssessment?.riskLevel || 'MODERATE',
    riskScore: res.riskScore !== undefined ? res.riskScore : res.riskAssessment?.riskScore,
    urgencyLevel: res.urgencyLevel || res.urgency?.level || 'PRIORITY',
    urgencyReason: res.urgencyReason || res.urgency?.reason || res.explanation || 'Consult a healthcare professional for clinical assessment.',
    triageRationale: res.triageRationale || res.urgency?.reason || res.explanation,
    probableDiseases: res.probableDiseases || res.possibleDiseases || [],
  });

  const resolveFacilities = async (
    recommended: FacilityRecommendationDto[] | undefined,
    coords: { lat: number; lng: number }
  ): Promise<FacilityRecommendationDto[]> => {
    if (recommended && recommended.length > 0) {
      return recommended;
    }
    try {
      // Query backend nearby facility discovery with accurate PostGIS distance calculation
      const nearby = await facilityApi.getNearbyFacilities(coords.lat, coords.lng, 100);
      if (nearby && nearby.length > 0) {
        return nearby;
      }
    } catch (err) {
      console.warn('Nearby facility lookup failed:', err);
    }

    try {
      const all = await facilityApi.getAllFacilities();
      return (all || []).map((f) => ({
        facilityId: f.id,
        name: f.name,
        facilityType: f.facilityType,
        facilityTypeDisplayName: f.facilityTypeDisplayName,
        villageTown: f.villageTown,
        district: f.district,
        state: f.state,
        pincode: f.pincode,
        contactNumber: f.contactNumber,
        emergencyNumber: f.emergencyNumber,
        isOperational: f.isOperational,
        isDemoData: f.isDemoData,
        availableServices: f.availableServices,
      }));
    } catch (facErr) {
      console.warn('Failed to load facilities:', facErr);
      return [];
    }
  };

  const loadExistingCaseForReReferral = async (caseId: string) => {
    setIsAiProcessing(true);
    try {
      // Fetch latest AI analysis for the case
      const res = await aiApi.getLatestAnalysis(caseId);
      setAiAnalysis(populateAiAnalysis(res));

      const facilities = await resolveFacilities(res.recommendedFacilities, activeCoords);
      setRecommendedFacilities(facilities);
      setActiveCase({ id: caseId } as any);
      setCurrentStep(4); // Jump directly to facility selection
    } catch (err: any) {
      setAiError(err.message || 'Could not load existing case details.');
    } finally {
      setIsAiProcessing(false);
    }
  };

  // Step 1: Patient and Background Case created
  const handleCaseCreated = (
    patient: PatientDto,
    healthcareCase: HealthcareCaseDto,
    coords: { lat: number; lng: number }
  ) => {
    setActivePatient(patient);
    setActiveCase(healthcareCase);
    setActiveCoords(coords);
    setCurrentStep(2);
  };

  // Step 2: Symptoms Submitted -> Trigger AI Screening
  const handleSymptomsSubmitted = async (symptoms: SymptomInputDto[]) => {
    setRecordedSymptoms(symptoms);
    if (!activeCase) return;

    setIsAiProcessing(true);
    setAiError(null);
    try {
      // POST /api/v1/cases/{caseId}/ai-analysis
      const res = await aiApi.runAiAnalysis(activeCase.id);
      const populated = populateAiAnalysis(res);
      setAiAnalysis(populated);

      const facilities = await resolveFacilities(res.recommendedFacilities, activeCoords);
      setRecommendedFacilities(facilities);

      // Always advance to Step 3 (Screening Result)
      setCurrentStep(3);
    } catch (err: any) {
      setAiError(err.message || 'Clinical screening failed. Please retry.');
    } finally {
      setIsAiProcessing(false);
    }
  };

  // Step 3: Screening reviewed -> Move to facility picker
  const handleContinueToFacilities = () => {
    setCurrentStep(4);
  };

  // Step 4: Facility selected -> Move to confirmation
  const handleSelectFacility = (facility: FacilityRecommendationDto) => {
    setSelectedFacility(facility);
    setCurrentStep(5);
  };

  return (
    <div>
      {/* 5-Step Progressive Journey Indicator */}
      <StepIndicator currentStep={currentStep} />

      {/* Dedicated Screening Processing Overlay */}
      {isAiProcessing && (
        <LoadingOverlay
          error={aiError}
          onRetry={() => {
            if (activeCase) handleSymptomsSubmitted(_recordedSymptoms);
          }}
        />
      )}

      {/* Step 1: Start (Who & Location) */}
      {!isAiProcessing && currentStep === 1 && (
        <Step1WhoAndLocation
          onCaseCreated={handleCaseCreated}
          initialPatient={activePatient}
          initialCoords={activeCoords}
        />
      )}

      {/* Step 2: Tell Symptoms */}
      {!isAiProcessing && currentStep === 2 && activeCase && (
        <Step2SymptomInput
          caseId={activeCase.id}
          onSymptomsSubmitted={handleSymptomsSubmitted}
          onBack={() => setCurrentStep(1)}
        />
      )}

      {/* Step 3: Screening Result */}
      {!isAiProcessing && currentStep === 3 && aiAnalysis && (
        <Step3ScreeningResult
          analysis={aiAnalysis}
          onContinueToFacilities={handleContinueToFacilities}
          onBack={() => setCurrentStep(2)}
        />
      )}

      {/* Step 4: Nearby Healthcare Facility Picker */}
      {!isAiProcessing && currentStep === 4 && (
        <Step4FacilityPicker
          facilities={recommendedFacilities}
          onSelectFacility={handleSelectFacility}
          onBack={() => setCurrentStep(3)}
        />
      )}

      {/* Step 5: Send Care Request Confirmation */}
      {!isAiProcessing && currentStep === 5 && activeCase && selectedFacility && aiAnalysis && (
        <Step5CareRequestSent
          caseId={activeCase.id}
          facility={selectedFacility}
          analysis={aiAnalysis}
          onTrackRequests={onComplete}
          onBack={() => setCurrentStep(4)}
        />
      )}
    </div>
  );
};
