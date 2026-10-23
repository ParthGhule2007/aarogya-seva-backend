// Scripts for firebase and firebase messaging in service worker
importScripts('https://www.gstatic.com/firebasejs/10.13.2/firebase-app-compat.js');
importScripts('https://www.gstatic.com/firebasejs/10.13.2/firebase-messaging-compat.js');

const firebaseConfig = {
  apiKey: "AIzaSyAGVpvEsUDlVol--5M2cr_LbTf_-Y_V6vc",
  authDomain: "aarogya-seva-e8f23.firebaseapp.com",
  projectId: "aarogya-seva-e8f23",
  storageBucket: "aarogya-seva-e8f23.firebasestorage.app",
  messagingSenderId: "53616066679",
  appId: "1:53616066679:web:f58bb54e9b5fe8eda7c672"
};

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
