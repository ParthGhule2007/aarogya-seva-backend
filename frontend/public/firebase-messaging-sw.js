// Scripts for firebase and firebase messaging in service worker
importScripts('https://www.gstatic.com/firebasejs/10.13.2/firebase-app-compat.js');
importScripts('https://www.gstatic.com/firebasejs/10.13.2/firebase-messaging-compat.js');

const params = new URL(location).searchParams;
const firebaseConfig = {
  apiKey: params.get('apiKey') || "YOUR_FIREBASE_API_KEY",
  authDomain: params.get('authDomain') || "YOUR_FIREBASE_AUTH_DOMAIN",
  projectId: params.get('projectId') || "YOUR_FIREBASE_PROJECT_ID",
  storageBucket: params.get('storageBucket') || "YOUR_FIREBASE_STORAGE_BUCKET",
  messagingSenderId: params.get('messagingSenderId') || "YOUR_FIREBASE_MESSAGING_SENDER_ID",
  appId: params.get('appId') || "YOUR_FIREBASE_APP_ID"
};

if (firebaseConfig.apiKey && firebaseConfig.apiKey !== "YOUR_FIREBASE_API_KEY") {
  firebase.initializeApp(firebaseConfig);
  const messaging = firebase.messaging();

  messaging.onBackgroundMessage((payload) => {
    console.log('[firebase-messaging-sw.js] Received background message: ', payload);
    const notificationTitle = payload.notification?.title || '🚨 Aarogya Seva Alert';
    const notificationOptions = {
      body: payload.notification?.body || 'Emergency notification received.',
      icon: '/vite.svg',
      data: payload.data
    };

    self.registration.showNotification(notificationTitle, notificationOptions);
  });
}
