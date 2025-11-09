package com.poojapurohit.auth.compose.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.airbnb.lottie.compose.*
import com.poojapurohit.R

@Composable
fun LoadingDialog() {
    Dialog(
        onDismissRequest = { /* Prevent dismissal */ },
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false
        )
    ) {
        Box(
            modifier = Modifier
                .size(200.dp)
                .background(Color.Transparent, RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center
        ) {
            // Option 1: Use Lottie Animation (if you have the animation file)
            val composition by rememberLottieComposition(
                LottieCompositionSpec.RawRes(R.raw.loading_animation)
            )
            val progress by animateLottieCompositionAsState(
                composition = composition,
                iterations = LottieConstants.IterateForever
            )

            LottieAnimation(
                composition = composition,
                progress = { progress },
                modifier = Modifier.size(200.dp)
            )

            // Option 2: Fallback to CircularProgressIndicator if Lottie is not available
            // Uncomment below if you want to use this instead:
            /*
            CircularProgressIndicator(
                modifier = Modifier.size(100.dp),
                color = Color(0xFFFFB500),
                strokeWidth = 6.dp
            )
            */
        }
    }
}