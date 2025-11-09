package com.poojapurohit.auth.compose.presentation.screens

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import com.poojapurohit.R
import com.poojapurohit.auth.compose.presentation.AuthViewModel
import com.poojapurohit.auth.compose.presentation.components.AuthTitle
import com.poojapurohit.auth.compose.presentation.components.GoogleSignInButton
import com.poojapurohit.auth.compose.presentation.components.ServicePartnerPrompt

@Composable
fun InitialAuthScreen(
    viewModel: AuthViewModel
) {
    val context = LocalContext.current
    val activity = context as? ComponentActivity

    if (activity == null) {
        // Handle error - should not happen in normal flow
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Title at top
        AuthTitle()

        // Push content to center
        Spacer(modifier = Modifier.weight(1f))

        // Google Sign-In Button
        GoogleSignInButton(
            onClick = {
                viewModel.signInWithGoogle(
                    activity = activity,
                    credentialManager = CredentialManager.create(context),
                    clientId = context.getString(R.string.google_client_id),
                    isServicePartner = false
                )
            }
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Service Partner Registration Prompt
        ServicePartnerPrompt(
            onClick = {
                viewModel.signInWithGoogle(
                    activity = activity,
                    credentialManager = CredentialManager.create(context),
                    clientId = context.getString(R.string.google_client_id),
                    isServicePartner = true
                )
            }
        )

        // Bottom spacing
        Spacer(modifier = Modifier.weight(1f))
    }
}