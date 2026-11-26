package com.parasaurpal.inkandquill.ui

import android.net.Uri
import android.util.Base64
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FormatAlignLeft
import androidx.compose.material.icons.automirrored.filled.FormatAlignRight
import androidx.compose.material.icons.automirrored.filled.FormatIndentDecrease
import androidx.compose.material.icons.automirrored.filled.FormatIndentIncrease
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import org.json.JSONObject

private data class EditorSelectionState(
    val bold: Boolean = false,
    val italic: Boolean = false,
    val underline: Boolean = false,
    val strike: Boolean = false,
    val sub: Boolean = false,
    val sup: Boolean = false,
    val ul: Boolean = false,
    val ol: Boolean = false,
    val alignLeft: Boolean = false,
    val alignCenter: Boolean = false,
    val alignRight: Boolean = false,
    val alignJustify: Boolean = false,
    val blockTag: String = "P",
    val inTable: Boolean = false,
    val linkHref: String = "",
    val selectedText: String = ""
)

@Composable
fun RichTextEditor(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    fontSize: Float = 18f,
    fontFamily: String = "sans-serif",
    customFontData: String = "",
    customFontMime: String = "font/ttf",
    lineSpacing: Float = 1.65f,
    pageMode: Boolean = false,
    enabledCommands: Set<String>? = null,
    pageWidth: Float = 760f,
    pageMargin: Float = 12f,
    paragraphSpacing: Float = 12f,
    backgroundColor: String = "transparent",
    backgroundImageData: String = "",
    backgroundImageMime: String = "image/jpeg",
    textColor: String = "#eee8f5",
    findTerm: String = "",
    findOccurrence: Int = 0,
    onFindHandled: () -> Unit = {}
) {
    val callback by rememberUpdatedState(onValueChange)
    val findCallback by rememberUpdatedState(onFindHandled)
    val context = LocalContext.current
    val web = remember { mutableStateOf<WebView?>(null) }

    var selState by remember { mutableStateOf(EditorSelectionState()) }
    var sourceMode by remember { mutableStateOf(false) }

    var showBlockMenu by remember { mutableStateOf(false) }
    var showSizeMenu by remember { mutableStateOf(false) }
    var showForeColorDialog by remember { mutableStateOf(false) }
    var showHiliteColorDialog by remember { mutableStateOf(false) }
    var showLinkDialog by remember { mutableStateOf(false) }
    var showTableDialog by remember { mutableStateOf(false) }
    var showSymbolsDialog by remember { mutableStateOf(false) }

    fun isEnabled(cmd: String): Boolean = enabledCommands == null || cmd in enabledCommands

    fun runJs(script: String) {
        web.value?.evaluateJavascript(script, null)
    }

    fun exec(cmd: String, arg: String? = null) {
        val jsArg = if (arg == null) "null" else JSONObject.quote(arg)
        runJs("document.execCommand('$cmd', false, $jsArg); syncEditorChange();")
    }

    val imagePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        uri?.let {
            runCatching {
                val bytes = context.contentResolver.openInputStream(it)?.use { stream -> stream.readBytes() } ?: byteArrayOf()
                val mime = context.contentResolver.getType(it) ?: "image/png"
                val data = "data:$mime;base64," + Base64.encodeToString(bytes, Base64.NO_WRAP)
                val imgHtml = """<figure style="margin:12px 0;text-align:center;"><img src="$data" style="max-width:100%;height:auto;border-radius:10px;" /></figure><p></p>"""
                runJs("document.execCommand('insertHTML', false, ${JSONObject.quote(imgHtml)}); syncEditorChange();")
            }
        }
    }

    Column(modifier) {
        // Row 1: History, Block Style, Font Size, Inline Formatting, Colors
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(vertical = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (!sourceMode) {
                if (isEnabled("undo")) {
                    RichToolbarIcon(
                        selected = false,
                        onClick = { exec("undo") },
                        contentDescription = "Undo"
                    ) { Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = null, modifier = Modifier.size(18.dp)) }
                }
                if (isEnabled("redo")) {
                    RichToolbarIcon(
                        selected = false,
                        onClick = { exec("redo") },
                        contentDescription = "Redo"
                    ) { Icon(Icons.AutoMirrored.Filled.Redo, contentDescription = null, modifier = Modifier.size(18.dp)) }
                }

                // Block Format Selector
                Box {
                    val blockLabel = when (selState.blockTag.uppercase()) {
                        "H1" -> "H1"
                        "H2" -> "H2"
                        "H3" -> "H3"
                        "H4" -> "H4"
                        "BLOCKQUOTE" -> "Quote"
                        "PRE" -> "Code"
                        else -> "Style"
                    }
                    FilledTonalButton(
                        onClick = { showBlockMenu = true },
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.defaultMinSize(minHeight = 40.dp).testTag("rich_block_style_button")
                    ) {
                        Text(blockLabel, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        Icon(Icons.Default.ArrowDropDown, contentDescription = null, modifier = Modifier.size(16.dp))
                    }
                    DropdownMenu(expanded = showBlockMenu, onDismissRequest = { showBlockMenu = false }) {
                        listOf(
                            "Paragraph" to "P",
                            "Heading 1" to "H1",
                            "Heading 2" to "H2",
                            "Heading 3" to "H3",
                            "Heading 4" to "H4",
                            "Blockquote" to "BLOCKQUOTE",
                            "Code Block" to "PRE"
                        ).forEach { (label, tag) ->
                            DropdownMenuItem(
                                text = { Text(label) },
                                onClick = {
                                    exec("formatBlock", tag)
                                    showBlockMenu = false
                                }
                            )
                        }
                        HorizontalDivider()
                        DropdownMenuItem(
                            text = { Text("Lore Callout Box") },
                            onClick = {
                                val box = """<div class="ink-callout"><strong>Lore Note:</strong> Write callout text here...</div><p></p>"""
                                runJs("document.execCommand('insertHTML', false, ${JSONObject.quote(box)}); syncEditorChange();")
                                showBlockMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Drop Cap Paragraph") },
                            onClick = {
                                runJs("toggleDropCap(); syncEditorChange();")
                                showBlockMenu = false
                            }
                        )
                    }
                }

                // Font Size Selector
                Box {
                    TextButton(
                        onClick = { showSizeMenu = true },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        modifier = Modifier.defaultMinSize(minHeight = 40.dp).testTag("rich_font_size_button")
                    ) {
                        Icon(Icons.Default.FormatSize, contentDescription = null, modifier = Modifier.size(17.dp))
                        Icon(Icons.Default.ArrowDropDown, contentDescription = null, modifier = Modifier.size(15.dp))
                    }
                    DropdownMenu(expanded = showSizeMenu, onDismissRequest = { showSizeMenu = false }) {
                        listOf(
                            "Small (14px)" to "14px",
                            "Normal (18px)" to "18px",
                            "Medium (21px)" to "21px",
                            "Large (26px)" to "26px",
                            "Display (32px)" to "32px"
                        ).forEach { (label, px) ->
                            DropdownMenuItem(
                                text = { Text(label) },
                                onClick = {
                                    runJs("applySelectionFontSize('$px'); syncEditorChange();")
                                    showSizeMenu = false
                                }
                            )
                        }
                    }
                }

                if (isEnabled("bold")) {
                    RichToolbarTextToggle("B", selState.bold, FontWeight.ExtraBold, { exec("bold") }, "Bold")
                }
                if (isEnabled("italic")) {
                    RichToolbarTextToggle("I", selState.italic, FontWeight.SemiBold, { exec("italic") }, "Italic")
                }
                if (isEnabled("underline")) {
                    RichToolbarTextToggle("U", selState.underline, FontWeight.SemiBold, { exec("underline") }, "Underline")
                }
                if (isEnabled("strikeThrough")) {
                    RichToolbarTextToggle("S", selState.strike, FontWeight.SemiBold, { exec("strikeThrough") }, "Strikethrough")
                }
                if (isEnabled("subscript")) {
                    RichToolbarTextToggle("X₂", selState.sub, FontWeight.Medium, { exec("subscript") }, "Subscript")
                }
                if (isEnabled("superscript")) {
                    RichToolbarTextToggle("X²", selState.sup, FontWeight.Medium, { exec("superscript") }, "Superscript")
                }
                if (isEnabled("inlineCode")) {
                    RichToolbarTextToggle("`c`", false, FontWeight.Medium, {
                        runJs("wrapInlineCode(); syncEditorChange();")
                    }, "Inline code")
                }
                if (isEnabled("foreColor")) {
                    RichToolbarIcon(
                        selected = false,
                        onClick = { showForeColorDialog = true },
                        contentDescription = "Text color"
                    ) { Icon(Icons.Default.FormatColorText, contentDescription = null, modifier = Modifier.size(18.dp)) }
                }
                if (isEnabled("hiliteColor")) {
                    RichToolbarIcon(
                        selected = false,
                        onClick = { showHiliteColorDialog = true },
                        contentDescription = "Highlight color"
                    ) { Icon(Icons.Default.BorderColor, contentDescription = null, modifier = Modifier.size(18.dp)) }
                }
                if (isEnabled("removeFormat")) {
                    RichToolbarIcon(
                        selected = false,
                        onClick = { exec("removeFormat") },
                        contentDescription = "Clear formatting"
                    ) { Icon(Icons.Default.FormatClear, contentDescription = null, modifier = Modifier.size(18.dp)) }
                }
            }
        }

        // Row 2: Lists, Alignment, Insert Media/Tables/Symbols, HTML Source Toggle
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(bottom = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (!sourceMode) {
                if (isEnabled("insertUnorderedList")) {
                    RichToolbarIcon(selState.ul, { exec("insertUnorderedList") }, "Bullet list") {
                        Icon(Icons.AutoMirrored.Filled.FormatListBulleted, contentDescription = null, modifier = Modifier.size(18.dp))
                    }
                }
                if (isEnabled("insertOrderedList")) {
                    RichToolbarIcon(selState.ol, { exec("insertOrderedList") }, "Numbered list") {
                        Icon(Icons.Default.FormatListNumbered, contentDescription = null, modifier = Modifier.size(18.dp))
                    }
                }
                if (isEnabled("checklist")) {
                    RichToolbarIcon(false, {
                        val checklist = """<ul class="ink-checklist"><li><input type="checkbox" /> Task item</li></ul><p></p>"""
                        runJs("document.execCommand('insertHTML', false, ${JSONObject.quote(checklist)}); syncEditorChange();")
                    }, "Insert checklist") {
                        Icon(Icons.Default.CheckBox, contentDescription = null, modifier = Modifier.size(18.dp))
                    }
                }
                if (isEnabled("indent")) {
                    RichToolbarIcon(false, { exec("indent") }, "Indent") {
                        Icon(Icons.AutoMirrored.Filled.FormatIndentIncrease, contentDescription = null, modifier = Modifier.size(18.dp))
                    }
                }
                if (isEnabled("outdent")) {
                    RichToolbarIcon(false, { exec("outdent") }, "Outdent") {
                        Icon(Icons.AutoMirrored.Filled.FormatIndentDecrease, contentDescription = null, modifier = Modifier.size(18.dp))
                    }
                }
                if (isEnabled("justifyLeft")) {
                    RichToolbarIcon(selState.alignLeft, { exec("justifyLeft") }, "Align left") {
                        Icon(Icons.AutoMirrored.Filled.FormatAlignLeft, contentDescription = null, modifier = Modifier.size(18.dp))
                    }
                }
                if (isEnabled("justifyCenter")) {
                    RichToolbarIcon(selState.alignCenter, { exec("justifyCenter") }, "Align center") {
                        Icon(Icons.Default.FormatAlignCenter, contentDescription = null, modifier = Modifier.size(18.dp))
                    }
                }
                if (isEnabled("justifyRight")) {
                    RichToolbarIcon(selState.alignRight, { exec("justifyRight") }, "Align right") {
                        Icon(Icons.AutoMirrored.Filled.FormatAlignRight, contentDescription = null, modifier = Modifier.size(18.dp))
                    }
                }
                if (isEnabled("justifyFull")) {
                    RichToolbarIcon(selState.alignJustify, { exec("justifyFull") }, "Justify text") {
                        Icon(Icons.Default.FormatAlignJustify, contentDescription = null, modifier = Modifier.size(18.dp))
                    }
                }
                if (isEnabled("formatBlock:BLOCKQUOTE")) {
                    RichToolbarIcon(selState.blockTag.equals("BLOCKQUOTE", true), { exec("formatBlock", "BLOCKQUOTE") }, "Blockquote") {
                        Icon(Icons.Default.FormatQuote, contentDescription = null, modifier = Modifier.size(18.dp))
                    }
                }

                VerticalDivider(modifier = Modifier.height(22.dp).padding(horizontal = 4.dp))

                TextButton(
                    onClick = { showLinkDialog = true },
                    contentPadding = PaddingValues(horizontal = 8.dp),
                    modifier = Modifier.testTag("rich_insert_link_button")
                ) {
                    Icon(Icons.Default.Link, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Link", fontSize = 12.sp)
                }
                TextButton(
                    onClick = {
                        imagePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    },
                    contentPadding = PaddingValues(horizontal = 8.dp),
                    modifier = Modifier.testTag("rich_insert_image_button")
                ) {
                    Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Image", fontSize = 12.sp)
                }
                TextButton(
                    onClick = { showTableDialog = true },
                    contentPadding = PaddingValues(horizontal = 8.dp),
                    modifier = Modifier.testTag("rich_insert_table_button")
                ) {
                    Icon(Icons.Default.TableChart, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Table", fontSize = 12.sp)
                }
                if (selState.inTable) {
                    TextButton(
                        onClick = { runJs("addTableRow(); syncEditorChange();") },
                        contentPadding = PaddingValues(horizontal = 6.dp)
                    ) { Text("+Row", fontSize = 11.sp) }
                    TextButton(
                        onClick = { runJs("addTableCol(); syncEditorChange();") },
                        contentPadding = PaddingValues(horizontal = 6.dp)
                    ) { Text("+Col", fontSize = 11.sp) }
                }
                TextButton(
                    onClick = { showSymbolsDialog = true },
                    contentPadding = PaddingValues(horizontal = 8.dp)
                ) { Text("✦ Symbols", fontSize = 12.sp) }
                if (isEnabled("insertHorizontalRule")) {
                    TextButton(
                        onClick = { exec("insertHorizontalRule") },
                        contentPadding = PaddingValues(horizontal = 8.dp)
                    ) { Text("Rule", fontSize = 12.sp) }
                }
                TextButton(
                    onClick = {
                        val pageBreak = """<div class="page-break"></div><p></p>"""
                        runJs("document.execCommand('insertHTML', false, ${JSONObject.quote(pageBreak)}); syncEditorChange();")
                    },
                    contentPadding = PaddingValues(horizontal = 8.dp)
                ) { Text("Page Break", fontSize = 12.sp) }
            }

            FilterChip(
                selected = sourceMode,
                onClick = { sourceMode = !sourceMode },
                label = { Text(if (sourceMode) "Visual Editor" else "</> HTML", fontSize = 11.sp) },
                modifier = Modifier.padding(start = 4.dp).testTag("rich_source_toggle_button")
            )
        }

        if (sourceMode) {
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 13.sp, lineHeight = 19.sp),
                label = { Text("HTML Source") },
                modifier = Modifier.weight(1f).fillMaxWidth().padding(top = 4.dp)
            )
        } else {
            AndroidView(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                factory = { ctx ->
                    WebView(ctx).apply {
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = false
                        isVerticalScrollBarEnabled = true
                        webViewClient = WebViewClient()
                        addJavascriptInterface(object {
                            @JavascriptInterface
                            fun changed(html: String) {
                                callback(html)
                            }

                            @JavascriptInterface
                            fun selectionChanged(stateJson: String) {
                                runCatching {
                                    val o = JSONObject(stateJson)
                                    val next = EditorSelectionState(
                                        bold = o.optBoolean("bold"),
                                        italic = o.optBoolean("italic"),
                                        underline = o.optBoolean("underline"),
                                        strike = o.optBoolean("strike"),
                                        sub = o.optBoolean("sub"),
                                        sup = o.optBoolean("sup"),
                                        ul = o.optBoolean("ul"),
                                        ol = o.optBoolean("ol"),
                                        alignLeft = o.optBoolean("alignLeft"),
                                        alignCenter = o.optBoolean("alignCenter"),
                                        alignRight = o.optBoolean("alignRight"),
                                        alignJustify = o.optBoolean("alignJustify"),
                                        blockTag = o.optString("blockTag", "P"),
                                        inTable = o.optBoolean("inTable"),
                                        linkHref = o.optString("linkHref", ""),
                                        selectedText = o.optString("selectedText", "")
                                    )
                                    post { selState = next }
                                }
                            }
                        }, "InkBridge")
                        val initial = JSONObject.quote(value)
                        val resolvedFont = if (customFontData.isNotBlank()) "Custom" else fontFamily
                        val page = buildRichEditorHtml(
                            initialQuoted = initial,
                            fontFamily = resolvedFont,
                            customFontData = customFontData,
                            customFontMime = customFontMime,
                            fontSize = fontSize,
                            lineSpacing = lineSpacing,
                            pageMode = pageMode,
                            pageWidth = pageWidth,
                            pageMargin = pageMargin,
                            paragraphSpacing = paragraphSpacing,
                            backgroundColor = backgroundColor,
                            backgroundImageData = backgroundImageData,
                            backgroundImageMime = backgroundImageMime,
                            textColor = textColor
                        )
                        loadDataWithBaseURL(null, page, "text/html", "UTF-8", null)
                    }.also { web.value = it }
                },
                update = { view ->
                    val resolvedFont = if (customFontData.isNotBlank()) "Custom" else fontFamily
                    val bgImageCss = if (backgroundImageData.isNotBlank()) "url(data:$backgroundImageMime;base64,$backgroundImageData)" else "none"
                    view.evaluateJavascript(
                        """
                        document.body.style.fontSize='${fontSize}px';
                        document.body.style.fontFamily='$resolvedFont';
                        document.body.style.lineHeight='$lineSpacing';
                        document.body.style.maxWidth='${if (pageMode) "${pageWidth}px" else "none"}';
                        document.body.style.minHeight='${if (pageMode) "1056px" else "95vh"}';
                        document.body.style.boxSizing='border-box';
                        document.body.style.boxShadow='${if (pageMode) "0 4px 22px rgba(0,0,0,.25)" else "none"}';
                        document.body.style.padding='${pageMargin}px';
                        document.body.style.background='$backgroundColor';
                        document.body.style.backgroundImage='$bgImageCss';
                        document.body.style.backgroundSize='cover';
                        document.body.style.backgroundAttachment='fixed';
                        document.body.style.color='$textColor';
                        document.querySelectorAll('p').forEach(p=>p.style.marginBottom='${paragraphSpacing}px');
                        """.trimIndent(),
                        null
                    )
                    if (!view.hasFocus()) {
                        val q = JSONObject.quote(value)
                        view.evaluateJavascript("setEditorHtml($q)", null)
                    }
                    if (findTerm.isNotBlank()) {
                        view.setFindListener { _, count, done ->
                            if (done) {
                                if (count > 0) repeat(findOccurrence.coerceIn(0, count - 1)) { view.findNext(true) }
                                findCallback()
                            }
                        }
                        view.findAllAsync(findTerm)
                    }
                }
            )
        }
    }

    if (showForeColorDialog) {
        RichColorPickerDialog(
            title = "Text Color",
            swatches = TEXT_COLOR_SWATCHES,
            onPick = { hex ->
                exec("foreColor", hex)
                showForeColorDialog = false
            },
            onClear = {
                exec("foreColor", textColor)
                showForeColorDialog = false
            },
            onDismiss = { showForeColorDialog = false }
        )
    }

    if (showHiliteColorDialog) {
        RichColorPickerDialog(
            title = "Highlight Color",
            swatches = HIGHLIGHT_COLOR_SWATCHES,
            onPick = { hex ->
                exec("hiliteColor", hex)
                showHiliteColorDialog = false
            },
            onClear = {
                exec("hiliteColor", "transparent")
                showHiliteColorDialog = false
            },
            onDismiss = { showHiliteColorDialog = false }
        )
    }

    if (showLinkDialog) {
        RichLinkDialog(
            initialUrl = selState.linkHref,
            initialText = selState.selectedText,
            hasActiveLink = selState.linkHref.isNotBlank(),
            onInsert = { url, displayText ->
                if (displayText.isNotBlank()) {
                    val anchor = """<a href="${escapeHtmlAttr(url)}">${escapeHtmlText(displayText)}</a>"""
                    runJs("document.execCommand('insertHTML', false, ${JSONObject.quote(anchor)}); syncEditorChange();")
                } else {
                    exec("createLink", url)
                }
                showLinkDialog = false
            },
            onUnlink = {
                exec("unlink")
                showLinkDialog = false
            },
            onDismiss = { showLinkDialog = false }
        )
    }

    if (showTableDialog) {
        RichTableBuilderDialog(
            onInsert = { rows, cols, includeHeader ->
                val tableHtml = buildHtmlTable(rows, cols, includeHeader)
                runJs("document.execCommand('insertHTML', false, ${JSONObject.quote(tableHtml)}); syncEditorChange();")
                showTableDialog = false
            },
            onDismiss = { showTableDialog = false }
        )
    }

    if (showSymbolsDialog) {
        RichSymbolsDialog(
            onPick = { symbol ->
                runJs("document.execCommand('insertText', false, ${JSONObject.quote(symbol)}); syncEditorChange();")
                showSymbolsDialog = false
            },
            onDismiss = { showSymbolsDialog = false }
        )
    }
}

@Composable
private fun RichToolbarIcon(
    selected: Boolean,
    onClick: () -> Unit,
    contentDescription: String,
    content: @Composable () -> Unit
) {
    FilledTonalIconToggleButton(
        checked = selected,
        onCheckedChange = { onClick() },
        modifier = Modifier.size(40.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            content()
        }
    }
}

@Composable
private fun RichToolbarTextToggle(
    label: String,
    selected: Boolean,
    weight: FontWeight,
    onClick: () -> Unit,
    contentDescription: String
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        onClick = onClick,
        modifier = Modifier.defaultMinSize(minWidth = 38.dp, minHeight = 40.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
        ) {
            Text(
                text = label,
                fontSize = 13.sp,
                fontWeight = weight,
                color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

private fun buildHtmlTable(rows: Int, cols: Int, includeHeader: Boolean): String {
    val sb = StringBuilder()
    sb.append("""<table style="border-collapse:collapse;width:100%;margin:10px 0;">""")
    if (includeHeader) {
        sb.append("<thead><tr>")
        for (c in 1..cols) {
            sb.append("""<th style="border:1px solid #8b8495;padding:8px;text-align:left;background:rgba(183,154,216,0.18);">Header $c</th>""")
        }
        sb.append("</tr></thead>")
    }
    sb.append("<tbody>")
    val bodyRows = if (includeHeader) (rows - 1).coerceAtLeast(1) else rows
    for (r in 1..bodyRows) {
        sb.append("<tr>")
        for (c in 1..cols) {
            sb.append("""<td style="border:1px solid #8b8495;padding:8px;">Cell</td>""")
        }
        sb.append("</tr>")
    }
    sb.append("</tbody></table><p></p>")
    return sb.toString()
}

private fun escapeHtmlText(text: String): String = text
    .replace("&", "&amp;")
    .replace("<", "&lt;")
    .replace(">", "&gt;")

private fun escapeHtmlAttr(text: String): String = escapeHtmlText(text).replace("\"", "&quot;")

private fun buildRichEditorHtml(
    initialQuoted: String,
    fontFamily: String,
    customFontData: String,
    customFontMime: String,
    fontSize: Float,
    lineSpacing: Float,
    pageMode: Boolean,
    pageWidth: Float,
    pageMargin: Float,
    paragraphSpacing: Float,
    backgroundColor: String,
    backgroundImageData: String,
    backgroundImageMime: String,
    textColor: String
): String {
    val bgImg = if (backgroundImageData.isNotBlank()) "url(data:$backgroundImageMime;base64,$backgroundImageData)" else "none"
    val borderCss = if (pageMode) "box-shadow:0 4px 22px rgba(0,0,0,.25);border:1px solid #8b8495;" else ""
    return """<!doctype html><html><head><meta name="viewport" content="width=device-width,initial-scale=1"><style>
@font-face{font-family:Custom;src:url(data:$customFontMime;base64,$customFontData)}
body{font-family:$fontFamily;font-size:${fontSize}px;line-height:$lineSpacing;padding:${pageMargin}px;margin:0 auto;max-width:${if (pageMode) "${pageWidth}px" else "none"};min-height:${if (pageMode) "1056px" else "95vh"};box-sizing:border-box;${borderCss}background:$backgroundColor;background-image:$bgImg;background-size:cover;background-attachment:fixed;color:$textColor}
p{margin-bottom:${paragraphSpacing}px}
#editor{min-height:95vh;outline:none;overflow-wrap:anywhere}
blockquote{border-left:3px solid #bfa7df;padding:6px 12px;margin:10px 0;background:rgba(191,167,223,0.08);border-radius:0 8px 8px 0;font-style:italic}
pre, code{font-family:monospace;background:rgba(128,128,128,0.18);border-radius:6px;padding:2px 5px}
pre{padding:10px 12px;overflow-x:auto}
.ink-callout{border-left:4px solid #c9b6e4;background:rgba(201,182,228,0.14);padding:10px 14px;border-radius:0 12px 12px 0;margin:12px 0}
.ink-dropcap::first-letter{float:left;font-size:3.1em;line-height:0.85;padding-right:8px;font-weight:bold;color:#c9b6e4}
ul.ink-checklist{list-style:none;padding-left:4px}
ul.ink-checklist li{display:flex;align-items:flex-start;gap:8px;margin:4px 0}
table{border-collapse:collapse;width:100%;margin:10px 0}
th,td{border:1px solid #8b8495;padding:8px}
img{max-width:100%;height:auto;border-radius:8px}
h1,h2,h3,h4{line-height:1.25;margin-top:0.9em;margin-bottom:0.4em}
a{color:#bfa7df}
.page-break{break-after:page;page-break-after:always;border-top:1px dashed #8b8495;margin:14px 0;height:1px}
@media print{.page-break{break-after:page;page-break-after:always;border:none}}
</style></head><body><div id="editor" contenteditable="true"></div><script>
const editor=document.getElementById('editor');
editor.innerHTML=$initialQuoted;

function syncEditorChange(){
    InkBridge.changed(editor.innerHTML);
    reportSelectionState();
}

editor.addEventListener('input', syncEditorChange);
editor.addEventListener('change', function(e){
    if(e.target && e.target.type==='checkbox'){
        if(e.target.checked) e.target.setAttribute('checked','checked');
        else e.target.removeAttribute('checked');
        syncEditorChange();
    }
});

function setEditorHtml(v){
    if(document.activeElement!==editor && editor.innerHTML!==v){
        editor.innerHTML=v;
    }
}

function applySelectionFontSize(px){
    const sel=window.getSelection();
    if(!sel || sel.rangeCount===0) return;
    if(sel.isCollapsed){
        const block=currentBlockElement();
        if(block) block.style.fontSize=px;
        return;
    }
    const span=document.createElement('span');
    span.style.fontSize=px;
    try{
        const range=sel.getRangeAt(0);
        const content=range.extractContents();
        span.appendChild(content);
        range.insertNode(span);
    }catch(e){}
}

function wrapInlineCode(){
    const sel=window.getSelection();
    if(!sel || sel.rangeCount===0) return;
    const text=sel.toString() || 'code';
    document.execCommand('insertHTML', false, '<code>'+text.replace(/</g,'&lt;')+'</code>');
}

function toggleDropCap(){
    const block=currentBlockElement();
    if(block){
        block.classList.toggle('ink-dropcap');
    }
}

function currentBlockElement(){
    const sel=window.getSelection();
    if(!sel || sel.rangeCount===0) return null;
    let node=sel.anchorNode;
    while(node && node!==editor){
        if(node.nodeType===1 && /^(P|DIV|H1|H2|H3|H4|BLOCKQUOTE|PRE|LI)$/i.test(node.tagName)){
            return node;
        }
        node=node.parentNode;
    }
    return null;
}

function findAncestorTag(tagName){
    const sel=window.getSelection();
    if(!sel || sel.rangeCount===0) return null;
    let node=sel.anchorNode;
    while(node && node!==editor){
        if(node.nodeType===1 && node.tagName && node.tagName.toUpperCase()===tagName) return node;
        node=node.parentNode;
    }
    return null;
}

function addTableRow(){
    const td=findAncestorTag('TD') || findAncestorTag('TH');
    const tr=td ? td.parentElement : null;
    if(!tr || !tr.parentElement) return;
    const newRow=document.createElement('tr');
    const cols=tr.children.length || 2;
    for(let i=0;i<cols;i++){
        const cell=document.createElement('td');
        cell.style.cssText='border:1px solid #8b8495;padding:8px;';
        cell.textContent='Cell';
        newRow.appendChild(cell);
    }
    tr.parentElement.appendChild(newRow);
}

function addTableCol(){
    const table=findAncestorTag('TABLE');
    if(!table) return;
    table.querySelectorAll('tr').forEach(tr=>{
        const isHead=tr.parentElement && tr.parentElement.tagName==='THEAD';
        const cell=document.createElement(isHead?'th':'td');
        cell.style.cssText=isHead?'border:1px solid #8b8495;padding:8px;text-align:left;background:rgba(183,154,216,0.18);':'border:1px solid #8b8495;padding:8px;';
        cell.textContent=isHead?'Header':'Cell';
        tr.appendChild(cell);
    });
}

function reportSelectionState(){
    try{
        const anchor=findAncestorTag('A');
        const table=findAncestorTag('TABLE');
        const block=currentBlockElement();
        const sel=window.getSelection();
        const state={
            bold:document.queryCommandState('bold'),
            italic:document.queryCommandState('italic'),
            underline:document.queryCommandState('underline'),
            strike:document.queryCommandState('strikeThrough'),
            sub:document.queryCommandState('subscript'),
            sup:document.queryCommandState('superscript'),
            ul:document.queryCommandState('insertUnorderedList'),
            ol:document.queryCommandState('insertOrderedList'),
            alignLeft:document.queryCommandState('justifyLeft'),
            alignCenter:document.queryCommandState('justifyCenter'),
            alignRight:document.queryCommandState('justifyRight'),
            alignJustify:document.queryCommandState('justifyFull'),
            blockTag:block ? block.tagName : 'P',
            inTable:!!table,
            linkHref:anchor ? (anchor.getAttribute('href')||'') : '',
            selectedText:sel ? sel.toString() : ''
        };
        InkBridge.selectionChanged(JSON.stringify(state));
    }catch(e){}
}

document.addEventListener('selectionchange', reportSelectionState);
editor.addEventListener('keyup', reportSelectionState);
editor.addEventListener('mouseup', reportSelectionState);
</script></body></html>"""
}
