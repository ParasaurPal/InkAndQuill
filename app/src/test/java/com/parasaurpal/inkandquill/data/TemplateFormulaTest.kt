package com.parasaurpal.inkandquill.data

import org.junit.Assert.assertEquals
import org.junit.Test

class TemplateFormulaTest {
    private val fields = listOf(
        TemplateField("f1", "t", "Word Count", "Number", false, 0),
        TemplateField("f2", "t", "Pages", "Number", false, 1),
        TemplateField("f3", "t", "Score", "Number", false, 2),
        TemplateField("f4", "t", "Colors", "Multi-select", false, 3)
    )
    private val values = mapOf("f1" to "120", "f2" to "8", "f3" to "92", "f4" to "red|blue|green")

    @Test
    fun supportsArithmeticAndBracedFieldNames() {
        assertEquals("256", TemplateFormula.evaluate("sum({Word Count}, Pages) * 2", fields, values))
        assertEquals("15", TemplateFormula.evaluate("(Pages + 2) / 2 * 3", fields, values))
    }

    @Test
    fun supportsAggregatesAndRounding() {
        assertEquals("220", TemplateFormula.evaluate("sum({Word Count}, Score, Pages)", fields, values))
        assertEquals("73.33", TemplateFormula.evaluate("round(avg({Word Count}, Score, Pages), 2)", fields, values))
        assertEquals("8", TemplateFormula.evaluate("max(Pages, 3, 8)", fields, values))
    }

    @Test
    fun supportsTextAndConditionalFunctions() {
        assertEquals("INK & QUILL", TemplateFormula.evaluate("uppercase(concat('Ink', ' & ', 'Quill'))", fields, values))
        assertEquals("Pass", TemplateFormula.evaluate("if(Score >= 80, 'Pass', 'Try again')", fields, values))
        assertEquals("3", TemplateFormula.evaluate("count(Colors)", fields, values))
        assertEquals("fallback", TemplateFormula.evaluate("coalesce('', 'fallback')", fields, values))
    }

    @Test
    fun invalidExpressionsReturnReadableErrors() {
        assertEquals("Invalid formula: division by zero", TemplateFormula.evaluate("Pages / 0", fields, values))
        assertEquals("Invalid formula: unknown field 'Missing'", TemplateFormula.evaluate("Missing + 1", fields, values))
    }
}
