package com.parasaurpal.inkandquill.data

import java.util.Base64

/** Compact, versioned storage for repeatable template groups. */
object TemplateRepeatableGroup {
    private const val PREFIX = "INKQRG1:"
    private val encoder = Base64.getUrlEncoder().withoutPadding()
    private val decoder = Base64.getUrlDecoder()

    fun fields(options: String): List<String> =
        options.split(",").map(String::trim).filter(String::isNotBlank).distinct()
            .ifEmpty { listOf("Name", "Details") }

    fun parse(raw: String, options: String): List<Map<String, String>> {
        if (raw.isBlank()) return emptyList()
        val names = fields(options)
        if (!raw.startsWith(PREFIX)) {
            return raw.lines().filter(String::isNotBlank).map { line ->
                names.associateWith { name -> if (name == names.first()) line else "" }
            }
        }
        val payload = raw.removePrefix(PREFIX)
        if (payload.isBlank()) return emptyList()
        return payload.split(';').map { row ->
            if (row.isBlank()) emptyMap()
            else row.split('&').associate { pair ->
                val split = pair.split(':', limit = 2)
                require(split.size == 2) { "Invalid repeatable group data." }
                decode(split[0]) to decode(split[1])
            }
        }
    }

    fun add(raw: String, options: String): String =
        serialize(parse(raw, options) + fields(options).associateWith { "" })

    fun update(raw: String, options: String, index: Int, field: String, value: String): String {
        val groups = parse(raw, options).toMutableList()
        require(index in groups.indices) { "Repeatable group no longer exists." }
        require(field in fields(options)) { "Unknown repeatable group field." }
        groups[index] = groups[index] + (field to value)
        return serialize(groups)
    }

    fun remove(raw: String, options: String, index: Int): String {
        val groups = parse(raw, options).toMutableList()
        require(index in groups.indices) { "Repeatable group no longer exists." }
        groups.removeAt(index)
        return serialize(groups)
    }

    fun hasValue(raw: String, options: String): Boolean =
        parse(raw, options).any { group -> fields(options).any { !group[it].isNullOrBlank() } }

    fun format(raw: String, options: String): String =
        parse(raw, options).mapIndexedNotNull { index, group ->
            val details = fields(options).filter { !group[it].isNullOrBlank() }
                .joinToString("\n") { "$it: ${group[it]}" }
            if (details.isBlank()) null else "Item ${index + 1}\n$details"
        }.joinToString("\n\n")

    private fun serialize(groups: List<Map<String, String>>): String =
        PREFIX + groups.joinToString(";") { group ->
            group.entries.joinToString("&") { (key, value) -> encode(key) + ":" + encode(value) }
        }

    private fun encode(value: String): String =
        encoder.encodeToString(value.toByteArray(Charsets.UTF_8))

    private fun decode(value: String): String =
        String(decoder.decode(value), Charsets.UTF_8)
}
