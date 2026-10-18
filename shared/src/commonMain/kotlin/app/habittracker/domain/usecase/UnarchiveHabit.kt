package app.habittracker.domain.usecase

import app.habittracker.domain.model.DomainError
import app.habittracker.domain.model.Outcome
import app.habittracker.domain.repository.HabitRepository
import app.habittracker.domain.time.AppClock

class UnarchiveHabit(private val habits: HabitRepository, private val clock: AppClock) {
    suspend operator fun invoke(id: String): Outcome<Unit> {
        val habit = habits.get(id) ?: return Outcome.Failure(DomainError.HabitNotFound)
        val open = habit.archivePeriods.firstOrNull { it.toExclusive == null }
            ?: return Outcome.Success(Unit)
        val today = clock.today()
        if (open.from == today) habits.deleteArchivePeriod(id, open.from)
        else habits.closeArchivePeriod(id, open.from, today)
        return Outcome.Success(Unit)
    }
}
