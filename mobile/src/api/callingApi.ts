import { request } from './client';
import { SERVICE_URLS } from '../config/apiConfig';
import {
  TalkToAiRequest,
  FollowUpCallRequest,
  CallResponse,
  CallStatusDetailResponse,
} from '../types/calling';

export const callingApi = {
  /**
   * Button 1: Initiate outbound "Talk to AI" phone call
   */
  talkToAi: async (req: TalkToAiRequest): Promise<CallResponse> => {
    const raw = await request<any>(
      SERVICE_URLS.CALLING,
      '/api/v1/calls/talk-to-ai',
      {
        method: 'POST',
        body: JSON.stringify(req),
      }
    );
    return {
      ...raw,
      sessionId: raw.sessionId || raw.callSessionId || '',
      callSessionId: raw.callSessionId || raw.sessionId || '',
    };
  },

  /**
   * Button 2: Initiate patient-requested "Follow-up" phone call
   */
  followUpCall: async (req: FollowUpCallRequest): Promise<CallResponse> => {
    const raw = await request<any>(
      SERVICE_URLS.CALLING,
      '/api/v1/calls/follow-up',
      {
        method: 'POST',
        body: JSON.stringify(req),
      }
    );
    return {
      ...raw,
      sessionId: raw.sessionId || raw.callSessionId || '',
      callSessionId: raw.callSessionId || raw.sessionId || '',
    };
  },

  /**
   * Get configured verified recipients
   */
  getVerifiedRecipients: async (): Promise<string[]> => {
    try {
      return await request<string[]>(
        SERVICE_URLS.CALLING,
        '/api/v1/calls/verified-recipients'
      );
    } catch {
      return [];
    }
  },

  /**
   * Get live status of call session by session ID
   */
  getCallStatus: async (sessionId: string): Promise<CallResponse> => {
    return request<CallResponse>(
      SERVICE_URLS.CALLING,
      `/api/v1/calls/${sessionId}`
    );
  },

  /**
   * Get detailed status with Twilio synchronization
   */
  getCallStatusDetail: async (sessionId: string): Promise<CallStatusDetailResponse> => {
    return request<CallStatusDetailResponse>(
      SERVICE_URLS.CALLING,
      `/api/v1/calls/${sessionId}/status`
    );
  },

  /**
   * Get call history for a phone number
   */
  getCallHistory: async (phoneNumber: string): Promise<CallResponse[]> => {
    return request<CallResponse[]>(
      SERVICE_URLS.CALLING,
      `/api/v1/calls/history?phoneNumber=${encodeURIComponent(phoneNumber)}`
    );
  },

  /**
   * Terminate/cancel an ongoing call
   */
  terminateCall: async (sessionId: string): Promise<CallResponse> => {
    return request<CallResponse>(
      SERVICE_URLS.CALLING,
      `/api/v1/calls/${sessionId}/terminate`,
      { method: 'POST' }
    );
  },
};
