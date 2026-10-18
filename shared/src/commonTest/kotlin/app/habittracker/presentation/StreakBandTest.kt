package app.habittracker.presentation

import app.habittracker.domain.model.DayStatus
import app.habittracker.domain.usecase.CalendarDay
import app.habittracker.date
import app.habittracker.presentation.calendar.StreakBand
import app.habittracker.presentation.calendar.StreakBand.End
import app.habittracker.presentation.calendar.StreakBand.Middle
import app.habittracker.presentation.calendar.StreakBand.None
import app.habittracker.presentation.calendar.StreakBand.Single
import app.habittracker.presentation.calendar.StreakBand.Start
import app.habittracker.presentation.calendar.streakBands
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.plus
import kotlin.test.Test
import kotlin.test.assertEquals

class StreakBandTest {
    // 'P' = Perfect, 'M' = Missed, 'N' = NoHabits, 'F' = Future, starting on the given date.
    private fun days(start: String, pattern: String): List<CalendarDay> =
        pattern.mapIndexed { i, c ->
            val status = when (c) {
                'P' -> DayStatus.Perfect(1)
                'M' -> DayStatus.Missed(1)
                'N' -> DayStatus.NoHabits
                else -> DayStatus.Future
            }
            CalendarDay(date(start).plus(DatePeriod(days = i)), status)
        }

    @Test
    fun loneDayIsSingle() {
        // 2026-10-06 is a Tuesday.
        assertEquals(listOf(None, Single, None), streakBands(days("2026-10-05", "MPM")))
    }

    @Test
    fun runOfThreeIsStartMiddleEnd() {
        assertEquals(listOf(Start, Middle, End), streakBands(days("2026-10-06", "PPP")))
    }

    @Test
    fun runBreaksAtWeekRowEdge() {
        // Sat 10, Sun 11, Mon 12: the band closes on Sunday and restarts on Monday's row.
        assertEquals(listOf(Start, End, Single), streakBands(days("2026-10-10", "PPP")))
    }

    @Test
    fun missedAndRestDaysBreakTheBand() {
        assertEquals(
            listOf(Start, End, None, Single, None, Single),
            streakBands(days("2026-10-05", "PPMPNP")),
        )
    }

    @Test
    fun monthEdgesCloseTheBand() {
        // Thu 1 Oct 2026 starts the month mid-week; the first and last cells have no outer neighbor.
        assertEquals(listOf(Start, End), streakBands(days("2026-10-01", "PP")))
        assertEquals(listOf(Single), streakBands(days("2026-10-31", "P")))
    }

    @Test
    fun emptyMonthHasNoBands() {
        assertEquals(emptyList(), streakBands(emptyList()))
    }
}
