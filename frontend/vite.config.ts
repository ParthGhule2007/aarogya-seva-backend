import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

// https://vitejs.dev/config/
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      '/api/v1/medications': {
        target: 'http://localhost:8088',
        changeOrigin: true,
        secure: false,
      },
      '/api/v1/medication-events': {
        target: 'http://localhost:8088',
        changeOrigin: true,
        secure: false,
      },
      '/api/v1/caregivers': {
        target: 'http://localhost:8088',
        changeOrigin: true,
        secure: false,
      },
      '/api/v1/chat': {
        target: 'http://localhost:8085',
        changeOrigin: true,
        secure: false,
      },
      '/api/v1/sos': {
        target: 'http://localhost:8082',
        changeOrigin: true,
        secure: false,
      },
      '/api/v1/devices': {
        target: 'http://localhost:8082',
        changeOrigin: true,
        secure: false,
      },
      '/api/doctors': {
        target: 'http://localhost:8086',
        changeOrigin: true,
        secure: false,
      },
      '/api/consultations': {
        target: 'http://localhost:8083',
        changeOrigin: true,
        secure: false,
      },
      '/api/communications': {
        target: 'http://localhost:8084',
        changeOrigin: true,
        secure: false,
      },
      '/api/v1/calls': {
        target: 'http://localhost:8089',
        changeOrigin: true,
        secure: false,
      },
      '/api/v1/follow-up': {
        target: 'http://localhost:8089',
        changeOrigin: true,
        secure: false,
      },
      '/api/v1/tools': {
        target: 'http://localhost:8089',
        changeOrigin: true,
        secure: false,
      },
      '/api': {
        target: 'http://localhost:8081',
        changeOrigin: true,
        secure: false,
      },
    },
  },
});
