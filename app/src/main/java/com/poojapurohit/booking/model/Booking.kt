package com.poojapurohit.booking.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot

enum class BookingStatus {
    PENDING_PAYMENT,
    PAYMENT_DONE,
    ACCEPTED,
    REJECTED,
    COMPLETED,
    CANCELLED_BY_USER,
    CANCELLED_BY_PUROHIT,
    REFUNDED,
    AUTO_CANCELLED;

    companion object {
        fun fromString(value: String?): BookingStatus =
            entries.firstOrNull { it.name == value } ?: PENDING_PAYMENT
    }
}

enum class BookingCategory {
    ACTIVE,
    CANCELLED,
    COMPLETED
}

val BookingStatus.category: BookingCategory
    get() = when (this) {
        BookingStatus.PENDING_PAYMENT,
        BookingStatus.PAYMENT_DONE,
        BookingStatus.ACCEPTED -> BookingCategory.ACTIVE

        BookingStatus.REJECTED,
        BookingStatus.CANCELLED_BY_USER,
        BookingStatus.CANCELLED_BY_PUROHIT,
        BookingStatus.REFUNDED,
        BookingStatus.AUTO_CANCELLED -> BookingCategory.CANCELLED

        BookingStatus.COMPLETED -> BookingCategory.COMPLETED
    }

val BookingStatus.displayLabel: String
    get() = when (this) {
        BookingStatus.PENDING_PAYMENT    -> "Pending Payment"
        BookingStatus.PAYMENT_DONE       -> "Payment Done"
        BookingStatus.ACCEPTED           -> "Accepted"
        BookingStatus.REJECTED           -> "Rejected"
        BookingStatus.COMPLETED          -> "Completed"
        BookingStatus.CANCELLED_BY_USER  -> "Cancelled"
        BookingStatus.CANCELLED_BY_PUROHIT -> "Cancelled"
        BookingStatus.REFUNDED           -> "Refunded"
        BookingStatus.AUTO_CANCELLED     -> "Auto Cancelled"
    }

data class Coordinates(
    val latitude : Double = 0.0,
    val longitude: Double = 0.0
)

data class Booking(
    val bookingId: String = "",

    val purohitId   : String = "",
    val purohitName : String = "",
    val purohitPhone: String = "",

    val userId   : String = "",
    val userName : String = "",
    val userPhone: String = "",

    val serviceName: String = "",
    val amount     : Long   = 0L,

    val status            : BookingStatus = BookingStatus.PENDING_PAYMENT,
    val razorpayOrderId   : String        = "",
    val razorpayPaymentId : String        = "",

    val scheduledDate: Timestamp?   = null,
    val address      : String       = "",
    val coordinates  : Coordinates? = null,

    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null,
    val confirmedAt: Timestamp? = null,

    val comments: String? = null,

    /**
     * 6-digit OTP written by the purohit app when initiating completion.
     * Cloud Functions push this to the customer via notification.
     * Cleared (set to null) once the booking is marked COMPLETED.
     *
     * Security: Firestore rules must restrict read access to userId == auth.uid
     * so the purohit cannot read back their own written value.
     */
    val completionOtp: String? = null
) {
    companion object {
        fun fromDocument(doc: DocumentSnapshot): Booking {
            return Booking(
                bookingId = doc.getString("bookingId") ?: doc.id,

                userId    = doc.getString("userId")    ?: "",
                userName  = doc.getString("userName")  ?: "",
                userPhone = doc.getString("userPhone") ?: "",

                purohitId    = doc.getString("purohitId")    ?: "",
                purohitName  = doc.getString("purohitName")  ?: "",
                purohitPhone = doc.getString("purohitPhone") ?: "",

                serviceName = doc.getString("serviceName") ?: "",
                amount      = doc.getLong("amount")        ?: 0L,

                status            = BookingStatus.fromString(doc.getString("status")),
                razorpayOrderId   = doc.getString("razorpayOrderId")   ?: "",
                razorpayPaymentId = doc.getString("razorpayPaymentId") ?: "",

                scheduledDate = doc.getTimestamp("scheduledDate"),
                address       = doc.getString("address") ?: "",
                coordinates   = (doc.get("coordinates") as? Map<*, *>)?.let { map ->
                    Coordinates(
                        latitude  = (map["latitude"]  as? Double) ?: 0.0,
                        longitude = (map["longitude"] as? Double) ?: 0.0
                    )
                },

                createdAt = doc.getTimestamp("createdAt"),
                updatedAt = doc.getTimestamp("updatedAt"),
                confirmedAt = doc.getTimestamp("confirmedAt"),

                comments      = doc.getString("comments"),
                completionOtp = doc.getString("completionOtp")
            )
        }
    }
}