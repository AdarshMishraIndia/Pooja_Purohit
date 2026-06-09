import { firestore } from "firebase-admin";

// ---------------------------------------------------------------------------
// Booking
// ---------------------------------------------------------------------------

export type BookingStatus =
  | "PENDING_PAYMENT"
  | "PAYMENT_DONE"
  | "PAYMENT_FAILED"
  | "ACCEPTED"
  | "REJECTED"
  | "CANCELLED_BY_USER"
  | "CANCELLED_BY_PUROHIT"
  | "COMPLETED"
  | "REFUNDED"
  | "AUTO_CANCELLED";

/**
 * Distinguishes why a booking was auto-cancelled.
 * Written to the booking document by the task handler BEFORE updating status,
 * so the Firestore trigger can read it and build context-appropriate messages.
 *
 * NO_PAYMENT          — user did not complete payment within 1 hour
 * NO_PUROHIT_RESPONSE — purohit did not accept/reject within 2 hours
 */
export type CancellationReason = "NO_PAYMENT" | "NO_PUROHIT_RESPONSE";

/** Partial booking document shape — only fields consumed by these functions */
export interface BookingData {
  bookingId: string;
  userId: string;
  purohitId: string;
  purohitName: string;
  serviceName: string;
  amount: number;
  status: BookingStatus;
  scheduledDate: firestore.Timestamp;
  createdAt: firestore.Timestamp;
  updatedAt: firestore.Timestamp;
  cancellationReason?: CancellationReason;
  completionOtp?: string;
}

// ---------------------------------------------------------------------------
// Notifications
// ---------------------------------------------------------------------------

export type NotificationType =
  | "PAYMENT_DONE"
  | "PAYMENT_FAILED"
  | "PAYMENT_REMINDER"
  | "PAYMENT_REFUNDED"
  | "PUROHIT_ACCEPTED"
  | "BOOKING_CONFIRMED"
  | "BOOKING_REJECTED"
  | "BOOKING_CANCELLED"
  | "BOOKING_COMPLETED"
  | "BOOKING_REMINDER"
  | "DAY_PRIOR_REMINDER"
  | "AUTO_CANCELLED"
  | "SYSTEM_ALERT"
  | "COMPLETION_OTP"
  | "GENERAL";

export interface NotificationPayload {
  title: string;
  body: string;
  type: NotificationType;
  deepLinkUrl?: string | null;
}

// ---------------------------------------------------------------------------
// Cloud Task Payloads
// ---------------------------------------------------------------------------

/** Payload for the 15-min purohit acceptance reminder chain */
export interface BookingReminderPayload {
  bookingId: string;
  attemptNumber: number; // 1-indexed; chain terminates at MAX_PUROHIT_REMINDER_ATTEMPTS
}

/** Payload for the 15-min user payment reminder chain */
export interface PaymentReminderPayload {
  bookingId: string;
  attemptNumber: number; // 1-indexed; chain terminates at MAX_PAYMENT_REMINDER_ATTEMPTS
}

/** Payload for the one-shot T-24h day-prior reminder */
export interface DayPriorReminderPayload {
  bookingId: string;
}