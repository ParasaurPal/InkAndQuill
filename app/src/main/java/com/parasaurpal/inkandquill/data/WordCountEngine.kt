package com.parasaurpal.inkandquill.data

import kotlin.math.ceil

data class DocumentWordStats(
    val proseWords: Int,
    val sheetWords: Int,
    val totalWords: Int,
    val charactersWithSpaces: Int,
    val charactersNoSpaces: Int,
    val paragraphs: Int,
    val readingMinutes: Int
)

data class TierWordCount(
    val tierLabel: String,
    val record: InkRecord?,
    val title: String,
    val wordCount: Int,
    val ownWords: Int,
    val childCount: Int,
    val goalWords: Int = 0,
    val storyWords: Int = 0,
    val loreWords: Int = 0
) {
    val progress: Float
        get() = if (goalWords <= 0) 0f else (wordCount.toFloat() / goalWords.toFloat()).coerceIn(0f, 1f)
}

data class ChildWordEntry(
    val record: InkRecord,
    val subtreeWords: Int,
    val ownWords: Int,
    val goalWords: Int
)

data class HierarchyWordReport(
    val currentRecord: InkRecord,
    val currentStats: DocumentWordStats,
    val currentGoal: Int,
    val sceneTier: TierWordCount?,
    val chapterTier: TierWordCount?,
    val bookTier: TierWordCount?,
    val seriesTier: TierWordCount?,
    val worldTier: TierWordCount?,
    val allWorldsTotalWords: Int,
    val childBreakdowns: List<ChildWordEntry>,
    val worldSummaryList: List<TierWordCount>
)

class WorldWordSnapshot(
    val recordsById: Map<String, InkRecord>,
    val childrenByParent: Map<String, List<InkRecord>>,
    val ownWordsById: Map<String, Int>,
    val proseWordsById: Map<String, Int>,
    val sheetWordsById: Map<String, Int>,
    val goalsById: Map<String, Int>,
    val allRecords: List<InkRecord>
) {
    fun subtreeWords(
        rootId: String,
        liveRecordId: String? = null,
        liveTotalWords: Int? = null
    ): Int {
        var total = 0
        val visited = HashSet<String>()
        val stack = ArrayDeque<String>()
        stack.addLast(rootId)
        while (stack.isNotEmpty()) {
            val id = stack.removeLast()
            if (!visited.add(id)) continue
            total += if (id == liveRecordId && liveTotalWords != null) {
                liveTotalWords
            } else {
                ownWordsById[id] ?: 0
            }
            childrenByParent[id].orEmpty().forEach { child ->
                stack.addLast(child.id)
            }
        }
        return total
    }

    fun subtreeStoryAndLoreWords(
        rootId: String,
        liveRecordId: String? = null,
        liveTotalWords: Int? = null
    ): Pair<Int, Int> {
        var story = 0
        var lore = 0
        val visited = HashSet<String>()
        val stack = ArrayDeque<String>()
        stack.addLast(rootId)
        while (stack.isNotEmpty()) {
            val id = stack.removeLast()
            if (!visited.add(id)) continue
            val rec = recordsById[id]
            val words = if (id == liveRecordId && liveTotalWords != null) liveTotalWords else (ownWordsById[id] ?: 0)
            if (rec != null && rec.kind in WordCountEngine.STORY_KINDS) {
                story += words
            } else {
                lore += words
            }
            childrenByParent[id].orEmpty().forEach { child ->
                stack.addLast(child.id)
            }
        }
        return story to lore
    }
}

object WordCountEngine {
    val STORY_KINDS = setOf("Series", "Book", "Novel", "Story", "Chapter", "Scene", "Poem", "Document", "Journal")
    private val BOOK_KINDS = setOf("Book", "Novel", "Story")
    private val NON_TEXT_SHEET_TYPES = setOf("Image", "Gallery", "Attachment", "Color", "Relationship", "Checkbox", "Rating")

    private val SCRIPT_OR_STYLE_REGEX = Regex("<(script|style)[^>]*>[\\s\\S]*?</\\1>", RegexOption.IGNORE_CASE)
    private val BLOCK_TAG_REGEX = Regex("</?(p|div|br|h[1-6]|li|tr|blockquote|pre|hr)[^>]*>", RegexOption.IGNORE_CASE)
    private val ANY_TAG_REGEX = Regex("<[^>]+>")
    private val DATA_URI_REGEX = Regex("data:[^\\s\"'<>]+")

    fun htmlToPlainFast(html: String): String {
        if (html.isBlank()) return ""
        val withoutBlocks = html
            .replace(SCRIPT_OR_STYLE_REGEX, " ")
            .replace(DATA_URI_REGEX, " ")
            .replace(BLOCK_TAG_REGEX, "\n")
            .replace(ANY_TAG_REGEX, " ")
            .replace("&nbsp;", " ")
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
            .replace("&mdash;", " — ")
            .replace("&ndash;", " – ")
        return withoutBlocks
    }

    fun countWordsInPlain(text: String): Int {
        if (text.isBlank()) return 0
        var count = 0
        var inWord = false
        for (i in text.indices) {
            val ch = text[i]
            val isWordChar = ch.isLetterOrDigit() ||
                (inWord && (ch == '\'' || ch == '’' || ch == '-') && i + 1 < text.length && text[i + 1].isLetterOrDigit())
            if (isWordChar) {
                if (!inWord) {
                    count++
                    inWord = true
                }
            } else {
                inWord = false
            }
        }
        return count
    }

    fun countSheetWords(
        fields: List<TemplateField>,
        values: Map<String, String>
    ): Int {
        if (values.isEmpty()) return 0
        if (fields.isEmpty()) {
            return values.values.sumOf { raw ->
                if (raw.startsWith("data:") || raw.startsWith("content://")) 0
                else countWordsInPlain(htmlToPlainFast(raw))
            }
        }
        val fieldById = fields.associateBy { it.id }
        var total = 0
        values.forEach { (fieldId, raw) ->
            if (raw.isBlank()) return@forEach
            val field = fieldById[fieldId]
            if (field != null && field.fieldType in NON_TEXT_SHEET_TYPES) return@forEach
            if (raw.startsWith("data:") || raw.startsWith("content://")) return@forEach
            val cleaned = when (field?.fieldType) {
                "Rich text" -> htmlToPlainFast(raw)
                "Table", "Repeatable group" -> raw.replace("|", " ").replace("||", " ").replace("::", " ")
                else -> htmlToPlainFast(raw)
            }
            total += countWordsInPlain(cleaned)
        }
        return total
    }

    fun analyzeDocument(
        html: String,
        fields: List<TemplateField> = emptyList(),
        sheetValues: Map<String, String> = emptyMap()
    ): DocumentWordStats {
        val plain = htmlToPlainFast(html)
        val proseWords = countWordsInPlain(plain)
        val sheetWords = countSheetWords(fields, sheetValues)
        val totalWords = proseWords + sheetWords
        val normalized = plain.trim()
        val charsWithSpaces = normalized.length
        val charsNoSpaces = normalized.count { !it.isWhitespace() }
        val paragraphs = if (normalized.isEmpty()) 0 else normalized.lines().count { it.isNotBlank() }
        val readingMinutes = if (totalWords <= 0) 0 else ceil(totalWords / 225.0).toInt().coerceAtLeast(1)
        return DocumentWordStats(
            proseWords = proseWords,
            sheetWords = sheetWords,
            totalWords = totalWords,
            charactersWithSpaces = charsWithSpaces,
            charactersNoSpaces = charsNoSpaces,
            paragraphs = paragraphs,
            readingMinutes = readingMinutes
        )
    }

    fun buildSnapshot(db: InkDatabase): WorldWordSnapshot {
        val records = db.all()
        val recordsById = records.associateBy { it.id }
        val childrenByParent = records.groupBy { it.parentId }
        val templateIdsByRecord = HashMap<String, String>()
        val valuesByRecord = HashMap<String, Map<String, String>>()
        val fieldsByTemplate = HashMap<String, List<TemplateField>>()

        db.readableDatabase.rawQuery("SELECT recordId, templateId FROM record_sheets", null).use { c ->
            while (c.moveToNext()) {
                templateIdsByRecord[c.getString(0)] = c.getString(1)
            }
        }
        val rawSheetValues = HashMap<String, MutableMap<String, String>>()
        db.readableDatabase.rawQuery("SELECT recordId, fieldId, value FROM record_sheet_values", null).use { c ->
            while (c.moveToNext()) {
                val rId = c.getString(0)
                val fId = c.getString(1)
                val v = c.getString(2)
                rawSheetValues.getOrPut(rId) { HashMap() }[fId] = v
            }
        }
        rawSheetValues.forEach { (rId, map) -> valuesByRecord[rId] = map }

        val goalsById = HashMap<String, Int>()
        db.readableDatabase.rawQuery("SELECT key, value FROM settings WHERE key LIKE 'wordGoal_%'", null).use { c ->
            while (c.moveToNext()) {
                val id = c.getString(0).removePrefix("wordGoal_")
                val goal = c.getString(1).toIntOrNull() ?: 0
                if (goal > 0) goalsById[id] = goal
            }
        }

        val proseWordsById = HashMap<String, Int>(records.size)
        val sheetWordsById = HashMap<String, Int>(records.size)
        val ownWordsById = HashMap<String, Int>(records.size)

        records.forEach { rec ->
            val prose = countWordsInPlain(htmlToPlainFast(rec.body))
            val tplId = templateIdsByRecord[rec.id].orEmpty()
            val vals = valuesByRecord[rec.id].orEmpty()
            val fields = if (tplId.isNotBlank()) {
                fieldsByTemplate.getOrPut(tplId) { db.templateFields(tplId) }
            } else emptyList()
            val sheet = countSheetWords(fields, vals)
            proseWordsById[rec.id] = prose
            sheetWordsById[rec.id] = sheet
            ownWordsById[rec.id] = prose + sheet
        }

        return WorldWordSnapshot(
            recordsById = recordsById,
            childrenByParent = childrenByParent,
            ownWordsById = ownWordsById,
            proseWordsById = proseWordsById,
            sheetWordsById = sheetWordsById,
            goalsById = goalsById,
            allRecords = records
        )
    }

    fun buildReport(
        snapshot: WorldWordSnapshot,
        record: InkRecord,
        liveHtml: String,
        liveFields: List<TemplateField> = emptyList(),
        liveSheetValues: Map<String, String> = emptyMap(),
        liveParentId: String = record.parentId
    ): HierarchyWordReport {
        val currentStats = analyzeDocument(liveHtml, liveFields, liveSheetValues)
        val liveTotal = currentStats.totalWords

        // Build ancestor chain respecting unsaved liveParentId for the current record
        val ancestors = mutableListOf<InkRecord>()
        val seen = mutableSetOf(record.id)
        var cursorParentId = liveParentId
        while (cursorParentId.isNotBlank() && seen.add(cursorParentId)) {
            val parent = snapshot.recordsById[cursorParentId] ?: break
            ancestors.add(parent)
            cursorParentId = parent.parentId
        }

        fun findTierRecord(predicate: (InkRecord) -> Boolean): InkRecord? {
            if (predicate(record)) return record
            return ancestors.firstOrNull(predicate)
        }

        fun buildTier(label: String, rec: InkRecord?): TierWordCount? {
            if (rec == null) return null
            val total = snapshot.subtreeWords(rec.id, record.id, liveTotal)
            val own = if (rec.id == record.id) liveTotal else (snapshot.ownWordsById[rec.id] ?: 0)
            val children = snapshot.childrenByParent[rec.id].orEmpty().size
            val goal = snapshot.goalsById[rec.id] ?: 0
            val (storyW, loreW) = if (rec.kind == "World") {
                snapshot.subtreeStoryAndLoreWords(rec.id, record.id, liveTotal)
            } else {
                total to 0
            }
            return TierWordCount(
                tierLabel = label,
                record = rec,
                title = rec.title.ifBlank { "Untitled $label" },
                wordCount = total,
                ownWords = own,
                childCount = children,
                goalWords = goal,
                storyWords = storyW,
                loreWords = loreW
            )
        }

        val sceneRec = findTierRecord { it.kind == "Scene" }
        val chapterRec = findTierRecord { it.kind == "Chapter" }
        val bookRec = findTierRecord { it.kind in BOOK_KINDS }
        val seriesRec = findTierRecord { it.kind == "Series" }
        val worldRec = findTierRecord { it.kind == "World" }

        val worlds = snapshot.allRecords.filter { it.kind == "World" }
        val worldSummaryList = worlds.map { w ->
            val total = snapshot.subtreeWords(w.id, record.id, liveTotal)
            val (storyW, loreW) = snapshot.subtreeStoryAndLoreWords(w.id, record.id, liveTotal)
            TierWordCount(
                tierLabel = "World",
                record = w,
                title = w.title.ifBlank { "Untitled World" },
                wordCount = total,
                ownWords = if (w.id == record.id) liveTotal else (snapshot.ownWordsById[w.id] ?: 0),
                childCount = snapshot.childrenByParent[w.id].orEmpty().size,
                goalWords = snapshot.goalsById[w.id] ?: 0,
                storyWords = storyW,
                loreWords = loreW
            )
        }

        val allTotal = snapshot.allRecords.sumOf { r ->
            if (r.id == record.id) liveTotal else (snapshot.ownWordsById[r.id] ?: 0)
        } + if (snapshot.recordsById.containsKey(record.id)) 0 else liveTotal

        val effectiveWorldTier = buildTier("World", worldRec) ?: TierWordCount(
            tierLabel = "Overall World",
            record = null,
            title = if (worlds.size == 1) worlds.first().title else "All Worlds & Library",
            wordCount = allTotal,
            ownWords = liveTotal,
            childCount = worlds.size,
            goalWords = 0,
            storyWords = snapshot.allRecords.filter { it.kind in STORY_KINDS }.sumOf {
                if (it.id == record.id) liveTotal else (snapshot.ownWordsById[it.id] ?: 0)
            },
            loreWords = snapshot.allRecords.filter { it.kind !in STORY_KINDS }.sumOf {
                if (it.id == record.id) liveTotal else (snapshot.ownWordsById[it.id] ?: 0)
            }
        )

        val directChildren = snapshot.childrenByParent[record.id].orEmpty()
            .sortedWith(compareBy<InkRecord>({ it.createdAt }, { it.title.lowercase() }))
            .map { child ->
                ChildWordEntry(
                    record = child,
                    subtreeWords = snapshot.subtreeWords(child.id, record.id, liveTotal),
                    ownWords = snapshot.ownWordsById[child.id] ?: 0,
                    goalWords = snapshot.goalsById[child.id] ?: 0
                )
            }

        return HierarchyWordReport(
            currentRecord = record,
            currentStats = currentStats,
            currentGoal = snapshot.goalsById[record.id] ?: 0,
            sceneTier = buildTier("Scene", sceneRec),
            chapterTier = buildTier("Chapter", chapterRec),
            bookTier = buildTier("Book", bookRec),
            seriesTier = buildTier("Series", seriesRec),
            worldTier = effectiveWorldTier,
            allWorldsTotalWords = allTotal,
            childBreakdowns = directChildren,
            worldSummaryList = worldSummaryList
        )
    }

    fun formatCompact(words: Int): String = when {
        words >= 1_000_000 -> String.format("%.1fM", words / 1_000_000f)
        words >= 10_000 -> String.format("%.1fk", words / 1_000f)
        else -> "%,d".format(words)
    }
}
