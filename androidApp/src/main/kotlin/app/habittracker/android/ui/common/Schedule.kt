package app.habittracker.android.ui.common

import android.content.Context
import android.text.format.DateFormat
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalTime
import java.util.Calendar

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

/** A reminder time in the device's 12/24-hour style, e.g. "9:00 AM" or "09:00". */
fun formatReminderTime(context: Context, time: LocalTime): String {
    val calendar = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, time.hour)
        set(Calendar.MINUTE, time.minute)
    }
    return DateFormat.getTimeFormat(context).format(calendar.time)
}

/** " · ⏰ 9:00 AM" for a habit with a reminder, "" without one. */
fun reminderSuffix(context: Context, time: LocalTime?): String =
    time?.let { " · ⏰ ${formatReminderTime(context, it)}" }.orEmpty()
