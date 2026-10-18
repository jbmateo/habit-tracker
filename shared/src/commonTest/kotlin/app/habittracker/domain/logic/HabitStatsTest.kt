package app.habittracker.domain.logic

import app.habittracker.MON_WED_FRI
import app.habittracker.date
import app.habittracker.habit
import app.habittracker.domain.model.ArchivePeriod
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class HabitStatsTest {
    private fun dates(vararg s: String) = s.map { date(it) }.toSet()

    @Test
    fun skipsNonDueDays() {
        val h = habit(days = MON_WED_FRI)
        val done = dates("2026-01-05", "2026-01-07", "2026-01-09", "2026-01-12")
        val stats = habitStats(h, done, date("2026-01-12"))
        assertEquals(4, stats.currentStreak)
        assertEquals(4, stats.bestStreak)
    }

    @Test
    fun todayUndoneDoesNotBreak() {
        val h = habit()
        val stats = habitStats(h, dates("2026-01-05", "2026-01-06", "2026-01-07"), date("2026-01-08"))
        assertEquals(3, stats.currentStreak)
        assertEquals(3, stats.bestStreak)
    }

    @Test
    fun pastMissBreaksCurrentButBestKeepsLongest() {
        val h = habit()
        val done = dates(
            "2026-01-05", "2026-01-06", "2026-01-07", "2026-01-08", "2026-01-09",
            "2026-01-11", "2026-01-12",
        )
        val stats = habitStats(h, done, date("2026-01-12"))
        assertEquals(2, stats.currentStreak)
        assertEquals(5, stats.bestStreak)
    }

    @Test
    fun completionRateExcludesTodayUnlessDone() {
        val h = habit()
        val past = dates("2026-01-05", "2026-01-06", "2026-01-07")
        val today = date("2026-01-09")
        assertEquals(0.75, habitStats(h, past, today).completionRate)
        assertEquals(0.8, habitStats(h, past + date("2026-01-09"), today).completionRate)
    }

    @Test
    fun createdTodayUndoneRateIsNull() {
        val h = habit(created = "2026-01-05")
        val stats = habitStats(h, emptySet(), date("2026-01-05"))
        assertNull(stats.completionRate)
        assertEquals(0, stats.currentStreak)
        assertEquals(0, stats.bestStreak)
    }

    @Test
    fun archivedGapIsSkipped() {
        val h = habit(archived = listOf(ArchivePeriod(date("2026-01-07"), date("2026-01-09"))))
        val stats = habitStats(h, dates("2026-01-05", "2026-01-06", "2026-01-09"), date("2026-01-09"))
        assertEquals(3, stats.currentStreak)
        assertEquals(1.0, stats.completionRate)
    }

    @Test
    fun doneOnNonDueDayIsIgnored() {
        val h = habit(days = MON_WED_FRI)
        val stats = habitStats(h, dates("2026-01-05", "2026-01-06"), date("2026-01-06"))
        assertEquals(1, stats.currentStreak)
        assertEquals(1.0, stats.completionRate)
    }
}
