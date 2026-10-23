package com.ruralhealthcare.patient.service;

import com.ruralhealthcare.auth.domain.User;
import com.ruralhealthcare.auth.domain.UserRole;
import com.ruralhealthcare.auth.dto.AuthResponse;
import com.ruralhealthcare.auth.dto.LoginRequest;
import com.ruralhealthcare.auth.dto.RegisterRequest;
import com.ruralhealthcare.auth.repository.UserRepository;
import com.ruralhealthcare.auth.security.JwtTokenProvider;
import com.ruralhealthcare.auth.service.impl.AuthServiceImpl;
import com.ruralhealthcare.common.exception.ResourceNotFoundException;
import com.ruralhealthcare.patient.api.dto.CreateCaseRequest;
import com.ruralhealthcare.patient.api.dto.HealthcareCaseDto;
import com.ruralhealthcare.patient.api.dto.PatientDto;
import com.ruralhealthcare.patient.domain.Gender;
import com.ruralhealthcare.patient.domain.HealthcareCase;
import com.ruralhealthcare.patient.domain.IntakeChannel;
import com.ruralhealthcare.patient.domain.Patient;
import com.ruralhealthcare.patient.repository.CaseSymptomRepository;
import com.ruralhealthcare.patient.repository.HealthcareCaseRepository;
import com.ruralhealthcare.patient.repository.PatientRepository;
import com.ruralhealthcare.patient.service.impl.HealthcareCaseServiceImpl;
import com.ruralhealthcare.patient.service.impl.PatientServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PatientIdentityRegressionTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PatientRepository patientRepository;

    @Mock
    private HealthcareCaseRepository caseRepository;

    @Mock
    private CaseSymptomRepository symptomRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    private AuthServiceImpl authService;
    private PatientServiceImpl patientService;
    private HealthcareCaseServiceImpl caseService;

    // User 1: Chanchal Sharma
    private UUID chanchalUserId;
    private UUID chanchalPatientId;
    private User chanchalUser;
    private Patient chanchalPatient;

    // User 2: Ramesh Patil
    private UUID rameshUserId;
    private UUID rameshPatientId;
    private User rameshUser;
    private Patient rameshPatient;

    @BeforeEach
    void setUp() {
        authService = new AuthServiceImpl(userRepository, patientRepository, passwordEncoder, jwtTokenProvider);
        patientService = new PatientServiceImpl(patientRepository, caseRepository, userRepository);
        caseService = new HealthcareCaseServiceImpl(caseRepository, patientRepository, symptomRepository);

        chanchalUserId = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
        chanchalPatientId = UUID.fromString("11111111-aaaa-bbbb-cccc-111111111111");

        chanchalUser = User.builder()
                .id(chanchalUserId)
                .username("chanchal.sharma")
                .fullName("Chanchal Sharma")
                .phoneNumber("9822019999")
                .email("chanchal@example.org")
                .passwordHash("hashedPass123")
                .role(UserRole.PATIENT)
                .associatedEntityId(chanchalPatientId.toString())
                .isActive(true)
                .build();

        chanchalPatient = Patient.builder()
                .id(chanchalPatientId)
                .fullName("Chanchal Sharma")
                .gender(Gender.FEMALE)
                .contactNumber("9822019999")
                .villageTown("Indapur")
                .subDistrict("Indapur")
                .district("Pune")
                .state("Maharashtra")
                .pincode("413132")
                .preferredLanguage("Hindi")
                .build();

        rameshUserId = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
        rameshPatientId = UUID.fromString("b1000000-0000-0000-0000-000000000001");

        rameshUser = User.builder()
                .id(rameshUserId)
                .username("patient.ramesh")
                .fullName("Ramesh Patil")
                .phoneNumber("9822012345")
                .email("ramesh.patil@ruralhealth.org")
                .passwordHash("hashedPass123")
                .role(UserRole.PATIENT)
                .associatedEntityId(rameshPatientId.toString())
                .isActive(true)
                .build();

        rameshPatient = Patient.builder()
                .id(rameshPatientId)
                .fullName("Ramesh Patil")
                .gender(Gender.MALE)
                .contactNumber("9822012345")
                .villageTown("Palasdeo")
                .subDistrict("Indapur")
                .district("Pune")
                .state("Maharashtra")
                .pincode("413132")
                .preferredLanguage("Marathi")
                .build();
    }

    @Test
    @DisplayName("Regression: User 1 (Chanchal Sharma) sees own profile and creates case with own patient ID")
    void testUser1ChanchalProfileAndCaseCreation() {
        when(userRepository.findByUsername("chanchal.sharma")).thenReturn(Optional.of(chanchalUser));
        when(passwordEncoder.matches("Password@123", "hashedPass123")).thenReturn(true);
        when(patientRepository.existsById(chanchalPatientId)).thenReturn(true);
        when(jwtTokenProvider.generateToken(any(User.class))).thenReturn("jwt.token.chanchal");
        when(jwtTokenProvider.getExpirationMs()).thenReturn(86400000L);

        // 1. Authenticate Chanchal Sharma
        AuthResponse authResponse = authService.login(new LoginRequest("chanchal.sharma", "Password@123"));
        assertNotNull(authResponse);
        assertEquals("Chanchal Sharma", authResponse.getUser().getFullName());
        assertEquals(chanchalPatientId.toString(), authResponse.getUser().getAssociatedEntityId());
        assertNotEquals(rameshPatientId.toString(), authResponse.getUser().getAssociatedEntityId(),
                "Identity mismatch: Chanchal Sharma must not receive Ramesh Patil's patient ID");

        // 2. Fetch patient profile for Step 1
        when(patientRepository.findById(chanchalPatientId)).thenReturn(Optional.of(chanchalPatient));
        Optional<PatientDto> patientDtoOpt = patientService.getPatientById(UUID.fromString(authResponse.getUser().getAssociatedEntityId()));
        assertTrue(patientDtoOpt.isPresent());
        PatientDto patientDto = patientDtoOpt.get();
        assertEquals("Chanchal Sharma", patientDto.getFullName());
        assertEquals("9822019999", patientDto.getContactNumber());
        assertEquals(chanchalPatientId, patientDto.getId());

        // 3. Create case for Chanchal Sharma in Step 2 -> Step 3
        CreateCaseRequest caseRequest = CreateCaseRequest.builder()
                .patientId(chanchalPatientId)
                .chiefComplaint("Fever and severe cough")
                .intakeChannel(IntakeChannel.FORM)
                .latitude(18.2562)
                .longitude(74.9048)
                .build();

        when(caseRepository.save(any(HealthcareCase.class))).thenAnswer(invocation -> invocation.getArgument(0));
        HealthcareCaseDto createdCase = caseService.createCase(caseRequest);

        assertNotNull(createdCase);
        assertEquals(chanchalPatientId, createdCase.getPatientId(),
                "Case must be registered under Chanchal Sharma's patient ID");
        assertNotEquals(rameshPatientId, createdCase.getPatientId(),
                "Case must NOT be assigned to Ramesh Patil");
        assertEquals("Fever and severe cough", createdCase.getChiefComplaint());
    }

    @Test
    @DisplayName("Regression: User 2 (Ramesh Patil) sees own profile and creates case with own patient ID")
    void testUser2RameshProfileAndCaseCreation() {
        when(userRepository.findByUsername("patient.ramesh")).thenReturn(Optional.of(rameshUser));
        when(passwordEncoder.matches("Password@123", "hashedPass123")).thenReturn(true);
        when(patientRepository.existsById(rameshPatientId)).thenReturn(true);
        when(jwtTokenProvider.generateToken(any(User.class))).thenReturn("jwt.token.ramesh");
        when(jwtTokenProvider.getExpirationMs()).thenReturn(86400000L);

        // 1. Authenticate Ramesh Patil
        AuthResponse authResponse = authService.login(new LoginRequest("patient.ramesh", "Password@123"));
        assertNotNull(authResponse);
        assertEquals("Ramesh Patil", authResponse.getUser().getFullName());
        assertEquals(rameshPatientId.toString(), authResponse.getUser().getAssociatedEntityId());

        // 2. Fetch patient profile for Step 1
        when(patientRepository.findById(rameshPatientId)).thenReturn(Optional.of(rameshPatient));
        Optional<PatientDto> patientDtoOpt = patientService.getPatientById(UUID.fromString(authResponse.getUser().getAssociatedEntityId()));
        assertTrue(patientDtoOpt.isPresent());
        PatientDto patientDto = patientDtoOpt.get();
        assertEquals("Ramesh Patil", patientDto.getFullName());
        assertEquals("9822012345", patientDto.getContactNumber());
        assertEquals(rameshPatientId, patientDto.getId());

        // 3. Create case for Ramesh Patil
        CreateCaseRequest caseRequest = CreateCaseRequest.builder()
                .patientId(rameshPatientId)
                .chiefComplaint("Chest tightness and shortness of breath")
                .intakeChannel(IntakeChannel.FORM)
                .latitude(18.2562)
                .longitude(74.9048)
                .build();

        when(caseRepository.save(any(HealthcareCase.class))).thenAnswer(invocation -> invocation.getArgument(0));
        HealthcareCaseDto createdCase = caseService.createCase(caseRequest);

        assertNotNull(createdCase);
        assertEquals(rameshPatientId, createdCase.getPatientId());
        assertEquals("Chest tightness and shortness of breath", createdCase.getChiefComplaint());
    }

    @Test
    @DisplayName("Regression: Missing patient profile returns 404 rather than silently returning another patient")
    void testMissingPatientProfileReturnsEmpty() {
        UUID unknownPatientId = UUID.fromString("99999999-9999-9999-9999-999999999999");
        when(patientRepository.findById(unknownPatientId)).thenReturn(Optional.empty());

        Optional<PatientDto> result = patientService.getPatientById(unknownPatientId);

        assertTrue(result.isEmpty(), "Missing patient ID must return empty Optional rather than fallback to demo patient");

        // Attempting to create a case with missing patient must throw ResourceNotFoundException
        CreateCaseRequest invalidCaseRequest = CreateCaseRequest.builder()
                .patientId(unknownPatientId)
                .chiefComplaint("Headache")
                .intakeChannel(IntakeChannel.FORM)
                .build();

        assertThrows(ResourceNotFoundException.class, () -> caseService.createCase(invalidCaseRequest),
                "Creating a case with non-existent patient ID must fail with ResourceNotFoundException");
    }

    @Test
    @DisplayName("Regression: Auto-creation of patient profile on user registration preserves identity")
    void testRegistrationAutoCreatesAndLinksPatientProfile() {
        RegisterRequest registerRequest = new RegisterRequest(
                "chanchal.new",
                "Password@123",
                "Chanchal Sharma",
                "9822018888",
                "chanchal.new@example.org",
                UserRole.PATIENT,
                null
        );

        when(userRepository.existsByUsername("chanchal.new")).thenReturn(false);
        when(userRepository.existsByPhoneNumber("9822018888")).thenReturn(false);
        when(patientRepository.findByContactNumber("9822018888")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("Password@123")).thenReturn("encodedNewPass");

        UUID generatedPatientId = UUID.randomUUID();
        when(patientRepository.save(any(Patient.class))).thenAnswer(invocation -> {
            Patient p = invocation.getArgument(0);
            return Patient.builder()
                    .id(generatedPatientId)
                    .fullName(p.getFullName())
                    .gender(p.getGender())
                    .contactNumber(p.getContactNumber())
                    .villageTown(p.getVillageTown())
                    .subDistrict(p.getSubDistrict())
                    .district(p.getDistrict())
                    .state(p.getState())
                    .pincode(p.getPincode())
                    .preferredLanguage(p.getPreferredLanguage())
                    .build();
        });

        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(jwtTokenProvider.generateToken(any(User.class))).thenReturn("jwt.token.new");
        when(jwtTokenProvider.getExpirationMs()).thenReturn(86400000L);

        AuthResponse res = authService.register(registerRequest);

        assertNotNull(res);
        assertNotNull(res.getUser());
        assertEquals("Chanchal Sharma", res.getUser().getFullName());
        assertEquals(generatedPatientId, res.getUser().getAssociatedEntityId(),
                "Newly registered user must be linked to the newly created patient profile");
        assertNotEquals(rameshPatientId, res.getUser().getAssociatedEntityId(),
                "Newly registered patient must never be linked to Ramesh Patil");
    }
}
