const { onCall, HttpsError } = require('firebase-functions/v2/https');
const { initializeApp } = require('firebase-admin/app');
const { getFirestore } = require('firebase-admin/firestore');
const { getMessaging } = require('firebase-admin/messaging');

initializeApp();
const db = getFirestore();

// The calling owner must authenticate with Firebase. Never put an Admin SDK
// credential or server secret in the Android APK.
exports.sendRemoteCommand = onCall(async (request) => {
  if (!request.auth) throw new HttpsError('unauthenticated', 'Authentication required');
  const { deviceId, command, pairingSecret } = request.data || {};
  if (!pairingSecret) throw new HttpsError('unauthenticated', 'Pairing code required');
  const allowed = new Set([
    'ACTIVATE_LOST_MODE', 'DEACTIVATE_LOST_MODE', 'PLAY_ALARM',
    'REQUEST_LOCATION', 'REQUEST_DEVICE_STATUS'
  ]);
  if (!deviceId || !allowed.has(command)) throw new HttpsError('invalid-argument', 'Invalid command');

  const ref = db.collection('devices').doc(deviceId);
  const snap = await ref.get();
  if (!snap.exists || !snap.data().pairingSecretHash) {
    throw new HttpsError('not-found', 'Device not registered');
  }
  const crypto = require('crypto');
  const suppliedHash = crypto.createHash('sha256').update(String(pairingSecret)).digest('hex');
  if (suppliedHash !== snap.data().pairingSecretHash) {
    throw new HttpsError('permission-denied', 'Invalid pairing code');
  }
  const token = snap.data().fcmToken;
  if (!token) throw new HttpsError('failed-precondition', 'Device is not registered for push');

  await getMessaging().send({
    token,
    data: { command, issuedAt: String(Date.now()) },
    android: { priority: 'high' }
  });
  return { ok: true };
});


exports.getDeviceStatus = onCall(async (request) => {
  if (!request.auth) throw new HttpsError('unauthenticated', 'Authentication required');
  const { deviceId, pairingSecret } = request.data || {};
  if (!deviceId || !pairingSecret) throw new HttpsError('unauthenticated', 'Pairing code required');
  const ref = db.collection('devices').doc(deviceId);
  const snap = await ref.get();
  if (!snap.exists || !snap.data().pairingSecretHash) throw new HttpsError('not-found', 'Device not registered');
  const crypto = require('crypto');
  const suppliedHash = crypto.createHash('sha256').update(String(pairingSecret)).digest('hex');
  if (suppliedHash !== snap.data().pairingSecretHash) throw new HttpsError('permission-denied', 'Invalid pairing code');
  const d = snap.data();
  return { deviceId: d.deviceId, appVersion: d.appVersion, lastSeen: d.lastSeen || null, batteryPercent: d.batteryPercent ?? null, guardMode: !!d.guardMode, lostMode: !!d.lostMode, lastLocation: d.lastLocation || null };
});
