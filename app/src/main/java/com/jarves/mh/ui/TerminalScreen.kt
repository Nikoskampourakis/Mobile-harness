package com.jarves.mh.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.AssistChip
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jarves.mh.R
import com.jarves.mh.model.ActiveProcessInfo
import com.jarves.mh.model.BroadcastScope
import com.jarves.mh.model.QuickCommand
import com.jarves.mh.model.TerminalFontFamily
import com.jarves.mh.model.TerminalOutputLine
import com.jarves.mh.model.TerminalProfile
import com.jarves.mh.model.TerminalSession
import com.jarves.mh.model.TerminalSkinConfig
import com.jarves.mh.model.TerminalSkinStyle
import com.jarves.mh.ui.theme.AppThemeMode
import com.jarves.mh.ui.theme.PocketGreen
import com.jarves.mh.ui.theme.PocketOrange
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.window.core.layout.WindowSizeClass
import androidx.window.core.layout.WindowWidthSizeClass

private const val ASCII_BANNER = """
 __  __       _     _ _        _    _                                
|  \/  |     | |   (_) |      | |  | |                               
| \  / | ___ | |__  _| | ___  | |__| | __ _ _ __ _ __   ___  ___ ___ 
| |\/| |/ _ \| '_ \| | |/ _ \ |  __  |/ _` | '__| '_ \ / _ \/ __/ __|
| |  | | (_) | |_) | | |  __/ | |  | | (_| | |  | | | |  __/\__ \__ \
|_|  |_|\___/|_.__/|_|_|\___| |_|  |_|\__,_|_|  |_| |_|\___||___/___/
[Mobile Harness Multi-Session Developer Terminal Workstation]
"""

@Composable
private fun rememberAsciiBanner(skinConfig: TerminalSkinConfig): AnnotatedString {
    return remember(skinConfig.customBannerText, skinConfig.bannerColor, skinConfig.bannerGradient) {
        val baseText = skinConfig.customBannerText ?: ASCII_BANNER
        buildAnnotatedString {
            if (skinConfig.bannerGradient != null && skinConfig.bannerGradient.size >= 2) {
                // Approximate gradient by line or character - here we do line-based simple gradient
                val lines = baseText.trim('\n').split('\n')
                lines.forEachIndexed { i, line ->
                    val ratio = i.toFloat() / (lines.size - 1).coerceAtLeast(1)
                    val color = interpolateColor(skinConfig.bannerGradient, ratio)
                    withStyle(SpanStyle(color = color)) {
                        append(line)
                        if (i < lines.size - 1) append("\n")
                    }
                }
            } else {
                withStyle(SpanStyle(color = skinConfig.bannerColor?.let { Color(it) } ?: PocketGreen)) {
                    append(baseText)
                }
            }
        }
    }
}

private fun interpolateColor(colors: List<Long>, ratio: Float): Color {
    if (colors.isEmpty()) return PocketGreen
    if (colors.size == 1) return Color(colors[0])
    val segmentCount = colors.size - 1
    val segment = (ratio * segmentCount).toInt().coerceIn(0, segmentCount - 1)
    val segmentRatio = (ratio * segmentCount) - segment
    val c1 = Color(colors[segment])
    val c2 = Color(colors[segment + 1])
    return Color(
        red = c1.red + (c2.red - c1.red) * segmentRatio,
        green = c1.green + (c2.green - c1.green) * segmentRatio,
        blue = c1.blue + (c2.blue - c1.blue) * segmentRatio,
        alpha = c1.alpha + (c2.alpha - c1.alpha) * segmentRatio
    )
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TerminalScreen(
    windowSizeClass: WindowSizeClass? = null,
    lines: List<TerminalOutputLine>,
    isRunning: Boolean,
    onRun: (String) -> Unit,
    onInput: (String) -> Unit = {},
    onInterrupt: (() -> Unit)? = null,
    onClear: () -> Unit,
    onToggleTheme: () -> Unit = {},
    themeMode: AppThemeMode = AppThemeMode.DARK,
    title: String = stringResource(R.string.ws_terminal_linux_title),
    subtitle: String = stringResource(R.string.ws_terminal_linux_subtitle),
    liveOutput: String = "",
    currentCommand: String? = null,
    commandDraft: String? = null,
    onCommandDraftConsumed: () -> Unit = {},
    promptPath: String = "/workspace",
    onStop: (() -> Unit)? = null,
    showThemeAction: Boolean = false,
    showQuickCommands: Boolean = false,
    compactHeader: Boolean = false,
    // Multi-Session & Customization enhancements
    sessions: List<TerminalSession> = emptyList(),
    activeSessionId: String = "",
    onSelectSession: (String) -> Unit = {},
    onNewSession: () -> Unit = {},
    onCloseSession: (String) -> Unit = {},
    onRenameSession: (String, String) -> Unit = { _, _ -> },
    isSplitEnabled: Boolean = false,
    onToggleSplit: () -> Unit = {},
    splitSession: TerminalSession? = null,
    onBroadcastCommand: (String, BroadcastScope) -> Unit = { _, _ -> },
    onStopAll: () -> Unit = {},
    onClearAll: () -> Unit = {},
    quickCommandsList: List<QuickCommand> = emptyList(),
    onAddQuickCommand: () -> Unit = {},
    skinConfig: TerminalSkinConfig = TerminalSkinConfig(),
    onUpdateSkin: (TerminalSkinConfig) -> Unit = {},
    profiles: List<TerminalProfile> = emptyList(),
    onAddProfile: (TerminalProfile) -> Unit = {},
    onDeleteProfile: (String) -> Unit = {},
    onResetProfile: (String) -> Unit = {},
    activeProcesses: List<ActiveProcessInfo> = emptyList(),
    onRefreshProcesses: () -> Unit = {},
    onTalkToAgent: (String) -> Unit = {},
) {
    var commandInput by remember { mutableStateOf(TextFieldValue()) }
    var commandHistory by remember { mutableStateOf(emptyList<String>()) }
    var historyIndex by remember { mutableStateOf(-1) }
    var altActive by rememberSaveable { mutableStateOf(false) }
    var ctrlActive by rememberSaveable { mutableStateOf(false) }
    val terminalScrollState = rememberScrollState()
    val terminalListState = rememberLazyListState()
    val inputFocusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val keyboardVisible = WindowInsets.ime.getBottom(LocalDensity.current) > 0
    val scope = rememberCoroutineScope()
    val terminalPromptPath = if (promptPath == "/workspace") "~" else "~${promptPath.removePrefix("/workspace")}"

    var showSettingsModal by remember { mutableStateOf(false) }
    var showProcessModal by remember { mutableStateOf(false) }

    val openTerminalKeyboard = {
        inputFocusRequester.requestFocus()
        scope.launch {
            delay(80)
            keyboardController?.show()
        }
    }
    val submitCommand = {
        if (commandInput.text.isNotBlank()) {
            val submitted = commandInput.text
            if (isRunning) {
                onInput(submitted)
            } else {
                onRun(submitted)
                commandHistory = (commandHistory + submitted).takeLast(50)
                historyIndex = -1
            }
            commandInput = TextFieldValue()
        }
    }

    LaunchedEffect(lines.size, liveOutput) {
        if (lines.isNotEmpty() || liveOutput.isNotEmpty()) {
            terminalListState.animateScrollToItem((lines.size + 3).coerceAtLeast(0))
        }
    }

    LaunchedEffect(commandDraft) {
        if (!commandDraft.isNullOrBlank()) {
            commandInput = TextFieldValue(commandDraft, TextRange(commandDraft.length))
            historyIndex = -1
            onCommandDraftConsumed()
        }
    }

    LaunchedEffect(Unit) {
        snapshotFlow { terminalScrollState.maxValue }
            .collect { maxValue ->
                terminalScrollState.scrollTo(maxValue)
            }
    }
    LaunchedEffect(lines.size, isRunning, commandInput.text.length) {
        delay(100)
        terminalScrollState.scrollTo(terminalScrollState.maxValue)
    }

    val isDark = when (themeMode) {
        AppThemeMode.DARK -> true
        AppThemeMode.LIGHT -> false
        AppThemeMode.SYSTEM -> isSystemInDarkTheme()
    }

    // Determine Skin styling colors
    val (terminalBg, promptColor, commandTextColor, outputTextColor, frameBorderColor) = when (skinConfig.style) {
        TerminalSkinStyle.MATRIX -> listOf(
            Color(0xFF040A05),
            Color(0xFF00FF66),
            Color(0xFF00FF66),
            Color(0xFF80FFB0),
            Color(0xFF00FF66).copy(alpha = 0.45f)
        )
        TerminalSkinStyle.CYBERPUNK -> listOf(
            Color(0xFF0D0616),
            Color(0xFFE879F9),
            Color(0xFFFDE047),
            Color(0xFFF1F5F9),
            Color(0xFFE879F9).copy(alpha = 0.5f)
        )
        TerminalSkinStyle.GLASS -> listOf(
            Color(0xFF111726).copy(alpha = 0.92f),
            Color(0xFF38BDF8),
            Color(0xFFF8FAFC),
            Color(0xFFCBD5E1),
            Color(0xFF38BDF8).copy(alpha = 0.4f)
        )
        TerminalSkinStyle.CRT -> listOf(
            Color(0xFF140B04),
            Color(0xFFFFB000),
            Color(0xFFFFCC00),
            Color(0xFFFFD54F),
            Color(0xFFFFB000).copy(alpha = 0.45f)
        )
        TerminalSkinStyle.MINIMAL -> listOf(
            if (isDark) Color(0xFF090D14) else Color(0xFFF6F8FA),
            if (isDark) PocketGreen else Color(0xFF0D7A3E),
            if (isDark) Color(0xFFF0F6FC) else MaterialTheme.colorScheme.onSurface,
            if (isDark) Color(0xFFC9D1D9) else MaterialTheme.colorScheme.onSurface,
            if (isDark) MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f) else MaterialTheme.colorScheme.outlineVariant
        )
    }

    val selectedFontFamily = when (skinConfig.fontFamily) {
        TerminalFontFamily.MONOSPACE, TerminalFontFamily.CODE -> FontFamily.Monospace
        TerminalFontFamily.SANS -> FontFamily.SansSerif
    }
    
    val baseFontSize = skinConfig.fontSizeSp.sp
    val responsiveFontSize = if (windowSizeClass?.windowWidthSizeClass == WindowWidthSizeClass.EXPANDED) {
        (skinConfig.fontSizeSp + 2).sp
    } else {
        baseFontSize
    }
    val responsiveLineHeight = if (windowSizeClass?.windowWidthSizeClass == WindowWidthSizeClass.EXPANDED) {
        (skinConfig.lineHeightSp + 2).sp
    } else {
        skinConfig.lineHeightSp.sp
    }

    Scaffold(
        modifier = if (compactHeader) Modifier else Modifier.statusBarsPadding(),
        topBar = {
            Column {
                if (sessions.isNotEmpty()) {
                    TerminalTabBar(
                        sessions = sessions,
                        activeSessionId = activeSessionId,
                        onSelectSession = onSelectSession,
                        onNewSession = onNewSession,
                        onCloseSession = onCloseSession,
                        onRenameSession = onRenameSession,
                        isSplitEnabled = isSplitEnabled,
                        onToggleSplit = onToggleSplit,
                        onOpenSettings = { showSettingsModal = true },
                        onOpenProcessManager = {
                            onRefreshProcesses()
                            showProcessModal = true
                        },
                    )
                } else if (!compactHeader) {
                    TopAppBar(
                        title = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), RoundedCornerShape(9.dp)),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(Icons.Default.Terminal, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(17.dp))
                                }
                                Spacer(Modifier.width(10.dp))
                                Column {
                                    Text(title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
                                    Text(subtitle, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        },
                        actions = {
                            IconButton(onClick = onClear) {
                                Icon(Icons.Default.DeleteOutline, contentDescription = stringResource(R.string.ws_terminal_clear_output))
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
                    )
                }
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .then(if (compactHeader) Modifier else Modifier.imePadding()),
        ) {
            // Main Output Area (or Split View)
            if (isSplitEnabled && splitSession != null) {
                // Dual Pane Split View
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    // Pane 1: Active Session
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .border(1.dp, frameBorderColor, RoundedCornerShape(10.dp)),
                        color = terminalBg,
                        shape = RoundedCornerShape(10.dp),
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(terminalScrollState)
                                .padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Text("Pane 1: Active Session", fontSize = 10.sp, color = promptColor, fontWeight = FontWeight.Bold)
                            for (item in lines.takeLast(30)) {
                                TerminalCommandPrompt(promptPath = terminalPromptPath, command = item.command, isDark = isDark)
                                if (item.output.isNotEmpty()) {
                                    Text(
                                        text = sanitizeTerminalOutput(item.output),
                                        fontFamily = selectedFontFamily,
                                        fontSize = skinConfig.fontSizeSp.sp,
                                        color = if (item.exitCode != 0) MaterialTheme.colorScheme.error else outputTextColor,
                                    )
                                }
                            }
                            if (liveOutput.isNotBlank()) {
                                Text(sanitizeTerminalOutput(liveOutput), fontFamily = selectedFontFamily, fontSize = skinConfig.fontSizeSp.sp, color = outputTextColor)
                            }
                        }
                    }

                    // Pane 2: Split Session
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .border(1.dp, Color(0xFF38BDF8).copy(alpha = 0.5f), RoundedCornerShape(10.dp)),
                        color = terminalBg,
                        shape = RoundedCornerShape(10.dp),
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Text("Pane 2: ${splitSession.name} (${splitSession.cwd})", fontSize = 10.sp, color = Color(0xFF38BDF8), fontWeight = FontWeight.Bold)
                            for (item in splitSession.lines.takeLast(30)) {
                                TerminalCommandPrompt(promptPath = splitSession.cwd, command = item.command, isDark = isDark)
                                if (item.output.isNotEmpty()) {
                                    Text(
                                        text = sanitizeTerminalOutput(item.output),
                                        fontFamily = selectedFontFamily,
                                        fontSize = skinConfig.fontSizeSp.sp,
                                        color = if (item.exitCode != 0) MaterialTheme.colorScheme.error else outputTextColor,
                                    )
                                }
                            }
                            if (splitSession.liveOutput.isNotBlank()) {
                                Text(sanitizeTerminalOutput(splitSession.liveOutput), fontFamily = selectedFontFamily, fontSize = skinConfig.fontSizeSp.sp, color = outputTextColor)
                            }
                        }
                    }
                }
            } else {
                // Single Pane Console View
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                        .border(1.dp, frameBorderColor, RoundedCornerShape(12.dp)),
                    color = terminalBg,
                    shape = RoundedCornerShape(12.dp),
                ) {
                    val asciiBanner = rememberAsciiBanner(skinConfig)
                    Column(modifier = Modifier.fillMaxSize()) {
                        SelectionContainer(modifier = Modifier.weight(1f).fillMaxWidth()) {
                            LazyColumn(
                                state = terminalListState,
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                // 1. ASCII Banner
                                if (skinConfig.showAsciiBanner) {
                                    item(key = "terminal-ascii-banner") {
                                        Text(
                                            text = asciiBanner,
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = (if (windowSizeClass?.windowWidthSizeClass == WindowWidthSizeClass.EXPANDED) skinConfig.fontSizeSp else skinConfig.fontSizeSp * 0.85f).sp,
                                            lineHeight = (if (windowSizeClass?.windowWidthSizeClass == WindowWidthSizeClass.EXPANDED) skinConfig.lineHeightSp else skinConfig.lineHeightSp * 0.85f).sp,
                                            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                                        )
                                    }
                                }

                                // 2. Welcome Message (if empty)
                                if (lines.isEmpty() && !isRunning) {
                                    item(key = "terminal-ready-msg") {
                                        Text(
                                            stringResource(R.string.workspace_terminal_ready),
                                            fontFamily = selectedFontFamily,
                                            fontSize = skinConfig.fontSizeSp.sp,
                                            color = outputTextColor.copy(alpha = 0.6f),
                                        )
                                    }
                                }

                                // 3. History Lines
                                items(lines, key = { it.id }) { item ->
                                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        TerminalCommandPrompt(promptPath = terminalPromptPath, command = item.command, isDark = isDark)
                                        if (item.output.isNotEmpty()) {
                                            Text(
                                                text = sanitizeTerminalOutput(item.output),
                                                fontFamily = selectedFontFamily,
                                                fontSize = responsiveFontSize,
                                                lineHeight = responsiveLineHeight,
                                                color = if (item.exitCode != 0) MaterialTheme.colorScheme.error else outputTextColor,
                                                modifier = Modifier.padding(start = 8.dp),
                                            )
                                        }
                                    }
                                }

                                // 4. Active Command
                                if (isRunning && currentCommand != null) {
                                    item(key = "current-running-command") {
                                        TerminalCommandPrompt(promptPath = terminalPromptPath, command = currentCommand, isDark = isDark)
                                    }
                                }

                                // 5. Live Output
                                if (isRunning && liveOutput.isNotBlank()) {
                                    item(key = "terminal-live-output") {
                                        Text(
                                            sanitizeTerminalOutput(liveOutput),
                                            fontFamily = selectedFontFamily,
                                            fontSize = responsiveFontSize,
                                            lineHeight = responsiveLineHeight,
                                            color = outputTextColor,
                                        )
                                    }
                                }
                            }
                        }

                        // 6. Inline Prompt Input
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            val prefix = if (isRunning) "" else "root@pocket:$terminalPromptPath# "
                            val prefixVisualTransformation = remember(prefix, isDark) {
                                VisualTransformation { text ->
                                    val transformed = buildAnnotatedString {
                                        withStyle(SpanStyle(color = promptColor, fontWeight = FontWeight.Bold)) {
                                            append(prefix)
                                        }
                                        withStyle(SpanStyle(color = commandTextColor, fontWeight = FontWeight.SemiBold)) {
                                            append(text.text)
                                        }
                                    }
                                    val offsetMapping = object : OffsetMapping {
                                        override fun originalToTransformed(offset: Int): Int =
                                            (offset + prefix.length).coerceIn(0, prefix.length + text.length)

                                        override fun transformedToOriginal(offset: Int): Int =
                                            (offset - prefix.length).coerceIn(0, text.length)
                                    }
                                    TransformedText(transformed, offsetMapping)
                                }
                            }

                            BasicTextField(
                                value = commandInput,
                                onValueChange = { next ->
                                    if (ctrlActive && next.text.length > commandInput.text.length) {
                                        val inserted = next.text.substring(
                                            commandInput.selection.start.coerceAtMost(next.text.length),
                                            next.selection.end.coerceAtMost(next.text.length),
                                        )
                                        when {
                                            inserted.any { it.equals('c', ignoreCase = true) } -> {
                                                if (isRunning) onInterrupt?.invoke() else commandInput = TextFieldValue()
                                                ctrlActive = false
                                            }
                                            inserted.any { it.equals('d', ignoreCase = true) } -> {
                                                if (isRunning) onInput("\u0004") else onRun("exit")
                                                ctrlActive = false
                                            }
                                            inserted.any { it.equals('l', ignoreCase = true) } -> {
                                                onClear()
                                                ctrlActive = false
                                            }
                                            inserted.any { it.equals('z', ignoreCase = true) } -> {
                                                onStop?.invoke()
                                                ctrlActive = false
                                            }
                                            inserted.any { it.equals('u', ignoreCase = true) } -> {
                                                commandInput = TextFieldValue()
                                                ctrlActive = false
                                            }
                                            else -> {
                                                commandInput = next
                                                ctrlActive = false
                                            }
                                        }
                                    } else {
                                        commandInput = next
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .focusRequester(inputFocusRequester),
                                singleLine = false,
                                visualTransformation = prefixVisualTransformation,
                                cursorBrush = SolidColor(promptColor),
                                textStyle = androidx.compose.ui.text.TextStyle(
                                    fontFamily = selectedFontFamily,
                                    fontSize = responsiveFontSize,
                                    lineHeight = responsiveLineHeight,
                                    fontWeight = FontWeight.SemiBold,
                                    color = commandTextColor,
                                ),
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                                keyboardActions = KeyboardActions(
                                    onSend = { submitCommand() },
                                    onDone = { submitCommand() },
                                    onGo = { submitCommand() },
                                ),
                            )
                            Spacer(Modifier.height(4.dp))
                        }
                    }
                }
            }

            // Mobile Terminal Key Toolbar (Ctrl, Alt, Tab, arrows)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TerminalKeyButton("Ctrl+C", stringResource(R.string.ws_terminal_ctrl_c)) {
                    if (isRunning) onInterrupt?.invoke() else commandInput = TextFieldValue()
                }
                TerminalKeyButton("Ctrl+D", "EOF / Exit") {
                    if (isRunning) onInput("\u0004") else onRun("exit")
                }
                TerminalKeyButton("Ctrl+Z", "Suspend") {
                    onStop?.invoke()
                }
                TerminalKeyButton("Tab", "Autocomplete") {
                    if (isRunning) {
                        onInput("\t")
                    } else {
                        commandInput = commandInput.copy(
                            text = commandInput.text + "    ",
                            selection = TextRange(commandInput.text.length + 4),
                        )
                    }
                }
                TerminalKeyButton("CTRL", stringResource(R.string.ws_terminal_ctrl_modifier), active = ctrlActive, fixedWidth = true) {
                    ctrlActive = !ctrlActive
                    if (ctrlActive) openTerminalKeyboard()
                }
                TerminalKeyButton("ESC", stringResource(R.string.ws_terminal_escape)) { commandInput = TextFieldValue() }
                TerminalKeyButton("↑", stringResource(R.string.ws_terminal_previous_command)) {
                    commandHistory.getOrNull(if (historyIndex < 0) commandHistory.lastIndex else (historyIndex - 1).coerceAtLeast(0))?.let {
                        historyIndex = if (historyIndex < 0) commandHistory.lastIndex else (historyIndex - 1).coerceAtLeast(0)
                        commandInput = TextFieldValue(it, TextRange(it.length))
                    }
                }
                TerminalKeyButton("↓", stringResource(R.string.ws_terminal_next_command)) {
                    if (historyIndex >= 0) {
                        historyIndex = (historyIndex + 1).takeIf { it < commandHistory.size } ?: -1
                        commandInput = TextFieldValue(commandHistory.getOrNull(historyIndex) ?: "", TextRange((commandHistory.getOrNull(historyIndex) ?: "").length))
                    }
                }
                TerminalIconKeyButton(Icons.Default.ArrowBack, stringResource(R.string.ws_terminal_cursor_left)) {
                    commandInput = commandInput.copy(selection = TextRange((commandInput.selection.start - 1).coerceAtLeast(0)))
                }
                TerminalIconKeyButton(Icons.Default.ArrowForward, stringResource(R.string.ws_terminal_cursor_right)) {
                    commandInput = commandInput.copy(selection = TextRange((commandInput.selection.end + 1).coerceAtMost(commandInput.text.length)))
                }
                TerminalKeyButton("ALT", stringResource(R.string.ws_terminal_alt_modifier), active = altActive, fixedWidth = true) { altActive = !altActive }
            }

            // Command Bar with broadcast and agent integration
            TerminalCommandBar(
                onRunCommand = onRun,
                onBroadcastCommand = onBroadcastCommand,
                onStopAll = onStopAll,
                onClearAll = onClearAll,
                quickCommands = quickCommandsList,
                onAddQuickCommand = onAddQuickCommand,
                onTalkToAgent = onTalkToAgent,
                autocorrectEnabled = skinConfig.autocorrectEnabled,
            )
        }
    }

    if (showSettingsModal) {
        TerminalSettingsModalSheet(
            skinConfig = skinConfig,
            onUpdateSkin = onUpdateSkin,
            profiles = profiles,
            onAddProfile = onAddProfile,
            onDeleteProfile = onDeleteProfile,
            onResetProfile = onResetProfile,
            onDismiss = { showSettingsModal = false },
        )
    }

    if (showProcessModal) {
        ProcessActivityManagerDialog(
            processes = activeProcesses,
            terminals = sessions,
            onClose = { showProcessModal = false },
            onStopTerminal = { _ -> onStopAll() },
            onRefresh = onRefreshProcesses,
        )
    }
}

@Composable
private fun TerminalKeyButton(
    label: String,
    description: String,
    active: Boolean = false,
    fixedWidth: Boolean = false,
    onClick: () -> Unit,
) {
    androidx.compose.material3.OutlinedButton(
        onClick = onClick,
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
        modifier = Modifier.height(34.dp).then(if (fixedWidth) Modifier.width(78.dp) else Modifier),
        colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
            containerColor = if (active) PocketOrange.copy(alpha = 0.18f) else Color.Transparent,
            contentColor = if (active) PocketOrange else MaterialTheme.colorScheme.onSurface,
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (active) PocketOrange else MaterialTheme.colorScheme.outlineVariant,
        ),
    ) {
        Text(
            if (active) "$label ✓" else label,
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp,
            maxLines = 1,
        )
    }
}

@Composable
private fun TerminalIconKeyButton(icon: androidx.compose.ui.graphics.vector.ImageVector, description: String, onClick: () -> Unit) {
    IconButton(onClick = onClick, modifier = Modifier.size(34.dp)) {
        Icon(icon, contentDescription = description, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun TerminalCommandPrompt(promptPath: String, command: String, isDark: Boolean = true) {
    val promptGreen = if (isDark) PocketGreen else Color(0xFF0D7A3E)
    val commandColor = if (isDark) Color(0xFFF0F6FC) else MaterialTheme.colorScheme.onSurface
    val promptText = remember(promptPath, command, isDark) {
        buildAnnotatedString {
            withStyle(SpanStyle(color = promptGreen, fontWeight = FontWeight.Bold)) {
                append("root@pocket:$promptPath# ")
            }
            withStyle(SpanStyle(color = commandColor, fontWeight = FontWeight.SemiBold)) {
                append(command)
            }
        }
    }
    Text(
        text = promptText,
        modifier = Modifier.fillMaxWidth(),
        fontFamily = FontFamily.Monospace,
        fontSize = 12.sp,
        lineHeight = 18.sp,
        softWrap = true,
    )
}
