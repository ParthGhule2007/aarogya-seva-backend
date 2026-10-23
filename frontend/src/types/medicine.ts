// ===== ENUMS (mirroring backend Java enums) =====

export type DosageUnit = 'TABLET' | 'ML' | 'MG' | 'G' | 'CAPSULE' | 'DROP' | 'PATCH' | 'SACHET' | 'UNIT';
export type FoodInstruction =
  | 'BEFORE_FOOD'
  | 'AFTER_FOOD'
  | 'WITH_FOOD'
  | 'EMPTY_STOMACH'
  | 'NONE'
  | 'BEFORE_MEAL'
  | 'AFTER_MEAL'
  | 'WITH_MEAL';
export type MedicationStatus = 'ACTIVE' | 'PAUSED' | 'COMPLETED' | 'CANCELLED';
export type DoseStatus = 'PENDING' | 'TAKEN' | 'MISSED' | 'SKIPPED';
export type NotificationChannel = 'WHATSAPP' | 'SMS' | 'CALL' | 'EMAIL';

// Computed on frontend from scheduledTime — does NOT come from backend
export type TimeSlot = 'MORNING' | 'AFTERNOON' | 'EVENING' | 'NIGHT';

// ===== BACKEND-MATCHING TYPES =====

/**
 * Mirrors MedicationResponse.java exactly.
 */
export interface Medication {
  id: string;                     // UUID
  patientId: string;              // UUID
  name: string;                   // medication name (backend field is "name")
  medicineName?: string;          // alias for UI compatibility
  extractedName?: string;
  imageKey?: string;
  imageUrl?: string;
  dosage?: string;                // alias for UI display
  dosageAmount: number;           // BigDecimal
  dosageUnit: DosageUnit;
  timesPerDay: number;
  reminderTimes: string[];        // LocalTime[] as "HH:mm:ss" strings
  timesOfDay?: TimeSlot[];        // UI helper
  startDate: string;              // "YYYY-MM-DD"
  endDate?: string;
  status: MedicationStatus;
  active?: boolean;               // alias for status === 'ACTIVE'
  foodInstruction: FoodInstruction;
  instructions?: string;
  frequency?: string;             // UI helper
  photoUrl?: string;              // alias for imageUrl
}

/**
 * Mirrors DoseEventResponse.java exactly.
 * timeSlot is COMPUTED from scheduledTime (not returned by backend).
 */
export interface ScheduledDose {
  id: string;                     // UUID — backend field is "id"
  eventId: string;                // alias of id for UI compatibility
  medicationId: string;           // UUID
  medicationName: string;         // backend field is "medicationName"
  medicineName?: string;          // alias for UI compatibility
  dosage?: string;                // alias for formatted dosage
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

/**
 * Payload for POST /api/v1/medications — mirrors CreateMedicationRequest.java exactly.
 */
export interface CreateMedicationPayload {
  patientId: string;
  name?: string;
  medicineName?: string;          // alias
  extractedName?: string;
  dosageAmount: number;           // required, > 0.01
  dosageUnit: DosageUnit;         // required
  timesPerDay: number;            // required, 1–24
  reminderTimes: string[];        // required, non-empty, e.g. ["08:00:00"]
  startDate: string;              // required, "YYYY-MM-DD"
  endDate?: string;
  foodInstruction?: FoodInstruction;
  instructions?: string;
}

/**
 * Mirrors CaregiverResponse.java exactly.
 */
export interface Caregiver {
  id: string;                     // UUID
  patientId: string;              // UUID
  name: string;                   // backend field is "name" (NOT caregiverName)
  caregiverName?: string;         // alias for UI compatibility
  relationship: string;
  phoneNumber: string;            // backend field is "phoneNumber" (NOT contactNumber)
  contactNumber?: string;         // alias for UI compatibility
  email?: string;
  preferredChannel: NotificationChannel;
  isActive: boolean;              // backend field is "isActive" (NOT active)
  active?: boolean;               // alias
  userId?: string;
  consentGiven?: boolean;         // user authorization for health alerts
  notifyOnMissedDose?: boolean;   // alias for UI
  createdAt?: string;
  updatedAt?: string;
}

/**
 * Payload for POST /api/v1/caregivers — mirrors CaregiverRequest.java exactly.
 */
export interface CreateCaregiverPayload {
  patientId: string;              // required
  name: string;                   // required, max 150
  caregiverName?: string;         // alias
  relationship: string;           // required, max 50
  phoneNumber?: string;           // max 30
  contactNumber?: string;         // alias
  email?: string;                 // max 150
  preferredChannel: NotificationChannel;  // required, default WHATSAPP
  userId?: string;
  consentGiven?: boolean;
}

export interface UpdateCaregiverPayload {
  name?: string;
  relationship?: string;
  phoneNumber?: string;
  email?: string;
  preferredChannel?: NotificationChannel;
  userId?: string;
  consentGiven?: boolean;
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

/**
 * Mirrors EscalationLogResponse.java.
 */
export interface CaregiverEscalation {
  id: string;
  patientId: string;
  medicationId?: string;
  medicationName?: string;
  consecutiveMissedCount?: number;
  triggeredAt?: string;
  caregiverNotified?: boolean;
  caregiverName?: string;
  recipient?: string;
  deliveryStatus?: 'PENDING' | 'SENT' | 'DELIVERED' | 'FAILED';
  messageContent?: string;
  providerReference?: string;
  deliveryAttemptedAt?: string;
  sentAt?: string;
  notifiedAt?: string;
  channel?: NotificationChannel;
  createdAt?: string;
}

export interface OcrExtractionResult {
  detectedName?: string;
  medicineName?: string;           // from OCR, maps to "name" in CreateMedicationPayload
  extractedName?: string;
  dosage?: string;
  frequency?: string;
  dosageAmount?: number;
  dosageUnit?: DosageUnit;
  timesPerDay?: number;
  reminderTimes?: string[];
  confidence?: number;
  confidenceScore?: number;
  rawText?: string;
  rawExtractedText?: string;
  isMock?: boolean;
  provider?: string;
  disclaimer?: string;
  candidateNames?: string[];
}

// ===== UTILITY FUNCTIONS =====

/**
 * Compute TimeSlot from scheduledTime like "08:00:00"
 */
export function computeTimeSlot(scheduledTime: any): TimeSlot {
  if (!scheduledTime) return 'MORNING';
  let hour = 8;
  if (Array.isArray(scheduledTime)) {
    hour = Number(scheduledTime[0]) || 8;
  } else if (typeof scheduledTime === 'string') {
    hour = parseInt(scheduledTime.split(':')[0], 10);
  } else if (typeof scheduledTime === 'number') {
    hour = scheduledTime;
  }
  if (isNaN(hour)) return 'MORNING';
  if (hour >= 5 && hour < 12) return 'MORNING';
  if (hour >= 12 && hour < 17) return 'AFTERNOON';
  if (hour >= 17 && hour < 21) return 'EVENING';
  return 'NIGHT';
}

/**
 * Format dosageAmount + dosageUnit for display.
 */
export function formatDosage(amount?: number, unit?: DosageUnit): string {
  if (!amount) return '1 dose';
  const label = unit ? unit.charAt(0) + unit.slice(1).toLowerCase() : '';
  return `${amount} ${label}`.trim();
}

/**
 * Convert timesPerDay integer to default reminder times.
 */
export function timesPerDayToReminderTimes(n: number): string[] {
  const defaults: Record<number, string[]> = {
    1: ['08:00:00'],
    2: ['08:00:00', '20:00:00'],
    3: ['08:00:00', '14:00:00', '20:00:00'],
    4: ['08:00:00', '12:00:00', '16:00:00', '20:00:00'],
  };
  return defaults[n] ?? ['08:00:00'];
}

/**
 * Convert TimeSlot[] (form UI) to reminderTimes string[].
 */
export function timeSlotsToReminderTimes(slots: TimeSlot[]): string[] {
  const map: Record<TimeSlot, string> = {
    MORNING: '08:00:00',
    AFTERNOON: '13:00:00',
    EVENING: '18:00:00',
    NIGHT: '21:00:00',
  };
  return slots.map((s) => map[s]);
}
