package app.habittracker.domain.logic

import app.habittracker.domain.model.CompletionIndex
import app.habittracker.domain.model.DayStatus
import app.habittracker.domain.model.Habit
import app.habittracker.domain.model.StreakInfo
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus

fun overallStreak(habits: List<Habit>, completions: CompletionIndex, today: LocalDate): StreakInfo {
    val statuses = datedStatuses(habits, completions, today).map { it.second }
    if (statuses.isEmpty()) return StreakInfo(0, 0)
    return StreakInfo(current = currentPerfectRun(statuses), best = longestPerfectRun(statuses))
}

/**
 * First day of the current perfect-day run, or null with no streak. Days with nothing due neither break nor extend
 * a run, so this is the earliest Perfect day after the last Partial/Missed one, not a fixed count of calendar days.
 */
fun overallRunStart(habits: List<Habit>, completions: CompletionIndex, today: LocalDate): LocalDate? {
    var start: LocalDate? = null
    for ((date, status) in datedStatuses(habits, completions, today).asReversed()) {
        when (status) {
            is DayStatus.Perfect -> start = date
            is DayStatus.Partial, is DayStatus.Missed -> return start
            DayStatus.NoHabits, is DayStatus.InProgress, DayStatus.Future -> Unit
        }
    }
    return start
}

/** Each day from the first habit's creation through [today], with its overall status. */
private fun datedStatuses(habits: List<Habit>, completions: CompletionIndex, today: LocalDate): List<Pair<LocalDate, DayStatus>> {
    val start = habits.minOfOrNull { it.createdOn } ?: return emptyList()
    return generateSequence(start) { it.plus(DatePeriod(days = 1)) }
        .takeWhile { it <= today }
        .map { it to dayStatus(it, today, habits, completions) }
        .toList()
}

/** Longest run of Perfect days; NoHabits/InProgress/Future are skipped, Partial/Missed reset it. */
fun longestPerfectRun(statuses: List<DayStatus>): Int {
    var best = 0
    var run = 0
    for (status in statuses) {
        when (status) {
            is DayStatus.Perfect -> { run++; if (run > best) best = run }
            is DayStatus.Partial, is DayStatus.Missed -> run = 0
            DayStatus.NoHabits, is DayStatus.InProgress, DayStatus.Future -> Unit
        }
    }
    return best
}

/** Perfect days counted backwards from the end until the first Partial/Missed. */
private fun currentPerfectRun(statuses: List<DayStatus>): Int {
    var run = 0
    for (status in statuses.asReversed()) {
        when (status) {
            is DayStatus.Perfect -> run++
            is DayStatus.Partial, is DayStatus.Missed -> return run
            DayStatus.NoHabits, is DayStatus.InProgress, DayStatus.Future -> Unit
        }
    }
    return run
}
