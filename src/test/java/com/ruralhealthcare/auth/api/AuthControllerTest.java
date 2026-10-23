package com.ruralhealthcare.auth.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruralhealthcare.auth.domain.UserRole;
import com.ruralhealthcare.auth.dto.AuthResponse;
import com.ruralhealthcare.auth.dto.LoginRequest;
import com.ruralhealthcare.auth.dto.RegisterRequest;
import com.ruralhealthcare.auth.dto.UserDto;
import com.ruralhealthcare.auth.service.AuthService;
import com.ruralhealthcare.common.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;

    @Test
    @DisplayName("POST /api/v1/auth/login should return 200 with token on valid credentials")
    void testLoginSuccess() throws Exception {
        LoginRequest request = new LoginRequest("patient.ramesh", "Password@123");
        UserDto userDto = new UserDto(
                UUID.randomUUID(),
                "patient.ramesh",
                "Ramesh Patil",
                "9876543210",
                "ramesh@ruralhealth.org",
                UserRole.PATIENT,
                null
        );
        AuthResponse response = new AuthResponse("mock.jwt.token", "Bearer", 86400, userDto);

        when(authService.login(any(LoginRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.token").value("mock.jwt.token"))
                .andExpect(jsonPath("$.data.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.data.user.username").value("patient.ramesh"))
                .andExpect(jsonPath("$.data.user.role").value("PATIENT"));
    }

    @Test
    @DisplayName("POST /api/v1/auth/login should return 401 on invalid credentials")
    void testLoginInvalidCredentials() throws Exception {
        LoginRequest request = new LoginRequest("patient.ramesh", "WrongPassword");

        when(authService.login(any(LoginRequest.class)))
                .thenThrow(new BusinessException("Invalid username or password", "AUTH_INVALID_CREDENTIALS", HttpStatus.UNAUTHORIZED));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("AUTH_INVALID_CREDENTIALS"));
    }

    @Test
    @DisplayName("POST /api/v1/auth/register should return 201 Created on valid input")
    void testRegisterSuccess() throws Exception {
        RegisterRequest request = new RegisterRequest(
                "caregiver.suresh",
                "Password@123",
                "Suresh Patil",
                "9876543211",
                "suresh@ruralhealth.org",
                UserRole.CAREGIVER,
                null
        );
        UserDto userDto = new UserDto(
                UUID.randomUUID(),
                "caregiver.suresh",
                "Suresh Patil",
                "9876543211",
                "suresh@ruralhealth.org",
                UserRole.CAREGIVER,
                null
        );
        AuthResponse response = new AuthResponse("mock.jwt.token.caregiver", "Bearer", 86400, userDto);

        when(authService.register(any(RegisterRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.token").value("mock.jwt.token.caregiver"))
                .andExpect(jsonPath("$.data.user.role").value("CAREGIVER"));
    }

    @Test
    @DisplayName("POST /api/v1/auth/register should return 409 Conflict if username taken")
    void testRegisterDuplicateUsername() throws Exception {
        RegisterRequest request = new RegisterRequest(
                "patient.ramesh",
                "Password@123",
                "Ramesh Patil",
                "9876543210",
                "ramesh@ruralhealth.org",
                UserRole.PATIENT,
                null
        );

        when(authService.register(any(RegisterRequest.class)))
                .thenThrow(new BusinessException("Username already taken", "AUTH_USERNAME_EXISTS", HttpStatus.CONFLICT));

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("AUTH_USERNAME_EXISTS"));
    }

    @Test
    @DisplayName("GET /api/v1/auth/me should return 200 with current profile")
    void testGetCurrentUser() throws Exception {
        UserDto userDto = new UserDto(
                UUID.randomUUID(),
                "asha.sunita",
                "Sunita Kamble (ASHA)",
                "9876543212",
                "sunita@ruralhealth.org",
                UserRole.ASHA,
                null
        );

        when(authService.getCurrentUser()).thenReturn(userDto);

        mockMvc.perform(get("/api/v1/auth/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.username").value("asha.sunita"))
                .andExpect(jsonPath("$.data.role").value("ASHA"));
    }
}
