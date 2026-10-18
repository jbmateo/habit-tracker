package app.habittracker.presentation

import kotlinx.datetime.LocalDate
import kotlinx.datetime.YearMonth
import kotlinx.datetime.isoDayNumber

internal fun LocalDate.toYearMonth(): YearMonth = YearMonth(year, month)

/** Empty grid cells before the 1st of this month in a Monday-first week (Monday = 0). */
internal val YearMonth.leadingBlanks: Int get() = firstDay.dayOfWeek.isoDayNumber - 1
