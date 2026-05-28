/**
 * Pooja Purohit — Cloud Functions Entry Point
 *
 * Exported functions:
 *
 *   onBookingStatusUpdated   — Firestore trigger on bookings/{bookingId}
 *                              Dispatches status-specific notifications.
 *                              Schedules Cloud Tasks on:
 *                                PENDING_PAYMENT → payment reminder chain
 *                                PAYMENT_DONE    → booking reminder chain
 *                                ACCEPTED        → day-prior reminder
 *
 *   processPaymentReminder   — Cloud Tasks handler (queue: processPaymentReminder)
 *                              Fires every 15 min up to 4 times (1 hour) while PENDING_PAYMENT.
 *                              Auto-cancels with reason=NO_PAYMENT on exhaustion.
 *
 *   processBookingReminder   — Cloud Tasks handler (queue: processBookingReminder)
 *                              Fires every 15 min up to 8 times (2 hours) while PAYMENT_DONE.
 *                              Auto-cancels with reason=NO_PUROHIT_RESPONSE on exhaustion.
 *
 *   processDayPriorReminder  — Cloud Tasks handler (queue: processDayPriorReminder)
 *                              Fires once at T-24h. Status-gated to ACCEPTED.
 *
 * Queue names MUST match the exported function names exactly.
 * Firebase resolves Cloud Run service URLs by these names at enqueue time.
 */

import * as admin from "firebase-admin";

admin.initializeApp();

export { onBookingStatusUpdated }  from "./handlers/booking-status.handler";
export { processPaymentReminder }  from "./handlers/payment-reminder.handler";
export { processBookingReminder }  from "./handlers/booking-reminder.handler";
export { processDayPriorReminder } from "./handlers/day-prior-reminder.handler";