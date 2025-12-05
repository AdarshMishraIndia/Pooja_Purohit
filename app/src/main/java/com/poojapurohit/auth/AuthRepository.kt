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
import com.google.firebase.firestore.FieldValue
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
            // Step 1: Register the service partner in users collection
            val userDocRef = firestore.collection("users").document(uid)
            val newUser = mapOf(
                "name" to name,
                "phone" to phone,
                "email" to email,
                "city" to city,
                "locality" to locality,
                "proficiency" to proficiency,
                "experience" to experience,
                "createdAt" to Timestamp.now()
            )
            userDocRef.set(newUser).await()

            // Step 2: Add service partner to locations collection and subcollection
            addServicePartnerToLocation(uid, city, locality)

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("AuthRepository", "Firestore service partner registration error", e)
            Result.failure(e)
        }
    }

    private suspend fun addServicePartnerToLocation(uid: String, city: String, locality: String) {
        try {
            // Normalize city and locality
            val normalizedCity = city.trim().replaceFirstChar {
                if (it.isLowerCase()) it.titlecase() else it.toString()
            }
            val normalizedLocality = locality.trim().replaceFirstChar {
                if (it.isLowerCase()) it.titlecase() else it.toString()
            }

            // Step 1: Update city-level document
            val cityDocRef = firestore.collection("locations").document(normalizedCity)
            val cityDoc = cityDocRef.get().await()

            if (cityDoc.exists()) {
                // City exists - add UID to array and increment count
                cityDocRef.update(
                    mapOf(
                        "servicePartners" to FieldValue.arrayUnion(uid),
                        "count" to FieldValue.increment(1)
                    )
                ).await()
            } else {
                // City doesn't exist - create new document
                cityDocRef.set(
                    mapOf(
                        "name" to normalizedCity,
                        "servicePartners" to listOf(uid),
                        "count" to 1
                    )
                ).await()
            }

            // Step 2: Update subcollection for locality
            val localityDocRef = cityDocRef
                .collection("subLocations")
                .document(normalizedLocality)

            val localityDoc = localityDocRef.get().await()

            if (localityDoc.exists()) {
                // Locality exists - add UID to array and increment count
                localityDocRef.update(
                    mapOf(
                        "servicePartners" to FieldValue.arrayUnion(uid),
                        "count" to FieldValue.increment(1)
                    )
                ).await()
            } else {
                // Locality doesn't exist - create new document
                localityDocRef.set(
                    mapOf(
                        "name" to normalizedLocality,
                        "servicePartners" to listOf(uid),
                        "count" to 1
                    )
                ).await()
            }

            Log.d("AuthRepository", "Successfully added service partner to $normalizedCity > $normalizedLocality")
        } catch (e: Exception) {
            Log.e("AuthRepository", "Failed to add service partner to location", e)
            // Don't throw - user registration succeeded, location update is secondary
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