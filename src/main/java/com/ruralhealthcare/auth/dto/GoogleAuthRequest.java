package com.ruralhealthcare.auth.dto;

import jakarta.validation.constraints.NotBlank;

public class GoogleAuthRequest {

    @NotBlank(message = "idToken is required")
    private String idToken;

    private String role;

    public GoogleAuthRequest() {
    }

    public GoogleAuthRequest(String idToken, String role) {
        this.idToken = idToken;
        this.role = role;
    }

    public String getIdToken() {
        return idToken;
    }

    public void setIdToken(String idToken) {
        this.idToken = idToken;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }
}
