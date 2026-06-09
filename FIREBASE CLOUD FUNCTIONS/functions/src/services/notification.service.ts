import * as admin from "firebase-admin";
import * as logger from "firebase-functions/logger";
import { NotificationPayload } from "../types";

const db = admin.firestore();

/**
 * Persists a notification document to notifications/{uid}/items/{autoId}.
 *
 * This is the SINGLE entry point for all Firestore notification writes —
 * both immediate status-change notifications and scheduled reminder notifications
 * flow through here to guarantee structural consistency with the schema.
 *
 * Document structure written:
 * {
 *   title, body, type, deepLinkUrl,
 *   isRead: false,
 *   isActive: true,
 *   timestamp: <server timestamp>
 * }
 *
 * @throws  Re-throws Firestore errors — callers should use Promise.allSettled
 *          so a write failure for one stakeholder doesn't block the other.
 */
export async function writeNotification(
  uid: string,
  payload: NotificationPayload
): Promise<void> {
  const ref = db
    .collection("notifications")
    .doc(uid)
    .collection("items")
    .doc(); // auto-ID

  await ref.set({
    title: payload.title,
    body: payload.body,
    type: payload.type,
    deepLinkUrl: payload.deepLinkUrl ?? null,
    isRead: false,
    isActive: true,
    timestamp: admin.firestore.FieldValue.serverTimestamp(),
  });

  logger.info(
    `[NotificationService] Written | uid=${uid} type=${payload.type}`
  );
}