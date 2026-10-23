# Rural Healthcare Platform - Architecture & Design Decisions

## 1. Architectural Philosophy
The Rural Healthcare Platform is engineered as a **Modular Monolith** in Java 17 and Spring Boot 3.3.
* **High Domain Cohesion**: Each domain module encapsulates its own entities, business rules, repositories, services, DTOs, and REST controllers.
* **Low Inter-Module Coupling**: Direct cross-module database table joins or entity references are prohibited. Modules communicate across boundaries solely through public Java service interfaces (e.g., `FacilityQueryService`) and asynchronous domain events.
* **Extraction Readiness**: Internal module interfaces and domain event contracts are designed to allow any module to be extracted into an independent microservice if future scale requires it, without rewriting business logic.

---

## 2. Module Blueprint & Boundaries

| Module | Core Responsibility | Persistence | Direct Dependencies |
| :--- | :--- | :--- | :--- |
| **Common Kernel** (`common`) | Shared exceptions, error envelope, `BaseEntity`, `GeoLocation` value object, audit logs | N/A | None |
| **Facility Module** (`facility`) *(Module 1 - Completed)* | Master healthcare facility catalog, service capabilities, PostGIS geospatial search | Supabase PostgreSQL (PostGIS + Flyway) | `common` |
| **Patient Module** (`patient`) *(Module 2 - Completed)* | Patient registration & profiles, healthcare consultation cases, multi-channel intake, case status lifecycle | Supabase PostgreSQL (PostGIS + Flyway) | `common` |
| **Symptom Module** (`symptom`) *(Module 3 - Completed)* | Clinical symptom intake, severity classification, duration tracking, case-symptom linkage | Supabase PostgreSQL (Flyway) | `common`, `patient` |
| **AI Intelligence & Decision Support** (`ai`) *(Module 4 - Completed)* | AI clinical decision support, disease screening, risk triage, facility matching | Supabase PostgreSQL (Flyway) | `common`, `patient`, `symptom`, `facility` |
| **Referral Module** (`referral`) *(Module 5)* | Referral tokens, facility assignment, referral status lifecycle | Supabase PostgreSQL (Flyway) | `common`, `ai`, `facility`, `patient` |
| **Surveillance & SOS Module** (`surveillance`/`sos`) *(Module 6)* | Anonymized case aggregation, epidemiological alerts, emergency SOS dispatch | Supabase PostgreSQL / Analytics | `common`, `screening`, `facility`, `patient` |

---

## 3. Storage Strategy: Supabase PostgreSQL + PostGIS
1. **Primary Database Engine**: **Supabase PostgreSQL** (`db.lnnvthckjlhhtnwpdnwy.supabase.co:5432/postgres`).
2. **Flyway Migrations**: All PostgreSQL schema migrations are version-controlled (`V1__...`, `V2__...`) in `src/main/resources/db/migration`.
3. **Database-Level Geospatial Engine (PostGIS)**:
   - Facilities store location in a first-class `GEOGRAPHY(Point, 4326)` column.
   - High-performance radius queries leverage spatial **GiST indexing** via `ST_DWithin`.
   - Spheroid distances are computed in kilometers directly within PostgreSQL using `ST_Distance / 1000.0`.
   - **No In-Memory Haversine Scanning**: Application-side distance looping and full-table scanning are strictly prohibited. All spatial filtering executes in the database engine.
4. **Seed / Demo Data**:
   - Seeded facility records are strictly flagged with `is_demo_data = TRUE`.
   - Seeded records represent **DEMO / DEVELOPMENT DATA — NOT VERIFIED REAL-WORLD HEALTHCARE DATA**.
   - Demo data is decoupled from business logic and can be replaced at any time with verified facility ingestion feeds via `DELETE FROM healthcare_facilities WHERE is_demo_data = TRUE;`.

---

## 4. Cross-Cutting Standards
* **Configuration & Secrets**:
  - All database credentials, secrets, and connection URLs are supplied via environment variables (`SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`).
  - No credentials or passwords may ever be hardcoded or checked into source control. A `.env.example` file provides placeholders.
* **Error Response Envelope**:
  ```json
  {
    "errorCode": "RESOURCE_NOT_FOUND",
    "message": "HealthcareFacility with identifier [id] was not found",
    "path": "/api/v1/facilities/...",
    "correlationId": "4a73e5f2-bdf2-475a-a43f-b88339c9eec1",
    "timestamp": "2026-09-22T08:30:00Z",
    "validationErrors": []
  }
  ```
* **Success Envelope**:
  ```json
  {
    "success": true,
    "message": "Operation successful",
    "data": { ... },
    "timestamp": "2026-09-22T08:30:00Z"
  }
  ```
* **Facility Search Ordering**:
  - Results are ordered transparently by geodesic proximity (`distanceKm ASC`).
  - Unapproved scoring algorithms (such as the 60/40 suitability formula) have been removed. Any future ranking formula requires explicit user review and approval.
