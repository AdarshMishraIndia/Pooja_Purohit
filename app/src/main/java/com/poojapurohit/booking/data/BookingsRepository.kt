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
    private val auth: FirebaseAuth
) {

    fun currentUserId(): String? = auth.currentUser?.uid

    fun observeUserBookings(): Flow<Result<List<Booking>>> {
        val uid = auth.currentUser?.uid
            ?: return flowOf(Result.failure(IllegalStateException("User not authenticated")))

        return callbackFlow {
            val listener = firestore
                .collection("bookings")
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
            val listener = firestore
                .collection("bookings")
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

    suspend fun updateBookingStatus(bookingId: String, newStatus: BookingStatus): Result<Unit> {
        return runCatching {
            firestore.collection("bookings")
                .document(bookingId)
                .update(
                    mapOf(
                        "status" to newStatus.name,
                        "updatedAt" to Timestamp.now()
                    )
                )
                .await()
        }
    }
}