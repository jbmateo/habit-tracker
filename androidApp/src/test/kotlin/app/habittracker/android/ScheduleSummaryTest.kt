package app.habittracker.android

import app.habittracker.android.ui.common.scheduleSummary
import kotlinx.datetime.DayOfWeek
import kotlin.test.Test
import kotlin.test.assertEquals

class ScheduleSummaryTest {
    @Test
    fun allSevenDaysIsEveryDay() {
        assertEquals("Every day", scheduleSummary(DayOfWeek.entries.toSet()))
    }

    @Test
    fun subsetIsMondayFirstRegardlessOfSetOrder() {
        assertEquals("Mon · Wed · Fri", scheduleSummary(setOf(DayOfWeek.FRIDAY, DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY)))
        assertEquals("Sat · Sun", scheduleSummary(setOf(DayOfWeek.SUNDAY, DayOfWeek.SATURDAY)))
    }
}
