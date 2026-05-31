package com.poojapurohit.booking.data

import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.poojapurohit.booking.model.Booking
import com.poojapurohit.booking.model.BookingStatus
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

    fun observeUserBookings(): Flow<Result<List<Booking>>> {
        val uid = auth.currentUser?.uid
            ?: return flowOf(Result.failure(IllegalStateException("User not authenticated")))
        return callbackFlow {
            val listener = firestore.collection("bookings")
                .whereEqualTo("userId", uid)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) { trySend(Result.failure(error)); return@addSnapshotListener }
                    val bookings = snapshot?.documents
                        ?.mapNotNull { runCatching { Booking.fromDocument(it) }.getOrNull() }
                        ?: emptyList()
                    trySend(Result.success(bookings))
                }
            awaitClose { listener.remove() }
        }
    }

    fun observePurohitBookings(): Flow<Result<List<Booking>>> {
        val uid = auth.currentUser?.uid
            ?: return flowOf(Result.failure(IllegalStateException("User not authenticated")))
        return callbackFlow {
            val listener = firestore.collection("bookings")
                .whereEqualTo("purohitId", uid)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) { trySend(Result.failure(error)); return@addSnapshotListener }
                    val bookings = snapshot?.documents
                        ?.mapNotNull { runCatching { Booking.fromDocument(it) }.getOrNull() }
                        ?: emptyList()
                    trySend(Result.success(bookings))
                }
            awaitClose { listener.remove() }
        }
    }

    /** Generic status update — accept, complete, user-cancel, payment flows. */
    suspend fun updateBookingStatus(
        bookingId: String,
        newStatus: BookingStatus
    ): Result<Unit> = runCatching {
        firestore.collection("bookings").document(bookingId)
            .update(mapOf("status" to newStatus.name, "updatedAt" to Timestamp.now()))
            .await()
    }

    /**
     * Reject with remarks — used for ALL purohit rejections (PAYMENT_DONE and ACCEPTED).
     * Writes status=REJECTED + remarks atomically.
     */
    suspend fun rejectBookingWithRemarks(
        bookingId: String,
        remarks  : String
    ): Result<Unit> = runCatching {
        firestore.collection("bookings").document(bookingId)
            .update(mapOf(
                "status"    to BookingStatus.REJECTED.name,
                "comments"   to remarks.trim(),
                "updatedAt" to Timestamp.now()
            )).await()
    }

    /**
     * Customer edit: updates address + scheduledDate on the booking document,
     * then writes a notification to the purohit's notification subcollection.
     *
     * Firestore path for notification:
     *   notifications/{purohitId}/items/{auto-id}
     */
    suspend fun updateBookingAddressAndTime(
        booking         : Booking,
        newAddress      : String,
        newScheduledDate: Timestamp,
        newCoordinates  : com.poojapurohit.booking.model.Coordinates? = null
    ): Result<Unit> = runCatching {
        val batch = firestore.batch()

        val bookingFields = mutableMapOf<String, Any>(
            "address"       to newAddress.trim(),
            "scheduledDate" to newScheduledDate,
            "updatedAt"     to Timestamp.now()
        )
        // Only write coordinates if the caller provided new ones
        if (newCoordinates != null) {
            bookingFields["coordinates"] = mapOf(
                "latitude"  to newCoordinates.latitude,
                "longitude" to newCoordinates.longitude
            )
        }

        val bookingRef = firestore.collection("bookings").document(booking.bookingId)
        batch.update(bookingRef, bookingFields)

        val notifRef = firestore
            .collection("notifications")
            .document(booking.purohitId)
            .collection("items")
            .document()
        batch.set(notifRef, mapOf(
            "title"       to "Booking Updated",
            "body"        to "The customer has updated the schedule or address for \"${booking.serviceName}\".",
            "isRead"      to false,
            "isActive"    to true,
            "timestamp"   to Timestamp.now(),
            "type"        to "BOOKING_UPDATED",
            "deepLinkUrl" to "poojapurohit://bookings/${booking.bookingId}"
        ))

        batch.commit().await()
    }
}