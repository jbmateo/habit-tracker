package app.habittracker.domain.reminders

import app.habittracker.domain.logic.habitStats
import app.habittracker.domain.model.CompletionIndex
import app.habittracker.domain.model.Habit
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.plus

/**
 * Reminders for the next [days] days (today included): one per habit with a reminder time, on each day the habit
 * is due, not yet done, and whose reminder moment is still after [now]. Sorted by date, time, name; capped at
 * [MAX_PLANNED_REMINDERS], keeping the earliest.
 */
fun planReminders(
    habits: List<Habit>,
    completions: CompletionIndex,
    now: LocalDateTime,
    days: Int = REMINDER_WINDOW_DAYS,
): List<PlannedReminder> {
    val today = now.date
    val withReminder = habits.filter { it.reminderTime != null }
    val doneDates = withReminder.associate { habit ->
        habit.id to completions.filterValues { habit.id in it }.keys
    }
    val planned = mutableListOf<PlannedReminder>()
    for (offset in 0 until days) {
        val date = today.plus(DatePeriod(days = offset))
        for (habit in withReminder) {
            val time = habit.reminderTime ?: continue
            if (!habit.isDueOn(date)) continue
            if (habit.id in completions[date].orEmpty()) continue
            if (LocalDateTime(date, time) <= now) continue
            val streak = habitStats(habit, doneDates.getValue(habit.id), today).currentStreak
            planned += PlannedReminder(habit.id, habit.name, habit.iconKey, date, time, streak)
        }
    }
    return planned
        .sortedWith(compareBy<PlannedReminder>({ it.date }, { it.time }, { it.habitName }))
        .take(MAX_PLANNED_REMINDERS)
}
