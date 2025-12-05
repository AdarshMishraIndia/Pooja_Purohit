package com.poojapurohit.auth

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
}