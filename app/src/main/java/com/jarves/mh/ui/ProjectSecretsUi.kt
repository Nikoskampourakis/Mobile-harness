package com.jarves.mh.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jarves.mh.data.ProjectSecretEntry
import com.jarves.mh.ui.theme.PocketGreen
import com.jarves.mh.ui.theme.PocketOrange

/**
 * Modal bottom sheet for viewing, adding, editing, and deleting encrypted API keys
 * and secrets for specific app projects.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectSecretsModalSheet(
    projectName: String,
    projectId: String,
    secrets: List<ProjectSecretEntry>,
    onSaveSecret: (keyName: String, value: String) -> Unit,
    onRemoveSecret: (keyName: String) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showAddDialog by rememberSaveable { mutableStateOf(false) }
    var editingSecret by remember { mutableStateOf<ProjectSecretEntry?>(null) }
    val revealedKeys = remember { mutableStateListOf<String>() }
    val clipboard = LocalClipboardManager.current

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 36.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .size(36.dp)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Default.VpnKey, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                    }
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text("App APIs & Secrets", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(projectName, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Button(
                    onClick = { showAddDialog = true },
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(8.dp),
                ) {
                    Icon(Icons.Default.Add, null, Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Add Key", fontSize = 12.sp)
                }
            }

            Spacer(Modifier.height(14.dp))

            // Encryption badge
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Lock, null, tint = PocketGreen, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Encrypted space: Keys are securely stored in hardware-backed Android Keystore and masked.",
                        fontSize = 11.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            if (secrets.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.KeyOff, null, Modifier.size(36.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                        Spacer(Modifier.height(8.dp))
                        Text("No API keys saved for this app", fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(4.dp))
                        Text("Add keys like GEMINI_API_KEY, STRIPE_KEY, or SUPABASE_KEY", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    items(secrets, key = { it.keyName }) { secret ->
                        val isRevealed = secret.keyName in revealedKeys
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        text = secret.keyName,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.primary,
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        text = if (isRevealed) secret.secretValue else secret.maskedValue,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(
                                        onClick = {
                                            if (isRevealed) revealedKeys.remove(secret.keyName)
                                            else revealedKeys.add(secret.keyName)
                                        },
                                        modifier = Modifier.size(32.dp),
                                    ) {
                                        Icon(
                                            if (isRevealed) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                            contentDescription = "Toggle reveal",
                                            modifier = Modifier.size(16.dp),
                                        )
                                    }
                                    IconButton(
                                        onClick = {
                                            clipboard.setText(AnnotatedString(secret.secretValue))
                                        },
                                        modifier = Modifier.size(32.dp),
                                    ) {
                                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy key", modifier = Modifier.size(16.dp))
                                    }
                                    IconButton(
                                        onClick = { editingSecret = secret },
                                        modifier = Modifier.size(32.dp),
                                    ) {
                                        Icon(Icons.Default.Edit, contentDescription = "Edit key", modifier = Modifier.size(16.dp))
                                    }
                                    IconButton(
                                        onClick = { onRemoveSecret(secret.keyName) },
                                        modifier = Modifier.size(32.dp),
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete key", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        SecretEditorDialog(
            title = "Add API Key for $projectName",
            initialKey = "",
            initialVal = "",
            onConfirm = { key, value ->
                showAddDialog = false
                onSaveSecret(key, value)
            },
            onDismiss = { showAddDialog = false },
        )
    }

    editingSecret?.let { secret ->
        SecretEditorDialog(
            title = "Edit ${secret.keyName}",
            initialKey = secret.keyName,
            initialVal = secret.secretValue,
            isKeyNameEditable = false,
            onConfirm = { key, value ->
                editingSecret = null
                onSaveSecret(key, value)
            },
            onDismiss = { editingSecret = null },
        )
    }
}

/**
 * Interactive dialog shown when the AI agent requests an API key from the user
 * to perform specific work for an application.
 */
@Composable
fun AgentApiKeyRequestDialog(
    keyName: String,
    reason: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var secretValue by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.VpnKey, null, tint = PocketOrange, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(8.dp))
                Text("API Key Required", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "The coding agent requires an API key for: $keyName",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                )
                if (reason.isNotBlank()) {
                    Text(
                        text = reason,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                OutlinedTextField(
                    value = secretValue,
                    onValueChange = { secretValue = it },
                    label = { Text(keyName) },
                    placeholder = { Text("Enter secret key or token...") },
                    singleLine = true,
                    visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                            Icon(
                                if (isPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    "This key will be encrypted and saved in your secure project space.",
                    fontSize = 11.sp,
                    color = PocketGreen,
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { if (secretValue.isNotBlank()) onConfirm(secretValue) },
                enabled = secretValue.isNotBlank(),
            ) {
                Text("Save & Continue")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}

@Composable
private fun SecretEditorDialog(
    title: String,
    initialKey: String,
    initialVal: String,
    isKeyNameEditable: Boolean = true,
    onConfirm: (keyName: String, value: String) -> Unit,
    onDismiss: () -> Unit,
) {
    var keyName by rememberSaveable { mutableStateOf(initialKey) }
    var secretValue by rememberSaveable { mutableStateOf(initialVal) }
    var isPasswordVisible by rememberSaveable { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (isKeyNameEditable) {
                    OutlinedTextField(
                        value = keyName,
                        onValueChange = { keyName = it.uppercase() },
                        label = { Text("Key Variable Name") },
                        placeholder = { Text("e.g. GEMINI_API_KEY") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                OutlinedTextField(
                    value = secretValue,
                    onValueChange = { secretValue = it },
                    label = { Text("Secret Value") },
                    placeholder = { Text("sk-...") },
                    singleLine = true,
                    visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                            Icon(if (isPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility, null, Modifier.size(18.dp))
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { if (keyName.isNotBlank() && secretValue.isNotBlank()) onConfirm(keyName, secretValue) },
                enabled = keyName.isNotBlank() && secretValue.isNotBlank(),
            ) {
                Text("Save Secret")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}
