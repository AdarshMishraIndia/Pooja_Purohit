package com.poojapurohit.bookpurohit.compose.presentation.screens

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.poojapurohit.booking.BookingsActivity
import com.poojapurohit.bookpurohit.compose.CheckoutViewModel
import com.poojapurohit.bookpurohit.compose.presentation.components.BookPurohitDecorOverlay
import com.poojapurohit.ui.theme.BrandOrange
import com.poojapurohit.ui.theme.BrandRed
import com.poojapurohit.ui.theme.DarkBrandOrange
import com.poojapurohit.ui.theme.DarkBrandRed
import com.poojapurohit.ui.theme.DarkSurface
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import androidx.core.net.toUri

// Deep Maroon / Sacred
private val LightGradTop = Color(0xFFFFFDF8)
private val LightGradBottom = Color(0xFFFFE8CC)
private val DarkGradTop = Color(0xFF0E0E0E)
private val DarkGradBottom = Color(0xFF1C1208)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckoutScreen(
    purohitId: String,
    onBackPressed: () -> Unit,
    onBookingSuccess: () -> Unit,
    viewModel: CheckoutViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val isDark = isSystemInDarkTheme()

    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var showMapPicker by remember { mutableStateOf(false) }
    var tempSelectedDateMillis by remember { mutableStateOf<Long?>(null) }

    val formattedDateDisplay = remember(uiState.scheduledDateMillis) {
        uiState.scheduledDateMillis?.let {
            SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(it))
        } ?: ""
    }

    // Rotation drives overlay symbol drift

    LaunchedEffect(uiState.error) {
        uiState.error?.let { Toast.makeText(context, it, Toast.LENGTH_SHORT).show(); viewModel.clearError() }
    }

    LaunchedEffect(uiState.bookingComplete) {
        if (uiState.bookingComplete) {
            val bookingId = uiState.completedBookingId
            if (bookingId != null) {
                val intent = Intent(
                    Intent.ACTION_VIEW,
                    "poojapurohit://bookings/$bookingId".toUri(),
                    context,
                    BookingsActivity::class.java
                ).apply { flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP }
                context.startActivity(intent)
            } else {
                Toast.makeText(context, "Booking recorded successfully!", Toast.LENGTH_LONG).show()
            }
            onBookingSuccess()
        }
    }

    Scaffold(
        topBar = { CheckoutTopBar(onBackPressed = onBackPressed, isDark = isDark) }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = if (isDark) listOf(DarkGradTop, DarkGradBottom)
                        else listOf(LightGradTop, LightGradBottom)
                    )
                )
                .padding(paddingValues)
        ) {
            BookPurohitDecorOverlay(
                accentColor = if (isDark) Color(0xFFFFD090) else BrandOrange,
                isDark = isDark
            )

            Column(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Booking Details",
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp,
                    color = if (isDark) Color.White else Color.Black
                )

                // Service (read-only — set from Service Selection screen)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = if (isDark) Color.White.copy(alpha = 0.06f) else Color.Black.copy(alpha = 0.04f),
                            shape = RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Service",
                        fontFamily = FontFamily.Serif,
                        fontSize = 12.sp,
                        color = if (isDark) DarkBrandOrange else BrandOrange
                    )
                    Text(
                        text = uiState.selectedService.ifBlank { "—" },
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp,
                        color = if (isDark) Color.White else Color.Black
                    )
                }

                // Date & time
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = formattedDateDisplay, onValueChange = {},
                        label = { Text("Date & Time") }, readOnly = true,
                        trailingIcon = {
                            Icon(Icons.Default.CalendarMonth, "Select Date and Time",
                                tint = if (isDark) DarkBrandOrange else BrandOrange)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = if (isDark) DarkBrandOrange else BrandOrange
                        )
                    )
                    Box(Modifier.matchParentSize().background(Color.Transparent).clickable { showDatePicker = true })
                }

                // Address + map pin
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    OutlinedTextField(
                        value = uiState.address, onValueChange = viewModel::onAddressChange,
                        label = { Text("Complete Address") },
                        modifier = Modifier.weight(1f), minLines = 3,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = if (isDark) DarkBrandOrange else BrandOrange
                        )
                    )
                    val pinSet = uiState.coordinates != null
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (pinSet) Color(0xFF4CAF50) else if (isDark) DarkBrandOrange else BrandOrange)
                            .clickable { showMapPicker = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.LocationOn,
                            if (pinSet) "Location pinned" else "Pin location on map",
                            tint = Color.White, modifier = Modifier.size(28.dp))
                    }
                }

                if (uiState.coordinates != null) {
                    val coords = uiState.coordinates!!
                    Text(
                        "📍 Pinned: ${"%.5f".format(coords.latitude)}, ${"%.5f".format(coords.longitude)}",
                        fontFamily = FontFamily.Serif, fontSize = 12.sp, color = Color(0xFF4CAF50)
                    )
                } else {
                    Text(
                        "⚠ Map pin required — tap the pin button to mark your exact location",
                        fontFamily = FontFamily.Serif, fontSize = 12.sp,
                        color = if (isDark) DarkBrandOrange else BrandRed
                    )
                }

                Spacer(Modifier.weight(1f))

                // Pay button
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
                            text = if (uiState.amount > 0) "Proceed to Pay ₹${"%,d".format(uiState.amount)}"
                            else "Proceed to Pay",
                            fontFamily = FontFamily.Serif,
                            fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color.White)
                    }
                }
            }

            // Date picker
            if (showDatePicker) {
                val datePickerState = rememberDatePickerState(
                    selectableDates = object : SelectableDates {
                        override fun isSelectableDate(utcTimeMillis: Long) =
                            utcTimeMillis >= System.currentTimeMillis() - 86400000L
                    }
                )
                DatePickerDialog(
                    onDismissRequest = { showDatePicker = false },
                    confirmButton = {
                        TextButton(onClick = {
                            tempSelectedDateMillis = datePickerState.selectedDateMillis
                            showDatePicker = false
                            if (tempSelectedDateMillis != null) showTimePicker = true
                        }) { Text("Next", color = if (isDark) DarkBrandOrange else BrandOrange) }
                    },
                    dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Cancel") } }
                ) { DatePicker(state = datePickerState) }
            }

            // Time picker
            if (showTimePicker) {
                val timePickerState = rememberTimePickerState(initialHour = 10, initialMinute = 0, is24Hour = false)
                AlertDialog(
                    onDismissRequest = { showTimePicker = false },
                    title = { Text("Select Time") },
                    text = {
                        Box(Modifier.fillMaxWidth(), Alignment.Center) { TimePicker(state = timePickerState) }
                    },
                    confirmButton = {
                        TextButton(onClick = {
                            val calendar = Calendar.getInstance().apply {
                                timeInMillis = tempSelectedDateMillis ?: System.currentTimeMillis()
                                set(Calendar.HOUR_OF_DAY, timePickerState.hour)
                                set(Calendar.MINUTE, timePickerState.minute)
                            }
                            viewModel.onDateChange(calendar.timeInMillis)
                            showTimePicker = false
                        }) { Text("Confirm", color = if (isDark) DarkBrandOrange else BrandOrange) }
                    },
                    dismissButton = { TextButton(onClick = { showTimePicker = false }) { Text("Cancel") } }
                )
            }

            // Map pin picker
            if (showMapPicker) {
                MapPinPickerScreen(
                    initialPin = uiState.coordinates, isDark = isDark,
                    onConfirm = { latLng -> viewModel.onCoordinatesSelected(latLng); showMapPicker = false },
                    onDismiss = { showMapPicker = false }
                )
            }

            // Razorpay stub dialog
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
        title = { Text("Razorpay Payment Stub", fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold) },
        text = { Text("Simulate the outcome of the payment gateway to test your Firestore triggers and UI state.") },
        containerColor = if (isDark) DarkSurface else Color.White,
        confirmButton = {
            Button(onClick = onSimulateSuccess,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))) {
                Text("Success (PAYMENT_DONE)")
            }
        },
        dismissButton = {
            Column(horizontalAlignment = Alignment.End) {
                Button(onClick = onSimulateFailure,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF44336))) {
                    Text("Fail (PENDING_PAYMENT)")
                }
                Spacer(Modifier.height(8.dp))
                TextButton(onClick = onDismiss) {
                    Text("Cancel", color = if (isDark) DarkBrandOrange else BrandOrange)
                }
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CheckoutTopBar(onBackPressed: () -> Unit, isDark: Boolean) {
    TopAppBar(
        title = {
            Text("Checkout", fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold,
                fontSize = 20.sp, color = Color.White)
        },
        navigationIcon = {
            IconButton(onClick = onBackPressed) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White)
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