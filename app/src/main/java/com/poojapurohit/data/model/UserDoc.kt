package com.poojapurohit.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot

/**
 * Firestore: users/{userId}  — customer profile. Document ID == Firebase Auth UID.
 *
 * Distinct from UI projections: this type mirrors the stored document only.
 */
data class UserDoc(
    val userId: String = "",
    val name: String = "",
    val phone: String = "",          // stored with "+91" prefix
    val email: String = "",
    val fcmTokens: List<String> = emptyList(),
    val createdAt: Timestamp? = null
) {

    /** Full-document write map. Null timestamps are omitted, never invented. */
    fun toMap(): Map<String, Any> = buildMap<String, Any> {
        put(Fields.USER_ID, userId)
        put(Fields.NAME, name)
        put(Fields.PHONE, phone)
        put(Fields.EMAIL, email)
        put(Fields.FCM_TOKENS, fcmTokens)
        createdAt?.let { put(Fields.CREATED_AT, it) }
    }

    object Fields {
        const val USER_ID = "userId"
        const val NAME = "name"
        const val PHONE = "phone"
        const val EMAIL = "email"
        const val FCM_TOKENS = "fcmTokens"
        const val CREATED_AT = "createdAt"
    }

    companion object {
        const val COLLECTION = "users"

        /** New customer: FCM token is attached by the repository at write time. */
        fun forRegistration(
            userId: String,
            name: String,
            phone: String,
            email: String
        ) = UserDoc(
            userId = userId,
            name = name,
            phone = phone,
            email = email,
            createdAt = Timestamp.now()
        )

        fun fromDocument(doc: DocumentSnapshot) = UserDoc(
            userId = doc.getString(Fields.USER_ID) ?: doc.id,
            name = doc.getString(Fields.NAME) ?: "",
            phone = doc.getString(Fields.PHONE) ?: "",
            email = doc.getString(Fields.EMAIL) ?: "",
            fcmTokens = doc.getStringList(Fields.FCM_TOKENS),
            createdAt = doc.getTimestamp(Fields.CREATED_AT)
        )
    }
}
