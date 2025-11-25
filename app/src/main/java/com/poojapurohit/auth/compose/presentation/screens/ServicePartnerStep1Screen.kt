package com.poojapurohit.auth.compose.presentation.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.poojapurohit.auth.compose.presentation.AuthViewModel
import com.poojapurohit.auth.compose.presentation.components.AuthButton
import com.poojapurohit.auth.compose.presentation.components.AuthTextField
import com.poojapurohit.auth.compose.presentation.components.AuthTitle
import com.poojapurohit.auth.compose.presentation.components.WelcomeText

@Composable
fun ServicePartnerStep1Screen(
    viewModel: AuthViewModel
) {
    var name by remember { mutableStateOf(viewModel.formData.name) }
    var phone by remember { mutableStateOf(viewModel.formData.phone) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

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

        // Error message display
        errorMessage?.let { error ->
            Text(
                text = error,
                color = Color.Red,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Serif,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        // Name Field
        AuthTextField(
            label = "Name",
            value = name,
            onValueChange = {
                // Only allow letters and spaces
                if (it.all { char -> char.isLetter() || char.isWhitespace() }) {
                    name = it
                    viewModel.formData.name = it
                    errorMessage = null  // Clear error on input
                }
            },
            placeholder = "Enter your name",
            keyboardType = KeyboardType.Text,
            capitalization = KeyboardCapitalization.Words
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Phone Field
        AuthTextField(
            label = "Phone",
            value = phone,
            onValueChange = {
                // Only allow digits and max 10 characters
                if (it.length <= 10 && it.all { char -> char.isDigit() }) {
                    phone = it
                    viewModel.formData.phone = it
                    errorMessage = null  // Clear error on input
                }
            },
            placeholder = "Enter your Phone Number",
            keyboardType = KeyboardType.Phone
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Next Button
        AuthButton(
            text = "Next",
            onClick = {
                // Validate locally first
                when {
                    name.isBlank() -> errorMessage = "Please enter your name"
                    name.length < 2 -> errorMessage = "Name must be at least 2 characters"
                    phone.isBlank() -> errorMessage = "Please enter your phone number"
                    phone.length != 10 -> errorMessage = "Phone number must be 10 digits"
                    else -> {
                        errorMessage = null
                        viewModel.nextStep(name = name, phone = phone)
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(24.dp))
    }
}