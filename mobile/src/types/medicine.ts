// ===== ENUMS (mirroring backend Java enums) =====

export type DosageUnit = 'TABLET' | 'CAPSULE' | 'SPOON' | 'ML' | 'DROP' | 'OTHER' | 'MG' | 'G' | 'PATCH' | 'SACHET' | 'UNIT';
export type FoodInstruction = 'BEFORE_FOOD' | 'AFTER_FOOD' | 'WITH_FOOD' | 'BEFORE_MEAL' | 'AFTER_MEAL' | 'WITH_MEAL' | 'EMPTY_STOMACH' | 'NONE';
export type MedicationStatus = 'ACTIVE' | 'PAUSED' | 'COMPLETED' | 'DISCONTINUED' | 'DEACTIVATED' | 'CANCELLED';
export type DoseStatus = 'PENDING' | 'TAKEN' | 'MISSED' | 'SKIPPED';
export type NotificationChannel = 'WHATSAPP' | 'SMS' | 'CALL' | 'EMAIL';

export type TimeSlot = 'MORNING' | 'AFTERNOON' | 'EVENING' | 'NIGHT';

export interface Medication {
  id: string;                     // UUID
  patientId: string;              // UUID
  name: string;                   // medication name (backend field is "name")
  medicineName?: string;          // alias for UI compatibility
  extractedName?: string;
  imageKey?: string;
  imageUrl?: string;
  dosage?: string;
  dosageAmount: number;
  dosageUnit: DosageUnit;
  timesPerDay: number;
  reminderTimes: string[];        // e.g. ["08:00:00"]
  timesOfDay?: TimeSlot[];
  startDate: string;              // "YYYY-MM-DD"
  endDate?: string;
  status: MedicationStatus;
  active?: boolean;
  foodInstruction: FoodInstruction;
  instructions?: string;
  createdAt?: string;
  updatedAt?: string;
}

export interface ScheduledDose {
  id: string;                     // UUID — backend field is "id"
  eventId: string;                // alias of id for UI compatibility
  medicationId: string;           // UUID
  medicationName: string;
  medicineName?: string;
  dosage?: string;
  patientId?: string;
  dosageAmount?: number;
  dosageUnit?: DosageUnit;
  foodInstruction?: FoodInstruction;
  instructions?: string;
  scheduledDate?: string;         // "YYYY-MM-DD"
  scheduledTime: string;          // "HH:mm:ss"
  timeSlot: TimeSlot;             // COMPUTED from scheduledTime
  status: DoseStatus;
  takenAt?: string;
  missedAt?: string;
  createdAt?: string;
}

export interface CreateMedicationPayload {
  patientId: string;
  name?: string;
  medicineName?: string;
  dosageAmount: number;
  dosageUnit: DosageUnit;
  timesPerDay: number;
  reminderTimes: string[];
  startDate: string;
  endDate?: string;
  foodInstruction?: FoodInstruction;
  instructions?: string;
}

export interface UpdateMedicationPayload {
  name?: string;
  dosageAmount?: number;
  dosageUnit?: DosageUnit;
  timesPerDay?: number;
  reminderTimes?: string[];
  endDate?: string;
  foodInstruction?: FoodInstruction;
  instructions?: string;
  status?: MedicationStatus;
}

export interface OcrExtractionResponse {
  detectedName?: string;
  brand?: string;
  strength?: string;
  rawExtractedText?: string;
  confidence?: number;
  isMock: boolean;
  provider?: string;
  disclaimer?: string;
  candidateNames: string[];
}

export interface Caregiver {
  id: string;
  patientId: string;
  name: string;
  caregiverName?: string;
  relationship: string;
  phoneNumber: string;
  contactNumber?: string;
  email?: string;
  preferredChannel: NotificationChannel;
  isActive: boolean;
  active?: boolean;
  notifyOnMissedDose?: boolean;
  createdAt?: string;
  updatedAt?: string;
}

export interface CreateCaregiverPayload {
  patientId: string;
  name: string;
  caregiverName?: string;
  relationship: string;
  phoneNumber?: string;
  contactNumber?: string;
  email?: string;
  preferredChannel: NotificationChannel;
}

export interface CaregiverEscalation {
  id: string;
  patientId: string;
  medicationId?: string;
  medicationName?: string;
  streakStartDoseId?: string;
  triggeringDoseId?: string;
  consecutiveMissedCount?: number;
  channel?: NotificationChannel;
  recipient?: string;
  deliveryStatus?: string;
  messageContent?: string;
  providerReference?: string;
  deliveryAttemptedAt?: string;
  createdAt?: string;
  triggeredAt?: string;
  caregiverNotified?: boolean;
  notifiedAt?: string;
}

export function computeTimeSlot(scheduledTime: string): TimeSlot {
  if (!scheduledTime) return 'MORNING';
  const hour = parseInt(scheduledTime.split(':')[0], 10);
  if (hour >= 6 && hour < 12) return 'MORNING';
  if (hour >= 12 && hour < 18) return 'AFTERNOON';
  if (hour >= 18 && hour < 22) return 'EVENING';
  return 'NIGHT';
}

export function formatDosage(amount?: number, unit?: DosageUnit): string {
  if (!amount) return '1 dose';
  const label = unit ? unit.charAt(0) + unit.slice(1).toLowerCase() : '';
  return `${amount} ${label}`.trim();
}
