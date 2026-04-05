package com.flexcilviewer.ui.components

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.flexcilviewer.data.FlexDocument
import com.flexcilviewer.data.formatDate
import com.flexcilviewer.data.formatFileSize
import com.flexcilviewer.ui.theme.*

private enum class ViewerTab(val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    PDF("PDF", Icons.Default.PictureAsPdf),
    PREVIEW("Preview", Icons.Default.Image),
    ANNOTATIONS("Annotations", Icons.Default.Draw),
    DETAILS("Details", Icons.Default.Info)
}

@Composable
fun DocumentViewer(
    doc: FlexDocument,
    onExportClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember(doc) {
        mutableStateOf(if (doc.pdfData != null) ViewerTab.PDF else ViewerTab.DETAILS)
    }

    Column(modifier = modifier.background(BackgroundDark)) {
        // Top bar
        Surface(
            color = SurfaceDark,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 8.dp, top = 12.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = doc.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary,
                        maxLines = 2
                    )
                    doc.info?.let {
                        Text(
                            text = "Modified: ${formatDate(it.modifiedDate)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                    }
                }
                IconButton(onClick = onExportClick) {
                    Icon(Icons.Default.FileDownload, contentDescription = "Export", tint = PrimaryIndigoLight)
                }
            }
        }

        // Tab row
        Surface(color = SurfaceDark, modifier = Modifier.fillMaxWidth()) {
            ScrollableTabRow(
                selectedTabIndex = selectedTab.ordinal,
                containerColor = SurfaceDark,
                contentColor = PrimaryIndigoLight,
                edgePadding = 0.dp,
                indicator = { tabPositions ->
                    if (selectedTab.ordinal < tabPositions.size) {
                        TabRowDefaults.SecondaryIndicator(
                            modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab.ordinal]),
                            color = PrimaryIndigo
                        )
                    }
                },
                divider = { HorizontalDivider(color = DividerColor) }
            ) {
                ViewerTab.entries.forEach { tab ->
                    val enabled = when (tab) {
                        ViewerTab.PDF -> doc.pdfData != null
                        ViewerTab.PREVIEW -> doc.thumbnail != null
                        ViewerTab.ANNOTATIONS -> true
                        ViewerTab.DETAILS -> true
                    }
                    Tab(
                        selected = selectedTab == tab,
                        onClick = { if (enabled) selectedTab = tab },
                        enabled = enabled,
                        text = {
                            Text(
                                tab.label,
                                style = MaterialTheme.typography.labelMedium,
                                color = when {
                                    !enabled -> TextMuted
                                    selectedTab == tab -> PrimaryIndigoLight
                                    else -> TextSecondary
                                }
                            )
                        },
                        icon = {
                            Icon(
                                tab.icon,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = when {
                                    !enabled -> TextMuted
                                    selectedTab == tab -> PrimaryIndigoLight
                                    else -> TextSecondary
                                }
                            )
                        }
                    )
                }
            }
        }

        // Tab content
        when (selectedTab) {
            ViewerTab.PDF -> {
                if (doc.pdfData != null) {
                    PdfViewer(pdfBytes = doc.pdfData, modifier = Modifier.fillMaxSize())
                }
            }
            ViewerTab.PREVIEW -> {
                PreviewPane(doc = doc, modifier = Modifier.fillMaxSize())
            }
            ViewerTab.ANNOTATIONS -> {
                AnnotationsPane(doc = doc, modifier = Modifier.fillMaxSize())
            }
            ViewerTab.DETAILS -> {
                DetailsPane(doc = doc, modifier = Modifier.fillMaxSize())
            }
        }
    }
}

@Composable
private fun PreviewPane(doc: FlexDocument, modifier: Modifier = Modifier) {
    val bitmap = remember(doc.thumbnail) {
        doc.thumbnail?.let { bytes ->
            runCatching { BitmapFactory.decodeByteArray(bytes, 0, bytes.size) }.getOrNull()
        }
    }

    Box(
        modifier = modifier.background(BackgroundDark).padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        if (bitmap != null) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = CardDark,
                    shadowElevation = 4.dp
                ) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "Document preview",
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 480.dp),
                        contentScale = ContentScale.Fit
                    )
                }
                Text(
                    "Cover preview — ${bitmap.width} × ${bitmap.height} px",
                    color = TextMuted,
                    style = MaterialTheme.typography.labelSmall
                )
            }
        } else {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.HideImage, contentDescription = null, tint = TextMuted, modifier = Modifier.size(48.dp))
                Spacer(Modifier.height(12.dp))
                Text("No preview available", color = TextSecondary, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(4.dp))
                Text("This document does not have a thumbnail image.", color = TextMuted, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun AnnotationsPane(doc: FlexDocument, modifier: Modifier = Modifier) {
    val scroll = rememberScrollState()
    val hasAnnotations = doc.strokeFileCount > 0 || doc.annotationFileCount > 0 || doc.highlightFileCount > 0

    Column(
        modifier = modifier
            .verticalScroll(scroll)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            "Annotation Summary",
            style = MaterialTheme.typography.titleMedium,
            color = PrimaryIndigoLight,
            fontWeight = FontWeight.SemiBold
        )

        if (!hasAnnotations) {
            Surface(
                color = SurfaceVariantDark,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = TextMuted)
                    Text(
                        "No annotation data found in this document.",
                        color = TextSecondary,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        } else {
            Surface(
                color = CardDark,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (doc.strokeFileCount > 0) {
                        AnnotationStatRow(
                            icon = Icons.Default.Draw,
                            iconTint = PrimaryIndigoLight,
                            label = "Pen Strokes",
                            detail = "${doc.strokeFileCount} page${if (doc.strokeFileCount != 1) "s" else ""} with strokes"
                        )
                    }
                    if (doc.annotationFileCount > 0) {
                        AnnotationStatRow(
                            icon = Icons.Default.EditNote,
                            iconTint = AccentAmber,
                            label = "Annotations",
                            detail = "${doc.annotationFileCount} page${if (doc.annotationFileCount != 1) "s" else ""} annotated"
                        )
                    }
                    if (doc.highlightFileCount > 0) {
                        AnnotationStatRow(
                            icon = Icons.Default.Highlight,
                            iconTint = AccentGreen,
                            label = "Highlights",
                            detail = "${doc.highlightFileCount} page${if (doc.highlightFileCount != 1) "s" else ""} highlighted"
                        )
                    }
                }
            }
        }

        // Annotation page count if we know it
        if (doc.pageCount > 0) {
            Surface(
                color = CardDark,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(Icons.Default.Pages, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(20.dp))
                    Column {
                        Text("Total Pages", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                        Text("${doc.pageCount} pages in document", style = MaterialTheme.typography.bodyLarge, color = TextPrimary)
                    }
                }
            }
        }
    }
}

@Composable
private fun AnnotationStatRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: androidx.compose.ui.graphics.Color,
    label: String,
    detail: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(iconTint.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(20.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
            Text(detail, style = MaterialTheme.typography.labelSmall, color = TextMuted)
        }
    }
}

@Composable
private fun DetailsPane(doc: FlexDocument, modifier: Modifier = Modifier) {
    val scroll = rememberScrollState()
    Column(
        modifier = modifier
            .verticalScroll(scroll)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Thumbnail at top if available
        doc.thumbnail?.let { thumbBytes ->
            val bitmap = remember(thumbBytes) {
                runCatching { BitmapFactory.decodeByteArray(thumbBytes, 0, thumbBytes.size) }.getOrNull()
            }
            bitmap?.let {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(CardDark),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        bitmap = it.asImageBitmap(),
                        contentDescription = "Thumbnail",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                }
            }
        }

        // Document info card
        Surface(
            color = CardDark,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "Document Details",
                    style = MaterialTheme.typography.titleMedium,
                    color = PrimaryIndigoLight,
                    fontWeight = FontWeight.SemiBold
                )
                HorizontalDivider(color = DividerColor)
                InfoRow(label = "Name", value = doc.name)
                InfoRow(label = "File size", value = formatFileSize(doc.flxSize))
                doc.info?.let { info ->
                    if (info.createDate > 0) InfoRow(label = "Created", value = formatDate(info.createDate))
                    if (info.modifiedDate > 0) InfoRow(label = "Modified", value = formatDate(info.modifiedDate))
                    InfoRow(label = "Type", value = when (info.type) {
                        0 -> "Document"
                        1 -> "Notebook"
                        2 -> "PDF Annotation"
                        else -> "Unknown (${info.type})"
                    })
                    if (info.key.isNotBlank()) InfoRow(label = "Key", value = info.key)
                }
                if (doc.pageCount > 0) InfoRow(label = "Pages", value = "${doc.pageCount}")
                InfoRow(label = "Has PDF", value = if (doc.pdfData != null) "Yes (${formatFileSize(doc.pdfData.size.toLong())})" else "No")
                InfoRow(label = "Has Preview", value = if (doc.thumbnail != null) "Yes" else "No")
            }
        }

        // Status badges
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            if (doc.pdfData != null) {
                BadgeChip(
                    icon = Icons.Default.PictureAsPdf,
                    label = "PDF",
                    bgColor = PrimaryIndigoDark.copy(alpha = 0.6f),
                    tint = PrimaryIndigoLight
                )
            }
            if (doc.thumbnail != null) {
                BadgeChip(
                    icon = Icons.Default.Image,
                    label = "Preview",
                    bgColor = AccentGreen.copy(alpha = 0.2f),
                    tint = AccentGreen
                )
            }
            if (doc.strokeFileCount > 0 || doc.annotationFileCount > 0) {
                BadgeChip(
                    icon = Icons.Default.Draw,
                    label = "Annotations",
                    bgColor = AccentAmber.copy(alpha = 0.2f),
                    tint = AccentAmber
                )
            }
        }

        if (doc.pdfData == null && doc.thumbnail == null) {
            Surface(
                color = SurfaceVariantDark,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = TextMuted)
                    Text(
                        "This document contains drawing/annotation data but no embedded PDF or thumbnail.",
                        color = TextSecondary,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

@Composable
private fun BadgeChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    bgColor: androidx.compose.ui.graphics.Color,
    tint: androidx.compose.ui.graphics.Color
) {
    Surface(color = bgColor, shape = RoundedCornerShape(20.dp)) {
        Row(
            Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(14.dp), tint = tint)
            Text(label, color = tint, style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Text(label, color = TextSecondary, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(0.4f))
        Text(value, color = TextPrimary, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(0.6f))
    }
}
