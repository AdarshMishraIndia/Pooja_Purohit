package com.poojapurohit.dashboard.compose.utils

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.sp

object MarkdownParser {

    fun parseMarkdown(markdown: String): AnnotatedString {
        return buildAnnotatedString {
            val lines = markdown.lines()
            var i = 0

            while (i < lines.size) {
                val line = lines[i]

                when {
                    // H1: # Title
                    line.startsWith("# ") && !line.startsWith("## ") -> {
                        withStyle(SpanStyle(fontSize = 24.sp, fontWeight = FontWeight.Bold)) {
                            append(line.removePrefix("# ").trim())
                        }
                        append("\n\n")
                    }

                    // H2: ## Section
                    line.startsWith("## ") && !line.startsWith("### ") -> {
                        withStyle(SpanStyle(fontSize = 20.sp, fontWeight = FontWeight.Bold)) {
                            append(line.removePrefix("## ").trim())
                        }
                        append("\n\n")
                    }

                    // H3: ### Subsection
                    line.startsWith("### ") -> {
                        withStyle(SpanStyle(fontSize = 18.sp, fontWeight = FontWeight.Bold)) {
                            append(line.removePrefix("### ").trim())
                        }
                        append("\n\n")
                    }

                    // Bold: **text**
                    line.contains("**") -> {
                        parseBoldLine(line)
                        if (i < lines.size - 1 && lines[i + 1].isNotBlank()) {
                            append("\n")
                        } else {
                            append("\n\n")
                        }
                    }

                    // Numbered list: 1. Item
                    line.trim().matches(Regex("^\\d+\\.\\s+.*")) -> {
                        append("  • ")
                        append(line.trim().replaceFirst(Regex("^\\d+\\.\\s+"), ""))
                        append("\n")
                    }

                    // Empty line
                    line.isBlank() -> {
                        append("\n")
                    }

                    // Regular text
                    else -> {
                        append(line)
                        if (i < lines.size - 1 && lines[i + 1].isNotBlank()) {
                            append("\n")
                        } else {
                            append("\n\n")
                        }
                    }
                }

                i++
            }
        }
    }

    private fun AnnotatedString.Builder.parseBoldLine(line: String) {
        var remainingText = line

        while (remainingText.contains("**")) {
            val startIndex = remainingText.indexOf("**")
            val endIndex = remainingText.indexOf("**", startIndex + 2)

            if (endIndex == -1) {
                // No closing **, just append the rest
                append(remainingText)
                break
            }

            // Append text before bold
            append(remainingText.substring(0, startIndex))

            // Append bold text
            withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                append(remainingText.substring(startIndex + 2, endIndex))
            }

            // Continue with remaining text
            remainingText = remainingText.substring(endIndex + 2)
        }

        // Append any remaining text
        if (remainingText.isNotEmpty()) {
            append(remainingText)
        }
    }
}