package com.ruralhealthcare.referral.exception;

import com.ruralhealthcare.common.exception.BusinessException;
import com.ruralhealthcare.referral.domain.ReferralStatus;
import org.springframework.http.HttpStatus;

import java.util.UUID;

public class ReferralException extends BusinessException {

    public ReferralException(String message, String errorCode, HttpStatus status) {
        super(message, errorCode, status);
    }

    public ReferralException(String message, String errorCode) {
        super(message, errorCode, HttpStatus.BAD_REQUEST);
    }

    public static ReferralException referralNotFound(UUID id) {
        return new ReferralException("Referral request not found with ID: " + id, "REFERRAL_NOT_FOUND", HttpStatus.NOT_FOUND);
    }

    public static ReferralException caseNotFound(UUID caseId) {
        return new ReferralException("Healthcare case not found with ID: " + caseId, "CASE_NOT_FOUND", HttpStatus.NOT_FOUND);
    }

    public static ReferralException facilityNotFound(UUID facilityId) {
        return new ReferralException("Healthcare facility not found with ID: " + facilityId, "FACILITY_NOT_FOUND", HttpStatus.NOT_FOUND);
    }

    public static ReferralException facilityNonOperational(UUID facilityId) {
        return new ReferralException("Selected healthcare facility [" + facilityId + "] is currently non-operational and cannot receive referrals.",
                "FACILITY_NON_OPERATIONAL", HttpStatus.BAD_REQUEST);
    }

    public static ReferralException aiAnalysisNotFound(UUID caseId) {
        return new ReferralException("AI clinical screening analysis must be completed for case [" + caseId + "] before a referral can be submitted.",
                "AI_ANALYSIS_REQUIRED", HttpStatus.BAD_REQUEST);
    }

    public static ReferralException activeReferralExists(UUID caseId) {
        return new ReferralException("An active referral (PENDING, ACCEPTED, or IN_PROGRESS) already exists for case [" + caseId + "]. " +
                "Only one active referral is allowed at a time.", "ACTIVE_REFERRAL_EXISTS", HttpStatus.CONFLICT);
    }

    public static ReferralException invalidStatusTransition(ReferralStatus currentStatus, ReferralStatus targetStatus) {
        return new ReferralException("Cannot transition referral status from " + currentStatus + " to " + targetStatus + ".",
                "INVALID_STATUS_TRANSITION", HttpStatus.BAD_REQUEST);
    }
}
