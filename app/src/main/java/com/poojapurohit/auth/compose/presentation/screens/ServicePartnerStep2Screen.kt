package com.poojapurohit.auth.compose.presentation.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.poojapurohit.auth.compose.presentation.AuthViewModel
import com.poojapurohit.auth.compose.presentation.components.AuthButton
import com.poojapurohit.auth.compose.presentation.components.AuthTextField
import com.poojapurohit.auth.compose.presentation.components.AuthTitle
import com.poojapurohit.auth.compose.presentation.components.WelcomeText

@Composable
fun ServicePartnerStep2Screen(
    viewModel: AuthViewModel
) {
    var location by remember { mutableStateOf(viewModel.formData.location) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.Start
    ) {
        // Title at top
        AuthTitle()

        Spacer(modifier = Modifier.height(200.dp))

        // Welcome Text
        WelcomeText()

        Spacer(modifier = Modifier.height(16.dp))

        // Location Field
        AuthTextField(
            label = "Location",
            value = location,
            onValueChange = {
                location = it
                viewModel.formData.location = it
            },
            placeholder = "Enter your location",
            keyboardType = KeyboardType.Text,
            capitalization = KeyboardCapitalization.Words
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Next Button
        AuthButton(
            text = "Next",
            onClick = {
                viewModel.nextStep(location = location)
            },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(24.dp))
    }
}