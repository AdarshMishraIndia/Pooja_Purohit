import * as admin from "firebase-admin";
import * as logger from "firebase-functions/logger";
import { NotificationPayload } from "../types";

const db = admin.firestore();

/**
 * Sends an FCM push notification to every registered device of the given stakeholder.
 *
 * Flow:
 * 1. Fetch fcmTokens[] from the Firestore document (users or purohits collection).
 * 2. Fire multicast push via sendEachForMulticast (supports up to 500 tokens).
 * 3. Prune permanently-invalid tokens from Firestore to prevent future noise.
 *
 * Stale token codes that trigger pruning:
 *   - messaging/invalid-registration-token
 *   - messaging/registration-token-not-registered
 *
 * Note: Transient errors (quota-exceeded, internal, etc.) are logged but do NOT
 * trigger pruning — the token may succeed on a retry.
 *
 * @param uid        Firestore document ID of the recipient
 * @param collection "users" or "purohits" — determines which collection to look up
 * @param payload    Notification content + metadata
 */
export async function sendPushNotification(
  uid: string,
  collection: "users" | "purohits",
  payload: NotificationPayload
): Promise<void> {
  // --- 1. Fetch token list ---
  const docSnap = await db.collection(collection).doc(uid).get();

  if (!docSnap.exists) {
    logger.warn(`[FCMService] Document not found: ${collection}/${uid}. Push skipped.`);
    return;
  }

  const tokens: string[] = docSnap.data()?.fcmTokens ?? [];

  if (tokens.length === 0) {
    logger.info(`[FCMService] No FCM tokens for ${collection}/${uid}. Push skipped.`);
    return;
  }

  // --- 2. Send multicast push ---
  const message: admin.messaging.MulticastMessage = {
    tokens,
    notification: {
      title: payload.title,
      body: payload.body,
    },
    // `data` carries structured metadata consumed by the Android app
    // for deep link routing and notification channel assignment.
    data: {
      type: payload.type,
      deepLinkUrl: payload.deepLinkUrl ?? "",
    },
    android: {
      priority: "high",
      notification: {
        sound: "default",
        // Android 8+ requires a channel ID. Must match the channel registered
        // in the Android app (NotificationChannel setup in MainActivity / App class).
        channelId: "booking_notifications",
      },
    },
  };

  const result = await admin.messaging().sendEachForMulticast(message);

  logger.info(
    `[FCMService] Push result | ${collection}/${uid} | ` +
    `success=${result.successCount} failed=${result.failureCount}`
  );

  // --- 3. Prune permanently invalid tokens ---
  const staleTokens = tokens.filter((_, i) => {
    const code = result.responses[i].error?.code;
    return (
      code === "messaging/invalid-registration-token" ||
      code === "messaging/registration-token-not-registered"
    );
  });

  if (staleTokens.length > 0) {
    // arrayRemove is atomic and idempotent — safe to call even if token was
    // already removed by another concurrent function invocation.
    await db.collection(collection).doc(uid).update({
      fcmTokens: admin.firestore.FieldValue.arrayRemove(...staleTokens),
    });
    logger.info(
      `[FCMService] Pruned ${staleTokens.length} stale token(s) from ${collection}/${uid}`
    );
  }
}