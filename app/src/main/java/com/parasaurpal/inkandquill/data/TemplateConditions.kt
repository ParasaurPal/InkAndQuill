package com.parasaurpal.inkandquill.data

/** Evaluates optional template-field visibility rules without executing user-supplied code. */
object TemplateConditions {
    fun isVisible(
        conditionField: String,
        conditionValue: String,
        fields: List<TemplateField>,
        values: Map<String, String>
    ): Boolean {
        if (conditionField.isBlank()) return true
        val field = fields.firstOrNull { it.name.equals(conditionField.trim(), ignoreCase = true) }
            ?: return false
        val actual = values[field.id].orEmpty()
        val rule = conditionValue.trim()
        val lower = rule.lowercase()

        return when {
            lower == "is empty" -> actual.isBlank()
            lower == "is not empty" -> actual.isNotBlank()
            lower.startsWith("not contains:") ->
                !actual.contains(rule.substringAfter(':').trim(), ignoreCase = true)
            lower.startsWith("contains:") ->
                actual.contains(rule.substringAfter(':').trim(), ignoreCase = true)
            lower.startsWith("starts with:") ->
                actual.startsWith(rule.substringAfter(':').trim(), ignoreCase = true)
            lower.startsWith("ends with:") ->
                actual.endsWith(rule.substringAfter(':').trim(), ignoreCase = true)
            lower.startsWith("not in:") ->
                rule.substringAfter(':').split('|', ',').map(String::trim)
                    .none { it.equals(actual, ignoreCase = true) }
            lower.startsWith("in:") ->
                rule.substringAfter(':').split('|', ',').map(String::trim)
                    .any { it.equals(actual, ignoreCase = true) }
            rule.startsWith(">=") -> compare(actual, rule.drop(2), Comparison.GREATER_OR_EQUAL)
            rule.startsWith("<=") -> compare(actual, rule.drop(2), Comparison.LESS_OR_EQUAL)
            rule.startsWith("!=") -> !actual.equals(rule.drop(2).trim(), ignoreCase = true)
            rule.startsWith(">") -> compare(actual, rule.drop(1), Comparison.GREATER)
            rule.startsWith("<") -> compare(actual, rule.drop(1), Comparison.LESS)
            rule.startsWith("=") -> actual.equals(rule.drop(1).trim(), ignoreCase = true)
            else -> actual.equals(rule, ignoreCase = true)
        }
    }

    private enum class Comparison { GREATER, LESS, GREATER_OR_EQUAL, LESS_OR_EQUAL }

    private fun compare(actual: String, expected: String, comparison: Comparison): Boolean {
        val right = expected.trim()
        val leftNumber = actual.trim().toDoubleOrNull()
        val rightNumber = right.toDoubleOrNull()
        val result = if (leftNumber != null && rightNumber != null) {
            leftNumber.compareTo(rightNumber)
        } else actual.trim().compareTo(right, ignoreCase = true)
        return when (comparison) {
            Comparison.GREATER -> result > 0
            Comparison.LESS -> result < 0
            Comparison.GREATER_OR_EQUAL -> result >= 0
            Comparison.LESS_OR_EQUAL -> result <= 0
        }
    }
}
