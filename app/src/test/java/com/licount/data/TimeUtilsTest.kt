package com.licount.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class TimeUtilsTest {

    @Test
    fun monthRangeStartsAtFirstDayMidnightAndEndsNextMonth() {
        val (start, end) = TimeUtils.currentMonthRange()
        val s = Calendar.getInstance().apply { timeInMillis = start }
        val e = Calendar.getInstance().apply { timeInMillis = end }
        assertEquals(1, s.get(Calendar.DAY_OF_MONTH))
        assertEquals(0, s.get(Calendar.HOUR_OF_DAY))
        assertEquals(0, s.get(Calendar.MINUTE))
        assertEquals(1, e.get(Calendar.DAY_OF_MONTH))
        assertEquals((s.get(Calendar.MONTH) + 1) % 12, e.get(Calendar.MONTH))
        assertTrue(end > start)
    }

    @Test
    fun withDayKeepsClockTimeAndMovesDate() {
        val src = Calendar.getInstance().apply {
            clear()
            set(2026, Calendar.NOVEMBER, 25, 16, 30, 0)
        }.timeInMillis
        val targetDay = Calendar.getInstance().apply {
            clear()
            set(2026, Calendar.NOVEMBER, 24, 0, 0, 0)
        }.timeInMillis
        val moved = TimeUtils.withDay(src, targetDay)
        val cal = Calendar.getInstance().apply { timeInMillis = moved }
        assertEquals(24, cal.get(Calendar.DAY_OF_MONTH))
        assertEquals(16, cal.get(Calendar.HOUR_OF_DAY))
        assertEquals(30, cal.get(Calendar.MINUTE))
        assertEquals(targetDay, TimeUtils.dayStart(moved))
    }

    @Test
    fun dayStartAndAddDays() {
        val day = TimeUtils.dayStart(System.currentTimeMillis())
        val cal = Calendar.getInstance().apply { timeInMillis = day }
        assertEquals(0, cal.get(Calendar.HOUR_OF_DAY))
        val yesterday = TimeUtils.addDays(day, -1)
        assertEquals(yesterday, TimeUtils.dayStart(yesterday + 12 * 60 * 60 * 1000L))
    }
}
