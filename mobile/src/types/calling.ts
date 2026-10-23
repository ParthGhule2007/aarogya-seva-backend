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
  callSessionId?: string;
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

export interface CallStatusDetailResponse {
  callSessionId: string;
  status: CallStatus;
  twilioCallSid?: string;
  twilioStatus?: string;
  errorCode?: number;
  errorMessage?: string;
  readableStatus: string;
  active: boolean;
}
