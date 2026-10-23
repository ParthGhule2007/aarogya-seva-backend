# Module 3: Symptom Intake & Management Module

## 1. Overview
The Symptom Intake & Management Module (`com.ruralhealthcare.symptom`) provides structured recording, updating, removal, and retrieval of clinical symptoms associated with healthcare cases. It functions as the clinical evidence layer, validating clinical inputs and exposing contract interfaces (`SymptomQueryService`) for downstream AI symptom analysis, risk classification, and specialist referral workflows.

---

## 2. Architecture & Domain Responsibilities
* **Package**: `com.ruralhealthcare.symptom`
* **Sub-packages**:
  * `domain`: JPA entity [`Symptom`](file:///d:/mit-india/Grand-Finale/src/main/java/com/ruralhealthcare/symptom/domain/Symptom.java) and enum [`SymptomSeverity`](file:///d:/mit-india/Grand-Finale/src/main/java/com/ruralhealthcare/symptom/domain/SymptomSeverity.java).
  * `repository`: [`SymptomRepository`](file:///d:/mit-india/Grand-Finale/src/main/java/com/ruralhealthcare/symptom/repository/SymptomRepository.java) (queries by case ID, severity filtering, symptom counting).
  * `service`: [`SymptomService`](file:///d:/mit-india/Grand-Finale/src/main/java/com/ruralhealthcare/symptom/service/SymptomService.java) and [`impl/SymptomServiceImpl`](file:///d:/mit-india/Grand-Finale/src/main/java/com/ruralhealthcare/symptom/service/impl/SymptomServiceImpl.java).
  * `service.SymptomQueryService`: Public cross-module contract exposed for downstream AI screening and risk classification modules.
  * `api`: [`SymptomController`](file:///d:/mit-india/Grand-Finale/src/main/java/com/ruralhealthcare/symptom/api/SymptomController.java) and DTOs.

---

## 3. Public Java Contracts (For Downstream Modules)
Downstream modules (e.g., Module 4 AI Screening & Risk Classification) interact with Module 3 exclusively through `SymptomQueryService`:

```java
package com.ruralhealthcare.symptom.service;

public interface SymptomQueryService {
    List<SymptomDto> getSymptomsForCase(UUID caseId);
    List<SymptomDto> getSymptomsBySeverity(UUID caseId, SymptomSeverity severity);
    boolean hasSevereOrCriticalSymptoms(UUID caseId);
    long countSymptoms(UUID caseId);
    boolean hasSymptoms(UUID caseId);
}
```

---

## 4. Cross-Module Independence
Module 3 depends on Module 2 (`patient`) only through [`PatientQueryService`](file:///d:/mit-india/Grand-Finale/src/main/java/com/ruralhealthcare/patient/service/PatientQueryService.java):
* Verifies case existence (`caseExists`) before recording symptoms.
* Protects closed and cancelled cases: throws `ValidationException` if attempting to add or modify symptoms on terminal cases.
* Introduces zero direct cross-module JPA table joins.

---

## 5. Database Schema & Flyway Migrations
Managed via Flyway script for Supabase PostgreSQL:
* **`V4__init_symptom_module_schema.sql`**:
  * **`symptoms` table**:
    * `id UUID PRIMARY KEY`
    * `case_id UUID NOT NULL REFERENCES healthcare_cases(id) ON DELETE CASCADE`
    * `symptom_name VARCHAR(150) NOT NULL`
    * `severity VARCHAR(20) NOT NULL DEFAULT 'MILD'` (`MILD`, `MODERATE`, `SEVERE`, `CRITICAL`)
    * `duration_days INTEGER NOT NULL DEFAULT 1`
    * `description TEXT`
    * `body_site VARCHAR(100)`
    * `created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP`
    * `updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP`
  * **B-tree Indexes**:
    * `idx_symptoms_case_id` on `symptoms(case_id)`
    * `idx_symptoms_name` on `symptoms(symptom_name)`
    * `idx_symptoms_severity` on `symptoms(severity)`
    * `idx_symptoms_created_at` on `symptoms(created_at)`

---

## 6. REST API Endpoints

| Method | Endpoint | Description | Status Code |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/v1/cases/{caseId}/symptoms` | Add a single symptom to a case | `201 Created` |
| `POST` | `/api/v1/cases/{caseId}/symptoms/batch` | Record multiple symptoms in a single atomic request | `201 Created` |
| `GET` | `/api/v1/cases/{caseId}/symptoms` | List all symptoms for a case (optional filter by `?severity=`) | `200 OK` |
| `GET` | `/api/v1/symptoms/{id}` | Retrieve individual symptom by ID | `200 OK` |
| `PUT` | `/api/v1/symptoms/{id}` | Update symptom attributes (severity, duration, notes, site) | `200 OK` |
| `DELETE` | `/api/v1/symptoms/{id}` | Delete a symptom from a case | `200 OK` |
