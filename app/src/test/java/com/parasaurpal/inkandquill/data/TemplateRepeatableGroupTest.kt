package com.parasaurpal.inkandquill.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TemplateRepeatableGroupTest {
    private val fields = "Name,Description,Age"

    @Test
    fun addsEditsAndRemovesStructuredItems() {
        var raw = TemplateRepeatableGroup.add("", fields)
        raw = TemplateRepeatableGroup.update(raw, fields, 0, "Name", "Mira 🌙")
        raw = TemplateRepeatableGroup.update(raw, fields, 0, "Description", "A cartographer")
        raw = TemplateRepeatableGroup.add(raw, fields)
        raw = TemplateRepeatableGroup.update(raw, fields, 1, "Name", "Tarin")

        val groups = TemplateRepeatableGroup.parse(raw, fields)
        assertEquals("Mira 🌙", groups[0]["Name"])
        assertEquals("A cartographer", groups[0]["Description"])
        assertEquals("Tarin", groups[1]["Name"])
        assertTrue(TemplateRepeatableGroup.hasValue(raw, fields))

        val remaining = TemplateRepeatableGroup.parse(TemplateRepeatableGroup.remove(raw, fields, 0), fields)
        assertEquals(1, remaining.size)
        assertEquals("Tarin", remaining[0]["Name"])
    }

    @Test
    fun formatsGroupsForNewDocumentAndKeepsLegacyText() {
        var raw = TemplateRepeatableGroup.add("", "Name,Role")
        raw = TemplateRepeatableGroup.update(raw, "Name,Role", 0, "Name", "Mira")
        raw = TemplateRepeatableGroup.update(raw, "Name,Role", 0, "Role", "Navigator")
        assertEquals("Item 1\nName: Mira\nRole: Navigator", TemplateRepeatableGroup.format(raw, "Name,Role"))
        assertEquals("Item 1\nName: Old item", TemplateRepeatableGroup.format("Old item", "Name,Role"))
    }

    @Test
    fun emptyGroupsDoNotSatisfyRequiredFields() {
        val raw = TemplateRepeatableGroup.add("", fields)
        assertFalse(TemplateRepeatableGroup.hasValue(raw, fields))
    }
}
