package com.poojapurohit.auth

import android.app.Activity
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import com.google.firebase.Timestamp

class AuthRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
) {

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
                Result.success(isNewUser)
            } else Result.failure(Exception("Signed in user has no UID"))
        } catch (e: Exception) {
            Log.e("AuthRepository", "Firebase sign-in failed", e)
            Result.failure(e)
        }
    }

    // Check both collections — returns true if NEW user (not found anywhere)
    private suspend fun checkUserExists(uid: String): Boolean {
        val userDoc = firestore.collection("users").document(uid).get().await()
        if (userDoc.exists()) return false

        val purohitDoc = firestore.collection("purohits").document(uid).get().await()
        return !purohitDoc.exists() // new user
    }

    suspend fun registerUser(
        uid: String,
        name: String,
        phone: String,
        email: String
    ): Result<Unit> {
        return try {
            val newUser = hashMapOf(
                "userId" to uid,
                "name" to name,
                "phone" to phone,
                "email" to email,
                "createdAt" to Timestamp.now()
            )
            firestore.collection("users").document(uid).set(newUser).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("AuthRepository", "User registration error", e)
            Result.failure(e)
        }
    }

    suspend fun registerServicePartner(
        uid: String,
        name: String,
        phone: String,
        email: String,
        city: String,
        locality: String,
        proficiency: List<String>,
        experience: String
    ): Result<Unit> {
        return try {
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
                "createdAt" to Timestamp.now()
            )
            firestore.collection("purohits").document(uid).set(newPurohit).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("AuthRepository", "Purohit registration error", e)
            Result.failure(e)
        }
    }

    // Check both collections for session persistence
    suspend fun isUserRegistered(): Boolean {
        val user = auth.currentUser ?: return false
        return try {
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
            val doc = firestore
                .collection("services")
                .document("BookAPurohit")
                .get()
                .await()
            val services = (doc.get("name") as? List<*>)?.filterIsInstance<String>()
                ?: emptyList()
            Result.success(services)
        } catch (e: Exception) {
            Log.e("AuthRepository", "Failed to load services", e)
            Result.success(emptyList())
        }
    }
}