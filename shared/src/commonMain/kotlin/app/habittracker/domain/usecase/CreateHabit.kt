package app.habittracker.domain.usecase

import app.habittracker.domain.model.Outcome
import app.habittracker.domain.repository.CategoryRepository
import app.habittracker.domain.repository.HabitRepository
import app.habittracker.domain.time.AppClock
import kotlinx.datetime.DayOfWeek

class CreateHabit(
    private val habits: HabitRepository,
    private val categories: CategoryRepository,
    private val clock: AppClock,
) {
    suspend operator fun invoke(
        name: String,
        iconKey: String,
        categoryId: String,
        days: Set<DayOfWeek>,
    ): Outcome<String> {
        val input = when (val r = validateHabitInput(categories, name, iconKey, categoryId, days)) {
            is Outcome.Failure -> return r
            is Outcome.Success -> r.value
        }
        return Outcome.Success(habits.create(input.name, iconKey, categoryId, clock.today(), input.days))
    }
}
