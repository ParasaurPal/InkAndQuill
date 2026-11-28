package com.parasaurpal.inkandquill.ui

import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import android.webkit.WebResourceRequest
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.automirrored.filled.NavigateBefore
import androidx.compose.material.icons.automirrored.filled.NavigateNext
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.parasaurpal.inkandquill.data.*
import kotlin.math.ceil

private data class ReaderThemePalette(
    val name: String,
    val bgHex: String,
    val textHex: String,
    val accentHex: String,
    val mutedHex: String,
    val containerColor: Color,
    val onContainerColor: Color
)

private val READER_THEMES = listOf(
    ReaderThemePalette(
        name = "Dusk",
        bgHex = "#1B1824",
        textHex = "#EAE3F2",
        accentHex = "#C9B6E4",
        mutedHex = "#A79BB8",
        containerColor = Color(0xFF252131),
        onContainerColor = Color(0xFFEAE3F2)
    ),
    ReaderThemePalette(
        name = "Parchment",
        bgHex = "#FBF6EC",
        textHex = "#2B2118",
        accentHex = "#7A4E2D",
        mutedHex = "#6E5D4F",
        containerColor = Color(0xFFF3EBD9),
        onContainerColor = Color(0xFF2B2118)
    ),
    ReaderThemePalette(
        name = "Paper",
        bgHex = "#FFFFFF",
        textHex = "#1D1B20",
        accentHex = "#6750A4",
        mutedHex = "#5F5B66",
        containerColor = Color(0xFFF3F1F6),
        onContainerColor = Color(0xFF1D1B20)
    ),
    ReaderThemePalette(
        name = "Sepia",
        bgHex = "#F1E3CC",
        textHex = "#3A2B1C",
        accentHex = "#80532F",
        mutedHex = "#705944",
        containerColor = Color(0xFFE7D4B6),
        onContainerColor = Color(0xFF3A2B1C)
    ),
    ReaderThemePalette(
        name = "Midnight",
        bgHex = "#0D0C11",
        textHex = "#D7D2DF",
        accentHex = "#C9B6E4",
        mutedHex = "#8F879C",
        containerColor = Color(0xFF17151F),
        onContainerColor = Color(0xFFD7D2DF)
    )
)

@Composable
fun ReadingModeScreen(
    db: InkDatabase,
    record: InkRecord,
    onEditRecord: (InkRecord) -> Unit,
    onOpenRecord: (InkRecord) -> Unit,
    onClose: () -> Unit
) {
    BackHandler { onClose() }

    val context = LocalContext.current
    val isBookKind = record.kind in listOf("Book", "Novel", "Story")
    val parentBook = remember(record.id, record.parentId) {
        if (!isBookKind && record.parentId.isNotBlank()) db.get(record.parentId) else null
    }
    val bookChapters = remember(record.id, isBookKind) {
        if (isBookKind) db.bookChapters(record.id) else emptyList()
    }
    val siblingChapters = remember(record.id, isBookKind) {
        if (!isBookKind) db.siblingChapters(record) else emptyList()
    }
    val tocList = remember(isBookKind, bookChapters, siblingChapters, parentBook) {
        when {
            isBookKind -> bookChapters
            parentBook != null && parentBook.kind in listOf("Book", "Novel", "Story") -> db.bookChapters(parentBook.id)
            else -> siblingChapters
        }
    }

    // Book can be read continuously ("Full Book") or chapter-by-chapter ("Chapter")
    var bookScopeMode by remember(record.id) {
        mutableStateOf(if (isBookKind) "Full Book" else "Chapter")
    }
    var activeChapterId by remember(record.id) {
        mutableStateOf(
            if (isBookKind) bookChapters.firstOrNull()?.id.orEmpty() else record.id
        )
    }

    val activeChapter = remember(activeChapterId, record.id, bookChapters, tocList) {
        if (!isBookKind) {
            tocList.firstOrNull { it.id == activeChapterId } ?: record
        } else {
            bookChapters.firstOrNull { it.id == activeChapterId }
        }
    }

    // Reader preferences persisted in settings
    var themeName by remember { mutableStateOf(db.setting("readerTheme", "Dusk")) }
    var readerFont by remember { mutableStateOf(db.setting("readerFont", "serif")) }
    var readerFontSize by remember { mutableFloatStateOf(db.setting("readerFontSize", "19").toFloatOrNull() ?: 19f) }
    var readerLineHeight by remember { mutableFloatStateOf(db.setting("readerLineHeight", "1.75").toFloatOrNull() ?: 1.75f) }
    var readerWidth by remember { mutableFloatStateOf(db.setting("readerWidth", "680").toFloatOrNull() ?: 680f) }
    var readerJustify by remember { mutableStateOf(db.setting("readerJustify", "false") == "true") }

    val customFontData = remember { db.setting("customFontData", "") }
    val customFontMime = remember { db.setting("customFontMime", "font/ttf") }

    var showToc by remember { mutableStateOf(false) }
    var showAppearance by remember { mutableStateOf(false) }
    var immersive by remember { mutableStateOf(false) }
    var scrollProgress by remember { mutableFloatStateOf(0f) }

    val themePalette = remember(themeName) {
        READER_THEMES.firstOrNull { it.name == themeName } ?: READER_THEMES.first()
    }

    val coverDataUri = remember(record.id, record.coverUri, parentBook?.coverUri) {
        val uriStr = record.coverUri.ifBlank { parentBook?.coverUri.orEmpty() }
        if (uriStr.isBlank()) "" else runCatching {
            val uri = Uri.parse(uriStr)
            val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: return@runCatching ""
            val mime = context.contentResolver.getType(uri) ?: "image/jpeg"
            "data:$mime;base64," + Base64.encodeToString(bytes, Base64.NO_WRAP)
        }.getOrDefault("")
    }

    val compiledBodyHtml = remember(
        record.id,
        isBookKind,
        bookScopeMode,
        activeChapter?.id,
        bookChapters,
        coverDataUri
    ) {
        buildReaderDocumentHtml(
            db = db,
            rootRecord = record,
            isBookKind = isBookKind,
            fullBookMode = isBookKind && bookScopeMode == "Full Book",
            activeChapter = activeChapter,
            bookChapters = bookChapters,
            parentBook = parentBook,
            coverDataUri = coverDataUri
        )
    }

    val wordCount = remember(compiledBodyHtml) {
        WordCountEngine.countWordsInPlain(WordCountEngine.htmlToPlainFast(compiledBodyHtml))
    }
    val readMinutes = remember(wordCount) {
        if (wordCount == 0) 0 else ceil(wordCount / 225.0).toInt().coerceAtLeast(1)
    }

    val currentTocIndex = remember(activeChapter?.id, tocList) {
        val id = activeChapter?.id ?: record.id
        tocList.indexOfFirst { it.id == id }
    }
    val prevChapter = if (currentTocIndex > 0) tocList[currentTocIndex - 1] else null
    val nextChapter = if (currentTocIndex >= 0 && currentTocIndex < tocList.lastIndex) tocList[currentTocIndex + 1] else null

    val fullHtmlPage = remember(
        compiledBodyHtml,
        themePalette,
        readerFont,
        readerFontSize,
        readerLineHeight,
        readerWidth,
        readerJustify,
        customFontData,
        customFontMime
    ) {
        wrapReaderPage(
            bodyHtml = compiledBodyHtml,
            palette = themePalette,
            fontFamily = if (readerFont == "Custom" && customFontData.isNotBlank()) "Custom" else readerFont,
            customFontData = customFontData,
            customFontMime = customFontMime,
            fontSize = readerFontSize,
            lineHeight = readerLineHeight,
            maxWidthPx = readerWidth,
            justify = readerJustify
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(themePalette.containerColor)
            .testTag("reading_mode_screen")
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            AnimatedVisibility(visible = !immersive) {
                Column {
                    Surface(
                        color = themePalette.containerColor,
                        contentColor = themePalette.onContainerColor,
                        tonalElevation = 2.dp
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 6.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = onClose,
                                modifier = Modifier.size(48.dp).testTag("reading_mode_close_button")
                            ) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Exit reading mode")
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (isBookKind && bookScopeMode == "Chapter" && activeChapter != null) {
                                        activeChapter.title.ifBlank { "Untitled Chapter" }
                                    } else {
                                        record.title.ifBlank { "Untitled ${record.kind}" }
                                    },
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = buildString {
                                        if (parentBook != null) append("${parentBook.title} · ")
                                        append("$wordCount words")
                                        if (readMinutes > 0) append(" · ~$readMinutes min read")
                                    },
                                    fontSize = 11.sp,
                                    color = themePalette.onContainerColor.copy(alpha = 0.72f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            if (tocList.isNotEmpty()) {
                                IconButton(
                                    onClick = { showToc = true },
                                    modifier = Modifier.size(48.dp).testTag("reading_mode_toc_button")
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.FormatListBulleted, contentDescription = "Table of contents")
                                }
                            }
                            IconButton(
                                onClick = { showAppearance = true },
                                modifier = Modifier.size(48.dp).testTag("reading_mode_typography_button")
                            ) {
                                Icon(Icons.Default.TextFields, contentDescription = "Reader typography and theme")
                            }
                            IconButton(
                                onClick = { immersive = true },
                                modifier = Modifier.size(48.dp).testTag("reading_mode_immersive_button")
                            ) {
                                Icon(Icons.Default.Fullscreen, contentDescription = "Distraction-free fullscreen")
                            }
                            TextButton(
                                onClick = {
                                    val targetToEdit = if (isBookKind && bookScopeMode == "Chapter" && activeChapter != null) {
                                        activeChapter
                                    } else {
                                        record
                                    }
                                    onEditRecord(targetToEdit)
                                },
                                modifier = Modifier.testTag("reading_mode_edit_button")
                            ) {
                                Text("Edit")
                            }
                        }
                    }

                    if (isBookKind && bookChapters.isNotEmpty()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            SingleChoiceSegmentedButtonRow(modifier = Modifier.weight(1f)) {
                                SegmentedButton(
                                    selected = bookScopeMode == "Full Book",
                                    onClick = { bookScopeMode = "Full Book" },
                                    shape = SegmentedButtonDefaults.itemShape(0, 2)
                                ) { Text("Full Book (${bookChapters.size} ch)") }
                                SegmentedButton(
                                    selected = bookScopeMode == "Chapter",
                                    onClick = {
                                        if (activeChapterId.isBlank()) {
                                            activeChapterId = bookChapters.firstOrNull()?.id.orEmpty()
                                        }
                                        bookScopeMode = "Chapter"
                                    },
                                    shape = SegmentedButtonDefaults.itemShape(1, 2)
                                ) { Text("By Chapter") }
                            }
                        }
                    } else if (!isBookKind && parentBook != null && parentBook.kind in listOf("Book", "Novel", "Story")) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(
                                onClick = { onOpenRecord(parentBook) },
                                modifier = Modifier.testTag("reading_mode_open_book_button")
                            ) {
                                Icon(Icons.Default.AutoStories, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Read Full Book: ${parentBook.title}")
                            }
                        }
                    }
                }
            }

            // Reading progress bar
            LinearProgressIndicator(
                progress = { scrollProgress.coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth().height(3.dp),
                color = MaterialTheme.colorScheme.primary,
                trackColor = themePalette.onContainerColor.copy(alpha = 0.12f)
            )

            // Main WebView Reader Surface
            AndroidView(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                factory = { ctx ->
                    WebView(ctx).apply {
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = false
                        isVerticalScrollBarEnabled = true
                        webViewClient = object : WebViewClient() {
                            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                                val uri = request?.url ?: return false
                                if (uri.scheme == "inkquill" && uri.host == "record") {
                                    val targetId = uri.lastPathSegment.orEmpty()
                                    if (targetId.isNotBlank()) db.get(targetId)?.let(onOpenRecord)
                                    return true
                                }
                                return false
                            }
                        }
                        addJavascriptInterface(object {
                            @JavascriptInterface
                            fun onScroll(ratio: Float) {
                                post { scrollProgress = ratio.coerceIn(0f, 1f) }
                            }
                        }, "InkReader")
                        loadDataWithBaseURL(null, fullHtmlPage, "text/html", "UTF-8", null)
                    }
                },
                update = { view ->
                    val currentTag = view.tag as? String
                    if (currentTag != fullHtmlPage) {
                        view.tag = fullHtmlPage
                        scrollProgress = 0f
                        view.loadDataWithBaseURL(null, fullHtmlPage, "text/html", "UTF-8", null)
                    }
                }
            )

            // Bottom Chapter Navigation Bar (when reading chapters)
            AnimatedVisibility(
                visible = !immersive && (!isBookKind || bookScopeMode == "Chapter") && (prevChapter != null || nextChapter != null)
            ) {
                Surface(
                    color = themePalette.containerColor,
                    contentColor = themePalette.onContainerColor,
                    tonalElevation = 2.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        TextButton(
                            enabled = prevChapter != null,
                            onClick = {
                                prevChapter?.let {
                                    if (isBookKind) activeChapterId = it.id else onOpenRecord(it)
                                }
                            },
                            modifier = Modifier.defaultMinSize(minHeight = 48.dp).testTag("reading_prev_chapter_button")
                        ) {
                            Icon(Icons.AutoMirrored.Filled.NavigateBefore, contentDescription = null)
                            Text("Previous")
                        }
                        Text(
                            text = if (currentTocIndex >= 0) "${currentTocIndex + 1} / ${tocList.size}" else "${(scrollProgress * 100).toInt()}%",
                            fontSize = 12.sp,
                            color = themePalette.onContainerColor.copy(alpha = 0.72f)
                        )
                        TextButton(
                            enabled = nextChapter != null,
                            onClick = {
                                nextChapter?.let {
                                    if (isBookKind) activeChapterId = it.id else onOpenRecord(it)
                                }
                            },
                            modifier = Modifier.defaultMinSize(minHeight = 48.dp).testTag("reading_next_chapter_button")
                        ) {
                            Text("Next")
                            Icon(Icons.AutoMirrored.Filled.NavigateNext, contentDescription = null)
                        }
                    }
                }
            }
        }

        // Floating restore button in immersive mode
        if (immersive) {
            FilledTonalButton(
                onClick = { immersive = false },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
                    .defaultMinSize(minHeight = 48.dp)
                    .testTag("reading_mode_exit_immersive_button")
            ) {
                Icon(Icons.Default.FullscreenExit, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Controls")
            }
        }
    }

    // Table of Contents Dialog
    if (showToc) {
        AlertDialog(
            onDismissRequest = { showToc = false },
            title = {
                Text(
                    parentBook?.title?.ifBlank { "Chapters" } ?: record.title.ifBlank { "Table of Contents" },
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 420.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (isBookKind) {
                        item {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (bookScopeMode == "Full Book") {
                                    MaterialTheme.colorScheme.primaryContainer
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .minimumInteractiveComponentSize()
                                    .clickable {
                                        bookScopeMode = "Full Book"
                                        showToc = false
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.AutoStories, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text("Full Book (Continuous)", fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }
                    itemsIndexed(tocList, key = { _, item -> item.id }) { idx, ch ->
                        val isCurrent = (isBookKind && bookScopeMode == "Chapter" && ch.id == activeChapterId) ||
                            (!isBookKind && ch.id == record.id)
                        val chWords = remember(ch.id, ch.body) {
                            DocumentExport.plain(ch.body).trim().split(Regex("\\s+")).count { it.isNotBlank() }
                        }
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isCurrent) MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .minimumInteractiveComponentSize()
                                .clickable {
                                    if (isBookKind) {
                                        activeChapterId = ch.id
                                        bookScopeMode = "Chapter"
                                    } else {
                                        onOpenRecord(ch)
                                    }
                                    showToc = false
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${idx + 1}.",
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.width(28.dp)
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = ch.title.ifBlank { "Untitled ${ch.kind}" },
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${ch.kind} · $chWords words",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showToc = false }) { Text("Close") }
            }
        )
    }

    // Reader Typography & Theme Dialog
    if (showAppearance) {
        AlertDialog(
            onDismissRequest = { showAppearance = false },
            title = { Text("Reader Appearance") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Theme", style = MaterialTheme.typography.labelLarge)
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        READER_THEMES.forEach { pal ->
                            FilterChip(
                                selected = themeName == pal.name,
                                onClick = {
                                    themeName = pal.name
                                    db.setSetting("readerTheme", pal.name)
                                },
                                label = { Text(pal.name) }
                            )
                        }
                    }

                    Text("Font Family", style = MaterialTheme.typography.labelLarge)
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val fontChoices = listOf("serif" to "Serif", "sans-serif" to "Sans", "monospace" to "Mono") +
                            if (customFontData.isNotBlank()) listOf("Custom" to "Custom") else emptyList()
                        fontChoices.forEach { (value, label) ->
                            FilterChip(
                                selected = readerFont == value,
                                onClick = {
                                    readerFont = value
                                    db.setSetting("readerFont", value)
                                },
                                label = { Text(label) }
                            )
                        }
                    }

                    Text("Font Size: ${readerFontSize.toInt()} px", fontSize = 13.sp)
                    Slider(
                        value = readerFontSize,
                        onValueChange = {
                            readerFontSize = it
                            db.setSetting("readerFontSize", it.toInt().toString())
                        },
                        valueRange = 14f..28f
                    )

                    Text("Line Spacing: ${"%.2f".format(readerLineHeight)}", fontSize = 13.sp)
                    Slider(
                        value = readerLineHeight,
                        onValueChange = {
                            readerLineHeight = it
                            db.setSetting("readerLineHeight", it.toString())
                        },
                        valueRange = 1.3f..2.2f
                    )

                    Text("Column Width", style = MaterialTheme.typography.labelLarge)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("Narrow" to 540f, "Balanced" to 680f, "Wide" to 860f).forEach { (label, width) ->
                            FilterChip(
                                selected = readerWidth.toInt() == width.toInt(),
                                onClick = {
                                    readerWidth = width
                                    db.setSetting("readerWidth", width.toInt().toString())
                                },
                                label = { Text(label) }
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = readerJustify,
                            onCheckedChange = {
                                readerJustify = it
                                db.setSetting("readerJustify", it.toString())
                            }
                        )
                        Text("Justify paragraph text")
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showAppearance = false }) { Text("Done") }
            }
        )
    }
}

private fun buildReaderDocumentHtml(
    db: InkDatabase,
    rootRecord: InkRecord,
    isBookKind: Boolean,
    fullBookMode: Boolean,
    activeChapter: InkRecord?,
    bookChapters: List<InkRecord>,
    parentBook: InkRecord?,
    coverDataUri: String
): String {
    val sb = StringBuilder()

    fun appendRecordProseOrSheet(rec: InkRecord) {
        val bodyHtml = rec.body.trim().takeIf { DocumentExport.plain(it).isNotBlank() }.orEmpty()
        val sheetTplId = db.sheetTemplateId(rec.id)
        val compiledSheet = if (sheetTplId.isNotBlank()) {
            val fields = db.templateFields(sheetTplId)
            val sections = db.templateSections(sheetTplId)
            val vals = db.sheetValues(rec.id)
            SheetEngine.compileToHtml(fields, sections, vals, "") { db.get(it) }
        } else ""
        if (bodyHtml.isNotBlank()) sb.append(bodyHtml)
        if (compiledSheet.isNotBlank()) {
            if (bodyHtml.isNotBlank()) sb.append("<hr/>")
            sb.append(compiledSheet)
        }
        if (sheetTplId.isNotBlank()) {
            val parentArticle = rec.parentId.takeIf { it.isNotBlank() }?.let { db.get(it) }
                ?.takeIf { db.sheetTemplateId(it.id).isNotBlank() }
            if (parentArticle != null) {
                sb.append("<p class=\"wiki-parent\">Filed under: <a href=\"inkquill://record/")
                    .append(Uri.encode(parentArticle.id)).append("\">")
                    .append(escapeHtml(parentArticle.title)).append("</a></p>")
            }
            val childArticles = db.descendants(rec.id).filter {
                it.parentId == rec.id && db.sheetTemplateId(it.id).isNotBlank()
            }
            if (childArticles.isNotEmpty()) {
                sb.append("<section class=\"wiki-child-articles\"><h2>Articles in this entry</h2><ul>")
                childArticles.forEach { child ->
                    sb.append("<li><a href=\"inkquill://record/").append(Uri.encode(child.id))
                        .append("\">").append(escapeHtml(child.title)).append("</a></li>")
                }
                sb.append("</ul></section>")
            }
        }
    }

    if (isBookKind) {
        if (fullBookMode) {
            sb.append("""<header class="book-hero">""")
            if (coverDataUri.isNotBlank()) {
                sb.append("""<img class="book-cover" src="$coverDataUri" alt="Book cover" />""")
            }
            sb.append("""<div class="book-kind">${escapeHtml(rootRecord.kind.uppercase())}</div>""")
            sb.append("""<h1 class="book-title">${escapeHtml(rootRecord.title.ifBlank { "Untitled Book" })}</h1>""")
            if (bookChapters.isNotEmpty()) {
                sb.append("""<div class="book-meta">${bookChapters.size} ${if (bookChapters.size == 1) "chapter" else "chapters"}</div>""")
            }
            sb.append("""</header>""")

            val beforeChapters = sb.length
            appendRecordProseOrSheet(rootRecord)
            if (sb.length > beforeChapters && bookChapters.isNotEmpty()) {
                sb.append("""<div class="ornament">✦ &nbsp; ✦ &nbsp; ✦</div>""")
            }

            bookChapters.forEachIndexed { idx, ch ->
                if (idx > 0) sb.append("""<div class="ornament">✦ &nbsp; ✦ &nbsp; ✦</div>""")
                sb.append("""<section class="chapter-block">""")
                sb.append("""<div class="chapter-eyebrow">CHAPTER ${idx + 1}</div>""")
                sb.append("""<h2 class="chapter-title">${escapeHtml(ch.title.ifBlank { "Untitled Chapter" })}</h2>""")
                appendRecordProseOrSheet(ch)
                val scenes = db.chapterScenes(ch.id)
                scenes.forEachIndexed { sIdx, scene ->
                    if (sIdx > 0 || ch.body.isNotBlank()) {
                        sb.append("""<div class="scene-break">* &nbsp; * &nbsp; *</div>""")
                    }
                    if (scene.title.isNotBlank() && !scene.title.startsWith("Untitled")) {
                        sb.append("""<h3 class="scene-title">${escapeHtml(scene.title)}</h3>""")
                    }
                    appendRecordProseOrSheet(scene)
                }
                sb.append("""</section>""")
            }

            if (bookChapters.isEmpty() && DocumentExport.plain(rootRecord.body).isBlank()) {
                sb.append("""<p class="empty-note">This ${escapeHtml(rootRecord.kind.lowercase())} has no prose or chapters yet. Tap Edit to start writing.</p>""")
            }
        } else if (activeChapter != null) {
            sb.append("""<header class="chapter-header">""")
            sb.append("""<div class="chapter-eyebrow">${escapeHtml(rootRecord.title.ifBlank { "BOOK" }).uppercase()}</div>""")
            sb.append("""<h1 class="chapter-title">${escapeHtml(activeChapter.title.ifBlank { "Untitled Chapter" })}</h1>""")
            sb.append("""</header>""")
            appendRecordProseOrSheet(activeChapter)
            val scenes = db.chapterScenes(activeChapter.id)
            scenes.forEach { scene ->
                sb.append("""<div class="scene-break">* &nbsp; * &nbsp; *</div>""")
                if (scene.title.isNotBlank() && !scene.title.startsWith("Untitled")) {
                    sb.append("""<h3 class="scene-title">${escapeHtml(scene.title)}</h3>""")
                }
                appendRecordProseOrSheet(scene)
            }
        }
    } else {
        sb.append("""<header class="chapter-header">""")
        if (parentBook != null) {
            sb.append("""<div class="chapter-eyebrow">${escapeHtml(parentBook.title.ifBlank { parentBook.kind }).uppercase()}</div>""")
        } else {
            sb.append("""<div class="chapter-eyebrow">${escapeHtml(rootRecord.kind.uppercase())}</div>""")
        }
        sb.append("""<h1 class="chapter-title">${escapeHtml(rootRecord.title.ifBlank { "Untitled ${rootRecord.kind}" })}</h1>""")
        sb.append("""</header>""")
        appendRecordProseOrSheet(rootRecord)
        val scenes = if (rootRecord.kind == "Chapter") db.chapterScenes(rootRecord.id) else emptyList()
        scenes.forEachIndexed { idx, scene ->
            if (idx > 0 || rootRecord.body.isNotBlank()) {
                sb.append("""<div class="scene-break">* &nbsp; * &nbsp; *</div>""")
            }
            if (scene.title.isNotBlank() && !scene.title.startsWith("Untitled")) {
                sb.append("""<h3 class="scene-title">${escapeHtml(scene.title)}</h3>""")
            }
            appendRecordProseOrSheet(scene)
        }
        if (DocumentExport.plain(sb.toString()).trim() == rootRecord.title.trim()) {
            sb.append("""<p class="empty-note">No prose written in this ${escapeHtml(rootRecord.kind.lowercase())} yet. Tap Edit to add content.</p>""")
        }
    }
    return sb.toString()
}

private fun wrapReaderPage(
    bodyHtml: String,
    palette: ReaderThemePalette,
    fontFamily: String,
    customFontData: String,
    customFontMime: String,
    fontSize: Float,
    lineHeight: Float,
    maxWidthPx: Float,
    justify: Boolean
): String {
    return """<!doctype html><html><head><meta name="viewport" content="width=device-width,initial-scale=1"><style>
@font-face{font-family:Custom;src:url(data:$customFontMime;base64,$customFontData)}
html,body{margin:0;padding:0;background:${palette.bgHex};color:${palette.textHex};font-family:$fontFamily;font-size:${fontSize}px;line-height:$lineHeight}
.reader-shell{max-width:${maxWidthPx}px;margin:0 auto;padding:28px 22px 64px 22px;box-sizing:border-box;${if (justify) "text-align:justify;" else ""}}
.book-hero{text-align:center;margin:8px 0 28px 0;padding-bottom:20px;border-bottom:1px solid ${palette.mutedHex}44}
.book-cover{max-width:170px;border-radius:12px;box-shadow:0 8px 24px rgba(0,0,0,0.3);margin-bottom:16px}
.book-kind,.chapter-eyebrow{font-family:sans-serif;font-size:11px;letter-spacing:2px;font-weight:700;color:${palette.accentHex};margin-bottom:6px}
.book-title{font-size:1.9em;line-height:1.18;margin:4px 0 8px 0}
.book-meta{font-family:sans-serif;font-size:12px;color:${palette.mutedHex}}
.chapter-header{text-align:center;margin:8px 0 24px 0;padding-bottom:14px;border-bottom:1px solid ${palette.mutedHex}33}
.chapter-title{font-size:1.5em;line-height:1.25;margin:4px 0 12px 0;text-align:center}
.scene-title{font-size:1.15em;color:${palette.accentHex};margin-top:18px}
.ornament,.scene-break{text-align:center;color:${palette.accentHex};letter-spacing:4px;margin:28px 0;font-size:14px}
.empty-note{text-align:center;color:${palette.mutedHex};font-style:italic;margin-top:32px}
p{margin:0 0 1.05em 0;overflow-wrap:anywhere}
h1,h2,h3,h4{line-height:1.25;margin:1.2em 0 0.5em 0}
blockquote{border-left:3px solid ${palette.accentHex};padding:6px 14px;margin:14px 0;background:${palette.accentHex}18;border-radius:0 8px 8px 0;font-style:italic}
.ink-callout{border-left:4px solid ${palette.accentHex};background:${palette.accentHex}1F;padding:12px 16px;border-radius:0 12px 12px 0;margin:14px 0}
.ink-dropcap::first-letter{float:left;font-size:3.2em;line-height:0.84;padding-right:8px;font-weight:bold;color:${palette.accentHex}}
ul.ink-checklist{list-style:none;padding-left:4px}
table{border-collapse:collapse;width:100%;margin:14px 0}
th,td{border:1px solid ${palette.mutedHex}66;padding:8px}
img{max-width:100%;height:auto;border-radius:10px}
a{color:${palette.accentHex}}
pre,code{font-family:monospace;background:${palette.mutedHex}26;border-radius:6px;padding:2px 5px}
.page-break{border-top:1px dashed ${palette.mutedHex}55;margin:24px 0}
</style></head><body><article class="reader-shell">$bodyHtml</article><script>
window.addEventListener('scroll', function(){
    const max=Math.max(1, document.documentElement.scrollHeight - window.innerHeight);
    const ratio=Math.min(1, Math.max(0, window.scrollY / max));
    if(window.InkReader) window.InkReader.onScroll(ratio);
});
</script></body></html>"""
}

private fun escapeHtml(text: String): String = text
    .replace("&", "&amp;")
    .replace("<", "&lt;")
    .replace(">", "&gt;")
    .replace("\"", "&quot;")
