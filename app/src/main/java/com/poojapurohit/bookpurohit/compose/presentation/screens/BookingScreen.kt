package com.poojapurohit.bookpurohit.compose.presentation.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.poojapurohit.bookpurohit.compose.BookingViewModel
import com.poojapurohit.dashboard.compose.theme.BrandOrange
import com.poojapurohit.dashboard.compose.theme.BrandRed
import com.poojapurohit.dashboard.compose.theme.DarkBackgroundGradientEnd
import com.poojapurohit.dashboard.compose.theme.DarkBackgroundGradientStart
import com.poojapurohit.dashboard.compose.theme.DarkBrandOrange
import com.poojapurohit.dashboard.compose.theme.DarkBrandRed
import com.poojapurohit.dashboard.compose.theme.DarkSurface
import com.poojapurohit.dashboard.compose.theme.LightBackgroundGradientEnd
import com.poojapurohit.dashboard.compose.theme.LightBackgroundGradientStart
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookingScreen(
    purohitId: String,
    onBackPressed: () -> Unit,
    onBookingSuccess: () -> Unit,
    viewModel: BookingViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                val savedState = createSavedStateHandle()
                // purohitId injected into SavedStateHandle via nav backstack entry
                BookingViewModel(savedState)
            }
        }
    )

) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val isDark = isSystemInDarkTheme()
    var expanded by remember { mutableStateOf(false) }

    // Dialogue States
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    // Temporary state to hold the date while the user selects the time
    var tempSelectedDateMillis by remember { mutableStateOf<Long?>(null) }

    // Display string for the UI field
    val formattedDateDisplay = remember(uiState.scheduledDateMillis) {
        uiState.scheduledDateMillis?.let {
            SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(it))
        } ?: ""
    }

    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearError()
        }
    }

    LaunchedEffect(uiState.bookingComplete) {
        if (uiState.bookingComplete) {
            Toast.makeText(context, "Booking recorded successfully!", Toast.LENGTH_LONG).show()
            onBookingSuccess()
        }
    }

    Scaffold(
        topBar = {
            BookingTopBar(onBackPressed = onBackPressed, isDark = isDark)
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.linearGradient(
                        colors = if (isDark) listOf(DarkBackgroundGradientStart, DarkBackgroundGradientEnd)
                        else listOf(LightBackgroundGradientStart, LightBackgroundGradientEnd),
                        start = Offset.Zero, end = Offset.Infinite
                    )
                )
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Booking Details",
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp,
                    color = if (isDark) Color.White else Color.Black
                )

                // Service Selection Dropdown
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedTextField(
                        value = uiState.selectedService,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Select Service") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(
                                type = ExposedDropdownMenuAnchorType.PrimaryNotEditable,
                                enabled = true
                            ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = if (isDark) DarkBrandOrange else BrandOrange
                        )
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        viewModel.availableServices.forEach { selectionOption ->
                            DropdownMenuItem(
                                text = { Text(selectionOption) },
                                onClick = {
                                    viewModel.onServiceChange(selectionOption)
                                    expanded = false
                                }
                            )
                        }
                    }
                }

                // Date & Time Picker Field
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = formattedDateDisplay,
                        onValueChange = { },
                        label = { Text("Date & Time") },
                        readOnly = true,
                        trailingIcon = {
                            Icon(
                                imageVector = Icons.Default.CalendarMonth,
                                contentDescription = "Select Date and Time",
                                tint = if (isDark) DarkBrandOrange else BrandOrange
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = if (isDark) DarkBrandOrange else BrandOrange
                        )
                    )
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .background(Color.Transparent)
                            .clickable { showDatePicker = true }
                    )
                }

                OutlinedTextField(
                    value = uiState.address,
                    onValueChange = viewModel::onAddressChange,
                    label = { Text("Complete Address") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = if (isDark) DarkBrandOrange else BrandOrange
                    )
                )

                Spacer(modifier = Modifier.weight(1f))

                // Pay Now Button
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isDark) DarkBrandOrange else BrandOrange)
                        .clickable { viewModel.showPaymentDialog() }
                        .padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (uiState.isLoading) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                    } else {
                        Text(
                            text = "Proceed to Pay ₹1500",
                            fontFamily = FontFamily.Serif,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = Color.White
                        )
                    }
                }
            }

            // 1. Date Picker Dialogue with validation
            if (showDatePicker) {
                val datePickerState = rememberDatePickerState(
                    selectableDates = object : SelectableDates {
                        override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                            // Prevent back-date selection (today or future only)
                            return utcTimeMillis >= System.currentTimeMillis() - 86400000
                        }
                    }
                )
                DatePickerDialog(
                    onDismissRequest = { showDatePicker = false },
                    confirmButton = {
                        TextButton(onClick = {
                            tempSelectedDateMillis = datePickerState.selectedDateMillis
                            showDatePicker = false
                            if (tempSelectedDateMillis != null) {
                                showTimePicker = true
                            }
                        }) {
                            Text("Next", color = if (isDark) DarkBrandOrange else BrandOrange)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDatePicker = false }) {
                            Text("Cancel")
                        }
                    }
                ) {
                    DatePicker(state = datePickerState)
                }
            }

            // 2. Time Picker Dialog
            if (showTimePicker) {
                val timePickerState = rememberTimePickerState(
                    initialHour = 10,
                    initialMinute = 0,
                    is24Hour = false
                )

                AlertDialog(
                    onDismissRequest = { showTimePicker = false },
                    title = { Text("Select Time") },
                    text = {
                        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            TimePicker(state = timePickerState)
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = {
                            val calendar = Calendar.getInstance().apply {
                                timeInMillis = tempSelectedDateMillis ?: System.currentTimeMillis()
                                set(Calendar.HOUR_OF_DAY, timePickerState.hour)
                                set(Calendar.MINUTE, timePickerState.minute)
                            }

                            // Send raw milliseconds to ViewModel for logic and Timestamp conversion
                            viewModel.onDateChange(calendar.timeInMillis)
                            showTimePicker = false
                        }) {
                            Text("Confirm", color = if (isDark) DarkBrandOrange else BrandOrange)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showTimePicker = false }) {
                            Text("Cancel")
                        }
                    }
                )
            }

            if (uiState.isPaymentDialogVisible) {
                RazorpayStubDialog(
                    isDark = isDark,
                    onDismiss = viewModel::hidePaymentDialog,
                    onSimulateSuccess = { viewModel.processPaymentStub(purohitId, "PAYMENT_DONE") },
                    onSimulateFailure = { viewModel.processPaymentStub(purohitId, "PENDING_PAYMENT") }
                )
            }
        }
    }
}

@Composable
fun RazorpayStubDialog(
    isDark: Boolean,
    onDismiss: () -> Unit,
    onSimulateSuccess: () -> Unit,
    onSimulateFailure: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Razorpay Payment Stub",
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Text("Simulate the outcome of the payment gateway to test your Firestore triggers and UI state.")
        },
        containerColor = if (isDark) DarkSurface else Color.White,
        confirmButton = {
            Button(
                onClick = onSimulateSuccess,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
            ) {
                Text("Success (PAYMENT_DONE)")
            }
        },
        dismissButton = {
            Column(horizontalAlignment = Alignment.End) {
                Button(
                    onClick = onSimulateFailure,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF44336))
                ) {
                    Text("Fail (PENDING_PAYMENT)")
                }
                Spacer(modifier = Modifier.height(8.dp))
                TextButton(onClick = onDismiss) {
                    Text("Cancel", color = if (isDark) DarkBrandOrange else BrandOrange)
                }
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BookingTopBar(onBackPressed: () -> Unit, isDark: Boolean) {
    Column {
        TopAppBar(
            title = {
                Text(
                    text = "Checkout",
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = Color.White
                )
            },
            navigationIcon = {
                IconButton(onClick = onBackPressed) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
            modifier = Modifier.background(
                brush = Brush.linearGradient(
                    colors = if (isDark) listOf(DarkBrandOrange, DarkBrandRed) else listOf(BrandOrange, BrandRed),
                    start = Offset.Zero, end = Offset.Infinite
                )
            )
        )
    }
}