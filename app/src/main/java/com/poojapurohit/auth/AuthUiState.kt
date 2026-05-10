package com.poojapurohit.auth

sealed class AuthUiState {
    object Idle : AuthUiState()
    object Loading : AuthUiState()
    object Success : AuthUiState()
    data class Error(val message: String) : AuthUiState()

    /**
     * Shown when network pre-check fails before even attempting auth.
     * Distinct from Error — allows UI to show a "retry" action.
     */
    object NetworkError : AuthUiState()

    /**
     * Shown during auto-retry loop.
     * [attempt] is 1-indexed (1, 2, 3).
     * [maxAttempts] is the configured retry ceiling.
     */
    data class RetryingConnection(
        val attempt: Int,
        val maxAttempts: Int,
        val statusMessage: String
    ) : AuthUiState()

    object ShowInitialState : AuthUiState()
    object ShowCustomerFields : AuthUiState()
    object ShowServicePartnerStep1 : AuthUiState()
    object ShowServicePartnerStep2 : AuthUiState()
    data class ShowServicePartnerStep3(val services: List<String>) : AuthUiState()
}
