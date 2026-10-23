# 📱 Aarogya Seva (आरोग्य सेवा) — Mobile App (Expo & React Native)

A rural telemedicine and care-navigation mobile app designed for rural patients and field healthcare workers. Connects directly to the Spring Boot microservices backend ecosystem.

---

## 🚀 How to Run in Expo Go (Real Android Device)

### 1. Prerequisites
- Node.js 18+ or 22+
- Android phone with the **Expo Go** app installed from Google Play Store
- Both your development machine and your phone connected to the **SAME Wi-Fi network**

### 2. Configure Your Computer's LAN IP
Open `.env` (or copy from `.env.example`):
```bash
# Find your computer's Wi-Fi IP using ipconfig (Windows) or ifconfig / ip a (Mac/Linux)
# e.g., 192.168.1.50
EXPO_PUBLIC_API_HOST=http://192.168.1.50
```
> [!IMPORTANT]
> **NEVER use `localhost` or `10.0.2.2`** in `.env` when testing with Expo Go on a physical phone, as the phone will try to connect to itself.

### 3. Start the Expo Dev Server on Port 8090
Metro's default port (`8081`) clashes with the Core Spring Boot service. The app is preconfigured to use port **8090**:

```bash
cd mobile
npm start
```
*(Runs `expo start --port 8090`)*

Scan the QR code displayed in your terminal using the **Expo Go** app on your phone.

---

## 📦 Building Standalone APK via EAS Build

### 1. Preview APK Profile
The project includes a ready-to-use `preview` profile in `eas.json` that outputs a standalone installable `.apk`:

```bash
# Install EAS CLI globally if not already installed
npm install -g eas-cli

# Login to your Expo account
eas login

# Build standalone APK
eas build -p android --profile preview
```

### 2. Updating Environment Variables for Standalone APK
- In standalone APKs, `.env` is **not read** at runtime; variables prefixed with `EXPO_PUBLIC_*` are inlined at build time.
- Update the `env` block inside `eas.json` before triggering a build.
- **Any change to backend URLs requires a rebuild of the APK.**

### 3. Cleartext Traffic Note
- Cleartext HTTP traffic is enabled via `expo-build-properties` in `app.json` for development on local network HTTP (`http://192.168.x.x`).
- Once the backend microservices are deployed behind HTTPS (e.g. Cloudflare tunnels or cloud domain), cleartext traffic can be safely set to `false`.

---

## 🔑 Pre-Seeded Demo Accounts (Password for all: `Password@123`)

The login screen features 1-tap quick fill buttons for hackathon demonstration:
- **Patient**: `patient.ramesh` (Patient UUID: `b1000000-0000-0000-0000-000000000001`)
- **ASHA Worker**: `asha.sunita`
- **Doctor**: `doctor.ananya` (Doctor ID: `1`)
- **Caregiver**: `caregiver.suresh`
- **Facility Admin**: `admin` (Facility UUID: `f1000000-0000-0000-0000-000000000001`)

---

## 🏗️ Architecture & Features in this Release

1. **Central HTTP Client (`src/api/client.ts`)**:
   - Manages JWT Bearer tokens in `expo-secure-store`
   - 15-second AbortController timeout
   - Automatically unwraps `{ success: true, data: T }` from Core service or raw JSON from microservices
   - Emits 401 events to auto-logout expired sessions
2. **Localization (`src/i18n`)**:
   - Supports English, Hindi (हिन्दी), and Marathi (मराठी) with instant runtime toggle
3. **Theming (`src/theme`)**:
   - High-contrast clinical palette matching the web client (Teal #0D9488 / Emerald #059669)
   - Dark mode toggle with persisted preference
4. **Patient Medication Cabinet**:
   - Daily schedule divided into Morning, Afternoon, Evening, and Night
   - 1-tap "Mark Taken" event logging with optimistic UI update
5. **Emergency SOS**:
   - Floating red SOS button reachable from all screens
   - Rapid GPS fix with 3s timeout and rural fallback coordinates
   - Direct speed dials to 108 (Ambulance) and 112 (National Emergency)
   - Multi-target digital broadcast (Caregivers, Nearby Responders, Nearest Facility)
6. **Teleconsultations**:
   - Lists active & past appointments
   - Book appointment modal with doctor roster selection
   - 1-tap "Join Video Consultation" via Jitsi WebRTC URL
7. **AI Voice Calling (Aarogya Mitra)**:
   - Initiates interactive doctor phone calls in Hindi, Marathi, or English via Twilio + Ultravox
   - Follow-up check-in trigger
8. **Geospatial Care Discovery**:
   - PostGIS nearby search for Sub-Centres, PHCs, CHCs, and District Hospitals
   - Displays distance in kilometers and 1-tap facility calling
