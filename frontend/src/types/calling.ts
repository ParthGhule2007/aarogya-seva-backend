export type CallStatus =
  | 'INITIATED'
  | 'RINGING'
  | 'IN_PROGRESS'
  | 'COMPLETED'
  | 'BUSY'
  | 'NO_ANSWER'
  | 'CANCELLED'
  | 'FAILED';

export type CallType = 'TALK_TO_AI' | 'FOLLOW_UP';

export type VoiceLanguage = 'en' | 'hi' | 'mr';

export interface TalkToAiRequest {
  phoneNumber: string;
  language?: VoiceLanguage | string;
  chosenName?: string;
  consentToSaveSummary?: boolean;
}

export interface FollowUpCallRequest {
  phoneNumber: string;
  language?: VoiceLanguage | string;
}

export interface CallResponse {
  sessionId: string;
  phoneNumber: string;
  callType: CallType;
  language: string;
  status: CallStatus;
  twilioCallSid?: string;
  durationSeconds?: number;
  errorMessage?: string;
  message?: string;
  startedAt?: string;
  endedAt?: string;
  createdAt?: string;
}

export interface FollowUpProfile {
  id: string;
  phoneNumber: string;
  chosenName?: string;
  preferredLanguage: string;
  concernSummary?: string;
  consentGiven: boolean;
  optedOut: boolean;
  firstCallCompleted: boolean;
  createdAt?: string;
  updatedAt?: string;
  optedOutAt?: string;
}

export interface SaveConcernSummaryRequest {
  phoneNumber: string;
  concernSummary: string;
  consentGiven: boolean;
}

export interface OptOutRequest {
  phoneNumber: string;
  reason?: string;
}
