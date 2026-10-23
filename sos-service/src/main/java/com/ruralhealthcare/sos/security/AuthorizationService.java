package com.ruralhealthcare.sos.security;

import com.ruralhealthcare.sos.exception.AccessDeniedException;
import com.ruralhealthcare.sos.exception.UnauthorizedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class AuthorizationService {

    private static final Logger log = LoggerFactory.getLogger(AuthorizationService.class);

    @org.springframework.beans.factory.annotation.Value("${sos.security.emergency-bypass-enabled:false}")
    private boolean emergencyBypassEnabled = false;

    public void verifyAuthorization(UUID targetPatientId) {
        UserPrincipal principal = AuthenticationContext.getPrincipal();
        validateSosTriggerAuthorization(principal, targetPatientId);
    }

    public void validateSosTriggerAuthorization(UserPrincipal principal, UUID targetPatientId) {
        if (principal == null || !principal.isAuthenticated() || principal.getUserId() == null) {
            if (emergencyBypassEnabled) {
                log.info("Lifesaving Emergency Bypass: Unauthenticated caller allowed to trigger SOS for patient [{}]", targetPatientId);
                return;
            }
            log.warn("Blocked unauthenticated SOS trigger attempt for patient [{}]", targetPatientId);
            throw new UnauthorizedException("Anonymous SOS requests are not permitted. Authentication is required.");
        }

        // 1. Patient triggering for themselves
        if (principal.getUserId().equals(targetPatientId)) {
            log.debug("User [{}] authorized as patient triggering their own SOS", principal.getUserId());
            return;
        }

        // 2. Health worker (ASHA / Doctor / Admin) or Caregiver triggering for assisted patient
        if (principal.isAshaOrHealthWorker() || principal.isCaregiver()) {
            log.info("Proxy [{}] ({}) authorized to trigger SOS on behalf of assisted patient [{}]",
                    principal.getUserId(), principal.getRole(), targetPatientId);
            return;
        }

        // 3. Unauthorized access
        log.warn("Access denied: User [{}] with role [{}] attempted to trigger SOS for patient [{}]",
                principal.getUserId(), principal.getRole(), targetPatientId);
        throw new AccessDeniedException(
                String.format("User [%s] is not authorized to trigger emergency SOS on behalf of patient [%s]",
                        principal.getUserId(), targetPatientId));
    }

    public void validateDeviceRegistrationAuthorization(UserPrincipal principal, UUID targetUserId) {
        if (principal == null || !principal.isAuthenticated() || principal.getUserId() == null) {
            // In local/dev or during initial device onboarding, if unauthenticated, allow registration
            return;
        }

        if (principal.getUserId().equals(targetUserId) || principal.isAshaOrHealthWorker()) {
            return;
        }

        log.warn("Access denied: User [{}] attempted device operation for user [{}]", principal.getUserId(), targetUserId);
        throw new AccessDeniedException("You are not authorized to manage device tokens for another user.");
    }
}
