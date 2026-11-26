package com.parasaurpal.inkandquill.ui

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.parasaurpal.inkandquill.data.*

@Composable
internal fun ArchetypeInspectorDialog(
    archetype: TemplateLibraryArchetype,
    targetRecord: InkRecord?,
    onDismiss: () -> Unit,
    onImportIntoSheet: () -> Unit,
    onCreateEntry: () -> Unit,
    onImportBlueprint: () -> Unit
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    var tab by remember { mutableStateOf("Structure") }

    val exportJsonLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        if (uri != null) {
            runCatching {
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    out.write(archetype.rawJson.toByteArray())
                }
            }.onSuccess {
                Toast.makeText(context, "Exported ${archetype.fileName}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(archetype.title, fontWeight = FontWeight.Bold)
                Text(
                    "${archetype.libraryCategory} · ${archetype.group} · ${archetype.sections.size} sections · ${archetype.fields.size} fields",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().heightIn(max = 460.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    SegmentedButton(
                        selected = tab == "Structure",
                        onClick = { tab = "Structure" },
                        shape = SegmentedButtonDefaults.itemShape(0, 2)
                    ) { Text("Fields") }
                    SegmentedButton(
                        selected = tab == "JSON",
                        onClick = { tab = "JSON" },
                        shape = SegmentedButtonDefaults.itemShape(1, 2)
                    ) { Text("Pre-built JSON") }
                }

                if (tab == "Structure") {
                    Column(
                        modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val grouped = archetype.fields.groupBy { it.section }
                        archetype.sections.forEach { sec ->
                            Text(
                                text = sec,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            grouped[sec].orEmpty().forEach { f ->
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = f.name + if (f.required) " *" else "",
                                                fontWeight = FontWeight.SemiBold,
                                                fontSize = 13.sp,
                                                modifier = Modifier.weight(1f)
                                            )
                                            Text(
                                                text = f.type,
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                        if (f.hint.isNotBlank()) {
                                            Text(
                                                text = f.hint,
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        if (f.options.isNotBlank()) {
                                            Text(
                                                text = "Options: ${f.options.replace("|", ", ")}",
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(
                            onClick = {
                                clipboard.setText(AnnotatedString(archetype.rawJson))
                                Toast.makeText(context, "JSON copied to clipboard.", Toast.LENGTH_SHORT).show()
                            }
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Copy JSON")
                        }
                        TextButton(
                            onClick = { exportJsonLauncher.launch("${archetype.id}.inktemplate.json") }
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Save .json")
                        }
                    }
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.weight(1f).fillMaxWidth()
                    ) {
                        Text(
                            text = archetype.rawJson,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            lineHeight = 15.sp,
                            modifier = Modifier
                                .padding(10.dp)
                                .verticalScroll(rememberScrollState())
                                .horizontalScroll(rememberScrollState())
                        )
                    }
                }
            }
        },
        confirmButton = {
            if (targetRecord != null) {
                Button(onClick = onImportIntoSheet) {
                    Text("Import into Sheet")
                }
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    TextButton(onClick = onImportBlueprint) {
                        Text("To Templates")
                    }
                    Button(onClick = onCreateEntry) {
                        Text("Use Sheet")
                    }
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
internal fun ImportIntoSheetDialog(
    db: InkDatabase,
    record: InkRecord,
    archetype: TemplateLibraryArchetype,
    onDismiss: () -> Unit,
    onConfirm: (SheetImportMode, Boolean) -> Unit
) {
    val hasExistingSheet = remember(record.id) { db.sheetTemplateId(record.id).isNotBlank() }
    var mode by remember(hasExistingSheet) {
        mutableStateOf(
            if (hasExistingSheet) SheetImportMode.MERGE_INTO_CURRENT
            else SheetImportMode.ATTACH_NEW_SHEET
        )
    }
    var prefillSamples by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Import \"${archetype.title}\"") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "Choose how to apply this ${archetype.sections.size}-section JSON structure (${archetype.fields.size} fields) to ${record.title.ifBlank { "this entry" }}:",
                    style = MaterialTheme.typography.bodySmall
                )
                if (hasExistingSheet) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = mode == SheetImportMode.MERGE_INTO_CURRENT,
                            onClick = { mode = SheetImportMode.MERGE_INTO_CURRENT },
                            label = { Text("Merge fields") },
                            modifier = Modifier.testTag("import_mode_merge")
                        )
                        FilterChip(
                            selected = mode == SheetImportMode.ATTACH_NEW_SHEET,
                            onClick = { mode = SheetImportMode.ATTACH_NEW_SHEET },
                            label = { Text("Replace sheet") },
                            modifier = Modifier.testTag("import_mode_replace")
                        )
                    }
                    Text(
                        text = if (mode == SheetImportMode.MERGE_INTO_CURRENT) {
                            "Adds missing sections and fields from ${archetype.title} into your current sheet while keeping all existing fields and values."
                        } else {
                            "Switches this entry to the ${archetype.title} sheet and carries over any matching field values."
                        },
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (archetype.sampleValues.isNotEmpty()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = prefillSamples,
                            onCheckedChange = { prefillSamples = it }
                        )
                        Text(
                            "Fill blank fields with example fantasy content",
                            fontSize = 13.sp
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(mode, prefillSamples) },
                modifier = Modifier.testTag("confirm_sheet_library_import_button")
            ) {
                Text("Import")
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
internal fun CustomJsonArchetypeDialog(
    hasTargetSheet: Boolean,
    onDismiss: () -> Unit,
    onParsedImport: (TemplateLibraryArchetype) -> Unit
) {
    val context = LocalContext.current
    var rawJson by remember { mutableStateOf("") }
    var parseError by remember { mutableStateOf("") }

    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) {
            runCatching {
                context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }.orEmpty()
            }.onSuccess { text ->
                rawJson = text
                parseError = ""
            }.onFailure { err ->
                parseError = err.message ?: "Could not read file."
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Import Archetype JSON") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    "Paste an Ink & Quill template JSON structure or load a .json file from your device.",
                    style = MaterialTheme.typography.bodySmall
                )
                OutlinedButton(
                    onClick = { filePicker.launch(arrayOf("application/json", "text/*")) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Load .json file")
                }
                OutlinedTextField(
                    value = rawJson,
                    onValueChange = {
                        rawJson = it
                        parseError = ""
                    },
                    label = { Text("Template JSON") },
                    placeholder = { Text("{\"format\":\"inkandquill-template\",\"title\":\"...\",\"sections\":[...],\"fields\":[...]}") },
                    minLines = 5,
                    maxLines = 8,
                    modifier = Modifier.fillMaxWidth().testTag("custom_json_archetype_input")
                )
                if (parseError.isNotBlank()) {
                    Text(
                        text = parseError,
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 12.sp
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    runCatching {
                        TemplateLibraryManager.parseArchetypeJson(rawJson, "custom_import.json")
                    }.onSuccess { parsed ->
                        onParsedImport(parsed)
                    }.onFailure { err ->
                        parseError = err.message ?: "Invalid template JSON structure."
                    }
                },
                enabled = rawJson.isNotBlank()
            ) {
                Text(if (hasTargetSheet) "Next" else "Import JSON")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
