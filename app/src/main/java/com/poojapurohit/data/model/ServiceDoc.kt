package com.poojapurohit.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot

/**
 * Firestore: services/{slug} — service catalogue entry. Document ID == slug.
 *
 * Read-only from the Android app; written exclusively by the Admin Portal
 * (hence no toMap()). Server-stamped timestamps; price is optional.
 */
data class ServiceDoc(
    val id: String = "",             // document ID (== slug); not a stored field
    val name: String = "",
    val slug: String = "",
    val price: Int? = null,          // INR; null = not set by admin
    val displayOrder: Int = 0,
    val isActive: Boolean = true,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
) {

    object Fields {
        const val NAME = "name"
        const val SLUG = "slug"
        const val PRICE = "price"
        const val DISPLAY_ORDER = "displayOrder"
        const val IS_ACTIVE = "isActive"
        const val CREATED_AT = "createdAt"
        const val UPDATED_AT = "updatedAt"
    }

    companion object {
        const val COLLECTION = "services"

        fun fromDocument(doc: DocumentSnapshot) = ServiceDoc(
            id = doc.id,
            name = doc.getString(Fields.NAME) ?: "",
            slug = doc.getString(Fields.SLUG) ?: doc.id,
            price = doc.getLong(Fields.PRICE)?.toInt(),
            displayOrder = (doc.getLong(Fields.DISPLAY_ORDER) ?: 0L).toInt(),
            isActive = doc.getBoolean(Fields.IS_ACTIVE) ?: true,
            createdAt = doc.getTimestamp(Fields.CREATED_AT),
            updatedAt = doc.getTimestamp(Fields.UPDATED_AT)
        )
    }
}
