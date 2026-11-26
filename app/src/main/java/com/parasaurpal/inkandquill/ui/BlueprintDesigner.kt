package com.parasaurpal.inkandquill.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.parasaurpal.inkandquill.data.*
import java.text.DateFormat
import java.util.Date

private val ALL_FIELD_TYPES = listOf(
    "Short text", "Long text", "Rich text", "Number", "Date",
    "Dropdown", "Multi-select", "Checkbox", "Image", "Gallery",
    "Attachment", "Color", "Rating", "Table", "Relationship",
    "Repeatable group", "Calculated"
)

@Composable
fun BlueprintDesignerScreen(
    db: InkDatabase,
    template: InkRecord,
    onTemplateUpdated: (InkRecord) -> Unit,
    onExportTemplate: (InkRecord) -> Unit,
    onDuplicateTemplate: (InkRecord) -> Unit,
    onUseTemplate: (InkRecord) -> Unit,
    onClose: () -> Unit
) {
    BackHandler { onClose() }

    var refresh by remember { mutableIntStateOf(0) }
    var currentTemplate by remember(template.id) { mutableStateOf(db.get(template.id) ?: template) }
    var title by remember(template.id) { mutableStateOf(currentTemplate.title) }
    var description by remember(template.id) { mutableStateOf(currentTemplate.body) }
    var category by remember(template.id) { mutableStateOf(currentTemplate.tags.ifBlank { "Other" }) }
    var defaultKind by remember(template.id) { mutableStateOf(currentTemplate.scope.ifBlank { "Document" }) }

    var designerTab by remember { mutableStateOf("Fields") }
    var showAddSectionDialog by remember { mutableStateOf(false) }
    var newSectionName by remember { mutableStateOf("") }
    var renamingSection by remember { mutableStateOf<String?>(null) }
    var renameSectionDraft by remember { mutableStateOf("") }

    var showFieldDialog by remember { mutableStateOf(false) }
    var editingField by remember { mutableStateOf<TemplateField?>(null) }

    val sections = remember(currentTemplate.id, refresh) { db.templateSections(currentTemplate.id) }
    val allFields = remember(currentTemplate.id, refresh) { db.templateFields(currentTemplate.id, includeRemoved = true) }
    val activeFieldIds = remember(currentTemplate.id, refresh) {
        db.templateFields(currentTemplate.id, includeRemoved = false).map { it.id }.toSet()
    }
    val orderedAllFields = remember(allFields, sections) {
        SheetEngine.orderedFields(allFields, sections)
    }
    val versions = remember(currentTemplate.id, refresh) { db.templateVersions(currentTemplate.id) }

    fun persistMeta(
        nextTitle: String = title,
        nextBody: String = description,
        nextCategory: String = category,
        nextKind: String = defaultKind
    ) {
        val updated = currentTemplate.copy(
            title = nextTitle,
            body = nextBody,
            tags = nextCategory,
            scope = nextKind
        )
        currentTemplate = updated
        db.save(updated)
        onTemplateUpdated(updated)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp, vertical = 8.dp)
            .testTag("blueprint_designer_screen")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onClose, modifier = Modifier.size(48.dp)) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to templates")
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title.ifBlank { "Untitled Sheet" },
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "$category · Creates: $defaultKind",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            FilledTonalButton(
                onClick = { onUseTemplate(currentTemplate) },
                modifier = Modifier.testTag("blueprint_use_sheet_button")
            ) {
                Text("Use Sheet")
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("Fields", "Sections", "Media", "Settings & Versions").forEach { tab ->
                FilterChip(
                    selected = designerTab == tab,
                    onClick = { designerTab = tab },
                    label = { Text(tab) }
                )
            }
        }

        when (designerTab) {
            "Fields" -> {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = {
                            editingField = null
                            showFieldDialog = true
                        },
                        modifier = Modifier.testTag("blueprint_add_field_button")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Add Field")
                    }
                    OutlinedButton(onClick = {
                        newSectionName = ""
                        showAddSectionDialog = true
                    }) {
                        Text("+ Section")
                    }
                    Spacer(Modifier.weight(1f))
                    Text(
                        "${activeFieldIds.size} active",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    items(orderedAllFields, key = { it.id }) { field ->
                        val isActive = field.id in activeFieldIds
                        ElevatedCard(
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = field.name + if (!isActive) " (Removed)" else "",
                                            fontWeight = FontWeight.SemiBold,
                                            color = if (isActive) MaterialTheme.colorScheme.onSurface
                                            else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = "${field.section.ifBlank { "General" }} · ${field.fieldType}" +
                                                if (field.required) " · Required" else "",
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    if (isActive) {
                                        IconButton(
                                            onClick = {
                                                db.moveTemplateField(currentTemplate.id, field.id, -1)
                                                refresh++
                                            },
                                            modifier = Modifier.size(40.dp)
                                        ) {
                                            Icon(Icons.Default.ArrowUpward, contentDescription = "Move field up", modifier = Modifier.size(18.dp))
                                        }
                                        IconButton(
                                            onClick = {
                                                db.moveTemplateField(currentTemplate.id, field.id, 1)
                                                refresh++
                                            },
                                            modifier = Modifier.size(40.dp)
                                        ) {
                                            Icon(Icons.Default.ArrowDownward, contentDescription = "Move field down", modifier = Modifier.size(18.dp))
                                        }
                                    }
                                }

                                if (field.hint.isNotBlank()) {
                                    Text(
                                        "Hint: ${field.hint}",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                if (field.options.isNotBlank()) {
                                    Text(
                                        "Options: ${field.options}",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                if (field.conditionField.isNotBlank()) {
                                    Text(
                                        "Visible when ${field.conditionField} → ${field.conditionValue}",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                }
                                if (field.formula.isNotBlank()) {
                                    Text(
                                        "Formula: ${field.formula}",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    if (isActive) {
                                        TextButton(onClick = {
                                            db.templateVersion(currentTemplate.id)
                                            editingField = field
                                            showFieldDialog = true
                                        }) { Text("Edit") }
                                        TextButton(onClick = {
                                            db.templateVersion(currentTemplate.id)
                                            db.removeTemplateField(field.id)
                                            refresh++
                                        }) {
                                            Text("Remove", color = MaterialTheme.colorScheme.error)
                                        }
                                    } else {
                                        FilledTonalButton(onClick = {
                                            db.restoreTemplateField(field.id)
                                            refresh++
                                        }) { Text("Restore Field") }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            "Sections" -> {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Section order controls layout in the Sheet editor.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )
                    Button(onClick = {
                        newSectionName = ""
                        showAddSectionDialog = true
                    }) {
                        Text("+ Section")
                    }
                }
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(sections.withIndex().toList(), key = { it.value }) { (idx, sec) ->
                        val fieldCount = allFields.count { it.id in activeFieldIds && it.section == sec }
                        ElevatedCard(shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(sec, fontWeight = FontWeight.SemiBold)
                                    Text(
                                        "$fieldCount active field(s)",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                IconButton(
                                    enabled = idx > 0,
                                    onClick = {
                                        db.moveTemplateSection(currentTemplate.id, sec, -1)
                                        refresh++
                                    }
                                ) { Icon(Icons.Default.ArrowUpward, contentDescription = "Move section up") }
                                IconButton(
                                    enabled = idx < sections.lastIndex,
                                    onClick = {
                                        db.moveTemplateSection(currentTemplate.id, sec, 1)
                                        refresh++
                                    }
                                ) { Icon(Icons.Default.ArrowDownward, contentDescription = "Move section down") }
                                IconButton(onClick = {
                                    renamingSection = sec
                                    renameSectionDraft = sec
                                }) { Icon(Icons.Default.Edit, contentDescription = "Rename section") }
                                if (sections.size > 1) {
                                    IconButton(onClick = {
                                        db.deleteTemplateSection(currentTemplate.id, sec)
                                        refresh++
                                    }) { Icon(Icons.Default.DeleteOutline, contentDescription = "Delete section") }
                                }
                            }
                        }
                    }
                }
            }

            "Media" -> {
                MediaManagerContent(
                    db = db,
                    initialRecord = null,
                    initialTemplateId = currentTemplate.id,
                    onMediaChanged = { refresh++ },
                    modifier = Modifier.weight(1f).fillMaxWidth()
                )
            }

            else -> {
                var categoryExpanded by remember { mutableStateOf(false) }
                var kindExpanded by remember { mutableStateOf(false) }
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = title,
                        onValueChange = {
                            title = it
                            persistMeta(nextTitle = it)
                        },
                        label = { Text("Template Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = description,
                        onValueChange = {
                            description = it
                            persistMeta(nextBody = it)
                        },
                        label = { Text("Template Description") },
                        minLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(modifier = Modifier.weight(1f)) {
                            OutlinedButton(
                                onClick = { categoryExpanded = true },
                                modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 48.dp)
                            ) {
                                Text("Category: $category", maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            DropdownMenu(
                                expanded = categoryExpanded,
                                onDismissRequest = { categoryExpanded = false }
                            ) {
                                FantasyTemplateCatalog.categories.filter { it != "All" }.forEach { cat ->
                                    DropdownMenuItem(
                                        text = { Text(cat) },
                                        onClick = {
                                            category = cat
                                            persistMeta(nextCategory = cat)
                                            categoryExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        Box(modifier = Modifier.weight(1f)) {
                            OutlinedButton(
                                onClick = { kindExpanded = true },
                                modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 48.dp)
                            ) {
                                Text("Kind: $defaultKind", maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            DropdownMenu(
                                expanded = kindExpanded,
                                onDismissRequest = { kindExpanded = false }
                            ) {
                                FantasyTemplateCatalog.starterRecordKinds.filter { it != "All" }.forEach { k ->
                                    DropdownMenuItem(
                                        text = { Text(k) },
                                        onClick = {
                                            defaultKind = k
                                            persistMeta(nextKind = k)
                                            kindExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilledTonalButton(onClick = { onDuplicateTemplate(currentTemplate) }) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Duplicate as Custom")
                        }
                        OutlinedButton(onClick = { onExportTemplate(currentTemplate) }) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Export JSON")
                        }
                    }

                    HorizontalDivider()

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Blueprint Snapshots", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(
                                "Restore an earlier field layout if needed",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        FilledTonalButton(onClick = {
                            db.templateVersion(currentTemplate.id)
                            refresh++
                        }) {
                            Text("Save Snapshot")
                        }
                    }

                    if (versions.isEmpty()) {
                        Text(
                            "No blueprint snapshots saved yet. Snapshots are created automatically when editing or removing fields.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        versions.take(10).forEach { (verId, ts) ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                tonalElevation = 1.dp,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        DateFormat.getDateTimeInstance().format(Date(ts)),
                                        modifier = Modifier.weight(1f),
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                    TextButton(onClick = {
                                        db.restoreTemplateVersion(currentTemplate.id, verId)
                                        refresh++
                                    }) {
                                        Text("Restore")
                                    }
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(24.dp))
                }
            }
        }
    }

    if (showAddSectionDialog) {
        AlertDialog(
            onDismissRequest = { showAddSectionDialog = false },
            title = { Text("Add Section") },
            text = {
                OutlinedTextField(
                    value = newSectionName,
                    onValueChange = { newSectionName = it },
                    label = { Text("Section title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (newSectionName.isNotBlank()) {
                        db.addTemplateSection(currentTemplate.id, newSectionName.trim())
                        showAddSectionDialog = false
                        refresh++
                    }
                }) { Text("Add") }
            },
            dismissButton = {
                TextButton(onClick = { showAddSectionDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (renamingSection != null) {
        val oldSec = renamingSection!!
        AlertDialog(
            onDismissRequest = { renamingSection = null },
            title = { Text("Rename Section") },
            text = {
                OutlinedTextField(
                    value = renameSectionDraft,
                    onValueChange = { renameSectionDraft = it },
                    label = { Text("Section title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (renameSectionDraft.isNotBlank()) {
                        db.renameTemplateSection(currentTemplate.id, oldSec, renameSectionDraft.trim())
                        renamingSection = null
                        refresh++
                    }
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { renamingSection = null }) { Text("Cancel") }
            }
        )
    }

    if (showFieldDialog) {
        FieldEditorDialog(
            editingField = editingField,
            sections = sections,
            existingFields = allFields.filter { it.id in activeFieldIds && it.id != editingField?.id },
            onDismiss = {
                showFieldDialog = false
                editingField = null
            },
            onSave = { name, type, req, sec, opts, hint, condField, condVal, formula, rep ->
                if (editingField == null) {
                    db.templateVersion(currentTemplate.id)
                    db.addTemplateField(
                        templateId = currentTemplate.id,
                        name = name,
                        type = type,
                        required = req,
                        section = sec,
                        options = opts,
                        hint = hint,
                        conditionField = condField,
                        conditionValue = condVal,
                        formula = formula,
                        repeatable = rep
                    )
                } else {
                    db.updateTemplateField(
                        id = editingField!!.id,
                        name = name,
                        type = type,
                        required = req,
                        section = sec,
                        options = opts,
                        hint = hint,
                        conditionField = condField,
                        conditionValue = condVal,
                        formula = formula,
                        repeatable = rep
                    )
                }
                showFieldDialog = false
                editingField = null
                refresh++
            }
        )
    }
}

@Composable
private fun FieldEditorDialog(
    editingField: TemplateField?,
    sections: List<String>,
    existingFields: List<TemplateField>,
    onDismiss: () -> Unit,
    onSave: (
        name: String,
        type: String,
        required: Boolean,
        section: String,
        options: String,
        hint: String,
        conditionField: String,
        conditionValue: String,
        formula: String,
        repeatable: Boolean
    ) -> Unit
) {
    var name by remember(editingField) { mutableStateOf(editingField?.name.orEmpty()) }
    var type by remember(editingField) { mutableStateOf(editingField?.fieldType ?: "Long text") }
    var section by remember(editingField) { mutableStateOf(editingField?.section ?: sections.firstOrNull() ?: "General") }
    var hint by remember(editingField) { mutableStateOf(editingField?.hint.orEmpty()) }
    var options by remember(editingField) { mutableStateOf(editingField?.options.orEmpty()) }
    var required by remember(editingField) { mutableStateOf(editingField?.required ?: false) }
    var condField by remember(editingField) { mutableStateOf(editingField?.conditionField.orEmpty()) }
    var condVal by remember(editingField) { mutableStateOf(editingField?.conditionValue.orEmpty()) }
    var formula by remember(editingField) { mutableStateOf(editingField?.formula.orEmpty()) }
    var repeatable by remember(editingField) { mutableStateOf(editingField?.repeatable ?: false) }

    var typeMenu by remember { mutableStateOf(false) }
    var sectionMenu by remember { mutableStateOf(false) }
    var condFieldMenu by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (editingField == null) "Add Field" else "Edit Field") },
        text = {
            Column(
                modifier = Modifier.heightIn(max = 520.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Field name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = { typeMenu = true },
                        modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 48.dp)
                    ) {
                        Text("Type: $type", modifier = Modifier.weight(1f))
                        Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                    }
                    DropdownMenu(expanded = typeMenu, onDismissRequest = { typeMenu = false }) {
                        ALL_FIELD_TYPES.forEach { t ->
                            DropdownMenuItem(
                                text = { Text(t) },
                                onClick = {
                                    type = t
                                    if (t == "Repeatable group") repeatable = true
                                    typeMenu = false
                                }
                            )
                        }
                    }
                }

                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = section,
                        onValueChange = { section = it },
                        label = { Text("Section") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        trailingIcon = {
                            IconButton(onClick = { sectionMenu = true }) {
                                Icon(Icons.Default.ArrowDropDown, contentDescription = "Pick section")
                            }
                        }
                    )
                    DropdownMenu(expanded = sectionMenu, onDismissRequest = { sectionMenu = false }) {
                        sections.forEach { sec ->
                            DropdownMenuItem(
                                text = { Text(sec) },
                                onClick = {
                                    section = sec
                                    sectionMenu = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = hint,
                    onValueChange = { hint = it },
                    label = {
                        Text(if (type == "Table") "Default columns (e.g. Col 1 | Col 2 | Col 3)" else "Help text / hint")
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                if (type == "Dropdown" || type == "Multi-select") {
                    OutlinedTextField(
                        value = options,
                        onValueChange = { options = it },
                        label = { Text("Options (comma separated)") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2
                    )
                }

                if (type == "Calculated") {
                    OutlinedTextField(
                        value = formula,
                        onValueChange = { formula = it },
                        label = { Text("Formula (e.g. sum(Field A, Field B))") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("sum()", "avg()", "count()", "concat()").forEach { fn ->
                            AssistChip(onClick = { formula = fn }, label = { Text(fn) })
                        }
                    }
                    if (existingFields.isNotEmpty()) {
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            existingFields.take(10).forEach { f ->
                                AssistChip(
                                    onClick = {
                                        formula = if (formula.endsWith("()")) {
                                            formula.dropLast(1) + f.name + ")"
                                        } else if (formula.endsWith(")")) {
                                            formula.dropLast(1) + ", " + f.name + ")"
                                        } else if (formula.isBlank()) f.name
                                        else "$formula + ${f.name}"
                                    },
                                    label = { Text(f.name) }
                                )
                            }
                        }
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                Text("Conditional Visibility (optional)", style = MaterialTheme.typography.labelLarge)
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = { condFieldMenu = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            if (condField.isBlank()) "Always visible (tap to set trigger)"
                            else "When: $condField",
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                    }
                    DropdownMenu(expanded = condFieldMenu, onDismissRequest = { condFieldMenu = false }) {
                        DropdownMenuItem(
                            text = { Text("Always visible (clear condition)") },
                            onClick = {
                                condField = ""
                                condVal = ""
                                condFieldMenu = false
                            }
                        )
                        existingFields.forEach { ef ->
                            DropdownMenuItem(
                                text = { Text(ef.name) },
                                onClick = {
                                    condField = ef.name
                                    condFieldMenu = false
                                }
                            )
                        }
                    }
                }

                if (condField.isNotBlank()) {
                    OutlinedTextField(
                        value = condVal,
                        onValueChange = { condVal = it },
                        label = { Text("Value rule (exact, !=val, contains:val, or not_empty)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("not_empty", "Yes", "true", "contains:").forEach { preset ->
                            AssistChip(onClick = { condVal = preset }, label = { Text(preset) })
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = required, onCheckedChange = { required = it })
                    Text("Required field")
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = name.isNotBlank(),
                onClick = {
                    onSave(
                        name.trim(),
                        type,
                        required,
                        section.trim().ifBlank { "General" },
                        options.trim(),
                        hint.trim(),
                        condField.trim(),
                        condVal.trim(),
                        formula.trim(),
                        repeatable || type == "Repeatable group"
                    )
                }
            ) { Text(if (editingField == null) "Add Field" else "Save Changes") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun TemplatePickerDialog(
    db: InkDatabase,
    title: String = "Choose a Fantasy Template Sheet",
    onDismiss: () -> Unit,
    onSelect: (InkRecord) -> Unit
) {
    var search by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("All") }
    val templates = remember(search, category) {
        db.templates().filter { t ->
            t.kind == "Template" &&
                (search.isBlank() || t.title.contains(search, true) || t.body.contains(search, true) || t.scope.contains(search, true)) &&
                (category == "All" || t.tags == category)
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }
                OutlinedTextField(
                    value = search,
                    onValueChange = { search = it },
                    modifier = Modifier.fillMaxWidth().testTag("template_picker_search"),
                    label = { Text("Search fantasy sheets (character, flora, dragon, spell...)") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true
                )
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FantasyTemplateCatalog.categories.forEach { cat ->
                        FilterChip(
                            selected = category == cat,
                            onClick = { category = cat },
                            label = { Text(cat) }
                        )
                    }
                }
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(templates, key = { it.id }) { t ->
                        val count = remember(t.id) { db.templateFields(t.id).size }
                        ElevatedCard(
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelect(t) }
                                .testTag("pick_template_${t.id}")
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        t.title,
                                        fontWeight = FontWeight.SemiBold,
                                        style = MaterialTheme.typography.titleMedium,
                                        modifier = Modifier.weight(1f)
                                    )
                                    AssistChip(
                                        onClick = { onSelect(t) },
                                        label = { Text(t.scope.ifBlank { "Document" }) }
                                    )
                                }
                                Text(
                                    t.body,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    "${t.tags.ifBlank { "Other" }} · $count fields",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
