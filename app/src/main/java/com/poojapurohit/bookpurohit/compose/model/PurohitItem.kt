package com.poojapurohit.bookpurohit.compose.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot

data class PurohitItem(
    val id: String = "",
    val name: String = "",
    val phone: String = "",          // intentionally excluded from listing UI — revealed only after booking ACCEPTED
    val email: String = "",          // same as phone — not surfaced in selection screens
    val city: String = "",
    val locality: String = "",
    val experience: Int = 0,
    val proficiency: List<String> = emptyList(),
    val isVerified: Boolean = false,
    val isAvailable: Boolean = false,
    val rating: Double = 0.0,
    val totalBookings: Int = 0,
    val trustIndex: Double = 0.0,
    val createdAt: Timestamp? = null
) {
    companion object {
        fun fromDocument(doc: DocumentSnapshot): PurohitItem {
            return PurohitItem(
                id = doc.id,
                name = doc.getString("name") ?: "",
                phone = doc.getString("phone") ?: "",
                email = doc.getString("email") ?: "",
                city = doc.getString("city") ?: "",
                locality = doc.getString("locality") ?: "",
                experience = parseExperience(doc.get("experience")),
                proficiency = (doc.get("proficiency") as? List<*>)
                    ?.filterIsInstance<String>() ?: emptyList(),
                isVerified = doc.getBoolean("isVerified") ?: false,
                isAvailable = doc.getBoolean("isAvailable") ?: false,
                rating = doc.getDouble("rating") ?: 0.0,
                totalBookings = parseExperience(doc.get("totalBookings")), // reuse Int parser
                trustIndex = doc.getDouble("trustIndex") ?: 0.0,
                createdAt = doc.getTimestamp("createdAt")
            )
        }

        private fun parseExperience(value: Any?): Int {
            return when (value) {
                is Long -> value.toInt()
                is Int -> value
                is String -> value.toIntOrNull() ?: 0
                else -> 0
            }
        }
    }
}