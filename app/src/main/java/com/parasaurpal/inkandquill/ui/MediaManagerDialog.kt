package com.parasaurpal.inkandquill.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import com.parasaurpal.inkandquill.data.*

@Composable
fun MediaManagerDialog(
    db: InkDatabase,
    initialRecord: InkRecord? = null,
    initialTemplateId: String = "",
    onAssignToSheetField: ((fieldId: String, uri: String) -> Unit)? = null,
    onInsertIntoProse: ((MediaAsset) -> Unit)? = null,
    onSetAsCover: ((MediaAsset) -> Unit)? = null,
    onOpenRecord: ((InkRecord) -> Unit)? = null,
    onMediaChanged: () -> Unit = {},
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.9f)
                .testTag("media_manager_dialog"),
            shape = RoundedCornerShape(24.dp),
            tonalElevation = 6.dp
        ) {
            Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.PhotoLibrary,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Media Manager",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Camera & gallery photos linked to characters, locations, and templates",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close media manager")
                    }
                }
                Spacer(Modifier.height(8.dp))
                MediaManagerContent(
                    db = db,
                    initialRecord = initialRecord,
                    initialTemplateId = initialTemplateId,
                    onAssignToSheetField = onAssignToSheetField,
                    onInsertIntoProse = onInsertIntoProse,
                    onSetAsCover = onSetAsCover,
                    onOpenRecord = onOpenRecord,
                    onMediaChanged = onMediaChanged,
                    modifier = Modifier.weight(1f).fillMaxWidth()
                )
            }
        }
    }
}

@Composable
fun MediaManagerContent(
    db: InkDatabase,
    initialRecord: InkRecord? = null,
    initialTemplateId: String = "",
    onAssignToSheetField: ((fieldId: String, uri: String) -> Unit)? = null,
    onInsertIntoProse: ((MediaAsset) -> Unit)? = null,
    onSetAsCover: ((MediaAsset) -> Unit)? = null,
    onOpenRecord: ((InkRecord) -> Unit)? = null,
    onMediaChanged: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var mediaRefresh by remember { mutableIntStateOf(0) }
    var searchQuery by remember { mutableStateOf("") }
    var activeFilter by remember(initialRecord?.id, initialTemplateId) {
        mutableStateOf(
            when {
                initialRecord != null -> "This Entry"
                initialTemplateId.isNotBlank() -> "This Template"
                else -> "All"
            }
        )
    }

    // Target association for new captures
    val allEntries = remember(mediaRefresh) { db.all() }
    val allTemplates = remember(mediaRefresh) { db.templates() }
    var targetRecordId by remember(initialRecord?.id, initialTemplateId) {
        mutableStateOf(initialRecord?.id ?: initialTemplateId)
    }
    var targetTemplateId by remember(initialRecord?.id, initialTemplateId) {
        mutableStateOf(
            initialTemplateId.ifBlank {
                if (initialRecord != null) db.sheetTemplateId(initialRecord.id) else ""
            }
        )
    }
    var showTargetMenu by remember { mutableStateOf(false) }
    var showTemplateMenu by remember { mutableStateOf(false) }
    var cameraPermissionDenied by remember { mutableStateOf(false) }
    var pendingCapture by remember { mutableStateOf<PendingCameraCapture?>(null) }
    var inspectingAsset by remember { mutableStateOf<MediaAsset?>(null) }

    val takePictureLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { saved ->
        val pending = pendingCapture
        pendingCapture = null
        if (saved && pending != null) {
            val targetRec = if (targetRecordId.isNotBlank()) db.get(targetRecordId) else null
            val resolvedTemplate = targetTemplateId.ifBlank {
                if (targetRec != null && targetRec.kind == "Template") targetRec.id
                else if (targetRec != null) db.sheetTemplateId(targetRec.id)
                else ""
            }
            val subjectKind = targetRec?.kind ?: if (resolvedTemplate.isNotBlank()) "Template" else ""
            val asset = MediaManager.finalizeCameraCapture(
                db = db,
                pending = pending,
                recordId = targetRecordId.ifBlank { resolvedTemplate },
                templateId = resolvedTemplate,
                subjectKind = subjectKind,
                displayName = "${targetRec?.title?.ifBlank { targetRec.kind } ?: "Camera"} photo"
            )
            if (asset != null) {
                mediaRefresh++
                onMediaChanged()
                Toast.makeText(context, "Photo saved alongside template.", Toast.LENGTH_SHORT).show()
            }
        } else if (pending != null) {
            runCatching { pending.file.delete() }
        }
    }

    fun launchCameraCapture() {
        val dest = MediaManager.createCameraDestination(context)
        pendingCapture = dest
        takePictureLauncher.launch(dest.contentUri)
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            cameraPermissionDenied = false
            launchCameraCapture()
        } else {
            cameraPermissionDenied = true
        }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            val targetRec = if (targetRecordId.isNotBlank()) db.get(targetRecordId) else null
            val resolvedTemplate = targetTemplateId.ifBlank {
                if (targetRec != null && targetRec.kind == "Template") targetRec.id
                else if (targetRec != null) db.sheetTemplateId(targetRec.id)
                else ""
            }
            val subjectKind = targetRec?.kind ?: if (resolvedTemplate.isNotBlank()) "Template" else ""
            val asset = MediaManager.importPickedPhoto(
                context = context,
                db = db,
                sourceUri = uri,
                recordId = targetRecordId.ifBlank { resolvedTemplate },
                templateId = resolvedTemplate,
                subjectKind = subjectKind
            )
            if (asset != null) {
                mediaRefresh++
                onMediaChanged()
                Toast.makeText(context, "Photo added to media library.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val rawAssets = remember(mediaRefresh, initialRecord?.id, initialTemplateId) {
        db.allMediaAssets()
    }

    val filteredAssets = remember(rawAssets, activeFilter, searchQuery, initialRecord?.id, initialTemplateId) {
        rawAssets.filter { asset ->
            val filterMatch = when (activeFilter) {
                "This Entry" -> initialRecord != null && asset.recordId == initialRecord.id
                "This Template" -> initialTemplateId.isNotBlank() &&
                    (asset.templateId == initialTemplateId || asset.recordId == initialTemplateId)
                "Characters" -> asset.subjectKind in MediaManager.CHARACTER_KINDS ||
                    db.get(asset.recordId)?.kind in MediaManager.CHARACTER_KINDS
                "Locations" -> asset.subjectKind in MediaManager.LOCATION_KINDS ||
                    db.get(asset.recordId)?.kind in MediaManager.LOCATION_KINDS
                "Camera" -> asset.source == "camera"
                else -> true
            }
            val q = searchQuery.trim()
            val queryMatch = if (q.isBlank()) true else {
                val recTitle = db.get(asset.recordId)?.title.orEmpty()
                val tplTitle = db.get(asset.templateId)?.title.orEmpty()
                asset.caption.contains(q, true) ||
                    asset.displayName.contains(q, true) ||
                    asset.subjectKind.contains(q, true) ||
                    recTitle.contains(q, true) ||
                    tplTitle.contains(q, true)
            }
            filterMatch && queryMatch
        }
    }

    val targetRecord = remember(targetRecordId, mediaRefresh) {
        if (targetRecordId.isBlank()) null else db.get(targetRecordId)
    }
    val targetTemplate = remember(targetTemplateId, mediaRefresh) {
        if (targetTemplateId.isBlank()) null else db.get(targetTemplateId)
    }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Capture & Associate Bar
        ElevatedCard(
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            val granted = ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.CAMERA
                            ) == PackageManager.PERMISSION_GRANTED
                            if (granted) {
                                launchCameraCapture()
                            } else {
                                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                            }
                        },
                        modifier = Modifier.weight(1f).defaultMinSize(minHeight = 48.dp).testTag("media_take_photo_button")
                    ) {
                        Icon(Icons.Default.PhotoCamera, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Take Photo")
                    }
                    FilledTonalButton(
                        onClick = {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        modifier = Modifier.weight(1f).defaultMinSize(minHeight = 48.dp).testTag("media_pick_photo_button")
                    ) {
                        Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Add Photo")
                    }
                }

                // Target association selectors
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        OutlinedButton(
                            onClick = { showTargetMenu = true },
                            modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 44.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.PersonPin, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = targetRecord?.let { "${it.title} (${it.kind})" } ?: "Link Character / Location",
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        DropdownMenu(expanded = showTargetMenu, onDismissRequest = { showTargetMenu = false }) {
                            val prioritized = allEntries.sortedBy {
                                when {
                                    it.kind in MediaManager.CHARACTER_KINDS -> 0
                                    it.kind in MediaManager.LOCATION_KINDS -> 1
                                    else -> 2
                                }
                            }
                            prioritized.forEach { candidate ->
                                DropdownMenuItem(
                                    text = { Text("${candidate.title.ifBlank { "Untitled" }} · ${candidate.kind}") },
                                    onClick = {
                                        targetRecordId = candidate.id
                                        val attached = db.sheetTemplateId(candidate.id)
                                        if (attached.isNotBlank()) targetTemplateId = attached
                                        showTargetMenu = false
                                    }
                                )
                            }
                        }
                    }

                    Box(modifier = Modifier.weight(1f)) {
                        OutlinedButton(
                            onClick = { showTemplateMenu = true },
                            modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 44.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.Dashboard, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = targetTemplate?.title ?: "No Template",
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        DropdownMenu(expanded = showTemplateMenu, onDismissRequest = { showTemplateMenu = false }) {
                            DropdownMenuItem(
                                text = { Text("No template") },
                                onClick = { targetTemplateId = ""; showTemplateMenu = false }
                            )
                            allTemplates.forEach { tpl ->
                                DropdownMenuItem(
                                    text = { Text("${tpl.title} (${tpl.scope})") },
                                    onClick = {
                                        targetTemplateId = tpl.id
                                        if (targetRecordId.isBlank()) targetRecordId = tpl.id
                                        showTemplateMenu = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        if (cameraPermissionDenied) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.errorContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Camera permission is required to take new photos directly. You can grant it or choose an existing photo instead.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = {
                            runCatching {
                                context.startActivity(
                                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                        data = Uri.fromParts("package", context.packageName, null)
                                    }
                                )
                            }
                        }) { Text("App Settings") }
                        TextButton(onClick = {
                            cameraPermissionDenied = false
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        }) { Text("Choose Photo") }
                    }
                }
            }
        }

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search captions, characters, locations, templates...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        val filterOptions = buildList {
            if (initialRecord != null) add("This Entry")
            if (initialTemplateId.isNotBlank()) add("This Template")
            addAll(listOf("All", "Characters", "Locations", "Camera"))
        }
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            filterOptions.forEach { f ->
                FilterChip(
                    selected = activeFilter == f,
                    onClick = { activeFilter = f },
                    label = { Text(f) }
                )
            }
        }

        if (filteredAssets.isEmpty()) {
            Box(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(24.dp)
                ) {
                    Icon(
                        Icons.Default.PhotoCamera,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(40.dp)
                    )
                    Text("No photos in this view yet", fontWeight = FontWeight.SemiBold)
                    Text(
                        text = "Capture a reference photo with your camera or add one from your device to store it alongside this character, location, or template.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                items(filteredAssets, key = { it.id }) { asset ->
                    val linkedRec = remember(asset.recordId, mediaRefresh) {
                        if (asset.recordId.isNotBlank()) db.get(asset.recordId) else null
                    }
                    val linkedTpl = remember(asset.templateId, mediaRefresh) {
                        if (asset.templateId.isNotBlank()) db.get(asset.templateId) else null
                    }
                    val bitmap = remember(asset.uri) {
                        MediaManager.decodeImageBitmap(context, asset.uri, maxDim = 640)
                    }
                    ElevatedCard(
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { inspectingAsset = asset }
                            .testTag("media_asset_card_${asset.id}")
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(84.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant),
                                contentAlignment = Alignment.Center
                            ) {
                                if (bitmap != null) {
                                    Image(
                                        bitmap = bitmap,
                                        contentDescription = asset.caption.ifBlank { asset.displayName },
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Icon(Icons.Default.BrokenImage, contentDescription = null)
                                }
                            }

                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = asset.caption.ifBlank { asset.displayName.ifBlank { "Captured Photo" } },
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = buildString {
                                        append(linkedRec?.let { "${it.title} (${it.kind})" } ?: asset.subjectKind.ifBlank { "Unlinked" })
                                    },
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.primary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    if (linkedTpl != null) {
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = MaterialTheme.colorScheme.secondaryContainer
                                        ) {
                                            Text(
                                                text = "Tpl: ${linkedTpl.title}",
                                                fontSize = 10.sp,
                                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant
                                    ) {
                                        Text(
                                            text = if (asset.source == "camera") "Camera" else "Photo",
                                            fontSize = 10.sp,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                            IconButton(
                                onClick = { inspectingAsset = asset },
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(Icons.Default.MoreVert, contentDescription = "Manage photo")
                            }
                        }
                    }
                }
            }
        }
    }

    inspectingAsset?.let { asset ->
        MediaAssetDetailDialog(
            db = db,
            asset = asset,
            initialTemplateId = targetTemplateId,
            onAssignToSheetField = onAssignToSheetField,
            onInsertIntoProse = onInsertIntoProse,
            onSetAsCover = onSetAsCover,
            onOpenRecord = onOpenRecord,
            onUpdated = {
                inspectingAsset = null
                mediaRefresh++
                onMediaChanged()
            },
            onDeleted = {
                db.removeAttachment(asset.id)
                inspectingAsset = null
                mediaRefresh++
                onMediaChanged()
            },
            onDismiss = { inspectingAsset = null }
        )
    }
}

@Composable
private fun MediaAssetDetailDialog(
    db: InkDatabase,
    asset: MediaAsset,
    initialTemplateId: String,
    onAssignToSheetField: ((fieldId: String, uri: String) -> Unit)?,
    onInsertIntoProse: ((MediaAsset) -> Unit)?,
    onSetAsCover: ((MediaAsset) -> Unit)?,
    onOpenRecord: ((InkRecord) -> Unit)?,
    onUpdated: () -> Unit,
    onDeleted: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var caption by remember(asset.id) { mutableStateOf(asset.caption) }
    var recordId by remember(asset.id) { mutableStateOf(asset.recordId) }
    var templateId by remember(asset.id) { mutableStateOf(asset.templateId) }
    var recordMenu by remember { mutableStateOf(false) }
    var templateMenu by remember { mutableStateOf(false) }
    val bitmap = remember(asset.uri) { MediaManager.decodeImageBitmap(context, asset.uri, maxDim = 1200) }
    val allEntries = remember { db.all() }
    val allTemplates = remember { db.templates() }

    val imageFields = remember(templateId, initialTemplateId) {
        val tpl = templateId.ifBlank { initialTemplateId }
        if (tpl.isBlank()) emptyList()
        else db.templateFields(tpl).filter { it.fieldType == "Image" || it.fieldType == "Gallery" }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(asset.displayName.ifBlank { "Photo Details" }, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        text = {
            Column(
                modifier = Modifier.heightIn(max = 480.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap,
                        contentDescription = caption.ifBlank { asset.displayName },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 220.dp)
                            .clip(RoundedCornerShape(14.dp)),
                        contentScale = ContentScale.Fit
                    )
                }

                OutlinedTextField(
                    value = caption,
                    onValueChange = { caption = it },
                    label = { Text("Caption / Notes") },
                    modifier = Modifier.fillMaxWidth()
                )

                Box {
                    val rec = allEntries.firstOrNull { it.id == recordId } ?: db.get(recordId)
                    OutlinedButton(onClick = { recordMenu = true }, modifier = Modifier.fillMaxWidth()) {
                        Text("Entry: ${rec?.let { "${it.title} (${it.kind})" } ?: "Unlinked"}")
                    }
                    DropdownMenu(expanded = recordMenu, onDismissRequest = { recordMenu = false }) {
                        allEntries.forEach { candidate ->
                            DropdownMenuItem(
                                text = { Text("${candidate.title.ifBlank { "Untitled" }} · ${candidate.kind}") },
                                onClick = {
                                    recordId = candidate.id
                                    val attached = db.sheetTemplateId(candidate.id)
                                    if (attached.isNotBlank()) templateId = attached
                                    recordMenu = false
                                }
                            )
                        }
                    }
                }

                Box {
                    val tpl = allTemplates.firstOrNull { it.id == templateId }
                    OutlinedButton(onClick = { templateMenu = true }, modifier = Modifier.fillMaxWidth()) {
                        Text("Template: ${tpl?.title ?: "None"}")
                    }
                    DropdownMenu(expanded = templateMenu, onDismissRequest = { templateMenu = false }) {
                        DropdownMenuItem(
                            text = { Text("No template") },
                            onClick = { templateId = ""; templateMenu = false }
                        )
                        allTemplates.forEach { tpl ->
                            DropdownMenuItem(
                                text = { Text("${tpl.title} (${tpl.scope})") },
                                onClick = { templateId = tpl.id; templateMenu = false }
                            )
                        }
                    }
                }

                if (onAssignToSheetField != null && imageFields.isNotEmpty()) {
                    Text("Use in Sheet Field", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                    imageFields.forEach { f ->
                        FilledTonalButton(
                            onClick = {
                                onAssignToSheetField(f.id, asset.uri)
                                Toast.makeText(context, "Assigned to ${f.name}.", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Set in \"${f.name}\" (${f.fieldType})")
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (onInsertIntoProse != null) {
                        TextButton(onClick = { onInsertIntoProse(asset); onDismiss() }) {
                            Text("Insert in Prose")
                        }
                    }
                    if (onSetAsCover != null) {
                        TextButton(onClick = { onSetAsCover(asset); onDismiss() }) {
                            Text("Set Cover")
                        }
                    }
                    if (onOpenRecord != null && recordId.isNotBlank()) {
                        val rec = db.get(recordId)
                        if (rec != null && rec.kind != "Template") {
                            TextButton(onClick = { onOpenRecord(rec); onDismiss() }) {
                                Text("Open Entry")
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val resolvedKind = db.get(recordId)?.kind ?: asset.subjectKind
                db.updateMediaAsset(
                    id = asset.id,
                    recordId = recordId,
                    templateId = templateId,
                    fieldId = asset.fieldId,
                    subjectKind = resolvedKind,
                    caption = caption
                )
                onUpdated()
            }) { Text("Save") }
        },
        dismissButton = {
            Row {
                TextButton(onClick = onDeleted) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
                TextButton(onClick = onDismiss) { Text("Close") }
            }
        }
    )
}
