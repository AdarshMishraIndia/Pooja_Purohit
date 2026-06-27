package com.poojapurohit.auth.compose.presentation.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.poojapurohit.R
import com.poojapurohit.auth.AuthUiState
import com.poojapurohit.auth.compose.presentation.AuthViewModel
import com.poojapurohit.auth.compose.presentation.components.LoadingDialog

@Composable
fun AuthScreen(
    viewModel: AuthViewModel = viewModel(),
    onNavigateToDashboard: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    Box(modifier = Modifier.fillMaxSize()) {

        // ── Background ────────────────────────────────────────────────────────
        Image(
            painter = painterResource(R.drawable.auth_bg),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // ── Screen Content ────────────────────────────────────────────────────
        when (val state = uiState) {

            is AuthUiState.ShowInitialState -> {
                InitialAuthScreen(viewModel = viewModel)
            }

            is AuthUiState.ShowCustomerFields -> {
                CustomerRegistrationScreen(viewModel = viewModel)
            }

            is AuthUiState.ShowServicePartnerStep1 -> {
                ServicePartnerStep1Screen(viewModel = viewModel)
            }

            is AuthUiState.ShowServicePartnerStep2 -> {
                ServicePartnerStep2Screen(viewModel = viewModel)
            }

            is AuthUiState.ShowServicePartnerStep3 -> {
                ServicePartnerStep3Screen(
                    viewModel = viewModel,
                    services = state.services
                )
            }

            is AuthUiState.Success -> {
                LaunchedEffect(Unit) {
                    onNavigateToDashboard()
                }
            }

            is AuthUiState.Error -> {
                LaunchedEffect(state.message) {
                    Toast.makeText(context, state.message, Toast.LENGTH_LONG).show()
                }
                InitialAuthScreen(viewModel = viewModel)
            }

            is AuthUiState.NetworkError -> {
                LaunchedEffect(Unit) {
                    Toast.makeText(
                        context,
                        "No internet connection. Please check your network and try again.",
                        Toast.LENGTH_LONG
                    ).show()
                }
                InitialAuthScreen(viewModel = viewModel)
            }

            is AuthUiState.Loading -> {
                // Render the appropriate screen behind the loading overlay
                when (viewModel.currentStep) {
                    0 ->
                        InitialAuthScreen(viewModel = viewModel)

                    1 if viewModel.isServicePartnerFlow ->
                        ServicePartnerStep1Screen(viewModel = viewModel)

                    1 if true ->
                        CustomerRegistrationScreen(viewModel = viewModel)

                    2 ->
                        ServicePartnerStep2Screen(viewModel = viewModel)

                    3 ->
                        ServicePartnerStep3Screen(viewModel = viewModel, services = emptyMap())

                    else -> InitialAuthScreen(viewModel = viewModel)
                }
            }

            is AuthUiState.RetryingConnection -> {
                InitialAuthScreen(viewModel = viewModel)
            }

            // OTP sent — keep the current registration screen visible behind any overlay
            is AuthUiState.OtpSent -> {
                when {
                    viewModel.isServicePartnerFlow -> ServicePartnerStep1Screen(viewModel = viewModel)
                    else -> CustomerRegistrationScreen(viewModel = viewModel)
                }
            }

            // Phone verified — keep the registration screen visible so user can hit Register/Next
            is AuthUiState.PhoneVerified -> {
                when {
                    viewModel.isServicePartnerFlow -> ServicePartnerStep1Screen(viewModel = viewModel)
                    else -> CustomerRegistrationScreen(viewModel = viewModel)
                }
            }

            AuthUiState.Idle -> {
                InitialAuthScreen(viewModel = viewModel)
            }
        }

        // ── Loading / Retry Overlay ───────────────────────────────────────────
        when (val state = uiState) {
            is AuthUiState.Loading -> {
                LoadingDialog(
                    statusMessage = "Signing you in…",
                    isRetrying = false,
                    onCancel = if (viewModel.currentStep == 0) viewModel::cancelSignIn else null
                )
            }

            is AuthUiState.RetryingConnection -> {
                LoadingDialog(
                    statusMessage = state.statusMessage,
                    isRetrying = true,
                    retryAttempt = state.attempt,
                    maxRetries = state.maxAttempts,
                    onCancel = viewModel::cancelSignIn
                )
            }

            else -> { /* No overlay */ }
        }
    }

    // ── Back Handler ──────────────────────────────────────────────────────────
    BackHandler(
        enabled = viewModel.currentStep > 0
                || uiState is AuthUiState.Loading
                || uiState is AuthUiState.RetryingConnection
    ) {
        viewModel.goBackToPreviousStep()
    }
}