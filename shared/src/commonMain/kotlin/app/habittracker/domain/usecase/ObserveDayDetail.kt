package app.habittracker.domain.usecase

import app.habittracker.domain.logic.dayStatus
import app.habittracker.domain.logic.dueHabits
import app.habittracker.domain.logic.isEditable
import app.habittracker.domain.model.CategoryGroup
import app.habittracker.domain.model.DayStatus
import app.habittracker.domain.model.Habit
import app.habittracker.domain.model.groupByCategory
import app.habittracker.domain.model.toIndex
import app.habittracker.domain.repository.CompletionRepository
import app.habittracker.domain.repository.HabitRepository
import app.habittracker.domain.time.AppClock
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.datetime.LocalDate

data class DayHabitItem(val habit: Habit, val isDone: Boolean)

data class DayDetail(
    val date: LocalDate,
    val status: DayStatus,
    val isEditable: Boolean,
    val groups: List<CategoryGroup<DayHabitItem>>,
)

class ObserveDayDetail(
    private val habits: HabitRepository,
    private val completions: CompletionRepository,
    private val clock: AppClock,
) {
    operator fun invoke(date: LocalDate): Flow<DayDetail> =
        combine(habits.observeAll(), completions.observeAll(), clock.todayFlow) { allHabits, allCompletions, today ->
            val index = allCompletions.toIndex()
            val doneIds = index[date].orEmpty()
            DayDetail(
                date = date,
                status = dayStatus(date, today, allHabits, index),
                isEditable = isEditable(date, today),
                groups = dueHabits(allHabits, date)
                    .map { DayHabitItem(it, it.id in doneIds) }
                    .groupByCategory { it.habit.category },
            )
        }
}
