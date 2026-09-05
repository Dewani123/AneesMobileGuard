# Privacy Scope — Anees Mobile Guard

## Data the app does NOT access
- Contacts
- SMS / MMS
- Call history
- Photos / videos
- User documents / arbitrary files
- Microphone recordings
- Camera images or video (the camera hardware is only used for the optional flashlight feature)
- Clipboard contents
- Browser history
- Passwords or other credentials stored outside this app

## Security telemetry
The anti-theft system may use:
- Device-generated ID
- Guard/Lost Mode state
- App version
- FCM push token
- Battery state shown locally
- Motion/charger/screen security events
- Last-known device location when the user grants Android location permission

Remote location is not silently collected without the required Android permission.

## Firebase
Firebase/FCM is used only as the transport/backend for Remote Rescue. No Firebase Admin/server credentials belong in the APK.
