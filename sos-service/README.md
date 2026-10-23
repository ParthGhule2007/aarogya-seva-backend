# Aarogya Seva - SOS Emergency Alert Microservice

An autonomous, production-oriented Spring Boot 3.3.3 microservice providing low-latency, fault-isolated emergency alert routing for the Aarogya Seva rural healthcare platform.

---

## Table of Contents
1. [Service Purpose](#1-service-purpose)
2. [Architecture](#2-architecture)
3. [Technology Stack](#3-technology-stack)
4. [Project Structure](#4-project-structure)
5. [Environment Variables](#5-environment-variables)
6. [Database Setup & Schema](#6-database-setup--schema)
7. [Flyway Migrations](#7-flyway-migrations)
8. [Running Locally](#8-running-locally)
9. [Running Tests](#9-running-tests)
10. [Docker Usage](#10-docker-usage)
11. [API Usage & Examples](#11-api-usage--examples)
12. [FCM Push Setup](#12-fcm-push-setup)
13. [External Service Contracts](#13-external-service-contracts)
14. [Security & Authorization Boundary](#14-security--authorization-boundary)
15. [Idempotency & Duplicate Protection](#15-idempotency--duplicate-protection)
16. [Asynchronous Processing Pipeline](#16-asynchronous-processing-pipeline)
17. [Fault Isolation & Partial Success](#17-fault-isolation--partial-success)
18. [Production Deployment Notes](#18-production-deployment-notes)

---

## 1. Service Purpose

In rural healthcare scenarios, medical emergencies require rapid notification across multiple stakeholders. The SOS service enables patients (and authorized community health workers/ASHAs) to broadcast emergency alerts to **any combination** of:
- **Caregiver** (primary family contact)
- **Healthcare Facility** (patient-selected hospital, PHC, or CHC)
- **Nearby Helpers / People Around Me** (registered healthcare volunteers, doctors, and ASHAs within a 5 km radius who have opted into emergency assistance)

The service is completely decoupled from the core application monolith, guaranteeing high availability even under high load or partial outages.

---

## 2. Architecture

The service adheres strictly to Clean Layered Architecture and Domain-Driven Microservice Isolation:

- **Isolated Bounded Context**: No shared classes, entities, DTOs, or database connections with the monolith.
- **Microservice Autonomy**: Owns its own dedicated PostgreSQL database and schema migrations.
- **Non-blocking Dispatch**: Responds with `202 ACCEPTED` in under 50ms; resolves recipients and dispatches notifications via a dedicated async thread pool (`@Async("sosNotificationExecutor")`).
- **Fault-Isolated Execution**: Each recipient dispatch is isolated. Failure to notify one party (e.g. facility network drop) never interrupts notifications to caregivers or nearby volunteers.

Refer to [docs/architecture.md](docs/architecture.md) for detailed diagrams and design rationale.

---

## 3. Technology Stack

- **Language**: Java 17
- **Framework**: Spring Boot 3.3.3
- **Data Access**: Spring Data JPA / Hibernate
- **Database**: PostgreSQL (Dev/Prod), H2 (Automated In-Memory Testing)
- **Migrations**: Flyway
- **Push Notifications**: Firebase Admin SDK 9.3.0
- **Validation**: Jakarta Bean Validation (`hibernate-validator`)
- **Documentation**: SpringDoc OpenAPI 2.6.0 (Swagger UI)
- **Testing**: JUnit 5, Mockito, Spring Test MockMvc
- **Conventions**: **Zero Lombok** (Explicit immutable builders and POJOs)

---

## 4. Project Structure

```
sos-service/
├── pom.xml
├── Dockerfile
├── .env.example
├── README.md
├── docs/
│   ├── api.md
│   ├── architecture.md
│   └── integration-contracts.md
└── src/
    ├── main/
    │   ├── java/com/ruralhealthcare/sos/
    │   │   ├── SosApplication.java
    │   │   ├── client/              # External client interfaces & REST/Mock adapters
    │   │   ├── config/              # Properties, Async, Firebase, and Web configuration
    │   │   ├── controller/          # REST endpoints (SosController, DeviceRegistrationController)
    │   │   ├── domain/              # JPA entities & Enums (Zero Lombok)
    │   │   ├── dto/                 # Immutable Request/Response payloads
    │   │   ├── exception/           # Custom domain exceptions & GlobalExceptionHandler
    │   │   ├── notification/        # Push channel abstraction (FCM & Mock)
    │   │   ├── repository/          # Spring Data JPA repositories
    │   │   ├── security/            # Security context, filter, & authorization service
    │   │   └── service/             # Business interfaces & async processor
    │   └── resources/
    │       ├── application.yml
    │       ├── application-dev.yml
    │       └── db/migration/
    │           └── V1__init_sos_schema.sql
    └── test/
        ├── java/com/ruralhealthcare/sos/
        │   ├── SosValidationTest.java
        │   ├── SosTargetCombinationsTest.java
        │   ├── NearbyPeopleRulesTest.java
        │   ├── NotificationFailureIsolationTest.java
        │   ├── IdempotencyAndSecurityTest.java
        │   ├── DeviceRegistrationTest.java
        │   └── SosControllerIntegrationTest.java
        └── resources/
            └── application-test.yml
```

---

## 5. Environment Variables

Copy `.env.example` to `.env` or supply variables in your container environment:

| Variable | Default (Dev) | Description |
|---|---|---|
| `SERVER_PORT` | `8082` | HTTP port |
| `SPRING_PROFILES_ACTIVE` | `dev` | Profile (`dev`, `test`, `prod`) |
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://localhost:5432/aarogya_sos_db` | Dedicated DB connection string |
| `SPRING_DATASOURCE_USERNAME` | `postgres` | DB user |
| `SPRING_DATASOURCE_PASSWORD` | `postgres` | DB password |
| `SOS_NOTIFICATION_PROVIDER` | `mock` | `mock` or `fcm` |
| `SOS_CLIENTS_MODE` | `mock` | `mock` (standalone) or `rest` (connected) |
| `FIREBASE_PROJECT_ID` | - | Firebase Project ID (if `fcm`) |
| `FIREBASE_CLIENT_EMAIL` | - | Service account email (if `fcm`) |
| `FIREBASE_PRIVATE_KEY` | - | Service account private key (if `fcm`) |
| `FIREBASE_CREDENTIALS_PATH` | - | Path to service account JSON (alternative) |
| `SOS_NEARBY_RADIUS_KM` | `5.0` | Server-fixed radius for nearby helper search |
| `SOS_DEDUPLICATION_WINDOW_MINUTES` | `5` | Window to suppress accidental repeated SOS |

---

## 6. Database Setup & Schema

The service operates on its own dedicated PostgreSQL database:

```sql
CREATE DATABASE aarogya_sos_db;
```

### Tables Created:
1. `sos_events`: Stores the root alert event, patient ID, coordinates, target array, facility ID, optional message, idempotency key, and status.
2. `sos_recipients`: Tracks individual recipients resolved per event (caregiver, facility responder, or nearby helper), their distance, and notification outcome.
3. `sos_notifications`: Audit trail of push notification dispatches per recipient, including message ID, status, and failure reason.
4. `sos_device_tokens`: Registry of FCM device push tokens, mapped to user IDs, user types, and explicit `emergency_alert_opt_in` flags.

---

## 7. Flyway Migrations

Flyway automatically migrates the database schema on startup:
- Location: `src/main/resources/db/migration/V1__init_sos_schema.sql`
- Contains constraints, check rules for enums, foreign keys, and indexes on `patient_id`, `idempotency_key`, and `fcm_token`.

---

## 8. Running Locally

### Development Mode (Mock Notification & Mock Clients)
No external database or Firebase credentials needed:
```bash
cd sos-service
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```
The service will start on `http://localhost:8082`.
- Swagger UI: `http://localhost:8082/swagger-ui/index.html`
- Actuator Health: `http://localhost:8082/actuator/health`

---

## 9. Running Tests

Execute the full suite of unit and integration tests (uses in-memory H2 database):
```bash
cd sos-service
mvn clean test
```

### Test Coverage Highlights:
- All 7 combinations of targets (`CAREGIVER`, `FACILITY`, `NEARBY_PEOPLE`).
- Validation enforcement (empty targets, latitude/longitude boundaries, facilityId mandatory for FACILITY).
- Server-side fixed 5 km radius verification.
- Triggering patient exclusion and opted-out user exclusion.
- Recipient fault isolation (`PARTIALLY_SENT` status).
- Idempotency key deduplication and deduplication window.
- Device token registration, update, and deactivation.
- Mock FCM delivery and MockMvc integration tests.

---

## 10. Docker Usage

### Build the Image
```bash
cd sos-service
docker build -t aarogya-seva/sos-service:latest .
```

### Run the Container
```bash
docker run -d \
  --name aarogya-sos \
  -p 8082:8082 \
  -e SPRING_PROFILES_ACTIVE=dev \
  -e SOS_NOTIFICATION_PROVIDER=mock \
  -e SOS_CLIENTS_MODE=mock \
  aarogya-seva/sos-service:latest
```

---

## 11. API Usage & Examples

### Trigger SOS (Multi-Target: Caregiver + Facility + Nearby)
```bash
curl -X POST http://localhost:8082/api/v1/sos \
  -H "Content-Type: application/json" \
  -H "Idempotency-Key: req-uuid-12345" \
  -d '{
    "patientId": "9b1deb4d-3b7d-4bad-9bdd-2b0d7b3dcb6d",
    "latitude": 26.6811,
    "longitude": 80.9984,
    "targets": ["CAREGIVER", "FACILITY", "NEARBY_PEOPLE"],
    "facilityId": "e5b8d2b9-7608-4107-b371-d41c19b0d6a2",
    "message": "Immediate help needed, severe chest pain"
  }'
```

**Response (HTTP 202 Accepted)**:
```json
{
  "sosId": "4a7b3c2d-1122-3344-5566-778899aabbcc",
  "status": "PROCESSING",
  "triggeredAt": "2026-09-24T10:30:00Z",
  "message": "Emergency SOS triggered successfully"
}
```

### Query SOS Status
```bash
curl http://localhost:8082/api/v1/sos/4a7b3c2d-1122-3344-5566-778899aabbcc
```

**Response (HTTP 200 OK)**:
```json
{
  "sosId": "4a7b3c2d-1122-3344-5566-778899aabbcc",
  "status": "NOTIFICATIONS_SENT",
  "triggeredAt": "2026-09-24T10:30:00Z",
  "resolvedAt": null,
  "totalRecipients": 3,
  "recipientsNotified": 3,
  "recipientsFailed": 0
}
```

### Register Device Push Token
```bash
curl -X POST http://localhost:8082/api/v1/sos/devices/register \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "9b1deb4d-3b7d-4bad-9bdd-2b0d7b3dcb6d",
    "userType": "ASHA",
    "fcmToken": "fcm_token_example_12345",
    "emergencyAlertOptIn": true
  }'
```

---

## 12. FCM Push Setup

To enable real push notifications via Google Firebase:
1. Set `SOS_NOTIFICATION_PROVIDER=fcm`.
2. Provide your Firebase Service Account credentials either by:
   - Mounting the JSON file and setting `FIREBASE_CREDENTIALS_PATH=/secrets/firebase-key.json`
   - Setting the environment variables `FIREBASE_PROJECT_ID`, `FIREBASE_CLIENT_EMAIL`, and `FIREBASE_PRIVATE_KEY`.
3. Never commit credentials to version control.

---

## 13. External Service Contracts

The service interacts with upstream domains via REST contracts:
- `PatientServiceClient`: Resolves caregiver for a patient.
- `FacilityServiceClient`: Validates facility existence and fetches authorized responders.
- `UserLocationServiceClient`: Discovers opted-in emergency volunteers within 5 km.

Refer to [docs/integration-contracts.md](docs/integration-contracts.md) for complete payload specifications.

---

## 14. Security & Authorization Boundary

- **Patient Self-Service**: Patients can trigger SOS alerts for themselves.
- **Assisted Alerts**: ASHA health workers and healthcare workers can trigger SOS alerts for registered patients.
- **Identity Enforcement**: `AuthenticationFilter` extracts caller credentials from platform headers (`X-User-Id`, `X-User-Role`) or security context, and `AuthorizationService` rejects unauthorized cross-patient triggers with `403 FORBIDDEN`.

---

## 15. Idempotency & Duplicate Protection

1. **Idempotency-Key Header**: Clients can send an `Idempotency-Key`. Identical requests return the original event without creating duplicate records or re-dispatching alerts.
2. **Accidental Rapid Retrigger**: If a patient triggers SOS repeatedly within 5 minutes, the service returns the active event to prevent notification flooding while keeping responders focused on the ongoing emergency.

---

## 16. Asynchronous Processing Pipeline

When `POST /api/v1/sos` is triggered:
1. Fast validation & persistence (< 50ms).
2. HTTP 202 returned to caller.
3. Event handed off to `SosAsyncProcessor` on a managed thread pool (`sosNotificationExecutor`).
4. Selected targets resolved in parallel.
5. Push notifications dispatched via `NotificationService`.
6. Recipient records updated (`SENT` or `FAILED`).
7. Overall status updated to `NOTIFICATIONS_SENT`, `PARTIALLY_SENT`, or `FAILED`.

---

## 17. Fault Isolation & Partial Success

- Target resolution steps are wrapped in isolated exception handlers.
- If the caregiver service is down, facility and nearby helpers still receive alerts.
- If one recipient device fails, other recipients still receive their notifications.
- The status `PARTIALLY_SENT` clearly indicates to responders that partial notifications succeeded.

---

## 18. Production Deployment Notes

1. Configure a dedicated PostgreSQL instance or schema separate from the monolith.
2. Store Firebase service account keys in a secure secret manager (e.g. AWS Secrets Manager, GCP Secret Manager, or Kubernetes Secret).
3. Set `SOS_NOTIFICATION_PROVIDER=fcm` and `SOS_CLIENTS_MODE=rest`.
4. Ensure ingress routes traffic for `/api/v1/sos/**` directly to this microservice.
