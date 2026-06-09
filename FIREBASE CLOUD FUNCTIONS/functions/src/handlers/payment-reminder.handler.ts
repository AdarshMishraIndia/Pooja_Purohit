import { onTaskDispatched } from "firebase-functions/v2/tasks";
import * as admin from "firebase-admin";
import * as logger from "firebase-functions/logger";
import {
  REGION,
  MAX_PAYMENT_REMINDER_ATTEMPTS,
  REMINDER_INTERVAL_SECONDS,
} from "../config/constants";
import {
  BookingData,
  PaymentReminderPayload,
  NotificationPayload,
} from "../types";
import { writeNotification } from "../services/notification.service";
import { sendPushNotification } from "../services/fcm.service";
import { enqueuePaymentReminder } from "../services/tasks.service";

const db = admin.firestore();

/**
 * Cloud Tasks handler: 15-minute user payment reminder chain.
 *
 * Chain lifecycle:
 * ┌───────────────────────────────────────────────────────────────────┐
 * │  PENDING_PAYMENT (booking created)                                │
 * │       │                                                           │
 * │       ▼  (fires after 15min, attempt=1)                          │
 * │  ┌── status still PENDING_PAYMENT?                                │
 * │  │       YES → notify user                                        │
 * │  │             attemptNumber < MAX?                               │
 * │  │                 YES → re-enqueue (attempt+1)                   │
 * │  │                 NO  → auto-cancel booking → chain ends        │
 * │  │       NO  → return (payment done or cancelled, chain ends)    │
 * │  └──────────────────────────────────────────────────────────────  │
 * └───────────────────────────────────────────────────────────────────┘
 *
 * Time budget: 4 attempts × 15 min = 1 hour total payment window.
 *
 * Auto-cancel flow (max attempts exceeded):
 *   1. Write `cancellationReason: "NO_PAYMENT"` to booking doc.
 *   2. Update `status: "AUTO_CANCELLED"`.
 *   3. onBookingStatusUpdated fires, reads cancellationReason, notifies user only
 *      (purohit was never informed — payment never completed).
 *
 * Note: The purohit is intentionally NOT notified at any point in this chain.
 * They only learn of a booking when payment is confirmed (PAYMENT_DONE).
 */
export const processPaymentReminder = onTaskDispatched<PaymentReminderPayload>(
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
      `[PaymentReminder] Task fired | bookingId=${bookingId} attempt=${attemptNumber}/${MAX_PAYMENT_REMINDER_ATTEMPTS}`
    );

    const bookingRef = db.collection("bookings").doc(bookingId);
    const bookingSnap = await bookingRef.get();

    if (!bookingSnap.exists) {
      logger.warn(`[PaymentReminder] Booking not found: ${bookingId}. Chain terminated.`);
      return;
    }

    const booking = bookingSnap.data() as BookingData;

    // --- Self-termination: payment was completed or booking was cancelled ---
    if (booking.status !== "PENDING_PAYMENT") {
      logger.info(
        `[PaymentReminder] Chain terminated | bookingId=${bookingId} ` +
        `status=${booking.status} (no longer PENDING_PAYMENT)`
      );
      return;
    }

    // --- Send payment reminder to user ---
    const deepLink = `poojapurohit://bookings/${bookingId}`;

    // Remaining time hint helps create urgency without being alarmist
    const remainingAttempts = MAX_PAYMENT_REMINDER_ATTEMPTS - attemptNumber;
    const remainingMinutes = remainingAttempts * (REMINDER_INTERVAL_SECONDS / 60);

    const reminderPayload: NotificationPayload = {
      title: "Complete Your Payment",
      body: remainingAttempts > 0
        ? `Your ${booking.serviceName} booking is awaiting payment of ₹${booking.amount}. Complete within ${remainingMinutes} minutes to confirm your booking.`
        : `Final reminder: Complete your payment of ₹${booking.amount} for ${booking.serviceName} now or the booking will be cancelled.`,
      type: "PAYMENT_REMINDER",
      deepLinkUrl: deepLink,
    };

    const notifResults = await Promise.allSettled([
      writeNotification(booking.userId, reminderPayload),
      sendPushNotification(booking.userId, "users", reminderPayload),
    ]);

    notifResults.forEach((result, i) => {
      if (result.status === "rejected") {
        const op = i === 0 ? "Firestore write" : "FCM push";
        logger.error(`[PaymentReminder] ${op} failed | userId=${booking.userId}`, result.reason);
      }
    });

    // --- Continue or terminate the chain ---
    if (attemptNumber < MAX_PAYMENT_REMINDER_ATTEMPTS) {
      await enqueuePaymentReminder({ bookingId, attemptNumber: attemptNumber + 1 });
      logger.info(
        `[PaymentReminder] Reminder sent, chain continued | bookingId=${bookingId} ` +
        `next attempt=${attemptNumber + 1} in ${REMINDER_INTERVAL_SECONDS / 60}min`
      );
    } else {
      // Max attempts reached — auto-cancel.
      // Write cancellationReason BEFORE status so the Firestore trigger has
      // full context when it fires on the status change.
      logger.info(
        `[PaymentReminder] Max attempts reached | bookingId=${bookingId}. Initiating auto-cancel.`
      );

      await bookingRef.update({
        cancellationReason: "NO_PAYMENT",
        updatedAt: admin.firestore.FieldValue.serverTimestamp(),
      });

      await bookingRef.update({
        status: "AUTO_CANCELLED",
        updatedAt: admin.firestore.FieldValue.serverTimestamp(),
      });

      logger.info(`[PaymentReminder] Booking auto-cancelled | bookingId=${bookingId} reason=NO_PAYMENT`);
    }
  }
);
