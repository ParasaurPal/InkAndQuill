package com.parasaurpal.inkandquill.data

import android.net.Uri
import android.text.TextUtils
import org.json.JSONArray
import org.json.JSONObject

data class SheetTableData(
    val columns: List<String>,
    val rows: List<List<String>>
)

data class RepeatableEntry(
    val label: String,
    val detail: String = ""
)

object SheetEngine {

    fun orderedFields(
        fields: List<TemplateField>,
        sectionOrder: List<String>
    ): List<TemplateField> {
        return fields.withIndex().sortedWith(
            compareBy<IndexedValue<TemplateField>> {
                val idx = sectionOrder.indexOf(it.value.section)
                if (idx < 0) Int.MAX_VALUE else idx
            }.thenBy { it.index }
        ).map { it.value }
    }

    fun isFieldVisible(
        field: TemplateField,
        allFields: List<TemplateField>,
        values: Map<String, String>
    ): Boolean {
        val triggerName = field.conditionField.trim()
        if (triggerName.isBlank()) return true
        val triggerField = allFields.firstOrNull { it.name.equals(triggerName, ignoreCase = true) }
            ?: return true
        val rawValue = values[triggerField.id].orEmpty().trim()
        val rule = field.conditionValue.trim()
        return when {
            rule.equals("not_empty", ignoreCase = true) || rule == "!not_empty" || rule == "*" ->
                rawValue.isNotBlank()
            rule.startsWith("!=") ->
                !rawValue.equals(rule.drop(2).trim(), ignoreCase = true)
            rule.startsWith("contains:", ignoreCase = true) ->
                rawValue.contains(rule.substringAfter(":").trim(), ignoreCase = true)
            rule.startsWith("~") ->
                rawValue.contains(rule.drop(1).trim(), ignoreCase = true)
            else -> {
                if (rawValue.equals(rule, ignoreCase = true)) true
                else rawValue.split("|").any { it.trim().equals(rule, ignoreCase = true) }
            }
        }
    }

    fun evaluateFormula(
        field: TemplateField,
        allFields: List<TemplateField>,
        values: Map<String, String>
    ): String {
        val formula = field.formula.trim()
        if (formula.isBlank()) return ""
        val fnMatch = Regex("^(sum|avg|min|max|concat|uppercase|lowercase|count)\\((.*)\\)$", RegexOption.IGNORE_CASE)
            .matchEntire(formula)
        if (fnMatch != null) {
            val fn = fnMatch.groupValues[1].lowercase()
            val args = fnMatch.groupValues[2].split(",").map { it.trim() }.filter { it.isNotBlank() }
            val resolved = args.map { ref -> resolveRef(ref, allFields, values) }
            return when (fn) {
                "sum" -> formatNumber(resolved.mapNotNull { it.toDoubleOrNull() }.sum())
                "avg" -> {
                    val nums = resolved.mapNotNull { it.toDoubleOrNull() }
                    if (nums.isEmpty()) "0" else formatNumber(nums.average())
                }
                "min" -> formatNumber(resolved.mapNotNull { it.toDoubleOrNull() }.minOrNull() ?: 0.0)
                "max" -> formatNumber(resolved.mapNotNull { it.toDoubleOrNull() }.maxOrNull() ?: 0.0)
                "concat" -> resolved.joinToString(" ") { it.trim() }.trim()
                "uppercase" -> resolved.firstOrNull().orEmpty().uppercase()
                "lowercase" -> resolved.firstOrNull().orEmpty().lowercase()
                "count" -> {
                    val refField = args.firstOrNull()?.let { name ->
                        allFields.firstOrNull { it.name.equals(name, ignoreCase = true) }
                    }
                    val firstVal = resolved.firstOrNull().orEmpty()
                    when (refField?.fieldType) {
                        "Repeatable group" -> parseRepeatable(firstVal).size.toString()
                        "Table" -> parseTable(firstVal, refField.hint).rows.size.toString()
                        "Multi-select" -> firstVal.split("|").count { it.isNotBlank() }.toString()
                        else -> firstVal.trim().split(Regex("\\s+")).count { it.isNotBlank() }.toString()
                    }
                }
                else -> ""
            }
        }

        val arithMatch = Regex("^(.+?)\\s*([+\\-*/])\\s*(.+)$").matchEntire(formula)
        if (arithMatch != null) {
            val left = resolveRef(arithMatch.groupValues[1].trim(), allFields, values)
            val op = arithMatch.groupValues[2]
            val right = resolveRef(arithMatch.groupValues[3].trim(), allFields, values)
            val a = left.toDoubleOrNull()
            val b = right.toDoubleOrNull()
            if (a != null && b != null) {
                val res = when (op) {
                    "+" -> a + b
                    "-" -> a - b
                    "*" -> a * b
                    "/" -> if (b != 0.0) a / b else 0.0
                    else -> 0.0
                }
                return formatNumber(res)
            }
            if (op == "+") return listOf(left, right).filter { it.isNotBlank() }.joinToString(" ")
        }

        val refs = formula.split("+").map { it.trim() }
        val resolved = refs.map { resolveRef(it, allFields, values) }
        return if (resolved.all { it.toDoubleOrNull() != null }) {
            formatNumber(resolved.sumOf { it.toDouble() })
        } else {
            resolved.filter { it.isNotBlank() }.joinToString(" ")
        }
    }

    private fun resolveRef(
        ref: String,
        allFields: List<TemplateField>,
        values: Map<String, String>
    ): String {
        val matchedField = allFields.firstOrNull { it.name.equals(ref, ignoreCase = true) }
        return if (matchedField != null) {
            values[matchedField.id].orEmpty()
        } else {
            ref.trim().removeSurrounding("\"").removeSurrounding("'")
        }
    }

    private fun formatNumber(value: Double): String {
        return if (value % 1.0 == 0.0) value.toLong().toString()
        else String.format(java.util.Locale.US, "%.2f", value).trimEnd('0').trimEnd('.')
    }

    fun parseTable(raw: String, hint: String = ""): SheetTableData {
        val trimmed = raw.trim()
        if (trimmed.startsWith("{")) {
            runCatching {
                val obj = JSONObject(trimmed)
                val colsArr = obj.optJSONArray("cols") ?: JSONArray()
                val cols = (0 until colsArr.length()).map { colsArr.optString(it) }
                    .ifEmpty { defaultColumnsFromHint(hint) }
                val rowsArr = obj.optJSONArray("rows") ?: JSONArray()
                val rows = (0 until rowsArr.length()).map { r ->
                    val rowArr = rowsArr.optJSONArray(r) ?: JSONArray()
                    List(cols.size) { c -> rowArr.optString(c, "") }
                }
                return SheetTableData(cols, rows)
            }
        }
        if (trimmed.isNotBlank()) {
            val lines = trimmed.lines().filter { it.isNotBlank() }
            if (lines.isNotEmpty()) {
                val parsed = lines.map { line -> line.split("|").map { it.trim() } }
                val maxCols = maxOf(parsed.maxOf { it.size }, defaultColumnsFromHint(hint).size)
                val hintCols = defaultColumnsFromHint(hint)
                val cols = List(maxCols) { idx ->
                    hintCols.getOrNull(idx) ?: "Column ${idx + 1}"
                }
                val rows = parsed.map { r -> List(maxCols) { idx -> r.getOrNull(idx).orEmpty() } }
                return SheetTableData(cols, rows)
            }
        }
        return SheetTableData(defaultColumnsFromHint(hint), emptyList())
    }

    private fun defaultColumnsFromHint(hint: String): List<String> {
        val fromHint = if ("|" in hint) {
            hint.split("|").map { it.trim() }.filter { it.isNotBlank() }
        } else emptyList()
        return fromHint.ifEmpty { listOf("Name", "Detail", "Notes") }
    }

    fun serializeTable(data: SheetTableData): String {
        if (data.rows.isEmpty() && data.columns.isEmpty()) return ""
        val cols = JSONArray()
        data.columns.forEach { cols.put(it) }
        val rows = JSONArray()
        data.rows.forEach { row ->
            val r = JSONArray()
            for (i in data.columns.indices) {
                r.put(row.getOrNull(i).orEmpty())
            }
            rows.put(r)
        }
        return JSONObject().put("cols", cols).put("rows", rows).toString()
    }

    fun parseRepeatable(raw: String): List<RepeatableEntry> {
        val trimmed = raw.trim()
        if (trimmed.isBlank()) return emptyList()
        if (trimmed.startsWith("[")) {
            runCatching {
                val arr = JSONArray(trimmed)
                return (0 until arr.length()).mapNotNull { i ->
                    val item = arr.optJSONObject(i)
                    if (item != null) {
                        val label = item.optString("label", "")
                        val detail = item.optString("detail", "")
                        if (label.isNotBlank() || detail.isNotBlank()) RepeatableEntry(label, detail) else null
                    } else {
                        val str = arr.optString(i, "")
                        if (str.isNotBlank()) RepeatableEntry(str) else null
                    }
                }
            }
        }
        return trimmed.lines()
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .map { line ->
                if (" — " in line) {
                    val parts = line.split(" — ", limit = 2)
                    RepeatableEntry(parts[0].trim(), parts[1].trim())
                } else {
                    RepeatableEntry(line)
                }
            }
    }

    fun serializeRepeatable(entries: List<RepeatableEntry>): String {
        val cleaned = entries.filter { it.label.isNotBlank() || it.detail.isNotBlank() }
        if (cleaned.isEmpty()) return ""
        val arr = JSONArray()
        cleaned.forEach { entry ->
            arr.put(JSONObject().put("label", entry.label).put("detail", entry.detail))
        }
        return arr.toString()
    }

    fun parseRelationshipIds(raw: String): List<String> {
        return raw.split("|", ",").map { it.trim() }.filter { it.isNotBlank() }.distinct()
    }

    fun formatFieldPlainValue(
        field: TemplateField,
        allFields: List<TemplateField>,
        values: Map<String, String>,
        recordResolver: (String) -> InkRecord?
    ): String {
        val raw = values[field.id].orEmpty()
        return when (field.fieldType) {
            "Calculated" -> evaluateFormula(field, allFields, values)
            "Checkbox" -> if (raw == "true") "Yes" else if (raw == "false") "No" else ""
            "Multi-select" -> raw.split("|").map { it.trim() }.filter { it.isNotBlank() }.joinToString(", ")
            "Relationship" -> parseRelationshipIds(raw).mapNotNull { recordResolver(it)?.title }.joinToString(", ")
            "Table" -> {
                val t = parseTable(raw, field.hint)
                if (t.rows.isEmpty()) "" else t.rows.joinToString("\n") { row ->
                    t.columns.indices.joinToString(" | ") { idx -> "${t.columns[idx]}: ${row.getOrNull(idx).orEmpty()}" }
                }
            }
            "Repeatable group" -> parseRepeatable(raw).joinToString("\n") {
                if (it.detail.isBlank()) "• ${it.label}" else "• ${it.label}: ${it.detail}"
            }
            "Rich text" -> DocumentExport.plain(raw).trim()
            "Image" -> if (raw.isNotBlank()) "[Image]" else ""
            "Gallery" -> {
                val count = raw.split("||").count { it.isNotBlank() }
                if (count > 0) "[$count image(s)]" else ""
            }
            "Attachment" -> if (raw.isNotBlank()) Uri.parse(raw).lastPathSegment ?: "[Attachment]" else ""
            "Rating" -> if (raw.isNotBlank()) "$raw / 5" else ""
            else -> raw.trim()
        }
    }

    fun compileToPlainText(
        fields: List<TemplateField>,
        sectionOrder: List<String>,
        values: Map<String, String>,
        recordResolver: (String) -> InkRecord?
    ): String {
        val ordered = orderedFields(fields, sectionOrder).filter { isFieldVisible(it, fields, values) }
        val grouped = ordered.groupBy { it.section.ifBlank { "Details" } }
        return buildString {
            grouped.forEach { (section, sectionFields) ->
                val filled = sectionFields.mapNotNull { f ->
                    val text = formatFieldPlainValue(f, fields, values, recordResolver)
                    if (text.isBlank()) null else "${f.name}: $text"
                }
                if (filled.isNotEmpty()) {
                    append("## ").append(section).append('\n')
                    filled.forEach { append(it).append('\n') }
                    append('\n')
                }
            }
        }.trim()
    }

    fun compileToMarkdown(
        title: String,
        fields: List<TemplateField>,
        sectionOrder: List<String>,
        values: Map<String, String>,
        proseHtml: String,
        recordResolver: (String) -> InkRecord?
    ): String {
        val ordered = orderedFields(fields, sectionOrder).filter { isFieldVisible(it, fields, values) }
        val grouped = ordered.groupBy { it.section.ifBlank { "Details" } }
        return buildString {
            if (title.isNotBlank()) append("# ").append(title).append("\n\n")
            grouped.forEach { (section, sectionFields) ->
                val entries = sectionFields.mapNotNull { f ->
                    val plain = formatFieldPlainValue(f, fields, values, recordResolver)
                    if (plain.isBlank()) null else f to plain
                }
                if (entries.isNotEmpty()) {
                    append("## ").append(section).append("\n\n")
                    entries.forEach { (f, text) ->
                        if (f.fieldType == "Table") {
                            val t = parseTable(values[f.id].orEmpty(), f.hint)
                            append("### ").append(f.name).append("\n\n")
                            append("| ").append(t.columns.joinToString(" | ")).append(" |\n")
                            append("| ").append(t.columns.joinToString(" | ") { "---" }).append(" |\n")
                            t.rows.forEach { row ->
                                append("| ").append(t.columns.indices.joinToString(" | ") { row.getOrNull(it).orEmpty() }).append(" |\n")
                            }
                            append('\n')
                        } else {
                            append("**").append(f.name).append(":** ").append(text).append("\n\n")
                        }
                    }
                }
            }
            val proseMd = DocumentExport.markdown(proseHtml)
            if (proseMd.isNotBlank()) {
                append("## Notes & Prose\n\n").append(proseMd)
            }
        }.trim()
    }

    fun compileToHtml(
        fields: List<TemplateField>,
        sectionOrder: List<String>,
        values: Map<String, String>,
        proseHtml: String,
        imageDataResolver: ((String) -> String?)? = null,
        recordResolver: (String) -> InkRecord?
    ): String {
        val ordered = orderedFields(fields, sectionOrder).filter { isFieldVisible(it, fields, values) }
        val grouped = ordered.groupBy { it.section.ifBlank { "Details" } }
        val sheetHtml = buildString {
            grouped.forEach { (section, sectionFields) ->
                val itemsHtml = buildString {
                    sectionFields.forEach { f ->
                        val raw = values[f.id].orEmpty()
                        when (f.fieldType) {
                            "Image" -> {
                                val resolved = if (raw.startsWith("data:")) raw else imageDataResolver?.invoke(raw)
                                if (!resolved.isNullOrBlank()) {
                                    append("<p><strong>").append(TextUtils.htmlEncode(f.name)).append(":</strong><br/>")
                                    append("<img src=\"").append(resolved).append("\" style=\"max-width:100%;height:auto;border-radius:8px;margin:4px 0;\"/></p>")
                                }
                            }
                            "Gallery" -> {
                                val imgs = raw.split("||")
                                    .filter { it.isNotBlank() }
                                    .mapNotNull { item -> if (item.startsWith("data:")) item else imageDataResolver?.invoke(item) }
                                if (imgs.isNotEmpty()) {
                                    append("<p><strong>").append(TextUtils.htmlEncode(f.name)).append(":</strong><br/>")
                                    imgs.forEach { img ->
                                        append("<img src=\"").append(img).append("\" style=\"max-width:48%;height:auto;border-radius:8px;margin:4px;\"/>")
                                    }
                                    append("</p>")
                                }
                            }
                            "Relationship" -> {
                                val links = parseRelationshipIds(raw).mapNotNull { id ->
                                    recordResolver(id)?.let { related ->
                                        "<a href=\\"inkquill://record/${Uri.encode(id)}\\">${TextUtils.htmlEncode(related.title)}</a>"
                                    }
                                }
                                if (links.isNotEmpty()) {
                                    append("<p><strong>").append(TextUtils.htmlEncode(f.name)).append(":</strong> ")
                                    append(links.joinToString(", ")).append("</p>")
                                }
                            }
                            "Table" -> {
                                val t = parseTable(raw, f.hint)
                                if (t.rows.isNotEmpty()) {
                                    append("<p><strong>").append(TextUtils.htmlEncode(f.name)).append("</strong></p>")
                                    append("<table border=\"1\" style=\"border-collapse:collapse;width:100%;margin-bottom:10px;\"><tr>")
                                    t.columns.forEach { col ->
                                        append("<th style=\"padding:6px;text-align:left;\">").append(TextUtils.htmlEncode(col)).append("</th>")
                                    }
                                    append("</tr>")
                                    t.rows.forEach { row ->
                                        append("<tr>")
                                        t.columns.indices.forEach { i ->
                                            append("<td style=\"padding:6px;\">").append(TextUtils.htmlEncode(row.getOrNull(i).orEmpty())).append("</td>")
                                        }
                                        append("</tr>")
                                    }
                                    append("</table>")
                                }
                            }
                            "Repeatable group" -> {
                                val entries = parseRepeatable(raw)
                                if (entries.isNotEmpty()) {
                                    append("<p><strong>").append(TextUtils.htmlEncode(f.name)).append(":</strong></p><ul>")
                                    entries.forEach { entry ->
                                        append("<li><strong>").append(TextUtils.htmlEncode(entry.label)).append("</strong>")
                                        if (entry.detail.isNotBlank()) {
                                            append(" — ").append(TextUtils.htmlEncode(entry.detail))
                                        }
                                        append("</li>")
                                    }
                                    append("</ul>")
                                }
                            }
                            "Rich text" -> {
                                if (DocumentExport.plain(raw).isNotBlank()) {
                                    append("<p><strong>").append(TextUtils.htmlEncode(f.name)).append(":</strong></p>")
                                    append("<div>").append(raw).append("</div>")
                                }
                            }
                            else -> {
                                val text = formatFieldPlainValue(f, fields, values, recordResolver)
                                if (text.isNotBlank()) {
                                    append("<p><strong>").append(TextUtils.htmlEncode(f.name)).append(":</strong> ")
                                    append(TextUtils.htmlEncode(text).replace("\n", "<br/>")).append("</p>")
                                }
                            }
                        }
                    }
                }
                if (itemsHtml.isNotBlank()) {
                    append("<h2>").append(TextUtils.htmlEncode(section)).append("</h2>")
                    append(itemsHtml)
                }
            }
        }
        val cleanProse = proseHtml.trim()
        return buildString {
            append(sheetHtml)
            if (cleanProse.isNotBlank() && DocumentExport.plain(cleanProse).isNotBlank()) {
                if (sheetHtml.isNotBlank()) append("<hr/><h2>Notes &amp; Prose</h2>")
                append(cleanProse)
            }
        }
    }
}
