package com.ruralhealthcare.medicine.security;

public class AuthenticationContext {

    private static final ThreadLocal<UserPrincipal> CURRENT_USER = new ThreadLocal<>();

    public static void setPrincipal(UserPrincipal principal) {
        CURRENT_USER.set(principal);
    }

    public static UserPrincipal getPrincipal() {
        UserPrincipal principal = CURRENT_USER.get();
        return principal != null ? principal : UserPrincipal.anonymous();
    }

    public static void clear() {
        CURRENT_USER.remove();
    }
}
