package app.habittracker.domain.usecase

import app.habittracker.domain.logic.isEditable
import app.habittracker.domain.model.DomainError
import app.habittracker.domain.model.Outcome
import app.habittracker.domain.repository.CompletionRepository
import app.habittracker.domain.repository.HabitRepository
import app.habittracker.domain.time.AppClock
import kotlinx.datetime.LocalDate

class ToggleCompletion(
    private val habits: HabitRepository,
    private val completions: CompletionRepository,
    private val clock: AppClock,
) {
    suspend operator fun invoke(habitId: String, date: LocalDate): Outcome<Boolean> {
        val habit = habits.get(habitId) ?: return Outcome.Failure(DomainError.HabitNotFound)
        if (!isEditable(date, clock.today())) return Outcome.Failure(DomainError.EditWindowClosed)
        if (!habit.isDueOn(date)) return Outcome.Failure(DomainError.NotDue)
        return Outcome.Success(completions.toggle(habitId, date))
    }
}
