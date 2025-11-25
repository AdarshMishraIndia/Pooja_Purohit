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

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        // Background Image
        Image(
            painter = painterResource(R.drawable.auth_bg),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // Main Content - Always show the current screen based on state
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
                // Show initial screen so user can retry
                InitialAuthScreen(viewModel = viewModel)
            }
            AuthUiState.Idle -> {
                // Show initial state instead of nothing
                InitialAuthScreen(viewModel = viewModel)
            }
            is AuthUiState.Loading -> {
                // Show previous screen behind the loading dialog
                // Determine which screen to show based on currentStep
                when {
                    viewModel.currentStep == 0 -> InitialAuthScreen(viewModel = viewModel)
                    viewModel.currentStep == 1 && viewModel.isServicePartnerFlow ->
                        ServicePartnerStep1Screen(viewModel = viewModel)
                    viewModel.currentStep == 1 && !viewModel.isServicePartnerFlow ->
                        CustomerRegistrationScreen(viewModel = viewModel)
                    viewModel.currentStep == 2 ->
                        ServicePartnerStep2Screen(viewModel = viewModel)
                    viewModel.currentStep == 3 ->
                        ServicePartnerStep3Screen(viewModel = viewModel, services = emptyList())
                    else -> InitialAuthScreen(viewModel = viewModel)
                }
            }
        }

        // Loading Dialog - Show on top of content
        if (uiState is AuthUiState.Loading) {
            LoadingDialog()
        }
    }

    // Back press handling
    BackHandler(enabled = viewModel.currentStep > 0) {
        viewModel.goBackToPreviousStep()
    }
}