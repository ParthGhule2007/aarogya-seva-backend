package com.ruralhealthcare.auth.security;

import com.ruralhealthcare.auth.domain.UserRole;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
@ConditionalOnBean(JwtTokenProvider.class)
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    private final JwtTokenProvider jwtTokenProvider;
    private final JwtProperties jwtProperties;

    public JwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider, JwtProperties jwtProperties) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.jwtProperties = jwtProperties;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            if (jwtProperties.isEnabled()) {
                String token = extractBearerToken(request);

                if (StringUtils.hasText(token)) {
                    Optional<UserPrincipal> principalOpt = jwtTokenProvider.parseAndValidateToken(token);
                    if (principalOpt.isPresent()) {
                        UserPrincipal principal = principalOpt.get();
                        AuthenticationContext.setPrincipal(principal);
                        request.setAttribute("userPrincipal", principal);
                        log.debug("Authenticated user [{}] with role [{}] via JWT", principal.getUsername(), principal.getRole());
                    } else if (jwtProperties.isStrictMode()) {
                        response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid or expired JWT token");
                        return;
                    }
                } else {
                    // Dual-Mode fallback: Check X-User-Id and X-User-Role headers for backwards compatibility
                    String headerUserId = request.getHeader("X-User-Id");
                    String headerUserRole = request.getHeader("X-User-Role");

                    if (StringUtils.hasText(headerUserId)) {
                        try {
                            UUID userId = UUID.fromString(headerUserId.trim());
                            UserRole role = UserRole.fromString(headerUserRole);
                            UserPrincipal fallbackPrincipal = new UserPrincipal(
                                    userId,
                                    "header-user-" + userId,
                                    role,
                                    "Header Authenticated User",
                                    null,
                                    null
                            );
                            AuthenticationContext.setPrincipal(fallbackPrincipal);
                            request.setAttribute("userPrincipal", fallbackPrincipal);
                            log.debug("Fallback authentication via headers: user [{}], role [{}]", userId, role);
                        } catch (IllegalArgumentException ignored) {
                            // Invalid UUID in header, ignore
                        }
                    }
                }
            }

            filterChain.doFilter(request, response);
        } finally {
            AuthenticationContext.clear();
        }
    }

    private String extractBearerToken(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (StringUtils.hasText(authHeader) && authHeader.regionMatches(true, 0, "Bearer ", 0, 7)) {
            return authHeader.substring(7).trim();
        }
        return null;
    }
}
