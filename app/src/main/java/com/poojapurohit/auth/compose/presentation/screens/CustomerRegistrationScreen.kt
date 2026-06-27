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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.poojapurohit.auth.AuthUiState
import com.poojapurohit.auth.compose.presentation.AuthViewModel
import com.poojapurohit.auth.compose.presentation.components.AuthButton
import com.poojapurohit.auth.compose.presentation.components.AuthTextField
import com.poojapurohit.auth.compose.presentation.components.AuthTitle
import com.poojapurohit.auth.compose.presentation.components.WelcomeText
import kotlinx.coroutines.delay
import kotlin.math.min
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun CustomerRegistrationScreen(viewModel: AuthViewModel) {
    val context = LocalContext.current
    val activity = context as? android.app.Activity

    var name by remember { mutableStateOf(viewModel.formData.name) }
    var phone by remember { mutableStateOf(viewModel.formData.phone) }
    var otp by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val uiState by viewModel.uiState.collectAsState()
    val isPhoneVerified = uiState is AuthUiState.PhoneVerified
    val isOtpSent = uiState is AuthUiState.OtpSent
    val verificationId = (uiState as? AuthUiState.OtpSent)?.verificationId

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

            // Name field
            AuthTextField(
                label = "Name",
                value = name,
                onValueChange = {
                    if (it.all { char -> char.isLetter() || char.isWhitespace() }) {
                        name = it
                        viewModel.formData.name = it
                        errorMessage = null
                    }
                },
                placeholder = "Enter your name",
                keyboardType = KeyboardType.Text,
                capitalization = KeyboardCapitalization.Words
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Phone field
            AuthTextField(
                label = "Phone",
                value = phone,
                onValueChange = {
                    if (it.length <= 10 && it.all { char -> char.isDigit() }) {
                        phone = it
                        viewModel.formData.phone = it
                        errorMessage = null
                    }
                },
                placeholder = "Enter your Phone Number",
                keyboardType = KeyboardType.Phone
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Verify button — appears when phone is 10 digits and not yet verified
            if (phone.length == 10 && !isPhoneVerified) {
                AuthButton(
                    text = if (isOtpSent) "Resend OTP" else "Verify with OTP",
                    onClick = {
                        if (activity != null) {
                            otp = ""
                            viewModel.sendPhoneOtp(phone, activity)
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            // OTP input + confirm — shown only after OTP is sent
            if (isOtpSent && verificationId != null) {
                AuthTextField(
                    label = "OTP",
                    value = otp,
                    onValueChange = {
                        if (it.length <= 6 && it.all { char -> char.isDigit() }) otp = it
                    },
                    placeholder = "Enter 6-digit OTP",
                    keyboardType = KeyboardType.NumberPassword
                )

                Spacer(modifier = Modifier.height(12.dp))

                AuthButton(
                    text = "Confirm OTP",
                    onClick = {
                        when {
                            otp.length != 6 -> errorMessage = "Enter the 6-digit OTP"
                            else -> viewModel.verifyPhoneOtp(verificationId, otp)
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))
            }

            // Verified badge
            if (isPhoneVerified) {
                Text(
                    text = "✅ Phone verified",
                    color = Color(0xFF2E7D32),
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
            }

            // Register button — greyed out until phone is verified
            AuthButton(
                text = "REGISTER",
                onClick = {
                    when {
                        name.isBlank() -> errorMessage = "Please enter your name"
                        name.length < 2 -> errorMessage = "Name must be at least 2 characters"
                        !isPhoneVerified -> errorMessage = "Please verify your phone number first"
                        else -> {
                            errorMessage = null
                            viewModel.registerUser()
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .then(if (!isPhoneVerified) Modifier.graphicsLayer { alpha = 0.4f } else Modifier)
            )

            Spacer(modifier = Modifier.height(24.dp))
        }

        errorMessage?.let { error ->
            ErrorDialog(message = error, onDismiss = { errorMessage = null })
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
                Text(text = "⚠️", fontSize = 40.sp)

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