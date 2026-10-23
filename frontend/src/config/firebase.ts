import { initializeApp, getApps, getApp } from 'firebase/app';
import { getMessaging, getToken, onMessage, isSupported, Messaging } from 'firebase/messaging';

// Public Web Configuration loaded via environment variables
const firebaseConfig = {
  apiKey: import.meta.env.VITE_FIREBASE_API_KEY,
  authDomain: import.meta.env.VITE_FIREBASE_AUTH_DOMAIN,
  projectId: import.meta.env.VITE_FIREBASE_PROJECT_ID,
  storageBucket: import.meta.env.VITE_FIREBASE_STORAGE_BUCKET,
  messagingSenderId: import.meta.env.VITE_FIREBASE_MESSAGING_SENDER_ID,
  appId: import.meta.env.VITE_FIREBASE_APP_ID,
  measurementId: import.meta.env.VITE_FIREBASE_MEASUREMENT_ID,
};

// Initialize Firebase client app
export const app = getApps().length === 0 ? initializeApp(firebaseConfig) : getApp();

let messagingInstance: Messaging | null = null;

export const getFirebaseMessaging = async (): Promise<Messaging | null> => {
  if (typeof window === 'undefined') return null;
  const supported = await isSupported().catch(() => false);
  if (!supported) {
    console.warn('[FCM] Firebase Messaging is not supported in this browser environment.');
    return null;
  }
  if (!messagingInstance) {
    messagingInstance = getMessaging(app);
  }
  return messagingInstance;
};

/**
 * Request notification permission and obtain real FCM Web Push device registration token.
 */
export const requestFcmToken = async (): Promise<string | null> => {
  try {
    if (typeof window === 'undefined' || !('Notification' in window)) {
      console.warn('[FCM] Notifications not supported in this browser.');
      return null;
    }

    const permission = await Notification.requestPermission();
    if (permission !== 'granted') {
      console.warn('[FCM] Notification permission was denied by the user.');
      return null;
    }

    const messaging = await getFirebaseMessaging();
    if (!messaging) return null;

    // Register service worker if not already registered
    let registration: ServiceWorkerRegistration | undefined;
    if ('serviceWorker' in navigator) {
      registration = await navigator.serviceWorker.register('/firebase-messaging-sw.js');
      await navigator.serviceWorker.ready;
    }

    const vapidKey = import.meta.env.VITE_FIREBASE_VAPID_KEY;
    const token = await getToken(messaging, {
      vapidKey,
      serviceWorkerRegistration: registration,
    });

    if (token) {
      console.log('[FCM] Device Push Token successfully generated:', token.substring(0, 10) + '...');
      return token;
    } else {
      console.warn('[FCM] No registration token available. Request permission to generate one.');
      return null;
    }
  } catch (error) {
    console.error('[FCM] An error occurred while retrieving token:', error);
    return null;
  }
};

/**
 * Subscribe to foreground push notifications.
 */
export const onForegroundMessage = async (callback: (payload: any) => void) => {
  const messaging = await getFirebaseMessaging();
  if (messaging) {
    return onMessage(messaging, (payload) => {
      console.log('[FCM] Foreground message received:', payload);
      callback(payload);
    });
  }
  return () => {};
};
