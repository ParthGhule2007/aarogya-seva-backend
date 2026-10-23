# SOS Emergency Alert Service - Architecture Guide

## 1. Architectural Philosophy

The **SOS Emergency Alert Service** is designed as a mission-critical, autonomous microservice within the Aarogya Seva healthcare platform. It provides high-speed, fault-tolerant dispatching of emergency distress signals without coupling to the monolith or transactional core application.

```
                      +-----------------------------+
                      |   Mobile / Web Client       |
                      +-----------------------------+
                                     |
                                     | POST /api/v1/sos (202 Accepted)
                                     v
                      +-----------------------------+
                      |    SosController            |
                      +-----------------------------+
                                     |
             +-----------------------+-----------------------+
             |                                               |
             v                                               v
    [Idempotency Check]                             [Security Validation]
             |                                               |
             +-----------------------+-----------------------+
                                     |
                                     v
                      +-----------------------------+
                      |    SosService (Persist)     |
                      +-----------------------------+
                                     |
                      +--------------+--------------+
                      | (Async Thread Pool)         |
                      v                             v
           +--------------------+         +--------------------+
           |  SosEvent (DB)     |         |  SosAsyncProcessor |
           +--------------------+         +--------------------+
                                                    |
                   +--------------------------------+--------------------------------+
                   |                                |                                |
                   v                                v                                v
          [CAREGIVER Target]               [FACILITY Target]             [NEARBY_PEOPLE Target]
                   |                                |                                |
         PatientServiceClient            FacilityServiceClient           UserLocationServiceClient
                   |                                |                                |
                   +--------------------------------+--------------------------------+
                                                    |
                                                    v
                                      +----------------------------+
                                      | DeviceRegistrationService  |
                                      +----------------------------+
                                                    |
                                                    v
                                      +----------------------------+
                                      |    NotificationService     |
                                      +----------------------------+
                                                    |
                                    +---------------+---------------+
                                    |                               |
                                    v                               v
                         [MockNotificationService]       [FcmNotificationService]
```

---

## 2. Key Design Principles

### A. Independent Bounded Context
- **No Shared Entities or Repositories**: Does not import or reference any Java classes, JPA entities, repositories, or services from the monolith (`Grand-Finale/src`).
- **Autonomous Database Ownership**: Manages its own dedicated schema (`sos_events`, `sos_recipients`, `sos_notifications`, `sos_device_tokens`) via Flyway.
- **Contract-Based Integration**: All interactions with external services (Patient, Facility, User Location) occur strictly via HTTP REST client interfaces.

### B. Multi-Target Flexibility
A patient can select **any combination** of:
1. `CAREGIVER`
2. `FACILITY`
3. `NEARBY_PEOPLE`

There is strictly no restriction requiring all three. Unselected targets are never evaluated or notified.

### C. Server-Side Distance & Opt-in Enforcement
- **Nearby Radius**: Fixed at **5.0 km** by backend policy (`sos.nearby-radius-km=5.0`). The client cannot alter or provide this value.
- **Double Filtering**: The triggering patient is strictly excluded from nearby helpers.
- **Explicit Opt-In**: Only users with `emergencyAlertOptIn = true` and an active FCM token receive nearby emergency notifications.

### D. Asynchronous Execution & Fault Isolation
- When `/api/v1/sos` is called, the request is validated, idempotency is checked, the event is persisted, and `HTTP 202 ACCEPTED` is returned immediately (< 50ms).
- Recipient resolution and push notification dispatch occur in a dedicated asynchronous thread pool (`@Async("sosNotificationExecutor")`).
- **Recipient Isolation**: A failure in one recipient (e.g. facility timeout or invalid token) never halts or cancels notifications to other recipients.
- If at least one recipient succeeds and at least one fails, the overall status resolves to `PARTIALLY_SENT`.

### E. Idempotency & Accidental Repeat Protection
- **Idempotency-Key Header**: Clients can provide a unique UUID/hash. If the service receives an identical key, it returns the existing SOS record immediately without creating duplicate alerts or re-dispatching push notifications.
- **Accidental Deduplication Window**: Configurable window (default 5 minutes). Repeated SOS alerts by the same patient within this window are recognized, logging and returning the active event to prevent notification flooding.

### F. Minimal Push Payload
Push notifications sent over Firebase Cloud Messaging (FCM) deliberately exclude sensitive clinical data, diagnostic histories, or precise coordinates. Only minimal identifiers (`sosId`, `targetType`) and contextual titles are transmitted, prompting the recipient to open the authenticated application for action details.

---

## 3. Technology Stack & Zero Lombok Convention

| Layer | Technology |
|---|---|
| Runtime | Java 17 |
| Framework | Spring Boot 3.3.3 |
| Persistence | Spring Data JPA / Hibernate |
| Database | PostgreSQL (Production/Dev), H2 (In-memory Test) |
| Schema Migration | Flyway |
| Push Delivery | Google Firebase Admin SDK 9.3.0 |
| Documentation | SpringDoc OpenAPI 2.6.0 (Swagger 3) |
| Testing | JUnit 5, Mockito, Spring Test (MockMvc) |
| Code Standards | **Zero Lombok** (Explicit immutable builders, POJOs, constructors) |
