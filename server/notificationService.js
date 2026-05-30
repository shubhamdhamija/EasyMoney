const admin = require('firebase-admin');
const fs = require('fs');

let initialized = false;
function initFirebase() {
  if (initialized) return;
  const keyPath = process.env.FIREBASE_SERVICE_ACCOUNT_JSON || '';
  if (keyPath && fs.existsSync(keyPath)) {
    const serviceAccount = require(keyPath);
    admin.initializeApp({ credential: admin.credential.cert(serviceAccount) });
    initialized = true;
  }
}

async function sendFCM(token, message) {
  try {
    initFirebase();
    if (!initialized) {
      console.log('FCM not configured; message would be:', token, message);
      return { ok: false, reason: 'not-configured' };
    }
    const payload = {
      token,
      notification: {
        title: message.title,
        body: message.body
      }
    };
    const resp = await admin.messaging().send(payload);
    return { ok: true, resp };
  } catch (e) {
    console.error('FCM error', e.message || e);
    return { ok: false, reason: e.message };
  }
}

async function sendNotification(token, title, body) {
  // For FCM, message = { title, body }
  return sendFCM(token, { title, body });
}

module.exports = { sendFCM, sendNotification };
