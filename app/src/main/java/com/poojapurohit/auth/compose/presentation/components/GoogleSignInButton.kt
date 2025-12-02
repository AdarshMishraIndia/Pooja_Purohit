package com.poojapurohit.auth.compose.presentation.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.poojapurohit.R

@Composable
fun GoogleSignInButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier.heightIn(min = 56.dp),
        shape = RoundedCornerShape(50.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF4285F4)
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp,
            pressedElevation = 6.dp
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            // Google Icon - positioned at start
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .size(40.dp)
                    .background(Color.White, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_google),
                    contentDescription = "Google Logo",
                    modifier = Modifier.size(40.dp)
                )
            }

            // Text - centered in remaining space
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Spacer(modifier = Modifier.size(40.dp))
                Text(
                    text = "Sign in with Google",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 22.sp,
                    style = TextStyle(
                        shadow = Shadow(
                            color = Color.Black.copy(alpha = 0.5f),
                            offset = Offset(2f, 2f),
                            blurRadius = 4f
                        )
                    )
                )
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 360)
@Composable
private fun GoogleSignInButtonPreview() {
    Box(modifier = Modifier.padding(16.dp)) {
        GoogleSignInButton(
            onClick = {},
            modifier = Modifier.fillMaxWidth()
        )
    }
}