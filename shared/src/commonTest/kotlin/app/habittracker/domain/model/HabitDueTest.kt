package app.habittracker.domain.model

import app.habittracker.MON_WED_FRI
import app.habittracker.date
import app.habittracker.habit
import kotlinx.datetime.DayOfWeek
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class HabitDueTest {
    @Test
    fun notDueBeforeCreatedOn() {
        val h = habit(created = "2026-01-05")
        assertFalse(h.isDueOn(date("2026-01-04")))
        assertTrue(h.isDueOn(date("2026-01-05")))
    }

    @Test
    fun weekdayScheduleRespected() {
        val h = habit(days = MON_WED_FRI)
        assertTrue(h.isDueOn(date("2026-01-05")))
        assertTrue(h.isDueOn(date("2026-01-07")))
        assertFalse(h.isDueOn(date("2026-01-06")))
    }

    @Test
    fun scheduleHistoryUsesVersionInEffect() {
        val h = habit(
            schedules = listOf(
                ScheduleVersion(date("2026-01-05"), MON_WED_FRI),
                ScheduleVersion(date("2026-01-12"), setOf(DayOfWeek.TUESDAY)),
            ),
        )
        assertTrue(h.isDueOn(date("2026-01-07")))
        assertFalse(h.isDueOn(date("2026-01-14")))
        assertTrue(h.isDueOn(date("2026-01-13")))
        assertEquals(setOf(DayOfWeek.TUESDAY), h.currentSchedule.days)
    }

    @Test
    fun archivedPeriodIsNotDue() {
        val h = habit(archived = listOf(ArchivePeriod(date("2026-01-10"), date("2026-01-15"))))
        assertFalse(h.isDueOn(date("2026-01-10")))
        assertFalse(h.isDueOn(date("2026-01-14")))
        assertTrue(h.isDueOn(date("2026-01-15")))
        assertTrue(h.isDueOn(date("2026-01-09")))
        assertFalse(h.isArchived)
    }

    @Test
    fun openArchivePeriodIsNotDueForever() {
        val h = habit(archived = listOf(ArchivePeriod(date("2026-01-10"), null)))
        assertFalse(h.isDueOn(date("2027-01-01")))
        assertTrue(h.isArchived)
    }

    @Test
    fun completionIndexGroupsByDate() {
        val index = listOf(
            Completion("a", date("2026-01-05")),
            Completion("b", date("2026-01-05")),
            Completion("a", date("2026-01-06")),
        ).toIndex()
        assertEquals(setOf("a", "b"), index[date("2026-01-05")])
        assertEquals(2, index.size)
    }
}
