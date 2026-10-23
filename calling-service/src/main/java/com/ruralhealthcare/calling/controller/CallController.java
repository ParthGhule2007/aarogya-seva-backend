package com.ruralhealthcare.calling.controller;

import com.ruralhealthcare.calling.dto.CallResponse;
import com.ruralhealthcare.calling.dto.FollowUpCallRequest;
import com.ruralhealthcare.calling.dto.TalkToAiRequest;
import com.ruralhealthcare.calling.service.CallManagementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/calls")
@Validated
@Tag(name = "AI Voice Calling", description = "Endpoints for initiating patient calls and monitoring session status")
public class CallController {

    private final CallManagementService callService;

    public CallController(CallManagementService callService) {
        this.callService = callService;
    }

    @Operation(
            summary = "Button 1: Talk to AI",
            description = "Initiates an outbound Twilio call to the patient's phone and connects it to Ultravox in English, Hindi, or Marathi."
    )
    @PostMapping("/talk-to-ai")
    public ResponseEntity<CallResponse> talkToAi(@Valid @RequestBody TalkToAiRequest request) {
        CallResponse response = callService.initiateTalkToAiCall(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(
            summary = "Button 2: Follow-up Call",
            description = "Initiates an independent follow-up check-in call. Can be requested anytime without requiring a prior consultation call or patient record."
    )
    @PostMapping("/follow-up")
    public ResponseEntity<CallResponse> followUpCall(@Valid @RequestBody FollowUpCallRequest request) {
        CallResponse response = callService.initiateFollowUpCall(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Get call session status by ID")
    @GetMapping("/{id}")
    public ResponseEntity<CallResponse> getCallStatus(@PathVariable UUID id) {
        return callService.getCallSession(id)
                .map(session -> ResponseEntity.ok(CallResponse.fromSession(session, "Session status retrieved")))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @Operation(summary = "Get detailed live call status with Twilio synchronization")
    @GetMapping("/{id}/status")
    public ResponseEntity<com.ruralhealthcare.calling.dto.CallStatusResponse> getDetailedCallStatus(@PathVariable UUID id) {
        return ResponseEntity.ok(callService.getCallStatus(id));
    }

    @Operation(
            summary = "Get TwiML for active call session",
            description = "Public endpoint providing TwiML instructions for Twilio to stream audio to Ultravox without requiring JWT authentication."
    )
    @RequestMapping(
            value = "/twiml/{callId}",
            method = {RequestMethod.GET, RequestMethod.POST},
            produces = {MediaType.APPLICATION_XML_VALUE, "text/xml"}
    )
    public ResponseEntity<String> getCallTwiml(@PathVariable("callId") String callId) {
        String twiml = callService.getTwimlForCall(callId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_XML_VALUE)
                .body(twiml);
    }

    @Operation(summary = "Get configured Twilio verified recipients")
    @GetMapping("/verified-recipients")
    public ResponseEntity<List<String>> getVerifiedRecipients() {
        return ResponseEntity.ok(callService.getConfiguredVerifiedRecipients());
    }

    @Operation(summary = "Get call history by phone number")
    @GetMapping("/history")
    public ResponseEntity<List<CallResponse>> getCallHistory(@RequestParam String phoneNumber) {
        List<CallResponse> list = callService.getCallHistory(phoneNumber).stream()
                .map(s -> CallResponse.fromSession(s, null))
                .toList();
        return ResponseEntity.ok(list);
    }

    @Operation(
            summary = "Synchronize call status with Twilio",
            description = "Safe recovery endpoint: queries Twilio REST API for authoritative call status and updates stuck or un-callbacked sessions."
    )
    @PostMapping("/{id}/sync")
    public ResponseEntity<CallResponse> syncCallStatus(@PathVariable UUID id) {
        CallResponse response = callService.syncCallStatusWithTwilio(id);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Terminate call session",
            description = "Cancels an ongoing/stuck call via Twilio and transitions the local session to CANCELLED."
    )
    @PostMapping("/{id}/terminate")
    public ResponseEntity<CallResponse> terminateCall(@PathVariable UUID id) {
        CallResponse response = callService.terminateCall(id);
        return ResponseEntity.ok(response);
    }
}
