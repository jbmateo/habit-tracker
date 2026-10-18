package app.habittracker.domain.usecase

import app.habittracker.domain.model.Habit
import app.habittracker.domain.repository.HabitRepository

/** One-shot read for the edit form. */
class GetHabit(private val habits: HabitRepository) {
    suspend operator fun invoke(id: String): Habit? = habits.get(id)
}
