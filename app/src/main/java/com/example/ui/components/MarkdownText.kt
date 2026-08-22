package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.LocalGatewayColors

/**
 * Robust, lightweight Markdown parser and renderer for Jetpack Compose.
 * Supports Headers (#, ##, ###), Bold (**), Italic (*), Inline Code (`),
 * Code Blocks (```), Blockquotes (>), Bullet Lists (- / *), Numbered Lists (1.),
 * Horizontal Dividers (---), and Links ([text](url)).
 */
@Composable
fun MarkdownText(
    markdown: String,
    modifier: Modifier = Modifier,
    color: Color = LocalGatewayColors.current.otherBubbleText,
    isUser: Boolean = false
) {
    val blocks = remember(markdown) { parseMarkdownBlocks(markdown) }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        blocks.forEach { block ->
            when (block) {
                is MarkdownBlock.Header -> {
                    val (fontSize, fontWeight) = when (block.level) {
                        1 -> 18.sp to FontWeight.ExtraBold
                        2 -> 16.sp to FontWeight.Bold
                        else -> 14.5.sp to FontWeight.SemiBold
                    }
                    Text(
                        text = buildAnnotatedMarkdown(block.text, color),
                        fontSize = fontSize,
                        fontWeight = fontWeight,
                        color = color,
                        lineHeight = (fontSize.value * 1.3).sp,
                        modifier = Modifier.padding(top = 2.dp, bottom = 2.dp)
                    )
                }
                is MarkdownBlock.CodeBlock -> {
                    MarkdownCodeBlock(
                        language = block.language,
                        code = block.code,
                        isUser = isUser
                    )
                }
                is MarkdownBlock.BlockQuote -> {
                    MarkdownBlockQuote(
                        text = block.text,
                        color = color,
                        isUser = isUser
                    )
                }
                is MarkdownBlock.BulletItem -> {
                    Row(
                        modifier = Modifier.padding(start = 4.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            text = "•",
                            color = color.copy(alpha = 0.85f),
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text(
                            text = buildAnnotatedMarkdown(block.text, color),
                            color = color,
                            fontSize = 14.sp,
                            lineHeight = 19.sp
                        )
                    }
                }
                is MarkdownBlock.NumberedItem -> {
                    Row(
                        modifier = Modifier.padding(start = 4.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            text = "${block.number}.",
                            color = color.copy(alpha = 0.85f),
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.5.sp,
                            modifier = Modifier.padding(end = 6.dp)
                        )
                        Text(
                            text = buildAnnotatedMarkdown(block.text, color),
                            color = color,
                            fontSize = 14.sp,
                            lineHeight = 19.sp
                        )
                    }
                }
                is MarkdownBlock.HorizontalRule -> {
                    HorizontalDivider(
                        color = color.copy(alpha = 0.2f),
                        thickness = 1.dp,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
                is MarkdownBlock.Paragraph -> {
                    Text(
                        text = buildAnnotatedMarkdown(block.text, color),
                        color = color,
                        fontSize = 14.sp,
                        lineHeight = 19.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun MarkdownCodeBlock(
    language: String,
    code: String,
    isUser: Boolean
) {
    val context = LocalContext.current
    val colors = LocalGatewayColors.current
    var copied by remember { mutableStateOf(false) }

    LaunchedEffect(copied) {
        if (copied) {
            kotlinx.coroutines.delay(2000)
            copied = false
        }
    }

    val codeBg = if (isUser) Color.Black.copy(alpha = 0.25f) else colors.frostedGlass.copy(alpha = 0.12f)
    val borderColor = if (isUser) Color.White.copy(alpha = 0.15f) else colors.frostedBorder.copy(alpha = 0.4f)
    val textColor = if (isUser) colors.userBubbleText else colors.otherBubbleText

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(codeBg)
            .border(1.dp, borderColor, RoundedCornerShape(8.dp))
    ) {
        // Code Block Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.15f))
                .padding(horizontal = 10.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = language.ifEmpty { "code" }.lowercase(),
                color = textColor.copy(alpha = 0.7f),
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .clickable {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("Copied Code", code)
                        clipboard.setPrimaryClip(clip)
                        copied = true
                        Toast.makeText(context, "Code copied to clipboard", Toast.LENGTH_SHORT).show()
                    }
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Icon(
                    imageVector = if (copied) Icons.Default.Check else Icons.Default.ContentCopy,
                    contentDescription = "Copy code",
                    tint = if (copied) Color(0xFF4CAF50) else textColor.copy(alpha = 0.7f),
                    modifier = Modifier.size(13.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = if (copied) "Copied" else "Copy",
                    color = if (copied) Color(0xFF4CAF50) else textColor.copy(alpha = 0.7f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // Code Content
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(10.dp)
        ) {
            Text(
                text = code,
                color = textColor,
                fontFamily = FontFamily.Monospace,
                fontSize = 12.5.sp,
                lineHeight = 17.sp
            )
        }
    }
}

@Composable
private fun MarkdownBlockQuote(
    text: String,
    color: Color,
    isUser: Boolean
) {
    val colors = LocalGatewayColors.current
    val barColor = if (isUser) colors.userBubbleText.copy(alpha = 0.6f) else colors.accent
    val bg = if (isUser) Color.Black.copy(alpha = 0.1f) else colors.frostedGlass.copy(alpha = 0.05f)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(4.dp))
            .background(bg)
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        Box(
            modifier = Modifier
                .width(3.dp)
                .fillMaxHeight()
                .background(barColor, RoundedCornerShape(2.dp))
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = buildAnnotatedMarkdown(text, color),
            color = color.copy(alpha = 0.9f),
            fontStyle = FontStyle.Italic,
            fontSize = 13.5.sp,
            lineHeight = 18.sp
        )
    }
}

/**
 * Builds an AnnotatedString parsing inline formatting:
 * - Bold: **text** or __text__
 * - Italic: *text* or _text_
 * - Bold+Italic: ***text***
 * - Inline code: `code`
 * - Links: [text](url)
 */
@Composable
fun buildAnnotatedMarkdown(text: String, baseColor: Color): AnnotatedString {
    val context = LocalContext.current
    return remember(text, baseColor) {
        buildAnnotatedString {
            var i = 0
            val len = text.length

            while (i < len) {
                // Code block or inline code
                if (text[i] == '`') {
                    val endBacktick = text.indexOf('`', i + 1)
                    if (endBacktick != -1) {
                        val codeContent = text.substring(i + 1, endBacktick)
                        pushStyle(
                            SpanStyle(
                                fontFamily = FontFamily.Monospace,
                                background = baseColor.copy(alpha = 0.12f),
                                fontWeight = FontWeight.Medium,
                                fontSize = 13.sp
                            )
                        )
                        append(" $codeContent ")
                        pop()
                        i = endBacktick + 1
                        continue
                    }
                }

                // Bold+Italic: ***text***
                if (i + 2 < len && text.substring(i, i + 3) == "***") {
                    val endTriple = text.indexOf("***", i + 3)
                    if (endTriple != -1) {
                        pushStyle(SpanStyle(fontWeight = FontWeight.Bold, fontStyle = FontStyle.Italic))
                        append(text.substring(i + 3, endTriple))
                        pop()
                        i = endTriple + 3
                        continue
                    }
                }

                // Bold: **text** or __text__
                if (i + 1 < len && (text.substring(i, i + 2) == "**" || text.substring(i, i + 2) == "__")) {
                    val marker = text.substring(i, i + 2)
                    val endMarker = text.indexOf(marker, i + 2)
                    if (endMarker != -1) {
                        pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
                        append(text.substring(i + 2, endMarker))
                        pop()
                        i = endMarker + 2
                        continue
                    }
                }

                // Italic: *text* or _text_ (when not followed by whitespace)
                if ((text[i] == '*' || text[i] == '_') && (i + 1 < len && !text[i + 1].isWhitespace())) {
                    val marker = text[i]
                    val endMarker = text.indexOf(marker, i + 1)
                    if (endMarker != -1 && (endMarker + 1 == len || text[endMarker + 1].isWhitespace() || text[endMarker + 1] in ",.!?;:)")) {
                        pushStyle(SpanStyle(fontStyle = FontStyle.Italic))
                        append(text.substring(i + 1, endMarker))
                        pop()
                        i = endMarker + 1
                        continue
                    }
                }

                // Link: [text](url)
                if (text[i] == '[') {
                    val closeBracket = text.indexOf(']', i + 1)
                    if (closeBracket != -1 && closeBracket + 1 < len && text[closeBracket + 1] == '(') {
                        val closeParen = text.indexOf(')', closeBracket + 2)
                        if (closeParen != -1) {
                            val linkText = text.substring(i + 1, closeBracket)
                            val linkUrl = text.substring(closeBracket + 2, closeParen)
                            pushStyle(
                                SpanStyle(
                                    color = Color(0xFF29B6F6),
                                    textDecoration = TextDecoration.Underline,
                                    fontWeight = FontWeight.Medium
                                )
                            )
                            append(linkText)
                            pop()
                            i = closeParen + 1
                            continue
                        }
                    }
                }

                append(text[i])
                i++
            }
        }
    }
}

private sealed class MarkdownBlock {
    data class Header(val level: Int, val text: String) : MarkdownBlock()
    data class CodeBlock(val language: String, val code: String) : MarkdownBlock()
    data class BlockQuote(val text: String) : MarkdownBlock()
    data class BulletItem(val text: String) : MarkdownBlock()
    data class NumberedItem(val number: String, val text: String) : MarkdownBlock()
    data class Paragraph(val text: String) : MarkdownBlock()
    object HorizontalRule : MarkdownBlock()
}

private fun parseMarkdownBlocks(markdown: String): List<MarkdownBlock> {
    val lines = markdown.lines()
    val blocks = mutableListOf<MarkdownBlock>()
    var i = 0

    while (i < lines.size) {
        val line = lines[i]
        val trimmed = line.trim()

        // Code block start
        if (trimmed.startsWith("```")) {
            val language = trimmed.removePrefix("```").trim()
            val codeBuilder = StringBuilder()
            i++
            while (i < lines.size && !lines[i].trim().startsWith("```")) {
                if (codeBuilder.isNotEmpty()) codeBuilder.append("\n")
                codeBuilder.append(lines[i])
                i++
            }
            blocks.add(MarkdownBlock.CodeBlock(language, codeBuilder.toString()))
            i++
            continue
        }

        // Horizontal rule
        if (trimmed == "---" || trimmed == "***" || trimmed == "___") {
            blocks.add(MarkdownBlock.HorizontalRule)
            i++
            continue
        }

        // Headers
        if (trimmed.startsWith("#")) {
            val level = trimmed.takeWhile { it == '#' }.length
            val headerText = trimmed.drop(level).trim()
            blocks.add(MarkdownBlock.Header(level, headerText))
            i++
            continue
        }

        // Blockquote
        if (trimmed.startsWith(">")) {
            val quoteText = trimmed.removePrefix(">").trim()
            blocks.add(MarkdownBlock.BlockQuote(quoteText))
            i++
            continue
        }

        // Bullet Item
        if (trimmed.startsWith("- ") || trimmed.startsWith("* ") || trimmed.startsWith("• ")) {
            val itemText = trimmed.substring(2).trim()
            blocks.add(MarkdownBlock.BulletItem(itemText))
            i++
            continue
        }

        // Numbered Item (e.g. 1. or 1) )
        val numberedMatch = "^(\\d+)[.)]\\s+(.*)$".toRegex().find(trimmed)
        if (numberedMatch != null) {
            val num = numberedMatch.groupValues[1]
            val text = numberedMatch.groupValues[2]
            blocks.add(MarkdownBlock.NumberedItem(num, text))
            i++
            continue
        }

        // Paragraph (or blank line)
        if (trimmed.isNotEmpty()) {
            blocks.add(MarkdownBlock.Paragraph(line))
        }
        i++
    }

    return blocks
}
