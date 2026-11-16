package com.parasaurpal.inkandquill.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TemplateConditionsTest {
    private val fields = listOf(
        TemplateField("choice", "t", "Category", "Dropdown", false, 0),
        TemplateField("score", "t", "Score", "Number", false, 1),
        TemplateField("notes", "t", "Notes", "Long text", false, 2)
    )
    private val values = mapOf("choice" to "Fantasy", "score" to "85", "notes" to "")

    @Test
    fun supportsEqualityAndInequality() {
        assertTrue(TemplateConditions.isVisible("Category", "fantasy", fields, values))
        assertTrue(TemplateConditions.isVisible("Category", "!=Science", fields, values))
        assertFalse(TemplateConditions.isVisible("Category", "Science", fields, values))
    }

    @Test
    fun supportsNumericComparisons() {
        assertTrue(TemplateConditions.isVisible("Score", ">=80", fields, values))
        assertTrue(TemplateConditions.isVisible("Score", ">84", fields, values))
        assertTrue(TemplateConditions.isVisible("Score", "<=85", fields, values))
        assertFalse(TemplateConditions.isVisible("Score", "<80", fields, values))
    }

    @Test
    fun supportsTextAndSetConditions() {
        assertTrue(TemplateConditions.isVisible("Category", "contains:tas", fields, values))
        assertTrue(TemplateConditions.isVisible("Category", "starts with:fan", fields, values))
        assertTrue(TemplateConditions.isVisible("Category", "ends with:asy", fields, values))
        assertTrue(TemplateConditions.isVisible("Category", "in:Romance|Fantasy|Mystery", fields, values))
        assertFalse(TemplateConditions.isVisible("Category", "not contains:fant", fields, values))
    }

    @Test
    fun supportsEmptyChecksAndMissingFields() {
        assertTrue(TemplateConditions.isVisible("Notes", "is empty", fields, values))
        assertFalse(TemplateConditions.isVisible("Notes", "is not empty", fields, values))
        assertFalse(TemplateConditions.isVisible("Missing", "anything", fields, values))
        assertTrue(TemplateConditions.isVisible("", "anything", fields, values))
    }
}
