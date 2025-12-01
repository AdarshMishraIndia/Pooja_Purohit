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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
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

@Composable
fun ServicePartnerStep2Screen(
    viewModel: AuthViewModel
) {
    var location by remember { mutableStateOf(viewModel.formData.location) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val fontScale = LocalDensity.current.fontScale
    // NEW: Adaptive top spacing
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

            Spacer(modifier = Modifier.height(adaptiveTopSpacing))  // CHANGED: adaptive

            WelcomeText()

            Spacer(modifier = Modifier.height(16.dp))

            AuthTextField(
                label = "Location",
                value = location,
                onValueChange = {
                    location = it
                    viewModel.formData.location = it
                    errorMessage = null
                },
                placeholder = "Enter your location",
                keyboardType = KeyboardType.Text,
                capitalization = KeyboardCapitalization.Words
            )

            Spacer(modifier = Modifier.height(24.dp))

            AuthButton(
                text = "Next",
                onClick = {
                    when {
                        location.isBlank() -> errorMessage = "Please enter your location"
                        location.length < 2 -> errorMessage = "Location must be at least 2 characters"
                        else -> {
                            errorMessage = null
                            viewModel.nextStep(location = location)
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