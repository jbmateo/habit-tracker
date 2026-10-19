package app.habittracker.domain.logic

import app.habittracker.domain.model.MilestoneSeen
import kotlinx.datetime.LocalDate

/** Perfect-day streak lengths worth celebrating once per run. */
val MILESTONES: List<Int> = listOf(7, 30, 100, 365)

/**
 * The highest milestone reached in the run starting [runStart] that hasn't been dismissed for that run, or null.
 * A dismissal from another run doesn't count, so a rebuilt streak celebrates again.
 */
fun pendingMilestone(currentStreak: Int, runStart: LocalDate?, seen: MilestoneSeen?): Int? {
    if (runStart == null) return null
    val dismissedUpTo = seen?.takeIf { it.runStart == runStart }?.milestone ?: 0
    return MILESTONES.lastOrNull { it <= currentStreak }?.takeIf { it > dismissedUpTo }
}
