package com.parasaurpal.inkandquill.ui

import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddLocation
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.ZoomOutMap
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.parasaurpal.inkandquill.data.InkDatabase
import com.parasaurpal.inkandquill.data.InkRecord
import com.parasaurpal.inkandquill.data.MapPin
import kotlin.math.roundToInt

private val pinColors = listOf("#D64545", "#4A90D9", "#4CAF50", "#E8A33D", "#9B83B5", "#E8E8E8")

private fun parsePinColor(hex: String): Color =
    runCatching { Color(android.graphics.Color.parseColor(hex)) }.getOrDefault(Color(0xFFD64545))

@Composable
fun MapScreen(
    db: InkDatabase,
    boardId: String,
    worldId: String,
    onOpenRecord: (InkRecord) -> Unit,
    onOpenBoard: (String) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    var refresh by remember { mutableIntStateOf(0) }
    val pins = remember(boardId, refresh) { db.mapPins(boardId) }
    val boardName = remember(boardId, refresh) { db.boardName(boardId) }
    val backgroundUri = remember(boardId, refresh) { db.boardBackground(boardId) }
    val bitmap = remember(backgroundUri) {
        if (backgroundUri.isBlank()) null
        else runCatching {
            context.contentResolver.openInputStream(Uri.parse(backgroundUri))?.use {
                BitmapFactory.decodeStream(it)?.asImageBitmap()
            }
        }.getOrNull()
    }
    var scale by remember { mutableFloatStateOf(1f) }
    var pan by remember { mutableStateOf(Offset.Zero) }
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }
    var placing by remember { mutableStateOf(false) }
    var movingPin by remember { mutableStateOf<MapPin?>(null) }
    var selectedPin by remember { mutableStateOf<MapPin?>(null) }
    var editingPin by remember { mutableStateOf<MapPin?>(null) }
    var newPinId by remember { mutableStateOf("") }

    val backgroundPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        uri?.let {
            runCatching {
                context.contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                db.setBoardBackground(boardId, it.toString())
                refresh++
            }
        }
    }

    // Fitted image rectangle inside the canvas, in pixels. Pins are stored as
    // normalized 0..1 coordinates relative to this rectangle, so they survive
    // background image changes.
    val imageRect = remember(bitmap, canvasSize) {
        val bmp = bitmap
        if (bmp == null || canvasSize == IntSize.Zero) null
        else {
            val fit = minOf(canvasSize.width / bmp.width.toFloat(), canvasSize.height / bmp.height.toFloat())
            val w = bmp.width * fit
            val h = bmp.height * fit
            Rect(Offset((canvasSize.width - w) / 2f, (canvasSize.height - h) / 2f), Size(w, h))
        }
    }

    val transformState = rememberTransformableState { zoomChange, panChange, _ ->
        val newScale = (scale * zoomChange).coerceIn(1f, 4f)
        scale = newScale
        pan = Offset(
            (pan.x + panChange.x).coerceIn(canvasSize.width * (1f - newScale), 0f),
            (pan.y + panChange.y).coerceIn(canvasSize.height * (1f - newScale), 0f)
        )
    }

    val latestPins by rememberUpdatedState(pins)
    val latestRect by rememberUpdatedState(imageRect)
    val latestScale by rememberUpdatedState(scale)
    val latestPan by rememberUpdatedState(pan)
    val latestPlacing by rememberUpdatedState(placing)
    val latestMoving by rememberUpdatedState(movingPin)

    fun contentPoint(tap: Offset): Offset = (tap - latestPan) / latestScale

    fun pinContentPosition(pin: MapPin, rect: Rect): Offset =
        Offset(rect.left + pin.x * rect.width, rect.top + pin.y * rect.height)

    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back to boards") }
            Text(boardName, Modifier.weight(1f), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            IconButton(onClick = { scale = 1f; pan = Offset.Zero }) { Icon(Icons.Default.ZoomOutMap, "Reset zoom") }
            TextButton(onClick = { backgroundPicker.launch(arrayOf("image/*")) }) { Text("Background") }
            Button(onClick = { placing = !placing; movingPin = null }, enabled = bitmap != null) {
                Icon(Icons.Default.AddLocation, null, Modifier.padding(end = 4.dp).size(18.dp))
                Text(if (placing) "Placing…" else "Add pin")
            }
        }
        when {
            bitmap == null -> Text("Choose a map image to start placing pins.", Modifier.padding(horizontal = 4.dp), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = .65f))
            placing -> Text("Tap the map to place a pin.", Modifier.padding(horizontal = 4.dp), fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
            movingPin != null -> Text("Tap the map to move “${movingPin?.label}”.", Modifier.padding(horizontal = 4.dp), fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
            else -> Text("${pins.size} pin${if (pins.size == 1) "" else "s"} · pinch to zoom, drag to pan", Modifier.padding(horizontal = 4.dp), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = .65f))
        }
        Box(
            Modifier.fillMaxWidth().height(520.dp)
                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
                .onSizeChanged { canvasSize = it }
                .transformable(transformState)
                .pointerInput(Unit) {
                    detectTapGestures { tap ->
                        val rect = latestRect ?: return@detectTapGestures
                        val content = contentPoint(tap)
                        val nx = (content.x - rect.left) / rect.width
                        val ny = (content.y - rect.top) / rect.height
                        val moving = latestMoving
                        if (moving != null) {
                            if (nx in 0f..1f && ny in 0f..1f) {
                                db.moveMapPin(moving.id, nx, ny)
                                movingPin = null
                                refresh++
                            }
                            return@detectTapGestures
                        }
                        // Hit-test pins first (48dp on screen, so pins stay tappable when zoomed out).
                        val hitRadius = with(density) { 48.dp.toPx() }
                        val hit = latestPins.minByOrNull { pin ->
                            (pinContentPosition(pin, rect) - content).getDistance() * latestScale
                        }?.takeIf { pin ->
                            (pinContentPosition(pin, rect) - content).getDistance() * latestScale < hitRadius
                        }
                        if (hit != null) {
                            selectedPin = hit
                            return@detectTapGestures
                        }
                        if (latestPlacing && nx in 0f..1f && ny in 0f..1f) {
                            val id = db.addMapPin(boardId, nx, ny, "New pin")
                            newPinId = id
                            editingPin = db.mapPins(boardId).firstOrNull { it.id == id }
                            placing = false
                            refresh++
                        }
                    }
                }
        ) {
            if (bitmap == null) {
                Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Image, null, tint = MaterialTheme.colorScheme.onSurface.copy(alpha = .4f), modifier = Modifier.size(48.dp))
                    Text("No map image yet", fontWeight = FontWeight.SemiBold)
                    Text("Pick an image of your world to pin places on.", fontSize = 12.sp, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurface.copy(alpha = .65f))
                    Button(onClick = { backgroundPicker.launch(arrayOf("image/*")) }) { Text("Choose map image") }
                }
            } else if (imageRect != null) {
                Box(
                    Modifier.fillMaxSize().graphicsLayer(
                        scaleX = scale, scaleY = scale,
                        translationX = pan.x, translationY = pan.y,
                        transformOrigin = TransformOrigin(0f, 0f)
                    )
                ) {
                    val rect = imageRect
                    Image(
                        bitmap, "Map",
                        Modifier.offset { IntOffset(rect.left.roundToInt(), rect.top.roundToInt()) }
                            .size(with(density) { rect.width.toDp() }, with(density) { rect.height.toDp() }),
                        contentScale = ContentScale.FillBounds
                    )
                    pins.forEach { pin ->
                        val px = rect.left + pin.x * rect.width
                        val py = rect.top + pin.y * rect.height
                        Column(
                            Modifier.offset { IntOffset((px - 24.dp.toPx()).roundToInt(), (py - 40.dp.toPx()).roundToInt()) },
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Default.Place, pin.label, tint = parsePinColor(pin.color), modifier = Modifier.size(32.dp))
                            if (pin.label.isNotBlank()) {
                                Text(
                                    pin.label, fontSize = 10.sp, maxLines = 1, color = Color.White,
                                    modifier = Modifier.background(Color.Black.copy(alpha = .6f), RoundedCornerShape(4.dp)).padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
        Text("Pins", Modifier.padding(top = 10.dp), fontWeight = FontWeight.SemiBold)
        if (pins.isEmpty()) Text("No pins yet. Tap Add pin, then tap the map.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = .65f))
        LazyColumn(Modifier.fillMaxWidth().weight(1f)) {
            items(pins) { pin ->
                Card(onClick = { selectedPin = pin }, Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
                    Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(14.dp).background(parsePinColor(pin.color), CircleShape))
                        Text(pin.label.ifBlank { "Untitled pin" }, Modifier.weight(1f).padding(start = 8.dp), fontWeight = FontWeight.Medium)
                        val target = when {
                            pin.recordId.isNotBlank() -> db.get(pin.recordId)?.title ?: "Linked record"
                            pin.linkedBoardId.isNotBlank() -> "Map: " + db.boardName(pin.linkedBoardId)
                            else -> ""
                        }
                        if (target.isNotBlank()) Text(target, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = .65f))
                    }
                }
            }
        }
    }

    val pin = selectedPin
    if (pin != null) {
        val linkedRecord = remember(pin.recordId, refresh) { if (pin.recordId.isNotBlank()) db.get(pin.recordId) else null }
        val linkedBoardName = remember(pin.linkedBoardId, refresh) { if (pin.linkedBoardId.isNotBlank()) db.boardName(pin.linkedBoardId) else "" }
        AlertDialog(
            onDismissRequest = { selectedPin = null },
            title = { Text(pin.label.ifBlank { "Pin" }) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (linkedRecord != null) Text("Linked to ${linkedRecord.kind}: ${linkedRecord.title}", fontSize = 13.sp)
                    if (linkedBoardName.isNotBlank()) Text("Nested map: $linkedBoardName", fontSize = 13.sp)
                    if (linkedRecord == null && linkedBoardName.isBlank()) Text("Not linked to anything yet.", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = .65f))
                }
            },
            confirmButton = {
                Row {
                    if (linkedRecord != null) TextButton(onClick = { selectedPin = null; onOpenRecord(linkedRecord) }) { Text("Open") }
                    if (pin.linkedBoardId.isNotBlank()) TextButton(onClick = { selectedPin = null; onOpenBoard(pin.linkedBoardId) }) { Text("Open map") }
                    TextButton(onClick = { selectedPin = null; editingPin = pin }) { Text("Edit") }
                }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = { selectedPin = null; movingPin = pin }) { Text("Move") }
                    TextButton(onClick = { db.deleteMapPin(pin.id); selectedPin = null; refresh++ }) { Text("Delete") }
                    TextButton(onClick = { selectedPin = null }) { Text("Close") }
                }
            }
        )
    }

    val editPin = editingPin
    if (editPin != null) {
        var label by remember(editPin.id) { mutableStateOf(editPin.label) }
        var color by remember(editPin.id) { mutableStateOf(editPin.color) }
        var recordId by remember(editPin.id) { mutableStateOf(editPin.recordId) }
        var linkedBoardId by remember(editPin.id) { mutableStateOf(editPin.linkedBoardId) }
        var recordMenu by remember { mutableStateOf(false) }
        var boardMenu by remember { mutableStateOf(false) }
        val cancelEdit = {
            if (editPin.id == newPinId) db.deleteMapPin(editPin.id)
            editingPin = null
            newPinId = ""
            refresh++
        }
        AlertDialog(
            onDismissRequest = cancelEdit,
            title = { Text(if (editPin.id == newPinId) "New pin" else "Edit pin") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(label, { label = it }, label = { Text("Label") }, singleLine = true)
                    Text("Color", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        pinColors.forEach { hex ->
                            Box(
                                Modifier.size(28.dp)
                                    .background(parsePinColor(hex), CircleShape)
                                    .then(if (color == hex) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, CircleShape) else Modifier)
                                    .clickable { color = hex }
                            )
                        }
                    }
                    OutlinedTextField(color, { color = it }, label = { Text("Custom color · hex") }, singleLine = true)
                    Box {
                        OutlinedButton(onClick = { recordMenu = true }) { Text("Link record: " + (db.get(recordId)?.title ?: "None")) }
                        DropdownMenu(expanded = recordMenu, onDismissRequest = { recordMenu = false }) {
                            DropdownMenuItem(text = { Text("No linked record") }, onClick = { recordId = ""; recordMenu = false })
                            db.all().filter { it.kind != "Template" }.forEach { r ->
                                DropdownMenuItem(text = { Text(r.title + " · " + r.kind) }, onClick = { recordId = r.id; recordMenu = false })
                            }
                        }
                    }
                    Box {
                        OutlinedButton(onClick = { boardMenu = true }) { Text("Nested map: " + (if (linkedBoardId.isBlank()) "None" else db.boardName(linkedBoardId))) }
                        DropdownMenu(expanded = boardMenu, onDismissRequest = { boardMenu = false }) {
                            DropdownMenuItem(text = { Text("No nested map") }, onClick = { linkedBoardId = ""; boardMenu = false })
                            db.boards(worldId).filter { it.third == "Map" && it.first != boardId }.forEach { b ->
                                DropdownMenuItem(text = { Text(b.second) }, onClick = { linkedBoardId = b.first; boardMenu = false })
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    db.updateMapPin(editPin.id, label, color, recordId, linkedBoardId)
                    editingPin = null
                    newPinId = ""
                    refresh++
                }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = cancelEdit) { Text("Cancel") } }
        )
    }
}
