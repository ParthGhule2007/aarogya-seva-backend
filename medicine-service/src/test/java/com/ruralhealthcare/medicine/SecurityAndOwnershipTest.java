package com.ruralhealthcare.medicine;

import com.ruralhealthcare.medicine.config.MedicineProperties;
import com.ruralhealthcare.medicine.exception.AccessDeniedException;
import com.ruralhealthcare.medicine.exception.UnauthorizedException;
import com.ruralhealthcare.medicine.security.AuthenticationContext;
import com.ruralhealthcare.medicine.security.AuthenticationFilter;
import com.ruralhealthcare.medicine.security.SecurityUtils;
import com.ruralhealthcare.medicine.security.UserPrincipal;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SecurityAndOwnershipTest {

    @AfterEach
    void tearDown() {
        AuthenticationContext.clear();
    }

    @Test
    @DisplayName("Patient cannot access another patient's medication records (AccessDeniedException 403)")
    void testPatientOwnershipAccessDenied() {
        MedicineProperties properties = new MedicineProperties();
        properties.getSecurity().setRequireAuthentication(true);
        SecurityUtils securityUtils = new SecurityUtils(properties);

        UUID patientA = UUID.randomUUID();
        UUID patientB = UUID.randomUUID();

        // Caller is Patient A
        AuthenticationContext.setPrincipal(new UserPrincipal(patientA, "PATIENT", true));

        // Attempting to access Patient B's data
        AccessDeniedException ex = assertThrows(AccessDeniedException.class, () ->
                securityUtils.assertPatientAccess(patientB)
        );
        assertTrue(ex.getMessage().contains("is not authorized to access patient"));

        // Accessing Patient A's own data is permitted
        assertDoesNotThrow(() -> securityUtils.assertPatientAccess(patientA));
    }

    @Test
    @DisplayName("Doctor or Admin role is authorized to access any patient's records")
    void testClinicianCrossPatientAccessAllowed() {
        MedicineProperties properties = new MedicineProperties();
        properties.getSecurity().setRequireAuthentication(true);
        SecurityUtils securityUtils = new SecurityUtils(properties);

        UUID doctorId = UUID.randomUUID();
        UUID patient = UUID.randomUUID();

        AuthenticationContext.setPrincipal(new UserPrincipal(doctorId, "DOCTOR", true));
        assertDoesNotThrow(() -> securityUtils.assertPatientAccess(patient));

        AuthenticationContext.setPrincipal(new UserPrincipal(UUID.randomUUID(), "ADMIN", true));
        assertDoesNotThrow(() -> securityUtils.assertPatientAccess(patient));
    }

    @Test
    @DisplayName("Unauthenticated caller blocked when authentication is required (401)")
    void testUnauthenticatedAccessBlocked() {
        MedicineProperties properties = new MedicineProperties();
        properties.getSecurity().setRequireAuthentication(true);
        SecurityUtils securityUtils = new SecurityUtils(properties);

        AuthenticationContext.setPrincipal(UserPrincipal.anonymous());

        assertThrows(UnauthorizedException.class, () ->
                securityUtils.assertPatientAccess(UUID.randomUUID())
        );
    }

    @Test
    @DisplayName("AuthenticationFilter blocks direct untrusted access to port 8088 when gateway token check is active")
    void testDirectUntrustedPort8088AccessBlockedByGatewayFilter() throws ServletException, IOException {
        MedicineProperties properties = new MedicineProperties();
        properties.getSecurity().setRequireGatewayToken(true);
        properties.getSecurity().setGatewayToken("aarogya-internal-secret-token");

        AuthenticationFilter filter = new AuthenticationFilter(properties);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/medications");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        // Call without X-Gateway-Token
        filter.doFilter(request, response, chain);

        assertEquals(403, response.getStatus());
        assertTrue(response.getContentAsString().contains("Direct untrusted access to port 8088 blocked"));
        verify(chain, never()).doFilter(any(), any());
    }

    @Test
    @DisplayName("AuthenticationFilter allows requests carrying verified gateway secret token")
    void testVerifiedGatewayTokenAllowed() throws ServletException, IOException {
        MedicineProperties properties = new MedicineProperties();
        properties.getSecurity().setRequireGatewayToken(true);
        properties.getSecurity().setGatewayToken("aarogya-internal-secret-token");

        AuthenticationFilter filter = new AuthenticationFilter(properties);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/medications");
        request.addHeader(AuthenticationFilter.HEADER_GATEWAY_TOKEN, "aarogya-internal-secret-token");
        request.addHeader(AuthenticationFilter.HEADER_USER_ID, UUID.randomUUID().toString());
        request.addHeader(AuthenticationFilter.HEADER_USER_ROLE, "PATIENT");

        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        assertEquals(200, response.getStatus());
        verify(chain, times(1)).doFilter(any(), any());
    }
}
