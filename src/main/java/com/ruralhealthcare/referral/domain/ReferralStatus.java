package com.ruralhealthcare.referral.domain;

import java.util.Set;

public enum ReferralStatus {
    PENDING("Pending Review"),
    ACCEPTED("Accepted"),
    REJECTED("Rejected"),
    IN_PROGRESS("In Progress"),
    COMPLETED("Completed"),
    CANCELLED("Cancelled");

    private final String displayName;

    ReferralStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    /**
     * Determines whether this status represents an active, ongoing referral.
     * Active referrals count toward the single active referral per case constraint.
     */
    public boolean isActive() {
        return this == PENDING || this == ACCEPTED || this == IN_PROGRESS;
    }

    /**
     * Determines whether transitioning from this status to targetStatus is permitted.
     *
     * Permitted flows:
     * - PENDING -> ACCEPTED, REJECTED, CANCELLED
     * - ACCEPTED -> IN_PROGRESS, COMPLETED, CANCELLED
     * - IN_PROGRESS -> COMPLETED, CANCELLED
     */
    public boolean canTransitionTo(ReferralStatus targetStatus) {
        if (targetStatus == null || this == targetStatus) {
            return false;
        }

        return switch (this) {
            case PENDING -> targetStatus == ACCEPTED || targetStatus == REJECTED || targetStatus == CANCELLED;
            case ACCEPTED -> targetStatus == IN_PROGRESS || targetStatus == COMPLETED || targetStatus == CANCELLED;
            case IN_PROGRESS -> targetStatus == COMPLETED || targetStatus == CANCELLED;
            case REJECTED, COMPLETED, CANCELLED -> false;
        };
    }

    public static Set<ReferralStatus> getActiveStatuses() {
        return Set.of(PENDING, ACCEPTED, IN_PROGRESS);
    }
}
