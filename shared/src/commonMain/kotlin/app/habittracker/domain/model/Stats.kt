package app.habittracker.domain.model

data class StreakInfo(val current: Int, val best: Int)

/** [completionRate] is in 0.0..1.0, or null when no due day has been counted yet. */
data class HabitStats(val currentStreak: Int, val bestStreak: Int, val completionRate: Double?)
