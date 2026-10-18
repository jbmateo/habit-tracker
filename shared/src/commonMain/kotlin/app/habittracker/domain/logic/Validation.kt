package app.habittracker.domain.logic

import app.habittracker.domain.model.DomainError
import app.habittracker.domain.model.Outcome
import kotlinx.datetime.DayOfWeek

const val MAX_NAME_LENGTH = 40

fun validateName(raw: String): Outcome<String> {
    val name = raw.trim()
    return if (name.length in 1..MAX_NAME_LENGTH) Outcome.Success(name)
    else Outcome.Failure(DomainError.InvalidName)
}

fun validateDays(days: Set<DayOfWeek>): Outcome<Set<DayOfWeek>> =
    if (days.isEmpty()) Outcome.Failure(DomainError.NoDaysSelected) else Outcome.Success(days)
