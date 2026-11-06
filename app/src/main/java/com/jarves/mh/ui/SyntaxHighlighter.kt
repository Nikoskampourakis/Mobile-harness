package com.jarves.mh.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.sp

/**
 * Universal high-performance syntax highlighting engine for Mobile Harness.
 * Supports: JavaScript, TypeScript, Python, Rust, Kotlin, Java, C, C++, C#, Go, Swift,
 * HTML, CSS, JSON, YAML, TOML, Bash, SQL, and Markdown.
 */
object SyntaxHighlighter {

    // Distinct, accessible dark-theme syntax palette
    val ColorKeyword = Color(0xFFFF7B72)       // Coral / Red
    val ColorFunction = Color(0xFFD2A8FF)      // Soft Purple
    val ColorString = Color(0xFFA5D6FF)        // Light Cyan / Sky
    val ColorNumber = Color(0xFF79C0FF)        // Blue
    val ColorComment = Color(0xFF8B949E)       // Dim Gray
    val ColorType = Color(0xFFFFA657)          // Orange / Amber
    val ColorAnnotation = Color(0xFF7EE787)    // Mint Green
    val ColorOperator = Color(0xFFFF7B72)      // Coral
    val ColorProperty = Color(0xFF79C0FF)      // Blue
    val ColorDefault = Color(0xFFE6EDF3)       // Crisp Off-White

    private val commonKeywords = setOf(
        "if", "else", "for", "while", "do", "return", "break", "continue",
        "switch", "case", "default", "try", "catch", "finally", "throw", "throws",
        "new", "delete", "typeof", "instanceof", "void", "true", "false", "null",
        "import", "export", "from", "as", "class", "interface", "extends", "implements",
        "public", "private", "protected", "static", "final", "const", "let", "var",
        "async", "await", "yield", "function", "def", "fn", "val", "fun", "package",
        "struct", "enum", "type", "impl", "trait", "pub", "mut", "ref", "use",
        "crate", "mod", "where", "select", "from", "where", "insert", "update", "delete",
        "create", "table", "alter", "drop", "and", "or", "not", "is", "in", "like",
    )

    private val commonTypes = setOf(
        "int", "float", "double", "char", "bool", "boolean", "string", "String",
        "Int", "Long", "Float", "Double", "Boolean", "Char", "Byte", "Short",
        "u8", "u16", "u32", "u64", "u128", "usize", "i8", "i16", "i32", "i64", "i128", "isize",
        "f32", "f64", "str", "Vec", "Option", "Result", "Box", "Rc", "Arc", "Any",
        "List", "Map", "Set", "Array", "Object", "Promise", "Unit", "void",
    )

    fun highlight(code: String, language: String): AnnotatedString {
        if (code.isEmpty()) return AnnotatedString("")
        val lang = language.lowercase().trim()

        return buildAnnotatedString {
            when (lang) {
                "json" -> highlightJson(code, this)
                "yaml", "yml" -> highlightYaml(code, this)
                "toml" -> highlightToml(code, this)
                "html", "xml", "svg" -> highlightXml(code, this)
                "css", "scss", "sass" -> highlightCss(code, this)
                "sql" -> highlightSql(code, this)
                "bash", "sh", "shell", "zsh" -> highlightBash(code, this)
                "python", "py" -> highlightGenericCode(code, this, isPython = true)
                "rust", "rs" -> highlightGenericCode(code, this, isRust = true)
                else -> highlightGenericCode(code, this)
            }
        }
    }

    private fun highlightGenericCode(
        code: String,
        builder: AnnotatedString.Builder,
        isPython: Boolean = false,
        isRust: Boolean = false,
    ) {
        val len = code.length
        var i = 0

        while (i < len) {
            val c = code[i]

            // Line comments
            if (c == '/' && i + 1 < len && code[i + 1] == '/') {
                val end = code.indexOf('\n', i)
                val lineEnd = if (end != -1) end else len
                builder.withStyle(SpanStyle(color = ColorComment, fontStyle = FontStyle.Italic)) {
                    builder.append(code.substring(i, lineEnd))
                }
                i = lineEnd
                continue
            }
            // Python / Bash line comments
            if (isPython && c == '#') {
                val end = code.indexOf('\n', i)
                val lineEnd = if (end != -1) end else len
                builder.withStyle(SpanStyle(color = ColorComment, fontStyle = FontStyle.Italic)) {
                    builder.append(code.substring(i, lineEnd))
                }
                i = lineEnd
                continue
            }
            // Block comments /* ... */
            if (c == '/' && i + 1 < len && code[i + 1] == '*') {
                val end = code.indexOf("*/", i + 2)
                val commentEnd = if (end != -1) end + 2 else len
                builder.withStyle(SpanStyle(color = ColorComment, fontStyle = FontStyle.Italic)) {
                    builder.append(code.substring(i, commentEnd))
                }
                i = commentEnd
                continue
            }

            // String literals: "...", '...', `...`
            if (c == '"' || c == '\'' || c == '`') {
                val quote = c
                var j = i + 1
                var escaped = false
                while (j < len) {
                    val curr = code[j]
                    if (curr == '\\') {
                        escaped = !escaped
                    } else if (curr == quote && !escaped) {
                        j++
                        break
                    } else {
                        escaped = false
                    }
                    if (curr == '\n' && quote != '`') break
                    j++
                }
                builder.withStyle(SpanStyle(color = ColorString)) {
                    builder.append(code.substring(i, j))
                }
                i = j
                continue
            }

            // Annotations / Decorators: @something
            if (c == '@' && i + 1 < len && (code[i + 1].isLetter() || code[i + 1] == '_')) {
                var j = i + 1
                while (j < len && (code[j].isLetterOrDigit() || code[j] == '_' || code[j] == '.')) {
                    j++
                }
                builder.withStyle(SpanStyle(color = ColorAnnotation, fontWeight = FontWeight.SemiBold)) {
                    builder.append(code.substring(i, j))
                }
                i = j
                continue
            }

            // Numbers
            if (c.isDigit() || (c == '.' && i + 1 < len && code[i + 1].isDigit())) {
                var j = i
                while (j < len && (code[j].isLetterOrDigit() || code[j] == '.' || code[j] == '_')) {
                    j++
                }
                builder.withStyle(SpanStyle(color = ColorNumber)) {
                    builder.append(code.substring(i, j))
                }
                i = j
                continue
            }

            // Words (keywords, types, functions, identifiers)
            if (c.isLetter() || c == '_') {
                var j = i
                while (j < len && (code[j].isLetterOrDigit() || code[j] == '_')) {
                    j++
                }
                val word = code.substring(i, j)
                // Check if followed by '(' -> function call
                var k = j
                while (k < len && (code[k] == ' ' || code[k] == '\t')) k++
                val isFunction = k < len && code[k] == '('

                when {
                    word in commonKeywords -> {
                        builder.withStyle(SpanStyle(color = ColorKeyword, fontWeight = FontWeight.Bold)) {
                            builder.append(word)
                        }
                    }
                    word in commonTypes || (word.first().isUpperCase() && !isFunction) -> {
                        builder.withStyle(SpanStyle(color = ColorType, fontWeight = FontWeight.SemiBold)) {
                            builder.append(word)
                        }
                    }
                    isFunction -> {
                        builder.withStyle(SpanStyle(color = ColorFunction)) {
                            builder.append(word)
                        }
                    }
                    else -> {
                        builder.withStyle(SpanStyle(color = ColorDefault)) {
                            builder.append(word)
                        }
                    }
                }
                i = j
                continue
            }

            // Default fallback
            builder.withStyle(SpanStyle(color = ColorDefault)) {
                builder.append(c)
            }
            i++
        }
    }

    private fun highlightJson(code: String, builder: AnnotatedString.Builder) {
        val regex = Regex("""("(?:\\.|[^"\\])*")(?:\s*(:))?|(-?\d+(?:\.\d+)?(?:[eE][+-]?\d+)?)|(\b(?:true|false|null)\b)|([\{\}\[\]:,])""")
        var lastIdx = 0
        for (match in regex.findAll(code)) {
            val range = match.range
            if (range.first > lastIdx) {
                builder.withStyle(SpanStyle(color = ColorDefault)) {
                    builder.append(code.substring(lastIdx, range.first))
                }
            }
            val groups = match.groups
            when {
                groups[1] != null -> {
                    val isKey = groups[2] != null
                    val color = if (isKey) ColorProperty else ColorString
                    builder.withStyle(SpanStyle(color = color, fontWeight = if (isKey) FontWeight.SemiBold else FontWeight.Normal)) {
                        builder.append(groups[1]!!.value)
                    }
                    if (isKey) {
                        builder.withStyle(SpanStyle(color = ColorDefault)) {
                            builder.append(groups[2]!!.value)
                        }
                    }
                }
                groups[3] != null -> {
                    builder.withStyle(SpanStyle(color = ColorNumber)) {
                        builder.append(match.value)
                    }
                }
                groups[4] != null -> {
                    builder.withStyle(SpanStyle(color = ColorKeyword, fontWeight = FontWeight.Bold)) {
                        builder.append(match.value)
                    }
                }
                else -> {
                    builder.withStyle(SpanStyle(color = ColorDefault)) {
                        builder.append(match.value)
                    }
                }
            }
            lastIdx = range.last + 1
        }
        if (lastIdx < code.length) {
            builder.withStyle(SpanStyle(color = ColorDefault)) {
                builder.append(code.substring(lastIdx))
            }
        }
    }

    private fun highlightYaml(code: String, builder: AnnotatedString.Builder) {
        val lines = code.lines()
        lines.forEachIndexed { index, line ->
            val trimmed = line.trimStart()
            val indent = line.length - trimmed.length
            builder.withStyle(SpanStyle(color = ColorDefault)) {
                builder.append(" ".repeat(indent))
            }
            if (trimmed.startsWith("#")) {
                builder.withStyle(SpanStyle(color = ColorComment, fontStyle = FontStyle.Italic)) {
                    builder.append(trimmed)
                }
            } else if (trimmed.contains(':')) {
                val key = trimmed.substringBefore(':')
                val rest = trimmed.substringAfter(':')
                builder.withStyle(SpanStyle(color = ColorProperty, fontWeight = FontWeight.SemiBold)) {
                    builder.append(key)
                }
                builder.withStyle(SpanStyle(color = ColorDefault)) {
                    builder.append(":")
                }
                val value = rest.trim()
                if (value.startsWith("\"") || value.startsWith("'")) {
                    builder.withStyle(SpanStyle(color = ColorString)) {
                        builder.append(" $value")
                    }
                } else if (value.toLongOrNull() != null || value.toDoubleOrNull() != null) {
                    builder.withStyle(SpanStyle(color = ColorNumber)) {
                        builder.append(" $value")
                    }
                } else if (value in setOf("true", "false", "yes", "no", "null")) {
                    builder.withStyle(SpanStyle(color = ColorKeyword, fontWeight = FontWeight.Bold)) {
                        builder.append(" $value")
                    }
                } else {
                    builder.withStyle(SpanStyle(color = ColorDefault)) {
                        builder.append(rest)
                    }
                }
            } else {
                builder.withStyle(SpanStyle(color = ColorDefault)) {
                    builder.append(trimmed)
                }
            }
            if (index < lines.size - 1) builder.append("\n")
        }
    }

    private fun highlightToml(code: String, builder: AnnotatedString.Builder) {
        val lines = code.lines()
        lines.forEachIndexed { index, line ->
            val trimmed = line.trim()
            if (trimmed.startsWith("#")) {
                builder.withStyle(SpanStyle(color = ColorComment, fontStyle = FontStyle.Italic)) {
                    builder.append(line)
                }
            } else if (trimmed.startsWith("[") && trimmed.endsWith("]")) {
                builder.withStyle(SpanStyle(color = ColorType, fontWeight = FontWeight.Bold)) {
                    builder.append(line)
                }
            } else if (trimmed.contains('=')) {
                val key = line.substringBefore('=')
                val rest = line.substringAfter('=')
                builder.withStyle(SpanStyle(color = ColorProperty, fontWeight = FontWeight.SemiBold)) {
                    builder.append(key)
                }
                builder.withStyle(SpanStyle(color = ColorOperator)) {
                    builder.append("=")
                }
                highlightGenericCode(rest, builder)
            } else {
                builder.withStyle(SpanStyle(color = ColorDefault)) {
                    builder.append(line)
                }
            }
            if (index < lines.size - 1) builder.append("\n")
        }
    }

    private fun highlightXml(code: String, builder: AnnotatedString.Builder) {
        val regex = Regex("""(<!--[\s\S]*?-->)|(</?[a-zA-Z0-9:-]+)|(\s+[a-zA-Z0-9:-]+(?==))|("(?:\\.|[^"\\])*")|(/?>)""")
        var lastIdx = 0
        for (match in regex.findAll(code)) {
            val range = match.range
            if (range.first > lastIdx) {
                builder.withStyle(SpanStyle(color = ColorDefault)) {
                    builder.append(code.substring(lastIdx, range.first))
                }
            }
            when {
                match.groups[1] != null -> builder.withStyle(SpanStyle(color = ColorComment, fontStyle = FontStyle.Italic)) {
                    builder.append(match.value)
                }
                match.groups[2] != null -> builder.withStyle(SpanStyle(color = ColorKeyword, fontWeight = FontWeight.Bold)) {
                    builder.append(match.value)
                }
                match.groups[3] != null -> builder.withStyle(SpanStyle(color = ColorProperty)) {
                    builder.append(match.value)
                }
                match.groups[4] != null -> builder.withStyle(SpanStyle(color = ColorString)) {
                    builder.append(match.value)
                }
                else -> builder.withStyle(SpanStyle(color = ColorKeyword, fontWeight = FontWeight.Bold)) {
                    builder.append(match.value)
                }
            }
            lastIdx = range.last + 1
        }
        if (lastIdx < code.length) {
            builder.withStyle(SpanStyle(color = ColorDefault)) {
                builder.append(code.substring(lastIdx))
            }
        }
    }

    private fun highlightCss(code: String, builder: AnnotatedString.Builder) {
        highlightGenericCode(code, builder)
    }

    private fun highlightSql(code: String, builder: AnnotatedString.Builder) {
        val sqlKeywords = setOf(
            "SELECT", "FROM", "WHERE", "INSERT", "INTO", "VALUES", "UPDATE", "SET",
            "DELETE", "CREATE", "TABLE", "DATABASE", "ALTER", "DROP", "INDEX",
            "PRIMARY", "KEY", "FOREIGN", "REFERENCES", "JOIN", "INNER", "LEFT",
            "RIGHT", "FULL", "ON", "GROUP", "BY", "ORDER", "ASC", "DESC", "HAVING",
            "LIMIT", "OFFSET", "UNION", "ALL", "DISTINCT", "AS", "AND", "OR", "NOT",
            "NULL", "IS", "IN", "BETWEEN", "LIKE", "EXISTS", "CASE", "WHEN", "THEN", "END",
        )
        val len = code.length
        var i = 0
        while (i < len) {
            val c = code[i]
            if (c == '-' && i + 1 < len && code[i + 1] == '-') {
                val end = code.indexOf('\n', i)
                val lineEnd = if (end != -1) end else len
                builder.withStyle(SpanStyle(color = ColorComment, fontStyle = FontStyle.Italic)) {
                    builder.append(code.substring(i, lineEnd))
                }
                i = lineEnd
                continue
            }
            if (c == '\'' || c == '"') {
                val quote = c
                var j = i + 1
                while (j < len && code[j] != quote) j++
                val end = if (j < len) j + 1 else len
                builder.withStyle(SpanStyle(color = ColorString)) {
                    builder.append(code.substring(i, end))
                }
                i = end
                continue
            }
            if (c.isLetter()) {
                var j = i
                while (j < len && (code[j].isLetterOrDigit() || code[j] == '_')) j++
                val word = code.substring(i, j)
                if (word.uppercase() in sqlKeywords) {
                    builder.withStyle(SpanStyle(color = ColorKeyword, fontWeight = FontWeight.Bold)) {
                        builder.append(word)
                    }
                } else {
                    builder.withStyle(SpanStyle(color = ColorDefault)) {
                        builder.append(word)
                    }
                }
                i = j
                continue
            }
            builder.withStyle(SpanStyle(color = ColorDefault)) {
                builder.append(c)
            }
            i++
        }
    }

    private fun highlightBash(code: String, builder: AnnotatedString.Builder) {
        val bashKeywords = setOf(
            "if", "then", "else", "elif", "fi", "for", "while", "do", "done", "in",
            "case", "esac", "function", "select", "time", "return", "exit", "echo",
            "export", "source", "alias", "set", "unset", "cd", "pwd", "sudo",
        )
        val len = code.length
        var i = 0
        while (i < len) {
            val c = code[i]
            if (c == '#') {
                val end = code.indexOf('\n', i)
                val lineEnd = if (end != -1) end else len
                builder.withStyle(SpanStyle(color = ColorComment, fontStyle = FontStyle.Italic)) {
                    builder.append(code.substring(i, lineEnd))
                }
                i = lineEnd
                continue
            }
            if (c == '$' && i + 1 < len && (code[i + 1].isLetterOrDigit() || code[i + 1] == '_' || code[i + 1] == '{')) {
                var j = i + 1
                if (code[j] == '{') {
                    val close = code.indexOf('}', j)
                    j = if (close != -1) close + 1 else len
                } else {
                    while (j < len && (code[j].isLetterOrDigit() || code[j] == '_')) j++
                }
                builder.withStyle(SpanStyle(color = ColorAnnotation, fontWeight = FontWeight.SemiBold)) {
                    builder.append(code.substring(i, j))
                }
                i = j
                continue
            }
            if (c.isLetter() || c == '_') {
                var j = i
                while (j < len && (code[j].isLetterOrDigit() || code[j] == '_' || code[j] == '-')) j++
                val word = code.substring(i, j)
                if (word in bashKeywords) {
                    builder.withStyle(SpanStyle(color = ColorKeyword, fontWeight = FontWeight.Bold)) {
                        builder.append(word)
                    }
                } else {
                    builder.withStyle(SpanStyle(color = ColorDefault)) {
                        builder.append(word)
                    }
                }
                i = j
                continue
            }
            builder.withStyle(SpanStyle(color = ColorDefault)) {
                builder.append(c)
            }
            i++
        }
    }
}
