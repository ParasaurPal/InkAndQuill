package com.parasaurpal.inkandquill.ui

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.parasaurpal.inkandquill.data.*

@Composable
fun TemplateLibraryDialog(
    db: InkDatabase,
    targetRecord: InkRecord? = null,
    initialGroup: String = "All",
    onDismiss: () -> Unit,
    onCreateEntryFromLibrary: (InkRecord) -> Unit = {},
    onImportedBlueprint: (InkRecord) -> Unit = {},
    onImportedIntoSheet: (ImportSheetResult) -> Unit = {}
) {
    val context = LocalContext.current
    val allArchetypes = remember(context) { TemplateLibraryManager.loadArchetypes(context) }
    val categories = remember(allArchetypes) { TemplateLibraryManager.availableCategories(allArchetypes) }

    val initialFilter = remember(allArchetypes, initialGroup) {
        when {
            initialGroup == "All" || initialGroup.isBlank() -> LibraryFilterState()
            initialGroup in categories -> LibraryFilterState(selectedCategory = initialGroup)
            else -> {
                val matchingCategory = allArchetypes
                    .firstOrNull { it.group.equals(initialGroup, ignoreCase = true) }
                    ?.libraryCategory
                    ?: "All"
                LibraryFilterState(
                    selectedCategory = matchingCategory,
                    selectedSubgroup = if (matchingCategory != "All") initialGroup else "All"
                )
            }
        }
    }

    var filterState by remember(initialFilter) { mutableStateOf(initialFilter) }
    var groupByCategory by remember { mutableStateOf(true) }
    var collapsedCategories by remember { mutableStateOf(setOf<String>()) }
    var includeExamplesOnCreate by remember { mutableStateOf(false) }
    var showSortMenu by remember { mutableStateOf(false) }

    var inspectedArchetype by remember { mutableStateOf<TemplateLibraryArchetype?>(null) }
    var sheetImportTarget by remember { mutableStateOf<TemplateLibraryArchetype?>(null) }
    var showCustomJsonDialog by remember { mutableStateOf(false) }

    val queryResult = remember(allArchetypes, filterState) {
        TemplateLibraryManager.queryArchetypes(allArchetypes, filterState)
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
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CollectionsBookmark,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (targetRecord != null) "Import into Sheet" else "Template Library",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (targetRecord != null) {
                                "JSON archetypes for ${targetRecord.title.ifBlank { "this entry" }}"
                            } else {
                                "${allArchetypes.size} pre-built JSON archetypes across ${categories.size - 1} categories"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    FilledTonalButton(
                        onClick = { showCustomJsonDialog = true },
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("import_custom_json_archetype_button")
                    ) {
                        Icon(Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("JSON")
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(48.dp).testTag("close_template_library_button")
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close Template Library")
                    }
                }

                // Search input
                OutlinedTextField(
                    value = filterState.query,
                    onValueChange = { filterState = filterState.copy(query = it) },
                    modifier = Modifier.fillMaxWidth().testTag("template_library_search"),
                    label = { Text("Search archetypes, categories, tags, or fields...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (filterState.query.isNotEmpty()) {
                            IconButton(onClick = { filterState = filterState.copy(query = "") }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear search")
                            }
                        }
                    },
                    singleLine = true
                )

                // Primary Category Filter Chips (Geography, Deities, Societies, Characters, Items, Magic, Ecology)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    categories.forEach { category ->
                        val count = queryResult.categoryCounts[category] ?: 0
                        val selected = filterState.selectedCategory == category
                        FilterChip(
                            selected = selected,
                            onClick = {
                                val nextCategory = if (selected && category != "All") "All" else category
                                filterState = filterState.copy(
                                    selectedCategory = nextCategory,
                                    selectedSubgroup = "All"
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = categoryIcon(category),
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            label = {
                                Text(
                                    text = if (category == "All") "All ($count)" else "$category · $count",
                                    maxLines = 1
                                )
                            },
                            modifier = Modifier.testTag(
                                "library_category_${category.lowercase().replace(" ", "_")}"
                            )
                        )
                    }
                }

                // Secondary Sub-group Filter Chips when a multi-group category is selected
                AnimatedVisibility(
                    visible = queryResult.availableSubgroups.isNotEmpty(),
                    enter = fadeIn(spring()) + expandVertically(spring()),
                    exit = fadeOut(spring()) + shrinkVertically(spring())
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${filterState.selectedCategory}:",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(end = 2.dp)
                        )
                        queryResult.availableSubgroups.forEach { subgroup ->
                            val selected = filterState.selectedSubgroup == subgroup
                            FilterChip(
                                selected = selected,
                                onClick = {
                                    val nextSub = if (selected && subgroup != "All") "All" else subgroup
                                    filterState = filterState.copy(selectedSubgroup = nextSub)
                                },
                                label = { Text(subgroup, fontSize = 12.sp) },
                                modifier = Modifier.testTag(
                                    "library_subgroup_${subgroup.lowercase().replace(" ", "_")}"
                                )
                            )
                        }
                    }
                }

                // Filter & Grouping Control Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilterChip(
                        selected = groupByCategory,
                        onClick = { groupByCategory = !groupByCategory },
                        leadingIcon = {
                            Icon(
                                imageVector = if (groupByCategory) Icons.Default.ViewAgenda else Icons.Default.ViewStream,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        label = { Text("Grouped") },
                        modifier = Modifier.testTag("library_toggle_grouped")
                    )

                    Box {
                        FilterChip(
                            selected = filterState.sortMode != LibrarySortMode.CATEGORY,
                            onClick = { showSortMenu = true },
                            leadingIcon = {
                                Icon(Icons.Default.Sort, contentDescription = null, modifier = Modifier.size(16.dp))
                            },
                            label = { Text("Sort: ${filterState.sortMode.label}") },
                            modifier = Modifier.testTag("library_sort_button")
                        )
                        DropdownMenu(
                            expanded = showSortMenu,
                            onDismissRequest = { showSortMenu = false }
                        ) {
                            LibrarySortMode.entries.forEach { mode ->
                                DropdownMenuItem(
                                    text = { Text(mode.label) },
                                    leadingIcon = {
                                        if (filterState.sortMode == mode) {
                                            Icon(Icons.Default.Check, contentDescription = null)
                                        }
                                    },
                                    onClick = {
                                        filterState = filterState.copy(sortMode = mode)
                                        showSortMenu = false
                                    }
                                )
                            }
                        }
                    }

                    FilterChip(
                        selected = filterState.onlyWithExamples,
                        onClick = {
                            filterState = filterState.copy(onlyWithExamples = !filterState.onlyWithExamples)
                        },
                        leadingIcon = {
                            Icon(Icons.Default.AutoFixHigh, contentDescription = null, modifier = Modifier.size(16.dp))
                        },
                        label = { Text("With Examples") },
                        modifier = Modifier.testTag("library_examples_filter")
                    )

                    if (!filterState.selectedTag.isNullOrBlank()) {
                        InputChip(
                            selected = true,
                            onClick = { filterState = filterState.copy(selectedTag = null) },
                            label = { Text("#${filterState.selectedTag}") },
                            trailingIcon = {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Clear tag filter",
                                    modifier = Modifier.size(14.dp)
                                )
                            },
                            modifier = Modifier.testTag("library_active_tag_chip")
                        )
                    }

                    if (filterState.hasActiveFilters) {
                        TextButton(
                            onClick = {
                                filterState = LibraryFilterState(sortMode = filterState.sortMode)
                                collapsedCategories = emptySet()
                            },
                            modifier = Modifier.testTag("library_reset_filters_button")
                        ) {
                            Icon(Icons.Default.RestartAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Reset")
                        }
                    }
                }

                // Active Status + Collapse/Expand All + Sample Data Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = buildString {
                            append("${queryResult.filtered.size} of ${allArchetypes.size} archetypes")
                            if (groupByCategory && queryResult.groupedByCategory.isNotEmpty()) {
                                append(" · ${queryResult.groupedByCategory.size} ")
                                append(if (queryResult.groupedByCategory.size == 1) "category" else "categories")
                            }
                        },
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )
                    if (groupByCategory && queryResult.groupedByCategory.size > 1) {
                        val allCollapsed = queryResult.groupedByCategory.keys.all { it in collapsedCategories }
                        TextButton(
                            onClick = {
                                collapsedCategories = if (allCollapsed) {
                                    emptySet()
                                } else {
                                    queryResult.groupedByCategory.keys.toSet()
                                }
                            },
                            modifier = Modifier.testTag("library_collapse_all_button")
                        ) {
                            Text(if (allCollapsed) "Expand all" else "Collapse all", fontSize = 12.sp)
                        }
                    }
                }

                if (targetRecord == null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { includeExamplesOnCreate = !includeExamplesOnCreate },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = includeExamplesOnCreate,
                            onCheckedChange = { includeExamplesOnCreate = it },
                            modifier = Modifier.testTag("library_include_samples_checkbox")
                        )
                        Text(
                            text = "Pre-fill new sheets with example worldbuilding data",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Archetype Results List (Grouped by Category or Flat Sorted)
                if (queryResult.filtered.isEmpty()) {
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
                                Icons.Default.SearchOff,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(44.dp)
                            )
                            Text("No matching JSON archetypes", fontWeight = FontWeight.SemiBold)
                            Text(
                                "Try clearing a category or tag filter, adjusting your search, or importing a custom JSON file.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (filterState.hasActiveFilters) {
                                FilledTonalButton(
                                    onClick = { filterState = LibraryFilterState(sortMode = filterState.sortMode) }
                                ) {
                                    Text("Reset Filters")
                                }
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f).testTag("template_library_list"),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(bottom = 16.dp)
                    ) {
                        if (groupByCategory) {
                            queryResult.groupedByCategory.forEach { (category, itemsInCategory) ->
                                val isCollapsed = category in collapsedCategories
                                item(key = "header_$category") {
                                    CategorySectionHeader(
                                        category = category,
                                        count = itemsInCategory.size,
                                        isCollapsed = isCollapsed,
                                        showFocusButton = filterState.selectedCategory == "All" &&
                                            queryResult.groupedByCategory.size > 1,
                                        onToggleCollapse = {
                                            collapsedCategories = if (isCollapsed) {
                                                collapsedCategories - category
                                            } else {
                                                collapsedCategories + category
                                            }
                                        },
                                        onFocusCategory = {
                                            filterState = filterState.copy(
                                                selectedCategory = category,
                                                selectedSubgroup = "All"
                                            )
                                            collapsedCategories = collapsedCategories - category
                                        }
                                    )
                                }
                                if (!isCollapsed) {
                                    items(itemsInCategory, key = { it.id }) { item ->
                                        ArchetypeLibraryCard(
                                            item = item,
                                            targetRecord = targetRecord,
                                            activeTag = filterState.selectedTag,
                                            onSelectCategory = { cat ->
                                                filterState = filterState.copy(
                                                    selectedCategory = cat,
                                                    selectedSubgroup = "All"
                                                )
                                            },
                                            onToggleTag = { tag ->
                                                val nextTag = if (filterState.selectedTag.equals(tag, ignoreCase = true)) {
                                                    null
                                                } else {
                                                    tag
                                                }
                                                filterState = filterState.copy(selectedTag = nextTag)
                                            },
                                            onUseSheet = {
                                                val created = TemplateLibraryManager.importAndCreateEntry(
                                                    db = db,
                                                    archetype = item,
                                                    includeSampleValues = includeExamplesOnCreate
                                                )
                                                Toast.makeText(
                                                    context,
                                                    "Created ${item.title} sheet",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                                onCreateEntryFromLibrary(created)
                                            },
                                            onEditBlueprint = {
                                                val blueprint = TemplateLibraryManager.importAsTemplateBlueprint(db, item)
                                                Toast.makeText(
                                                    context,
                                                    "Imported ${blueprint.title} to Templates",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                                onImportedBlueprint(blueprint)
                                            },
                                            onImportIntoSheet = { sheetImportTarget = item },
                                            onInspect = { inspectedArchetype = item }
                                        )
                                    }
                                }
                            }
                        } else {
                            items(queryResult.filtered, key = { it.id }) { item ->
                                ArchetypeLibraryCard(
                                    item = item,
                                    targetRecord = targetRecord,
                                    activeTag = filterState.selectedTag,
                                    onSelectCategory = { cat ->
                                        filterState = filterState.copy(
                                            selectedCategory = cat,
                                            selectedSubgroup = "All"
                                        )
                                    },
                                    onToggleTag = { tag ->
                                        val nextTag = if (filterState.selectedTag.equals(tag, ignoreCase = true)) {
                                            null
                                        } else {
                                            tag
                                        }
                                        filterState = filterState.copy(selectedTag = nextTag)
                                    },
                                    onUseSheet = {
                                        val created = TemplateLibraryManager.importAndCreateEntry(
                                            db = db,
                                            archetype = item,
                                            includeSampleValues = includeExamplesOnCreate
                                        )
                                        Toast.makeText(
                                            context,
                                            "Created ${item.title} sheet",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                        onCreateEntryFromLibrary(created)
                                    },
                                    onEditBlueprint = {
                                        val blueprint = TemplateLibraryManager.importAsTemplateBlueprint(db, item)
                                        Toast.makeText(
                                            context,
                                            "Imported ${blueprint.title} to Templates",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                        onImportedBlueprint(blueprint)
                                    },
                                    onImportIntoSheet = { sheetImportTarget = item },
                                    onInspect = { inspectedArchetype = item }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    inspectedArchetype?.let { archetype ->
        ArchetypeInspectorDialog(
            archetype = archetype,
            targetRecord = targetRecord,
            onDismiss = { inspectedArchetype = null },
            onImportIntoSheet = {
                inspectedArchetype = null
                sheetImportTarget = archetype
            },
            onCreateEntry = {
                inspectedArchetype = null
                val created = TemplateLibraryManager.importAndCreateEntry(
                    db = db,
                    archetype = archetype,
                    includeSampleValues = includeExamplesOnCreate
                )
                onCreateEntryFromLibrary(created)
            },
            onImportBlueprint = {
                inspectedArchetype = null
                val tpl = TemplateLibraryManager.importAsTemplateBlueprint(db, archetype)
                onImportedBlueprint(tpl)
            }
        )
    }

    if (targetRecord != null && sheetImportTarget != null) {
        ImportIntoSheetDialog(
            db = db,
            record = targetRecord,
            archetype = sheetImportTarget!!,
            onDismiss = { sheetImportTarget = null },
            onConfirm = { mode, prefill ->
                val target = sheetImportTarget!!
                val result = TemplateLibraryManager.importIntoExistingSheet(
                    db = db,
                    recordId = targetRecord.id,
                    archetype = target,
                    mode = mode,
                    includeSampleValues = prefill
                )
                sheetImportTarget = null
                val msg = if (mode == SheetImportMode.MERGE_INTO_CURRENT) {
                    "Merged ${result.fieldsAdded} field(s) into sheet"
                } else {
                    "Attached ${target.title} sheet"
                }
                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                onImportedIntoSheet(result)
            }
        )
    }

    if (showCustomJsonDialog) {
        CustomJsonArchetypeDialog(
            hasTargetSheet = targetRecord != null,
            onDismiss = { showCustomJsonDialog = false },
            onParsedImport = { parsed ->
                showCustomJsonDialog = false
                if (targetRecord != null) {
                    sheetImportTarget = parsed
                } else {
                    val tpl = TemplateLibraryManager.importAsTemplateBlueprint(db, parsed)
                    Toast.makeText(context, "Imported ${tpl.title}", Toast.LENGTH_SHORT).show()
                    onImportedBlueprint(tpl)
                }
            }
        )
    }
}

@Composable
private fun CategorySectionHeader(
    category: String,
    count: Int,
    isCollapsed: Boolean,
    showFocusButton: Boolean,
    onToggleCollapse: () -> Unit,
    onFocusCategory: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 2.dp,
        modifier = Modifier
            .fillMaxWidth()
            .minimumInteractiveComponentSize()
            .clickable(onClick = onToggleCollapse)
            .testTag("library_section_header_${category.lowercase().replace(" ", "_")}")
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Icon(
                    imageVector = categoryIcon(category),
                    contentDescription = category,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(8.dp).size(20.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = category,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer
                    ) {
                        Text(
                            text = count.toString(),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                        )
                    }
                }
                Text(
                    text = TemplateLibraryManager.categorySubtitle(category),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (showFocusButton) {
                TextButton(
                    onClick = onFocusCategory,
                    modifier = Modifier.testTag("library_focus_${category.lowercase().replace(" ", "_")}")
                ) {
                    Text("Only", fontSize = 12.sp)
                }
            }
            Icon(
                imageVector = if (isCollapsed) Icons.Default.ExpandMore else Icons.Default.ExpandLess,
                contentDescription = if (isCollapsed) "Expand $category" else "Collapse $category",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ArchetypeLibraryCard(
    item: TemplateLibraryArchetype,
    targetRecord: InkRecord?,
    activeTag: String?,
    onSelectCategory: (String) -> Unit,
    onToggleTag: (String) -> Unit,
    onUseSheet: () -> Unit,
    onEditBlueprint: () -> Unit,
    onImportIntoSheet: () -> Unit,
    onInspect: () -> Unit
) {
    ElevatedCard(
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.fillMaxWidth().testTag("library_card_${item.id}")
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = subgroupIcon(item.group, item.libraryCategory),
                    contentDescription = item.group,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${item.defaultKind} · ${item.sections.size} sections · ${item.fields.size} fields",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    modifier = Modifier.clickable { onSelectCategory(item.libraryCategory) }
                ) {
                    Text(
                        text = "${item.libraryCategory} · ${item.group}",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            if (item.description.isNotBlank()) {
                Text(
                    text = item.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (item.tags.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    item.tags.forEach { tag ->
                        val isTagActive = activeTag.equals(tag, ignoreCase = true)
                        FilterChip(
                            selected = isTagActive,
                            onClick = { onToggleTag(tag) },
                            label = { Text(tag, fontSize = 11.sp) }
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (targetRecord != null) {
                    Button(
                        onClick = onImportIntoSheet,
                        modifier = Modifier.testTag("library_import_into_sheet_${item.id}")
                    ) {
                        Icon(Icons.Default.DownloadDone, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Import into Sheet")
                    }
                } else {
                    Button(
                        onClick = onUseSheet,
                        modifier = Modifier.testTag("library_create_entry_${item.id}")
                    ) {
                        Icon(Icons.Default.NoteAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Use Sheet")
                    }
                    FilledTonalButton(
                        onClick = onEditBlueprint,
                        modifier = Modifier.testTag("library_to_blueprint_${item.id}")
                    ) {
                        Text("Edit Blueprint")
                    }
                }
                TextButton(
                    onClick = onInspect,
                    modifier = Modifier.testTag("library_inspect_${item.id}")
                ) {
                    Icon(Icons.Default.Code, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Preview JSON")
                }
            }
        }
    }
}

private fun categoryIcon(category: String): ImageVector = when (category) {
    "All" -> Icons.Default.Apps
    "Geography" -> Icons.Default.Public
    "Deities" -> Icons.Default.AutoAwesome
    "Societies" -> Icons.Default.Groups
    "Characters" -> Icons.Default.Person
    "Items" -> Icons.Default.WorkspacePremium
    "Magic" -> Icons.Default.Bolt
    "Ecology" -> Icons.Default.Forest
    else -> Icons.Default.DashboardCustomize
}

private fun subgroupIcon(group: String, libraryCategory: String): ImageVector = when (group) {
    "Cities" -> Icons.Default.LocationCity
    "Realms" -> Icons.Default.Public
    "Gods" -> Icons.Default.AutoAwesome
    "Religions" -> Icons.Default.TempleHindu
    "Cultures" -> Icons.Default.Groups
    "Factions" -> Icons.Default.Shield
    "Languages" -> Icons.Default.Translate
    "Characters" -> Icons.Default.Person
    "Items" -> Icons.Default.WorkspacePremium
    "Magic & Systems" -> Icons.Default.Bolt
    "Creatures" -> Icons.Default.Pets
    "Flora" -> Icons.Default.Spa
    else -> categoryIcon(libraryCategory)
}
