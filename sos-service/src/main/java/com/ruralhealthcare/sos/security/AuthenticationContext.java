package com.ruralhealthcare.sos.security;

public final class AuthenticationContext {

    private static final ThreadLocal<UserPrincipal> CURRENT_USER = new ThreadLocal<>();

    private AuthenticationContext() {
    }

    public static void setPrincipal(UserPrincipal principal) {
        CURRENT_USER.set(principal);
    }

    public static void setCurrentUser(UserPrincipal principal) {
        setPrincipal(principal);
    }

    public static UserPrincipal getPrincipal() {
        UserPrincipal principal = CURRENT_USER.get();
        return principal != null ? principal : UserPrincipal.anonymous();
    }

    public static UserPrincipal getCurrentUser() {
        return getPrincipal();
    }

    public static void clear() {
        CURRENT_USER.remove();
    }
}
