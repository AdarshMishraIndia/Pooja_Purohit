/**
 * Central configuration for Pooja Purohit Cloud Functions.
 * All magic numbers, queue names, and timing values live here.
 */

export const REGION = "asia-south1";

/**
 * Cloud Tasks queue names MUST exactly match the exported function names in index.ts.
 * Firebase resolves the Cloud Run service URL from these names at enqueue time.
 */
export const QUEUE_BOOKING_REMINDER  = "processBookingReminder";
export const QUEUE_PAYMENT_REMINDER  = "processPaymentReminder";
export const QUEUE_DAY_PRIOR_REMINDER = "processDayPriorReminder";

/** Interval between purohit acceptance reminders (while status is PAYMENT_DONE) */
export const REMINDER_INTERVAL_SECONDS = 15 * 60; // 15 minutes

/**
 * Maximum number of 15-min reminders sent to purohit before the booking is
 * auto-cancelled with reason NO_PUROHIT_RESPONSE.
 * 4 attempts × 15 min = 1 hour total response window.
 */
export const MAX_PUROHIT_REMINDER_ATTEMPTS = 4;

/**
 * Maximum number of 15-min reminders sent to user before the booking is
 * auto-cancelled with reason NO_PAYMENT.
 * 4 attempts × 15 min = 1 hour total payment window.
 */
export const MAX_PAYMENT_REMINDER_ATTEMPTS = 4;

/** How far before the scheduled event to fire the day-prior reminder */
export const DAY_PRIOR_OFFSET_MS = 24 * 60 * 60 * 1000; // 24 hours in ms

/**
 * Cloud Tasks hard ceiling for scheduling delay.
 * Bookings scheduled > 30 days out will not get a day-prior reminder task.
 */
export const CLOUD_TASKS_MAX_DELAY_SECONDS = 30 * 24 * 60 * 60; // 30 days