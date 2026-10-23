package com.ruralhealthcare.calling.service.twilio;

public interface TwilioClient {

    /**
     * Initiates an outbound telephone call through Twilio using a public TwiML URL or WebSocket join URL.
     *
     * @param toPhoneNumber destination phone number in E.164 format
     * @param twimlUrlOrJoinUrl public TwiML endpoint URL (http/https) or Ultravox WebSocket stream URL (wss://...)
     * @param statusCallbackUrl Webhook endpoint for call lifecycle status updates
     * @return unique Twilio Call SID
     */
    String initiateOutboundCall(String toPhoneNumber, String twimlUrlOrJoinUrl, String statusCallbackUrl);

    /**
     * Cancels or terminates an ongoing/ringing call.
     *
     * @param callSid Twilio Call SID
     */
    void cancelCall(String callSid);

    /**
     * Queries Twilio's authoritative REST API for the current lifecycle status of the given Call SID.
     *
     * @param callSid Twilio Call SID
     * @return status string from Twilio (e.g. queued, ringing, in-progress, completed, busy, failed, no-answer, canceled)
     */
    String fetchCallStatus(String callSid);

    /**
     * Queries Twilio's authoritative REST API for the status, error code, and error message.
     *
     * @param callSid Twilio Call SID
     * @return call details with status, errorCode, and errorMessage
     */
    TwilioCallDetails fetchCallDetails(String callSid);

    boolean isMock();
}
