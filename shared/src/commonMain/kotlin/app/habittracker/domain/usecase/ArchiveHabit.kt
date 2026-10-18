package app.habittracker.domain.usecase

import app.habittracker.domain.model.DomainError
import app.habittracker.domain.model.Outcome
import app.habittracker.domain.repository.HabitRepository
import app.habittracker.domain.time.AppClock

class ArchiveHabit(private val habits: HabitRepository, private val clock: AppClock) {
    suspend operator fun invoke(id: String): Outcome<Unit> {
        val habit = habits.get(id) ?: return Outcome.Failure(DomainError.HabitNotFound)
        if (!habit.isArchived) habits.openArchivePeriod(id, clock.today())
        return Outcome.Success(Unit)
    }
}
