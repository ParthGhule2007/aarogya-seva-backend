package com.ruralhealthcare.sos.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

@Component
@Order(1)
public class AuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenValidator jwtTokenValidator;

    public AuthenticationFilter(JwtTokenValidator jwtTokenValidator) {
        this.jwtTokenValidator = jwtTokenValidator;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            // 1. Try Bearer JWT validation first
            String authHeader = request.getHeader("Authorization");
            if (StringUtils.hasText(authHeader) && authHeader.regionMatches(true, 0, "Bearer ", 0, 7)) {
                String token = authHeader.substring(7).trim();
                Optional<UserPrincipal> principalOpt = jwtTokenValidator.validateToken(token);
                if (principalOpt.isPresent()) {
                    AuthenticationContext.setPrincipal(principalOpt.get());
                    filterChain.doFilter(request, response);
                    return;
                }
            }

            // 2. Dual-mode fallback: check X-User-Id and X-User-Role headers
            String userIdHeader = request.getHeader("X-User-Id");
            String roleHeader = request.getHeader("X-User-Role");

            if (userIdHeader != null && !userIdHeader.isBlank()) {
                try {
                    UUID userId = UUID.fromString(userIdHeader.trim());
                    String role = (roleHeader != null && !roleHeader.isBlank()) ? roleHeader.trim() : "PATIENT";
                    AuthenticationContext.setPrincipal(UserPrincipal.of(userId, role));
                } catch (IllegalArgumentException e) {
                    AuthenticationContext.setPrincipal(UserPrincipal.anonymous());
                }
            } else {
                AuthenticationContext.setPrincipal(UserPrincipal.anonymous());
            }

            filterChain.doFilter(request, response);
        } finally {
            AuthenticationContext.clear();
        }
    }
}
