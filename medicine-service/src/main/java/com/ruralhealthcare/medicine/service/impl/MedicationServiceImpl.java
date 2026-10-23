package com.ruralhealthcare.medicine.service.impl;

import com.ruralhealthcare.medicine.domain.*;
import com.ruralhealthcare.medicine.dto.CreateMedicationRequest;
import com.ruralhealthcare.medicine.dto.MedicationResponse;
import com.ruralhealthcare.medicine.dto.UpdateMedicationRequest;
import com.ruralhealthcare.medicine.exception.MedicationValidationException;
import com.ruralhealthcare.medicine.exception.ResourceNotFoundException;
import com.ruralhealthcare.medicine.repository.DoseEventRepository;
import com.ruralhealthcare.medicine.repository.MedicationReminderTimeRepository;
import com.ruralhealthcare.medicine.repository.MedicationRepository;
import com.ruralhealthcare.medicine.security.SecurityUtils;
import com.ruralhealthcare.medicine.service.MedicationService;
import com.ruralhealthcare.medicine.service.StorageService;
import com.ruralhealthcare.medicine.util.ImageValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class MedicationServiceImpl implements MedicationService {

    private static final Logger log = LoggerFactory.getLogger(MedicationServiceImpl.class);

    private final MedicationRepository medicationRepository;
    private final MedicationReminderTimeRepository reminderTimeRepository;
    private final DoseEventRepository doseEventRepository;
    private final StorageService storageService;
    private final SecurityUtils securityUtils;

    public MedicationServiceImpl(MedicationRepository medicationRepository,
                                 MedicationReminderTimeRepository reminderTimeRepository,
                                 DoseEventRepository doseEventRepository,
                                 StorageService storageService,
                                 SecurityUtils securityUtils) {
        this.medicationRepository = medicationRepository;
        this.reminderTimeRepository = reminderTimeRepository;
        this.doseEventRepository = doseEventRepository;
        this.storageService = storageService;
        this.securityUtils = securityUtils;
    }

    @Override
    public MedicationResponse createMedication(CreateMedicationRequest request, MultipartFile image) {
        validateCreateRequest(request, image);

        // Security check: caller must have access to patient
        securityUtils.assertPatientAccess(request.getPatientId());

        // Resolve medication name and image key across creation modes
        String imageKey = null;
        if (image != null && !image.isEmpty()) {
            ImageValidator.validate(image);
            imageKey = storageService.storeImage(image, request.getPatientId());
            log.info("Stored medicine photo with key [{}] for patient [{}]", imageKey, request.getPatientId());
        }

        String resolvedName = resolveMedicationName(request, imageKey != null);

        // 2. Create Medication entity
        FoodInstruction foodInstruction = request.getFoodInstruction() != null ? request.getFoodInstruction() : FoodInstruction.NONE;
        Medication medication = Medication.builder()
                .patientId(request.getPatientId())
                .name(resolvedName)
                .extractedName(request.getExtractedName())
                .imageKey(imageKey)
                .dosageAmount(request.getDosageAmount())
                .dosageUnit(request.getDosageUnit())
                .timesPerDay(request.getTimesPerDay())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .status(MedicationStatus.ACTIVE)
                .foodInstruction(foodInstruction)
                .instructions(request.getInstructions())
                .build();

        // 3. Add Reminder Times
        for (LocalTime time : request.getReminderTimes()) {
            MedicationReminderTime reminderTime = MedicationReminderTime.builder()
                    .medication(medication)
                    .reminderTime(time)
                    .build();
            medication.addReminderTime(reminderTime);
        }

        Medication saved = medicationRepository.save(medication);
        log.info("Persisted new medication reminder [{}] ('{}') with {} daily reminder time(s)",
                saved.getId(), saved.getName(), saved.getReminderTimes().size());

        // 4. If today falls within the active schedule, generate today's dose events immediately
        LocalDate today = LocalDate.now();
        if (!today.isBefore(saved.getStartDate()) && (saved.getEndDate() == null || !today.isAfter(saved.getEndDate()))) {
            generateDoseEventsForMedicationOnDate(saved, today);
        }

        return mapToResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public MedicationResponse getMedicationById(UUID id) {
        Medication medication = medicationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Medication reminder not found with id: " + id));
        securityUtils.assertPatientAccess(medication.getPatientId());
        return mapToResponse(medication);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MedicationResponse> getMedicationsByPatientId(UUID patientId) {
        securityUtils.assertPatientAccess(patientId);
        List<Medication> medications = medicationRepository.findByPatientId(patientId);
        return medications.stream()
                .filter(m -> m.getStatus() != MedicationStatus.DEACTIVATED)
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public MedicationResponse updateMedication(UUID id, UpdateMedicationRequest request) {
        Medication medication = medicationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Medication reminder not found with id: " + id));

        securityUtils.assertPatientAccess(medication.getPatientId());

        if (request.getName() != null && !request.getName().isBlank()) {
            medication.setName(request.getName().trim());
        }
        if (request.getDosageAmount() != null) {
            medication.setDosageAmount(request.getDosageAmount());
        }
        if (request.getDosageUnit() != null) {
            medication.setDosageUnit(request.getDosageUnit());
        }
        if (request.getTimesPerDay() != null) {
            medication.setTimesPerDay(request.getTimesPerDay());
        }
        if (request.getEndDate() != null) {
            medication.setEndDate(request.getEndDate());
        }
        if (request.getFoodInstruction() != null) {
            medication.setFoodInstruction(request.getFoodInstruction());
        }
        if (request.getInstructions() != null) {
            medication.setInstructions(request.getInstructions());
        }
        if (request.getStatus() != null) {
            medication.setStatus(request.getStatus());
        }

        if (request.getReminderTimes() != null && !request.getReminderTimes().isEmpty()) {
            medication.getReminderTimes().clear();
            for (LocalTime time : request.getReminderTimes()) {
                MedicationReminderTime reminderTime = MedicationReminderTime.builder()
                        .medication(medication)
                        .reminderTime(time)
                        .build();
                medication.addReminderTime(reminderTime);
            }
        }

        Medication updated = medicationRepository.save(medication);
        log.info("Updated medication reminder [{}]", updated.getId());
        return mapToResponse(updated);
    }

    @Override
    public MedicationResponse completeMedication(UUID id) {
        Medication medication = medicationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Medication reminder not found with id: " + id));
        securityUtils.assertPatientAccess(medication.getPatientId());
        medication.setStatus(MedicationStatus.COMPLETED);
        Medication updated = medicationRepository.save(medication);
        log.info("Completed medication reminder course [{}] for patient [{}]", id, medication.getPatientId());
        return mapToResponse(updated);
    }

    @Override
    public MedicationResponse discontinueMedication(UUID id) {
        Medication medication = medicationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Medication reminder not found with id: " + id));
        securityUtils.assertPatientAccess(medication.getPatientId());
        medication.setStatus(MedicationStatus.DISCONTINUED);
        Medication updated = medicationRepository.save(medication);
        log.info("Discontinued medication reminder [{}] for patient [{}]", id, medication.getPatientId());
        return mapToResponse(updated);
    }

    @Override
    public void deactivateMedication(UUID id) {
        Medication medication = medicationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Medication reminder not found with id: " + id));
        securityUtils.assertPatientAccess(medication.getPatientId());
        medication.setStatus(MedicationStatus.DEACTIVATED);
        medicationRepository.save(medication);

        // Remove future pending dose events for this deactivated medication
        try {
            List<DoseEvent> doses = doseEventRepository.findByMedicationIdOrderByScheduledDateAscScheduledTimeAsc(id);
            for (DoseEvent d : doses) {
                if (d.getStatus() == DoseStatus.PENDING) {
                    doseEventRepository.delete(d);
                }
            }
        } catch (Exception e) {
            log.warn("Could not delete pending doses for deactivated medication [{}]: {}", id, e.getMessage());
        }

        log.info("Deactivated medication reminder [{}] and cleared pending doses", id);
    }

    private void generateDoseEventsForMedicationOnDate(Medication medication, LocalDate date) {
        for (MedicationReminderTime time : medication.getReminderTimes()) {
            List<DoseEvent> existing = doseEventRepository.findByMedicationIdAndScheduledDateAndScheduledTime(
                    medication.getId(), date, time.getReminderTime());

            if (existing.isEmpty()) {
                DoseEvent event = DoseEvent.builder()
                        .medication(medication)
                        .patientId(medication.getPatientId())
                        .scheduledDate(date)
                        .scheduledTime(time.getReminderTime())
                        .status(DoseStatus.PENDING)
                        .notificationTriggered(false)
                        .build();
                doseEventRepository.save(event);
                log.info("Generated dose event for medication [{}] at {} {}", medication.getId(), date, time.getReminderTime());
            }
        }
    }

    private String resolveMedicationName(CreateMedicationRequest request, boolean hasImage) {
        if (request.getName() != null && !request.getName().isBlank()) {
            return request.getName().trim();
        }
        if (hasImage) {
            // Legacy backward compatibility fallback when image was provided but name field omitted
            return "Prescribed Medicine";
        }
        throw new MedicationValidationException("Medicine photograph is required when medication name is not specified.");
    }

    private void validateCreateRequest(CreateMedicationRequest request, MultipartFile image) {
        if (request == null) {
            throw new MedicationValidationException("Request payload cannot be null");
        }
        if (request.getPatientId() == null) {
            throw new MedicationValidationException("patientId is required");
        }
        boolean hasImage = image != null && !image.isEmpty();
        boolean hasName = request.getName() != null && !request.getName().isBlank();

        // Either image or name must be provided
        if (!hasImage && !hasName) {
            throw new MedicationValidationException("Medicine photograph is required. Please upload a clear photo of the medicine.");
        }
        if (request.getDosageAmount() == null || request.getDosageAmount().doubleValue() <= 0) {
            throw new MedicationValidationException("dosageAmount must be greater than zero");
        }
        if (request.getDosageUnit() == null) {
            throw new MedicationValidationException("dosageUnit is required");
        }
        if (request.getTimesPerDay() == null || request.getTimesPerDay() < 1) {
            throw new MedicationValidationException("timesPerDay must be at least 1");
        }
        if (request.getReminderTimes() == null || request.getReminderTimes().isEmpty()) {
            throw new MedicationValidationException("At least one reminder time must be configured");
        }
        if (request.getStartDate() == null) {
            throw new MedicationValidationException("startDate is required");
        }
        if (request.getEndDate() != null && request.getEndDate().isBefore(request.getStartDate())) {
            throw new MedicationValidationException("endDate cannot be before startDate");
        }
    }

    private MedicationResponse mapToResponse(Medication medication) {
        String presignedUrl = (medication.getImageKey() != null)
                ? storageService.getPresignedImageUrl(medication.getImageKey())
                : null;
        List<LocalTime> times = medication.getReminderTimes().stream()
                .map(MedicationReminderTime::getReminderTime)
                .sorted()
                .collect(Collectors.toList());

        return MedicationResponse.builder()
                .id(medication.getId())
                .patientId(medication.getPatientId())
                .name(medication.getName())
                .extractedName(medication.getExtractedName())
                .imageKey(medication.getImageKey())
                .imageUrl(presignedUrl)
                .dosageAmount(medication.getDosageAmount())
                .dosageUnit(medication.getDosageUnit())
                .timesPerDay(medication.getTimesPerDay())
                .reminderTimes(times)
                .startDate(medication.getStartDate())
                .endDate(medication.getEndDate())
                .status(medication.getStatus())
                .foodInstruction(medication.getFoodInstruction())
                .instructions(medication.getInstructions())
                .createdAt(medication.getCreatedAt())
                .updatedAt(medication.getUpdatedAt())
                .build();
    }
}
