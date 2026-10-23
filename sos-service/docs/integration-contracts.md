# External Integration Contracts

The **SOS Emergency Alert Service** communicates with external platform boundaries exclusively through HTTP REST contracts. Direct database sharing or class imports are strictly forbidden.

---

## 1. Patient & Caregiver Service Contract

### Status:
- **Mock Client**: `MockPatientServiceClient` (active when `sos.clients.mode=mock`)
- **Production REST Client**: `RestPatientServiceClient` (active when `sos.clients.mode=rest`)
- **External Platform State**: `EXTERNAL DEPENDENCY`

### Endpoint Specification
- **Method**: `GET`
- **Path**: `/api/v1/patients/{patientId}/caregiver`
- **Headers**:
  - `Accept: application/json`

### Success Response `200 OK`
```json
{
  "caregiverId": "c3b8a1e2-9d32-4ef8-a3f2-1a2b3c4d5e6f",
  "name": "Sunita Devi",
  "phone": "+919876543210",
  "relationship": "DAUGHTER"
}
```

### Not Found Response `404 NOT FOUND`
When the patient does not have a designated primary caregiver registered. The SOS service handles this gracefully without throwing an uncaught exception, recording an unnotified recipient with status `FAILED`.

---

## 2. Facility Service Contract

### Status:
- **Mock Client**: `MockFacilityServiceClient` (active when `sos.clients.mode=mock`)
- **Production REST Client**: `RestFacilityServiceClient` (active when `sos.clients.mode=rest`)
- **External Platform State**: `EXTERNAL DEPENDENCY`

### Endpoint Specification
- **Method**: `GET`
- **Path**: `/api/v1/facilities/{facilityId}/contact`
- **Headers**:
  - `Accept: application/json`

### Success Response `200 OK`
```json
{
  "facilityId": "e5b8d2b9-7608-4107-b371-d41c19b0d6a2",
  "facilityName": "Community Health Centre - Mohanlalganj",
  "facilityType": "CHC",
  "contactPhone": "+915222345678",
  "authorizedRecipientIds": [
    "f1111111-1111-1111-1111-111111111111",
    "f2222222-2222-2222-2222-222222222222"
  ]
}
```

### Behavioral Notes
- The patient **must explicitly select** the facility on the frontend. The SOS service will never auto-select a facility.
- If `authorizedRecipientIds` is empty, the service falls back to looking up tokens registered directly against the `facilityId`.

---

## 3. User & Location Service Contract

### Status:
- **Mock Client**: `MockUserLocationServiceClient` (active when `sos.clients.mode=mock`)
- **Production REST Client**: `RestUserLocationServiceClient` (active when `sos.clients.mode=rest`)
- **External Platform State**: `EXTERNAL DEPENDENCY`

### Endpoint Specification
- **Method**: `GET`
- **Path**: `/api/v1/users/nearby-emergency-helpers`
- **Query Parameters**:
  - `latitude`: (double, required) e.g. `26.6811`
  - `longitude`: (double, required) e.g. `80.9984`
  - `radiusKm`: (double, required, fixed to 5.0)
  - `excludeUserId`: (uuid, required) Patient ID triggering the alert
  - `emergencyAlertOptIn`: (boolean, fixed to true)

### Success Response `200 OK`
```json
[
  {
    "userId": "d4a7b2e1-8899-4c3d-b2a1-0e1f2a3b4c5d",
    "userType": "ASHA",
    "distanceKm": 1.8
  },
  {
    "userId": "a9b8c7d6-1122-3344-5566-778899aabbcc",
    "userType": "DOCTOR",
    "distanceKm": 3.4
  }
]
```

### Behavioral Notes
- Only registered platform users who have opted into emergency assistance (`emergencyAlertOptIn = true`) and have an active device token will be returned.
- The triggering patient is strictly excluded from the result list to prevent self-notification.
