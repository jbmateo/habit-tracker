package app.habittracker.domain.usecase

import app.habittracker.domain.logic.dayStatus
import app.habittracker.domain.logic.dueHabits
import app.habittracker.domain.logic.habitStats
import app.habittracker.domain.logic.overallStreak
import app.habittracker.domain.model.CategoryGroup
import app.habittracker.domain.model.DayStatus
import app.habittracker.domain.model.Habit
import app.habittracker.domain.model.StreakInfo
import app.habittracker.domain.model.groupByCategory
import app.habittracker.domain.model.toIndex
import app.habittracker.domain.repository.CompletionRepository
import app.habittracker.domain.repository.HabitRepository
import app.habittracker.domain.time.AppClock
import app.habittracker.domain.logic.weekComparison
import app.habittracker.domain.model.WeekComparison
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.datetime.LocalDate

data class TodayHabitItem(val habit: Habit, val isDone: Boolean, val streak: Int)

data class TodayDashboard(
    val date: LocalDate,
    val done: Int,
    val due: Int,
    val streak: StreakInfo,
    val isPerfect: Boolean,
    val groups: List<CategoryGroup<TodayHabitItem>>,
    val hasAnyHabits: Boolean,
    val week: WeekComparison,
)

class ObserveTodayDashboard(
    private val habits: HabitRepository,
    private val completions: CompletionRepository,
    private val clock: AppClock,
) {
    operator fun invoke(): Flow<TodayDashboard> =
        combine(habits.observeAll(), completions.observeAll(), clock.todayFlow) { allHabits, allCompletions, today ->
            val index = allCompletions.toIndex()
            val doneToday = index[today].orEmpty()
            val due = dueHabits(allHabits, today)
            val doneDatesByHabit = allCompletions.groupBy({ it.habitId }, { it.date }).mapValues { it.value.toSet() }
            val items = due.map { habit ->
                TodayHabitItem(
                    habit = habit,
                    isDone = habit.id in doneToday,
                    streak = habitStats(habit, doneDatesByHabit[habit.id].orEmpty(), today).currentStreak,
                )
            }
            TodayDashboard(
                date = today,
                done = items.count { it.isDone },
                due = items.size,
                streak = overallStreak(allHabits, index, today),
                isPerfect = dayStatus(today, today, allHabits, index) is DayStatus.Perfect,
                groups = items.groupByCategory { it.habit.category },
                hasAnyHabits = allHabits.any { !it.isArchived },
                week = weekComparison(allHabits, index, today),
            )
        }
}
