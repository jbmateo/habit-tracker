package app.habittracker.presentation.calendar

import app.habittracker.domain.model.DayStatus
import app.habittracker.domain.usecase.CalendarDay
import app.habittracker.domain.usecase.HabitDayState
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate

/** Where a day sits in the highlighted band joining consecutive streak days on a month grid. */
enum class StreakBand { None, Single, Start, Middle, End }

/** Bands for the main calendar: consecutive Perfect days join. */
fun streakBands(days: List<CalendarDay>): List<StreakBand> =
    bands(days.map { it.date }) { i -> days[i].status is DayStatus.Perfect }

/** Bands for one habit's calendar: consecutive Done days join; not-due days break the band. */
fun habitStreakBands(days: List<Pair<LocalDate, HabitDayState>>): List<StreakBand> =
    bands(days.map { it.first }) { i -> days[i].second == HabitDayState.Done }

/**
 * One band position per date. Only adjacent streak days join; any other day breaks the band.
 * Bands also break at the Monday-first week row edges so each grid row draws its own rounded piece.
 */
private fun bands(dates: List<LocalDate>, isStreak: (Int) -> Boolean): List<StreakBand> {
    fun streakAt(i: Int) = i in dates.indices && isStreak(i)
    return dates.mapIndexed { i, date ->
        if (!streakAt(i)) return@mapIndexed StreakBand.None
        val joinsLeft = streakAt(i - 1) && date.dayOfWeek != DayOfWeek.MONDAY
        val joinsRight = streakAt(i + 1) && date.dayOfWeek != DayOfWeek.SUNDAY
        when {
            joinsLeft && joinsRight -> StreakBand.Middle
            joinsLeft -> StreakBand.End
            joinsRight -> StreakBand.Start
            else -> StreakBand.Single
        }
    }
}
