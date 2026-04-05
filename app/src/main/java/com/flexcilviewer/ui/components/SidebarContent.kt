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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.flexcilviewer.data.FlexDocument
import com.flexcilviewer.data.FolderNode
import com.flexcilviewer.data.getAllDocuments
import com.flexcilviewer.data.formatDate
import com.flexcilviewer.data.formatFileSize
import com.flexcilviewer.ui.theme.*

@Composable
fun SidebarContent(
    folders: List<FolderNode>,
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
    modifier: Modifier = Modifier
) {
    val keyboardController = LocalSoftwareKeyboardController.current

    val searchResults: List<Pair<FlexDocument, String>> = remember(searchQuery, folders) {
        if (searchQuery.isBlank()) emptyList()
        else getAllDocuments(folders).filter { (doc, _) ->
            doc.name.contains(searchQuery, ignoreCase = true)
        }
    }

    Column(modifier = modifier.background(SurfaceDark)) {
        // Header
        Surface(color = CardDark, modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.FolderOpen, contentDescription = null, tint = PrimaryIndigoLight, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    "Folders",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    modifier = Modifier.weight(1f)
                )
                if (checkedDocs.isNotEmpty()) {
                    TextButton(onClick = onDeselectAll, contentPadding = PaddingValues(horizontal = 4.dp)) {
                        Text("Clear ${checkedDocs.size}", color = TextSecondary, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }

        // Search bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            placeholder = { Text("Search documents…", style = MaterialTheme.typography.bodyMedium, color = TextMuted) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextMuted, modifier = Modifier.size(18.dp)) },
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

        if (checkedDocs.isNotEmpty()) {
            Surface(color = PrimaryIndigoDark.copy(alpha = 0.3f), modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "${checkedDocs.size} selected",
                        color = PrimaryIndigoLight,
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = onExportSelected, contentPadding = PaddingValues(horizontal = 8.dp)) {
                        Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(16.dp), tint = PrimaryIndigoLight)
                        Spacer(Modifier.width(4.dp))
                        Text("Export", color = PrimaryIndigoLight, style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }

        HorizontalDivider(color = DividerColor)

        if (searchQuery.isNotBlank()) {
            // Search results — flat list
            if (searchResults.isEmpty()) {
                Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.SearchOff, contentDescription = null, tint = TextMuted, modifier = Modifier.size(32.dp))
                        Spacer(Modifier.height(8.dp))
                        Text("No results for "$searchQuery"", color = TextMuted, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            } else {
                Text(
                    "${searchResults.size} result${if (searchResults.size != 1) "s" else ""}",
                    color = TextMuted,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                )
                LazyColumn(modifier = Modifier.fillMaxSize()) {
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
            // Normal tree view
            LazyColumn(modifier = Modifier.fillMaxSize()) {
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
    var expanded by remember(folder.fullPath) { mutableStateOf(depth == 0) }
    val indent: Dp = (depth * 16).dp

    // Folder header row
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                expanded = !expanded
                onFolderClick(folder)
            }
            .background(
                if (selectedFolder?.fullPath == folder.fullPath) SelectedBg else SurfaceDark
            )
            .padding(start = 8.dp + indent, end = 8.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            if (expanded) Icons.Default.ExpandMore else Icons.Default.ChevronRight,
            contentDescription = null,
            tint = TextSecondary,
            modifier = Modifier.size(18.dp)
        )
        Spacer(Modifier.width(4.dp))
        Icon(
            if (expanded) Icons.Default.FolderOpen else Icons.Default.Folder,
            contentDescription = null,
            tint = PrimaryIndigoLight,
            modifier = Modifier.size(18.dp)
        )
        Spacer(Modifier.width(8.dp))
        Column(Modifier.weight(1f)) {
            Text(
                folder.name,
                style = MaterialTheme.typography.bodyMedium,
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                "${folder.totalDocuments} docs",
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted
            )
        }
        IconButton(onClick = { onCheckAll(folder) }, modifier = Modifier.size(28.dp)) {
            Icon(Icons.Default.CheckBox, contentDescription = "Select all", tint = TextMuted, modifier = Modifier.size(16.dp))
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
                    indent = indent + 24.dp,
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
            .clickable { onDocClick(doc, folderPath) }
            .background(if (isSelected) SelectedBg else SurfaceDark)
            .padding(start = indent, end = 8.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = isChecked,
            onCheckedChange = { onCheckToggle() },
            colors = CheckboxDefaults.colors(
                checkedColor = PrimaryIndigo,
                uncheckedColor = TextMuted
            ),
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.width(8.dp))

        if (thumbBitmap != null) {
            Image(
                bitmap = thumbBitmap.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(CardDark),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(
                Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(SurfaceVariantDark),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (doc.pdfData != null) Icons.Default.PictureAsPdf else Icons.Default.Description,
                    contentDescription = null,
                    tint = PrimaryIndigoLight,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Spacer(Modifier.width(8.dp))
        Column(Modifier.weight(1f)) {
            Text(
                doc.name,
                style = MaterialTheme.typography.bodyMedium,
                color = TextPrimary.copy(alpha = if (isSelected) 1f else 0.9f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                buildString {
                    append(formatFileSize(doc.flxSize))
                    doc.info?.modifiedDate?.takeIf { it > 0 }?.let { append("  •  ${formatDate(it)}") }
                },
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
