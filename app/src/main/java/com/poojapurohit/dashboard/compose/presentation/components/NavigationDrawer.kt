package com.poojapurohit.dashboard.compose.presentation.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.google.firebase.auth.FirebaseAuth
import com.poojapurohit.R
import com.poojapurohit.ui.theme.*

@Composable
fun NavigationDrawerContent(
    userName: String,
    userEmail: String,
    onEditAccount: () -> Unit,
    onAboutUs: () -> Unit,
    onTermsConditions: () -> Unit,
    onPrivacyPolicy: () -> Unit,
    onSignOut: () -> Unit,
    onDeleteAccount: () -> Unit,
    onClose: () -> Unit
) {
    val isDark = isSystemInDarkTheme()

    ModalDrawerSheet(
        modifier = Modifier.width(280.dp),
        drawerContainerColor = if (isDark) DarkBackground else LightBackground
    ) {
        // Header
        DrawerHeader(userName = userName, userEmail = userEmail)

        Spacer(modifier = Modifier.height(8.dp))

        // Menu items
        DrawerMenuItem(
            icon = Icons.Default.Edit,
            text = "Edit Account",
            onClick = {
                onEditAccount()
                onClose()
            }
        )

        DrawerMenuItem(
            icon = Icons.Default.Info,
            text = "About Us",
            onClick = {
                onAboutUs()
                onClose()
            }
        )

        DrawerMenuItem(
            icon = Icons.Default.Description,
            text = "Terms & Conditions",
            onClick = {
                onTermsConditions()
                onClose()
            }
        )

        DrawerMenuItem(
            icon = Icons.Default.PrivacyTip,
            text = "Privacy Policy",
            onClick = {
                onPrivacyPolicy()
                onClose()
            }
        )

        DrawerMenuItem(
            icon = Icons.AutoMirrored.Filled.ExitToApp,
            text = "Sign Out",
            onClick = {
                onSignOut()
                onClose()
            }
        )

        DrawerMenuItem(
            icon = Icons.Default.Delete,
            text = "Delete Account",
            onClick = {
                onDeleteAccount()
                onClose()
            },
            textColor = DeleteRed
        )
    }
}

@Composable
private fun DrawerHeader(
    userName: String,
    userEmail: String
) {
    val isDark = isSystemInDarkTheme()
    val photoUrl = FirebaseAuth.getInstance().currentUser?.photoUrl?.toString()

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(176.dp)
            .background(
                brush = Brush.linearGradient(
                    colors = if (isDark) {
                        listOf(DarkBrandOrange, DarkBrandRed)
                    } else {
                        listOf(BrandOrange, BrandRed)
                    },
                    start = Offset.Zero,
                    end = Offset.Infinite
                )
            )
            .padding(16.dp),
        contentAlignment = Alignment.BottomStart
    ) {
        Column {
            // Profile picture with placeholder and border
            Box(
                modifier = Modifier.size(80.dp),
                contentAlignment = Alignment.Center
            ) {
                if (photoUrl != null) {
                    AsyncImage(
                        model = photoUrl,
                        contentDescription = "Profile Picture",
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .border(
                                width = 3.dp,
                                color = Color.White.copy(alpha = 0.8f),
                                shape = CircleShape
                            ),
                        contentScale = ContentScale.Crop,
                        error = painterResource(id = R.drawable.ic_user_profile)
                    )
                } else {
                    // Fallback placeholder
                    Image(
                        painter = painterResource(id = R.drawable.ic_user_profile),
                        contentDescription = "Profile Picture",
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .border(
                                width = 3.dp,
                                color = Color.White.copy(alpha = 0.8f),
                                shape = CircleShape
                            )
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Name with shadow
            Text(
                text = userName,
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = Color.White,
                style = TextStyle(
                    shadow = Shadow(
                        color = Color.Black.copy(alpha = 0.7f),
                        offset = Offset(2f, 2f),
                        blurRadius = 4f
                    )
                )
            )

            // Email with shadow
            Text(
                text = userEmail,
                fontFamily = FontFamily.Serif,
                fontSize = 14.sp,
                color = if (isDark) DarkBrandOrange else BrandOrange,
                style = TextStyle(
                    shadow = Shadow(
                        color = Color.Black.copy(alpha = 0.7f),
                        offset = Offset(1.5f, 1.5f),
                        blurRadius = 3f
                    )
                )
            )
        }
    }
}

@Composable
private fun DrawerMenuItem(
    icon: ImageVector,
    text: String,
    onClick: () -> Unit,
    textColor: Color? = null
) {
    val isDark = isSystemInDarkTheme()
    val defaultTextColor = if (isDark) Color.White else BrandRed

    NavigationDrawerItem(
        icon = {
            Icon(
                imageVector = icon,
                contentDescription = text,
                tint = if (isDark) DarkBrandOrange else BrandOrange
            )
        },
        label = {
            Text(
                text = text,
                fontFamily = FontFamily.Serif,
                color = textColor ?: defaultTextColor
            )
        },
        selected = false,
        onClick = onClick,
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
    )
}