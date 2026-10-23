# Module 2: Patient & Healthcare Case Management Module

## 1. Overview
The Patient & Healthcare Case Management Module manages patient profiles, multi-channel intake sessions, recorded symptoms, and consultation case lifecycles across rural healthcare networks. It acts as the core clinical entity layer, providing immutable auditability and state progression while exposing contract interfaces for future AI screening and referral dispatch without tight cross-module coupling.

---

## 2. Architecture & Domain Responsibilities
* **Package**: `com.ruralhealthcare.patient`
* **Sub-packages**:
  * `domain`: JPA entities (`Patient`, `HealthcareCase`, `CaseSymptom`) and enums (`Gender`, `CaseStatus`, `CasePriority`, `IntakeChannel`, `SymptomSeverity`).
  * `repository`: Spring Data JPA repositories with custom queries and eager-fetch semantics (`PatientRepository`, `HealthcareCaseRepository`, `CaseSymptomRepository`).
  * `service`: Core business services (`PatientService`, `HealthcareCaseService`), internal implementation (`impl/`), and mappers (`PatientMapper`).
  * `service.PatientQueryService`: Public cross-module contract exposed for downstream modules (Screening/AI, Referral, SOS).
  * `api`: REST controllers (`PatientController`, `HealthcareCaseController`) and DTO definitions.

---

## 3. Public Java Contracts (For Downstream Modules)
Downstream modules (e.g., Module 3 Screening/AI, Module 4 Referral) interact with Module 2 exclusively through `PatientQueryService`:

```java
package com.ruralhealthcare.patient.service;

public interface PatientQueryService {
    Optional<PatientDto> findPatientById(UUID patientId);
    boolean patientExists(UUID patientId);
    Optional<HealthcareCaseDto> findCaseById(UUID caseId);
    boolean caseExists(UUID caseId);
}
```

---

## 4. Case Lifecycle State Machine
Healthcare cases strictly advance through a validated state transition model:

```
[ CREATED ]
     │ (via symptom recording)
     ▼
[ SYMPTOMS_RECORDED ]
     │ (submitted for triage/screening)
     ▼
[ PENDING_TRIAGE ]
     │ (triaged by AI / intake clinician)
     ▼
[ TRIAGED ]
     │ (referred to higher facility / specialist)
     ▼
[ REFERRED ]
     │ (consultation finished)
     ▼
[ CLOSED ]
```
*Any active case can transition to `CANCELLED` prior to closure. Transitions directly from `CREATED` to `CLOSED` or from terminal states are rejected with `ValidationException`.*

---

## 5. Intake Modalities & Channels
Designed for diverse rural accessibility constraints:
* `FORM`: Web portal or mobile app structured submission.
* `CHAT`: Multi-turn conversational symptom intake.
* `VOICE`: Audio/speech-to-text recorded session.
* `COMMUNITY_HEALTH_WORKER`: On-ground assisted triage by ASHA or ANM workers.

---

## 6. Database Schema & Flyway Migrations
Managed via Flyway script for Supabase PostgreSQL:
* **`V3__init_patient_case_schema.sql`**:
  * **`patients`**: Stores demographics, verified phone number, village/district address, emergency contacts, blood group, preferred language, and optional GPS coordinates.
  * **`healthcare_cases`**: Stores case numbers (`CASE-YYYYMMDD-XXXXXX`), patient foreign key, status, priority, intake channel, chief complaint, notes, and PostGIS `location GEOGRAPHY(Point, 4326)` synchronized via database trigger `trg_update_case_location`.
  * **`case_symptoms`**: Stores structured symptoms, severity (`MILD`, `MODERATE`, `SEVERE`, `CRITICAL`), duration in days, and detailed descriptions with cascading delete to cases.
  * **Spatial & B-tree Indexes**:
    * `idx_patients_contact` on `patients(contact_number)`
    * `idx_patients_district_state` on `patients(district, state)`
    * `idx_cases_patient` on `healthcare_cases(patient_id)`
    * `idx_cases_status` on `healthcare_cases(status)`
    * `idx_cases_number` on `healthcare_cases(case_number)`
    * `idx_cases_location_gist` on `healthcare_cases USING GIST(location)`
    * `idx_symptoms_case` on `case_symptoms(case_id)`

---

## 7. REST API Endpoints

### Patient Endpoints (`/api/v1/patients`)
* `POST /api/v1/patients`: Register new patient.
* `GET /api/v1/patients/{id}`: Retrieve patient profile.
* `PUT /api/v1/patients/{id}`: Update patient details.
* `GET /api/v1/patients/by-phone?contactNumber=+91...`: Search by phone number.
* `GET /api/v1/patients/search?query=...`: Search patients by name.

### Healthcare Case Endpoints (`/api/v1/cases`)
* `POST /api/v1/cases`: Open a new healthcare consultation case.
* `GET /api/v1/cases/{id}`: Retrieve case details with symptoms.
* `GET /api/v1/cases/by-number/{caseNumber}`: Retrieve case by unique case number.
* `GET /api/v1/cases/patient/{patientId}`: List all cases for a patient.
* `POST /api/v1/cases/{id}/symptoms`: Append symptoms to an active case.
* `PATCH /api/v1/cases/{id}/status`: Update case lifecycle status and priority.

---

## 8. Extensibility & Future Module Integration
* **Module 3 (AI Screening & Risk Triage)**: Will consume `PatientQueryService.findCaseById()` to evaluate symptoms against clinical protocols and transition cases from `PENDING_TRIAGE` to `TRIAGED` with assigned risk tier.
* **Module 4 (Specialist Referral & Teleconsultation)**: Will link triaged cases to facilities discovered via `FacilityQueryService` (Module 1) and record referral notes.
* **Module 5 (SOS & Emergency Dispatch)**: Will listen for cases escalated with priority `CRITICAL` for instant dispatch alerting.
