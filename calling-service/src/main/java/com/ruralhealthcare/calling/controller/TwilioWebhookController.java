package com.ruralhealthcare.calling.controller;

import com.ruralhealthcare.calling.security.TwilioSignatureValidator;
import com.ruralhealthcare.calling.service.CallManagementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/webhooks/twilio")
@Tag(name = "Twilio Webhooks", description = "Webhooks receiving Twilio call status events and voice routing")
public class TwilioWebhookController {

    private static final Logger log = LoggerFactory.getLogger(TwilioWebhookController.class);

    private final CallManagementService callService;
    private final TwilioSignatureValidator signatureValidator;

    public TwilioWebhookController(CallManagementService callService, TwilioSignatureValidator signatureValidator) {
        this.callService = callService;
        this.signatureValidator = signatureValidator;
    }

    @Operation(summary = "Twilio status callback webhook")
    @PostMapping("/status-callback")
    public ResponseEntity<Void> onStatusCallback(
            HttpServletRequest request,
            @RequestHeader(value = "X-Twilio-Signature", required = false) String signature,
            @RequestParam Map<String, String> allParams
    ) {
        String callSid = allParams.get("CallSid");
        String callStatus = allParams.get("CallStatus");

        if (!signatureValidator.validate(request, allParams, signature)) {
            log.warn("[TwilioWebhookController] Rejected status callback with invalid Twilio signature for CallSid [{}]", callSid);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String durationStr = allParams.get("CallDuration");
        String errorMessage = allParams.get("ErrorMessage");

        Integer duration = null;
        if (durationStr != null && !durationStr.isBlank()) {
            try {
                duration = Integer.parseInt(durationStr.trim());
            } catch (NumberFormatException ignored) {
            }
        }

        log.info("[TwilioWebhookController] Validated status webhook received for CallSid [{}], Status [{}]", callSid, callStatus);
        callService.handleTwilioStatusCallback(callSid, callStatus, duration, errorMessage);

        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Twilio voice fallback endpoint returning TwiML")
    @PostMapping(value = "/voice", produces = MediaType.APPLICATION_XML_VALUE)
    public ResponseEntity<String> onVoice(
            HttpServletRequest request,
            @RequestHeader(value = "X-Twilio-Signature", required = false) String signature,
            @RequestParam Map<String, String> allParams
    ) {
        if (!signatureValidator.validate(request, allParams, signature)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String twiml = "<Response><Say voice=\"alice\">Connecting to Aarogya Mitra Voice Health Assistant.</Say></Response>";
        return ResponseEntity.ok(twiml);
    }
}
