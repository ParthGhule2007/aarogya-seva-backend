package com.ruralhealthcare.patient.api.dto;

import com.ruralhealthcare.patient.domain.CasePriority;
import com.ruralhealthcare.patient.domain.CaseStatus;
import jakarta.validation.constraints.NotNull;

public class UpdateCaseStatusRequest {

    @NotNull(message = "New case status is mandatory")
    private CaseStatus status;

    private CasePriority priority;

    private String notes;

    public UpdateCaseStatusRequest() {
    }

    public UpdateCaseStatusRequest(CaseStatus status, CasePriority priority, String notes) {
        this.status = status;
        this.priority = priority;
        this.notes = notes;
    }

    public static Builder builder() {
        return new Builder();
    }

    public CaseStatus getStatus() {
        return status;
    }

    public void setStatus(CaseStatus status) {
        this.status = status;
    }

    public CasePriority getPriority() {
        return priority;
    }

    public void setPriority(CasePriority priority) {
        this.priority = priority;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public static class Builder {
        private CaseStatus status;
        private CasePriority priority;
        private String notes;

        public Builder status(CaseStatus status) {
            this.status = status;
            return this;
        }

        public Builder priority(CasePriority priority) {
            this.priority = priority;
            return this;
        }

        public Builder notes(String notes) {
            this.notes = notes;
            return this;
        }

        public UpdateCaseStatusRequest build() {
            return new UpdateCaseStatusRequest(status, priority, notes);
        }
    }
}
