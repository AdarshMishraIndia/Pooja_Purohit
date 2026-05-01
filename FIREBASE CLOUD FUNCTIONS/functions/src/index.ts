import { onDocumentWritten } from "firebase-functions/v2/firestore";
import * as logger from "firebase-functions/logger";
import * as admin from "firebase-admin";

admin.initializeApp();
const db = admin.firestore();

type NotificationType = 
  | "BOOKING_CONFIRMED"
  | "BOOKING_CANCELLED"
  | "BOOKING_REJECTED"
  | "PUROHIT_ACCEPTED"
  | "PAYMENT_DONE"
  | "PAYMENT_REFUNDED"
  | "PAYMENT_FAILED"
  | "AUTO_CANCELLED"
  | "SYSTEM_ALERT"
  | "GENERAL";

interface NotificationPayload {
  title: string;
  body: string;
  type: NotificationType;
  deepLinkUrl?: string | null;
}

export const onBookingStatusUpdated = onDocumentWritten("bookings/{bookingId}", async (event) => {
  const change = event.data;
  const bookingId = event.params.bookingId;

  if (!change) return;
  if (!change.after.exists) return;

  const data = change.after.data();
  if (!data) return;

  const purohitId = data.purohitId;
  const userId = data.userId;
  const serviceName = data.serviceName;
  const status = data.status;

  const bookingDeepLink = `poojapurohit://bookings/${bookingId}`;

  let userPayload: NotificationPayload | null = null;
  let purohitPayload: NotificationPayload | null = null;

  switch (status) {
    case "PENDING_PAYMENT":
      break;

    case "PAYMENT_FAILED":
      userPayload = {
        title: "Payment Failed",
        body: `Payment for ${serviceName} failed. Please try again.`,
        type: "PAYMENT_FAILED",
        deepLinkUrl: bookingDeepLink
      };
      break;

    case "PAYMENT_DONE":
      userPayload = {
        title: "Payment Successful",
        body: `Payment of ${data.amount} INR for ${serviceName} is successful.`,
        type: "PAYMENT_DONE",
        deepLinkUrl: bookingDeepLink
      };
      purohitPayload = {
        title: "New Booking Request",
        body: `You have a new booking request for ${serviceName}.`,
        type: "PAYMENT_DONE",
        deepLinkUrl: bookingDeepLink
      };
      break;

    case "ACCEPTED":
      userPayload = {
        title: "Booking Accepted",
        body: `Your booking for ${serviceName} has been accepted by the purohit.`,
        type: "PUROHIT_ACCEPTED",
        deepLinkUrl: bookingDeepLink
      };
      purohitPayload = {
        title: "Booking Confirmed",
        body: `You have accepted the booking for ${serviceName}.`,
        type: "PUROHIT_ACCEPTED",
        deepLinkUrl: bookingDeepLink
      };
      break;

    case "REJECTED":
      userPayload = {
        title: "Booking Rejected",
        body: `Your booking for ${serviceName} was rejected. A refund will be initiated.`,
        type: "BOOKING_REJECTED",
        deepLinkUrl: bookingDeepLink
      };
      purohitPayload = {
        title: "Booking Rejected",
        body: `You have rejected the booking for ${serviceName}.`,
        type: "BOOKING_REJECTED",
        deepLinkUrl: bookingDeepLink
      };
      break;

    case "CANCELLED_BY_USER":
      userPayload = {
        title: "Booking Cancelled",
        body: `Your booking for ${serviceName} was cancelled.`,
        type: "BOOKING_CANCELLED",
        deepLinkUrl: bookingDeepLink
      };
      purohitPayload = {
        title: "Booking Cancelled",
        body: `The booking for ${serviceName} was cancelled by the user.`,
        type: "BOOKING_CANCELLED",
        deepLinkUrl: bookingDeepLink
      };
      break;

    case "CANCELLED_BY_PUROHIT":
      userPayload = {
        title: "Booking Cancelled",
        body: `The purohit has cancelled the confirmed booking for ${serviceName}.`,
        type: "BOOKING_CANCELLED",
        deepLinkUrl: bookingDeepLink
      };
      purohitPayload = {
        title: "Booking Cancelled",
        body: `You cancelled the booking for ${serviceName}.`,
        type: "BOOKING_CANCELLED",
        deepLinkUrl: bookingDeepLink
      };
      break;

    case "AUTO_CANCELLED":
      userPayload = {
        title: "Booking Expired",
        body: `Your booking for ${serviceName} was auto-cancelled due to non-payment.`,
        type: "AUTO_CANCELLED",
        deepLinkUrl: bookingDeepLink
      };
      purohitPayload = {
        title: "Booking Expired",
        body: `The booking for ${serviceName} was auto-cancelled due to non-payment.`,
        type: "AUTO_CANCELLED",
        deepLinkUrl: bookingDeepLink
      };
      break;

    default:
      logger.debug(`Status updated to ${status}. No specific notification mapped.`);
      return;
  }

  const writeNotification = async (uid: string | undefined, payload: NotificationPayload | null) => {
    if (!uid || !payload) return;

    try {
      const notificationRef = db
        .collection("notifications")
        .doc(uid)
        .collection("items")
        .doc(); 

      await notificationRef.set({
        title: payload.title,
        body: payload.body,
        type: payload.type,
        deepLinkUrl: payload.deepLinkUrl || null,
        isRead: false,
        isActive: true,
        timestamp: admin.firestore.FieldValue.serverTimestamp()
      });
      
      logger.info(`Notification sent to ${uid} of type ${payload.type}`);
    } catch (error) {
      logger.error(`Failed to write notification for ${uid}`, error);
    }
  };

  await Promise.all([
    writeNotification(userId, userPayload),
    writeNotification(purohitId, purohitPayload)
  ]);
});