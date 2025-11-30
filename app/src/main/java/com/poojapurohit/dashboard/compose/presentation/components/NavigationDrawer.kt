package com.poojapurohit.dashboard.compose.presentation.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.poojapurohit.R
import com.poojapurohit.dashboard.compose.theme.*

@Composable
fun NavigationDrawerContent(
    userName: String,
    userEmail: String,
    onEditAccount: () -> Unit,
    onAboutUs: () -> Unit,
    onTermsConditions: () -> Unit,
    onSignOut: () -> Unit,
    onDeleteAccount: () -> Unit,
    onClose: () -> Unit
) {
    val isDark = isSystemInDarkTheme()

    ModalDrawerSheet(
        modifier = Modifier.width(280.dp),
        drawerContainerColor = if (isDark) DarkSurface else LightBackground
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
            Image(
                painter = painterResource(id = R.drawable.ic_user_profile),
                contentDescription = "Profile Picture",
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .padding(top = 8.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = userName,
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = Color.White
            )

            Text(
                text = userEmail,
                fontFamily = FontFamily.Serif,
                fontSize = 14.sp,
                color = if (isDark) DarkBrandOrange else BrandOrange
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