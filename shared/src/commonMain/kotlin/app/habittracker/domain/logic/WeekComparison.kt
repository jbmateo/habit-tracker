package app.habittracker.domain.logic

import app.habittracker.domain.model.CompletionIndex
import app.habittracker.domain.model.Habit
import app.habittracker.domain.model.WeekComparison
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.minus
import kotlinx.datetime.plus

/**
 * Completion rate for this week (Monday to [today]) against the full previous Monday–Sunday week.
 * Past days count every due check-in; today counts only check-ins already done, so the rate never
 * drops just because the day isn't over.
 */
fun weekComparison(habits: List<Habit>, completions: CompletionIndex, today: LocalDate): WeekComparison {
    val monday = today.minus(DatePeriod(days = today.dayOfWeek.isoDayNumber - 1))
    val lastMonday = monday.minus(DatePeriod(days = 7))
    return WeekComparison(
        thisWeek = rate(habits, completions, monday, today, today),
        lastWeek = rate(habits, completions, lastMonday, monday.minus(DatePeriod(days = 1)), today),
    )
}

private fun rate(habits: List<Habit>, completions: CompletionIndex, from: LocalDate, to: LocalDate, today: LocalDate): Double? {
    var due = 0
    var done = 0
    var day = from
    while (day <= to) {
        val doneIds = completions[day].orEmpty()
        for (habit in habits) {
            if (!habit.isDueOn(day)) continue
            val isDone = habit.id in doneIds
            if (day == today && !isDone) continue
            due++
            if (isDone) done++
        }
        day = day.plus(DatePeriod(days = 1))
    }
    return if (due == 0) null else done.toDouble() / due
}
