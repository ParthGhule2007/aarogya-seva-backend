package com.ruralhealthcare.referral.api;

import com.ruralhealthcare.common.dto.ApiResponse;
import com.ruralhealthcare.referral.api.dto.CreateReferralRequest;
import com.ruralhealthcare.referral.api.dto.ReferralDetailDto;
import com.ruralhealthcare.referral.api.dto.ReferralResponseDto;
import com.ruralhealthcare.referral.api.dto.UpdateReferralStatusRequest;
import com.ruralhealthcare.referral.domain.ReferralStatus;
import com.ruralhealthcare.referral.service.ReferralService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/referrals")
@Validated
@Tag(name = "Referrals & Care Navigation", description = "Endpoints for creating patient referral requests, care facility review, and referral status lifecycles")
public class ReferralController {

    private final ReferralService referralService;

    public ReferralController(ReferralService referralService) {
        this.referralService = referralService;
    }

    @Operation(
            summary = "Create a referral request",
            description = "Submits a patient referral request to a chosen healthcare facility for an evaluated healthcare case."
    )
    @PostMapping
    public ResponseEntity<ApiResponse<ReferralResponseDto>> createReferral(@Valid @RequestBody CreateReferralRequest request) {
        ReferralResponseDto created = referralService.createReferral(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Referral request submitted successfully", created));
    }

    @Operation(
            summary = "Get referral details by ID",
            description = "Retrieves full referral context including patient demographics, case data, recorded symptoms, and AI clinical assessment for facility evaluation."
    )
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ReferralDetailDto>> getReferralById(@PathVariable UUID id) {
        ReferralDetailDto detail = referralService.getReferralById(id);
        return ResponseEntity.ok(ApiResponse.ok(detail));
    }

    @Operation(
            summary = "Get referrals by case ID",
            description = "Retrieves all referral attempts and audit history for a specific healthcare case ordered by request time descending."
    )
    @GetMapping("/case/{caseId}")
    public ResponseEntity<ApiResponse<List<ReferralResponseDto>>> getReferralsByCaseId(@PathVariable UUID caseId) {
        List<ReferralResponseDto> referrals = referralService.getReferralsByCaseId(caseId);
        return ResponseEntity.ok(ApiResponse.ok(referrals));
    }

    @Operation(
            summary = "Get referrals by patient ID",
            description = "Retrieves all referral requests associated with a patient ordered by request time descending."
    )
    @GetMapping("/patient/{patientId}")
    public ResponseEntity<ApiResponse<List<ReferralResponseDto>>> getReferralsByPatientId(@PathVariable UUID patientId) {
        List<ReferralResponseDto> referrals = referralService.getReferralsByPatientId(patientId);
        return ResponseEntity.ok(ApiResponse.ok(referrals));
    }

    @Operation(
            summary = "Get referrals for a facility",
            description = "Retrieves referral requests directed to a specific healthcare facility, with optional status filter (e.g. PENDING)."
    )
    @GetMapping("/facility/{facilityId}")
    public ResponseEntity<ApiResponse<List<ReferralResponseDto>>> getReferralsByFacilityId(
            @PathVariable UUID facilityId,
            @RequestParam(required = false) ReferralStatus status) {
        List<ReferralResponseDto> referrals = referralService.getReferralsByFacilityId(facilityId, status);
        return ResponseEntity.ok(ApiResponse.ok(referrals));
    }

    @Operation(
            summary = "Update referral status",
            description = "Allows healthcare facility personnel or dispatchers to accept, reject, advance, or complete a referral request."
    )
    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<ReferralResponseDto>> updateReferralStatus(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateReferralStatusRequest request) {
        ReferralResponseDto updated = referralService.updateReferralStatus(id, request);
        return ResponseEntity.ok(ApiResponse.ok("Referral status updated successfully", updated));
    }
}
