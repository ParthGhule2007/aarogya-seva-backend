package com.ruralhealthcare.auth.service.impl;

import com.ruralhealthcare.auth.domain.User;
import com.ruralhealthcare.auth.domain.UserRole;
import com.ruralhealthcare.auth.dto.AuthResponse;
import com.ruralhealthcare.auth.dto.GoogleAuthRequest;
import com.ruralhealthcare.auth.dto.LinkEntityRequest;
import com.ruralhealthcare.auth.dto.LoginRequest;
import com.ruralhealthcare.auth.dto.RegisterRequest;
import com.ruralhealthcare.auth.dto.UserDto;
import com.ruralhealthcare.auth.repository.UserRepository;
import com.ruralhealthcare.auth.security.AuthenticationContext;
import com.ruralhealthcare.auth.security.JwtTokenProvider;
import com.ruralhealthcare.auth.security.UserPrincipal;
import com.ruralhealthcare.auth.service.AuthService;
import com.ruralhealthcare.common.exception.BusinessException;
import com.ruralhealthcare.common.exception.ResourceNotFoundException;
import com.ruralhealthcare.patient.domain.Gender;
import com.ruralhealthcare.patient.domain.Patient;
import com.ruralhealthcare.patient.repository.PatientRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class AuthServiceImpl implements AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthServiceImpl.class);

    private final UserRepository userRepository;
    private final PatientRepository patientRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    @Value("${google.web-client-id:${GOOGLE_WEB_CLIENT_ID:}}")
    private String googleWebClientId;

    public AuthServiceImpl(UserRepository userRepository,
                           PatientRepository patientRepository,
                           PasswordEncoder passwordEncoder,
                           JwtTokenProvider jwtTokenProvider) {
        this.userRepository = userRepository;
        this.patientRepository = patientRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        String identifier = request.getUsername().trim();
        log.info("Processing login request for identifier: {}", identifier);

        // Lookup by username, phone, or email
        Optional<User> userOpt = userRepository.findByUsername(identifier);
        if (userOpt.isEmpty()) {
            userOpt = userRepository.findByPhoneNumber(identifier);
        }
        if (userOpt.isEmpty()) {
            userOpt = userRepository.findByEmail(identifier);
        }

        if (userOpt.isEmpty()) {
            log.warn("Login failed: User not found for identifier [{}]", identifier);
            throw new BusinessException("Invalid username or password", "AUTH_INVALID_CREDENTIALS", HttpStatus.UNAUTHORIZED);
        }

        User user = userOpt.get();

        if (!user.isActive()) {
            log.warn("Login failed: Account deactivated for user [{}]", user.getUsername());
            throw new BusinessException("Account is deactivated. Please contact support.", "AUTH_ACCOUNT_DEACTIVATED", HttpStatus.FORBIDDEN);
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            log.warn("Login failed: Incorrect password for user [{}]", user.getUsername());
            throw new BusinessException("Invalid username or password", "AUTH_INVALID_CREDENTIALS", HttpStatus.UNAUTHORIZED);
        }

        resolvePatientAssociationIfNeeded(user);

        String token = jwtTokenProvider.generateToken(user);
        long expiresInSeconds = jwtTokenProvider.getExpirationMs() / 1000;

        log.info("User [{}] successfully logged in with role [{}] and associatedEntityId [{}]",
                user.getUsername(), user.getRole(), user.getAssociatedEntityId());

        return AuthResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .expiresIn(expiresInSeconds)
                .user(UserDto.fromEntity(user))
                .build();
    }

    @Override
    public AuthResponse register(RegisterRequest request) {
        String username = request.getUsername().trim();
        log.info("Processing registration for new user [{}] with role [{}]", username, request.getRole());

        if (userRepository.existsByUsername(username)) {
            throw new BusinessException("Username '" + username + "' is already taken", "AUTH_USERNAME_EXISTS", HttpStatus.CONFLICT);
        }

        if (StringUtils.hasText(request.getPhoneNumber())) {
            String phone = request.getPhoneNumber().trim();
            if (userRepository.existsByPhoneNumber(phone)) {
                throw new BusinessException("Phone number '" + phone + "' is already registered", "AUTH_PHONE_EXISTS", HttpStatus.CONFLICT);
            }
        }

        if (StringUtils.hasText(request.getEmail())) {
            String email = request.getEmail().trim();
            if (userRepository.existsByEmail(email)) {
                throw new BusinessException("Email address '" + email + "' is already registered", "AUTH_EMAIL_EXISTS", HttpStatus.CONFLICT);
            }
        }

        String hashedPassword = passwordEncoder.encode(request.getPassword());
        UserRole role = request.getRole() != null ? request.getRole() : UserRole.PATIENT;

        UUID newUserId = UUID.randomUUID();
        String associatedEntityId = request.getAssociatedEntityId();

        if (role == UserRole.PATIENT) {
            if (StringUtils.hasText(associatedEntityId)) {
                try {
                    UUID pUuid = UUID.fromString(associatedEntityId.trim());
                    if (patientRepository.existsById(pUuid)) {
                        log.info("Using provided associatedEntityId [{}] for new patient user [{}]", associatedEntityId, username);
                    }
                } catch (IllegalArgumentException ignored) {
                    associatedEntityId = null;
                }
            }
            if (!StringUtils.hasText(associatedEntityId)) {
                String cleanPhone = StringUtils.hasText(request.getPhoneNumber())
                        ? request.getPhoneNumber().trim()
                        : ("99" + String.format("%08d", Math.abs(username.hashCode() % 100000000)));

                Gender gender = Gender.OTHER;
                if (StringUtils.hasText(request.getGender())) {
                    try {
                        gender = Gender.valueOf(request.getGender().trim().toUpperCase());
                    } catch (Exception ignored) {
                        gender = Gender.OTHER;
                    }
                }

                Patient newPatient = Patient.builder()
                        .id(UUID.randomUUID())
                        .fullName(request.getFullName().trim())
                        .gender(gender)
                        .age(request.getAge() != null ? request.getAge() : 30)
                        .contactNumber(cleanPhone)
                        .villageTown(StringUtils.hasText(request.getVillageTown()) ? request.getVillageTown().trim() : "Aerocity")
                        .subDistrict(StringUtils.hasText(request.getSubDistrict()) ? request.getSubDistrict().trim() : "South West Delhi")
                        .district(StringUtils.hasText(request.getDistrict()) ? request.getDistrict().trim() : "New Delhi")
                        .state(StringUtils.hasText(request.getState()) ? request.getState().trim() : "Delhi")
                        .pincode(StringUtils.hasText(request.getPincode()) ? request.getPincode().trim() : "110037")
                        .preferredLanguage("Hindi")
                        .heightCm(request.getHeightCm())
                        .weightKg(request.getWeightKg())
                        .medicalHistory(StringUtils.hasText(request.getMedicalHistory()) ? request.getMedicalHistory().trim() : null)
                        .build();
                Patient savedPatient = patientRepository.save(newPatient);
                associatedEntityId = savedPatient.getId().toString();
                log.info("Created domain patient [{}] for user [{}]", associatedEntityId, username);
            }
        } else if (role == UserRole.DOCTOR) {
            if (!StringUtils.hasText(associatedEntityId)) {
                try {
                    String doctorServiceUrl = System.getenv().getOrDefault("DOCTOR_SERVICE_URL", "http://localhost:8086");
                    RestClient restClient = RestClient.create();
                    Map<String, Object> docReq = new LinkedHashMap<>();
                    docReq.put("userId", newUserId.toString());
                    docReq.put("name", request.getFullName().trim());
                    docReq.put("specialization", StringUtils.hasText(request.getSpecialization()) ? request.getSpecialization().trim() : "General Medicine");
                    docReq.put("licenseNumber", StringUtils.hasText(request.getLicenseNumber()) ? request.getLicenseNumber().trim() : ("DOC-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase()));
                    docReq.put("experienceYears", request.getExperienceYears() != null ? request.getExperienceYears() : 5);
                    docReq.put("qualification", StringUtils.hasText(request.getQualification()) ? request.getQualification().trim() : "MBBS");
                    docReq.put("hospitalName", StringUtils.hasText(request.getHospitalName()) ? request.getHospitalName().trim() : "Aerocity Demo Hospital");

                    Map resp = restClient.post()
                            .uri(doctorServiceUrl + "/api/doctors")
                            .contentType(MediaType.APPLICATION_JSON)
                            .body(docReq)
                            .retrieve()
                            .body(Map.class);
                    if (resp != null && resp.get("id") != null) {
                        associatedEntityId = String.valueOf(resp.get("id"));
                        log.info("Auto-registered doctor profile in doctor-service with id [{}] for user [{}]", associatedEntityId, username);
                    }
                } catch (Exception ex) {
                    log.warn("Could not auto-register doctor in doctor-service during auth/register: {}", ex.getMessage());
                }
            }
        } else if (role == UserRole.FACILITY_ADMIN || role == UserRole.ADMIN) {
            if (StringUtils.hasText(request.getFacilityId())) {
                associatedEntityId = request.getFacilityId().trim();
            }
        } else if (role == UserRole.CAREGIVER) {
            if (StringUtils.hasText(request.getLinkedPatientPhone())) {
                Optional<Patient> pOpt = patientRepository.findByContactNumber(request.getLinkedPatientPhone().trim());
                if (pOpt.isPresent()) {
                    associatedEntityId = pOpt.get().getId().toString();
                }
            } else if (StringUtils.hasText(request.getLinkedPatientUsername())) {
                Optional<User> uOpt = userRepository.findByUsername(request.getLinkedPatientUsername().trim());
                if (uOpt.isPresent() && StringUtils.hasText(uOpt.get().getAssociatedEntityId())) {
                    associatedEntityId = uOpt.get().getAssociatedEntityId();
                }
            }
        }

        User newUser = User.builder()
                .id(newUserId)
                .username(username)
                .passwordHash(hashedPassword)
                .fullName(request.getFullName().trim())
                .phoneNumber(StringUtils.hasText(request.getPhoneNumber()) ? request.getPhoneNumber().trim() : null)
                .email(StringUtils.hasText(request.getEmail()) ? request.getEmail().trim() : null)
                .role(role)
                .associatedEntityId(associatedEntityId)
                .isActive(true)
                .build();

        User saved = userRepository.save(newUser);
        log.info("User [{}] successfully registered with id [{}] and role [{}] (associatedEntityId: [{}])",
                saved.getUsername(), saved.getId(), saved.getRole(), saved.getAssociatedEntityId());

        String token = jwtTokenProvider.generateToken(saved);
        long expiresInSeconds = jwtTokenProvider.getExpirationMs() / 1000;

        return AuthResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .expiresIn(expiresInSeconds)
                .user(UserDto.fromEntity(saved))
                .build();
    }

    @Override
    public AuthResponse linkEntity(LinkEntityRequest request) {
        UserPrincipal principal = AuthenticationContext.getPrincipal()
                .orElseThrow(() -> new BusinessException("Not authenticated", "AUTH_UNAUTHORIZED", HttpStatus.UNAUTHORIZED));

        User user = userRepository.findById(principal.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User", principal.getUserId()));

        user.setAssociatedEntityId(request.getAssociatedEntityId().trim());
        User saved = userRepository.save(user);

        log.info("Linked entity [{}] to user [{}]", saved.getAssociatedEntityId(), saved.getUsername());

        String token = jwtTokenProvider.generateToken(saved);
        long expiresInSeconds = jwtTokenProvider.getExpirationMs() / 1000;

        return AuthResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .expiresIn(expiresInSeconds)
                .user(UserDto.fromEntity(saved))
                .build();
    }

    @Override
    public UserDto getCurrentUser() {
        UserPrincipal principal = AuthenticationContext.getPrincipal()
                .orElseThrow(() -> new BusinessException("Not authenticated", "AUTH_UNAUTHORIZED", HttpStatus.UNAUTHORIZED));

        User user = userRepository.findById(principal.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User", principal.getUserId()));

        resolvePatientAssociationIfNeeded(user);
        return UserDto.fromEntity(user);
    }

    @Override
    @Transactional
    public AuthResponse authenticateWithGoogle(GoogleAuthRequest request) {
        String idToken = request.getIdToken();
        log.info("Processing Google OAuth2 token verification");

        Map<String, Object> tokenInfo;
        try {
            RestClient restClient = RestClient.create();
            tokenInfo = restClient.get()
                    .uri("https://oauth2.googleapis.com/tokeninfo?id_token=" + idToken)
                    .retrieve()
                    .body(Map.class);
        } catch (Exception ex) {
            log.error("Google token verification failed: {}", ex.getMessage());
            throw new BusinessException("Invalid Google ID token", "AUTH_GOOGLE_TOKEN_INVALID", HttpStatus.UNAUTHORIZED);
        }

        if (tokenInfo == null || tokenInfo.get("sub") == null) {
            throw new BusinessException("Invalid Google token payload", "AUTH_GOOGLE_PAYLOAD_INVALID", HttpStatus.UNAUTHORIZED);
        }

        String sub = String.valueOf(tokenInfo.get("sub"));
        String email = tokenInfo.get("email") != null ? String.valueOf(tokenInfo.get("email")).trim() : null;
        String name = tokenInfo.get("name") != null ? String.valueOf(tokenInfo.get("name")).trim() : null;
        String aud = tokenInfo.get("aud") != null ? String.valueOf(tokenInfo.get("aud")).trim() : null;

        // Verify audience if GOOGLE_WEB_CLIENT_ID is configured
        if (StringUtils.hasText(googleWebClientId) && !googleWebClientId.equals(aud)) {
            log.warn("Google token audience [{}] does not match configured client ID [{}]", aud, googleWebClientId);
            throw new BusinessException("Google token audience mismatch", "AUTH_GOOGLE_AUD_MISMATCH", HttpStatus.UNAUTHORIZED);
        }

        // Look for existing user by googleSub or email
        Optional<User> existingUser = userRepository.findByGoogleSub(sub);
        if (existingUser.isEmpty() && StringUtils.hasText(email)) {
            existingUser = userRepository.findByEmail(email);
        }

        User user;
        if (existingUser.isPresent()) {
            user = existingUser.get();
            if (!user.isActive()) {
                throw new BusinessException("Account is deactivated. Please contact support.", "AUTH_ACCOUNT_DEACTIVATED", HttpStatus.FORBIDDEN);
            }
            user.setGoogleSub(sub);
            user.setAuthProvider("GOOGLE");
            user = userRepository.save(user);
        } else {
            // Auto-provision new user via Google
            String username = email != null ? email.split("@")[0] : ("google_" + sub.substring(0, Math.min(sub.length(), 8)));
            if (userRepository.existsByUsername(username)) {
                username = username + "_" + UUID.randomUUID().toString().substring(0, 4);
            }

            UserRole role = UserRole.PATIENT;
            if (StringUtils.hasText(request.getRole())) {
                try {
                    role = UserRole.valueOf(request.getRole().trim().toUpperCase());
                } catch (IllegalArgumentException ignored) {}
            }

            user = User.builder()
                    .username(username)
                    .email(email)
                    .fullName(StringUtils.hasText(name) ? name : "Google User")
                    .role(role)
                    .authProvider("GOOGLE")
                    .googleSub(sub)
                    .isActive(true)
                    .build();

            user = userRepository.save(user);

            // Auto-create patient record if role is PATIENT
            if (role == UserRole.PATIENT) {
                Patient patient = Patient.builder()
                        .fullName(user.getFullName())
                        .contactNumber(user.getPhoneNumber() != null ? user.getPhoneNumber() : "0000000000")
                        .gender(Gender.OTHER)
                        .age(30)
                        .villageTown("Aerocity")
                        .district("New Delhi")
                        .build();
                patient = patientRepository.save(patient);
                user.setAssociatedEntityId(patient.getId().toString());
                user = userRepository.save(user);
            }
        }

        resolvePatientAssociationIfNeeded(user);

        String token = jwtTokenProvider.generateToken(user);
        long expiresInSeconds = jwtTokenProvider.getExpirationMs() / 1000;

        log.info("Google OAuth user [{}] successfully authenticated with role [{}] and associatedEntityId [{}]",
                user.getUsername(), user.getRole(), user.getAssociatedEntityId());

        return AuthResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .expiresIn(expiresInSeconds)
                .user(UserDto.fromEntity(user))
                .build();
    }

    private void resolvePatientAssociationIfNeeded(User user) {
        if (StringUtils.hasText(user.getAssociatedEntityId())) {
            return;
        }

        if (user.getRole() == UserRole.PATIENT) {
            // 1. Check demo user patient.ramesh
            if ("patient.ramesh".equalsIgnoreCase(user.getUsername())) {
                user.setAssociatedEntityId("b1000000-0000-0000-0000-000000000001");
                userRepository.save(user);
                return;
            }

            // 2. Check if patient exists with user.getId()
            if (patientRepository.existsById(user.getId())) {
                user.setAssociatedEntityId(user.getId().toString());
                userRepository.save(user);
                return;
            }

            // 3. Check if patient exists with user's phone number
            if (StringUtils.hasText(user.getPhoneNumber())) {
                Optional<Patient> pOpt = patientRepository.findByContactNumber(user.getPhoneNumber().trim());
                if (pOpt.isPresent()) {
                    user.setAssociatedEntityId(pOpt.get().getId().toString());
                    userRepository.save(user);
                    return;
                }
            }
        } else if (user.getRole() == UserRole.DOCTOR) {
            if ("doctor.ananya".equalsIgnoreCase(user.getUsername())) {
                user.setAssociatedEntityId("1");
                userRepository.save(user);
                return;
            }
            try {
                String doctorServiceUrl = System.getenv().getOrDefault("DOCTOR_SERVICE_URL", "http://localhost:8086");
                RestClient restClient = RestClient.create();
                Map resp = restClient.get()
                        .uri(doctorServiceUrl + "/api/doctors/user/" + user.getId().toString())
                        .retrieve()
                        .body(Map.class);
                if (resp != null && resp.get("id") != null) {
                    user.setAssociatedEntityId(String.valueOf(resp.get("id")));
                    userRepository.save(user);
                    log.info("Resolved doctor association [{}] for user [{}]", user.getAssociatedEntityId(), user.getUsername());
                }
            } catch (Exception ex) {
                log.debug("No doctor record found in doctor-service for userId [{}]: {}", user.getId(), ex.getMessage());
            }
        } else if (user.getRole() == UserRole.ADMIN || user.getRole() == UserRole.FACILITY_ADMIN) {
            if ("admin".equalsIgnoreCase(user.getUsername())) {
                user.setAssociatedEntityId("f1000000-0000-0000-0000-000000000001");
                userRepository.save(user);
            }
        } else if (user.getRole() == UserRole.CAREGIVER) {
            if ("caregiver.suresh".equalsIgnoreCase(user.getUsername())) {
                user.setAssociatedEntityId("b1000000-0000-0000-0000-000000000001");
                userRepository.save(user);
            }
        }
    }

    @Override
    public Object processInternalGoogleAuth(com.ruralhealthcare.auth.dto.InternalGoogleAuthRequest request) {
        String email = request.getEmail() != null ? request.getEmail().trim() : null;
        String googleSub = request.getGoogleSub() != null ? request.getGoogleSub().trim() : null;
        String fullName = StringUtils.hasText(request.getFullName()) ? request.getFullName().trim() : (email != null ? email.split("@")[0] : "Google User");

        // 1. Look for existing user by googleSub or email
        Optional<User> existingUser = Optional.empty();
        if (StringUtils.hasText(googleSub)) {
            existingUser = userRepository.findByGoogleSub(googleSub);
        }
        if (existingUser.isEmpty() && StringUtils.hasText(email)) {
            existingUser = userRepository.findByEmail(email);
        }

        if (existingUser.isPresent()) {
            User user = existingUser.get();
            if (!user.isActive()) {
                throw new BusinessException("User account is disabled", "AUTH_USER_DISABLED", HttpStatus.FORBIDDEN);
            }
            if (!StringUtils.hasText(user.getGoogleSub()) && StringUtils.hasText(googleSub)) {
                user.setGoogleSub(googleSub);
                user.setAuthProvider("GOOGLE");
                user = userRepository.save(user);
            }
            resolvePatientAssociationIfNeeded(user);
            String token = jwtTokenProvider.generateToken(user);
            long expiresInSeconds = jwtTokenProvider.getExpirationMs() / 1000;
            return AuthResponse.builder()
                    .token(token)
                    .tokenType("Bearer")
                    .expiresIn(expiresInSeconds)
                    .user(UserDto.fromEntity(user))
                    .build();
        }

        // 2. New user: If role is not provided, registration is required
        if (!StringUtils.hasText(request.getRole())) {
            Map<String, Object> regReq = new HashMap<>();
            regReq.put("registrationRequired", true);
            regReq.put("email", email);
            regReq.put("fullName", fullName);
            return regReq;
        }

        // 3. New user with role and profile fields: Create User + Domain record
        UserRole userRole;
        try {
            userRole = UserRole.valueOf(request.getRole().trim().toUpperCase());
        } catch (Exception ex) {
            userRole = UserRole.PATIENT;
        }

        String username = email != null ? email.toLowerCase() : ("google_" + googleSub);
        if (userRepository.existsByUsername(username)) {
            username = username + "_" + UUID.randomUUID().toString().substring(0, 4);
        }

        String associatedEntityId = null;
        if (userRole == UserRole.PATIENT) {
            Gender gender = Gender.OTHER;
            if (StringUtils.hasText(request.getGender())) {
                try {
                    gender = Gender.valueOf(request.getGender().trim().toUpperCase());
                } catch (Exception ignored) {
                }
            }
            String phone = StringUtils.hasText(request.getPhoneNumber()) ? request.getPhoneNumber().trim() : null;
            Patient newPatient = Patient.builder()
                    .id(UUID.randomUUID())
                    .fullName(fullName)
                    .gender(gender)
                    .age(request.getAge() != null ? request.getAge() : 30)
                    .contactNumber(phone != null ? phone : ("99" + String.format("%08d", Math.abs(username.hashCode() % 100000000))))
                    .villageTown(StringUtils.hasText(request.getVillageTown()) ? request.getVillageTown().trim() : "Aerocity")
                    .subDistrict(StringUtils.hasText(request.getSubDistrict()) ? request.getSubDistrict().trim() : "South West Delhi")
                    .district(StringUtils.hasText(request.getDistrict()) ? request.getDistrict().trim() : "New Delhi")
                    .state(StringUtils.hasText(request.getState()) ? request.getState().trim() : "Delhi")
                    .pincode(StringUtils.hasText(request.getPincode()) ? request.getPincode().trim() : "110037")
                    .preferredLanguage("Hindi")
                    .heightCm(request.getHeightCm())
                    .weightKg(request.getWeightKg())
                    .medicalHistory(StringUtils.hasText(request.getMedicalHistory()) ? request.getMedicalHistory().trim() : null)
                    .build();
            Patient savedPatient = patientRepository.save(newPatient);
            associatedEntityId = savedPatient.getId().toString();
        }

        User newUser = User.builder()
                .id(UUID.randomUUID())
                .username(username)
                .passwordHash(null)
                .fullName(fullName)
                .email(email)
                .phoneNumber(StringUtils.hasText(request.getPhoneNumber()) ? request.getPhoneNumber().trim() : null)
                .role(userRole)
                .authProvider("GOOGLE")
                .googleSub(googleSub)
                .associatedEntityId(associatedEntityId)
                .isActive(true)
                .build();

        User savedUser = userRepository.save(newUser);
        resolvePatientAssociationIfNeeded(savedUser);

        String token = jwtTokenProvider.generateToken(savedUser);
        long expiresInSeconds = jwtTokenProvider.getExpirationMs() / 1000;

        return AuthResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .expiresIn(expiresInSeconds)
                .user(UserDto.fromEntity(savedUser))
                .build();
    }
}
