import { onDocumentWritten } from "firebase-functions/v2/firestore";
import * as logger from "firebase-functions/logger";
import { REGION } from "../config/constants";
import {
  BookingData,
  BookingStatus,
  CancellationReason,
  NotificationPayload,
} from "../types";
import { writeNotification } from "../services/notification.service";
import { sendPushNotification } from "../services/fcm.service";
import {
  enqueueBookingReminder,
  enqueuePaymentReminder,
  enqueueDayPriorReminder,
} from "../services/tasks.service";

// ---------------------------------------------------------------------------
// Notification payload builder
// ---------------------------------------------------------------------------

/**
 * Maps booking status → [userPayload, purohitPayload].
 * null means no notification for that stakeholder in this transition.
 *
 * Business rules encoded here:
 *   - Purohit is NEVER notified about payment state (PENDING/FAILED) —
 *     they only learn about a booking once payment is confirmed (PAYMENT_DONE).
 *   - AUTO_CANCELLED messaging is context-sensitive based on cancellationReason.
 *   - User payment reminders are handled by processPaymentReminder, not here.
 *   - Purohit acceptance reminders are handled by processBookingReminder, not here.
 */
function buildPayloads(
  status: BookingStatus,
  booking: BookingData,
  deepLink: string
): [NotificationPayload | null, NotificationPayload | null] {
  const { serviceName, amount, cancellationReason } = booking;

  switch (status) {

    // ── Payment states ──────────────────────────────────────────────────────

    case "PENDING_PAYMENT":
      // No immediate notification — user is mid-payment flow.
      // Payment reminder chain starts via task queue.
      return [null, null];

    case "PAYMENT_FAILED":
      return [
        {
          title: "Payment Failed",
          body: `Your payment of ₹${amount} for ${serviceName} could not be processed. Please try again.`,
          type: "PAYMENT_FAILED",
          deepLinkUrl: deepLink,
        },
        null, // Purohit unaware of failed payments
      ];

    case "PAYMENT_DONE":
      // User confirmed, purohit notified for the first time.
      return [
        {
          title: "Payment Confirmed",
          body: `Your payment of ₹${amount} for ${serviceName} is confirmed. Awaiting purohit acceptance.`,
          type: "PAYMENT_DONE",
          deepLinkUrl: deepLink,
        },
        {
          title: "New Booking Request",
          body: `You have received a new booking request for ${serviceName}. Please accept or reject at your earliest.`,
          type: "PAYMENT_DONE",
          deepLinkUrl: deepLink,
        },
      ];

    // ── Purohit response ────────────────────────────────────────────────────

    case "ACCEPTED":
      return [
        {
          title: "Booking Accepted",
          body: `Your ${serviceName} booking has been accepted by the purohit. You're all set!`,
          type: "PUROHIT_ACCEPTED",
          deepLinkUrl: deepLink,
        },
        {
          title: "Booking Confirmed",
          body: `You have accepted the ${serviceName} booking. Please be present at the scheduled time.`,
          type: "PUROHIT_ACCEPTED",
          deepLinkUrl: deepLink,
        },
      ];

    case "REJECTED":
      return [
        {
          title: "Booking Rejected",
          body: `Your ${serviceName} booking was rejected by the purohit. A full refund will be initiated shortly.`,
          type: "BOOKING_REJECTED",
          deepLinkUrl: deepLink,
        },
        {
          title: "Booking Rejected",
          body: `You have rejected the ${serviceName} booking. The user will be notified and refunded.`,
          type: "BOOKING_REJECTED",
          deepLinkUrl: deepLink,
        },
      ];

    // ── Cancellations ───────────────────────────────────────────────────────

    case "CANCELLED_BY_USER":
      return [
        {
          title: "Booking Cancelled",
          body: `Your ${serviceName} booking has been successfully cancelled.`,
          type: "BOOKING_CANCELLED",
          deepLinkUrl: deepLink,
        },
        {
          title: "Booking Cancelled by User",
          body: `The user has cancelled the ${serviceName} booking.`,
          type: "BOOKING_CANCELLED",
          deepLinkUrl: deepLink,
        },
      ];

    case "CANCELLED_BY_PUROHIT":
      return [
        {
          title: "Booking Cancelled",
          body: `Your confirmed ${serviceName} booking has been cancelled by the purohit. A full refund will be initiated.`,
          type: "BOOKING_CANCELLED",
          deepLinkUrl: deepLink,
        },
        {
          title: "Booking Cancelled",
          body: `You have cancelled the ${serviceName} booking. The user will be refunded.`,
          type: "BOOKING_CANCELLED",
          deepLinkUrl: deepLink,
        },
      ];

    case "AUTO_CANCELLED":
      // Context-sensitive messaging based on why the booking was auto-cancelled.
      return buildAutoCancelledPayloads(
        serviceName,
        amount,
        cancellationReason,
        deepLink
      );

    // ── Completion ──────────────────────────────────────────────────────────

    case "COMPLETED":
      return [
        {
          title: "Ceremony Completed",
          body: `Your ${serviceName} ceremony has been successfully completed. Thank you for choosing Pooja Purohit!`,
          type: "BOOKING_COMPLETED",
          deepLinkUrl: deepLink,
        },
        {
          title: "Booking Completed",
          body: `The ${serviceName} ceremony has been marked as complete. Great work!`,
          type: "BOOKING_COMPLETED",
          deepLinkUrl: deepLink,
        },
      ];

    // ── Refund ──────────────────────────────────────────────────────────────

    case "REFUNDED":
      // REFUNDED = refund has been credited to the user.
      // The "refund initiated" signal is the REJECTED / CANCELLED_BY_PUROHIT notification above.
      return [
        {
          title: "Refund Processed",
          body: `Your refund of ₹${amount} for ${serviceName} has been successfully credited. Please allow 5–7 business days to reflect.`,
          type: "PAYMENT_REFUNDED",
          deepLinkUrl: deepLink,
        },
        null,
      ];

    default:
      logger.warn(`[BookingHandler] Unhandled booking status: ${status}`);
      return [null, null];
  }
}

/**
 * Builds the correct AUTO_CANCELLED payloads based on why the cancellation occurred.
 *
 * NO_PAYMENT:
 *   Only the user is notified — the purohit was never informed of this booking
 *   (payment never completed), so no purohit notification is warranted.
 *
 * NO_PUROHIT_RESPONSE:
 *   Both are notified. User learns the booking expired. Purohit is informed
 *   they missed a booking opportunity.
 */
function buildAutoCancelledPayloads(
  serviceName: string,
  amount: number,
  reason: CancellationReason | undefined,
  deepLink: string
): [NotificationPayload | null, NotificationPayload | null] {
  if (reason === "NO_PAYMENT") {
    return [
      {
        title: "Booking Expired",
        body: `Your ${serviceName} booking has been cancelled due to incomplete payment. Please create a new booking if you wish to proceed.`,
        type: "AUTO_CANCELLED",
        deepLinkUrl: deepLink,
      },
      null, // Purohit was never notified about this booking
    ];
  }

  if (reason === "NO_PUROHIT_RESPONSE") {
    return [
      {
        title: "Booking Expired",
        body: `Your ${serviceName} booking has been cancelled as the purohit did not respond in time. A full refund of ₹${amount} will be initiated.`,
        type: "AUTO_CANCELLED",
        deepLinkUrl: deepLink,
      },
      {
        title: "Booking Opportunity Missed",
        body: `A ${serviceName} booking was cancelled because you did not respond in time. Please keep the app open to respond to booking requests promptly.`,
        type: "AUTO_CANCELLED",
        deepLinkUrl: deepLink,
      },
    ];
  }

  // Fallback — reason field missing (should not happen in production)
  logger.error(`[BookingHandler] AUTO_CANCELLED without cancellationReason for deepLink=${deepLink}`);
  return [
    {
      title: "Booking Cancelled",
      body: `Your ${serviceName} booking has been automatically cancelled.`,
      type: "AUTO_CANCELLED",
      deepLinkUrl: deepLink,
    },
    null,
  ];
}

// ---------------------------------------------------------------------------
// Stakeholder dispatcher
// ---------------------------------------------------------------------------

/**
 * Sends notification to a single stakeholder.
 * Firestore write and FCM push are independent — one failing does not block the other.
 */
async function notifyStakeholder(
  uid: string,
  collection: "users" | "purohits",
  payload: NotificationPayload
): Promise<void> {
  const results = await Promise.allSettled([
    writeNotification(uid, payload),
    sendPushNotification(uid, collection, payload),
  ]);

  results.forEach((result, i) => {
    if (result.status === "rejected") {
      const op = i === 0 ? "Firestore write" : "FCM push";
      logger.error(
        `[BookingHandler] ${op} failed | uid=${uid}`,
        result.reason
      );
    }
  });
}

// ---------------------------------------------------------------------------
// Cloud Function export
// ---------------------------------------------------------------------------

/**
 * Firestore trigger: fires on any write to bookings/{bookingId}.
 *
 * Responsibilities:
 *   1. Guard against no-op writes (status unchanged → skip).
 *   2. Dispatch status-appropriate notifications to user and/or purohit.
 *   3. Schedule Cloud Tasks on relevant status transitions:
 *        PENDING_PAYMENT → payment reminder chain (user, 15min × 4)
 *        PAYMENT_DONE    → booking reminder chain (purohit, 15min × 8)
 *        ACCEPTED        → day-prior reminder (both, T-24h)
 */
export const onBookingStatusUpdated = onDocumentWritten(
  { document: "bookings/{bookingId}", region: REGION },
  async (event) => {
    const { bookingId } = event.params;
    const change = event.data;

    if (!change?.after.exists) return;

    const after = change.after.data() as BookingData;
    if (!after) return;

    const prevStatus = change.before.exists
      ? (change.before.data() as BookingData)?.status
      : null;
    const newStatus = after.status;

    // --- Guard: skip writes that don't change status ---
    // Booking docs are updated for non-status fields (razorpayPaymentId, updatedAt, etc.).
    // Without this guard those writes re-trigger notifications for the current status.
    if (prevStatus === newStatus) {
      logger.debug(
        `[BookingHandler] No status change | bookingId=${bookingId} status=${newStatus}. Skipping.`
      );
      return;
    }

    logger.info(
      `[BookingHandler] Transition | bookingId=${bookingId} ${prevStatus ?? "NEW"} → ${newStatus}`
    );

    const { userId, purohitId } = after;
    const deepLink = `poojapurohit://bookings/${bookingId}`;

    const [userPayload, purohitPayload] = buildPayloads(newStatus, after, deepLink);

    // Dispatch notifications concurrently
    const notifTasks: Promise<void>[] = [];
    if (userPayload)   notifTasks.push(notifyStakeholder(userId,    "users",    userPayload));
    if (purohitPayload) notifTasks.push(notifyStakeholder(purohitId, "purohits", purohitPayload));
    await Promise.allSettled(notifTasks);

    // --- Task scheduling on specific transitions ---
    const schedulingResults = await Promise.allSettled(
      getTasksToSchedule(newStatus, bookingId, after)
    );

    schedulingResults.forEach((result, i) => {
      if (result.status === "rejected") {
        logger.error(
          `[BookingHandler] Task scheduling failed [index=${i}] | bookingId=${bookingId}`,
          result.reason
        );
      }
    });
  }
);

/**
 * Returns the Cloud Task enqueue promises for a given status transition.
 * Isolated as a pure function for readability — no side effects.
 */
function getTasksToSchedule(
  status: BookingStatus,
  bookingId: string,
  booking: BookingData
): Promise<void>[] {
  switch (status) {

    case "PENDING_PAYMENT":
      // Start the user payment reminder chain. Fires 15 min from now.
      // Chain self-terminates when status moves away from PENDING_PAYMENT
      // or when MAX_PAYMENT_REMINDER_ATTEMPTS is exhausted.
      return [enqueuePaymentReminder({ bookingId, attemptNumber: 1 })];

    case "PAYMENT_DONE": {
      // Start the purohit acceptance reminder chain. Fires 15 min from now.
      // Payment reminder chain will self-terminate on next fire (status ≠ PENDING_PAYMENT).
      return [enqueueBookingReminder({ bookingId, attemptNumber: 1 })];
    }

    case "ACCEPTED": {
      // Schedule T-24h day-prior reminder now that the event is confirmed.
      const scheduledDateMillis = booking.scheduledDate?.toMillis?.();
      if (!scheduledDateMillis) {
        logger.error(
          `[BookingHandler] scheduledDate missing | bookingId=${bookingId}. Day-prior task NOT scheduled.`
        );
        return [];
      }
      return [enqueueDayPriorReminder({ bookingId }, scheduledDateMillis)];
    }

    default:
      return [];
  }
}

// ---------------------------------------------------------------------------
// OTP completion trigger
// ---------------------------------------------------------------------------

/**
 * Firestore trigger: fires when completionOtp is written to a booking document.
 *
 * Condition to act:
 *   - booking.status === "ACCEPTED"  (still in progress, not yet completed)
 *   - after.completionOtp is a non-empty string
 *   - before.completionOtp was null/undefined  (field newly appeared, not updated)
 *
 * Responsibility:
 *   Writes a COMPLETION_OTP notification to notifications/{userId}/items/{autoId}.
 *   The Firestore listener on the Android app picks this up in real-time and
 *   displays the OTP in BookingCard and BookingDetailsScreen.
 *   An FCM push is also fired so the user sees it immediately if the app is backgrounded.
 *
 * The purohit is NOT notified — they initiated the flow and are waiting for the user
 * to read the code verbally.
 *
 * Security note: completionOtp is written by the purohit app and read here server-side.
 * Firestore rules must ensure the purohit cannot read completionOtp back from the document.
 */
export const onCompletionOtpWritten = onDocumentWritten(
  { document: "bookings/{bookingId}", region: REGION },
  async (event) => {
    const { bookingId } = event.params;
    const change = event.data;

    if (!change?.after.exists) return;

    const after  = change.after.data()  as BookingData;
    const before = change.before.exists ? change.before.data() as BookingData : null;

    // Guard 1: OTP must be newly set (not a re-write or unrelated field update)
    const otpBefore = (before as any)?.completionOtp as string | undefined;
    const otpAfter  = (after  as any)?.completionOtp as string | undefined;

    if (!otpAfter || otpAfter === otpBefore) {
      return; // OTP unchanged or cleared — not our event
    }

    // Guard 2: booking must still be ACCEPTED
    if (after.status !== "ACCEPTED") {
      logger.warn(
        `[OtpHandler] completionOtp written but status=${after.status} | bookingId=${bookingId}. Skipping.`
      );
      return;
    }

    logger.info(`[OtpHandler] completionOtp detected | bookingId=${bookingId}. Notifying user.`);

    const { userId, serviceName } = after;
    const deepLink = `poojapurohit://bookings/${bookingId}`;

    const payload: NotificationPayload = {
      title: "Your Completion Code",
      body: `Your completion code is ${otpAfter}. Share this with the purohit to complete your ${serviceName} booking.`,
      type: "COMPLETION_OTP",
      deepLinkUrl: deepLink,
    };

    const results = await Promise.allSettled([
      writeNotification(userId, payload),
      sendPushNotification(userId, "users", payload),
    ]);

    results.forEach((result, i) => {
      if (result.status === "rejected") {
        const op = i === 0 ? "Firestore write" : "FCM push";
        logger.error(
          `[OtpHandler] ${op} failed | userId=${userId} bookingId=${bookingId}`,
          result.reason
        );
      }
    });

    logger.info(`[OtpHandler] Completion OTP notification sent | bookingId=${bookingId} userId=${userId}`);
  }
);