package com.poojapurohit.bookpurohit

import android.app.Application
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestoreSettings
import com.google.firebase.firestore.persistentCacheSettings
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class BookPurohitApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // FIX #1 — OFFLINE PERSISTENCE
        // Firestore writes are committed to local cache first.
        // .set().await() resolves on local write — not server round-trip.
        // SDK queues unsynced writes and flushes when connectivity returns.
        // This makes processPaymentStub() atomic from the app's perspective:
        // either the local write succeeds (and will eventually sync) or it throws.
        FirebaseFirestore.getInstance().firestoreSettings = firestoreSettings {
            setLocalCacheSettings(
                persistentCacheSettings {
                    // 50 MB cache — adjust if needed (default is 40 MB)
                    setSizeBytes(50L * 1024L * 1024L)
                }
            )
        }
    }
}