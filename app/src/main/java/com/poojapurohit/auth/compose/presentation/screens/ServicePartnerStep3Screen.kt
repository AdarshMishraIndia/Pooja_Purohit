package com.poojapurohit.auth.compose.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
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
import com.poojapurohit.auth.compose.presentation.components.WelcomeText
import kotlinx.coroutines.delay
import kotlin.math.min
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun ServicePartnerStep3Screen(
    viewModel: AuthViewModel,
    services: Map<String, String>
) {
    // 1. Use a SnapshotStateMap to track selections (Key: Slug, Value: Display Name)
    val selectedServices = remember { mutableStateMapOf<String, String>() }

    var experience by remember {
        mutableStateOf(viewModel.formData.experience)
    }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val fontScale = LocalDensity.current.fontScale
    val adaptiveTopSpacing = (200 / min(fontScale, 1.5f)).dp

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.Start
        ) {
            AuthTitle()

            Spacer(modifier = Modifier.height(adaptiveTopSpacing))

            WelcomeText()

            Spacer(modifier = Modifier.height(16.dp))

            // Specialization Label
            Text(
                text = "Specialization",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Serif,
                lineHeight = 24.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 2. Replaced ServicesList with an inline loop to handle the Map correctly
            // Using a standard Column with forEach because the parent is already vertically scrollable
            Column(modifier = Modifier.fillMaxWidth()) {
                services.forEach { (slug, name) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (selectedServices.containsKey(slug)) {
                                    selectedServices.remove(slug)
                                } else {
                                    selectedServices[slug] = name
                                }
                                errorMessage = null
                            }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = selectedServices.containsKey(slug),
                            onCheckedChange = { isChecked ->
                                if (isChecked) {
                                    selectedServices[slug] = name
                                } else {
                                    selectedServices.remove(slug)
                                }
                                errorMessage = null
                            },
                            colors = CheckboxDefaults.colors(
                                checkedColor = Color(0xFFE53935), // Adjust colour to match your theme
                                uncheckedColor = Color.White,
                                checkmarkColor = Color.White
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = name,
                            color = Color.White,
                            fontSize = 16.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Experience Field
            AuthTextField(
                label = "Experience",
                value = experience,
                onValueChange = {
                    if (it.isEmpty() || (it.all { char -> char.isDigit() } && it.length <= 3 && (it.toIntOrNull() ?: 0) <= 100)) {
                        experience = it
                        viewModel.formData.experience = it
                        errorMessage = null
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
                    when {
                        selectedServices.isEmpty() -> errorMessage = "Please select at least one service"
                        experience.isBlank() -> errorMessage = "Please enter your years of experience"
                        experience.toIntOrNull() == null -> errorMessage = "Please enter a valid number for experience"
                        experience.toInt() < 0 -> errorMessage = "Experience cannot be negative"
                        experience.toInt() > 100 -> errorMessage = "Experience cannot exceed 100 years"
                        else -> {
                            errorMessage = null
                            // 3. Updated function call to pass the parameters the ViewModel expects
                            viewModel.registerServicePartner(
                                experience = experience,
                                selectedServices = selectedServices.toMap()
                            )
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(24.dp))
        }

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
    LaunchedEffect(message) {
        delay(3000.milliseconds)
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
                Text(
                    text = "⚠️",
                    fontSize = 40.sp
                )

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

                AuthButton(
                    text = "OK",
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}