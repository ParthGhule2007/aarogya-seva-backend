package com.ruralhealthcare.auth.dto;

import jakarta.validation.constraints.NotBlank;

public class LinkEntityRequest {

    @NotBlank(message = "associatedEntityId is required")
    private String associatedEntityId;

    public LinkEntityRequest() {
    }

    public LinkEntityRequest(String associatedEntityId) {
        this.associatedEntityId = associatedEntityId;
    }

    public String getAssociatedEntityId() {
        return associatedEntityId;
    }

    public void setAssociatedEntityId(String associatedEntityId) {
        this.associatedEntityId = associatedEntityId;
    }
}
