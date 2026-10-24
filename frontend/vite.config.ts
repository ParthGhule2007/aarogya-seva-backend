import { defineConfig, loadEnv } from 'vite';
import react from '@vitejs/plugin-react';

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '');

  const USE_LOCAL = env.VITE_USE_LOCAL_BACKEND === 'true';

  const CORE_TARGET = USE_LOCAL ? 'http://localhost:8081' : (env.VITE_CORE_URL || 'https://aarogya-core-service.onrender.com');
  const MED_TARGET = USE_LOCAL ? 'http://localhost:8088' : (env.VITE_MEDICATION_SERVICE_URL || 'https://arogya-medicine-service.onrender.com');
  const CHAT_TARGET = USE_LOCAL ? 'http://localhost:8085' : (env.VITE_CHAT_SERVICE_URL || 'https://aarogya-chatbot-service-s7ra.onrender.com');
  const SOS_TARGET = USE_LOCAL ? 'http://localhost:8082' : (env.VITE_SOS_SERVICE_URL || 'https://aarogya-sos-service-yjfs.onrender.com');
  const DOC_TARGET = USE_LOCAL ? 'http://localhost:8086' : (env.VITE_DOCTOR_SERVICE_URL || 'https://aarogya-doctor-service-e7gs.onrender.com');
  const CONSULT_TARGET = USE_LOCAL ? 'http://localhost:8083' : (env.VITE_CONSULTATION_SERVICE_URL || 'https://aarogya-consultation-service-wksh.onrender.com');
  const COMM_TARGET = USE_LOCAL ? 'http://localhost:8084' : (env.VITE_COMMUNICATION_SERVICE_URL || 'https://aarogya-communication-service-25yt.onrender.com');
  const CALL_TARGET = USE_LOCAL ? 'http://localhost:8089' : (env.VITE_CALLING_SERVICE_URL || 'https://aarogya-calling-service-ppbx.onrender.com');

  return {
    plugins: [react()],
    server: {
      port: 5173,
      proxy: {
        '/api/v1/medications': { target: MED_TARGET, changeOrigin: true, secure: false },
        '/api/v1/medication-events': { target: MED_TARGET, changeOrigin: true, secure: false },
        '/api/v1/caregivers': { target: MED_TARGET, changeOrigin: true, secure: false },
        '/api/v1/chat': { target: CHAT_TARGET, changeOrigin: true, secure: false },
        '/api/v1/sos': { target: SOS_TARGET, changeOrigin: true, secure: false },
        '/api/v1/devices': { target: SOS_TARGET, changeOrigin: true, secure: false },
        '/api/doctors': { target: DOC_TARGET, changeOrigin: true, secure: false },
        '/api/consultations': { target: CONSULT_TARGET, changeOrigin: true, secure: false },
        '/api/communications': { target: COMM_TARGET, changeOrigin: true, secure: false },
        '/api/v1/calls': { target: CALL_TARGET, changeOrigin: true, secure: false },
        '/api/v1/follow-up': { target: CALL_TARGET, changeOrigin: true, secure: false },
        '/api/v1/tools': { target: CALL_TARGET, changeOrigin: true, secure: false },
        '/api': { target: CORE_TARGET, changeOrigin: true, secure: false },
      },
    },
  };
});
