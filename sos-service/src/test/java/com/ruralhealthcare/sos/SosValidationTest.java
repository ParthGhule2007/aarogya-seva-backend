package com.ruralhealthcare.sos;

import com.ruralhealthcare.sos.domain.TargetType;
import com.ruralhealthcare.sos.dto.CreateSosRequest;
import com.ruralhealthcare.sos.exception.SosValidationException;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SosValidationTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    @DisplayName("Test 8: Reject empty targets list")
    void testRejectEmptyTargets() {
        CreateSosRequest request = new CreateSosRequest();
        request.setPatientId(UUID.randomUUID());
        request.setLatitude(26.6811);
        request.setLongitude(80.9984);
        request.setTargets(new ArrayList<>());

        assertFalse(validator.validate(request).isEmpty());
        assertThrows(SosValidationException.class, request::normalizeAndValidate);
    }

    @Test
    @DisplayName("Test 9: Reject FACILITY target when facilityId is missing")
    void testRejectFacilityWithoutFacilityId() {
        CreateSosRequest request = new CreateSosRequest();
        request.setPatientId(UUID.randomUUID());
        request.setLatitude(26.6811);
        request.setLongitude(80.9984);
        request.setTargets(List.of(TargetType.FACILITY));
        request.setFacilityId(null);

        SosValidationException ex = assertThrows(SosValidationException.class, request::normalizeAndValidate);
        assertTrue(ex.getMessage().contains("facilityId is required when FACILITY target is selected"));
    }

    @Test
    @DisplayName("FacilityId is ignored and valid when FACILITY is not selected")
    void testFacilityIdIgnoredWhenFacilityNotSelected() {
        CreateSosRequest request = new CreateSosRequest();
        request.setPatientId(UUID.randomUUID());
        request.setLatitude(26.6811);
        request.setLongitude(80.9984);
        request.setTargets(List.of(TargetType.CAREGIVER));
        request.setFacilityId(UUID.randomUUID());

        assertDoesNotThrow(request::normalizeAndValidate);
    }

    @Test
    @DisplayName("Reject invalid latitude range outside [-90, 90]")
    void testRejectInvalidLatitude() {
        CreateSosRequest request = new CreateSosRequest();
        request.setPatientId(UUID.randomUUID());
        request.setLatitude(95.0); // Invalid
        request.setLongitude(80.9984);
        request.setTargets(List.of(TargetType.CAREGIVER));

        assertFalse(validator.validate(request).isEmpty());
        assertThrows(SosValidationException.class, request::normalizeAndValidate);
    }

    @Test
    @DisplayName("Reject invalid longitude range outside [-180, 180]")
    void testRejectInvalidLongitude() {
        CreateSosRequest request = new CreateSosRequest();
        request.setPatientId(UUID.randomUUID());
        request.setLatitude(26.6811);
        request.setLongitude(195.0); // Invalid
        request.setTargets(List.of(TargetType.CAREGIVER));

        assertFalse(validator.validate(request).isEmpty());
        assertThrows(SosValidationException.class, request::normalizeAndValidate);
    }

    @Test
    @DisplayName("Normalize duplicate targets in list")
    void testNormalizeDuplicateTargets() {
        CreateSosRequest request = new CreateSosRequest();
        request.setPatientId(UUID.randomUUID());
        request.setLatitude(26.6811);
        request.setLongitude(80.9984);
        request.setTargets(List.of(TargetType.CAREGIVER, TargetType.CAREGIVER, TargetType.CAREGIVER));

        request.normalizeAndValidate();
        assertEquals(1, request.getTargets().size());
        assertEquals(TargetType.CAREGIVER, request.getTargets().get(0));
    }
}
