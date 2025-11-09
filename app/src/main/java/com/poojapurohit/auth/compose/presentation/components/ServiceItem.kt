package com.poojapurohit.auth.compose.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ServiceItem(
    service: String,
    isSelected: Boolean,
    onSelectionChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.White, RoundedCornerShape(4.dp))
            .clickable { onSelectionChange(!isSelected) }
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Service name
        Text(
            text = service,
            fontSize = 16.sp,
            color = Color.Black,
            modifier = Modifier.weight(1f)
        )

        // Selection indicator
        if (isSelected) {
            RadioTick()
        } else {
            RadioUntick()
        }
    }
}

@Composable
private fun RadioTick() {
    Box(
        modifier = Modifier
            .size(25.dp)
            .background(Color(0xFF31832A), CircleShape) // solid green fill
            .border(2.dp, Color(0xFF3A5B3A), CircleShape), // darker green stroke
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Filled.Check,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(16.dp)
        )
    }
}

@Composable
private fun RadioUntick() {
    Box(
        modifier = Modifier
            .size(25.dp)
            .background(Color.Transparent, CircleShape)
            .border(3.dp, Color(0xFFBFBDBD), CircleShape)
    )
}
