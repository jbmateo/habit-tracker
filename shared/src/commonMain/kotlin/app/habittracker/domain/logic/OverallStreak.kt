package app.habittracker.domain.logic

import app.habittracker.domain.model.CompletionIndex
import app.habittracker.domain.model.DayStatus
import app.habittracker.domain.model.Habit
import app.habittracker.domain.model.StreakInfo
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus

fun overallStreak(habits: List<Habit>, completions: CompletionIndex, today: LocalDate): StreakInfo {
    val start = habits.minOfOrNull { it.createdOn } ?: return StreakInfo(0, 0)
    if (start > today) return StreakInfo(0, 0)
    val statuses = generateSequence(start) { it.plus(DatePeriod(days = 1)) }
        .takeWhile { it <= today }
        .map { dayStatus(it, today, habits, completions) }
        .toList()
    return StreakInfo(current = currentPerfectRun(statuses), best = longestPerfectRun(statuses))
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
