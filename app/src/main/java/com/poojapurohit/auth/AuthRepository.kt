package com.poojapurohit.auth

import android.app.Activity
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
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

class AuthRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
) {

    private suspend fun getFcmToken(): String? {
        return try {
            FirebaseMessaging.getInstance().token.await()
        } catch (e: Exception) {
            Log.e("AuthRepository", "Failed to get FCM token", e)
            null
        }
    }

    /**
     * Forces a refresh of the Firebase ID Token.
     * This is the standard way to handle 401s in Firebase-backed apps.
     * @return true if token refresh was successful
     */
    suspend fun refreshToken(): Boolean = withContext(Dispatchers.IO) {
        return@withContext try {
            val user = auth.currentUser
            if (user != null) {
                // forceRefresh = true ensures the token is fetched from the server
                user.getIdToken(true).await()
                Log.d("AuthRepository", "Firebase ID token refreshed successfully.")
                true
            } else {
                false
            }
        } catch (e: Exception) {
            Log.e("AuthRepository", "Failed to refresh token", e)
            false
        }
    }

    suspend fun signInWithGoogle(
        activity: Activity,
        credentialManager: CredentialManager,
        clientId: String
    ): Result<Boolean> {
        return try {
            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(clientId)
                .build()
            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()
            val result = credentialManager.getCredential(activity, request)
            val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(result.credential.data)
            val idToken = googleIdTokenCredential.idToken
            if (idToken.isEmpty()) Result.failure(Exception("Missing ID token"))
            else firebaseAuthWithGoogle(idToken)
        } catch (e: GetCredentialException) {
            Log.e("AuthRepository", "Credential fetch failed", e)
            Result.failure(e)
        } catch (e: Exception) {
            Log.e("AuthRepository", "Unexpected error", e)
            Result.failure(e)
        }
    }

    suspend fun firebaseAuthWithGoogle(idToken: String): Result<Boolean> {
        return try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val authResult = auth.signInWithCredential(credential).await()
            val user = authResult.user
            if (user?.uid != null) {
                val isNewUser = checkUserExists(user.uid)
                if (!isNewUser) {
                    appendFcmTokenToExistingUser(user.uid)
                }
                Result.success(isNewUser)
            } else Result.failure(Exception("Signed in user has no UID"))
        } catch (e: Exception) {
            Log.e("AuthRepository", "Firebase sign-in failed", e)
            Result.failure(e)
        }
    }

    private suspend fun appendFcmTokenToExistingUser(uid: String) {
        val token = getFcmToken() ?: return
        try {
            val userDoc = firestore.collection("users").document(uid).get().await()
            if (userDoc.exists()) {
                firestore.collection("users").document(uid)
                    .update("fcmTokens", FieldValue.arrayUnion(token))
                    .await()
                return
            }
            val purohitDoc = firestore.collection("purohits").document(uid).get().await()
            if (purohitDoc.exists()) {
                firestore.collection("purohits").document(uid)
                    .update("fcmTokens", FieldValue.arrayUnion(token))
                    .await()
            }
        } catch (e: Exception) {
            Log.e("AuthRepository", "Failed to append FCM token", e)
        }
    }

    private suspend fun checkUserExists(uid: String): Boolean {
        return withContext(Dispatchers.IO) {
            val userDoc = firestore.collection("users").document(uid).get().await()
            if (userDoc.exists()) return@withContext false

            val purohitDoc = firestore.collection("purohits").document(uid).get().await()
            return@withContext !purohitDoc.exists()
        }
    }

    suspend fun registerUser(uid: String, name: String, phone: String, email: String): Result<Unit> {
        return try {
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
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun registerServicePartner(
        uid: String, name: String, phone: String, email: String,
        city: String, locality: String, proficiency: List<String>, experience: String
    ): Result<Unit> {
        return try {
            val token = getFcmToken()
            val newPurohit = hashMapOf(
                "purohitId" to uid,
                "name" to name,
                "phone" to phone,
                "email" to email,
                "city" to city,
                "locality" to locality,
                "proficiency" to proficiency,
                "experience" to (experience.toIntOrNull() ?: 0),
                "isVerified" to false,
                "isAvailable" to false,
                "rating" to 0.0,
                "totalBookings" to 0,
                "createdAt" to Timestamp.now(),
                "fcmTokens" to listOfNotNull(token)
            )
            firestore.collection("purohits").document(uid).set(newPurohit).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun isUserRegistered(): Boolean {
        val user = auth.currentUser ?: return false
        return try {
            // Refresh to ensure we have the latest auth state
            user.reload().await()
            val userDoc = firestore.collection("users").document(user.uid).get().await()
            if (userDoc.exists()) return true

            val purohitDoc = firestore.collection("purohits").document(user.uid).get().await()
            purohitDoc.exists()
        } catch (_: Exception) {
            auth.signOut()
            false
        }
    }

    suspend fun loadServices(): Result<List<String>> {
        return try {
            val doc = firestore.collection("services").document("BookAPurohit").get().await()
            val services = (doc.get("name") as? List<*>)?.filterIsInstance<String>() ?: emptyList()
            Result.success(services)
        } catch (_: Exception) {
            Result.success(emptyList())
        }
    }
}