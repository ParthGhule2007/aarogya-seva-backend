import React, { useState } from 'react';
import { useMedicine } from '../../hooks/useMedicine';
import { useLanguage } from '../../context/LanguageContext';
import { TimeSlot, ScheduledDose, timeSlotsToReminderTimes } from '../../types/medicine';
import {
  Pill,
  Sun,
  Sunset,
  Moon,
  Clock,
  Camera,
  Plus,
  CheckCircle,
  XCircle,
  AlertTriangle,
  Users,
  Trash2,
  Upload,
  RefreshCw,
} from 'lucide-react';

export const MedicineCabinet: React.FC = () => {
  const {
    todaySchedule,
    medications,
    caregivers,
    escalations,
    isLoading,
    isOcrLoading,
    error,
    markDose,
    addMedication,
    scanPillPhoto,
    addCaregiver,
    deleteCaregiver,
  } = useMedicine();

  const { t } = useLanguage();

  const [isAddModalOpen, setIsAddModalOpen] = useState(false);
  const [newMedName, setNewMedName] = useState('');
  const [newDosage, setNewDosage] = useState('');
  const [newFrequency, setNewFrequency] = useState('ONCE_DAILY');
  const [newInstructions, setNewInstructions] = useState('');
  const [newTimesOfDay, setNewTimesOfDay] = useState<TimeSlot[]>(['MORNING']);

  // Caregiver form state
  const [isAddCaregiverOpen, setIsAddCaregiverOpen] = useState(false);
  const [caregiverName, setCaregiverName] = useState('');
  const [caregiverRelationship, setCaregiverRelationship] = useState('Spouse');
  const [caregiverPhone, setCaregiverPhone] = useState('');

  // Handle OCR Photo Upload
  const handlePhotoUpload = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;

    try {
      const ocrResult = await scanPillPhoto(file);
      if (ocrResult.medicineName) setNewMedName(ocrResult.medicineName);
      if (ocrResult.dosage) setNewDosage(ocrResult.dosage);
      if (ocrResult.frequency) setNewFrequency(ocrResult.frequency);
      setIsAddModalOpen(true);
    } catch {
      setIsAddModalOpen(true);
    }
  };

  const handleAddMedicationSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!newMedName) return;

    await addMedication({
      name: newMedName,
      medicineName: newMedName,
      dosage: newDosage || '1 tablet',
      dosageAmount: 1,
      dosageUnit: 'TABLET',
      timesPerDay: newTimesOfDay.length || 1,
      reminderTimes: timeSlotsToReminderTimes(newTimesOfDay),
      timesOfDay: newTimesOfDay,
      instructions: newInstructions,
      status: 'ACTIVE',
      active: true,
      startDate: new Date().toISOString().split('T')[0],
      foodInstruction: 'NONE',
    });

    setIsAddModalOpen(false);
    setNewMedName('');
    setNewDosage('');
    setNewInstructions('');
  };

  const handleAddCaregiverSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!caregiverName || !caregiverPhone) return;

    await addCaregiver({
      name: caregiverName,
      caregiverName,
      relationship: caregiverRelationship,
      phoneNumber: caregiverPhone,
      contactNumber: caregiverPhone,
      preferredChannel: 'WHATSAPP',
      isActive: true,
      notifyOnMissedDose: true,
      active: true,
    });

    setIsAddCaregiverOpen(false);
    setCaregiverName('');
    setCaregiverPhone('');
  };

  const toggleTimeOfDay = (slot: TimeSlot) => {
    if (newTimesOfDay.includes(slot)) {
      if (newTimesOfDay.length > 1) {
        setNewTimesOfDay(newTimesOfDay.filter((s) => s !== slot));
      }
    } else {
      setNewTimesOfDay([...newTimesOfDay, slot]);
    }
  };

  const filterDosesBySlot = (slot: TimeSlot): ScheduledDose[] => {
    return todaySchedule.filter((d) => d.timeSlot === slot);
  };

  const timeSlots: { key: TimeSlot; label: string; icon: React.ReactNode }[] = [
    { key: 'MORNING', label: t('medSlotMorning'), icon: <Sun size={20} color="#f59e0b" /> },
    { key: 'AFTERNOON', label: t('medSlotAfternoon'), icon: <Sun size={20} color="#ea580c" /> },
    { key: 'EVENING', label: t('medSlotEvening'), icon: <Sunset size={20} color="#8b5cf6" /> },
    { key: 'NIGHT', label: t('medSlotNight'), icon: <Moon size={20} color="#3b82f6" /> },
  ];

  return (
    <div style={{ maxWidth: '1000px', margin: '0 auto' }}>
      {/* Escalation Alerts */}
      {escalations.length > 0 && (
        <div className="alert-box danger">
          <AlertTriangle size={24} />
          <div>
            <div style={{ fontWeight: 800 }}>{t('medEscalationAlert')}</div>
            <div style={{ fontSize: '0.9rem' }}>
              Missed dose on <strong>{escalations[0].medicationName}</strong>. Automatic notification dispatched to designated family caregiver.
            </div>
          </div>
        </div>
      )}

      {/* Header with Add & OCR buttons */}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1.5rem', flexWrap: 'wrap', gap: '0.75rem' }}>
        <div>
          <h2 style={{ fontSize: '1.4rem', fontWeight: 800, color: 'var(--text-main)' }}>
            {t('medCabinetTitle')}
          </h2>
          <p style={{ fontSize: '0.9rem', color: 'var(--text-subtle)' }}>
            {t('medTodaySchedule')}
          </p>
        </div>

        <div style={{ display: 'flex', gap: '0.5rem' }}>
          {/* OCR Pill Photo Scanner */}
          <label className="btn btn-outline" style={{ cursor: 'pointer' }}>
            <Camera size={18} color="var(--primary)" />
            <span>{isOcrLoading ? 'Scanning...' : t('medOcrScan')}</span>
            <input
              type="file"
              accept="image/*"
              style={{ display: 'none' }}
              onChange={handlePhotoUpload}
              disabled={isOcrLoading}
            />
          </label>

          {/* Add Medication Manually */}
          <button className="btn btn-primary" onClick={() => setIsAddModalOpen(true)}>
            <Plus size={18} />
            <span>{t('medAddTitle')}</span>
          </button>
        </div>
      </div>

      {/* Four Daily Timeslot Cards */}
      <div className="med-timeslot-grid">
        {timeSlots.map(({ key, label, icon }) => {
          const slotDoses = filterDosesBySlot(key);
          return (
            <div key={key} className="timeslot-card">
              <div className="timeslot-header">
                <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                  {icon}
                  <span>{label}</span>
                </div>
                <span className="badge badge-routine" style={{ fontSize: '0.75rem' }}>
                  {slotDoses.length} {slotDoses.length === 1 ? 'dose' : 'doses'}
                </span>
              </div>

              <div style={{ minHeight: '120px' }}>
                {slotDoses.length === 0 ? (
                  <div style={{ padding: '1.5rem 1rem', textAlign: 'center', color: 'var(--text-subtle)', fontSize: '0.85rem' }}>
                    No medicines scheduled
                  </div>
                ) : (
                  slotDoses.map((dose) => (
                    <div key={dose.eventId || dose.id} className="dose-item">
                      <div>
                        <div style={{ fontWeight: 700, fontSize: '0.95rem' }}>
                          {dose.medicationName || dose.medicineName}
                        </div>
                        <div style={{ fontSize: '0.8rem', color: 'var(--text-subtle)' }}>
                          {dose.dosage || (dose.dosageAmount ? `${dose.dosageAmount} ${dose.dosageUnit || ''}` : '1 dose')} • {dose.instructions || 'With water'}
                        </div>
                      </div>

                      <div style={{ display: 'flex', gap: '0.35rem' }}>
                        {dose.status === 'TAKEN' ? (
                          <span className="badge badge-routine" style={{ fontSize: '0.75rem' }}>
                            ✓ {t('medDoseTaken')}
                          </span>
                        ) : dose.status === 'SKIPPED' ? (
                          <span className="badge badge-urgent" style={{ fontSize: '0.75rem' }}>
                            ✕ {t('medDoseSkipped')}
                          </span>
                        ) : (
                          <>
                            <button
                              type="button"
                              className="btn btn-primary"
                              style={{ minHeight: '36px', padding: '0 0.6rem', fontSize: '0.8rem' }}
                              onClick={() => markDose(dose.eventId, 'taken')}
                              title={t('medTakeDose')}
                            >
                              <CheckCircle size={14} />
                              <span>{t('medTakeDose')}</span>
                            </button>
                            <button
                              type="button"
                              className="btn btn-outline"
                              style={{ minHeight: '36px', padding: '0 0.5rem', fontSize: '0.8rem' }}
                              onClick={() => markDose(dose.eventId, 'skip')}
                              title={t('medSkipDose')}
                            >
                              <XCircle size={14} />
                            </button>
                          </>
                        )}
                      </div>
                    </div>
                  ))
                )}
              </div>
            </div>
          );
        })}
      </div>

      {/* Emergency Caregiver Management Section */}
      <div className="card" style={{ marginTop: '2rem' }}>
        <div className="card-header">
          <div className="card-title">
            <Users size={20} color="var(--primary)" />
            <span>{t('medCaregivers')}</span>
          </div>
          <button
            className="btn btn-outline"
            style={{ minHeight: '38px', fontSize: '0.85rem' }}
            onClick={() => setIsAddCaregiverOpen(true)}
          >
            <Plus size={16} />
            <span>Add Caregiver</span>
          </button>
        </div>

        {caregivers.length === 0 ? (
          <div style={{ padding: '1rem', color: 'var(--text-subtle)', fontSize: '0.9rem' }}>
            No caregiver contacts registered. Add a family member to receive missed dose alerts and emergency SOS calls.
          </div>
        ) : (
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(260px, 1fr))', gap: '0.75rem' }}>
            {caregivers.map((cg) => (
              <div
                key={cg.id}
                style={{
                  border: '1px solid var(--border-light)',
                  borderRadius: 'var(--radius-md)',
                  padding: '0.85rem 1rem',
                  display: 'flex',
                  justifyContent: 'space-between',
                  alignItems: 'center',
                }}
              >
                <div>
                  <div style={{ fontWeight: 700 }}>{cg.name || cg.caregiverName}</div>
                  <div style={{ fontSize: '0.8rem', color: 'var(--text-subtle)' }}>
                    {cg.relationship} • Ph: {cg.phoneNumber || cg.contactNumber}
                  </div>
                  {(cg.notifyOnMissedDose || cg.isActive) && (
                    <span className="badge badge-routine" style={{ marginTop: '0.25rem', fontSize: '0.7rem' }}>
                      Auto-Escalation Enabled
                    </span>
                  )}
                </div>
                <button
                  className="icon-btn"
                  style={{ minHeight: '36px', padding: '0 0.4rem', border: 'none', color: '#dc2626' }}
                  onClick={() => deleteCaregiver(cg.id)}
                  title="Remove caregiver"
                >
                  <Trash2 size={16} />
                </button>
              </div>
            ))}
          </div>
        )}
      </div>

      {/* Add Medication Modal */}
      {isAddModalOpen && (
        <div className="modal-overlay" onClick={() => setIsAddModalOpen(false)}>
          <div className="modal-dialog" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <h3 style={{ fontWeight: 700 }}>{t('medAddTitle')}</h3>
              <button className="icon-btn" onClick={() => setIsAddModalOpen(false)}>✕</button>
            </div>
            <form onSubmit={handleAddMedicationSubmit}>
              <div className="modal-body">
                <div className="form-group">
                  <label className="form-label">Medicine Name: *</label>
                  <input
                    type="text"
                    className="form-control"
                    placeholder="e.g. Paracetamol 500mg"
                    value={newMedName}
                    onChange={(e) => setNewMedName(e.target.value)}
                    required
                  />
                </div>

                <div className="form-group">
                  <label className="form-label">Dosage: *</label>
                  <input
                    type="text"
                    className="form-control"
                    placeholder="e.g. 1 Tablet"
                    value={newDosage}
                    onChange={(e) => setNewDosage(e.target.value)}
                    required
                  />
                </div>

                <div className="form-group">
                  <label className="form-label">Times of Day (Select all that apply):</label>
                  <div style={{ display: 'flex', gap: '0.5rem', flexWrap: 'wrap' }}>
                    {(['MORNING', 'AFTERNOON', 'EVENING', 'NIGHT'] as TimeSlot[]).map((slot) => (
                      <button
                        key={slot}
                        type="button"
                        onClick={() => toggleTimeOfDay(slot)}
                        className={`btn ${newTimesOfDay.includes(slot) ? 'btn-primary' : 'btn-outline'}`}
                        style={{ minHeight: '38px', fontSize: '0.85rem' }}
                      >
                        {slot}
                      </button>
                    ))}
                  </div>
                </div>

                <div className="form-group">
                  <label className="form-label">Instructions / Notes:</label>
                  <input
                    type="text"
                    className="form-control"
                    placeholder="e.g. After meals with warm water"
                    value={newInstructions}
                    onChange={(e) => setNewInstructions(e.target.value)}
                  />
                </div>
              </div>

              <div className="modal-footer">
                <button type="button" className="btn btn-outline" onClick={() => setIsAddModalOpen(false)}>
                  Cancel
                </button>
                <button type="submit" className="btn btn-primary">
                  Save Medication
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Add Caregiver Modal */}
      {isAddCaregiverOpen && (
        <div className="modal-overlay" onClick={() => setIsAddCaregiverOpen(false)}>
          <div className="modal-dialog" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <h3 style={{ fontWeight: 700 }}>Add Caregiver Contact</h3>
              <button className="icon-btn" onClick={() => setIsAddCaregiverOpen(false)}>✕</button>
            </div>
            <form onSubmit={handleAddCaregiverSubmit}>
              <div className="modal-body">
                <div className="form-group">
                  <label className="form-label">Caregiver Name: *</label>
                  <input
                    type="text"
                    className="form-control"
                    placeholder="e.g. Suresh Patil"
                    value={caregiverName}
                    onChange={(e) => setCaregiverName(e.target.value)}
                    required
                  />
                </div>

                <div className="form-group">
                  <label className="form-label">Relationship: *</label>
                  <select
                    className="form-control"
                    value={caregiverRelationship}
                    onChange={(e) => setCaregiverRelationship(e.target.value)}
                  >
                    <option value="Spouse">Spouse (पति / पत्नी)</option>
                    <option value="Son">Son (बेटा / मुलगा)</option>
                    <option value="Daughter">Daughter (बेटी / मुलगी)</option>
                    <option value="Parent">Parent (माता / पिता)</option>
                    <option value="Neighbor">Neighbor / Volunteer</option>
                  </select>
                </div>

                <div className="form-group">
                  <label className="form-label">Phone Number: *</label>
                  <input
                    type="tel"
                    className="form-control"
                    placeholder="e.g. 9876543210"
                    value={caregiverPhone}
                    onChange={(e) => setCaregiverPhone(e.target.value)}
                    required
                  />
                </div>
              </div>

              <div className="modal-footer">
                <button type="button" className="btn btn-outline" onClick={() => setIsAddCaregiverOpen(false)}>
                  Cancel
                </button>
                <button type="submit" className="btn btn-primary">
                  Save Caregiver
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};
