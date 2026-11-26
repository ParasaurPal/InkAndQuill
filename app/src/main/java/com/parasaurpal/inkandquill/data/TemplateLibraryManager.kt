package com.parasaurpal.inkandquill.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class TemplateLibraryArchetype(
    val id: String,
    val fileName: String,
    val libraryCategory: String,
    val group: String,
    val title: String,
    val description: String,
    val category: String,
    val defaultKind: String,
    val tags: List<String>,
    val sections: List<String>,
    val fields: List<FieldSpec>,
    val sampleValues: Map<String, String>,
    val rawJson: String
)

enum class SheetImportMode {
    ATTACH_NEW_SHEET,
    MERGE_INTO_CURRENT
}

enum class LibrarySortMode(val label: String) {
    CATEGORY("Category"),
    TITLE_ASC("Title A–Z"),
    COMPLEXITY_DESC("Most Fields")
}

data class LibraryFilterState(
    val query: String = "",
    val selectedCategory: String = "All",
    val selectedSubgroup: String = "All",
    val selectedTag: String? = null,
    val onlyWithExamples: Boolean = false,
    val sortMode: LibrarySortMode = LibrarySortMode.CATEGORY
) {
    val hasActiveFilters: Boolean
        get() = query.isNotBlank() ||
            selectedCategory != "All" ||
            selectedSubgroup != "All" ||
            !selectedTag.isNullOrBlank() ||
            onlyWithExamples
}

data class LibraryQueryResult(
    val filtered: List<TemplateLibraryArchetype>,
    val groupedByCategory: Map<String, List<TemplateLibraryArchetype>>,
    val categoryCounts: Map<String, Int>,
    val availableSubgroups: List<String>,
    val availableTags: List<String>
)

data class ImportSheetResult(
    val templateId: String,
    val fieldsAdded: Int,
    val valuesPrefilled: Int
)

object TemplateLibraryManager {

    private val preferredCategoryOrder = listOf(
        "All",
        "Geography",
        "Deities",
        "Societies",
        "Characters",
        "Items",
        "Magic",
        "Ecology",
        "Custom"
    )

    private val preferredGroupOrder = listOf(
        "All",
        "Cities",
        "Realms",
        "Gods",
        "Religions",
        "Cultures",
        "Factions",
        "Languages",
        "Characters",
        "Items",
        "Magic & Systems",
        "Creatures",
        "Flora",
        "Custom"
    )

    @Volatile
    private var cachedArchetypes: List<TemplateLibraryArchetype>? = null

    fun loadArchetypes(context: Context): List<TemplateLibraryArchetype> {
        cachedArchetypes?.let { return it }
        val assets = context.applicationContext.assets
        val files = runCatching { assets.list("template_library")?.toList().orEmpty() }
            .getOrDefault(emptyList())
            .filter { it.endsWith(".json", ignoreCase = true) }
            .sorted()

        val loaded = files.mapNotNull { fileName ->
            runCatching {
                val raw = assets.open("template_library/$fileName").bufferedReader().use { it.readText() }
                parseArchetypeJson(raw, fileName)
            }.getOrNull()
        }.sortedWith(
            compareBy<TemplateLibraryArchetype>(
                {
                    val idx = preferredCategoryOrder.indexOf(it.libraryCategory)
                    if (idx >= 0) idx else preferredCategoryOrder.size
                },
                {
                    val idx = preferredGroupOrder.indexOf(it.group)
                    if (idx >= 0) idx else preferredGroupOrder.size
                },
                { it.title.lowercase() }
            )
        )
        cachedArchetypes = loaded
        return loaded
    }

    fun availableCategories(archetypes: List<TemplateLibraryArchetype>): List<String> {
        val present = archetypes.map { it.libraryCategory }.toSet()
        return listOf("All") +
            preferredCategoryOrder.filter { it != "All" && it in present } +
            present.filter { it !in preferredCategoryOrder }.sorted()
    }

    fun availableGroups(archetypes: List<TemplateLibraryArchetype>): List<String> {
        val present = archetypes.map { it.group }.toSet()
        return listOf("All") + preferredGroupOrder.filter { it != "All" && it in present } +
            present.filter { it !in preferredGroupOrder }.sorted()
    }

    fun categorySubtitle(category: String): String = when (category) {
        "Geography" -> "Cities, ports, kingdoms & realms"
        "Deities" -> "Gods, eldritch patrons & religions"
        "Societies" -> "Cultures, factions & languages"
        "Characters" -> "Heroes, villains & lineages"
        "Items" -> "Sentient relics, arms & artifacts"
        "Magic" -> "Arcane laws & power systems"
        "Ecology" -> "Apex beasts, flora & ecosystems"
        else -> "Custom & imported archetypes"
    }

    fun queryArchetypes(
        archetypes: List<TemplateLibraryArchetype>,
        filter: LibraryFilterState
    ): LibraryQueryResult {
        val q = filter.query.trim()
        val baseMatches = archetypes.filter { item ->
            val queryMatch = q.isBlank() ||
                item.title.contains(q, ignoreCase = true) ||
                item.libraryCategory.contains(q, ignoreCase = true) ||
                item.group.contains(q, ignoreCase = true) ||
                item.category.contains(q, ignoreCase = true) ||
                item.description.contains(q, ignoreCase = true) ||
                item.defaultKind.contains(q, ignoreCase = true) ||
                item.tags.any { it.contains(q, ignoreCase = true) } ||
                item.sections.any { it.contains(q, ignoreCase = true) } ||
                item.fields.any { it.name.contains(q, ignoreCase = true) }
            val tagMatch = filter.selectedTag.isNullOrBlank() ||
                item.tags.any { it.equals(filter.selectedTag, ignoreCase = true) }
            val exampleMatch = !filter.onlyWithExamples || item.sampleValues.isNotEmpty()
            queryMatch && tagMatch && exampleMatch
        }

        val categoryCounts = buildMap {
            put("All", baseMatches.size)
            availableCategories(archetypes).forEach { cat ->
                if (cat != "All") {
                    put(cat, baseMatches.count { it.libraryCategory == cat })
                }
            }
        }

        val categoryMatches = if (filter.selectedCategory == "All") {
            baseMatches
        } else {
            baseMatches.filter { it.libraryCategory == filter.selectedCategory }
        }

        val subgroupCandidates = if (filter.selectedCategory == "All") {
            emptyList()
        } else {
            val presentGroups = archetypes
                .filter { it.libraryCategory == filter.selectedCategory }
                .map { it.group }
                .toSet()
            if (presentGroups.size <= 1) {
                emptyList()
            } else {
                listOf("All") +
                    preferredGroupOrder.filter { it != "All" && it in presentGroups } +
                    presentGroups.filter { it !in preferredGroupOrder }.sorted()
            }
        }

        val effectiveSubgroup = if (
            filter.selectedSubgroup == "All" || filter.selectedSubgroup in subgroupCandidates
        ) {
            filter.selectedSubgroup
        } else {
            "All"
        }

        val afterSubgroup = if (effectiveSubgroup == "All") {
            categoryMatches
        } else {
            categoryMatches.filter { it.group == effectiveSubgroup }
        }

        val sorted = when (filter.sortMode) {
            LibrarySortMode.CATEGORY -> afterSubgroup.sortedWith(
                compareBy<TemplateLibraryArchetype>(
                    {
                        val idx = preferredCategoryOrder.indexOf(it.libraryCategory)
                        if (idx >= 0) idx else preferredCategoryOrder.size
                    },
                    {
                        val idx = preferredGroupOrder.indexOf(it.group)
                        if (idx >= 0) idx else preferredGroupOrder.size
                    },
                    { it.title.lowercase() }
                )
            )
            LibrarySortMode.TITLE_ASC -> afterSubgroup.sortedBy { it.title.lowercase() }
            LibrarySortMode.COMPLEXITY_DESC -> afterSubgroup.sortedWith(
                compareByDescending<TemplateLibraryArchetype> { it.fields.size }
                    .thenBy { it.title.lowercase() }
            )
        }

        val grouped = LinkedHashMap<String, List<TemplateLibraryArchetype>>()
        val orderedCats = availableCategories(sorted).filter { it != "All" }
        orderedCats.forEach { cat ->
            val itemsForCat = sorted.filter { it.libraryCategory == cat }
            if (itemsForCat.isNotEmpty()) {
                grouped[cat] = itemsForCat
            }
        }

        val availableTags = categoryMatches
            .flatMap { it.tags }
            .distinct()
            .sorted()

        return LibraryQueryResult(
            filtered = sorted,
            groupedByCategory = grouped,
            categoryCounts = categoryCounts,
            availableSubgroups = subgroupCandidates,
            availableTags = availableTags
        )
    }

    fun parseArchetypeJson(rawJson: String, fileName: String = "custom.json"): TemplateLibraryArchetype {
        val obj = JSONObject(rawJson.trim())
        val format = obj.optString("format", "")
        val fieldsArr = obj.optJSONArray("fields")
        val sectionsArr = obj.optJSONArray("sections")
        require(format == "inkandquill-template" || fieldsArr != null || sectionsArr != null) {
            "JSON must be an Ink & Quill template or contain 'fields' / 'sections'."
        }

        val title = obj.optString("title", "").trim()
            .ifBlank { fileName.removeSuffix(".json").replace('_', ' ').replaceFirstChar(Char::uppercase) }
        val description = obj.optString("body", obj.optString("description", "")).trim()
        val category = obj.optString("category", "Other").trim().ifBlank { "Other" }
        val defaultKind = obj.optString("defaultKind", "Document").trim().ifBlank { "Document" }
        val group = obj.optString("archetypeGroup", "").trim()
            .ifBlank { inferGroup(category, defaultKind) }
        val libraryCategory = resolveLibraryCategory(
            explicit = obj.optString("libraryCategory", "").trim(),
            group = group,
            templateCategory = category,
            defaultKind = defaultKind
        )
        val id = obj.optString("archetypeId", "").trim()
            .ifBlank { fileName.removeSuffix(".json").lowercase().replace(Regex("[^a-z0-9_]+"), "_") }

        val tags = buildList {
            val tagsArr = obj.optJSONArray("tags")
            if (tagsArr != null) {
                for (i in 0 until tagsArr.length()) {
                    val t = tagsArr.optString(i).trim()
                    if (t.isNotBlank()) add(t)
                }
            }
        }

        val sections = buildList {
            if (sectionsArr != null) {
                for (i in 0 until sectionsArr.length()) {
                    val s = sectionsArr.optString(i).trim()
                    if (s.isNotBlank() && s !in this) add(s)
                }
            }
        }.toMutableList()

        val fields = buildList {
            if (fieldsArr != null) {
                for (i in 0 until fieldsArr.length()) {
                    val f = fieldsArr.optJSONObject(i) ?: continue
                    val name = f.optString("name", "").trim()
                    if (name.isBlank()) continue
                    val sec = f.optString("section", "General").trim().ifBlank { "General" }
                    if (sec !in sections) sections.add(sec)
                    add(
                        FieldSpec(
                            name = name,
                            type = f.optString("type", "Long text").ifBlank { "Long text" },
                            section = sec,
                            required = f.optBoolean("required", false),
                            options = f.optString("options", ""),
                            hint = f.optString("hint", ""),
                            conditionField = f.optString("conditionField", ""),
                            conditionValue = f.optString("conditionValue", ""),
                            formula = f.optString("formula", ""),
                            repeatable = f.optBoolean("repeatable", false)
                        )
                    )
                }
            }
        }

        if (sections.isEmpty()) sections.add("General")

        val samples = buildMap {
            val sampleObj = obj.optJSONObject("sampleValues")
            if (sampleObj != null) {
                sampleObj.keys().forEach { k ->
                    val v = sampleObj.optString(k, "")
                    if (v.isNotBlank()) put(k, v)
                }
            }
            if (fieldsArr != null) {
                for (i in 0 until fieldsArr.length()) {
                    val f = fieldsArr.optJSONObject(i) ?: continue
                    val name = f.optString("name", "").trim()
                    val defVal = f.optString("defaultValue", "")
                    if (name.isNotBlank() && defVal.isNotBlank() && !containsKey(name)) {
                        put(name, defVal)
                    }
                }
            }
        }

        val normalizedJson = runCatching { obj.toString(2) }.getOrDefault(rawJson)

        return TemplateLibraryArchetype(
            id = id,
            fileName = fileName,
            libraryCategory = libraryCategory,
            group = group,
            title = title,
            description = description,
            category = category,
            defaultKind = defaultKind,
            tags = tags,
            sections = sections,
            fields = fields,
            sampleValues = samples,
            rawJson = normalizedJson
        )
    }

    fun toImportableTemplateJson(archetype: TemplateLibraryArchetype): String {
        val sectionsArr = JSONArray().apply {
            archetype.sections.forEach { put(it) }
        }
        val fieldsArr = JSONArray().apply {
            archetype.fields.forEachIndexed { idx, f ->
                put(
                    JSONObject()
                        .put("name", f.name)
                        .put("type", f.type)
                        .put("required", f.required)
                        .put("position", idx)
                        .put("section", f.section)
                        .put("options", f.options)
                        .put("hint", f.hint)
                        .put("conditionField", f.conditionField)
                        .put("conditionValue", f.conditionValue)
                        .put("formula", f.formula)
                        .put("repeatable", f.repeatable)
                )
            }
        }
        return JSONObject()
            .put("format", "inkandquill-template")
            .put("version", 2)
            .put("title", archetype.title)
            .put("body", archetype.description)
            .put("category", archetype.category)
            .put("defaultKind", archetype.defaultKind)
            .put("sections", sectionsArr)
            .put("fields", fieldsArr)
            .toString(2)
    }

    fun importAsTemplateBlueprint(
        db: InkDatabase,
        archetype: TemplateLibraryArchetype
    ): InkRecord {
        return db.importTemplateJson(toImportableTemplateJson(archetype))
    }

    fun importAndCreateEntry(
        db: InkDatabase,
        archetype: TemplateLibraryArchetype,
        parentId: String = "",
        scope: String = "World",
        includeSampleValues: Boolean = false
    ): InkRecord {
        val tpl = findOrImportTemplate(db, archetype)
        val initialTitle = if (includeSampleValues) {
            archetype.sampleValues.values.firstOrNull()?.takeIf { it.isNotBlank() }.orEmpty()
        } else {
            ""
        }
        val created = db.createRecordFromTemplate(
            templateId = tpl.id,
            parentId = parentId,
            scope = scope,
            initialTitle = initialTitle
        )
        if (archetype.tags.isNotEmpty()) {
            db.save(created.copy(tags = archetype.tags.joinToString(", ")))
        }
        if (includeSampleValues && archetype.sampleValues.isNotEmpty()) {
            applySampleValues(db, created.id, tpl.id, archetype.sampleValues, overwriteExisting = false)
        }
        return db.get(created.id) ?: created
    }

    fun importIntoExistingSheet(
        db: InkDatabase,
        recordId: String,
        archetype: TemplateLibraryArchetype,
        mode: SheetImportMode,
        includeSampleValues: Boolean = false
    ): ImportSheetResult {
        val currentTplId = db.sheetTemplateId(recordId)
        val existingValues = db.sheetValues(recordId)
        val existingFieldById = if (currentTplId.isNotBlank()) {
            db.templateFields(currentTplId, includeRemoved = true).associateBy { it.id }
        } else {
            emptyMap()
        }
        val existingValuesByName = buildMap {
            existingValues.forEach { (fieldId, value) ->
                if (value.isNotBlank()) {
                    val name = existingFieldById[fieldId]?.name?.lowercase()
                    if (!name.isNullOrBlank()) put(name, value)
                }
            }
        }

        if (mode == SheetImportMode.ATTACH_NEW_SHEET || currentTplId.isBlank()) {
            val tpl = findOrImportTemplate(db, archetype)
            db.attachSheet(recordId, tpl.id)
            val newFields = db.templateFields(tpl.id)

            // Carry over matching named field values from previous sheet if any
            val carried = mutableMapOf<String, String>()
            newFields.forEach { field ->
                val prev = existingValuesByName[field.name.lowercase()]
                if (!prev.isNullOrBlank()) carried[field.id] = prev
            }
            if (carried.isNotEmpty()) {
                db.saveSheetValues(recordId, carried)
            }

            val prefilled = if (includeSampleValues) {
                applySampleValues(db, recordId, tpl.id, archetype.sampleValues, overwriteExisting = false)
            } else {
                0
            }
            return ImportSheetResult(
                templateId = tpl.id,
                fieldsAdded = newFields.size,
                valuesPrefilled = prefilled
            )
        }

        // Merge sections and fields into the record's current sheet template
        archetype.sections.forEach { sec ->
            db.addTemplateSection(currentTplId, sec)
        }
        val existingNames = db.templateFields(currentTplId, includeRemoved = true)
            .map { it.name.trim().lowercase() }
            .toMutableSet()

        var addedCount = 0
        archetype.fields.forEach { field ->
            val key = field.name.trim().lowercase()
            if (key.isNotBlank() && key !in existingNames) {
                db.addTemplateField(
                    templateId = currentTplId,
                    name = field.name,
                    type = field.type,
                    required = field.required,
                    section = field.section,
                    options = field.options,
                    hint = field.hint,
                    conditionField = field.conditionField,
                    conditionValue = field.conditionValue,
                    formula = field.formula,
                    repeatable = field.repeatable
                )
                existingNames.add(key)
                addedCount++
            }
        }

        val prefilled = if (includeSampleValues) {
            applySampleValues(db, recordId, currentTplId, archetype.sampleValues, overwriteExisting = false)
        } else {
            0
        }

        return ImportSheetResult(
            templateId = currentTplId,
            fieldsAdded = addedCount,
            valuesPrefilled = prefilled
        )
    }

    private fun findOrImportTemplate(
        db: InkDatabase,
        archetype: TemplateLibraryArchetype
    ): InkRecord {
        val existing = db.templates().firstOrNull {
            it.kind == "Template" && it.title.equals(archetype.title, ignoreCase = true)
        }
        if (existing != null && db.templateFields(existing.id).isNotEmpty()) {
            return existing
        }
        return importAsTemplateBlueprint(db, archetype)
    }

    private fun applySampleValues(
        db: InkDatabase,
        recordId: String,
        templateId: String,
        sampleValues: Map<String, String>,
        overwriteExisting: Boolean
    ): Int {
        if (sampleValues.isEmpty()) return 0
        val sampleByLower = sampleValues.mapKeys { it.key.trim().lowercase() }
        val current = db.sheetValues(recordId).toMutableMap()
        var filled = 0
        db.templateFields(templateId).forEach { field ->
            val sample = sampleByLower[field.name.trim().lowercase()]
            if (!sample.isNullOrBlank()) {
                val existingVal = current[field.id].orEmpty()
                if (overwriteExisting || existingVal.isBlank()) {
                    current[field.id] = sample
                    filled++
                }
            }
        }
        if (filled > 0) {
            db.saveSheetValues(recordId, current)
        }
        return filled
    }

    private fun resolveLibraryCategory(
        explicit: String,
        group: String,
        templateCategory: String,
        defaultKind: String
    ): String {
        if (explicit.isNotBlank()) return explicit
        val g = group.lowercase()
        val c = templateCategory.lowercase()
        val k = defaultKind.lowercase()
        return when {
            g in listOf("cities", "realms", "geography", "locations") ||
                k in listOf("settlement", "stronghold", "dungeon", "kingdom", "region", "realm", "location", "city") ||
                c.contains("location") || c.contains("realm") || c.contains("geography") -> "Geography"

            g in listOf("gods", "religions", "deities") ||
                k in listOf("deity", "myth", "religion", "god", "patron") ||
                c.contains("belief") || c.contains("myth") || c.contains("deit") || c.contains("relig") -> "Deities"

            g in listOf("cultures", "factions", "languages", "societies") ||
                k in listOf("culture", "faction", "language", "script", "society", "guild") ||
                c.contains("culture") || c.contains("societ") || c.contains("language") || c.contains("faction") -> "Societies"

            g in listOf("characters", "character") ||
                k in listOf("character", "npc", "hero", "villain") ||
                c.contains("character") || c.contains("lineage") -> "Characters"

            g in listOf("items", "item", "artifacts", "relics") ||
                k in listOf("artifact", "item", "weapon", "relic") ||
                c.contains("item") || c.contains("artifact") || c.contains("relic") -> "Items"

            g in listOf("magic & systems", "magic", "systems") ||
                k in listOf("magic system", "spell", "magic") ||
                c.contains("magic") || c.contains("system") -> "Magic"

            g in listOf("creatures", "flora", "ecology", "fauna") ||
                k in listOf("dragon", "monster", "fauna", "species", "flora", "herb") ||
                c.contains("fauna") || c.contains("flora") || c.contains("ecolog") || c.contains("monster") -> "Ecology"

            else -> "Custom"
        }
    }

    private fun inferGroup(category: String, defaultKind: String): String {
        return when {
            defaultKind in listOf("Settlement", "Stronghold", "Dungeon") -> "Cities"
            defaultKind in listOf("Deity", "Myth") -> "Gods"
            defaultKind in listOf("Culture") -> "Cultures"
            defaultKind in listOf("Character", "NPC") -> "Characters"
            defaultKind in listOf("Magic System", "Spell") -> "Magic & Systems"
            defaultKind in listOf("Religion") -> "Religions"
            defaultKind in listOf("Faction") -> "Factions"
            defaultKind in listOf("Artifact", "Item", "Weapon") -> "Items"
            defaultKind in listOf("Dragon", "Monster", "Fauna", "Species") -> "Creatures"
            defaultKind in listOf("Flora", "Herb") -> "Flora"
            defaultKind in listOf("Language", "Script") -> "Languages"
            defaultKind in listOf("Kingdom", "Region", "Realm") -> "Realms"
            category.contains("Culture", ignoreCase = true) -> "Cultures"
            category.contains("Belief", ignoreCase = true) -> "Gods"
            category.contains("Location", ignoreCase = true) -> "Cities"
            else -> "Custom"
        }
    }
}
