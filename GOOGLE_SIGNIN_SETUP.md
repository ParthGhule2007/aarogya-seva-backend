# Native Google Sign-In Configuration Guide (Aarogya Seva)

This guide details how to configure Google Sign-In credentials across Google Cloud Console, the backend `oauth-service` (port 8087), and the React Native / Expo mobile app.

---

## 1. Google Cloud Platform (GCP) Setup

### Step A: Configure OAuth Consent Screen
1. Go to the [Google Cloud Console](https://console.cloud.google.com/).
2. Select your project (or create a new one, e.g., `aarogya-seva-prod`).
3. Navigate to **APIs & Services** > **OAuth consent screen**.
4. Select User Type **External** (or Internal for Google Workspace orgs) and click **Create**.
5. Fill in the required fields:
   - **App name**: `Aarogya Seva`
   - **User support email**: your admin email
   - **Developer contact information**: your developer email
6. Under **Scopes**, ensure `openid`, `email`, and `profile` are selected (these are default standard scopes).
7. Save and continue. If the app is in "Testing" publishing status, add your test Google account emails under **Test users**.

---

### Step B: Create OAuth Client IDs

You need **two** Client IDs in GCP:
1. **Web application client ID** (used by backend verification and mobile token exchange)
2. **Android client ID** (used by the Android app for native signing)

#### 1. Web Application Client ID
1. Navigate to **APIs & Services** > **Credentials**.
2. Click **+ Create Credentials** > **OAuth client ID**.
3. Select **Application type**: **Web application**.
4. Name: `Aarogya Seva Web / Backend Client`.
5. Click **Create**.
6. Note down the **Client ID** (format: `xxxxxxxxxxxx-xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx.apps.googleusercontent.com`).

#### 2. Android Client ID
1. Click **+ Create Credentials** > **OAuth client ID**.
2. Select **Application type**: **Android**.
3. Name: `Aarogya Seva Android Client`.
4. Package name: `com.ruralhealthcare.aarogyaseva` (as configured in `mobile/app.json`).
5. SHA-1 certificate fingerprint:
   - **For Local Debug Builds**:
     ```bash
     keytool -keystore "%USERPROFILE%\.android\debug.keystore" -list -v -alias androiddebugkey -storepass android -keypass android
     ```
     Copy the `SHA1` fingerprint.
   - **For EAS Production / Preview Builds**:
     Run:
     ```bash
     cd mobile
     npx eas-cli credentials
     ```
     Select Android -> build profile -> copy the SHA-1 fingerprint.
6. Click **Create**.

---

## 2. Backend Configuration (`oauth-service`)

The `oauth-service` microservice runs on port `8087` and verifies Google ID tokens using Google API Client.

1. Open `oauth-service/src/main/resources/application.yml` or set environment variable `GOOGLE_WEB_CLIENT_ID`:
   ```yaml
   google:
     web-client-id: ${GOOGLE_WEB_CLIENT_ID:YOUR_WEB_CLIENT_ID.apps.googleusercontent.com}
   ```
2. Re-run or restart `oauth-service`:
   ```powershell
   cd oauth-service
   mvn spring-boot:run
   ```

---

## 3. Mobile App Configuration (`mobile`)

### Step A: Configure Environment Variables
1. In `mobile/src/config/apiConfig.ts`:
   The default Web Client ID is loaded via `process.env.EXPO_PUBLIC_GOOGLE_WEB_CLIENT_ID`.
2. Add your Web Client ID to `mobile/.env`:
   ```env
   EXPO_PUBLIC_GOOGLE_WEB_CLIENT_ID=YOUR_WEB_CLIENT_ID.apps.googleusercontent.com
   ```
3. In `mobile/eas.json`, update the `preview` and `production` environment blocks:
   ```json
   "env": {
     "EXPO_PUBLIC_GOOGLE_WEB_CLIENT_ID": "YOUR_WEB_CLIENT_ID.apps.googleusercontent.com"
   }
   ```

---

## 4. Testing & Expo Go Compatibility

### Expo Go (SDK 57)
- Native Google Sign-In requires native binary modules (`@react-native-google-signin/google-signin`), which are not compiled into the stock Expo Go client.
- The app automatically detects Expo Go environment (`Constants.executionEnvironment === ExecutionEnvironment.StoreClient`).
- In Expo Go, the "Continue with Google" button is safely displayed as disabled with the badge: **"Available in installed app"**.
- This guarantees zero crashes or bundle failures in Expo Go SDK 57.

### Running in Development Build (Native Testing)
To test native Google Sign-in on an Android device or emulator:
1. Generate the native project:
   ```bash
   cd mobile
   npx expo run:android
   ```
   Or build a preview APK using EAS:
   ```bash
   cd mobile
   npx eas-cli build --profile preview --platform android
   ```
2. When testing, tap **Continue with Google**:
   - Google account picker opens natively.
   - If user already exists: logs directly into dashboard.
   - If user is new: redirects to Register Screen with Email and Name prefilled, requiring only role selection and profile details.
