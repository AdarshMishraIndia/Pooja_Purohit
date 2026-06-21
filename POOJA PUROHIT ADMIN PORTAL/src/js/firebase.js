/**
 * firebase.js
 * Initialises the Firebase app and exports the Firestore instance.
 * Must be loaded after firebase-app-compat.js, firebase-firestore-compat.js,
 * and pooja-purohit-admin-portal-config.js.
 */

firebase.initializeApp(FIREBASE_CONFIG);

// Single shared Firestore reference used by all modules.
const db = firebase.firestore();
