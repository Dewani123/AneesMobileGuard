# One-time Firebase setup for remote recovery

This is external account configuration, not a code update.

1. Create/select a Firebase project.
2. Add Android app package `com.anees.mobileguard`; download `google-services.json` into `app/`.
3. Enable Anonymous Authentication.
4. Create Firestore and deploy `firebase/firestore.rules`.
5. Deploy `firebase/functions`.
6. Add a Web app and put its config into `remote-dashboard/firebase-config.js` using the included example.
7. Deploy `remote-dashboard` over HTTPS.

Never put Firebase Admin credentials in the APK or web dashboard.
