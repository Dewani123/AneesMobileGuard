# Anees Mobile Guard — Final V2.0

A privacy-first Lost Phone Recovery app with optional local anti-tamper protection and Firebase/FCM remote controls.

## Final product behavior
- Daily use is quiet: movement and charger-removal alarms are OFF by default and no longer used as the main recovery flow.
- Lost Mode can be activated locally or remotely.
- Remote commands: Lost Mode, alarm, location request, device status.
- Lost Mode foreground protection periodically syncs last-known location and device status when permission/network are available.
- Device ID + pairing code protect remote commands.
- Airplane Mode can trigger a best-effort local alarm only when Guard Mode is armed; the app cannot force Airplane Mode off.
- PIN protection, alarm, vibration, flashlight, optional screen-wake guard, smartwatch guard, event history and privacy-first local assistant are retained.
- The app never reads contacts, SMS, call logs, photos/videos, arbitrary files, microphone recordings, clipboard, browser history or passwords.

## Firebase requirement
The Android project is intentionally buildable without Firebase configuration. For real remote recovery, add the Firebase Android configuration file at `app/google-services.json` and enable Anonymous Authentication, Firestore and Cloud Functions in your Firebase project. The web dashboard uses a Firebase Web config in `remote-dashboard/firebase-config.js`. These are project/account configuration steps that cannot be generated honestly without access to your Firebase project.

## Remote dashboard
`remote-dashboard/` is a ready static dashboard. It supports pairing by Device ID + Pairing Code and controls the phone through the callable Cloud Functions. Host it over HTTPS (Firebase Hosting is recommended).

## Build
The included GitHub Actions workflow uses Java 17, Android SDK 35 and Gradle 8.7 directly, so the missing Gradle wrapper JAR is not required by CI.

## Publishing
Before Google Play publishing, create a signed release build and complete the Play Console data-safety/privacy declarations truthfully. Remote functionality is only active after Firebase is configured.
