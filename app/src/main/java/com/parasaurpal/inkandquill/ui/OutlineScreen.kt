package com.parasaurpal.inkandquill.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.parasaurpal.inkandquill.data.DocumentExport
import com.parasaurpal.inkandquill.data.InkDatabase
import com.parasaurpal.inkandquill.data.InkRecord

private fun plainWords(html: String): Int =
    DocumentExport.plain(html).trim().split(Regex("\\s+")).count { it.isNotBlank() }

private fun synopsisOf(record: InkRecord): String {
    val plain = DocumentExport.plain(record.body).trim().replace(Regex("\\s+"), " ")
    return if (plain.length > 140) plain.take(137) + "..." else plain
}

// What kind of child each manuscript kind accepts in the outline. Series holds
// books, books hold chapters, chapters and standalone stories hold scenes.
private fun childKindFor(parentKind: String): String? = when (parentKind) {
    "Series" -> "Book"
    "Novel", "Book" -> "Chapter"
    "Chapter" -> "Scene"
    "Story" -> "Scene"
    else -> null
}

@Composable
fun OutlineScreen(db: InkDatabase, root: InkRecord, onOpen: (InkRecord) -> Unit, onBack: () -> Unit) {
    var refresh by remember { mutableIntStateOf(0) }
    val children = remember(root.id, refresh) { db.childrenOrdered(root.id) }
    val descendants = remember(root.id, refresh) { db.descendants(root.id) }
    val totalWords = plainWords(root.body) + descendants.sumOf { plainWords(it.body) }
    val sceneCount = descendants.count { it.kind == "Scene" }
    val childLabel = if (root.kind == "Series") "books" else "chapters"
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") }
            Column(Modifier.weight(1f)) {
                Text(root.title.ifBlank { "Untitled" }, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text(
                    "${root.kind} · ${children.size} $childLabel · $sceneCount scenes · $totalWords words",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = .7f)
                )
            }
        }
        children.forEachIndexed { index, child ->
            OutlineNode(db, child, 0, children.size, index, refresh, onOpen, { refresh++ })
        }
        val addKind = childKindFor(root.kind)
        if (addKind != null) {
            TextButton(onClick = {
                val record = InkRecord(title = "Untitled $addKind", kind = addKind, parentId = root.id, scope = root.scope)
                db.save(record)
                onOpen(record)
            }) { Text("+ Add $addKind") }
        }
    }
}

@Composable
private fun OutlineNode(
    db: InkDatabase,
    record: InkRecord,
    depth: Int,
    siblingCount: Int,
    index: Int,
    refresh: Int,
    onOpen: (InkRecord) -> Unit,
    onChanged: () -> Unit
) {
    val children = remember(record.id, refresh) { db.childrenOrdered(record.id) }
    val subtreeWords = remember(record.id, refresh) {
        plainWords(record.body) + db.descendants(record.id).sumOf { plainWords(it.body) }
    }
    val synopsis = remember(record.id, refresh) { synopsisOf(record) }
    val addKind = if (depth < 2) childKindFor(record.kind) else null
    Card(Modifier.fillMaxWidth().padding(start = (depth * 12).dp).clickable { onOpen(record) }) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(record.title.ifBlank { "Untitled" }, fontWeight = FontWeight.SemiBold)
                    Text(
                        "${record.kind} · ${children.size} ${if (record.kind == "Chapter") "scenes" else "items"} · $subtreeWords words",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = .7f)
                    )
                }
                IconButton(
                    onClick = { db.moveRecord(record.id, -1); onChanged() },
                    enabled = index > 0
                ) { Icon(Icons.Default.ArrowUpward, contentDescription = "Move up") }
                IconButton(
                    onClick = { db.moveRecord(record.id, 1); onChanged() },
                    enabled = index < siblingCount - 1
                ) { Icon(Icons.Default.ArrowDownward, contentDescription = "Move down") }
            }
            if (synopsis.isNotBlank()) {
                Text(synopsis, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = .8f))
            }
        }
    }
    if (depth < 3) {
        children.forEachIndexed { childIndex, child ->
            OutlineNode(db, child, depth + 1, children.size, childIndex, refresh, onOpen, onChanged)
        }
    }
    if (addKind != null) {
        TextButton(
            onClick = {
                val child = InkRecord(title = "Untitled $addKind", kind = addKind, parentId = record.id, scope = record.scope)
                db.save(child)
                onOpen(child)
            },
            modifier = Modifier.padding(start = ((depth + 1) * 12).dp)
        ) { Text("+ Add $addKind") }
    }
}
