package com.jarves.mh.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jarves.mh.R
import com.jarves.mh.ui.theme.PocketGreen
import com.jarves.mh.ui.theme.PocketOrange
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

sealed interface MarkdownBlock {
    data class Header(val level: Int, val text: String) : MarkdownBlock
    data class CodeBlock(val language: String, val code: String) : MarkdownBlock
    data class MathBlock(val latex: String) : MarkdownBlock
    data class Table(val headers: List<String>, val rows: List<List<String>>) : MarkdownBlock
    data class BulletItem(val depth: Int, val text: String) : MarkdownBlock
    data class NumberedItem(val number: String, val text: String) : MarkdownBlock
    data class BlockQuote(val text: String) : MarkdownBlock
    object HorizontalRule : MarkdownBlock
    data class Paragraph(val text: String) : MarkdownBlock
}

@Composable
fun MarkdownText(
    markdown: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    onRunCode: ((String) -> Unit)? = null,
) {
    val blocks = remember(markdown) { parseMarkdown(markdown) }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        blocks.forEach { block ->
            when (block) {
                is MarkdownBlock.Header -> HeaderBlock(block)
                is MarkdownBlock.CodeBlock -> CodeSnippetBlock(block, onRunCode)
                is MarkdownBlock.MathBlock -> MathEquationBlock(block)
                is MarkdownBlock.Table -> MarkdownTableBlock(block)
                is MarkdownBlock.BulletItem -> BulletBlock(block, color)
                is MarkdownBlock.NumberedItem -> NumberedBlock(block, color)
                is MarkdownBlock.BlockQuote -> QuoteBlock(block)
                is MarkdownBlock.HorizontalRule -> HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                    modifier = Modifier.padding(vertical = 4.dp),
                )
                is MarkdownBlock.Paragraph -> {
                    Text(
                        text = formatInlineMarkdown(block.text),
                        style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 21.sp),
                        color = color,
                    )
                }
            }
        }
    }
}

@Composable
private fun HeaderBlock(header: MarkdownBlock.Header) {
    val style = when (header.level) {
        1 -> MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, fontSize = 19.sp)
        2 -> MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontSize = 16.sp)
        else -> MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold, fontSize = 14.5.sp)
    }
    Text(
        text = formatInlineMarkdown(header.text),
        style = style,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.padding(top = 4.dp),
    )
}

@Composable
private fun BulletBlock(item: MarkdownBlock.BulletItem, color: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = (item.depth * 12).dp),
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            modifier = Modifier
                .padding(top = 8.dp, end = 8.dp)
                .size(4.5.dp)
                .background(PocketOrange, CircleShape),
        )
        Text(
            text = formatInlineMarkdown(item.text),
            style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 21.sp),
            color = color,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun NumberedBlock(item: MarkdownBlock.NumberedItem, color: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            text = item.number,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = PocketOrange),
            modifier = Modifier.padding(end = 6.dp),
        )
        Text(
            text = formatInlineMarkdown(item.text),
            style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 21.sp),
            color = color,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun QuoteBlock(quote: MarkdownBlock.BlockQuote) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(20.dp)
                .background(PocketOrange, RoundedCornerShape(2.dp)),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = formatInlineMarkdown(quote.text),
            style = MaterialTheme.typography.bodyMedium.copy(
                fontStyle = FontStyle.Italic,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            ),
        )
    }
}

@Composable
private fun MathEquationBlock(math: MarkdownBlock.MathBlock) {
    val formattedMath = remember(math.latex) { renderLatexToUnicode(math.latex) }
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = Color(0xFF131722),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF283044)),
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Icon(
                Icons.Default.Calculate,
                contentDescription = null,
                tint = PocketOrange.copy(alpha = 0.7f),
                modifier = Modifier.size(16.dp).padding(end = 6.dp),
            )
            Text(
                text = formattedMath,
                fontFamily = FontFamily.Serif,
                fontStyle = FontStyle.Italic,
                fontWeight = FontWeight.Medium,
                fontSize = 15.sp,
                color = Color(0xFFE2E8F0),
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun MarkdownTableBlock(table: MarkdownBlock.Table) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = Color(0xFF14171E),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2A2E39)),
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
    ) {
        Column(
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .padding(8.dp),
        ) {
            // Header Row
            Row(
                modifier = Modifier
                    .background(Color(0xFF1E2330), RoundedCornerShape(6.dp))
                    .padding(vertical = 6.dp, horizontal = 4.dp),
            ) {
                table.headers.forEach { header ->
                    Text(
                        text = header.trim(),
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = PocketOrange,
                        modifier = Modifier.widthIn(min = 90.dp).padding(horizontal = 8.dp),
                    )
                }
            }

            HorizontalDivider(
                color = Color(0xFF323846),
                modifier = Modifier.padding(vertical = 4.dp),
            )

            // Data Rows
            table.rows.forEachIndexed { idx, row ->
                val rowBg = if (idx % 2 == 0) Color.Transparent else Color(0xFF181C26)
                Row(
                    modifier = Modifier
                        .background(rowBg, RoundedCornerShape(4.dp))
                        .padding(vertical = 5.dp, horizontal = 4.dp),
                ) {
                    row.forEachIndexed { colIdx, cell ->
                        Text(
                            text = cell.trim(),
                            fontSize = 12.sp,
                            color = Color(0xFFE2E8F0),
                            modifier = Modifier.widthIn(min = 90.dp).padding(horizontal = 8.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CodeSnippetBlock(block: MarkdownBlock.CodeBlock, onRunCode: ((String) -> Unit)?) {
    val clipboard = LocalClipboardManager.current
    var copied by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val highlightedCode = remember(block.code, block.language) {
        SyntaxHighlighter.highlight(block.code, block.language)
    }

    Surface(
        shape = RoundedCornerShape(10.dp),
        color = Color(0xFF10141D),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color(0xFF262C3A), RoundedCornerShape(10.dp)),
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF181D2A))
                    .padding(horizontal = 10.dp, vertical = 3.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = block.language.ifBlank { "code" },
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF9AA0A6),
                    fontFamily = FontFamily.Monospace,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val shellLanguage = block.language.lowercase() in setOf("", "bash", "sh", "shell", "zsh", "console", "terminal")
                    if (onRunCode != null && shellLanguage) {
                        IconButton(
                            onClick = { onRunCode(block.code.trim()) },
                            modifier = Modifier.size(26.dp),
                        ) {
                            Icon(
                                Icons.Default.PlayArrow,
                                contentDescription = stringResource(R.string.ws_md_run_in_terminal),
                                tint = PocketOrange,
                                modifier = Modifier.size(16.dp),
                            )
                        }
                    }
                    IconButton(
                        onClick = {
                            clipboard.setText(AnnotatedString(block.code))
                            copied = true
                            scope.launch {
                                delay(2000)
                                copied = false
                            }
                        },
                        modifier = Modifier.size(26.dp),
                    ) {
                        Icon(
                            imageVector = if (copied) Icons.Default.Check else Icons.Default.ContentCopy,
                            contentDescription = stringResource(R.string.ws_md_copy_code),
                            tint = if (copied) PocketGreen else Color(0xFF9AA0A6),
                            modifier = Modifier.size(15.dp),
                        )
                    }
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(10.dp),
            ) {
                Text(
                    text = highlightedCode,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.5.sp,
                    lineHeight = 18.sp,
                )
            }
        }
    }
}

@Composable
fun formatInlineMarkdown(text: String): AnnotatedString {
    val codeBg = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
    val codeColor = PocketOrange
    val primaryColor = MaterialTheme.colorScheme.primary

    return remember(text, codeBg, codeColor, primaryColor) {
        buildAnnotatedString {
            var i = 0
            val len = text.length

            while (i < len) {
                when {
                    // Inline Code: `code`
                    text[i] == '`' -> {
                        val end = text.indexOf('`', i + 1)
                        if (end != -1) {
                            withStyle(
                                SpanStyle(
                                    fontFamily = FontFamily.Monospace,
                                    background = codeBg,
                                    color = codeColor,
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                ),
                            ) {
                                append(" ${text.substring(i + 1, end)} ")
                            }
                            i = end + 1
                        } else {
                            append(text[i])
                            i++
                        }
                    }
                    // Inline Math: $latex$
                    text[i] == '$' && i + 1 < len && text[i + 1] != '$' -> {
                        val end = text.indexOf('$', i + 1)
                        if (end != -1 && end > i + 1) {
                            val mathText = text.substring(i + 1, end)
                            withStyle(
                                SpanStyle(
                                    fontFamily = FontFamily.Serif,
                                    fontStyle = FontStyle.Italic,
                                    color = Color(0xFF93C5FD),
                                    fontSize = 13.5.sp,
                                ),
                            ) {
                                append(renderLatexToUnicode(mathText))
                            }
                            i = end + 1
                        } else {
                            append(text[i])
                            i++
                        }
                    }
                    // Bold & Italic: ***text***
                    text.startsWith("***", i) -> {
                        val end = text.indexOf("***", i + 3)
                        if (end != -1) {
                            withStyle(SpanStyle(fontWeight = FontWeight.Bold, fontStyle = FontStyle.Italic)) {
                                append(text.substring(i + 3, end))
                            }
                            i = end + 3
                        } else {
                            append(text[i])
                            i++
                        }
                    }
                    // Bold: **text**
                    text.startsWith("**", i) -> {
                        val end = text.indexOf("**", i + 2)
                        if (end != -1) {
                            withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                                append(text.substring(i + 2, end))
                            }
                            i = end + 2
                        } else {
                            append(text[i])
                            i++
                        }
                    }
                    // Italic: *text* or _text_
                    (text[i] == '*' || text[i] == '_') && (i + 1 < len && !text[i + 1].isWhitespace()) -> {
                        val mark = text[i]
                        val end = text.indexOf(mark, i + 1)
                        if (end != -1) {
                            withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                                append(text.substring(i + 1, end))
                            }
                            i = end + 1
                        } else {
                            append(text[i])
                            i++
                        }
                    }
                    // Strikethrough: ~~text~~
                    text.startsWith("~~", i) -> {
                        val end = text.indexOf("~~", i + 2)
                        if (end != -1) {
                            withStyle(SpanStyle(textDecoration = TextDecoration.LineThrough)) {
                                append(text.substring(i + 2, end))
                            }
                            i = end + 2
                        } else {
                            append(text[i])
                            i++
                        }
                    }
                    // Markdown Link: [label](url)
                    text[i] == '[' -> {
                        val closeBracket = text.indexOf(']', i)
                        if (closeBracket != -1 && closeBracket + 1 < len && text[closeBracket + 1] == '(') {
                            val closeParen = text.indexOf(')', closeBracket + 2)
                            if (closeParen != -1) {
                                val label = text.substring(i + 1, closeBracket)
                                withStyle(
                                    SpanStyle(
                                        color = primaryColor,
                                        fontWeight = FontWeight.Medium,
                                        textDecoration = TextDecoration.Underline,
                                    ),
                                ) {
                                    append(label)
                                }
                                i = closeParen + 1
                            } else {
                                append(text[i])
                                i++
                            }
                        } else {
                            append(text[i])
                            i++
                        }
                    }
                    else -> {
                        append(text[i])
                        i++
                    }
                }
            }
        }
    }
}

/**
 * Parses raw markdown text into structured MarkdownBlock components,
 * with full support for tables, LaTeX math blocks ($$...$$), headers, code snippets,
 * quotes, and lists.
 */
fun parseMarkdown(raw: String): List<MarkdownBlock> {
    val lines = raw.lines()
    val blocks = mutableListOf<MarkdownBlock>()
    var inCodeBlock = false
    var codeLang = ""
    val codeLines = mutableListOf<String>()

    var inMathBlock = false
    val mathLines = mutableListOf<String>()

    var inTable = false
    val tableLines = mutableListOf<String>()

    val currentParagraphLines = mutableListOf<String>()

    fun flushParagraph() {
        if (currentParagraphLines.isNotEmpty()) {
            val text = currentParagraphLines.joinToString("\n").trim()
            if (text.isNotEmpty()) {
                blocks.add(MarkdownBlock.Paragraph(text))
            }
            currentParagraphLines.clear()
        }
    }

    fun flushTable() {
        if (tableLines.size >= 2) {
            val headers = tableLines[0].split('|').map(String::trim).filter(String::isNotEmpty)
            val rows = tableLines.drop(1)
                .filterNot { it.contains("---") }
                .map { line ->
                    line.split('|').map(String::trim).filter(String::isNotEmpty)
                }
                .filter { it.isNotEmpty() }
            if (headers.isNotEmpty()) {
                blocks.add(MarkdownBlock.Table(headers, rows))
            }
        }
        tableLines.clear()
        inTable = false
    }

    for (line in lines) {
        val trimmed = line.trim()

        // Code block toggle (```)
        if (trimmed.startsWith("```")) {
            flushParagraph()
            if (inTable) flushTable()
            if (inCodeBlock) {
                blocks.add(MarkdownBlock.CodeBlock(codeLang, codeLines.joinToString("\n")))
                codeLines.clear()
                codeLang = ""
                inCodeBlock = false
            } else {
                inCodeBlock = true
                codeLang = trimmed.removePrefix("```").trim()
            }
            continue
        }

        if (inCodeBlock) {
            codeLines.add(line)
            continue
        }

        // Display LaTeX Math block ($$ ... $$)
        if (trimmed.startsWith("$$")) {
            flushParagraph()
            if (inTable) flushTable()
            if (inMathBlock) {
                blocks.add(MarkdownBlock.MathBlock(mathLines.joinToString("\n")))
                mathLines.clear()
                inMathBlock = false
            } else {
                val singleLineMath = trimmed.removePrefix("$$").removeSuffix("$$").trim()
                if (trimmed.endsWith("$$") && trimmed.length > 2) {
                    blocks.add(MarkdownBlock.MathBlock(singleLineMath))
                } else {
                    inMathBlock = true
                    if (singleLineMath.isNotEmpty()) mathLines.add(singleLineMath)
                }
            }
            continue
        }

        if (inMathBlock) {
            if (trimmed.endsWith("$$")) {
                val endPart = trimmed.removeSuffix("$$").trim()
                if (endPart.isNotEmpty()) mathLines.add(endPart)
                blocks.add(MarkdownBlock.MathBlock(mathLines.joinToString("\n")))
                mathLines.clear()
                inMathBlock = false
            } else {
                mathLines.add(line)
            }
            continue
        }

        // Table lines (| col | col |)
        if (trimmed.startsWith("|") && trimmed.endsWith("|")) {
            flushParagraph()
            inTable = true
            tableLines.add(trimmed)
            continue
        } else if (inTable) {
            flushTable()
        }

        if (trimmed.isEmpty()) {
            flushParagraph()
            continue
        }

        // Horizontal Rule
        if (trimmed == "---" || trimmed == "***" || trimmed == "___") {
            flushParagraph()
            blocks.add(MarkdownBlock.HorizontalRule)
            continue
        }

        // Headings (#, ##, ###)
        if (trimmed.startsWith("#")) {
            flushParagraph()
            val level = trimmed.takeWhile { it == '#' }.length
            val text = trimmed.drop(level).trim()
            blocks.add(MarkdownBlock.Header(level, text))
            continue
        }

        // Blockquotes (> text)
        if (trimmed.startsWith(">")) {
            flushParagraph()
            val text = trimmed.removePrefix(">").trim()
            blocks.add(MarkdownBlock.BlockQuote(text))
            continue
        }

        // Unordered List (- item, * item, + item)
        if (trimmed.startsWith("- ") || trimmed.startsWith("* ") || trimmed.startsWith("+ ")) {
            flushParagraph()
            val indent = line.takeWhile { it.isWhitespace() }.length / 2
            val text = trimmed.substring(2).trim()
            blocks.add(MarkdownBlock.BulletItem(indent, text))
            continue
        }

        // Ordered List (1. item, 2. item)
        val orderedMatch = Regex("^([0-9]+[.)])\\s+(.*)").find(trimmed)
        if (orderedMatch != null) {
            flushParagraph()
            val num = orderedMatch.groupValues[1]
            val text = orderedMatch.groupValues[2]
            blocks.add(MarkdownBlock.NumberedItem(num, text))
            continue
        }

        // Normal paragraph text
        currentParagraphLines.add(line)
    }

    if (inCodeBlock) {
        blocks.add(MarkdownBlock.CodeBlock(codeLang, codeLines.joinToString("\n")))
    }
    if (inMathBlock) {
        blocks.add(MarkdownBlock.MathBlock(mathLines.joinToString("\n")))
    }
    if (inTable) {
        flushTable()
    }
    flushParagraph()

    return blocks
}

/**
 * Converts common LaTeX math expressions into beautifully styled Unicode mathematical notation.
 */
fun renderLatexToUnicode(latex: String): String {
    var s = latex.trim()
    // Common Greek letters
    s = s.replace("\\alpha", "α")
        .replace("\\beta", "β")
        .replace("\\gamma", "γ")
        .replace("\\delta", "δ")
        .replace("\\epsilon", "ε")
        .replace("\\theta", "θ")
        .replace("\\lambda", "λ")
        .replace("\\mu", "μ")
        .replace("\\pi", "π")
        .replace("\\sigma", "σ")
        .replace("\\tau", "τ")
        .replace("\\omega", "ω")
        .replace("\\Delta", "Δ")
        .replace("\\Omega", "Ω")
        .replace("\\Sigma", "Σ")

    // Math symbols
    s = s.replace("\\times", "×")
        .replace("\\div", "÷")
        .replace("\\pm", "±")
        .replace("\\mp", "∓")
        .replace("\\le", "≤")
        .replace("\\leq", "≤")
        .replace("\\ge", "≥")
        .replace("\\geq", "≥")
        .replace("\\ne", "≠")
        .replace("\\neq", "≠")
        .replace("\\approx", "≈")
        .replace("\\infty", "∞")
        .replace("\\int", "∫")
        .replace("\\sum", "∑")
        .replace("\\prod", "∏")
        .replace("\\sqrt", "√")
        .replace("\\cdot", "·")
        .replace("\\to", "→")
        .replace("\\in", "∈")
        .replace("\\notin", "∉")
        .replace("\\subset", "⊂")

    // Fractions \frac{a}{b} -> (a / b)
    s = s.replace(Regex("""\\frac\{([^}]+)\}\{([^}]+)\}"""), "($1 / $2)")

    // Superscripts ^0 to ^9, ^n, ^x, ^2
    val superscripts = mapOf(
        '0' to '⁰', '1' to '¹', '2' to '²', '3' to '³', '4' to '⁴',
        '5' to '⁵', '6' to '⁶', '7' to '⁷', '8' to '⁸', '9' to '⁹',
        '+' to '⁺', '-' to '⁻', '=' to '⁼', '(' to '⁽', ')' to '⁾',
        'n' to 'ⁿ', 'i' to 'ⁱ', 'x' to 'ˣ',
    )
    s = s.replace(Regex("""\^\{?([0-9nix+\-()]+)\}?""")) { match ->
        val inner = match.groupValues[1]
        inner.map { superscripts[it] ?: it }.joinToString("")
    }

    // Subscripts _0 to _9
    val subscripts = mapOf(
        '0' to '₀', '1' to '₁', '2' to '₂', '3' to '₃', '4' to '₄',
        '5' to '₅', '6' to '₆', '7' to '₇', '8' to '₈', '9' to '₉',
        'i' to 'ᵢ', 'j' to 'ⱼ', 'k' to 'ₖ', 'n' to 'ₙ', 'm' to 'ₘ',
    )
    s = s.replace(Regex("""_\{?([0-9ijknm]+)\}?""")) { match ->
        val inner = match.groupValues[1]
        inner.map { subscripts[it] ?: it }.joinToString("")
    }

    // Clean remaining braces
    s = s.replace("{", "").replace("}", "")

    return s
}
