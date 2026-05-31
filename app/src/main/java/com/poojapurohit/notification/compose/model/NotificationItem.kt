package com.poojapurohit.notification.compose.model

import com.google.firebase.Timestamp

/**
 * Canonical notification types for BookAPurohit.
 *
 * MVP (critical) types are active — sent by Cloud Functions on booking/payment events.
 * Future (non-critical) types are provisioned — to activate one:
 *   1. Add its Cloud Function trigger.
 *   2. Add its entry in NotificationTemplateRegistry.
 *   That's all. No other app-side changes needed.
 *
 * Firestore field: "type" (String, matched case-insensitively to enum name)
 */
enum class NotificationType {

    // ── MVP: Booking lifecycle ────────────────────────────────────────────────
    BOOKING_CONFIRMED,
    BOOKING_CANCELLED,
    BOOKING_REJECTED,
    PUROHIT_ACCEPTED,

    // ── MVP: Payment lifecycle ────────────────────────────────────────────────
    PAYMENT_DONE,
    PAYMENT_REFUNDED,
    PAYMENT_FAILED,

    // ── Future: Reminders (provisioned, not sent in MVP) ─────────────────────
    UPCOMING_RITUAL,        // "Your booked pooja is tomorrow"
    HOLY_DAY_REMINDER,      // "Ekadashi tomorrow — book a purohit"
    BOOKING_REVIEW_PROMPT,  // "Rate your recent pooja"

    // ── Future: Promotional / Informational ───────────────────────────────────
    PROMO,
    SYSTEM_ALERT,

    // ── MVP: Booking completion OTP ───────────────────────────────────────────
    COMPLETION_OTP,         // "Your completion code is 482916. Share with the purohit."

    // ── MVP: Auto-cancellation (unpaid bookings) ──────────────────────────────
    AUTO_CANCELLED,

    // ── Fallback ──────────────────────────────────────────────────────────────
    GENERAL;

    companion object {
        fun fromString(value: String?): NotificationType =
            entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: GENERAL
    }
}

data class NotificationItem(
    val id: String = "",
    val title: String = "",
    val body: String = "",
    val timestamp: Timestamp = Timestamp.now(),
    val isRead: Boolean = false,
    val type: NotificationType = NotificationType.GENERAL,
    /**
     * Optional deep link URI. Pattern examples:
     *   poojapurohit://bookings/{bookingId}
     *   poojapurohit://payments/{paymentId}
     * Set by Cloud Function when writing the notification document.
     * Null = informational only, no navigation on tap.
     */
    val deepLinkUrl: String? = null
)