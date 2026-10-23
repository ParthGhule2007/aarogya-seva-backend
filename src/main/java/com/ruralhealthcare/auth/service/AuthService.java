package com.ruralhealthcare.auth.service;

import com.ruralhealthcare.auth.dto.AuthResponse;
import com.ruralhealthcare.auth.dto.LoginRequest;
import com.ruralhealthcare.auth.dto.RegisterRequest;
import com.ruralhealthcare.auth.dto.UserDto;

public interface AuthService {

    AuthResponse login(LoginRequest request);

    AuthResponse register(RegisterRequest request);

    AuthResponse linkEntity(com.ruralhealthcare.auth.dto.LinkEntityRequest request);

    AuthResponse authenticateWithGoogle(com.ruralhealthcare.auth.dto.GoogleAuthRequest request);

    Object processInternalGoogleAuth(com.ruralhealthcare.auth.dto.InternalGoogleAuthRequest request);

    UserDto getCurrentUser();
}
