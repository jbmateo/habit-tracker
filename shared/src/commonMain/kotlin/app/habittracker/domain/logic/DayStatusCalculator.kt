package app.habittracker.domain.logic

import app.habittracker.domain.model.CompletionIndex
import app.habittracker.domain.model.DayStatus
import app.habittracker.domain.model.Habit
import kotlinx.datetime.LocalDate

fun dueHabits(habits: List<Habit>, date: LocalDate): List<Habit> = habits.filter { it.isDueOn(date) }

fun dayStatus(
    date: LocalDate,
    today: LocalDate,
    habits: List<Habit>,
    completions: CompletionIndex,
): DayStatus {
    if (date > today) return DayStatus.Future
    val due = dueHabits(habits, date)
    if (due.isEmpty()) return DayStatus.NoHabits
    val completedIds = completions[date].orEmpty()
    val done = due.count { it.id in completedIds }
    return when {
        done == due.size -> DayStatus.Perfect(due.size)
        date == today -> DayStatus.InProgress(done, due.size)
        done > 0 -> DayStatus.Partial(done, due.size)
        else -> DayStatus.Missed(due.size)
    }
}
