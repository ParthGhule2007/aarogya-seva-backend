# 🏥 Aarogya Seva (आरोग्य सेवा) — Spring Boot Integration Guide

> **Production Clinical AI Microservice & Multilingual Tele-Triage Engine**  
> *Official API Contract & Developer Reference for External Spring Boot Backend Integration.*

---

## 📌 1. Deployment Details & Live Endpoints

The clinical intelligence model and FastAPI backend are deployed live on Hugging Face Spaces. You can trigger API calls directly over HTTPS from any Spring Boot / Java application without managing Python runtimes.

| Environment | Base URL | Protocol | Notes |
| :--- | :--- | :--- | :--- |
| **Hugging Face Live API** | `https://parthgule-arogya-seva.hf.space` | HTTPS / REST | **Primary Production URL for API Calls** |
| **Interactive Web UI** | `https://huggingface.co/spaces/ParthGule/arogya-seva` | Web Browser | Web Dashboard & Live Testing Playground |
| **Real-Time Voice Gateway** | `wss://parthgule-arogya-seva.hf.space/ws/free-call` | Secure WSS | Bidirectional Audio & Silero VAD Tele-Triage |
| **Local On-Premise Host** | `http://127.0.0.1:9000` | HTTP | Central Brain Service (Local Dev) |

---

## ⚡ 2. Quick Connection & Timeout Best Practices

1. **Authentication**:
   - The public Space requires **no authentication token** for standard requests.
   - If the space is set to private in the future, pass the header:  
     `Authorization: Bearer <HF_USER_ACCESS_TOKEN>`
2. **Cold-Start Handling (Important)**:
   - Hugging Face Spaces enter idle sleep after ~15 minutes of zero traffic.
   - The first request might take **15–30 seconds** while the container wakes up.
   - Configure your Spring Boot HTTP Client with:
     - **Connect Timeout**: `10 seconds`
     - **Read Timeout**: `45 seconds` (LLM multi-lingual inference requires compute time).
3. **Health Check**:
   - Verify uptime before sending high-volume traffic:  
     `GET https://parthgule-arogya-seva.hf.space/health`

---

## 🩺 3. Endpoint Reference & Data Contracts

---

### Endpoint A: Clinical Tele-Triage (`POST /predict`)

This is the primary clinical endpoint. It accepts patient demographics, age, and recorded symptoms, running them through the fine-tuned clinical engine and deterministic guidelines. It returns **exact uppercase Java Enums** for safe mapping to your domain entities.

#### 1. Request URL & Headers
- **URL**: `https://parthgule-arogya-seva.hf.space/predict`
- **Method**: `POST`
- **Headers**:
  ```http
  Content-Type: application/json
  Accept: application/json
  ```

#### 2. Request Payload Schema
```json
{
  "caseId": "9b1deb4d-3b7d-4bad-9bdd-2b0d7b3dcb6d",
  "patient": {
    "age": 68,
    "gender": "MALE"
  },
  "symptoms": [
    {
      "name": "Bukhar (Persistent High Fever)",
      "severity": "SEVERE",
      "durationDays": 4,
      "description": "Temperature 103F with chills and lethargy",
      "bodySite": "General"
    },
    {
      "name": "Sans fulna (Shortness of breath)",
      "severity": "MODERATE",
      "durationDays": 2,
      "description": "Exertional dyspnea while walking",
      "bodySite": "Chest"
    }
  ]
}
```

#### 3. Validation Rules (Strict HTTP 422 Rejection)
- **`caseId`**: Required UUID or String tracking the transaction.
- **`patient.age`**: Strictly bounded between `0` and `120` years.
- **`patient.gender`**: Must be one of `MALE`, `FEMALE`, `OTHER`.
- **`symptoms`**: Minimum 1 symptom required (maximum 50).
- **Placeholder Rejection**: Placeholder values like `"string"`, `"test"`, `"n/a"`, `"none"`, or pure numbers are **strictly rejected** with `422 Unprocessable Entity`.
- **`symptoms[].severity`**: Must be one of `MILD`, `MODERATE`, `SEVERE`, `CRITICAL`.
- **`symptoms[].durationDays`**: Must be between `0` and `3650` days.

#### 4. Response Payload Schema
```json
{
  "caseId": "9b1deb4d-3b7d-4bad-9bdd-2b0d7b3dcb6d",
  "modelVersion": "aarogya-clinical-engine-v2.0",
  "processingStatus": "COMPLETED",
  "riskAssessment": {
    "riskScore": 84.5,
    "riskLevel": "CRITICAL"
  },
  "urgency": {
    "level": "EMERGENCY",
    "reason": "Life-threatening acute presentation requiring immediate stabilization."
  },
  "possibleDiseases": [
    {
      "category": "Enteric Fever (Typhoid) / Dengue",
      "probability": 0.78
    },
    {
      "category": "Acute Lower Respiratory Tract Infection / Pneumonia",
      "probability": 0.85
    }
  ],
  "precautions": [
    "Perform Rapid Diagnostic Test (RDT) or Peripheral Blood Smear.",
    "Check SpO2 saturation immediately; keep patient in an upright sitting posture.",
    "Do NOT delay transport. Call emergency ambulance (108) immediately."
  ],
  "requiredFacilityTypes": [
    "TERTIARY HOSPITAL",
    "DISTRICT HOSPITAL"
  ],
  "recommendedServices": [
    "EMERGENCY",
    "ICU BEDS",
    "INPATIENT BEDS",
    "DIAGNOSTIC LAB",
    "RADIOLOGY XRAY",
    "BLOOD STORAGE BANK"
  ],
  "confidence": 0.88,
  "explanation": "Patient classified under Geriatric (68y MALE) cohort presenting with Bukhar (SEVERE, 4d), Sans fulna (MODERATE, 2d). Assigned Risk Score 84.5/100 (CRITICAL) based on symptom severity, duration threshold, and epidemiological vulnerability indicators. Routed to TERTIARY HOSPITAL."
}
```

---

### Endpoint B: Multilingual Health Assistant Chatbot (`POST /chat`)

Interactive health guidance assistant supporting **Hindi (`hi`)**, **Marathi (`mr`)**, and **English (`en`)**. It enforces clinical domain guardrails (rejects non-health prompts) and can query Ayushman Bharat / PM-JAY schemes live.

#### 1. Request URL & Schema
- **URL**: `https://parthgule-arogya-seva.hf.space/chat`
- **Method**: `POST`

```json
{
  "message": "मेरी माँ की उम्र 58 साल है, उन्हें अचानक ठंडा पसीना आ रहा है और बाएं हाथ में भारीपन के साथ घबराहट हो रही है। क्या यह गैस की समस्या है या दिल का दौरा?",
  "language": "hi",
  "custom_system_directive": "You are a rural health triage assistant.",
  "custom_task_type": "GENERAL_TRIAGE"
}
```

#### 2. Response Schema
```json
{
  "domain_verified": true,
  "reply": "नमस्ते। 50 से अधिक उम्र में अचानक ठंडा पसीना आना और बाएं हाथ में भारीपन दिल के दौरे (Heart Attack) का गंभीर संकेत हो सकता है। इसे केवल गैस समझकर नजरअंदाज न करें। तुरंत 108 पर एम्बुलेंस बुलाएं और नजदीकी अस्पताल पहुंचें।",
  "language": "hi",
  "detected_intent": "GENERAL_TRIAGE",
  "evidence_sources": [
    {
      "title": "National Health Portal / ICMR Guidelines",
      "url": "https://nhp.gov.in",
      "snippet": "Acute Coronary Syndrome presentation in elderly cohorts."
    }
  ],
  "suggested_actions": [
    "Call 108 Emergency Ambulance",
    "Do not exert physically",
    "Loosen tight clothes"
  ]
}
```

---

### Endpoint C: Telephonic Voice Turn Generator (`POST /voice-agent-turn`)

Generates concise, spoken assistant dialog for telephony IVR or voicebots.
- **Constraints**: Maximum 1–2 conversational sentences.
- **Zero Markdown**: Strictly contains no asterisks, hashes, or bullet points so TTS engines read it naturally.

#### Request:
```json
{
  "caller_text": "मला दोन दिवसांपासून खूप तीव्र ताप आणि कोरडा खोकला आहे",
  "language": "mr",
  "patient_age": 70
}
```

#### Response:
```json
{
  "spoken_response": "मी समजू शकतो. आपण पुरेशी विश्रांती घ्या, कोमट पाणी प्या, आणि जास्त त्रास असल्यास ताबडतोब आरोग्य केंद्रात जा.",
  "detected_language": "mr",
  "is_emergency": false,
  "recommended_action": "ROUTINE_FOLLOWUP"
}
```

---

### Endpoint D: ABHA Patient Verification (`GET /api/helpline/caller-lookup`)

Simulates National Health Stack (ABHA) and caller profile verification by phone number.

- **URL**: `https://parthgule-arogya-seva.hf.space/api/helpline/caller-lookup?phone=9876543218`
- **Method**: `GET`

#### Response:
```json
{
  "status": "FOUND",
  "phone": "9876543218",
  "callerName": "Sunita Sharma",
  "age": 68,
  "gender": "MALE",
  "abhaId": "91-9876-5432-18",
  "district": "Pune, Maharashtra",
  "assignedPHC": "Khadakwasla PHC",
  "registered": true
}
```

---

## ☕ 4. Complete Spring Boot 3.x Implementation

Here is a ready-to-copy implementation using **Spring Boot 3.x (Java 17 / 21)** and modern `RestClient`.

### 1. Maven Dependencies (`pom.xml`)
```xml
<dependencies>
    <!-- Spring Web -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>

    <!-- Jackson Annotations & Databind -->
    <dependency>
        <groupId>com.fasterxml.jackson.core</groupId>
        <artifactId>jackson-databind</artifactId>
    </dependency>

    <!-- Lombok (Optional, for clean getters/setters) -->
    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
        <optional>true</optional>
    </dependency>
</dependencies>
```

---

### 2. Java Enums with Jackson Mapping
Notice that facility types and services contain spaces (e.g., `"SUB CENTRE"`, `"TERTIARY HOSPITAL"`). Using `@JsonValue` and `@JsonProperty` ensures seamless serialization and deserialization.

#### `RiskLevel.java`
```java
package com.example.arogya.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum RiskLevel {
    @JsonProperty("LOW") LOW,
    @JsonProperty("MODERATE") MODERATE,
    @JsonProperty("HIGH") HIGH,
    @JsonProperty("CRITICAL") CRITICAL
}
```

#### `UrgencyLevel.java`
```java
package com.example.arogya.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum UrgencyLevel {
    @JsonProperty("ROUTINE") ROUTINE,
    @JsonProperty("PRIORITY") PRIORITY,
    @JsonProperty("URGENT") URGENT,
    @JsonProperty("EMERGENCY") EMERGENCY
}
```

#### `Severity.java`
```java
package com.example.arogya.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum Severity {
    @JsonProperty("MILD") MILD,
    @JsonProperty("MODERATE") MODERATE,
    @JsonProperty("SEVERE") SEVERE,
    @JsonProperty("CRITICAL") CRITICAL
}
```

#### `Gender.java`
```java
package com.example.arogya.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum Gender {
    @JsonProperty("MALE") MALE,
    @JsonProperty("FEMALE") FEMALE,
    @JsonProperty("OTHER") OTHER
}
```

#### `FacilityType.java`
```java
package com.example.arogya.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum FacilityType {
    SUB_CENTRE("SUB CENTRE"),
    PHC("PHC"),
    CHC("CHC"),
    SUB_DISTRICT_HOSPITAL("SUB DISTRICT HOSPITAL"),
    DISTRICT_HOSPITAL("DISTRICT HOSPITAL"),
    TERTIARY_HOSPITAL("TERTIARY HOSPITAL");

    private final String value;

    FacilityType(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static FacilityType fromValue(String text) {
        for (FacilityType b : FacilityType.values()) {
            if (b.value.equalsIgnoreCase(text)) {
                return b;
            }
        }
        throw new IllegalArgumentException("Unknown facility type: " + text);
    }
}
```

#### `ServiceType.java`
```java
package com.example.arogya.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum ServiceType {
    GENERAL_OPD("GENERAL OPD"),
    EMERGENCY("EMERGENCY"),
    MATERNAL_HEALTH("MATERNAL HEALTH"),
    PEDIATRIC_CARE("PEDIATRIC CARE"),
    INPATIENT_BEDS("INPATIENT BEDS"),
    ICU_BEDS("ICU BEDS"),
    SURGICAL_UNIT("SURGICAL UNIT"),
    DIAGNOSTIC_LAB("DIAGNOSTIC LAB"),
    RADIOLOGY_XRAY("RADIOLOGY XRAY"),
    BLOOD_STORAGE_BANK("BLOOD STORAGE BANK"),
    TELEMEDICINE("TELEMEDICINE");

    private final String value;

    ServiceType(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static ServiceType fromValue(String text) {
        for (ServiceType b : ServiceType.values()) {
            if (b.value.equalsIgnoreCase(text)) {
                return b;
            }
        }
        throw new IllegalArgumentException("Unknown service: " + text);
    }
}
```

---

### 3. Request & Response DTOs

#### `PredictRequest.java`
```java
package com.example.arogya.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PredictRequest {
    private String caseId;
    private PatientInfo patient;
    private List<SymptomItem> symptoms;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PatientInfo {
        private int age;
        private Gender gender;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SymptomItem {
        private String name;
        private Severity severity;
        private int durationDays;
        private String description;
        private String bodySite;
    }
}
```

#### `PredictResponse.java`
```java
package com.example.arogya.dto;

import lombok.Data;
import java.util.List;

@Data
public class PredictResponse {
    private String caseId;
    private String modelVersion;
    private String processingStatus;
    private RiskAssessment riskAssessment;
    private UrgencyAssessment urgency;
    private List<DiseaseProbability> possibleDiseases;
    private List<String> precautions;
    private List<FacilityType> requiredFacilityTypes;
    private List<ServiceType> recommendedServices;
    private double confidence;
    private String explanation;

    @Data
    public static class RiskAssessment {
        private double riskScore;
        private RiskLevel riskLevel;
    }

    @Data
    public static class UrgencyAssessment {
        private UrgencyLevel level;
        private String reason;
    }

    @Data
    public static class DiseaseProbability {
        private String category;
        private double probability;
    }
}
```

#### `ChatRequest.java` & `ChatResponse.java`
```java
package com.example.arogya.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatRequest {
    private String message;
    private String language;
    private String custom_system_directive;
    private String custom_task_type;
}

@Data
public class ChatResponse {
    private boolean domain_verified;
    private String reply;
    private String language;
    private String detected_intent;
    private List<EvidenceSource> evidence_sources;
    private List<String> suggested_actions;

    @Data
    public static class EvidenceSource {
        private String title;
        private String url;
        private String snippet;
    }
}
```

---

### 4. Spring Service with Modern `RestClient`

```java
package com.example.arogya.service;

import com.example.arogya.dto.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.Duration;

@Service
public class AarogyaClinicalService {

    private final RestClient restClient;

    public AarogyaClinicalService(
            @Value("${arogya.api.base-url:https://parthgule-arogya-seva.hf.space}") String baseUrl
    ) {
        // Configure timeouts for LLM compute and potential Hugging Face wake-up latency
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout((int) Duration.ofSeconds(10).toMillis());
        factory.setReadTimeout((int) Duration.ofSeconds(45).toMillis());

        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(factory)
                .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader("Accept", MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    /**
     * Run deterministic and LLM-assisted clinical triage.
     */
    public PredictResponse evaluateTriage(PredictRequest request) {
        return restClient.post()
                .uri("/predict")
                .body(request)
                .retrieve()
                .body(PredictResponse.class);
    }

    /**
     * Ask health assistant in Hindi, Marathi, or English.
     */
    public ChatResponse askAssistant(String message, String lang) {
        ChatRequest req = ChatRequest.builder()
                .message(message)
                .language(lang != null ? lang : "auto")
                .build();

        return restClient.post()
                .uri("/chat")
                .body(req)
                .retrieve()
                .body(ChatResponse.class);
    }

    /**
     * Check health status of the Hugging Face AI service.
     */
    public String checkHealth() {
        return restClient.get()
                .uri("/health")
                .retrieve()
                .body(String.class);
    }
}
```

---

### 5. Sample Spring Boot Controller

```java
package com.example.arogya.controller;

import com.example.arogya.dto.PredictRequest;
import com.example.arogya.dto.PredictResponse;
import com.example.arogya.service.AarogyaClinicalService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/clinical")
public class ClinicalTriageController {

    private final AarogyaClinicalService clinicalService;

    public ClinicalTriageController(AarogyaClinicalService clinicalService) {
        this.clinicalService = clinicalService;
    }

    @PostMapping("/triage")
    public ResponseEntity<PredictResponse> runTriage(@RequestBody PredictRequest request) {
        if (request.getCaseId() == null || request.getCaseId().isBlank()) {
            request.setCaseId(UUID.randomUUID().toString());
        }
        PredictResponse response = clinicalService.evaluateTriage(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok(clinicalService.checkHealth());
    }
}
```

---

### 6. Application Configuration (`application.yml`)

```yaml
server:
  port: 8080

arogya:
  api:
    base-url: https://parthgule-arogya-seva.hf.space
```

---

## 💻 5. cURL Reference for Testing

Your colleagues can immediately test these requests directly in terminal:

### 1. Test Service Health
```bash
curl -X GET "https://parthgule-arogya-seva.hf.space/health"
```

### 2. Test Clinical Tele-Triage (`/predict`)
```bash
curl -X POST "https://parthgule-arogya-seva.hf.space/predict" \
  -H "Content-Type: application/json" \
  -d '{
    "caseId": "test-uuid-101",
    "patient": {
      "age": 68,
      "gender": "MALE"
    },
    "symptoms": [
      {
        "name": "Bukhar (Persistent High Fever)",
        "severity": "SEVERE",
        "durationDays": 4,
        "description": "Temperature 103F with chills",
        "bodySite": "General"
      },
      {
        "name": "Sans fulna (Shortness of breath)",
        "severity": "MODERATE",
        "durationDays": 2,
        "description": "Difficulty breathing on exertion",
        "bodySite": "Chest"
      }
    ]
  }'
```

### 3. Test Multilingual Health Chatbot (`/chat`)
```bash
curl -X POST "https://parthgule-arogya-seva.hf.space/chat" \
  -H "Content-Type: application/json" \
  -d '{
    "message": "आयुष्मान भारत योजना के तहत ₹5 लाख का मुफ्त इलाज कैसे मिलता है?",
    "language": "hi"
  }'
```

### 4. Test Telephonic Spoken Turn (`/voice-agent-turn`)
```bash
curl -X POST "https://parthgule-arogya-seva.hf.space/voice-agent-turn" \
  -H "Content-Type: application/json" \
  -d '{
    "caller_text": "मला दोन दिवसांपासून ताप आणि खोकला आहे",
    "language": "mr",
    "patient_age": 70
  }'
```

---

## 🛡️ 6. Troubleshooting & Gotchas

1. **HTTP 422 Unprocessable Entity**:
   - Cause: Sending dummy placeholder values (e.g., `"name": "string"` or `"name": "test"`).
   - Fix: Provide real clinical strings (e.g., `"High Fever"`, `"Bukhar"`, `"Chest pain"`, `"Sans fulna"`).
2. **HTTP 504 / Connection Timeout**:
   - Cause: Hugging Face Space waking up from idle mode (cold start).
   - Fix: Increase client read timeout to 45 seconds or call `/health` periodically as a keepalive.
3. **JSON Deserialization Exception for Facility Types**:
   - Cause: Spring Boot default Jackson parser fails on values containing spaces like `"SUB CENTRE"` or `"TERTIARY HOSPITAL"`.
   - Fix: Include `@JsonValue` and `@JsonCreator` annotations on your Java Enums as provided in Section 4.

---

*Guide generated for Aarogya Seva Clinical AI Microservice v2.0.*
