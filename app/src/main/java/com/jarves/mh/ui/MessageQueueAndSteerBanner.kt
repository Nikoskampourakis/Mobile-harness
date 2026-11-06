package com.jarves.mh.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Queue
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jarves.mh.model.QueuedFollowUp
import com.jarves.mh.model.SteerInstruction
import com.jarves.mh.model.SteerStatus
import com.jarves.mh.ui.theme.PocketGreen
import com.jarves.mh.ui.theme.PocketOrange

@Composable
fun MessageQueueAndSteerBanner(
    queuedMessages: List<QueuedFollowUp>,
    activeSteers: List<SteerInstruction>,
    isSteerMode: Boolean,
    isQueuePaused: Boolean,
    isRunning: Boolean,
    onToggleSteerMode: () -> Unit,
    onToggleQueuePause: () -> Unit,
    onEditQueued: (id: String, newPrompt: String) -> Unit,
    onSendNextQueued: (id: String) -> Unit,
    onSteerQueued: (id: String) -> Unit,
    onRemoveQueued: (id: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expandedQueue by rememberSaveable { mutableStateOf(false) }
    var editingItem by remember { mutableStateOf<QueuedFollowUp?>(null) }
    var editDraftText by remember { mutableStateOf("") }

    if (editingItem != null) {
        AlertDialog(
            onDismissRequest = { editingItem = null },
            title = { Text("Edit Queued Message") },
            text = {
                OutlinedTextField(
                    value = editDraftText,
                    onValueChange = { editDraftText = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Message Prompt") },
                    maxLines = 6,
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        editingItem?.let { item ->
                            if (editDraftText.isNotBlank()) {
                                onEditQueued(item.id, editDraftText.trim())
                            }
                        }
                        editingItem = null
                    },
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { editingItem = null }) {
                    Text("Cancel")
                }
            },
        )
    }

    Column(modifier = modifier.fillMaxWidth()) {
        // Active Steering Instructions Badges
        if (activeSteers.isNotEmpty()) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp),
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Navigation,
                            contentDescription = null,
                            tint = Color(0xFF38BDF8),
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "In-Flight Steering Instructions",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    activeSteers.forEach { steer ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                steer.text,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f),
                            )
                            Spacer(Modifier.width(8.dp))
                            SteerStatusBadge(steer.status)
                        }
                    }
                }
            }
        }

        // Steer Mode Active Bar
        if (isSteerMode && isRunning) {
            Surface(
                color = Color(0xFF38BDF8).copy(alpha = 0.12f),
                shape = RoundedCornerShape(8.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = 0.4f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 2.dp),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Icon(
                            Icons.Default.Navigation,
                            null,
                            tint = Color(0xFF38BDF8),
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Steer Mode Active: Instruction will guide agent live",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF0284C7),
                        )
                    }
                    IconButton(onClick = onToggleSteerMode, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Close, "Exit Steer Mode", tint = Color(0xFF0284C7), modifier = Modifier.size(14.dp))
                    }
                }
            }
        }

        // Message Queue Bar
        if (queuedMessages.isNotEmpty()) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp),
            ) {
                Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
                    // Header Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { expandedQueue = !expandedQueue },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Queue,
                                null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "Message Queue (${queuedMessages.size})",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            if (isQueuePaused) {
                                Spacer(Modifier.width(6.dp))
                                Surface(
                                    shape = CircleShape,
                                    color = PocketOrange.copy(alpha = 0.15f),
                                ) {
                                    Text(
                                        "Paused",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = PocketOrange,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp),
                                    )
                                }
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = onToggleQueuePause,
                                modifier = Modifier.size(30.dp),
                            ) {
                                Icon(
                                    if (isQueuePaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                                    if (isQueuePaused) "Resume Queue" else "Pause Queue",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp),
                                )
                            }
                            IconButton(
                                onClick = { expandedQueue = !expandedQueue },
                                modifier = Modifier.size(30.dp),
                            ) {
                                Icon(
                                    if (expandedQueue) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                    "Toggle expand",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                        }
                    }

                    // Expanded Queue List
                    AnimatedVisibility(visible = expandedQueue) {
                        Column(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                            Spacer(Modifier.height(6.dp))
                            queuedMessages.forEachIndexed { index, item ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                        modifier = Modifier.size(20.dp),
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                "${index + 1}",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary,
                                            )
                                        }
                                    }
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        item.prompt,
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f),
                                    )
                                    Spacer(Modifier.width(4.dp))

                                    // Send Next (Promote to front)
                                    if (index > 0) {
                                        IconButton(
                                            onClick = { onSendNextQueued(item.id) },
                                            modifier = Modifier.size(28.dp),
                                        ) {
                                            Icon(Icons.Default.HourglassTop, "Send next", Modifier.size(14.dp), tint = Color(0xFF38BDF8))
                                        }
                                    }

                                    // Steer immediately
                                    if (isRunning) {
                                        IconButton(
                                            onClick = { onSteerQueued(item.id) },
                                            modifier = Modifier.size(28.dp),
                                        ) {
                                            Icon(Icons.Default.Navigation, "Steer now", Modifier.size(14.dp), tint = PocketGreen)
                                        }
                                    }

                                    // Edit
                                    IconButton(
                                        onClick = {
                                            editDraftText = item.prompt
                                            editingItem = item
                                        },
                                        modifier = Modifier.size(28.dp),
                                    ) {
                                        Icon(Icons.Default.Edit, "Edit message", Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }

                                    // Delete
                                    IconButton(
                                        onClick = { onRemoveQueued(item.id) },
                                        modifier = Modifier.size(28.dp),
                                    ) {
                                        Icon(Icons.Default.Delete, "Delete from queue", Modifier.size(14.dp), tint = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SteerStatusBadge(status: SteerStatus) {
    val (label, bg, fg) = when (status) {
        SteerStatus.WAITING -> Triple("Waiting", PocketOrange.copy(alpha = 0.15f), PocketOrange)
        SteerStatus.DELIVERED -> Triple("Delivered", Color(0xFF38BDF8).copy(alpha = 0.15f), Color(0xFF0284C7))
        SteerStatus.ACCEPTED -> Triple("Accepted", PocketGreen.copy(alpha = 0.15f), PocketGreen)
    }
    Surface(
        shape = CircleShape,
        color = bg,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(fg),
            )
            Spacer(Modifier.width(4.dp))
            Text(label, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = fg)
        }
    }
}
