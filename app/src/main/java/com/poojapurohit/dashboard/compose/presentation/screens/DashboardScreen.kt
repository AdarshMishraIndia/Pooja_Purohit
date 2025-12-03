package com.poojapurohit.dashboard.compose.presentation.screens

import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.poojapurohit.auth.compose.AuthActivity
import com.poojapurohit.dashboard.EditProfileActivity
import com.poojapurohit.dashboard.InfoActivity
import com.poojapurohit.dashboard.compose.DashboardEffect
import com.poojapurohit.dashboard.compose.DashboardEvent
import com.poojapurohit.dashboard.compose.DashboardViewModel
import com.poojapurohit.dashboard.compose.presentation.components.ContactSection
import com.poojapurohit.dashboard.compose.presentation.components.DashboardTopBar
import com.poojapurohit.dashboard.compose.presentation.components.NavigationDrawerContent
import com.poojapurohit.dashboard.compose.presentation.components.ServiceCard
import com.poojapurohit.dashboard.compose.theme.BrandOrange
import com.poojapurohit.dashboard.compose.theme.DarkBackgroundGradientCenter
import com.poojapurohit.dashboard.compose.theme.DarkBackgroundGradientEnd
import com.poojapurohit.dashboard.compose.theme.DarkBackgroundGradientStart
import com.poojapurohit.dashboard.compose.theme.DarkBrandOrange
import com.poojapurohit.dashboard.compose.theme.DeleteRed
import com.poojapurohit.dashboard.compose.theme.LightBackgroundGradientCenter
import com.poojapurohit.dashboard.compose.theme.LightBackgroundGradientEnd
import com.poojapurohit.dashboard.compose.theme.LightBackgroundGradientStart
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val effect by viewModel.effect.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    var showDeleteDialog by remember { mutableStateOf(false) }

    val isDark = isSystemInDarkTheme()

    // Handle side effects
    LaunchedEffect(effect) {
        when (val currentEffect = effect) {
            is DashboardEffect.NavigateToAuth -> {
                context.startActivity(Intent(context, AuthActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                })
                viewModel.clearEffect()
            }
            is DashboardEffect.NavigateToEditAccount -> {
                context.startActivity(Intent(context, EditProfileActivity::class.java))
                viewModel.clearEffect()
            }
            is DashboardEffect.ShowToast -> {
                Toast.makeText(context, currentEffect.message, Toast.LENGTH_SHORT).show()
                viewModel.clearEffect()
            }
            is DashboardEffect.NavigateToInfo -> {
                context.startActivity(Intent(context, InfoActivity::class.java).apply {
                    putExtra("title", currentEffect.title)
                    putExtra("content", currentEffect.content)
                })
                viewModel.clearEffect()
            }
            is DashboardEffect.MakePhoneCall -> {
                val intent = Intent(Intent.ACTION_DIAL).apply {
                    data = "tel:${currentEffect.phoneNumber}".toUri()
                }
                context.startActivity(intent)
                viewModel.clearEffect()
            }
            null -> {}
        }
    }

    // Back press handler
    BackHandler(enabled = drawerState.isOpen) {
        scope.launch { drawerState.close() }
    }

    // Delete confirmation dialog
    if (showDeleteDialog) {
        DeleteAccountDialog(
            onConfirm = {
                showDeleteDialog = false
                viewModel.onEvent(DashboardEvent.DeleteAccountConfirmed)
            },
            onDismiss = { showDeleteDialog = false }
        )
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            NavigationDrawerContent(
                userName = uiState.userName,
                userEmail = uiState.userEmail,
                onEditAccount = { viewModel.onEvent(DashboardEvent.NavigateToEditAccount) },
                onAboutUs = { viewModel.onEvent(DashboardEvent.NavigateToAboutUs) },
                onTermsConditions = { viewModel.onEvent(DashboardEvent.NavigateToTerms, context) }, // Add context here
                onSignOut = { viewModel.onEvent(DashboardEvent.SignOut) },
                onDeleteAccount = { showDeleteDialog = true },
                onClose = { scope.launch { drawerState.close() } }
            )
        },
        gesturesEnabled = true
    ) {
        Scaffold(
            topBar = {
                DashboardTopBar(
                    onMenuClick = { scope.launch { drawerState.open() } }
                )
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.linearGradient(
                            colors = if (isDark) {
                                listOf(
                                    DarkBackgroundGradientStart,
                                    DarkBackgroundGradientCenter,
                                    DarkBackgroundGradientEnd
                                )
                            } else {
                                listOf(
                                    LightBackgroundGradientStart,
                                    LightBackgroundGradientCenter,
                                    LightBackgroundGradientEnd
                                )
                            },
                            start = Offset.Zero,
                            end = Offset.Infinite
                        )
                    )
                    .padding(paddingValues)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentPadding = PaddingValues(
                            horizontal = 8.dp,
                            vertical = 8.dp
                        )
                    ) {
                        items(uiState.services) { service ->
                            ServiceCard(
                                service = service,
                                onClick = { viewModel.onEvent(DashboardEvent.ServiceClicked(service)) }
                            )
                        }
                    }

                    // Elegant divider with phone icon
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        HorizontalDivider(
                            modifier = Modifier.weight(1f),
                            thickness = 1.dp,
                            color = if (isDark) {
                                DarkBrandOrange.copy(alpha = 0.5f)
                            } else {
                                BrandOrange.copy(alpha = 0.5f)
                            }
                        )

                        Icon(
                            imageVector = Icons.Default.Phone,
                            contentDescription = "Contact",
                            modifier = Modifier
                                .padding(horizontal = 12.dp)
                                .size(24.dp),
                            tint = if (isDark) DarkBrandOrange else BrandOrange
                        )

                        HorizontalDivider(
                            modifier = Modifier.weight(1f),
                            thickness = 1.dp,
                            color = if (isDark) {
                                DarkBrandOrange.copy(alpha = 0.5f)
                            } else {
                                BrandOrange.copy(alpha = 0.5f)
                            }
                        )
                    }

                    ContactSection(
                        onCallClick = { viewModel.onEvent(DashboardEvent.CallContact) }
                    )
                }
            }
        }
    }
}

@Composable
private fun DeleteAccountDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete Account") },
        text = {
            Text("Are you sure you want to permanently delete your account? This action cannot be undone.")
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Delete", color = DeleteRed)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}