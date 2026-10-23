/// <reference types="vite/client" />

interface ImportMetaEnv {
  readonly VITE_FIREBASE_API_KEY: string;
  readonly VITE_FIREBASE_AUTH_DOMAIN: string;
  readonly VITE_FIREBASE_PROJECT_ID: string;
  readonly VITE_FIREBASE_STORAGE_BUCKET: string;
  readonly VITE_FIREBASE_MESSAGING_SENDER_ID: string;
  readonly VITE_FIREBASE_APP_ID: string;
  readonly VITE_FIREBASE_MEASUREMENT_ID?: string;
  readonly VITE_FIREBASE_VAPID_KEY: string;
  readonly VITE_API_BASE_URL?: string;
  readonly VITE_DOCTOR_SERVICE_URL?: string;
  readonly VITE_CONSULTATION_SERVICE_URL?: string;
  readonly VITE_COMMUNICATION_SERVICE_URL?: string;
  readonly VITE_SOS_SERVICE_URL?: string;
  readonly VITE_CHAT_SERVICE_URL?: string;
  readonly VITE_MEDICATION_SERVICE_URL?: string;
  readonly VITE_CALLING_SERVICE_URL?: string;
  readonly VITE_JITSI_DOMAIN?: string;
  readonly [key: string]: any;
}

interface ImportMeta {
  readonly env: ImportMetaEnv;
}
