package com.poojapurohit.bookpurohit.compose.presentation.util

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.graphics.Color
import com.poojapurohit.ui.theme.BrandOrange
import com.poojapurohit.ui.theme.DarkBrandOrange

/**
 * Returns an [AnnotatedString] with all occurrences of [query] highlighted
 * using the brand orange accent. Case-insensitive.
 *
 * Used by LocationSelectionScreen, SubLocationSelectionScreen, PurohitSelectionScreen.
 */
fun highlightSearchQuery(
    text: String,
    query: String,
    isDark: Boolean
): AnnotatedString {
    val normalColor = if (isDark) Color.White else Color.Black
    val highlightColor = if (isDark) DarkBrandOrange else BrandOrange

    if (query.isBlank()) {
        return buildAnnotatedString {
            withStyle(SpanStyle(color = normalColor)) { append(text) }
        }
    }

    return buildAnnotatedString {
        val lower = text.lowercase()
        val lowerQuery = query.lowercase()
        var cursor = 0

        while (cursor < text.length) {
            val match = lower.indexOf(lowerQuery, cursor)
            if (match == -1) {
                withStyle(SpanStyle(color = normalColor)) { append(text.substring(cursor)) }
                break
            }
            if (match > cursor) {
                withStyle(SpanStyle(color = normalColor)) { append(text.substring(cursor, match)) }
            }
            withStyle(
                SpanStyle(
                    color = highlightColor,
                    fontWeight = FontWeight.ExtraBold,
                    background = highlightColor.copy(alpha = if (isDark) 0.2f else 0.15f)
                )
            ) {
                append(text.substring(match, match + query.length))
            }
            cursor = match + query.length
        }
    }
}