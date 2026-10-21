package com.poojapurohit.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot

/**
 * Firestore: purohits/{purohitId} — service provider profile. Document ID == Firebase Auth UID.
 *
 * Writers: Android app (registration, profile edit), Admin Portal (verification, profile edit).
 * The Admin Portal may DELETE phone/city/locality/experience, so every field must tolerate absence.
 */
data class PurohitDoc(
    val purohitId: String = "",
    val name: String = "",
    val phone: String = "",              // stored with "+91" prefix
    val email: String = "",
    val city: String = "",
    val locality: String = "",
    val experience: Int = 0,             // years
    val proficiency: List<String> = emptyList(),   // human-readable service names
    val serviceIds: List<String> = emptyList(),    // services/{slug} document IDs
    val fcmTokens: List<String> = emptyList(),
    val isVerified: Boolean = false,
    val isAvailable: Boolean = false,
    val rating: Double = 0.0,
    val totalBookings: Int = 0,
    /** Not in the reference schema; read by Admin Portal + app. Never written at registration. */
    val trustIndex: Double? = null,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
) {

    /** Full-document write map. Null optionals/timestamps are omitted, never invented. */
    fun toMap(): Map<String, Any> = buildMap<String, Any> {
        put(Fields.PUROHIT_ID, purohitId)
        put(Fields.NAME, name)
        put(Fields.PHONE, phone)
        put(Fields.EMAIL, email)
        put(Fields.CITY, city)
        put(Fields.LOCALITY, locality)
        put(Fields.EXPERIENCE, experience)
        put(Fields.PROFICIENCY, proficiency)
        put(Fields.SERVICE_IDS, serviceIds)
        put(Fields.FCM_TOKENS, fcmTokens)
        put(Fields.IS_VERIFIED, isVerified)
        put(Fields.IS_AVAILABLE, isAvailable)
        put(Fields.RATING, rating)
        put(Fields.TOTAL_BOOKINGS, totalBookings)
        trustIndex?.let { put(Fields.TRUST_INDEX, it) }
        createdAt?.let { put(Fields.CREATED_AT, it) }
        updatedAt?.let { put(Fields.UPDATED_AT, it) }
    }

    object Fields {
        const val PUROHIT_ID = "purohitId"
        const val NAME = "name"
        const val PHONE = "phone"
        const val EMAIL = "email"
        const val CITY = "city"
        const val LOCALITY = "locality"
        const val EXPERIENCE = "experience"
        const val PROFICIENCY = "proficiency"
        const val SERVICE_IDS = "serviceIds"
        const val FCM_TOKENS = "fcmTokens"
        const val IS_VERIFIED = "isVerified"
        const val IS_AVAILABLE = "isAvailable"
        const val RATING = "rating"
        const val TOTAL_BOOKINGS = "totalBookings"
        const val TRUST_INDEX = "trustIndex"
        const val CREATED_AT = "createdAt"
        const val UPDATED_AT = "updatedAt"
    }

    companion object {
        const val COLLECTION = "purohits"

        /** New purohit: unverified/unavailable until admin review. FCM token attached by repository. */
        fun forRegistration(
            purohitId: String,
            name: String,
            phone: String,
            email: String,
            city: String,
            locality: String,
            experience: Int,
            proficiency: List<String>,
            serviceIds: List<String>
        ): PurohitDoc {
            val now = Timestamp.now()
            return PurohitDoc(
                purohitId = purohitId,
                name = name,
                phone = phone,
                email = email,
                city = city,
                locality = locality,
                experience = experience,
                proficiency = proficiency,
                serviceIds = serviceIds,
                isVerified = false,
                isAvailable = false,
                rating = 0.0,
                totalBookings = 0,
                createdAt = now,
                updatedAt = now
            )
        }

        fun fromDocument(doc: DocumentSnapshot) = PurohitDoc(
            purohitId = doc.getString(Fields.PUROHIT_ID) ?: doc.id,
            name = doc.getString(Fields.NAME) ?: "",
            phone = doc.getString(Fields.PHONE) ?: "",
            email = doc.getString(Fields.EMAIL) ?: "",
            city = doc.getString(Fields.CITY) ?: "",
            locality = doc.getString(Fields.LOCALITY) ?: "",
            experience = doc.get(Fields.EXPERIENCE).asIntOrZero(),
            proficiency = doc.getStringList(Fields.PROFICIENCY),
            serviceIds = doc.getStringList(Fields.SERVICE_IDS),
            fcmTokens = doc.getStringList(Fields.FCM_TOKENS),
            isVerified = doc.getBoolean(Fields.IS_VERIFIED) ?: false,
            isAvailable = doc.getBoolean(Fields.IS_AVAILABLE) ?: false,
            rating = doc.getDouble(Fields.RATING) ?: 0.0,
            totalBookings = doc.get(Fields.TOTAL_BOOKINGS).asIntOrZero(),
            trustIndex = doc.getDouble(Fields.TRUST_INDEX),
            createdAt = doc.getTimestamp(Fields.CREATED_AT),
            updatedAt = doc.getTimestamp(Fields.UPDATED_AT)
        )
    }
}
