package app.habittracker.domain.logic

import app.habittracker.domain.model.Habit
import app.habittracker.domain.model.HabitStats
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus

/**
 * Walks due days from creation to [today]. Done dates on non-due days are ignored. An undone [today]
 * is neither a miss nor counted in the rate; any earlier undone due day breaks the streak.
 */
fun habitStats(habit: Habit, doneDates: Set<LocalDate>, today: LocalDate): HabitStats {
    var run = 0
    var best = 0
    var counted = 0
    var done = 0
    var day = habit.createdOn
    while (day <= today) {
        if (habit.isDueOn(day)) {
            if (day in doneDates) {
                run++
                done++
                counted++
                if (run > best) best = run
            } else if (day < today) {
                run = 0
                counted++
            }
        }
        day = day.plus(DatePeriod(days = 1))
    }
    return HabitStats(
        currentStreak = run,
        bestStreak = best,
        completionRate = if (counted == 0) null else done.toDouble() / counted,
    )
}
