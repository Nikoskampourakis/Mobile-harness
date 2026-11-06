package com.jarves.mh.ui

import android.graphics.BitmapFactory
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.NoteAdd
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jarves.mh.model.ChangeItem
import com.jarves.mh.model.ChangeKind
import com.jarves.mh.model.Project
import com.jarves.mh.model.WorkspaceEntry
import com.jarves.mh.ui.theme.PocketGreen
import com.jarves.mh.ui.theme.PocketOrange
import java.io.File

enum class ArtifactFilter(val label: String, val icon: ImageVector) {
    ALL("All", Icons.Default.Layers),
    CREATED("Created", Icons.Default.NoteAdd),
    MODIFIED("Modified", Icons.Default.EditNote),
    IMAGES("Images", Icons.Default.Image),
    DOCS("Docs", Icons.Default.Description),
    CODE("Code", Icons.Default.Code),
    BUILDS("Builds", Icons.Default.Build),
}

data class ArtifactItem(
    val name: String,
    val relativePath: String,
    val absolutePath: String,
    val isDirectory: Boolean,
    val sizeBytes: Long,
    val changeKind: ChangeKind? = null,
    val extension: String,
)

@Composable
fun ProjectArtifactsView(
    project: Project?,
    files: List<WorkspaceEntry>,
    changes: List<ChangeItem>,
    onOpenFile: (WorkspaceEntry) -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var selectedFilter by rememberSaveable { mutableStateOf(ArtifactFilter.ALL) }
    val clipboardManager = LocalClipboardManager.current
    var previewImagePath by remember { mutableStateOf<String?>(null) }

    val artifacts = remember(files, changes, project) {
        val changeMap = changes.associateBy { it.path }
        files.filterNot { it.isDirectory && (it.name == ".git" || it.name == "node_modules" || it.name == ".gradle" || it.name == "build-cache") }
            .map { entry ->
                val change = changeMap[entry.path]
                val ext = entry.name.substringAfterLast('.', "").lowercase()
                ArtifactItem(
                    name = entry.name,
                    relativePath = entry.path,
                    absolutePath = entry.path,
                    isDirectory = entry.isDirectory,
                    sizeBytes = entry.sizeBytes,
                    changeKind = change?.changeKind,
                    extension = ext,
                )
            }
    }

    val filtered = remember(artifacts, selectedFilter, changes) {
        when (selectedFilter) {
            ArtifactFilter.ALL -> artifacts
            ArtifactFilter.CREATED -> artifacts.filter {
                it.changeKind == ChangeKind.CREATED || changes.any { c -> c.path == it.relativePath && c.changeKind == ChangeKind.CREATED }
            }
            ArtifactFilter.MODIFIED -> artifacts.filter {
                it.changeKind == ChangeKind.MODIFIED || changes.any { c -> c.path == it.relativePath && c.changeKind == ChangeKind.MODIFIED }
            }
            ArtifactFilter.IMAGES -> artifacts.filter {
                it.extension in setOf("png", "jpg", "jpeg", "webp", "gif", "svg", "bmp", "ico")
            }
            ArtifactFilter.DOCS -> artifacts.filter {
                it.extension in setOf("md", "txt", "pdf", "json", "yaml", "yml", "toml", "xml", "csv")
            }
            ArtifactFilter.CODE -> artifacts.filter {
                it.extension in setOf("kt", "java", "rs", "py", "js", "ts", "tsx", "jsx", "cpp", "c", "h", "go", "swift", "php", "html", "css", "sh")
            }
            ArtifactFilter.BUILDS -> artifacts.filter {
                it.extension in setOf("apk", "aab", "jar", "so", "tar", "gz", "zip") ||
                    it.relativePath.contains("/build/") || it.relativePath.contains("/dist/") || it.relativePath.contains("/target/")
            }
        }
    }

    Column(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        // Top filter bar
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 1.dp,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp)
                    .horizontalScroll(rememberScrollState()),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                ArtifactFilter.entries.forEach { filter ->
                    val isSelected = filter == selectedFilter
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedFilter = filter },
                        label = { Text(filter.label, fontSize = 12.sp) },
                        leadingIcon = {
                            Icon(filter.icon, null, Modifier.size(14.dp))
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        ),
                    )
                }
                Spacer(Modifier.width(4.dp))
                IconButton(onClick = onRefresh, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Refresh, "Refresh artifacts", Modifier.size(16.dp))
                }
            }
        }

        if (filtered.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(32.dp),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        selectedFilter.icon,
                        null,
                        Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "No ${selectedFilter.label.lowercase()} artifacts found",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Files generated or modified by agents will appear here",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                item {
                    Text(
                        "${filtered.size} ${if (filtered.size == 1) "item" else "items"}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                    )
                }

                items(filtered, key = { it.relativePath }) { item ->
                    ArtifactCard(
                        item = item,
                        onOpen = {
                            onOpenFile(
                                WorkspaceEntry(
                                    path = item.relativePath,
                                    name = item.name,
                                    isDirectory = item.isDirectory,
                                    depth = 0,
                                    sizeBytes = item.sizeBytes,
                                ),
                            )
                        },
                        onCopyPath = {
                            clipboardManager.setText(AnnotatedString(item.relativePath))
                        },
                        onPreviewImage = {
                            if (item.extension in setOf("png", "jpg", "jpeg", "webp", "bmp")) {
                                previewImagePath = item.absolutePath
                            }
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun ArtifactCard(
    item: ArtifactItem,
    onOpen: () -> Unit,
    onCopyPath: () -> Unit,
    onPreviewImage: () -> Unit,
) {
    val isImage = item.extension in setOf("png", "jpg", "jpeg", "webp", "bmp", "svg")
    val isCreated = item.changeKind == ChangeKind.CREATED
    val isModified = item.changeKind == ChangeKind.MODIFIED

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpen),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        ),
        border = BorderStroke(
            1.dp,
            when {
                isCreated -> PocketGreen.copy(alpha = 0.4f)
                isModified -> PocketOrange.copy(alpha = 0.4f)
                else -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
            },
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Icon / Thumbnail
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        when {
                            isImage -> Color(0xFFE0E7FF)
                            item.isDirectory -> Color(0xFFFEF3C7)
                            isCreated -> PocketGreen.copy(alpha = 0.15f)
                            isModified -> PocketOrange.copy(alpha = 0.15f)
                            else -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                        },
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = when {
                        item.isDirectory -> Icons.Default.Folder
                        isImage -> Icons.Default.Image
                        item.extension in setOf("apk", "aab") -> Icons.Default.Build
                        item.extension in setOf("zip", "tar", "gz") -> Icons.Default.FolderZip
                        item.extension in setOf("kt", "java", "rs", "py", "js", "ts", "cpp", "c") -> Icons.Default.Code
                        else -> Icons.Default.Description
                    },
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = when {
                        isCreated -> PocketGreen
                        isModified -> PocketOrange
                        isImage -> Color(0xFF4F46E5)
                        item.isDirectory -> Color(0xFFD97706)
                        else -> MaterialTheme.colorScheme.primary
                    },
                )
            }

            Spacer(Modifier.width(10.dp))

            // File details
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        item.name,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    if (isCreated) {
                        Spacer(Modifier.width(6.dp))
                        Surface(
                            shape = CircleShape,
                            color = PocketGreen.copy(alpha = 0.15f),
                            modifier = Modifier.padding(horizontal = 4.dp),
                        ) {
                            Text("New", fontSize = 10.sp, color = PocketGreen, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp))
                        }
                    } else if (isModified) {
                        Spacer(Modifier.width(6.dp))
                        Surface(
                            shape = CircleShape,
                            color = PocketOrange.copy(alpha = 0.15f),
                            modifier = Modifier.padding(horizontal = 4.dp),
                        ) {
                            Text("Modified", fontSize = 10.sp, color = PocketOrange, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp))
                        }
                    }
                }
                Spacer(Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        item.relativePath,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    if (item.sizeBytes > 0) {
                        Spacer(Modifier.width(6.dp))
                        Text(
                            formatArtifactSize(item.sizeBytes),
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        )
                    }
                }
            }

            // Quick actions
            IconButton(onClick = onCopyPath, modifier = Modifier.size(30.dp)) {
                Icon(Icons.Default.ContentCopy, "Copy path", Modifier.size(15.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = onOpen, modifier = Modifier.size(30.dp)) {
                Icon(Icons.Default.OpenInNew, "Open in editor", Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

private fun formatArtifactSize(bytes: Long): String = when {
    bytes < 1024 -> "$bytes B"
    bytes < 1024 * 1024 -> "${bytes / 1024} KB"
    else -> String.format(java.util.Locale.US, "%.1f MB", bytes.toDouble() / (1024 * 1024))
}
