package com.ruralhealthcare.patient.domain;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

public enum CaseStatus {
    CREATED,
    SYMPTOMS_RECORDED,
    PENDING_TRIAGE,
    TRIAGED,
    REFERRED,
    CLOSED,
    CANCELLED;

    public boolean canTransitionTo(CaseStatus target) {
        if (target == null || this == target) {
            return false;
        }
        return switch (this) {
            case CREATED -> EnumSet.of(SYMPTOMS_RECORDED, PENDING_TRIAGE, CANCELLED).contains(target);
            case SYMPTOMS_RECORDED -> EnumSet.of(PENDING_TRIAGE, TRIAGED, CANCELLED).contains(target);
            case PENDING_TRIAGE -> EnumSet.of(TRIAGED, CANCELLED).contains(target);
            case TRIAGED -> EnumSet.of(REFERRED, CLOSED, CANCELLED).contains(target);
            case REFERRED -> EnumSet.of(CLOSED, CANCELLED).contains(target);
            case CLOSED, CANCELLED -> false; // Terminal states
        };
    }

    public Set<CaseStatus> getValidNextStates() {
        return switch (this) {
            case CREATED -> EnumSet.of(SYMPTOMS_RECORDED, PENDING_TRIAGE, CANCELLED);
            case SYMPTOMS_RECORDED -> EnumSet.of(PENDING_TRIAGE, TRIAGED, CANCELLED);
            case PENDING_TRIAGE -> EnumSet.of(TRIAGED, CANCELLED);
            case TRIAGED -> EnumSet.of(REFERRED, CLOSED, CANCELLED);
            case REFERRED -> EnumSet.of(CLOSED, CANCELLED);
            case CLOSED, CANCELLED -> Collections.emptySet();
        };
    }
}
