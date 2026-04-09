package com.poojapurohit.notification

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.poojapurohit.notification.compose.model.NotificationItem
import com.poojapurohit.notification.compose.model.NotificationType
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class NotificationRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    companion object {
        private const val TAG = "NotificationRepository"
        private const val COLLECTION_NOTIFICATIONS = "notifications"
        private const val SUBCOLLECTION_ITEMS = "items"
        private const val FIELD_TIMESTAMP = "timestamp"
        private const val FIELD_IS_READ = "isRead"
        private const val FIELD_TYPE = "type"
        private const val FIELD_DEEP_LINK = "deepLinkUrl"
    }

    /**
     * Returns a real-time Flow of notifications using Firestore snapshot listener.
     * Automatically emits on any remote or local change.
     * Path: notifications/{uid}/items
     */
    fun observeNotifications(): Flow<Result<List<NotificationItem>>> = callbackFlow {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            trySend(Result.failure(Exception("User not signed in")))
            close()
            return@callbackFlow
        }

        val registration: ListenerRegistration = firestore
            .collection(COLLECTION_NOTIFICATIONS)
            .document(uid)
            .collection(SUBCOLLECTION_ITEMS)
            .orderBy(FIELD_TIMESTAMP, Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Snapshot listener error", error)
                    trySend(Result.failure(error))
                    return@addSnapshotListener
                }

                if (snapshot == null) {
                    trySend(Result.success(emptyList()))
                    return@addSnapshotListener
                }

                val items = snapshot.documents.mapNotNull { doc ->
                    try {
                        NotificationItem(
                            id = doc.id,
                            title = doc.getString("title") ?: "",
                            body = doc.getString("body") ?: "",
                            timestamp = doc.getTimestamp(FIELD_TIMESTAMP)
                                ?: com.google.firebase.Timestamp.now(),
                            isRead = doc.getBoolean(FIELD_IS_READ) ?: false,
                            type = NotificationType.fromString(doc.getString(FIELD_TYPE)),
                            deepLinkUrl = doc.getString(FIELD_DEEP_LINK)
                        )
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to parse notification doc: ${doc.id}", e)
                        null
                    }
                }
                trySend(Result.success(items))
            }

        awaitClose { registration.remove() }
    }

    /**
     * Marks a single notification as read.
     */
    suspend fun markAsRead(notificationId: String): Result<Unit> {
        val uid = auth.currentUser?.uid
            ?: return Result.failure(Exception("User not signed in"))

        return try {
            firestore
                .collection(COLLECTION_NOTIFICATIONS)
                .document(uid)
                .collection(SUBCOLLECTION_ITEMS)
                .document(notificationId)
                .update(FIELD_IS_READ, true)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to mark notification as read: $notificationId", e)
            Result.failure(e)
        }
    }

    /**
     * Marks all unread notifications as read in a single batched write.
     */
    suspend fun markAllAsRead(unreadIds: List<String>): Result<Unit> {
        val uid = auth.currentUser?.uid
            ?: return Result.failure(Exception("User not signed in"))

        if (unreadIds.isEmpty()) return Result.success(Unit)

        return try {
            val batch = firestore.batch()
            val collectionRef = firestore
                .collection(COLLECTION_NOTIFICATIONS)
                .document(uid)
                .collection(SUBCOLLECTION_ITEMS)

            unreadIds.forEach { id ->
                batch.update(collectionRef.document(id), FIELD_IS_READ, true)
            }
            batch.commit().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to mark notifications as read", e)
            Result.failure(e)
        }
    }

    /**
     * Deletes a single notification document.
     */
    suspend fun deleteNotification(notificationId: String): Result<Unit> {
        val uid = auth.currentUser?.uid
            ?: return Result.failure(Exception("User not signed in"))

        return try {
            firestore
                .collection(COLLECTION_NOTIFICATIONS)
                .document(uid)
                .collection(SUBCOLLECTION_ITEMS)
                .document(notificationId)
                .delete()
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete notification: $notificationId", e)
            Result.failure(e)
        }
    }

    /**
     * Deletes all notifications for the current user in a batched write.
     */
    suspend fun deleteAllNotifications(ids: List<String>): Result<Unit> {
        val uid = auth.currentUser?.uid
            ?: return Result.failure(Exception("User not signed in"))

        if (ids.isEmpty()) return Result.success(Unit)

        return try {
            // Firestore batch limit is 500 operations
            val collectionRef = firestore
                .collection(COLLECTION_NOTIFICATIONS)
                .document(uid)
                .collection(SUBCOLLECTION_ITEMS)

            ids.chunked(500).forEach { chunk ->
                val batch = firestore.batch()
                chunk.forEach { id -> batch.delete(collectionRef.document(id)) }
                batch.commit().await()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete all notifications", e)
            Result.failure(e)
        }
    }
}
