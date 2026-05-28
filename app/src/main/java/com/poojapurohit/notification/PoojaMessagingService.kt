package com.poojapurohit.notification

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.net.toUri
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.poojapurohit.R
import com.poojapurohit.booking.BookingsActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout

/**
 * Handles two FCM lifecycle events:
 *
 *  1. onNewToken  — FCM rotates the device token periodically. When it does,
 *                   the old token becomes invalid and must be replaced in Firestore.
 *                   Without this, pushes silently stop working after a token rotation.
 *
 *  2. onMessageReceived — FCM only calls this when the app is in the FOREGROUND.
 *                         When the app is in background/killed, the system tray displays
 *                         the notification automatically using the `notification` block.
 *                         For foreground, we build and show it manually here.
 */
class PoojaMessagingService : FirebaseMessagingService() {

    companion object {
        private const val TAG = "PoojaMessagingService"
        private const val CHANNEL_ID = "booking_notifications"
        private const val FIRESTORE_TIMEOUT_MS = 10_000L
    }

    // SupervisorJob stored as a named val so it can be explicitly cancelled in onDestroy
    private val supervisorJob = SupervisorJob()
    private val serviceScope = CoroutineScope(supervisorJob + Dispatchers.IO)

    // ─── Token Refresh ────────────────────────────────────────────────────────

    /**
     * Called by FCM when the registration token is rotated.
     *
     * Strategy:
     * - We don't know the old token here, so we use arrayUnion (new token added)
     *   combined with the stale-token pruning in fcm.service.ts (Cloud Function side).
     * - Checks both `users` and `purohits` collections since this service is shared
     *   between both stakeholder types.
     */
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "FCM token refreshed")

        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid == null) {
            // User not signed in yet — AuthRepository.registerUser / registerServicePartner
            // will pick up the fresh token on next sign-in/registration.
            Log.d(TAG, "No signed-in user — token will be written on next login")
            return
        }

        serviceScope.launch {
            updateTokenInFirestore(uid, token)
        }
    }

    private suspend fun updateTokenInFirestore(uid: String, token: String) {
        val firestore = FirebaseFirestore.getInstance()
        try {
            withTimeout(FIRESTORE_TIMEOUT_MS) {
                // Check users first, then purohits — same pattern as AuthRepository
                val userDoc = firestore.collection("users").document(uid).get().await()
                if (userDoc.exists()) {
                    firestore.collection("users").document(uid)
                        .update("fcmTokens", FieldValue.arrayUnion(token))
                        .await()
                    Log.d(TAG, "FCM token updated for user uid=$uid")
                    return@withTimeout
                }

                val purohitDoc = firestore.collection("purohits").document(uid).get().await()
                if (purohitDoc.exists()) {
                    firestore.collection("purohits").document(uid)
                        .update("fcmTokens", FieldValue.arrayUnion(token))
                        .await()
                    Log.d(TAG, "FCM token updated for purohit uid=$uid")
                }
            }
        } catch (e: Exception) {
            // Non-critical — Cloud Function's stale-token pruning will eventually clean up.
            // The next successful push or login will re-sync the token.
            Log.e(TAG, "Failed to update FCM token in Firestore", e)
        }
    }

    // ─── Foreground Message Display ───────────────────────────────────────────

    /**
     * Called ONLY when the app is in the foreground.
     * Background/killed state: system displays the notification automatically.
     *
     * The Cloud Function sends both a `notification` block (for background display)
     * and a `data` block (for structured metadata). Here we use both to build
     * a foreground notification with the correct deep link.
     */
    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        val title = message.notification?.title ?: message.data["title"] ?: return
        val body = message.notification?.body ?: message.data["body"] ?: return
        val deepLinkUrl = message.data["deepLinkUrl"]

        Log.d(TAG, "Foreground push received | type=${message.data["type"]}")

        showNotification(title, body, deepLinkUrl)
    }

    private fun showNotification(title: String, body: String, deepLinkUrl: String?) {
        // Build the tap intent — routes to BookingsActivity with the deep link URI
        val intent = if (!deepLinkUrl.isNullOrEmpty()) {
            Intent(Intent.ACTION_VIEW, deepLinkUrl.toUri()).apply {
                setClass(this@PoojaMessagingService, BookingsActivity::class.java)
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
        } else {
            Intent(this, BookingsActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            System.currentTimeMillis().toInt(), // unique request code prevents intent collision
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.logo)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .build()

        val notificationManager = getSystemService(NotificationManager::class.java)
        // Unique notification ID prevents all pushes from collapsing into one
        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }

    override fun onDestroy() {
        super.onDestroy()
        // Cancel all pending coroutines tied to this service instance
        supervisorJob.cancel()
    }
}