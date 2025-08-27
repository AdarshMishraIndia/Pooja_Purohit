package com.poojapurohit.auth

import android.content.Context
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
        context: Context,
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
            val result = credentialManager.getCredential(context, request)
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

    private suspend fun firebaseAuthWithGoogle(idToken: String): Result<Boolean> {
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

    private suspend fun checkUserExists(uid: String): Boolean {
        val userDocRef = firestore.collection("users").document(uid)
        val document = userDocRef.get().await()
        return !document.exists()
    }

    suspend fun registerUser(uid: String, name: String, phone: String, email: String): Result<Unit> {
        return try {
            val userDocRef = firestore.collection("users").document(uid)
            val newUser = mapOf(
                "name" to name,
                "phone" to phone,
                "email" to email,
                "createdAt" to Timestamp.now()
            )
            userDocRef.set(newUser).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("AuthRepository", "Firestore registration error", e)
            Result.failure(e)
        }
    }

    suspend fun registerServicePartner(uid: String, name: String, phone: String, email: String, location: String, proficiency: List<String>, experience: String): Result<Unit> {
        return try {
            val userDocRef = firestore.collection("users").document(uid)
            val newUser = mapOf(
                "name" to name,
                "phone" to phone,
                "email" to email,
                "location" to location,
                "proficiency" to proficiency,
                "experience" to experience,
                "createdAt" to Timestamp.now()
            )
            userDocRef.set(newUser).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("AuthRepository", "Firestore service partner registration error", e)
            Result.failure(e)
        }
    }

    suspend fun isUserRegistered(): Boolean {
        val user = auth.currentUser ?: return false
        return try {
            user.reload().await()
            val doc = firestore.collection("users").document(user.uid).get().await()
            doc.exists()
        } catch (_: Exception) {
            auth.signOut()
            false
        }
    }
}
