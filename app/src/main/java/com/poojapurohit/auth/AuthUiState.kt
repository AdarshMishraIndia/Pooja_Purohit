package com.poojapurohit.auth

sealed class AuthUiState {
    object Idle : AuthUiState()
    object Loading : AuthUiState()
    object Success : AuthUiState()
    data class Error(val message: String) : AuthUiState()

    object ShowInitialState : AuthUiState()
    object ShowCustomerFields : AuthUiState()
    object ShowServicePartnerStep1 : AuthUiState()
    object ShowServicePartnerStep2 : AuthUiState()
    data class ShowServicePartnerStep3(val services: List<String>) : AuthUiState()
}
