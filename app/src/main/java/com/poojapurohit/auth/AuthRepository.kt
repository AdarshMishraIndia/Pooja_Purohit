package com.poojapurohit.auth

import android.app.Activity
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlin.time.Duration.Companion.milliseconds

class AuthRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
) {

    companion object {
        private const val TAG = "AuthRepository"

        /** Max time for the full Google credential + Firebase sign-in round trip. */
        private const val AUTH_TIMEOUT_MS = 20_000L

        /** Max time for Firestore read/write operations. */
        private const val FIRESTORE_TIMEOUT_MS = 15_000L
    }

    // ─── Sealed result for typed error surfacing to ViewModel ────────────────

    sealed class AuthResult {
        data class Success(val isNewUser: Boolean) : AuthResult()
        object NoCredentials : AuthResult()       // No Google account on device
        object Cancelled : AuthResult()           // User dismissed the picker
        object Timeout : AuthResult()             // Network too slow
        data class Failure(val cause: Throwable) : AuthResult()
    }

    // ─── FCM Token ───────────────────────────────────────────────────────────

    private suspend fun getFcmToken(): String? {
        return try {
            FirebaseMessaging.getInstance().token.await()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get FCM token", e)
            null
        }
    }

    // ─── Token Refresh ───────────────────────────────────────────────────────

    /**
     * Forces a refresh of the Firebase ID Token.
     * @return true if token refresh was successful
     */
    suspend fun refreshToken(): Boolean = withContext(Dispatchers.IO) {
        return@withContext try {
            val user = auth.currentUser
            if (user != null) {
                user.getIdToken(true).await()
                Log.d(TAG, "Firebase ID token refreshed successfully.")
                true
            } else {
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to refresh token", e)
            false
        }
    }

    // ─── Google Sign-In ──────────────────────────────────────────────────────

    /**
     * Two-pass Google Sign-In strategy:
     *
     * Pass 1 — [GetGoogleIdOption] with filterByAuthorizedAccounts=true:
     *   Fast, zero-friction bottom sheet for users who have previously
     *   signed in. No account picker UI shown if only one account qualifies.
     *
     * Pass 2 — [GetSignInWithGoogleOption] (triggered on [NoCredentialException]):
     *   Full Google account picker that includes "Use another account",
     *   allowing users to add a new account not yet on the device.
     *
     * Returns a typed [AuthResult] instead of a generic Result<Boolean>,
     * so the ViewModel can show specific messages per failure reason.
     */
    suspend fun signInWithGoogle(
        activity: Activity,
        credentialManager: CredentialManager,
        clientId: String
    ): AuthResult {
        return try {
            withTimeout(AUTH_TIMEOUT_MS.milliseconds) {
                val signInWithGoogleOption = GetSignInWithGoogleOption.Builder(clientId).build()

                val request = GetCredentialRequest.Builder()
                    .addCredentialOption(signInWithGoogleOption)
                    .build()

                val result = credentialManager.getCredential(activity, request)
                val googleIdTokenCredential =
                    GoogleIdTokenCredential.createFrom(result.credential.data)
                val idToken = googleIdTokenCredential.idToken

                if (idToken.isEmpty()) {
                    AuthResult.Failure(Exception("Missing ID token"))
                } else {
                    firebaseAuthWithGoogle(idToken)
                }
            }
        } catch (_: kotlinx.coroutines.TimeoutCancellationException) {
            Log.w(TAG, "signInWithGoogle timed out after ${AUTH_TIMEOUT_MS}ms")
            AuthResult.Timeout
        } catch (e: NoCredentialException) {
            Log.w(TAG, "No credentials found on device", e)
            AuthResult.NoCredentials
        } catch (e: GetCredentialCancellationException) {
            Log.d(TAG, "User cancelled credential picker", e)
            AuthResult.Cancelled
        } catch (e: GetCredentialException) {
            Log.e(TAG, "Credential fetch failed: ${e.type}", e)
            AuthResult.Failure(e)
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error during sign-in", e)
            AuthResult.Failure(e)
        }
    }

    suspend fun firebaseAuthWithGoogle(idToken: String): AuthResult {
        return try {
            withTimeout(AUTH_TIMEOUT_MS.milliseconds) {
                val credential = GoogleAuthProvider.getCredential(idToken, null)
                val authResult = auth.signInWithCredential(credential).await()
                val user = authResult.user

                if (user?.uid != null) {
                    val isNewUser = checkUserExists(user.uid)
                    if (!isNewUser) {
                        appendFcmTokenToExistingUser(user.uid)
                    }
                    AuthResult.Success(isNewUser)
                } else {
                    AuthResult.Failure(Exception("Signed in user has no UID"))
                }
            }
        } catch (_: kotlinx.coroutines.TimeoutCancellationException) {
            Log.w(TAG, "firebaseAuthWithGoogle timed out")
            AuthResult.Timeout
        } catch (e: Exception) {
            Log.e(TAG, "Firebase sign-in failed", e)
            AuthResult.Failure(e)
        }
    }

    // ─── FCM Token Append ────────────────────────────────────────────────────

    private suspend fun appendFcmTokenToExistingUser(uid: String) {
        val token = getFcmToken() ?: return
        try {
            withTimeout(FIRESTORE_TIMEOUT_MS.milliseconds) {
                val userDoc = firestore.collection("users").document(uid).get().await()
                if (userDoc.exists()) {
                    firestore.collection("users").document(uid)
                        .update("fcmTokens", FieldValue.arrayUnion(token))
                        .await()
                    return@withTimeout
                }
                val purohitDoc = firestore.collection("purohits").document(uid).get().await()
                if (purohitDoc.exists()) {
                    firestore.collection("purohits").document(uid)
                        .update("fcmTokens", FieldValue.arrayUnion(token))
                        .await()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to append FCM token", e)
            // Non-critical — swallow silently
        }
    }

    // ─── User Existence Check ────────────────────────────────────────────────

    private suspend fun checkUserExists(uid: String): Boolean {
        return withContext(Dispatchers.IO) {
            withTimeout(FIRESTORE_TIMEOUT_MS.milliseconds) {
                val userDoc = firestore.collection("users").document(uid).get().await()
                if (userDoc.exists()) return@withTimeout false

                val purohitDoc = firestore.collection("purohits").document(uid).get().await()
                return@withTimeout !purohitDoc.exists()
            }
        }
    }

    // ─── Register User ───────────────────────────────────────────────────────

    suspend fun registerUser(
        uid: String,
        name: String,
        phone: String,
        email: String
    ): Result<Unit> {
        return try {
            withTimeout(FIRESTORE_TIMEOUT_MS.milliseconds) {
                val token = getFcmToken()
                val newUser = hashMapOf(
                    "userId" to uid,
                    "name" to name,
                    "phone" to phone,
                    "email" to email,
                    "createdAt" to Timestamp.now(),
                    "fcmTokens" to listOfNotNull(token)
                )
                firestore.collection("users").document(uid).set(newUser).await()
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ─── Register Service Partner ────────────────────────────────────────────

    suspend fun registerServicePartner(
        uid: String,
        name: String,
        phone: String,
        email: String,
        city: String,
        locality: String,
        proficiency: List<String>,
        serviceIds: List<String>,
        experience: String
    ): Result<Unit> {
        return try {
            withTimeout(FIRESTORE_TIMEOUT_MS.milliseconds) {
                val token = getFcmToken()

                val newPurohit = hashMapOf(
                    "purohitId" to uid,
                    "name" to name,
                    "phone" to phone,
                    "email" to email,
                    "city" to city,
                    "locality" to locality,
                    "proficiency" to proficiency,       // Array of human-readable names
                    "serviceIds" to serviceIds,         // Array of unique service slugs/IDs
                    "fcmTokens" to listOfNotNull(token),
                    "isVerified" to false,
                    "isAvailable" to false,
                    "rating" to 0.0,
                    "totalBookings" to 0,
                    "createdAt" to Timestamp.now(),
                    "updatedAt" to Timestamp.now()
                )

                // Safe parsing wrapper to prevent formatting crashes
                val parsedExperience = experience.toIntOrNull() ?: 0
                newPurohit["experience"] = parsedExperience

                firestore.collection("purohits").document(uid).set(newPurohit).await()
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Critical: Service partner registration failed for UID: $uid", e)
            Result.failure(e)
        }
    }

    // ─── Is User Registered ──────────────────────────────────────────────────

    suspend fun isUserRegistered(): Boolean {
        val user = auth.currentUser ?: return false
        return try {
            withTimeout(FIRESTORE_TIMEOUT_MS.milliseconds) {
                user.reload().await()
                val userDoc = firestore.collection("users").document(user.uid).get().await()
                if (userDoc.exists()) return@withTimeout true

                val purohitDoc =
                    firestore.collection("purohits").document(user.uid).get().await()
                purohitDoc.exists()
            }
        } catch (_: Exception) {
            auth.signOut()
            false
        }
    }

    // ─── Load Services Map ───────────────────────────────────────────────────

    /**
     * Queries the services collection and maps the Document ID (slug) to its Name.
     */
    suspend fun loadServicesMap(): Result<Map<String, String>> {
        return try {
            withTimeout(FIRESTORE_TIMEOUT_MS.milliseconds) {
                val snapshot = firestore.collection("services")
                    .whereEqualTo("isActive", true)
                    .orderBy("displayOrder")
                    .get()
                    .await()

                val servicesMap = snapshot.documents.associate { doc ->
                    doc.id to (doc.getString("name") ?: "")
                }.filterValues { it.isNotEmpty() }

                Result.success(servicesMap)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load services map", e)
            Result.success(emptyMap())
        }
    }
}