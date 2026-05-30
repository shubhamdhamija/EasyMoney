const { generateEventInsight } = require('./eventInsightService');
const { sendNotification } = require('../notificationService');

// Deduplication cache (in-memory, for demo)
const recentEvents = new Map();
const EVENT_TTL_MS = 60 * 60 * 1000; // 1 hour

function isDuplicate(event) {
  const key = `${event.type}:${event.symbol}`;
  const now = Date.now();
  if (recentEvents.has(key) && now - recentEvents.get(key) < EVENT_TTL_MS) return true;
  recentEvents.set(key, now);
  return false;
}

async function handleEvent(event, users) {
  try {
    if (isDuplicate(event)) {
      console.log('Duplicate event, skipping:', event);
      return;
    }
    const insight = await generateEventInsight(event);
    for (const user of users) {
      if (!user.deviceToken) continue;
      await sendNotification(
        user.deviceToken,
        `📊 ${event.symbol} Alert`,
        (insight || '').substring(0, 200)
      );
    }
    // Log event
    console.log('Event processed:', event, 'Insight:', insight);
  } catch (err) {
    console.error('Event processing failed:', err);
  }
}

module.exports = { handleEvent };

