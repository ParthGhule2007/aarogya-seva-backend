package com.ruralhealthcare.symptom.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public class BatchCreateSymptomsRequest {

    @NotEmpty(message = "At least one symptom must be provided")
    @Valid
    private List<CreateSymptomRequest> symptoms;

    public BatchCreateSymptomsRequest() {
    }

    public BatchCreateSymptomsRequest(List<CreateSymptomRequest> symptoms) {
        this.symptoms = symptoms;
    }

    public static Builder builder() {
        return new Builder();
    }

    public List<CreateSymptomRequest> getSymptoms() {
        return symptoms;
    }

    public void setSymptoms(List<CreateSymptomRequest> symptoms) {
        this.symptoms = symptoms;
    }

    public static class Builder {
        private List<CreateSymptomRequest> symptoms;

        public Builder symptoms(List<CreateSymptomRequest> symptoms) {
            this.symptoms = symptoms;
            return this;
        }

        public BatchCreateSymptomsRequest build() {
            return new BatchCreateSymptomsRequest(symptoms);
        }
    }
}
