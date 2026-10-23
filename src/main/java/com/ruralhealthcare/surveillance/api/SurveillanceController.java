package com.ruralhealthcare.surveillance.api;

import com.ruralhealthcare.ai.domain.AiPredictionResult;
import com.ruralhealthcare.ai.domain.RiskLevel;
import com.ruralhealthcare.ai.dto.DiseasePrediction;
import com.ruralhealthcare.ai.repository.AiPredictionRepository;
import com.ruralhealthcare.common.dto.ApiResponse;
import com.ruralhealthcare.facility.domain.HealthcareFacility;
import com.ruralhealthcare.facility.repository.HealthcareFacilityRepository;
import com.ruralhealthcare.patient.domain.CasePriority;
import com.ruralhealthcare.patient.domain.CaseStatus;
import com.ruralhealthcare.patient.domain.CaseSymptom;
import com.ruralhealthcare.patient.domain.HealthcareCase;
import com.ruralhealthcare.patient.domain.Patient;
import com.ruralhealthcare.patient.repository.HealthcareCaseRepository;
import com.ruralhealthcare.patient.repository.PatientRepository;
import com.ruralhealthcare.symptom.domain.Symptom;
import com.ruralhealthcare.symptom.repository.SymptomRepository;
import com.ruralhealthcare.surveillance.dto.OutbreakZoneDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/surveillance")
@Tag(name = "Epidemiological Surveillance & Outbreak Zones", description = "Real-time aggregation of clinical cases, symptoms, and AI risk assessments into RED, YELLOW, and GREEN outbreak zones")
public class SurveillanceController {

    private final HealthcareCaseRepository caseRepository;
    private final PatientRepository patientRepository;
    private final AiPredictionRepository aiPredictionRepository;
    private final HealthcareFacilityRepository facilityRepository;
    private final SymptomRepository symptomRepository;

    public SurveillanceController(
            HealthcareCaseRepository caseRepository,
            PatientRepository patientRepository,
            AiPredictionRepository aiPredictionRepository,
            HealthcareFacilityRepository facilityRepository,
            SymptomRepository symptomRepository) {
        this.caseRepository = caseRepository;
        this.patientRepository = patientRepository;
        this.aiPredictionRepository = aiPredictionRepository;
        this.facilityRepository = facilityRepository;
        this.symptomRepository = symptomRepository;
    }

    @Operation(
            summary = "Get real-time outbreak risk zones (RED / YELLOW / GREEN)",
            description = "Aggregates real PostgreSQL healthcare_cases, symptoms, and ai_predictions by rural area/village/district to classify areas into RED, YELLOW, or GREEN outbreak risk zones."
    )
    @GetMapping("/outbreak-zones")
    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse<List<OutbreakZoneDto>>> getOutbreakZones(
            @RequestParam(required = false) Double latitude,
            @RequestParam(required = false) Double longitude
    ) {
        List<Patient> allPatients = patientRepository.findAll();
        List<HealthcareCase> allCases = caseRepository.findAll();
        List<AiPredictionResult> allPredictions = aiPredictionRepository.findAll();
        List<HealthcareFacility> allFacilities = facilityRepository.findAll();
        List<Symptom> allModuleSymptoms = symptomRepository.findAll();

        // Map latest AI prediction per caseId
        Map<UUID, AiPredictionResult> latestPredictionByCase = new HashMap<>();
        for (AiPredictionResult pred : allPredictions) {
            UUID cid = pred.getCaseId();
            AiPredictionResult existing = latestPredictionByCase.get(cid);
            if (existing == null || (pred.getCreatedAt() != null && existing.getCreatedAt() != null
                    && pred.getCreatedAt().isAfter(existing.getCreatedAt()))) {
                latestPredictionByCase.put(cid, pred);
            }
        }

        // Map symptoms from standalone symptoms table by caseId
        Map<UUID, List<Symptom>> symptomsByCase = new HashMap<>();
        for (Symptom s : allModuleSymptoms) {
            symptomsByCase.computeIfAbsent(s.getCaseId(), k -> new ArrayList<>()).add(s);
        }

        // Group areas by normalized key (villageTown + "|" + district)
        Map<String, AreaBucket> buckets = new LinkedHashMap<>();

        // 1. Seed areas from registered patients
        Map<UUID, String> patientAreaKey = new HashMap<>();
        for (Patient p : allPatients) {
            String village = p.getVillageTown() != null && !p.getVillageTown().isBlank() ? p.getVillageTown().trim() : "Rural Sector";
            String district = p.getDistrict() != null && !p.getDistrict().isBlank() ? p.getDistrict().trim() : "District";
            String state = p.getState() != null && !p.getState().isBlank() ? p.getState().trim() : "Maharashtra";
            String key = (village + "|" + district).toLowerCase(Locale.ROOT);
            patientAreaKey.put(p.getId(), key);

            AreaBucket bucket = buckets.computeIfAbsent(key, k -> new AreaBucket(village, district, state));
            if (p.getGeoLocation() != null && p.getGeoLocation().getLatitude() != null && p.getGeoLocation().getLongitude() != null) {
                bucket.addCoordinate(p.getGeoLocation().getLatitude(), p.getGeoLocation().getLongitude());
            }
        }

        // 2. Seed areas from registered facilities so monitored PHC/CHC zones also appear
        for (HealthcareFacility f : allFacilities) {
            String village = f.getVillageTown() != null && !f.getVillageTown().isBlank() ? f.getVillageTown().trim() : "Rural Sector";
            String district = f.getDistrict() != null && !f.getDistrict().isBlank() ? f.getDistrict().trim() : "District";
            String state = f.getState() != null && !f.getState().isBlank() ? f.getState().trim() : "Uttar Pradesh";
            String key = (village + "|" + district).toLowerCase(Locale.ROOT);

            AreaBucket bucket = buckets.computeIfAbsent(key, k -> new AreaBucket(village, district, state));
            if (bucket.nearestFacilityName == null) {
                bucket.nearestFacilityName = f.getName();
            }
            if (f.getGeoLocation() != null && f.getGeoLocation().getLatitude() != null && f.getGeoLocation().getLongitude() != null) {
                bucket.addCoordinate(f.getGeoLocation().getLatitude(), f.getGeoLocation().getLongitude());
            }
        }

        // 3. Aggregate all healthcare cases + AI predictions + symptoms into their area bucket
        for (HealthcareCase c : allCases) {
            UUID pid = c.getPatient() != null ? c.getPatient().getId() : null;
            String key = pid != null ? patientAreaKey.get(pid) : null;
            if (key == null) {
                key = "general|rural";
                buckets.computeIfAbsent(key, k -> new AreaBucket("General Rural Sector", "Rural District", "India"));
            }

            AreaBucket bucket = buckets.get(key);
            if (c.getGeoLocation() != null && c.getGeoLocation().getLatitude() != null && c.getGeoLocation().getLongitude() != null) {
                bucket.addCoordinate(c.getGeoLocation().getLatitude(), c.getGeoLocation().getLongitude());
            }

            bucket.totalCases++;
            boolean isActive = c.getStatus() != CaseStatus.CLOSED && c.getStatus() != CaseStatus.CANCELLED;
            if (isActive) {
                bucket.activeCases++;
            }

            AiPredictionResult ai = latestPredictionByCase.get(c.getId());
            boolean isHighRisk = false;
            double caseRiskScore = 25.0;

            if (ai != null) {
                if (ai.getRiskScore() != null) {
                    caseRiskScore = ai.getRiskScore();
                }
                if (ai.getRiskLevel() == RiskLevel.HIGH || ai.getRiskLevel() == RiskLevel.CRITICAL) {
                    isHighRisk = true;
                }
                if (ai.getPossibleDiseases() != null) {
                    for (DiseasePrediction dp : ai.getPossibleDiseases()) {
                        if (dp.getCategory() != null && !dp.getCategory().isBlank()) {
                            bucket.diseases.add(dp.getCategory().trim());
                        }
                    }
                }
            } else {
                if (c.getPriority() == CasePriority.CRITICAL) {
                    caseRiskScore = 85.0;
                    isHighRisk = true;
                } else if (c.getPriority() == CasePriority.HIGH) {
                    caseRiskScore = 70.0;
                    isHighRisk = true;
                } else if (c.getPriority() == CasePriority.MEDIUM) {
                    caseRiskScore = 45.0;
                }
            }

            if (isHighRisk && isActive) {
                bucket.highRiskCases++;
            }
            bucket.riskScoreSum += caseRiskScore;

            if (c.getSymptoms() != null) {
                for (CaseSymptom cs : c.getSymptoms()) {
                    if (cs.getSymptomName() != null && !cs.getSymptomName().isBlank()) {
                        bucket.symptoms.add(cs.getSymptomName().trim());
                    }
                }
            }
            List<Symptom> extraSymptoms = symptomsByCase.get(c.getId());
            if (extraSymptoms != null) {
                for (Symptom s : extraSymptoms) {
                    if (s.getSymptomName() != null && !s.getSymptomName().isBlank()) {
                        bucket.symptoms.add(s.getSymptomName().trim());
                    }
                }
            }
            if (c.getChiefComplaint() != null && !c.getChiefComplaint().isBlank() && bucket.symptoms.isEmpty()) {
                bucket.symptoms.add(c.getChiefComplaint().trim());
            }
        }

        // 4. Build OutbreakZoneDto list from buckets
        Instant now = Instant.now();
        List<OutbreakZoneDto> zones = new ArrayList<>();

        for (Map.Entry<String, AreaBucket> entry : buckets.entrySet()) {
            AreaBucket b = entry.getValue();
            OutbreakZoneDto dto = new OutbreakZoneDto();
            dto.setZoneId(entry.getKey().replace("|", "-").replaceAll("\\s+", "-"));
            dto.setVillageTown(b.villageTown);
            dto.setDistrict(b.district);
            dto.setState(b.state);
            dto.setAreaName(b.villageTown + ", " + b.district);

            Double lat = b.getAvgLat();
            Double lon = b.getAvgLon();
            dto.setLatitude(lat);
            dto.setLongitude(lon);

            if (latitude != null && longitude != null && lat != null && lon != null) {
                double dist = haversineKm(latitude, longitude, lat, lon);
                dto.setDistanceKm(Math.round(dist * 10.0) / 10.0);
            }

            dto.setTotalCases(b.totalCases);
            dto.setActiveCases(b.activeCases);
            dto.setHighRiskCases(b.highRiskCases);
            double avgScore = b.totalCases > 0 ? Math.round((b.riskScoreSum / b.totalCases) * 10.0) / 10.0 : 0.0;
            dto.setAverageRiskScore(avgScore);
            dto.setTopDiseases(new ArrayList<>(b.diseases));
            dto.setReportedSymptoms(new ArrayList<>(b.symptoms));
            dto.setNearestFacilityName(b.nearestFacilityName != null ? b.nearestFacilityName : "Local PHC / CHC");
            dto.setEvaluatedAt(now);

            // Classify RED / YELLOW / GREEN strictly from real case & AI risk metrics
            if (b.highRiskCases >= 2 || b.activeCases >= 4 || avgScore >= 70.0) {
                dto.setZoneColor("RED");
                dto.setZoneLabel("RED ZONE • High Outbreak / Clinical Risk");
                dto.setAdvisoryMessage(String.format(
                        "Alert: %d high-risk case(s) and %d active case(s) recorded in %s (Avg AI Risk: %.1f%%). Active ASHA surveillance and immediate PHC/CHC triage advised.",
                        b.highRiskCases, b.activeCases, b.villageTown, avgScore));
            } else if (b.highRiskCases == 1 || b.activeCases >= 2 || avgScore >= 40.0) {
                dto.setZoneColor("YELLOW");
                dto.setZoneLabel("YELLOW ZONE • Elevated Cluster Watch");
                dto.setAdvisoryMessage(String.format(
                        "Caution: %d active case(s) (%d high-risk) under observation in %s (Avg AI Risk: %.1f%%). Monitor febrile and infectious symptoms closely.",
                        b.activeCases, b.highRiskCases, b.villageTown, avgScore));
            } else {
                dto.setZoneColor("GREEN");
                dto.setZoneLabel("GREEN ZONE • Normal Baseline");
                dto.setAdvisoryMessage(String.format(
                        "Normal baseline in %s (%d active case(s)). Standard preventive hygiene and routine OPD services active.",
                        b.villageTown, b.activeCases));
            }

            zones.add(dto);
        }

        // Sort by zone severity (RED -> YELLOW -> GREEN), then by activeCases descending, then distanceKm
        zones.sort(Comparator
                .comparingInt((OutbreakZoneDto z) -> zoneSeverityRank(z.getZoneColor()))
                .thenComparing(Comparator.comparingInt(OutbreakZoneDto::getActiveCases).reversed())
                .thenComparing(z -> z.getDistanceKm() != null ? z.getDistanceKm() : Double.MAX_VALUE));

        return ResponseEntity.ok(ApiResponse.ok("Outbreak risk zones aggregated from live clinical cases and AI predictions", zones));
    }

    private static int zoneSeverityRank(String color) {
        if ("RED".equalsIgnoreCase(color)) return 0;
        if ("YELLOW".equalsIgnoreCase(color)) return 1;
        return 2;
    }

    private static double haversineKm(double lat1, double lon1, double lat2, double lon2) {
        double r = 6371.0;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return r * c;
    }

    private static class AreaBucket {
        final String villageTown;
        final String district;
        final String state;
        double latSum = 0.0;
        double lonSum = 0.0;
        int coordCount = 0;
        int totalCases = 0;
        int activeCases = 0;
        int highRiskCases = 0;
        double riskScoreSum = 0.0;
        final Set<String> diseases = new LinkedHashSet<>();
        final Set<String> symptoms = new LinkedHashSet<>();
        String nearestFacilityName;

        AreaBucket(String villageTown, String district, String state) {
            this.villageTown = villageTown;
            this.district = district;
            this.state = state;
        }

        void addCoordinate(double lat, double lon) {
            this.latSum += lat;
            this.lonSum += lon;
            this.coordCount++;
        }

        Double getAvgLat() {
            return coordCount > 0 ? Math.round((latSum / coordCount) * 10000.0) / 10000.0 : null;
        }

        Double getAvgLon() {
            return coordCount > 0 ? Math.round((lonSum / coordCount) * 10000.0) / 10000.0 : null;
        }
    }
}
