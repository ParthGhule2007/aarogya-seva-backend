package com.ruralhealthcare.patient.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public class RecordSymptomsRequest {

    @NotEmpty(message = "At least one symptom must be provided")
    @Valid
    private List<SymptomInputDto> symptoms;

    public RecordSymptomsRequest() {
    }

    public RecordSymptomsRequest(List<SymptomInputDto> symptoms) {
        this.symptoms = symptoms;
    }

    public static Builder builder() {
        return new Builder();
    }

    public List<SymptomInputDto> getSymptoms() {
        return symptoms;
    }

    public void setSymptoms(List<SymptomInputDto> symptoms) {
        this.symptoms = symptoms;
    }

    public static class Builder {
        private List<SymptomInputDto> symptoms;

        public Builder symptoms(List<SymptomInputDto> symptoms) {
            this.symptoms = symptoms;
            return this;
        }

        public RecordSymptomsRequest build() {
            return new RecordSymptomsRequest(symptoms);
        }
    }
}
