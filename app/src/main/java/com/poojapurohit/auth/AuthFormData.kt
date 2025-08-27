package com.poojapurohit.auth

data class AuthFormData(
    var name: String = "",
    var phone: String = "",
    var location: String = "",
    var services: List<String> = emptyList(),
    var experience: String = ""
) {
    fun getFormattedPhone(): String =
        if (!phone.startsWith("+91")) "+91$phone" else phone

}