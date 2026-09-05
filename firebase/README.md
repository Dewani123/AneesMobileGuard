# Firebase / FCM setup — Anees Mobile Guard

This folder contains the production backend layer for Remote Rescue.

## 1. Create Firebase project
Create a Firebase project and add an Android app with package:
`com.anees.mobileguard`

Download `google-services.json` and put the real file at:
`app/google-services.json`

Do **not** commit private server credentials. The Android config file is not a server credential, but still keep it inside your own repository as appropriate for your Firebase project.

## 2. Enable services
Enable Authentication (Anonymous is used by the current device-registration foundation), Cloud Firestore and Cloud Messaging.

## 3. Deploy Firestore rules and Functions
From the `firebase/` directory, initialize Firebase CLI for the project, copy `firestore.rules`, and deploy the functions in `functions/`.

The callable function accepts only these anti-theft commands:
- ACTIVATE_LOST_MODE
- DEACTIVATE_LOST_MODE
- PLAY_ALARM
- REQUEST_LOCATION
- REQUEST_DEVICE_STATUS

## Privacy boundary
The app does not read or upload contacts, SMS, call logs, photos, videos, files, microphone recordings, clipboard contents, browser history or passwords.
Remote telemetry is limited to device security state, FCM token, app version and optional last-known location when location permission is granted.

Location is sensitive data, so the user must explicitly grant Android location permission. The app should only use it for Find My Phone / Remote Rescue.
