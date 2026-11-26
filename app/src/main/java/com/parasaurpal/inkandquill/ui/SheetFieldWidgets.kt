package com.parasaurpal.inkandquill.ui

import android.Manifest
import android.app.DatePickerDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.parasaurpal.inkandquill.data.*
import java.util.Calendar

private val FantasySwatches = listOf(
    "#C9B6E4", "#8DB9D8", "#9DBD9A", "#E0A8B8", "#D9B66F",
    "#D64545", "#4A6FA5", "#5B8C5A", "#7A4988", "#2B2D42",
    "#8D5B4C", "#F4EFE6"
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SheetFieldInput(
    db: InkDatabase,
    recordId: String,
    field: TemplateField,
    allFields: List<TemplateField>,
    values: Map<String, String>,
    onValueChange: (String) -> Unit,
    onOpenRecord: (InkRecord) -> Unit,
    fontSize: Float
) {
    val context = LocalContext.current
    val value = values[field.id].orEmpty()

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = field.name + if (field.required) " *" else "",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            if (field.required && value.isBlank() && field.fieldType != "Calculated") {
                Text(
                    text = "Required",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.Medium
                )
            }
        }
        if (field.hint.isNotBlank() && field.fieldType != "Table") {
            Text(
                text = field.hint,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        when (field.fieldType) {
            "Short text" -> {
                OutlinedTextField(
                    value = value,
                    onValueChange = onValueChange,
                    modifier = Modifier.fillMaxWidth().testTag("sheet_field_${field.id}"),
                    singleLine = true,
                    placeholder = { Text("Enter ${field.name.lowercase()}") }
                )
            }

            "Long text" -> {
                OutlinedTextField(
                    value = value,
                    onValueChange = onValueChange,
                    modifier = Modifier.fillMaxWidth().testTag("sheet_field_${field.id}"),
                    minLines = 3,
                    maxLines = 10,
                    placeholder = { Text("Write ${field.name.lowercase()}...") }
                )
            }

            "Rich text" -> {
                var showRichEditor by remember { mutableStateOf(false) }
                val plainPreview = remember(value) { DocumentExport.plain(value).trim() }
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showRichEditor = true }
                        .testTag("sheet_richtext_${field.id}")
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = if (plainPreview.isBlank()) "Tap to write formatted text..." else plainPreview,
                            maxLines = 5,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (plainPreview.isBlank()) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            FilledTonalButton(onClick = { showRichEditor = true }) {
                                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(if (plainPreview.isBlank()) "Write Formatted Text" else "Edit Formatted Text")
                            }
                        }
                    }
                }
                if (showRichEditor) {
                    var draftHtml by remember(field.id, showRichEditor) { mutableStateOf(value) }
                    Dialog(
                        onDismissRequest = {
                            onValueChange(draftHtml)
                            showRichEditor = false
                        },
                        properties = DialogProperties(usePlatformDefaultWidth = false)
                    ) {
                        Surface(
                            modifier = Modifier.fillMaxSize().padding(12.dp),
                            shape = RoundedCornerShape(20.dp),
                            color = MaterialTheme.colorScheme.background
                        ) {
                            Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        field.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.weight(1f)
                                    )
                                    TextButton(onClick = { showRichEditor = false }) { Text("Cancel") }
                                    Button(onClick = {
                                        onValueChange(draftHtml)
                                        showRichEditor = false
                                    }) { Text("Apply") }
                                }
                                HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))
                                RichTextEditor(
                                    value = draftHtml,
                                    onValueChange = { draftHtml = it },
                                    modifier = Modifier.weight(1f).fillMaxWidth(),
                                    fontSize = fontSize
                                )
                            }
                        }
                    }
                }
            }

            "Number" -> {
                OutlinedTextField(
                    value = value,
                    onValueChange = onValueChange,
                    modifier = Modifier.fillMaxWidth().testTag("sheet_number_${field.id}"),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    placeholder = { Text("0") }
                )
            }

            "Date" -> {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = value,
                        onValueChange = onValueChange,
                        modifier = Modifier.weight(1f).testTag("sheet_date_${field.id}"),
                        singleLine = true,
                        placeholder = { Text("YYYY-MM-DD or fantasy era date") }
                    )
                    FilledTonalIconButton(
                        onClick = {
                            val cal = Calendar.getInstance()
                            DatePickerDialog(
                                context,
                                { _, y, m, d -> onValueChange("%04d-%02d-%02d".format(y, m + 1, d)) },
                                cal.get(Calendar.YEAR),
                                cal.get(Calendar.MONTH),
                                cal.get(Calendar.DAY_OF_MONTH)
                            ).show()
                        },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(Icons.Default.CalendarMonth, contentDescription = "Pick calendar date")
                    }
                }
            }

            "Dropdown" -> {
                var expanded by remember { mutableStateOf(false) }
                val options = remember(field.options) {
                    field.options.split(",").map { it.trim() }.filter { it.isNotBlank() }
                }
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = { expanded = true },
                        modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 48.dp)
                            .testTag("sheet_dropdown_${field.id}")
                    ) {
                        Text(
                            text = value.ifBlank { "Select ${field.name.lowercase()}" },
                            modifier = Modifier.weight(1f),
                            color = if (value.isBlank()) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
                        )
                        Icon(Icons.Default.ArrowDropDown, contentDescription = "Open options")
                    }
                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        if (!field.required && value.isNotBlank()) {
                            DropdownMenuItem(
                                text = { Text("Clear selection") },
                                onClick = {
                                    onValueChange("")
                                    expanded = false
                                }
                            )
                        }
                        options.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option) },
                                onClick = {
                                    onValueChange(option)
                                    expanded = false
                                },
                                trailingIcon = if (option == value) {
                                    { Icon(Icons.Default.Check, contentDescription = null) }
                                } else null
                            )
                        }
                    }
                }
            }

            "Multi-select" -> {
                val selectedSet = remember(value) {
                    value.split("|").map { it.trim() }.filter { it.isNotBlank() }.toSet()
                }
                val options = remember(field.options, selectedSet) {
                    (field.options.split(",").map { it.trim() }.filter { it.isNotBlank() } + selectedSet).distinct()
                }
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    options.forEach { option ->
                        val isSelected = option in selectedSet
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                val next = if (isSelected) selectedSet - option else selectedSet + option
                                onValueChange(next.joinToString("|"))
                            },
                            label = { Text(option) }
                        )
                    }
                }
            }

            "Checkbox" -> {
                val checked = value == "true"
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 48.dp)
                        .clickable { onValueChange((!checked).toString()) }
                        .testTag("sheet_checkbox_${field.id}")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = checked,
                            onCheckedChange = { onValueChange(it.toString()) }
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = if (checked) "Yes" else "No",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            "Color" -> {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FantasySwatches.forEach { hex ->
                            val swatchColor = remember(hex) {
                                runCatching { Color(android.graphics.Color.parseColor(hex)) }
                                    .getOrDefault(Color.Gray)
                            }
                            val selected = value.equals(hex, ignoreCase = true)
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(swatchColor)
                                    .border(
                                        width = if (selected) 3.dp else 1.dp,
                                        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                        shape = CircleShape
                                    )
                                    .clickable { onValueChange(hex) }
                            )
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val preview = remember(value) {
                            runCatching { Color(android.graphics.Color.parseColor(value)) }.getOrNull()
                        }
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(preview ?: MaterialTheme.colorScheme.surfaceVariant)
                                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                        )
                        OutlinedTextField(
                            value = value,
                            onValueChange = onValueChange,
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            placeholder = { Text("#C9B6E4 or color name") }
                        )
                    }
                }
            }

            "Rating" -> {
                val current = value.toIntOrNull()?.coerceIn(0, 5) ?: 0
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    (1..5).forEach { star ->
                        IconButton(
                            onClick = { onValueChange(if (current == star) "0" else star.toString()) },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                imageVector = if (star <= current) Icons.Default.Star else Icons.Default.StarBorder,
                                contentDescription = "Rate $star of 5",
                                tint = if (star <= current) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = if (current == 0) "Unrated" else "$current / 5",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            "Image" -> {
                var pendingCam by remember { mutableStateOf<PendingCameraCapture?>(null) }
                val takePhotoLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { saved ->
                    val pending = pendingCam
                    pendingCam = null
                    if (saved && pending != null) {
                        val recKind = db.get(recordId)?.kind.orEmpty()
                        val asset = MediaManager.finalizeCameraCapture(
                            db = db,
                            pending = pending,
                            recordId = recordId,
                            templateId = field.templateId,
                            fieldId = field.id,
                            subjectKind = recKind,
                            displayName = "${field.name} photo"
                        )
                        if (asset != null) {
                            onValueChange(asset.uri)
                        }
                    } else if (pending != null) {
                        runCatching { pending.file.delete() }
                    }
                }
                val camPermLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
                    if (granted) {
                        val dest = MediaManager.createCameraDestination(context)
                        pendingCam = dest
                        takePhotoLauncher.launch(dest.contentUri)
                    } else {
                        Toast.makeText(context, "Camera permission denied. You can still choose a photo.", Toast.LENGTH_SHORT).show()
                    }
                }
                val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri: Uri? ->
                    uri?.let {
                        val recKind = db.get(recordId)?.kind.orEmpty()
                        val asset = MediaManager.importPickedPhoto(
                            context = context,
                            db = db,
                            sourceUri = it,
                            recordId = recordId,
                            templateId = field.templateId,
                            fieldId = field.id,
                            subjectKind = recKind
                        )
                        if (asset != null) {
                            onValueChange(asset.uri)
                        }
                    }
                }
                val bitmap = remember(value) { MediaManager.decodeImageBitmap(context, value) }
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (bitmap != null) {
                        Image(
                            bitmap = bitmap,
                            contentDescription = field.name,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 220.dp)
                                .clip(RoundedCornerShape(12.dp)),
                            contentScale = ContentScale.Crop
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(onClick = {
                            val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
                            if (granted) {
                                val dest = MediaManager.createCameraDestination(context)
                                pendingCam = dest
                                takePhotoLauncher.launch(dest.contentUri)
                            } else {
                                camPermLauncher.launch(Manifest.permission.CAMERA)
                            }
                        }) {
                            Icon(Icons.Default.PhotoCamera, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Take Photo")
                        }
                        FilledTonalButton(onClick = {
                            imagePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        }) {
                            Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(if (bitmap == null) "Choose Photo" else "Replace")
                        }
                        if (value.isNotBlank()) {
                            TextButton(onClick = { onValueChange("") }) { Text("Remove") }
                        }
                    }
                }
            }

            "Gallery" -> {
                val existing = remember(value) {
                    value.split("||").filter { it.isNotBlank() }
                }
                var pendingCam by remember { mutableStateOf<PendingCameraCapture?>(null) }
                val takePhotoLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { saved ->
                    val pending = pendingCam
                    pendingCam = null
                    if (saved && pending != null) {
                        val recKind = db.get(recordId)?.kind.orEmpty()
                        val asset = MediaManager.finalizeCameraCapture(
                            db = db,
                            pending = pending,
                            recordId = recordId,
                            templateId = field.templateId,
                            fieldId = field.id,
                            subjectKind = recKind,
                            displayName = "${field.name} gallery photo"
                        )
                        if (asset != null) {
                            onValueChange((existing + asset.uri).joinToString("||"))
                        }
                    } else if (pending != null) {
                        runCatching { pending.file.delete() }
                    }
                }
                val camPermLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
                    if (granted) {
                        val dest = MediaManager.createCameraDestination(context)
                        pendingCam = dest
                        takePhotoLauncher.launch(dest.contentUri)
                    } else {
                        Toast.makeText(context, "Camera permission denied. You can still add photos from your library.", Toast.LENGTH_SHORT).show()
                    }
                }
                val galleryPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia()) { uris ->
                    if (uris.isNotEmpty()) {
                        val recKind = db.get(recordId)?.kind.orEmpty()
                        val added = uris.mapNotNull { uri ->
                            MediaManager.importPickedPhoto(
                                context = context,
                                db = db,
                                sourceUri = uri,
                                recordId = recordId,
                                templateId = field.templateId,
                                fieldId = field.id,
                                subjectKind = recKind
                            )?.uri
                        }
                        if (added.isNotEmpty()) {
                            onValueChange((existing + added).joinToString("||"))
                        }
                    }
                }
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (existing.isNotEmpty()) {
                        Row(
                            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            existing.forEachIndexed { idx, imgRef ->
                                val bmp = remember(imgRef) { MediaManager.decodeImageBitmap(context, imgRef, maxDim = 640) }
                                Box(modifier = Modifier.size(110.dp)) {
                                    if (bmp != null) {
                                        Image(
                                            bitmap = bmp,
                                            contentDescription = "${field.name} image ${idx + 1}",
                                            modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(10.dp)),
                                            contentScale = ContentScale.Crop
                                        )
                                    }
                                    IconButton(
                                        onClick = {
                                            val updated = existing.toMutableList().apply { removeAt(idx) }
                                            onValueChange(updated.joinToString("||"))
                                        },
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .size(32.dp)
                                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.8f), CircleShape)
                                    ) {
                                        Icon(Icons.Default.Close, contentDescription = "Remove image", modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(onClick = {
                            val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
                            if (granted) {
                                val dest = MediaManager.createCameraDestination(context)
                                pendingCam = dest
                                takePhotoLauncher.launch(dest.contentUri)
                            } else {
                                camPermLauncher.launch(Manifest.permission.CAMERA)
                            }
                        }) {
                            Icon(Icons.Default.PhotoCamera, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Take Photo")
                        }
                        FilledTonalButton(onClick = {
                            galleryPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        }) {
                            Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(if (existing.isEmpty()) "Add Photos" else "Add More (${existing.size})")
                        }
                    }
                }
            }

            "Attachment" -> {
                val attachmentPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
                    uri?.let {
                        runCatching {
                            context.contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        onValueChange(it.toString())
                    }
                }
                if (value.isNotBlank()) {
                    val uri = remember(value) { Uri.parse(value) }
                    val fileName = remember(value) { uri.lastPathSegment ?: "Attached file" }
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.AttachFile, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(8.dp))
                            Text(fileName, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                            IconButton(
                                onClick = {
                                    runCatching {
                                        val mime = context.contentResolver.getType(uri) ?: "*/*"
                                        context.startActivity(
                                            Intent(Intent.ACTION_VIEW)
                                                .setDataAndType(uri, mime)
                                                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                        )
                                    }
                                }
                            ) {
                                Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = "Open file")
                            }
                            IconButton(onClick = { onValueChange("") }) {
                                Icon(Icons.Default.Close, contentDescription = "Remove file")
                            }
                        }
                    }
                }
                FilledTonalButton(onClick = { attachmentPicker.launch(arrayOf("*/*")) }) {
                    Icon(Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(if (value.isBlank()) "Attach File" else "Replace File")
                }
            }

            "Relationship" -> {
                var showPicker by remember { mutableStateOf(false) }
                val linkedIds = remember(value) { SheetEngine.parseRelationshipIds(value) }
                val linkedRecords = remember(linkedIds) { linkedIds.mapNotNull { db.get(it) } }
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (linkedRecords.isNotEmpty()) {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            linkedRecords.forEach { target ->
                                InputChip(
                                    selected = true,
                                    onClick = { onOpenRecord(target) },
                                    label = { Text("${target.title} · ${target.kind}") },
                                    trailingIcon = {
                                        Icon(
                                            Icons.Default.Close,
                                            contentDescription = "Unlink ${target.title}",
                                            modifier = Modifier
                                                .size(18.dp)
                                                .clickable {
                                                    val updated = linkedIds.filterNot { it == target.id }
                                                    onValueChange(updated.joinToString("|"))
                                                }
                                        )
                                    }
                                )
                            }
                        }
                    }
                    FilledTonalButton(onClick = { showPicker = true }) {
                        Icon(Icons.Default.Link, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Link Entry")
                    }
                }
                if (showPicker) {
                    var search by remember { mutableStateOf("") }
                    val candidates = remember(search, linkedIds) {
                        db.all(search).filter { it.id != recordId && it.id !in linkedIds }
                    }
                    AlertDialog(
                        onDismissRequest = { showPicker = false },
                        title = { Text("Link ${field.name}") },
                        text = {
                            Column(
                                modifier = Modifier.fillMaxWidth().heightIn(max = 420.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = search,
                                    onValueChange = { search = it },
                                    modifier = Modifier.fillMaxWidth(),
                                    label = { Text("Search entries") },
                                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                                    singleLine = true
                                )
                                if (candidates.isEmpty()) {
                                    Text(
                                        "No matching entries found.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                } else {
                                    LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        items(candidates) { cand ->
                                            Surface(
                                                shape = RoundedCornerShape(10.dp),
                                                tonalElevation = 1.dp,
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable {
                                                        onValueChange((linkedIds + cand.id).distinct().joinToString("|"))
                                                        showPicker = false
                                                    }
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(12.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Text(cand.title, fontWeight = FontWeight.Medium)
                                                        Text(
                                                            cand.kind,
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = MaterialTheme.colorScheme.primary
                                                        )
                                                    }
                                                    Icon(Icons.Default.Add, contentDescription = "Link")
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        },
                        confirmButton = {
                            TextButton(onClick = { showPicker = false }) { Text("Close") }
                        }
                    )
                }
            }

            "Table" -> {
                val table = remember(value, field.hint) { SheetEngine.parseTable(value, field.hint) }
                var editingColIndex by remember { mutableStateOf<Int?>(null) }
                var colDraft by remember { mutableStateOf("") }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    table.rows.forEachIndexed { rIdx, row ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        "Row ${rIdx + 1}",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.weight(1f)
                                    )
                                    IconButton(
                                        onClick = {
                                            val updatedRows = table.rows.toMutableList().apply { removeAt(rIdx) }
                                            onValueChange(SheetEngine.serializeTable(table.copy(rows = updatedRows)))
                                        },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(Icons.Default.DeleteOutline, contentDescription = "Delete row", modifier = Modifier.size(18.dp))
                                    }
                                }
                                table.columns.forEachIndexed { cIdx, colName ->
                                    OutlinedTextField(
                                        value = row.getOrNull(cIdx).orEmpty(),
                                        onValueChange = { nextCell ->
                                            val updatedRows = table.rows.mapIndexed { ri, existingRow ->
                                                if (ri != rIdx) existingRow
                                                else List(table.columns.size) { ci ->
                                                    if (ci == cIdx) nextCell else existingRow.getOrNull(ci).orEmpty()
                                                }
                                            }
                                            onValueChange(SheetEngine.serializeTable(table.copy(rows = updatedRows)))
                                        },
                                        label = { Text(colName) },
                                        modifier = Modifier.fillMaxWidth(),
                                        singleLine = true
                                    )
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilledTonalButton(onClick = {
                            val newRow = List(table.columns.size) { "" }
                            onValueChange(SheetEngine.serializeTable(table.copy(rows = table.rows + listOf(newRow))))
                        }) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Add Row")
                        }
                        OutlinedButton(onClick = {
                            val nextCols = table.columns + "Column ${table.columns.size + 1}"
                            val nextRows = table.rows.map { it + "" }
                            onValueChange(SheetEngine.serializeTable(SheetTableData(nextCols, nextRows)))
                        }) {
                            Text("+ Column")
                        }
                        table.columns.forEachIndexed { idx, col ->
                            AssistChip(
                                onClick = {
                                    editingColIndex = idx
                                    colDraft = col
                                },
                                label = { Text(col) }
                            )
                        }
                    }
                }

                if (editingColIndex != null) {
                    val cIdx = editingColIndex!!
                    AlertDialog(
                        onDismissRequest = { editingColIndex = null },
                        title = { Text("Edit Column") },
                        text = {
                            OutlinedTextField(
                                value = colDraft,
                                onValueChange = { colDraft = it },
                                label = { Text("Column header") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        },
                        confirmButton = {
                            TextButton(onClick = {
                                val nextCols = table.columns.mapIndexed { i, s ->
                                    if (i == cIdx && colDraft.isNotBlank()) colDraft.trim() else s
                                }
                                onValueChange(SheetEngine.serializeTable(table.copy(columns = nextCols)))
                                editingColIndex = null
                            }) { Text("Save") }
                        },
                        dismissButton = {
                            Row {
                                if (table.columns.size > 1) {
                                    TextButton(onClick = {
                                        val nextCols = table.columns.filterIndexed { i, _ -> i != cIdx }
                                        val nextRows = table.rows.map { r -> r.filterIndexed { i, _ -> i != cIdx } }
                                        onValueChange(SheetEngine.serializeTable(SheetTableData(nextCols, nextRows)))
                                        editingColIndex = null
                                    }) { Text("Remove", color = MaterialTheme.colorScheme.error) }
                                }
                                TextButton(onClick = { editingColIndex = null }) { Text("Cancel") }
                            }
                        }
                    )
                }
            }

            "Repeatable group" -> {
                val entries = remember(value) { SheetEngine.parseRepeatable(value) }
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    entries.forEachIndexed { idx, entry ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        "Entry ${idx + 1}",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.weight(1f)
                                    )
                                    if (idx > 0) {
                                        IconButton(
                                            onClick = {
                                                val list = entries.toMutableList()
                                                val moved = list.removeAt(idx)
                                                list.add(idx - 1, moved)
                                                onValueChange(SheetEngine.serializeRepeatable(list))
                                            },
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Icon(Icons.Default.ArrowUpward, contentDescription = "Move up", modifier = Modifier.size(16.dp))
                                        }
                                    }
                                    IconButton(
                                        onClick = {
                                            val list = entries.toMutableList().apply { removeAt(idx) }
                                            onValueChange(SheetEngine.serializeRepeatable(list))
                                        },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(Icons.Default.DeleteOutline, contentDescription = "Delete item", modifier = Modifier.size(18.dp))
                                    }
                                }
                                OutlinedTextField(
                                    value = entry.label,
                                    onValueChange = { nextLabel ->
                                        val list = entries.mapIndexed { i, e -> if (i == idx) e.copy(label = nextLabel) else e }
                                        onValueChange(SheetEngine.serializeRepeatable(list))
                                    },
                                    label = { Text("Name / Title") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true
                                )
                                OutlinedTextField(
                                    value = entry.detail,
                                    onValueChange = { nextDetail ->
                                        val list = entries.mapIndexed { i, e -> if (i == idx) e.copy(detail = nextDetail) else e }
                                        onValueChange(SheetEngine.serializeRepeatable(list))
                                    },
                                    label = { Text("Details") },
                                    modifier = Modifier.fillMaxWidth(),
                                    minLines = 2
                                )
                            }
                        }
                    }
                    FilledTonalButton(onClick = {
                        val next = entries + RepeatableEntry(label = "New ${field.name.lowercase()}", detail = "")
                        onValueChange(SheetEngine.serializeRepeatable(next))
                    }) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Add Item")
                    }
                }
            }

            "Calculated" -> {
                val computed = remember(field, allFields, values) {
                    SheetEngine.evaluateFormula(field, allFields, values)
                }
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = computed.ifBlank { "—" },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        if (field.formula.isNotBlank()) {
                            Text(
                                text = "Formula: ${field.formula}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f)
                            )
                        }
                    }
                }
            }

            else -> {
                OutlinedTextField(
                    value = value,
                    onValueChange = onValueChange,
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )
            }
        }
    }
}

private fun decodeBase64Image(dataUri: String) =
    if (!dataUri.startsWith("data:")) null
    else runCatching {
        val raw = dataUri.substringAfter("base64,", "")
        if (raw.isBlank()) null
        else {
            val bytes = Base64.decode(raw, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
        }
    }.getOrNull()
