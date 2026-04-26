package com.poojapurohit.splash

import android.net.Uri
import android.os.Bundle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.poojapurohit.auth.AuthRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException

// Assuming standard HTTP exception structures
class UnauthorizedException : Exception("HTTP 401 Unauthorized")

sealed class SplashState {
    data object Loading : SplashState()
    data class Navigate(
        val destination: Destination,
        val deepLink: Uri? = null,
        val extras: Bundle? = null
    ) : SplashState()
    data class Error(val message: String, val deepLink: Uri?, val extras: Bundle?) : SplashState()
}

enum class Destination { AUTH, DASHBOARD }

class SplashViewModel(
    private val authRepository: AuthRepository = AuthRepository()
) : ViewModel() {

    private val _splashState = MutableStateFlow<SplashState>(SplashState.Loading)
    val splashState: StateFlow<SplashState> = _splashState.asStateFlow()

    /**
     * Initiates the authentication check.
     * Deep links and extras are passed here to survive the configuration changes
     * and be handed back during the navigation phase.
     */
    fun checkAuthStatus(intentData: Uri?, extras: Bundle?) {
        _splashState.value = SplashState.Loading

        viewModelScope.launch {
            try {
                // Offload IO operations to background thread
                val isRegistered = withContext(Dispatchers.IO) {
                    performAuthCheckWithRefresh()
                }

                val destination = if (isRegistered) Destination.DASHBOARD else Destination.AUTH
                _splashState.value = SplashState.Navigate(destination, intentData, extras)

            } catch (_: IOException) {
                // Network/Server unreachable error
                _splashState.value = SplashState.Error(
                    message = "Server unreachable. Please check your connection.",
                    deepLink = intentData,
                    extras = extras
                )
            } catch (_: Exception) {
                // Graceful failover to Auth screen for unknown errors
                // instead of crashing or showing a blank screen forever
                _splashState.value = SplashState.Navigate(Destination.AUTH, intentData, extras)
            }
        }
    }

    /**
     * Executes the auth check with a built-in silent token refresh mechanism.
     */
    private suspend fun performAuthCheckWithRefresh(): Boolean {
        return try {
            authRepository.isUserRegistered()
        } catch (_: UnauthorizedException) {
            // 401 Received -> Attempt silent refresh
            val isTokenRefreshed = authRepository.refreshToken()
            if (isTokenRefreshed) {
                // Retry original request if refresh succeeded
                authRepository.isUserRegistered()
            } else {
                // Refresh failed, user must log in again
                false
            }
        }
    }
}