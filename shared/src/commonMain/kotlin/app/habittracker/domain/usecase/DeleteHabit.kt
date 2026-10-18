package app.habittracker.domain.usecase

import app.habittracker.domain.model.DomainError
import app.habittracker.domain.model.Outcome
import app.habittracker.domain.repository.HabitRepository

class DeleteHabit(private val habits: HabitRepository) {
    suspend operator fun invoke(id: String): Outcome<Unit> {
        habits.get(id) ?: return Outcome.Failure(DomainError.HabitNotFound)
        habits.delete(id)
        return Outcome.Success(Unit)
    }
}
