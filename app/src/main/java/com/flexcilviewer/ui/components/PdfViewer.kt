package com.flexcilviewer.ui.components

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
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
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.flexcilviewer.ui.theme.*
import com.tom_roush.pdfbox.pdmodel.PDDocument
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File

// ── Encryption detection ──────────────────────────────────────────────────────

private fun isPdfEncrypted(bytes: ByteArray): Boolean {
    // Scan the first 64 KB of the PDF for the /Encrypt dictionary key
    val sampleSize = minOf(bytes.size, 65536)
    val sample = String(bytes, 0, sampleSize, Charsets.ISO_8859_1)
    return sample.contains("/Encrypt")
}

private fun decryptPdfBytes(context: android.content.Context, bytes: ByteArray, password: String): ByteArray {
    val doc = PDDocument.load(bytes, password)
    doc.isAllSecurityToBeRemoved = true
    val out = ByteArrayOutputStream()
    doc.save(out)
    doc.close()
    return out.toByteArray()
}

// ── Render helper ─────────────────────────────────────────────────────────────

private suspend fun renderPdfBytes(context: android.content.Context, bytes: ByteArray): List<Bitmap> =
    withContext(Dispatchers.IO) {
        val tmpFile = File.createTempFile("flex_pdf_", ".pdf", context.cacheDir)
        try {
            tmpFile.writeBytes(bytes)
            val pfd = ParcelFileDescriptor.open(tmpFile, ParcelFileDescriptor.MODE_READ_ONLY)
            val renderer = PdfRenderer(pfd)
            val bitmaps = mutableListOf<Bitmap>()
            for (i in 0 until renderer.pageCount) {
                val page = renderer.openPage(i)
                val scale = 2.0f
                val bmp = Bitmap.createBitmap(
                    (page.width * scale).toInt(),
                    (page.height * scale).toInt(),
                    Bitmap.Config.ARGB_8888
                )
                bmp.eraseColor(android.graphics.Color.WHITE)
                page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                page.close()
                bitmaps.add(bmp)
            }
            renderer.close()
            pfd.close()
            bitmaps
        } finally {
            tmpFile.delete()
        }
    }

// ── States ────────────────────────────────────────────────────────────────────

private sealed class PdfState {
    object Loading : PdfState()
    data class Ready(val pages: List<Bitmap>) : PdfState()
    object PasswordNeeded : PdfState()
    object PasswordWrong : PdfState()
    data class Error(val message: String) : PdfState()
}

// ── Main composable ───────────────────────────────────────────────────────────

@Composable
fun PdfViewer(pdfBytes: ByteArray, modifier: Modifier = Modifier) {
    val context = LocalContext.current

    var state by remember { mutableStateOf<PdfState>(PdfState.Loading) }
    var globalScale by remember { mutableFloatStateOf(1.2f) }
    var horizontalMode by remember { mutableStateOf(false) }
    var maximized by remember { mutableStateOf(false) }

    var passwordInput by remember { mutableStateOf("") }
    var showPasswordText by remember { mutableStateOf(false) }
    val passwordFocus = remember { FocusRequester() }

    // ── Load / reload with optional password ─────────────────────────────────
    fun tryLoad(password: String = "") {
        state = PdfState.Loading
    }

    LaunchedEffect(pdfBytes) {
        state = PdfState.Loading
        passwordInput = ""
        withContext(Dispatchers.IO) {
            try {
                val pages = renderPdfBytes(context, pdfBytes)
                state = PdfState.Ready(pages)
            } catch (e: Exception) {
                state = if (isPdfEncrypted(pdfBytes)) PdfState.PasswordNeeded
                        else PdfState.Error(e.message ?: "Failed to render PDF")
            }
        }
    }

    // Called when the user submits a password
    val submitPassword: (String) -> Unit = { pwd ->
        state = PdfState.Loading
    }

    LaunchedEffect(state) {
        // When we flip to Loading *after* PasswordNeeded/PasswordWrong it means
        // the user just submitted a password — we handle it here via a side channel.
        // Instead we use a dedicated key below.
    }

    // Separate effect that fires when the user intentionally submits a password
    var pendingPassword by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(pendingPassword) {
        val pwd = pendingPassword ?: return@LaunchedEffect
        state = PdfState.Loading
        withContext(Dispatchers.IO) {
            try {
                val decrypted = decryptPdfBytes(context, pdfBytes, pwd)
                val pages = renderPdfBytes(context, decrypted)
                state = PdfState.Ready(pages)
                passwordInput = ""
                pendingPassword = null
            } catch (e: Exception) {
                val msg = e.message ?: ""
                state = if (msg.contains("password", ignoreCase = true) ||
                           msg.contains("encrypt", ignoreCase = true) ||
                           msg.contains("decrypt", ignoreCase = true) ||
                           msg.contains("BadPadding", ignoreCase = true) ||
                           msg.contains("incorrect", ignoreCase = true)) {
                    PdfState.PasswordWrong
                } else {
                    PdfState.Error(msg.ifBlank { "Failed to decrypt PDF" })
                }
                pendingPassword = null
            }
        }
    }

    // Focus password field when needed
    LaunchedEffect(state) {
        if (state is PdfState.PasswordNeeded || state is PdfState.PasswordWrong) {
            kotlinx.coroutines.delay(100)
            runCatching { passwordFocus.requestFocus() }
        }
    }

    // ── Fullscreen dialog ─────────────────────────────────────────────────────
    val pages = (state as? PdfState.Ready)?.pages ?: emptyList()

    if (maximized && pages.isNotEmpty()) {
        Dialog(
            onDismissRequest = { maximized = false },
            properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnBackPress = true, dismissOnClickOutside = false)
        ) {
            Box(modifier = Modifier.fillMaxSize().background(BackgroundDark)) {
                PdfContent(
                    pages = pages,
                    globalScale = globalScale,
                    horizontalMode = horizontalMode,
                    onScaleChange = { globalScale = it },
                    onHorizontalToggle = { horizontalMode = it },
                    modifier = Modifier.fillMaxSize(),
                    isMaximized = true,
                    onToggleMaximize = { maximized = false }
                )
            }
        }
    }

    Box(modifier = modifier.background(BackgroundDark)) {
        when (val s = state) {
            is PdfState.Loading -> {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(color = PrimaryIndigoLight)
                    Spacer(Modifier.height(12.dp))
                    Text("Rendering PDF…", color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
                }
            }

            is PdfState.PasswordNeeded, is PdfState.PasswordWrong -> {
                val isWrong = s is PdfState.PasswordWrong
                PasswordPrompt(
                    isWrongPassword = isWrong,
                    password = passwordInput,
                    showPasswordText = showPasswordText,
                    focusRequester = passwordFocus,
                    onPasswordChange = { passwordInput = it },
                    onToggleVisibility = { showPasswordText = !showPasswordText },
                    onSubmit = {
                        if (passwordInput.isNotBlank()) {
                            pendingPassword = passwordInput
                        }
                    }
                )
            }

            is PdfState.Error -> {
                Column(
                    modifier = Modifier.fillMaxSize().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = AccentRed, modifier = Modifier.size(48.dp))
                    Spacer(Modifier.height(12.dp))
                    Text("Unable to render PDF", color = AccentRed, style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    Text(s.message, color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
                }
            }

            is PdfState.Ready -> {
                PdfContent(
                    pages = s.pages,
                    globalScale = globalScale,
                    horizontalMode = horizontalMode,
                    onScaleChange = { globalScale = it },
                    onHorizontalToggle = { horizontalMode = it },
                    modifier = Modifier.fillMaxSize(),
                    isMaximized = false,
                    onToggleMaximize = { maximized = true }
                )
            }
        }
    }
}

// ── Password prompt ───────────────────────────────────────────────────────────

@Composable
private fun PasswordPrompt(
    isWrongPassword: Boolean,
    password: String,
    showPasswordText: Boolean,
    focusRequester: FocusRequester,
    onPasswordChange: (String) -> Unit,
    onToggleVisibility: () -> Unit,
    onSubmit: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
            elevation = CardDefaults.cardElevation(8.dp)
        ) {
            Column(
                modifier = Modifier.padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Lock icon
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = AccentAmber.copy(alpha = 0.15f),
                    modifier = Modifier.size(64.dp)
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Icon(
                            Icons.Default.Lock,
                            contentDescription = null,
                            tint = AccentAmber,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }

                Text(
                    "Password Protected",
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary
                )

                Text(
                    if (isWrongPassword)
                        "Incorrect password. Please try again."
                    else
                        "This PDF is locked. Enter the password to view it.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )

                // Password input
                OutlinedTextField(
                    value = password,
                    onValueChange = onPasswordChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester),
                    label = { Text("PDF Password") },
                    placeholder = { Text("Enter password") },
                    visualTransformation = if (showPasswordText) VisualTransformation.None
                                           else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(onDone = { onSubmit() }),
                    trailingIcon = {
                        IconButton(onClick = onToggleVisibility) {
                            Icon(
                                if (showPasswordText) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = if (showPasswordText) "Hide password" else "Show password",
                                tint = TextMuted
                            )
                        }
                    },
                    isError = isWrongPassword,
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryIndigoLight,
                        unfocusedBorderColor = DividerColor,
                        errorBorderColor = AccentRed,
                        focusedLabelColor = PrimaryIndigoLight,
                        cursorColor = PrimaryIndigoLight
                    )
                )

                if (isWrongPassword) {
                    Text(
                        "Wrong password — please try again.",
                        color = AccentRed,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Button(
                    onClick = onSubmit,
                    enabled = password.isNotBlank(),
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigoLight)
                ) {
                    Icon(Icons.Default.LockOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Unlock PDF", style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

// ── Shared PDF content (used in both normal and maximized modes) ──────────────

@Composable
private fun PdfContent(
    pages: List<Bitmap>,
    globalScale: Float,
    horizontalMode: Boolean,
    onScaleChange: (Float) -> Unit,
    onHorizontalToggle: (Boolean) -> Unit,
    isMaximized: Boolean,
    onToggleMaximize: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {

        // ── Toolbar ───────────────────────────────────────────────────────────
        Surface(color = SurfaceDark, modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isMaximized) {
                    IconButton(onClick = onToggleMaximize, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Exit fullscreen", tint = PrimaryIndigoLight, modifier = Modifier.size(20.dp))
                    }
                    Spacer(Modifier.width(4.dp))
                }

                IconButton(onClick = { onScaleChange((globalScale - 0.2f).coerceAtLeast(0.5f)) }, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.ZoomOut, contentDescription = "Zoom out", tint = TextSecondary, modifier = Modifier.size(20.dp))
                }

                Text(
                    "${(globalScale * 100).toInt()}%",
                    color = TextSecondary,
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.width(48.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )

                IconButton(onClick = { onScaleChange((globalScale + 0.2f).coerceAtMost(4f)) }, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.ZoomIn, contentDescription = "Zoom in", tint = TextSecondary, modifier = Modifier.size(20.dp))
                }

                IconButton(onClick = { onScaleChange(1.2f) }, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.RestartAlt, contentDescription = "Reset zoom", tint = TextMuted, modifier = Modifier.size(18.dp))
                }

                Spacer(Modifier.weight(1f))

                Text(
                    "${pages.size} page${if (pages.size != 1) "s" else ""}",
                    color = TextMuted,
                    style = MaterialTheme.typography.labelSmall
                )

                Spacer(Modifier.width(4.dp))

                IconButton(onClick = { onHorizontalToggle(!horizontalMode) }, modifier = Modifier.size(36.dp)) {
                    Icon(
                        if (horizontalMode) Icons.Default.SwapVert else Icons.Default.SwapHoriz,
                        contentDescription = if (horizontalMode) "Switch to vertical" else "Switch to horizontal",
                        tint = PrimaryIndigoLight,
                        modifier = Modifier.size(20.dp)
                    )
                }

                IconButton(onClick = onToggleMaximize, modifier = Modifier.size(36.dp)) {
                    Icon(
                        if (isMaximized) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                        contentDescription = if (isMaximized) "Exit fullscreen" else "Fullscreen",
                        tint = TextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        HorizontalDivider(color = DividerColor)

        // ── Pages ─────────────────────────────────────────────────────────────
        if (horizontalMode) {
            HorizontalPdfPages(pages = pages, globalScale = globalScale)
        } else {
            VerticalPdfPages(pages = pages, globalScale = globalScale)
        }
    }
}

// ── Vertical scroll with scrollbar indicator ──────────────────────────────────

@Composable
private fun VerticalPdfPages(pages: List<Bitmap>, globalScale: Float) {
    val listState = rememberLazyListState()

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            itemsIndexed(pages) { index, bmp ->
                PdfPageItem(bitmap = bmp, pageNumber = index + 1, total = pages.size, globalScale = globalScale)
            }
        }

        if (pages.size > 1) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .fillMaxHeight()
                    .width(12.dp)
                    .padding(vertical = 8.dp, horizontal = 2.dp)
                    .drawWithContent {
                        drawRoundRect(color = SurfaceVariantDark, cornerRadius = CornerRadius(6f))
                        val totalItems = listState.layoutInfo.totalItemsCount.takeIf { it > 0 } ?: return@drawWithContent
                        val visibleItems = listState.layoutInfo.visibleItemsInfo
                        if (visibleItems.isEmpty()) return@drawWithContent
                        val thumbFraction = (visibleItems.size.toFloat() / totalItems).coerceIn(0.1f, 1f)
                        val scrollFraction = listState.firstVisibleItemIndex.toFloat() / totalItems
                        val trackHeight = size.height
                        val thumbHeight = (trackHeight * thumbFraction).coerceAtLeast(40f)
                        val thumbTop = ((trackHeight - thumbHeight) * scrollFraction).coerceIn(0f, trackHeight - thumbHeight)
                        drawRoundRect(
                            color = PrimaryIndigoLight.copy(alpha = if (listState.isScrollInProgress) 0.9f else 0.55f),
                            topLeft = Offset(0f, thumbTop),
                            size = Size(size.width, thumbHeight),
                            cornerRadius = CornerRadius(6f)
                        )
                    }
            )
        }

        val currentPage by remember { derivedStateOf { (listState.firstVisibleItemIndex + 1).coerceIn(1, pages.size) } }
        Surface(
            color = BackgroundDark.copy(alpha = 0.80f),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 12.dp)
        ) {
            Text(
                "$currentPage / ${pages.size}",
                color = TextPrimary,
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
            )
        }
    }
}

// ── Horizontal page swipe mode ────────────────────────────────────────────────

@Composable
private fun HorizontalPdfPages(pages: List<Bitmap>, globalScale: Float) {
    val pagerState = rememberPagerState(pageCount = { pages.size })

    Box(modifier = Modifier.fillMaxSize()) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp),
            pageSpacing = 12.dp
        ) { index ->
            PdfPageItem(bitmap = pages[index], pageNumber = index + 1, total = pages.size, globalScale = globalScale)
        }

        Surface(
            color = BackgroundDark.copy(alpha = 0.80f),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 12.dp)
        ) {
            Text(
                "${pagerState.currentPage + 1} / ${pages.size}",
                color = TextPrimary,
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
            )
        }

        if (pages.size in 2..10) {
            Row(
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 40.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                pages.indices.forEach { i ->
                    Box(
                        modifier = Modifier
                            .size(if (i == pagerState.currentPage) 8.dp else 5.dp)
                            .clip(RoundedCornerShape(50))
                            .background(if (i == pagerState.currentPage) PrimaryIndigoLight else TextMuted.copy(alpha = 0.5f))
                    )
                }
            }
        }
    }
}

// ── Single page item ──────────────────────────────────────────────────────────

@Composable
private fun PdfPageItem(bitmap: Bitmap, pageNumber: Int, total: Int, globalScale: Float) {
    var pinchScale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Page $pageNumber / $total", color = TextMuted, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(bottom = 6.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(Color.White)
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        pinchScale = (pinchScale * zoom).coerceIn(0.5f, 4f)
                        offsetX += pan.x
                        offsetY += pan.y
                    }
                }
        ) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "Page $pageNumber",
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer(
                        scaleX = globalScale * pinchScale,
                        scaleY = globalScale * pinchScale,
                        translationX = offsetX,
                        translationY = offsetY
                    ),
                contentScale = ContentScale.FillWidth
            )
        }
    }
}
