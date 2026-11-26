package com.parasaurpal.inkandquill.data

data class GlobalSearchResult(
    val record: InkRecord,
    val isTemplate: Boolean,
    val categoryGroup: String,
    val templateTitle: String,
    val matchedField: String,
    val snippet: String,
    val score: Int
)

object GlobalSearchEngine {

    val filterCategories = listOf(
        "All",
        "Characters",
        "Locations",
        "Items",
        "Flora & Fauna",
        "Lore & Systems",
        "Stories & Notes",
        "Templates"
    )

    private val CHARACTER_KINDS = setOf("Character", "NPC", "Dynasty", "Lineage", "Deity", "Faction")
    private val LOCATION_KINDS = setOf(
        "World", "Realm", "Location", "Region", "Kingdom", "Settlement",
        "Dungeon", "Stronghold", "Biome", "Celestial", "Map Reference"
    )
    private val ITEM_KINDS = setOf("Item", "Artifact", "Weapon", "Potion", "Material", "Vessel")
    private val FLORA_FAUNA_KINDS = setOf("Flora", "Herb", "Fungus", "Fauna", "Monster", "Species", "Dragon", "Creature")
    private val LORE_KINDS = setOf(
        "Language", "Script", "Culture", "Festival", "Economy", "Government", "Military",
        "Magic System", "Spell", "Technology", "Affliction", "Religion", "Myth", "Prophecy",
        "Calendar", "Timeline Event", "Organization"
    )

    fun categoryFor(docType: String, kind: String): String {
        if (docType == "template") return "Templates"
        val cleanKind = kind.trim()
        return when {
            cleanKind in CHARACTER_KINDS -> "Characters"
            cleanKind in LOCATION_KINDS -> "Locations"
            cleanKind in ITEM_KINDS -> "Items"
            cleanKind in FLORA_FAUNA_KINDS -> "Flora & Fauna"
            cleanKind in LORE_KINDS -> "Lore & Systems"
            else -> "Stories & Notes"
        }
    }

    fun buildFtsQuery(rawQuery: String): String {
        val tokens = Regex("[\\p{L}\\p{N}_]+")
            .findAll(rawQuery.trim())
            .map { it.value.trim() }
            .filter { it.isNotBlank() }
            .distinct()
            .toList()
        if (tokens.isEmpty()) return ""
        return tokens.joinToString(" ") { "$it*" }
    }

    fun search(
        db: InkDatabase,
        rawQuery: String,
        categoryFilter: String = "All",
        limit: Int = 120
    ): List<GlobalSearchResult> {
        val trimmed = rawQuery.trim()
        val ftsQuery = buildFtsQuery(trimmed)
        val rows = when {
            trimmed.isBlank() -> db.allIndexedRows(limit = 250)
            ftsQuery.isNotBlank() -> {
                val indexed = db.indexedSearchRows(ftsQuery, limit = 250)
                if (indexed.isNotEmpty()) indexed else fallbackScan(db, trimmed)
            }
            else -> fallbackScan(db, trimmed)
        }

        val tokens = Regex("[\\p{L}\\p{N}_]+")
            .findAll(trimmed.lowercase())
            .map { it.value }
            .filter { it.isNotBlank() }
            .toList()
            .ifEmpty { if (trimmed.isNotBlank()) listOf(trimmed.lowercase()) else emptyList() }

        val results = rows.mapNotNull { row ->
            val group = categoryFor(row.docType, row.kind)
            if (categoryFilter != "All" && group != categoryFilter) return@mapNotNull null

            val record = db.get(row.docId) ?: return@mapNotNull null
            if (record.kind == "Archived Template") return@mapNotNull null

            if (tokens.isNotEmpty()) {
                val matchesContent = tokens.all { t ->
                    row.title.contains(t, true) ||
                        row.aliases.contains(t, true) ||
                        row.tags.contains(t, true) ||
                        row.kind.contains(t, true) ||
                        row.templateTitle.contains(t, true) ||
                        row.sheetContent.contains(t, true) ||
                        row.proseContent.contains(t, true)
                }
                if (!matchesContent) return@mapNotNull null
            }

            val (matchedField, snippet, score) = analyzeMatch(
                row = row,
                record = record,
                rawQuery = trimmed,
                tokens = tokens
            )

            GlobalSearchResult(
                record = record,
                isTemplate = row.docType == "template",
                categoryGroup = group,
                templateTitle = row.templateTitle,
                matchedField = matchedField,
                snippet = snippet,
                score = score
            )
        }

        return if (trimmed.isBlank()) {
            results.sortedWith(
                compareBy<GlobalSearchResult> { it.isTemplate }
                    .thenByDescending { it.record.favorite }
                    .thenByDescending { it.record.updatedAt }
            ).take(limit)
        } else {
            results.sortedWith(
                compareByDescending<GlobalSearchResult> { it.score }
                    .thenBy { it.isTemplate }
                    .thenByDescending { it.record.updatedAt }
            ).take(limit)
        }
    }

    private fun fallbackScan(db: InkDatabase, query: String): List<IndexedSearchRow> {
        return db.allIndexedRows(limit = 400).filter { row ->
            row.title.contains(query, true) ||
                row.aliases.contains(query, true) ||
                row.tags.contains(query, true) ||
                row.kind.contains(query, true) ||
                row.templateTitle.contains(query, true) ||
                row.sheetContent.contains(query, true) ||
                row.proseContent.contains(query, true)
        }
    }

    private fun analyzeMatch(
        row: IndexedSearchRow,
        record: InkRecord,
        rawQuery: String,
        tokens: List<String>
    ): Triple<String, String, Int> {
        if (tokens.isEmpty()) {
            val preview = row.sheetContent.lines().firstOrNull { it.isNotBlank() }
                ?: row.proseContent.take(120)
            return Triple(
                if (row.docType == "template") "Template Blueprint" else row.kind,
                preview.take(120),
                if (record.favorite) 10 else 0
            )
        }

        val qLower = rawQuery.lowercase()
        var score = if (record.favorite) 5 else 0
        if (row.docType == "entry") score += 15

        if (row.title.equals(rawQuery, ignoreCase = true)) {
            score += 200
            return Triple("Title", row.title, score)
        }
        if (row.title.contains(qLower, ignoreCase = true) || tokens.all { row.title.contains(it, true) }) {
            score += if (row.title.lowercase().startsWith(qLower)) 160 else 130
            return Triple("Title", extractExcerpt(row.sheetContent.ifBlank { row.proseContent }, tokens) ?: row.title, score)
        }
        if (tokens.any { row.aliases.contains(it, true) }) {
            score += 105
            return Triple("Alias", row.aliases, score)
        }
        if (tokens.any { row.tags.contains(it, true) || row.kind.contains(it, true) }) {
            score += 85
            val tagSummary = listOf(row.kind, row.tags).filter { it.isNotBlank() }.joinToString(" · ")
            return Triple("Kind / Tags", tagSummary, score)
        }

        val matchingSheetLine = row.sheetContent.lines().firstOrNull { line ->
            tokens.any { line.contains(it, true) }
        }
        if (matchingSheetLine != null) {
            score += 75
            val fieldLabel = matchingSheetLine.substringBefore(":", "Sheet").trim()
            val fieldValue = matchingSheetLine.substringAfter(":", matchingSheetLine).trim()
            val excerpt = extractExcerpt(fieldValue, tokens) ?: fieldValue.take(120)
            return Triple("Sheet · $fieldLabel", excerpt, score)
        }

        if (tokens.any { row.templateTitle.contains(it, true) }) {
            score += 65
            return Triple("Template", row.templateTitle, score)
        }

        val proseExcerpt = extractExcerpt(row.proseContent, tokens) ?: row.proseContent.take(120)
        score += 45
        return Triple("Prose", proseExcerpt, score)
    }

    private fun extractExcerpt(text: String, tokens: List<String>, radius: Int = 55): String? {
        if (text.isBlank()) return null
        val lower = text.lowercase()
        val hitIdx = tokens.mapNotNull { t ->
            val idx = lower.indexOf(t)
            if (idx >= 0) idx else null
        }.minOrNull() ?: return text.take(120)

        val start = (hitIdx - radius).coerceAtLeast(0)
        val end = (hitIdx + radius + 30).coerceAtMost(text.length)
        val prefix = if (start > 0) "…" else ""
        val suffix = if (end < text.length) "…" else ""
        return (prefix + text.substring(start, end).replace('\n', ' ').trim() + suffix)
    }
}
