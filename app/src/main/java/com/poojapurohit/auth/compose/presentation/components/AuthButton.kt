package com.poojapurohit.auth.compose.presentation.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun AuthButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier.heightIn(min = 56.dp),  // CHANGED: heightIn instead of height
        colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFF811C01)
        ),
        shape = RoundedCornerShape(50.dp),
        border = BorderStroke(2.dp, Color(0xFFFFB500))
    ) {
        Text(
            text = text,
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Serif,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),  // CHANGED: added vertical padding
            maxLines = 1,  // NEW: prevent text wrapping
            overflow = TextOverflow.Ellipsis  // NEW: truncate if too long
        )
    }
}