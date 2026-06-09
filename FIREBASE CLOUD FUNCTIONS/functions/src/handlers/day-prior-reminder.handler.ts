import { onTaskDispatched } from "firebase-functions/v2/tasks";
import * as admin from "firebase-admin";
import * as logger from "firebase-functions/logger";
import { REGION } from "../config/constants";
import { BookingData, DayPriorReminderPayload, NotificationPayload } from "../types";
import { writeNotification } from "../services/notification.service";
import { sendPushNotification } from "../services/fcm.service";

const db = admin.firestore();

/**
 * Cloud Tasks handler: one-shot T-24h reminder for both user and purohit.
 *
 * Enqueued when booking transitions to ACCEPTED.
 * Status gate: only fires if booking is still ACCEPTED at the time of execution.
 * If the booking was cancelled or completed between enqueue and fire, this is a no-op.
 */
export const processDayPriorReminder = onTaskDispatched<DayPriorReminderPayload>(
  {
    region: REGION,
    retryConfig: {
      maxAttempts: 3,
      minBackoffSeconds: 10,
      maxBackoffSeconds: 60,
      maxDoublings: 2,
    },
    rateLimits: { maxConcurrentDispatches: 10 },
  },
  async (request) => {
    const { bookingId } = request.data;

    logger.info(`[DayPriorReminder] Task fired | bookingId=${bookingId}`);

    const bookingSnap = await db.collection("bookings").doc(bookingId).get();

    if (!bookingSnap.exists) {
      logger.warn(`[DayPriorReminder] Booking not found: ${bookingId}. Skipping.`);
      return;
    }

    const booking = bookingSnap.data() as BookingData;

    // Status gate — only notify if the event is still happening
    if (booking.status !== "ACCEPTED") {
      logger.info(
        `[DayPriorReminder] Skipped | bookingId=${bookingId} status=${booking.status} (not ACCEPTED)`
      );
      return;
    }

    const { userId, purohitId, serviceName } = booking;
    const deepLink = `poojapurohit://bookings/${bookingId}`;

    const userPayload: NotificationPayload = {
      title: "Ceremony is Tomorrow",
      body: `Your ${serviceName} ceremony is scheduled for tomorrow. Please ensure all necessary arrangements are in place.`,
      type: "DAY_PRIOR_REMINDER",
      deepLinkUrl: deepLink,
    };

    const purohitPayload: NotificationPayload = {
      title: "Ceremony Tomorrow",
      body: `You have a ${serviceName} ceremony scheduled for tomorrow. Please be prepared and on time.`,
      type: "DAY_PRIOR_REMINDER",
      deepLinkUrl: deepLink,
    };

    const results = await Promise.allSettled([
      writeNotification(userId,    userPayload),
      sendPushNotification(userId,    "users",    userPayload),
      writeNotification(purohitId, purohitPayload),
      sendPushNotification(purohitId, "purohits", purohitPayload),
    ]);

    results.forEach((result, i) => {
      if (result.status === "rejected") {
        const ops = [
          "Firestore write (user)", "FCM push (user)",
          "Firestore write (purohit)", "FCM push (purohit)",
        ];
        logger.error(
          `[DayPriorReminder] ${ops[i]} failed | bookingId=${bookingId}`,
          result.reason
        );
      }
    });

    logger.info(`[DayPriorReminder] Reminder sent to both stakeholders | bookingId=${bookingId}`);
  }
);