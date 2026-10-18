package app.habittracker.android.ui.common

import kotlinx.datetime.DayOfWeek

private val shortNames = mapOf(
    DayOfWeek.MONDAY to "Mon",
    DayOfWeek.TUESDAY to "Tue",
    DayOfWeek.WEDNESDAY to "Wed",
    DayOfWeek.THURSDAY to "Thu",
    DayOfWeek.FRIDAY to "Fri",
    DayOfWeek.SATURDAY to "Sat",
    DayOfWeek.SUNDAY to "Sun",
)

/** "Every day" or Monday-first names joined with " · ", e.g. "Mon · Wed · Fri". */
fun scheduleSummary(days: Set<DayOfWeek>): String =
    if (days.size == 7) "Every day"
    else DayOfWeek.entries.filter { it in days }.joinToString(" · ") { shortNames.getValue(it) }
