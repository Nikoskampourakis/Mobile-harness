package com.jarves.mh.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.WrapText
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jarves.mh.ui.theme.PocketGreen
import com.jarves.mh.ui.theme.PocketOrange
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * State representing code editor customization preferences.
 */
data class CodeEditorConfig(
    val wordWrap: Boolean = false,
    val fontSizeSp: Int = 13,
    val showLineNumbers: Boolean = true,
    val showMinimap: Boolean = true,
    val stickyScroll: Boolean = true,
    val codeFolding: Boolean = true,
    val renderIndentGuides: Boolean = true,
)

/**
 * Advanced customizable Code Editor screen for Mobile Harness.
 * Supports:
 * - Word wrapping toggle vs horizontal scrolling
 * - Font size adjustments (10sp to 24sp)
 * - Minimap visual preview column
 * - Line numbers gutter
 * - Sticky scroll (pins enclosing class/function/scope at top while scrolling)
 * - Code folding (collapses and expands code blocks)
 * - Indentation guides (renders vertical lines for scope depth)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdvancedCodeEditorScreen(
    filePath: String,
    content: String?,
    loading: Boolean,
    initialConfig: CodeEditorConfig = CodeEditorConfig(),
    onConfigChanged: (CodeEditorConfig) -> Unit = {},
    onClose: () -> Unit,
) {
    val fileName = filePath.substringAfterLast('/')
    val ext = fileName.substringAfterLast('.', "")
    val isMarkdown = ext == "md"
    val clipboard = LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    var copied by remember { mutableStateOf(false) }

    var config by rememberSaveable {
        mutableStateOf(initialConfig)
    }
    var showSettingsSheet by rememberSaveable { mutableStateOf(false) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var showSearchBar by rememberSaveable { mutableStateOf(false) }

    // Set of folded line starting indices
    val foldedLines = remember { mutableStateListOf<Int>() }

    fun updateConfig(newConfig: CodeEditorConfig) {
        config = newConfig
        onConfigChanged(newConfig)
    }

    Scaffold(
        topBar = {
            Column(Modifier.background(MaterialTheme.colorScheme.background)) {
                TopAppBar(
                    title = {
                        Column {
                            Text(fileName, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(filePath, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onClose) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Close file")
                        }
                    },
                    actions = {
                        IconButton(onClick = { showSearchBar = !showSearchBar }) {
                            Icon(Icons.Default.Search, contentDescription = "Search text")
                        }
                        IconButton(onClick = { showSettingsSheet = true }) {
                            Icon(Icons.Default.Tune, contentDescription = "Editor settings")
                        }
                        if (!content.isNullOrEmpty()) {
                            IconButton(onClick = {
                                clipboard.setText(AnnotatedString(content))
                                copied = true
                                scope.launch { delay(2000); copied = false }
                            }) {
                                Icon(
                                    if (copied) Icons.Default.Check else Icons.Default.ContentCopy,
                                    contentDescription = "Copy code",
                                    tint = if (copied) PocketGreen else MaterialTheme.colorScheme.onSurface,
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
                )

                // Quick Action Bar for Editor
                if (!isMarkdown && !content.isNullOrEmpty()) {
                    EditorQuickControlBar(
                        config = config,
                        onToggleWrap = { updateConfig(config.copy(wordWrap = !config.wordWrap)) },
                        onZoomIn = { updateConfig(config.copy(fontSizeSp = (config.fontSizeSp + 1).coerceAtMost(24))) },
                        onZoomOut = { updateConfig(config.copy(fontSizeSp = (config.fontSizeSp - 1).coerceAtLeast(10))) },
                        onToggleLineNumbers = { updateConfig(config.copy(showLineNumbers = !config.showLineNumbers)) },
                        onToggleMinimap = { updateConfig(config.copy(showMinimap = !config.showMinimap)) },
                        onToggleStickyScroll = { updateConfig(config.copy(stickyScroll = !config.stickyScroll)) },
                        onToggleIndentGuides = { updateConfig(config.copy(renderIndentGuides = !config.renderIndentGuides)) },
                    )
                }

                // Search Bar
                AnimatedVisibility(visible = showSearchBar) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(8.dp),
                    ) {
                        Row(
                            Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(Icons.Default.Search, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.width(8.dp))
                            TextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                placeholder = { Text("Search in $fileName...", fontSize = 12.sp) },
                                modifier = Modifier.weight(1f),
                                textStyle = LocalTextStyle.current.copy(fontSize = 12.sp),
                                singleLine = true,
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent,
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent,
                                ),
                            )
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }, Modifier.size(24.dp)) {
                                    Icon(Icons.Default.Close, null, Modifier.size(14.dp))
                                }
                            }
                        }
                    }
                }
            }
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when {
                loading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = PocketOrange)
                    }
                }
                content == null -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Description, null, Modifier.size(40.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.height(8.dp))
                            Text("No content", fontWeight = FontWeight.SemiBold)
                            Text("The file could not be read.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                isMarkdown -> {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        item { MarkdownText(markdown = content, color = MaterialTheme.colorScheme.onSurface) }
                    }
                }
                else -> {
                    CodeEditorView(
                        content = content,
                        language = ext,
                        config = config,
                        searchQuery = searchQuery,
                        foldedLines = foldedLines,
                        onToggleFold = { lineIdx ->
                            if (lineIdx in foldedLines) foldedLines.remove(lineIdx)
                            else foldedLines.add(lineIdx)
                        },
                    )
                }
            }
        }
    }

    if (showSettingsSheet) {
        CodeEditorSettingsModal(
            config = config,
            onConfigChanged = ::updateConfig,
            onDismiss = { showSettingsSheet = false },
        )
    }
}

@Composable
private fun EditorQuickControlBar(
    config: CodeEditorConfig,
    onToggleWrap: () -> Unit,
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit,
    onToggleLineNumbers: () -> Unit,
    onToggleMinimap: () -> Unit,
    onToggleStickyScroll: () -> Unit,
    onToggleIndentGuides: () -> Unit,
) {
    Surface(
        color = Color(0xFF161B22),
        modifier = Modifier.fillMaxWidth().height(38.dp),
        border = BorderStroke(1.dp, Color(0xFF30363D)),
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            // Text Wrap toggle
            EditorControlChip(
                label = "Wrap",
                active = config.wordWrap,
                onClick = onToggleWrap,
                icon = Icons.AutoMirrored.Filled.WrapText,
            )

            // Font Zoom Out
            EditorIconChip(
                icon = Icons.Default.Remove,
                tooltip = "Decrease Font Size",
                onClick = onZoomOut,
            )

            // Font Indicator
            Text(
                "${config.fontSizeSp}sp",
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF8B949E),
                modifier = Modifier.padding(horizontal = 4.dp),
            )

            // Font Zoom In
            EditorIconChip(
                icon = Icons.Default.Add,
                tooltip = "Increase Font Size",
                onClick = onZoomIn,
            )

            VerticalDivider(color = Color(0xFF30363D), modifier = Modifier.height(18.dp).padding(horizontal = 2.dp))

            // Line numbers toggle
            EditorControlChip(
                label = "# Lines",
                active = config.showLineNumbers,
                onClick = onToggleLineNumbers,
            )

            // Minimap toggle
            EditorControlChip(
                label = "Minimap",
                active = config.showMinimap,
                onClick = onToggleMinimap,
            )

            // Sticky Scroll toggle
            EditorControlChip(
                label = "Sticky Scope",
                active = config.stickyScroll,
                onClick = onToggleStickyScroll,
            )

            // Indent Guides toggle
            EditorControlChip(
                label = "Guides",
                active = config.renderIndentGuides,
                onClick = onToggleIndentGuides,
            )
        }
    }
}

@Composable
private fun EditorControlChip(
    label: String,
    active: Boolean,
    onClick: () -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = if (active) Color(0xFF238636) else Color(0xFF21262D),
        contentColor = if (active) Color.White else Color(0xFF8B949E),
        modifier = Modifier.clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (icon != null) {
                Icon(icon, null, Modifier.size(13.dp))
                Spacer(Modifier.width(3.dp))
            }
            Text(label, fontSize = 11.sp, fontWeight = if (active) FontWeight.Bold else FontWeight.Medium)
        }
    }
}

@Composable
private fun EditorIconChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tooltip: String,
    onClick: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = Color(0xFF21262D),
        contentColor = Color(0xFF8B949E),
        modifier = Modifier.clickable(onClick = onClick),
    ) {
        Box(Modifier.size(24.dp), contentAlignment = Alignment.Center) {
            Icon(icon, tooltip, Modifier.size(13.dp))
        }
    }
}

@Composable
private fun CodeEditorView(
    content: String,
    language: String = "",
    config: CodeEditorConfig,
    searchQuery: String,
    foldedLines: List<Int>,
    onToggleFold: (Int) -> Unit,
) {
    val allLines = remember(content) { content.lines() }
    val listState = rememberLazyListState()

    // Sticky scope header calculation
    val currentStickyScope by remember(listState.firstVisibleItemIndex, allLines, config.stickyScroll) {
        derivedStateOf {
            if (!config.stickyScroll) null
            else {
                val currentIdx = listState.firstVisibleItemIndex
                findEnclosingScope(allLines, currentIdx)
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D1117)),
    ) {
        Row(Modifier.fillMaxSize()) {
            // Main editor code column
            val horizontalScrollState = rememberScrollState()
            val textRowModifier = if (config.wordWrap) Modifier.fillMaxWidth() else Modifier.horizontalScroll(horizontalScrollState)

            Box(Modifier.weight(1f).fillMaxHeight()) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 64.dp),
                ) {
                    itemsIndexed(allLines) { idx, lineText ->
                        // Check if line is hidden inside a folded block
                        val isHidden = isLineFoldedHidden(idx, allLines, foldedLines)
                        if (!isHidden) {
                            val isFoldHeader = isFoldableLine(lineText)
                            val isFolded = idx in foldedLines

                            CodeLineRow(
                                lineIndex = idx,
                                lineText = lineText,
                                language = language,
                                config = config,
                                isFoldHeader = isFoldHeader,
                                isFolded = isFolded,
                                searchQuery = searchQuery,
                                onToggleFold = { onToggleFold(idx) },
                                modifier = textRowModifier,
                            )
                        }
                    }
                }

                // Sticky Scroll Scope Header
                if (currentStickyScope != null) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.TopCenter),
                        color = Color(0xFF161B22).copy(alpha = 0.96f),
                        border = BorderStroke(1.dp, Color(0xFF30363D)),
                        shadowElevation = 4.dp,
                    ) {
                        Row(
                            Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                Icons.Default.Code,
                                contentDescription = null,
                                tint = PocketGreen,
                                modifier = Modifier.size(15.dp),
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = currentStickyScope!!.trim(),
                                fontSize = config.fontSizeSp.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF58A6FF),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }

            // Minimap Column
            if (config.showMinimap && allLines.isNotEmpty()) {
                MinimapBar(
                    lines = allLines,
                    firstVisibleIndex = listState.firstVisibleItemIndex,
                    visibleItemCount = listState.layoutInfo.visibleItemsInfo.size,
                    modifier = Modifier
                        .width(48.dp)
                        .fillMaxHeight()
                        .background(Color(0xFF090D12)),
                )
            }
        }
    }
}

@Composable
private fun CodeLineRow(
    lineIndex: Int,
    lineText: String,
    language: String = "",
    config: CodeEditorConfig,
    isFoldHeader: Boolean,
    isFolded: Boolean,
    searchQuery: String,
    onToggleFold: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val indentSpaces = lineText.takeWhile { it == ' ' }.length
    val isSearchHit = searchQuery.isNotBlank() && lineText.contains(searchQuery, ignoreCase = true)

    val highlightedText = remember(lineText, language, isFolded) {
        if (isFolded) {
            AnnotatedString("$lineText  { ... }")
        } else {
            SyntaxHighlighter.highlight(lineText, language)
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (isSearchHit) Color(0xFF388BFD).copy(alpha = 0.25f) else Color.Transparent)
            .padding(vertical = 1.dp),
        verticalAlignment = Alignment.Top,
    ) {
        // Line numbers & folding chevron gutter
        if (config.showLineNumbers) {
            Row(
                modifier = Modifier
                    .width(46.dp)
                    .padding(end = 4.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (config.codeFolding && isFoldHeader) {
                    Icon(
                        if (isFolded) Icons.Default.ChevronRight else Icons.Default.ExpandMore,
                        contentDescription = "Fold block",
                        tint = Color(0xFF8B949E),
                        modifier = Modifier
                            .size(14.dp)
                            .clickable(onClick = onToggleFold),
                    )
                } else {
                    Spacer(Modifier.width(14.dp))
                }
                Text(
                    text = "${lineIndex + 1}",
                    fontFamily = FontFamily.Monospace,
                    fontSize = (config.fontSizeSp - 2).coerceAtLeast(9).sp,
                    color = Color(0xFF4A5568),
                    textAlign = TextAlign.End,
                )
            }
        }

        // Line content with Indentation Guides
        Box(modifier = modifier.weight(1f, fill = false)) {
            // Render vertical indentation guide lines
            if (config.renderIndentGuides && indentSpaces >= 2) {
                Canvas(
                    modifier = Modifier
                        .matchParentSize()
                        .padding(start = 2.dp),
                ) {
                    val spaceWidthPx = size.width / lineText.length.coerceAtLeast(1)
                    val guideInterval = 4
                    var sp = guideInterval
                    while (sp <= indentSpaces) {
                        val x = sp * spaceWidthPx
                        drawLine(
                            color = Color(0xFF21262D),
                            start = Offset(x, 0f),
                            end = Offset(x, size.height),
                            strokeWidth = 1.5f,
                        )
                        sp += guideInterval
                    }
                }
            }

            Text(
                text = highlightedText,
                fontFamily = FontFamily.Monospace,
                fontSize = config.fontSizeSp.sp,
                lineHeight = (config.fontSizeSp * 1.45f).sp,
                modifier = Modifier.padding(start = 6.dp, end = 12.dp),
            )
        }
    }
}

@Composable
private fun MinimapBar(
    lines: List<String>,
    firstVisibleIndex: Int,
    visibleItemCount: Int,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        val totalLines = lines.size.coerceAtLeast(1)
        val lineH = (size.height / totalLines).coerceIn(1f, 4f)

        // Draw micro lines
        lines.forEachIndexed { i, text ->
            val y = i * lineH
            val trimmedLen = text.trim().length
            if (trimmedLen > 0) {
                val lineW = (trimmedLen * 1.5f).coerceAtMost(size.width * 0.85f)
                val color = if (text.contains("fun ") || text.contains("class ")) Color(0xFF58A6FF).copy(alpha = 0.7f)
                else Color(0xFF8B949E).copy(alpha = 0.35f)
                drawRect(
                    color = color,
                    topLeft = Offset(4f, y),
                    size = androidx.compose.ui.geometry.Size(lineW, (lineH * 0.75f).coerceAtLeast(1f)),
                )
            }
        }

        // Draw visible viewport highlight
        val viewportTop = (firstVisibleIndex.toFloat() / totalLines) * size.height
        val viewportH = ((visibleItemCount.toFloat() / totalLines) * size.height).coerceAtLeast(12f)
        drawRect(
            color = Color(0x3358A6FF),
            topLeft = Offset(0f, viewportTop),
            size = androidx.compose.ui.geometry.Size(size.width, viewportH),
        )
        drawLine(
            color = Color(0x8858A6FF),
            start = Offset(0f, viewportTop),
            end = Offset(size.width, viewportTop),
            strokeWidth = 2f,
        )
        drawLine(
            color = Color(0x8858A6FF),
            start = Offset(0f, viewportTop + viewportH),
            end = Offset(size.width, viewportTop + viewportH),
            strokeWidth = 2f,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CodeEditorSettingsModal(
    config: CodeEditorConfig,
    onConfigChanged: (CodeEditorConfig) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Tune, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(10.dp))
                Text("Code Editor Settings", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }

            HorizontalDivider()

            // Font size slider
            Column {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Font Size", fontWeight = FontWeight.Medium)
                    Text("${config.fontSizeSp} sp", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
                Slider(
                    value = config.fontSizeSp.toFloat(),
                    onValueChange = { onConfigChanged(config.copy(fontSizeSp = it.toInt())) },
                    valueRange = 10f..24f,
                    steps = 13,
                )
            }

            // Word wrap
            EditorToggleRow(
                title = "Text Wrapping",
                subtitle = "Wrap long lines instead of horizontal scroll",
                checked = config.wordWrap,
                onCheckedChange = { onConfigChanged(config.copy(wordWrap = it)) },
            )

            // Line numbers
            EditorToggleRow(
                title = "Line Numbers",
                subtitle = "Show line count gutter on the left",
                checked = config.showLineNumbers,
                onCheckedChange = { onConfigChanged(config.copy(showLineNumbers = it)) },
            )

            // Minimap
            EditorToggleRow(
                title = "Minimap",
                subtitle = "Show miniature code overview strip on right edge",
                checked = config.showMinimap,
                onCheckedChange = { onConfigChanged(config.copy(showMinimap = it)) },
            )

            // Sticky scroll
            EditorToggleRow(
                title = "Sticky Scroll",
                subtitle = "Pin current class or function header at top",
                checked = config.stickyScroll,
                onCheckedChange = { onConfigChanged(config.copy(stickyScroll = it)) },
            )

            // Code folding
            EditorToggleRow(
                title = "Code Folding",
                subtitle = "Allow collapsing blocks and scopes",
                checked = config.codeFolding,
                onCheckedChange = { onConfigChanged(config.copy(codeFolding = it)) },
            )

            // Indentation guides
            EditorToggleRow(
                title = "Indentation Guides",
                subtitle = "Render vertical guide lines for indent levels",
                checked = config.renderIndentGuides,
                onCheckedChange = { onConfigChanged(config.copy(renderIndentGuides = it)) },
            )
        }
    }
}

@Composable
private fun EditorToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f).padding(end = 12.dp)) {
            Text(title, fontWeight = FontWeight.Medium, fontSize = 14.sp)
            Text(subtitle, fontSize = 11.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
        )
    }
}

private fun isFoldableLine(line: String): Boolean {
    val trimmed = line.trim()
    return trimmed.endsWith("{") || trimmed.endsWith("(") || (trimmed.endsWith(":") && (trimmed.startsWith("def ") || trimmed.startsWith("class ")))
}

private fun isLineFoldedHidden(idx: Int, allLines: List<String>, foldedLines: List<Int>): Boolean {
    for (foldStart in foldedLines) {
        if (idx > foldStart) {
            val startLine = allLines.getOrNull(foldStart) ?: continue
            val startIndent = startLine.takeWhile { it == ' ' }.length
            // Check if idx is within this fold's block
            var endIdx = foldStart + 1
            while (endIdx < allLines.size) {
                val l = allLines[endIdx]
                if (l.isNotBlank()) {
                    val indent = l.takeWhile { it == ' ' }.length
                    if (indent <= startIndent && !l.trim().startsWith("}")) {
                        break
                    }
                    if (l.trim() == "}" && indent == startIndent) {
                        endIdx++
                        break
                    }
                }
                endIdx++
            }
            if (idx < endIdx) return true
        }
    }
    return false
}

private fun findEnclosingScope(lines: List<String>, currentIdx: Int): String? {
    val scopeKeywords = listOf("fun ", "class ", "interface ", "def ", "function ", "async function ", "struct ", "impl ", "enum class ")
    for (i in currentIdx downTo 0) {
        val line = lines.getOrNull(i) ?: continue
        val trimmed = line.trim()
        if (scopeKeywords.any { trimmed.startsWith(it) }) {
            return line
        }
    }
    return null
}
