package com.parasaurpal.inkandquill.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
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

private val QUICK_FIELD_TYPES = listOf(
    "Short text", "Long text", "Rich text", "Number", "Date",
    "Dropdown", "Multi-select", "Checkbox", "Image", "Rating",
    "Table", "Relationship", "Repeatable group"
)

@Composable
fun CreateTemplateDialog(
    db: InkDatabase,
    initialCategory: String = "Other",
    parentWorldId: String = "",
    onDismiss: () -> Unit,
    onCreatedForDesign: (InkRecord) -> Unit,
    onCreatedAndUse: (InkRecord) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var category by remember(initialCategory) {
        mutableStateOf(if (initialCategory == "All" || initialCategory.isBlank()) "Other" else initialCategory)
    }
    var customCategory by remember { mutableStateOf("") }
    var defaultKind by remember { mutableStateOf("Document") }
    var customKind by remember { mutableStateOf("") }
    var starterMode by remember { mutableStateOf("Quick Starter") } // Blank, Quick Starter, Clone Existing
    var baseTemplateId by remember { mutableStateOf<String?>(null) }
    var baseMenuOpen by remember { mutableStateOf(false) }

    var sections by remember { mutableStateOf(listOf("Overview", "Details", "Lore & Notes")) }
    var newSectionDraft by remember { mutableStateOf("") }

    var starterFields by remember {
        mutableStateOf(
            listOf(
                FieldSpec(name = "Summary", type = "Long text", section = "Overview", required = true),
                FieldSpec(name = "Key Traits", type = "Long text", section = "Details"),
                FieldSpec(name = "Background & Notes", type = "Rich text", section = "Lore & Notes")
            )
        )
    }
    var newFieldName by remember { mutableStateOf("") }
    var newFieldType by remember { mutableStateOf("Short text") }
    var newFieldSection by remember { mutableStateOf("Overview") }
    var newFieldOptions by remember { mutableStateOf("") }
    var newFieldRequired by remember { mutableStateOf(false) }

    var categoryMenuOpen by remember { mutableStateOf(false) }
    var kindMenuOpen by remember { mutableStateOf(false) }
    var fieldTypeMenuOpen by remember { mutableStateOf(false) }
    var fieldSecMenuOpen by remember { mutableStateOf(false) }

    val existingTemplates = remember { db.templates().filter { it.kind == "Template" } }

    fun buildTemplate(): InkRecord {
        val resolvedCategory = if (category == "Custom...") {
            customCategory.trim().ifBlank { "Other" }
        } else {
            category
        }
        val resolvedKind = if (defaultKind == "Custom...") {
            customKind.trim().ifBlank { "Document" }
        } else {
            defaultKind
        }
        val chosenBase = if (starterMode == "Clone Existing") baseTemplateId else null
        return db.createCustomTemplate(
            title = title.trim().ifBlank { "Custom Fantasy Sheet" },
            description = description,
            category = resolvedCategory,
            defaultKind = resolvedKind,
            sections = if (starterMode == "Clone Existing") emptyList() else sections,
            fields = if (starterMode == "Clone Existing") emptyList() else starterFields,
            parentId = parentWorldId,
            baseTemplateId = chosenBase
        )
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize().padding(12.dp),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.DashboardCustomize,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(26.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Create Your Own Template",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Define reusable sections and fields for your world",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(48.dp).testTag("close_create_template_button")
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close template creator")
                    }
                }

                Column(
                    modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Template Name *") },
                        placeholder = { Text("e.g. Skyship Vessel, Guild Contract, Pantheon") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("create_template_title_input")
                    )

                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Description (optional)") },
                        placeholder = { Text("What does this template track?") },
                        minLines = 2,
                        maxLines = 3,
                        modifier = Modifier.fillMaxWidth().testTag("create_template_description_input")
                    )

                    // Category & Default Entry Kind
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(modifier = Modifier.weight(1f)) {
                            OutlinedButton(
                                onClick = { categoryMenuOpen = true },
                                modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 48.dp)
                            ) {
                                Text(
                                    "Category: $category",
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                            }
                            DropdownMenu(expanded = categoryMenuOpen, onDismissRequest = { categoryMenuOpen = false }) {
                                (FantasyTemplateCatalog.categories.filter { it != "All" } + "Custom...").forEach { cat ->
                                    DropdownMenuItem(
                                        text = { Text(cat) },
                                        onClick = {
                                            category = cat
                                            categoryMenuOpen = false
                                        }
                                    )
                                }
                            }
                        }

                        Box(modifier = Modifier.weight(1f)) {
                            OutlinedButton(
                                onClick = { kindMenuOpen = true },
                                modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 48.dp)
                            ) {
                                Text(
                                    "Kind: $defaultKind",
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                            }
                            DropdownMenu(expanded = kindMenuOpen, onDismissRequest = { kindMenuOpen = false }) {
                                (FantasyTemplateCatalog.starterRecordKinds.filter { it != "All" } + "Custom...").forEach { k ->
                                    DropdownMenuItem(
                                        text = { Text(k) },
                                        onClick = {
                                            defaultKind = k
                                            kindMenuOpen = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    if (category == "Custom...") {
                        OutlinedTextField(
                            value = customCategory,
                            onValueChange = { customCategory = it },
                            label = { Text("Custom Category Name") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    if (defaultKind == "Custom...") {
                        OutlinedTextField(
                            value = customKind,
                            onValueChange = { customKind = it },
                            label = { Text("Custom Entry Kind (e.g. Airship, Guild)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // Starting Structure Mode
                    Text("Starting Layout", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("Quick Starter", "Blank", "Clone Existing").forEach { mode ->
                            FilterChip(
                                selected = starterMode == mode,
                                onClick = {
                                    starterMode = mode
                                    if (mode == "Blank") {
                                        sections = listOf("Overview")
                                        starterFields = emptyList()
                                        newFieldSection = "Overview"
                                    } else if (mode == "Quick Starter" && starterFields.isEmpty()) {
                                        sections = listOf("Overview", "Details", "Lore & Notes")
                                        starterFields = listOf(
                                            FieldSpec(name = "Summary", type = "Long text", section = "Overview", required = true),
                                            FieldSpec(name = "Key Traits", type = "Long text", section = "Details"),
                                            FieldSpec(name = "Background & Notes", type = "Rich text", section = "Lore & Notes")
                                        )
                                        newFieldSection = "Overview"
                                    }
                                },
                                label = { Text(mode) },
                                modifier = Modifier.testTag("template_base_${mode.lowercase().replace(" ", "_")}")
                            )
                        }
                    }

                    if (starterMode == "Clone Existing") {
                        Box(modifier = Modifier.fillMaxWidth()) {
                            val chosenTitle = existingTemplates.firstOrNull { it.id == baseTemplateId }?.title
                                ?: "Choose a template to start from"
                            OutlinedButton(
                                onClick = { baseMenuOpen = true },
                                modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 48.dp)
                            ) {
                                Text(chosenTitle, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                            }
                            DropdownMenu(expanded = baseMenuOpen, onDismissRequest = { baseMenuOpen = false }) {
                                existingTemplates.forEach { tpl ->
                                    DropdownMenuItem(
                                        text = { Text("${tpl.title} · ${tpl.tags.ifBlank { "Other" }}") },
                                        onClick = {
                                            baseTemplateId = tpl.id
                                            if (title.isBlank()) title = "${tpl.title} Custom"
                                            baseMenuOpen = false
                                        }
                                    )
                                }
                            }
                        }
                    } else {
                        // Sections Builder
                        HorizontalDivider()
                        Text("Sections (${sections.size})", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = newSectionDraft,
                                onValueChange = { newSectionDraft = it },
                                label = { Text("Add section") },
                                singleLine = true,
                                modifier = Modifier.weight(1f).testTag("create_template_section_input")
                            )
                            FilledTonalButton(
                                onClick = {
                                    val clean = newSectionDraft.trim()
                                    if (clean.isNotBlank() && clean !in sections) {
                                        sections = sections + clean
                                        newFieldSection = clean
                                        newSectionDraft = ""
                                    }
                                },
                                modifier = Modifier.defaultMinSize(minHeight = 48.dp)
                            ) {
                                Text("Add")
                            }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            sections.forEach { sec ->
                                InputChip(
                                    selected = false,
                                    onClick = { newFieldSection = sec },
                                    label = { Text(sec) },
                                    trailingIcon = if (sections.size > 1) {
                                        {
                                            IconButton(
                                                onClick = {
                                                    sections = sections - sec
                                                    starterFields = starterFields.filterNot { it.section == sec }
                                                    if (newFieldSection == sec) newFieldSection = sections.firstOrNull() ?: "Overview"
                                                },
                                                modifier = Modifier.size(18.dp)
                                            ) {
                                                Icon(Icons.Default.Close, contentDescription = "Remove $sec", modifier = Modifier.size(14.dp))
                                            }
                                        }
                                    } else null
                                )
                            }
                        }

                        // Starter Fields Builder
                        HorizontalDivider()
                        Text("Fields (${starterFields.size})", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        ElevatedCard(
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = newFieldName,
                                    onValueChange = { newFieldName = it },
                                    label = { Text("Field name") },
                                    placeholder = { Text("e.g. Hull Material, Crew Size, Allegiance") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth().testTag("create_template_field_name_input")
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Box(modifier = Modifier.weight(1f)) {
                                        OutlinedButton(
                                            onClick = { fieldTypeMenuOpen = true },
                                            modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 48.dp)
                                        ) {
                                            Text(newFieldType, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                                            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                                        }
                                        DropdownMenu(expanded = fieldTypeMenuOpen, onDismissRequest = { fieldTypeMenuOpen = false }) {
                                            QUICK_FIELD_TYPES.forEach { t ->
                                                DropdownMenuItem(
                                                    text = { Text(t) },
                                                    onClick = {
                                                        newFieldType = t
                                                        fieldTypeMenuOpen = false
                                                    }
                                                )
                                            }
                                        }
                                    }
                                    Box(modifier = Modifier.weight(1f)) {
                                        OutlinedButton(
                                            onClick = { fieldSecMenuOpen = true },
                                            modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 48.dp)
                                        ) {
                                            Text(newFieldSection, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                                            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                                        }
                                        DropdownMenu(expanded = fieldSecMenuOpen, onDismissRequest = { fieldSecMenuOpen = false }) {
                                            sections.forEach { s ->
                                                DropdownMenuItem(
                                                    text = { Text(s) },
                                                    onClick = {
                                                        newFieldSection = s
                                                        fieldSecMenuOpen = false
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                                if (newFieldType == "Dropdown" || newFieldType == "Multi-select") {
                                    OutlinedTextField(
                                        value = newFieldOptions,
                                        onValueChange = { newFieldOptions = it },
                                        label = { Text("Options (comma separated)") },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Checkbox(checked = newFieldRequired, onCheckedChange = { newFieldRequired = it })
                                    Text("Required", fontSize = 13.sp, modifier = Modifier.weight(1f))
                                    Button(
                                        enabled = newFieldName.isNotBlank(),
                                        onClick = {
                                            starterFields = starterFields + FieldSpec(
                                                name = newFieldName.trim(),
                                                type = newFieldType,
                                                section = newFieldSection,
                                                required = newFieldRequired,
                                                options = newFieldOptions.trim(),
                                                repeatable = newFieldType == "Repeatable group"
                                            )
                                            newFieldName = ""
                                            newFieldOptions = ""
                                            newFieldRequired = false
                                        },
                                        modifier = Modifier.testTag("create_template_add_field_button")
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("Add Field")
                                    }
                                }
                            }
                        }

                        starterFields.forEachIndexed { idx, field ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                tonalElevation = 1.dp,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = field.name + if (field.required) " *" else "",
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 13.sp
                                        )
                                        Text(
                                            text = "${field.section} · ${field.type}",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    IconButton(
                                        onClick = {
                                            starterFields = starterFields.filterIndexed { i, _ -> i != idx }
                                        },
                                        modifier = Modifier.size(48.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.DeleteOutline,
                                            contentDescription = "Remove ${field.name}",
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        enabled = starterMode != "Clone Existing" || !baseTemplateId.isNullOrBlank(),
                        onClick = {
                            val tpl = buildTemplate()
                            val created = db.createRecordFromTemplate(tpl.id, parentId = parentWorldId)
                            onCreatedAndUse(created)
                        },
                        modifier = Modifier.weight(1f).defaultMinSize(minHeight = 48.dp).testTag("confirm_create_and_use_template_button")
                    ) {
                        Text("Create & Use", maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    Button(
                        enabled = starterMode != "Clone Existing" || !baseTemplateId.isNullOrBlank(),
                        onClick = {
                            val tpl = buildTemplate()
                            onCreatedForDesign(tpl)
                        },
                        modifier = Modifier.weight(1f).defaultMinSize(minHeight = 48.dp).testTag("confirm_create_template_button")
                    ) {
                        Text("Create & Design", maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
    }
}

@Composable
fun SaveAsTemplateDialog(
    record: InkRecord,
    hasAttachedSheet: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (title: String, category: String, includeProseFields: Boolean) -> Unit
) {
    var title by remember(record.id) {
        mutableStateOf(
            if (record.title.isBlank()) "Custom ${record.kind} Template"
            else "${record.title} Template"
        )
    }
    var category by remember { mutableStateOf("Other") }
    var categoryExpanded by remember { mutableStateOf(false) }
    var includeProseFields by remember { mutableStateOf(true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Save as Custom Template") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = if (hasAttachedSheet) {
                        "Saves this entry's structured sections and fields as a reusable custom template."
                    } else {
                        "Creates a reusable custom template from this entry's headings and structure."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Template Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("save_as_template_title_input")
                )
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = { categoryExpanded = true },
                        modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 48.dp)
                    ) {
                        Text("Category: $category", modifier = Modifier.weight(1f))
                        Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                    }
                    DropdownMenu(expanded = categoryExpanded, onDismissRequest = { categoryExpanded = false }) {
                        FantasyTemplateCatalog.categories.filter { it != "All" }.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat) },
                                onClick = {
                                    category = cat
                                    categoryExpanded = false
                                }
                            )
                        }
                    }
                }
                if (!hasAttachedSheet) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = includeProseFields, onCheckedChange = { includeProseFields = it })
                        Text("Create rich-text fields from document headings", fontSize = 13.sp)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                enabled = title.isNotBlank(),
                onClick = { onConfirm(title.trim(), category, includeProseFields) },
                modifier = Modifier.testTag("confirm_save_as_template_button")
            ) {
                Text("Save Template")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
