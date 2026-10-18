package app.habittracker.domain.usecase

import app.habittracker.domain.model.DomainError
import app.habittracker.domain.model.Outcome
import app.habittracker.domain.model.ScheduleVersion
import app.habittracker.domain.repository.CategoryRepository
import app.habittracker.domain.repository.HabitRepository
import app.habittracker.domain.time.AppClock
import kotlinx.datetime.DayOfWeek

class UpdateHabit(
    private val habits: HabitRepository,
    private val categories: CategoryRepository,
    private val clock: AppClock,
) {
    suspend operator fun invoke(
        id: String,
        name: String,
        iconKey: String,
        categoryId: String,
        days: Set<DayOfWeek>,
    ): Outcome<Unit> {
        val habit = habits.get(id) ?: return Outcome.Failure(DomainError.HabitNotFound)
        val input = when (val r = validateHabitInput(categories, name, iconKey, categoryId, days)) {
            is Outcome.Failure -> return r
            is Outcome.Success -> r.value
        }
        val schedule = if (input.days != habit.currentSchedule.days) ScheduleVersion(clock.today(), input.days) else null
        habits.update(id, input.name, iconKey, categoryId, schedule)
        return Outcome.Success(Unit)
    }
}
