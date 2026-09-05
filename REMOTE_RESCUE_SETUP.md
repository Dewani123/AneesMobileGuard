# Remote Rescue / Lost Mode

The app contains the device-side Remote Rescue layer. For real remote commands from an owner's second phone/watch, connect a backend such as:
- Firebase Authentication for owner login
- Firebase Cloud Messaging (FCM) for command delivery
- Firestore or Realtime Database for device registration/status/audit
- Fused Location Provider for fresh location

Security requirements: authenticated owner, device binding, HTTPS, short-lived commands, nonce/timestamp replay protection, rate limits, revocation, and audit logs. Never put admin/service-account credentials in the APK.

Android limitations remain: a powered-off/dead phone cannot receive a command; no network means no remote command; force-stopped apps and OEM restrictions can block background execution; location permissions/settings must allow location access.
