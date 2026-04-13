package com.poojapurohit.booking.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.poojapurohit.booking.model.Booking
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOf

class BookingRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {

    /**
     * Observes all bookings for the currently authenticated user in real time.
     * Queries by userId — forward-compatible with purohit-side queries by swapping the field.
     *
     * Returns Flow<Result<List<Booking>>> so the ViewModel handles errors without try/catch noise.
     */
    fun observeUserBookings(): Flow<Result<List<Booking>>> {
        val uid = auth.currentUser?.uid
            ?: return flowOf(Result.failure(IllegalStateException("User not authenticated")))

        return callbackFlow {
            val listener = firestore
                .collection("bookings")
                .whereEqualTo("userId", uid)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        trySend(Result.failure(error))
                        return@addSnapshotListener
                    }
                    val bookings = snapshot?.documents
                        ?.mapNotNull { doc ->
                            runCatching { Booking.fromDocument(doc) }.getOrNull()
                        }
                        ?: emptyList()

                    trySend(Result.success(bookings))
                }

            awaitClose { listener.remove() }
        }
    }

    /**
     * Stub: observes bookings for a purohit account.
     * Swap userId → purohitId when purohit-side dashboard is built.
     */
    fun observePurohitBookings(): Flow<Result<List<Booking>>> {
        val uid = auth.currentUser?.uid
            ?: return flowOf(Result.failure(IllegalStateException("User not authenticated")))

        return callbackFlow {
            val listener = firestore
                .collection("bookings")
                .whereEqualTo("purohitId", uid)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        trySend(Result.failure(error))
                        return@addSnapshotListener
                    }
                    val bookings = snapshot?.documents
                        ?.mapNotNull { doc ->
                            runCatching { Booking.fromDocument(doc) }.getOrNull()
                        }
                        ?: emptyList()

                    trySend(Result.success(bookings))
                }

            awaitClose { listener.remove() }
        }
    }
}
