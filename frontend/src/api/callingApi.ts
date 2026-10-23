import { apiClient } from './client';
import {
  TalkToAiRequest,
  FollowUpCallRequest,
  CallResponse,
  FollowUpProfile,
  SaveConcernSummaryRequest,
  OptOutRequest,
} from '../types/calling';

export const callingApi = {
  /**
   * Button 1: Initiate outbound "Talk to AI" phone call
   */
  talkToAi: async (request: TalkToAiRequest): Promise<CallResponse> => {
    return apiClient.post<CallResponse>('/api/v1/calls/talk-to-ai', request);
  },

  /**
   * Button 2: Initiate patient-requested "Follow-up" phone call
   */
  followUpCall: async (request: FollowUpCallRequest): Promise<CallResponse> => {
    return apiClient.post<CallResponse>('/api/v1/calls/follow-up', request);
  },

  /**
   * Get live status of call session by session ID
   */
  getCallStatus: async (sessionId: string): Promise<CallResponse> => {
    return apiClient.get<CallResponse>(`/api/v1/calls/${sessionId}`);
  },

  /**
   * Get call history for a phone number
   */
  getCallHistory: async (phoneNumber: string): Promise<CallResponse[]> => {
    return apiClient.get<CallResponse[]>(`/api/v1/calls/history?phoneNumber=${encodeURIComponent(phoneNumber)}`);
  },

  /**
   * Reconcile call status with Twilio
   */
  syncCallStatus: async (sessionId: string): Promise<CallResponse> => {
    return apiClient.post<CallResponse>(`/api/v1/calls/${sessionId}/sync`);
  },

  /**
   * Terminate/cancel an ongoing call
   */
  terminateCall: async (sessionId: string): Promise<CallResponse> => {
    return apiClient.post<CallResponse>(`/api/v1/calls/${sessionId}/terminate`);
  },

  /**
   * Get follow-up profile and consent state
   */
  getProfile: async (phoneNumber: string): Promise<FollowUpProfile> => {
    return apiClient.get<FollowUpProfile>(`/api/v1/follow-up/profile?phoneNumber=${encodeURIComponent(phoneNumber)}`);
  },

  /**
   * Save health concern summary with consent
   */
  saveConcernSummary: async (request: SaveConcernSummaryRequest): Promise<FollowUpProfile> => {
    return apiClient.post<FollowUpProfile>('/api/v1/follow-up/concern-summary', request);
  },

  /**
   * Register patient opt-out
   */
  optOut: async (request: OptOutRequest): Promise<FollowUpProfile> => {
    return apiClient.post<FollowUpProfile>('/api/v1/follow-up/opt-out', request);
  },

  /**
   * Delete follow-up profile
   */
  deleteProfile: async (phoneNumber: string): Promise<void> => {
    return apiClient.delete<void>(`/api/v1/follow-up/profile?phoneNumber=${encodeURIComponent(phoneNumber)}`);
  },
};
