package app.habittracker.domain.logic

import app.habittracker.date
import app.habittracker.domain.model.ArchivePeriod
import app.habittracker.domain.model.CompletionIndex
import app.habittracker.domain.model.DayStatus
import app.habittracker.domain.model.ScheduleVersion
import app.habittracker.domain.model.StreakInfo
import app.habittracker.habit
import app.habittracker.MON_WED_FRI
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus
import kotlinx.datetime.DatePeriod
import kotlin.test.Test
import kotlin.test.assertEquals

class OverallStreakTest {
    private fun done(vararg entries: Pair<String, Set<String>>): CompletionIndex =
        entries.associate { (d, ids) -> date(d) to ids }

    /** Every date from..to inclusive completed for [ids]. */
    private fun doneRange(from: String, to: String, vararg ids: String): CompletionIndex {
        val result = mutableMapOf<LocalDate, Set<String>>()
        var d = date(from)
        val end = date(to)
        while (d <= end) {
            result[d] = ids.toSet()
            d = d.plus(DatePeriod(days = 1))
        }
        return result
    }

    @Test
    fun noHabitsIsZero() {
        assertEquals(StreakInfo(0, 0), overallStreak(emptyList(), emptyMap(), date("2026-01-08")))
    }

    @Test
    fun threePerfectDaysThenTodayInProgress() {
        val h = habit("h1")
        val result = overallStreak(listOf(h), doneRange("2026-01-05", "2026-01-07", "h1"), date("2026-01-08"))
        assertEquals(StreakInfo(3, 3), result)
    }

    @Test
    fun todayPerfectCounts() {
        val h = habit("h1")
        val result = overallStreak(listOf(h), doneRange("2026-01-05", "2026-01-08", "h1"), date("2026-01-08"))
        assertEquals(StreakInfo(4, 4), result)
    }

    @Test
    fun noHabitsGapDoesNotBreak() {
        val h = habit("h1", days = MON_WED_FRI)
        val completions = done(
            "2026-01-05" to setOf("h1"),
            "2026-01-07" to setOf("h1"),
            "2026-01-09" to setOf("h1"),
        )
        assertEquals(StreakInfo(3, 3), overallStreak(listOf(h), completions, date("2026-01-09")))
    }

    @Test
    fun missedDayStops() {
        val h = habit("h1")
        val completions = doneRange("2026-01-05", "2026-01-08", "h1") +
            doneRange("2026-01-10", "2026-01-11", "h1") // 01-09 missed; 01-12 (today) not yet done
        assertEquals(StreakInfo(2, 4), overallStreak(listOf(h), completions, date("2026-01-12")))
    }

    @Test
    fun habitCreatedTodayDoesNotBreakPastPerfectDays() {
        val old = habit("old", created = "2026-01-01")
        val new = habit("new", created = "2026-01-06")
        val completions = doneRange("2026-01-01", "2026-01-05", "old")
        assertEquals(StreakInfo(5, 5), overallStreak(listOf(old, new), completions, date("2026-01-06")))
    }

    @Test
    fun archivingMidStreakKeepsStreak() {
        val a = habit("a", archived = listOf(ArchivePeriod(date("2026-01-08"), null)))
        val b = habit("b")
        val completions = doneRange("2026-01-05", "2026-01-07", "a", "b") +
            doneRange("2026-01-08", "2026-01-10", "b")
        assertEquals(StreakInfo(6, 6), overallStreak(listOf(a, b), completions, date("2026-01-10")))
    }

    @Test
    fun scheduleChangeMidStreak() {
        // Daily until Wed 01-07, then Mon/Wed/Fri from Thu 01-08.
        val h = habit(
            "h1",
            created = "2026-01-05",
            schedules = listOf(
                ScheduleVersion(date("2026-01-05"), DayOfWeek.entries.toSet()),
                ScheduleVersion(date("2026-01-08"), MON_WED_FRI),
            ),
        )
        val completions = done(
            "2026-01-05" to setOf("h1"),
            "2026-01-06" to setOf("h1"),
            "2026-01-07" to setOf("h1"),
            // Thu 01-08 no longer due, Sat/Sun not due either
            "2026-01-09" to setOf("h1"),
            "2026-01-12" to setOf("h1"),
        )
        assertEquals(StreakInfo(5, 5), overallStreak(listOf(h), completions, date("2026-01-12")))
    }

    @Test
    fun acrossYearBoundaryAndLeapDay() {
        val year = habit("h1", created = "2027-12-30")
        assertEquals(
            StreakInfo(4, 4),
            overallStreak(listOf(year), doneRange("2027-12-30", "2028-01-02", "h1"), date("2028-01-02")),
        )
        val leap = habit("h1", created = "2028-02-28")
        assertEquals(
            StreakInfo(3, 3),
            overallStreak(listOf(leap), doneRange("2028-02-28", "2028-03-01", "h1"), date("2028-03-01")),
        )
    }

    @Test
    fun longestPerfectRunSkipsNeutralAndResetsOnFailure() {
        val statuses = listOf(
            DayStatus.Perfect(1), DayStatus.NoHabits, DayStatus.Perfect(1), DayStatus.InProgress(0, 1),
            DayStatus.Partial(1, 2),
            DayStatus.Perfect(1), DayStatus.Future,
        )
        assertEquals(2, longestPerfectRun(statuses))
        assertEquals(0, longestPerfectRun(emptyList()))
    }
}
