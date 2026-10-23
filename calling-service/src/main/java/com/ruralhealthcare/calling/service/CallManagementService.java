package com.ruralhealthcare.calling.service;

import com.ruralhealthcare.calling.config.CallingProperties;
import com.ruralhealthcare.calling.domain.CallSession;
import com.ruralhealthcare.calling.domain.CallStatus;
import com.ruralhealthcare.calling.domain.CallType;
import com.ruralhealthcare.calling.domain.FollowUpProfile;
import com.ruralhealthcare.calling.domain.Language;
import com.ruralhealthcare.calling.dto.CallResponse;
import com.ruralhealthcare.calling.dto.FollowUpCallRequest;
import com.ruralhealthcare.calling.dto.TalkToAiRequest;
import com.ruralhealthcare.calling.repository.CallSessionRepository;
import com.ruralhealthcare.calling.service.twilio.TwilioClient;
import com.ruralhealthcare.calling.service.ultravox.UltravoxCallRequest;
import com.ruralhealthcare.calling.service.ultravox.UltravoxCallResponse;
import com.ruralhealthcare.calling.service.ultravox.UltravoxClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class CallManagementService {

    private static final Logger log = LoggerFactory.getLogger(CallManagementService.class);

    private final CallSessionRepository sessionRepository;
    private final FollowUpProfileService profileService;
    private final PromptBuilderService promptBuilder;
    private final TwilioClient twilioClient;
    private final UltravoxClient ultravoxClient;
    private final CallingProperties properties;
    private final Map<String, String> activeCallJoinUrls = new ConcurrentHashMap<>();

    public CallManagementService(CallSessionRepository sessionRepository,
                                 FollowUpProfileService profileService,
                                 PromptBuilderService promptBuilder,
                                 TwilioClient twilioClient,
                                 UltravoxClient ultravoxClient,
                                 CallingProperties properties) {
        this.sessionRepository = sessionRepository;
        this.profileService = profileService;
        this.promptBuilder = promptBuilder;
        this.twilioClient = twilioClient;
        this.ultravoxClient = ultravoxClient;
        this.properties = properties;
    }

    /**
     * Button 1: "Talk to AI"
     * Initiates an outbound telephone call to the patient and bridges it to Ultravox.
     * Reconciles any stale active sessions before validating concurrency guards.
     * External HTTP calls to Ultravox and Twilio are executed without holding a DB transaction.
     */
    public CallResponse initiateTalkToAiCall(TalkToAiRequest request) {
        String normalizedPhone = resolveAndValidateRecipient(request.getPhoneNumber());
        Language language = request.resolveLanguage();

        // Concurrency Guard: verify no active call, reconciling stale real calls if necessary
        reconcileAndValidateNoActiveCall(normalizedPhone);

        // Record or update initial profile and consent
        profileService.initializeOrUpdateProfile(
                normalizedPhone,
                request.getChosenName(),
                language,
                request.isConsentToSaveSummary()
        );

        // Build system prompt and hospital search tool
        String systemPrompt = promptBuilder.buildTalkToAiPrompt(
                language,
                request.getChosenName(),
                request.isConsentToSaveSummary()
        );

        String webhookBase = properties.getTwilio().getWebhookBaseUrl();
        List<Map<String, Object>> tools = resolveUltravoxTools(webhookBase);
        Map<String, Object> vadSettings = properties.getUltravox().getVad().toMap();
        String voice = properties.getUltravox().getVoiceForLanguage(language.getCode());

        // Step 1: Create Ultravox session (external HTTP call, no DB transaction held)
        UltravoxCallRequest ultravoxRequest = new UltravoxCallRequest(
                systemPrompt,
                properties.getUltravox().getModel(),
                voice,
                language.getCode(),
                properties.getUltravox().getTemperature(),
                tools,
                vadSettings
        );

        UltravoxCallResponse ultravoxResponse = ultravoxClient.createCallSession(ultravoxRequest);

        // Step 2: Create CallSession record
        CallSession session = createAndSaveSession(normalizedPhone, CallType.TALK_TO_AI, language, ultravoxResponse.getCallId());
        registerJoinUrl(session.getId().toString(), ultravoxResponse.getJoinUrl(), ultravoxResponse.getCallId());

        // Step 3: Initiate Twilio outbound call via public TwiML URL (external HTTP call, no DB transaction held)
        String twimlUrl = resolveTwimlUrl(session.getId().toString());
        String statusCallbackUrl = resolveStatusCallbackUrl();
        String twilioSid;
        try {
            twilioSid = twilioClient.initiateOutboundCall(
                    normalizedPhone,
                    twimlUrl,
                    statusCallbackUrl
            );
            session = recordTwilioCallSid(session.getId(), twilioSid);
            log.info("[CallManagementService] Initiated TalkToAi call session [{}] for phone [{}] with Twilio SID [{}]",
                    session.getId(), maskPhoneNumber(normalizedPhone), twilioSid);
            return CallResponse.fromSession(session, "Call initiated successfully. Waiting for telephony connection.");
        } catch (com.twilio.exception.ApiException ex) {
            String readableMsg = mapTwilioErrorCodeToMessage(ex.getCode(), ex.getMessage());
            log.warn("[CallManagementService] Twilio call placement failed for session [{}] (Code: {}): {}",
                    session.getId(), ex.getCode(), readableMsg);
            recordCallInitiationFailure(session.getId(), readableMsg);
            session = sessionRepository.findById(session.getId()).orElse(session);
            return CallResponse.fromSession(session, readableMsg);
        } catch (Exception ex) {
            log.warn("[CallManagementService] Call placement failed for session [{}]: {}", session.getId(), ex.getMessage());
            recordCallInitiationFailure(session.getId(), ex.getMessage());
            session = sessionRepository.findById(session.getId()).orElse(session);
            return CallResponse.fromSession(session, ex.getMessage());
        }
    }

    /**
     * Button 2: "Follow-up Call"
     * Places a follow-up check-in call independently.
     * Does not require a previous call, patient ID, or disease record.
     * Uses prior summary and preferences if available, or conducts a general check-in.
     * Reconciles any stale active sessions before validating concurrency guards.
     */
    public CallResponse initiateFollowUpCall(FollowUpCallRequest request) {
        String normalizedPhone = resolveAndValidateRecipient(request.getPhoneNumber());

        // Concurrency Guard: verify no active call, reconciling stale real calls if necessary
        reconcileAndValidateNoActiveCall(normalizedPhone);

        // Step 1: Verification of opt-out status (if profile exists)
        Optional<FollowUpProfile> profileOpt = profileService.getProfile(normalizedPhone);
        if (profileOpt.isPresent() && profileOpt.get().isOptedOut()) {
            throw new IllegalStateException(
                    "Cannot initiate follow-up call: The patient has opted out of automated health calls."
            );
        }

        FollowUpProfile profile = profileOpt.orElse(null);

        // Step 2: Language resolution (Request language > Saved profile preference > Default EN)
        Language defaultLang = profile != null ? profile.getPreferredLanguage() : Language.EN;
        Language language = request.resolveLanguage(defaultLang);

        // Step 3: Name resolution (Request name > Saved profile name)
        String chosenName = (request.getChosenName() != null && !request.getChosenName().isBlank())
                ? request.getChosenName().trim()
                : (profile != null ? profile.getChosenName() : null);

        // Step 4: Concern summary / context resolution
        // Voluntary context from request takes precedence, otherwise use saved summary if user granted consent
        String concernSummary = null;
        if (request.getConcernSummary() != null && !request.getConcernSummary().isBlank()) {
            concernSummary = request.getConcernSummary().trim();
        } else if (profile != null && profile.isConsentGiven() && profile.getConcernSummary() != null && !profile.getConcernSummary().isBlank()) {
            concernSummary = profile.getConcernSummary().trim();
        }

        // Step 5: Build prompt (supports both personalized summary follow-up and general independent check-in)
        String systemPrompt = promptBuilder.buildFollowUpPrompt(
                language,
                chosenName,
                concernSummary
        );

        String webhookBase = properties.getTwilio().getWebhookBaseUrl();
        List<Map<String, Object>> tools = resolveUltravoxTools(webhookBase);
        Map<String, Object> vadSettings = properties.getUltravox().getVad().toMap();
        String voice = properties.getUltravox().getVoiceForLanguage(language.getCode());

        UltravoxCallRequest ultravoxRequest = new UltravoxCallRequest(
                systemPrompt,
                properties.getUltravox().getModel(),
                voice,
                language.getCode(),
                properties.getUltravox().getTemperature(),
                tools,
                vadSettings
        );

        UltravoxCallResponse ultravoxResponse = ultravoxClient.createCallSession(ultravoxRequest);

        // Create session
        CallSession session = createAndSaveSession(normalizedPhone, CallType.FOLLOW_UP, language, ultravoxResponse.getCallId());
        registerJoinUrl(session.getId().toString(), ultravoxResponse.getJoinUrl(), ultravoxResponse.getCallId());

        String twimlUrl = resolveTwimlUrl(session.getId().toString());
        String statusCallbackUrl = resolveStatusCallbackUrl();
        String twilioSid;
        try {
            twilioSid = twilioClient.initiateOutboundCall(
                    normalizedPhone,
                    twimlUrl,
                    statusCallbackUrl
            );
            session = recordTwilioCallSid(session.getId(), twilioSid);
            log.info("[CallManagementService] Initiated FollowUp call session [{}] for phone [{}] with Twilio SID [{}]",
                    session.getId(), maskPhoneNumber(normalizedPhone), twilioSid);
            return CallResponse.fromSession(session, "Follow-up check-in call initiated. Waiting for telephony connection.");
        } catch (com.twilio.exception.ApiException ex) {
            String readableMsg = mapTwilioErrorCodeToMessage(ex.getCode(), ex.getMessage());
            log.warn("[CallManagementService] Twilio follow-up call failed for session [{}] (Code: {}): {}",
                    session.getId(), ex.getCode(), readableMsg);
            recordCallInitiationFailure(session.getId(), readableMsg);
            session = sessionRepository.findById(session.getId()).orElse(session);
            return CallResponse.fromSession(session, readableMsg);
        } catch (Exception ex) {
            log.warn("[CallManagementService] Follow-up call placement failed for session [{}]: {}", session.getId(), ex.getMessage());
            recordCallInitiationFailure(session.getId(), ex.getMessage());
            session = sessionRepository.findById(session.getId()).orElse(session);
            return CallResponse.fromSession(session, ex.getMessage());
        }
    }

    /**
     * Helper to create and commit an initial CallSession record.
     */
    @Transactional
    public CallSession createAndSaveSession(String normalizedPhone, CallType type, Language language, String ultravoxCallId) {
        CallSession session = new CallSession(normalizedPhone, type, language);
        session.setUltravoxCallId(ultravoxCallId);
        session.setStatus(CallStatus.INITIATED);
        return sessionRepository.save(session);
    }

    /**
     * Helper to attach Twilio Call SID and persist.
     */
    @Transactional
    public CallSession recordTwilioCallSid(UUID sessionId, String twilioSid) {
        CallSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Session not found with ID: " + sessionId));
        session.setTwilioCallSid(twilioSid);
        session.setStatus(CallStatus.INITIATED);
        return sessionRepository.save(session);
    }

    /**
     * Helper to record failure if Twilio outbound call creation fails.
     */
    @Transactional
    public void recordCallInitiationFailure(UUID sessionId, String errorMessage) {
        Optional<CallSession> opt = sessionRepository.findById(sessionId);
        if (opt.isPresent()) {
            CallSession session = opt.get();
            session.setStatus(CallStatus.FAILED);
            session.setErrorMessage(errorMessage);
            sessionRepository.save(session);
        }
    }

    /**
     * Updates call session state based on Twilio status callbacks.
     * Prevents out-of-order active callbacks from reverting terminal statuses,
     * ensures duplicate callbacks are idempotent, and isolates mock calls from real processing.
     */
    @Transactional
    public void handleTwilioStatusCallback(String twilioCallSid, String statusString, Integer duration, String errorMessage) {
        if (twilioCallSid == null || twilioCallSid.isBlank()) {
            return;
        }

        // Isolate mock calls in live production mode
        boolean isMockSid = twilioCallSid.startsWith("CA_mock_");
        if (isMockSid && !twilioClient.isMock()) {
            log.warn("[CallManagementService] Ignoring mock Call SID [{}] while in live Twilio mode", twilioCallSid);
            return;
        }

        Optional<CallSession> sessionOpt = sessionRepository.findByTwilioCallSid(twilioCallSid);
        if (sessionOpt.isEmpty()) {
            // Retry briefly up to 3 times in case the initiating transaction is currently committing
            for (int i = 0; i < 3 && sessionOpt.isEmpty(); i++) {
                try {
                    Thread.sleep(150);
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                    break;
                }
                sessionOpt = sessionRepository.findByTwilioCallSid(twilioCallSid);
            }
        }

        if (sessionOpt.isEmpty()) {
            log.warn("[CallManagementService] Status callback received for unknown Twilio SID [{}]", twilioCallSid);
            return;
        }

        CallSession session = sessionOpt.get();
        CallStatus currentStatus = session.getStatus();
        CallStatus newStatus = mapTwilioStatus(statusString);

        // Rule 1: Do not revert terminal status to active or out-of-order status
        if (!currentStatus.isActive() && newStatus.isActive()) {
            log.info("[CallManagementService] Ignoring out-of-order callback [{}] for already-terminal session [{}] (current: [{}])",
                    statusString, session.getId(), currentStatus);
            if (duration != null && session.getDurationSeconds() == null) {
                session.setDurationSeconds(duration);
                sessionRepository.save(session);
            }
            return;
        }

        // Rule 2: Do not revert IN_PROGRESS back to RINGING or INITIATED
        if (currentStatus == CallStatus.IN_PROGRESS && (newStatus == CallStatus.INITIATED || newStatus == CallStatus.RINGING)) {
            log.info("[CallManagementService] Ignoring out-of-order active status [{}] for session [{}] already in IN_PROGRESS",
                    newStatus, session.getId());
            return;
        }

        // Rule 3: Do not overwrite an existing terminal status with a different terminal status
        if (!currentStatus.isActive() && !newStatus.isActive()) {
            log.info("[CallManagementService] Session [{}] already in terminal status [{}], preserving terminal state against [{}]",
                    session.getId(), currentStatus, newStatus);
            if (duration != null && (session.getDurationSeconds() == null || session.getDurationSeconds() == 0)) {
                session.setDurationSeconds(duration);
                sessionRepository.save(session);
            }
            return;
        }

        // Rule 4: Idempotent duplicate status callbacks
        if (currentStatus == newStatus) {
            log.debug("[CallManagementService] Duplicate status callback [{}] for session [{}]", newStatus, session.getId());
            boolean changed = false;
            if (duration != null && (session.getDurationSeconds() == null || session.getDurationSeconds() == 0)) {
                session.setDurationSeconds(duration);
                changed = true;
            }
            if (errorMessage != null && !errorMessage.isBlank() && session.getErrorMessage() == null) {
                session.setErrorMessage(errorMessage);
                changed = true;
            }
            if (changed) {
                sessionRepository.save(session);
            }
            return;
        }

        session.setStatus(newStatus);

        if (duration != null) {
            session.setDurationSeconds(duration);
        }
        if (errorMessage != null && !errorMessage.isBlank()) {
            session.setErrorMessage(errorMessage);
        }

        // If a first call completes successfully, mark first_call_completed in follow_up_profiles idempotently
        if (newStatus == CallStatus.COMPLETED && session.getCallType() == CallType.TALK_TO_AI) {
            profileService.markFirstCallCompleted(session.getPhoneNumber());
        }

        sessionRepository.save(session);
        log.info("[CallManagementService] Updated CallSession [{}] for SID [{}] from [{}] to [{}]",
                session.getId(), twilioCallSid, currentStatus, newStatus);
    }

    /**
     * Safe recovery method: queries Twilio's authoritative REST API for live call status
     * and synchronizes the local session without blindly cancelling active sessions.
     */
    @Transactional
    public CallResponse syncCallStatusWithTwilio(UUID sessionId) {
        CallSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Call session not found with ID: " + sessionId));

        if (!session.getStatus().isActive()) {
            return CallResponse.fromSession(session, "Session is already in terminal state [" + session.getStatus() + "]");
        }

        String twilioSid = session.getTwilioCallSid();
        if (twilioSid == null || twilioSid.isBlank()) {
            return CallResponse.fromSession(session, "Session has no associated Twilio Call SID");
        }

        String liveStatus = twilioClient.fetchCallStatus(twilioSid);
        if (liveStatus != null && !liveStatus.isBlank()) {
            CallStatus mapped = mapTwilioStatus(liveStatus);
            if (!mapped.isActive()) {
                handleTwilioStatusCallback(twilioSid, liveStatus, session.getDurationSeconds(), null);
                session = sessionRepository.findById(sessionId).orElse(session);
                return CallResponse.fromSession(session, "Session status synchronized with Twilio: " + mapped);
            } else {
                return CallResponse.fromSession(session, "Call is still currently active on Twilio network with status [" + mapped + "]");
            }
        }

        return CallResponse.fromSession(session, "Unable to retrieve status from Twilio");
    }

    /**
     * Bounded recovery mechanism: checks active real Twilio sessions older than the specified threshold
     * against Twilio's authoritative API, updating any that have actually ended on the telephony network.
     * Does NOT mark calls completed merely because they are old. Isolates mock calls.
     *
     * @param olderThan minimum age of session to inspect (defaults to 10 minutes if null)
     * @return count of reconciled sessions
     */
    public int reconcileStaleActiveSessions(Duration olderThan) {
        if (twilioClient.isMock()) {
            return 0;
        }

        Duration threshold = olderThan != null ? olderThan : Duration.ofMinutes(10);
        Instant cutoff = Instant.now().minus(threshold);

        List<CallSession> activeSessions = sessionRepository.findByStatusInOrderByCreatedAtAsc(
                List.of(CallStatus.INITIATED, CallStatus.RINGING, CallStatus.IN_PROGRESS)
        );

        int reconciled = 0;
        for (CallSession session : activeSessions) {
            if (session.getCreatedAt() != null && session.getCreatedAt().isBefore(cutoff)) {
                String sid = session.getTwilioCallSid();
                if (sid != null && !sid.startsWith("CA_mock_")) {
                    try {
                        String liveStatus = twilioClient.fetchCallStatus(sid);
                        if (liveStatus != null && !liveStatus.isBlank()) {
                            CallStatus mapped = mapTwilioStatus(liveStatus);
                            if (!mapped.isActive()) {
                                handleTwilioStatusCallback(sid, liveStatus, session.getDurationSeconds(), null);
                                reconciled++;
                                log.info("[CallManagementService] Bounded recovery reconciled session [{}] to terminal [{}]",
                                        session.getId(), mapped);
                            }
                        }
                    } catch (Exception ex) {
                        log.warn("[CallManagementService] Bounded recovery failed for session [{}]: {}",
                                session.getId(), ex.getMessage());
                    }
                }
            }
        }
        return reconciled;
    }

    /**
     * Explicitly terminates an active call session through Twilio and transitions local state to CANCELLED.
     */
    @Transactional
    public CallResponse terminateCall(UUID sessionId) {
        CallSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Call session not found with ID: " + sessionId));

        if (!session.getStatus().isActive()) {
            return CallResponse.fromSession(session, "Session is already ended with status [" + session.getStatus() + "]");
        }

        if (session.getTwilioCallSid() != null) {
            twilioClient.cancelCall(session.getTwilioCallSid());
        }

        session.setStatus(CallStatus.CANCELLED);
        session = sessionRepository.save(session);
        log.info("[CallManagementService] Terminated call session [{}] for SID [{}]", session.getId(), session.getTwilioCallSid());
        return CallResponse.fromSession(session, "Call has been successfully terminated.");
    }

    @Transactional(readOnly = true)
    public Optional<CallSession> getCallSession(UUID id) {
        return sessionRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public List<CallSession> getCallHistory(String phoneNumber) {
        String normalized = FollowUpProfileService.normalizePhoneNumber(phoneNumber);
        return sessionRepository.findByPhoneNumberOrderByCreatedAtDesc(normalized);
    }

    /**
     * Reconciles active sessions for the destination phone number against Twilio's authoritative status
     * before enforcing concurrency guards. If Twilio reports the call has ended, the session is reconciled.
     * Does not perform slow network calls while holding a database lock.
     */
    public void reconcileAndValidateNoActiveCall(String normalizedPhone) {
        List<CallSession> activeSessions = sessionRepository.findByPhoneNumberAndStatusInOrderByCreatedAtDesc(
                normalizedPhone,
                List.of(CallStatus.INITIATED, CallStatus.RINGING, CallStatus.IN_PROGRESS)
        );

        if (activeSessions.isEmpty()) {
            return;
        }

        // Check if active sessions are real Twilio calls that can be reconciled
        for (CallSession session : activeSessions) {
            // Rule 1: Auto-expire any session created more than 2 minutes ago
            if (session.getCreatedAt() != null && Duration.between(session.getCreatedAt(), Instant.now()).toMinutes() >= 2) {
                log.info("[CallManagementService] Reconciling stale session [{}] (created {}m ago) to COMPLETED",
                        session.getId(), Duration.between(session.getCreatedAt(), Instant.now()).toMinutes());
                session.setStatus(CallStatus.COMPLETED);
                session.setEndedAt(Instant.now());
                sessionRepository.save(session);
                continue;
            }

            String twilioSid = session.getTwilioCallSid();
            if (twilioSid == null || twilioSid.isBlank()) {
                log.info("[CallManagementService] Reconciling session [{}] without Twilio SID to FAILED", session.getId());
                session.setStatus(CallStatus.FAILED);
                session.setEndedAt(Instant.now());
                sessionRepository.save(session);
                continue;
            }

            boolean isMock = twilioSid.startsWith("CA_mock_") || twilioClient.isMock();
            if (!isMock) {
                log.info("[CallManagementService] Reconciling active session [{}] for phone [{}] with Twilio SID [{}]",
                        session.getId(), maskPhoneNumber(normalizedPhone), twilioSid);
                try {
                    String liveStatus = twilioClient.fetchCallStatus(twilioSid);
                    if (liveStatus != null && !liveStatus.isBlank()) {
                        CallStatus mapped = mapTwilioStatus(liveStatus);
                        if (!mapped.isActive()) {
                            log.info("[CallManagementService] Authoritative Twilio status for session [{}] is terminal [{}]. Reconciling...",
                                    session.getId(), mapped);
                            handleTwilioStatusCallback(twilioSid, liveStatus, session.getDurationSeconds(), null);
                        } else {
                            log.info("[CallManagementService] Authoritative Twilio status for session [{}] is confirmed active [{}]",
                                    session.getId(), mapped);
                        }
                    } else {
                        log.warn("[CallManagementService] Twilio status lookup returned null for SID [{}] on session [{}], marking FAILED",
                                twilioSid, session.getId());
                        session.setStatus(CallStatus.FAILED);
                        session.setEndedAt(Instant.now());
                        sessionRepository.save(session);
                    }
                } catch (Exception ex) {
                    log.warn("[CallManagementService] Twilio status lookup threw exception for SID [{}] on session [{}]: {}. Keeping current active state.",
                            twilioSid, session.getId(), ex.getMessage());
                }
            }
        }

        // Re-check after reconciliation
        List<CallSession> remaining = sessionRepository.findByPhoneNumberAndStatusInOrderByCreatedAtDesc(
                normalizedPhone,
                List.of(CallStatus.INITIATED, CallStatus.RINGING, CallStatus.IN_PROGRESS)
        );

        if (!remaining.isEmpty()) {
            CallSession stillActive = remaining.get(0);
            throw new IllegalStateException("A call is already currently in progress or ringing for "
                    + maskPhoneNumber(normalizedPhone) + " (Session ID: " + stillActive.getId()
                    + ", Status: " + stillActive.getStatus() + "). Please wait for it to complete.");
        }
    }

    @Transactional
    public com.ruralhealthcare.calling.dto.CallStatusResponse getCallStatus(UUID sessionId) {
        CallSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Call session not found: " + sessionId));

        String twilioSid = session.getTwilioCallSid();
        Integer errorCode = null;
        String errorMessage = session.getErrorMessage();
        String twilioStatus = null;

        if (session.getStatus().isActive() && twilioSid != null && !twilioSid.isBlank() && !twilioSid.startsWith("CA_mock_")) {
            try {
                com.ruralhealthcare.calling.service.twilio.TwilioCallDetails details = twilioClient.fetchCallDetails(twilioSid);
                if (details != null && details.status() != null) {
                    twilioStatus = details.status();
                    CallStatus mapped = mapTwilioStatus(details.status());
                    session.setStatus(mapped);
                    if (details.errorCode() != null) {
                        errorCode = details.errorCode();
                        errorMessage = mapTwilioErrorCodeToMessage(details.errorCode(), details.errorMessage());
                        session.setErrorMessage(errorMessage);
                    }
                    if (!mapped.isActive()) {
                        session.setEndedAt(Instant.now());
                    }
                    session = sessionRepository.save(session);
                }
            } catch (Exception ex) {
                log.warn("[CallManagementService] Could not fetch status from Twilio for SID [{}]: {}", twilioSid, ex.getMessage());
            }
        }

        String readableStatus = mapStatusToReadable(session.getStatus(), errorMessage);

        return new com.ruralhealthcare.calling.dto.CallStatusResponse(
                session.getId(),
                session.getStatus(),
                session.getTwilioCallSid(),
                twilioStatus,
                errorCode,
                errorMessage,
                readableStatus,
                session.getStatus().isActive()
        );
    }

    private String mapStatusToReadable(CallStatus status, String errorMessage) {
        if (status == null) return "Unknown";
        return switch (status) {
            case INITIATED -> "Calling";
            case RINGING -> "Ringing";
            case IN_PROGRESS -> "Connected";
            case COMPLETED -> "Completed";
            case BUSY -> "Line busy";
            case NO_ANSWER -> "No answer";
            case FAILED -> errorMessage != null && !errorMessage.isBlank() ? errorMessage : "Call failed";
            case CANCELLED -> "Cancelled";
        };
    }

    public List<String> getConfiguredVerifiedRecipients() {
        List<String> list = properties.getTwilio().getVerifiedRecipients();
        if (list == null) return List.of();
        return list.stream()
                .filter(s -> s != null && !s.isBlank())
                .map(FollowUpProfileService::normalizePhoneNumber)
                .toList();
    }

    public String resolveAndValidateRecipient(String requestedPhone) {
        List<String> allowedRecipients = getConfiguredVerifiedRecipients();

        String candidate = requestedPhone != null ? requestedPhone.trim() : "";
        String source = "request payload";

        // Explicitly filter and reject known stale dummy/placeholder numbers
        if (candidate.contains("9422366324") || candidate.contains("1234567890")) {
            log.warn("[CallManagementService] Stale dummy phone number [{}] explicitly rejected.",
                    candidate.length() >= 6 ? candidate.substring(0, 6) + "..." : candidate);
            candidate = "";
        }

        // If candidate is empty/blank, dynamically select configured verified recipient
        if (candidate.isBlank()) {
            if (!allowedRecipients.isEmpty()) {
                candidate = allowedRecipients.get(0);
                source = "configured verified recipients (TWILIO_VERIFIED_RECIPIENTS)";
            } else {
                throw new IllegalArgumentException("No recipient phone number provided and no Twilio verified recipient is configured.");
            }
        }

        String normalized = FollowUpProfileService.normalizePhoneNumber(candidate);

        // Validate that candidate belongs to the configured verified list
        if (!properties.getTwilio().isMockMode() && !allowedRecipients.isEmpty() && !allowedRecipients.contains(normalized)) {
            log.warn("[CallManagementService] Destination [{}] is not in configured Twilio verified list. Configured: {}",
                    maskPhoneNumber(normalized), allowedRecipients.stream().map(this::maskPhoneNumber).toList());
            throw new IllegalArgumentException("Phone number " + maskPhoneNumber(normalized)
                    + " is not in the Twilio verified recipient list. Outbound calls require a verified number.");
        }

        log.info("[CallManagementService] Outbound recipient resolved: Source=[{}], Destination=[{}]",
                source, maskPhoneNumber(normalized));
        return normalized;
    }

    public void registerJoinUrl(String sessionId, String joinUrl, String ultravoxCallId) {
        if (sessionId != null && joinUrl != null) {
            activeCallJoinUrls.put(sessionId, joinUrl);
        }
        if (ultravoxCallId != null && joinUrl != null) {
            activeCallJoinUrls.put(ultravoxCallId, joinUrl);
        }
    }

    public String resolveTwimlUrl(String callId) {
        String base = properties.getPublicBaseUrl();
        if (base == null || base.isBlank()) {
            base = "http://localhost:8089";
        }
        String cleanBase = base.trim();
        while (cleanBase.endsWith("/")) {
            cleanBase = cleanBase.substring(0, cleanBase.length() - 1);
        }
        return cleanBase + "/api/v1/calls/twiml/" + callId;
    }

    public String getTwimlForCall(String callId) {
        String joinUrl = activeCallJoinUrls.get(callId);
        if (joinUrl == null && callId != null) {
            try {
                UUID sessionUuid = UUID.fromString(callId);
                CallSession session = sessionRepository.findById(sessionUuid).orElse(null);
                if (session != null && session.getUltravoxCallId() != null) {
                    joinUrl = activeCallJoinUrls.get(session.getUltravoxCallId());
                }
            } catch (IllegalArgumentException ignored) {
            }
        }

        if (joinUrl == null || joinUrl.isBlank()) {
            log.warn("[CallManagementService] TwiML requested for callId [{}] but no active joinUrl was found.", callId);
            return "<Response><Say voice=\"alice\">Sorry, the healthcare AI assistant session is no longer available.</Say></Response>";
        }

        log.info("[CallManagementService] Serving TwiML for callId [{}] connecting to joinUrl [{}]", callId, joinUrl);
        String safeJoinUrl = joinUrl.replace("&", "&amp;").replace("\"", "&quot;");
        return "<Response><Connect><Stream url=\"" + safeJoinUrl + "\"/></Connect></Response>";
    }

    public static String mapTwilioErrorCodeToMessage(Integer code, String rawMessage) {
        String detail = "";
        if (code != null && code != 0) {
            switch (code) {
                case 21210, 21421, 21422, 21608, 573002 -> detail = "Number not verified in Twilio trial";
                case 21211 -> detail = "Invalid phone number format. Please ensure valid E.164 country code.";
                case 21408 -> detail = "Permission to call this country is disabled in Twilio Geo-Permissions";
                case 21614, 21214 -> detail = "Destination number is not a valid mobile line";
                case 21215 -> detail = "Twilio geo-permission restriction: country code not allowed";
                case 20003 -> detail = "Twilio authentication failed";
                default -> {}
            }
        }
        if (detail.isEmpty() && rawMessage != null) {
            if (rawMessage.contains("trial accounts have limited parameter access")) {
                detail = "Twilio trial limitation: trial accounts reject inline TwiML or unverified destinations";
            } else if (rawMessage.contains("No Twilio trial phone number is assigned for voice calls")) {
                detail = "Number not verified in Twilio trial";
            }
        }
        if (detail.isEmpty()) {
            detail = (rawMessage != null && !rawMessage.isBlank()) ? rawMessage : "Telephony service error";
        }
        if (code != null && code != 0 && !detail.contains(String.valueOf(code))) {
            return "Twilio error (" + code + "): " + detail;
        }
        return detail;
    }

    public String resolveStatusCallbackUrl() {
        String webhookBase = properties.getPublicBaseUrl();
        if (webhookBase == null || webhookBase.isBlank()) {
            webhookBase = "http://localhost:8089";
        }
        String cleanBase = webhookBase.trim();
        while (cleanBase.endsWith("/")) {
            cleanBase = cleanBase.substring(0, cleanBase.length() - 1);
        }
        return cleanBase + "/api/v1/webhooks/twilio/status-callback";
    }

    private CallStatus mapTwilioStatus(String statusStr) {
        if (statusStr == null) return CallStatus.INITIATED;
        return switch (statusStr.toLowerCase().trim()) {
            case "queued", "initiated" -> CallStatus.INITIATED;
            case "ringing" -> CallStatus.RINGING;
            case "in-progress", "answered" -> CallStatus.IN_PROGRESS;
            case "completed" -> CallStatus.COMPLETED;
            case "busy" -> CallStatus.BUSY;
            case "no-answer" -> CallStatus.NO_ANSWER;
            case "canceled", "cancelled" -> CallStatus.CANCELLED;
            case "failed" -> CallStatus.FAILED;
            default -> CallStatus.INITIATED;
        };
    }

    private String maskPhoneNumber(String phone) {
        if (phone == null || phone.isBlank()) return "[unknown]";
        if (phone.length() <= 4) return "****";
        return phone.substring(0, Math.min(3, phone.length())) + "*****" + phone.substring(phone.length() - 4);
    }

    private List<Map<String, Object>> resolveUltravoxTools(String webhookBase) {
        String cleanBase = webhookBase != null ? webhookBase.trim() : "";
        while (cleanBase.endsWith("/")) {
            cleanBase = cleanBase.substring(0, cleanBase.length() - 1);
        }
        if (cleanBase.toLowerCase().startsWith("https://")) {
            return promptBuilder.buildHospitalSearchTool(cleanBase);
        }
        if (ultravoxClient.isMock()) {
            return promptBuilder.buildHospitalSearchTool("https://mock.ruralhealthcare.com");
        }
        throw new IllegalStateException(
                "Cannot initiate real Ultravox call with tools: Ultravox requires a public HTTPS URL for tool webhooks. " +
                "The currently configured calling.twilio.webhook-base-url is [" + webhookBase + "]. " +
                "Please configure CALLING_WEBHOOK_BASE_URL to your public HTTPS domain or ngrok tunnel URL (e.g. https://your-domain.ngrok-free.app)."
        );
    }
}
