import React, { useState } from 'react';
import { useMedicine } from '../../hooks/useMedicine';
import { useLanguage } from '../../context/LanguageContext';
import { TimeSlot, ScheduledDose, Medication, Caregiver, FoodInstruction, timeSlotsToReminderTimes, computeTimeSlot } from '../../types/medicine';
import {
  Pill,
  Sun,
  Sunset,
  Moon,
  Clock,
  Plus,
  CheckCircle,
  XCircle,
  AlertTriangle,
  Users,
  Trash2,
  Edit2,
  Upload,
  Calendar,
  ShieldCheck,
  ShieldAlert,
  Info,
  Check,
  X,
  FileText,
  Phone,
  Mail,
  MessageSquare,
  Ban,
  Eye,
  ExternalLink,
} from 'lucide-react';
import { ConfettiCelebration } from '../common/medical/ConfettiCelebration';

export const MedicineCabinet: React.FC = () => {
  const {
    patientId,
    todaySchedule,
    medications,
    caregivers,
    escalations,
    isLoading,
    error,
    markDose,
    addMedication,
    updateMedication,
    discontinueMedication,
    addCaregiver,
    updateCaregiver,
    deleteCaregiver,
  } = useMedicine();

  const { t } = useLanguage();

  // Tab State
  const [activeTab, setActiveTab] = useState<'today' | 'medications' | 'caregivers'>('today');
  const [showCelebration, setShowCelebration] = useState(false);

  const handleTakeDose = async (id: string) => {
    try {
      await markDose(id, 'taken');
      setShowCelebration(true);
    } catch (e: any) {
      console.warn('Failed to record dose:', e);
    }
  };

  // Food instruction display and normalization helpers
  const formatFoodInstruction = (fi?: string): string => {
    if (!fi || fi === 'NONE') return t('medFoodNone');
    const upper = fi.toUpperCase();
    if (upper === 'AFTER_FOOD' || upper === 'AFTER_MEAL') return t('medFoodAfter');
    if (upper === 'BEFORE_FOOD' || upper === 'BEFORE_MEAL') return t('medFoodBefore');
    if (upper === 'WITH_FOOD' || upper === 'WITH_MEAL') return t('medFoodWith');
    if (upper === 'EMPTY_STOMACH') return t('medFoodEmpty');
    return fi.replace('_', ' ');
  };

  const normalizeFoodInstruction = (fi?: string): FoodInstruction => {
    if (!fi) return 'NONE';
    const upper = fi.toUpperCase();
    if (upper === 'AFTER_FOOD' || upper === 'AFTER_MEAL') return 'AFTER_FOOD';
    if (upper === 'BEFORE_FOOD' || upper === 'BEFORE_MEAL') return 'BEFORE_FOOD';
    if (upper === 'WITH_FOOD' || upper === 'WITH_MEAL') return 'WITH_FOOD';
    if (upper === 'EMPTY_STOMACH') return 'EMPTY_STOMACH';
    return 'NONE';
  };

  // Add / Edit Medication Form State
  const [isMedModalOpen, setIsMedModalOpen] = useState(false);
  const [editingMedicationId, setEditingMedicationId] = useState<string | null>(null);
  const [medName, setMedName] = useState('');
  const [dosageAmount, setDosageAmount] = useState<number>(1);
  const [dosageUnit, setDosageUnit] = useState('TABLET');
  const [frequency, setFrequency] = useState('ONCE_DAILY');
  const [selectedSlots, setSelectedSlots] = useState<TimeSlot[]>(['MORNING']);
  const [foodInstruction, setFoodInstruction] = useState<FoodInstruction>('AFTER_FOOD');
  const [startDate, setStartDate] = useState(new Date().toISOString().split('T')[0]);
  const [endDate, setEndDate] = useState('');
  const [instructions, setInstructions] = useState('');
  const [imageFile, setImageFile] = useState<File | null>(null);
  const [existingImageUrl, setExistingImageUrl] = useState<string | undefined>(undefined);
  const [formValidationError, setFormValidationError] = useState<string | null>(null);
  const [showConfirmSave, setShowConfirmSave] = useState(false);

  // Discontinue Modal State
  const [discontinueTarget, setDiscontinueTarget] = useState<Medication | null>(null);

  // Caregiver Modal State
  const [isCaregiverModalOpen, setIsCaregiverModalOpen] = useState(false);
  const [editingCaregiverId, setEditingCaregiverId] = useState<string | null>(null);
  const [caregiverName, setCaregiverName] = useState('');
  const [caregiverRelationship, setCaregiverRelationship] = useState('Spouse');
  const [caregiverPhone, setCaregiverPhone] = useState('');
  const [caregiverEmail, setCaregiverEmail] = useState('');
  const [caregiverChannel, setCaregiverChannel] = useState<'WHATSAPP' | 'SMS' | 'EMAIL'>('WHATSAPP');
  const [caregiverConsent, setCaregiverConsent] = useState(true);
  const [caregiverFormError, setCaregiverFormError] = useState<string | null>(null);

  // Photo Preview Modal
  const [previewPhotoUrl, setPreviewPhotoUrl] = useState<string | null>(null);

  // Time slot configurations
  const timeSlots: { key: TimeSlot; label: string; icon: React.ReactNode; defaultTime: string }[] = [
    { key: 'MORNING', label: t('medSlotMorning'), icon: <Sun size={20} color="#f59e0b" />, defaultTime: '08:00' },
    { key: 'AFTERNOON', label: t('medSlotAfternoon'), icon: <Sun size={20} color="#ea580c" />, defaultTime: '13:00' },
    { key: 'EVENING', label: t('medSlotEvening'), icon: <Sunset size={20} color="#8b5cf6" />, defaultTime: '18:00' },
    { key: 'NIGHT', label: t('medSlotNight'), icon: <Moon size={20} color="#3b82f6" />, defaultTime: '21:00' },
  ];

  const toggleSlot = (slot: TimeSlot) => {
    if (selectedSlots.includes(slot)) {
      if (selectedSlots.length > 1) {
        setSelectedSlots(selectedSlots.filter((s) => s !== slot));
      }
    } else {
      setSelectedSlots([...selectedSlots, slot]);
    }
  };

  // Open modal for creating new medication
  const handleOpenAddMedModal = () => {
    setEditingMedicationId(null);
    setMedName('');
    setDosageAmount(1);
    setDosageUnit('TABLET');
    setFrequency('ONCE_DAILY');
    setSelectedSlots(['MORNING']);
    setFoodInstruction('AFTER_FOOD');
    setStartDate(new Date().toISOString().split('T')[0]);
    setEndDate('');
    setInstructions('');
    setImageFile(null);
    setExistingImageUrl(undefined);
    setFormValidationError(null);
    setShowConfirmSave(false);
    setIsMedModalOpen(true);
  };

  // Open modal for editing existing medication
  const handleOpenEditMedModal = (med: Medication) => {
    setEditingMedicationId(med.id);
    setMedName(med.name || med.medicineName || '');
    setDosageAmount(med.dosageAmount || 1);
    setDosageUnit(med.dosageUnit || 'TABLET');
    setFrequency(med.frequency || 'ONCE_DAILY');

    // Infer slots from reminderTimes
    if (med.reminderTimes && med.reminderTimes.length > 0) {
      const slots: TimeSlot[] = [];
      med.reminderTimes.forEach((rt) => {
        const hour = parseInt(rt.split(':')[0], 10);
        if (hour < 12) slots.push('MORNING');
        else if (hour < 16) slots.push('AFTERNOON');
        else if (hour < 20) slots.push('EVENING');
        else slots.push('NIGHT');
      });
      setSelectedSlots(slots.length > 0 ? Array.from(new Set(slots)) : ['MORNING']);
    } else {
      setSelectedSlots(['MORNING']);
    }

    setFoodInstruction(normalizeFoodInstruction(med.foodInstruction));
    setStartDate(med.startDate || new Date().toISOString().split('T')[0]);
    setEndDate(med.endDate || '');
    setInstructions(med.instructions || '');
    setImageFile(null);
    setExistingImageUrl(med.imageUrl || med.photoUrl);
    setFormValidationError(null);
    setShowConfirmSave(false);
    setIsMedModalOpen(true);
  };

  // Validate form before confirmation step
  const handleValidateMedicationForm = (e: React.FormEvent) => {
    e.preventDefault();
    if (!medName.trim()) {
      setFormValidationError('Please enter a medicine name.');
      return;
    }
    if (!dosageAmount || dosageAmount <= 0) {
      setFormValidationError('Please enter a valid dosage amount greater than zero.');
      return;
    }
    if (selectedSlots.length === 0) {
      setFormValidationError('Please select at least one daily reminder slot.');
      return;
    }
    setFormValidationError(null);
    setShowConfirmSave(true);
  };

  // Final confirmed submission
  const handleConfirmSaveMedication = async () => {
    try {
      const reminderTimes = timeSlotsToReminderTimes(selectedSlots);
      if (editingMedicationId) {
        await updateMedication(editingMedicationId, {
          name: medName.trim(),
          dosageAmount: Number(dosageAmount),
          dosageUnit,
          timesPerDay: selectedSlots.length,
          reminderTimes,
          foodInstruction,
          instructions: instructions.trim(),
          endDate: endDate ? endDate : undefined,
        });
      } else {
        await addMedication(
          {
            patientId,
            name: medName.trim(),
            medicineName: medName.trim(),
            dosageAmount: Number(dosageAmount),
            dosageUnit,
            frequency,
            timesPerDay: selectedSlots.length,
            reminderTimes,
            foodInstruction,
            startDate: startDate || new Date().toISOString().split('T')[0],
            endDate: endDate ? endDate : undefined,
            instructions: instructions.trim(),
          },
          imageFile || undefined
        );
      }
      setIsMedModalOpen(false);
      setShowConfirmSave(false);
    } catch (err: any) {
      setFormValidationError(err.message || 'Failed to save medication');
      setShowConfirmSave(false);
    }
  };

  // Discontinue handling
  const handleConfirmDiscontinue = async () => {
    if (!discontinueTarget) return;
    try {
      await discontinueMedication(discontinueTarget.id);
      setDiscontinueTarget(null);
    } catch (err: any) {
      alert('Failed to discontinue medication: ' + err.message);
    }
  };

  // Caregiver Modal Handlers
  const handleOpenAddCaregiverModal = () => {
    setEditingCaregiverId(null);
    setCaregiverName('');
    setCaregiverRelationship('Spouse');
    setCaregiverPhone('');
    setCaregiverEmail('');
    setCaregiverChannel('WHATSAPP');
    setCaregiverConsent(true);
    setCaregiverFormError(null);
    setIsCaregiverModalOpen(true);
  };

  const handleOpenEditCaregiverModal = (cg: Caregiver) => {
    setEditingCaregiverId(cg.id);
    setCaregiverName(cg.name || cg.caregiverName || '');
    setCaregiverRelationship(cg.relationship || 'Spouse');
    setCaregiverPhone(cg.phoneNumber || cg.contactNumber || '');
    setCaregiverEmail(cg.email || '');
    setCaregiverChannel((cg.preferredChannel as any) || 'WHATSAPP');
    setCaregiverConsent(cg.consentGiven !== false);
    setCaregiverFormError(null);
    setIsCaregiverModalOpen(true);
  };

  const handleSaveCaregiverSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!caregiverName.trim()) {
      setCaregiverFormError('Please enter caregiver name.');
      return;
    }
    if (!caregiverPhone.trim()) {
      setCaregiverFormError('Please enter caregiver phone number.');
      return;
    }

    try {
      if (editingCaregiverId) {
        await updateCaregiver(editingCaregiverId, {
          name: caregiverName.trim(),
          relationship: caregiverRelationship,
          phoneNumber: caregiverPhone.trim(),
          email: caregiverEmail.trim() || undefined,
          preferredChannel: caregiverChannel,
          consentGiven: caregiverConsent,
        });
      } else {
        await addCaregiver({
          patientId: patientId || undefined,
          name: caregiverName.trim(),
          relationship: caregiverRelationship,
          phoneNumber: caregiverPhone.trim(),
          email: caregiverEmail.trim() || undefined,
          preferredChannel: caregiverChannel,
          consentGiven: caregiverConsent,
          isActive: true,
          notifyOnMissedDose: caregiverConsent,
        });
      }
      setIsCaregiverModalOpen(false);
    } catch (err: any) {
      setCaregiverFormError(err.message || 'Failed to save caregiver');
    }
  };

  const filterDosesBySlot = (slot: TimeSlot): ScheduledDose[] => {
    return todaySchedule.filter((d) => {
      const doseSlot = d.timeSlot || (d.scheduledTime ? computeTimeSlot(d.scheduledTime) : undefined);
      return doseSlot === slot;
    });
  };

  const activeMedicationsCount = medications.filter((m) => m.status === 'ACTIVE' || m.active).length;
  const pendingDosesCount = todaySchedule.filter((d) => d.status === 'PENDING').length;

  return (
    <div style={{ maxWidth: '1050px', margin: '0 auto', paddingBottom: '3rem' }}>
      {/* Escalation Alerts Bar */}
      {escalations.length > 0 && (
        <div className="alert-box danger" style={{ marginBottom: '1.25rem' }}>
          <AlertTriangle size={24} color="#dc2626" />
          <div style={{ flex: 1 }}>
            <div style={{ fontWeight: 800, fontSize: '1rem', color: '#991b1b' }}>
              {t('medEscalationAlert')}
            </div>
            <div style={{ fontSize: '0.88rem', color: '#7f1d1d', marginTop: '0.2rem' }}>
              Consecutive missed dose detected on <strong>{escalations[0].medicationName || 'Scheduled Medicine'}</strong>. Automated alert dispatched to registered caregiver.
            </div>
          </div>
        </div>
      )}

      {/* Main Header */}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1.25rem', flexWrap: 'wrap', gap: '1rem' }}>
        <div>
          <h2 style={{ fontSize: '1.6rem', fontWeight: 800, color: 'var(--text-main)', display: 'flex', alignItems: 'center', gap: '0.6rem' }}>
            <Pill size={28} color="var(--primary)" />
            <span>{t('medCabinetTitle')}</span>
          </h2>
          <p style={{ fontSize: '0.92rem', color: 'var(--text-subtle)', marginTop: '0.25rem' }}>
            Accurate dosage tracking, private AWS S3 prescription storage & family caregiver alerts
          </p>
        </div>

        <div style={{ display: 'flex', gap: '0.6rem' }}>
          <button
            type="button"
            className="btn btn-primary"
            style={{ minHeight: '44px', padding: '0 1.25rem', fontSize: '0.95rem', fontWeight: 700 }}
            onClick={handleOpenAddMedModal}
          >
            <Plus size={20} />
            <span>{t('medAddTitle')}</span>
          </button>
        </div>
      </div>

      {/* Daily Dose Adherence Progress Strip */}
      {todaySchedule.length > 0 && (() => {
        const takenCount = todaySchedule.filter((d) => d.status === 'TAKEN').length;
        const missedCount = todaySchedule.filter((d) => d.status === 'MISSED').length;
        const adherencePct = Math.round((takenCount / todaySchedule.length) * 100);
        return (
          <div
            className="stat-card-modern"
            style={{
              marginBottom: '1.25rem',
              background: 'linear-gradient(135deg, #f0fdfa 0%, #ffffff 100%)',
              borderColor: 'var(--primary-border)',
            }}
          >
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: '0.75rem', marginBottom: '0.5rem' }}>
              <div>
                <div style={{ fontSize: '0.78rem', fontWeight: 800, color: 'var(--primary)', textTransform: 'uppercase', letterSpacing: '0.04em' }}>
                  Daily Medication Adherence • आजचे औषध पालन
                </div>
                <div style={{ fontSize: '1rem', fontWeight: 800, color: 'var(--text-main)' }}>
                  {takenCount} of {todaySchedule.length} scheduled doses taken today ({adherencePct}%)
                </div>
              </div>
              <div style={{ display: 'flex', gap: '0.5rem', flexWrap: 'wrap' }}>
                <span className="badge badge-routine">{takenCount} Taken</span>
                <span className="badge badge-urgent">{pendingDosesCount} Pending</span>
                {missedCount > 0 && <span className="badge badge-emergency">{missedCount} Missed</span>}
              </div>
            </div>
            <div style={{ width: '100%', height: '8px', backgroundColor: '#ccfbf1', borderRadius: '9999px', overflow: 'hidden' }}>
              <div
                style={{
                  width: `${Math.max(4, adherencePct)}%`,
                  height: '100%',
                  backgroundColor: 'var(--primary)',
                  borderRadius: '9999px',
                  transition: 'width 0.4s ease',
                }}
              />
            </div>
          </div>
        );
      })()}

      {/* Navigation Tabs */}
      <div className="tabs-nav" style={{ marginBottom: '1.5rem' }}>
        <button
          type="button"
          className={`tab-btn ${activeTab === 'today' ? 'active' : ''}`}
          onClick={() => setActiveTab('today')}
        >
          <Clock size={18} />
          <span>{t('medTabToday')}</span>
          {pendingDosesCount > 0 && (
            <span className="badge badge-urgent" style={{ marginLeft: '0.4rem', fontSize: '0.75rem' }}>
              {pendingDosesCount} pending
            </span>
          )}
        </button>

        <button
          type="button"
          className={`tab-btn ${activeTab === 'medications' ? 'active' : ''}`}
          onClick={() => setActiveTab('medications')}
        >
          <Pill size={18} />
          <span>{t('medTabAllMeds')}</span>
          <span className="badge badge-routine" style={{ marginLeft: '0.4rem', fontSize: '0.75rem' }}>
            {activeMedicationsCount} active
          </span>
        </button>

        <button
          type="button"
          className={`tab-btn ${activeTab === 'caregivers' ? 'active' : ''}`}
          onClick={() => setActiveTab('caregivers')}
        >
          <Users size={18} />
          <span>{t('medTabCaregivers')}</span>
          <span className="badge" style={{ background: '#e0e7ff', color: '#3730a3', marginLeft: '0.4rem', fontSize: '0.75rem' }}>
            {caregivers.length}
          </span>
        </button>
      </div>

      {/* TAB 1: TODAY'S SCHEDULE */}
      {activeTab === 'today' && (
        <div>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1rem' }}>
            <h3 style={{ fontSize: '1.15rem', fontWeight: 700, color: 'var(--text-main)' }}>
              {t('medTodaySchedule')}
            </h3>
            <span style={{ fontSize: '0.85rem', color: 'var(--text-subtle)' }}>
              Timezone: IST (UTC+05:30) • Grace period: 60 mins
            </span>
          </div>

          <div className="med-timeslot-grid">
            {timeSlots.map(({ key, label, icon, defaultTime }) => {
              const slotDoses = filterDosesBySlot(key);
              return (
                <div key={key} className="timeslot-card" style={{ boxShadow: 'var(--shadow-sm)' }}>
                  <div className="timeslot-header">
                    <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                      {icon}
                      <span style={{ fontSize: '1rem' }}>{label}</span>
                    </div>
                    <span className="badge badge-routine" style={{ fontSize: '0.75rem' }}>
                      {slotDoses.length} {slotDoses.length === 1 ? 'dose' : 'doses'}
                    </span>
                  </div>

                  <div style={{ minHeight: '130px', padding: '0.5rem 0' }}>
                    {slotDoses.length === 0 ? (
                      <div style={{ padding: '2rem 1rem', textAlign: 'center', color: 'var(--text-subtle)', fontSize: '0.88rem' }}>
                        No medicines scheduled
                      </div>
                    ) : (
                      slotDoses.map((dose) => {
                        const isTaken = dose.status === 'TAKEN';
                        const isSkipped = dose.status === 'SKIPPED';
                        const isMissed = dose.status === 'MISSED';

                        return (
                          <div
                            key={dose.eventId || dose.id}
                            className="dose-item"
                            style={{
                              background: isTaken ? 'rgba(34, 197, 94, 0.04)' : isMissed ? 'rgba(239, 68, 68, 0.04)' : undefined,
                            }}
                          >
                            <div style={{ flex: 1 }}>
                              <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', flexWrap: 'wrap' }}>
                                <span style={{ fontWeight: 800, fontSize: '1rem', color: 'var(--text-main)' }}>
                                  {dose.medicationName || dose.medicineName}
                                </span>
                                {dose.scheduledTime && (
                                  <span className="badge" style={{ fontSize: '0.75rem', background: '#f3f4f6', color: '#4b5563', padding: '0.15rem 0.4rem' }}>
                                    <Clock size={12} style={{ display: 'inline', marginRight: '0.2rem', verticalAlign: '-1px' }} />
                                    {typeof dose.scheduledTime === 'string' ? dose.scheduledTime.slice(0, 5) : String(dose.scheduledTime)}
                                  </span>
                                )}
                              </div>
                              <div style={{ fontSize: '0.84rem', color: 'var(--text-subtle)', marginTop: '0.15rem' }}>
                                <strong>
                                  {dose.dosageAmount ? `${dose.dosageAmount} ${dose.dosageUnit || 'dose'}` : dose.dosage || '1 dose'}
                                </strong>
                                {dose.foodInstruction && dose.foodInstruction !== 'NONE' && (
                                  <span style={{ marginLeft: '0.4rem', color: '#4b5563' }}>
                                    • {formatFoodInstruction(dose.foodInstruction)}
                                  </span>
                                )}
                              </div>
                              {dose.instructions && (
                                <div style={{ fontSize: '0.78rem', color: 'var(--text-muted)', fontStyle: 'italic', marginTop: '0.1rem' }}>
                                  "{dose.instructions}"
                                </div>
                              )}
                            </div>

                            <div style={{ display: 'flex', flexDirection: 'column', gap: '0.4rem', alignItems: 'flex-end' }}>
                              {isTaken ? (
                                <span className="badge badge-routine" style={{ fontSize: '0.82rem', padding: '0.35rem 0.65rem' }}>
                                  <Check size={14} /> {t('medDoseTaken')}
                                </span>
                              ) : isSkipped ? (
                                <span className="badge badge-urgent" style={{ fontSize: '0.82rem', padding: '0.35rem 0.65rem', background: '#f3f4f6', color: '#4b5563' }}>
                                  <X size={14} /> {t('medDoseSkipped')}
                                </span>
                              ) : isMissed ? (
                                <span className="badge badge-urgent" style={{ fontSize: '0.82rem', padding: '0.35rem 0.65rem' }}>
                                  <AlertTriangle size={14} /> {t('medDoseMissed')}
                                </span>
                              ) : (
                                <div style={{ display: 'flex', gap: '0.4rem' }}>
                                  <button
                                    type="button"
                                    className="btn btn-primary"
                                    style={{
                                      minHeight: '40px',
                                      padding: '0 0.85rem',
                                      fontSize: '0.85rem',
                                      fontWeight: 700,
                                      background: '#15803d',
                                    }}
                                    onClick={() => handleTakeDose(dose.eventId || dose.id)}
                                    title={t('medTakeDose')}
                                  >
                                    <CheckCircle size={16} />
                                    <span>{t('medTakeDose')}</span>
                                  </button>
                                  <button
                                    type="button"
                                    className="btn btn-outline"
                                    style={{ minHeight: '40px', padding: '0 0.65rem', fontSize: '0.85rem', color: '#6b7280' }}
                                    onClick={() => markDose(dose.eventId || dose.id, 'skip')}
                                    title={t('medSkipDose')}
                                  >
                                    <XCircle size={16} />
                                    <span>{t('medSkipDose')}</span>
                                  </button>
                                </div>
                              )}
                            </div>
                          </div>
                        );
                      })
                    )}
                  </div>
                </div>
              );
            })}
          </div>
        </div>
      )}

      {/* TAB 2: ALL MEDICINES */}
      {activeTab === 'medications' && (
        <div>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1.25rem' }}>
            <h3 style={{ fontSize: '1.15rem', fontWeight: 700, color: 'var(--text-main)' }}>
              Prescribed Medicines & Active Regimens ({medications.length})
            </h3>
            <button
              type="button"
              className="btn btn-primary"
              style={{ minHeight: '40px', fontSize: '0.88rem' }}
              onClick={handleOpenAddMedModal}
            >
              <Plus size={16} />
              <span>{t('medAddTitle')}</span>
            </button>
          </div>

          {medications.length === 0 ? (
            <div className="card" style={{ textAlign: 'center', padding: '3rem 1.5rem', color: 'var(--text-subtle)' }}>
              <Pill size={40} color="var(--primary)" style={{ margin: '0 auto 1rem', opacity: 0.7 }} />
              <div style={{ fontWeight: 700, fontSize: '1.1rem', color: 'var(--text-main)' }}>
                No Medications Registered
              </div>
              <p style={{ marginTop: '0.5rem', fontSize: '0.9rem' }}>
                Add your prescribed medicines to begin receiving timely reminders and automated caregiver monitoring.
              </p>
              <button
                type="button"
                className="btn btn-primary"
                style={{ marginTop: '1.25rem' }}
                onClick={handleOpenAddMedModal}
              >
                <Plus size={16} />
                <span>{t('medAddTitle')}</span>
              </button>
            </div>
          ) : (
            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(320px, 1fr))', gap: '1rem' }}>
              {medications.map((med) => {
                const isActive = med.status === 'ACTIVE' || (med.status === undefined && med.active !== false);
                const photoSrc = med.imageUrl || med.photoUrl;

                return (
                  <div
                    key={med.id}
                    className="card"
                    style={{
                      display: 'flex',
                      flexDirection: 'column',
                      justifyContent: 'space-between',
                      opacity: isActive ? 1 : 0.75,
                      border: isActive ? '1px solid var(--border-light)' : '1px dashed #9ca3af',
                      background: isActive ? 'var(--bg-surface)' : '#f9fafb',
                    }}
                  >
                    <div>
                      {/* Top Header of Card */}
                      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: '0.75rem' }}>
                        <div>
                          <h4 style={{ fontSize: '1.15rem', fontWeight: 800, color: 'var(--text-main)' }}>
                            {med.name || med.medicineName}
                          </h4>
                          <div style={{ fontSize: '0.85rem', color: 'var(--text-subtle)', marginTop: '0.1rem' }}>
                            {med.dosageAmount ? `${med.dosageAmount} ${med.dosageUnit || 'dose'}` : med.dosage || 'Standard dose'}
                          </div>
                        </div>

                        <span
                          className={`badge ${isActive ? 'badge-routine' : 'badge-urgent'}`}
                          style={{
                            background: isActive ? 'var(--risk-low-bg)' : '#e5e7eb',
                            color: isActive ? 'var(--risk-low-text)' : '#4b5563',
                          }}
                        >
                          {isActive ? t('medActive') : t('medDiscontinued')}
                        </span>
                      </div>

                      {/* Reminder Times & Food Instructions */}
                      <div style={{ display: 'flex', flexWrap: 'wrap', gap: '0.4rem', margin: '0.6rem 0' }}>
                        {med.reminderTimes && med.reminderTimes.length > 0 ? (
                          med.reminderTimes.map((time, idx) => (
                            <span
                              key={idx}
                              style={{
                                display: 'inline-flex',
                                alignItems: 'center',
                                gap: '0.25rem',
                                padding: '0.2rem 0.5rem',
                                background: '#f3f4f6',
                                borderRadius: '4px',
                                fontSize: '0.8rem',
                                fontWeight: 600,
                              }}
                            >
                              <Clock size={12} color="#6b7280" />
                              {time.slice(0, 5)}
                            </span>
                          ))
                        ) : (
                          <span style={{ fontSize: '0.8rem', color: 'var(--text-subtle)' }}>
                            {med.timesPerDay || 1} time(s) daily
                          </span>
                        )}

                        {med.foodInstruction && med.foodInstruction !== 'NONE' && (
                          <span
                            style={{
                              display: 'inline-flex',
                              alignItems: 'center',
                              gap: '0.25rem',
                              padding: '0.2rem 0.5rem',
                              background: '#eff6ff',
                              color: '#1d4ed8',
                              borderRadius: '4px',
                              fontSize: '0.8rem',
                              fontWeight: 600,
                            }}
                          >
                            {formatFoodInstruction(med.foodInstruction)}
                          </span>
                        )}
                      </div>

                      {/* Clinical Instructions */}
                      {med.instructions && (
                        <div style={{ fontSize: '0.84rem', color: '#4b5563', background: '#f9fafb', padding: '0.5rem 0.75rem', borderRadius: '4px', marginBottom: '0.75rem' }}>
                          <strong>Note:</strong> {med.instructions}
                        </div>
                      )}

                      {/* Photo / Prescription reference */}
                      {photoSrc && (
                        <div style={{ marginTop: '0.5rem', marginBottom: '0.75rem' }}>
                          <button
                            type="button"
                            className="btn btn-outline"
                            style={{ minHeight: '34px', padding: '0 0.6rem', fontSize: '0.8rem', gap: '0.4rem' }}
                            onClick={() => setPreviewPhotoUrl(photoSrc)}
                          >
                            <Eye size={14} color="var(--primary)" />
                            <span>View S3 Prescription Photo</span>
                          </button>
                        </div>
                      )}

                      {/* Start / End Dates */}
                      <div style={{ fontSize: '0.8rem', color: 'var(--text-subtle)', marginTop: '0.4rem' }}>
                        Start: {med.startDate || 'Current'} {med.endDate ? `• End: ${med.endDate}` : ''}
                      </div>
                    </div>

                    {/* Bottom Action Buttons */}
                    <div style={{ display: 'flex', gap: '0.5rem', marginTop: '1.25rem', borderTop: '1px solid var(--border-light)', paddingTop: '0.85rem' }}>
                      <button
                        type="button"
                        className="btn btn-outline"
                        style={{ flex: 1, minHeight: '38px', fontSize: '0.85rem' }}
                        onClick={() => handleOpenEditMedModal(med)}
                      >
                        <Edit2 size={14} />
                        <span>Edit</span>
                      </button>

                      {isActive && (
                        <button
                          type="button"
                          className="btn btn-outline"
                          style={{ minHeight: '38px', fontSize: '0.85rem', color: '#dc2626', borderColor: '#fca5a5' }}
                          onClick={() => setDiscontinueTarget(med)}
                        >
                          <Ban size={14} />
                          <span>{t('medDiscontinue')}</span>
                        </button>
                      )}
                    </div>
                  </div>
                );
              })}
            </div>
          )}
        </div>
      )}

      {/* TAB 3: CAREGIVERS & ESCALATION ALERTS */}
      {activeTab === 'caregivers' && (
        <div>
          <div className="card" style={{ marginBottom: '1.5rem' }}>
            <div className="card-header" style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
              <div>
                <h3 style={{ fontSize: '1.15rem', fontWeight: 700, display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                  <Users size={20} color="var(--primary)" />
                  <span>{t('medCaregivers')}</span>
                </h3>
                <p style={{ fontSize: '0.85rem', color: 'var(--text-subtle)', marginTop: '0.2rem' }}>
                  Designated family members who receive automated alerts if you miss your scheduled medicine.
                </p>
              </div>

              <button
                type="button"
                className="btn btn-primary"
                style={{ minHeight: '40px', fontSize: '0.88rem' }}
                onClick={handleOpenAddCaregiverModal}
              >
                <Plus size={16} />
                <span>Add Caregiver</span>
              </button>
            </div>

            {caregivers.length === 0 ? (
              <div style={{ textAlign: 'center', padding: '2.5rem 1rem', color: 'var(--text-subtle)' }}>
                <ShieldAlert size={36} color="#d97706" style={{ margin: '0 auto 0.75rem' }} />
                <div style={{ fontWeight: 700, fontSize: '1rem', color: 'var(--text-main)' }}>
                  No Caregivers Registered
                </div>
                <p style={{ fontSize: '0.88rem', marginTop: '0.3rem', maxWidth: '480px', margin: '0.3rem auto 1rem' }}>
                  Add a family member (spouse, child, parent) to receive SMS, WhatsApp or Email notifications when a medicine is missed.
                </p>
                <button
                  type="button"
                  className="btn btn-primary"
                  onClick={handleOpenAddCaregiverModal}
                >
                  <Plus size={16} />
                  <span>Add First Caregiver</span>
                </button>
              </div>
            ) : (
              <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(300px, 1fr))', gap: '1rem' }}>
                {caregivers.map((cg) => {
                  const hasConsent = cg.consentGiven !== false;
                  return (
                    <div
                      key={cg.id}
                      style={{
                        border: '1px solid var(--border-light)',
                        borderRadius: 'var(--radius-md)',
                        padding: '1rem',
                        background: 'var(--bg-surface)',
                        display: 'flex',
                        flexDirection: 'column',
                        justifyContent: 'space-between',
                      }}
                    >
                      <div>
                        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
                          <div>
                            <div style={{ fontWeight: 800, fontSize: '1.05rem', color: 'var(--text-main)' }}>
                              {cg.name || cg.caregiverName}
                            </div>
                            <div style={{ fontSize: '0.84rem', color: 'var(--text-subtle)' }}>
                              {cg.relationship}
                            </div>
                          </div>

                          <span
                            className="badge"
                            style={{
                              background: hasConsent ? '#dcfce7' : '#fef3c7',
                              color: hasConsent ? '#166534' : '#92400e',
                              fontSize: '0.72rem',
                            }}
                          >
                            {hasConsent ? (
                              <>
                                <ShieldCheck size={12} /> Consent Granted
                              </>
                            ) : (
                              <>
                                <ShieldAlert size={12} /> Consent Missing
                              </>
                            )}
                          </span>
                        </div>

                        <div style={{ marginTop: '0.75rem', fontSize: '0.85rem', display: 'flex', flexDirection: 'column', gap: '0.3rem' }}>
                          <div style={{ display: 'flex', alignItems: 'center', gap: '0.4rem', color: '#374151' }}>
                            <Phone size={14} color="#6b7280" />
                            <span>{cg.phoneNumber || cg.contactNumber || 'No phone'}</span>
                          </div>

                          {cg.email && (
                            <div style={{ display: 'flex', alignItems: 'center', gap: '0.4rem', color: '#374151' }}>
                              <Mail size={14} color="#6b7280" />
                              <span>{cg.email}</span>
                            </div>
                          )}

                          <div style={{ display: 'flex', alignItems: 'center', gap: '0.4rem', color: '#374151' }}>
                            <MessageSquare size={14} color="#6b7280" />
                            <span>Channel: <strong>{cg.preferredChannel || 'WHATSAPP'}</strong></span>
                          </div>
                        </div>
                      </div>

                      <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '0.5rem', marginTop: '1rem', borderTop: '1px solid var(--border-light)', paddingTop: '0.75rem' }}>
                        <button
                          type="button"
                          className="btn btn-outline"
                          style={{ minHeight: '34px', padding: '0 0.75rem', fontSize: '0.82rem' }}
                          onClick={() => handleOpenEditCaregiverModal(cg)}
                        >
                          <Edit2 size={13} />
                          <span>Edit</span>
                        </button>
                        <button
                          type="button"
                          className="btn btn-outline"
                          style={{ minHeight: '34px', padding: '0 0.75rem', fontSize: '0.82rem', color: '#dc2626', borderColor: '#fca5a5' }}
                          onClick={() => deleteCaregiver(cg.id)}
                        >
                          <Trash2 size={13} />
                          <span>Remove</span>
                        </button>
                      </div>
                    </div>
                  );
                })}
              </div>
            )}
          </div>

          {/* Missed-Dose Escalation Audit History */}
          <div className="card">
            <div className="card-header">
              <h3 style={{ fontSize: '1.1rem', fontWeight: 700, display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                <Clock size={18} color="var(--primary)" />
                <span>Missed-Dose Escalation History & Audit Logs</span>
              </h3>
              <p style={{ fontSize: '0.84rem', color: 'var(--text-subtle)', marginTop: '0.2rem' }}>
                Immutable audit trail of automated caregiver alerts dispatched upon missed medication doses.
              </p>
            </div>

            {escalations.length === 0 ? (
              <div style={{ padding: '1.5rem', textAlign: 'center', color: 'var(--text-subtle)', fontSize: '0.9rem' }}>
                No missed dose alerts on record. All doses have been adhered to or are within grace periods.
              </div>
            ) : (
              <div style={{ overflowX: 'auto' }}>
                <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: '0.88rem' }}>
                  <thead>
                    <tr style={{ borderBottom: '2px solid var(--border-light)', textAlign: 'left', color: 'var(--text-subtle)' }}>
                      <th style={{ padding: '0.6rem 0.75rem' }}>Medication</th>
                      <th style={{ padding: '0.6rem 0.75rem' }}>Caregiver</th>
                      <th style={{ padding: '0.6rem 0.75rem' }}>Channel</th>
                      <th style={{ padding: '0.6rem 0.75rem' }}>Status</th>
                      <th style={{ padding: '0.6rem 0.75rem' }}>Dispatched At</th>
                    </tr>
                  </thead>
                  <tbody>
                    {escalations.map((esc) => (
                      <tr key={esc.id} style={{ borderBottom: '1px solid var(--border-light)' }}>
                        <td style={{ padding: '0.65rem 0.75rem', fontWeight: 700 }}>
                          {esc.medicationName || 'Scheduled Medicine'}
                        </td>
                        <td style={{ padding: '0.65rem 0.75rem' }}>
                          {esc.caregiverName || 'Designated Contact'}
                        </td>
                        <td style={{ padding: '0.65rem 0.75rem' }}>
                          <span className="badge" style={{ background: '#f3f4f6', color: '#374151', fontSize: '0.72rem' }}>
                            {esc.channel || 'EMAIL'}
                          </span>
                        </td>
                        <td style={{ padding: '0.65rem 0.75rem' }}>
                          <span
                            className="badge"
                            style={{
                              background: esc.deliveryStatus === 'DELIVERED' || esc.deliveryStatus === 'SENT' ? '#dcfce7' : '#fee2e2',
                              color: esc.deliveryStatus === 'DELIVERED' || esc.deliveryStatus === 'SENT' ? '#166534' : '#991b1b',
                              fontSize: '0.72rem',
                            }}
                          >
                            {esc.deliveryStatus}
                          </span>
                        </td>
                        <td style={{ padding: '0.65rem 0.75rem', color: 'var(--text-subtle)' }}>
                          {esc.sentAt ? new Date(esc.sentAt).toLocaleString() : 'Recent'}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </div>
        </div>
      )}

      {/* ADD / EDIT MEDICATION MODAL */}
      {isMedModalOpen && (
        <div className="modal-overlay" onClick={() => !isLoading && setIsMedModalOpen(false)}>
          <div className="modal-dialog wide" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <h3 style={{ fontWeight: 800, fontSize: '1.25rem', display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                <Pill size={22} color="var(--primary)" />
                <span>{editingMedicationId ? t('medEditTitle') : t('medAddTitle')}</span>
              </h3>
              <button
                type="button"
                className="icon-btn"
                onClick={() => setIsMedModalOpen(false)}
                disabled={isLoading}
              >
                ✕
              </button>
            </div>

            {formValidationError && (
              <div style={{ background: '#fee2e2', color: '#991b1b', padding: '0.75rem 1.25rem', fontSize: '0.88rem', fontWeight: 600 }}>
                {formValidationError}
              </div>
            )}

            {!showConfirmSave ? (
              /* STEP 1: FORM INPUTS */
              <form onSubmit={handleValidateMedicationForm}>
                <div className="modal-body" style={{ maxHeight: '70vh', overflowY: 'auto' }}>
                  {/* Medicine Name */}
                  <div className="form-group" style={{ marginBottom: '1rem' }}>
                    <label className="form-label" style={{ fontWeight: 700 }}>
                      Medicine Name: *
                    </label>
                    <input
                      type="text"
                      className="form-control"
                      placeholder="e.g. Metformin 500mg, Paracetamol"
                      value={medName}
                      onChange={(e) => setMedName(e.target.value)}
                      required
                    />
                  </div>

                  {/* Dosage Amount & Unit */}
                  <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '1rem', marginBottom: '1rem' }}>
                    <div className="form-group">
                      <label className="form-label" style={{ fontWeight: 700 }}>
                        Dosage Amount: *
                      </label>
                      <input
                        type="number"
                        min="0.25"
                        step="0.25"
                        className="form-control"
                        value={dosageAmount}
                        onChange={(e) => setDosageAmount(parseFloat(e.target.value) || 1)}
                        required
                      />
                    </div>
                    <div className="form-group">
                      <label className="form-label" style={{ fontWeight: 700 }}>
                        Dosage Unit: *
                      </label>
                      <select
                        className="form-control"
                        value={dosageUnit}
                        onChange={(e) => setDosageUnit(e.target.value)}
                      >
                        <option value="TABLET">Tablet(s) (गोली / गोळ्या)</option>
                        <option value="CAPSULE">Capsule(s)</option>
                        <option value="SPOON">Spoon(s) (चम्मच)</option>
                        <option value="ML">ML (Milliliters / सिरप)</option>
                        <option value="DROP">Drop(s) (बूंद)</option>
                        <option value="OTHER">Other (अन्य)</option>
                      </select>
                    </div>
                  </div>

                  {/* Daily Reminder Time Slots */}
                  <div className="form-group" style={{ marginBottom: '1rem' }}>
                    <label className="form-label" style={{ fontWeight: 700 }}>
                      Daily Reminder Times (Choose slots): *
                    </label>
                    <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(130px, 1fr))', gap: '0.6rem' }}>
                      {timeSlots.map(({ key, label, icon, defaultTime }) => {
                        const isSelected = selectedSlots.includes(key);
                        return (
                          <button
                            key={key}
                            type="button"
                            onClick={() => toggleSlot(key)}
                            style={{
                              display: 'flex',
                              flexDirection: 'column',
                              alignItems: 'center',
                              justifyContent: 'center',
                              padding: '0.75rem 0.5rem',
                              borderRadius: '8px',
                              border: isSelected ? '2px solid var(--primary)' : '1px solid var(--border-light)',
                              background: isSelected ? 'var(--primary-light)' : 'var(--bg-surface)',
                              cursor: 'pointer',
                              fontWeight: isSelected ? 700 : 500,
                              color: isSelected ? 'var(--primary)' : 'var(--text-main)',
                            }}
                          >
                            <div style={{ display: 'flex', alignItems: 'center', gap: '0.35rem' }}>
                              {icon}
                              <span>{key}</span>
                            </div>
                            <span style={{ fontSize: '0.75rem', color: isSelected ? 'var(--primary)' : 'var(--text-subtle)', marginTop: '0.2rem' }}>
                              {defaultTime}
                            </span>
                          </button>
                        );
                      })}
                    </div>
                  </div>

                  {/* Food Instructions */}
                  <div className="form-group" style={{ marginBottom: '1rem' }}>
                    <label className="form-label" style={{ fontWeight: 700 }}>
                      Food Instruction:
                    </label>
                    <select
                      className="form-control"
                      value={foodInstruction}
                      onChange={(e) => setFoodInstruction(e.target.value as FoodInstruction)}
                    >
                      <option value="AFTER_FOOD">{t('medFoodAfter')}</option>
                      <option value="BEFORE_FOOD">{t('medFoodBefore')}</option>
                      <option value="WITH_FOOD">{t('medFoodWith')}</option>
                      <option value="EMPTY_STOMACH">{t('medFoodEmpty')}</option>
                      <option value="NONE">{t('medFoodNone')}</option>
                    </select>
                  </div>

                  {/* Dates */}
                  <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '1rem', marginBottom: '1rem' }}>
                    <div className="form-group">
                      <label className="form-label" style={{ fontWeight: 700 }}>
                        Start Date: *
                      </label>
                      <input
                        type="date"
                        className="form-control"
                        value={startDate}
                        onChange={(e) => setStartDate(e.target.value)}
                        required
                      />
                    </div>
                    <div className="form-group">
                      <label className="form-label" style={{ fontWeight: 700 }}>
                        End Date (Optional):
                      </label>
                      <input
                        type="date"
                        className="form-control"
                        value={endDate}
                        onChange={(e) => setEndDate(e.target.value)}
                      />
                    </div>
                  </div>

                  {/* Instructions / Notes */}
                  <div className="form-group" style={{ marginBottom: '1rem' }}>
                    <label className="form-label" style={{ fontWeight: 700 }}>
                      Doctor's Instructions / Notes:
                    </label>
                    <input
                      type="text"
                      className="form-control"
                      placeholder="e.g. Take with warm water, avoid milk"
                      value={instructions}
                      onChange={(e) => setInstructions(e.target.value)}
                    />
                  </div>

                  {/* Prescription / Medicine Photo Upload to AWS S3 */}
                  {!editingMedicationId && (
                    <div className="form-group" style={{ border: '1px dashed var(--border-light)', padding: '1rem', borderRadius: '8px', background: '#f9fafb' }}>
                      <label className="form-label" style={{ fontWeight: 700, display: 'flex', alignItems: 'center', gap: '0.4rem' }}>
                        <Upload size={16} color="var(--primary)" />
                        <span>{t('medUploadPhotoReference')}</span>
                      </label>
                      <p style={{ fontSize: '0.8rem', color: 'var(--text-subtle)', marginBottom: '0.6rem' }}>
                        Photos are stored in a private AWS S3 bucket. Authorized short-lived presigned URLs are generated on demand. Automated OCR dosage extraction is disabled to prevent dosage errors.
                      </p>
                      <input
                        type="file"
                        accept="image/*"
                        onChange={(e) => setImageFile(e.target.files?.[0] || null)}
                      />
                      {imageFile && (
                        <div style={{ marginTop: '0.4rem', fontSize: '0.82rem', color: '#16a34a', fontWeight: 600 }}>
                          ✓ Attached: {imageFile.name} ({(imageFile.size / 1024).toFixed(1)} KB)
                        </div>
                      )}
                    </div>
                  )}
                </div>

                <div className="modal-footer">
                  <button
                    type="button"
                    className="btn btn-outline"
                    onClick={() => setIsMedModalOpen(false)}
                    disabled={isLoading}
                  >
                    Cancel
                  </button>
                  <button
                    type="submit"
                    className="btn btn-primary"
                    style={{ minHeight: '42px', padding: '0 1.5rem', fontWeight: 700 }}
                  >
                    Review & Confirm Details →
                  </button>
                </div>
              </form>
            ) : (
              /* STEP 2: USER CONFIRMATION SCREEN */
              <div>
                <div className="modal-body">
                  <div style={{ background: '#f0fdf4', border: '1px solid #bbf7d0', padding: '1rem', borderRadius: '8px', marginBottom: '1.25rem' }}>
                    <div style={{ fontWeight: 800, fontSize: '1rem', color: '#166534', marginBottom: '0.5rem', display: 'flex', alignItems: 'center', gap: '0.4rem' }}>
                      <CheckCircle size={18} color="#16a34a" />
                      <span>{t('medConfirmAddTitle')}</span>
                    </div>
                    <p style={{ fontSize: '0.85rem', color: '#14532d' }}>
                      Please carefully review the prescribed medication details below before confirming.
                    </p>
                  </div>

                  <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '0.85rem', fontSize: '0.9rem' }}>
                    <div>
                      <strong style={{ color: 'var(--text-subtle)' }}>Medicine Name:</strong>
                      <div style={{ fontWeight: 800, fontSize: '1.05rem', color: 'var(--text-main)' }}>{medName}</div>
                    </div>

                    <div>
                      <strong style={{ color: 'var(--text-subtle)' }}>Prescribed Dose:</strong>
                      <div style={{ fontWeight: 700 }}>{dosageAmount} {dosageUnit}</div>
                    </div>

                    <div>
                      <strong style={{ color: 'var(--text-subtle)' }}>Reminder Times:</strong>
                      <div style={{ fontWeight: 700 }}>
                        {selectedSlots.join(', ')} ({timeSlotsToReminderTimes(selectedSlots).join(', ')})
                      </div>
                    </div>

                    <div>
                      <strong style={{ color: 'var(--text-subtle)' }}>Food Timing:</strong>
                      <div>{formatFoodInstruction(foodInstruction)}</div>
                    </div>

                    <div>
                      <strong style={{ color: 'var(--text-subtle)' }}>Schedule:</strong>
                      <div>Starts {startDate} {endDate ? `until ${endDate}` : '(Continuous)'}</div>
                    </div>

                    <div>
                      <strong style={{ color: 'var(--text-subtle)' }}>AWS S3 Photo:</strong>
                      <div>{imageFile ? imageFile.name : existingImageUrl ? 'Existing S3 Photo' : 'None'}</div>
                    </div>
                  </div>

                  {instructions && (
                    <div style={{ marginTop: '1rem', fontSize: '0.88rem', background: '#f9fafb', padding: '0.75rem', borderRadius: '6px' }}>
                      <strong>Doctor's Instructions:</strong> {instructions}
                    </div>
                  )}
                </div>

                <div className="modal-footer">
                  <button
                    type="button"
                    className="btn btn-outline"
                    onClick={() => setShowConfirmSave(false)}
                    disabled={isLoading}
                  >
                    ← Back to Edit
                  </button>
                  <button
                    type="button"
                    className="btn btn-primary"
                    style={{ minHeight: '44px', padding: '0 1.5rem', fontWeight: 800 }}
                    onClick={handleConfirmSaveMedication}
                    disabled={isLoading}
                  >
                    {isLoading ? 'Saving...' : '✓ Confirm & Save Medicine'}
                  </button>
                </div>
              </div>
            )}
          </div>
        </div>
      )}

      {/* DISCONTINUE CONFIRMATION MODAL */}
      {discontinueTarget && (
        <div className="modal-overlay" onClick={() => !isLoading && setDiscontinueTarget(null)}>
          <div className="modal-dialog" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <h3 style={{ fontWeight: 800, color: '#dc2626', display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                <AlertTriangle size={22} color="#dc2626" />
                <span>{t('medConfirmDiscontinueTitle')}</span>
              </h3>
              <button type="button" className="icon-btn" onClick={() => setDiscontinueTarget(null)}>✕</button>
            </div>
            <div className="modal-body">
              <p style={{ fontSize: '0.95rem', color: 'var(--text-main)', marginBottom: '1rem' }}>
                Are you sure you want to discontinue <strong>{discontinueTarget.name || discontinueTarget.medicineName}</strong>?
              </p>
              <div style={{ background: '#fef2f2', border: '1px solid #fecaca', borderRadius: '6px', padding: '0.75rem', fontSize: '0.85rem', color: '#991b1b' }}>
                This will immediately stop all future scheduled daily reminders and pause caregiver escalation alerts for this medication.
              </div>
            </div>
            <div className="modal-footer">
              <button
                type="button"
                className="btn btn-outline"
                onClick={() => setDiscontinueTarget(null)}
                disabled={isLoading}
              >
                Cancel
              </button>
              <button
                type="button"
                className="btn btn-primary"
                style={{ background: '#dc2626', borderColor: '#dc2626', fontWeight: 700 }}
                onClick={handleConfirmDiscontinue}
                disabled={isLoading}
              >
                {isLoading ? 'Processing...' : 'Yes, Discontinue Medicine'}
              </button>
            </div>
          </div>
        </div>
      )}

      {/* ADD / EDIT CAREGIVER MODAL */}
      {isCaregiverModalOpen && (
        <div className="modal-overlay" onClick={() => setIsCaregiverModalOpen(false)}>
          <div className="modal-dialog" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <h3 style={{ fontWeight: 800, display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                <Users size={20} color="var(--primary)" />
                <span>{editingCaregiverId ? 'Edit Caregiver Contact' : 'Register Designated Caregiver'}</span>
              </h3>
              <button type="button" className="icon-btn" onClick={() => setIsCaregiverModalOpen(false)}>✕</button>
            </div>

            {caregiverFormError && (
              <div style={{ background: '#fee2e2', color: '#991b1b', padding: '0.75rem 1.25rem', fontSize: '0.88rem', fontWeight: 600 }}>
                {caregiverFormError}
              </div>
            )}

            <form onSubmit={handleSaveCaregiverSubmit}>
              <div className="modal-body">
                <div className="form-group" style={{ marginBottom: '1rem' }}>
                  <label className="form-label" style={{ fontWeight: 700 }}>Caregiver Full Name: *</label>
                  <input
                    type="text"
                    className="form-control"
                    placeholder="e.g. Suresh Patil"
                    value={caregiverName}
                    onChange={(e) => setCaregiverName(e.target.value)}
                    required
                  />
                </div>

                <div className="form-group" style={{ marginBottom: '1rem' }}>
                  <label className="form-label" style={{ fontWeight: 700 }}>Relationship to Patient: *</label>
                  <select
                    className="form-control"
                    value={caregiverRelationship}
                    onChange={(e) => setCaregiverRelationship(e.target.value)}
                  >
                    <option value="Spouse">Spouse (पति / पत्नी)</option>
                    <option value="Son">Son (बेटा / मुलगा)</option>
                    <option value="Daughter">Daughter (बेटी / मुलगी)</option>
                    <option value="Parent">Parent (माता / पिता)</option>
                    <option value="Sibling">Brother / Sister (भाई / बहन)</option>
                    <option value="Caregiver">Volunteer / ASHA Worker</option>
                    <option value="Neighbor">Neighbor / Guardian</option>
                  </select>
                </div>

                <div className="form-group" style={{ marginBottom: '1rem' }}>
                  <label className="form-label" style={{ fontWeight: 700 }}>Phone Number (Mobile): *</label>
                  <input
                    type="tel"
                    className="form-control"
                    placeholder="e.g. +91 98765 43210"
                    value={caregiverPhone}
                    onChange={(e) => setCaregiverPhone(e.target.value)}
                    required
                  />
                </div>

                <div className="form-group" style={{ marginBottom: '1rem' }}>
                  <label className="form-label" style={{ fontWeight: 700 }}>Email Address (Optional for Email Alerts):</label>
                  <input
                    type="email"
                    className="form-control"
                    placeholder="e.g. suresh.patil@example.com"
                    value={caregiverEmail}
                    onChange={(e) => setCaregiverEmail(e.target.value)}
                  />
                </div>

                <div className="form-group" style={{ marginBottom: '1.25rem' }}>
                  <label className="form-label" style={{ fontWeight: 700 }}>Preferred Alert Channel:</label>
                  <select
                    className="form-control"
                    value={caregiverChannel}
                    onChange={(e) => setCaregiverChannel(e.target.value as any)}
                  >
                    <option value="WHATSAPP">WhatsApp Message</option>
                    <option value="SMS">SMS Text Message</option>
                    <option value="EMAIL">Email Alert</option>
                  </select>
                </div>

                {/* Explicit Consent Checkbox */}
                <div style={{ background: '#f8fafc', border: '1px solid #e2e8f0', borderRadius: '8px', padding: '0.85rem' }}>
                  <label style={{ display: 'flex', alignItems: 'flex-start', gap: '0.6rem', cursor: 'pointer' }}>
                    <input
                      type="checkbox"
                      checked={caregiverConsent}
                      onChange={(e) => setCaregiverConsent(e.target.checked)}
                      style={{ marginTop: '0.2rem', width: '18px', height: '18px' }}
                    />
                    <div>
                      <div style={{ fontWeight: 700, fontSize: '0.9rem', color: 'var(--text-main)' }}>
                        {t('medCaregiverConsent')}
                      </div>
                      <div style={{ fontSize: '0.8rem', color: 'var(--text-subtle)', marginTop: '0.2rem' }}>
                        {t('medCaregiverConsentDesc')}
                      </div>
                    </div>
                  </label>
                </div>
              </div>

              <div className="modal-footer">
                <button
                  type="button"
                  className="btn btn-outline"
                  onClick={() => setIsCaregiverModalOpen(false)}
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  className="btn btn-primary"
                  style={{ minHeight: '42px', fontWeight: 700 }}
                >
                  Save Caregiver Contact
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* S3 PHOTO PREVIEW MODAL */}
      {previewPhotoUrl && (
        <div className="modal-overlay" onClick={() => setPreviewPhotoUrl(null)}>
          <div className="modal-dialog" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <h3 style={{ fontWeight: 700 }}>Prescription / Medicine Photo Reference</h3>
              <button type="button" className="icon-btn" onClick={() => setPreviewPhotoUrl(null)}>✕</button>
            </div>
            <div className="modal-body" style={{ textAlign: 'center' }}>
              <img
                src={previewPhotoUrl}
                alt="Prescription Reference"
                style={{ maxWidth: '100%', maxHeight: '60vh', borderRadius: '8px', objectFit: 'contain' }}
              />
              <div style={{ marginTop: '0.75rem', fontSize: '0.8rem', color: 'var(--text-subtle)' }}>
                Loaded securely via AWS S3 authenticated presigned URL.
              </div>
            </div>
            <div className="modal-footer">
              <button
                type="button"
                className="btn btn-primary"
                onClick={() => setPreviewPhotoUrl(null)}
              >
                Close
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Dose Taken Confetti & Positive Encouragement */}
      <ConfettiCelebration
        active={showCelebration}
        onComplete={() => setShowCelebration(false)}
        message="🎉 शाबाश! औषध घेतले / Dose Recorded!"
        subMessage="Great job staying consistent with your treatment routine."
      />
    </div>
  );
};
