package com.poojapurohit.auth.compose.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.poojapurohit.R

/**
 * Loading dialogue with three modes:
 *
 * 1. [isRetrying] = false, [onCancel] = null  →  plain spinner (e.g. Firestore writes)
 * 2. [isRetrying] = false, [onCancel] != null →  spinner + cancel button (initial auth attempt)
 * 3. [isRetrying] = true                      →  retry status with progress bar + cancel button
 *
 * @param statusMessage   Optional message shown below the animation.
 * @param isRetrying      True when in the exponential-backoff retry loop.
 * @param retryAttempt    Current retry attempt (1-indexed). Used for progress bar.
 * @param maxRetries      Total allowed retries. Used for progress bar.
 * @param onCancel        If non-null, a "Cancel" button is rendered. Pass [AuthViewModel.cancelSignIn].
 */
@Composable
fun LoadingDialog(
    statusMessage: String? = null,
    isRetrying: Boolean = false,
    retryAttempt: Int = 0,
    maxRetries: Int = 3,
    onCancel: (() -> Unit)? = null
) {
    Dialog(
        onDismissRequest = { /* Prevent accidental dismissal */ },
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = Color(0xFF1C1C1C),
                    shape = RoundedCornerShape(20.dp)
                )
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // ── Lottie Animation ─────────────────────────────────────────
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
                    modifier = Modifier.size(120.dp)
                )

                // ── Status Message ────────────────────────────────────────────
                val displayMessage = when {
                    statusMessage != null -> statusMessage
                    isRetrying -> "Retrying connection…"
                    else -> "Please wait…"
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = displayMessage,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    fontFamily = FontFamily.Serif,
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp,
                    modifier = Modifier.fillMaxWidth()
                )

                // ── Retry Progress Bar ────────────────────────────────────────
                if (isRetrying && retryAttempt > 0) {
                    Spacer(modifier = Modifier.height(12.dp))

                    LinearProgressIndicator(
                        progress = { retryAttempt.toFloat() / maxRetries.toFloat() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp),
                        color = Color(0xFFFFB500),
                        trackColor = Color(0xFF3A3A3A)
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Attempt $retryAttempt of $maxRetries",
                        color = Color(0xFFAAAAAA),
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Serif,
                        textAlign = TextAlign.Center
                    )
                }

                // ── Cancel Button ─────────────────────────────────────────────
                if (onCancel != null) {
                    Spacer(modifier = Modifier.height(16.dp))

                    AuthButton(
                        text = "Cancel",
                        onClick = onCancel,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}
