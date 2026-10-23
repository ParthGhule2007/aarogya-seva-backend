# SOS Emergency Alert Service - API Reference

Base URL: `http://localhost:8082` (or environment ingress)

---

## 1. Trigger Emergency SOS

Triggers an emergency SOS alert for a patient. The caller can specify any combination of `CAREGIVER`, `FACILITY`, and `NEARBY_PEOPLE`. The backend responds immediately with `HTTP 202 ACCEPTED` and dispatches notifications asynchronously.

- **Method**: `POST`
- **Path**: `/api/v1/sos`
- **Headers**:
  - `Content-Type: application/json`
  - `X-User-Id: <uuid>` *(Optional in dev, required in production to verify caller identity)*
  - `X-User-Role: PATIENT | ASHA | HEALTH_WORKER` *(Caller role)*
  - `Idempotency-Key: <string>` *(Optional unique client token to prevent duplicate alerts on network retries)*

### Request Body
```json
{
  "patientId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
  "latitude": 26.6811,
  "longitude": 80.9984,
  "targets": [
    "CAREGIVER",
    "FACILITY",
    "NEARBY_PEOPLE"
  ],
  "facilityId": "e5b8d2b9-7608-4107-b371-d41c19b0d6a2",
  "message": "Chest pain and dizziness, need immediate assistance"
}
```

### Validation Rules
1. `patientId`: Required valid UUID.
2. `latitude`: Required number between `-90.0` and `+90.0`.
3. `longitude`: Required number between `-180.0` and `+180.0`.
4. `targets`: Required non-empty list of `CAREGIVER`, `FACILITY`, `NEARBY_PEOPLE`.
5. `facilityId`: **Mandatory** if `targets` includes `FACILITY`. Ignored if `FACILITY` is not selected.
6. `message`: Optional, max length 500 characters.

### Response `202 ACCEPTED`
```json
{
  "sosId": "9b1deb4d-3b7d-4bad-9bdd-2b0d7b3dcb6d",
  "status": "PROCESSING",
  "triggeredAt": "2026-09-24T10:30:00Z",
  "message": "Emergency SOS triggered successfully"
}
```

---

## 2. Get SOS Status

Retrieves the current status of an SOS event, including recipient metrics. Sensitive medical details are excluded.

- **Method**: `GET`
- **Path**: `/api/v1/sos/{sosId}`

### Response `200 OK`
```json
{
  "sosId": "9b1deb4d-3b7d-4bad-9bdd-2b0d7b3dcb6d",
  "status": "NOTIFICATIONS_SENT",
  "triggeredAt": "2026-09-24T10:30:00Z",
  "resolvedAt": null,
  "totalRecipients": 3,
  "recipientsNotified": 3,
  "recipientsFailed": 0
}
```

Possible statuses:
- `TRIGGERED`: Received and queued.
- `PROCESSING`: Target resolution and push dispatch in progress.
- `NOTIFICATIONS_SENT`: All resolved recipients notified successfully.
- `PARTIALLY_SENT`: At least one recipient succeeded and at least one failed.
- `FAILED`: No recipients could be notified or all attempts failed.
- `RESOLVED`: Emergency has been marked resolved by a responder or healthcare provider.

---

## 3. Resolve SOS Event

Marks an active SOS event as resolved.

- **Method**: `POST`
- **Path**: `/api/v1/sos/{sosId}/resolve`

### Response `200 OK`
```json
{
  "sosId": "9b1deb4d-3b7d-4bad-9bdd-2b0d7b3dcb6d",
  "status": "RESOLVED",
  "triggeredAt": "2026-09-24T10:30:00Z",
  "resolvedAt": "2026-09-24T11:05:00Z",
  "totalRecipients": 3,
  "recipientsNotified": 3,
  "recipientsFailed": 0
}
```

---

## 4. Register Device Push Token

Registers an FCM device token for a user, mapping their role and emergency alert opt-in preference.

- **Method**: `POST`
- **Path**: `/api/v1/sos/devices/register`
- **Headers**: `Content-Type: application/json`

### Request Body
```json
{
  "userId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
  "userType": "ASHA",
  "fcmToken": "eK3X..._example_firebase_device_token_...9z",
  "emergencyAlertOptIn": true
}
```

Supported `userType` values: `PATIENT`, `CAREGIVER`, `ASHA`, `FACILITY`, `DOCTOR`.

### Response `201 CREATED`
```json
{
  "tokenId": "771e892b-8a50-48e0-a7d1-0f7ff3b4b57c",
  "userId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
  "userType": "ASHA",
  "emergencyAlertOptIn": true,
  "active": true,
  "registeredAt": "2026-09-24T10:00:00Z",
  "message": "Device registered successfully for emergency notifications"
}
```

---

## 5. Deactivate Device Token

Removes or deactivates a device push token (e.g. on logout or disabling emergency participation).

- **Method**: `DELETE`
- **Path**: `/api/v1/sos/devices/{token}`

### Response `204 NO CONTENT`

---

## 6. Health & Liveness Check

- **Method**: `GET`
- **Path**: `/actuator/health`

### Response `200 OK`
```json
{
  "status": "UP"
}
```

---

## 7. Error Responses

Consistent error representation across all endpoints:

```json
{
  "timestamp": "2026-09-24T10:30:00Z",
  "status": 400,
  "error": "VALIDATION_ERROR",
  "message": "facilityId is required when FACILITY target is selected",
  "path": "/api/v1/sos"
}
```
