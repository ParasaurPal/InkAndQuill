package com.parasaurpal.inkandquill.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

private val Context.templateAutoSaveDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "ink_template_autosave"
)

data class AutoSaveStatus(
    val lastSavedAt: Long = 0L,
    val templateCount: Int = 0,
    val sheetCount: Int = 0,
    val isSaving: Boolean = false,
    val lastMessage: String = "Auto-save ready (every 30s)"
)

data class ActiveEntryDraft(
    val recordId: String,
    val title: String,
    val body: String,
    val tags: String,
    val aliases: String,
    val parentId: String,
    val scope: String,
    val coverUri: String,
    val templateId: String,
    val sheetValues: Map<String, String>,
    val updatedAt: Long = System.currentTimeMillis()
)

data class ActiveBlueprintDraft(
    val templateId: String,
    val title: String,
    val description: String,
    val category: String,
    val defaultKind: String,
    val updatedAt: Long = System.currentTimeMillis()
)

data class AutoSaveRestoreResult(
    val templatesRestored: Int,
    val sheetsRestored: Int,
    val draftRestored: Boolean
)

class TemplateAutoSaveManager(context: Context) {

    companion object {
        const val AUTO_SAVE_INTERVAL_MS = 30_000L

        private val KEY_SNAPSHOT_JSON = stringPreferencesKey("template_snapshot_json")
        private val KEY_ACTIVE_DRAFT_JSON = stringPreferencesKey("active_draft_json")
        private val KEY_LAST_SAVED_AT = longPreferencesKey("last_saved_at")
        private val KEY_TEMPLATE_COUNT = intPreferencesKey("saved_template_count")
        private val KEY_SHEET_COUNT = intPreferencesKey("saved_sheet_count")
    }

    private val appContext = context.applicationContext
    private val autoSaveFile = File(File(appContext.filesDir, "autosave"), "template_autosave.json")

    private val _status = MutableStateFlow(AutoSaveStatus())
    val status: StateFlow<AutoSaveStatus> = _status.asStateFlow()

    @Volatile
    private var activeEntryDraft: ActiveEntryDraft? = null

    @Volatile
    private var activeBlueprintDraft: ActiveBlueprintDraft? = null

    fun updateEntryDraft(draft: ActiveEntryDraft?) {
        activeEntryDraft = draft
    }

    fun clearEntryDraft(recordId: String? = null) {
        if (recordId == null || activeEntryDraft?.recordId == recordId) {
            activeEntryDraft = null
        }
    }

    fun updateBlueprintDraft(draft: ActiveBlueprintDraft?) {
        activeBlueprintDraft = draft
    }

    fun clearBlueprintDraft(templateId: String? = null) {
        if (templateId == null || activeBlueprintDraft?.templateId == templateId) {
            activeBlueprintDraft = null
        }
    }

    suspend fun loadInitialStatus() {
        withContext(Dispatchers.IO) {
            runCatching {
                val prefs = appContext.templateAutoSaveDataStore.data.first()
                val lastAt = prefs[KEY_LAST_SAVED_AT] ?: 0L
                val tplCount = prefs[KEY_TEMPLATE_COUNT] ?: 0
                val sheetCount = prefs[KEY_SHEET_COUNT] ?: 0
                if (lastAt > 0L) {
                    _status.value = AutoSaveStatus(
                        lastSavedAt = lastAt,
                        templateCount = tplCount,
                        sheetCount = sheetCount,
                        isSaving = false,
                        lastMessage = "Last auto-saved"
                    )
                }
            }
        }
    }

    suspend fun recoverPendingDraftIfNewer(db: InkDatabase): Boolean {
        return withContext(Dispatchers.IO) {
            runCatching {
                val prefs = appContext.templateAutoSaveDataStore.data.first()
                val draftRaw = prefs[KEY_ACTIVE_DRAFT_JSON].orEmpty()
                if (draftRaw.isBlank()) return@runCatching false
                val obj = JSONObject(draftRaw)
                var recovered = false

                obj.optJSONObject("entry")?.let { entry ->
                    val recordId = entry.optString("recordId")
                    val draftAt = entry.optLong("updatedAt", 0L)
                    val existing = db.get(recordId)
                    if (existing != null && draftAt > existing.updatedAt) {
                        db.save(
                            existing.copy(
                                title = entry.optString("title", existing.title),
                                body = entry.optString("body", existing.body),
                                tags = entry.optString("tags", existing.tags),
                                aliases = entry.optString("aliases", existing.aliases),
                                parentId = entry.optString("parentId", existing.parentId),
                                scope = entry.optString("scope", existing.scope),
                                coverUri = entry.optString("coverUri", existing.coverUri)
                            )
                        )
                        val tplId = entry.optString("templateId")
                        if (tplId.isNotBlank()) db.attachSheet(recordId, tplId)
                        entry.optJSONObject("sheetValues")?.let { sv ->
                            val map = buildMap {
                                sv.keys().forEach { k -> put(k, sv.optString(k, "")) }
                            }
                            db.saveSheetValues(recordId, map)
                        }
                        recovered = true
                    }
                }

                obj.optJSONObject("blueprint")?.let { bp ->
                    val tplId = bp.optString("templateId")
                    val draftAt = bp.optLong("updatedAt", 0L)
                    val existing = db.get(tplId)
                    if (existing != null && draftAt > existing.updatedAt) {
                        db.save(
                            existing.copy(
                                title = bp.optString("title", existing.title),
                                body = bp.optString("description", existing.body),
                                tags = bp.optString("category", existing.tags),
                                scope = bp.optString("defaultKind", existing.scope)
                            )
                        )
                        recovered = true
                    }
                }

                if (recovered) {
                    appContext.templateAutoSaveDataStore.edit { it.remove(KEY_ACTIVE_DRAFT_JSON) }
                }
                recovered
            }.getOrDefault(false)
        }
    }

    suspend fun saveSnapshot(db: InkDatabase, reason: String = "Scheduled 30s auto-save"): Boolean {
        _status.value = _status.value.copy(isSaving = true)
        return withContext(Dispatchers.IO) {
            runCatching {
                val entryDraft = activeEntryDraft
                if (entryDraft != null) {
                    db.get(entryDraft.recordId)?.let { current ->
                        if (current.title != entryDraft.title ||
                            current.body != entryDraft.body ||
                            current.tags != entryDraft.tags ||
                            current.aliases != entryDraft.aliases ||
                            current.parentId != entryDraft.parentId ||
                            current.scope != entryDraft.scope
                        ) {
                            db.save(
                                current.copy(
                                    title = entryDraft.title,
                                    body = entryDraft.body,
                                    tags = entryDraft.tags,
                                    aliases = entryDraft.aliases,
                                    parentId = entryDraft.parentId,
                                    scope = entryDraft.scope,
                                    coverUri = entryDraft.coverUri
                                )
                            )
                        }
                        if (entryDraft.templateId.isNotBlank()) {
                            db.saveSheetValues(entryDraft.recordId, entryDraft.sheetValues)
                        }
                    }
                }

                val bpDraft = activeBlueprintDraft
                if (bpDraft != null) {
                    db.get(bpDraft.templateId)?.let { tpl ->
                        if (tpl.title != bpDraft.title ||
                            tpl.body != bpDraft.description ||
                            tpl.tags != bpDraft.category ||
                            tpl.scope != bpDraft.defaultKind
                        ) {
                            db.save(
                                tpl.copy(
                                    title = bpDraft.title,
                                    body = bpDraft.description,
                                    tags = bpDraft.category,
                                    scope = bpDraft.defaultKind
                                )
                            )
                        }
                    }
                }

                val now = System.currentTimeMillis()
                val allTemplates = db.templates().filter { tpl ->
                    !tpl.id.startsWith("template_") || db.templateVersionCount(tpl.id) > 0
                }
                val templatesArr = JSONArray()
                allTemplates.forEach { tpl ->
                    val sectionsArr = JSONArray()
                    db.templateSections(tpl.id).forEach { sectionsArr.put(it) }
                    val fieldsArr = JSONArray()
                    db.templateFields(tpl.id, includeRemoved = true).forEach { f ->
                        fieldsArr.put(
                            JSONObject()
                                .put("id", f.id)
                                .put("name", f.name)
                                .put("type", f.fieldType)
                                .put("required", f.required)
                                .put("position", f.position)
                                .put("section", f.section)
                                .put("options", f.options)
                                .put("hint", f.hint)
                                .put("conditionField", f.conditionField)
                                .put("conditionValue", f.conditionValue)
                                .put("formula", f.formula)
                                .put("repeatable", f.repeatable)
                        )
                    }
                    templatesArr.put(
                        JSONObject()
                            .put("id", tpl.id)
                            .put("title", tpl.title)
                            .put("body", tpl.body)
                            .put("kind", tpl.kind)
                            .put("category", tpl.tags)
                            .put("defaultKind", tpl.scope)
                            .put("parentId", tpl.parentId)
                            .put("sections", sectionsArr)
                            .put("fields", fieldsArr)
                    )
                }

                val sheetsArr = JSONArray()
                val entriesWithSheets = db.all().filter { db.sheetTemplateId(it.id).isNotBlank() }
                entriesWithSheets.forEach { entry ->
                    val tplId = db.sheetTemplateId(entry.id)
                    val valuesObj = JSONObject()
                    db.sheetValues(entry.id).forEach { (fieldId, value) ->
                        // Keep DataStore compact; skip huge inline base64 blobs over 180 KB in preferences mirror
                        if (value.length <= 180_000) {
                            valuesObj.put(fieldId, value)
                        }
                    }
                    sheetsArr.put(
                        JSONObject()
                            .put("recordId", entry.id)
                            .put("title", entry.title)
                            .put("kind", entry.kind)
                            .put("templateId", tplId)
                            .put("values", valuesObj)
                    )
                }

                val snapshotJson = JSONObject()
                    .put("format", "inkandquill-autosave")
                    .put("version", 1)
                    .put("savedAt", now)
                    .put("reason", reason)
                    .put("templates", templatesArr)
                    .put("sheets", sheetsArr)
                    .toString()

                val draftJson = buildDraftJson(entryDraft, bpDraft)

                // 1. Persist to Jetpack DataStore Preferences
                appContext.templateAutoSaveDataStore.edit { prefs ->
                    prefs[KEY_SNAPSHOT_JSON] = snapshotJson
                    if (draftJson.isNotBlank()) prefs[KEY_ACTIVE_DRAFT_JSON] = draftJson
                    else prefs.remove(KEY_ACTIVE_DRAFT_JSON)
                    prefs[KEY_LAST_SAVED_AT] = now
                    prefs[KEY_TEMPLATE_COUNT] = allTemplates.size
                    prefs[KEY_SHEET_COUNT] = entriesWithSheets.size
                }

                // 2. Mirror atomically to local auto-save recovery file
                autoSaveFile.parentFile?.mkdirs()
                val tmpFile = File(autoSaveFile.parentFile, "${autoSaveFile.name}.tmp")
                tmpFile.writeText(snapshotJson)
                if (autoSaveFile.exists()) autoSaveFile.delete()
                tmpFile.renameTo(autoSaveFile)

                _status.value = AutoSaveStatus(
                    lastSavedAt = now,
                    templateCount = allTemplates.size,
                    sheetCount = entriesWithSheets.size,
                    isSaving = false,
                    lastMessage = reason
                )
                true
            }.getOrElse { err ->
                _status.value = _status.value.copy(
                    isSaving = false,
                    lastMessage = err.message ?: "Auto-save failed"
                )
                false
            }
        }
    }

    suspend fun restoreFromAutoSave(db: InkDatabase): AutoSaveRestoreResult {
        return withContext(Dispatchers.IO) {
            val prefs = appContext.templateAutoSaveDataStore.data.first()
            val raw = prefs[KEY_SNAPSHOT_JSON]?.takeIf { it.isNotBlank() }
                ?: if (autoSaveFile.isFile) autoSaveFile.readText() else ""
            if (raw.isBlank()) {
                return@withContext AutoSaveRestoreResult(0, 0, false)
            }
            val root = JSONObject(raw)
            var templatesRestored = 0
            var sheetsRestored = 0

            val templatesArr = root.optJSONArray("templates") ?: JSONArray()
            for (i in 0 until templatesArr.length()) {
                val obj = templatesArr.optJSONObject(i) ?: continue
                val id = obj.optString("id")
                if (id.isBlank()) continue
                val existing = db.get(id)
                val record = (existing ?: InkRecord(id = id)).copy(
                    title = obj.optString("title", "Custom Template"),
                    body = obj.optString("body", ""),
                    kind = obj.optString("kind", "Template"),
                    tags = obj.optString("category", "Other"),
                    scope = obj.optString("defaultKind", "Document"),
                    parentId = obj.optString("parentId", "")
                )
                db.save(record)
                val secArr = obj.optJSONArray("sections") ?: JSONArray()
                for (s in 0 until secArr.length()) {
                    val secName = secArr.optString(s)
                    if (secName.isNotBlank()) db.addTemplateSection(id, secName)
                }
                val existingFieldNames = db.templateFields(id, includeRemoved = true).map { it.name.lowercase() }.toSet()
                val fieldsArr = obj.optJSONArray("fields") ?: JSONArray()
                for (fIdx in 0 until fieldsArr.length()) {
                    val fObj = fieldsArr.optJSONObject(fIdx) ?: continue
                    val fName = fObj.optString("name", "Field")
                    if (fName.lowercase() !in existingFieldNames) {
                        db.addTemplateField(
                            templateId = id,
                            name = fName,
                            type = fObj.optString("type", "Long text"),
                            required = fObj.optBoolean("required", false),
                            section = fObj.optString("section", "General"),
                            options = fObj.optString("options", ""),
                            hint = fObj.optString("hint", ""),
                            conditionField = fObj.optString("conditionField", ""),
                            conditionValue = fObj.optString("conditionValue", ""),
                            formula = fObj.optString("formula", ""),
                            repeatable = fObj.optBoolean("repeatable", false)
                        )
                    }
                }
                templatesRestored++
            }

            val sheetsArr = root.optJSONArray("sheets") ?: JSONArray()
            for (i in 0 until sheetsArr.length()) {
                val sObj = sheetsArr.optJSONObject(i) ?: continue
                val recordId = sObj.optString("recordId")
                val templateId = sObj.optString("templateId")
                if (recordId.isBlank() || templateId.isBlank() || db.get(recordId) == null) continue
                db.attachSheet(recordId, templateId)
                sObj.optJSONObject("values")?.let { valsObj ->
                    val map = buildMap {
                        valsObj.keys().forEach { k -> put(k, valsObj.optString(k, "")) }
                    }
                    if (map.isNotEmpty()) db.saveSheetValues(recordId, map)
                }
                sheetsRestored++
            }

            val draftRecovered = recoverPendingDraftIfNewer(db)
            db.rebuildSearchIndex()
            AutoSaveRestoreResult(templatesRestored, sheetsRestored, draftRecovered)
        }
    }

    private fun buildDraftJson(
        entry: ActiveEntryDraft?,
        blueprint: ActiveBlueprintDraft?
    ): String {
        if (entry == null && blueprint == null) return ""
        val root = JSONObject()
        if (entry != null) {
            val vals = JSONObject()
            entry.sheetValues.forEach { (k, v) ->
                if (v.length <= 180_000) vals.put(k, v)
            }
            root.put(
                "entry",
                JSONObject()
                    .put("recordId", entry.recordId)
                    .put("title", entry.title)
                    .put("body", entry.body)
                    .put("tags", entry.tags)
                    .put("aliases", entry.aliases)
                    .put("parentId", entry.parentId)
                    .put("scope", entry.scope)
                    .put("coverUri", entry.coverUri)
                    .put("templateId", entry.templateId)
                    .put("sheetValues", vals)
                    .put("updatedAt", entry.updatedAt)
            )
        }
        if (blueprint != null) {
            root.put(
                "blueprint",
                JSONObject()
                    .put("templateId", blueprint.templateId)
                    .put("title", blueprint.title)
                    .put("description", blueprint.description)
                    .put("category", blueprint.category)
                    .put("defaultKind", blueprint.defaultKind)
                    .put("updatedAt", blueprint.updatedAt)
            )
        }
        return root.toString()
    }
}
