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
import androidx.compose.runtime.rememberCoroutineScope
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
import com.poojapurohit.bookpurohit.BookPurohitActivity
import com.poojapurohit.dashboard.EditProfileActivity
import com.poojapurohit.dashboard.compose.DashboardEffect
import com.poojapurohit.dashboard.compose.DashboardEvent
import com.poojapurohit.dashboard.compose.DashboardViewModel
import com.poojapurohit.dashboard.compose.presentation.components.ContactSection
import com.poojapurohit.dashboard.compose.presentation.components.DashboardTopBar
import com.poojapurohit.dashboard.compose.presentation.components.NavigationDrawerContent
import com.poojapurohit.dashboard.compose.presentation.components.ServiceCard
import com.poojapurohit.ui.theme.BrandOrange
import com.poojapurohit.ui.theme.DarkBackgroundGradientCenter
import com.poojapurohit.ui.theme.DarkBackgroundGradientEnd
import com.poojapurohit.ui.theme.DarkBackgroundGradientStart
import com.poojapurohit.ui.theme.DarkBrandOrange
import com.poojapurohit.ui.theme.DeleteRed
import com.poojapurohit.ui.theme.LightBackgroundGradientCenter
import com.poojapurohit.ui.theme.LightBackgroundGradientEnd
import com.poojapurohit.ui.theme.LightBackgroundGradientStart
import com.poojapurohit.notification.compose.NotificationActivity
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

    val isDark = isSystemInDarkTheme()

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
            is DashboardEffect.NavigateToBookPurohit -> {
                context.startActivity(Intent(context, BookPurohitActivity::class.java))
                viewModel.clearEffect()
            }
            is DashboardEffect.NavigateToNotifications -> {
                context.startActivity(Intent(context, NotificationActivity::class.java))
                viewModel.clearEffect()
            }
            is DashboardEffect.ShowToast -> {
                Toast.makeText(context, currentEffect.message, Toast.LENGTH_SHORT).show()
                viewModel.clearEffect()
            }
            is DashboardEffect.OpenUrl -> {
                context.startActivity(Intent(Intent.ACTION_VIEW, currentEffect.url.toUri()))
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

    BackHandler(enabled = drawerState.isOpen) {
        scope.launch { drawerState.close() }
    }

    if (uiState.showDeleteDialog) {
        DeleteAccountDialog(
            onConfirm = { viewModel.onEvent(DashboardEvent.DeleteAccountConfirmed) },
            onDismiss = { viewModel.onEvent(DashboardEvent.DeleteAccountDismissed) }
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
                onTermsConditions = { viewModel.onEvent(DashboardEvent.NavigateToTerms) },
                onPrivacyPolicy = { viewModel.onEvent(DashboardEvent.NavigateToPrivacyPolicy) },
                onSignOut = { viewModel.onEvent(DashboardEvent.SignOut) },
                onDeleteAccount = { viewModel.onEvent(DashboardEvent.DeleteAccountRequested) },
                onClose = { scope.launch { drawerState.close() } }
            )
        },
        gesturesEnabled = true
    ) {
        Scaffold(
            topBar = {
                DashboardTopBar(
                    onMenuClick = { scope.launch { drawerState.open() } },
                    onNotificationsClick = { viewModel.onEvent(DashboardEvent.NavigateToNotifications) },
                    unreadNotificationCount = uiState.unreadNotificationCount
                )
            },
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
                    .padding(top = paddingValues.calculateTopPadding())
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
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
                            color = if (isDark) DarkBrandOrange.copy(alpha = 0.5f)
                            else BrandOrange.copy(alpha = 0.5f)
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
                            color = if (isDark) DarkBrandOrange.copy(alpha = 0.5f)
                            else BrandOrange.copy(alpha = 0.5f)
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