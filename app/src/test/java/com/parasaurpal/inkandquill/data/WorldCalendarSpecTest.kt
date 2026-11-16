package com.parasaurpal.inkandquill.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WorldCalendarSpecTest {
    @Test
    fun validatesMonthLengthsAndRejectsImpossibleDates() {
        val calendar = WorldCalendarSpec.parse("Dawn,Dusk", "30,28", "Mon,Tue,Wed,Thu,Fri,Sat,Sun")
        assertTrue(calendar.isValidDate(1, 1, 30))
        assertTrue(calendar.isValidDate(1, 2, 28))
        assertFalse(calendar.isValidDate(1, 2, 29))
        assertFalse(calendar.isValidDate(0, 1, 1))
        assertFalse(calendar.isValidDate(1, 3, 1))
    }

    @Test
    fun appliesLeapDaysToConfiguredMonthOnlyInLeapYears() {
        val calendar = WorldCalendarSpec.parse(
            "Dawn,Bloom,Harvest",
            "30,28,30",
            "One,Two,Three,Four,Five",
            "every 4 years add 1 day to month 2"
        )
        assertEquals(28, calendar.daysInMonth(3, 2))
        assertEquals(29, calendar.daysInMonth(4, 2))
        assertEquals(30, calendar.daysInMonth(4, 3))
        assertTrue(calendar.isValidDate(4, 2, 29))
        assertFalse(calendar.isValidDate(3, 2, 29))
    }

    @Test
    fun calculatesWeekdayAcrossLeapYearBoundary() {
        val calendar = WorldCalendarSpec.parse(
            "Dawn,Bloom",
            "2,2",
            "One,Two,Three",
            "every 2 years add 1 day to month 2"
        )
        assertEquals(0, calendar.weekdayIndex(1, 1, 1))
        assertEquals(1, calendar.weekdayIndex(1, 1, 2))
        assertEquals(2, calendar.weekdayIndex(1, 2, 1))
        assertEquals(1, calendar.weekdayIndex(2, 1, 1))
    }

    @Test
    fun comparesDatesUsingCalendarArithmetic() {
        val calendar = WorldCalendarSpec.parse("A,B", "3,4", "Day 1,Day 2")
        assertTrue(calendar.compareDates(1, 2, 1, 1, 1, 3) > 0)
        assertEquals(0, calendar.compareDates(2, 1, 1, 2, 1, 1))
        assertTrue(calendar.compareDates(1, 1, 2, 1, 1, 3) < 0)
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsMonthLengthCountMismatch() {
        WorldCalendarSpec.parse("A,B,C", "30,30", "Day 1,Day 2")
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsMalformedLeapRule() {
        WorldCalendarSpec.parse("A,B", "30,30", "Day 1,Day 2", "every year sometimes")
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsLeapMonthOutsideCalendar() {
        WorldCalendarSpec.parse("A,B", "30,30", "Day 1,Day 2", "every 4 years add 1 day to month 3")
    }
}
