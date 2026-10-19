package app.habittracker.domain.logic

import app.habittracker.domain.model.MilestoneSeen
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus

/** Perfect-day streak lengths worth celebrating once per run. */
val MILESTONES: List<Int> = listOf(7, 30, 100, 365)

/**
 * First day of the current perfect-day run, or null with no streak. The streak counts through yesterday while today
 * is still undone, and includes today once [todayDone].
 */
fun streakRunStart(today: LocalDate, currentStreak: Int, todayDone: Boolean): LocalDate? {
    if (currentStreak <= 0) return null
    val lastDay = if (todayDone) today else today.minus(DatePeriod(days = 1))
    return lastDay.minus(DatePeriod(days = currentStreak - 1))
}

/**
 * The highest milestone reached in the run starting [runStart] that hasn't been dismissed for that run, or null.
 * A dismissal from another run doesn't count, so a rebuilt streak celebrates again.
 */
fun pendingMilestone(currentStreak: Int, runStart: LocalDate?, seen: MilestoneSeen?): Int? {
    if (runStart == null) return null
    val dismissedUpTo = seen?.takeIf { it.runStart == runStart }?.milestone ?: 0
    return MILESTONES.lastOrNull { it <= currentStreak }?.takeIf { it > dismissedUpTo }
}
