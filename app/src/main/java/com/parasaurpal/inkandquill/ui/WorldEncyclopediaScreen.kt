package com.parasaurpal.inkandquill.ui

import android.graphics.BitmapFactory
import android.net.Uri

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.parasaurpal.inkandquill.data.InkDatabase
import com.parasaurpal.inkandquill.data.InkRecord

@Composable
fun WorldEncyclopediaScreen(
    db: InkDatabase,
    world: InkRecord,
    refresh: Int,
    onEditArticle: (InkRecord) -> Unit,
    onReadArticle: (InkRecord) -> Unit,
    onBack: () -> Unit
) {
    BackHandler { onBack() }
    var tab by remember { mutableStateOf("Articles") }
    var category by remember { mutableStateOf("All") }
    var query by remember { mutableStateOf("") }
    var createTemplate by remember { mutableStateOf<InkRecord?>(null) }
    var selectedParentId by remember(world.id) { mutableStateOf(world.id) }
    var parentMenuExpanded by remember { mutableStateOf(false) }
    fun requestTemplate(template: InkRecord) { selectedParentId = world.id; createTemplate = template }

    val templates = remember(refresh) {
        db.all().filter { it.kind == "Template" }
            .sortedWith(compareBy<InkRecord> { it.tags.lowercase() }.thenBy { it.title.lowercase() })
    }
    val articles = remember(world.id, refresh) {
        db.descendants(world.id)
            .filter { db.sheetTemplateId(it.id).isNotBlank() }
            .sortedByDescending { it.updatedAt }
    }
    val parentChoices = remember(world.id, articles) { listOf(world) + articles }
    val categories = remember(templates) {
        listOf("All") + templates.map { it.tags.ifBlank { "Other" } }.distinct().sorted()
    }
    val visibleTemplates = templates.filter { template ->
        (category == "All" || template.tags.ifBlank { "Other" } == category) &&
            (query.isBlank() || template.title.contains(query, true) ||
                template.body.contains(query, true) || template.tags.contains(query, true))
    }
    val visibleArticles = articles.filter { article ->
        val template = templates.firstOrNull { it.id == db.sheetTemplateId(article.id) }
        (category == "All" || template?.tags?.ifBlank { "Other" } == category) &&
            (query.isBlank() || article.title.contains(query, true) ||
                article.body.contains(query, true) || article.tags.contains(query, true) ||
                db.sheetValues(article.id).values.any { it.contains(query, true) })
    }

    Column(
        Modifier.fillMaxSize().padding(horizontal = 4.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to world modules")
            }
            Column(Modifier.weight(1f)) {
                Text("World encyclopedia", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(world.title, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            }
            Text("${articles.size} articles", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            placeholder = { Text("Search article types and entries") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) }
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = tab == "Articles", onClick = { tab = "Articles"; category = "All" }, label = { Text("Articles · ${articles.size}") })
            FilterChip(selected = tab == "Templates", onClick = { tab = "Templates"; category = "All" }, label = { Text("Templates · ${templates.size}") })
        }
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            categories.forEach { value ->
                FilterChip(
                    selected = category == value,
                    onClick = { category = value },
                    label = { Text(value, maxLines = 1) }
                )
            }
        }
        if (tab == "Templates") {
            Text(
                "Choose a specialist structure. Each template has its own sections and fields.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(9.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                items(visibleTemplates, key = { it.id }) { template ->
                    val fields = remember(template.id, refresh) { db.templateFields(template.id) }
                    val sections = remember(template.id, refresh) { db.templateSections(template.id) }
                    ElevatedCard(
                        modifier = Modifier.fillMaxWidth().clickable { requestTemplate(template) },
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(template.title, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleMedium)
                                    Text(template.tags.ifBlank { "Other" } + " · " + template.scope, color = MaterialTheme.colorScheme.primary, fontSize = 11.sp)
                                }
                                FilledTonalIconButton(onClick = { requestTemplate(template) }) {
                                    Icon(Icons.Default.Add, contentDescription = "Create ${template.title} article")
                                }
                            }
                            Text(template.body, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 3, overflow = TextOverflow.Ellipsis)
                            Text("${sections.size} sections · ${fields.size} specialized fields", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                if (visibleTemplates.isEmpty()) item { EmptyEncyclopediaMessage("No templates match this search.") }
            }
        } else {
            Text(
                "Your saved world entries. Open one to read it, or edit it to continue building the article.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(9.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                items(visibleArticles, key = { it.id }) { article ->
                    val template = templates.firstOrNull { it.id == db.sheetTemplateId(article.id) }
                    ElevatedCard(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(article.title.ifBlank { "Untitled ${article.kind}" }, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleMedium)
                                    Text(template?.title ?: article.kind, color = MaterialTheme.colorScheme.primary, fontSize = 11.sp)
                                }
                                IconButton(onClick = { onEditArticle(article) }) {
                                    Icon(Icons.Default.Edit, contentDescription = "Edit ${article.title}")
                                }
                            }
                            articles.firstOrNull { it.id == article.parentId }?.let { parent ->
                                Text("Filed under: ${parent.title}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            val values = remember(article.id, refresh) { db.sheetValues(article.id) }
                            val fields = remember(article.id, template?.id, refresh) { template?.id?.let { db.templateFields(it) }.orEmpty() }
                            val previewUri = remember(article.id, article.coverUri, refresh) {
                                db.mediaForRecord(article.id).firstOrNull { it.mimeType.startsWith("image/") }?.uri
                                    ?.takeIf { it.isNotBlank() } ?: article.coverUri
                            }
                            if (previewUri.isNotBlank()) EncyclopediaCoverImage(previewUri)
                            val completed = fields.mapNotNull { field -> values[field.id]?.takeIf { it.isNotBlank() }?.let { "${field.name}: ${it.replace("\n", " ").take(90)}" } }
                            if (article.body.isNotBlank()) {
                                Text(article.body, style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            } else if (completed.isNotEmpty()) {
                                Text(completed.take(2).joinToString(" · "), style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            } else {
                                Text("No details yet · open to start building this article", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            if (article.tags.isNotBlank()) Text(article.tags, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("${completed.size} of ${fields.size} fields completed", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                TextButton(onClick = { onReadArticle(article) }) {
                                    Icon(Icons.Default.MenuBook, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(5.dp))
                                    Text("Read article")
                                }
                                TextButton(onClick = { onEditArticle(article) }) { Text("Edit") }
                            }
                        }
                    }
                }
                if (visibleArticles.isEmpty()) item {
                    EmptyEncyclopediaMessage("No articles here yet. Switch to Templates to create your first entry.")
                }
            }
        }
    }

    createTemplate?.let { template ->
        AlertDialog(
            onDismissRequest = { createTemplate = null; parentMenuExpanded = false },
            title = { Text("Create ${template.title} article") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Choose where this article belongs. It can be moved later by editing its parent.")
                    Box {
                        OutlinedButton(onClick = { parentMenuExpanded = true }, modifier = Modifier.fillMaxWidth()) {
                            Text("Filed under: ${parentChoices.firstOrNull { it.id == selectedParentId }?.title ?: world.title}")
                        }
                        DropdownMenu(
                            expanded = parentMenuExpanded,
                            onDismissRequest = { parentMenuExpanded = false }
                        ) {
                            parentChoices.forEach { parent ->
                                DropdownMenuItem(
                                    text = { Text(if (parent.id == world.id) "${parent.title} · World" else parent.title) },
                                    onClick = { selectedParentId = parent.id; parentMenuExpanded = false }
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val record = db.createRecordFromTemplate(
                        template.id,
                        parentId = selectedParentId,
                        scope = "World"
                    )
                    createTemplate = null
                    onEditArticle(record)
                }) { Text("Create article") }
            },
            dismissButton = {
                TextButton(onClick = { createTemplate = null; parentMenuExpanded = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun EmptyEncyclopediaMessage(message: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(14.dp)
    ) {
        Text(message, Modifier.padding(18.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun EncyclopediaCoverImage(uri: String) {
    val context = LocalContext.current
    val bitmap = remember(uri) {
        runCatching {
            val parsed = Uri.parse(uri)
            val options = BitmapFactory.Options().apply { inSampleSize = 4 }
            val stream = when (parsed.scheme) {
                "content", "file" -> context.contentResolver.openInputStream(parsed)
                else -> java.io.FileInputStream(uri)
            }
            stream?.use { BitmapFactory.decodeStream(it, null, options) }
        }.getOrNull()
    }
    if (bitmap != null) {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = "Article image",
            modifier = Modifier.fillMaxWidth().heightIn(max = 170.dp),
            contentScale = ContentScale.Crop
        )
    }
}
