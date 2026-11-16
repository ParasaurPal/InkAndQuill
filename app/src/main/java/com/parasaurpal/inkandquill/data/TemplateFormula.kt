package com.parasaurpal.inkandquill.data

import java.math.BigDecimal

private fun Double.toDisplayString(): String {
    require(isFinite()) { "result is not finite" }
    return BigDecimal(toString()).stripTrailingZeros().toPlainString()
}

/** Safe, deterministic expression evaluator for calculated template fields. */
object TemplateFormula {
    fun evaluate(formula: String, fields: List<TemplateField>, values: Map<String, String>): String {
        if (formula.isBlank()) return ""
        val references = fields.associate { field ->
            field.name.trim().lowercase() to values[field.id].orEmpty()
        }
        return runCatching { Parser(formula, references).parse().display() }
            .getOrElse { error -> "Invalid formula: ${error.message ?: "check the expression"}" }
    }

    private data class Value(val raw: Any) {
        fun text(): String = when (raw) {
            is Double -> raw.toDisplayString()
            is Boolean -> raw.toString()
            else -> raw.toString()
        }
        fun number(): Double = when (raw) {
            is Number -> raw.toDouble()
            is Boolean -> if (raw) 1.0 else 0.0
            else -> raw.toString().trim().toDoubleOrNull()
                ?: throw IllegalArgumentException("'$raw' is not a number")
        }
        fun truthy(): Boolean = when (raw) {
            is Boolean -> raw
            is Number -> raw.toDouble() != 0.0
            else -> raw.toString().isNotBlank() && !raw.toString().equals("false", true)
        }
        fun display(): String = text()
    }

    private class Parser(
        private val source: String,
        private val references: Map<String, String>
    ) {
        private var index = 0

        fun parse(): Value {
            val result = comparison()
            whitespace()
            require(index == source.length) { "unexpected text at position ${index + 1}" }
            return result
        }

        private fun comparison(): Value {
            val left = additive()
            whitespace()
            val operator = listOf("==", "!=", ">=", "<=", ">", "<").firstOrNull(::startsWith)
                ?: return left
            index += operator.length
            val right = additive()
            val result = when (operator) {
                "==" -> left.text().equals(right.text(), true)
                "!=" -> !left.text().equals(right.text(), true)
                ">" -> left.number() > right.number()
                "<" -> left.number() < right.number()
                ">=" -> left.number() >= right.number()
                else -> left.number() <= right.number()
            }
            return Value(result)
        }

        private fun additive(): Value {
            var value = multiplicative()
            while (true) {
                whitespace()
                value = when {
                    take('+') -> {
                        val right = multiplicative()
                        val a = value.text().toDoubleOrNull()
                        val b = right.text().toDoubleOrNull()
                        Value(if (a != null && b != null) a + b else value.text() + right.text())
                    }
                    take('-') -> Value(value.number() - multiplicative().number())
                    else -> return value
                }
            }
        }

        private fun multiplicative(): Value {
            var value = unary()
            while (true) {
                whitespace()
                value = when {
                    take('*') -> Value(value.number() * unary().number())
                    take('/') -> {
                        val divisor = unary().number()
                        require(divisor != 0.0) { "division by zero" }
                        Value(value.number() / divisor)
                    }
                    else -> return value
                }
            }
        }

        private fun unary(): Value {
            whitespace()
            return when {
                take('-') -> Value(-unary().number())
                take('+') -> Value(unary().number())
                else -> primary()
            }
        }

        private fun primary(): Value {
            whitespace()
            require(index < source.length) { "expected a value" }
            return when (source[index]) {
                '(' -> {
                    index++
                    val value = comparison()
                    whitespace()
                    require(take(')')) { "missing closing parenthesis" }
                    value
                }
                '{' -> reference(braced = true)
                '\'', '"' -> quoted()
                else -> {
                    val number = numberToken()
                    if (number != null) Value(number)
                    else {
                        val name = identifier()
                        require(name.isNotBlank()) { "unexpected character at position ${index + 1}" }
                        whitespace()
                        if (take('(')) function(name) else reference(name)
                    }
                }
            }
        }

        private fun function(name: String): Value {
            val args = mutableListOf<Value>()
            whitespace()
            if (!take(')')) {
                do {
                    args += comparison()
                    whitespace()
                } while (take(','))
                require(take(')')) { "missing closing parenthesis for $name" }
            }
            return when (name.lowercase()) {
                "sum" -> Value(args.sumOf { it.number() })
                "avg", "average" -> {
                    require(args.isNotEmpty()) { "$name needs at least one value" }
                    Value(args.map { it.number() }.average())
                }
                "min" -> { require(args.isNotEmpty()) { "min needs at least one value" }; Value(args.minOf { it.number() }) }
                "max" -> { require(args.isNotEmpty()) { "max needs at least one value" }; Value(args.maxOf { it.number() }) }
                "concat" -> Value(args.joinToString("") { it.text() })
                "uppercase", "upper" -> Value(args.firstOrNull()?.text()?.uppercase().orEmpty())
                "lowercase", "lower" -> Value(args.firstOrNull()?.text()?.lowercase().orEmpty())
                "count" -> {
                    val items = if (args.size == 1) args.first().text().split(Regex("[,|]"))
                    else args.map { it.text() }
                    Value(items.count { it.isNotBlank() }.toDouble())
                }
                "if" -> {
                    require(args.size == 3) { "if needs condition, true value, and false value" }
                    if (args[0].truthy()) args[1] else args[2]
                }
                "coalesce" -> Value(args.firstOrNull { it.text().isNotBlank() }?.text().orEmpty())
                "round" -> {
                    require(args.size in 1..2) { "round needs a value and optional decimal places" }
                    val places = args.getOrNull(1)?.number()?.toInt() ?: 0
                    require(places in 0..10) { "round decimal places must be between 0 and 10" }
                    Value("%.${places}f".format(java.util.Locale.ROOT, args[0].number()))
                }
                else -> throw IllegalArgumentException("unknown function '$name'")
            }
        }

        private fun reference(braced: Boolean): Value {
            val name = if (braced) {
                index++
                val start = index
                while (index < source.length && source[index] != '}') index++
                require(index < source.length) { "missing } after field name" }
                source.substring(start, index).trim().also { index++ }
            } else throw IllegalStateException("reference name missing")
            return reference(name)
        }

        private fun reference(name: String): Value {
            val key = name.trim().lowercase()
            val value = references[key] ?: throw IllegalArgumentException("unknown field '$name'")
            return Value(value)
        }

        private fun quoted(): Value {
            val quote = source[index++]
            val result = StringBuilder()
            while (index < source.length) {
                val char = source[index++]
                if (char == quote) return Value(result.toString())
                if (char == '\\' && index < source.length) {
                    result.append(when (val escaped = source[index++]) {
                        'n' -> '\n'
                        't' -> '\t'
                        else -> escaped
                    })
                } else result.append(char)
            }
            throw IllegalArgumentException("unclosed text value")
        }

        private fun numberToken(): Double? {
            val match = Regex("""(?:\d+(?:\.\d*)?|\.\d+)(?:[eE][+-]?\d+)?""")
                .find(source, index)?.takeIf { it.range.first == index } ?: return null
            index = match.range.last + 1
            return match.value.toDoubleOrNull()
        }

        private fun identifier(): String {
            val start = index
            while (index < source.length && (source[index].isLetterOrDigit() || source[index] == '_')) index++
            return source.substring(start, index)
        }

        private fun startsWith(value: String): Boolean = source.startsWith(value, index)
        private fun whitespace() { while (index < source.length && source[index].isWhitespace()) index++ }
        private fun take(char: Char): Boolean {
            if (index < source.length && source[index] == char) { index++; return true }
            return false
        }
    }
}
