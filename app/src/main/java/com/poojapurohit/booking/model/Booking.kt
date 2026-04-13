package com.poojapurohit.booking.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot

enum class BookingStatus {
    PENDING_PAYMENT,
    PAYMENT_DONE,
    ACCEPTED,
    REJECTED,
    COMPLETED,
    CANCELLED,
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
        BookingStatus.CANCELLED,
        BookingStatus.REFUNDED,
        BookingStatus.AUTO_CANCELLED -> BookingCategory.CANCELLED

        BookingStatus.COMPLETED -> BookingCategory.COMPLETED
    }

val BookingStatus.displayLabel: String
    get() = when (this) {
        BookingStatus.PENDING_PAYMENT -> "Pending Payment"
        BookingStatus.PAYMENT_DONE -> "Payment Done"
        BookingStatus.ACCEPTED -> "Accepted"
        BookingStatus.REJECTED -> "Rejected"
        BookingStatus.COMPLETED -> "Completed"
        BookingStatus.CANCELLED -> "Cancelled"
        BookingStatus.REFUNDED -> "Refunded"
        BookingStatus.AUTO_CANCELLED -> "Auto Cancelled"
    }

data class Booking(
    val bookingId: String = "",
    val userId: String = "",
    val purohitId: String = "",
    val purohitName: String = "",
    val userPhone: String = "",
    val serviceName: String = "",
    val amount: Long = 0L,
    val status: BookingStatus = BookingStatus.PENDING_PAYMENT,
    val razorpayOrderId: String = "",
    val razorpayPaymentId: String = "",
    val scheduledDate: Timestamp? = null,
    val address: String = "",
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
) {
    companion object {
        fun fromDocument(doc: DocumentSnapshot): Booking {
            return Booking(
                bookingId = doc.getString("bookingId") ?: doc.id,
                userId = doc.getString("userId") ?: "",
                purohitId = doc.getString("purohitId") ?: "",
                purohitName = doc.getString("purohitName") ?: "",
                userPhone = doc.getString("userPhone") ?: "",
                serviceName = doc.getString("serviceName") ?: "",
                amount = doc.getLong("amount") ?: 0L,
                status = BookingStatus.fromString(doc.getString("status")),
                razorpayOrderId = doc.getString("razorpayOrderId") ?: "",
                razorpayPaymentId = doc.getString("razorpayPaymentId") ?: "",
                scheduledDate = doc.getTimestamp("scheduledDate"),
                address = doc.getString("address") ?: "",
                createdAt = doc.getTimestamp("createdAt"),
                updatedAt = doc.getTimestamp("updatedAt")
            )
        }
    }
}
