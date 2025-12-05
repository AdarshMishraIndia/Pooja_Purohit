package com.poojapurohit.auth

class AuthFormValidator {

    fun validateNameAndPhone(name: String, phone: String): String? {
        if (name.isBlank()) return "Please enter your name"
        if (phone.isBlank()) return "Please enter your phone number"
        if (!phone.all { it.isDigit() }) return "Phone number must contain only digits"
        if (phone.length != 10) return "Phone number must be 10 digits"
        return null
    }

    fun validateCityAndLocality(city: String, locality: String): String? {
        if (city.isBlank()) return "Please enter your city"
        if (locality.isBlank()) return "Please enter your locality"
        return null
    }

    fun validateExperience(experience: String): String? {
        if (experience.isBlank()) return "Please enter your experience"
        if (!experience.all { it.isDigit() }) return "Experience must be a number"
        val years = experience.toIntOrNull()
        if (years == null || years < 0 || years > 100) {
            return "Experience must be between 0 and 100 years"
        }
        return null
    }

    fun validateServices(services: List<String>): String? {
        if (services.isEmpty()) return "Please select at least one service"
        return null
    }
}