package com.ruralhealthcare.referral.service;

import com.ruralhealthcare.referral.api.dto.CreateReferralRequest;
import com.ruralhealthcare.referral.api.dto.ReferralDetailDto;
import com.ruralhealthcare.referral.api.dto.ReferralResponseDto;
import com.ruralhealthcare.referral.api.dto.UpdateReferralStatusRequest;
import com.ruralhealthcare.referral.domain.ReferralStatus;

import java.util.List;
import java.util.UUID;

public interface ReferralService {

    /**
     * Creates a new referral request from a patient to a chosen healthcare facility for an existing case.
     * Enforces that AI screening has been completed and that no other active referral exists for the case.
     *
     * @param request the referral creation request
     * @return summary response DTO of the created referral
     */
    ReferralResponseDto createReferral(CreateReferralRequest request);

    /**
     * Retrieves full clinical, case, symptom, AI assessment, and facility context for a specific referral.
     * Used by facility personnel during evaluation.
     *
     * @param referralId the referral UUID
     * @return comprehensive detail DTO
     */
    ReferralDetailDto getReferralById(UUID referralId);

    /**
     * Retrieves the referral history for a healthcare case ordered by request time descending.
     *
     * @param caseId the healthcare case UUID
     * @return list of referral summaries
     */
    List<ReferralResponseDto> getReferralsByCaseId(UUID caseId);

    /**
     * Retrieves all referrals associated with a patient ordered by request time descending.
     *
     * @param patientId the patient UUID
     * @return list of referral summaries
     */
    List<ReferralResponseDto> getReferralsByPatientId(UUID patientId);

    /**
     * Retrieves referrals directed to a healthcare facility, with optional status filter.
     *
     * @param facilityId the healthcare facility UUID
     * @param status optional status filter (e.g. PENDING)
     * @return list of referral summaries
     */
    List<ReferralResponseDto> getReferralsByFacilityId(UUID facilityId, ReferralStatus status);

    /**
     * Updates the status of an existing referral (e.g. ACCEPT, REJECT, IN_PROGRESS, COMPLETED, CANCELLED).
     * Enforces valid state transitions and updates corresponding lifecycle timestamps.
     *
     * @param referralId the referral UUID
     * @param request status update request with optional facility response note
     * @return updated referral summary DTO
     */
    ReferralResponseDto updateReferralStatus(UUID referralId, UpdateReferralStatusRequest request);
}
