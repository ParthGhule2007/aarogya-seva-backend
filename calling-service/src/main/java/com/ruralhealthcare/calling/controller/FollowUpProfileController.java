package com.ruralhealthcare.calling.controller;

import com.ruralhealthcare.calling.dto.FollowUpProfileResponse;
import com.ruralhealthcare.calling.dto.OptOutRequest;
import com.ruralhealthcare.calling.dto.SaveConcernSummaryRequest;
import com.ruralhealthcare.calling.service.FollowUpProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/follow-up")
@Validated
@Tag(name = "Follow-up Profile & Consent", description = "Endpoints for managing patient consent, concern summaries, and opt-outs")
public class FollowUpProfileController {

    private final FollowUpProfileService profileService;

    public FollowUpProfileController(FollowUpProfileService profileService) {
        this.profileService = profileService;
    }

    @Operation(summary = "Get follow-up profile by phone number")
    @GetMapping("/profile")
    public ResponseEntity<FollowUpProfileResponse> getProfile(@RequestParam String phoneNumber) {
        return profileService.getProfile(phoneNumber)
                .map(p -> ResponseEntity.ok(FollowUpProfileResponse.fromProfile(p)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @Operation(
            summary = "Save health concern summary with consent",
            description = "Stores a minimal, patient-reported concern summary for future follow-up. Explicit consent must be true."
    )
    @PostMapping("/concern-summary")
    public ResponseEntity<FollowUpProfileResponse> saveConcernSummary(@Valid @RequestBody SaveConcernSummaryRequest request) {
        FollowUpProfileResponse updated = profileService.saveConcernSummary(
                request.getPhoneNumber(),
                request.getConcernSummary(),
                request.isConsentGiven()
        );
        return ResponseEntity.ok(updated);
    }

    @Operation(
            summary = "Patient opt-out",
            description = "Registers patient opt-out and prevents any future automated follow-up calls to this phone number."
    )
    @PostMapping("/opt-out")
    public ResponseEntity<FollowUpProfileResponse> optOut(@Valid @RequestBody OptOutRequest request) {
        FollowUpProfileResponse updated = profileService.optOut(request.getPhoneNumber(), request.getReason());
        return ResponseEntity.ok(updated);
    }

    @Operation(
            summary = "Delete follow-up profile",
            description = "Permanently deletes the patient's saved follow-up profile and concern summary."
    )
    @DeleteMapping("/profile")
    public ResponseEntity<Void> deleteProfile(@RequestParam String phoneNumber) {
        boolean deleted = profileService.deleteProfile(phoneNumber);
        if (deleted) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }
}
