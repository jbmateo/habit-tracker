package app.habittracker.domain.usecase

import app.habittracker.domain.model.DomainError
import app.habittracker.domain.model.Outcome
import app.habittracker.domain.repository.CompletionRepository
import app.habittracker.domain.repository.HabitRepository
import kotlinx.coroutines.flow.first
import kotlinx.datetime.LocalDate

/** "Mark done" from a reminder: checks the habit off for [date]; never un-checks one that is already done. */
class MarkDoneFromReminder(
    private val habits: HabitRepository,
    private val completions: CompletionRepository,
    private val toggle: ToggleCompletion,
) {
    suspend operator fun invoke(habitId: String, date: LocalDate): Outcome<Unit> {
        habits.get(habitId) ?: return Outcome.Failure(DomainError.HabitNotFound)
        if (date in completions.observeForHabit(habitId).first()) return Outcome.Success(Unit)
        return when (val result = toggle(habitId, date)) {
            is Outcome.Success -> Outcome.Success(Unit)
            is Outcome.Failure -> result
        }
    }
}
