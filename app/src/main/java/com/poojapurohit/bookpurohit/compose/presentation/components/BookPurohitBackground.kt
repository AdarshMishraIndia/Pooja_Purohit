package com.poojapurohit.bookpurohit.compose.presentation.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private data class Bubble(
    val xFrac: Float,       // base X as fraction of screen width
    val yFrac: Float,       // base Y as fraction of screen height
    val radius: Float,      // circle radius in dp
    val freqMulti: Float,    // speed multiplier — higher = faster orbit
    val phase: Float,       // X-axis sine phase offset (radians)
    val phaseY: Float,      // Y-axis cosine phase offset (radians)
    val ampXFrac: Float,    // horizontal travel as fraction of screen width
    val ampYFrac: Float,    // vertical travel as fraction of screen height
    val filled: Boolean     // filled circle vs outline ring
)

@Composable
fun BookPurohitDecorOverlay(
    accentColor: Color,
    // kept for API compat — unused here
    isDark: Boolean
) {
    val alpha = if (isDark) 0.35f else 0.45f

    val transition = rememberInfiniteTransition(label = "bubbles")

    // Single time driver — 0→1 over 12 s, loops
    val time by transition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(12000, easing = LinearEasing), RepeatMode.Restart),
        label = "bubbleTime"
    )

    // Slow pulse for gentle opacity breathing
    val pulse by transition.animateFloat(
        initialValue = 0.6f, targetValue = 1.0f,
        animationSpec = infiniteRepeatable(tween(3000, easing = LinearEasing), RepeatMode.Reverse),
        label = "bubblePulse"
    )

    val bubbles = remember {
        listOf(
            Bubble(0.12f, 0.08f, 22f, 1.0f,  0.0f,   0.5f,  0.06f, 0.04f, false),
            Bubble(0.55f, 0.05f, 14f, 1.4f,  1.2f,   2.1f,  0.05f, 0.03f, true),
            Bubble(0.82f, 0.12f, 30f, 0.8f,  2.5f,   0.9f,  0.04f, 0.05f, false),
            Bubble(0.30f, 0.20f, 18f, 1.2f,  0.7f,   1.8f,  0.07f, 0.03f, true),
            Bubble(0.70f, 0.18f, 26f, 0.9f,  3.1f,   0.3f,  0.05f, 0.04f, false),
            Bubble(0.05f, 0.35f, 16f, 1.5f,  1.9f,   2.7f,  0.04f, 0.06f, true),
            Bubble(0.90f, 0.32f, 20f, 1.1f,  0.4f,   1.4f,  0.06f, 0.03f, false),
            Bubble(0.45f, 0.40f, 12f, 1.7f,  2.3f,   0.8f,  0.05f, 0.05f, true),
            Bubble(0.20f, 0.52f, 28f, 0.7f,  1.1f,   3.0f,  0.04f, 0.04f, false),
            Bubble(0.75f, 0.48f, 16f, 1.3f,  2.8f,   1.6f,  0.06f, 0.03f, true),
            Bubble(0.60f, 0.60f, 22f, 1.0f,  0.6f,   2.4f,  0.05f, 0.05f, false),
            Bubble(0.08f, 0.65f, 14f, 1.6f,  3.3f,   0.2f,  0.04f, 0.06f, true),
            Bubble(0.85f, 0.62f, 18f, 0.9f,  1.5f,   1.0f,  0.06f, 0.04f, false),
            Bubble(0.38f, 0.72f, 24f, 1.2f,  2.0f,   2.9f,  0.05f, 0.03f, true),
            Bubble(0.65f, 0.78f, 12f, 1.4f,  0.3f,   1.7f,  0.04f, 0.05f, false),
            Bubble(0.18f, 0.83f, 20f, 1.1f,  2.6f,   0.6f,  0.06f, 0.04f, true),
            Bubble(0.50f, 0.88f, 16f, 0.8f,  1.3f,   2.2f,  0.05f, 0.03f, false),
            Bubble(0.78f, 0.91f, 26f, 1.3f,  3.0f,   1.2f,  0.04f, 0.05f, true),
        )
    }

    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val twoPi = (2.0 * PI).toFloat()

        bubbles.forEach { b ->
            val bx = w * b.xFrac + w * b.ampXFrac * sin(twoPi * time * b.freqMulti + b.phase)
            val by = h * b.yFrac + h * b.ampYFrac * cos(twoPi * time * b.freqMulti * 0.71f + b.phaseY)
            val center = Offset(bx, by)
            val r = b.radius.dp.toPx()
            val a = alpha * pulse

            if (b.filled) {
                drawCircle(color = accentColor, radius = r, center = center, alpha = a * 0.4f)
                drawCircle(
                    color = accentColor, radius = r, center = center,
                    style = Stroke(width = 1.2f.dp.toPx()), alpha = a
                )
            } else {
                drawCircle(
                    color = accentColor, radius = r, center = center,
                    style = Stroke(width = 1.5f.dp.toPx()), alpha = a
                )
                drawCircle(color = accentColor, radius = r * 0.22f, center = center, alpha = a * 0.6f)
            }
        }
    }
}