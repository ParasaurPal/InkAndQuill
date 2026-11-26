package com.parasaurpal.inkandquill.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.parasaurpal.inkandquill.data.*

@Composable
fun SheetEditorTab(
    db: InkDatabase,
    record: InkRecord,
    templateId: String,
    values: Map<String, String>,
    onValueChange: (fieldId: String, newValue: String) -> Unit,
    onOpenRecord: (InkRecord) -> Unit,
    onSwitchSheet: () -> Unit,
    onEditBlueprint: (String) -> Unit,
    onCopyToProse: () -> Unit,
    onImportFromLibrary: () -> Unit = {},
    sheetRefresh: Int = 0,
    fontSize: Float,
    modifier: Modifier = Modifier
) {
    val template = remember(templateId, sheetRefresh) { db.get(templateId) }
    val sectionOrder = remember(templateId, sheetRefresh) { db.templateSections(templateId) }
    val activeFields = remember(templateId, sheetRefresh) {
        SheetEngine.orderedFields(db.templateFields(templateId, includeRemoved = false), sectionOrder)
    }
    val allFieldsIncludingRemoved = remember(templateId, sheetRefresh) {
        db.templateFields(templateId, includeRemoved = true)
    }

    val visibleFields = remember(activeFields, values) {
        activeFields.filter { SheetEngine.isFieldVisible(it, activeFields, values) }
    }
    val groupedSections = remember(visibleFields) {
        visibleFields.groupBy { it.section.ifBlank { "Details" } }
    }
    val sectionKeys = remember(groupedSections) { groupedSections.keys.toList() }

    var collapsedSections by remember(templateId) { mutableStateOf<Set<String>>(emptySet()) }
    var showArchived by remember { mutableStateOf(false) }

    val archivedWithValues = remember(allFieldsIncludingRemoved, activeFields, values) {
        val activeIds = activeFields.map { it.id }.toSet()
        allFieldsIncludingRemoved.filter { it.id !in activeIds && values[it.id].orEmpty().isNotBlank() }
    }

    val nonCalcVisible = remember(visibleFields) { visibleFields.filter { it.fieldType != "Calculated" } }
    val filledCount = remember(nonCalcVisible, values) {
        nonCalcVisible.count { values[it.id].orEmpty().isNotBlank() }
    }
    val progress = if (nonCalcVisible.isEmpty()) 0f else filledCount.toFloat() / nonCalcVisible.size.toFloat()

    LazyColumn(
        modifier = modifier.fillMaxSize().testTag("sheet_editor_list"),
        contentPadding = PaddingValues(vertical = 12.dp, horizontal = 2.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item(key = "sheet_header") {
            ElevatedCard(
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = template?.title ?: "Attached Sheet",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${template?.tags.orEmpty().ifBlank { "Worldbuilding" }} · $filledCount of ${nonCalcVisible.size} fields filled",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        TextButton(
                            onClick = onSwitchSheet,
                            modifier = Modifier.testTag("switch_sheet_button")
                        ) {
                            Text("Switch")
                        }
                    }

                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxWidth().height(6.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = {
                                collapsedSections = if (collapsedSections.size >= sectionKeys.size) emptySet()
                                else sectionKeys.toSet()
                            }
                        ) {
                            Icon(
                                if (collapsedSections.size >= sectionKeys.size) Icons.Default.UnfoldMore else Icons.Default.UnfoldLess,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(if (collapsedSections.size >= sectionKeys.size) "Expand All" else "Collapse All")
                        }
                        if (template != null) {
                            TextButton(
                                onClick = { onEditBlueprint(template.id) },
                                modifier = Modifier.testTag("edit_sheet_blueprint_button")
                            ) {
                                Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Blueprint")
                            }
                        }
                        TextButton(
                            onClick = onImportFromLibrary,
                            modifier = Modifier.testTag("sheet_import_library_button")
                        ) {
                            Icon(Icons.Default.CollectionsBookmark, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Library")
                        }
                        Spacer(Modifier.weight(1f))
                        FilledTonalButton(
                            onClick = onCopyToProse,
                            modifier = Modifier.testTag("copy_sheet_to_prose_button")
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("To Prose")
                        }
                    }
                }
            }
        }

        if (visibleFields.isEmpty()) {
            item(key = "sheet_empty") {
                ElevatedCard(
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.DashboardCustomize,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(32.dp)
                        )
                        Text("This sheet has no active fields yet", fontWeight = FontWeight.SemiBold)
                        Text(
                            "Open Blueprint to add sections and custom fields, or switch to another template sheet.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (template != null) {
                            Button(onClick = { onEditBlueprint(template.id) }) {
                                Text("Design Sheet Fields")
                            }
                        }
                    }
                }
            }
        }

        items(sectionKeys, key = { "section_$it" }) { sectionName ->
            val sectionFields = groupedSections[sectionName].orEmpty()
            val expanded = sectionName !in collapsedSections
            val sectionNonCalc = sectionFields.filter { it.fieldType != "Calculated" }
            val sectionFilled = sectionNonCalc.count { values[it.id].orEmpty().isNotBlank() }

            ElevatedCard(
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 52.dp)
                            .clickable {
                                collapsedSections = if (expanded) collapsedSections + sectionName
                                else collapsedSections - sectionName
                            }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = sectionName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "$sectionFilled / ${sectionNonCalc.size} completed",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Icon(
                            imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = if (expanded) "Collapse $sectionName" else "Expand $sectionName"
                        )
                    }

                    AnimatedVisibility(visible = expanded) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            HorizontalDivider()
                            sectionFields.forEach { field ->
                                SheetFieldInput(
                                    db = db,
                                    recordId = record.id,
                                    field = field,
                                    allFields = activeFields,
                                    values = values,
                                    onValueChange = { next -> onValueChange(field.id, next) },
                                    onOpenRecord = onOpenRecord,
                                    fontSize = fontSize
                                )
                            }
                        }
                    }
                }
            }
        }

        if (archivedWithValues.isNotEmpty()) {
            item(key = "archived_fields") {
                OutlinedCard(
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showArchived = !showArchived },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Inventory2, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                            Spacer(Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "Archived Fields (${archivedWithValues.size})",
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    "Data saved before fields were removed from the template",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Icon(
                                if (showArchived) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = "Toggle archived fields"
                            )
                        }
                        AnimatedVisibility(visible = showArchived) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                archivedWithValues.forEach { archivedField ->
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(10.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(archivedField.name, fontWeight = FontWeight.Medium)
                                                Text(
                                                    values[archivedField.id].orEmpty(),
                                                    style = MaterialTheme.typography.bodySmall
                                                )
                                            }
                                            TextButton(onClick = { onValueChange(archivedField.id, "") }) {
                                                Text("Clear")
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        item { Spacer(Modifier.height(24.dp)) }
    }
}
