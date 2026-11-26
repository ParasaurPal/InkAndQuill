package com.parasaurpal.inkandquill.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

internal val TEXT_COLOR_SWATCHES = listOf(
    "Parchment" to "#EEE8F5",
    "Obsidian" to "#17141E",
    "Plum" to "#C9B6E4",
    "Amethyst" to "#B79AD8",
    "Gold" to "#E8C56E",
    "Crimson" to "#E07A7A",
    "Emerald" to "#82C99A",
    "Azure" to "#82B7E8",
    "Amber" to "#E59D64",
    "Silver" to "#B5B2BE"
)

internal val HIGHLIGHT_COLOR_SWATCHES = listOf(
    "Candlelight" to "#FFF2A8",
    "Plum Mist" to "#523B6E",
    "Emerald Glow" to "#2C5E44",
    "Crimson Wash" to "#6B2D35",
    "Cobalt Tint" to "#2C4C6E",
    "Amber Tint" to "#6E4A24",
    "Soft Rose" to "#F5C6D6",
    "Mint" to "#C7F0D8"
)

internal val FANTASY_SYMBOLS = listOf(
    "—", "–", "…", "§", "†", "‡", "✦", "✧", "❧", "«", "»", "•", "°", "×", "※", "¶"
)

@Composable
internal fun RichColorPickerDialog(
    title: String,
    swatches: List<Pair<String, String>>,
    onPick: (String) -> Unit,
    onClear: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                swatches.chunked(5).forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        row.forEach { (label, hex) ->
                            val parsedColor = remember(hex) {
                                runCatching { Color(android.graphics.Color.parseColor(hex)) }.getOrDefault(Color.Gray)
                            }
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .clickable { onPick(hex) }
                                    .padding(4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(parsedColor)
                                        .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                                )
                                Text(label, fontSize = 10.sp, maxLines = 1)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onClear) {
                Text("Reset Color")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
internal fun RichLinkDialog(
    initialUrl: String,
    initialText: String,
    hasActiveLink: Boolean,
    onInsert: (url: String, text: String) -> Unit,
    onUnlink: () -> Unit,
    onDismiss: () -> Unit
) {
    var url by remember(initialUrl) { mutableStateOf(initialUrl.ifBlank { "https://" }) }
    var displayText by remember(initialText) { mutableStateOf(initialText) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (hasActiveLink) "Edit Hyperlink" else "Insert Hyperlink") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = displayText,
                    onValueChange = { displayText = it },
                    label = { Text("Display text (optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text("URL (https:// or http://)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("rich_link_url_input")
                )
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (hasActiveLink) {
                    TextButton(onClick = onUnlink) {
                        Text("Unlink", color = MaterialTheme.colorScheme.error)
                    }
                }
                Button(
                    enabled = url.startsWith("https://") || url.startsWith("http://"),
                    onClick = { onInsert(url.trim(), displayText.trim()) }
                ) {
                    Text("Apply")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
internal fun RichTableBuilderDialog(
    onInsert: (rows: Int, cols: Int, includeHeader: Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    var rows by remember { mutableIntStateOf(3) }
    var cols by remember { mutableIntStateOf(3) }
    var includeHeader by remember { mutableStateOf(true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Insert Table") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Rows: $rows", fontWeight = FontWeight.Medium)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        FilledTonalButton(
                            onClick = { if (rows > 1) rows-- },
                            modifier = Modifier.defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                        ) { Text("-") }
                        FilledTonalButton(
                            onClick = { if (rows < 12) rows++ },
                            modifier = Modifier.defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                        ) { Text("+") }
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Columns: $cols", fontWeight = FontWeight.Medium)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        FilledTonalButton(
                            onClick = { if (cols > 1) cols-- },
                            modifier = Modifier.defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                        ) { Text("-") }
                        FilledTonalButton(
                            onClick = { if (cols < 8) cols++ },
                            modifier = Modifier.defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                        ) { Text("+") }
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = includeHeader, onCheckedChange = { includeHeader = it })
                    Text("Include styled header row")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onInsert(rows, cols, includeHeader) },
                modifier = Modifier.testTag("confirm_insert_table_button")
            ) {
                Text("Insert ${rows}×${cols}")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
internal fun RichSymbolsDialog(
    onPick: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Insert Special Symbol") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                FANTASY_SYMBOLS.chunked(4).forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        row.forEach { symbol ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                tonalElevation = 2.dp,
                                modifier = Modifier
                                    .size(52.dp)
                                    .clickable { onPick(symbol) }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(symbol, fontSize = 20.sp, fontWeight = FontWeight.Medium)
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}
