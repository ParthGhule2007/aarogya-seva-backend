package com.ruralhealthcare.auth.security;

import java.util.Optional;

public final class AuthenticationContext {

    private static final ThreadLocal<UserPrincipal> CURRENT_USER = new ThreadLocal<>();

    private AuthenticationContext() {
    }

    public static void setPrincipal(UserPrincipal principal) {
        CURRENT_USER.set(principal);
    }

    public static Optional<UserPrincipal> getPrincipal() {
        return Optional.ofNullable(CURRENT_USER.get());
    }

    public static void clear() {
        CURRENT_USER.remove();
    }
}
