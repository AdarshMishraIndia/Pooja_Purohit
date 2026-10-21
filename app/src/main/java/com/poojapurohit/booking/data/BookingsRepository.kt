package com.poojapurohit.booking.data

import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.poojapurohit.data.model.BookingDoc
import com.poojapurohit.data.model.BookingStatus
import com.poojapurohit.data.model.Coordinates
import com.poojapurohit.data.model.NotificationDoc
import com.poojapurohit.data.model.NotificationType
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BookingsRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth     : FirebaseAuth
) {
    fun currentUserId(): String? = auth.currentUser?.uid

    fun observeUserBookings(): Flow<Result<List<BookingDoc>>> {
        val uid = auth.currentUser?.uid
            ?: return flowOf(Result.failure(IllegalStateException("User not authenticated")))
        return callbackFlow {
            val listener = firestore.collection(BookingDoc.COLLECTION)
                .whereEqualTo(BookingDoc.Fields.USER_ID, uid)
                .orderBy(BookingDoc.Fields.CREATED_AT, Query.Direction.DESCENDING)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) { trySend(Result.failure(error)); return@addSnapshotListener }
                    val bookings = snapshot?.documents
                        ?.mapNotNull { runCatching { BookingDoc.fromDocument(it) }.getOrNull() }
                        ?: emptyList()
                    trySend(Result.success(bookings))
                }
            awaitClose { listener.remove() }
        }
    }

    fun observePurohitBookings(): Flow<Result<List<BookingDoc>>> {
        val uid = auth.currentUser?.uid
            ?: return flowOf(Result.failure(IllegalStateException("User not authenticated")))
        return callbackFlow {
            val listener = firestore.collection(BookingDoc.COLLECTION)
                .whereEqualTo(BookingDoc.Fields.PUROHIT_ID, uid)
                .orderBy(BookingDoc.Fields.CREATED_AT, Query.Direction.DESCENDING)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) { trySend(Result.failure(error)); return@addSnapshotListener }
                    val bookings = snapshot?.documents
                        ?.mapNotNull { runCatching { BookingDoc.fromDocument(it) }.getOrNull() }
                        ?: emptyList()
                    trySend(Result.success(bookings))
                }
            awaitClose { listener.remove() }
        }
    }

    /** Generic status update — accept, user-cancel, payment flows. */
    suspend fun updateBookingStatus(
        bookingId: String,
        newStatus: BookingStatus
    ): Result<Unit> = runCatching {
        firestore.collection(BookingDoc.COLLECTION).document(bookingId)
            .update(mapOf(
                BookingDoc.Fields.STATUS to newStatus.name,
                BookingDoc.Fields.UPDATED_AT to Timestamp.now()
            ) + if (newStatus == BookingStatus.ACCEPTED) mapOf(BookingDoc.Fields.CONFIRMED_AT to Timestamp.now()) else emptyMap()
            ).await()
    }

    /**
     * Reject with remarks — used for ALL purohit rejections (PAYMENT_DONE and ACCEPTED).
     * Writes status=REJECTED + remarks atomically.
     */
    suspend fun rejectBookingWithRemarks(
        bookingId: String,
        remarks  : String
    ): Result<Unit> = runCatching {
        firestore.collection(BookingDoc.COLLECTION).document(bookingId)
            .update(mapOf(
                BookingDoc.Fields.STATUS to BookingStatus.REJECTED.name,
                BookingDoc.Fields.COMMENTS to remarks.trim(),
                BookingDoc.Fields.UPDATED_AT to Timestamp.now()
            )).await()
    }

    /**
     * Step 1 of the OTP completion flow (purohit side).
     *
     * Generates a cryptographically random 6-digit OTP, writes it to
     * [bookings/{bookingId}.completionOtp], and returns it as part of the
     * Result so the ViewModel can hold it in memory for comparison.
     *
     * The OTP is intentionally NOT returned to any UI — only the ViewModel
     * holds it temporarily. Cloud Functions (wired later) will push it to
     * the customer via notification.
     *
     * Firestore security rule requirement:
     *   allow read of completionOtp only when request.auth.uid == resource.data.userId
     *   (customer can read; purohit cannot)
     */
    suspend fun initiateCompletion(bookingId: String): Result<String> = runCatching {
        val otp = (100_000..999_999).random().toString()
        firestore.collection(BookingDoc.COLLECTION).document(bookingId)
            .update(mapOf(
                BookingDoc.Fields.COMPLETION_OTP to otp,
                BookingDoc.Fields.UPDATED_AT to Timestamp.now()
            )).await()
        otp   // returned to ViewModel only; never surfaced in UI
    }

    /**
     * Step 2 of the OTP completion flow (purohit side).
     *
     * Fetches the booking document, compares [inputOtp] against the stored
     * [completionOtp], and on match atomically sets status=COMPLETED and
     * clears [completionOtp] via FieldValue.delete().
     *
     * Returns:
     *  - Result.success(Unit) on correct OTP + successful write
     *  - Result.failure(WrongOtpException) on mismatch
     *  - Result.failure(...) on any Firestore error
     */
    suspend fun verifyOtpAndComplete(
        bookingId: String,
        inputOtp : String
    ): Result<Unit> = runCatching {
        val doc = firestore.collection(BookingDoc.COLLECTION).document(bookingId).get().await()
        val storedOtp = doc.getString(BookingDoc.Fields.COMPLETION_OTP)
            ?: throw WrongOtpException("OTP not found. Please initiate completion again.")

        if (storedOtp.trim() != inputOtp.trim()) {
            throw WrongOtpException("Incorrect code. Please ask the customer for the correct code.")
        }

        firestore.collection(BookingDoc.COLLECTION).document(bookingId)
            .update(mapOf(
                BookingDoc.Fields.STATUS to BookingStatus.COMPLETED.name,
                BookingDoc.Fields.COMPLETION_OTP to FieldValue.delete(),
                BookingDoc.Fields.UPDATED_AT to Timestamp.now()
            )).await()
    }

    /**
     * Customer edit: updates address + scheduledDate on the booking document,
     * then writes a notification to the purohit's notification subcollection.
     */
    suspend fun updateBookingAddressAndTime(
        booking         : BookingDoc,
        newAddress      : String,
        newScheduledDate: Timestamp,
        newCoordinates  : Coordinates? = null
    ): Result<Unit> = runCatching {
        val batch = firestore.batch()

        val bookingFields = mutableMapOf<String, Any>(
            BookingDoc.Fields.ADDRESS to newAddress.trim(),
            BookingDoc.Fields.SCHEDULED_DATE to newScheduledDate,
            BookingDoc.Fields.UPDATED_AT to Timestamp.now()
        )
        if (newCoordinates != null) {
            bookingFields[BookingDoc.Fields.COORDINATES] = newCoordinates.toMap()
        }

        val bookingRef = firestore.collection(BookingDoc.COLLECTION).document(booking.bookingId)
        batch.update(bookingRef, bookingFields)

        val notifRef = firestore
            .collection(NotificationDoc.COLLECTION)
            .document(booking.purohitId)
            .collection(NotificationDoc.ITEMS)
            .document()
        batch.set(notifRef, NotificationDoc(
            title       = "Booking Updated",
            body        = "The customer has updated the schedule or address for \"${booking.serviceName}\".",
            isRead      = false,
            isActive    = true,
            timestamp   = Timestamp.now(),
            type        = NotificationType.BOOKING_UPDATED,
            deepLinkUrl = "poojapurohit://bookings/${booking.bookingId}"
        ).toMap())

        batch.commit().await()
    }
}

/** Thrown when the purohit enters an OTP that doesn't match the stored value. */
class WrongOtpException(message: String) : Exception(message)