package com.ashish.personalshopperagent.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ashish.personalshopperagent.ui.theme.Emerald
import com.ashish.personalshopperagent.ui.theme.GoldenStar
import com.ashish.personalshopperagent.ui.theme.White
import com.ashish.personalshopperagent.ui.theme.WhiteMid
import com.ashish.personalshopperagent.ui.theme.WhiteDim

private val LinkBlue = androidx.compose.ui.graphics.Color(0xFF64B5F6)
private val BODY_SIZE = 16.sp

@Composable
fun MarkdownText(markdown: String, modifier: Modifier = Modifier) {
    val uriHandler = LocalUriHandler.current
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        markdown.split("\n").forEach { line ->
            when {
                line.startsWith("# ") -> {
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = parseInline(line.removePrefix("# ")),
                        style = MaterialTheme.typography.headlineSmall,
                        color = White,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(4.dp))
                }

                line.startsWith("## ") -> {
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = parseInline(line.removePrefix("## ")),
                        style = MaterialTheme.typography.titleMedium,
                        color = GoldenStar,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(2.dp))
                }

                line.startsWith("### ") -> {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = parseInline(line.removePrefix("### ")),
                        style = MaterialTheme.typography.titleSmall,
                        color = White,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                line.startsWith("- ") || line.startsWith("* ") -> {
                    Row(modifier = Modifier.padding(start = 4.dp)) {
                        Text("•  ", color = Emerald, fontSize = BODY_SIZE)
                        val parsed = parseInline(line.drop(2))
                        ClickableText(
                            text = parsed,
                            style = TextStyle(color = WhiteMid, fontSize = BODY_SIZE),
                            onClick = { offset ->
                                parsed.getStringAnnotations("URL", offset, offset)
                                    .firstOrNull()?.let { uriHandler.openUri(it.item) }
                            }
                        )
                    }
                }

                line.isBlank() -> Spacer(Modifier.height(6.dp))

                else -> {
                    val parsed = parseInline(line)
                    ClickableText(
                        text = parsed,
                        style = TextStyle(color = WhiteMid, fontSize = BODY_SIZE),
                        onClick = { offset ->
                            parsed.getStringAnnotations("URL", offset, offset)
                                .firstOrNull()?.let { uriHandler.openUri(it.item) }
                        }
                    )
                }
            }
        }
    }
}

fun parseInline(text: String): AnnotatedString = buildAnnotatedString {
    val regex = Regex("""\*\*(.+?)\*\*|\[(.+?)\]\((.+?)\)|(https?://\S+)""")
    var lastIndex = 0
    regex.findAll(text).forEach { match ->
        append(text.substring(lastIndex, match.range.first))
        when {
            match.value.startsWith("**") -> {
                withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = White)) {
                    append(match.groupValues[1])
                }
            }
            match.value.startsWith("[") -> {
                val linkText = match.groupValues[2]
                val url = match.groupValues[3]
                pushStringAnnotation("URL", url)
                withStyle(SpanStyle(color = LinkBlue, textDecoration = TextDecoration.Underline)) {
                    append(linkText)
                }
                pop()
            }
            else -> {
                val url = match.value
                pushStringAnnotation("URL", url)
                withStyle(SpanStyle(color = LinkBlue, textDecoration = TextDecoration.Underline)) {
                    append(url)
                }
                pop()
            }
        }
        lastIndex = match.range.last + 1
    }
    append(text.substring(lastIndex))
}
