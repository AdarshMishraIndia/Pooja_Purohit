package com.poojapurohit.bookpurohit.compose.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot

data class PurohitItem(
    val id: String = "",
    val name: String = "",
    val phone: String = "",
    val city: String = "",
    val locality: String = "",
    val email: String = "",
    val experience: Int = 0,
    val proficiency: List<String> = emptyList(),
    val createdAt: Timestamp? = null
) {
    companion object {

        fun fromDocument(doc: DocumentSnapshot): PurohitItem {
            return PurohitItem(
                id = doc.id,
                name = doc.getString("name") ?: "",
                phone = doc.getString("phone") ?: "",
                city = doc.getString("city") ?: "",
                locality = doc.getString("locality") ?: "",
                email = doc.getString("email") ?: "",
                experience = parseExperience(doc.get("experience")),
                proficiency = doc.get("proficiency") as? List<String> ?: emptyList(),
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