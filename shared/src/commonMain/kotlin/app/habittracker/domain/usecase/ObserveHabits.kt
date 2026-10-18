package app.habittracker.domain.usecase

import app.habittracker.domain.model.CategoryGroup
import app.habittracker.domain.model.Habit
import app.habittracker.domain.model.groupByCategory
import app.habittracker.domain.repository.HabitRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

data class HabitsOverview(val active: List<CategoryGroup<Habit>>, val archived: List<Habit>)

class ObserveHabits(private val habits: HabitRepository) {
    operator fun invoke(): Flow<HabitsOverview> =
        habits.observeAll().map { all ->
            val (archived, active) = all.partition { it.isArchived }
            HabitsOverview(active.groupByCategory { it.category }, archived)
        }
}
