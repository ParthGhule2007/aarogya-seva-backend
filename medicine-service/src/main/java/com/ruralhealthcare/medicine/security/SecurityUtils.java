package com.ruralhealthcare.medicine.security;

import com.ruralhealthcare.medicine.config.MedicineProperties;
import com.ruralhealthcare.medicine.exception.AccessDeniedException;
import com.ruralhealthcare.medicine.exception.UnauthorizedException;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class SecurityUtils {

    private final MedicineProperties properties;

    public SecurityUtils(MedicineProperties properties) {
        this.properties = properties;
    }

    public void assertPatientAccess(UUID targetPatientId) {
        if (targetPatientId == null) {
            return;
        }

        UserPrincipal principal = AuthenticationContext.getPrincipal();
        boolean requireAuth = properties.getSecurity().isRequireAuthentication();

        if (!principal.isAuthenticated()) {
            if (requireAuth) {
                throw new UnauthorizedException("Authentication required: Missing or unverified caller identity header");
            }
            return;
        }

        String role = principal.getRole();
        if ("PATIENT".equalsIgnoreCase(role)) {
            UUID callerId = principal.getUserId();
            if (callerId != null && !callerId.equals(targetPatientId)) {
                throw new AccessDeniedException("Access denied: Caller [" + callerId + "] is not authorized to access patient [" + targetPatientId + "]");
            }
        }
        // Roles like DOCTOR, CLINICIAN, ADMIN, CAREGIVER are permitted cross-patient access
    }
}
