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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.poojapurohit.auth.compose.presentation.AuthViewModel
import com.poojapurohit.auth.compose.presentation.components.AuthButton
import com.poojapurohit.auth.compose.presentation.components.AuthTextField
import com.poojapurohit.auth.compose.presentation.components.AuthTitle
import com.poojapurohit.auth.compose.presentation.components.ServicesList
import com.poojapurohit.auth.compose.presentation.components.WelcomeText

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
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Experience Field
        AuthTextField(
            label = "Experience",
            value = experience,
            onValueChange = {
                // Only allow digits
                if (it.all { char -> char.isDigit() }) {
                    experience = it
                    viewModel.formData.experience = it
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
                viewModel.registerServicePartner(
                    experience = experience,
                    services = selectedServices.toList()
                )
            },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(24.dp))
    }
}