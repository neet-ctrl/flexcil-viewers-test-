package com.flexcilviewer.ui.components

import android.graphics.BitmapFactory
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.flexcilviewer.data.*
import com.flexcilviewer.ui.theme.*

// 3-state checkbox state for folders
private enum class FolderCheckState { ALL, NONE, PARTIAL }

private fun folderCheckState(folder: FolderNode, checkedDocs: Set<String>): FolderCheckState {
    val all = getAllDocuments(listOf(folder))
    if (all.isEmpty()) return FolderCheckState.NONE
    val checkedCount = all.count { (doc, path) -> "${path}/${doc.name}" in checkedDocs }
    return when (checkedCount) {
        0 -> FolderCheckState.NONE
        all.size -> FolderCheckState.ALL
        else -> FolderCheckState.PARTIAL
    }
}

private fun countAllFolders(folders: List<FolderNode>): Int =
    folders.sumOf { 1 + countAllFolders(it.subfolders) }

@Composable
fun SidebarContent(
    folders: List<FolderNode>,
    backupInfo: FlexBackupInfo,
    totalDocuments: Int,
    selectedDoc: FlexDocument?,
    selectedFolder: FolderNode?,
    checkedDocs: Set<String>,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onDocClick: (FlexDocument, String) -> Unit,
    onFolderClick: (FolderNode) -> Unit,
    onCheckToggle: (String) -> Unit,
    onCheckAll: (FolderNode) -> Unit,
    onDeselectAll: () -> Unit,
    onExportSelected: () -> Unit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier
) {
    val keyboardController = LocalSoftwareKeyboardController.current

    val searchResults: List<Pair<FlexDocument, String>> = remember(searchQuery, folders) {
        if (searchQuery.isBlank()) emptyList()
        else getAllDocuments(folders).filter { (doc, _) ->
            doc.name.contains(searchQuery, ignoreCase = true) ||
            (doc.info?.name?.contains(searchQuery, ignoreCase = true) == true)
        }
    }

    val totalFolders = remember(folders) { countAllFolders(folders) }

    Column(modifier = modifier.background(SurfaceDark)) {

        // ── Sidebar header (matches web: home button, backup name + date) ──
        Surface(
            color = CardDark,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onReset,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(Icons.Default.Home, contentDescription = "Open another file", tint = TextSecondary, modifier = Modifier.size(18.dp))
                }
                Spacer(Modifier.width(6.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        backupInfo.appName.ifBlank { "Flexcil Backup" },
                        style = MaterialTheme.typography.titleSmall,
                        color = TextPrimary,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (backupInfo.backupDate.isNotBlank()) {
                        Text(
                            backupInfo.backupDate,
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        // ── Search bar ──
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            placeholder = { Text("Search documents…", style = MaterialTheme.typography.bodyMedium, color = TextMuted) },
            leadingIcon = {
                Icon(Icons.Default.Search, contentDescription = null, tint = TextMuted, modifier = Modifier.size(18.dp))
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchQueryChange("") }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear search", tint = TextMuted, modifier = Modifier.size(18.dp))
                    }
                }
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { keyboardController?.hide() }),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary,
                cursorColor = PrimaryIndigoLight,
                focusedBorderColor = PrimaryIndigo,
                unfocusedBorderColor = DividerColor,
                focusedContainerColor = SurfaceVariantDark,
                unfocusedContainerColor = SurfaceVariantDark
            ),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            textStyle = MaterialTheme.typography.bodyMedium
        )

        // ── "N selected" banner ──
        if (checkedDocs.isNotEmpty()) {
            Surface(
                color = PrimaryIndigoDark.copy(alpha = 0.3f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "${checkedDocs.size} selected — tap Export",
                        color = PrimaryIndigoLight,
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(
                        onClick = onExportSelected,
                        contentPadding = PaddingValues(horizontal = 8.dp)
                    ) {
                        Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(16.dp), tint = PrimaryIndigoLight)
                        Spacer(Modifier.width(4.dp))
                        Text("Export", color = PrimaryIndigoLight, style = MaterialTheme.typography.labelLarge)
                    }
                    TextButton(
                        onClick = onDeselectAll,
                        contentPadding = PaddingValues(horizontal = 4.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(14.dp), tint = TextSecondary)
                    }
                }
            }
        }

        HorizontalDivider(color = DividerColor)

        // ── Tree or search results ──
        if (searchQuery.isNotBlank()) {
            if (searchResults.isEmpty()) {
                Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.SearchOff, contentDescription = null, tint = TextMuted, modifier = Modifier.size(32.dp))
                        Spacer(Modifier.height(8.dp))
                        Text("No results for \"$searchQuery\"", color = TextMuted, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            } else {
                Text(
                    "${searchResults.size} result${if (searchResults.size != 1) "s" else ""}",
                    color = TextMuted,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                )
                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(searchResults, key = { (doc, path) -> "$path/${doc.name}" }) { (doc, path) ->
                        DocumentRow(
                            doc = doc,
                            folderPath = path,
                            indent = 12.dp,
                            isSelected = selectedDoc?.name == doc.name,
                            isChecked = "$path/${doc.name}" in checkedDocs,
                            onDocClick = onDocClick,
                            onCheckToggle = { onCheckToggle("$path/${doc.name}") }
                        )
                        HorizontalDivider(color = DividerColor, thickness = 0.5.dp)
                    }
                }
            }
        } else {
            LazyColumn(modifier = Modifier.weight(1f)) {
                for (folder in folders) {
                    item(key = folder.fullPath) {
                        FolderTreeNode(
                            folder = folder,
                            depth = 0,
                            selectedDoc = selectedDoc,
                            selectedFolder = selectedFolder,
                            checkedDocs = checkedDocs,
                            onDocClick = onDocClick,
                            onFolderClick = onFolderClick,
                            onCheckToggle = onCheckToggle,
                            onCheckAll = onCheckAll
                        )
                    }
                }
            }
        }

        // ── Footer (matches web: "N documents · N folders" + version) ──
        HorizontalDivider(color = DividerColor)
        Surface(color = CardDark, modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                Text(
                    "$totalDocuments documents · $totalFolders folders",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary
                )
                if (backupInfo.appVersion.isNotBlank()) {
                    Text(
                        "Flexcil v${backupInfo.appVersion}",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted
                    )
                }
            }
        }
    }
}

@Composable
private fun FolderTreeNode(
    folder: FolderNode,
    depth: Int,
    selectedDoc: FlexDocument?,
    selectedFolder: FolderNode?,
    checkedDocs: Set<String>,
    onDocClick: (FlexDocument, String) -> Unit,
    onFolderClick: (FolderNode) -> Unit,
    onCheckToggle: (String) -> Unit,
    onCheckAll: (FolderNode) -> Unit
) {
    var expanded by remember(folder.fullPath) { mutableStateOf(true) }
    val indent: Dp = (depth * 14).dp
    val checkState = folderCheckState(folder, checkedDocs)
    val docCount = folder.totalDocuments

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (selectedFolder?.fullPath == folder.fullPath) SelectedBg else SurfaceDark)
            .padding(start = 4.dp + indent, end = 4.dp, top = 2.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 3-state folder checkbox
        IconButton(
            onClick = { onCheckAll(folder) },
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                when (checkState) {
                    FolderCheckState.ALL -> Icons.Default.CheckBox
                    FolderCheckState.PARTIAL -> Icons.Default.IndeterminateCheckBox
                    FolderCheckState.NONE -> Icons.Default.CheckBoxOutlineBlank
                },
                contentDescription = null,
                tint = when (checkState) {
                    FolderCheckState.NONE -> TextMuted
                    else -> PrimaryIndigoLight
                },
                modifier = Modifier.size(16.dp)
            )
        }

        // Folder name + expand toggle
        Row(
            modifier = Modifier
                .weight(1f)
                .clickable {
                    expanded = !expanded
                    onFolderClick(folder)
                }
                .padding(vertical = 8.dp, horizontal = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                if (expanded) Icons.Default.ExpandMore else Icons.Default.ChevronRight,
                contentDescription = null,
                tint = TextSecondary,
                modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.width(4.dp))
            Icon(
                if (expanded) Icons.Default.FolderOpen else Icons.Default.Folder,
                contentDescription = null,
                tint = PrimaryIndigoLight,
                modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.width(6.dp))
            Text(
                folder.name,
                style = MaterialTheme.typography.bodyMedium,
                color = TextPrimary,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            // Doc count pill (matches web)
            Surface(
                color = SurfaceVariantDark,
                shape = RoundedCornerShape(50)
            ) {
                Text(
                    "$docCount",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }
    }

    AnimatedVisibility(
        visible = expanded,
        enter = expandVertically(),
        exit = shrinkVertically()
    ) {
        Column {
            for (sub in folder.subfolders) {
                FolderTreeNode(
                    folder = sub,
                    depth = depth + 1,
                    selectedDoc = selectedDoc,
                    selectedFolder = selectedFolder,
                    checkedDocs = checkedDocs,
                    onDocClick = onDocClick,
                    onFolderClick = onFolderClick,
                    onCheckToggle = onCheckToggle,
                    onCheckAll = onCheckAll
                )
            }
            for (doc in folder.documents) {
                DocumentRow(
                    doc = doc,
                    folderPath = folder.fullPath,
                    indent = indent + 28.dp,
                    isSelected = selectedDoc?.name == doc.name,
                    isChecked = "${folder.fullPath}/${doc.name}" in checkedDocs,
                    onDocClick = onDocClick,
                    onCheckToggle = { onCheckToggle("${folder.fullPath}/${doc.name}") }
                )
            }
        }
    }
    HorizontalDivider(color = DividerColor, thickness = 0.5.dp)
}

@Composable
private fun DocumentRow(
    doc: FlexDocument,
    folderPath: String,
    indent: Dp,
    isSelected: Boolean,
    isChecked: Boolean,
    onDocClick: (FlexDocument, String) -> Unit,
    onCheckToggle: () -> Unit
) {
    val thumbBitmap = remember(doc.thumbnail) {
        doc.thumbnail?.let { bytes ->
            runCatching { BitmapFactory.decodeByteArray(bytes, 0, bytes.size) }.getOrNull()
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (isSelected) SelectedBg else SurfaceDark)
            .padding(start = indent, end = 8.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.Top
    ) {
        // Per-doc checkbox
        Checkbox(
            checked = isChecked,
            onCheckedChange = { onCheckToggle() },
            colors = CheckboxDefaults.colors(
                checkedColor = PrimaryIndigo,
                uncheckedColor = TextMuted
            ),
            modifier = Modifier.size(20.dp).padding(top = 6.dp)
        )
        Spacer(Modifier.width(6.dp))

        // Clickable area
        Row(
            modifier = Modifier
                .weight(1f)
                .clickable { onDocClick(doc, folderPath) }
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Thumbnail or icon
            if (thumbBitmap != null) {
                Image(
                    bitmap = thumbBitmap.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(CardDark),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(SurfaceVariantDark),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        if (doc.pdfData != null) Icons.Default.PictureAsPdf else Icons.Default.Description,
                        contentDescription = null,
                        tint = if (doc.pdfData != null) PrimaryIndigoLight else TextMuted,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(Modifier.width(8.dp))

            Column(Modifier.weight(1f)) {
                Text(
                    doc.info?.name?.takeIf { it.isNotBlank() } ?: doc.name,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isSelected) PrimaryIndigoLight else TextPrimary,
                    fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                // Modified date (matches web)
                doc.info?.modifiedDate?.takeIf { it > 0 }?.let {
                    Text(
                        formatDate(it),
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted,
                        maxLines = 1
                    )
                }
                // PDF badge (matches web)
                if (doc.pdfData != null) {
                    Spacer(Modifier.height(2.dp))
                    Surface(
                        color = PrimaryIndigoDark.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            "PDF",
                            style = MaterialTheme.typography.labelSmall,
                            color = PrimaryIndigoLight,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                        )
                    }
                }
            }
        }
    }
}
