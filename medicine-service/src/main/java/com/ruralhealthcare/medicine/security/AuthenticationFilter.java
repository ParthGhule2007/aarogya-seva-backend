package com.ruralhealthcare.medicine.security;

import com.ruralhealthcare.medicine.config.MedicineProperties;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.time.Instant;
import java.util.UUID;

@Component
@Order(1)
public class AuthenticationFilter implements Filter {

    public static final String HEADER_USER_ID = "X-User-Id";
    public static final String HEADER_USER_ROLE = "X-User-Role";
    public static final String HEADER_GATEWAY_TOKEN = "X-Gateway-Token";

    private final MedicineProperties properties;
    private final JwtTokenValidator jwtTokenValidator;

    public AuthenticationFilter(MedicineProperties properties) {
        this(properties, new JwtTokenValidator("aarogya-seva-secret-key-rural-healthcare-jwt-token-2024-secure-key-32bytes"));
    }

    @org.springframework.beans.factory.annotation.Autowired
    public AuthenticationFilter(MedicineProperties properties, JwtTokenValidator jwtTokenValidator) {
        this.properties = properties;
        this.jwtTokenValidator = jwtTokenValidator;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        try {
            if (request instanceof HttpServletRequest && response instanceof HttpServletResponse) {
                HttpServletRequest httpRequest = (HttpServletRequest) request;
                HttpServletResponse httpResponse = (HttpServletResponse) response;

                String path = httpRequest.getRequestURI();

                // 1. Check Gateway Secret Boundary if enabled
                if (properties.getSecurity().isRequireGatewayToken()) {
                    String gatewayToken = httpRequest.getHeader(HEADER_GATEWAY_TOKEN);
                    String expectedToken = properties.getSecurity().getGatewayToken();
                    if (!StringUtils.hasText(gatewayToken) || !gatewayToken.equals(expectedToken)) {
                        writeErrorResponse(httpResponse, HttpServletResponse.SC_FORBIDDEN, "FORBIDDEN",
                                "Direct untrusted access to port 8088 blocked: Trusted gateway verification failed.", path);
                        return;
                    }
                }

                // 2. Extract Caller Identity via JWT Bearer Token first
                String authHeader = httpRequest.getHeader("Authorization");
                if (StringUtils.hasText(authHeader) && authHeader.regionMatches(true, 0, "Bearer ", 0, 7)) {
                    String token = authHeader.substring(7).trim();
                    java.util.Optional<UserPrincipal> principalOpt = jwtTokenValidator.validateToken(token);
                    if (principalOpt.isPresent()) {
                        AuthenticationContext.setPrincipal(principalOpt.get());
                        chain.doFilter(request, response);
                        return;
                    }
                }

                // 3. Fallback: Extract Caller Identity via Headers
                String userIdHeader = httpRequest.getHeader(HEADER_USER_ID);
                String userRoleHeader = httpRequest.getHeader(HEADER_USER_ROLE);

                if (StringUtils.hasText(userIdHeader)) {
                    try {
                        UUID userId = UUID.fromString(userIdHeader.trim());
                        UserPrincipal principal = new UserPrincipal(userId, userRoleHeader != null ? userRoleHeader : "PATIENT", true);
                        AuthenticationContext.setPrincipal(principal);
                    } catch (IllegalArgumentException e) {
                        writeErrorResponse(httpResponse, HttpServletResponse.SC_BAD_REQUEST, "INVALID_IDENTITY_HEADER",
                                "Malformed header: X-User-Id must be a valid UUID.", path);
                        return;
                    }
                } else {
                    AuthenticationContext.setPrincipal(UserPrincipal.anonymous());

                    // If authentication is strictly required on protected /api/v1/ routes
                    if (properties.getSecurity().isRequireAuthentication() && isProtectedRoute(path)) {
                        writeErrorResponse(httpResponse, HttpServletResponse.SC_UNAUTHORIZED, "UNAUTHORIZED",
                                "Authentication required: Missing trusted X-User-Id caller identity header.", path);
                        return;
                    }
                }
            }
            chain.doFilter(request, response);
        } finally {
            AuthenticationContext.clear();
        }
    }

    private boolean isProtectedRoute(String path) {
        if (path == null) return false;
        return path.startsWith("/api/v1/medications") ||
                path.startsWith("/api/v1/medication-events") ||
                path.startsWith("/api/v1/caregivers");
    }

    private void writeErrorResponse(HttpServletResponse response, int status, String error, String message, String path) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        String json = String.format("{\"timestamp\":\"%s\",\"status\":%d,\"error\":\"%s\",\"message\":\"%s\",\"path\":\"%s\",\"details\":[]}",
                Instant.now(), status, error, message, path);
        response.getWriter().write(json);
    }
}
