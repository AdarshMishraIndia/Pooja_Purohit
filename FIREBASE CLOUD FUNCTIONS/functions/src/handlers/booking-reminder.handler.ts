import { onTaskDispatched } from "firebase-functions/v2/tasks";
import * as admin from "firebase-admin";
import * as logger from "firebase-functions/logger";
import {
  REGION,
  MAX_PUROHIT_REMINDER_ATTEMPTS,
  REMINDER_INTERVAL_SECONDS,
} from "../config/constants";
import {
  BookingData,
  BookingReminderPayload,
  NotificationPayload,
} from "../types";
import { writeNotification } from "../services/notification.service";
import { sendPushNotification } from "../services/fcm.service";
import { enqueueBookingReminder } from "../services/tasks.service";

const db = admin.firestore();

/**
 * Cloud Tasks handler: 15-minute purohit acceptance reminder chain.
 *
 * Chain lifecycle:
 * ┌───────────────────────────────────────────────────────────────────┐
 * │  PAYMENT_DONE transition                                          │
 * │       │                                                           │
 * │       ▼  (fires after 15min, attempt=1)                          │
 * │  ┌── status still PAYMENT_DONE?                                   │
 * │  │       YES → notify purohit                                     │
 * │  │             attemptNumber < MAX?                               │
 * │  │                 YES → re-enqueue (attempt+1)                   │
 * │  │                 NO  → auto-cancel booking → chain ends        │
 * │  │       NO  → return (status changed, chain ends naturally)     │
 * │  └──────────────────────────────────────────────────────────────  │
 * └───────────────────────────────────────────────────────────────────┘
 *
 * Auto-cancel flow (max attempts exceeded):
 *   1. Write `cancellationReason: "NO_PUROHIT_RESPONSE"` to booking doc.
 *   2. Update `status: "AUTO_CANCELLED"`.
 *   3. onBookingStatusUpdated fires, reads cancellationReason, sends both
 *      the user and purohit appropriate "no response" notifications.
 *   Steps 1 & 2 are separate writes intentionally — if step 2 fails and retries,
 *   step 1 is idempotent (same value written again). This prevents the trigger
 *   from firing with a missing cancellationReason.
 */
export const processBookingReminder = onTaskDispatched<BookingReminderPayload>(
  {
    region: REGION,
    retryConfig: {
      maxAttempts: 3,
      minBackoffSeconds: 10,
      maxBackoffSeconds: 60,
      maxDoublings: 2,
    },
    rateLimits: { maxConcurrentDispatches: 20 },
  },
  async (request) => {
    const { bookingId, attemptNumber } = request.data;

    logger.info(
      `[BookingReminder] Task fired | bookingId=${bookingId} attempt=${attemptNumber}/${MAX_PUROHIT_REMINDER_ATTEMPTS}`
    );

    const bookingRef = db.collection("bookings").doc(bookingId);
    const bookingSnap = await bookingRef.get();

    if (!bookingSnap.exists) {
      logger.warn(`[BookingReminder] Booking not found: ${bookingId}. Chain terminated.`);
      return;
    }

    const booking = bookingSnap.data() as BookingData;

    // --- Self-termination: status moved on ---
    if (booking.status !== "PAYMENT_DONE") {
      logger.info(
        `[BookingReminder] Chain terminated | bookingId=${bookingId} ` +
        `status=${booking.status} (no longer PAYMENT_DONE)`
      );
      return;
    }

    // --- Send reminder to purohit ---
    const deepLink = `poojapurohit://bookings/${bookingId}`;
    const reminderPayload: NotificationPayload = {
      title: "Pending Booking Request",
      body: `You have a pending booking request for ${booking.serviceName}. Please accept or reject to avoid auto-cancellation.`,
      type: "BOOKING_REMINDER",
      deepLinkUrl: deepLink,
    };

    const notifResults = await Promise.allSettled([
      writeNotification(booking.purohitId, reminderPayload),
      sendPushNotification(booking.purohitId, "purohits", reminderPayload),
    ]);

    notifResults.forEach((result, i) => {
      if (result.status === "rejected") {
        const op = i === 0 ? "Firestore write" : "FCM push";
        logger.error(`[BookingReminder] ${op} failed | purohitId=${booking.purohitId}`, result.reason);
      }
    });

    // --- Continue or terminate the chain ---
    if (attemptNumber < MAX_PUROHIT_REMINDER_ATTEMPTS) {
      // Continue chain
      await enqueueBookingReminder({ bookingId, attemptNumber: attemptNumber + 1 });
      logger.info(
        `[BookingReminder] Reminder sent, chain continued | bookingId=${bookingId} ` +
        `next attempt=${attemptNumber + 1} in ${REMINDER_INTERVAL_SECONDS / 60}min`
      );
    } else {
      // Max attempts reached — auto-cancel the booking.
      // Write cancellationReason FIRST so the Firestore trigger can read it
      // before building the notification messages.
      logger.info(
        `[BookingReminder] Max attempts reached | bookingId=${bookingId}. Initiating auto-cancel.`
      );

      await bookingRef.update({
        cancellationReason: "NO_PUROHIT_RESPONSE",
        updatedAt: admin.firestore.FieldValue.serverTimestamp(),
      });

      await bookingRef.update({
        status: "AUTO_CANCELLED",
        updatedAt: admin.firestore.FieldValue.serverTimestamp(),
      });

      logger.info(`[BookingReminder] Booking auto-cancelled | bookingId=${bookingId} reason=NO_PUROHIT_RESPONSE`);
    }
  }
);