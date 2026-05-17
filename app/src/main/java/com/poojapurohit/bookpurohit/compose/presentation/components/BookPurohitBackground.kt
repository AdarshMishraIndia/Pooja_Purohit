package com.poojapurohit.bookpurohit.compose.presentation.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.cos
import kotlin.math.sin

/**
 * Decorative overlay — placed inside a full-size Box BEFORE the content Column.
 * Renders rotating mandala corner arcs + centered ॐ watermark.
 *
 * @param mandalaColor   Mid-tone accent of the screen's gradient palette.
 * @param rotationDegrees Animated 0f→360f value driving mandala rotation.
 * @param isDark          Controls opacity levels.
 */
@Composable
fun BookPurohitDecorOverlay(
    mandalaColor: Color,
    rotationDegrees: Float,
    isDark: Boolean
) {
    val mandalaAlpha = if (isDark) 0.13f else 0.09f
    val omAlpha = if (isDark) 0.07f else 0.05f

    // OM watermark
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text = "ॐ",
            fontSize = 210.sp,
            color = mandalaColor.copy(alpha = omAlpha),
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Serif
        )
    }

    // Corner mandalas
    Canvas(modifier = Modifier.fillMaxSize()) {
        val baseRadius = size.minDimension * 0.30f
        val corners = listOf(
            Offset(0f, 0f) to 0f,
            Offset(size.width, 0f) to 90f,
            Offset(0f, size.height) to 270f,
            Offset(size.width, size.height) to 180f
        )
        corners.forEach { (corner, angleOffset) ->
            drawCornerMandala(
                center = corner,
                baseRadius = baseRadius,
                rotation = rotationDegrees + angleOffset,
                color = mandalaColor,
                alpha = mandalaAlpha
            )
        }
    }
}

private fun DrawScope.drawCornerMandala(
    center: Offset,
    baseRadius: Float,
    rotation: Float,
    color: Color,
    alpha: Float
) {
    data class Ring(val rf: Float, val petals: Int, val stroke: Float, val af: Float)

    val rings = listOf(
        Ring(0.25f, 6,  2.5f, 1.00f),
        Ring(0.45f, 8,  2.0f, 0.88f),
        Ring(0.65f, 10, 1.5f, 0.75f),
        Ring(0.85f, 12, 1.0f, 0.60f)
    )

    rings.forEach { ring ->
        val radius = baseRadius * ring.rf
        val slot = 360f / ring.petals
        val sweep = slot * 0.48f
        val ra = alpha * ring.af
        val tl = Offset(center.x - radius, center.y - radius)
        val sz = Size(radius * 2, radius * 2)

        for (p in 0 until ring.petals) {
            drawArc(
                color = color,
                startAngle = rotation + p * slot,
                sweepAngle = sweep,
                useCenter = false,
                topLeft = tl,
                size = sz,
                style = Stroke(width = ring.stroke.dp.toPx()),
                alpha = ra
            )
            val dotRad = Math.toRadians(
                (rotation + p * slot + sweep + (slot - sweep) / 2.0)
            )
            drawCircle(
                color = color,
                radius = 1.8f.dp.toPx(),
                center = Offset(
                    center.x + radius * cos(dotRad).toFloat(),
                    center.y + radius * sin(dotRad).toFloat()
                ),
                alpha = ra * 0.8f
            )
        }
    }

    drawCircle(
        color = color, radius = baseRadius * 0.10f, center = center,
        style = Stroke(width = 2f.dp.toPx()), alpha = alpha
    )
    drawCircle(color = color, radius = baseRadius * 0.04f, center = center, alpha = alpha)
}
