/**
 * Service URLs configured for physical devices.
 * Uses EXPO_PUBLIC_* environment variables with fallback to LAN IP.
 * Never uses 10.0.2.2 or localhost so physical phones can connect over Wi-Fi.
 */
const HOST = process.env.EXPO_PUBLIC_API_HOST ?? 'http://192.168.0.101';

export const IS_DEMO_MODE = process.env.EXPO_PUBLIC_DEMO_MODE === 'true';

function parseMockLocation(raw?: string): { latitude: number; longitude: number } | null {
  if (!raw) return null;
  const parts = raw.split(',').map((p) => parseFloat(p.trim()));
  if (parts.length === 2 && !isNaN(parts[0]) && !isNaN(parts[1])) {
    return { latitude: parts[0], longitude: parts[1] };
  }
  return null;
}

export const MOCK_LOCATION = parseMockLocation(process.env.EXPO_PUBLIC_MOCK_LOCATION);

export const SERVICE_URLS = {
  CORE: process.env.EXPO_PUBLIC_CORE_URL ?? `${HOST}:8081`,
  SOS: process.env.EXPO_PUBLIC_SOS_URL ?? `${HOST}:8082`,
  CONSULTATION: process.env.EXPO_PUBLIC_CONSULTATION_URL ?? `${HOST}:8083`,
  COMMUNICATION: process.env.EXPO_PUBLIC_COMMUNICATION_URL ?? `${HOST}:8084`,
  CHATBOT: process.env.EXPO_PUBLIC_CHATBOT_URL ?? `${HOST}:8085`,
  DOCTOR: process.env.EXPO_PUBLIC_DOCTOR_URL ?? `${HOST}:8086`,
  MEDICINE: process.env.EXPO_PUBLIC_MEDICINE_URL ?? `${HOST}:8088`,
  CALLING: process.env.EXPO_PUBLIC_CALLING_URL ?? `${HOST}:8089`,
  OAUTH: process.env.EXPO_PUBLIC_OAUTH_URL ?? `${HOST}:8087`,
};

export const GOOGLE_WEB_CLIENT_ID = process.env.EXPO_PUBLIC_GOOGLE_WEB_CLIENT_ID ?? '';
