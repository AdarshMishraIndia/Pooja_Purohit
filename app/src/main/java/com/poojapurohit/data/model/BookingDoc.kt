package com.poojapurohit.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot

enum class BookingStatus {
    PENDING_PAYMENT,
    PAYMENT_DONE,
    PAYMENT_FAILED,
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
        BookingStatus.PAYMENT_FAILED -> BookingCategory.CANCELLED
        BookingStatus.REJECTED,
        BookingStatus.CANCELLED_BY_USER,
        BookingStatus.CANCELLED_BY_PUROHIT,
        BookingStatus.REFUNDED,
        BookingStatus.AUTO_CANCELLED -> BookingCategory.CANCELLED

        BookingStatus.COMPLETED -> BookingCategory.COMPLETED
    }

val BookingStatus.displayLabel: String
    get() = when (this) {
        BookingStatus.PAYMENT_FAILED     -> "Payment Failed"
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

/** Firestore: bookings/{bookingId}.coordinates — embedded map. */
data class Coordinates(
    val latitude : Double = 0.0,
    val longitude: Double = 0.0
) {
    fun toMap(): Map<String, Double> = mapOf(
        Fields.LATITUDE to latitude,
        Fields.LONGITUDE to longitude
    )

    object Fields {
        const val LATITUDE = "latitude"
        const val LONGITUDE = "longitude"
    }
}

/**
 * Firestore: bookings/{bookingId} — transaction record.
 *
 * Writers: Android app (checkout, status/address updates), Cloud Functions
 * (cancellationReason, status transitions).
 */
data class BookingDoc(
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

    val createdAt  : Timestamp? = null,
    val updatedAt  : Timestamp? = null,
    val confirmedAt: Timestamp? = null,

    val comments: String? = null,

    /**
     * 6-digit OTP written by the purohit app when initiating completion.
     * Cloud Functions push this to the customer via notification.
     * Cleared (field deleted) once the booking is marked COMPLETED.
     *
     * Security: Firestore rules must restrict read access to userId == auth.uid
     * so the purohit cannot read back their own written value.
     */
    val completionOtp: String? = null,

    /** Written by Cloud Functions on auto-cancel: NO_PAYMENT | NO_PUROHIT_RESPONSE. */
    val cancellationReason: String? = null
) {

    /** Full-document write map. Null optionals/timestamps are omitted, never invented. */
    fun toMap(): Map<String, Any> = buildMap<String, Any> {
        put(Fields.BOOKING_ID, bookingId)
        put(Fields.USER_ID, userId)
        put(Fields.USER_NAME, userName)
        put(Fields.USER_PHONE, userPhone)
        put(Fields.PUROHIT_ID, purohitId)
        put(Fields.PUROHIT_NAME, purohitName)
        put(Fields.PUROHIT_PHONE, purohitPhone)
        put(Fields.SERVICE_NAME, serviceName)
        put(Fields.AMOUNT, amount)
        put(Fields.STATUS, status.name)
        put(Fields.RAZORPAY_ORDER_ID, razorpayOrderId)
        put(Fields.RAZORPAY_PAYMENT_ID, razorpayPaymentId)
        scheduledDate?.let { put(Fields.SCHEDULED_DATE, it) }
        put(Fields.ADDRESS, address)
        coordinates?.let { put(Fields.COORDINATES, it.toMap()) }
        createdAt?.let { put(Fields.CREATED_AT, it) }
        updatedAt?.let { put(Fields.UPDATED_AT, it) }
        confirmedAt?.let { put(Fields.CONFIRMED_AT, it) }
        comments?.let { put(Fields.COMMENTS, it) }
        completionOtp?.let { put(Fields.COMPLETION_OTP, it) }
        cancellationReason?.let { put(Fields.CANCELLATION_REASON, it) }
    }

    object Fields {
        const val BOOKING_ID = "bookingId"
        const val USER_ID = "userId"
        const val USER_NAME = "userName"
        const val USER_PHONE = "userPhone"
        const val PUROHIT_ID = "purohitId"
        const val PUROHIT_NAME = "purohitName"
        const val PUROHIT_PHONE = "purohitPhone"
        const val SERVICE_NAME = "serviceName"
        const val AMOUNT = "amount"
        const val STATUS = "status"
        const val RAZORPAY_ORDER_ID = "razorpayOrderId"
        const val RAZORPAY_PAYMENT_ID = "razorpayPaymentId"
        const val SCHEDULED_DATE = "scheduledDate"
        const val ADDRESS = "address"
        const val COORDINATES = "coordinates"
        const val CREATED_AT = "createdAt"
        const val UPDATED_AT = "updatedAt"
        const val CONFIRMED_AT = "confirmedAt"
        const val COMMENTS = "comments"
        const val COMPLETION_OTP = "completionOtp"
        const val CANCELLATION_REASON = "cancellationReason"
    }

    companion object {
        const val COLLECTION = "bookings"

        fun fromDocument(doc: DocumentSnapshot): BookingDoc {
            return BookingDoc(
                bookingId = doc.getString(Fields.BOOKING_ID) ?: doc.id,

                userId    = doc.getString(Fields.USER_ID)    ?: "",
                userName  = doc.getString(Fields.USER_NAME)  ?: "",
                userPhone = doc.getString(Fields.USER_PHONE) ?: "",

                purohitId    = doc.getString(Fields.PUROHIT_ID)    ?: "",
                purohitName  = doc.getString(Fields.PUROHIT_NAME)  ?: "",
                purohitPhone = doc.getString(Fields.PUROHIT_PHONE) ?: "",

                serviceName = doc.getString(Fields.SERVICE_NAME) ?: "",
                amount      = doc.getLong(Fields.AMOUNT)         ?: 0L,

                status            = BookingStatus.fromString(doc.getString(Fields.STATUS)),
                razorpayOrderId   = doc.getString(Fields.RAZORPAY_ORDER_ID)   ?: "",
                razorpayPaymentId = doc.getString(Fields.RAZORPAY_PAYMENT_ID) ?: "",

                scheduledDate = doc.getTimestamp(Fields.SCHEDULED_DATE),
                address       = doc.getString(Fields.ADDRESS) ?: "",
                coordinates   = (doc.get(Fields.COORDINATES) as? Map<*, *>)?.let { map ->
                    Coordinates(
                        latitude  = (map[Coordinates.Fields.LATITUDE]  as? Double) ?: 0.0,
                        longitude = (map[Coordinates.Fields.LONGITUDE] as? Double) ?: 0.0
                    )
                },

                createdAt   = doc.getTimestamp(Fields.CREATED_AT),
                updatedAt   = doc.getTimestamp(Fields.UPDATED_AT),
                confirmedAt = doc.getTimestamp(Fields.CONFIRMED_AT),

                comments           = doc.getString(Fields.COMMENTS),
                completionOtp      = doc.getString(Fields.COMPLETION_OTP),
                cancellationReason = doc.getString(Fields.CANCELLATION_REASON)
            )
        }
    }
}
