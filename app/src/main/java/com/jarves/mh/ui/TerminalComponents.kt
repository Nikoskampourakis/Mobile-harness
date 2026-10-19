package com.jarves.mh.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Splitscreen
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jarves.mh.model.ActiveProcessInfo
import com.jarves.mh.model.BroadcastScope
import com.jarves.mh.model.QuickCommand
import com.jarves.mh.model.TerminalAutocorrect
import com.jarves.mh.model.TerminalFontFamily
import com.jarves.mh.model.TerminalProfile
import com.jarves.mh.model.TerminalSession
import com.jarves.mh.model.TerminalSkinConfig
import com.jarves.mh.model.TerminalSkinStyle
import com.jarves.mh.ui.theme.PocketGreen
import com.jarves.mh.ui.theme.PocketOrange

/**
 * Top Tab Bar for multi-session terminals.
 */
@Composable
fun TerminalTabBar(
    sessions: List<TerminalSession>,
    activeSessionId: String,
    onSelectSession: (String) -> Unit,
    onNewSession: () -> Unit,
    onCloseSession: (String) -> Unit,
    onRenameSession: (String, String) -> Unit,
    isSplitEnabled: Boolean,
    onToggleSplit: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenProcessManager: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var sessionToRename by remember { mutableStateOf<TerminalSession?>(null) }
    var renameDraft by remember { mutableStateOf("") }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFF0F1218))
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .horizontalScroll(rememberScrollState()),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            sessions.forEach { session ->
                val isActive = session.id == activeSessionId
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isActive) Color(0xFF1E2430) else Color(0xFF141820),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (isActive) PocketOrange.copy(alpha = 0.6f) else Color(0xFF283040),
                    ),
                    modifier = Modifier
                        .clickable { onSelectSession(session.id) }
                        .testTag("terminal_tab_${session.id}"),
                ) {
                    Row(
                        modifier = Modifier.padding(start = 10.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (session.isRunning) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(PocketGreen, CircleShape)
                            )
                            Spacer(Modifier.width(6.dp))
                        }
                        Text(
                            text = session.name,
                            fontSize = 12.sp,
                            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
                            color = if (isActive) Color.White else Color(0xFF94A3B8),
                            maxLines = 1,
                        )
                        Spacer(Modifier.width(4.dp))
                        // Rename icon on active tab
                        if (isActive) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Rename",
                                tint = Color(0xFF64748B),
                                modifier = Modifier
                                    .size(14.dp)
                                    .clickable {
                                        sessionToRename = session
                                        renameDraft = session.name
                                    }
                            )
                            Spacer(Modifier.width(4.dp))
                        }
                        // Close tab icon
                        if (sessions.size > 1) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close tab",
                                tint = if (isActive) Color(0xFFEF4444) else Color(0xFF64748B),
                                modifier = Modifier
                                    .size(16.dp)
                                    .clickable { onCloseSession(session.id) }
                            )
                        }
                    }
                }
            }

            // New Tab Button
            IconButton(
                onClick = onNewSession,
                modifier = Modifier.size(30.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "New Terminal Tab",
                    tint = PocketOrange,
                    modifier = Modifier.size(18.dp),
                )
            }
        }

        Spacer(Modifier.width(4.dp))

        // Split view toggle
        IconButton(
            onClick = onToggleSplit,
            modifier = Modifier.size(32.dp),
        ) {
            Icon(
                imageVector = Icons.Default.Splitscreen,
                contentDescription = "Toggle Split View",
                tint = if (isSplitEnabled) PocketOrange else Color(0xFF94A3B8),
                modifier = Modifier.size(18.dp),
            )
        }

        // Process Manager icon
        IconButton(
            onClick = onOpenProcessManager,
            modifier = Modifier.size(32.dp),
        ) {
            Icon(
                imageVector = Icons.Default.Speed,
                contentDescription = "Activity & Process Manager",
                tint = Color(0xFF94A3B8),
                modifier = Modifier.size(18.dp),
            )
        }

        // Settings icon
        IconButton(
            onClick = onOpenSettings,
            modifier = Modifier.size(32.dp),
        ) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = "Terminal Settings",
                tint = Color(0xFF94A3B8),
                modifier = Modifier.size(18.dp),
            )
        }
    }

    // Rename dialog
    sessionToRename?.let { targetSession ->
        AlertDialog(
            onDismissRequest = { sessionToRename = null },
            title = { Text("Rename Terminal Session") },
            text = {
                OutlinedTextField(
                    value = renameDraft,
                    onValueChange = { renameDraft = it },
                    label = { Text("Session Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            confirmButton = {
                Button(onClick = {
                    onRenameSession(targetSession.id, renameDraft)
                    sessionToRename = null
                }) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { sessionToRename = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

/**
 * Slide-up or persistent Command Bar for quick actions, broadcast execution,
 * and inline AI agent talk.
 */
@Composable
fun TerminalCommandBar(
    onRunCommand: (String) -> Unit,
    onBroadcastCommand: (String, BroadcastScope) -> Unit,
    onStopAll: () -> Unit,
    onClearAll: () -> Unit,
    quickCommands: List<QuickCommand>,
    onAddQuickCommand: () -> Unit,
    onTalkToAgent: (String) -> Unit,
    autocorrectEnabled: Boolean = true,
    modifier: Modifier = Modifier,
) {
    var commandText by remember { mutableStateOf("") }
    var suggestedCorrection by remember { mutableStateOf<String?>(null) }
    var showBroadcastMenu by remember { mutableStateOf(false) }
    var showAgentDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFF11141A))
            .border(width = 1.dp, color = Color(0xFF222834))
            .padding(8.dp)
            .testTag("terminal_command_bar"),
    ) {
        // Quick Action Chips Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            quickCommands.forEach { qc ->
                AssistChip(
                    onClick = { onRunCommand(qc.command) },
                    label = { Text(qc.label, fontSize = 11.sp, color = Color(0xFFE2E8F0)) },
                    colors = AssistChipDefaults.assistChipColors(containerColor = Color(0xFF1E232E)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF333D4F)),
                )
            }

            // Stop All & Clear All Chips
            AssistChip(
                onClick = onStopAll,
                label = { Text("Stop All", fontSize = 11.sp, color = Color(0xFFF87171)) },
                leadingIcon = { Icon(Icons.Default.Stop, contentDescription = null, tint = Color(0xFFF87171), modifier = Modifier.size(14.dp)) },
                colors = AssistChipDefaults.assistChipColors(containerColor = Color(0xFF2A1515)),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF5A2222)),
            )

            AssistChip(
                onClick = onClearAll,
                label = { Text("Clear All", fontSize = 11.sp, color = Color(0xFF94A3B8)) },
                leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = Color(0xFF94A3B8), modifier = Modifier.size(14.dp)) },
                colors = AssistChipDefaults.assistChipColors(containerColor = Color(0xFF1E232E)),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF333D4F)),
            )

            // Talk to Agent button
            AssistChip(
                onClick = { showAgentDialog = true },
                label = { Text("Talk to Agent", fontSize = 11.sp, color = PocketOrange) },
                leadingIcon = { Icon(Icons.Default.SmartToy, contentDescription = null, tint = PocketOrange, modifier = Modifier.size(14.dp)) },
                colors = AssistChipDefaults.assistChipColors(containerColor = PocketOrange.copy(alpha = 0.12f)),
                border = androidx.compose.foundation.BorderStroke(1.dp, PocketOrange.copy(alpha = 0.35f)),
            )
        }

        Spacer(Modifier.height(6.dp))

        // Autocorrect banner
        if (autocorrectEnabled && suggestedCorrection != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(PocketOrange.copy(alpha = 0.1f), RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "Did you mean: $suggestedCorrection?",
                    fontSize = 11.sp,
                    color = PocketOrange,
                )
                TextButton(onClick = {
                    commandText = suggestedCorrection ?: commandText
                    suggestedCorrection = null
                }) {
                    Text("Apply", fontSize = 11.sp, color = PocketOrange)
                }
            }
            Spacer(Modifier.height(4.dp))
        }

        // Input row + Action buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                value = commandText,
                onValueChange = {
                    commandText = it
                    if (autocorrectEnabled) {
                        suggestedCorrection = TerminalAutocorrect.suggestCorrection(it)
                    }
                },
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                placeholder = { Text("Command or broadcast...", fontSize = 12.sp, color = Color(0xFF64748B)) },
                singleLine = true,
                textStyle = androidx.compose.ui.text.TextStyle(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    color = Color.White,
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = {
                    if (commandText.isNotBlank()) {
                        onRunCommand(commandText.trim())
                        commandText = ""
                        suggestedCorrection = null
                    }
                }),
                shape = RoundedCornerShape(8.dp),
            )

            Spacer(Modifier.width(6.dp))

            // Run in Current
            IconButton(
                onClick = {
                    if (commandText.isNotBlank()) {
                        onRunCommand(commandText.trim())
                        commandText = ""
                        suggestedCorrection = null
                    }
                },
                modifier = Modifier
                    .size(42.dp)
                    .background(PocketOrange, RoundedCornerShape(8.dp)),
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = "Run", tint = Color.Black)
            }

            Spacer(Modifier.width(4.dp))

            // Broadcast options toggle
            IconButton(
                onClick = { showBroadcastMenu = true },
                modifier = Modifier
                    .size(42.dp)
                    .background(Color(0xFF1E2430), RoundedCornerShape(8.dp)),
            ) {
                Icon(Icons.Default.Send, contentDescription = "Broadcast options", tint = Color(0xFF38BDF8))
            }
        }
    }

    // Broadcast Dialog / BottomSheet
    if (showBroadcastMenu) {
        AlertDialog(
            onDismissRequest = { showBroadcastMenu = false },
            title = { Text("Broadcast Command") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = if (commandText.isBlank()) "Enter a command in the bar first, or choose broadcast target:" else "Command: \"$commandText\"",
                        fontSize = 13.sp,
                    )
                    Button(
                        onClick = {
                            if (commandText.isNotBlank()) onBroadcastCommand(commandText.trim(), BroadcastScope.ALL)
                            showBroadcastMenu = false
                            commandText = ""
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = PocketOrange),
                    ) {
                        Text("Broadcast to ALL Open Terminals")
                    }
                    OutlinedButton(
                        onClick = {
                            if (commandText.isNotBlank()) onBroadcastCommand(commandText.trim(), BroadcastScope.OTHERS)
                            showBroadcastMenu = false
                            commandText = ""
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Broadcast to All EXCEPT Current")
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showBroadcastMenu = false }) { Text("Cancel") }
            }
        )
    }

    // Agent Dialog
    if (showAgentDialog) {
        var agentPrompt by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAgentDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.SmartToy, contentDescription = null, tint = PocketOrange)
                    Spacer(Modifier.width(8.dp))
                    Text("Talk to Terminal Agent")
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Ask your coding agent for assistance, shell command suggestions, or code fixes. Output will stream into your active terminal tab.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    OutlinedTextField(
                        value = agentPrompt,
                        onValueChange = { agentPrompt = it },
                        label = { Text("Ask or instruct the agent...") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (agentPrompt.isNotBlank()) {
                            onTalkToAgent(agentPrompt.trim())
                            showAgentDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PocketOrange),
                ) {
                    Text("Send to Terminal")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAgentDialog = false }) { Text("Cancel") }
            }
        )
    }
}

/**
 * Activity & Process Manager dialog to inspect active processes and terminals.
 */
@Composable
fun ProcessActivityManagerDialog(
    processes: List<ActiveProcessInfo>,
    terminals: List<TerminalSession>,
    onClose: () -> Unit,
    onStopTerminal: (String) -> Unit,
    onRefresh: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onClose,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Speed, contentDescription = null, tint = PocketOrange)
                Spacer(Modifier.width(8.dp))
                Text("Process & Activity Manager")
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(380.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Active Sessions (${terminals.size})", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    IconButton(onClick = onRefresh, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh", modifier = Modifier.size(16.dp))
                    }
                }

                Spacer(Modifier.height(6.dp))

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(terminals) { session ->
                        Card(
                            shape = RoundedCornerShape(8.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF171B24)),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(session.name, fontWeight = FontWeight.SemiBold, fontSize = 12.5.sp, color = Color.White)
                                    Text(
                                        "CWD: ${session.cwd} · Lines: ${session.lines.size} · Status: ${if (session.isRunning) "Running" else "Idle"}",
                                        fontSize = 10.5.sp,
                                        color = Color(0xFF94A3B8),
                                    )
                                }
                                if (session.isRunning) {
                                    IconButton(
                                        onClick = { onStopTerminal(session.id) },
                                        modifier = Modifier.size(28.dp),
                                    ) {
                                        Icon(Icons.Default.Stop, contentDescription = "Stop", tint = Color(0xFFEF4444), modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }
                    }

                    item {
                        Spacer(Modifier.height(8.dp))
                        Text("Active System Processes (${processes.size})", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Spacer(Modifier.height(4.dp))
                    }

                    items(processes) { proc ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF141820), RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Column {
                                Text("${proc.name} (PID ${proc.pid})", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = Color.White)
                                Text("CPU: ${proc.cpuPercent} · Mem: ${proc.memoryUsage} · ${proc.status}", fontSize = 10.5.sp, color = Color(0xFF94A3B8))
                            }
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = if (proc.status == "Active" || proc.status == "Running") PocketGreen.copy(alpha = 0.2f) else Color(0xFF334155),
                            ) {
                                Text(
                                    proc.status,
                                    fontSize = 9.5.sp,
                                    color = if (proc.status == "Active" || proc.status == "Running") PocketGreen else Color(0xFFCBD5E1),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onClose) { Text("Done") }
        }
    )
}

/**
 * Terminal Settings Sheet for skins, typography, isolation, profiles, and export/import.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TerminalSettingsModalSheet(
    skinConfig: TerminalSkinConfig,
    onUpdateSkin: (TerminalSkinConfig) -> Unit,
    profiles: List<TerminalProfile>,
    onAddProfile: (TerminalProfile) -> Unit,
    onDeleteProfile: (String) -> Unit,
    onResetProfile: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var currentSkin by remember { mutableStateOf(skinConfig) }
    var selectedTab by remember { mutableStateOf(0) } // 0: Skins, 1: Typography, 2: Profiles & Isolation

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Palette, contentDescription = null, tint = PocketOrange)
                    Spacer(Modifier.width(10.dp))
                    Text("Terminal Settings & Customization", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }

            Spacer(Modifier.height(10.dp))

            // Category selector
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterChip(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    label = { Text("Frame Skins") },
                )
                FilterChip(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    label = { Text("Typography") },
                )
                FilterChip(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    label = { Text("Profiles") },
                )
            }

            Spacer(Modifier.height(14.dp))

            when (selectedTab) {
                0 -> {
                    // Skins
                    Text("Select Terminal Frame Skin", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    Spacer(Modifier.height(8.dp))
                    LazyColumn(
                        modifier = Modifier.height(260.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(TerminalSkinStyle.entries) { style ->
                            val isSelected = currentSkin.style == style
                            Card(
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) PocketOrange.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                ),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (isSelected) PocketOrange else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        currentSkin = currentSkin.copy(style = style)
                                        onUpdateSkin(currentSkin)
                                    },
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    Column {
                                        Text(style.label, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        Text(style.description, fontSize = 11.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    if (isSelected) {
                                        Icon(Icons.Default.Terminal, contentDescription = null, tint = PocketOrange)
                                    }
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text("Show ASCII Startup Banner", fontSize = 13.sp)
                        Switch(
                            checked = currentSkin.showAsciiBanner,
                            onCheckedChange = {
                                currentSkin = currentSkin.copy(showAsciiBanner = it)
                                onUpdateSkin(currentSkin)
                            },
                        )
                    }
                }
                1 -> {
                    // Typography
                    Text("Font Family", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TerminalFontFamily.entries.forEach { family ->
                            FilterChip(
                                selected = currentSkin.fontFamily == family,
                                onClick = {
                                    currentSkin = currentSkin.copy(fontFamily = family)
                                    onUpdateSkin(currentSkin)
                                },
                                label = { Text(family.label) },
                            )
                        }
                    }

                    Spacer(Modifier.height(16.dp))
                    Text("Font Size: ${currentSkin.fontSizeSp.toInt()} sp", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    Slider(
                        value = currentSkin.fontSizeSp,
                        onValueChange = {
                            currentSkin = currentSkin.copy(fontSizeSp = it)
                            onUpdateSkin(currentSkin)
                        },
                        valueRange = 10f..20f,
                        steps = 9,
                    )

                    Spacer(Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text("Autocorrect Common Typos", fontSize = 13.sp)
                        Switch(
                            checked = currentSkin.autocorrectEnabled,
                            onCheckedChange = {
                                currentSkin = currentSkin.copy(autocorrectEnabled = it)
                                onUpdateSkin(currentSkin)
                            },
                        )
                    }
                }
                2 -> {
                    // Profiles & Isolation
                    var showNewProfileDialog by remember { mutableStateOf(false) }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text("Terminal Profiles", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        Button(
                            onClick = { showNewProfileDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = PocketOrange),
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("New Profile")
                        }
                    }

                    Spacer(Modifier.height(10.dp))

                    LazyColumn(
                        modifier = Modifier.height(240.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(profiles) { profile ->
                            Card(
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(profile.name, fontWeight = FontWeight.Bold, fontSize = 13.5.sp)
                                            if (profile.isIsolated) {
                                                Spacer(Modifier.width(6.dp))
                                                Surface(
                                                    shape = RoundedCornerShape(4.dp),
                                                    color = PocketOrange.copy(alpha = 0.2f),
                                                ) {
                                                    Text("Sandboxed", fontSize = 10.sp, color = PocketOrange, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                                                }
                                            }
                                        }

                                        Row {
                                            if (profile.customHomeDir != null) {
                                                TextButton(onClick = { onResetProfile(profile.id) }) {
                                                    Text("Reset", fontSize = 11.sp, color = Color(0xFFEAB308))
                                                }
                                            }
                                            if (profiles.size > 1) {
                                                IconButton(onClick = { onDeleteProfile(profile.id) }, modifier = Modifier.size(24.dp)) {
                                                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFEF4444), modifier = Modifier.size(16.dp))
                                                }
                                            }
                                        }
                                    }
                                    if (profile.customHomeDir != null) {
                                        Text("Home: ${profile.customHomeDir}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    if (!profile.startupScript.isNullOrBlank()) {
                                        Text("Startup: ${profile.startupScript}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }
                    }

                    if (showNewProfileDialog) {
                        var newName by remember { mutableStateOf("") }
                        var newHome by remember { mutableStateOf("") }
                        var newStartup by remember { mutableStateOf("") }
                        var isIsolated by remember { mutableStateOf(true) }

                        AlertDialog(
                            onDismissRequest = { showNewProfileDialog = false },
                            title = { Text("Create Sandboxed Profile") },
                            text = {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(
                                        value = newName,
                                        onValueChange = { newName = it },
                                        label = { Text("Profile Name (e.g. Node Sandbox)") },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth(),
                                    )
                                    OutlinedTextField(
                                        value = newHome,
                                        onValueChange = { newHome = it },
                                        label = { Text("Custom Home Dir (e.g. /root/node_env)") },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth(),
                                    )
                                    OutlinedTextField(
                                        value = newStartup,
                                        onValueChange = { newStartup = it },
                                        label = { Text("Startup Command (optional)") },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth(),
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                    ) {
                                        Text("Isolate environment", fontSize = 13.sp)
                                        Switch(checked = isIsolated, onCheckedChange = { isIsolated = it })
                                    }
                                }
                            },
                            confirmButton = {
                                Button(onClick = {
                                    if (newName.isNotBlank()) {
                                        onAddProfile(
                                            TerminalProfile(
                                                name = newName.trim(),
                                                customHomeDir = newHome.trim().ifBlank { null },
                                                startupScript = newStartup.trim().ifBlank { null },
                                                isIsolated = isIsolated,
                                            )
                                        )
                                        showNewProfileDialog = false
                                    }
                                }) {
                                    Text("Create")
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { showNewProfileDialog = false }) { Text("Cancel") }
                            }
                        )
                    }
                }
            }
        }
    }
}
