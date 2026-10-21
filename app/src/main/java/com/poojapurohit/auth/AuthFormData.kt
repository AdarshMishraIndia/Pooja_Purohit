package com.poojapurohit.auth

import com.poojapurohit.data.model.PurohitDoc
import com.poojapurohit.data.model.UserDoc

data class AuthFormData(
    var name: String = "",
    var phone: String = "",
    var city: String = "",
    var locality: String = "",
    var services: List<String> = emptyList(),
    var experience: String = ""
) {
    fun getFormattedPhone(): String =
        if (!phone.startsWith("+91")) "+91$phone" else phone

    fun getFullLocation(): String =
        if (locality.isNotBlank()) "$locality, $city" else city

    fun toUserDoc(userId: String, email: String): UserDoc =
        UserDoc.forRegistration(
            userId = userId,
            name = name,
            phone = getFormattedPhone(),
            email = email
        )

    fun toPurohitDoc(
        purohitId: String,
        email: String,
        serviceIds: List<String>,
        proficiency: List<String>
    ): PurohitDoc =
        PurohitDoc.forRegistration(
            purohitId = purohitId,
            name = name,
            phone = getFormattedPhone(),
            email = email,
            city = city,
            locality = locality,
            experience = experience.toIntOrNull() ?: 0,
            proficiency = proficiency,
            serviceIds = serviceIds
        )
}