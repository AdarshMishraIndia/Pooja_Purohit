package com.poojapurohit.auth.compose.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.poojapurohit.auth.compose.presentation.AuthViewModel
import com.poojapurohit.auth.compose.presentation.components.AuthButton
import com.poojapurohit.auth.compose.presentation.components.AuthTextField
import com.poojapurohit.auth.compose.presentation.components.AuthTitle
import com.poojapurohit.auth.compose.presentation.components.ServicesList
import com.poojapurohit.auth.compose.presentation.components.WelcomeText
import kotlinx.coroutines.delay

@Composable
fun ServicePartnerStep3Screen(
    viewModel: AuthViewModel,
    services: List<String>
) {
    var selectedServices by remember {
        mutableStateOf(viewModel.formData.services.toSet())
    }
    var experience by remember {
        mutableStateOf(viewModel.formData.experience)
    }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
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

            // Specialization Label
            Text(
                text = "Specialization",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Serif
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Services List
            ServicesList(
                services = services,
                selectedServices = selectedServices,
                onSelectionChange = { service, isSelected ->
                    selectedServices = if (isSelected) {
                        selectedServices + service
                    } else {
                        selectedServices - service
                    }
                    viewModel.formData.services = selectedServices.toList()
                    errorMessage = null  // Clear error on selection change
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Experience Field
            AuthTextField(
                label = "Experience",
                value = experience,
                onValueChange = {
                    // Only allow digits and max 3 digits (0-100 years)
                    if (it.isEmpty() || (it.all { char -> char.isDigit() } && it.length <= 3 && (it.toIntOrNull() ?: 0) <= 100)) {
                        experience = it
                        viewModel.formData.experience = it
                        errorMessage = null  // Clear error on input
                    }
                },
                placeholder = "Enter your experience (in years)",
                keyboardType = KeyboardType.Number
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Register Button
            AuthButton(
                text = "REGISTER",
                onClick = {
                    // Validate locally first
                    when {
                        selectedServices.isEmpty() -> errorMessage = "Please select at least one service"
                        experience.isBlank() -> errorMessage = "Please enter your years of experience"
                        experience.toIntOrNull() == null -> errorMessage = "Please enter a valid number for experience"
                        experience.toInt() < 0 -> errorMessage = "Experience cannot be negative"
                        experience.toInt() > 100 -> errorMessage = "Experience cannot exceed 100 years"
                        else -> {
                            errorMessage = null
                            viewModel.registerServicePartner(
                                experience = experience,
                                services = selectedServices.toList()
                            )
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(24.dp))
        }

        // Error Dialog Overlay
        errorMessage?.let { error ->
            ErrorDialog(
                message = error,
                onDismiss = { errorMessage = null }
            )
        }
    }
}

@Composable
private fun ErrorDialog(
    message: String,
    onDismiss: () -> Unit
) {
    // Auto-dismiss after 3 seconds
    LaunchedEffect(message) {
        delay(3000)
        onDismiss()
    }

    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = Color(0xFFFFEBEE),
                    shape = RoundedCornerShape(16.dp)
                )
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Error icon
                Text(
                    text = "⚠️",
                    fontSize = 40.sp
                )

                // Error message
                Text(
                    text = message,
                    color = Color(0xFFB71C1C),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Serif,
                    textAlign = TextAlign.Center,
                    lineHeight = 22.sp
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Dismiss button
                AuthButton(
                    text = "OK",
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}