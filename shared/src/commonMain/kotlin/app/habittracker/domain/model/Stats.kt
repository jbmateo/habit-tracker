package app.habittracker.domain.model

data class StreakInfo(val current: Int, val best: Int)

/** [completionRate] is in 0.0..1.0, or null when no due day has been counted yet. */
data class HabitStats(val currentStreak: Int, val bestStreak: Int, val completionRate: Double?)

/** Completion rates (done ÷ due, 0.0..1.0) for this week so far and last week; `null` when nothing was due. */
data class WeekComparison(val thisWeek: Double?, val lastWeek: Double?)

/** How good a completion rate is: Good ≥ 80%, Fair 50–79%, Low < 50% (of the rounded percent shown). */
enum class CompletionLevel { Good, Fair, Low }
