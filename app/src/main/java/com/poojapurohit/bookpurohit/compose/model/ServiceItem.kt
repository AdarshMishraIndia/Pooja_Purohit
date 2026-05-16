package com.poojapurohit.bookpurohit.compose.model

import com.google.firebase.firestore.DocumentSnapshot

data class ServiceItem(
    val id: String = "",
    val name: String = "",
    val slug: String = "",
    val displayOrder: Int = 0,
    val isActive: Boolean = true
) {
    companion object {
        fun fromDocument(doc: DocumentSnapshot): ServiceItem? {
            val isActive = doc.getBoolean("isActive") ?: true
            if (!isActive) return null
            return ServiceItem(
                id = doc.id,
                name = doc.getString("name") ?: return null,
                slug = doc.getString("slug") ?: doc.id,
                displayOrder = (doc.getLong("displayOrder") ?: 0L).toInt(),
                isActive = isActive
            )
        }
    }
}