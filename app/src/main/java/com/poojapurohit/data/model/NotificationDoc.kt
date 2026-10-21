package com.poojapurohit.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot

/**
 * Canonical notification types.
 *
 * MVP (critical) types are sent by Cloud Functions on booking/payment events.
 * Types without an entry in NotificationTemplateRegistry render with the GENERAL template.
 * To give a new type its own look:
 *   1. Add its Cloud Function trigger.
 *   2. Add its entry in NotificationTemplateRegistry.
 *
 * Firestore field: "type" (String, matched case-insensitively to enum name; unknown -> GENERAL)
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

    // ── Written by the app / Cloud Functions; no dedicated template (-> GENERAL) ──
    BOOKING_UPDATED,        // app: customer edited address/schedule -> purohit notified
    BOOKING_COMPLETED,      // functions
    PAYMENT_REMINDER,       // functions
    BOOKING_REMINDER,       // functions
    DAY_PRIOR_REMINDER,     // functions

    // ── Fallback ──────────────────────────────────────────────────────────────
    GENERAL;

    companion object {
        fun fromString(value: String?): NotificationType =
            entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: GENERAL
    }
}

/**
 * Firestore: notifications/{stakeholderUid}/items/{notificationId}.
 * Document ID of the parent == users/purohits document ID.
 */
data class NotificationDoc(
    val id: String = "",             // document ID; not a stored field
    val title: String = "",
    val body: String = "",
    val timestamp: Timestamp = Timestamp.now(),
    val isRead: Boolean = false,
    val isActive: Boolean = true,
    val type: NotificationType = NotificationType.GENERAL,
    /**
     * Optional deep link URI. Pattern examples:
     *   poojapurohit://bookings/{bookingId}
     *   poojapurohit://payments/{paymentId}
     * Set by Cloud Function when writing the notification document.
     * Null = informational only, no navigation on tap.
     */
    val deepLinkUrl: String? = null
) {

    /** Write map. Null deepLinkUrl is omitted. */
    fun toMap(): Map<String, Any> = buildMap<String, Any> {
        put(Fields.TITLE, title)
        put(Fields.BODY, body)
        put(Fields.IS_READ, isRead)
        put(Fields.IS_ACTIVE, isActive)
        put(Fields.TIMESTAMP, timestamp)
        put(Fields.TYPE, type.name)
        deepLinkUrl?.let { put(Fields.DEEP_LINK_URL, it) }
    }

    object Fields {
        const val TITLE = "title"
        const val BODY = "body"
        const val IS_READ = "isRead"
        const val IS_ACTIVE = "isActive"
        const val TIMESTAMP = "timestamp"
        const val TYPE = "type"
        const val DEEP_LINK_URL = "deepLinkUrl"
    }

    companion object {
        const val COLLECTION = "notifications"
        const val ITEMS = "items"

        fun fromDocument(doc: DocumentSnapshot) = NotificationDoc(
            id = doc.id,
            title = doc.getString(Fields.TITLE) ?: "",
            body = doc.getString(Fields.BODY) ?: "",
            timestamp = doc.getTimestamp(Fields.TIMESTAMP) ?: Timestamp.now(),
            isRead = doc.getBoolean(Fields.IS_READ) ?: false,
            isActive = doc.getBoolean(Fields.IS_ACTIVE) ?: true,
            type = NotificationType.fromString(doc.getString(Fields.TYPE)),
            deepLinkUrl = doc.getString(Fields.DEEP_LINK_URL)
        )
    }
}
