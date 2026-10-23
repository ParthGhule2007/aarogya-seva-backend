# Aarogya Seva (आरोग्य सेवा) — Rural Healthcare Frontend

A rural-first, elderly-accessible frontend web application built with **React 18 + TypeScript + Vite** for the Rural Healthcare Platform (Modules 1–5).

---

## 🌟 Core Product Experience

The platform is designed around **three distinct real-world user groups**:

### 1. Rural Patient (Primary, Elderly-First)
- **Zero Technical Friction**: Rural patients do not deal with "Healthcare Cases", "UUIDs", "DTOs", or complex multi-page routing.
- **Single-Page Continuous Journey**:
  `① Start -> ② Symptoms -> ③ Screening -> ④ Healthcare -> ⑤ Request`
- **Large Touch Targets & High Contrast**: Built with minimum 52px button heights, 18px base typography, and WCAG AAA compliant color contrasts.
- **Device Geolocation**: Automatically senses approximate device location (`navigator.geolocation`) without exposing raw coordinates to users, with graceful fallback.
- **Symptom Input**:
  - 📝 **Simple Form**: Common rural ailment chips (Fever, Cough, Breathing Issue, Chest Pain, Diarrhea, etc.), severity chips (`Mild`, `Moderate`, `Severe`), duration counters, and multi-symptom support.
  - 🤖 **Talk to Aarogya (Chatbot)**: Isolated conversational UI placeholder (clearly labeled as under development).
- **Patient-Friendly AI Screening**:
  - Risk Level badge (`⚠️ HIGH RISK`, `MODERATE RISK`, `LOW RISK`, `CRITICAL`)
  - Urgency level in plain language
  - Possible health conditions (diseases)
  - Precautions checklist ("What you should do now")
  - Collapsible technical metrics for healthcare transparency
- **Facility Discovery**:
  - PostGIS distance-ranked nearby PHCs, CHCs, and Hospitals
  - Available clinical services chips
  - Single-facility selection
- **Care Request Confirmation & Tracking**:
  - Send care request to the selected facility
  - Track status: `Waiting for Facility`, `Accepted`, `In Progress`, `Completed`, `Rejected`
  - **Re-Referral Support**: If a facility rejects the request (e.g. beds full), the patient can tap `[ Choose Another Facility ]` to re-refer the **SAME** healthcare case without re-entering symptoms.

### 2. ASHA Worker (Community Health Navigator)
- **Village Patient Directory**: Search by name or village, view patient profiles.
- **Quick Patient Registration**: Fast enrollment of rural villagers.
- **Assisted Screening**: Select an enrolled villager and run the full screening wizard on their behalf.
- **Cases & Referral Tracking**: Oversee community care referrals.

### 3. Healthcare Facility Staff / Doctor
- **Facility Switcher**: Select active workplace (e.g., Shirwal PHC, Khandala CHC, Satara Civil Hospital).
- **Care Request Triage**: Filter incoming requests by `Pending`, `Accepted`, `In Progress`, `Completed`, `Rejected`.
- **Full Clinical Review**: Inspect patient demographics, case consultation, recorded symptoms, and AI risk/urgency evaluation before deciding.
- **Action Workflow**:
  - `Accept Request` (with clinical response note)
  - `Reject Request` (with reason note, freeing the case for re-referral)
  - `Start Clinical Care` (`IN_PROGRESS`)
  - `Mark Complete` (`COMPLETED`)

---

## 🚫 Zero Admin UI

There are no admin roles, admin dashboards, or administrative management screens. The platform focuses solely on Patient, ASHA, and Facility workflows.

---

## 🚀 How to Run

### Step 1: Start the Spring Boot Backend (Terminal 1)
```powershell
mvn clean spring-boot:run
```
*Backend runs on `http://localhost:8081`.*

### Step 2: Start the React Frontend (Terminal 2)
```powershell
cd frontend
npm run dev
```
*Frontend runs on `http://localhost:5173`.*

Vite automatically proxies `/api` calls directly to `http://localhost:8081`.

---

## 📱 Responsive Testing
Tested and optimized for:
- 360px mobile viewports (low-end Android devices)
- 390px / 430px modern smartphones
- Tablets and Desktops
