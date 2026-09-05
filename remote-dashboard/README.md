# Anees Mobile Guard — Remote Recovery Dashboard

This static dashboard controls a paired phone through Firebase Cloud Functions.

1. Copy `firebase-config.example.js` to `firebase-config.js` and add your Firebase Web app config.
2. Enable Firebase Authentication (Anonymous is enough for this pairing-code dashboard), Firestore and Cloud Functions.
3. Deploy the dashboard with Firebase Hosting or another HTTPS static host.
4. On the phone, open Remote Rescue and copy the Device ID + Pairing Code.

The dashboard never receives contacts, SMS, photos, files, calls, microphone recordings or passwords.
