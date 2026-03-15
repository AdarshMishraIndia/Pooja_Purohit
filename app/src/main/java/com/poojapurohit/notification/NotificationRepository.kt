package com.poojapurohit.notification

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.poojapurohit.notification.compose.model.NotificationItem
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
    }

    /**
     * Fetches all notifications for the current user, ordered by timestamp descending (latest first).
     * Path: notifications/{uid}/items
     */
    suspend fun fetchNotifications(): Result<List<NotificationItem>> {
        val uid = auth.currentUser?.uid
            ?: return Result.failure(Exception("User not signed in"))

        return try {
            val snapshot = firestore
                .collection(COLLECTION_NOTIFICATIONS)
                .document(uid)
                .collection(SUBCOLLECTION_ITEMS)
                .orderBy(FIELD_TIMESTAMP, Query.Direction.DESCENDING)
                .get()
                .await()

            val items = snapshot.documents.mapNotNull { doc ->
                try {
                    NotificationItem(
                        id = doc.id,
                        title = doc.getString("title") ?: "",
                        body = doc.getString("body") ?: "",
                        timestamp = doc.getTimestamp(FIELD_TIMESTAMP)
                            ?: com.google.firebase.Timestamp.now(),
                        isRead = doc.getBoolean(FIELD_IS_READ) ?: false
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to parse notification doc: ${doc.id}", e)
                    null
                }
            }
            Result.success(items)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch notifications", e)
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
}
