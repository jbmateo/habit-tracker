package app.habittracker.domain.usecase

import app.habittracker.domain.logic.validateDays
import app.habittracker.domain.logic.validateName
import app.habittracker.domain.model.DomainError
import app.habittracker.domain.model.IconKeys
import app.habittracker.domain.model.Outcome
import app.habittracker.domain.repository.CategoryRepository
import kotlinx.datetime.DayOfWeek

internal data class ValidHabitInput(val name: String, val days: Set<DayOfWeek>)

/** Name, then days, then icon, then category existence. */
internal suspend fun validateHabitInput(
    categories: CategoryRepository,
    name: String,
    iconKey: String,
    categoryId: String,
    days: Set<DayOfWeek>,
): Outcome<ValidHabitInput> {
    val validName = when (val r = validateName(name)) {
        is Outcome.Failure -> return r
        is Outcome.Success -> r.value
    }
    val validDays = when (val r = validateDays(days)) {
        is Outcome.Failure -> return r
        is Outcome.Success -> r.value
    }
    if (iconKey !in IconKeys.all) return Outcome.Failure(DomainError.UnknownIcon)
    if (categories.get(categoryId) == null) return Outcome.Failure(DomainError.CategoryNotFound)
    return Outcome.Success(ValidHabitInput(validName, validDays))
}
