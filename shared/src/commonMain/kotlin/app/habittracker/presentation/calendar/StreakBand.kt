package app.habittracker.presentation.calendar

import app.habittracker.domain.model.DayStatus
import app.habittracker.domain.usecase.CalendarDay
import kotlinx.datetime.DayOfWeek

/** Where a day sits in the highlighted band joining consecutive Perfect days on the calendar grid. */
enum class StreakBand { None, Single, Start, Middle, End }

/**
 * One band position per day. Only adjacent Perfect days join; any other day breaks the band.
 * Bands also break at the Monday-first week row edges so each grid row draws its own rounded piece.
 */
fun streakBands(days: List<CalendarDay>): List<StreakBand> =
    days.mapIndexed { i, day ->
        if (!days.isPerfect(i)) return@mapIndexed StreakBand.None
        val joinsLeft = days.isPerfect(i - 1) && day.date.dayOfWeek != DayOfWeek.MONDAY
        val joinsRight = days.isPerfect(i + 1) && day.date.dayOfWeek != DayOfWeek.SUNDAY
        when {
            joinsLeft && joinsRight -> StreakBand.Middle
            joinsLeft -> StreakBand.End
            joinsRight -> StreakBand.Start
            else -> StreakBand.Single
        }
    }

private fun List<CalendarDay>.isPerfect(index: Int): Boolean = getOrNull(index)?.status is DayStatus.Perfect
