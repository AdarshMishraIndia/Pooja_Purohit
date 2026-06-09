import { getFunctions } from "firebase-admin/functions";
import * as logger from "firebase-functions/logger";
import {
  QUEUE_BOOKING_REMINDER,
  QUEUE_PAYMENT_REMINDER,
  QUEUE_DAY_PRIOR_REMINDER,
  REMINDER_INTERVAL_SECONDS,
  DAY_PRIOR_OFFSET_MS,
  CLOUD_TASKS_MAX_DELAY_SECONDS,
} from "../config/constants";
import {
  BookingReminderPayload,
  PaymentReminderPayload,
  DayPriorReminderPayload,
} from "../types";

/**
 * Enqueues the next purohit acceptance reminder task.
 *
 * Called from:
 *   - onBookingStatusUpdated on PAYMENT_DONE transition (attemptNumber = 1)
 *   - processBookingReminder itself to continue the chain (attemptNumber + 1)
 *
 * Chain terminates inside processBookingReminder when:
 *   - Status is no longer PAYMENT_DONE (purohit responded or user cancelled)
 *   - attemptNumber exceeds MAX_PUROHIT_REMINDER_ATTEMPTS (auto-cancel triggered)
 */
export async function enqueueBookingReminder(
  payload: BookingReminderPayload
): Promise<void> {
  const queue = getFunctions().taskQueue<BookingReminderPayload>(
    QUEUE_BOOKING_REMINDER
  );

  await queue.enqueue(payload, {
    scheduleDelaySeconds: REMINDER_INTERVAL_SECONDS,
  });

  logger.info(
    `[TasksService] Booking reminder enqueued | bookingId=${payload.bookingId} ` +
    `attempt=${payload.attemptNumber} fires in ${REMINDER_INTERVAL_SECONDS / 60}min`
  );
}

/**
 * Enqueues the next user payment reminder task.
 *
 * Called from:
 *   - onBookingStatusUpdated on PENDING_PAYMENT creation (attemptNumber = 1)
 *   - processPaymentReminder itself to continue the chain (attemptNumber + 1)
 *
 * Chain terminates inside processPaymentReminder when:
 *   - Status is no longer PENDING_PAYMENT (payment completed or cancelled)
 *   - attemptNumber exceeds MAX_PAYMENT_REMINDER_ATTEMPTS (auto-cancel triggered)
 */
export async function enqueuePaymentReminder(
  payload: PaymentReminderPayload
): Promise<void> {
  const queue = getFunctions().taskQueue<PaymentReminderPayload>(
    QUEUE_PAYMENT_REMINDER
  );

  await queue.enqueue(payload, {
    scheduleDelaySeconds: REMINDER_INTERVAL_SECONDS,
  });

  logger.info(
    `[TasksService] Payment reminder enqueued | bookingId=${payload.bookingId} ` +
    `attempt=${payload.attemptNumber} fires in ${REMINDER_INTERVAL_SECONDS / 60}min`
  );
}

/**
 * Enqueues the one-shot T-24h day-prior reminder for both stakeholders.
 * Enqueued on ACCEPTED transition since the event only proceeds if accepted.
 *
 * Delay = scheduledDate - 24h - now
 *
 * Silent skip conditions:
 *   1. delay <= 0  → event already within 24h; sending now would be misleading
 *   2. delay > 30 days → Cloud Tasks platform ceiling
 */
export async function enqueueDayPriorReminder(
  payload: DayPriorReminderPayload,
  scheduledDateMillis: number
): Promise<void> {
  const delayMs = scheduledDateMillis - DAY_PRIOR_OFFSET_MS - Date.now();
  const delaySeconds = Math.floor(delayMs / 1000);

  if (delaySeconds <= 0) {
    logger.info(
      `[TasksService] Day-prior reminder skipped | bookingId=${payload.bookingId} ` +
      `reason=event_within_24h`
    );
    return;
  }

  if (delaySeconds > CLOUD_TASKS_MAX_DELAY_SECONDS) {
    logger.warn(
      `[TasksService] Day-prior reminder skipped | bookingId=${payload.bookingId} ` +
      `reason=exceeds_cloud_tasks_max delay=${delaySeconds}s`
    );
    return;
  }

  const queue = getFunctions().taskQueue<DayPriorReminderPayload>(
    QUEUE_DAY_PRIOR_REMINDER
  );

  await queue.enqueue(payload, { scheduleDelaySeconds: delaySeconds });

  logger.info(
    `[TasksService] Day-prior reminder enqueued | bookingId=${payload.bookingId} ` +
    `fires in ~${Math.round(delaySeconds / 3600)}h`
  );
}