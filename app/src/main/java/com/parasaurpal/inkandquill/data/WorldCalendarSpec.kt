package com.parasaurpal.inkandquill.data

/**
 * Validated arithmetic for user-defined world calendars.
 *
 * Supported leap rule: "every 4 years add 1 day to month 2".
 * Leap years are multiples of the interval, beginning with year 1 as the epoch.
 */
data class WorldCalendarSpec private constructor(
    val months: List<String>,
    val monthLengths: List<Int>,
    val weekdays: List<String>,
    private val leapEveryYears: Int?,
    private val leapDays: Int,
    private val leapMonth: Int
) {
    val baseYearLength: Int = monthLengths.sum()

    fun isLeapYear(year: Int): Boolean =
        leapEveryYears != null && year > 0 && year % leapEveryYears == 0

    fun daysInMonth(year: Int, month: Int): Int {
        require(year in 1..MAX_YEAR) { "Year must be between 1 and $MAX_YEAR." }
        require(month in 1..months.size) { "Month is outside this calendar." }
        return monthLengths[month - 1] + if (isLeapYear(year) && month == leapMonth) leapDays else 0
    }

    fun isValidDate(year: Int, month: Int, day: Int): Boolean =
        year in 1..MAX_YEAR && month in 1..months.size &&
            day in 1..daysInMonthSafe(year, month)

    fun requireValidDate(year: Int, month: Int, day: Int) {
        require(isValidDate(year, month, day)) {
            "Date is outside this calendar. Check the year, month, and number of days in that month."
        }
    }

    /** Zero-based weekday for the supplied date; year 1/month 1/day 1 starts on weekday 0. */
    fun weekdayIndex(year: Int, month: Int, day: Int): Int {
        requireValidDate(year, month, day)
        val priorYears = year - 1
        var days = priorYears.toLong() * baseYearLength
        leapEveryYears?.let { days += (priorYears / it).toLong() * leapDays }
        for (m in 1 until month) days += daysInMonth(year, m)
        days += day - 1
        return (days % weekdays.size).toInt()
    }

    /** Zero-based day offset from the beginning of year 1. */
    fun absoluteDay(year: Int, month: Int, day: Int): Long {
        requireValidDate(year, month, day)
        val priorYears = year - 1
        var days = priorYears.toLong() * baseYearLength
        leapEveryYears?.let { days += (priorYears / it).toLong() * leapDays }
        for (m in 1 until month) days += daysInMonth(year, m)
        return days + day - 1
    }

    fun compareDates(
        firstYear: Int, firstMonth: Int, firstDay: Int,
        secondYear: Int, secondMonth: Int, secondDay: Int
    ): Int = absoluteDay(firstYear, firstMonth, firstDay)
        .compareTo(absoluteDay(secondYear, secondMonth, secondDay))

    private fun daysInMonthSafe(year: Int, month: Int): Int =
        if (month in 1..months.size && year in 1..MAX_YEAR)
            monthLengths[month - 1] + if (isLeapYear(year) && month == leapMonth) leapDays else 0
        else 0

    companion object {
        const val MAX_YEAR = 1_000_000
        private val leapPattern = Regex(
            """(?i)^every\\s+(\\d+)\\s+years?,?\\s+add\\s+(\\d+)\\s+days?\\s+to\\s+month\\s+(\\d+)$"""
        )

        fun parse(
            monthNames: String,
            monthLengthText: String,
            weekdayNames: String,
            leapRule: String = ""
        ): WorldCalendarSpec {
            val months = monthNames.split(",").map(String::trim).filter(String::isNotEmpty)
            require(months.isNotEmpty()) { "Add at least one month." }
            require(months.size <= 24) { "A calendar can have at most 24 months." }
            require(months.map(String::lowercase).distinct().size == months.size) {
                "Month names must be unique."
            }

            val lengths = if (monthLengthText.isBlank()) List(months.size) { 30 }
            else monthLengthText.split(",").map { it.trim().toIntOrNull()
                ?: throw IllegalArgumentException("Every month length must be a whole number.") }
            require(lengths.size == months.size) { "Enter one day count for every month." }
            require(lengths.all { it in 1..100 }) { "Each month must contain between 1 and 100 days." }

            val weekdays = weekdayNames.split(",").map(String::trim).filter(String::isNotEmpty)
            require(weekdays.isNotEmpty() && weekdays.size <= 14) {
                "Add between 1 and 14 weekdays."
            }
            require(weekdays.map(String::lowercase).distinct().size == weekdays.size) {
                "Weekday names must be unique."
            }

            val rule = leapRule.trim()
            if (rule.isEmpty()) return WorldCalendarSpec(months, lengths, weekdays, null, 0, 0)
            val match = leapPattern.matchEntire(rule)
                ?: throw IllegalArgumentException(
                    "Use the leap-rule format: every 4 years add 1 day to month 2."
                )
            val interval = match.groupValues[1].toIntOrNull()
                ?: throw IllegalArgumentException("Leap interval is too large.")
            val extraDays = match.groupValues[2].toIntOrNull()
                ?: throw IllegalArgumentException("Leap-day count is too large.")
            val month = match.groupValues[3].toIntOrNull()
                ?: throw IllegalArgumentException("Leap month is invalid.")
            require(interval in 2..1000) { "Leap interval must be between 2 and 1000 years." }
            require(extraDays in 1..30) { "A leap rule can add between 1 and 30 days." }
            require(month in 1..months.size) { "Leap month must match one of the calendar months." }
            return WorldCalendarSpec(months, lengths, weekdays, interval, extraDays, month)
        }
    }
}
